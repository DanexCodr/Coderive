package cod.compiler;

import java.util.*;

public class BytecodeProgram {
    private Map<String, List<BytecodeInstruction>> methods = new HashMap<>();
    private Map<String, String> nativeMethods = new HashMap<>(); // NEW: Native assembly storage
    private List<BytecodeInstruction> mainCode = new ArrayList<>();

    public void addMethod(String name, List<BytecodeInstruction> code) {
        methods.put(name, new ArrayList<>(code));
    }

    // NEW: Add native method implementation
    public void addNativeMethod(String name, String assemblyCode) {
        nativeMethods.put(name, assemblyCode);
    }

    public List<BytecodeInstruction> getMethod(String name) {
        return methods.get(name);
    }

    public Map<String, List<BytecodeInstruction>> getMethods() {
        return methods;
    }

    // NEW: Get native method implementation
    public String getNativeMethod(String name) {
        return nativeMethods.get(name);
    }
    
    // --- ADDED: The missing method ---
    public Map<String, String> getNativeMethods() {
        return nativeMethods;
    }
    // --- END ADDED ---

    public boolean hasNativeImplementation(String name) {
        return nativeMethods.containsKey(name);
    }

    public void setMainCode(List<BytecodeInstruction> code) {
        this.mainCode = new ArrayList<>(code);
    }

    public List<BytecodeInstruction> getMainCode() {
        return mainCode;
    }

    public void disassemble() {
        System.out.println("=== MTOT Bytecode Disassembly ===");
        for (Map.Entry<String, List<BytecodeInstruction>> entry : methods.entrySet()) {
            System.out.println("\nMethod: " + entry.getKey());
            List<BytecodeInstruction> instructions = entry.getValue();
            for (int i = 0; i < instructions.size(); i++) {
                BytecodeInstruction instr = instructions.get(i);
                System.out.printf(
                        "  %04d: %-15s %s%n",
                        i, instr.opcode, instr.operand != null ? instr.operand : "");
            }
        }
    }

    // --- MODIFIED: Display native code ---
    public void disassembleNative() {
        // System.out.println("=== MTOT Native Code ==="); // <-- REMOVED
        for (Map.Entry<String, String> entry : nativeMethods.entrySet()) {
            // System.out.println("\nMethod: " + entry.getKey()); // <-- REMOVED
            System.out.println(entry.getValue());
        }
    }
    // --- END MODIFIED ---
}