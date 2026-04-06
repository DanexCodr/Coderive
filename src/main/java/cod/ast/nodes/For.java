package cod.ast.nodes;

import cod.ast.VisitorImpl;

public class For extends Stmt {
    public String iterator;
    public Range range;
    public Expr arraySource;
    public Block body = new Block();

    public For() {}

    public For(String iterator, Range range) {
        this.iterator = iterator;
        this.range = range;
    }
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
    
}
