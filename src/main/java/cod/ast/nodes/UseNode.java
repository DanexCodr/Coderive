package cod.ast.nodes;

import cod.ast.ASTVisitor;

import java.util.ArrayList;
import java.util.List;

public class UseNode extends ASTNode {
    public List<String> imports = new ArrayList<>();
    
               @Override
        public final <T> T accept(ASTVisitor<T> visitor) {
           return visitor.visit(this);
        }
    
}