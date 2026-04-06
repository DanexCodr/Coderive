package cod.ast.node;

import cod.ast.VisitorImpl;
import java.util.List;

public class Tuple extends Expr {
    public List<Expr> elements;

               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}