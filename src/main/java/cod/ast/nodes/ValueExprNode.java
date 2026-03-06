package cod.ast.nodes;

import cod.ast.VisitorImpl;

/**
 * Simple wrapper to convert runtime values back to expression nodes
 */
public class ValueExprNode extends ExprNode {
    private final Object value;
    
    public ValueExprNode(Object value) {
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