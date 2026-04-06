package cod.ast.node;

import cod.ast.VisitorImpl;

public class IndexAccess extends Expr {
    public Expr array;
    public Expr index;
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}