package cod.compiler;

import static cod.compiler.MTOTRegistry.*;
import static cod.compiler.MTOTRegistry.AArch64Registers.*;
import static cod.compiler.MTOTRegistry.x86_64Registers.*;
import cod.compiler.BytecodeInstruction.Opcode;
import cod.compiler.natives.*;

import cod.ast.nodes.*;
import cod.debug.DebugSystem;
import java.util.*;
import java.lang.reflect.Field;

public class MTOTNativeCompiler {

    public final CPUProfile cpuProfile;
    
    public final String thisRegister;
    private final Set<String> calleeSavedSet;
    private final Set<String> abiCallerSavedSet;

    public final RegisterManager registerManager;
    private final RegisterManager.RegisterAllocator registerAllocator;
    private final RegisterManager.RegisterSpiller spiller;
    public final OperandStack operandStack;

    public List<String> assemblyCode = new ArrayList<String>();
    public final List<String> dataSection = new ArrayList<String>();

    private int dataLabelCounter = 0;
    private String currentMethodName = "";
    public final List<String> argumentRegisters;
    
    private int currentPc = 0;
    private List<BytecodeInstruction> currentMethodBytecode = null;
    private int currentLoopDepth = 0;

    public final RuntimeCallHelper runtimeCallHelper;
    public final OperationCompiler operationCompiler;
    public final MemoryAccessCompiler memoryAccessCompiler;
    public final ControlFlowCompiler controlFlowCompiler;
    public final MethodCallCompiler methodCallCompiler;

    public MTOTNativeCompiler(CPUProfile cpuProfile) {
        this.cpuProfile = cpuProfile;

        if (cpuProfile.architecture.equals("aarch64")) {
            this.thisRegister = AArch64Registers.x19;
            this.calleeSavedSet = new HashSet<String>(Arrays.asList(
                x19, x20, x21, x22, x23, x24, x25, x26, x27, x28
            ));
            this.abiCallerSavedSet = new HashSet<String>(Arrays.asList(
                x0, x1, x2, x3, x4, x5, x6, x7,
                x9, x10, x11, x12, x13, x14, x15, x16, x17
            ));
        } else {
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

        this.registerManager = new RegisterManager(this);
        this.registerAllocator = this.registerManager.getAllocator();
        this.spiller = this.registerManager.getSpiller();
        this.operandStack = new OperandStack(this);
        this.argumentRegisters = cpuProfile.registerFile.argumentRegisters;

        this.runtimeCallHelper = new RuntimeCallHelper(this);
        this.operationCompiler = new OperationCompiler(this);
        this.memoryAccessCompiler = new MemoryAccessCompiler(this);
        this.controlFlowCompiler = new ControlFlowCompiler(this);
        this.methodCallCompiler = new MethodCallCompiler(this);
    }

    public int getCurrentPc() { return currentPc; }
    public List<BytecodeInstruction> getCurrentMethodBytecode() { return currentMethodBytecode; }
    public int getCurrentLoopDepth() { return currentLoopDepth; }

    public String compileMethodFromBytecode(String methodName, List<BytecodeInstruction> bytecode) {
    DebugSystem.debug("MTOT", "Compiling method from bytecode: " + methodName);

    operandStack.clear();
    dataSection.clear();
    registerManager.reset();
    memoryAccessCompiler.reset();
    dataLabelCounter = 0;
    this.currentMethodName = methodName;
    this.currentMethodBytecode = bytecode;
    this.currentPc = 0;
    this.currentLoopDepth = 0;

    List<String> tempAssemblyCode = new ArrayList<String>();
    List<String> originalAssemblyCodeRef = this.assemblyCode;
    setAssemblyTarget(tempAssemblyCode);

    registerAllocator.markRegisterUsed(this.thisRegister);
    spiller.updateRegisterDefinitionDepth(this.thisRegister, 0);

    int maxParamsInRegs;
    int argStartIndex;
    if (cpuProfile.architecture.equals("aarch64")) {
        maxParamsInRegs = argumentRegisters.size() - 1;
        argStartIndex = 1;
    } else {
        maxParamsInRegs = (argumentRegisters.contains(x86_64Registers.rax)) ? 
                            argumentRegisters.size() - 1 : 
                            argumentRegisters.size();
        argStartIndex = 0;
    }

    for (int i = 0; i < maxParamsInRegs; i++) {
        String argReg = argumentRegisters.get(i + argStartIndex);
        registerAllocator.markRegisterUsed(argReg);
        spiller.updateRegisterDefinitionDepth(argReg, 0);
    }

    compileFromBytecode(bytecode, new HashMap<String, String>());

    setAssemblyTarget(originalAssemblyCodeRef);

    Set<String> usedCalleeSavedRegs = new HashSet<String>();
    Set<String> writtenRegisters = spiller.getWrittenRegistersDuringCompilation();
    writtenRegisters.retainAll(this.calleeSavedSet);
    usedCalleeSavedRegs.addAll(writtenRegisters);
    
    if (registerAllocator.getUsedRegisters().contains(this.thisRegister)) {
        usedCalleeSavedRegs.add(this.thisRegister);
    }

    List<String> calleeSavedToSave = new ArrayList<String>(usedCalleeSavedRegs);
    Collections.sort(calleeSavedToSave);

    int spillAreaSize = spiller.getTotalSpillSize();
    int calleeSavedAreaSize;
    if (cpuProfile.architecture.equals("aarch64")) {
        calleeSavedAreaSize = calleeSavedToSave.size() * 8;
    } else {
        calleeSavedAreaSize = 0;
    }
    
    int sizeBelowFp = calleeSavedAreaSize + spillAreaSize;
    int alignedSizeBelowFp = (sizeBelowFp + 15) & ~15;

    List<String> finalAssembly = new ArrayList<String>();

    if (!dataSection.isEmpty()) {
        finalAssembly.add(cpuProfile.syntax.dataSection);
        finalAssembly.addAll(dataSection);
    }

    finalAssembly.add(cpuProfile.syntax.textSection);
    
    if (cpuProfile.architecture.equals("x86_64")) {
        finalAssembly.add("    " + cpuProfile.syntax.commentMarker + " NASM requires extern declarations for runtime functions");
        finalAssembly.add("    extern runtime_print");
        finalAssembly.add("    extern string_concat");
        finalAssembly.add("    extern runtime_int_to_string");
        finalAssembly.add("    extern array_new");
        finalAssembly.add("    extern array_load");
        finalAssembly.add("    extern array_store");
        finalAssembly.add("    extern runtime_read_input");
        finalAssembly.add("");
    }
    
    finalAssembly.add(cpuProfile.syntax.globalDirective.replace("{name}", methodName));
    finalAssembly.add(methodName + ":");

    InstructionPattern prologuePattern = cpuProfile.getPattern("prologue");
    if (prologuePattern == null) throw new RuntimeException("Missing 'prologue' pattern!");
    for (String template : prologuePattern.assemblyTemplate) {
        finalAssembly.add("    " + template);
    }

    if (cpuProfile.architecture.equals("aarch64")) {
        if (alignedSizeBelowFp > 0) {
            String asm = cpuProfile.getPattern("alloc_stack_frame").assemblyTemplate.get(0)
                .replace("{size}", String.valueOf(alignedSizeBelowFp));
            finalAssembly.add("    " + asm);
        }
        
        int currentCalleeSaveOffset = -16;
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

    } else {
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
        spiller.setBaseSpillOffset(-8);
    }

    finalAssembly.add("    mov " + this.thisRegister + ", " + argumentRegisters.get(0) + " " + cpuProfile.syntax.commentMarker + " Copy 'this' pointer");
    
    finalAssembly.addAll(tempAssemblyCode);

    Map<Integer, String> slotLocations = memoryAccessCompiler.getSlotLocations();
    String returnReg0 = (cpuProfile.architecture.equals("x86_64")) ? x86_64Registers.rax : argumentRegisters.get(0);

    if (!slotLocations.isEmpty()) {
        List<Integer> sortedSlotIndices = new ArrayList<Integer>(slotLocations.keySet());
        Collections.sort(sortedSlotIndices);
        int returnRegIndex = 0;
        
        for (Integer slotIndex : sortedSlotIndices) {
            String abiReturnReg;
            if (cpuProfile.architecture.equals("x86_64")) {
                if (returnRegIndex == 0) abiReturnReg = x86_64Registers.rax;
                else if (returnRegIndex == 1) abiReturnReg = x86_64Registers.rdx;
                else break;
            } else {
                if (returnRegIndex >= argumentRegisters.size()) break;
                abiReturnReg = argumentRegisters.get(returnRegIndex);
            }
            
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
        DebugSystem.debug("MTOT", "Method " + methodName + " returning " + sortedSlotIndices.size() + " slot values");
    } 
    else if (!operandStack.isEmpty()) {
        String retValReg = operandStack.popToRegister();
        spiller.fillRegister(retValReg);
        
        String abiReturnReg = returnReg0;
        
        if (!retValReg.equals(abiReturnReg)) {
            finalAssembly.add("    mov " + abiReturnReg + ", " + retValReg);
            spiller.markRegisterModified(abiReturnReg);
            registerAllocator.freeRegister(retValReg);
        } else {
            spiller.markRegisterModified(abiReturnReg);
        }
        registerAllocator.markRegisterUsed(abiReturnReg);
        DebugSystem.debug("MTOT", "Method " + methodName + " returning single value from stack");
    } 
    else {
        String abiReturnReg = returnReg0;
        InstructionPattern p = cpuProfile.getPattern("load_immediate_int");
        finalAssembly.add("    " + p.assemblyTemplate.get(0).replace("{dest}", abiReturnReg).replace("{value}", "0") + " " + cpuProfile.syntax.commentMarker + " Default return 0");
        spiller.markRegisterModified(abiReturnReg);
        DebugSystem.debug("MTOT", "Method " + methodName + " returning default value 0");
    }

    InstructionPattern epiloguePattern = cpuProfile.getPattern("epilogue");
    if (epiloguePattern == null) throw new RuntimeException("Missing 'epilogue' pattern!");

    if (cpuProfile.architecture.equals("aarch64")) {
        if (!calleeSavedToSave.isEmpty()) {
            finalAssembly.add("    " + cpuProfile.syntax.commentMarker + " Restoring callee-saved registers: " + calleeSavedToSave);
            InstructionPattern restorePair = cpuProfile.getPattern("restore_callee_reg_pair");
            InstructionPattern restoreSingle = cpuProfile.getPattern("restore_callee_reg_single");
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
            finalAssembly.add("    " + epiloguePattern.assemblyTemplate.get(0));
        }
        
        for (int i = 1; i < epiloguePattern.assemblyTemplate.size(); i++) {
            finalAssembly.add("    " + epiloguePattern.assemblyTemplate.get(i));
        }

    } else {
        int x86SpillAreaSize = (spiller.getTotalSpillSize() + 15) & ~15;
        
        finalAssembly.add("    " + epiloguePattern.assemblyTemplate.get(0));
        
        if (!calleeSavedToSave.isEmpty()) {
            finalAssembly.add("    " + cpuProfile.syntax.commentMarker + " Restoring callee-saved registers: " + calleeSavedToSave);
            InstructionPattern restoreSingle = cpuProfile.getPattern("restore_callee_reg_single");
            List<String> reversedSaves = new ArrayList<>(calleeSavedToSave);
            Collections.reverse(reversedSaves);
            for (String reg : reversedSaves) {
                finalAssembly.add("    " + restoreSingle.assemblyTemplate.get(0).replace("{reg}", reg));
            }
        }
        
        for (int i = 1; i < epiloguePattern.assemblyTemplate.size(); i++) {
            finalAssembly.add("    " + epiloguePattern.assemblyTemplate.get(i));
        }
    }

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
        try { 
            Field listField = MTOTNativeCompiler.class.getDeclaredField("assemblyCode"); 
            listField.setAccessible(true); 
            listField.set(this, targetList); 
        } catch (Exception e) { 
            throw new RuntimeException("Failed to swap assembly target list via reflection", e); 
        }
    }

    private void compileFromBytecode(List<BytecodeInstruction> bytecode, Map<String, String> localRegisterMap) {
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

        for (int i = 0; i < bytecode.size(); i++) {
            this.currentPc = i;

            if (addressToLabelMap.containsKey(i)) {
                for(String label : addressToLabelMap.get(i)) {
                    assemblyCode.add(label + ":");
                    if (label.contains("loop_start") || label.contains("loop_body")) {
                        currentLoopDepth++;
                        DebugSystem.debug("LOOP_DEPTH", "Entered loop, depth = " + currentLoopDepth + " at label " + label);
                    } else if (label.contains("loop_end")) {
                        if (currentLoopDepth > 0) {
                            currentLoopDepth--;
                            DebugSystem.debug("LOOP_DEPTH", "Exited loop, depth = " + currentLoopDepth + " at label " + label);
                        } else {
                            DebugSystem.warn("LOOP_DEPTH", "Attempted to exit loop at depth 0, label " + label);
                        }
                    }
                }
            }

            BytecodeInstruction instr = bytecode.get(i);
            try {
                switch (instr.opcode) {
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

                    case ADD_INT: operationCompiler.generateBinaryOp("add_int"); break;
                    case SUB_INT: operationCompiler.generateBinaryOp("sub_int"); break;
                    case MUL_INT: operationCompiler.generateBinaryOp("mul_int"); break;
                    case DIV_INT: operationCompiler.generateBinaryOp("div_int"); break;
                    case MOD_INT: operationCompiler.generateBinaryOp("mod_int"); break;
                    case NEG_INT: operationCompiler.generateUnaryOp("neg_int"); break;

                    case CMP_EQ_INT: operationCompiler.generateBinaryOp("cmp_eq_int"); break;
                    case CMP_NE_INT: operationCompiler.generateBinaryOp("cmp_ne_int"); break;
                    case CMP_LT_INT: operationCompiler.generateBinaryOp("cmp_lt_int"); break;
                    case CMP_LE_INT: operationCompiler.generateBinaryOp("cmp_le_int"); break;
                    case CMP_GT_INT: operationCompiler.generateBinaryOp("cmp_gt_int"); break;
                    case CMP_GE_INT: operationCompiler.generateBinaryOp("cmp_ge_int"); break;

                    case JMP:         controlFlowCompiler.compileJmp(labelNameMap.get((String)instr.operand)); break;
                    case JMP_IF_TRUE: controlFlowCompiler.compileJmpIfTrue(labelNameMap.get((String)instr.operand)); break;
                    case JMP_IF_FALSE:controlFlowCompiler.compileJmpIfFalse(labelNameMap.get((String)instr.operand)); break;
                    case LABEL:       break;
                    case RET:         break;

                    case CALL:       methodCallCompiler.compileCall(instr, bytecode, i, localRegisterMap); break;
                    case CALL_SLOTS: methodCallCompiler.compileCallSlots(instr, bytecode, i, localRegisterMap); break;

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

    public String escapeString(String str) {
        str = str.replace("\\", "\\\\");
        str = str.replace("\"", "\\\"");
        str = str.replace("\n", "\\n");
        str = str.replace("\t", "\\t");
        str = str.replace("\0", "\\0");
        return str;
    }

    public String generateDataLabel(String prefix) {
        return prefix + "_" + this.currentMethodName + "_" + (dataLabelCounter++);
    }

    public void generateSpillCode(String register, int offset) {
        int finalOffset = offset;
        String baseReg = (cpuProfile.architecture.equals("x86_64")) ? x86_64Registers.rbp : AArch64Registers.fp;
        if (cpuProfile.architecture.equals("x86_64")) {
            if (offset > 0) finalOffset = -offset;
            baseReg = x86_64Registers.rbp;
        } else {
            baseReg = AArch64Registers.fp;
        }
        
        InstructionPattern pattern = cpuProfile.getPattern("store_to_stack");
        if (pattern == null) { throw new RuntimeException("Missing 'store_to_stack' instruction pattern for architecture: " + cpuProfile.architecture); }
        String asm = pattern.assemblyTemplate.get(0)
            .replace("{src_reg}", register)
            .replace("{offset}", String.valueOf(finalOffset));
            
        assemblyCode.add("    " + cpuProfile.syntax.commentMarker + " Spill " + register + " to [" + baseReg + ((finalOffset < 0) ? "" : "+") + finalOffset + "]");
        assemblyCode.add("    " + asm);
    }

    public void generateFillCode(String register, int offset) {
        int finalOffset = offset;
        String baseReg = (cpuProfile.architecture.equals("x86_64")) ? x86_64Registers.rbp : AArch64Registers.fp;
        if (cpuProfile.architecture.equals("x86_64")) {
            if (offset > 0) finalOffset = -offset;
            baseReg = x86_64Registers.rbp;
        } else {
            baseReg = AArch64Registers.fp;
        }
        
        InstructionPattern pattern = cpuProfile.getPattern("load_from_stack");
        if (pattern == null) { throw new RuntimeException("Missing 'load_from_stack' instruction pattern for architecture: " + cpuProfile.architecture); }
        String asm = pattern.assemblyTemplate.get(0)
            .replace("{dest_reg}", register)
            .replace("{offset}", String.valueOf(finalOffset));
            
        assemblyCode.add("    " + cpuProfile.syntax.commentMarker + " Fill " + register + " from [" + baseReg + ((finalOffset < 0) ? "" : "+") + finalOffset + "]");
        assemblyCode.add("    " + asm);
    }

    public Set<String> spillCallerSavedRegisters(Set<String> excludeArgs) {
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
                if (registerAllocator.usedRegisters.contains(reg)){
                    spiller.forceSpill(reg);
                }
            }
        } else {
            assemblyCode.add("    " + cpuProfile.syntax.commentMarker + " No caller-saved registers needed spilling before call.");
        }
        return callerSavedToSpill;
    }

    public void fillCallerSavedRegisters(Set<String> registersToFill) {
        if (registersToFill != null && !registersToFill.isEmpty()) {
            List<String> orderedRestore = new ArrayList<String>(registersToFill);
            Collections.sort(orderedRestore);

            assemblyCode.add("    " + cpuProfile.syntax.commentMarker + " Filling caller-saved registers after call: " + orderedRestore);
            for (String reg : orderedRestore) {
                spiller.fillRegister(reg);
                registerAllocator.markRegisterUsed(reg);
            }
        } else {
            assemblyCode.add("    " + cpuProfile.syntax.commentMarker + " No caller-saved registers needed filling after call.");
        }
    }
}