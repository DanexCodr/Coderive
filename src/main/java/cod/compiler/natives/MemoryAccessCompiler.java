package cod.compiler.natives;

import cod.compiler.MTOTNativeCompiler;
import cod.compiler.RegisterManager;

import cod.debug.DebugSystem;

import java.util.*;

public class MemoryAccessCompiler {
    private final MTOTNativeCompiler compiler;
    private final cod.compiler.natives.OperandStack operandStack;
    private final RegisterManager.RegisterAllocator registerAllocator;
    private final RegisterManager.RegisterSpiller spiller;
    
    private Map<String, Integer> fieldOffsets = new HashMap<String, Integer>();
    private int nextFieldOffset = 0;
    private Map<Integer, String> slotLocations = new HashMap<Integer, String>();
    private Map<String, String> stringLiteralLabels = new HashMap<String, String>();

    public MemoryAccessCompiler(MTOTNativeCompiler compiler) {
        this.compiler = compiler;
        this.operandStack = compiler.operandStack;
        this.registerAllocator = compiler.registerManager.getAllocator();
        this.spiller = compiler.registerManager.getSpiller();
    }

    public void reset() {
        fieldOffsets.clear();
        nextFieldOffset = 0;
        slotLocations.clear();
        stringLiteralLabels.clear();
    }

    public Map<Integer, String> getSlotLocations() {
        return new HashMap<Integer, String>(slotLocations);
    }

    private int getFieldOffset(String fieldName) {
        if (!fieldOffsets.containsKey(fieldName)) {
            fieldOffsets.put(fieldName, nextFieldOffset);
            cod.debug.DebugSystem.debug("MTOT_FIELDS", "Assigned offset " + nextFieldOffset + " to field '" + fieldName + "'");
            nextFieldOffset += 8;
        }
        return fieldOffsets.get(fieldName).intValue();
    }

    public void compilePushInt(int value) {
        String r = registerAllocator.allocateRegister();
        cod.compiler.MTOTRegistry.InstructionPattern p = compiler.cpuProfile.getPattern("load_immediate_int");
        compiler.assemblyCode.add("    " + p.assemblyTemplate.get(0).replace("{dest}", r).replace("{value}", String.valueOf(value)));
        operandStack.pushFromRegister(r);
        spiller.markRegisterModified(r);
    }

    public void compilePushFloat(float value) {
    // Create float constant in data section
    String label = compiler.generateDataLabel("float");
    String directive = compiler.cpuProfile.syntax.floatDirective
        .replace("{label}", label)
        .replace("{value}", String.valueOf(value));
    compiler.dataSection.add(directive);
    
    // Allocate register for the float value
    String r = registerAllocator.allocateRegister();
    
    // Load the actual float value from memory
    if (compiler.cpuProfile.architecture.equals("x86_64")) {
        // x86_64: Use SSE registers for floats (simplified - load into general purpose register)
        // For proper float handling, we'd need XMM registers, but for now load as integer
        compiler.assemblyCode.add("    mov " + r + ", [" + label + "] " + compiler.cpuProfile.syntax.commentMarker + " Load float value (as integer)");
    } else {
        // AArch64: Load float value
        compiler.assemblyCode.add("    ldr " + r + ", [" + label + "] " + compiler.cpuProfile.syntax.commentMarker + " Load float value");
    }
    
    operandStack.pushFromRegister(r);
    spiller.markRegisterModified(r);
    
    DebugSystem.debug("MTOT_FLOAT", "Pushed float value " + value + " (label: " + label + ")");
}

    public void compilePushString(String str) {
        String label;
        if (stringLiteralLabels.containsKey(str)) { 
            label = stringLiteralLabels.get(str); 
            cod.debug.DebugSystem.debug("MTOT_STR", "Reusing string literal label '" + label + "' for: \"" + compiler.escapeString(str) + "\""); 
        } else { 
            label = compiler.generateDataLabel("str"); 
            String directive = compiler.cpuProfile.syntax.stringDirective.replace("{label}", label).replace("{value}", compiler.escapeString(str)); 
            compiler.dataSection.add(directive); 
            stringLiteralLabels.put(str, label); 
            cod.debug.DebugSystem.debug("MTOT_STR", "Created string literal label '" + label + "' for: \"" + compiler.escapeString(str) + "\""); 
        }
        String r = registerAllocator.allocateRegister();
        for(String t : compiler.cpuProfile.getPattern("load_address").assemblyTemplate) {
            compiler.assemblyCode.add("    "+t.replace("{dest}",r).replace("{label}",label));
        }
        operandStack.pushFromRegister(r);
        spiller.markRegisterModified(r);
    }

    public void compilePushNull() {
        String r = registerAllocator.allocateRegister();
        cod.compiler.MTOTRegistry.InstructionPattern p = compiler.cpuProfile.getPattern("load_immediate_int");
        compiler.assemblyCode.add("    " + p.assemblyTemplate.get(0).replace("{dest}", r).replace("{value}", "0"));
        operandStack.pushFromRegister(r);
        spiller.markRegisterModified(r);
    }

    public void compilePop() {
        if (!operandStack.isEmpty()) {
            String r = operandStack.popToRegister();
            registerAllocator.freeRegister(r);
            cod.debug.DebugSystem.debug("MTOT_REG", "POP freed register: " + r);
        } else {
            cod.debug.DebugSystem.warn("MTOT", "POP on empty stack.");
        }
    }

    public void compileDup() {
        String r = operandStack.peek();
        if (r != null) {
            spiller.fillRegister(r);
            String n = registerAllocator.allocateRegister(Collections.singleton(r));
            compiler.assemblyCode.add("    " + compiler.cpuProfile.getPattern("move_reg").assemblyTemplate.get(0).replace("{dest}", n).replace("{src}", r));
            operandStack.pushFromRegister(n);
            spiller.markRegisterModified(n);
        } else {
            throw new RuntimeException("DUP on empty stack.");
        }
    }

    public void compileSwap() {
        String r1 = operandStack.popToRegister();
        String r2 = operandStack.popToRegister();
        operandStack.pushFromRegister(r1);
        operandStack.pushFromRegister(r2);
        cod.debug.DebugSystem.debug("MTOT_REG", "SWAPped " + r1 + " and " + r2 + " on stack");
    }

    public void compileLoadField(String fieldName) {
        int offset = getFieldOffset(fieldName);
        String destReg = registerAllocator.allocateRegister(Collections.singleton(compiler.thisRegister));
        spiller.fillRegister(compiler.thisRegister);
        cod.compiler.MTOTRegistry.InstructionPattern pattern = compiler.cpuProfile.getPattern("load_field_offset");
        if (pattern == null) throw new RuntimeException("Missing 'load_field_offset' pattern!");
        String asm = pattern.assemblyTemplate.get(0)
            .replace("{dest_reg}", destReg)
            .replace("{base_reg}", compiler.thisRegister)
            .replace("{offset}", String.valueOf(offset));
        compiler.assemblyCode.add("    " + compiler.cpuProfile.syntax.commentMarker + " Load field " + fieldName + " (offset " + offset + ")");
        compiler.assemblyCode.add("    " + asm);
        operandStack.pushFromRegister(destReg);
        spiller.markRegisterModified(destReg);
    }

    public void compileStoreField(String fieldName) {
        int offset = getFieldOffset(fieldName);
        String valueReg = operandStack.popToRegister();
        spiller.fillRegister(compiler.thisRegister);
        spiller.fillRegister(valueReg);
        cod.compiler.MTOTRegistry.InstructionPattern pattern = compiler.cpuProfile.getPattern("store_field_offset");
        if (pattern == null) throw new RuntimeException("Missing 'store_field_offset' pattern!");
        String asm = pattern.assemblyTemplate.get(0)
            .replace("{src_reg}", valueReg)
            .replace("{base_reg}", compiler.thisRegister)
            .replace("{offset}", String.valueOf(offset));
        compiler.assemblyCode.add("    " + compiler.cpuProfile.syntax.commentMarker + " Store field " + fieldName + " (offset " + offset + ")");
        compiler.assemblyCode.add("    " + asm);
        registerAllocator.freeRegister(valueReg);
    }

    public void compileStoreLocal(int slotIndex, Map<String, String> localRegisterMap) {
        String valueReg = operandStack.popToRegister();
        String varRegKey = "local_" + slotIndex;
        String targetReg = localRegisterMap.get(varRegKey);

        if (targetReg == null) {
            targetReg = registerAllocator.allocateRegister(Collections.singleton(valueReg));
            localRegisterMap.put(varRegKey, targetReg);
            spiller.mapSlotToRegister(slotIndex, targetReg);
            cod.debug.DebugSystem.debug("MTOT_REG", "Allocated register " + targetReg + " for " + varRegKey + " during STORE");
        } else {
            spiller.fillRegister(targetReg);
        }

        spiller.fillRegister(valueReg);

        if (!targetReg.equals(valueReg)) {
            String asm = compiler.cpuProfile.getPattern("move_reg").assemblyTemplate.get(0)
                .replace("{dest}", targetReg)
                .replace("{src}", valueReg);
            compiler.assemblyCode.add("    " + asm);
            registerAllocator.freeRegister(valueReg);
        }

        spiller.markRegisterModified(targetReg);
        spiller.updateRegisterDefinitionDepth(targetReg, compiler.getCurrentLoopDepth());
        spiller.trackRegisterUsage(targetReg);
    }

    public void compileLoadLocal(int slotIndex, Map<String, String> localRegisterMap) {
        String varRegKey = "local_" + slotIndex;
        String varReg = localRegisterMap.get(varRegKey);

        if (varReg == null) {
            cod.debug.DebugSystem.error("MTOT_REG", "Register for " + varRegKey + " is null during LOAD! Recovering by allocating new register and assuming value 0.");
            varReg = registerAllocator.allocateRegister();
            localRegisterMap.put(varRegKey, varReg);
            spiller.mapSlotToRegister(slotIndex, varReg);
            cod.compiler.MTOTRegistry.InstructionPattern p = compiler.cpuProfile.getPattern("load_immediate_int");
            compiler.assemblyCode.add("    " + p.assemblyTemplate.get(0).replace("{dest}", varReg).replace("{value}", "0") + " " + compiler.cpuProfile.syntax.commentMarker + " WARNING: Used uninitialized local slot " + slotIndex);
            spiller.markRegisterModified(varReg);
            spiller.updateRegisterDefinitionDepth(varReg, compiler.getCurrentLoopDepth());
        } else {
             spiller.fillRegister(varReg);
        }

        String tempReg = registerAllocator.allocateRegister(Collections.singleton(varReg));
        String asm = compiler.cpuProfile.getPattern("move_reg").assemblyTemplate.get(0)
            .replace("{dest}", tempReg)
            .replace("{src}", varReg);
        compiler.assemblyCode.add("    " + asm);

        operandStack.pushFromRegister(tempReg);
        spiller.markRegisterModified(tempReg);
        spiller.trackRegisterUsage(varReg);
    }

    public void compileStoreSlot(Object operand, Map<String, String> localRegisterMap) {
        if (operand instanceof Integer) {
            int slotIndex = ((Integer) operand).intValue();
            compileStoreLocal(slotIndex, localRegisterMap);
            String location = localRegisterMap.get("local_" + slotIndex);
            if (location != null) {
                slotLocations.put(Integer.valueOf(slotIndex), location);
                cod.debug.DebugSystem.debug("MTOT_SLOT", "Recorded register location for slot index " + slotIndex + ": " + location);
            } else {
                cod.debug.DebugSystem.warn("MTOT_SLOT", "Cannot determine register location for slot index: " + slotIndex);
                int offset = spiller.getSpillOffsetForSlotIndex(slotIndex);
                if (offset != Integer.MIN_VALUE) {
                    slotLocations.put(Integer.valueOf(slotIndex), "spill_" + offset);
                    cod.debug.DebugSystem.debug("MTOT_SLOT", "Recorded spill location for slot index " + slotIndex + ": spill_" + offset);
                } else {
                    cod.debug.DebugSystem.warn("MTOT_SLOT", "Cannot determine any location for slot index: " + slotIndex);
                }
            }
        } else {
            cod.debug.DebugSystem.error("MTOT", "STORE_SLOT operand is not an Integer index: " + operand);
        }
    }
}