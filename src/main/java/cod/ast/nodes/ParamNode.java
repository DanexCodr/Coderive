package cod.ast.nodes;

import cod.ast.VisitorImpl;

public class ParamNode extends ASTNode {
    public String name;
    public String type;
    public ExprNode defaultValue;
    public boolean hasDefaultValue = false;
    public boolean typeInferred = false;
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}