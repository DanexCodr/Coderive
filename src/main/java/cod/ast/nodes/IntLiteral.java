package cod.ast.nodes;

import cod.ast.VisitorImpl;
import cod.math.AutoStackingNumber;

public class IntLiteral extends Expr {
    public final AutoStackingNumber value;
    
    public IntLiteral(long value) {
        this.value = AutoStackingNumber.fromLong(value);
    }
    
    public IntLiteral(AutoStackingNumber value) {
        this.value = value;
    }
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}