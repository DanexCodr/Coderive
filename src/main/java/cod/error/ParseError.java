package cod.error;

import cod.ast.SourceSpan;
import cod.lexer.Token;

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
 @SuppressWarnings("serial")
public class ParseError extends RuntimeException {
    private int line;
    private int column;
    private SourceSpan sourceSpan;
    
    public ParseError(String message, int line, int column) {
        super(message + " at line " + line + ":" + column);
        this.line = line;
        this.column = column;
        this.sourceSpan = new SourceSpan(line, column, line, column);
    }
    
    // Constructor with SourceSpan
    public ParseError(String message, SourceSpan span) {
        super(message + " at " + (span != null ? span.format() : "unknown location"));
        this.sourceSpan = span;
        if (span != null) {
            this.line = span.startLine;
            this.column = span.startColumn;
        }
    }
    
    // Constructor with Token
    public ParseError(String message, Token token) {
        this(message, new SourceSpan(token));
    }
    
    // Constructor with two tokens (start and end)
    public ParseError(String message, Token startToken, Token endToken) {
        this(message, new SourceSpan(startToken, endToken));
    }
    
    public int getLine() {
        return line;
    }
    
    public int getColumn() {
        return column;
    }
    
    // Get source span
    public SourceSpan getSourceSpan() {
        return sourceSpan;
    }
}