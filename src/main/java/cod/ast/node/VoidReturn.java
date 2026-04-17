package cod.ast.node;

import cod.ast.VisitorImpl;

public class VoidReturn extends Stmt {

           @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    

}
