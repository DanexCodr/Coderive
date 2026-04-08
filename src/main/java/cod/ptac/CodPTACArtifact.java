package cod.ptac;

import cod.ast.node.Type;


public final class CodPTACArtifact {
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
