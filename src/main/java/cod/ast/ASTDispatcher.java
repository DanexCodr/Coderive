package cod.ast;

import java.util.EnumMap;
import java.util.List;
import java.util.ArrayList;

public class ASTDispatcher<T> {
    private final EnumMap<NodeKind, NodeOp<T>> table;
    private T defaultResult;

    public ASTDispatcher() {
        this.table = new EnumMap<NodeKind, NodeOp<T>>(NodeKind.class);
    }

    public ASTDispatcher<T> on(NodeKind kind, NodeOp<T> op) {
        table.put(kind, op);
        return this;
    }

    public ASTDispatcher<T> defaultResult(T val) {
        this.defaultResult = val;
        return this;
    }

    public T dispatch(FlatAST ast, int node) {
        if (ast == null || node == FlatAST.NULL) return defaultResult;
        NodeKind k = ast.kind(node);
        NodeOp<T> op = table.get(k);
        return op != null ? op.exec(ast, node) : defaultResult;
    }

    public List<T> dispatchList(FlatAST ast, int[] nodes) {
        List<T> results = new ArrayList<T>(nodes.length);
        for (int n : nodes) results.add(dispatch(ast, n));
        return results;
    }

    public void dispatchAll(FlatAST ast, int[] nodes) {
        for (int n : nodes) dispatch(ast, n);
    }
}
