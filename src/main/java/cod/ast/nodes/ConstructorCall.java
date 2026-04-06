package cod.ast.nodes;

import cod.ast.VisitorImpl;

import java.util.List;

public class ConstructorCall extends Expr {
    public String className;
    public List<Expr> arguments;
    public List<String> argNames;
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}