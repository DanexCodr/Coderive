package cod.compiler.natives;

import cod.compiler.MTOTNativeCompiler;
import cod.compiler.RegisterManager;
import cod.compiler.BytecodeInstruction;
import java.util.*;

public class MethodCallCompiler {
    private final MTOTNativeCompiler compiler;
    private final cod.compiler.natives.OperandStack operandStack;
    private final RegisterManager.RegisterAllocator registerAllocator;
    private final RegisterManager.RegisterSpiller spiller;

    public MethodCallCompiler(MTOTNativeCompiler compiler) {
        this.compiler = compiler;
        this.operandStack = compiler.operandStack;
        this.registerAllocator = compiler.registerManager.getAllocator();
        this.spiller = compiler.registerManager.getSpiller();
    }

    public void compileCall(BytecodeInstruction instr, java.util.List<BytecodeInstruction> bytecode, int index, java.util.Map<String, String> localRegisterMap) {
        String methodName = (String) instr.operand;
        int argCount = countArgsForCall(bytecode, index);
        cod.debug.DebugSystem.debug("MTOT", "Compiling call to " + methodName + " with " + argCount + " arguments.");

        java.util.List<String> argValueRegs = new java.util.ArrayList<String>();
        for (int i = 0; i < argCount; i++) {
            argValueRegs.add(operandStack.popToRegister());
        }
        java.util.Collections.reverse(argValueRegs);

        java.util.Set<String> excludeArgs = new java.util.HashSet<String>();
        excludeArgs.add(compiler.argumentRegisters.get(0));
        java.util.List<String> abiArgsUsedForParams = new java.util.ArrayList<String>();
        
        int argStartIndex = (compiler.cpuProfile.architecture.equals("x86_64")) ? 0 : 1;
        
        for (int i = 0; i < argCount; i++) {
            int abiArgIndex = i + argStartIndex;
            if (abiArgIndex < compiler.argumentRegisters.size()) {
                String abiArgReg = compiler.argumentRegisters.get(abiArgIndex);
                if (compiler.cpuProfile.architecture.equals("x86_64") && abiArgReg.equals(cod.compiler.MTOTRegistry.x86_64Registers.rax)) {
                    cod.debug.DebugSystem.warn("MTOT", "x86 ABI does not use rax for arguments. Skipping arg " + i);
                    abiArgIndex++;
                    if (abiArgIndex >= compiler.argumentRegisters.size()) continue;
                    abiArgReg = compiler.argumentRegisters.get(abiArgIndex);
                }
                excludeArgs.add(abiArgReg);
                abiArgsUsedForParams.add(abiArgReg);
            }
        }

        java.util.Set<String> regsToSave = compiler.spillCallerSavedRegisters(excludeArgs);

        for (int i = 0; i < argCount; i++) {
            if (i < abiArgsUsedForParams.size()) {
                String abiArgReg = abiArgsUsedForParams.get(i);
                String valueReg = argValueRegs.get(i);
                spiller.fillRegister(valueReg);
                registerAllocator.markRegisterUsed(abiArgReg);
                if (!abiArgReg.equals(valueReg)) {
                    compiler.assemblyCode.add("    mov " + abiArgReg + ", " + valueReg + " " + compiler.cpuProfile.syntax.commentMarker + " Setup arg " + (i + 1));
                }
            } else {
                cod.debug.DebugSystem.warn("MTOT", "Argument " + i + " stack passing not implemented for call to " + methodName);
            }
        }

        String thisAbiArgReg = (compiler.cpuProfile.architecture.equals("x86_64")) ? 
            cod.compiler.MTOTRegistry.x86_64Registers.rdi : compiler.argumentRegisters.get(0);
        spiller.fillRegister(compiler.thisRegister);
        registerAllocator.markRegisterUsed(thisAbiArgReg);
        if (!thisAbiArgReg.equals(compiler.thisRegister)) {
            compiler.assemblyCode.add("    mov " + thisAbiArgReg + ", " + compiler.thisRegister + " " + compiler.cpuProfile.syntax.commentMarker + " Setup 'this' pointer");
        }

        for (int i = 0; i < argValueRegs.size(); i++) {
            String valueReg = argValueRegs.get(i);
            boolean isUsedForArg = (i < abiArgsUsedForParams.size() && valueReg.equals(abiArgsUsedForParams.get(i)));
            if (!isUsedForArg) {
                registerAllocator.freeRegister(valueReg);
            }
        }

        String callAsm = compiler.cpuProfile.getPattern("call").assemblyTemplate.get(0).replace("{name}", methodName);
        compiler.assemblyCode.add("    " + callAsm);

        compiler.fillCallerSavedRegisters(regsToSave);

        for (String reg : abiArgsUsedForParams) {
            registerAllocator.freeRegister(reg);
        }

        String returnAbiReg = (compiler.cpuProfile.architecture.equals("x86_64")) ? 
            cod.compiler.MTOTRegistry.x86_64Registers.rax : compiler.argumentRegisters.get(0);
        
        java.util.Set<String> avoidReturnReg = java.util.Collections.singleton(returnAbiReg);
        String resultReg = registerAllocator.allocateRegister(avoidReturnReg);
        compiler.assemblyCode.add("    mov " + resultReg + ", " + returnAbiReg + " " + compiler.cpuProfile.syntax.commentMarker + " Get result");
        operandStack.pushFromRegister(resultReg);
        spiller.markRegisterModified(resultReg);

        if (!resultReg.equals(returnAbiReg)) {
            registerAllocator.freeRegister(returnAbiReg);
        }
    }

    public void compileCallSlots(BytecodeInstruction instr, java.util.List<BytecodeInstruction> bytecode, int index, java.util.Map<String, String> localRegisterMap) {
        Object[] operand = (Object[]) instr.operand;
        String methodName = (String) operand[0];
        int numSlots = ((Integer) operand[1]).intValue();
        int argCount = countArgsForCall(bytecode, index);
        cod.debug.DebugSystem.debug("MTOT", "Compiling CALL_SLOTS to " + methodName + " with " + argCount + " arguments, expecting " + numSlots + " slots.");

        java.util.List<String> argValueRegs = new java.util.ArrayList<String>();
        for (int i = 0; i < argCount; i++) {
            argValueRegs.add(operandStack.popToRegister());
        }
        java.util.Collections.reverse(argValueRegs);

        java.util.Set<String> excludeArgs = new java.util.HashSet<String>();
        
        String thisAbiArgReg = (compiler.cpuProfile.architecture.equals("x86_64")) ? 
            cod.compiler.MTOTRegistry.x86_64Registers.rdi : compiler.argumentRegisters.get(0);
        excludeArgs.add(thisAbiArgReg);
        
        java.util.List<String> abiArgsUsedForParams = new java.util.ArrayList<String>();
        
        int argStartIndex = (compiler.cpuProfile.architecture.equals("x86_64")) ? 1 : 1;
        
        for (int i = 0; i < argCount; i++) {
            int abiArgIndex = i + argStartIndex;
            if (abiArgIndex < compiler.argumentRegisters.size()) {
                String abiArgReg = compiler.argumentRegisters.get(abiArgIndex);
                if (compiler.cpuProfile.architecture.equals("x86_64") && abiArgReg.equals(cod.compiler.MTOTRegistry.x86_64Registers.rax)) {
                    cod.debug.DebugSystem.warn("MTOT", "x86 ABI does not use rax for arguments. Skipping arg " + i);
                    abiArgIndex++;
                    if (abiArgIndex >= compiler.argumentRegisters.size()) continue;
                    abiArgReg = compiler.argumentRegisters.get(abiArgIndex);
                }
                excludeArgs.add(abiArgReg);
                abiArgsUsedForParams.add(abiArgReg);
            }
        }

        java.util.Set<String> regsToSave = compiler.spillCallerSavedRegisters(excludeArgs);

        for (int i = 0; i < argCount; i++) {
            if (i < abiArgsUsedForParams.size()) {
                String abiArgReg = abiArgsUsedForParams.get(i);
                String valueReg = argValueRegs.get(i);
                spiller.fillRegister(valueReg);
                registerAllocator.markRegisterUsed(abiArgReg);
                if (!abiArgReg.equals(valueReg)) {
                    compiler.assemblyCode.add("    mov " + abiArgReg + ", " + valueReg + " " + compiler.cpuProfile.syntax.commentMarker + " Setup arg " + (i + 1));
                }
            } else {
                cod.debug.DebugSystem.warn("MTOT", "Argument " + i + " stack passing not implemented for CALL_SLOTS to " + methodName);
            }
        }

        spiller.fillRegister(compiler.thisRegister);
        registerAllocator.markRegisterUsed(thisAbiArgReg);
        if (!thisAbiArgReg.equals(compiler.thisRegister)) {
            compiler.assemblyCode.add("    mov " + thisAbiArgReg + ", " + compiler.thisRegister + " " + compiler.cpuProfile.syntax.commentMarker + " Setup 'this' pointer");
        }

        for (int i = 0; i < argValueRegs.size(); i++) {
            String valueReg = argValueRegs.get(i);
            boolean isUsedForArg = (i < abiArgsUsedForParams.size() && valueReg.equals(abiArgsUsedForParams.get(i)));
            if (!isUsedForArg) {
                registerAllocator.freeRegister(valueReg);
            }
        }

        String callAsm = compiler.cpuProfile.getPattern("call").assemblyTemplate.get(0).replace("{name}", methodName);
        compiler.assemblyCode.add("    " + callAsm);

        compiler.fillCallerSavedRegisters(regsToSave);

        java.util.Set<String> returnSlotRegs = new java.util.HashSet<String>();
        
        if (compiler.cpuProfile.architecture.equals("x86_64")) {
            String ret0 = cod.compiler.MTOTRegistry.x86_64Registers.rax;
            String ret1 = cod.compiler.MTOTRegistry.x86_64Registers.rdx;
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
        } else {
            for (int i = 0; i < numSlots && i < compiler.argumentRegisters.size(); i++) {
                returnSlotRegs.add(compiler.argumentRegisters.get(i));
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

        java.util.Set<String> avoidReturnRegs = new java.util.HashSet<String>(returnSlotRegs);
        for (int i = 0; i < numSlots; i++) {
            String returnRegAbi;
            if (compiler.cpuProfile.architecture.equals("x86_64")) {
                if (i == 0) returnRegAbi = cod.compiler.MTOTRegistry.x86_64Registers.rax;
                else if (i == 1) returnRegAbi = cod.compiler.MTOTRegistry.x86_64Registers.rdx;
                else {
                    cod.debug.DebugSystem.error("MTOT_SLOT", "Requested slot " + i + " but x86 only supports 2 return registers (rax, rdx)!");
                    returnRegAbi = null;
                }
            } else {
                if (i >= compiler.argumentRegisters.size()) {
                    cod.debug.DebugSystem.error("MTOT_SLOT", "Requested slot " + i + " but only " + compiler.argumentRegisters.size() + " return registers exist!");
                    returnRegAbi = null;
                } else {
                    returnRegAbi = compiler.argumentRegisters.get(i);
                }
            }

            if (returnRegAbi == null) {
                String errReg = registerAllocator.allocateRegister(avoidReturnRegs);
                cod.compiler.MTOTRegistry.InstructionPattern p = compiler.cpuProfile.getPattern("load_immediate_int");
                compiler.assemblyCode.add("    " + p.assemblyTemplate.get(0).replace("{dest}", errReg).replace("{value}", "0") + " " + compiler.cpuProfile.syntax.commentMarker + " ERROR: Missing return slot " + i);
                operandStack.pushFromRegister(errReg);
                spiller.markRegisterModified(errReg);
                avoidReturnRegs.add(errReg);
                continue;
            }

            String resultReg = registerAllocator.allocateRegister(avoidReturnRegs);
            compiler.assemblyCode.add("    mov " + resultReg + ", " + returnRegAbi + " " + compiler.cpuProfile.syntax.commentMarker + " Get result slot " + i);
            operandStack.pushFromRegister(resultReg);
            spiller.markRegisterModified(resultReg);

            if (!resultReg.equals(returnRegAbi)) {
                registerAllocator.freeRegister(returnRegAbi);
            }
            avoidReturnRegs.remove(returnRegAbi);
            avoidReturnRegs.add(resultReg);
        }
    }

    private int countArgsForCall(java.util.List<BytecodeInstruction> bytecode, int callInstructionIndex) {
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
                break;
            }

            required_depth -= pushes;
            required_depth += pops;
            if (required_depth < 0) {
                max_required_depth++;
                required_depth = 0;
            }
        }
        return Math.max(0, max_required_depth);
    }
}