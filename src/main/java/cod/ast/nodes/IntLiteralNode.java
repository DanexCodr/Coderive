package cod.ast.nodes;

import cod.ast.VisitorImpl;
import cod.math.AutoStackingNumber;

public class IntLiteralNode extends ExprNode {
    public final AutoStackingNumber value;
    
    public IntLiteralNode(long value) {
        this.value = AutoStackingNumber.fromLong(value);
    }
    
    public IntLiteralNode(AutoStackingNumber value) {
        this.value = value;
    }
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}