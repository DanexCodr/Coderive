package cod.ast.node;

import cod.ast.VisitorImpl;
import java.util.List;

public class ChainedComparison extends Expr {
    public List<Expr> expressions;
    public List<String> operators;  // Will have size = expressions.size() - 1
    
    public ChainedComparison(List<Expr> expressions, List<String> operators) {
        this.expressions = expressions;
        this.operators = operators;
    }
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}