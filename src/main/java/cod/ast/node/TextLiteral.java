package cod.ast.node;

import cod.ast.VisitorImpl;

public class TextLiteral extends Expr {
    public final String value;
    public final boolean isInterpolated;
    
    public TextLiteral(String value) {
        this.value = value;
        this.isInterpolated = false;
    }
    
    public TextLiteral(String value, boolean isInterpolated) {
        this.value = value;
        this.isInterpolated = isInterpolated;
    }
    
    @Override
    public String toString() {
        return value;
    }
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}