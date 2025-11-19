package cod.compiler.natives;

import cod.compiler.MTOTNativeCompiler;
import cod.compiler.RegisterManager;

public class ControlFlowCompiler {
    private final MTOTNativeCompiler compiler;
    private final cod.compiler.natives.OperandStack operandStack;
    private final RegisterManager.RegisterAllocator registerAllocator;
    private final RegisterManager.RegisterSpiller spiller;

    public ControlFlowCompiler(MTOTNativeCompiler compiler) {
        this.compiler = compiler;
        this.operandStack = compiler.operandStack;
        this.registerAllocator = compiler.registerManager.getAllocator();
        this.spiller = compiler.registerManager.getSpiller();
    }

    public void compileJmp(String label) {
        if (label == null) { 
            cod.debug.DebugSystem.error("MTOT", "JMP target label is null!"); 
            return; 
        }
        compiler.assemblyCode.add("    " + compiler.cpuProfile.getPattern("jmp").assemblyTemplate.get(0).replace("{label}", label));
    }

    public void compileJmpIfFalse(String label) {
        if (label == null) { 
            cod.debug.DebugSystem.error("MTOT", "JMP_IF_FALSE target label is null!"); 
            return; 
        }
        String c = operandStack.popToRegister();
        spiller.fillRegister(c);
        for (String t : compiler.cpuProfile.getPattern("jmp_if_false").assemblyTemplate) {
            compiler.assemblyCode.add("    " + t.replace("{condition}", c).replace("{label}", label));
        }
        registerAllocator.freeRegister(c);
    }

    public void compileJmpIfTrue(String label) {
        if (label == null) { 
            cod.debug.DebugSystem.error("MTOT", "JMP_IF_TRUE target label is null!"); 
            return; 
        }
        String c = operandStack.popToRegister();
        spiller.fillRegister(c);
        cod.compiler.MTOTRegistry.InstructionPattern p = compiler.cpuProfile.getPattern("jmp_if_true");
        if (p == null) {
            if (compiler.cpuProfile.architecture.equals("aarch64")) {
                compiler.assemblyCode.add("    cmp " + c + ", #0");
                compiler.assemblyCode.add("    b.ne " + label);
            } else {
                compiler.assemblyCode.add("    test " + c + ", " + c);
                compiler.assemblyCode.add("    jnz " + label);
            }
        } else {
            for (String t : p.assemblyTemplate) {
                compiler.assemblyCode.add("    " + t.replace("{condition}", c).replace("{label}", label));
            }
        }
        registerAllocator.freeRegister(c);
    }
}