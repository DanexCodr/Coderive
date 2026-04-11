package cod.ptac.opt;

import cod.ptac.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DeadTempElimination implements Optimization {
    @Override
    public void apply(Unit unit) {
        if (unit == null || unit.functions == null) return;

        for (Function function : unit.functions) {
            if (function == null || function.instructions == null) continue;
            Set<String> used = collectUsedRegisters(function.instructions);
            List<Instruction> rewritten = new ArrayList<Instruction>();
            for (Instruction inst : function.instructions) {
                if (canDrop(inst, used)) continue;
                rewritten.add(inst);
            }
            function.instructions = rewritten;
        }
    }

    private Set<String> collectUsedRegisters(List<Instruction> instructions) {
        Set<String> used = new HashSet<String>();
        for (Instruction inst : instructions) {
            if (inst == null || inst.operands == null) continue;
            for (Operand operand : inst.operands) {
                if (operand != null && operand.kind == OperandKind.REGISTER && operand.value instanceof String) {
                    used.add((String) operand.value);
                }
            }
        }
        return used;
    }

    private boolean canDrop(Instruction inst, Set<String> used) {
        if (inst == null || inst.dest == null) return false;
        if (!inst.dest.startsWith("t")) return false;
        if (hasSideEffect(inst.opcode)) return false;
        return !used.contains(inst.dest);
    }

    private boolean hasSideEffect(Opcode opcode) {
        return opcode == Opcode.STORE
            || opcode == Opcode.CALL
            || opcode == Opcode.SLOT_SET
            || opcode == Opcode.LAZY_SET
            || opcode == Opcode.LAZY_COMMIT;
    }
}
