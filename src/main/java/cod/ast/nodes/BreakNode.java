package cod.ast.nodes;

import cod.ast.ASTVisitor;

/**
 * Represents a 'break' statement that exits the current loop.
 * Nothing can follow 'break' in the same statement.
 */
public class BreakNode extends StmtNode {
    
    // No fields needed - break is a simple statement
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
}