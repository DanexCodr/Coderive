package cod.ast.nodes;

import cod.ast.VisitorImpl;

import java.util.ArrayList;
import java.util.List;

public class PolicyMethodNode extends ASTNode {
    public String methodName;
    public List<ParamNode> parameters = new ArrayList<ParamNode>();
    public List<SlotNode> returnSlots = new ArrayList<SlotNode>();
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}