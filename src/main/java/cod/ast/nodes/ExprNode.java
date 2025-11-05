package cod.ast.nodes;

public class ExprNode extends StatementNode {
    public String name; // For identifiers or method names
    public Object value; // For literals
    public String op; // Operator (+, -, *, /, slot_cast, etc.)
    public ExprNode left; // Left operand
    public ExprNode right; // Right operand

    public ExprNode() {}
    
    // Remove all ANTLR imports and references
}