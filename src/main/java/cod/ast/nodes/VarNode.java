package cod.ast.nodes;

import cod.ast.ASTVisitor;

public class VarNode extends StmtNode {
    public String name;
    public ExprNode value;
    public String explicitType; // <<< ADDED THIS FIELD


           @Override
        public final <T> T accept(ASTVisitor<T> visitor) {
           return visitor.visit(this);
        }
    
}