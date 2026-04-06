package cod.ast.node;

import cod.ast.VisitorImpl;
import cod.math.AutoStackingNumber;

public class FloatLiteral extends Expr {
    public final AutoStackingNumber value;
    
    public FloatLiteral(AutoStackingNumber value) {
        this.value = value;
    }
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}