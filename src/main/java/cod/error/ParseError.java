package cod.error;

/**
 * Parser error that encapsulates both the error message and exact location.
 * 
 * DESIGN PHILOSOPHY:
 * - ParseError should be thrown DIRECTLY at the point where parsing fails
 * - The message should include all necessary context for debugging
 * - Line/column info is appended to the message in the constructor
 * - DO NOT catch and re-throw ParseError just to add context
 * - Instead, add context directly to the error message before throwing
 * 
 * This ensures clean error flow without duplication of location information.
 */
public class ParseError extends RuntimeException {
    private int line;
    private int column;
    
    public ParseError(String message, int line, int column) {
        super(message + " at line " + line + ":" + column);
        this.line = line;
        this.column = column;
    }
    
    public int getLine() {
      return line;
    }
    
    public int getColumn() {
      return column;
    }
}