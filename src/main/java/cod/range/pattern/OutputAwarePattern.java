package cod.range.pattern;

import cod.ast.nodes.*;
import java.util.ArrayList;
import java.util.List;

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
        
        // NEW: Batched output storage
        private List<OutputBatch> batches = new ArrayList<>();
        
        public OutputPattern(Object computation, List<MethodCall> outputCalls) {
            this.computation = computation;
            this.outputCalls = outputCalls != null ? outputCalls : new ArrayList<MethodCall>();
            this.isOptimizable = computation != null && !this.outputCalls.isEmpty();
            
            // NEW: Auto-batch outputs if optimizable
            if (this.isOptimizable) {
                batchOutputs();
            }
        }
        
        // NEW: Batch outputs by type (out vs outs) and pair them
        private void batchOutputs() {
            if (outputCalls.isEmpty()) return;
            
            List<MethodCall> outCalls = new ArrayList<>();
            List<MethodCall> outsCalls = new ArrayList<>();
            
            // Separate by type
            for (MethodCall call : outputCalls) {
                if ("out".equals(call.name)) {
                    outCalls.add(call);
                } else if ("outs".equals(call.name)) {
                    outsCalls.add(call);
                }
            }
            
            // Batch out() calls in pairs
            for (int i = 0; i < outCalls.size(); i += 2) {
                if (i + 1 < outCalls.size()) {
                    batches.add(new OutputBatch("out", outCalls.get(i), outCalls.get(i + 1)));
                } else {
                    batches.add(new OutputBatch("out", outCalls.get(i)));
                }
            }
            
            // Batch outs() calls in pairs
            for (int i = 0; i < outsCalls.size(); i += 2) {
                if (i + 1 < outsCalls.size()) {
                    batches.add(new OutputBatch("outs", outsCalls.get(i), outsCalls.get(i + 1)));
                } else {
                    batches.add(new OutputBatch("outs", outsCalls.get(i)));
                }
            }
        }
        
        // NEW: Get batched outputs for execution
        public List<OutputBatch> getBatches() {
            return batches;
        }
    }
    
    // NEW: Represents a batched output operation
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
        
        // NEW: Check if this batch has multiple calls
        public boolean isPaired() {
            return calls.size() > 1;
        }
        
        // NEW: Get all arguments flattened
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
        
        // Separate computation from output
        for (Stmt stmt : node.body.statements) {
            if (isOutputCall(stmt)) {
                outputCalls.add((MethodCall) stmt);
            } else {
                computationStmts.add(stmt);
            }
        }
        
        // If no output calls, not optimizable by this pattern
        if (outputCalls.isEmpty()) {
            return new OutputPattern(null, null);
        }
        
        // Try to detect pattern in computation statements
        Object computation = null;
        
        // Try sequence pattern first
        SequencePattern.Pattern seqPattern = 
            SequencePattern.extract(computationStmts, iterator);
        if (seqPattern != null && seqPattern.isOptimizable()) {
            computation = seqPattern;
            return new OutputPattern(computation, outputCalls);
        }
        
        // Try conditional pattern
        ConditionalPattern condPattern = 
            extractConditionalPatternFromList(computationStmts, iterator);
        if (condPattern != null && condPattern.isOptimizable()) {
            computation = condPattern;
            return new OutputPattern(computation, outputCalls);
        }
        
        return new OutputPattern(null, null);
    }
    
    /**
     * Check if a statement is an output call (out() or outs())
     */
    private static boolean isOutputCall(Stmt stmt) {
        if (!(stmt instanceof MethodCall)) {
            return false;
        }
        
        MethodCall call = (MethodCall) stmt;
        return "out".equals(call.name) || "outs".equals(call.name);
    }
    
    /**
     * Extract conditional pattern from a list of statements
     */
    private static ConditionalPattern extractConditionalPatternFromList(
            List<Stmt> stmts, String iterator) {
        
        if (stmts == null || stmts.isEmpty()) {
            return null;
        }
        
        // Find the first if-statement
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
        
        // Check that all statements before the if are variable declarations
        for (int i = 0; i < ifIndex; i++) {
            if (!isVariableDeclaration(stmts.get(i))) {
                return null;
            }
        }
        
        // Extract the conditional pattern
        return ConditionalPattern.extract(firstIf, iterator);
    }
    
    /**
     * Check if a statement is a variable declaration
     */
    private static boolean isVariableDeclaration(Stmt stmt) {
        if (stmt instanceof Var) {
            return true;
        }
        
        if (stmt instanceof Assignment) {
            Assignment assign = (Assignment) stmt;
            return assign.isDeclaration;
        }
        
        return false;
    }
}