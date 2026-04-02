package cod.ast.nodes;

import cod.ast.VisitorImpl;
import java.util.List;
import java.util.ArrayList;

public class LambdaNode extends ExprNode {
    public List<ParamNode> parameters;
    public List<SlotNode> returnSlots;
    public StmtNode body;
    public ExprNode expressionBody;
    
    public LambdaNode() {
        this.parameters = new ArrayList<ParamNode>();
        this.returnSlots = new ArrayList<SlotNode>();
    }
    
    public LambdaNode(List<ParamNode> parameters, List<SlotNode> returnSlots, StmtNode body) {
        this.parameters = parameters != null ? parameters : new ArrayList<ParamNode>();
        this.returnSlots = returnSlots != null ? returnSlots : new ArrayList<SlotNode>();
        this.body = body;
        this.expressionBody = null;
    }
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}
