package cod.ast.nodes;

import cod.ast.VisitorImpl;
import cod.syntax.Keyword;

public class FieldNode extends StmtNode {
    public String name;
    public String type;
    public Keyword visibility;
    public ExprNode value;
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
    
}
