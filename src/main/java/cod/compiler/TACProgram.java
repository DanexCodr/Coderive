package cod.compiler;

import java.util.*;

public class TACProgram {
    private Map<String, List<TACInstruction>> methods = new HashMap<String, List<TACInstruction>>();
    private Map<String, String> nativeMethods = new HashMap<String, String>(); 

    public void addMethod(String name, List<TACInstruction> code) {
        methods.put(name, new ArrayList<TACInstruction>(code));
    }

    public void addNativeMethod(String name, String assemblyCode) {
        nativeMethods.put(name, assemblyCode);
    }

    public List<TACInstruction> getMethod(String name) {
        return methods.get(name);
    }

    public Map<String, List<TACInstruction>> getMethods() {
        return methods;
    }

    public Map<String, String> getNativeMethods() {
        return nativeMethods;
    }

    public void disassemble() {
        System.out.println("=== MTOT Three-Address Code Disassembly ===");
        for (Map.Entry<String, List<TACInstruction>> entry : methods.entrySet()) {
            System.out.println("\nMethod: " + entry.getKey());
            List<TACInstruction> instructions = entry.getValue();
            for (int i = 0; i < instructions.size(); i++) {
                System.out.printf("  %04d: %s%n", i, instructions.get(i).toString());
            }
        }
    }
}