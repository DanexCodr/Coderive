package cod.ptac;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class Instruction {
    public final Opcode opcode;
    public final String dest;
    public final List<Operand> operands;
    public final EnumSet<Flag> flags;

    public Instruction(
        Opcode opcode,
        String dest,
        List<Operand> operands,
        EnumSet<Flag> flags
    ) {
        this.opcode = opcode;
        this.dest = dest;
        this.operands = operands != null ? operands : new ArrayList<Operand>();
        this.flags = flags != null ? flags : EnumSet.noneOf(Flag.class);
    }

    public Instruction(Opcode opcode, String dest, List<Operand> operands) {
        this(opcode, dest, operands, null);
    }

    public Instruction withFlag(Flag flag) {
        EnumSet<Flag> copy = EnumSet.copyOf(this.flags);
        copy.add(flag);
        return new Instruction(this.opcode, this.dest, this.operands, copy);
    }
}
