package cod.ptac.opt;

import cod.ptac.*;

import java.util.ArrayList;
import java.util.List;

public final class CodPTACPatternFusionPass implements CodPTACOptimizationPass {
    @Override
    public void apply(CodPTACUnit unit) {
        if (unit == null || unit.functions == null) return;

        for (CodPTACFunction function : unit.functions) {
            if (function == null || function.instructions == null) continue;

            List<CodPTACInstruction> rewritten = new ArrayList<CodPTACInstruction>();
            for (int i = 0; i < function.instructions.size(); i++) {
                CodPTACInstruction current = function.instructions.get(i);
                CodPTACInstruction next = i + 1 < function.instructions.size()
                    ? function.instructions.get(i + 1) : null;

                if (canFuseFilterMap(current, next)) {
                    List<CodPTACOperand> fusedOps = new ArrayList<CodPTACOperand>();
                    fusedOps.add(current.operands.get(0)); // source
                    fusedOps.add(current.operands.get(1)); // filter lambda
                    fusedOps.add(next.operands.get(1));    // map lambda
                    rewritten.add(new CodPTACInstruction(CodPTACOpcode.FILTER_MAP, next.dest, fusedOps, next.flags));
                    i++;
                    continue;
                }

                rewritten.add(current);
            }
            function.instructions = rewritten;
        }
    }

    private boolean canFuseFilterMap(CodPTACInstruction filter, CodPTACInstruction map) {
        if (filter == null || map == null) return false;
        if (filter.opcode != CodPTACOpcode.FILTER) return false;
        if (map.opcode != CodPTACOpcode.MAP) return false;
        if (filter.dest == null) return false;
        if (map.operands == null || map.operands.isEmpty()) return false;
        CodPTACOperand mapSource = map.operands.get(0);
        return mapSource.kind == CodPTACOperandKind.REGISTER && filter.dest.equals(mapSource.value);
    }
}
