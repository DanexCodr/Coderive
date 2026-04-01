package cod.lexer;

import java.util.*;

public class MainLexer {

    private final CommentLexer commentLexer;
    private final NumberLexer numberLexer;
    private final IdentifierLexer identifierLexer;
    private final StringLexer stringLexer;
    private final SymbolLexer symbolLexer;
    private final WhitespaceLexer whitespaceLexer;

    private char[] input;          // char array instead of String
    private int position = 0;
    public int line = 1;
    public int column = 1;
    private final String originalInput;
    private final boolean isTemporary;

    public MainLexer(String input) {
        this(input, false);
    }

    public MainLexer(String input, boolean isTemporary) {
        // Normalize line endings: convert Windows \r\n to Unix \n
        // Also handle standalone \r (old Mac) as \n
        String normalized = input.replace("\r\n", "\n").replace("\r", "\n");
        this.input = normalized.toCharArray();
        this.originalInput = normalized;
        this.isTemporary = isTemporary;

        // Initialize component lexers with reference to this lexer
        this.commentLexer = new CommentLexer(this);
        this.numberLexer = new NumberLexer(this);
        this.identifierLexer = new IdentifierLexer(this);
        this.stringLexer = new StringLexer(this);
        this.symbolLexer = new SymbolLexer(this);
        this.whitespaceLexer = new WhitespaceLexer(this);
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<Token>();
        while (position < input.length) {
            Token token = scanNextToken();
            if (token != null) {
                tokens.add(token);
            }
        }

        if (!isTemporary) {
            tokens.add(createEOFToken());
        }

        return tokens;
    }

    private Token scanNextToken() {
        if (position >= input.length) {
            return null;
        }
        
        Token token;

        // WHITESPACE - completely skip, never return
        token = whitespaceLexer.scan();
        if (token != null) {
            return scanNextToken();
        }

        // COMMENTS - skip
        token = commentLexer.scan();
        if (token != null) {
            return scanNextToken();
        }

        token = stringLexer.scan();
        if (token != null) return token;

        token = numberLexer.scan();
        if (token != null) return token;

        token = identifierLexer.scan();
        if (token != null) return token;

        token = symbolLexer.scan();
        if (token != null) return token;

        if (position >= input.length) {
            return null;
        }

        return createInvalidToken();
    }

    // Fast character access methods
    char peek() {
        return peek(0);
    }

    char peek(int offset) {
        return (position + offset >= input.length) ? '\0' : input[position + offset];
    }

    char consume() {
        char c = input[position++];
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }
    
    // Consume without returning (for performance)
    void skip() {
        position++;
        column++;
    }
    
    // Check if next characters match pattern
    boolean matches(String pattern) {
        if (position + pattern.length() > input.length) {
            return false;
        }
        for (int i = 0; i < pattern.length(); i++) {
            if (input[position + i] != pattern.charAt(i)) {
                return false;
            }
        }
        return true;
    }
    
    // Get slice of input as char array (zero-copy)
    char[] getSlice(int start, int length) {
        char[] slice = new char[length];
        System.arraycopy(input, start, slice, 0, length);
        return slice;
    }
    
    // Direct access to input array (for zero-copy tokens)
    char[] getInputArray() {
        return input;
    }

    // State getters/setters
    int getPosition() { return position; }
    void setPosition(int pos) { this.position = pos; }
    int getLine() { return line; }
    int getColumn() { return column; }
    char[] getInput() { return input; }

    private Token createEOFToken() {
        return new Token(TokenType.EOF, new char[0], 0, 0, 
                         line, column, null, null, null, null);
    }

    private Token createInvalidToken() {
        int startLine = line;
        int startCol = column;
        int startPos = position;
        return new Token(TokenType.INVALID, input, startPos, 1, 
                         startLine, startCol, null, null, null, null);
    }
    
    // Legacy methods for backward compatibility
    public void restoreOriginalInput() {
        if (!isTemporary) {
            this.input = originalInput.toCharArray();
        }
    }
    
    public String stripComments() {
        final StringBuilder result = new StringBuilder();
        commentLexer.processWithoutComments(new CommentLexer.CommentProcessor() {
            @Override
            public void process(String text, CommentLexer.CommentType type) {
                result.append(text);
            }
        });
        return result.toString();
    }
    
    public String stripWhitespace() {
        StringBuilder result = new StringBuilder();
        int pos = 0;
        while (pos < input.length) {
            char c = input[pos];
            if (!Character.isWhitespace(c)) {
                result.append(c);
            }
            pos++;
        }
        return result.toString();
    }
}