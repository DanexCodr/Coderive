package cod.interpreter;

import cod.ast.nodes.ExprNode;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class ConditionalFormula {
    public final long start;
    public final long end;
    public final String indexVar;
    public final ExprNode condition;
    public final ExprNode thenExpr;
    public final ExprNode elseExpr;
    
    public ConditionalFormula(long start, long end, String indexVar, 
                            ExprNode condition, ExprNode thenExpr, ExprNode elseExpr) {
        this.start = start;
        this.end = end;
        this.indexVar = indexVar;
        this.condition = condition;
        this.thenExpr = thenExpr;
        this.elseExpr = elseExpr;
    }
    
    public boolean contains(long index) {
        return index >= start && index <= end;
    }
    
    public Object evaluate(long index, InterpreterVisitor visitor) {
    
    try {
        // Create evaluation context with index variable
        ExecutionContext currentCtx = visitor.getCurrentContext();
        Map<String, Object> mergedLocals = new HashMap<>(currentCtx.locals);
        mergedLocals.put(indexVar, index);
        
        ExecutionContext tempCtx = new ExecutionContext(
            currentCtx.objectInstance,
            mergedLocals,
            currentCtx.slotValues,
            currentCtx.slotTypes,
            currentCtx.currentClass
        );
        
        visitor.pushContext(tempCtx);
        try {
            // Evaluate condition
            Object condResult = condition.accept(visitor);
            
            boolean isTruthy = isTruthy(condResult);
            
            // Evaluate appropriate branch
            Object result;
            if (isTruthy) {
                result = thenExpr.accept(visitor);
            } else {
                result = elseExpr.accept(visitor);
            }
            return result;
        } finally {
            visitor.popContext();
        }
    } catch (Exception e) {
        e.printStackTrace();
        return null;
    }
}
    
    private boolean isTruthy(Object value) {
    
    if (value == null) return false;
    if (value instanceof Boolean) return (Boolean) value;
    if (value instanceof Number) {
        if (value instanceof BigDecimal) {
            BigDecimal bd = (BigDecimal) value;
            return bd.compareTo(BigDecimal.ZERO) != 0;
        }
        double d = ((Number) value).doubleValue();
        return d != 0.0;
    }
    if (value instanceof String) {
        String s = (String) value;
        return !s.isEmpty() && !s.equalsIgnoreCase("false");
    }
    return true;
}
    
    @Override
    public String toString() {
        return String.format("ConditionalFormula[%d to %d]: if (%s) then %s else %s", 
            start, end, condition, thenExpr, elseExpr);
    }
}