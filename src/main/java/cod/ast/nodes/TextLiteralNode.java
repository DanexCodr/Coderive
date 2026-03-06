package cod.ast.nodes;

import cod.ast.VisitorImpl;

public class TextLiteralNode extends ExprNode {
    public final String value;
    public final boolean isInterpolated;
    
    public TextLiteralNode(String value) {
        this.value = value;
        this.isInterpolated = false;
    }
    
    public TextLiteralNode(String value, boolean isInterpolated) {
        this.value = value;
        this.isInterpolated = isInterpolated;
    }
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}