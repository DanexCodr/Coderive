package cod.ptac;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class CodPTACUnit implements Serializable {
    private static final long serialVersionUID = 1L;

    public String unitName;
    public String className;
    public String entryFunction;
    public List<CodPTACFunction> functions = new ArrayList<CodPTACFunction>();
}
