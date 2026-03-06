package cod.ast.nodes;

import cod.ast.VisitorImpl;

public class IdentifierNode extends ExprNode {
    public final String name;
    
    public IdentifierNode(String name) {
        this.name = name;
    }
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}