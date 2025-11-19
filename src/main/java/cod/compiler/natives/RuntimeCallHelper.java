package cod.compiler.natives;

import cod.compiler.MTOTNativeCompiler;
import cod.compiler.RegisterManager;
import java.util.*;

public class RuntimeCallHelper {
    private final MTOTNativeCompiler compiler;
    private final cod.compiler.natives.OperandStack operandStack;
    private final RegisterManager.RegisterAllocator registerAllocator;
    private final RegisterManager.RegisterSpiller spiller;

    public RuntimeCallHelper(MTOTNativeCompiler compiler) {
        this.compiler = compiler;
        this.operandStack = compiler.operandStack;
        this.registerAllocator = compiler.registerManager.getAllocator();
        this.spiller = compiler.registerManager.getSpiller();
    }

    public void compileRuntimeCall(String functionName, int argCount, boolean hasReturnValue) {
        List<String> argValueRegs = new ArrayList<String>();
        Set<String> excludeArgs = new HashSet<String>();
        List<String> abiArgsUsed = new ArrayList<String>();

        int argStartIndex = (compiler.cpuProfile.architecture.equals("x86_64")) ? 0 : 0;

        for (int i = 0; i < argCount; i++) {
            if (i + argStartIndex >= compiler.argumentRegisters.size()) {
                cod.debug.DebugSystem.error("MTOT", "Runtime call " + functionName + " needs arg " + i + " but ABI only has " + compiler.argumentRegisters.size());
                break;
            }
            argValueRegs.add(operandStack.popToRegister());
            String abiArg = compiler.argumentRegisters.get(i + argStartIndex);
            excludeArgs.add(abiArg);
            abiArgsUsed.add(abiArg);
        }
        Collections.reverse(argValueRegs);

        Set<String> regsToSave = compiler.spillCallerSavedRegisters(excludeArgs);

        for (int i = 0; i < argValueRegs.size(); i++) {
            String valueReg = argValueRegs.get(i);
            String abiArg = abiArgsUsed.get(i);
            spiller.fillRegister(valueReg);
            registerAllocator.markRegisterUsed(abiArg);
            if (!abiArg.equals(valueReg)) {
                compiler.assemblyCode.add("    mov " + abiArg + ", " + valueReg);
            }
        }
        
        String callAsm = compiler.cpuProfile.getPattern("call").assemblyTemplate.get(0).replace("{name}", functionName);
        compiler.assemblyCode.add("    " + callAsm + " " + compiler.cpuProfile.syntax.commentMarker + " Call runtime helper");
        
        compiler.fillCallerSavedRegisters(regsToSave);

        String returnAbiReg = (compiler.cpuProfile.architecture.equals("x86_64")) ? 
            cod.compiler.MTOTRegistry.x86_64Registers.rax : compiler.argumentRegisters.get(0);
        
        if (hasReturnValue) {
            Set<String> avoidReturnRegs = new HashSet<String>(abiArgsUsed);
            avoidReturnRegs.add(returnAbiReg);

            String resultReg = registerAllocator.allocateRegister(avoidReturnRegs);
            compiler.assemblyCode.add("    mov " + resultReg + ", " + returnAbiReg + " " + compiler.cpuProfile.syntax.commentMarker + " Get result");
            operandStack.pushFromRegister(resultReg);
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
        String typeLabel = compiler.generateDataLabel("input_type");
        String directive = compiler.cpuProfile.syntax.stringDirective
            .replace("{label}", typeLabel)
            .replace("{value}", compiler.escapeString(expectedType));
        compiler.dataSection.add(directive);

        String abiArg0 = (compiler.cpuProfile.architecture.equals("x86_64")) ? 
            cod.compiler.MTOTRegistry.x86_64Registers.rdi : compiler.argumentRegisters.get(0);
        
        Set<String> excludeArgs = new HashSet<String>();
        excludeArgs.add(abiArg0);

        Set<String> regsToSave = compiler.spillCallerSavedRegisters(excludeArgs);

        registerAllocator.markRegisterUsed(abiArg0);
        cod.compiler.MTOTRegistry.InstructionPattern loadAddrPattern = compiler.cpuProfile.getPattern("load_address");
        for (String t : loadAddrPattern.assemblyTemplate) {
            compiler.assemblyCode.add("    " + t.replace("{dest}", abiArg0).replace("{label}", typeLabel));
        }

        String callAsm = compiler.cpuProfile.getPattern("call").assemblyTemplate.get(0).replace("{name}", "runtime_read_input");
        compiler.assemblyCode.add("    " + callAsm + " " + compiler.cpuProfile.syntax.commentMarker + " Call runtime helper (expects type* in arg0)");
        
        compiler.fillCallerSavedRegisters(regsToSave);

        String returnAbiReg = (compiler.cpuProfile.architecture.equals("x86_64")) ? 
            cod.compiler.MTOTRegistry.x86_64Registers.rax : compiler.argumentRegisters.get(0);

        Set<String> avoidReturnRegs = new HashSet<String>(Collections.singletonList(returnAbiReg));
        String resultReg = registerAllocator.allocateRegister(avoidReturnRegs);
        compiler.assemblyCode.add("    mov " + resultReg + ", " + returnAbiReg + " " + compiler.cpuProfile.syntax.commentMarker + " Get input result");
        operandStack.pushFromRegister(resultReg);
        spiller.markRegisterModified(resultReg);

        if (!resultReg.equals(returnAbiReg)) registerAllocator.freeRegister(returnAbiReg);
    }
}
