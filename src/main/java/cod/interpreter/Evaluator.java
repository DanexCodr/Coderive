package cod.interpreter;

import cod.ast.nodes.*;
import cod.interpreter.context.ExecutionContext;
import java.util.List;

public interface Evaluator {
    Object evaluate(ExprNode node, ExecutionContext ctx);
    Object evaluate(StmtNode node, ExecutionContext ctx);
    Object invokeLambda(Object callback, List<Object> arguments, ExecutionContext ctx, String ownerMethod);
}
