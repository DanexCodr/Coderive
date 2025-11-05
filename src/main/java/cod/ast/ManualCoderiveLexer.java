package cod.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cdrv.Constants.*;

/**
 * A self-contained, ANTLR-free lexer for the Coderive language. Contains the nested static Token
 * class. This lexer is compatible with Java 7.
 */
public class ManualCoderiveLexer {

    /**
     * A simple, self-contained Token class, free of ANTLR dependencies. It is nested within the
     * Lexer that produces it.
     */
    public static class Token {
        public final int type;
        public final String text;
        public final int line;
        public final int column;

        public static final int EOF = -1;

        public Token(int type, String text, int line, int column) {
            this.type = type;
            this.text = text;
            this.line = line;
            this.column = column;
        }

        @Override
        public String toString() {
            return "Token{"
                    + "type="
                    + getTypeName(type)
                    + ", text='"
                    + text
                    + '\''
                    + ", line="
                    + line
                    + ", column="
                    + column
                    + '}';
        }
    }

    // Token type constants
    public static final int SHARE = 1,
            LOCAL = 2,
            UNIT = 3,
            GET = 4,
            EXTEND = 5,
            THIS = 6,
            VAR = 7,
            OUTPUT = 8,
            INPUT = 9,
            IF = 10,
            ELSE = 11,
            ELIF = 12,
            FOR = 13,
            IN = 14,
            TO = 15,
            BY = 16,
            INT = 17,
            STRING = 18,
            FLOAT = 19,
            BOOL = 20,
            INT_LIT = 21,
            FLOAT_LIT = 22,
            STRING_LIT = 23,
            BOOL_LIT = 24,
            ID = 25,
            ASSIGN = 26,
            PLUS = 27,
            MINUS = 28,
            MUL = 29,
            DIV = 30,
            MOD = 31,
            COLON = 32,
            GT = 33,
            LT = 34,
            GTE = 35,
            LTE = 36,
            EQ = 37,
            NEQ = 38,
            DOT = 39,
            COMMA = 40,
            LPAREN = 41,
            RPAREN = 42,
            LBRACE = 43,
            RBRACE = 44,
            LBRACKET = 45,
            RBRACKET = 46,
            LINE_COMMENT = 47,
            BLOCK_COMMENT = 48,
            WS = 49,
            PLUS_ASSIGN = 50,
            MINUS_ASSIGN = 51,
            MUL_ASSIGN = 52,
            DIV_ASSIGN = 53,
            // --- NEW TOKENS ---
            TILDE_BAR = 54,
            TILDE = 55,
            // ---
            INVALID = -2;

    private static final Map<Integer, String> TOKEN_NAMES = new HashMap<Integer, String>();
    private static final Map<String, Integer> KEYWORDS = new HashMap<String, Integer>();

    static {
        // Bi-directional mapping for keywords and their names for debugging.
        KEYWORDS.put(share, SHARE);
        TOKEN_NAMES.put(SHARE, _share);
        KEYWORDS.put(local, LOCAL);
        TOKEN_NAMES.put(LOCAL, _local);
        KEYWORDS.put(unit, UNIT);
        TOKEN_NAMES.put(UNIT, _unit);
        KEYWORDS.put(get, GET);
        TOKEN_NAMES.put(GET, _get);
        KEYWORDS.put(extend, EXTEND);
        TOKEN_NAMES.put(EXTEND, _extend);
        KEYWORDS.put(this_, THIS);
        TOKEN_NAMES.put(THIS, _this);
        KEYWORDS.put(var, VAR);
        TOKEN_NAMES.put(VAR, _var);
        KEYWORDS.put(output, OUTPUT);
        TOKEN_NAMES.put(OUTPUT, _output);
        KEYWORDS.put(input_, INPUT);
        TOKEN_NAMES.put(INPUT, _input);
        KEYWORDS.put(if_, IF);
        TOKEN_NAMES.put(IF, _if);
        KEYWORDS.put(else_, ELSE);
        TOKEN_NAMES.put(ELSE, _else);
        KEYWORDS.put(elif_, ELIF);
        TOKEN_NAMES.put(ELIF, _elif);
        KEYWORDS.put(for_, FOR);
        TOKEN_NAMES.put(FOR, _for);
        KEYWORDS.put(in_, IN);
        TOKEN_NAMES.put(IN, _in);
        KEYWORDS.put(to_, TO);
        TOKEN_NAMES.put(TO, _to);
        KEYWORDS.put(by_, BY);
        TOKEN_NAMES.put(BY, _by);
        KEYWORDS.put(int_, INT);
        TOKEN_NAMES.put(INT, _int);
        KEYWORDS.put(string, STRING);
        TOKEN_NAMES.put(STRING, _string);
        KEYWORDS.put(float_, FLOAT);
        TOKEN_NAMES.put(FLOAT, _float);
        KEYWORDS.put(bool, BOOL);
        TOKEN_NAMES.put(BOOL, _bool);
        KEYWORDS.put(true_, BOOL_LIT);
        KEYWORDS.put(false_, BOOL_LIT);

        TOKEN_NAMES.put(Token.EOF, _eof);
        TOKEN_NAMES.put(INT_LIT, _int_lit);
        TOKEN_NAMES.put(FLOAT_LIT, _float_lit);
        TOKEN_NAMES.put(STRING_LIT, _string_lit);
        TOKEN_NAMES.put(BOOL_LIT, _bool_lit);
        TOKEN_NAMES.put(ID, _id);
        TOKEN_NAMES.put(ASSIGN, _assign);
        TOKEN_NAMES.put(PLUS, _plus);
        TOKEN_NAMES.put(MINUS, _minus);
        TOKEN_NAMES.put(MUL, _mul);
        TOKEN_NAMES.put(DIV, _div);
        TOKEN_NAMES.put(MOD, _mod);
        TOKEN_NAMES.put(COLON, _colon);
        TOKEN_NAMES.put(GT, _gt);
        TOKEN_NAMES.put(LT, _lt);
        TOKEN_NAMES.put(GTE, _gte);
        TOKEN_NAMES.put(LTE, _lte);
        TOKEN_NAMES.put(EQ, _eq);
        TOKEN_NAMES.put(NEQ, _neq);
        TOKEN_NAMES.put(DOT, _dot);
        TOKEN_NAMES.put(COMMA, _comma);
        TOKEN_NAMES.put(LPAREN, _lparen);
        TOKEN_NAMES.put(RPAREN, _rparen);
        TOKEN_NAMES.put(LBRACE, _lbrace);
        TOKEN_NAMES.put(RBRACE, _rbrace);
        TOKEN_NAMES.put(LBRACKET, _lbracket);
        TOKEN_NAMES.put(RBRACKET, _rbracket);
        TOKEN_NAMES.put(PLUS_ASSIGN, _plus_assign);
        TOKEN_NAMES.put(MINUS_ASSIGN, _minus_assign);
        TOKEN_NAMES.put(MUL_ASSIGN, _mul_assign);
        TOKEN_NAMES.put(DIV_ASSIGN, _div_assign);
        // --- NEW TOKEN NAMES ---
        TOKEN_NAMES.put(TILDE_BAR, "TILDE_BAR");
        TOKEN_NAMES.put(TILDE, "TILDE");
        // ---
        TOKEN_NAMES.put(INVALID, _invalid);
    }

    public static String getTypeName(int type) {
        return TOKEN_NAMES.get(type);
    }

    private final String input;
    private int position = 0;
    private int line = 1;
    private int column = 1;

    public ManualCoderiveLexer(String input) {
        this.input = input;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<Token>();
        while (position < input.length()) {
            skipWhitespaceAndComments();
            if (position < input.length()) {
                tokens.add(scanNextToken());
            }
        }
        tokens.add(new Token(Token.EOF, "<EOF>", line, column));
        return tokens;
    }

    private void skipWhitespaceAndComments() {
        while (position < input.length()) {
            char c = peek();
            if (Character.isWhitespace(c)) {
                consume();
            } else if (c == '/' && peek(1) == '/') {
                scanLineComment();
            } else if (c == '/' && peek(1) == '*') {
                scanBlockComment();
            } else {
                break;
            }
        }
    }

    private Token scanNextToken() {
        int startLine = line;
        int startColumn = column;
        char c = peek();

        if (Character.isLetter(c) || c == '_')
            return readIdentifierOrKeyword(startLine, startColumn);
        if (Character.isDigit(c)) return readNumber(startLine, startColumn);
        if (c == '"') return readString(startLine, startColumn);
        return readSymbol(startLine, startColumn);
    }

    private Token readIdentifierOrKeyword(int startLine, int startColumn) {
        StringBuilder sb = new StringBuilder();
        while (position < input.length() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            sb.append(consume());
        }
        String text = sb.toString();
        Integer type = KEYWORDS.get(text);
        return new Token(type != null ? type : ID, text, startLine, startColumn);
    }

private Token readNumber(int startLine, int startColumn) {
        StringBuilder sb = new StringBuilder();
        boolean isFloat = false;
        while (position < input.length() && Character.isDigit(peek())) {
            sb.append(consume());
        }
        if (peek() == '.') {
            isFloat = true;
            sb.append(consume());
            while (position < input.length() && Character.isDigit(peek())) {
                sb.append(consume());
            }
        }
        return new Token(isFloat ? FLOAT_LIT : INT_LIT, sb.toString(), startLine, startColumn);
    }

    private Token readString(int startLine, int startColumn) {
        StringBuilder sb = new StringBuilder();
        consume(); // Consume opening quote
        while (position < input.length() && peek() != '"') {
            if (peek() == '\\') {
                consume();
                if (position < input.length()) sb.append(consume());
            } else {
                sb.append(consume());
            }
        }
        if (position < input.length()) consume(); // Consume closing quote
        return new Token(STRING_LIT, sb.toString(), startLine, startColumn);
    }

    private Token readSymbol(int startLine, int startColumn) {
        char c1 = consume();
        switch (c1) {
            case '=':
                if (peek() == '=') {
                    consume();
                    return new Token(EQ, "==", startLine, startColumn);
                } else {
                    return new Token(ASSIGN, "=", startLine, startColumn);
                }
            case '>':
                return (peek() == '=')
                        ? new Token(GTE, ">=", startLine, startColumn) {
                            {
                                consume();
                            }
                        }
                        : new Token(GT, ">", startLine, startColumn);
            case '<':
                return (peek() == '=')
                        ? new Token(LTE, "<=", startLine, startColumn) {
                            {
                                consume();
                            }
                        }
                        : new Token(LT, "<", startLine, startColumn);
            case '!':
                return (peek() == '=')
                        ? new Token(NEQ, "!=", startLine, startColumn) {
                            {
                                consume();
                            }
                        }
                        : new Token(INVALID, "!", startLine, startColumn);
            case '+':
                return (peek() == '=')
                        ? new Token(PLUS_ASSIGN, "+=", startLine, startColumn) {
                            {
                                consume();
                            }
                        }
                        : new Token(PLUS, "+", startLine, startColumn);
            case '-':
                return (peek() == '=')
                        ? new Token(MINUS_ASSIGN, "-=", startLine, startColumn) {
                            {
                                consume();
                            }
                        }
                        : new Token(MINUS, "-", startLine, startColumn);
            case '*':
                return (peek() == '=')
                        ? new Token(MUL_ASSIGN, "*=", startLine, startColumn) {
                            {
                                consume();
                            }
                        }
                        : new Token(MUL, "*", startLine, startColumn);
            case '/':
                return (peek() == '=')
                        ? new Token(DIV_ASSIGN, "/=", startLine, startColumn) {
                            {
                                consume();
                            }
                        }
                        : new Token(DIV, "/", startLine, startColumn);
            // --- NEW SYMBOL LOGIC ---
            case '~':
                if (peek() == '|') {
                    consume();
                    return new Token(TILDE_BAR, "~|", startLine, startColumn);
                } else {
                    return new Token(TILDE, "~", startLine, startColumn);
                }
            // --- END NEW SYMBOL LOGIC ---
            case '%':
                return new Token(MOD, "%", startLine, startColumn);
            case ':':
                return new Token(COLON, ":", startLine, startColumn);
            case '.':
                return new Token(DOT, ".", startLine, startColumn);
            case ',':
                return new Token(COMMA, ",", startLine, startColumn);
            case '(':
                return new Token(LPAREN, "(", startLine, startColumn);
            case ')':
                return new Token(RPAREN, ")", startLine, startColumn);
            case '{':
                return new Token(LBRACE, "{", startLine, startColumn);
            case '}':
                return new Token(RBRACE, "}", startLine, startColumn);
            case '[':
                return new Token(LBRACKET, "[", startLine, startColumn);
            case ']':
                return new Token(RBRACKET, "]", startLine, startColumn);
            default:
                return new Token(INVALID, String.valueOf(c1), startLine, startColumn);
        }
    }

    private void scanLineComment() {
        while (position < input.length() && peek() != '\n') consume();
    }

    private void scanBlockComment() {
        consume();
        consume(); // consume '/*'
        while (position < input.length() - 1) {
            if (peek() == '*' && peek(1) == '/') {
                consume();
                consume();
                return;
            }
            consume();
        }
    }

    private char peek() {
        return peek(0);
    }

    private char peek(int offset) {
        return (position + offset >= input.length()) ? '\0' : input.charAt(position + offset);
    }

    private char consume() {
        char c = input.charAt(position++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }
}