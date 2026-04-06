package cod.ast.nodes;

import cod.ast.VisitorImpl;

/**
 * Simple wrapper to convert runtime values back to expression nodes
 */
public class ValueExpr extends Expr {
    private final Object value;
    
    public ValueExpr(Object value) {
        this.value = value;
    }
    
    public Object getValue() {
        return value;
    }
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return null; // Not meant for visitor pattern
    }
}