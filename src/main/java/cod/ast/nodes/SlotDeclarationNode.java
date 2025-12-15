package cod.ast.nodes;

import cod.ast.ASTVisitor;

import java.util.ArrayList;
import java.util.List;

public class SlotDeclarationNode extends StmtNode {
    public List<String> slotNames = new ArrayList<>();
    
               @Override
        public final <T> T accept(ASTVisitor<T> visitor) {
           return visitor.visit(this);
        }
    
}