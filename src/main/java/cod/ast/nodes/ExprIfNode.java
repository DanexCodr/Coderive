package cod.ast.nodes;

import cod.ast.ASTVisitor;

public class ExprIfNode extends ExprNode {
    public ExprNode condition;
    public ExprNode thenExpr;
    public ExprNode elseExpr;

    public ExprIfNode() {}
    
    public ExprIfNode(ExprNode condition, ExprNode thenExpr, ExprNode elseExpr) {
        this.condition = condition;
        this.thenExpr = thenExpr;
        this.elseExpr = elseExpr;
    }
    
    @Override
    public final <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}