package cod.ast.nodes;

import cod.ast.VisitorImpl;

public class SkipNode extends StmtNode {

    // No fields needed - just a marker
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
    
}