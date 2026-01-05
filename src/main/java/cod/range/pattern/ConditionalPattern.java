package cod.range.pattern;

import cod.ast.nodes.*;
import java.util.*;  // IMPORT ADDED

public class ConditionalPattern {
    public final ExprNode condition;
    public final ExprNode thenExpr;
    public final ExprNode elseExpr;
    public final ExprNode arrayExpression;
    public final String indexVar;
    
    public final List<ExprNode> elifConditions;
    public final List<ExprNode> elifExpressions;
    
    public ConditionalPattern(ExprNode condition, ExprNode thenExpr, ExprNode elseExpr,
                            ExprNode arrayExpression, String indexVar) {
        this(condition, thenExpr, elseExpr, arrayExpression, indexVar,
             new ArrayList<ExprNode>(), new ArrayList<ExprNode>());
    }
    
    public ConditionalPattern(ExprNode condition, ExprNode thenExpr, ExprNode elseExpr,
                            ExprNode arrayExpression, String indexVar,
                            List<ExprNode> elifConditions, List<ExprNode> elifExpressions) {
        this.condition = condition;
        this.thenExpr = thenExpr;
        this.elseExpr = elseExpr;
        this.arrayExpression = arrayExpression;
        this.indexVar = indexVar;
        this.elifConditions = elifConditions != null ? elifConditions : new ArrayList<ExprNode>();
        this.elifExpressions = elifExpressions != null ? elifExpressions : new ArrayList<ExprNode>();
    }
    
    public boolean hasElif() {
        return elifConditions != null && !elifConditions.isEmpty();
    }

    public boolean isOptimizable() {
        return condition != null && thenExpr != null && elseExpr != null &&
               arrayExpression != null && indexVar != null;
    }
    
    public static ConditionalPattern extract(StmtIfNode ifStmt, String iterator) {
        return extractRecursive(ifStmt, iterator, null);
    }

    private static ConditionalPattern extractRecursive(StmtIfNode ifStmt, String iterator, ExprNode targetArray) {
        // Extract then branch (must be single assignment)
        if (ifStmt.thenBlock == null || ifStmt.thenBlock.statements.size() != 1) {
            return null;
        }
        
        AssignmentPattern thenPattern = AssignmentPattern.extract(
            ifStmt.thenBlock.statements.get(0), iterator);
        
        if (thenPattern == null) {
            return null;
        }
        
        // Track which array we're assigning to
        if (targetArray == null) {
            targetArray = thenPattern.array;
        } else if (!areSameArray(targetArray, thenPattern.array)) {
            return null;  // All branches must assign to SAME array
        }
        
        List<ExprNode> conditions = new ArrayList<>();
        List<ExprNode> expressions = new ArrayList<>();
        conditions.add(ifStmt.condition);
        expressions.add(thenPattern.expression);
        
        // Handle else/elif recursively
        ExprNode finalElse = null;
        
        if (ifStmt.elseBlock != null && ifStmt.elseBlock.statements.size() == 1) {
            StmtNode elseStmt = ifStmt.elseBlock.statements.get(0);
            
            if (elseStmt instanceof StmtIfNode) {
                // RECURSION: This is an elif branch!
                ConditionalPattern subPattern = extractRecursive(
                    (StmtIfNode) elseStmt, iterator, targetArray);
                
                if (subPattern != null) {
                    // Merge: our conditions + their conditions
                    if (subPattern.hasElif()) {
                        conditions.addAll(subPattern.elifConditions);
                        conditions.add(subPattern.condition);
                        expressions.addAll(subPattern.elifExpressions);
                        expressions.add(subPattern.thenExpr);
                    } else {
                        conditions.add(subPattern.condition);
                        expressions.add(subPattern.thenExpr);
                    }
                    finalElse = subPattern.elseExpr;
                } else {
                    return null; // Subpattern invalid
                }
            } else {
                // Base case: final else (assignment)
                AssignmentPattern elsePattern = AssignmentPattern.extract(elseStmt, iterator);
                if (elsePattern == null || !areSameArray(targetArray, elsePattern.array)) {
                    return null;
                }
                finalElse = elsePattern.expression;
            }
        } else {
            // No else: implicit else = original array value (i)
            finalElse = createVariableExpr(iterator);
        }
        
        // Build the pattern
        if (conditions.size() > 1) {
            // Has elif branches
            ExprNode firstCondition = conditions.remove(0);
            ExprNode firstExpression = expressions.remove(0);
            
            return new ConditionalPattern(
                firstCondition, firstExpression, finalElse,
                targetArray, iterator, conditions, expressions
            );
        } else {
            // Simple if-else or if-only
            return new ConditionalPattern(
                conditions.get(0), expressions.get(0), finalElse,
                targetArray, iterator
            );
        }
    }
    
    // Helper to check if two array expressions refer to the same array
    private static boolean areSameArray(ExprNode array1, ExprNode array2) {
        if (array1 == null || array2 == null) return false;
        
        // Simple check: if both are variable references with same name
        if (array1 instanceof ExprNode && array2 instanceof ExprNode) {
            ExprNode expr1 = (ExprNode) array1;
            ExprNode expr2 = (ExprNode) array2;
            
            if (expr1.name != null && expr2.name != null) {
                return expr1.name.equals(expr2.name);
            }
        }
        
        // For now, assume they're the same if their string representations match
        return array1.toString().equals(array2.toString());
    }
    
    // Helper to create a simple variable expression
    private static ExprNode createVariableExpr(String varName) {
        ExprNode expr = new ExprNode();
        expr.name = varName;
        return expr;
    }
}