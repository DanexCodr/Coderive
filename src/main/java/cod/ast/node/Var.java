package cod.ast.node;

import cod.ast.VisitorImpl;

public class Var extends Stmt {
    public String name;
    public Expr value;
    public String explicitType; // <<< ADDED THIS FIELD


           @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}