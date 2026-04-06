// ArgumentList.java
package cod.ast.node;

import cod.ast.VisitorImpl;

import java.util.List;

public class ArgumentList extends Expr {
    public List<Expr> arguments;
    
              @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}