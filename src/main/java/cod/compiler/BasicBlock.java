package cod.compiler;

import java.util.*;

public class BasicBlock {
    public int id;
    public List<TACInstruction> instructions = new ArrayList<TACInstruction>();
    
    // Graph edges
    public List<BasicBlock> predecessors = new ArrayList<BasicBlock>();
    public List<BasicBlock> successors = new ArrayList<BasicBlock>();

    // Liveness Sets
    public Set<String> def = new HashSet<String>();
    public Set<String> use = new HashSet<String>();
    public Set<String> liveIn = new HashSet<String>();
    public Set<String> liveOut = new HashSet<String>();

    public BasicBlock(int id) {
        this.id = id;
    }

    public void addInstruction(TACInstruction instr) {
        instructions.add(instr);
    }
    
    @Override
    public String toString() {
        return "Block " + id;
    }
}