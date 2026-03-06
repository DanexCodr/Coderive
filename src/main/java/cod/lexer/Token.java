package cod.lexer;

import cod.syntax.Symbol;
import cod.syntax.Keyword;
import java.util.List;
import java.util.ArrayList;

public class Token {
    public final TokenType type;
    public final String text;
    public final int line;
    public final int column;
    public final Symbol symbol;
    public final Keyword keyword;
    public final List<Token> childTokens;  // For INTERPOL tokens only
    public final String fileName;

    // === SINGLE CONSTRUCTOR ===
    
    public Token(TokenType type, String text, int line, int column, 
                 Symbol symbol, Keyword keyword, 
                 List<Token> childTokens, String fileName) {
        this.type = type;
        this.text = text;
        this.line = line;
        this.column = column;
        this.symbol = symbol;
        this.keyword = keyword;
        // Ensure childTokens is never null for INTERPOL tokens
        if (type == TokenType.INTERPOL) {
            this.childTokens = childTokens != null ? childTokens : new ArrayList<Token>();
        } else {
            this.childTokens = null;  // null for non-INTERPOL tokens
        }
        this.fileName = fileName;
    }

    // === FACTORY METHODS ===
    
    public static Token createKeyword(String text, int line, int column, Keyword keyword) {
        return new Token(TokenType.KEYWORD, text, line, column, null, keyword, null, null);
    }
    
    public static Token createSymbol(String text, int line, int column, Symbol symbol) {
        return new Token(TokenType.SYMBOL, text, line, column, symbol, null, null, null);
    }
    
    public static Token createInterpolation(int line, int column, List<Token> childTokens) {
        return new Token(TokenType.INTERPOL, "", line, column, null, null, childTokens, null);
    }
    
    public static Token createTextLiteral(String text, int line, int column) {
        return new Token(TokenType.TEXT_LIT, text, line, column, null, null, null, null);
    }
    
    public static Token createIdentifier(String text, int line, int column) {
        return new Token(TokenType.ID, text, line, column, null, null, null, null);
    }
    
    public static Token createNumber(String text, boolean isFloat, int line, int column) {
        return new Token(isFloat ? TokenType.FLOAT_LIT : TokenType.INT_LIT, 
                        text, line, column, null, null, null, null);
    }

    // === INSTANCE METHODS ===
    
    public boolean isKeyword() {
        return type == TokenType.KEYWORD;
    }
    
    public boolean isKeyword(Keyword expected) {
        return type == TokenType.KEYWORD && keyword == expected;
    }
    
    public boolean isSymbol() {
        return type == TokenType.SYMBOL;
    }
    
    public boolean isSymbol(Symbol expected) {
        return type == TokenType.SYMBOL && symbol == expected;
    }
    
    public boolean hasChildTokens() {
        return childTokens != null && !childTokens.isEmpty();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Token{");
        sb.append("type=").append(type.name());
        sb.append(", text='").append(text).append('\'');
        
        if (symbol != null) sb.append(", symbol=").append(symbol.name());
        if (keyword != null) sb.append(", keyword=").append(keyword.name());
        if (childTokens != null) sb.append(", childTokens=").append(childTokens.size());
        if (fileName != null) sb.append(", file='").append(fileName).append('\'');
        
        sb.append(", line=").append(line);
        sb.append(", column=").append(column);
        sb.append('}');
        
        return sb.toString();
    }
}