package cod.ast.nodes;

import cod.ast.VisitorImpl;
import cod.lexer.Token;

public class PropertyAccessNode extends ExprNode {
    public ExprNode left;      // The object/lambda
    public ExprNode right;     // The property/body
    public Token dotToken;     // For span info
    
    public PropertyAccessNode() {}
    
    public PropertyAccessNode(ExprNode left, ExprNode right, Token dotToken) {
        this.left = left;
        this.right = right;
        this.dotToken = dotToken;
    }
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}