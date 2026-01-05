package cod.ast.nodes;

import cod.ast.VisitorImpl;

public class ExitNode extends StmtNode {

           @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    

}