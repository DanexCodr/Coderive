package cod.ast.nodes;

import cod.ast.ASTVisitor;

import java.util.*;

public class MethodCallNode extends ExprNode {
    public List<ExprNode> arguments = new ArrayList<>();
    public List<String> slotNames = new ArrayList<>();
    public String qualifiedName;
    
    public String chainType;
    public List<ExprNode> chainArguments;
   
              @Override
        public final <T> T accept(ASTVisitor<T> visitor) {
           return visitor.visit(this);
        }
    
}