package cod.ast.node;

import cod.ast.VisitorImpl;
import java.util.*;

public class EqualityChain extends Expr {
    public Expr left;
    public String operator;
    public boolean isAllChain;
    public List<Expr> chainArguments;
    
    public EqualityChain() {
        this.chainArguments = new ArrayList<Expr>();
    }
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}