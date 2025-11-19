package cod.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cod.Constants.*;

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
                    + TokenType.getName(type)
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

    /**
     * Self-contained token type constants with their names.
     */
    public static class TokenType {
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
                TILDE_BAR = 54,
                TILDE_ARROW = 55,
                BUILTIN = 56,
                INVALID = -2;
        
        private static final Map<Integer, String> NAMES = new HashMap<Integer, String>();
        
        static {
            // Keywords
            NAMES.put(SHARE, "SHARE");
            NAMES.put(LOCAL, "LOCAL");
            NAMES.put(UNIT, "UNIT");
            NAMES.put(GET, "GET");
            NAMES.put(EXTEND, "EXTEND");
            NAMES.put(THIS, "THIS");
            NAMES.put(VAR, "VAR");
            NAMES.put(OUTPUT, "OUTPUT");
            NAMES.put(INPUT, "INPUT");
            NAMES.put(IF, "IF");
            NAMES.put(ELSE, "ELSE");
            NAMES.put(ELIF, "ELIF");
            NAMES.put(FOR, "FOR");
            NAMES.put(IN, "IN");
            NAMES.put(TO, "TO");
            NAMES.put(BY, "BY");
            NAMES.put(INT, "INT");
            NAMES.put(STRING, "STRING");
            NAMES.put(FLOAT, "FLOAT");
            NAMES.put(BOOL, "BOOL");
            NAMES.put(BUILTIN, "BUILTIN");
            
            // Literals
            NAMES.put(INT_LIT, "INT_LIT");
            NAMES.put(FLOAT_LIT, "FLOAT_LIT");
            NAMES.put(STRING_LIT, "STRING_LIT");
            NAMES.put(BOOL_LIT, "BOOL_LIT");
            NAMES.put(ID, "ID");
            
            // Operators and symbols
            NAMES.put(ASSIGN, "ASSIGN");
            NAMES.put(PLUS, "PLUS");
            NAMES.put(MINUS, "MINUS");
            NAMES.put(MUL, "MUL");
            NAMES.put(DIV, "DIV");
            NAMES.put(MOD, "MOD");
            NAMES.put(COLON, "COLON");
            NAMES.put(GT, "GT");
            NAMES.put(LT, "LT");
            NAMES.put(GTE, "GTE");
            NAMES.put(LTE, "LTE");
            NAMES.put(EQ, "EQ");
            NAMES.put(NEQ, "NEQ");
            NAMES.put(DOT, "DOT");
            NAMES.put(COMMA, "COMMA");
            NAMES.put(LPAREN, "LPAREN");
            NAMES.put(RPAREN, "RPAREN");
            NAMES.put(LBRACE, "LBRACE");
            NAMES.put(RBRACE, "RBRACE");
            NAMES.put(LBRACKET, "LBRACKET");
            NAMES.put(RBRACKET, "RBRACKET");
            NAMES.put(PLUS_ASSIGN, "PLUS_ASSIGN");
            NAMES.put(MINUS_ASSIGN, "MINUS_ASSIGN");
            NAMES.put(MUL_ASSIGN, "MUL_ASSIGN");
            NAMES.put(DIV_ASSIGN, "DIV_ASSIGN");
            NAMES.put(TILDE_BAR, "TILDE_BAR");
            NAMES.put(TILDE_ARROW, "TILDE_ARROW");
            
            // Special tokens
            NAMES.put(Token.EOF, "EOF");
            NAMES.put(INVALID, "INVALID");
            NAMES.put(LINE_COMMENT, "LINE_COMMENT");
            NAMES.put(BLOCK_COMMENT, "BLOCK_COMMENT");
            NAMES.put(WS, "WS");
        }
        
        public static String getName(int type) {
            String name = NAMES.get(type);
            return name != null ? name : "UNKNOWN(" + type + ")";
        }
    }

    private static final Map<String, Integer> KEYWORDS = new HashMap<String, Integer>();

    static {
        // Keywords mapping
        KEYWORDS.put(share, TokenType.SHARE);
        KEYWORDS.put(local, TokenType.LOCAL);
        KEYWORDS.put(unit, TokenType.UNIT);
        KEYWORDS.put(get, TokenType.GET);
        KEYWORDS.put(extend, TokenType.EXTEND);
        KEYWORDS.put(this_, TokenType.THIS);
        KEYWORDS.put(var, TokenType.VAR);
        KEYWORDS.put(output, TokenType.OUTPUT);
        KEYWORDS.put(input_, TokenType.INPUT);
        KEYWORDS.put(if_, TokenType.IF);
        KEYWORDS.put(else_, TokenType.ELSE);
        KEYWORDS.put(elif_, TokenType.ELIF);
        KEYWORDS.put(for_, TokenType.FOR);
        KEYWORDS.put(in_, TokenType.IN);
        KEYWORDS.put(to_, TokenType.TO);
        KEYWORDS.put(by_, TokenType.BY);
        KEYWORDS.put(int_, TokenType.INT);
        KEYWORDS.put(string, TokenType.STRING);
        KEYWORDS.put(float_, TokenType.FLOAT);
        KEYWORDS.put(builtin, TokenType.BUILTIN);
        KEYWORDS.put(bool, TokenType.BOOL);
        KEYWORDS.put(true_, TokenType.BOOL_LIT);
        KEYWORDS.put(false_, TokenType.BOOL_LIT);
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
        return new Token(type != null ? type : TokenType.ID, text, startLine, startColumn);
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
        return new Token(isFloat ? TokenType.FLOAT_LIT : TokenType.INT_LIT, sb.toString(), startLine, startColumn);
    }

    private Token readString(int startLine, int startColumn) {
        StringBuilder sb = new StringBuilder();
        consume(); // Consume opening quote
        while (position < input.length() && peek() != '"') {
            if (peek() == '\\') {
                consume(); // consume the backslash
                char escaped = consume();
                switch (escaped) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case '\\': sb.append('\\'); break;
                    case '"': sb.append('"'); break;
                    default: sb.append('\\').append(escaped); break;
                }
            } else {
                sb.append(consume());
            }
        }
        if (position < input.length()) consume(); // Consume closing quote
        return new Token(TokenType.STRING_LIT, sb.toString(), startLine, startColumn);
    }

    private Token readSymbol(int startLine, int startColumn) {
        char c1 = consume();
        switch (c1) {
            case '=':
                if (peek() == '=') {
                    consume();
                    return new Token(TokenType.EQ, "==", startLine, startColumn);
                } else {
                    return new Token(TokenType.ASSIGN, "=", startLine, startColumn);
                }
            case '>':
                if (peek() == '=') {
                    consume();
                    return new Token(TokenType.GTE, ">=", startLine, startColumn);
                } else {
                    return new Token(TokenType.GT, ">", startLine, startColumn);
                }
            case '<':
                if (peek() == '=') {
                    consume();
                    return new Token(TokenType.LTE, "<=", startLine, startColumn);
                } else {
                    return new Token(TokenType.LT, "<", startLine, startColumn);
                }
            case '!':
                if (peek() == '=') {
                    consume();
                    return new Token(TokenType.NEQ, "!=", startLine, startColumn);
                } else {
                    return new Token(TokenType.INVALID, "!", startLine, startColumn);
                }
            case '+':
                if (peek() == '=') {
                    consume();
                    return new Token(TokenType.PLUS_ASSIGN, "+=", startLine, startColumn);
                } else {
                    return new Token(TokenType.PLUS, "+", startLine, startColumn);
                }
            case '-':
                if (peek() == '=') {
                    consume();
                    return new Token(TokenType.MINUS_ASSIGN, "-=", startLine, startColumn);
                } else {
                    return new Token(TokenType.MINUS, "-", startLine, startColumn);
                }
            case '*':
                if (peek() == '=') {
                    consume();
                    return new Token(TokenType.MUL_ASSIGN, "*=", startLine, startColumn);
                } else {
                    return new Token(TokenType.MUL, "*", startLine, startColumn);
                }
            case '/':
                if (peek() == '=') {
                    consume();
                    return new Token(TokenType.DIV_ASSIGN, "/=", startLine, startColumn);
                } else {
                    return new Token(TokenType.DIV, "/", startLine, startColumn);
                }
            case '~':
                if (peek() == '|') {
                    consume();
                    return new Token(TokenType.TILDE_BAR, "~|", startLine, startColumn);
                } else if (peek() == '>') {
                    consume();
                    return new Token(TokenType.TILDE_ARROW, "~>", startLine, startColumn);
                } else {
                    return new Token(TokenType.INVALID, "~", startLine, startColumn);
                }
            case '%':
                return new Token(TokenType.MOD, "%", startLine, startColumn);
            case ':':
                return new Token(TokenType.COLON, ":", startLine, startColumn);
            case '.':
                return new Token(TokenType.DOT, ".", startLine, startColumn);
            case ',':
                return new Token(TokenType.COMMA, ",", startLine, startColumn);
            case '(':
                return new Token(TokenType.LPAREN, "(", startLine, startColumn);
            case ')':
                return new Token(TokenType.RPAREN, ")", startLine, startColumn);
            case '{':
                return new Token(TokenType.LBRACE, "{", startLine, startColumn);
            case '}':
                return new Token(TokenType.RBRACE, "}", startLine, startColumn);
            case '[':
                return new Token(TokenType.LBRACKET, "[", startLine, startColumn);
            case ']':
                return new Token(TokenType.RBRACKET, "]", startLine, startColumn);
            default:
                return new Token(TokenType.INVALID, String.valueOf(c1), startLine, startColumn);
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