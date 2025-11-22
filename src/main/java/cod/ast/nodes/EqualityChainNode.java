package cod.ast.nodes;

import java.util.*;

public class EqualityChainNode extends ExprNode {
    public ExprNode left;
    public String operator;
    public boolean isAllChain;
    public List<ExprNode> chainArguments;
    
    public EqualityChainNode() {
        this.chainArguments = new ArrayList<ExprNode>();
    }
}