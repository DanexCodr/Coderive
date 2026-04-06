package cod.ast.node;

import cod.ast.VisitorImpl;

import java.util.ArrayList;
import java.util.List;

public class Use extends Base {
    public List<String> imports = new ArrayList<>();
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}