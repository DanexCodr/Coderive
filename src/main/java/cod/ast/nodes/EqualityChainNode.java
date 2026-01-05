package cod.ast.nodes;

import cod.ast.VisitorImpl;
import java.util.*;

public class EqualityChainNode extends ExprNode {
    public ExprNode left;
    public String operator;
    public boolean isAllChain;
    public List<ExprNode> chainArguments;
    
    public EqualityChainNode() {
        this.chainArguments = new ArrayList<ExprNode>();
    }
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}