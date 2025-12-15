package cod.ast.nodes;

import cod.ast.ASTVisitor;

import java.util.List;
import java.util.ArrayList;

public class ConstructorNode extends ASTNode {
    public List<ParamNode> parameters = new ArrayList<ParamNode>();
    public List<StmtNode> body = new ArrayList<StmtNode>();
    
        @Override
        public final <T> T accept(ASTVisitor<T> visitor) {
           return visitor.visit(this);
        }
     
}