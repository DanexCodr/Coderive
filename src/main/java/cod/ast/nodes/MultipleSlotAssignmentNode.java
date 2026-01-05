package cod.ast.nodes;

import cod.ast.VisitorImpl;

import java.util.List;

public class MultipleSlotAssignmentNode extends StmtNode {
    public List<SlotAssignmentNode> assignments;
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}