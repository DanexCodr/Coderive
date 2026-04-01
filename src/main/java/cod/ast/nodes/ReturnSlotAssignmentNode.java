package cod.ast.nodes;

import cod.ast.VisitorImpl;

import java.util.ArrayList;
import java.util.List;

public class ReturnSlotAssignmentNode extends StmtNode {
    public List<String> variableNames = new ArrayList<>();
    public MethodCallNode methodCall;
    public LambdaNode lambda; // NEW: Support for lambda expressions
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}