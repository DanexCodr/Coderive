package cod.compiler;

import java.util.*;

public class InterferenceGraph {
    public final Map<String, Set<String>> adjList = new HashMap<String, Set<String>>();
    public final Map<String, Integer> degrees = new HashMap<String, Integer>();
    
    // NOTE: This will be injected/called by MTOTNativeCompiler 
    // to provide the necessary register sets (x9-x17 for AArch64)
    public Set<String> callerSavedRegisters = Collections.emptySet(); 

    public void addNode(String node) {
        if (!adjList.containsKey(node)) {
            adjList.put(node, new HashSet<String>());
            degrees.put(node, 0);
        }
    }

    public void addEdge(String u, String v) {
        if (u.equals(v)) return;
        addNode(u);
        addNode(v);
        if (adjList.get(u).add(v)) {
            degrees.put(u, degrees.get(u) + 1);
        }
        if (adjList.get(v).add(u)) {
            degrees.put(v, degrees.get(v) + 1);
        }
    }

    public void buildFromCFG(List<BasicBlock> blocks) {
        for (BasicBlock block : blocks) {
            Set<String> liveNow = new HashSet<String>(block.liveOut);
            
            for (int i = block.instructions.size() - 1; i >= 0; i--) {
                TACInstruction instr = block.instructions.get(i);
                
                String def = instr.getDef();
                List<String> uses = instr.getUses();

                if (def != null) {
                    addNode(def);
                    for (String liveVar : liveNow) {
                        addEdge(def, liveVar);
                    }
                    liveNow.remove(def);
                }

                if (instr.opcode == TACInstruction.Opcode.CALL || 
                    instr.opcode == TACInstruction.Opcode.CALL_SLOTS) {
                    
                    // CRITICAL FIX: Add interference between live variables 
                    // and ALL caller-saved registers (register clobbering model)
                    addCallInterferences(liveNow);
                }

                for (String use : uses) {
                    addNode(use);
                    liveNow.add(use);
                }
            }
        }
    }
    
    /**
     * Models the fact that a function call clobbers all caller-saved registers.
     * Any variable currently live (in liveNow) must interfere with all 
     * caller-saved registers to ensure it is allocated to a callee-saved 
     * register or spilled to the stack.
     */
    private void addCallInterferences(Set<String> liveNow) {
        for (String liveVar : liveNow) {
            for (String clobberReg : callerSavedRegisters) {
                // Add the caller-saved register as a pseudo-node in the graph
                addNode(clobberReg); 
                // Add an edge from the live variable to the clobbered register
                addEdge(liveVar, clobberReg);
            }
        }
    }
}