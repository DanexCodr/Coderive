package cod.ast.nodes;

import cod.ast.VisitorImpl;

public class StmtIfNode extends StmtNode {
    public ExprNode condition;
    public BlockNode thenBlock = new BlockNode();
    public BlockNode elseBlock = new BlockNode();

    public StmtIfNode() {}
    
    public StmtIfNode(ExprNode condition) {
        this.condition = condition;
    }
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}