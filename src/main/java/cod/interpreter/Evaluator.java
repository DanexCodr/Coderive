package cod.interpreter;

import cod.ast.nodes.*;
import cod.interpreter.context.ExecutionContext;

public interface Evaluator {
    Object evaluate(ExprNode node, ExecutionContext ctx);
    Object evaluate(StmtNode node, ExecutionContext ctx);
}