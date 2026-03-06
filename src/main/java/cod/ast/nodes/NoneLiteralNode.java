package cod.ast.nodes;

import cod.ast.VisitorImpl;

public class NoneLiteralNode extends ExprNode {
    
    public NoneLiteralNode() {}
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}