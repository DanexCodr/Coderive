package cod.ast.nodes;

import cod.ast.VisitorImpl;

public abstract class ExprNode extends StmtNode {

    public ExprNode() {}
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}