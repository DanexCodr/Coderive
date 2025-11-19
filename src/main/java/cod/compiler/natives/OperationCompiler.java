package cod.compiler.natives;

import cod.compiler.MTOTNativeCompiler;
import cod.compiler.RegisterManager;
import java.util.*;

public class OperationCompiler {
    private final MTOTNativeCompiler compiler;
    private final cod.compiler.natives.OperandStack operandStack;
    private final RegisterManager.RegisterAllocator registerAllocator;
    private final RegisterManager.RegisterSpiller spiller;

    public OperationCompiler(MTOTNativeCompiler compiler) {
        this.compiler = compiler;
        this.operandStack = compiler.operandStack;
        this.registerAllocator = compiler.registerManager.getAllocator();
        this.spiller = compiler.registerManager.getSpiller();
    }

    public void generateBinaryOp(String patternName) {
        cod.compiler.MTOTRegistry.InstructionPattern p = compiler.cpuProfile.getPattern(patternName);
        if (p == null) throw new RuntimeException("No pattern for " + patternName);

        String[] ops = operandStack.popTwoOperands();
        String l = ops[0], r = ops[1];
        Set<String> avoid = new HashSet<String>(Arrays.asList(l, r));
        String res = registerAllocator.allocateRegister(avoid);

        spiller.fillRegister(l);
        spiller.fillRegister(r);
        
        Set<String> clobberedRegs = new HashSet<String>();
        if (p.requiredRegisters != null) {
            for (String clobbered : p.requiredRegisters) {
                if (clobbered.equals("al")) clobbered = cod.compiler.MTOTRegistry.x86_64Registers.rax;
                registerAllocator.markRegisterUsed(clobbered);
                clobberedRegs.add(clobbered);
            }
        }

        for (String t : p.assemblyTemplate) {
            String asm = t.replace("{dest}", res).replace("{src1}", l).replace("{src2}", r);
            compiler.assemblyCode.add("    " + asm);
        }
        
        for (String clobbered : clobberedRegs) {
            if (patternName.equals("mod_int") && clobbered.equals(cod.compiler.MTOTRegistry.x86_64Registers.rax)) {
                 registerAllocator.freeRegister(clobbered);
            }
            else if (patternName.equals("div_int") && clobbered.equals(cod.compiler.MTOTRegistry.x86_64Registers.rdx)) {
                 registerAllocator.freeRegister(clobbered);
            }
            else if (!clobbered.equals(res)) {
                registerAllocator.freeRegister(clobbered);
            }
        }

        operandStack.pushFromRegister(res);
        spiller.markRegisterModified(res);
        registerAllocator.freeRegister(l);
        registerAllocator.freeRegister(r);
    }

    public void generateUnaryOp(String patternName) {
        cod.compiler.MTOTRegistry.InstructionPattern pattern = compiler.cpuProfile.getPattern(patternName);
        if (pattern == null) { throw new RuntimeException("No pattern for unary op: " + patternName); }

        String operandReg = operandStack.popToRegister();
        String resultReg = registerAllocator.allocateRegister(Collections.singleton(operandReg));

        spiller.fillRegister(operandReg);

        for (String template : pattern.assemblyTemplate) {
            String asm = template.replace("{dest}", resultReg).replace("{src}", operandReg);
            compiler.assemblyCode.add("    " + asm);
        }

        operandStack.pushFromRegister(resultReg);
        spiller.markRegisterModified(resultReg);
        registerAllocator.freeRegister(operandReg);
    }
}