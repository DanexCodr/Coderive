package cod.ast.nodes;

import cod.ast.ASTVisitor;

import java.util.ArrayList;
import java.util.List;

public class ReturnSlotAssignmentNode extends StmtNode {
    public List<String> variableNames = new ArrayList<>();
    public MethodCallNode methodCall;
    
               @Override
        public final <T> T accept(ASTVisitor<T> visitor) {
           return visitor.visit(this);
        }
    
}
