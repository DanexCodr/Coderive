package cod.ast.nodes;

import cod.ast.VisitorImpl;

import java.util.*;

public class MethodCallNode extends ExprNode {
    public List<ExprNode> arguments = new ArrayList<>();
    public List<String> slotNames = new ArrayList<>();
    public List<String> argNames; // NEW: Store argument names
    public String qualifiedName;
    public boolean isConstructor = false;
    public boolean isSuperCall = false;
    
              @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}