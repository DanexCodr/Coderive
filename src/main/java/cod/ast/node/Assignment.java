package cod.ast.node;

import cod.ast.VisitorImpl;

/**
 * Represents an assignment operation, such as 'x = 5' or 'arr[0] = 10'.
 */
public class Assignment extends Stmt {
    public Expr left;  // The target of the assignment (identifier, index access, etc.)
    public Expr right; // The value being assigned
    public boolean isDeclaration; // true for :=, false for = (default)
    
    public Assignment() {
        this.isDeclaration = false; // Default to assignment
    }
    
    public Assignment(Expr left, Expr right) {
        this.left = left;
        this.right = right;
        this.isDeclaration = false; // Default to assignment
    }
    
    @Override
    public final <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}