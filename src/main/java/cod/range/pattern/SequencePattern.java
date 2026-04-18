package cod.range.pattern;

import cod.ast.node.*;
import java.util.*;

public class SequencePattern {
    
    public static class Step {
        public final String tempVar;
        public final Expr expression;
        
        public Step(String tempVar, Expr expression) {
            this.tempVar = tempVar;
            this.expression = expression;
        }
        
        public boolean isFinal() {
            return tempVar == null;
        }
    }
    
    public static class Pattern {
        public final List<Step> steps;
        public final Expr targetArray;
        public final String indexVar;
        public Expr substitutedFinalExpr;  // For output-aware optimization
        
        public Pattern(List<Step> steps, Expr targetArray, String indexVar) {
            this.steps = steps;
            this.targetArray = targetArray;
            this.indexVar = indexVar;
            this.substitutedFinalExpr = null;
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
        
        public Expr getFinalExpression() {
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
        
        public List<Expr> getTempExpressions() {
            List<Expr> exprs = new ArrayList<Expr>();
            for (Step step : steps) {
                if (step.tempVar != null) {
                    exprs.add(step.expression);
                }
            }
            return exprs;
        }
    }
    
    public static Pattern extract(List<Stmt> statements, String iterator) {
        if (statements == null || statements.isEmpty()) {
            return null;
        }
        
        List<Step> steps = new ArrayList<Step>();
        Set<String> definedVars = new HashSet<String>();
        
        for (int i = 0; i < statements.size() - 1; i++) {
            Stmt stmt = statements.get(i);
            Step step = extractVariableDefinition(stmt, iterator);
            
            if (step == null) {
                return null;
            }
            
            if ("_".equals(step.tempVar) || iterator.equals(step.tempVar)) {
                return null;
            }
            
            if (definedVars.contains(step.tempVar)) {
                return null;
            }
            
            steps.add(step);
            definedVars.add(step.tempVar);
        }
        
        Stmt lastStmt = statements.get(statements.size() - 1);
        ArrayAssignment arrayAssign = extractArrayAssignment(lastStmt, iterator);
        
        if (arrayAssign == null) {
            return null;
        }
        
        if (!validateVariableUsage(definedVars, arrayAssign.expression)) {
            return null;
        }
        
        steps.add(new Step(null, arrayAssign.expression));
        
        return new Pattern(steps, arrayAssign.targetArray, iterator);
    }
    
    private static Step extractVariableDefinition(Stmt stmt, String iterator) {
        String varName = null;
        Expr varExpr = null;
        
        if (stmt instanceof Var) {
            Var varDecl = (Var) stmt;
            varName = varDecl.name;
            varExpr = varDecl.value;
        } else if (stmt instanceof Assignment) {
            Assignment assign = (Assignment) stmt;
            if (!(assign.left instanceof Identifier)) {
                return null;
            }
            Identifier leftExpr = (Identifier) assign.left;
            varName = leftExpr.name;
            varExpr = assign.right;
            if (!assign.isDeclaration) {
                return null;
            }
        } else {
            return null;
        }
        
        return new Step(varName, varExpr);
    }
    
    private static class ArrayAssignment {
        final Expr targetArray;
        final Expr expression;
        
        ArrayAssignment(Expr targetArray, Expr expression) {
            this.targetArray = targetArray;
            this.expression = expression;
        }
    }
    
    private static ArrayAssignment extractArrayAssignment(Stmt stmt, String iterator) {
        if (!(stmt instanceof Assignment)) {
            return null;
        }
        
        Assignment assign = (Assignment) stmt;
        
        if (!(assign.left instanceof IndexAccess)) {
            return null;
        }
        
        IndexAccess indexAccess = (IndexAccess) assign.left;
        
        if (!(indexAccess.index instanceof Identifier)) {
            return null;
        }
        
        Identifier indexExpr = (Identifier) indexAccess.index;
        
        if (!iterator.equals(indexExpr.name)) {
            return null;
        }
        
        return new ArrayAssignment(indexAccess.array, assign.right);
    }
    
    private static boolean validateVariableUsage(Set<String> definedVars, Expr finalExpr) {
        Set<String> usedVars = new HashSet<String>();
        collectUsedVariables(finalExpr, usedVars);
        
        for (String var : definedVars) {
            if (!usedVars.contains(var)) {
                return false;
            }
        }
        
        return true;
    }
    
    private static void collectUsedVariables(Expr expr, Set<String> usedVars) {
        if (expr == null) return;
        
        if (expr instanceof Identifier) {
            usedVars.add(((Identifier) expr).name);
            return;
        }
        
        if (expr instanceof BinaryOp) {
            BinaryOp binOp = (BinaryOp) expr;
            collectUsedVariables(binOp.left, usedVars);
            collectUsedVariables(binOp.right, usedVars);
            return;
        }
        
        if (expr instanceof Unary) {
            Unary unary = (Unary) expr;
            collectUsedVariables(unary.operand, usedVars);
            return;
        }
        
        if (expr instanceof MethodCall) {
            MethodCall call = (MethodCall) expr;
            if (call.arguments != null) {
                for (Expr arg : call.arguments) {
                    collectUsedVariables(arg, usedVars);
                }
            }
            return;
        }
        
        if (expr instanceof IndexAccess) {
            IndexAccess access = (IndexAccess) expr;
            collectUsedVariables(access.array, usedVars);
            collectUsedVariables(access.index, usedVars);
            return;
        }
        
        if (expr instanceof TypeCast) {
            TypeCast cast = (TypeCast) expr;
            collectUsedVariables(cast.expression, usedVars);
            return;
        }
        
        if (expr instanceof Array) {
            Array array = (Array) expr;
            if (array.elements != null) {
                for (Expr elem : array.elements) {
                    collectUsedVariables(elem, usedVars);
                }
            }
            return;
        }
        
        if (expr instanceof Tuple) {
            Tuple tuple = (Tuple) expr;
            if (tuple.elements != null) {
                for (Expr elem : tuple.elements) {
                    collectUsedVariables(elem, usedVars);
                }
            }
            return;
        }
        
        if (expr instanceof PropertyAccess) {
            PropertyAccess prop = (PropertyAccess) expr;
            collectUsedVariables(prop.left, usedVars);
            collectUsedVariables(prop.right, usedVars);
            return;
        }
        
        if (expr instanceof RangeIndex) {
            RangeIndex range = (RangeIndex) expr;
            if (range.step != null) collectUsedVariables(range.step, usedVars);
            collectUsedVariables(range.start, usedVars);
            collectUsedVariables(range.end, usedVars);
            return;
        }
        
        if (expr instanceof MultiRangeIndex) {
            MultiRangeIndex multiRange = (MultiRangeIndex) expr;
            if (multiRange.ranges != null) {
                for (RangeIndex range : multiRange.ranges) {
                    if (range.step != null) collectUsedVariables(range.step, usedVars);
                    collectUsedVariables(range.start, usedVars);
                    collectUsedVariables(range.end, usedVars);
                }
            }
            return;
        }
        
        if (expr instanceof EqualityChain) {
            EqualityChain chain = (EqualityChain) expr;
            collectUsedVariables(chain.left, usedVars);
            if (chain.chainArguments != null) {
                for (Expr arg : chain.chainArguments) {
                    collectUsedVariables(arg, usedVars);
                }
            }
            return;
        }
        
        if (expr instanceof BooleanChain) {
            BooleanChain chain = (BooleanChain) expr;
            if (chain.expressions != null) {
                for (Expr e : chain.expressions) {
                    collectUsedVariables(e, usedVars);
                }
            }
            return;
        }
    }
}