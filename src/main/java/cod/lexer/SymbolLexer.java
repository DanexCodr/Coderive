package cod.lexer;

import cod.syntax.Symbol;
import static cod.syntax.Symbol.*;
import java.util.*;

public class SymbolLexer {

    private final MainLexer lexer;
    private static final Map<String, Symbol> SYMBOL_PATTERNS = new LinkedHashMap<String, Symbol>();

    static {
        // Multi-character symbols first (for longest match)
        SYMBOL_PATTERNS.put(":=", DOUBLE_COLON_ASSIGN);
        SYMBOL_PATTERNS.put("::", DOUBLE_COLON);
        SYMBOL_PATTERNS.put("==", EQ);
        SYMBOL_PATTERNS.put(">=", GTE);
        SYMBOL_PATTERNS.put("<=", LTE);
        SYMBOL_PATTERNS.put("!=", NEQ);
        SYMBOL_PATTERNS.put("+=", PLUS_ASSIGN);
        SYMBOL_PATTERNS.put("-=", MINUS_ASSIGN);
        SYMBOL_PATTERNS.put("*=", MUL_ASSIGN);
        SYMBOL_PATTERNS.put("/=", DIV_ASSIGN);
        SYMBOL_PATTERNS.put("~>", TILDE_ARROW);
        SYMBOL_PATTERNS.put("..", RANGE_DOTDOT);
        
        // Single-character symbols
        SYMBOL_PATTERNS.put(":", COLON);
        SYMBOL_PATTERNS.put("=", ASSIGN);
        SYMBOL_PATTERNS.put(">", GT);
        SYMBOL_PATTERNS.put("<", LT);
        SYMBOL_PATTERNS.put("!", BANG);
        SYMBOL_PATTERNS.put("+", PLUS);
        SYMBOL_PATTERNS.put("-", MINUS);
        SYMBOL_PATTERNS.put("*", MUL);
        SYMBOL_PATTERNS.put("/", DIV);
        SYMBOL_PATTERNS.put("|", PIPE);
        SYMBOL_PATTERNS.put("&", AMPERSAND);
        SYMBOL_PATTERNS.put("?", QUESTION);
        SYMBOL_PATTERNS.put("%", MOD);
        SYMBOL_PATTERNS.put(".", DOT);
        SYMBOL_PATTERNS.put("#", RANGE_HASH);
        SYMBOL_PATTERNS.put(",", COMMA);
        SYMBOL_PATTERNS.put("(", LPAREN);
        SYMBOL_PATTERNS.put(")", RPAREN);
        SYMBOL_PATTERNS.put("{", LBRACE);
        SYMBOL_PATTERNS.put("}", RBRACE);
        SYMBOL_PATTERNS.put("[", LBRACKET);
        SYMBOL_PATTERNS.put("]", RBRACKET);
        SYMBOL_PATTERNS.put("_", UNDERSCORE);
        SYMBOL_PATTERNS.put("\\", LAMBDA);
    }

    public SymbolLexer(MainLexer lexer) {
        this.lexer = lexer;
    }

    public Token scan() {
        int line = lexer.line;
        int col = lexer.column;

        // Try each pattern in order (LinkedHashMap maintains insertion order)
        for (Map.Entry<String, Symbol> entry : SYMBOL_PATTERNS.entrySet()) {
            String pattern = entry.getKey();
            if (lexer.matches(pattern)) {
                String text = lexer.consume(pattern.length());
                return Token.createSymbol(text, line, col, entry.getValue());
            }
        }

        return null;
    }

    public boolean isSymbolStart(char c) {
        for (String pattern : SYMBOL_PATTERNS.keySet()) {
            if (pattern.charAt(0) == c) {
                return true;
            }
        }
        return false;
    }

    public Symbol getSymbolForPattern(String pattern) {
        return SYMBOL_PATTERNS.get(pattern);
    }
}