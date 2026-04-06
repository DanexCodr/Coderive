package cod.ast.node;

import cod.ast.VisitorImpl;

public class Skip extends Stmt {

    // No fields needed - just a marker
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
    
}