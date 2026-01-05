package cod.ast.nodes;

import cod.ast.VisitorImpl;
import java.util.List;

public class TupleNode extends ExprNode {
    public List<ExprNode> elements;

               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}