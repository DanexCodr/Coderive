package cod.ast.nodes;

import cod.ast.ASTVisitor;

public class TypeCastNode extends ExprNode {
    public String targetType;    // The target type to cast to
    public ExprNode expression;  // The expression being cast
    
               @Override
        public final <T> T accept(ASTVisitor<T> visitor) {
           return visitor.visit(this);
        }
    
}