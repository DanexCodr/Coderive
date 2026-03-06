package cod.ast.nodes;

import cod.ast.VisitorImpl;
import java.util.List;
import java.util.ArrayList;

public class MethodCallNode extends ExprNode {
    public String name;
    public String qualifiedName;
    public List<ExprNode> arguments;
    public List<String> slotNames;
    public List<String> argNames;
    public boolean isSuperCall;
    public boolean isGlobal;
    public ExprNode target;
    
    // NEW: Flag to indicate this method call should return a single slot value directly
    public boolean isSingleSlotCall;

    public MethodCallNode() {
        this.arguments = new ArrayList<ExprNode>();
        this.slotNames = new ArrayList<String>();
        this.argNames = new ArrayList<String>();
        this.isSuperCall = false;
        this.isGlobal = false;
        this.isSingleSlotCall = false;
    }
    
    @Override
    public final <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}