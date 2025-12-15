package cod.ast.nodes;

import cod.ast.ASTVisitor;

public class ForNode extends StmtNode {
    public String iterator;
    public RangeNode range;
    public BlockNode body = new BlockNode();

    public ForNode() {}

    public ForNode(String iterator, RangeNode range) {
        this.iterator = iterator;
        this.range = range;
    }
    
               @Override
        public final <T> T accept(ASTVisitor<T> visitor) {
           return visitor.visit(this);
        }
    
    
}
