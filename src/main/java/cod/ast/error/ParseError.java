package cod.ast.error;

public class ParseError extends RuntimeException {
    private int line;
    private int column;
    
    public ParseError(String message, int line, int column) {
        super(message + " at line " + line + ":" + column);
        this.line = line;
        this.column = column;
    }
    
    public ParseError(String message) {
        super(message);
    }
}
