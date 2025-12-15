package cod.ast.nodes;

import cod.ast.ASTVisitor;
import cod.parser.ProgramType;

public class ProgramNode extends ASTNode {
    public UnitNode unit;
    public ProgramType programType;  // NEW FIELD
    
    @Override
    public final <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}