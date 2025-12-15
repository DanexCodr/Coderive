package cod.ast.nodes;

import cod.ast.ASTVisitor;

public class SlotAssignmentNode extends StmtNode {
    public String slotName;
    public ExprNode value;
    
               @Override
        public final <T> T accept(ASTVisitor<T> visitor) {
           return visitor.visit(this);
        }
    
}