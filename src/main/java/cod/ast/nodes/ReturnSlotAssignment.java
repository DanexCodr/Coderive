package cod.ast.nodes;

import cod.ast.VisitorImpl;

import java.util.ArrayList;
import java.util.List;

public class ReturnSlotAssignment extends Stmt {
    public List<String> variableNames = new ArrayList<>();
    public MethodCall methodCall;
    public Lambda lambda; // NEW: Support for lambda expressions
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}