package cdrv.compiler;

// --- MODIFIED IMPORTS ---
import static cdrv.compiler.MTOTRegistry.*;
import static cdrv.compiler.MTOTRegistry.AArch64Registers.*;
import cdrv.compiler.BytecodeInstruction.Opcode; // Import Opcode
// --- END MODIFIED IMPORTS ---

import cdrv.ast.nodes.*;
import cdrv.debug.DebugSystem;
import java.util.*;
import java.lang.reflect.Field; // For swapping assembly target (workaround)

public class MTOTNativeCompiler {

    // --- MODIFIED FIELD ---
    final CPUProfile cpuProfile;
    // --- END MODIFIED FIELD ---

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
    // --- END MODIFIED CONSTRUCTOR ---
        this.cpuProfile = cpuProfile;

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

        registerAllocator.markRegisterUsed(x19); // Mark "this" register used early
        spiller.updateRegisterDefinitionDepth(x19, 0); // Mark 'this' as defined outside loops

        // Mark incoming argument registers as used initially
        int maxParamsInRegs = argumentRegisters.size() - 1;
        for (int i = 0; i < maxParamsInRegs; i++) {
             String argReg = argumentRegisters.get(i + 1);
             registerAllocator.markRegisterUsed(argReg);
             spiller.updateRegisterDefinitionDepth(argReg, 0); // Args defined outside loops
        }

        // Compile the actual bytecode instructions
        compileFromBytecode(bytecode, new HashMap<String, String>()); // Pass empty map initially

        setAssemblyTarget(originalAssemblyCodeRef);
        // --- END STAGE 1 ---


        // --- STAGE 2: Analyze Usage and Construct Final Assembly ---
        // ... (This section remains unchanged from previous version) ...
         Set<String> usedCalleeSavedRegs = new HashSet<String>();
        Set<String> calleeSavedSet = new HashSet<String>(Arrays.asList(
                x19, x20, x21, x22, x23, x24, x25, x26, x27, x28
        ));
        Set<String> writtenRegisters = spiller.getWrittenRegistersDuringCompilation();
        writtenRegisters.retainAll(calleeSavedSet);
        usedCalleeSavedRegs.addAll(writtenRegisters);
        if (registerAllocator.getUsedRegisters().contains(x19)) {
             usedCalleeSavedRegs.add(x19);
        }

        List<String> calleeSavedToSave = new ArrayList<String>(usedCalleeSavedRegs);
        Collections.sort(calleeSavedToSave);

        int spillAreaSize = spiller.getTotalSpillSize();
        int calleeSavedAreaSize = calleeSavedToSave.size() * 8;
        int sizeBelowFp = calleeSavedAreaSize + spillAreaSize;
        int alignedSizeBelowFp = (sizeBelowFp + 15) & ~15;

        List<String> finalAssembly = new ArrayList<String>();

        if (!dataSection.isEmpty()) {
            finalAssembly.add(cpuProfile.syntax.dataSection);
            finalAssembly.addAll(dataSection);
        }

        finalAssembly.add(cpuProfile.syntax.textSection);
        finalAssembly.add(cpuProfile.syntax.globalDirective.replace("{name}", methodName));
        finalAssembly.add(methodName + ":");

        // --- Corrected PROLOGUE ---
        finalAssembly.add(arm_prologue_1);
        finalAssembly.add("    mov x29, sp");
        if (alignedSizeBelowFp > 0) {
            finalAssembly.add("    sub sp, sp, #" + alignedSizeBelowFp);
        }
        int currentCalleeSaveOffset = -16;
        if (!calleeSavedToSave.isEmpty()) {
            finalAssembly.add("    " + cpuProfile.syntax.commentMarker + " Saving callee-saved registers: " + calleeSavedToSave);
            for (int i = 0; i < calleeSavedToSave.size(); i += 2) {
                if (i + 1 < calleeSavedToSave.size()) {
                    finalAssembly.add("    stp " + calleeSavedToSave.get(i) + ", " + calleeSavedToSave.get(i+1) + ", [x29, #" + currentCalleeSaveOffset + "]");
                } else {
                    finalAssembly.add("    str " + calleeSavedToSave.get(i) + ", [x29, #" + currentCalleeSaveOffset + "]");
                }
                 currentCalleeSaveOffset -= 16;
            }
        }
        spiller.setBaseSpillOffset(currentCalleeSaveOffset + 8);
        // --- END Corrected PROLOGUE ---

        finalAssembly.add("    mov " + x19 + ", " + argumentRegisters.get(0) + " " + cpuProfile.syntax.commentMarker + " Copy 'this' pointer");
        finalAssembly.addAll(tempAssemblyCode); // Add the compiled body

        // --- Corrected EPILOGUE (Unchanged from previous version) ---
        // ... (Previous Epilogue code here) ...
         Map<Integer, String> slotLocations = memoryAccessCompiler.getSlotLocations();
        if (!slotLocations.isEmpty()) {
             List<Integer> sortedSlotIndices = new ArrayList<Integer>(slotLocations.keySet());
             Collections.sort(sortedSlotIndices);
             int returnRegIndex = 0;
             for (Integer slotIndex : sortedSlotIndices) {
                 if (returnRegIndex >= argumentRegisters.size()) break;
                 String valueLocation = slotLocations.get(slotIndex);
                 String abiReturnReg = argumentRegisters.get(returnRegIndex);
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
            String abiReturnReg = argumentRegisters.get(0);
            if (!retValReg.equals(abiReturnReg)) {
                finalAssembly.add("    mov " + abiReturnReg + ", " + retValReg);
                spiller.markRegisterModified(abiReturnReg);
                registerAllocator.freeRegister(retValReg);
            } else {
                 spiller.markRegisterModified(abiReturnReg);
            }
            registerAllocator.markRegisterUsed(abiReturnReg);
        } else {
            finalAssembly.add("    mov " + argumentRegisters.get(0) + ", #0 " + cpuProfile.syntax.commentMarker + " Default return 0");
            spiller.markRegisterModified(argumentRegisters.get(0));
        }

        // Restore callee-saved registers
        if (!calleeSavedToSave.isEmpty()) {
             finalAssembly.add("    " + cpuProfile.syntax.commentMarker + " Restoring callee-saved registers: " + calleeSavedToSave);
             currentCalleeSaveOffset = -16 - (calleeSavedToSave.size() / 2) * 16 - (calleeSavedToSave.size() % 2 == 0 ? 0 : -8);
             for (int i = (calleeSavedToSave.size() % 2 == 0 ? calleeSavedToSave.size() - 2 : calleeSavedToSave.size() - 1); i >= 0; i -= 2) {
                 if (i + 1 < calleeSavedToSave.size()) {
                     finalAssembly.add("    ldp " + calleeSavedToSave.get(i) + ", " + calleeSavedToSave.get(i+1) + ", [x29, #" + currentCalleeSaveOffset + "]");
                 } else {
                      finalAssembly.add("    ldr " + calleeSavedToSave.get(i) + ", [x29, #" + currentCalleeSaveOffset + "]");
                 }
                 currentCalleeSaveOffset += 16;
             }
        }

        if (alignedSizeBelowFp > 0) {
            finalAssembly.add("    mov sp, x29");
        }

        finalAssembly.add("    ldp x29, x30, [sp], #16");
        finalAssembly.add("    ret");
        // --- END Corrected EPILOGUE ---

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
                    assemblyCode.add(label + ":");
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
    // --- INNER CLASSES FOR SEPARATION OF CONCERNS (Mostly Unchanged) ---
    // --- ======================================================= ---

    // Keep OperandStack, RuntimeCallHelper, ControlFlowCompiler, MethodCallCompiler
    // exactly as they were in the previous correct update.
    // ... (Previous inner class code here) ...
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
        // ... (Keep compileRuntimeCall and compileReadInput unchanged) ...
        public void compileRuntimeCall(String functionName, int argCount, boolean hasReturnValue) {
            List<String> argValueRegs = new ArrayList<String>();
            Set<String> excludeArgs = new HashSet<String>();
            List<String> abiArgsUsed = new ArrayList<String>();

            for (int i = 0; i < argCount; i++) {
                if (i >= argumentRegisters.size()) {
                    DebugSystem.error("MTOT", "Runtime call " + functionName + " needs arg " + i + " but ABI only has " + argumentRegisters.size());
                    break;
                }
                argValueRegs.add(operandStack.popToRegister());
                String abiArg = argumentRegisters.get(i);
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

            assemblyCode.add("    bl " + functionName + " " + cpuProfile.syntax.commentMarker + " Call runtime helper");
            fillCallerSavedRegisters(regsToSave);

            String returnAbiReg = argumentRegisters.get(0);
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

            String abiArg0 = argumentRegisters.get(0);
            Set<String> excludeArgs = new HashSet<String>();
            excludeArgs.add(abiArg0);

            Set<String> regsToSave = spillCallerSavedRegisters(excludeArgs);

            registerAllocator.markRegisterUsed(abiArg0);
            // --- MODIFIED TYPE ---
            InstructionPattern loadAddrPattern = cpuProfile.getPattern("load_address");
            // --- END MODIFIED TYPE ---
            for (String t : loadAddrPattern.assemblyTemplate) {
                assemblyCode.add("    " + t.replace("{dest}", abiArg0).replace("{label}", typeLabel));
            }

            assemblyCode.add("    bl runtime_read_input " + cpuProfile.syntax.commentMarker + " Call runtime helper (expects type* in x0)");
            fillCallerSavedRegisters(regsToSave);

            Set<String> avoidReturnRegs = new HashSet<String>(Collections.singletonList(abiArg0));
            String resultReg = registerAllocator.allocateRegister(avoidReturnRegs);
            assemblyCode.add("    mov " + resultReg + ", " + abiArg0 + " " + cpuProfile.syntax.commentMarker + " Get input result");
            operandStack.pushFromRegister(resultReg); // pushFromRegister updates depth
            spiller.markRegisterModified(resultReg);

            if (!resultReg.equals(abiArg0)) registerAllocator.freeRegister(abiArg0);
        }
    }

    private class OperationCompiler {
        // --- UPDATE: Call spiller.updateRegisterDefinitionDepth ---
        public void generateBinaryOp(String patternName) {
            InstructionPattern p = cpuProfile.getPattern(patternName);
            if (p == null) throw new RuntimeException("No pattern for " + patternName);

            String[] ops = operandStack.popTwoOperands();
            String l = ops[0], r = ops[1];
            Set<String> avoid = new HashSet<String>(Arrays.asList(l, r));
            String res = registerAllocator.allocateRegister(avoid);

            spiller.fillRegister(l);
            spiller.fillRegister(r);

            for (String t : p.assemblyTemplate) {
                assemblyCode.add("    " + t.replace("{dest}", res).replace("{src1}", l).replace("{src2}", r));
            }

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
        // --- END UPDATE ---
    }

    private class MemoryAccessCompiler {
        // --- UPDATE: Call spiller.updateRegisterDefinitionDepth in push/load methods ---
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
            assemblyCode.add("    mov " + r + ", #" + value);
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
            assemblyCode.add("    mov " + r + ", #0");
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
            String destReg = registerAllocator.allocateRegister(Collections.singleton(x19));
            spiller.fillRegister(x19);
            InstructionPattern pattern = cpuProfile.getPattern("load_field_offset");
            if (pattern == null) throw new RuntimeException("Missing 'load_field_offset' pattern!");
            String asm = pattern.assemblyTemplate.get(0).replace("{dest_reg}", destReg).replace("{base_reg}", x19).replace("{offset}", String.valueOf(offset));
            assemblyCode.add("    " + cpuProfile.syntax.commentMarker + " Load field " + fieldName + " (offset " + offset + ")");
            assemblyCode.add("    " + asm);
            operandStack.pushFromRegister(destReg); // Updates depth
            spiller.markRegisterModified(destReg);
        }

        public void compileStoreField(String fieldName) { /* ... unchanged ... */
            int offset = getFieldOffset(fieldName);
            String valueReg = operandStack.popToRegister();
            spiller.fillRegister(x19);
            spiller.fillRegister(valueReg);
            InstructionPattern pattern = cpuProfile.getPattern("store_field_offset");
            if (pattern == null) throw new RuntimeException("Missing 'store_field_offset' pattern!");
            String asm = pattern.assemblyTemplate.get(0).replace("{src_reg}", valueReg).replace("{base_reg}", x19).replace("{offset}", String.valueOf(offset));
            assemblyCode.add("    " + cpuProfile.syntax.commentMarker + " Store field " + fieldName + " (offset " + offset + ")");
            assemblyCode.add("    " + asm);
            registerAllocator.freeRegister(valueReg);
        }

        public void compileStoreLocal(int slotIndex, Map<String, String> localRegisterMap) {
            // Keep the logic from the previous "safer" version
            // Ensure markRegisterModified is called on targetReg
            // This now implicitly handles forced spills if needed
            // ... (Previous compileStoreLocal code here) ...
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
            // Keep the logic from the previous "safer" version
            // Ensure spiller.fillRegister is called on varReg
            // Ensure depth is updated for tempReg
            // ... (Previous compileLoadLocal code here) ...
             String varRegKey = "local_" + slotIndex;
            String varReg = localRegisterMap.get(varRegKey);

            if (varReg == null) {
                DebugSystem.error("MTOT_REG", "Register for " + varRegKey + " is null during LOAD! Recovering by allocating new register and assuming value 0.");
                varReg = registerAllocator.allocateRegister();
                localRegisterMap.put(varRegKey, varReg);
                spiller.mapSlotToRegister(slotIndex, varReg);
                assemblyCode.add("    mov " + varReg + ", #0 " + cpuProfile.syntax.commentMarker + " WARNING: Used uninitialized local slot " + slotIndex);
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

        public void compileStoreSlot(Object operand, Map<String, String> localRegisterMap) { /* ... unchanged ... */
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
        // --- END UPDATE ---
    }

    private class ControlFlowCompiler { /* ... unchanged ... */
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
                assemblyCode.add("    cmp " + c + ", #0");
                assemblyCode.add("    b.ne " + label);
            } else {
                for (String t : p.assemblyTemplate) {
                    assemblyCode.add("    " + t.replace("{condition}", c).replace("{label}", label));
                }
            }
             registerAllocator.freeRegister(c); // Condition register likely not needed after jump
        }
    }

    private class MethodCallCompiler { /* ... unchanged ... */
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
            excludeArgs.add(argumentRegisters.get(0)); // x0 for 'this'
            List<String> abiArgsUsedForParams = new ArrayList<String>();
            for (int i = 0; i < argCount; i++) {
                if (i < argumentRegisters.size() - 1) {
                    String abiArgReg = argumentRegisters.get(i + 1);
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

            String thisAbiArgReg = argumentRegisters.get(0);
            spiller.fillRegister(x19);
            registerAllocator.markRegisterUsed(thisAbiArgReg);
            if (!thisAbiArgReg.equals(x19)) {
                assemblyCode.add("    mov " + thisAbiArgReg + ", " + x19 + " " + cpuProfile.syntax.commentMarker + " Setup 'this' pointer");
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

            for (String reg : abiArgsUsedForParams) {
                registerAllocator.freeRegister(reg);
            }

            String returnRegAbi = argumentRegisters.get(0);
            Set<String> avoidReturnReg = Collections.singleton(returnRegAbi);
            String resultReg = registerAllocator.allocateRegister(avoidReturnReg);
            assemblyCode.add("    mov " + resultReg + ", " + returnRegAbi + " " + cpuProfile.syntax.commentMarker + " Get result");
            operandStack.pushFromRegister(resultReg); // Updates depth
            spiller.markRegisterModified(resultReg);

            if (!resultReg.equals(returnRegAbi)) {
                registerAllocator.freeRegister(returnRegAbi);
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
            excludeArgs.add(argumentRegisters.get(0)); // x0 for 'this'
            List<String> abiArgsUsedForParams = new ArrayList<String>();
            for (int i = 0; i < argCount; i++) {
                if (i < argumentRegisters.size() - 1) {
                    String abiArgReg = argumentRegisters.get(i + 1);
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

            String thisAbiArgReg = argumentRegisters.get(0);
            spiller.fillRegister(x19);
            registerAllocator.markRegisterUsed(thisAbiArgReg);
            if (!thisAbiArgReg.equals(x19)) {
                assemblyCode.add("    mov " + thisAbiArgReg + ", " + x19 + " " + cpuProfile.syntax.commentMarker + " Setup 'this' pointer");
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

            Set<String> avoidReturnRegs = new HashSet<String>(returnSlotRegs);
            for (int i = 0; i < numSlots; i++) {
                if (i >= argumentRegisters.size()) {
                    DebugSystem.error("MTOT_SLOT", "Requested slot " + i + " but only " + argumentRegisters.size() + " return registers exist!");
                    String errReg = registerAllocator.allocateRegister(avoidReturnRegs);
                    assemblyCode.add("    mov " + errReg + ", #0 " + cpuProfile.syntax.commentMarker + " ERROR: Missing return slot " + i);
                    operandStack.pushFromRegister(errReg); // Updates depth
                    spiller.markRegisterModified(errReg);
                    avoidReturnRegs.add(errReg);
                    continue;
                }

                String returnRegAbi = argumentRegisters.get(i);
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

    // Keep escapeString, generateDataLabel, generateSpillCode, generateFillCode,
    // spillCallerSavedRegisters, fillCallerSavedRegisters unchanged from previous correct version
    // ... (Previous helper method code here) ...
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
        InstructionPattern pattern = cpuProfile.getPattern("store_to_stack");
        if (pattern == null) { throw new RuntimeException("Missing 'store_to_stack' instruction pattern for architecture: " + cpuProfile.architecture); }
        String asm = pattern.assemblyTemplate.get(0).replace("{src_reg}", register).replace("{offset}", String.valueOf(offset));
        assemblyCode.add("    " + cpuProfile.syntax.commentMarker + " Spill " + register + " to [fp" + (offset < 0 ? "" : "+") + offset + "]");
        assemblyCode.add("    " + asm);
    }

    public void generateFillCode(String register, int offset) {
        InstructionPattern pattern = cpuProfile.getPattern("load_from_stack");
        if (pattern == null) { throw new RuntimeException("Missing 'load_from_stack' instruction pattern for architecture: " + cpuProfile.architecture); }
        String asm = pattern.assemblyTemplate.get(0).replace("{dest_reg}", register).replace("{offset}", String.valueOf(offset));
        assemblyCode.add("    " + cpuProfile.syntax.commentMarker + " Fill " + register + " from [fp" + (offset < 0 ? "" : "+") + offset + "]");
        assemblyCode.add("    " + asm);
    }

    private Set<String> spillCallerSavedRegisters(Set<String> excludeArgs) {
        Set<String> currentlyLiveRegs = registerAllocator.getUsedRegisters();
        Set<String> callerSavedToSpill = new HashSet<String>();

        Set<String> abiCallerSaved = new HashSet<String>(Arrays.asList(
            x0, x1, x2, x3, x4, x5, x6, x7,
            x9, x10, x11, x12, x13, x14, x15
            // x16, x17 are often caller-saved scratch but sometimes handled specially
        ));

        callerSavedToSpill.addAll(currentlyLiveRegs);
        callerSavedToSpill.retainAll(abiCallerSaved);
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