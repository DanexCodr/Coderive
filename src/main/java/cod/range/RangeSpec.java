// RangeSpec.java
package cod.range;

public class RangeSpec {
    public final Object step;
    public final Object start;
    public final Object end;
    
    public RangeSpec(Object step, Object start, Object end) {
        this.step = step;
        this.start = start;
        this.end = end;
    }
    
    @Override
    public String toString() {
        return "RangeSpec{" + 
               (step != null ? "by " + step + " in " : "") + 
               start + " to " + end + "}";
    }
}