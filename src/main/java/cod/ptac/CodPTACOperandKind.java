package cod.ptac;

import java.io.Serializable;

public enum CodPTACOperandKind implements Serializable {
    REGISTER,
    IMMEDIATE,
    LABEL,
    FUNCTION,
    SLOT,
    IDENTIFIER
}
