package cod.ast;

import cod.lexer.Token;
import java.util.List;

/**
 * Represents a span of source code with start and end positions.
 * Used for precise error reporting and debugging.
 */
public class SourceSpan {
    public final String fileName;  // Optional: source file name
    public final int startLine;
    public final int startColumn;
    public final int endLine;
    public final int endColumn;
    
    public SourceSpan(int startLine, int startColumn, int endLine, int endColumn) {
        this(null, startLine, startColumn, endLine, endColumn);
    }
    
    public SourceSpan(String fileName, int startLine, int startColumn, int endLine, int endColumn) {
        this.fileName = fileName;
        this.startLine = startLine;
        this.startColumn = startColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
    }
    
    public SourceSpan(Token startToken, Token endToken) {
        this(
            startToken != null ? startToken.fileName : null,
            startToken != null ? startToken.line : 1,
            startToken != null ? startToken.column : 1,
            endToken != null ? endToken.line : 1,
            endToken != null ? endToken.column + (endToken.text != null ? endToken.text.length() - 1 : 0) : 1
        );
    }
    
    public SourceSpan(Token token) {
        this(token, token);
    }
    
    public static SourceSpan merge(SourceSpan first, SourceSpan second) {
        if (first == null) return second;
        if (second == null) return first;
        
        return new SourceSpan(
            first.fileName,
            Math.min(first.startLine, second.startLine),
            Math.min(first.startColumn, second.startColumn),
            Math.max(first.endLine, second.endLine),
            Math.max(first.endColumn, second.endColumn)
        );
    }
    
    public static SourceSpan fromTokens(List<Token> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return new SourceSpan(1, 1, 1, 1);
        }
        
        Token first = tokens.get(0);
        Token last = tokens.get(tokens.size() - 1);
        return new SourceSpan(first, last);
    }
    
    @Override
    public String toString() {
        String filePrefix = fileName != null ? fileName + ":" : "";
        
        if (startLine == endLine && startColumn == endColumn) {
            return filePrefix + startLine + ":" + startColumn;
        } else if (startLine == endLine) {
            return filePrefix + startLine + ":" + startColumn + "-" + endColumn;
        } else {
            return filePrefix + startLine + ":" + startColumn + " to " + endLine + ":" + endColumn;
        }
    }
    
    public String format() {
        return toString();
    }
}