package cod.ast;

/**
 * Tracks statistics about optimization passes
 */
public class OptimizationStats {
    private int constantExpressionsFolded = 0;
    private int binaryOpsSimplified = 0;
    private int booleanChainsOptimized = 0;
    private int typeCastsEliminated = 0;
    private long startTime = 0;
    private long endTime = 0;
    
    public void startTiming() {
        startTime = System.nanoTime();
    }
    
    public void stopTiming() {
        endTime = System.nanoTime();
    }
    
    public void incrementConstantExpressions() {
        constantExpressionsFolded++;
    }
    
    public void incrementBinaryOps() {
        binaryOpsSimplified++;
    }
    
    public void incrementBooleanChains() {
        booleanChainsOptimized++;
    }
    
    public void incrementTypeCasts() {
        typeCastsEliminated++;
    }
    
    public void printSummary() {
        long duration = endTime - startTime;
        System.out.println("\n=== OPTIMIZATION SUMMARY ===");
        System.out.println("Constant expressions folded: " + constantExpressionsFolded);
        System.out.println("Binary operations simplified: " + binaryOpsSimplified);
        System.out.println("Boolean chains optimized: " + booleanChainsOptimized);
        System.out.println("Type casts eliminated: " + typeCastsEliminated);
        System.out.println("Total optimizations: " + getTotalOptimizations());
        System.out.printf("Optimization time: %.3f ms\n", duration / 1_000_000.0);
        System.out.println("=============================\n");
    }
    
    public int getTotalOptimizations() {
        return constantExpressionsFolded + binaryOpsSimplified + 
               booleanChainsOptimized + typeCastsEliminated;
    }
}