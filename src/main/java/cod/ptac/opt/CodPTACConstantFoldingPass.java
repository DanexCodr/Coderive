package cod.ptac.opt;

import cod.ptac.*;

import java.util.ArrayList;
import java.util.List;

public final class CodPTACConstantFoldingPass implements CodPTACOptimizationPass {
    @Override
    public void apply(CodPTACUnit unit) {
        if (unit == null || unit.functions == null) return;

        for (CodPTACFunction function : unit.functions) {
            if (function == null || function.instructions == null) continue;
            List<CodPTACInstruction> rewritten = new ArrayList<CodPTACInstruction>();

            for (CodPTACInstruction inst : function.instructions) {
                rewritten.add(fold(inst));
            }
            function.instructions = rewritten;
        }
    }

    private CodPTACInstruction fold(CodPTACInstruction inst) {
        if (inst == null || inst.operands == null || inst.operands.size() != 2) return inst;

        if (!isFoldable(inst.opcode)) return inst;
        CodPTACOperand left = inst.operands.get(0);
        CodPTACOperand right = inst.operands.get(1);
        if (left.kind != CodPTACOperandKind.IMMEDIATE || right.kind != CodPTACOperandKind.IMMEDIATE) return inst;
        if (!(left.value instanceof Number) || !(right.value instanceof Number)) return inst;

        double a = ((Number) left.value).doubleValue();
        double b = ((Number) right.value).doubleValue();
        Object folded = compute(inst.opcode, a, b);
        if (folded == null) return inst;

        List<CodPTACOperand> operands = new ArrayList<CodPTACOperand>();
        operands.add(CodPTACOperand.immediate(folded));
        return new CodPTACInstruction(CodPTACOpcode.ASSIGN, inst.dest, operands, inst.flags);
    }

    private boolean isFoldable(CodPTACOpcode opcode) {
        return opcode == CodPTACOpcode.ADD
            || opcode == CodPTACOpcode.SUB
            || opcode == CodPTACOpcode.MUL
            || opcode == CodPTACOpcode.DIV
            || opcode == CodPTACOpcode.MOD;
    }

    private Object compute(CodPTACOpcode opcode, double a, double b) {
        if (opcode == CodPTACOpcode.ADD) return a + b;
        if (opcode == CodPTACOpcode.SUB) return a - b;
        if (opcode == CodPTACOpcode.MUL) return a * b;
        if (opcode == CodPTACOpcode.DIV) return b == 0.0d ? null : a / b;
        if (opcode == CodPTACOpcode.MOD) return b == 0.0d ? null : a % b;
        return null;
    }
}
