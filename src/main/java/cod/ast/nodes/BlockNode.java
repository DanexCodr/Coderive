package cod.ast.nodes;

import cod.ast.ASTVisitor;

import java.util.ArrayList;
import java.util.List;

public class BlockNode extends StmtNode {
    public List<StmtNode> statements = new ArrayList<>();

    public BlockNode() {}

    public BlockNode(List<StmtNode> statements) {
        this.statements = statements;
    }
    
               @Override
        public final <T> T accept(ASTVisitor<T> visitor) {
           return visitor.visit(this);
        }
    
}
