package cod.lexer;

import java.util.*;

public class MainLexer {

    private final CommentLexer commentLexer;
    private final NumberLexer numberLexer;
    private final IdentifierLexer identifierLexer;
    private final StringLexer stringLexer;
    private final SymbolLexer symbolLexer;
    private final WhitespaceLexer whitespaceLexer;

    private String input;
    private int position = 0;
    public int line = 1;
    public int column = 1;
    private final String originalInput;
    private final boolean isTemporary;

    public MainLexer(String input) {
        this(input, false);
    }

    public MainLexer(String input, boolean isTemporary) {
        this.input = input;
        this.originalInput = input;
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
    while (position < input.length()) {
        Token token = scanNextToken();
        if (token != null) {
            tokens.add(token);  // Only meaningful tokens
        }
    }

    if (!isTemporary) {
        tokens.add(createEOFToken());
    }

    return tokens;
}

    private Token scanNextToken() {
    if (position >= input.length()) {
        return null;
    }
    
    Token token;

    // WHITESPACE - completely skip, never return
    token = whitespaceLexer.scan();
    if (token != null) {
        // Consumed whitespace, continue to next token
        return scanNextToken();
    }

    // COMMENTS - handle based on whether we want them
    token = commentLexer.scan();
    if (token != null) {
        // For comments, you have a choice:
        // Option A: Never return comments (standard compiler behavior)
        return scanNextToken();  // Skip comments completely
        
        // Option B: Return comments only if specifically requested
        // if (includeComments) return token;
        // else return scanNextToken();
    }

    // Rest of lexers...
    token = stringLexer.scan();
    if (token != null) return token;

    token = numberLexer.scan();
    if (token != null) return token;

    token = identifierLexer.scan();
    if (token != null) return token;

    token = symbolLexer.scan();
    if (token != null) return token;

    if (position >= input.length()) {
        return null;
    }

    return createInvalidToken();
}

    // Public API for extracting specific components

    public List<CommentLexer.Comment> extractComments() {
        return commentLexer.extractAllComments();
    }

    public List<String> extractIdentifiers() {
        return identifierLexer.extractAllIdentifiers();
    }

    public List<NumberLexer.NumberValue> extractNumbers() {
        return numberLexer.extractAllNumbers();
    }

    public List<String> extractStringLiterals() {
        return stringLexer.extractAllStrings();
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
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (!Character.isWhitespace(c)) {
                result.append(c);
            }
            pos++;
        }
        return result.toString();
    }

    public boolean isKeyword(String text) {
        return identifierLexer.isKeyword(text);
    }

    public void restoreOriginalInput() {
        if (!isTemporary) {
            this.input = this.originalInput;
        }
    }

    // Core lexer methods used by component lexers

    char peek() {
        return peek(0);
    }

    char peek(int offset) {
        return (position + offset >= input.length()) ? '\0' : input.charAt(position + offset);
    }

    char consume() {
        char c = input.charAt(position++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    String consume(int length) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            result.append(consume());
        }
        return result.toString();
    }

    boolean matches(String pattern) {
        if (position + pattern.length() > input.length()) {
            return false;
        }
        for (int i = 0; i < pattern.length(); i++) {
            if (input.charAt(position + i) != pattern.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    // State getters/setters for component lexers
    int getPosition() { return position; }
    void setPosition(int pos) { this.position = pos; }
    int getLine() { return line; }
    int getColumn() { return column; }
    String getInput() { return input; }

    private Token createEOFToken() {
        return new Token(TokenType.EOF, "", line, column, null, null, null, null);
    }

    private Token createInvalidToken() {
        int startLine = line;
        int startCol = column;
        char c = consume();
        return new Token(TokenType.INVALID, String.valueOf(c), startLine, startCol, null, null, null, null);
    }
}