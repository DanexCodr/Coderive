package cod.ast.nodes;

import cod.ast.VisitorImpl;

/**
 * Represents an assignment operation, such as 'x = 5' or 'arr[0] = 10'.
 */
public class AssignmentNode extends StmtNode {
    public ExprNode left;  // The target of the assignment (identifier, index access, etc.)
    public ExprNode right; // The value being assigned
    public boolean isDeclaration; // NEW: true for :=, false for = (default)
    
    public AssignmentNode() {
        this.isDeclaration = false; // Default to assignment
    }
    
    public AssignmentNode(ExprNode left, ExprNode right) {
        this.left = left;
        this.right = right;
        this.isDeclaration = false; // Default to assignment
    }
    
    @Override
    public final <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}