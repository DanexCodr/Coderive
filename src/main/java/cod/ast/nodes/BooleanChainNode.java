package cod.ast.nodes;

import cod.ast.ASTVisitor;
import java.util.*;

public class BooleanChainNode extends ExprNode {
    public boolean isAll;
    public List<ExprNode> expressions;

    public BooleanChainNode() {
        this.expressions = new ArrayList<ExprNode>();
    }
    
               @Override
        public final <T> T accept(ASTVisitor<T> visitor) {
           return visitor.visit(this);
        }
    
}