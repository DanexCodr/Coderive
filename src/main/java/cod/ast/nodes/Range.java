package cod.ast.nodes;

import cod.ast.VisitorImpl;

public class Range extends Expr {
    public Expr step;    // NEW: Step comes first now
    public Expr start;   // NEW: Start after IN
    public Expr end;     // NEW: End after TO
    
    public Range() {}
    
    // UPDATED: New parameter order
    public Range(Expr step, Expr start, Expr end) {
        this.step = step;
        this.start = start;
        this.end = end;
    }
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
    
}