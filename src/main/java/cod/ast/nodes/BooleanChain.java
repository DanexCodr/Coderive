package cod.ast.nodes;

import cod.ast.VisitorImpl;
import java.util.*;

public class BooleanChain extends Expr {
    public boolean isAll;
    public List<Expr> expressions;

    public BooleanChain() {
        this.expressions = new ArrayList<Expr>();
    }
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}