package cod.ast.nodes;

import cod.ast.VisitorImpl;

public class TypeCast extends Expr {
    public String targetType;    // The target type to cast to
    public Expr expression;  // The expression being cast
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}