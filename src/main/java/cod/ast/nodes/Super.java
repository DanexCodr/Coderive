package cod.ast.nodes;

import cod.ast.VisitorImpl;

public class Super extends Expr {
    
    public Super() {}
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}