package cod.ptac.opt;

import cod.ptac.*;

import java.util.ArrayList;
import java.util.List;

public final class PatternLowering implements Optimization {
    private final boolean enabled;

    public PatternLowering(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void apply(Unit unit) {
        if (!enabled || unit == null || unit.functions == null) return;

        for (Function function : unit.functions) {
            if (function == null || function.instructions == null) continue;

            List<Instruction> rewritten = new ArrayList<Instruction>();
            for (Instruction inst : function.instructions) {
                if (inst == null) continue;
                if (isUnsupportedPattern(inst.opcode)) {
                    List<Operand> operands = new ArrayList<Operand>();
                    operands.add(Operand.function(inst.opcode.name()));
                    rewritten.add(new Instruction(Opcode.CALL, inst.dest, operands, inst.flags));
                } else {
                    rewritten.add(inst);
                }
            }
            function.instructions = rewritten;
        }
    }

    private boolean isUnsupportedPattern(Opcode opcode) {
        return opcode == Opcode.ZIP
            || opcode == Opcode.SCAN
            || opcode == Opcode.FORMULA_RECUR
            || opcode == Opcode.FORMULA_FUSE;
    }
}
