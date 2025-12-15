package cod.ast.nodes;

import cod.ast.ASTVisitor;

public class UnaryNode extends ExprNode {
    public String op;  // "+" or "-"
    public ExprNode operand;
    
    public UnaryNode() {}
    
    public UnaryNode(String op, ExprNode operand) {
        this.op = op;
        this.operand = operand;
    }
    
               @Override
        public final <T> T accept(ASTVisitor<T> visitor) {
           return visitor.visit(this);
        }
    
}