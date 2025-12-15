// ArgumentListNode.java
package cod.ast.nodes;

import cod.ast.ASTVisitor;

import java.util.List;

public class ArgumentListNode extends ExprNode {
    public List<ExprNode> arguments;
    
              @Override
        public final <T> T accept(ASTVisitor<T> visitor) {
           return visitor.visit(this);
        }
    
}