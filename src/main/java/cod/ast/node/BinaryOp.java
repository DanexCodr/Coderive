package cod.ast.node;

import cod.ast.VisitorImpl;

public class BinaryOp extends Expr {
    public Expr left;
    public String op;
    public Expr right;
    
    public BinaryOp() {}
    
    public BinaryOp(Expr left, String op, Expr right) {
        this.left = left;
        this.op = op;
        this.right = right;
    }
    
    @Override
    public final <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}