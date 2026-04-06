package cod.ast.nodes;

import cod.ast.VisitorImpl;

public class ExprIf extends Expr {
    public Expr condition;
    public Expr thenExpr;
    public Expr elseExpr;

    public ExprIf() {}
    
    public ExprIf(Expr condition, Expr thenExpr, Expr elseExpr) {
        this.condition = condition;
        this.thenExpr = thenExpr;
        this.elseExpr = elseExpr;
    }
    
    @Override
    public final <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}