package cod.parser.context;

import cod.lexer.Token;
import static cod.lexer.TokenType.*;
import java.util.List;
import java.util.Objects;

/**
 * Immutable parser state representing current parsing position.
 * All state modifications return a new ParserState instance.
 */
public final class ParserState {
    private final List<Token> tokens;
    private final int position;
    private final int line;
    private final int column;
    
    // Cache for current token (performance optimization)
    private transient Token currentTokenCache;
    
    public ParserState(List<Token> tokens) {
        this(tokens, 0, 1, 1);
    }
    
    private ParserState(List<Token> tokens, int position, int line, int column) {
        this.tokens = Objects.requireNonNull(tokens, "tokens cannot be null");
        this.position = position;
        this.line = line;
        this.column = column;
        updateCurrentTokenCache();
    }
    
    private void updateCurrentTokenCache() {
        if (position >= 0 && position < tokens.size()) {
            Token token = tokens.get(position);
            currentTokenCache = token;
            // Note: line/column should already be set from constructor
        } else {
            currentTokenCache = null;
        }
    }
    
    // === FACTORY METHODS (IMMUTABLE OPERATIONS) ===
    
    /**
     * Returns a new ParserState advanced by one token.
     */
    public ParserState advance() {
        if (position >= tokens.size()) {
            return this; // At EOF, stay where we are
        }
        
        Token current = currentToken();
        if (current == null) {
            return this;
        }
        
        // Calculate new position
        int newPosition = position + 1;
        int newLine = current.line;
        int newColumn = current.column + current.text.length();
        
        // If we have a next token, use its position
        if (newPosition < tokens.size()) {
            Token next = tokens.get(newPosition);
            newLine = next.line;
            newColumn = next.column;
        }
        
        return new ParserState(tokens, newPosition, newLine, newColumn);
    }
    
    /**
     * Returns a new ParserState at the specified position.
     */
    public ParserState withPosition(int newPosition) {
        if (newPosition < 0 || newPosition > tokens.size()) {
            throw new IllegalArgumentException("Invalid position: " + newPosition);
        }
        
        if (newPosition == position) {
            return this;
        }
        
        int newLine = 1;
        int newColumn = 1;
        
        if (newPosition < tokens.size()) {
            Token token = tokens.get(newPosition);
            newLine = token.line;
            newColumn = token.column;
        } else if (!tokens.isEmpty()) {
            // At EOF, use last token's position + 1
            Token lastToken = tokens.get(tokens.size() - 1);
            newLine = lastToken.line;
            newColumn = lastToken.column + lastToken.text.length();
        }
        
        return new ParserState(tokens, newPosition, newLine, newColumn);
    }
    
    /**
     * Returns a new ParserState with manually specified line/column.
     * Used for error reporting when position doesn't match token.
     */
    public ParserState withPositionAndLineCol(int newPosition, int newLine, int newColumn) {
        return new ParserState(tokens, newPosition, newLine, newColumn);
    }
    
    // === QUERY METHODS ===
    
    public Token currentToken() {
        return currentTokenCache;
    }
    
    public Token peek(int offset) {
        int targetPos = position + offset;
        return targetPos >= 0 && targetPos < tokens.size() ? tokens.get(targetPos) : null;
    }
    
    public boolean hasMore() {
        return position < tokens.size();
    }
    
    public boolean atEOF() {
        return !hasMore();
    }
    
    // === GETTERS ===
    
    public List<Token> getTokens() {
        return tokens;
    }
    
    public int getPosition() {
        return position;
    }
    
    public int getLine() {
        return line;
    }
    
    public int getColumn() {
        return column;
    }
    
    // === UTILITY METHODS ===
    
    public ParserState skipWhitespaceAndComments() {
        ParserState current = this;
        while (current.hasMore()) {
            Token t = current.currentToken();
            if (t.type == WS || t.type == LINE_COMMENT || t.type == BLOCK_COMMENT) {
                current = current.advance();
            } else {
                break;
            }
        }
        return current;
    }
    
    /**
     * Creates a copy of this state (useful for backtracking patterns).
     */
    public ParserState copy() {
        return new ParserState(tokens, position, line, column);
    }
    
    @Override
    public String toString() {
        Token current = currentToken();
        return String.format(
            "ParserState[pos=%d, line=%d, col=%d, current=%s]",
            position, line, column,
            current != null ? "'" + current.text + "'" : "EOF"
        );
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParserState that = (ParserState) o;
        return position == that.position && 
               line == that.line && 
               column == that.column &&
               tokens.equals(that.tokens);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(tokens, position, line, column);
    }
}