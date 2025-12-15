package cod.ast.nodes;

import cod.ast.ASTVisitor;

public class RangeNode extends ExprNode {
    public ExprNode step;    // NEW: Step comes first now
    public ExprNode start;   // NEW: Start after IN
    public ExprNode end;     // NEW: End after TO
    
    public RangeNode() {}
    
    // UPDATED: New parameter order
    public RangeNode(ExprNode step, ExprNode start, ExprNode end) {
        this.step = step;
        this.start = start;
        this.end = end;
    }
    
               @Override
        public final <T> T accept(ASTVisitor<T> visitor) {
           return visitor.visit(this);
        }
    
    
}