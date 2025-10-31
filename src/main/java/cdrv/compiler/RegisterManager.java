package cdrv.compiler;

import static cdrv.compiler.MTOTRegistry.CPUProfile.*;
import static cdrv.compiler.MTOTRegistry.AArch64Registers.*;
import static cdrv.compiler.MTOTRegistry.*;
import cdrv.compiler.BytecodeInstruction.Opcode; // Import Opcode
import cdrv.debug.DebugSystem;
import java.util.*;

public class RegisterManager {
    private final RegisterAllocator registerAllocator;
    private final RegisterSpiller registerSpiller;
    private final RegisterFile registerFile; // Store reference

    public RegisterManager(MTOTNativeCompiler compiler) {
        // --- MODIFIED: Pass registerFile directly ---
        this.registerFile = compiler.cpuProfile.registerFile;
        // --- END MODIFIED ---
        this.registerSpiller = new RegisterSpiller(compiler);
        this.registerAllocator = new RegisterAllocator(this.registerFile, registerSpiller);
    }

    public RegisterAllocator getAllocator() {
        return registerAllocator;
    }

    public RegisterSpiller getSpiller() {
        return registerSpiller;
    }

    public void reset() {
        registerAllocator.reset();
        registerSpiller.reset();
    }

public class RegisterAllocator {
    private final RegisterFile registerFile;
    private final Stack<String> availableRegisters = new Stack<String>();
    // --- MODIFIED: Use LinkedHashSet for predictable iteration ---
    final LinkedHashSet<String> usedRegisters = new LinkedHashSet<String>();
    // --- END MODIFIED ---
    private final RegisterSpiller spiller;

    RegisterAllocator(RegisterFile registerFile, RegisterSpiller spiller) {
        this.registerFile = registerFile;
        this.spiller = spiller;
        reset();
    }

    public Set<String> getUsedRegisters() {
        return new LinkedHashSet<String>(usedRegisters); // Return a copy
    }

    public void reset() {
        availableRegisters.clear();
        usedRegisters.clear();
        List<String> gp = new ArrayList<String>(registerFile.generalPurpose);
        Collections.reverse(gp); // Prefer higher numbered regs first
        availableRegisters.addAll(gp);
        if (spiller != null) spiller.reset();
        DebugSystem.debug("MTOT_REG", "Allocator reset. Available: " + availableRegisters.size());
    }

    public String allocateRegister() {
        return allocateRegister(Collections.<String>emptySet());
    }

    public String allocateRegister(Set<String> avoidSpilling) {
        if (availableRegisters.isEmpty()) {
            DebugSystem.warn("MTOT_REG", "Out of registers! Requesting spill... Used: " + usedRegisters + ". Avoid: " + avoidSpilling);
            if (spiller == null) {
                throw new RuntimeException("Out of registers and no spiller configured!");
            }
            // --- MODIFICATION: Pass current used registers to spiller ---
            String freedRegister = spiller.spillRegister(new LinkedHashSet<>(usedRegisters), avoidSpilling);
            // --- END MODIFICATION ---

            if (freedRegister == null || !usedRegisters.contains(freedRegister)) {
                 // Spiller might return null if it decides not to spill (e.g., only avoided regs left)
                 // Or, if usedRegisters was somehow modified concurrently (less likely here)
                 // Let's try to grab *any* available register again just in case spiller logic changes
                 if (!availableRegisters.isEmpty()) {
                     DebugSystem.warn("MTOT_REG", "Spiller didn't free a used register as expected, but one became available. Using that.");
                     // Proceed to pop from availableRegisters below
                 } else if (freedRegister != null && !usedRegisters.contains(freedRegister)) {
                     // Spiller returned something, but it wasn't marked as used? State inconsistency.
                     DebugSystem.error("MTOT_REG", "Spiller returned register " + freedRegister + " which was not in the used set! State inconsistent. Used: " + usedRegisters);
                     availableRegisters.push(freedRegister); // Put it back, maybe it helps
                     // Attempt allocation again below, might throw
                 } else { // freedRegister is null
                     throw new RuntimeException("Spiller failed to provide a register to spill (returned null)! Used: " + usedRegisters + ", Avoid: " + avoidSpilling);
                 }
            } else {
                // Successfully spilled, remove from used and add to available
                usedRegisters.remove(freedRegister);
                if (!availableRegisters.contains(freedRegister)) { // Avoid duplicates
                    availableRegisters.push(freedRegister);
                }
                DebugSystem.debug("MTOT_REG", "Spiller successfully freed register: " + freedRegister + ". Re-allocating.");
            }

            // Check again if a register is available now
            if (availableRegisters.isEmpty()) {
                 throw new RuntimeException("Allocation failed: No registers available even after spill attempt! Used: " + usedRegisters + ", Avoid: " + avoidSpilling);
            }
        }

        String reg = availableRegisters.pop();
        usedRegisters.add(reg); // Add maintains insertion order in LinkedHashSet
        if (spiller != null) spiller.trackRegisterUsage(reg); // Let spiller know it's used now
        DebugSystem.debug("MTOT_REG", "Allocated register: " + reg + ". Used: " + usedRegisters + ". Available: " + availableRegisters.size());
        return reg;
    }

    public void freeRegister(String register) {
        if (register == null) return;

        // Check if it's actually marked as used
        if (usedRegisters.contains(register)) {
            usedRegisters.remove(register); // Remove maintains order for others
            if (registerFile.generalPurpose.contains(register)) {
                // Only push back GP registers to the available pool
                if (!availableRegisters.contains(register)) { // Avoid duplicates
                    availableRegisters.push(register);
                }
                if (spiller != null) spiller.untrackRegisterUsage(register); // Notify spiller it's free
                DebugSystem.debug("MTOT_REG", "Freed register: " + register + ". Used: " + usedRegisters + ". Available: " + availableRegisters.size());
            } else {
                 // If it's not GP (e.g., arg reg, FP, SP), just removing it from used is enough.
                 DebugSystem.debug("MTOT_REG", "Unmarked non-GP register as used: " + register + ". Used: " + usedRegisters);
            }
        }
        // --- MODIFICATION: Handle freeing registers not marked used (e.g., after call) ---
        else if (registerFile.generalPurpose.contains(register)) {
            // If it's GP but wasn't marked used, still ensure it's available
             if (!availableRegisters.contains(register)) {
                 availableRegisters.push(register);
                 DebugSystem.debug("MTOT_REG", "Ensured non-used GP register is available: " + register);
             }
             if (spiller != null) spiller.untrackRegisterUsage(register); // Ensure spiller knows too
        }
        // --- END MODIFICATION ---
        else if (!registerFile.argumentRegisters.contains(register) &&
                 !register.equals(registerFile.stackPointer) &&
                 !register.equals(registerFile.framePointer)) {
             // Warn only for truly unknown registers
            DebugSystem.warn("MTOT_REG", "Attempted to free unknown/invalid register: " + register);
        }
    }


    public void markRegisterUsed(String register) {
        if (register == null) return;

        // Check if it's a register we track (GP, Arg, SP, FP)
         if (registerFile.generalPurpose.contains(register) ||
             registerFile.argumentRegisters.contains(register) ||
             register.equals(registerFile.stackPointer) ||
             register.equals(registerFile.framePointer) ||
             register.equals(AArch64Registers.x19)) { // Include 'this' explicitly if needed

            boolean wasAvailable = availableRegisters.remove(register);
            boolean added = usedRegisters.add(register); // Add or update order in LinkedHashSet

            if (added || wasAvailable) { // If it was newly added or just moved from available
                 if (spiller != null && registerFile.generalPurpose.contains(register)) {
                     spiller.trackRegisterUsage(register); // Track its usage for LRU
                 }
                 DebugSystem.debug("MTOT_REG", "Marked register as used: " + register + ". Used: " + usedRegisters);
            }
        } else {
            DebugSystem.warn("MTOT_REG", "Attempted to mark unknown/invalid register as used: " + register);
        }
    }
}


// =======================================================
// ===     RegisterSpiller (Hybrid "Future Cost")      ===
// =======================================================
public class RegisterSpiller {
    private final MTOTNativeCompiler compiler;
    private final RegisterFile registerFile;
    private final Map<String, Integer> spillSlots = new HashMap<>(); // Register -> Stack Offset
    private final LinkedHashSet<String> usageOrder = new LinkedHashSet<>(); // LRU (first) to MRU (last)
    private int nextSpillOffset = -8;
    private int totalSpillSize = 0;
    private final Map<Integer, String> slotToRegisterMap = new HashMap<>(); // SlotIndex -> Register
    // --- NEW: Track loop depth ---
    private final Map<String, Integer> registerLoopDepth = new HashMap<>(); // Register -> Loop depth at definition
    // --- END NEW ---
    private final Set<String> writtenRegistersInCurrentMethod = new HashSet<>();

    public RegisterSpiller(MTOTNativeCompiler compiler) {
        this.compiler = compiler;
        this.registerFile = compiler.cpuProfile.registerFile;
    }

    // --- NEW: Method to update loop depth for a register ---
    public void updateRegisterDefinitionDepth(String register, int depth) {
        if (register != null && registerFile.generalPurpose.contains(register)) {
            registerLoopDepth.put(register, depth);
            // DebugSystem.debug("SPILL_COST", "Updated loop depth for " + register + " to " + depth);
        }
    }
    // --- END NEW ---

    // --- REVISED: spillRegister using "Future Cost" heuristic ---
    public String spillRegister(LinkedHashSet<String> currentlyUsedRegisters, Set<String> avoidSpilling) {
        if (currentlyUsedRegisters.isEmpty()) {
            DebugSystem.error("SPILL", "Spill requested but no registers are marked as used!");
            return null;
        }

        String bestVictim = null;
        double bestScore = -1.0; // Higher score is better (spill this one)

        String fallbackVictim = null; // LRU overall
        double fallbackScore = -1.0;

        List<BytecodeInstruction> bytecode = compiler.getCurrentMethodBytecode(); // Get current method's bytecode
        int currentPc = compiler.getCurrentPc();                     // Get current instruction index

        // Iterate through usageOrder (LRU first) to find candidates
        for (String reg : usageOrder) {
            if (!currentlyUsedRegisters.contains(reg)) continue; // Only consider registers currently holding a needed value

            int nextUse = estimateNextUseDistance(reg, currentPc, bytecode);
            int cost = estimateSpillCost(reg);

            // Score: Maximize nextUse distance, minimize cost (loop depth)
            // Higher score means it's used further away OR less costly to reload
            double score = (double) nextUse / (cost + 0.001); // Add epsilon to avoid div by zero

            // Track the absolute LRU candidate as fallback
            if (fallbackVictim == null) {
                fallbackVictim = reg;
                fallbackScore = score;
            }

            // If this register is explicitly requested to be avoided, skip primary selection
            if (avoidSpilling != null && avoidSpilling.contains(reg)) {
                DebugSystem.debug("SPILL", "Skipping " + reg + " (in avoid set). Score: " + score);
                continue;
            }

            // Is this the best candidate *so far* that we're allowed to pick?
            if (bestVictim == null || score > bestScore) {
                bestScore = score;
                bestVictim = reg;
                DebugSystem.debug("SPILL", "New best candidate: " + reg + " (Score: " + score + ", NextUse: " + nextUse + ", Cost: " + cost + ")");
            } else {
                 DebugSystem.debug("SPILL", "Considering " + reg + " (Score: " + score + ", NextUse: " + nextUse + ", Cost: " + cost + ") - Not better than " + bestVictim);
            }
        }

        // If we didn't find any suitable victim outside the avoid set
        if (bestVictim == null) {
            if (fallbackVictim != null) {
                 DebugSystem.warn("SPILL", "Forced to spill fallback LRU victim " + fallbackVictim + " (Score: " + fallbackScore + ") because best options were in avoid set.");
                 bestVictim = fallbackVictim; // Use the absolute LRU we tracked
            } else {
                 DebugSystem.error("SPILL", "Cannot determine any spill victim! Used: " + currentlyUsedRegisters + ", UsageOrder: " + usageOrder);
                 return null; // Should not happen
            }
        } else {
            DebugSystem.debug("SPILL", "Selected final victim " + bestVictim + " (Score: " + bestScore + ")");
        }

        forceSpill(bestVictim);
        return bestVictim;
    }
    // --- END REVISED ---

    // --- NEW: Helper - Estimate next use distance (simplified) ---
    private int estimateNextUseDistance(String register, int currentPc, List<BytecodeInstruction> bytecode) {
        if (bytecode == null) return Integer.MAX_VALUE; // Safety check

        Integer slotIndex = getSlotIndexForRegister(register);
        if (slotIndex == null) {
            // If we don't know which variable this register holds (e.g., temporary),
            // assume it might be needed soon (give it a low distance -> high spill priority if cost is equal)
            return 1;
        }

        int lookaheadLimit = currentPc + 50; // Limit scan distance
        for (int i = currentPc + 1; i < bytecode.size() && i < lookaheadLimit; i++) {
            BytecodeInstruction instr = bytecode.get(i);
            // Check if this instruction READS the slot associated with our register
            if (readsSlotIndex(instr, slotIndex)) {
                return i - currentPc; // Return distance
            }
            // Stop scan at basic block boundaries (jumps, calls, returns, labels)
            if (isBasicBlockEnd(instr)) {
                break;
            }
        }
        return Integer.MAX_VALUE; // Not found within lookahead or basic block
    }

    // Helper to find the slot index currently held by a register
    private Integer getSlotIndexForRegister(String register) {
        for (Map.Entry<Integer, String> entry : slotToRegisterMap.entrySet()) {
            if (register.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    // Helper to check if an instruction is likely to read a specific local slot
    private boolean readsSlotIndex(BytecodeInstruction instr, int slotIndex) {
        // This is a heuristic! More accurate analysis would need stack simulation.
        switch (instr.opcode) {
            case LOAD_LOCAL:
                // LOAD_LOCAL *defines* a value, it doesn't read the *previous* value of that slot.
                // However, if the instruction *itself* is loading our target slot,
                // it means the value *currently* in the register will be overwritten.
                // We consider this a "use" in the sense that the current register value is needed *before* this instruction.
                return instr.operand instanceof Integer && ((Integer) instr.operand).intValue() == slotIndex;

            // Instructions that read 1 value (potentially from our slot if it's on top)
            case POP:
            case STORE_LOCAL: // Reads value to store
            case STORE_FIELD:
            case STORE_SLOT:
            case PRINT:
            case NEG_INT:
            case INT_TO_STRING:
            case JMP_IF_TRUE:
            case JMP_IF_FALSE:
                 // Approximation: Assume it might read our slot if it reads the top of the stack.
                 // We can't know for sure without stack tracking. For simplicity, return false.
                 // A more complex check could see if LOAD_LOCAL slotIndex immediately preceded this.
                return false;

            // Instructions that read 2 values
            case ADD_INT: case SUB_INT: case MUL_INT: case DIV_INT: case MOD_INT:
            case CMP_EQ_INT: case CMP_NE_INT: case CMP_LT_INT: case CMP_LE_INT: case CMP_GT_INT: case CMP_GE_INT:
            case CONCAT_STRING:
            case ARRAY_LOAD: // Reads array ref and index
            case ARRAY_STORE: // Reads array ref, index, and value (reads 3, but simplifies logic)
                 // Approximation: Assume it might read our slot if it reads top 2 stack items. Return false.
                return false;

             // Reads arguments based on arg count - complex
             case CALL:
             case CALL_SLOTS:
                return false; // Too complex for simple lookahead

            default:
                return false;
        }
    }

    // Helper to identify instructions ending a basic block
    private boolean isBasicBlockEnd(BytecodeInstruction instr) {
        switch (instr.opcode) {
            case JMP:
            case JMP_IF_TRUE:
            case JMP_IF_FALSE:
            case RET:
            case CALL:
            case CALL_SLOTS:
            case LABEL: // Start of a new block
                return true;
            default:
                return false;
        }
    }
    // --- END NEW ---

    // --- NEW: Helper - Estimate spill cost ---
    private int estimateSpillCost(String register) {
        int depth = registerLoopDepth.getOrDefault(register, 0);
        // Heavily penalize spilling registers defined inside loops
        // Cost = 1 (base) + depth^2 * 10 (example weighting)
        return 1 + (depth * depth * 10);
    }
    // --- END NEW ---


    public void mapSlotToRegister(int slotIndex, String register) {
        slotToRegisterMap.put(slotIndex, register);
        DebugSystem.debug("SPILL_MAP", "Mapped slot " + slotIndex + " -> register " + register);
    }

    public int getSpillOffsetForSlotIndex(int slotIndex) {
        String register = slotToRegisterMap.get(slotIndex);
        if (register == null) {
             DebugSystem.debug("SPILL_MAP", "No register mapped for slot " + slotIndex + ", cannot get spill offset.");
            return Integer.MIN_VALUE;
        }
        Integer offset = spillSlots.get(register);
        if (offset == null) {
             DebugSystem.debug("SPILL_MAP", "Register " + register + " (for slot "+slotIndex+") has no spill slot assigned.");
            return Integer.MIN_VALUE;
        }
        return offset.intValue();
    }


    public void forceSpill(String register) {
        if (register == null || !registerFile.generalPurpose.contains(register)) {
             DebugSystem.debug("SPILL", "Attempted to force spill non-GP register: " + register + " - Skipping.");
             return; // Only spill general-purpose registers
        }
        int offset = getOrCreateSpillSlot(register);
        DebugSystem.debug("SPILL", "Force spilling register " + register + " to stack offset [fp" + offset + "]");
        compiler.generateSpillCode(register, offset);
        usageOrder.remove(register); // Remove from usage tracking as its value is now safely on stack
        // REMOVED: registersOnStack.add(register);
    }


    // --- MAJOR REVISION (Kept from previous version): Always load if spill slot exists ---
    public void fillRegister(String register) {
         if (register == null || !registerFile.generalPurpose.contains(register)) {
            // Don't attempt to fill non-GP registers (like args, sp, fp)
            return;
         }

        Integer offset = spillSlots.get(register);
        // If a spill slot *exists* for this register, we MUST reload.
        if (offset != null) {
            DebugSystem.debug("SPILL", "Filling register " + register + " from stack offset [fp" + offset + "] (spill slot exists).");
            compiler.generateFillCode(register, offset.intValue());
            // REMOVED: registersOnStack.remove(register);
            trackRegisterUsage(register); // Mark as recently used *after* filling
        } else {
             // No spill slot exists. Assume the register holds a valid value
             // OR this is its first use and it will be written to shortly.
             // We still track its usage if it was potentially used without spilling.
             trackRegisterUsage(register);
             DebugSystem.debug("SPILL", "Register " + register + " has no spill slot, assuming value is live or will be written.");
        }
    }
    // --- END MAJOR REVISION ---

    public void trackRegisterUsage(String register) {
        if (register != null && registerFile.generalPurpose.contains(register)) {
            usageOrder.remove(register); // Remove if exists
            usageOrder.add(register);    // Add to end (MRU)
        }
    }

    public void untrackRegisterUsage(String register) {
        if (register != null) {
            usageOrder.remove(register);
            // REMOVED: registersOnStack.remove(register);
        }
    }

    private int getOrCreateSpillSlot(String register) {
        Integer offset = spillSlots.get(register);
        if (offset == null) {
            offset = Integer.valueOf(nextSpillOffset);
            spillSlots.put(register, offset);
            totalSpillSize += 8; // Assuming 64-bit registers
            DebugSystem.debug("SPILL", "Allocated spill slot for " + register + " at offset [fp" + nextSpillOffset + "]. Total spill size (raw): " + totalSpillSize);
            nextSpillOffset -= 8;
        }
        return offset.intValue();
    }

    public int getTotalSpillSize() {
        // Ensure 16-byte alignment for the stack frame
        return (totalSpillSize + 15) & ~15;
    }

    // --- MAJOR REVISION (Kept from previous version): Update stack immediately if needed ---
    public void markRegisterModified(String register) {
        if (register == null || !registerFile.generalPurpose.contains(register)) {
            return; // Only track modifications to GP registers we manage/spill
        }
        trackRegisterUsage(register); // Update LRU status
        writtenRegistersInCurrentMethod.add(register); // Track that it was written in this method

        // Check if this register has a designated spill slot.
        Integer offset = spillSlots.get(register);
        if (offset != null) {
            // If it has a spill slot, the value on the stack is now potentially stale.
            // Force spill the *new* value immediately to keep the stack consistent.
             DebugSystem.debug("SPILL", "Register " + register + " modified, forcing update to its spill slot [fp" + offset + "]");
             forceSpill(register); // forceSpill handles generating code and updating state
        } else {
             // If it doesn't have a spill slot, no need to update the stack yet.
             DebugSystem.debug("SPILL", "Register " + register + " modified (no spill slot yet).");
        }
        // REMOVED: registersOnStack.remove(register); // This line is now redundant
    }
    // --- END MAJOR REVISION ---


    public Set<String> getWrittenRegistersDuringCompilation() {
        return new HashSet<String>(writtenRegistersInCurrentMethod);
    }

    public void reset() {
        spillSlots.clear();
        usageOrder.clear();
        slotToRegisterMap.clear();
        registerLoopDepth.clear(); // <-- NEW: Clear depth map
        // REMOVED: registersOnStack.clear();
        writtenRegistersInCurrentMethod.clear();
        nextSpillOffset = -8; // Will be set correctly by setBaseSpillOffset
        totalSpillSize = 0;
        DebugSystem.debug("SPILL", "Spiller reset.");
    }

    public void setBaseSpillOffset(int baseOffset) {
        // Base offset is typically the negative offset of the lowest callee-saved register stored.
        // Spills start 8 bytes below that.
        this.nextSpillOffset = baseOffset - 8;
        DebugSystem.debug("SPILL", "Spiller base offset set relative to FP: " + baseOffset + ". Next spill slot at: [fp" + this.nextSpillOffset + "]");
    }

    // This method might still be useful if loading from a *known fixed offset* (like arguments passed on stack)
    // For general spilling/filling, rely on the main fillRegister logic.
    public void fillRegisterFromOffset(String register, int offset) {
         if (register == null) return;
        DebugSystem.debug("SPILL", "Explicitly filling register " + register + " from known stack offset [fp" + offset + "]");
        compiler.generateFillCode(register, offset);
        // REMOVED: registersOnStack.remove(register);
        trackRegisterUsage(register); // Mark as recently used
        updateRegisterDefinitionDepth(register, compiler.getCurrentLoopDepth()); // Also update depth on explicit fill
    }
}

}