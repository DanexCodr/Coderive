// ArgumentListNode.java
package cod.ast.nodes;

import cod.ast.VisitorImpl;

import java.util.List;

public class ArgumentListNode extends ExprNode {
    public List<ExprNode> arguments;
    
              @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}