// RangeIndex.java
package cod.ast.nodes;

import cod.ast.VisitorImpl;

public class RangeIndex extends Expr {
    public Expr step;
    public Expr start;
    public Expr end;
    
    public RangeIndex(Expr step, Expr start, Expr end) {
        this.step = step;
        this.start = start;
        this.end = end;
    }
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}