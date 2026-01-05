package cod.ast.nodes;

import cod.ast.VisitorImpl;

public class ForNode extends StmtNode {
    public String iterator;
    public RangeNode range;
    public ExprNode arraySource;
    public BlockNode body = new BlockNode();

    public ForNode() {}

    public ForNode(String iterator, RangeNode range) {
        this.iterator = iterator;
        this.range = range;
    }
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
    
}
