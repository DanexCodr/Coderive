// ASTNode.java
package cod.ast.nodes;

public abstract class ASTNode {
    protected int line = -1;
    protected int column = -1;
    
    public int getLine() { return line; }
    public int getColumn() { return column; }
    public void setSourcePosition(int line, int column) {
        this.line = line;
        this.column = column;
    }
}
