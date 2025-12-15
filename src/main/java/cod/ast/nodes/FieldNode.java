package cod.ast.nodes;

import cod.ast.ASTVisitor;
import cod.syntax.Keyword;

public class FieldNode extends StmtNode {
    public String name;
    public String type;
    public Keyword visibility; // [FIX] Added visibility property
    public ExprNode value;
    
               @Override
        public final <T> T accept(ASTVisitor<T> visitor) {
           return visitor.visit(this);
        }
    
    
}
