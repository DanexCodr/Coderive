package cod.ast.nodes;

import cod.ast.VisitorImpl;

public class NoneLiteral extends Expr {
    
    public NoneLiteral() {}
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}