// MultiRangeSpec.java
package cod.range;

import java.util.List;

public class MultiRangeSpec {
    public final List<RangeSpec> ranges;
    
    public MultiRangeSpec(List<RangeSpec> ranges) {
        this.ranges = ranges;
    }
    
    @Override
    public String toString() {
        return "MultiRangeSpec{ranges=" + ranges + "}";
    }
}