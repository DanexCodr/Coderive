package cod.compiler;

public class BytecodeInstruction {
    public enum Opcode {
    PUSH_INT,
    PUSH_FLOAT,
    PUSH_STRING,
    PUSH_BOOL,
    PUSH_NULL,
    POP,
    DUP,
    SWAP,

    ADD_INT,
    SUB_INT,
    MUL_INT,
    DIV_INT,
    MOD_INT,
    NEG_INT,

    CONCAT_STRING,
    INT_TO_STRING,

    CMP_EQ_INT,
    CMP_NE_INT,
    CMP_LT_INT,
    CMP_LE_INT,
    CMP_GT_INT,
    CMP_GE_INT,

    JMP,
    JMP_IF_TRUE,
    JMP_IF_FALSE,
    CALL,
    CALL_SLOTS,
    RET,
    LABEL,

    LOAD_LOCAL,
    STORE_LOCAL,
    LOAD_FIELD,
    STORE_FIELD,
    STORE_SLOT,

    ARRAY_NEW,
    ARRAY_LOAD,
    ARRAY_STORE,
    ARRAY_LENGTH,

    PRINT,
    READ_INPUT,
    RANGE_START,
    RANGE_END,
    RANGE_STEP
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