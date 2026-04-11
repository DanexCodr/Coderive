package cod.ptac;

import cod.ptac.opt.*;

import java.util.ArrayList;
import java.util.List;

public final class Optimizer {
    private final List<Optimization> passes;

    public Optimizer() {
        this(false);
    }

    public Optimizer(boolean enableOptionalLowering) {
        this.passes = new ArrayList<Optimization>();
        this.passes.add(new PatternFusion());
        this.passes.add(new RangePropagation());
        this.passes.add(new ConstantFolding());
        this.passes.add(new DeadTempElimination());
        this.passes.add(new PatternLowering(enableOptionalLowering));
    }

    public Unit optimize(Unit unit) {
        if (unit == null) return null;
        for (Optimization pass : passes) {
            pass.apply(unit);
        }
        return unit;
    }
}
