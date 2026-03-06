package cod.ast.nodes;

import cod.ast.VisitorImpl;
import cod.math.AutoStackingNumber;

public class FloatLiteralNode extends ExprNode {
    public final AutoStackingNumber value;
    
    public FloatLiteralNode(AutoStackingNumber value) {
        this.value = value;
    }
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}