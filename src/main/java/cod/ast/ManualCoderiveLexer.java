package cod.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cod.Constants.*;

public class ManualCoderiveLexer {

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

    public static class TokenType {
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
                ALL = 57,
                ANY = 58,
                BANG = 59,
                INVALID = -2;
        
        private static final Map<Integer, String> NAMES = new HashMap<Integer, String>();
        
        static {
            NAMES.put(SHARE, _share);
            NAMES.put(LOCAL, _local);
            NAMES.put(UNIT, _unit);
            NAMES.put(GET, _get);
            NAMES.put(EXTEND, _extend);
            NAMES.put(THIS, _this);
            NAMES.put(VAR, _var);
            NAMES.put(OUTPUT, _output);
            NAMES.put(INPUT, _input);
            NAMES.put(IF, _if);
            NAMES.put(ELSE, _else);
            NAMES.put(ELIF, _elif);
            NAMES.put(FOR, _for);
            NAMES.put(IN, _in);
            NAMES.put(TO, _to);
            NAMES.put(BY, _by);
            NAMES.put(INT, _int);
            NAMES.put(STRING, _string);
            NAMES.put(FLOAT, _float);
            NAMES.put(BOOL, _bool);
            NAMES.put(BUILTIN, _builtin);
            NAMES.put(ALL, _all);
            NAMES.put(ANY, _any);
            
            NAMES.put(INT_LIT, _int_lit);
            NAMES.put(FLOAT_LIT, _float_lit);
            NAMES.put(STRING_LIT, _string_lit);
            NAMES.put(BOOL_LIT, _bool_lit);
            NAMES.put(ID, _id);
            
            NAMES.put(ASSIGN, _assign);
            NAMES.put(PLUS, _plus);
            NAMES.put(MINUS, _minus);
            NAMES.put(MUL, _mul);
            NAMES.put(DIV, _div);
            NAMES.put(MOD, _mod);
            NAMES.put(COLON, _colon);
            NAMES.put(GT, _gt);
            NAMES.put(LT, _lt);
            NAMES.put(GTE, _gte);
            NAMES.put(LTE, _lte);
            NAMES.put(EQ, _eq);
            NAMES.put(NEQ, _neq);
            NAMES.put(DOT, _dot);
            NAMES.put(COMMA, _comma);
            NAMES.put(LPAREN, _lparen);
            NAMES.put(RPAREN, _rparen);
            NAMES.put(LBRACE, _lbrace);
            NAMES.put(RBRACE, _rbrace);
            NAMES.put(LBRACKET, _lbracket);
            NAMES.put(RBRACKET, _rbracket);
            NAMES.put(PLUS_ASSIGN, _plus_assign);
            NAMES.put(MINUS_ASSIGN, _minus_assign);
            NAMES.put(MUL_ASSIGN, _mul_assign);
            NAMES.put(DIV_ASSIGN, _div_assign);
            NAMES.put(BANG, _bang);
            NAMES.put(TILDE_BAR, _tilde_bar);
            NAMES.put(TILDE_ARROW, _tilde_arrow);
            
            NAMES.put(Token.EOF, _eof);
            NAMES.put(INVALID, _invalid);
            NAMES.put(LINE_COMMENT, _line_comment);
            NAMES.put(BLOCK_COMMENT, _block_comment);
            NAMES.put(WS, _ws);
        }
        
        public static String getName(int type) {
            String name = NAMES.get(type);
            return name != null ? name : "UNKNOWN(" + type + ")";
        }
    }

    private static final Map<String, Integer> KEYWORDS = new HashMap<String, Integer>();

    static {
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
        KEYWORDS.put(all, TokenType.ALL);
        KEYWORDS.put(any, TokenType.ANY);
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
                    return new Token(TokenType.BANG, "!", startLine, startColumn);
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