package cod.ast.nodes;

import cod.ast.VisitorImpl;
import java.util.List;

public class ChainedComparisonNode extends ExprNode {
    public List<ExprNode> expressions;
    public List<String> operators;  // Will have size = expressions.size() - 1
    
    public ChainedComparisonNode(List<ExprNode> expressions, List<String> operators) {
        this.expressions = expressions;
        this.operators = operators;
    }
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}