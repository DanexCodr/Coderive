package cod.ast.node;

import cod.ast.VisitorImpl;

public class Unary extends Expr {
    public String op;  // "+" or "-"
    public Expr operand;
    
    public Unary() {}
    
    public Unary(String op, Expr operand) {
        this.op = op;
        this.operand = operand;
    }
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}