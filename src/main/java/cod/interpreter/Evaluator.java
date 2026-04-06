package cod.interpreter;

import cod.ast.node.*;
import cod.interpreter.context.ExecutionContext;
import java.util.List;

public interface Evaluator {
    Object evaluate(Expr node, ExecutionContext ctx);
    Object evaluate(Stmt node, ExecutionContext ctx);
    Object invokeLambda(Object callback, List<Object> arguments, ExecutionContext ctx, String ownerMethod);
}
