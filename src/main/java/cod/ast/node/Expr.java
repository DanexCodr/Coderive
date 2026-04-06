package cod.ast.node;

import cod.ast.VisitorImpl;

public abstract class Expr extends Stmt {

    public Expr() {}
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}