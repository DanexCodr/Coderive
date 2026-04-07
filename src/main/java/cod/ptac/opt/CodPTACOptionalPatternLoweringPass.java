package cod.ptac.opt;

import cod.ptac.*;

import java.util.ArrayList;
import java.util.List;

public final class CodPTACOptionalPatternLoweringPass implements CodPTACOptimizationPass {
    private final boolean enabled;

    public CodPTACOptionalPatternLoweringPass(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void apply(CodPTACUnit unit) {
        if (!enabled || unit == null || unit.functions == null) return;

        for (CodPTACFunction function : unit.functions) {
            if (function == null || function.instructions == null) continue;

            List<CodPTACInstruction> rewritten = new ArrayList<CodPTACInstruction>();
            for (CodPTACInstruction inst : function.instructions) {
                if (inst == null) continue;
                if (isUnsupportedPattern(inst.opcode)) {
                    List<CodPTACOperand> operands = new ArrayList<CodPTACOperand>();
                    operands.add(CodPTACOperand.function(inst.opcode.name()));
                    rewritten.add(new CodPTACInstruction(CodPTACOpcode.CALL, inst.dest, operands, inst.flags));
                } else {
                    rewritten.add(inst);
                }
            }
            function.instructions = rewritten;
        }
    }

    private boolean isUnsupportedPattern(CodPTACOpcode opcode) {
        return opcode == CodPTACOpcode.ZIP
            || opcode == CodPTACOpcode.SCAN
            || opcode == CodPTACOpcode.FORMULA_RECUR
            || opcode == CodPTACOpcode.FORMULA_FUSE;
    }
}
