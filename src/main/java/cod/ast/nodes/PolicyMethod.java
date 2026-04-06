package cod.ast.nodes;

import cod.ast.VisitorImpl;

import java.util.ArrayList;
import java.util.List;

public class PolicyMethod extends Base {
    public String methodName;
    public List<Param> parameters = new ArrayList<Param>();
    public List<Slot> returnSlots = new ArrayList<Slot>();
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}