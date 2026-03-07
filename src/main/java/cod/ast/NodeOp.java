package cod.ast;

public interface NodeOp<T> {
    T exec(FlatAST ast, int node);
}
