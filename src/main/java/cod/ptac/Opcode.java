package cod.ptac;


public enum Opcode {
    // Core TAC
    NOP(Layer.CORE_TAC),
    ASSIGN(Layer.CORE_TAC),
    ADD(Layer.CORE_TAC),
    SUB(Layer.CORE_TAC),
    MUL(Layer.CORE_TAC),
    DIV(Layer.CORE_TAC),
    MOD(Layer.CORE_TAC),
    EQ(Layer.CORE_TAC),
    NE(Layer.CORE_TAC),
    GT(Layer.CORE_TAC),
    LT(Layer.CORE_TAC),
    GTE(Layer.CORE_TAC),
    LTE(Layer.CORE_TAC),
    BRANCH(Layer.CORE_TAC),
    BRANCH_IF(Layer.CORE_TAC),
    CALL(Layer.CORE_TAC),
    RETURN(Layer.CORE_TAC),
    LOAD(Layer.CORE_TAC),
    STORE(Layer.CORE_TAC),

    // Coderive pattern ops
    RANGE(Layer.PATTERN),
    RANGE_Q(Layer.PATTERN),
    RANGE_S(Layer.PATTERN),
    RANGE_L(Layer.PATTERN),
    RANGE_LS(Layer.PATTERN),
    MAP(Layer.PATTERN),
    FILTER(Layer.PATTERN),
    REDUCE(Layer.PATTERN),
    WHERE(Layer.PATTERN),
    SCAN(Layer.PATTERN),
    ZIP(Layer.PATTERN),
    TAKE(Layer.PATTERN),
    FILTER_MAP(Layer.PATTERN),
    FILTER_MAP_REDUCE(Layer.PATTERN),

    // Lambda / recursion / closures
    LAMBDA(Layer.PATTERN),
    CLOSURE(Layer.PATTERN),
    ANCESTOR(Layer.PATTERN),
    SELF(Layer.PATTERN),
    TAIL_CALL(Layer.PATTERN),

    // Slot ops
    SLOT_GET(Layer.PATTERN),
    SLOT_SET(Layer.PATTERN),
    SLOT_RET(Layer.PATTERN),
    SLOT_UNPACK(Layer.PATTERN),
    SLOT_DIV(Layer.PATTERN),

    // Lazy ops
    LAZY_GET(Layer.PATTERN),
    LAZY_SET(Layer.PATTERN),
    LAZY_COMMIT(Layer.PATTERN),
    LAZY_SIZE(Layer.PATTERN),
    LAZY_SLICE(Layer.PATTERN),

    // Formula ops
    FORMULA_SEQ(Layer.PATTERN),
    FORMULA_COND(Layer.PATTERN),
    FORMULA_RECUR(Layer.PATTERN),
    FORMULA_FUSE(Layer.PATTERN);

    private final Layer layer;

    Opcode(Layer layer) {
        this.layer = layer;
    }

    public Layer getLayer() {
        return layer;
    }
}
