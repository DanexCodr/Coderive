package cod.ast.nodes;

import cod.ast.ASTVisitor;

import java.util.List;

public class MultipleSlotAssignmentNode extends StmtNode {
    public List<SlotAssignmentNode> assignments;
    
               @Override
        public final <T> T accept(ASTVisitor<T> visitor) {
           return visitor.visit(this);
        }
    
}