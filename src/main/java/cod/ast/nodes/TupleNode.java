package cod.ast.nodes;

import cod.ast.ASTVisitor;
import java.util.List;

public class TupleNode extends ExprNode {
    public List<ExprNode> elements;

               @Override
        public final <T> T accept(ASTVisitor<T> visitor) {
           return visitor.visit(this);
        }
    
}