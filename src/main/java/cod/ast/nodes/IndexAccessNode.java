package cod.ast.nodes;

import cod.ast.VisitorImpl;

public class IndexAccessNode extends ExprNode {
    public ExprNode array;
    public ExprNode index;
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}