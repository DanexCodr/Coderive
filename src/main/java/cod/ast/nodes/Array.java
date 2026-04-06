package cod.ast.nodes;

import cod.ast.VisitorImpl;

import java.util.ArrayList;
import java.util.List;

public class Array extends Expr {
    public List<Expr> elements = new ArrayList<>();
    public String elementType; // int: [], text: [], etc.
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}
