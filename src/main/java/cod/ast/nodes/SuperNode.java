package cod.ast.nodes;

import cod.ast.VisitorImpl;

public class SuperNode extends ExprNode {
    
    public SuperNode() {}
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}