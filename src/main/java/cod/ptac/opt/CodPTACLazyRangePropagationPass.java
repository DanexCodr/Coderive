package cod.ptac.opt;

import cod.ptac.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class CodPTACLazyRangePropagationPass implements CodPTACOptimizationPass {
    @Override
    public void apply(CodPTACUnit unit) {
        if (unit == null || unit.functions == null) return;

        for (CodPTACFunction function : unit.functions) {
            if (function == null || function.instructions == null) continue;
            List<CodPTACInstruction> rewritten = new ArrayList<CodPTACInstruction>();

            for (CodPTACInstruction inst : function.instructions) {
                rewritten.add(markLazyIfNeeded(inst));
            }

            function.instructions = rewritten;
        }
    }

    private CodPTACInstruction markLazyIfNeeded(CodPTACInstruction inst) {
        if (inst == null) return null;
        if (inst.opcode != CodPTACOpcode.RANGE
            && inst.opcode != CodPTACOpcode.RANGE_Q
            && inst.opcode != CodPTACOpcode.RANGE_S
            && inst.opcode != CodPTACOpcode.RANGE_L
            && inst.opcode != CodPTACOpcode.RANGE_LS) {
            return inst;
        }
        EnumSet<CodPTACFlag> flags = inst.flags != null
            ? EnumSet.copyOf(inst.flags)
            : EnumSet.noneOf(CodPTACFlag.class);
        flags.add(CodPTACFlag.LAZY);
        return new CodPTACInstruction(inst.opcode, inst.dest, inst.operands, flags);
    }
}
