package cod.compiler;

import cod.debug.DebugSystem;
import java.util.*;

public class GraphColoringAllocator {
    private final MTOTRegistry.RegisterFile registerFile;
    private final RegisterManager.RegisterSpiller spiller;
    // NEW: Store a reference to the InterferenceGraph
    private InterferenceGraph interferenceGraph; 

    public GraphColoringAllocator(MTOTRegistry.RegisterFile registerFile, RegisterManager.RegisterSpiller spiller) {
        this.registerFile = registerFile;
        this.spiller = spiller;
        // Initialize the graph so it exists when RegisterManager calls the setter
        this.interferenceGraph = new InterferenceGraph(); 
    }
    
    // NEW METHOD: Allows RegisterManager to access the graph for ABI setup
    public InterferenceGraph getInterferenceGraph() {
        return interferenceGraph;
    }

    public Map<String, String> allocate(InterferenceGraph graph) {
        // ASSIGN THE INPUT GRAPH TO THE MEMBER VARIABLE BEFORE PROCEEDING
        this.interferenceGraph = graph; 
        
        DebugSystem.info("ALLOC", "Coloring graph with " + graph.adjList.size() + " nodes");

        Stack<String> stack = new Stack<String>();
        Set<String> spilledNodes = new HashSet<String>();
        
        // Deep copy graph for manipulation
        Map<String, Set<String>> workingGraph = new HashMap<String, Set<String>>();
        Map<String, Integer> workingDegrees = new HashMap<String, Integer>();
        
        for (Map.Entry<String, Set<String>> entry : graph.adjList.entrySet()) {
            workingGraph.put(entry.getKey(), new HashSet<String>(entry.getValue()));
            workingDegrees.put(entry.getKey(), graph.degrees.get(entry.getKey()));
        }

        int K = registerFile.generalPurpose.size();

        // SIMPLIFY
        int numNodes = workingGraph.size();
        while (stack.size() + spilledNodes.size() < numNodes) {
            String nodeToRemove = null;
            
            // Find trivially colorable node
            for (Map.Entry<String, Integer> entry : workingDegrees.entrySet()) {
                if (entry.getValue() != -1 && entry.getValue() < K) {
                    nodeToRemove = entry.getKey();
                    break;
                }
            }
            
            // Optimistic Spill
            if (nodeToRemove == null) {
                int maxDegree = -1;
                for (Map.Entry<String, Integer> entry : workingDegrees.entrySet()) {
                    if (entry.getValue() != -1 && entry.getValue() > maxDegree) {
                        maxDegree = entry.getValue();
                        nodeToRemove = entry.getKey();
                    }
                }
            }

            if (nodeToRemove == null) break;

            stack.push(nodeToRemove);
            workingDegrees.put(nodeToRemove, -1);
            
            for (String neighbor : workingGraph.get(nodeToRemove)) {
                if (workingDegrees.get(neighbor) != -1) {
                    workingDegrees.put(neighbor, workingDegrees.get(neighbor) - 1);
                }
            }
        }

        // SELECT
        Map<String, String> assignments = new HashMap<String, String>();
        
        while (!stack.isEmpty()) {
            String node = stack.pop();
            Set<String> usedColors = new HashSet<String>();
            
            if (graph.adjList.containsKey(node)) {
                for (String neighbor : graph.adjList.get(node)) {
                    if (assignments.containsKey(neighbor)) {
                        usedColors.add(assignments.get(neighbor));
                    }
                }
            }
            
            String chosenColor = null;
            for (String reg : registerFile.generalPurpose) {
                if (!usedColors.contains(reg)) {
                    chosenColor = reg;
                    break;
                }
            }
            
            if (chosenColor != null) {
                assignments.put(node, chosenColor);
            } else {
                spiller.forceSpill(node);
                spilledNodes.add(node);
            }
        }
        
        return assignments;
    }
}