package cod.ast.nodes;

import cod.ast.ASTVisitor;

public class ExitNode extends StmtNode {

           @Override
        public final <T> T accept(ASTVisitor<T> visitor) {
           return visitor.visit(this);
        }
    

}