package cod.ptac;

import cod.ast.node.Type;

public final class CodPTACCompiler {
    private final CodPTACLowerer lowerer;
    private final CodPTACOptimizer optimizer;

    public CodPTACCompiler() {
        this(false);
    }

    public CodPTACCompiler(boolean enableOptionalLowering) {
        this.lowerer = new CodPTACLowerer();
        this.optimizer = new CodPTACOptimizer(enableOptionalLowering);
    }

    public CodPTACArtifact compile(String unitName, Type type) {
        CodPTACArtifact artifact = new CodPTACArtifact();
        artifact.version = CodPTACArtifact.FORMAT_VERSION;
        artifact.unitName = unitName;
        artifact.className = type != null ? type.name : null;
        artifact.typeSnapshot = type;
        artifact.unit = optimizer.optimize(lowerer.lower(unitName, type));
        return artifact;
    }
}
