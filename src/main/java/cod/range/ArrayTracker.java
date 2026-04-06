// ArrayTracker.java
package cod.range;

import cod.ast.node.For;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks NaturalArray instances used inside loops for optimization and debugging.
 * Uses integer IDs for efficient tracking and lookup.
 */
public class ArrayTracker {
    
    // Counter for generating unique integer IDs
    private static final AtomicInteger nextId = new AtomicInteger(1);
    
    // Track arrays per loop (loopId -> Set of array IDs)
    private static final ThreadLocal<Map<Integer, Set<Integer>>> loopArrays = 
        new ThreadLocal<Map<Integer, Set<Integer>>>() {
            @Override
            protected Map<Integer, Set<Integer>> initialValue() {
                return new HashMap<Integer, Set<Integer>>();
            }
        };
    
    // Track current loop being processed
    private static final ThreadLocal<Stack<LoopInfo>> loopStack = 
        new ThreadLocal<Stack<LoopInfo>>() {
            @Override
            protected Stack<LoopInfo> initialValue() {
                return new Stack<LoopInfo>();
            }
        };
    
    // Statistics per loop (loopId -> stats)
    private static final ThreadLocal<Map<Integer, LoopStats>> loopStats = 
        new ThreadLocal<Map<Integer, LoopStats>>() {
            @Override
            protected Map<Integer, LoopStats> initialValue() {
                return new HashMap<Integer, LoopStats>();
            }
        };
    
    // Map array IDs to their instances (for global lookup)
    private static final Map<Integer, NaturalArray> arrayRegistry = 
        new HashMap<Integer, NaturalArray>();
    
    // Map to track which loops have been optimized
    private static final Set<Integer> optimizedLoops = new HashSet<Integer>();
    
    /**
     * Information about a loop being tracked
     */
    public static class LoopInfo {
        public final int loopId;
        public final For node;
        public final long startTime;
        
        public LoopInfo(int loopId, For node) {
            this.loopId = loopId;
            this.node = node;
            this.startTime = System.nanoTime();
        }
    }
    
    /**
     * Statistics for a loop - UPDATED with size and side effects
     */
    public static class LoopStats {
        public final int loopId;
        public int iterationCount;
        public long totalExecutionTime;
        public int arrayAccessCount;
        public int arrayModificationCount;
        public int pendingUpdatesCount;
        public int cacheHits;
        public int cacheMisses;
        public int formulaApplications;
        public Set<Integer> arrayIds;
        public boolean wasOptimized;
        
        // NEW: Simple tracking fields
        public long estimatedSize;
        public boolean hadSideEffects;
        
        public LoopStats(int loopId) {
            this.loopId = loopId;
            this.iterationCount = 0;
            this.totalExecutionTime = 0;
            this.arrayAccessCount = 0;
            this.arrayModificationCount = 0;
            this.pendingUpdatesCount = 0;
            this.cacheHits = 0;
            this.cacheMisses = 0;
            this.formulaApplications = 0;
            this.arrayIds = new HashSet<Integer>();
            this.wasOptimized = false;
            
            // NEW: Initialize simple tracking
            this.estimatedSize = -1;
            this.hadSideEffects = false;
        }
        
        public double getCacheHitRate() {
            int total = cacheHits + cacheMisses;
            return total > 0 ? (cacheHits * 100.0 / total) : 0;
        }
        
        // UPDATED toString with size and side effects
        @Override
        public String toString() {
            return String.format(
                "LoopStats[id=%d, size=%d, sideEffects=%s, iter=%d, time=%.3fms, " +
                "accesses=%d, mods=%d, cache=%.1f%%, optimized=%s]",
                loopId, estimatedSize, hadSideEffects, iterationCount, 
                totalExecutionTime / 1_000_000.0,
                arrayAccessCount, arrayModificationCount, 
                getCacheHitRate(), wasOptimized
            );
        }
    }
    
    /**
     * Start tracking a loop
     * @return integer ID for the loop
     */
    public static int beginLoop(For node) {
        int loopId = nextId.getAndIncrement();
        LoopInfo info = new LoopInfo(loopId, node);
        loopStack.get().push(info);
        
        // Initialize stats
        loopStats.get().put(loopId, new LoopStats(loopId));
        
        // Initialize array set for this loop
        loopArrays.get().put(loopId, new HashSet<Integer>());
        
        return loopId;
    }
    
    /**
     * End tracking current loop
     * @return statistics for the loop
     */
    public static LoopStats endLoop() {
        Stack<LoopInfo> stack = loopStack.get();
        if (stack.isEmpty()) {
            return null;
        }
        
        LoopInfo info = stack.pop();
        long endTime = System.nanoTime();
        
        LoopStats stats = loopStats.get().get(info.loopId);
        if (stats != null) {
            stats.totalExecutionTime = endTime - info.startTime;
            stats.wasOptimized = optimizedLoops.contains(info.loopId);
        }
        
        // Clean up thread-local data
        loopArrays.get().remove(info.loopId);
        loopStats.get().remove(info.loopId);
        
        return stats;
    }
    
    /**
     * Get current loop ID (0 if not in a loop)
     */
    public static int getCurrentLoopId() {
        Stack<LoopInfo> stack = loopStack.get();
        return stack.isEmpty() ? 0 : stack.peek().loopId;
    }
    
    /**
     * Get current loop info (null if not in a loop)
     */
    public static LoopInfo getCurrentLoop() {
        Stack<LoopInfo> stack = loopStack.get();
        return stack.isEmpty() ? null : stack.peek();
    }
    
    /**
     * Register that an array is used in the current loop
     */
    public static void registerArray(NaturalArray array) {
        int loopId = getCurrentLoopId();
        if (loopId == 0) return;
        
        int arrayId = array.getArrayId();
        Set<Integer> arrays = loopArrays.get().get(loopId);
        if (arrays != null) {
            if (arrays.add(arrayId)) {
                // New array in this loop
                LoopStats stats = loopStats.get().get(loopId);
                if (stats != null) {
                    stats.arrayIds.add(arrayId);
                }
            }
        }
    }
    
    // ========== NEW METHODS FOR SIMPLE TRACKING ==========
    
    /**
     * Set estimated size for the current loop
     */
    public static void setLoopSize(int loopId, long size) {
        LoopStats stats = loopStats.get().get(loopId);
        if (stats != null) {
            stats.estimatedSize = size;
        }
    }
    
    /**
     * Set whether loop has side effects
     */
    public static void setSideEffects(int loopId, boolean hasSideEffects) {
        LoopStats stats = loopStats.get().get(loopId);
        if (stats != null) {
            stats.hadSideEffects = hasSideEffects;
        }
    }
    
    // ========== EXISTING RECORDING METHODS ==========
    
    /**
     * Record an array access
     */
    public static void recordArrayAccess(NaturalArray array) {
        int loopId = getCurrentLoopId();
        if (loopId == 0) return;
        
        LoopStats stats = loopStats.get().get(loopId);
        if (stats != null) {
            stats.arrayAccessCount++;
        }
    }
    
    /**
     * Record an array modification
     */
    public static void recordArrayModification(NaturalArray array) {
        int loopId = getCurrentLoopId();
        if (loopId == 0) return;
        
        LoopStats stats = loopStats.get().get(loopId);
        if (stats != null) {
            stats.arrayModificationCount++;
        }
    }
    
    /**
     * Record pending updates
     */
    public static void recordPendingUpdates(NaturalArray array, int count) {
        int loopId = getCurrentLoopId();
        if (loopId == 0) return;
        
        LoopStats stats = loopStats.get().get(loopId);
        if (stats != null) {
            stats.pendingUpdatesCount += count;
        }
    }
    
    /**
     * Record cache hit
     */
    public static void recordCacheHit(NaturalArray array) {
        int loopId = getCurrentLoopId();
        if (loopId == 0) return;
        
        LoopStats stats = loopStats.get().get(loopId);
        if (stats != null) {
            stats.cacheHits++;
        }
    }
    
    /**
     * Record cache miss
     */
    public static void recordCacheMiss(NaturalArray array) {
        int loopId = getCurrentLoopId();
        if (loopId == 0) return;
        
        LoopStats stats = loopStats.get().get(loopId);
        if (stats != null) {
            stats.cacheMisses++;
        }
    }
    
    /**
     * Record formula application
     */
    public static void recordFormulaApplication(NaturalArray array) {
        int loopId = getCurrentLoopId();
        if (loopId == 0) return;
        
        LoopStats stats = loopStats.get().get(loopId);
        if (stats != null) {
            stats.formulaApplications++;
        }
    }
    
    /**
     * Mark a loop as optimized
     */
    public static void markLoopOptimized(int loopId) {
        optimizedLoops.add(loopId);
    }
    
    /**
     * Increment iteration count for current loop
     */
    public static void incrementIteration() {
        int loopId = getCurrentLoopId();
        if (loopId == 0) return;
        
        LoopStats stats = loopStats.get().get(loopId);
        if (stats != null) {
            stats.iterationCount++;
        }
    }
    
    /**
     * Get all arrays used in a specific loop
     */
    public static Set<Integer> getArraysInLoop(int loopId) {
        Set<Integer> arrays = loopArrays.get().get(loopId);
        return arrays != null ? new HashSet<Integer>(arrays) : new HashSet<Integer>();
    }
    
    /**
     * Get statistics for a loop
     */
    public static LoopStats getLoopStats(int loopId) {
        return loopStats.get().get(loopId);
    }
    
    /**
     * Clear all tracking data
     */
    public static void clearAll() {
        loopArrays.get().clear();
        while (!loopStack.get().isEmpty()) {
            loopStack.get().pop();
        }
        loopStats.get().clear();
        optimizedLoops.clear();
        arrayRegistry.clear();
    }
    
    /**
     * Check if any arrays are shared between loops
     */
    public static Set<Integer> findSharedArrays() {
        Map<Integer, Set<Integer>> arrayToLoops = new HashMap<Integer, Set<Integer>>();
        Set<Integer> shared = new HashSet<Integer>();
        
        for (Map.Entry<Integer, Set<Integer>> entry : loopArrays.get().entrySet()) {
            int loopId = entry.getKey();
            for (int arrayId : entry.getValue()) {
                Set<Integer> loops = arrayToLoops.get(arrayId);
                if (loops == null) {
                    loops = new HashSet<Integer>();
                    arrayToLoops.put(arrayId, loops);
                }
                loops.add(loopId);
                if (loops.size() > 1) {
                    shared.add(arrayId);
                }
            }
        }
        
        return shared;
    }
    
    /**
     * Print tracking summary for current thread
     */
    public static void printSummary() {
        System.out.println("\n=== ARRAY TRACKER SUMMARY ===");
        
        Map<Integer, LoopStats> stats = loopStats.get();
        if (stats.isEmpty()) {
            System.out.println("No loops tracked.");
            return;
        }
        
        for (LoopStats stat : stats.values()) {
            System.out.println(stat);
        }
        
        Set<Integer> shared = findSharedArrays();
        if (!shared.isEmpty()) {
            System.out.println("\nShared arrays between loops: " + shared);
        }
        
        System.out.println("==============================\n");
    }
}