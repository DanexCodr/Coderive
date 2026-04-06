package cod.ast.node;

import cod.ast.VisitorImpl;

public class Identifier extends Expr {
    public final String name;
    
    public Identifier(String name) {
        this.name = name;
    }
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}