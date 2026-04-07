package cod.ptac;

import java.io.Serializable;

public final class CodPTACOperand implements Serializable {
    private static final long serialVersionUID = 1L;

    public final CodPTACOperandKind kind;
    public final Object value;

    private CodPTACOperand(CodPTACOperandKind kind, Object value) {
        this.kind = kind;
        this.value = value;
    }

    public static CodPTACOperand register(String name) {
        return new CodPTACOperand(CodPTACOperandKind.REGISTER, name);
    }

    public static CodPTACOperand immediate(Object value) {
        return new CodPTACOperand(CodPTACOperandKind.IMMEDIATE, value);
    }

    public static CodPTACOperand label(String name) {
        return new CodPTACOperand(CodPTACOperandKind.LABEL, name);
    }

    public static CodPTACOperand function(String name) {
        return new CodPTACOperand(CodPTACOperandKind.FUNCTION, name);
    }

    public static CodPTACOperand slot(String name) {
        return new CodPTACOperand(CodPTACOperandKind.SLOT, name);
    }

    public static CodPTACOperand identifier(String name) {
        return new CodPTACOperand(CodPTACOperandKind.IDENTIFIER, name);
    }
}
