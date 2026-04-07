package cod.ptac;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class CodPTACFunction implements Serializable {
    private static final long serialVersionUID = 1L;

    public String name;
    public List<String> parameters = new ArrayList<String>();
    public List<CodPTACInstruction> instructions = new ArrayList<CodPTACInstruction>();
    public boolean lambdaBlock;
    public int closureLevel;
}
