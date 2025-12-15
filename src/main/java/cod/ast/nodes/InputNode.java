package cod.ast.nodes;

import cod.ast.ASTVisitor;

public class InputNode extends ExprNode {
    public String targetType;    // The type being read (int, string, float, bool)
    public String variableName;  // The variable to assign the input to
    
               @Override
        public final <T> T accept(ASTVisitor<T> visitor) {
           return visitor.visit(this);
        }
    
}