package cod.ast.nodes;

import cod.ast.VisitorImpl;

import java.util.ArrayList;
import java.util.List;

public class UseNode extends ASTNode {
    public List<String> imports = new ArrayList<>();
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}