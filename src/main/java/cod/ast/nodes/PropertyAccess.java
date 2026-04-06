package cod.ast.nodes;

import cod.ast.VisitorImpl;
import cod.lexer.Token;

public class PropertyAccess extends Expr {
    public Expr left;
    public Expr right;
    public transient Token dotToken;
    
    public PropertyAccess() {}
    
    public PropertyAccess(Expr left, Expr right, Token dotToken) {
        this.left = left;
        this.right = right;
        this.dotToken = dotToken;
    }
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}