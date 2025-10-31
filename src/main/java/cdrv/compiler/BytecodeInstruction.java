package cdrv.compiler;

public class BytecodeInstruction {
    public enum Opcode {
        // Stack operations
        PUSH_INT,
        PUSH_FLOAT,
        PUSH_STRING,
        PUSH_BOOL,
        PUSH_NULL,
        POP,
        DUP,
        SWAP,

        // Integer Arithmetic
        ADD_INT,
        SUB_INT,
        MUL_INT,
        DIV_INT,
        MOD_INT,
        NEG_INT,

        // String Operations
        CONCAT_STRING, // <<< ADDED
        INT_TO_STRING,

        // Integer Comparison
        CMP_EQ_INT,
        CMP_NE_INT,
        CMP_LT_INT,
        CMP_LE_INT,
        CMP_GT_INT,
        CMP_GE_INT,

        // Control flow
        JMP,
        JMP_IF_TRUE,
        JMP_IF_FALSE,
        CALL,
        // --- NEW ---
        CALL_SLOTS, // Call a method that returns multiple slot values
        // --- END NEW ---
        RET,
        LABEL,

        // Memory operations
        LOAD_LOCAL,
        STORE_LOCAL,
        LOAD_FIELD,
        STORE_FIELD,
        // --- MODIFIED ---
        STORE_SLOT, // Replaces generic STORE_SLOT (now specifically for return slots)
        // LOAD_SLOT removed as slots are only written to, not read directly
        // --- END MODIFIED ---

        // Arrays
        ARRAY_NEW,
        ARRAY_LOAD,
        ARRAY_STORE,
        ARRAY_LENGTH,

        // Built-in functions
        PRINT,
        // --- NEW ---
        READ_INPUT, // Instruction to call runtime for input
        // --- END NEW ---
        RANGE_START, // Potentially used by complex For loops (Not implemented yet)
        RANGE_END,   // Potentially used by complex For loops (Not implemented yet)
        RANGE_STEP   // Potentially used by complex For loops (Not implemented yet)
    }

    public final Opcode opcode;
    public final Object operand;

    public BytecodeInstruction(Opcode opcode, Object operand) {
        this.opcode = opcode;
        this.operand = operand;
    }

    public BytecodeInstruction(Opcode opcode) {
        this(opcode, null);
    }
}