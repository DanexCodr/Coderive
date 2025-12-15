package cod.ast.nodes;

import cod.ast.ASTVisitor;

/**
 * Represents an assignment operation, such as 'x = 5' or 'arr[0] = 10'.
 */
public class AssignmentNode extends StmtNode {
    public ExprNode left;  // The target of the assignment (identifier, index access, etc.)
    public ExprNode right; // The value being assigned
    
    public AssignmentNode() {}
    
    public AssignmentNode(ExprNode left, ExprNode right) {
        this.left = left;
        this.right = right;
    }
    
               @Override
        public final <T> T accept(ASTVisitor<T> visitor) {
           return visitor.visit(this);
        }
    
}