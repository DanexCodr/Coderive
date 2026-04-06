package cod.lexer;

import cod.syntax.Symbol;
import cod.syntax.Keyword;
import java.util.List;
import java.util.ArrayList;

public class Token {
    public final TokenType type;
    public final char[] source;
    public final int start;
    public final int length;
    public final int line;
    public final int column;
    public final Symbol symbol;
    public final Keyword keyword;
    public final List<Token> childTokens;
    public final String fileName;

    // Cached text for when string is actually needed (lazy)
    private transient String cachedText = null;

    // === SINGLE CONSTRUCTOR (Zero-Copy) ===

    public Token(
        TokenType type,
        char[] source,
        int start,
        int length,
        int line,
        int column,
        Symbol symbol,
        Keyword keyword,
        List<Token> childTokens,
        String fileName) {
        this.type = type;
        this.source = source;
        this.start = start;
        this.length = length;
        this.line = line;
        this.column = column;
        this.symbol = symbol;
        this.keyword = keyword;
        this.childTokens = childTokens != null ? childTokens : new ArrayList<Token>();
        this.fileName = fileName;
    }

    // Legacy constructor for backward compatibility (converts String to char[])
    public Token(
        TokenType type,
        String text,
        int line,
        int column,
        Symbol symbol,
        Keyword keyword,
        List<Token> childTokens,
        String fileName) {
        this(
            type,
            text != null ? text.toCharArray() : new char[0],
            0,
            text != null ? text.length() : 0,
            line,
            column,
            symbol,
            keyword,
            childTokens,
            fileName);
    }

    // === FAST ACCESS METHODS (Zero-Copy) ===

    public String getText() {
        if (cachedText == null && length > 0) {
            if (type == TokenType.TEXT_LIT && length >= 2) {
                int end = start + length - 1;
                if (length >= 4 &&
                    source[start] == '|' && source[start + 1] == '"' &&
                    source[end - 1] == '"' && source[end] == '|') {
                    cachedText = new String(source, start + 2, length - 4);
                } else if (source[start] == '"' && source[end] == '"') {
                    cachedText = new String(source, start + 1, length - 2);
                } else {
                    cachedText = new String(source, start, length);
                }
            } else {
                cachedText = new String(source, start, length);
            }
        }
        return cachedText != null ? cachedText : "";
    }

    // O(1) length check - no string creation
    public int getLength() {
        return length;
    }

    // Fast equality without creating string
    public boolean matches(String s) {
        if (s == null) return false;
        if (s.length() != length) return false;
        for (int i = 0; i < length; i++) {
            if (source[start + i] != s.charAt(i)) return false;
        }
        return true;
    }

    // Fast case-insensitive match
    public boolean matchesIgnoreCase(String s) {
        if (s == null) return false;
        if (s.length() != length) return false;
        for (int i = 0; i < length; i++) {
            char c1 = source[start + i];
            char c2 = s.charAt(i);
            if (Character.toLowerCase(c1) != Character.toLowerCase(c2)) return false;
        }
        return true;
    }

    // Fast character access
    public char charAt(int index) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + length);
        }
        return source[start + index];
    }

    // Check if token starts with prefix (no string creation)
    public boolean startsWith(String prefix) {
        if (prefix == null) return false;
        if (prefix.length() > length) return false;
        for (int i = 0; i < prefix.length(); i++) {
            if (source[start + i] != prefix.charAt(i)) return false;
        }
        return true;
    }

    // Check if token ends with suffix
    public boolean endsWith(String suffix) {
        if (suffix == null) return false;
        if (suffix.length() > length) return false;
        int offset = length - suffix.length();
        for (int i = 0; i < suffix.length(); i++) {
            if (source[start + offset + i] != suffix.charAt(i)) return false;
        }
        return true;
    }

    // Extract substring without creating intermediate string
    public String substring(int startOffset, int endOffset) {
        if (startOffset < 0 || endOffset > length || startOffset >= endOffset) {
            throw new IndexOutOfBoundsException();
        }
        return new String(source, this.start + startOffset, endOffset - startOffset);
    }

    // === TYPE CHECKING METHODS ===

    public boolean isKeyword() {
        return type == TokenType.KEYWORD && keyword != null;
    }

    public boolean isKeyword(Keyword expected) {
        return type == TokenType.KEYWORD && keyword == expected;
    }

    public boolean isSymbol() {
        return type == TokenType.SYMBOL && symbol != null;
    }

    public boolean isSymbol(Symbol expected) {
        return type == TokenType.SYMBOL && symbol == expected;
    }

    // === CHILD TOKEN METHODS ===

    public boolean hasChildTokens() {
        return childTokens != null && !childTokens.isEmpty();
    }

    public List<Token> getChildTokens() {
        return childTokens;
    }

    // === FACTORY METHODS (Zero-Copy Versions) ===

    public static Token createKeyword(
        char[] source, int start, int length, int line, int column, Keyword keyword) {
        return new Token(
            TokenType.KEYWORD, source, start, length, line, column, null, keyword, null, null);
    }

    public static Token createSymbol(
        char[] source, int start, int length, int line, int column, Symbol symbol) {
        return new Token(
            TokenType.SYMBOL, source, start, length, line, column, symbol, null, null, null);
    }

    public static Token createIdentifier(char[] source, int start, int length, int line, int column) {
        return new Token(TokenType.ID, source, start, length, line, column, null, null, null, null);
    }

    public static Token createNumber(
        char[] source, int start, int length, boolean isFloat, int line, int column) {
        return new Token(
            isFloat ? TokenType.FLOAT_LIT : TokenType.INT_LIT,
            source,
            start,
            length,
            line,
            column,
            null,
            null,
            null,
            null);
    }

    public static Token createTextLiteral(
        char[] source, int start, int length, int line, int column) {
        return new Token(
            TokenType.TEXT_LIT, source, start, length, line, column, null, null, null, null);
    }

    public static Token createInterpolation(int line, int column, List<Token> childTokens) {
        return new Token(
            TokenType.INTERPOL, new char[0], 0, 0, line, column, null, null, childTokens, null);
    }

    // Legacy factory methods (for compatibility)
    public static Token createKeyword(String text, int line, int column, Keyword keyword) {
        char[] chars = text.toCharArray();
        return createKeyword(chars, 0, chars.length, line, column, keyword);
    }

    public static Token createSymbol(String text, int line, int column, Symbol symbol) {
        char[] chars = text.toCharArray();
        return createSymbol(chars, 0, chars.length, line, column, symbol);
    }

    public static Token createIdentifier(String text, int line, int column) {
        char[] chars = text.toCharArray();
        return createIdentifier(chars, 0, chars.length, line, column);
    }

    public static Token createNumber(String text, boolean isFloat, int line, int column) {
        char[] chars = text.toCharArray();
        return createNumber(chars, 0, chars.length, isFloat, line, column);
    }

    public static Token createTextLiteral(String text, int line, int column) {
        char[] chars = text.toCharArray();
        return createTextLiteral(chars, 0, chars.length, line, column);
    }

    // === LEGACY GETTER (for backward compatibility) ===

    public String getTextLegacy() {
        return getText();
    }

    // Override toString to be zero-copy friendly
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Token{");
        sb.append("type=").append(type.name());
        sb.append(", text='").append(getText()).append('\'');
        if (symbol != null) sb.append(", symbol=").append(symbol.name());
        if (keyword != null) sb.append(", keyword=").append(keyword.name());
        if (childTokens != null && !childTokens.isEmpty())
            sb.append(", childTokens=").append(childTokens.size());
        if (fileName != null) sb.append(", file='").append(fileName).append('\'');
        sb.append(", line=").append(line);
        sb.append(", column=").append(column);
        sb.append('}');
        return sb.toString();
    }

    // Reset for token pooling (optional)
    public void reset() {
        cachedText = null;
    }

    public void releaseSource() {
        if (cachedText == null && length > 0) {
            cachedText = getText();
        }
    }
}
