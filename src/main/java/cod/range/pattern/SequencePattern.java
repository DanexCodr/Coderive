package cod.range.pattern;

import cod.ast.nodes.*;
import java.util.*;

/**
 * Detects and extracts sequence patterns from loop bodies.
 * Handles patterns like:
 * - Simple: arr[i] = i * i
 * - 2-step: squared = i * i; arr[i] = squared + 10
 * - N-step: a = i + 1; b = a * 2; c = b - 3; arr[i] = c
 */
public class SequencePattern {
    
    /**
     * Represents a single step in the sequence
     */
    public static class Step {
        public final String tempVar;      // null for final step
        public final ExprNode expression;
        
        public Step(String tempVar, ExprNode expression) {
            this.tempVar = tempVar;
            this.expression = expression;
        }
        
        public boolean isFinal() {
            return tempVar == null;
        }
    }
    
    /**
     * The complete sequence pattern extracted from loop body
     */
    public static class Pattern {
        public final List<Step> steps;           // All steps in sequence
        public final ExprNode targetArray;       // The array being assigned to
        public final String indexVar;             // Loop index variable
        
        public Pattern(List<Step> steps, ExprNode targetArray, String indexVar) {
            this.steps = steps;
            this.targetArray = targetArray;
            this.indexVar = indexVar;
        }
        
        public boolean isOptimizable() {
            return steps != null && !steps.isEmpty() && 
                   targetArray != null && indexVar != null;
        }
        
        public int getStepCount() {
            return steps.size();
        }
        
        public boolean isSimple() {
            return steps.size() == 1 && steps.get(0).isFinal();
        }
        
        public ExprNode getFinalExpression() {
            if (steps.isEmpty()) return null;
            return steps.get(steps.size() - 1).expression;
        }
        
        public List<String> getTempVarNames() {
            List<String> names = new ArrayList<String>();
            for (Step step : steps) {
                if (step.tempVar != null) {
                    names.add(step.tempVar);
                }
            }
            return names;
        }
        
        public List<ExprNode> getTempExpressions() {
            List<ExprNode> exprs = new ArrayList<ExprNode>();
            for (Step step : steps) {
                if (step.tempVar != null) {
                    exprs.add(step.expression);
                }
            }
            return exprs;
        }
    }
    
    /**
     * Extract a sequence pattern from a list of statements
     */
    public static Pattern extract(List<StmtNode> statements, String iterator) {
        if (statements == null || statements.isEmpty()) {
            return null;
        }
        
        List<Step> steps = new ArrayList<Step>();
        Set<String> definedVars = new HashSet<String>();
        
        // Process all statements except the last one (temp variable definitions)
        for (int i = 0; i < statements.size() - 1; i++) {
            StmtNode stmt = statements.get(i);
            Step step = extractVariableDefinition(stmt, iterator);
            
            if (step == null) {
                return null; // Not a valid variable definition
            }
            
            // Skip if assigning to '_' or iterator variable
            if ("_".equals(step.tempVar) || iterator.equals(step.tempVar)) {
                return null;
            }
            
            // Check for duplicate variable names
            if (definedVars.contains(step.tempVar)) {
                return null; // Can't redefine variable in same sequence
            }
            
            steps.add(step);
            definedVars.add(step.tempVar);
        }
        
        // Process the last statement (must be array assignment)
        StmtNode lastStmt = statements.get(statements.size() - 1);
        ArrayAssignment arrayAssign = extractArrayAssignment(lastStmt, iterator);
        
        if (arrayAssign == null) {
            return null;
        }
        
        // Validate that all temp variables are used in the expression chain
        if (!validateVariableUsage(definedVars, arrayAssign.expression)) {
            return null;
        }
        
        // Add final step (no temp variable)
        steps.add(new Step(null, arrayAssign.expression));
        
        return new Pattern(steps, arrayAssign.targetArray, iterator);
    }
    
    /**
     * Extract a variable definition step
     */
    private static Step extractVariableDefinition(StmtNode stmt, String iterator) {
        String varName = null;
        ExprNode varExpr = null;
        
        if (stmt instanceof VarNode) {
            // Declaration with := (VarNode)
            VarNode varDecl = (VarNode) stmt;
            varName = varDecl.name;
            varExpr = varDecl.value;
        } else if (stmt instanceof AssignmentNode) {
            // Assignment with = (AssignmentNode)
            AssignmentNode assign = (AssignmentNode) stmt;
            
            // Must assign to a simple variable (not array[index])
            if (!(assign.left instanceof IdentifierNode)) {
                return null;
            }
            IdentifierNode leftExpr = (IdentifierNode) assign.left;
            varName = leftExpr.name;
            varExpr = assign.right;
            
            // Only optimize if this is a declaration (:=) 
            if (!assign.isDeclaration) {
                return null;
            }
        } else {
            return null; // Not a variable declaration/assignment
        }
        
        return new Step(varName, varExpr);
    }
    
    /**
     * Represents an array assignment
     */
    private static class ArrayAssignment {
        final ExprNode targetArray;
        final ExprNode expression;
        
        ArrayAssignment(ExprNode targetArray, ExprNode expression) {
            this.targetArray = targetArray;
            this.expression = expression;
        }
    }
    
    /**
     * Extract array assignment from statement
     */
    private static ArrayAssignment extractArrayAssignment(StmtNode stmt, String iterator) {
        if (!(stmt instanceof AssignmentNode)) {
            return null;
        }
        
        AssignmentNode assign = (AssignmentNode) stmt;
        
        // Must be array[index] assignment
        if (!(assign.left instanceof IndexAccessNode)) {
            return null;
        }
        
        IndexAccessNode indexAccess = (IndexAccessNode) assign.left;
        
        // Check if index is the iterator variable
        if (!(indexAccess.index instanceof IdentifierNode)) {
            return null;
        }
        
        IdentifierNode indexExpr = (IdentifierNode) indexAccess.index;
        
        // Check if index matches iterator name
        if (!iterator.equals(indexExpr.name)) {
            return null;
        }
        
        return new ArrayAssignment(indexAccess.array, assign.right);
    }
    
    /**
     * Validate that all defined variables are used in the final expression
     */
    private static boolean validateVariableUsage(Set<String> definedVars, ExprNode finalExpr) {
        Set<String> usedVars = new HashSet<String>();
        collectUsedVariables(finalExpr, usedVars);
        
        // Check that every defined variable is used somewhere
        for (String var : definedVars) {
            if (!usedVars.contains(var)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Collect all variable names used in an expression
     */
    private static void collectUsedVariables(ExprNode expr, Set<String> usedVars) {
        if (expr == null) return;
        
        if (expr instanceof IdentifierNode) {
            usedVars.add(((IdentifierNode) expr).name);
            return;
        }
        
        if (expr instanceof BinaryOpNode) {
            BinaryOpNode binOp = (BinaryOpNode) expr;
            collectUsedVariables(binOp.left, usedVars);
            collectUsedVariables(binOp.right, usedVars);
            return;
        }
        
        if (expr instanceof UnaryNode) {
            UnaryNode unary = (UnaryNode) expr;
            collectUsedVariables(unary.operand, usedVars);
            return;
        }
        
        if (expr instanceof MethodCallNode) {
            MethodCallNode call = (MethodCallNode) expr;
            if (call.arguments != null) {
                for (ExprNode arg : call.arguments) {
                    collectUsedVariables(arg, usedVars);
                }
            }
            return;
        }
        
        if (expr instanceof IndexAccessNode) {
            IndexAccessNode access = (IndexAccessNode) expr;
            collectUsedVariables(access.array, usedVars);
            collectUsedVariables(access.index, usedVars);
            return;
        }
        
        if (expr instanceof TypeCastNode) {
            TypeCastNode cast = (TypeCastNode) expr;
            collectUsedVariables(cast.expression, usedVars);
            return;
        }
        
        if (expr instanceof ArrayNode) {
            ArrayNode array = (ArrayNode) expr;
            if (array.elements != null) {
                for (ExprNode elem : array.elements) {
                    collectUsedVariables(elem, usedVars);
                }
            }
            return;
        }
        
        if (expr instanceof TupleNode) {
            TupleNode tuple = (TupleNode) expr;
            if (tuple.elements != null) {
                for (ExprNode elem : tuple.elements) {
                    collectUsedVariables(elem, usedVars);
                }
            }
            return;
        }
        
        if (expr instanceof PropertyAccessNode) {
            PropertyAccessNode prop = (PropertyAccessNode) expr;
            collectUsedVariables(prop.left, usedVars);
            collectUsedVariables(prop.right, usedVars);
            return;
        }
        
        if (expr instanceof RangeIndexNode) {
            RangeIndexNode range = (RangeIndexNode) expr;
            if (range.step != null) collectUsedVariables(range.step, usedVars);
            collectUsedVariables(range.start, usedVars);
            collectUsedVariables(range.end, usedVars);
            return;
        }
        
        if (expr instanceof MultiRangeIndexNode) {
            MultiRangeIndexNode multiRange = (MultiRangeIndexNode) expr;
            if (multiRange.ranges != null) {
                for (RangeIndexNode range : multiRange.ranges) {
                    if (range.step != null) collectUsedVariables(range.step, usedVars);
                    collectUsedVariables(range.start, usedVars);
                    collectUsedVariables(range.end, usedVars);
                }
            }
            return;
        }
        
        if (expr instanceof EqualityChainNode) {
            EqualityChainNode chain = (EqualityChainNode) expr;
            collectUsedVariables(chain.left, usedVars);
            if (chain.chainArguments != null) {
                for (ExprNode arg : chain.chainArguments) {
                    collectUsedVariables(arg, usedVars);
                }
            }
            return;
        }
        
        if (expr instanceof BooleanChainNode) {
            BooleanChainNode chain = (BooleanChainNode) expr;
            if (chain.expressions != null) {
                for (ExprNode e : chain.expressions) {
                    collectUsedVariables(e, usedVars);
                }
            }
            return;
        }
    }
}