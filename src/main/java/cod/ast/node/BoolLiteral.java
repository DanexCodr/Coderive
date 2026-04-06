package cod.ast.node;

import cod.ast.VisitorImpl;

public class BoolLiteral extends Expr {
    public final boolean value;
    
    public BoolLiteral(boolean value) {
        this.value = value;
    }
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}