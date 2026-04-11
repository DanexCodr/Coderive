package cod.ptac;

import java.util.ArrayList;
import java.util.List;

public final class Function {
    public String name;
    public List<String> parameters = new ArrayList<String>();
    public List<Instruction> instructions = new ArrayList<Instruction>();
    public boolean lambdaBlock;
    public int closureLevel;
}
