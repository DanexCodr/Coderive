package cod.ast.node;

import cod.ast.VisitorImpl;

public class This extends Expr {
    public final String className; // null for simple "this", set for "ClassName.this"
    
    public This() {
        this.className = null;
    }
    
    public This(String className) {
        this.className = className;
    }
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}