package cod.ptac.opt;

import cod.ptac.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class RangePropagation implements Optimization {
    @Override
    public void apply(Unit unit) {
        if (unit == null || unit.functions == null) return;

        for (Function function : unit.functions) {
            if (function == null || function.instructions == null) continue;
            List<Instruction> rewritten = new ArrayList<Instruction>();

            for (Instruction inst : function.instructions) {
                rewritten.add(markLazyIfNeeded(inst));
            }

            function.instructions = rewritten;
        }
    }

    private Instruction markLazyIfNeeded(Instruction inst) {
        if (inst == null) return null;
        if (inst.opcode != Opcode.RANGE
            && inst.opcode != Opcode.RANGE_Q
            && inst.opcode != Opcode.RANGE_S
            && inst.opcode != Opcode.RANGE_L
            && inst.opcode != Opcode.RANGE_LS) {
            return inst;
        }
        EnumSet<Flag> flags = inst.flags != null
            ? EnumSet.copyOf(inst.flags)
            : EnumSet.noneOf(Flag.class);
        flags.add(Flag.LAZY);
        return new Instruction(inst.opcode, inst.dest, inst.operands, flags);
    }
}
