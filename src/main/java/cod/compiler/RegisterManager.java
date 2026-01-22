package cod.compiler;

import java.util.*;

public class RegisterManager {
    private final LivenessAnalyzer analyzer;
    private final GraphColoringAllocator graphAllocator;
    private final RegisterSpiller registerSpiller;
    private final MTOTRegistry.RegisterFile registerFile;

    private Map<String, String> currentAllocation = new HashMap<String, String>();

    public RegisterManager(MTOTNativeCompiler compiler) {
        this.registerFile = compiler.cpuProfile.registerFile;
        this.registerSpiller = new RegisterSpiller();
        this.analyzer = new LivenessAnalyzer();
        this.graphAllocator = new GraphColoringAllocator(registerFile, registerSpiller);
    }

    // NEW METHOD: Used by MTOTNativeCompiler to set the registers that CALLs clobber.
    public void setCallerSavedRegisters(Set<String> registers) {
        this.graphAllocator.getInterferenceGraph().callerSavedRegisters = registers;
    }

    public void runAllocation(List<TACInstruction> instructions) {
        registerSpiller.reset();
        
        List<BasicBlock> blocks = analyzer.buildCFG(instructions);
        analyzer.computeGlobalLiveness(blocks);
        
        InterferenceGraph graph = new InterferenceGraph();
        
        // Ensure the graph used for building is the one referenced by the allocator 
        // if they were separate, but since we are replacing the call here:
        graph.callerSavedRegisters = this.graphAllocator.getInterferenceGraph().callerSavedRegisters;
        
        graph.buildFromCFG(blocks);
        
        this.currentAllocation = graphAllocator.allocate(graph);
    }

    public String getRegister(String temp) {
        return currentAllocation.get(temp);
    }

    public RegisterSpiller getSpiller() {
        return registerSpiller;
    }

    public void reset() {
        registerSpiller.reset();
        currentAllocation.clear();
    }

    public static class RegisterSpiller {
        private final Map<String, Integer> spillSlots = new HashMap<String, Integer>();
        // FIX: Start offset at -24 to clear the 16 bytes of saved FP/LR.
        private int nextSpillOffset = -24; 
        private int totalSpillSize = 0;

        public RegisterSpiller() {
        }

        public void reset() {
            spillSlots.clear();
            totalSpillSize = 0;
            // Reset to the new starting offset
            nextSpillOffset = -24; 
        }

        public void forceSpill(String temp) {
            if (!spillSlots.containsKey(temp)) {
                spillSlots.put(temp, nextSpillOffset);
                totalSpillSize += 8;
                nextSpillOffset -= 8;
            }
        }

        public Integer getSpillOffset(String temp) {
            return spillSlots.get(temp);
        }

        public int getTotalSpillSize() {
            return (totalSpillSize + 15) & ~15;
        }
    }
}