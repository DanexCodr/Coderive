package cod.ast.nodes;

import cod.ast.VisitorImpl;

import java.util.ArrayList;
import java.util.List;

public class Block extends Stmt {
    public List<Stmt> statements = new ArrayList<>();

    public Block() {}

    public Block(List<Stmt> statements) {
        this.statements = statements;
    }
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}
