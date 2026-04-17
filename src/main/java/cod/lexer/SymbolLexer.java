package cod.lexer;

import cod.lexer.TokenType.Symbol;
import static cod.lexer.TokenType.Symbol.*;
import java.util.*;

public class SymbolLexer {

    private final MainLexer lexer;
    
    // DFA states
    private static final int _START = 0;
    private static final int _COLON = 1;
    private static final int _COLON_COLON = 2;
    private static final int _COLON_EQUALS = 3;
    private static final int _EQUALS = 4;
    private static final int _EQUALS_EQUALS = 5;
    private static final int _GREATER = 6;
    private static final int _GREATER_EQUALS = 7;
    private static final int _LESS = 8;
    private static final int _LESS_EQUALS = 9;
    private static final int _BANG = 10;
    private static final int _BANG_EQUALS = 11;
    private static final int _PLUS = 12;
    private static final int _PLUS_EQUALS = 13;
    private static final int _MINUS = 14;
    private static final int _MINUS_EQUALS = 15;
    private static final int _MUL = 16;
    private static final int _MUL_EQUALS = 17;
    private static final int _DIV = 18;
    private static final int _DIV_EQUALS = 19;
    private static final int _TILDE = 20;
    private static final int _TILDE_ARROW = 21;
    private static final int _DOT = 22;
    private static final int _DOT_DOT = 23;
    
    private static final int MAX_STATE = 30;
    
    // Transition table: [current_state][char] = next_state
    private static final int[][] TRANSITION = new int[MAX_STATE][128];
    
    // Accept states: [state] = Symbol (for multi-character symbols)
    private static final Symbol[] ACCEPT = new Symbol[MAX_STATE];
    
    // Single-character symbols (character -> Symbol)
    private static final Symbol[] SINGLE_CHAR_SYMBOLS = new Symbol[128];
    
    static {
        // Initialize all transitions to -1 (invalid)
        for (int i = 0; i < MAX_STATE; i++) {
            Arrays.fill(TRANSITION[i], -1);
        }
        
        // ===== SINGLE-CHARACTER SYMBOLS =====
        SINGLE_CHAR_SYMBOLS['|'] = PIPE;
        SINGLE_CHAR_SYMBOLS['&'] = AMPERSAND;
        SINGLE_CHAR_SYMBOLS['?'] = QUESTION;
        SINGLE_CHAR_SYMBOLS['$'] = DOLLAR;
        SINGLE_CHAR_SYMBOLS['%'] = MOD;
        SINGLE_CHAR_SYMBOLS[','] = COMMA;
        SINGLE_CHAR_SYMBOLS['('] = LPAREN;
        SINGLE_CHAR_SYMBOLS[')'] = RPAREN;
        SINGLE_CHAR_SYMBOLS['{'] = LBRACE;
        SINGLE_CHAR_SYMBOLS['}'] = RBRACE;
        SINGLE_CHAR_SYMBOLS['['] = LBRACKET;
        SINGLE_CHAR_SYMBOLS[']'] = RBRACKET;
        SINGLE_CHAR_SYMBOLS['_'] = UNDERSCORE;
        SINGLE_CHAR_SYMBOLS['\\'] = LAMBDA;
        SINGLE_CHAR_SYMBOLS['#'] = RANGE_HASH;
        
        // ===== MULTI-CHARACTER SYMBOLS - Setup transitions =====
        
        // Setup transitions from start state
        TRANSITION[_START][':'] = _COLON;
        TRANSITION[_START]['='] = _EQUALS;
        TRANSITION[_START]['>'] = _GREATER;
        TRANSITION[_START]['<'] = _LESS;
        TRANSITION[_START]['!'] = _BANG;
        TRANSITION[_START]['+'] = _PLUS;
        TRANSITION[_START]['-'] = _MINUS;
        TRANSITION[_START]['*'] = _MUL;
        TRANSITION[_START]['/'] = _DIV;
        TRANSITION[_START]['~'] = _TILDE;
        TRANSITION[_START]['.'] = _DOT;
        
        // :: (double colon)
        TRANSITION[_COLON][':'] = _COLON_COLON;
        ACCEPT[_COLON_COLON] = DOUBLE_COLON;
        
        // := (colon equals)
        TRANSITION[_COLON]['='] = _COLON_EQUALS;
        ACCEPT[_COLON_EQUALS] = DOUBLE_COLON_ASSIGN;
        
        // Single colon is accepted at state COLON
        ACCEPT[_COLON] = COLON;
        
        // == (equals equals)
        TRANSITION[_EQUALS]['='] = _EQUALS_EQUALS;
        ACCEPT[_EQUALS_EQUALS] = EQ;
        
        // Single equals is accepted at state EQUALS
        ACCEPT[_EQUALS] = ASSIGN;
        
        // >= (greater than or equal)
        TRANSITION[_GREATER]['='] = _GREATER_EQUALS;
        ACCEPT[_GREATER_EQUALS] = GTE;
        
        // Single greater than is accepted at state GREATER
        ACCEPT[_GREATER] = GT;
        
        // <= (less than or equal)
        TRANSITION[_LESS]['='] = _LESS_EQUALS;
        ACCEPT[_LESS_EQUALS] = LTE;
        
        // Single less than is accepted at state LESS
        ACCEPT[_LESS] = LT;
        
        // != (bang equals)
        TRANSITION[_BANG]['='] = _BANG_EQUALS;
        ACCEPT[_BANG_EQUALS] = NEQ;
        
        // Single bang is accepted at state BANG
        ACCEPT[_BANG] = BANG;
        
        // += (plus equals)
        TRANSITION[_PLUS]['='] = _PLUS_EQUALS;
        ACCEPT[_PLUS_EQUALS] = PLUS_ASSIGN;
        
        // Single plus is accepted at state PLUS
        ACCEPT[_PLUS] = PLUS;
        
        // -= (minus equals)
        TRANSITION[_MINUS]['='] = _MINUS_EQUALS;
        ACCEPT[_MINUS_EQUALS] = MINUS_ASSIGN;
        
        // Single minus is accepted at state MINUS
        ACCEPT[_MINUS] = MINUS;
        
        // *= (multiply equals)
        TRANSITION[_MUL]['='] = _MUL_EQUALS;
        ACCEPT[_MUL_EQUALS] = MUL_ASSIGN;
        
        // Single multiply is accepted at state MUL
        ACCEPT[_MUL] = MUL;
        
        // /= (divide equals)
        TRANSITION[_DIV]['='] = _DIV_EQUALS;
        ACCEPT[_DIV_EQUALS] = DIV_ASSIGN;
        
        // Single divide is accepted at state DIV
        ACCEPT[_DIV] = DIV;
        
        // ~> (tilde arrow)
        TRANSITION[_TILDE]['>'] = _TILDE_ARROW;
        ACCEPT[_TILDE_ARROW] = TILDE_ARROW;
        
        // Single tilde is not used, but could be accepted
        ACCEPT[_TILDE] = TILDE_ARROW;
        
        // .. (range dots)
        TRANSITION[_DOT]['.'] = _DOT_DOT;
        ACCEPT[_DOT_DOT] = RANGE_DOTDOT;
        
        // Single dot is accepted at state DOT
        ACCEPT[_DOT] = DOT;
    }

    public SymbolLexer(MainLexer lexer) {
        this.lexer = lexer;
    }

    public Token scan() {
        int startLine = lexer.line;
        int startCol = lexer.column;
        int startPos = lexer.getPosition();
        
        char first = lexer.peek();
        if (first == 0 || first >= 128) return null;
        
        int state = _START;
        int lastAcceptPos = -1;
        Symbol lastAcceptSymbol = null;
        int length = 0;
        
        // DFA traversal for multi-character symbols
        while (lexer.getPosition() < lexer.getInput().length) {
            char c = lexer.peek();
            if (c >= 128) break;
            
            int nextState = TRANSITION[state][c];
            if (nextState == -1) break;
            
            state = nextState;
            lexer.consume();
            length++;
            
            if (ACCEPT[state] != null) {
                lastAcceptPos = startPos + length;
                lastAcceptSymbol = ACCEPT[state];
            }
        }
        
        // If we had a multi-character match, use it
        if (lastAcceptPos != -1) {
            // Roll back to last accept position
            lexer.setPosition(lastAcceptPos);
            
            int finalLength = lastAcceptPos - startPos;
            char[] source = lexer.getInputArray();
            return Token.createSymbol(source, startPos, finalLength, 
                                      startLine, startCol, lastAcceptSymbol);
        }
        
        // Check if the first character is a single-character symbol
        if (first < 128 && SINGLE_CHAR_SYMBOLS[first] != null) {
            lexer.consume();
            char[] source = lexer.getInputArray();
            return Token.createSymbol(source, startPos, 1, 
                                      startLine, startCol, SINGLE_CHAR_SYMBOLS[first]);
        }
        
        return null;
    }

    public boolean isSymbolStart(char c) {
        if (c >= 128) return false;
        return TRANSITION[_START][c] != -1 || SINGLE_CHAR_SYMBOLS[c] != null;
    }

    public Symbol getSymbolForPattern(String pattern) {
        if (pattern == null || pattern.isEmpty()) return null;
        
        // Check single character first
        if (pattern.length() == 1) {
            char c = pattern.charAt(0);
            if (c < 128 && SINGLE_CHAR_SYMBOLS[c] != null) {
                return SINGLE_CHAR_SYMBOLS[c];
            }
        }
        
        // Check multi-character patterns
        char[] chars = pattern.toCharArray();
        int state = _START;
        for (char c : chars) {
            if (c >= 128) return null;
            state = TRANSITION[state][c];
            if (state == -1) return null;
        }
        return ACCEPT[state];
    }
}
