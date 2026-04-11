package cod.ptac.opt;

import cod.ptac.*;

import java.util.ArrayList;
import java.util.List;

public final class ConstantFolding implements Optimization {
    @Override
    public void apply(Unit unit) {
        if (unit == null || unit.functions == null) return;

        for (Function function : unit.functions) {
            if (function == null || function.instructions == null) continue;
            List<Instruction> rewritten = new ArrayList<Instruction>();

            for (Instruction inst : function.instructions) {
                rewritten.add(fold(inst));
            }
            function.instructions = rewritten;
        }
    }

    private Instruction fold(Instruction inst) {
        if (inst == null || inst.operands == null || inst.operands.size() != 2) return inst;

        if (!isFoldable(inst.opcode)) return inst;
        Operand left = inst.operands.get(0);
        Operand right = inst.operands.get(1);
        if (left.kind != OperandKind.IMMEDIATE || right.kind != OperandKind.IMMEDIATE) return inst;
        if (!(left.value instanceof Number) || !(right.value instanceof Number)) return inst;

        double a = ((Number) left.value).doubleValue();
        double b = ((Number) right.value).doubleValue();
        Object folded = compute(inst.opcode, a, b);
        if (folded == null) return inst;

        List<Operand> operands = new ArrayList<Operand>();
        operands.add(Operand.immediate(folded));
        return new Instruction(Opcode.ASSIGN, inst.dest, operands, inst.flags);
    }

    private boolean isFoldable(Opcode opcode) {
        return opcode == Opcode.ADD
            || opcode == Opcode.SUB
            || opcode == Opcode.MUL
            || opcode == Opcode.DIV
            || opcode == Opcode.MOD;
    }

    private Object compute(Opcode opcode, double a, double b) {
        if (opcode == Opcode.ADD) return a + b;
        if (opcode == Opcode.SUB) return a - b;
        if (opcode == Opcode.MUL) return a * b;
        if (opcode == Opcode.DIV) return b == 0.0d ? null : a / b;
        if (opcode == Opcode.MOD) return b == 0.0d ? null : a % b;
        return null;
    }
}
