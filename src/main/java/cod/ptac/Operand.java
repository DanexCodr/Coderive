package cod.ptac;


public final class Operand {
    public final OperandKind kind;
    public final Object value;

    private Operand(OperandKind kind, Object value) {
        this.kind = kind;
        this.value = value;
    }

    public static Operand register(String name) {
        return new Operand(OperandKind.REGISTER, name);
    }

    public static Operand immediate(Object value) {
        return new Operand(OperandKind.IMMEDIATE, value);
    }

    public static Operand label(String name) {
        return new Operand(OperandKind.LABEL, name);
    }

    public static Operand function(String name) {
        return new Operand(OperandKind.FUNCTION, name);
    }

    public static Operand slot(String name) {
        return new Operand(OperandKind.SLOT, name);
    }

    public static Operand identifier(String name) {
        return new Operand(OperandKind.IDENTIFIER, name);
    }
}
