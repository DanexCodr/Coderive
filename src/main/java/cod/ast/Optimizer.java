package cod.ast;

import cod.ast.nodes.*;

public class Optimizer {
    
    private static OptimizationStats stats = new OptimizationStats();
    
    /**
     * Apply expression flattening optimization
     */
    public static ASTNode flattenExpressions(ASTNode node) {
        ExpressionFlattener flattener = new ExpressionFlattener();
        return flattener.flatten(node);
    }
    
    /**
     * Apply constant folding optimization to any AST node
     */
    public static ASTNode foldConstants(ASTNode node) {
        stats.startTiming();
        ConstantFolder folder = new ConstantFolder();
        ASTNode result = folder.visit(node);
        stats.stopTiming();
        return result;
    }
    
    /**
     * Apply multiple optimization passes with intelligent ordering
     */
    public static ASTNode optimize(ASTNode node, boolean flattenExpressions, 
                                   boolean constantFolding, boolean otherOptimizations) {
        ASTNode result = node;
        
        // 1. First flatten expressions (exposes more constant folding opportunities)
        if (flattenExpressions) {
            result = flattenExpressions(result);
        }
        
        // 2. Then fold constants (works better on flattened expressions)
        if (constantFolding) {
            result = foldConstants(result);
        }
        
        // 3. Other optimizations (could include more flattening/folding cycles)
        if (otherOptimizations) {
            // Optionally run another round of flattening after constant folding
            // to clean up newly created expression structures
            if (flattenExpressions) {
                result = flattenExpressions(result);
            }
            
            // Add other optimization passes here as needed
            // result = new Inliner().visit(result);
            // result = new DeadCodeEliminator().visit(result);
        }
        
        return result;
    }
    
    /**
     * Convenience method: full optimization pipeline
     */
    public static ASTNode optimizeFull(ASTNode node) {
        return optimize(node, true, true, false);
    }
    
    /**
     * Get optimization statistics
     */
    public static OptimizationStats getStats() {
        return stats;
    }
    
    /**
     * Reset optimization statistics
     */
    public static void resetStats() {
        stats = new OptimizationStats();
    }
    
    /**
     * Print optimization statistics
     */
    public static void printStats() {
        stats.printSummary();
    }
}