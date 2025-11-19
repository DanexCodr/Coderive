package cod.compiler.natives;

import cod.compiler.MTOTNativeCompiler;
import cod.compiler.RegisterManager;

public class OperandStack {
    private final java.util.Stack<String> stack = new java.util.Stack<String>();
    private final MTOTNativeCompiler compiler;
    private final RegisterManager.RegisterAllocator registerAllocator;
    private final RegisterManager.RegisterSpiller spiller;

    public OperandStack(MTOTNativeCompiler compiler) {
        this.compiler = compiler;
        this.registerAllocator = compiler.registerManager.getAllocator();
        this.spiller = compiler.registerManager.getSpiller();
    }

    public String popToRegister() {
        if (stack.isEmpty()) {
            cod.debug.DebugSystem.warn("OPERAND_STACK", "Popping from empty stack - allocating new register");
            String reg = registerAllocator.allocateRegister();
            spiller.updateRegisterDefinitionDepth(reg, compiler.getCurrentLoopDepth());
            return reg;
        }
        return stack.pop();
    }

    public void pushFromRegister(String reg) {
        stack.push(reg);
        spiller.updateRegisterDefinitionDepth(reg, compiler.getCurrentLoopDepth());
    }

    public String peek() {
        return stack.isEmpty() ? null : stack.peek();
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public int size() {
        return stack.size();
    }

    public void clear() {
        for (String reg : stack) {
            if (reg != null) {
                registerAllocator.freeRegister(reg);
            }
        }
        stack.clear();
    }

    public String[] popTwoOperands() {
        String[] operands = new String[2];
        operands[1] = popToRegister();
        operands[0] = popToRegister();
        return operands;
    }
}