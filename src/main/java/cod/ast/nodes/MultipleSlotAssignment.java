package cod.ast.nodes;

import cod.ast.VisitorImpl;

import java.util.List;

public class MultipleSlotAssignment extends Stmt {
    public List<SlotAssignment> assignments;
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}