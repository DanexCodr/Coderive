package cod.ast.node;

import cod.ast.VisitorImpl;

public class SlotAssignment extends Stmt {
    public String slotName;
    public Expr value;
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}