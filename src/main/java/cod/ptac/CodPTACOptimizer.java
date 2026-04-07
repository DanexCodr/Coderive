package cod.ptac;

import cod.ptac.opt.*;

import java.util.ArrayList;
import java.util.List;

public final class CodPTACOptimizer {
    private final List<CodPTACOptimizationPass> passes;

    public CodPTACOptimizer() {
        this(false);
    }

    public CodPTACOptimizer(boolean enableOptionalLowering) {
        this.passes = new ArrayList<CodPTACOptimizationPass>();
        this.passes.add(new CodPTACPatternFusionPass());
        this.passes.add(new CodPTACLazyRangePropagationPass());
        this.passes.add(new CodPTACConstantFoldingPass());
        this.passes.add(new CodPTACDeadTempEliminationPass());
        this.passes.add(new CodPTACOptionalPatternLoweringPass(enableOptionalLowering));
    }

    public CodPTACUnit optimize(CodPTACUnit unit) {
        if (unit == null) return null;
        for (CodPTACOptimizationPass pass : passes) {
            pass.apply(unit);
        }
        return unit;
    }
}
