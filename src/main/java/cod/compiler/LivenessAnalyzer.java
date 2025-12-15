package cod.compiler;

import cod.compiler.TACInstruction.Opcode;
import java.util.*;

public class LivenessAnalyzer {

    public List<BasicBlock> buildCFG(List<TACInstruction> instructions) {
        List<BasicBlock> blocks = new ArrayList<BasicBlock>();
        if (instructions.isEmpty()) return blocks;

        Map<String, BasicBlock> labelToBlock = new HashMap<String, BasicBlock>();
        BasicBlock currentBlock = new BasicBlock(0);
        blocks.add(currentBlock);

        for (int i = 0; i < instructions.size(); i++) {
            TACInstruction instr = instructions.get(i);
            
            if (instr.opcode == Opcode.LABEL) {
                if (!currentBlock.instructions.isEmpty()) {
                    currentBlock = new BasicBlock(blocks.size());
                    blocks.add(currentBlock);
                }
                labelToBlock.put((String)instr.operand1, currentBlock);
            }
            
            currentBlock.addInstruction(instr);

            if (isBranchOrTerminator(instr)) {
                if (i + 1 < instructions.size()) {
                    currentBlock = new BasicBlock(blocks.size());
                    blocks.add(currentBlock);
                }
            }
        }

        // Compute Local Liveness (Def/Use)
        for (BasicBlock block : blocks) {
            computeLocalLiveness(block);
        }

        // Connect Edges
        for (int i = 0; i < blocks.size(); i++) {
            BasicBlock block = blocks.get(i);
            if (block.instructions.isEmpty()) continue;

            TACInstruction last = block.instructions.get(block.instructions.size() - 1);

            if (last.opcode == Opcode.GOTO) {
                String targetLabel = (String) last.operand1;
                connect(block, labelToBlock.get(targetLabel));
            } 
            else if (last.opcode == Opcode.IF_GOTO) {
                // Operand2 holds the label in our compiler's emission logic
                String targetLabel = (String) last.operand2; 
                if (i + 1 < blocks.size()) connect(block, blocks.get(i + 1)); // Fallthrough
                if (targetLabel != null) connect(block, labelToBlock.get(targetLabel)); // Branch
            } 
            else if (last.opcode == Opcode.RET) {
                // No successors
            } 
            else {
                if (i + 1 < blocks.size()) connect(block, blocks.get(i + 1));
            }
        }
        
        return blocks;
    }

    private void connect(BasicBlock from, BasicBlock to) {
        if (from != null && to != null) {
            from.successors.add(to);
            to.predecessors.add(from);
        }
    }

    private boolean isBranchOrTerminator(TACInstruction instr) {
        switch (instr.opcode) {
            case GOTO: case IF_GOTO: case RET: return true;
            default: return false;
        }
    }

    private void computeLocalLiveness(BasicBlock block) {
        block.def.clear();
        block.use.clear();
        
        for (TACInstruction instr : block.instructions) {
            List<String> uses = instr.getUses();
            String def = instr.getDef();

            for (String u : uses) {
                if (!block.def.contains(u)) {
                    block.use.add(u);
                }
            }
            if (def != null) {
                block.def.add(def);
            }
        }
    }

    public void computeGlobalLiveness(List<BasicBlock> blocks) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = blocks.size() - 1; i >= 0; i--) {
                BasicBlock block = blocks.get(i);

                Set<String> newLiveOut = new HashSet<String>();
                for (BasicBlock succ : block.successors) {
                    newLiveOut.addAll(succ.liveIn);
                }

                Set<String> newLiveIn = new HashSet<String>(newLiveOut);
                newLiveIn.removeAll(block.def);
                newLiveIn.addAll(block.use);

                if (!newLiveIn.equals(block.liveIn) || !newLiveOut.equals(block.liveOut)) {
                    block.liveIn = newLiveIn;
                    block.liveOut = newLiveOut;
                    changed = true;
                }
            }
        }
    }
}