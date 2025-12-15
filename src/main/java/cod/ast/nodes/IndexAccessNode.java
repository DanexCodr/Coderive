package cod.ast.nodes;

import cod.ast.ASTVisitor;

public class IndexAccessNode extends ExprNode {
    public ExprNode array;
    public ExprNode index;
    
               @Override
        public final <T> T accept(ASTVisitor<T> visitor) {
           return visitor.visit(this);
        }
    
}