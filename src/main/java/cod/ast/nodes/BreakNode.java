package cod.ast.nodes;

import cod.ast.VisitorImpl;

/**
 * Represents a 'break' statement that exits the current loop.
 * Nothing can follow 'break' in the same statement.
 */
public class BreakNode extends StmtNode {
    
    // No fields needed - break is a simple statement
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
    
}