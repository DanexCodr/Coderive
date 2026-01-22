package cod.ast.nodes;

import cod.ast.VisitorImpl;
import cod.parser.program.ProgramType;

public class ProgramNode extends ASTNode {
    public UnitNode unit;
    public ProgramType programType;  // NEW FIELD
    
    @Override
    public final <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}