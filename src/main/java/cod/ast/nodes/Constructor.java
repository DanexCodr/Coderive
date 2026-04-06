package cod.ast.nodes;

import cod.ast.VisitorImpl;

import java.util.List;
import java.util.ArrayList;

public class Constructor extends Base {
    public List<Param> parameters = new ArrayList<Param>();
    public List<Stmt> body = new ArrayList<Stmt>();
    
        @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
     
}