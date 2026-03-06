package cod.ast.nodes;

import cod.ast.VisitorImpl;

public class ThisNode extends ExprNode {
    public final String className; // null for simple "this", set for "ClassName.this"
    
    public ThisNode() {
        this.className = null;
    }
    
    public ThisNode(String className) {
        this.className = className;
    }
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}