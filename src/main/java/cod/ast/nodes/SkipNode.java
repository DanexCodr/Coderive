package cod.ast.nodes;

import cod.ast.ASTVisitor;

public class SkipNode extends StmtNode {

    // No fields needed - just a marker
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
}