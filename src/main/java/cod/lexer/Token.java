package cod.lexer;

import cod.syntax.Symbol;
import java.util.List;

public class Token {
    public final TokenType type;
    public final String text;
    public final int line;
    public final int column;
    public final Symbol symbol;
    // NEW: Store interpolation parts
    public final List<Token> interpolations;
    // NEW: Optional source file name
    public final String fileName;

    // === SIMPLE CONSTRUCTORS ===
    
    public Token(TokenType type, String text, int line, int column) {
        this(type, text, line, column, null, null, null);
    }

    public Token(TokenType type, String text, int line, int column, Symbol symbol) {
        this(type, text, line, column, symbol, null, null);
    }

    // === CONSTRUCTORS WITH INTERPOLATIONS ===
    
    public Token(TokenType type, String text, int line, int column, List<Token> interpolations) {
        this(type, text, line, column, null, interpolations, null);
    }

    public Token(TokenType type, String text, int line, int column, Symbol symbol, 
                 List<Token> interpolations) {
        this(type, text, line, column, symbol, interpolations, null);
    }

    // === FULL CONSTRUCTOR (PRIMARY) ===
    
    public Token(TokenType type, String text, int line, int column, Symbol symbol, 
                 List<Token> interpolations, String fileName) {
        this.type = type;
        this.text = text;
        this.line = line;
        this.column = column;
        this.symbol = symbol;
        this.interpolations = interpolations;
        this.fileName = fileName;
    }

    // === CONVENIENCE CONSTRUCTORS ===
    
    public Token(TokenType type, String text, int line, int column, String fileName) {
        this(type, text, line, column, null, null, fileName);
    }
    
    public Token(TokenType type, String text, Token other) {
        this(type, text, other.line, other.column, other.symbol, other.interpolations, 
             other.fileName);
    }
    
    // === FACTORY METHODS ===
    
    public static Token withFileName(Token original, String newFileName) {
        return new Token(original.type, original.text, original.line, original.column,
                        original.symbol, original.interpolations, newFileName);
    }
    
    public static Token withSymbol(Token original, Symbol symbol) {
        return new Token(original.type, original.text, original.line, original.column,
                        symbol, original.interpolations, original.fileName);
    }
    
    public static Token withInterpolations(Token original, List<Token> interpolations) {
        return new Token(original.type, original.text, original.line, original.column,
                        original.symbol, interpolations, original.fileName);
    }

    // === INSTANCE METHODS ===
    
    public Token withFileName(String newFileName) {
        return new Token(type, text, line, column, symbol, interpolations, newFileName);
    }
    
    public boolean hasInterpolations() {
        return interpolations != null && !interpolations.isEmpty();
    }
    
    public boolean hasFileName() {
        return fileName != null;
    }
    
    public boolean hasSymbol() {
        return symbol != null;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Token{");
        sb.append("type=").append(type.name());
        sb.append(", text='").append(text).append('\'');
        
        if (symbol != null) {
            sb.append(", symbol=").append(symbol.name());
        }
        
        if (interpolations != null) {
            sb.append(", interpolations=").append(interpolations.size());
        }
        
        if (fileName != null) {
            sb.append(", file='").append(fileName).append('\'');
        }
        
        sb.append(", line=").append(line);
        sb.append(", column=").append(column);
        sb.append('}');
        
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Token token = (Token) o;
        
        if (line != token.line) return false;
        if (column != token.column) return false;
        if (type != token.type) return false;
        if (!text.equals(token.text)) return false;
        if (symbol != token.symbol) return false;
        if (interpolations != null ? !interpolations.equals(token.interpolations) : token.interpolations != null)
            return false;
        return fileName != null ? fileName.equals(token.fileName) : token.fileName == null;
    }
    
    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + text.hashCode();
        result = 31 * result + line;
        result = 31 * result + column;
        result = 31 * result + (symbol != null ? symbol.hashCode() : 0);
        result = 31 * result + (interpolations != null ? interpolations.hashCode() : 0);
        result = 31 * result + (fileName != null ? fileName.hashCode() : 0);
        return result;
    }
}