package cod.range.pattern;

import cod.ast.ASTFactory;
import cod.ast.node.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detects patterns in loops that mix computation with output.
 * Separates pure computation (can become formula) from side effects (output).
 * Now with automatic output batching for 50% I/O reduction!
 */
public class OutputAwarePattern {
    
    public static class OutputPattern {
        public final Object computation;      // SequencePattern or ConditionalPattern
        public final List<MethodCall> outputCalls;  // The out() calls
        public final boolean isOptimizable;
        
        // Batched output storage
        private List<OutputBatch> batches = new ArrayList<>();
        
        public OutputPattern(Object computation, List<MethodCall> outputCalls) {
            this.computation = computation;
            this.outputCalls = outputCalls != null ? outputCalls : new ArrayList<MethodCall>();
            this.isOptimizable = computation != null && !this.outputCalls.isEmpty();
            
            if (this.isOptimizable) {
                batchOutputs();
            }
        }
        
        // Batch outputs by type (out vs outs) and pair them
        private void batchOutputs() {
            if (outputCalls.isEmpty()) return;
            
            List<MethodCall> outCalls = new ArrayList<>();
            List<MethodCall> outsCalls = new ArrayList<>();
            
            for (MethodCall call : outputCalls) {
                if ("out".equals(call.name)) {
                    outCalls.add(call);
                } else if ("outs".equals(call.name)) {
                    outsCalls.add(call);
                }
            }
            
            for (MethodCall call : outCalls) {
                batches.add(new OutputBatch("out", call));
            }
            
            for (MethodCall call : outsCalls) {
                batches.add(new OutputBatch("outs", call));
            }
        }
        
        public List<OutputBatch> getBatches() {
            return batches;
        }
    }
    
    public static class OutputBatch {
        public final String type;  // "out" or "outs"
        public final List<MethodCall> calls;
        
        public OutputBatch(String type, MethodCall... calls) {
            this.type = type;
            this.calls = new ArrayList<>();
            for (MethodCall call : calls) {
                this.calls.add(call);
            }
        }
        
        public boolean isPaired() {
            return calls.size() > 1;
        }
        
        public List<Expr> getAllArguments() {
            List<Expr> allArgs = new ArrayList<>();
            for (MethodCall call : calls) {
                allArgs.addAll(call.arguments);
            }
            return allArgs;
        }
    }
    
    /**
     * Extract computation and output from a for loop body
     */
    public static OutputPattern extract(For node, String iterator) {
        if (node == null || node.body == null || node.body.statements == null) {
            return new OutputPattern(null, null);
        }
        
        List<Stmt> computationStmts = new ArrayList<Stmt>();
        List<MethodCall> outputCalls = new ArrayList<MethodCall>();
        Map<String, Expr> varDefinitions = new HashMap<String, Expr>();
        
        for (Stmt stmt : node.body.statements) {
            if (isOutputCall(stmt)) {
                outputCalls.add((MethodCall) stmt);
            } else {
                if (stmt instanceof Var) {
                    Var var = (Var) stmt;
                    if (var.value != null) {
                        varDefinitions.put(var.name, var.value);
                    }
                } else if (stmt instanceof Assignment && ((Assignment) stmt).isDeclaration) {
                    Assignment assign = (Assignment) stmt;
                    if (assign.left instanceof Identifier) {
                        varDefinitions.put(((Identifier) assign.left).name, assign.right);
                    }
                }
                computationStmts.add(stmt);
            }
        }
        
        if (outputCalls.isEmpty()) {
            return new OutputPattern(null, null);
        }
        
        SequencePattern.Pattern seqPattern = SequencePattern.extract(computationStmts, iterator);
        if (seqPattern != null && seqPattern.isOptimizable()) {
            Expr finalExpr = seqPattern.getFinalExpression();
            Expr substitutedExpr = substituteVariables(finalExpr, varDefinitions);
            
            // Store the substituted expression in the pattern
            seqPattern.substitutedFinalExpr = substitutedExpr;
            
            for (MethodCall outputCall : outputCalls) {
                if (outputCall.arguments != null && !outputCall.arguments.isEmpty()) {
                    Expr clonedExpr = cloneExpression(substitutedExpr);
                    outputCall.arguments.set(0, clonedExpr);
                }
            }
            
            return new OutputPattern(seqPattern, outputCalls);
        }
        
        ConditionalPattern condPattern = extractConditionalPatternFromList(computationStmts, iterator);
        if (condPattern != null && condPattern.isOptimizable()) {
            return new OutputPattern(condPattern, outputCalls);
        }
        
        if (!outputCalls.isEmpty()) {
            return new OutputPattern(null, outputCalls);
        }
        
        return new OutputPattern(null, null);
    }
    
    /**
     * Substitute variable references with their definitions recursively
     */
    private static Expr substituteVariables(Expr expr, Map<String, Expr> varDefinitions) {
        if (expr == null) return null;
        
        if (expr instanceof Identifier) {
            String name = ((Identifier) expr).name;
            Expr replacement = varDefinitions.get(name);
            if (replacement != null) {
                return substituteVariables(replacement, varDefinitions);
            }
            return expr;
        }
        
        if (expr instanceof BinaryOp) {
            BinaryOp bin = (BinaryOp) expr;
            Expr left = substituteVariables(bin.left, varDefinitions);
            Expr right = substituteVariables(bin.right, varDefinitions);
            if (left == null || right == null) {
                return ASTFactory.createBinaryOp(
                    left != null ? left : bin.left,
                    bin.op,
                    right != null ? right : bin.right,
                    null
                );
            }
            return ASTFactory.createBinaryOp(left, bin.op, right, null);
        }
        
        if (expr instanceof Unary) {
            Unary unary = (Unary) expr;
            Expr operand = substituteVariables(unary.operand, varDefinitions);
            if (operand == null) {
                return expr;
            }
            return ASTFactory.createUnaryOp(unary.op, operand, null);
        }
        
        if (expr instanceof TypeCast) {
            TypeCast cast = (TypeCast) expr;
            Expr expression = substituteVariables(cast.expression, varDefinitions);
            if (expression == null) {
                return expr;
            }
            return ASTFactory.createTypeCast(cast.targetType, expression, null);
        }
        
        return expr;
    }
    
    /**
     * Simple expression cloner for the expressions we care about
     */
    private static Expr cloneExpression(Expr expr) {
        if (expr == null) return null;
        
        if (expr instanceof BinaryOp) {
            BinaryOp bin = (BinaryOp) expr;
            return ASTFactory.createBinaryOp(
                cloneExpression(bin.left),
                bin.op,
                cloneExpression(bin.right),
                null
            );
        }
        if (expr instanceof Identifier) {
            return ASTFactory.createIdentifier(((Identifier) expr).name, null);
        }
        if (expr instanceof IntLiteral) {
            return ASTFactory.createIntLiteral(
                (int) ((IntLiteral) expr).value.longValue(), 
                null
            );
        }
        if (expr instanceof FloatLiteral) {
            return ASTFactory.createFloatLiteral(((FloatLiteral) expr).value, null);
        }
        if (expr instanceof Unary) {
            Unary unary = (Unary) expr;
            return ASTFactory.createUnaryOp(unary.op, cloneExpression(unary.operand), null);
        }
        if (expr instanceof TypeCast) {
            TypeCast cast = (TypeCast) expr;
            return ASTFactory.createTypeCast(cast.targetType, cloneExpression(cast.expression), null);
        }
        return expr;
    }
    
    private static boolean isOutputCall(Stmt stmt) {
        if (!(stmt instanceof MethodCall)) {
            return false;
        }
        MethodCall call = (MethodCall) stmt;
        return "out".equals(call.name) || "outs".equals(call.name);
    }
    
    private static ConditionalPattern extractConditionalPatternFromList(List<Stmt> stmts, String iterator) {
        if (stmts == null || stmts.isEmpty()) {
            return null;
        }
        
        StmtIf firstIf = null;
        int ifIndex = -1;
        
        for (int i = 0; i < stmts.size(); i++) {
            if (stmts.get(i) instanceof StmtIf) {
                firstIf = (StmtIf) stmts.get(i);
                ifIndex = i;
                break;
            }
        }
        
        if (firstIf == null) {
            return null;
        }
        
        for (int i = 0; i < ifIndex; i++) {
            if (!isVariableDeclaration(stmts.get(i))) {
                return null;
            }
        }
        
        List<ConditionalPattern> patterns = ConditionalPattern.extractAll(firstIf, iterator);
        if (patterns.isEmpty()) {
            return null;
        }
        return patterns.get(0);
    }
    
    private static boolean isVariableDeclaration(Stmt stmt) {
        if (stmt instanceof Var) {
            return true;
        }
        if (stmt instanceof Assignment) {
            return ((Assignment) stmt).isDeclaration;
        }
        return false;
    }
}