package cod.interpreter;

import cod.ast.nodes.ExprNode;
import cod.ast.nodes.*;
import java.util.Map;

public class LoopFormula {
    public final long start;
    public final long end;  // inclusive
    public final ExprNode formula;
    public final String indexVar;
    
    public LoopFormula(long start, long end, ExprNode formula, String indexVar) {
        this.start = start;
        this.end = end;
        this.formula = formula;
        this.indexVar = indexVar;
    }
    
    public boolean contains(long index) {
        return index >= start && index <= end;
    }
}