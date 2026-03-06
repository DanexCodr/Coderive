package cod.ast.nodes;

import cod.ast.VisitorImpl;

public class BinaryOpNode extends ExprNode {
    public ExprNode left;
    public String op;
    public ExprNode right;
    
    public BinaryOpNode() {}
    
    public BinaryOpNode(ExprNode left, String op, ExprNode right) {
        this.left = left;
        this.op = op;
        this.right = right;
    }
    
    @Override
    public final <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}