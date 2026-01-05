package cod.ast.nodes;

import cod.ast.VisitorImpl;
import java.util.*;

public class BooleanChainNode extends ExprNode {
    public boolean isAll;
    public List<ExprNode> expressions;

    public BooleanChainNode() {
        this.expressions = new ArrayList<ExprNode>();
    }
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}