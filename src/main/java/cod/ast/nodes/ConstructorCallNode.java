package cod.ast.nodes;

import cod.ast.VisitorImpl;

import java.util.List;

public class ConstructorCallNode extends ExprNode {
    public String className;
    public List<ExprNode> arguments;
    public List<String> argNames;
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}