package cod.range.formula;

import cod.ast.nodes.ExprNode;
import cod.interpreter.InterpreterVisitor;
import cod.interpreter.context.ExecutionContext;

import java.util.*;

public class MultiBranchFormula {
    public final long start;
    public final long end;
    public final String indexVar;
    public final ExprNode firstCondition;
    public final ExprNode firstThenExpr;
    public final List<ExprNode> elifConditions;
    public final List<ExprNode> elifExpressions;
    public final ExprNode elseExpr;
    
    public MultiBranchFormula(long start, long end, String indexVar,
                            ExprNode firstCondition, ExprNode firstThenExpr,
                            List<ExprNode> elifConditions, List<ExprNode> elifExpressions,
                            ExprNode elseExpr) {
        this.start = start;
        this.end = end;
        this.indexVar = indexVar;
        this.firstCondition = firstCondition;
        this.firstThenExpr = firstThenExpr;
        this.elifConditions = elifConditions != null ? elifConditions : new ArrayList<ExprNode>();
        this.elifExpressions = elifExpressions != null ? elifExpressions : new ArrayList<ExprNode>();
        this.elseExpr = elseExpr;
    }
    
    public boolean contains(long index) {
        return index >= start && index <= end;
    }
    
    public Object evaluate(long index, InterpreterVisitor visitor) {
        try {
            ExecutionContext currentCtx = visitor.getCurrentContext();
            
            // Create evaluation context with index variable
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
                // Evaluate first condition
                Object condResult = firstCondition.accept(visitor);
                if (isTruthy(condResult)) {
                    return firstThenExpr.accept(visitor);
                }
                
                // Evaluate elif conditions
                for (int i = 0; i < elifConditions.size(); i++) {
                    condResult = elifConditions.get(i).accept(visitor);
                    if (isTruthy(condResult)) {
                        return elifExpressions.get(i).accept(visitor);
                    }
                }
                
                // Else case
                return elseExpr.accept(visitor);
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
            if (value instanceof java.math.BigDecimal) {
                return ((java.math.BigDecimal) value).compareTo(java.math.BigDecimal.ZERO) != 0;
            }
            return ((Number) value).doubleValue() != 0.0;
        }
        if (value instanceof String) {
            String s = (String) value;
            return !s.isEmpty() && !s.equalsIgnoreCase("false");
        }
        return true;
    }
    
    @Override
    public String toString() {
        return String.format("MultiBranchFormula[%d to %d]: %d branches", 
            start, end, 1 + elifConditions.size() + 1);
    }
}