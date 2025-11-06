package cod.compiler;

// --- MODIFIED IMPORTS ---
import static cod.compiler.MTOTRegistry.*;
import static cod.compiler.MTOTRegistry.AArch64Registers.*;
import static cod.compiler.MTOTRegistry.x86_64Registers.*; // <-- IMPORT x86 REGS
import cod.compiler.BytecodeInstruction.Opcode; // Import Opcode
// --- END MODIFIED IMPORTS ---

import cod.ast.nodes.*;
import cod.debug.DebugSystem;
import java.util.*;
import java.lang.reflect.Field; // For swapping assembly target (workaround)

public class MTOTNativeCompiler {

    // --- MODIFIED FIELD ---
    final CPUProfile cpuProfile;
    // --- END MODIFIED FIELD ---
    
    // --- NEW ABSTRACTION FIELDS ---
    private final String thisRegister; // Replaces hardcoded 'x19'
    private final Set<String> calleeSavedSet; // Arch-specific callee-saved regs
    private final Set<String> abiCallerSavedSet; // Arch-specific caller-saved regs (for spilling)
    // --- END NEW ---

    // --- Core Components ---
    private final RegisterManager registerManager;
    private final RegisterManager.RegisterAllocator registerAllocator;
    private final RegisterManager.RegisterSpiller spiller;
    private final OperandStack operandStack; // Now an inner class

    // --- Assembly/Data Lists ---
    private List<String> assemblyCode = new ArrayList<String>(); // Current target list
    private final List<String> dataSection = new ArrayList<String>();

    // --- State Counters/Helpers ---
    private int dataLabelCounter = 0;
    private String currentMethodName = "";
    private final List<String> argumentRegisters;
    // --- NEW STATE ---
    private int currentPc = 0; // Current bytecode instruction index
    private List<BytecodeInstruction> currentMethodBytecode = null;
    private int currentLoopDepth = 0; // Current loop nesting depth
    // --- END NEW STATE ---


    // --- Inner Class Helpers ---
    private final RuntimeCallHelper runtimeCallHelper;
    private final OperationCompiler operationCompiler;
    private final MemoryAccessCompiler memoryAccessCompiler;
    private final ControlFlowCompiler controlFlowCompiler;
    private final MethodCallCompiler methodCallCompiler;

    // --- MODIFIED CONSTRUCTOR ---
    public MTOTNativeCompiler(CPUProfile cpuProfile) {
        this.cpuProfile = cpuProfile;

        // --- Initialize Arch-Specific Abstractions ---
        if (cpuProfile.architecture.equals("aarch64")) {
            this.thisRegister = AArch64Registers.x19;
            this.calleeSavedSet = new HashSet<String>(Arrays.asList(
                x19, x20, x21, x22, x23, x24, x25, x26, x27, x28
            ));
            this.abiCallerSavedSet = new HashSet<String>(Arrays.asList(
                x0, x1, x2, x3, x4, x5, x6, x7,
                x9, x10, x11, x12, x13, x14, x15, x16, x17 // x16, x17 are scratch but caller-saved
            ));
        } else { // x86_64
            // Use r15 as the dedicated 'this' register (callee-saved)
            this.thisRegister = x86_64Registers.r15; 
            this.calleeSavedSet = new HashSet<String>(Arrays.asList(
                x86_64Registers.rbx, x86_64Registers.r12, x86_64Registers.r13, 
                x86_64Registers.r14, x86_64Registers.r15
            ));
            this.abiCallerSavedSet = new HashSet<String>(Arrays.asList(
                x86_64Registers.rax, x86_64Registers.rcx, x86_64Registers.rdx,
                x86_64Registers.rsi, x86_64Registers.rdi, x86_64Registers.r8,
                x86_64Registers.r9, x86_64Registers.r10, x86_64Registers.r11
            ));
        }
        // --- END ---

        // --- Initialize Core Components ---
        this.registerManager = new RegisterManager(this);
        this.registerAllocator = this.registerManager.getAllocator();
        this.spiller = this.registerManager.getSpiller();
        this.operandStack = new OperandStack();
        this.argumentRegisters = cpuProfile.registerFile.argumentRegisters;

        // --- Initialize Inner Class Helpers ---
        this.runtimeCallHelper = new RuntimeCallHelper();
        this.operationCompiler = new OperationCompiler();
        this.memoryAccessCompiler = new MemoryAccessCompiler();
        this.controlFlowCompiler = new ControlFlowCompiler();
        this.methodCallCompiler = new MethodCallCompiler();
    }
    // --- END MODIFIED CONSTRUCTOR ---

    // --- NEW GETTERS for Spiller access ---
    public int getCurrentPc() { return currentPc; }
    public List<BytecodeInstruction> getCurrentMethodBytecode() { return currentMethodBytecode; }
    public int getCurrentLoopDepth() { return currentLoopDepth; }
    // --- END NEW GETTERS ---

    public String compileMethodFromBytecode(String methodName, List<BytecodeInstruction> bytecode) {
        DebugSystem.debug("MTOT", "Compiling method from bytecode: " + methodName);

        // --- Reset state for the method ---
        operandStack.clear();
        dataSection.clear();
        registerManager.reset(); // Resets allocator and spiller
        memoryAccessCompiler.reset(); // Resets field/slot/string literal state
        dataLabelCounter = 0;
        this.currentMethodName = methodName;
        this.currentMethodBytecode = bytecode; // <-- Store bytecode
        this.currentPc = 0;                  // <-- Reset PC
        this.currentLoopDepth = 0;           // <-- Reset loop depth
        // --- End Reset ---

        // --- STAGE 1: Compile to temporary assembly ---
        List<String> tempAssemblyCode = new ArrayList<String>();
        List<String> originalAssemblyCodeRef = this.assemblyCode;
        setAssemblyTarget(tempAssemblyCode);

        // --- MODIFIED: Use thisRegister abstract field ---
        registerAllocator.markRegisterUsed(this.thisRegister); // Mark "this" register used early
        spiller.updateRegisterDefinitionDepth(this.thisRegister, 0); // Mark 'this' as defined outside loops
        // --- END MODIFIED ---

        // Mark incoming argument registers as used initially
        // --- MODIFIED: x86 arg regs for params start at index 0 ---
        int maxParamsInRegs;
        int argStartIndex;
        if (cpuProfile.architecture.equals("aarch64")) {
            maxParamsInRegs = argumentRegisters.size() - 1;
            argStartIndex = 1;
        } else {
            // x86: rdi, rsi, rdx, rcx, r8, r9 are params. rax is return-only.
            maxParamsInRegs = (argumentRegisters.contains(x86_64Registers.rax)) ? 
                                argumentRegisters.size() - 1 : 
                                argumentRegisters.size();
            argStartIndex = 0;
        }

        for (int i = 0; i < maxParamsInRegs; i++) {
             String argReg = argumentRegisters.get(i + argStartIndex);
             // --- END MODIFIED ---
             registerAllocator.markRegisterUsed(argReg);
             spiller.updateRegisterDefinitionDepth(argReg, 0); // Args defined outside loops
        }

        // Compile the actual bytecode instructions
        compileFromBytecode(bytecode, new HashMap<String, String>()); // Pass empty map initially

        setAssemblyTarget(originalAssemblyCodeRef);
        // --- END STAGE 1 ---


        // --- STAGE 2: Analyze Usage and Construct Final Assembly ---
        Set<String> usedCalleeSavedRegs = new HashSet<String>();
        
        // --- MODIFIED: Use calleeSavedSet abstract field ---
        Set<String> writtenRegisters = spiller.getWrittenRegistersDuringCompilation();
        writtenRegisters.retainAll(this.calleeSavedSet);
        // --- END MODIFIED ---
        
        usedCalleeSavedRegs.addAll(writtenRegisters);
        
        // --- MODIFIED: Use thisRegister abstract field ---
        if (registerAllocator.getUsedRegisters().contains(this.thisRegister)) {
             usedCalleeSavedRegs.add(this.thisRegister);
        }
        // --- END MODIFIED ---

        List<String> calleeSavedToSave = new ArrayList<String>(usedCalleeSavedRegs);
        Collections.sort(calleeSavedToSave); // Sort for deterministic order

        int spillAreaSize = spiller.getTotalSpillSize();
        // --- MODIFIED: Stack size calculation differs ---
        int calleeSavedAreaSize;
        if (cpuProfile.architecture.equals("aarch64")) {
            calleeSavedAreaSize = calleeSavedToSave.size() * 8; // ARM saves to stack frame
        } else {
            calleeSavedAreaSize = 0; // x86 pushes, so it's not part of the 'sub rsp'
        }
        // --- END MODIFIED ---
        
        int sizeBelowFp = calleeSavedAreaSize + spillAreaSize;
        int alignedSizeBelowFp = (sizeBelowFp + 15) & ~15; // 16-byte alignment

        List<String> finalAssembly = new ArrayList<String>();

        if (!dataSection.isEmpty()) {
            finalAssembly.add(cpuProfile.syntax.dataSection);
            finalAssembly.addAll(dataSection);
        }

        finalAssembly.add(cpuProfile.syntax.textSection);
        
        // --- ============================================ ---
        // --- NEW: Add extern declarations for x86_64/NASM ---
        // --- ============================================ ---
        if (cpuProfile.architecture.equals("x86_64")) {
            finalAssembly.add("    " + cpuProfile.syntax.commentMarker + " NASM requires extern declarations for runtime functions");
            finalAssembly.add("    extern runtime_print");
            finalAssembly.add("    extern string_concat");
            finalAssembly.add("    extern runtime_int_to_string");
            finalAssembly.add("    extern array_new");
            finalAssembly.add("    extern array_load");
            finalAssembly.add("    extern array_store");
            finalAssembly.add("    extern runtime_read_input");
            finalAssembly.add(""); // Add a newline for readability
        }
        // --- END NEW ---
        
        finalAssembly.add(cpuProfile.syntax.globalDirective.replace("{name}", methodName));
        finalAssembly.add(methodName + ":");

        // --- ============================================ ---
        // --- REBUILT: Architecture-Agnostic PROLOGUE ---
        // --- ============================================ ---
        InstructionPattern prologuePattern = cpuProfile.getPattern("prologue");
        if (prologuePattern == null) throw new RuntimeException("Missing 'prologue' pattern!");
        for (String template : prologuePattern.assemblyTemplate) {
            finalAssembly.add("    " + template);
        }

        if (cpuProfile.architecture.equals("aarch64")) {
            // AArch64: Allocate stack space, then save callee regs into it
            if (alignedSizeBelowFp > 0) {
                String asm = cpuProfile.getPattern("alloc_stack_frame").assemblyTemplate.get(0)
                    .replace("{size}", String.valueOf(alignedSizeBelowFp));
                finalAssembly.add("    " + asm);
            }
            
            int currentCalleeSaveOffset = -16; // Offset from FP (x29)
            if (!calleeSavedToSave.isEmpty()) {
                finalAssembly.add("    " + cpuProfile.syntax.commentMarker + " Saving callee-saved registers: " + calleeSavedToSave);
                InstructionPattern savePair = cpuProfile.getPattern("save_callee_reg_pair");
                InstructionPattern saveSingle = cpuProfile.getPattern("save_callee_reg_single");

                for (int i = 0; i < calleeSavedToSave.size(); i += 2) {
                    if (i + 1 < calleeSavedToSave.size()) {
                        finalAssembly.add("    " + savePair.assemblyTemplate.get(0)
                            .replace("{reg1}", calleeSavedToSave.get(i))
                            .replace("{reg2}", calleeSavedToSave.get(i+1))
                            .replace("{offset}", String.valueOf(currentCalleeSaveOffset)));
                    } else {
                        finalAssembly.add("    " + saveSingle.assemblyTemplate.get(0)
                            .replace("{reg1}", calleeSavedToSave.get(i))
                            .replace("{offset}", String.valueOf(currentCalleeSaveOffset)));
                    }
                     currentCalleeSaveOffset -= 16;
                }
            }
            spiller.setBaseSpillOffset(currentCalleeSaveOffset + 8);

        } else { // x86_64
            // x86_64: Save callee regs (push), then allocate stack space for spills
            int x86SpillAreaSize = (spiller.getTotalSpillSize() + 15) & ~15;

            if (!calleeSavedToSave.isEmpty()) {
                finalAssembly.add("    " + cpuProfile.syntax.commentMarker + " Saving callee-saved registers: " + calleeSavedToSave);
                InstructionPattern saveSingle = cpuProfile.getPattern("save_callee_reg_single");
                if (saveSingle.assemblyTemplate.isEmpty()) throw new RuntimeException("Missing 'save_callee_reg_single' pattern for x86!");
                for (String reg : calleeSavedToSave) {
                    finalAssembly.add("    " + saveSingle.assemblyTemplate.get(0).replace("{reg}", reg));
                }
            }
            
            if (x86SpillAreaSize > 0) {
                 String asm = cpuProfile.getPattern("alloc_stack_frame").assemblyTemplate.get(0)
                    .replace("{size}", String.valueOf(x86SpillAreaSize));
                finalAssembly.add("    " + asm);
            }
            // On x86, spill offsets are negative relative to RBP
            spiller.setBaseSpillOffset(-8); // Spiller will decrement from here
        }
        // --- END PROLOGUE ---

        // --- MODIFIED: Use thisRegister abstract field ---
        finalAssembly.add("    mov " + this.thisRegister + ", " + argumentRegisters.get(0) + " " + cpuProfile.syntax.commentMarker + " Copy 'this' pointer");
        // --- END MODIFIED ---
        
        finalAssembly.addAll(tempAssemblyCode); // Add the compiled body

        // --- ============================================ ---
        // --- REBUILT: Architecture-Agnostic EPILOGUE ---
        // --- ============================================ ---
        
        // --- Slot/Return Value Handling (Unchanged) ---
        Map<Integer, String> slotLocations = memoryAccessCompiler.getSlotLocations();
        String returnReg0 = (cpuProfile.architecture.equals("x86_64")) ? x86_64Registers.rax : argumentRegisters.get(0);
        
        if (!slotLocations.isEmpty()) {
             List<Integer> sortedSlotIndices = new ArrayList<Integer>(slotLocations.keySet());
             Collections.sort(sortedSlotIndices);
             int returnRegIndex = 0;
             for (Integer slotIndex : sortedSlotIndices) {
                 
                 String abiReturnReg;
                 // --- MODIFIED: Handle x86 return regs (rax, rdx) ---
                 if (cpuProfile.architecture.equals("x86_64")) {
                     if (returnRegIndex == 0) abiReturnReg = x86_64Registers.rax;
                     else if (returnRegIndex == 1) abiReturnReg = x86_64Registers.rdx;
                     else break; // x86 only has 2 return regs
                 } else { // AArch64
                     if (returnRegIndex >= argumentRegisters.size()) break;
                     abiReturnReg = argumentRegisters.get(returnRegIndex);
                 }
                 // --- END MODIFIED ---
                 
                 String valueLocation = slotLocations.get(slotIndex);
                 if (valueLocation.startsWith("spill_")) {
                     int offset = Integer.parseInt(valueLocation.substring("spill_".length()));
                     spiller.fillRegisterFromOffset(abiReturnReg, offset);
                 } else {
                     spiller.fillRegister(valueLocation);
                     if (!valueLocation.equals(abiReturnReg)) {
                         finalAssembly.add("    mov " + abiReturnReg + ", " + valueLocation);
                     }
                 }
                 spiller.markRegisterModified(abiReturnReg);
                 registerAllocator.markRegisterUsed(abiReturnReg);
                 returnRegIndex++;
             }
        } else if (!operandStack.isEmpty()) {
            String retValReg = operandStack.popToRegister();
            spiller.fillRegister(retValReg);
            
            // --- MODIFIED: Use arch-specific return reg ---
            String abiReturnReg = returnReg0;
            // --- END MODIFIED ---
            
            if (!retValReg.equals(abiReturnReg)) {
                finalAssembly.add("    mov " + abiReturnReg + ", " + retValReg);
                spiller.markRegisterModified(abiReturnReg);
                registerAllocator.freeRegister(retValReg);
            } else {
                 spiller.markRegisterModified(abiReturnReg);
            }
            registerAllocator.markRegisterUsed(abiReturnReg);
        } else {
            // --- MODIFIED: Use arch-specific return reg ---
            String abiReturnReg = returnReg0;
            InstructionPattern p = cpuProfile.getPattern("load_immediate_int");
            assemblyCode.add("    " + p.assemblyTemplate.get(0).replace("{dest}", abiReturnReg).replace("{value}", "0") + " " + cpuProfile.syntax.commentMarker + " Default return 0");
            // --- END MODIFIED ---
            spiller.markRegisterModified(abiReturnReg);
        }
        // --- End Slot/Return Value Handling ---


        InstructionPattern epiloguePattern = cpuProfile.getPattern("epilogue");
        if (epiloguePattern == null) throw new RuntimeException("Missing 'epilogue' pattern!");

        if (cpuProfile.architecture.equals("aarch64")) {
            // AArch64: Restore callee regs from [fp, #offset], dealloc stack, ldp, ret
            if (!calleeSavedToSave.isEmpty()) {
                 finalAssembly.add("    " + cpuProfile.syntax.commentMarker + " Restoring callee-saved registers: " + calleeSavedToSave);
                 InstructionPattern restorePair = cpuProfile.getPattern("restore_callee_reg_pair");
                 InstructionPattern restoreSingle = cpuProfile.getPattern("restore_callee_reg_single");
                 // Calculate starting offset (reverse of saving)
                 int calleeSaveSlots = (calleeSavedToSave.size() / 2) + (calleeSavedToSave.size() % 2);
                 int currentCalleeSaveOffset = -16 - (calleeSaveSlots - 1) * 16;

                 for (int i = 0; i < calleeSavedToSave.size(); i += 2) {
                     if (i + 1 < calleeSavedToSave.size()) {
                         finalAssembly.add("    " + restorePair.assemblyTemplate.get(0)
                             .replace("{reg1}", calleeSavedToSave.get(i))
                             .replace("{reg2}", calleeSavedToSave.get(i+1))
                             .replace("{offset}", String.valueOf(currentCalleeSaveOffset)));
                     } else {
                          finalAssembly.add("    " + restoreSingle.assemblyTemplate.get(0)
                             .replace("{reg1}", calleeSavedToSave.get(i))
                             .replace("{offset}", String.valueOf(currentCalleeSaveOffset)));
                     }
                     currentCalleeSaveOffset += 16;
                 }
            }
            
            if (alignedSizeBelowFp > 0) {
                // This is arm_epilogue_1
                finalAssembly.add("    " + epiloguePattern.assemblyTemplate.get(0));
            }
            
            // Add arm_epilogue_2 and arm_epilogue_3
            for (int i = 1; i < epiloguePattern.assemblyTemplate.size(); i++) {
                 finalAssembly.add("    " + epiloguePattern.assemblyTemplate.get(i));
            }

        } else { // x86_64
            // x86_64: Dealloc stack (mov rsp, rbp), restore callee regs (pop), pop rbp, ret
            int x86SpillAreaSize = (spiller.getTotalSpillSize() + 15) & ~15;
            
            // x86_epilogue_1 (mov rsp, rbp) should always run
            finalAssembly.add("    " + epiloguePattern.assemblyTemplate.get(0));
            
            if (!calleeSavedToSave.isEmpty()) {
                 finalAssembly.add("    " + cpuProfile.syntax.commentMarker + " Restoring callee-saved registers: " + calleeSavedToSave);
                 InstructionPattern restoreSingle = cpuProfile.getPattern("restore_callee_reg_single");
                 // Restore in reverse order of push
                 List<String> reversedSaves = new ArrayList<>(calleeSavedToSave);
                 Collections.reverse(reversedSaves);
                 for (String reg : reversedSaves) {
                    finalAssembly.add("    " + restoreSingle.assemblyTemplate.get(0).replace("{reg}", reg));
                }
            }
            
            // Add x86_epilogue_2 (pop rbp) and x86_epilogue_3 (ret)
            for (int i = 1; i < epiloguePattern.assemblyTemplate.size(); i++) {
                 finalAssembly.add("    " + epiloguePattern.assemblyTemplate.get(i));
            }
        }
        // --- END EPILOGUE ---

        operandStack.clear();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < finalAssembly.size(); i++) {
            sb.append(finalAssembly.get(i));
            if (i < finalAssembly.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private void setAssemblyTarget(List<String> targetList) {
         try { Field listField = MTOTNativeCompiler.class.getDeclaredField("assemblyCode"); listField.setAccessible(true); listField.set(this, targetList); }
         catch (Exception e) { throw new RuntimeException("Failed to swap assembly target list via reflection", e); }
    }

    private void compileFromBytecode(List<BytecodeInstruction> bytecode, Map<String, String> localRegisterMap) {
        // --- Label Pre-pass (Unchanged) ---
        Map<String, String> labelNameMap = new HashMap<String, String>();
        Map<Integer, List<String>> addressToLabelMap = new HashMap<Integer, List<String>>();
        int assemblyLabelCounter = 0;
        for (int i = 0; i < bytecode.size(); i++) {
            BytecodeInstruction instr = bytecode.get(i);
            if (instr.opcode == BytecodeInstruction.Opcode.LABEL) {
                String bytecodeLabel = (String) instr.operand;
                String assemblyLabel = "L_" + this.currentMethodName + "_" + (assemblyLabelCounter++);
                labelNameMap.put(bytecodeLabel, assemblyLabel);
                int targetAddress = i;
                if (!addressToLabelMap.containsKey(targetAddress)) {
                    addressToLabelMap.put(targetAddress, new ArrayList<String>());
                }
                addressToLabelMap.get(targetAddress).add(assemblyLabel);
            }
        }

        // --- Compilation Pass ---
        for (int i = 0; i < bytecode.size(); i++) {
            this.currentPc = i; // <-- UPDATE PC

            // --- Detect Loop Depth (Heuristic based on labels) ---
            if (addressToLabelMap.containsKey(i)) {
                for(String label : addressToLabelMap.get(i)) {
                    // --- MODIFIED: Don't indent labels ---
                    assemblyCode.add(label + ":");
                    // --- END MODIFIED ---
                    // Simple heuristic: If label name contains "loop_start" or "loop_body", increment depth
                    if (label.contains("loop_start") || label.contains("loop_body")) {
                        currentLoopDepth++;
                        DebugSystem.debug("LOOP_DEPTH", "Entered loop, depth = " + currentLoopDepth + " at label " + label);
                    }
                    // Simple heuristic: If label name contains "loop_end", decrement depth
                    else if (label.contains("loop_end")) {
                         if (currentLoopDepth > 0) {
                             currentLoopDepth--;
                             DebugSystem.debug("LOOP_DEPTH", "Exited loop, depth = " + currentLoopDepth + " at label " + label);
                         } else {
                             DebugSystem.warn("LOOP_DEPTH", "Attempted to exit loop at depth 0, label " + label);
                         }
                    }
                }
            }
            // --- End Loop Depth Detection ---


            BytecodeInstruction instr = bytecode.get(i);
            try {
                switch (instr.opcode) {
                    // --- Memory / Stack ---
                    case PUSH_INT:    memoryAccessCompiler.compilePushInt((Integer) instr.operand); break;
                    case PUSH_FLOAT:  memoryAccessCompiler.compilePushFloat((Float) instr.operand); break;
                    case PUSH_STRING: memoryAccessCompiler.compilePushString((String) instr.operand); break;
                    case PUSH_BOOL:   memoryAccessCompiler.compilePushInt(((Boolean) instr.operand) ? 1 : 0); break;
                    case PUSH_NULL:   memoryAccessCompiler.compilePushNull(); break;
                    case POP:         memoryAccessCompiler.compilePop(); break;
                    case DUP:         memoryAccessCompiler.compileDup(); break;
                    case SWAP:        memoryAccessCompiler.compileSwap(); break;
                    case STORE_LOCAL: memoryAccessCompiler.compileStoreLocal((Integer) instr.operand, localRegisterMap); break;
                    case LOAD_LOCAL:  memoryAccessCompiler.compileLoadLocal((Integer) instr.operand, localRegisterMap); break;
                    case LOAD_FIELD:  memoryAccessCompiler.compileLoadField((String) instr.operand); break;
                    case STORE_FIELD: memoryAccessCompiler.compileStoreField((String) instr.operand); break;
                    case STORE_SLOT:  memoryAccessCompiler.compileStoreSlot(instr.operand, localRegisterMap); break;

                    // --- Operations ---
                    case ADD_INT: operationCompiler.generateBinaryOp("add_int"); break;
                    case SUB_INT: operationCompiler.generateBinaryOp("sub_int"); break;
                    case MUL_INT: operationCompiler.generateBinaryOp("mul_int"); break;
                    case DIV_INT: operationCompiler.generateBinaryOp("div_int"); break;
                    case MOD_INT: operationCompiler.generateBinaryOp("mod_int"); break;
                    case NEG_INT: operationCompiler.generateUnaryOp("neg_int"); break;

                    // --- Comparisons ---
                    case CMP_EQ_INT: operationCompiler.generateBinaryOp("cmp_eq_int"); break;
                    case CMP_NE_INT: operationCompiler.generateBinaryOp("cmp_ne_int"); break;
                    case CMP_LT_INT: operationCompiler.generateBinaryOp("cmp_lt_int"); break;
                    case CMP_LE_INT: operationCompiler.generateBinaryOp("cmp_le_int"); break;
                    case CMP_GT_INT: operationCompiler.generateBinaryOp("cmp_gt_int"); break;
                    case CMP_GE_INT: operationCompiler.generateBinaryOp("cmp_ge_int"); break;

                    // --- Control Flow ---
                    case JMP:         controlFlowCompiler.compileJmp(labelNameMap.get((String)instr.operand)); break;
                    case JMP_IF_TRUE: controlFlowCompiler.compileJmpIfTrue(labelNameMap.get((String)instr.operand)); break;
                    case JMP_IF_FALSE:controlFlowCompiler.compileJmpIfFalse(labelNameMap.get((String)instr.operand)); break;
                    case LABEL:       break; // Handled in pre-pass
                    case RET:         break; // Handled by epilogue

                    // --- Method Calls ---
                    case CALL:       methodCallCompiler.compileCall(instr, bytecode, i, localRegisterMap); break;
                    case CALL_SLOTS: methodCallCompiler.compileCallSlots(instr, bytecode, i, localRegisterMap); break;

                    // --- Runtime Calls ---
                    case PRINT:         runtimeCallHelper.compileRuntimeCall("runtime_print", 1, false); break;
                    case CONCAT_STRING: runtimeCallHelper.compileRuntimeCall("string_concat", 2, true); break;
                    case INT_TO_STRING: runtimeCallHelper.compileRuntimeCall("runtime_int_to_string", 1, true); break;
                    case ARRAY_NEW:     runtimeCallHelper.compileRuntimeCall("array_new", 1, true); break;
                    case ARRAY_LOAD:    runtimeCallHelper.compileRuntimeCall("array_load", 2, true); break;
                    case ARRAY_STORE:   runtimeCallHelper.compileRuntimeCall("array_store", 3, false); break;
                    case READ_INPUT:    runtimeCallHelper.compileReadInput((String) instr.operand); break;

                    default:
                        DebugSystem.warn("MTOT", "Unhandled bytecode instruction: " + instr.opcode);
                        assemblyCode.add("    " + cpuProfile.syntax.commentMarker + " Unhandled: " + instr.opcode);
                        break;
                }
            } catch (Exception e) {
                DebugSystem.error("MTOT", "Error compiling instruction " + i + ": " + instr.opcode + " ("+instr.operand+") - " + e.getMessage());
                assemblyCode.add("    " + cpuProfile.syntax.commentMarker + " ERROR compiling " + instr.opcode + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // --- ======================================================= ---
    // --- INNER CLASSES FOR SEPARATION OF CONCERNS ---
    // --- ======================================================= ---

    // ... (OperandStack unchanged) ...
    private class OperandStack {
        private final Stack<String> stack = new Stack<String>();

        public OperandStack() { }

        public String popToRegister() {
            if (stack.isEmpty()) {
                DebugSystem.warn("OPERAND_STACK", "Popping from empty stack - allocating new register");
                String reg = registerAllocator.allocateRegister();
                spiller.updateRegisterDefinitionDepth(reg, currentLoopDepth); // Update depth on allocation
                return reg;
            }
            return stack.pop();
        }

        public void pushFromRegister(String reg) {
            stack.push(reg);
            spiller.updateRegisterDefinitionDepth(reg, currentLoopDepth); // Update depth when pushing result
        }

        // Keep other OperandStack methods (peek, isEmpty, size, clear, popTwoOperands)
        public String peek() {
            return stack.isEmpty() ? null : stack.peek();
        }

        public boolean isEmpty() {
            return stack.isEmpty();
        }

        public int size() {
            return stack.size();
        }

        public void clear() {
            for (String reg : stack) {
                if (reg != null) {
                    registerAllocator.freeRegister(reg);
                }
            }
            stack.clear();
        }

        public String[] popTwoOperands() {
            String[] operands = new String[2];
            operands[1] = popToRegister(); // right operand
            operands[0] = popToRegister(); // left operand
            return operands;
        }

    }

    private class RuntimeCallHelper {
        public void compileRuntimeCall(String functionName, int argCount, boolean hasReturnValue) {
            List<String> argValueRegs = new ArrayList<String>();
            Set<String> excludeArgs = new HashSet<String>();
            List<String> abiArgsUsed = new ArrayList<String>();

            // --- MODIFIED: Handle arg offset for x86 ---
            int argStartIndex = (cpuProfile.architecture.equals("x86_64")) ? 0 : 0; // x86/AArch64 runtime calls all start at arg 0
            // --- END MODIFIED ---

            for (int i = 0; i < argCount; i++) {
                if (i + argStartIndex >= argumentRegisters.size()) {
                    DebugSystem.error("MTOT", "Runtime call " + functionName + " needs arg " + i + " but ABI only has " + argumentRegisters.size());
                    break;
                }
                argValueRegs.add(operandStack.popToRegister());
                String abiArg = argumentRegisters.get(i + argStartIndex);
                excludeArgs.add(abiArg);
                abiArgsUsed.add(abiArg);
            }
            Collections.reverse(argValueRegs);

            Set<String> regsToSave = spillCallerSavedRegisters(excludeArgs);

            for (int i = 0; i < argValueRegs.size(); i++) {
                String valueReg = argValueRegs.get(i);
                String abiArg = abiArgsUsed.get(i);
                spiller.fillRegister(valueReg);
                registerAllocator.markRegisterUsed(abiArg);
                if (!abiArg.equals(valueReg)) {
                    assemblyCode.add("    mov " + abiArg + ", " + valueReg);
                }
            }
            
            String callAsm = cpuProfile.getPattern("call").assemblyTemplate.get(0).replace("{name}", functionName);
            assemblyCode.add("    " + callAsm + " " + cpuProfile.syntax.commentMarker + " Call runtime helper");
            
            fillCallerSavedRegisters(regsToSave);

            // --- MODIFIED: Handle arch-specific return reg ---
            String returnAbiReg = (cpuProfile.architecture.equals("x86_64")) ? x86_64Registers.rax : argumentRegisters.get(0);
            // --- END MODIFIED ---
            
            if (hasReturnValue) {
                Set<String> avoidReturnRegs = new HashSet<String>(abiArgsUsed);
                avoidReturnRegs.add(returnAbiReg);

                String resultReg = registerAllocator.allocateRegister(avoidReturnRegs);
                assemblyCode.add("    mov " + resultReg + ", " + returnAbiReg + " " + cpuProfile.syntax.commentMarker + " Get result");
                operandStack.pushFromRegister(resultReg); // pushFromRegister updates depth
                spiller.markRegisterModified(resultReg);
            }

            for (int i = 0; i < abiArgsUsed.size(); i++) {
                String abiArg = abiArgsUsed.get(i);
                String valueReg = argValueRegs.get(i);

                if (!abiArg.equals(valueReg)) {
                    registerAllocator.freeRegister(valueReg);
                }

                boolean neededForResult = hasReturnValue && abiArg.equals(returnAbiReg) && operandStack.peek() != null && operandStack.peek().equals(returnAbiReg);
                if (!neededForResult) {
                    registerAllocator.freeRegister(abiArg);
                }
            }
        }

        public void compileReadInput(String expectedType) {
            String typeLabel = generateDataLabel("input_type");
            String directive = cpuProfile.syntax.stringDirective
                .replace("{label}", typeLabel)
                .replace("{value}", escapeString(expectedType));
            dataSection.add(directive);

            // --- MODIFIED: Handle arch-specific arg reg ---
            String abiArg0 = (cpuProfile.architecture.equals("x86_64")) ? x86_64Registers.rdi : argumentRegisters.get(0);
            // --- END MODIFIED ---
            
            Set<String> excludeArgs = new HashSet<String>();
            excludeArgs.add(abiArg0);

            Set<String> regsToSave = spillCallerSavedRegisters(excludeArgs);

            registerAllocator.markRegisterUsed(abiArg0);
            InstructionPattern loadAddrPattern = cpuProfile.getPattern("load_address");
            for (String t : loadAddrPattern.assemblyTemplate) {
                assemblyCode.add("    " + t.replace("{dest}", abiArg0).replace("{label}", typeLabel));
            }

            String callAsm = cpuProfile.getPattern("call").assemblyTemplate.get(0).replace("{name}", "runtime_read_input");
            assemblyCode.add("    " + callAsm + " " + cpuProfile.syntax.commentMarker + " Call runtime helper (expects type* in arg0)");
            
            fillCallerSavedRegisters(regsToSave);

            // --- MODIFIED: Handle arch-specific return reg ---
            String returnAbiReg = (cpuProfile.architecture.equals("x86_64")) ? x86_64Registers.rax : argumentRegisters.get(0);
            // --- END MODIFIED ---

            Set<String> avoidReturnRegs = new HashSet<String>(Collections.singletonList(returnAbiReg));
            String resultReg = registerAllocator.allocateRegister(avoidReturnRegs);
            assemblyCode.add("    mov " + resultReg + ", " + returnAbiReg + " " + cpuProfile.syntax.commentMarker + " Get input result");
            operandStack.pushFromRegister(resultReg); // pushFromRegister updates depth
            spiller.markRegisterModified(resultReg);

            if (!resultReg.equals(returnAbiReg)) registerAllocator.freeRegister(returnAbiReg);
        }
    }

    private class OperationCompiler {
        public void generateBinaryOp(String patternName) {
            InstructionPattern p = cpuProfile.getPattern(patternName);
            if (p == null) throw new RuntimeException("No pattern for " + patternName);

            String[] ops = operandStack.popTwoOperands();
            String l = ops[0], r = ops[1];
            Set<String> avoid = new HashSet<String>(Arrays.asList(l, r));
            String res = registerAllocator.allocateRegister(avoid);

            spiller.fillRegister(l);
            spiller.fillRegister(r);
            
            // --- MODIFIED: Handle clobbered registers explicitly ---
            Set<String> clobberedRegs = new HashSet<String>();
            if (p.requiredRegisters != null) {
                for (String clobbered : p.requiredRegisters) {
                    if (clobbered.equals("al")) clobbered = x86_64Registers.rax; // Treat 'al' as 'rax'
                    registerAllocator.markRegisterUsed(clobbered);
                    clobberedRegs.add(clobbered);
                }
            }
            // --- END MODIFIED ---

            for (String t : p.assemblyTemplate) {
                String asm = t.replace("{dest}", res).replace("{src1}", l).replace("{src2}", r);
                assemblyCode.add("    " + asm);
            }
            
            // --- MODIFIED: Free clobbered registers ---
            for (String clobbered : clobberedRegs) {
                // Don't free the register if it's the destination
                if (patternName.equals("mod_int") && clobbered.equals(x86_64Registers.rax)) {
                     registerAllocator.freeRegister(clobbered); // rax is never result of mod
                }
                else if (patternName.equals("div_int") && clobbered.equals(x86_64Registers.rdx)) {
                     registerAllocator.freeRegister(clobbered); // rdx is never result of div
                }
                else if (!clobbered.equals(res)) {
                    registerAllocator.freeRegister(clobbered);
                }
            }
            // --- END MODIFIED ---

            operandStack.pushFromRegister(res); // pushFromRegister updates depth
            spiller.markRegisterModified(res);
            registerAllocator.freeRegister(l);
            registerAllocator.freeRegister(r);
        }

        public void generateUnaryOp(String patternName) {
            InstructionPattern pattern = cpuProfile.getPattern(patternName);
            if (pattern == null) { throw new RuntimeException("No pattern for unary op: " + patternName); }

            String operandReg = operandStack.popToRegister();
            String resultReg = registerAllocator.allocateRegister(Collections.singleton(operandReg));

            spiller.fillRegister(operandReg);

            for (String template : pattern.assemblyTemplate) {
                String asm = template.replace("{dest}", resultReg).replace("{src}", operandReg);
                assemblyCode.add("    " + asm);
            }

            operandStack.pushFromRegister(resultReg); // pushFromRegister updates depth
            spiller.markRegisterModified(resultReg);
            registerAllocator.freeRegister(operandReg);
        }
    }

    private class MemoryAccessCompiler {
        // ... (reset, getSlotLocations, getFieldOffset unchanged) ...
        private Map<String, Integer> fieldOffsets = new HashMap<String, Integer>();
        private int nextFieldOffset = 0;
        private Map<Integer, String> slotLocations = new HashMap<Integer, String>();
        private Map<String, String> stringLiteralLabels = new HashMap<String, String>();

        public void reset() { /* ... unchanged ... */
             fieldOffsets.clear();
            nextFieldOffset = 0;
            slotLocations.clear();
            stringLiteralLabels.clear();
         }
        public Map<Integer, String> getSlotLocations() { /* ... unchanged ... */
             return new HashMap<Integer, String>(slotLocations);
         }
        private int getFieldOffset(String fieldName) { /* ... unchanged ... */
            if (!fieldOffsets.containsKey(fieldName)) {
                fieldOffsets.put(fieldName, nextFieldOffset);
                DebugSystem.debug("MTOT_FIELDS", "Assigned offset " + nextFieldOffset + " to field '" + fieldName + "'");
                nextFieldOffset += 8;
            }
            return fieldOffsets.get(fieldName).intValue();
        }

        public void compilePushInt(int value) {
            String r = registerAllocator.allocateRegister();
            InstructionPattern p = cpuProfile.getPattern("load_immediate_int");
            assemblyCode.add("    " + p.assemblyTemplate.get(0).replace("{dest}", r).replace("{value}", String.valueOf(value)));
            operandStack.pushFromRegister(r); // Updates depth
            spiller.markRegisterModified(r);
        }

        public void compilePushFloat(float value) {
            String label = generateDataLabel("float");
            String directive = cpuProfile.syntax.floatDirective
                .replace("{label}", label)
                .replace("{value}", String.valueOf(value));
            dataSection.add(directive);
            String r = registerAllocator.allocateRegister();
            for(String t : cpuProfile.getPattern("load_address").assemblyTemplate) {
                assemblyCode.add("    "+t.replace("{dest}",r).replace("{label}",label));
            }
            operandStack.pushFromRegister(r); // Updates depth
            spiller.markRegisterModified(r);
            DebugSystem.warn("MTOT", "PUSH_FLOAT currently pushes address, not value. Needs float register handling.");
        }

        public void compilePushString(String str) {
            String label;
            if (stringLiteralLabels.containsKey(str)) { label = stringLiteralLabels.get(str); DebugSystem.debug("MTOT_STR", "Reusing string literal label '" + label + "' for: \"" + escapeString(str) + "\""); }
            else { label = generateDataLabel("str"); String directive = cpuProfile.syntax.stringDirective.replace("{label}", label).replace("{value}", escapeString(str)); dataSection.add(directive); stringLiteralLabels.put(str, label); DebugSystem.debug("MTOT_STR", "Created string literal label '" + label + "' for: \"" + escapeString(str) + "\""); }
            String r = registerAllocator.allocateRegister();
            for(String t : cpuProfile.getPattern("load_address").assemblyTemplate) {
                assemblyCode.add("    "+t.replace("{dest}",r).replace("{label}",label));
            }
            operandStack.pushFromRegister(r); // Updates depth
            spiller.markRegisterModified(r);
        }

        public void compilePushNull() {
            String r = registerAllocator.allocateRegister();
            InstructionPattern p = cpuProfile.getPattern("load_immediate_int");
            assemblyCode.add("    " + p.assemblyTemplate.get(0).replace("{dest}", r).replace("{value}", "0"));
            operandStack.pushFromRegister(r); // Updates depth
            spiller.markRegisterModified(r);
        }

        public void compilePop() { /* ... unchanged ... */
             if (!operandStack.isEmpty()) {
                String r = operandStack.popToRegister();
                registerAllocator.freeRegister(r);
                DebugSystem.debug("MTOT_REG", "POP freed register: " + r);
            } else {
                DebugSystem.warn("MTOT", "POP on empty stack.");
            }
        }
        public void compileDup() { /* ... unchanged ... */
             String r = operandStack.peek();
            if (r != null) {
                spiller.fillRegister(r);
                String n = registerAllocator.allocateRegister(Collections.singleton(r));
                assemblyCode.add("    " + cpuProfile.getPattern("move_reg").assemblyTemplate.get(0).replace("{dest}", n).replace("{src}", r));
                operandStack.pushFromRegister(n); // Updates depth
                spiller.markRegisterModified(n);
            } else {
                throw new RuntimeException("DUP on empty stack.");
            }
        }
        public void compileSwap() { /* ... unchanged ... */
             String r1 = operandStack.popToRegister();
            String r2 = operandStack.popToRegister();
            operandStack.pushFromRegister(r1); // Order matters, r1 was top, now pushed back first
            operandStack.pushFromRegister(r2); // r2 was second, now pushed back on top
            DebugSystem.debug("MTOT_REG", "SWAPped " + r1 + " and " + r2 + " on stack");
        }

        public void compileLoadField(String fieldName) {
            int offset = getFieldOffset(fieldName);
            String destReg = registerAllocator.allocateRegister(Collections.singleton(thisRegister));
            spiller.fillRegister(thisRegister);
            InstructionPattern pattern = cpuProfile.getPattern("load_field_offset");
            if (pattern == null) throw new RuntimeException("Missing 'load_field_offset' pattern!");
            String asm = pattern.assemblyTemplate.get(0)
                .replace("{dest_reg}", destReg)
                .replace("{base_reg}", thisRegister)
                .replace("{offset}", String.valueOf(offset));
            assemblyCode.add("    " + cpuProfile.syntax.commentMarker + " Load field " + fieldName + " (offset " + offset + ")");
            assemblyCode.add("    " + asm);
            operandStack.pushFromRegister(destReg); // Updates depth
            spiller.markRegisterModified(destReg);
        }

        public void compileStoreField(String fieldName) {
            int offset = getFieldOffset(fieldName);
            String valueReg = operandStack.popToRegister();
            spiller.fillRegister(thisRegister);
            spiller.fillRegister(valueReg);
            InstructionPattern pattern = cpuProfile.getPattern("store_field_offset");
            if (pattern == null) throw new RuntimeException("Missing 'store_field_offset' pattern!");
            String asm = pattern.assemblyTemplate.get(0)
                .replace("{src_reg}", valueReg)
                .replace("{base_reg}", thisRegister)
                .replace("{offset}", String.valueOf(offset));
            assemblyCode.add("    " + cpuProfile.syntax.commentMarker + " Store field " + fieldName + " (offset " + offset + ")");
            assemblyCode.add("    " + asm);
            registerAllocator.freeRegister(valueReg);
        }

        public void compileStoreLocal(int slotIndex, Map<String, String> localRegisterMap) {
            // ... (Logic remains unchanged, relies on spiller) ...
             String valueReg = operandStack.popToRegister();
            String varRegKey = "local_" + slotIndex;
            String targetReg = localRegisterMap.get(varRegKey);

            if (targetReg == null) {
                // First time storing to this local, allocate a register for it.
                targetReg = registerAllocator.allocateRegister(Collections.singleton(valueReg));
                localRegisterMap.put(varRegKey, targetReg);
                spiller.mapSlotToRegister(slotIndex, targetReg); // Map slot to its new home register
                DebugSystem.debug("MTOT_REG", "Allocated register " + targetReg + " for " + varRegKey + " during STORE");
            } else {
                // Register already assigned, ensure it's ready (might need filling if spilled)
                spiller.fillRegister(targetReg);
            }

            spiller.fillRegister(valueReg);

            // Move the value
            if (!targetReg.equals(valueReg)) {
                String asm = cpuProfile.getPattern("move_reg").assemblyTemplate.get(0)
                    .replace("{dest}", targetReg)
                    .replace("{src}", valueReg);
                assemblyCode.add("    " + asm);
                registerAllocator.freeRegister(valueReg); // Value is now copied, free its original reg
            }

            spiller.markRegisterModified(targetReg); // This now handles immediate spill if needed
            spiller.updateRegisterDefinitionDepth(targetReg, currentLoopDepth); // Update depth on store
            spiller.trackRegisterUsage(targetReg); // Keep tracking usage
        }

        public void compileLoadLocal(int slotIndex, Map<String, String> localRegisterMap) {
            // ... (Logic remains unchanged, relies on spiller) ...
             String varRegKey = "local_" + slotIndex;
            String varReg = localRegisterMap.get(varRegKey);

            if (varReg == null) {
                DebugSystem.error("MTOT_REG", "Register for " + varRegKey + " is null during LOAD! Recovering by allocating new register and assuming value 0.");
                varReg = registerAllocator.allocateRegister();
                localRegisterMap.put(varRegKey, varReg);
                spiller.mapSlotToRegister(slotIndex, varReg);
                InstructionPattern p = cpuProfile.getPattern("load_immediate_int");
                assemblyCode.add("    " + p.assemblyTemplate.get(0).replace("{dest}", varReg).replace("{value}", "0") + " " + cpuProfile.syntax.commentMarker + " WARNING: Used uninitialized local slot " + slotIndex);
                spiller.markRegisterModified(varReg);
                spiller.updateRegisterDefinitionDepth(varReg, currentLoopDepth); // Update depth
            } else {
                 spiller.fillRegister(varReg); // Ensures loaded if needed
            }

            // Allocate a NEW temporary register to push onto the stack.
            String tempReg = registerAllocator.allocateRegister(Collections.singleton(varReg));
            String asm = cpuProfile.getPattern("move_reg").assemblyTemplate.get(0)
                .replace("{dest}", tempReg)
                .replace("{src}", varReg);
            assemblyCode.add("    " + asm);

            operandStack.pushFromRegister(tempReg); // pushFromRegister updates depth
            spiller.markRegisterModified(tempReg);
            spiller.trackRegisterUsage(varReg); // Original was just used
        }

        public void compileStoreSlot(Object operand, Map<String, String> localRegisterMap) {
             // ... (Logic remains unchanged) ...
            if (operand instanceof Integer) {
                int slotIndex = ((Integer) operand).intValue();
                compileStoreLocal(slotIndex, localRegisterMap); // StoreLocal now handles depth/spill
                String location = localRegisterMap.get("local_" + slotIndex);
                if (location != null) {
                    slotLocations.put(Integer.valueOf(slotIndex), location);
                    DebugSystem.debug("MTOT_SLOT", "Recorded register location for slot index " + slotIndex + ": " + location);
                } else {
                    DebugSystem.warn("MTOT_SLOT", "Cannot determine register location for slot index: " + slotIndex);
                    int offset = spiller.getSpillOffsetForSlotIndex(slotIndex);
                    if (offset != Integer.MIN_VALUE) {
                        slotLocations.put(Integer.valueOf(slotIndex), "spill_" + offset);
                        DebugSystem.debug("MTOT_SLOT", "Recorded spill location for slot index " + slotIndex + ": spill_" + offset);
                    } else {
                        DebugSystem.warn("MTOT_SLOT", "Cannot determine any location for slot index: " + slotIndex);
                    }
                }
            } else {
                DebugSystem.error("MTOT", "STORE_SLOT operand is not an Integer index: " + operand);
            }
        }
    }

    private class ControlFlowCompiler {
        public void compileJmp(String label) {
            if (label == null) { DebugSystem.error("MTOT", "JMP target label is null!"); return; }
            assemblyCode.add("    " + cpuProfile.getPattern("jmp").assemblyTemplate.get(0).replace("{label}", label));
        }
        public void compileJmpIfFalse(String label) {
            if (label == null) { DebugSystem.error("MTOT", "JMP_IF_FALSE target label is null!"); return; }
            String c = operandStack.popToRegister();
            spiller.fillRegister(c);
            for (String t : cpuProfile.getPattern("jmp_if_false").assemblyTemplate) {
                assemblyCode.add("    " + t.replace("{condition}", c).replace("{label}", label));
            }
             registerAllocator.freeRegister(c); // Condition register likely not needed after jump
        }
        public void compileJmpIfTrue(String label) {
            if (label == null) { DebugSystem.error("MTOT", "JMP_IF_TRUE target label is null!"); return; }
            String c = operandStack.popToRegister();
            spiller.fillRegister(c);
            InstructionPattern p = cpuProfile.getPattern("jmp_if_true");
            if (p == null) { // Fallback if pattern missing
                 if (cpuProfile.architecture.equals("aarch64")) {
                    assemblyCode.add("    cmp " + c + ", #0");
                    assemblyCode.add("    b.ne " + label);
                 } else {
                    assemblyCode.add("    test " + c + ", " + c);
                    assemblyCode.add("    jnz " + label);
                 }
            } else {
                for (String t : p.assemblyTemplate) {
                    assemblyCode.add("    " + t.replace("{condition}", c).replace("{label}", label));
                }
            }
             registerAllocator.freeRegister(c); // Condition register likely not needed after jump
        }
    }

    private class MethodCallCompiler {
        public void compileCall(BytecodeInstruction instr, List<BytecodeInstruction> bytecode, int index, Map<String, String> localRegisterMap) {
            String methodName = (String) instr.operand;
            int argCount = countArgsForCall(bytecode, index);
            DebugSystem.debug("MTOT", "Compiling call to " + methodName + " with " + argCount + " arguments.");

            List<String> argValueRegs = new ArrayList<String>();
            for (int i = 0; i < argCount; i++) {
                argValueRegs.add(operandStack.popToRegister());
            }
            Collections.reverse(argValueRegs);

            Set<String> excludeArgs = new HashSet<String>();
            excludeArgs.add(argumentRegisters.get(0)); // arg0 for 'this'
            List<String> abiArgsUsedForParams = new ArrayList<String>();
            
            // --- MODIFIED: Handle arg offset for x86 ---
            int argStartIndex = (cpuProfile.architecture.equals("x86_64")) ? 0 : 1;
            // --- END MODIFIED ---
            
            for (int i = 0; i < argCount; i++) {
                int abiArgIndex = i + argStartIndex;
                if (abiArgIndex < argumentRegisters.size()) {
                    String abiArgReg = argumentRegisters.get(abiArgIndex);
                    // --- MODIFIED: x86 'rax' is not an arg reg ---
                    if (cpuProfile.architecture.equals("x86_64") && abiArgReg.equals(x86_64Registers.rax)) {
                        DebugSystem.warn("MTOT", "x86 ABI does not use rax for arguments. Skipping arg " + i);
                        abiArgIndex++;
                        if (abiArgIndex >= argumentRegisters.size()) continue;
                        abiArgReg = argumentRegisters.get(abiArgIndex);
                    }
                    // --- END MODIFIED ---
                    excludeArgs.add(abiArgReg);
                    abiArgsUsedForParams.add(abiArgReg);
                }
            }

            Set<String> regsToSave = spillCallerSavedRegisters(excludeArgs);

            for (int i = 0; i < argCount; i++) {
                if (i < abiArgsUsedForParams.size()) {
                    String abiArgReg = abiArgsUsedForParams.get(i);
                    String valueReg = argValueRegs.get(i);
                    spiller.fillRegister(valueReg);
                    registerAllocator.markRegisterUsed(abiArgReg);
                    if (!abiArgReg.equals(valueReg)) {
                        assemblyCode.add("    mov " + abiArgReg + ", " + valueReg + " " + cpuProfile.syntax.commentMarker + " Setup arg " + (i + 1));
                    }
                } else {
                    DebugSystem.warn("MTOT", "Argument " + i + " stack passing not implemented for call to " + methodName);
                }
            }

            // --- MODIFIED: Handle arch-specific 'this' arg reg ---
            String thisAbiArgReg = (cpuProfile.architecture.equals("x86_64")) ? x86_64Registers.rdi : argumentRegisters.get(0);
            spiller.fillRegister(thisRegister);
            registerAllocator.markRegisterUsed(thisAbiArgReg);
            if (!thisAbiArgReg.equals(thisRegister)) {
                assemblyCode.add("    mov " + thisAbiArgReg + ", " + thisRegister + " " + cpuProfile.syntax.commentMarker + " Setup 'this' pointer");
            }
            // --- END MODIFIED ---

            for (int i = 0; i < argValueRegs.size(); i++) {
                String valueReg = argValueRegs.get(i);
                boolean isUsedForArg = (i < abiArgsUsedForParams.size() && valueReg.equals(abiArgsUsedForParams.get(i)));
                if (!isUsedForArg) {
                    registerAllocator.freeRegister(valueReg);
                }
            }

            String callAsm = cpuProfile.getPattern("call").assemblyTemplate.get(0).replace("{name}", methodName);
            assemblyCode.add("    " + callAsm);

            fillCallerSavedRegisters(regsToSave);

            for (String reg : abiArgsUsedForParams) {
                registerAllocator.freeRegister(reg);
            }

            // --- MODIFIED: Handle arch-specific return reg ---
            String returnAbiReg = (cpuProfile.architecture.equals("x86_64")) ? x86_64Registers.rax : argumentRegisters.get(0);
            // --- END MODIFIED ---
            
            Set<String> avoidReturnReg = Collections.singleton(returnAbiReg);
            String resultReg = registerAllocator.allocateRegister(avoidReturnReg);
            assemblyCode.add("    mov " + resultReg + ", " + returnAbiReg + " " + cpuProfile.syntax.commentMarker + " Get result");
            operandStack.pushFromRegister(resultReg); // Updates depth
            spiller.markRegisterModified(resultReg);

            if (!resultReg.equals(returnAbiReg)) {
                registerAllocator.freeRegister(returnAbiReg);
            }
        }

        public void compileCallSlots(BytecodeInstruction instr, List<BytecodeInstruction> bytecode, int index, Map<String, String> localRegisterMap) {
             Object[] operand = (Object[]) instr.operand;
            String methodName = (String) operand[0];
            int numSlots = ((Integer) operand[1]).intValue();
            int argCount = countArgsForCall(bytecode, index);
            DebugSystem.debug("MTOT", "Compiling CALL_SLOTS to " + methodName + " with " + argCount + " arguments, expecting " + numSlots + " slots.");

            List<String> argValueRegs = new ArrayList<String>();
            for (int i = 0; i < argCount; i++) {
                argValueRegs.add(operandStack.popToRegister());
            }
            Collections.reverse(argValueRegs);

            Set<String> excludeArgs = new HashSet<String>();
            
            // --- MODIFIED: Handle arch-specific 'this' arg reg ---
            String thisAbiArgReg = (cpuProfile.architecture.equals("x86_64")) ? x86_64Registers.rdi : argumentRegisters.get(0);
            excludeArgs.add(thisAbiArgReg);
            // --- END MODIFIED ---
            
            List<String> abiArgsUsedForParams = new ArrayList<String>();
            
            // --- MODIFIED: Handle arg offset for x86 ---
            int argStartIndex = (cpuProfile.architecture.equals("x86_64")) ? 1 : 1; // x86 params start at rsi (index 1)
            // --- END MODIFIED ---
            
            for (int i = 0; i < argCount; i++) {
                int abiArgIndex = i + argStartIndex;
                if (abiArgIndex < argumentRegisters.size()) {
                    String abiArgReg = argumentRegisters.get(abiArgIndex);
                    // --- MODIFIED: x86 'rax' is not an arg reg ---
                    if (cpuProfile.architecture.equals("x86_64") && abiArgReg.equals(x86_64Registers.rax)) {
                        DebugSystem.warn("MTOT", "x86 ABI does not use rax for arguments. Skipping arg " + i);
                        abiArgIndex++;
                        if (abiArgIndex >= argumentRegisters.size()) continue;
                        abiArgReg = argumentRegisters.get(abiArgIndex);
                    }
                    // --- END MODIFIED ---
                    excludeArgs.add(abiArgReg);
                    abiArgsUsedForParams.add(abiArgReg);
                }
            }

            Set<String> regsToSave = spillCallerSavedRegisters(excludeArgs);

            for (int i = 0; i < argCount; i++) {
                 if (i < abiArgsUsedForParams.size()) {
                    String abiArgReg = abiArgsUsedForParams.get(i);
                    String valueReg = argValueRegs.get(i);
                    spiller.fillRegister(valueReg);
                    registerAllocator.markRegisterUsed(abiArgReg);
                    if (!abiArgReg.equals(valueReg)) {
                        assemblyCode.add("    mov " + abiArgReg + ", " + valueReg + " " + cpuProfile.syntax.commentMarker + " Setup arg " + (i + 1));
                    }
                } else {
                    DebugSystem.warn("MTOT", "Argument " + i + " stack passing not implemented for CALL_SLOTS to " + methodName);
                }
            }

            spiller.fillRegister(thisRegister);
            registerAllocator.markRegisterUsed(thisAbiArgReg);
            if (!thisAbiArgReg.equals(thisRegister)) {
                assemblyCode.add("    mov " + thisAbiArgReg + ", " + thisRegister + " " + cpuProfile.syntax.commentMarker + " Setup 'this' pointer");
            }

            for (int i = 0; i < argValueRegs.size(); i++) {
                String valueReg = argValueRegs.get(i);
                boolean isUsedForArg = (i < abiArgsUsedForParams.size() && valueReg.equals(abiArgsUsedForParams.get(i)));
                if (!isUsedForArg) {
                    registerAllocator.freeRegister(valueReg);
                }
            }

            String callAsm = cpuProfile.getPattern("call").assemblyTemplate.get(0).replace("{name}", methodName);
            assemblyCode.add("    " + callAsm);

            fillCallerSavedRegisters(regsToSave);

            Set<String> returnSlotRegs = new HashSet<String>();
            
            // --- MODIFIED: Handle x86 return regs (rax, rdx) ---
            if (cpuProfile.architecture.equals("x86_64")) {
                 String ret0 = x86_64Registers.rax;
                 String ret1 = x86_64Registers.rdx;
                 returnSlotRegs.add(ret0);
                 if (numSlots > 1) returnSlotRegs.add(ret1);
                 
                 for (String reg : abiArgsUsedForParams) {
                     if (!reg.equals(ret0) && (numSlots < 2 || !reg.equals(ret1))) {
                         registerAllocator.freeRegister(reg);
                     }
                 }
                 if (!thisAbiArgReg.equals(ret0) && (numSlots < 2 || !thisAbiArgReg.equals(ret1))) {
                     registerAllocator.freeRegister(thisAbiArgReg);
                 }
            } else { // AArch64 logic
                for (int i = 0; i < numSlots && i < argumentRegisters.size(); i++) {
                    returnSlotRegs.add(argumentRegisters.get(i));
                }
                for (String reg : abiArgsUsedForParams) {
                    if (!returnSlotRegs.contains(reg)) {
                        registerAllocator.freeRegister(reg);
                    }
                }
                if (!returnSlotRegs.contains(thisAbiArgReg)) {
                    registerAllocator.freeRegister(thisAbiArgReg);
                }
            }
            // --- END MODIFIED ---

            Set<String> avoidReturnRegs = new HashSet<String>(returnSlotRegs);
            for (int i = 0; i < numSlots; i++) {
                String returnRegAbi;
                // --- MODIFIED: Handle x86 return regs (rax, rdx) ---
                if (cpuProfile.architecture.equals("x86_64")) {
                    if (i == 0) returnRegAbi = x86_64Registers.rax;
                    else if (i == 1) returnRegAbi = x86_64Registers.rdx;
                    else {
                         DebugSystem.error("MTOT_SLOT", "Requested slot " + i + " but x86 only supports 2 return registers (rax, rdx)!");
                         returnRegAbi = null;
                    }
                } else { // AArch64
                    if (i >= argumentRegisters.size()) {
                        DebugSystem.error("MTOT_SLOT", "Requested slot " + i + " but only " + argumentRegisters.size() + " return registers exist!");
                        returnRegAbi = null;
                    } else {
                        returnRegAbi = argumentRegisters.get(i);
                    }
                }
                // --- END MODIFIED ---

                if (returnRegAbi == null) {
                    String errReg = registerAllocator.allocateRegister(avoidReturnRegs);
                    InstructionPattern p = cpuProfile.getPattern("load_immediate_int");
                    assemblyCode.add("    " + p.assemblyTemplate.get(0).replace("{dest}", errReg).replace("{value}", "0") + " " + cpuProfile.syntax.commentMarker + " ERROR: Missing return slot " + i);
                    operandStack.pushFromRegister(errReg); // Updates depth
                    spiller.markRegisterModified(errReg);
                    avoidReturnRegs.add(errReg);
                    continue;
                }

                String resultReg = registerAllocator.allocateRegister(avoidReturnRegs);
                assemblyCode.add("    mov " + resultReg + ", " + returnRegAbi + " " + cpuProfile.syntax.commentMarker + " Get result slot " + i);
                operandStack.pushFromRegister(resultReg); // Updates depth
                spiller.markRegisterModified(resultReg);

                if (!resultReg.equals(returnRegAbi)) {
                     registerAllocator.freeRegister(returnRegAbi);
                }
                avoidReturnRegs.remove(returnRegAbi);
                avoidReturnRegs.add(resultReg);
            }
        }
        private int countArgsForCall(List<BytecodeInstruction> bytecode, int callInstructionIndex) { /* ... unchanged ... */
              int required_depth = 0;
            int max_required_depth = 0;
            for (int i = callInstructionIndex - 1; i >= 0; i--) {
                BytecodeInstruction instr = bytecode.get(i);
                BytecodeInstruction.Opcode op = instr.opcode;
                int pops = 0;
                int pushes = 0;

                if ((op.ordinal() >= BytecodeInstruction.Opcode.PUSH_INT.ordinal() && op.ordinal() <= BytecodeInstruction.Opcode.PUSH_NULL.ordinal())
                    || op == BytecodeInstruction.Opcode.LOAD_LOCAL
                    || op == BytecodeInstruction.Opcode.LOAD_FIELD
                    || op == BytecodeInstruction.Opcode.ARRAY_NEW
                    || op == BytecodeInstruction.Opcode.READ_INPUT
                    || op == BytecodeInstruction.Opcode.CALL) {
                    pushes = 1;
                }
                else if (op == BytecodeInstruction.Opcode.DUP) { pops = 1; pushes = 2; }
                else if (op == BytecodeInstruction.Opcode.SWAP) { pops = 2; pushes = 2; }
                else if (op == BytecodeInstruction.Opcode.CALL_SLOTS) { Object[] callOperand = (Object[]) instr.operand; pushes = ((Integer) callOperand[1]).intValue(); }
                else if ((op.ordinal() >= BytecodeInstruction.Opcode.ADD_INT.ordinal() && op.ordinal() <= BytecodeInstruction.Opcode.CMP_GE_INT.ordinal())
                    || op == BytecodeInstruction.Opcode.CONCAT_STRING
                    || op == BytecodeInstruction.Opcode.ARRAY_LOAD) {
                    pops = 2; pushes = 1;
                } else if (op == BytecodeInstruction.Opcode.NEG_INT
                    || op == BytecodeInstruction.Opcode.INT_TO_STRING) {
                    pops = 1; pushes = 1;
                }
                else if (op == BytecodeInstruction.Opcode.ARRAY_STORE) { pops = 3; }
                else if (op == BytecodeInstruction.Opcode.POP
                    || op == BytecodeInstruction.Opcode.PRINT
                    || op == BytecodeInstruction.Opcode.STORE_LOCAL
                    || op == BytecodeInstruction.Opcode.STORE_FIELD
                    || op == BytecodeInstruction.Opcode.STORE_SLOT
                    || op == BytecodeInstruction.Opcode.JMP_IF_FALSE
                    || op == BytecodeInstruction.Opcode.JMP_IF_TRUE) {
                    pops = 1;
                }
                else if (op == BytecodeInstruction.Opcode.JMP
                    || op == BytecodeInstruction.Opcode.LABEL
                    || op == BytecodeInstruction.Opcode.RET) {
                     // Stop counting when we hit a jump, label, or return
                    break;
                }

                required_depth -= pushes;
                required_depth += pops;
                if (required_depth < 0) {
                     // This means we encountered a value push that wasn't popped by an intermediate instruction.
                     // It must be an argument for the call we're analyzing.
                     // Increment max_required_depth for each argument found this way.
                    max_required_depth++;
                    required_depth = 0; // Reset depth for analyzing arguments further back
                }
                // We don't need to track max_required_depth in the same way anymore,
                // the logic above counts arguments directly based on negative depth.
            }
            return Math.max(0, max_required_depth);
        }
    }


    // --- Main Class Helper Methods (used by inner classes) ---
    
    // ... (escapeString, generateDataLabel unchanged) ...
    private String escapeString(String str) {
        str = str.replace("\\", "\\\\");
        str = str.replace("\"", "\\\"");
        str = str.replace("\n", "\\n");
        str = str.replace("\t", "\\t");
        str = str.replace("\0", "\\0");
        return str;
    }

    private String generateDataLabel(String prefix) {
        return prefix + "_" + this.currentMethodName + "_" + (dataLabelCounter++);
    }


    public void generateSpillCode(String register, int offset) {
        // --- MODIFIED: Handle x86 negative offsets ---
        int finalOffset = offset;
        String baseReg = (cpuProfile.architecture.equals("x86_64")) ? x86_64Registers.rbp : AArch64Registers.fp;
        if (cpuProfile.architecture.equals("x86_64")) {
            if (offset > 0) finalOffset = -offset;
            baseReg = x86_64Registers.rbp;
        } else {
            baseReg = AArch64Registers.fp;
        }
        // --- END MODIFIED ---
        
        InstructionPattern pattern = cpuProfile.getPattern("store_to_stack");
        if (pattern == null) { throw new RuntimeException("Missing 'store_to_stack' instruction pattern for architecture: " + cpuProfile.architecture); }
        String asm = pattern.assemblyTemplate.get(0)
            .replace("{src_reg}", register)
            .replace("{offset}", String.valueOf(finalOffset));
            
        assemblyCode.add("    " + cpuProfile.syntax.commentMarker + " Spill " + register + " to [" + baseReg + ((finalOffset < 0) ? "" : "+") + finalOffset + "]");
        assemblyCode.add("    " + asm);
    }

    public void generateFillCode(String register, int offset) {
        // --- MODIFIED: Handle x86 negative offsets ---
        int finalOffset = offset;
        String baseReg = (cpuProfile.architecture.equals("x86_64")) ? x86_64Registers.rbp : AArch64Registers.fp;
        if (cpuProfile.architecture.equals("x86_64")) {
            if (offset > 0) finalOffset = -offset;
            baseReg = x86_64Registers.rbp;
        } else {
            baseReg = AArch64Registers.fp;
        }
        // --- END MODIFIED ---
        
        InstructionPattern pattern = cpuProfile.getPattern("load_from_stack");
        if (pattern == null) { throw new RuntimeException("Missing 'load_from_stack' instruction pattern for architecture: " + cpuProfile.architecture); }
        String asm = pattern.assemblyTemplate.get(0)
            .replace("{dest_reg}", register)
            .replace("{offset}", String.valueOf(finalOffset));
            
        assemblyCode.add("    " + cpuProfile.syntax.commentMarker + " Fill " + register + " from [" + baseReg + ((finalOffset < 0) ? "" : "+") + finalOffset + "]");
        assemblyCode.add("    " + asm);
    }

private Set<String> spillCallerSavedRegisters(Set<String> excludeArgs) {
        Set<String> currentlyLiveRegs = registerAllocator.getUsedRegisters();
        Set<String> callerSavedToSpill = new HashSet<String>();

        callerSavedToSpill.addAll(currentlyLiveRegs);
        callerSavedToSpill.retainAll(this.abiCallerSavedSet);
        
        if (excludeArgs != null) {
            callerSavedToSpill.removeAll(excludeArgs);
        }

        if (!callerSavedToSpill.isEmpty()) {
            List<String> orderedSpill = new ArrayList<String>(callerSavedToSpill);
            Collections.sort(orderedSpill);
            assemblyCode.add("    " + cpuProfile.syntax.commentMarker + " Spilling caller-saved registers before call: " + orderedSpill);
            for (String reg : orderedSpill) {
                if (registerAllocator.usedRegisters.contains(reg)){ // Double check if still used
                     spiller.forceSpill(reg);
                }
            }
        } else {
             assemblyCode.add("    " + cpuProfile.syntax.commentMarker + " No caller-saved registers needed spilling before call.");
        }
        return callerSavedToSpill;
    }

    private void fillCallerSavedRegisters(Set<String> registersToFill) {
        if (registersToFill != null && !registersToFill.isEmpty()) {
            List<String> orderedRestore = new ArrayList<String>(registersToFill);
            Collections.sort(orderedRestore);

            assemblyCode.add("    " + cpuProfile.syntax.commentMarker + " Filling caller-saved registers after call: " + orderedRestore);
            for (String reg : orderedRestore) {
                spiller.fillRegister(reg); // fillRegister now handles depth update
                registerAllocator.markRegisterUsed(reg);
            }
        } else {
            assemblyCode.add("    " + cpuProfile.syntax.commentMarker + " No caller-saved registers needed filling after call.");
        }
    }


} // End of MTOTNativeCompiler class