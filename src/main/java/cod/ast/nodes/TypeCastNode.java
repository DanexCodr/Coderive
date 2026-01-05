package cod.ast.nodes;

import cod.ast.VisitorImpl;

public class TypeCastNode extends ExprNode {
    public String targetType;    // The target type to cast to
    public ExprNode expression;  // The expression being cast
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}