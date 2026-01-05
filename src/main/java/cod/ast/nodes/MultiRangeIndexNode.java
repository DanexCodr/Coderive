// MultiRangeIndexNode.java
package cod.ast.nodes;

import cod.ast.VisitorImpl;
import java.util.List;

public class MultiRangeIndexNode extends ExprNode {
    public List<RangeIndexNode> ranges;
    
    public MultiRangeIndexNode(List<RangeIndexNode> ranges) {
        this.ranges = ranges;
    }
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}