package cod.ptac;

import java.util.ArrayList;
import java.util.List;

public final class CodPTACFunction {
    public String name;
    public List<String> parameters = new ArrayList<String>();
    public List<CodPTACInstruction> instructions = new ArrayList<CodPTACInstruction>();
    public boolean lambdaBlock;
    public int closureLevel;
}
