package cod.ast.nodes;

import cod.ast.VisitorImpl;

public class BoolLiteralNode extends ExprNode {
    public final boolean value;
    
    public BoolLiteralNode(boolean value) {
        this.value = value;
    }
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}