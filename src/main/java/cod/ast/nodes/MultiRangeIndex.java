// MultiRangeIndex.java
package cod.ast.nodes;

import cod.ast.VisitorImpl;
import java.util.List;

public class MultiRangeIndex extends Expr {
    public List<RangeIndex> ranges;
    
    public MultiRangeIndex(List<RangeIndex> ranges) {
        this.ranges = ranges;
    }
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}