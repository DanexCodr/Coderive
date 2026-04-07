package cod.ptac;

import cod.ast.node.Type;

import java.io.Serializable;

public final class CodPTACArtifact implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int FORMAT_VERSION = 1;

    public int version = FORMAT_VERSION;
    public String unitName;
    public String className;
    public CodPTACUnit unit;
    public Type typeSnapshot;

    public boolean hasExecutableUnit() {
        return unit != null && unit.functions != null && !unit.functions.isEmpty();
    }
}
