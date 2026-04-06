package cod.ast.node;

import cod.ast.VisitorImpl;
import cod.syntax.Keyword;

public class Field extends Stmt {
    public String name;
    public String type;
    public Keyword visibility;
    public Expr value;
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
    
}
