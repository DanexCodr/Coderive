package cod.ptac;

import cod.ast.node.Type;

public final class Compiler {
    private final Lowerer lowerer;
    private final Optimizer optimizer;

    public Compiler() {
        this(false);
    }

    public Compiler(boolean enableOptionalLowering) {
        this.lowerer = new Lowerer();
        this.optimizer = new Optimizer(enableOptionalLowering);
    }

    public Artifact compile(String unitName, Type type) {
        Artifact artifact = new Artifact();
        artifact.version = Artifact.FORMAT_VERSION;
        artifact.unitName = unitName;
        artifact.className = type != null ? type.name : null;
        artifact.typeSnapshot = type;
        artifact.unit = optimizer.optimize(lowerer.lower(unitName, type));
        return artifact;
    }
}
