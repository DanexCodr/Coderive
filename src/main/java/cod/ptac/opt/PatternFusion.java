package cod.ptac.opt;

import cod.ptac.*;

import java.util.ArrayList;
import java.util.List;

public final class PatternFusion implements Optimization {
    @Override
    public void apply(Unit unit) {
        if (unit == null || unit.functions == null) return;

        for (Function function : unit.functions) {
            if (function == null || function.instructions == null) continue;

            List<Instruction> rewritten = new ArrayList<Instruction>();
            for (int i = 0; i < function.instructions.size(); i++) {
                Instruction current = function.instructions.get(i);
                Instruction next = i + 1 < function.instructions.size()
                    ? function.instructions.get(i + 1) : null;

                if (canFuseFilterMap(current, next)) {
                    List<Operand> fusedOps = new ArrayList<Operand>();
                    fusedOps.add(current.operands.get(0)); // source
                    fusedOps.add(current.operands.get(1)); // filter lambda
                    fusedOps.add(next.operands.get(1));    // map lambda
                    rewritten.add(new Instruction(Opcode.FILTER_MAP, next.dest, fusedOps, next.flags));
                    i++;
                    continue;
                }

                rewritten.add(current);
            }
            function.instructions = rewritten;
        }
    }

    private boolean canFuseFilterMap(Instruction filter, Instruction map) {
        if (filter == null || map == null) return false;
        if (filter.opcode != Opcode.FILTER) return false;
        if (map.opcode != Opcode.MAP) return false;
        if (filter.dest == null) return false;
        if (map.operands == null || map.operands.isEmpty()) return false;
        Operand mapSource = map.operands.get(0);
        return mapSource.kind == OperandKind.REGISTER && filter.dest.equals(mapSource.value);
    }
}
