package cod.ptac.opt;

import cod.ptac.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CodPTACDeadTempEliminationPass implements CodPTACOptimizationPass {
    @Override
    public void apply(CodPTACUnit unit) {
        if (unit == null || unit.functions == null) return;

        for (CodPTACFunction function : unit.functions) {
            if (function == null || function.instructions == null) continue;
            Set<String> used = collectUsedRegisters(function.instructions);
            List<CodPTACInstruction> rewritten = new ArrayList<CodPTACInstruction>();
            for (CodPTACInstruction inst : function.instructions) {
                if (canDrop(inst, used)) continue;
                rewritten.add(inst);
            }
            function.instructions = rewritten;
        }
    }

    private Set<String> collectUsedRegisters(List<CodPTACInstruction> instructions) {
        Set<String> used = new HashSet<String>();
        for (CodPTACInstruction inst : instructions) {
            if (inst == null || inst.operands == null) continue;
            for (CodPTACOperand operand : inst.operands) {
                if (operand != null && operand.kind == CodPTACOperandKind.REGISTER && operand.value instanceof String) {
                    used.add((String) operand.value);
                }
            }
        }
        return used;
    }

    private boolean canDrop(CodPTACInstruction inst, Set<String> used) {
        if (inst == null || inst.dest == null) return false;
        if (!inst.dest.startsWith("t")) return false;
        if (hasSideEffect(inst.opcode)) return false;
        return !used.contains(inst.dest);
    }

    private boolean hasSideEffect(CodPTACOpcode opcode) {
        return opcode == CodPTACOpcode.STORE
            || opcode == CodPTACOpcode.CALL
            || opcode == CodPTACOpcode.SLOT_SET
            || opcode == CodPTACOpcode.LAZY_SET
            || opcode == CodPTACOpcode.LAZY_COMMIT;
    }
}
