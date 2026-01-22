package cod.semantic;

import cod.lexer.*;
import cod.syntax.*;

public final class TokenValidator {
    private TokenValidator() {}
    
    // JUST VARARGS - covers all cases
    public static boolean is(Token token, TokenType... types) {
        if (token == null || token.type == null) return false;
        for (TokenType type : types) {
            if (token.type == type) return true;
        }
        return false;
    }
    
    public static boolean is(Token token, Symbol... symbols) {
        if (token == null || token.symbol == null) return false;
        for (Symbol symbol : symbols) {
            if (token.symbol == symbol) return true;
        }
        return false;
    }
    
    public static boolean is(Token token, Keyword... keywords) {
        if (token == null || token.text == null) return false;
        for (Keyword keyword : keywords) {
            if (token.text.equals(keyword.toString())) return true;
        }
        return false;
    }
}