// RangeIndexNode.java
package cod.ast.nodes;

import cod.ast.VisitorImpl;

public class RangeIndexNode extends ExprNode {
    public ExprNode step;
    public ExprNode start;
    public ExprNode end;
    
    public RangeIndexNode(ExprNode step, ExprNode start, ExprNode end) {
        this.step = step;
        this.start = start;
        this.end = end;
    }
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}