package cod.ptac;

import java.io.Serializable;

public enum CodPTACOpcode implements Serializable {
    // Core TAC
    NOP(CodPTACLayer.CORE_TAC),
    ASSIGN(CodPTACLayer.CORE_TAC),
    ADD(CodPTACLayer.CORE_TAC),
    SUB(CodPTACLayer.CORE_TAC),
    MUL(CodPTACLayer.CORE_TAC),
    DIV(CodPTACLayer.CORE_TAC),
    MOD(CodPTACLayer.CORE_TAC),
    EQ(CodPTACLayer.CORE_TAC),
    NE(CodPTACLayer.CORE_TAC),
    GT(CodPTACLayer.CORE_TAC),
    LT(CodPTACLayer.CORE_TAC),
    GTE(CodPTACLayer.CORE_TAC),
    LTE(CodPTACLayer.CORE_TAC),
    BRANCH(CodPTACLayer.CORE_TAC),
    BRANCH_IF(CodPTACLayer.CORE_TAC),
    CALL(CodPTACLayer.CORE_TAC),
    RETURN(CodPTACLayer.CORE_TAC),
    LOAD(CodPTACLayer.CORE_TAC),
    STORE(CodPTACLayer.CORE_TAC),

    // Coderive pattern ops
    RANGE(CodPTACLayer.PATTERN),
    RANGE_Q(CodPTACLayer.PATTERN),
    RANGE_S(CodPTACLayer.PATTERN),
    RANGE_L(CodPTACLayer.PATTERN),
    RANGE_LS(CodPTACLayer.PATTERN),
    MAP(CodPTACLayer.PATTERN),
    FILTER(CodPTACLayer.PATTERN),
    REDUCE(CodPTACLayer.PATTERN),
    WHERE(CodPTACLayer.PATTERN),
    SCAN(CodPTACLayer.PATTERN),
    ZIP(CodPTACLayer.PATTERN),
    TAKE(CodPTACLayer.PATTERN),
    FILTER_MAP(CodPTACLayer.PATTERN),
    FILTER_MAP_REDUCE(CodPTACLayer.PATTERN),

    // Lambda / recursion / closures
    LAMBDA(CodPTACLayer.PATTERN),
    CLOSURE(CodPTACLayer.PATTERN),
    ANCESTOR(CodPTACLayer.PATTERN),
    SELF(CodPTACLayer.PATTERN),
    TAIL_CALL(CodPTACLayer.PATTERN),

    // Slot ops
    SLOT_GET(CodPTACLayer.PATTERN),
    SLOT_SET(CodPTACLayer.PATTERN),
    SLOT_RET(CodPTACLayer.PATTERN),
    SLOT_UNPACK(CodPTACLayer.PATTERN),
    SLOT_DIV(CodPTACLayer.PATTERN),

    // Lazy ops
    LAZY_GET(CodPTACLayer.PATTERN),
    LAZY_SET(CodPTACLayer.PATTERN),
    LAZY_COMMIT(CodPTACLayer.PATTERN),
    LAZY_SIZE(CodPTACLayer.PATTERN),
    LAZY_SLICE(CodPTACLayer.PATTERN),

    // Formula ops
    FORMULA_SEQ(CodPTACLayer.PATTERN),
    FORMULA_COND(CodPTACLayer.PATTERN),
    FORMULA_RECUR(CodPTACLayer.PATTERN),
    FORMULA_FUSE(CodPTACLayer.PATTERN);

    private final CodPTACLayer layer;

    CodPTACOpcode(CodPTACLayer layer) {
        this.layer = layer;
    }

    public CodPTACLayer getLayer() {
        return layer;
    }
}
