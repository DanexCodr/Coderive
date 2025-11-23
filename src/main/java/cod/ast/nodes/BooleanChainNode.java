package cod.ast.nodes;

import java.util.*;

public class BooleanChainNode extends ExprNode {
    public boolean isAll;
    public List<ExprNode> expressions;

    public BooleanChainNode() {
        this.expressions = new ArrayList<ExprNode>();
    }
}