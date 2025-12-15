package cod.ast.nodes;

import cod.ast.ASTVisitor;

public class ExprNode extends StmtNode {
    public String name; // For identifiers or method names
    public Object value; // For literals
    public String op; // Operator (+, -, *, /, slot_cast, etc.)
    public ExprNode left; // Left operand
    public ExprNode right; // Right operand
    public boolean isNull = false;

    public ExprNode() {}
    
            @Override
        public <T> T accept(ASTVisitor<T> visitor) {
           return visitor.visit(this);
        }
    
}