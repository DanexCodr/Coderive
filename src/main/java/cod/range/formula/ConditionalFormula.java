// In ConditionalFormula.java
package cod.range.formula;

import cod.ast.nodes.*;
import cod.interpreter.Evaluator;
import cod.interpreter.context.ExecutionContext;
import cod.interpreter.handler.TypeHandler;
import java.util.*;

public class ConditionalFormula {
    public final long start;
    public final long end;
    public final String indexVar;
    public final List<ExprNode> conditions;
    public final List<List<StmtNode>> branchStatements;
    public final List<StmtNode> elseStatements;
    
    public ConditionalFormula(long start, long end, String indexVar,
                             List<ExprNode> conditions,
                             List<List<StmtNode>> branchStatements,
                             List<StmtNode> elseStatements) {
        this.start = start;
        this.end = end;
        this.indexVar = indexVar;
        this.conditions = conditions != null ? conditions : new ArrayList<ExprNode>();
        this.branchStatements = branchStatements != null ? branchStatements : new ArrayList<List<StmtNode>>();
        this.elseStatements = elseStatements != null ? elseStatements : new ArrayList<StmtNode>();
    }
    
    public boolean contains(long index) {
        return index >= start && index <= end;
    }
    
    public Object evaluate(long index, Evaluator evaluator, ExecutionContext context) {
        TypeHandler typeSystem = new TypeHandler(); // or get from somewhere
        
        // Create base context with index variable
        ExecutionContext evalCtx = context.copyWithVariable(indexVar, index, null);
        
        try {
            // Try each branch in order
            for (int i = 0; i < conditions.size(); i++) {
                Object condResult = evaluator.evaluate(conditions.get(i), evalCtx);
                if (typeSystem.isTruthy(typeSystem.unwrap(condResult))) {
                    // Execute branch statements in sequence
                    return executeStatementSequence(branchStatements.get(i), evaluator, evalCtx);
                }
            }
            
            // No branch matched - execute else statements
            return executeStatementSequence(elseStatements, evaluator, evalCtx);
            
        } catch (Exception e) {
            throw new RuntimeException("Error evaluating conditional formula at index " + index, e);
        }
    }
    
    private Object executeStatementSequence(
            List<StmtNode> statements, Evaluator evaluator, ExecutionContext ctx) {
        
        Object lastResult = null;
        
        // Create a new scope for temporary variables
        ctx.pushScope();
        
        try {
            for (StmtNode stmt : statements) {
                lastResult = evaluator.evaluate(stmt, ctx);
            }
        } finally {
            ctx.popScope();
        }
        
        return lastResult;
    }
}