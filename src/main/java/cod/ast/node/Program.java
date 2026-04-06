package cod.ast.node;

import cod.ast.VisitorImpl;
import cod.parser.MainParser.ProgramType;

public class Program extends Base {
    public Unit unit;
    public ProgramType programType;  // NEW FIELD
    
    @Override
    public final <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}