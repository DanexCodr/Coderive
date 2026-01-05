package cod.ast.nodes;

import cod.ast.VisitorImpl;

public class SlotAssignmentNode extends StmtNode {
    public String slotName;
    public ExprNode value;
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}