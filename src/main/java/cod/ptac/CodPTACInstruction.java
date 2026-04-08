package cod.ptac;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class CodPTACInstruction {
    public final CodPTACOpcode opcode;
    public final String dest;
    public final List<CodPTACOperand> operands;
    public final EnumSet<CodPTACFlag> flags;

    public CodPTACInstruction(
        CodPTACOpcode opcode,
        String dest,
        List<CodPTACOperand> operands,
        EnumSet<CodPTACFlag> flags
    ) {
        this.opcode = opcode;
        this.dest = dest;
        this.operands = operands != null ? operands : new ArrayList<CodPTACOperand>();
        this.flags = flags != null ? flags : EnumSet.noneOf(CodPTACFlag.class);
    }

    public CodPTACInstruction(CodPTACOpcode opcode, String dest, List<CodPTACOperand> operands) {
        this(opcode, dest, operands, null);
    }

    public CodPTACInstruction withFlag(CodPTACFlag flag) {
        EnumSet<CodPTACFlag> copy = EnumSet.copyOf(this.flags);
        copy.add(flag);
        return new CodPTACInstruction(this.opcode, this.dest, this.operands, copy);
    }
}
