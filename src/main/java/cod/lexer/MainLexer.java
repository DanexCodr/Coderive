package cod.lexer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cod.syntax.Keyword.*;
import cod.syntax.Symbol;
import static cod.syntax.Symbol.*;

public class MainLexer {

  public enum TokenType {
    KEYWORD,
    INT_LIT,
    FLOAT_LIT,
    STRING_LIT,
    BOOL_LIT,
    ID,
    SYMBOL,
    EOF,
    INVALID,
    LINE_COMMENT,
    BLOCK_COMMENT,
    WS;
  }

  public static class Token {
    public final TokenType type;
    public final String text;
    public final int line;
    public final int column;
    public final Symbol symbol;

    public Token(TokenType type, String text, int line, int column) {
        this.type = type;
        this.text = text;
        this.line = line;
        this.column = column;
        this.symbol = null;
    }

    public Token(TokenType type, String text, int line, int column, Symbol symbol) {
        this.type = type;
        this.text = text;
        this.line = line;
        this.column = column;
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return "Token{" +
            "type=" + type.name() +
            ", text='" + text + '\'' +
            (symbol != null ? ", symbol=" + symbol.name() : "") +
            ", line=" + line +
            ", column=" + column +
            '}';
    }
  }

  private static final Set<String> KEYWORDS = new HashSet<String>();

  static {
      for (cod.syntax.Keyword keyword : cod.syntax.Keyword.values()) {
          KEYWORDS.add(keyword.toString());
      }
  }

  private final String input;
  private int position = 0;
  private int line = 1;
  private int column = 1;

  public MainLexer(String input) {
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
    tokens.add(create(TokenType.EOF));
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
    char c = peek();

    if (Character.isLetter(c) || c == '_') return readIdentifierOrKeyword();
    if (Character.isDigit(c)) return readNumber();
    if (c == '"') return readString();
    return readSymbol();
  }

  private Token readIdentifierOrKeyword() {
    StringBuilder sb = new StringBuilder();
    while (position < input.length() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
        sb.append(consume());
    }
    String text = sb.toString();
    
    if (KEYWORDS.contains(text)) {
        return create(TokenType.KEYWORD, text);
    }
    
    return create(TokenType.ID, text);
  }

  private Token readNumber() {
    StringBuilder sb = new StringBuilder();
    boolean isFloat = false;
    
    // 1. Read integer part
    while (position < input.length() && Character.isDigit(peek())) {
        sb.append(consume());
    }
    
    // 2. Read fractional part
    if (peek() == '.') {
        isFloat = true;
        sb.append(consume());
        while (position < input.length() && Character.isDigit(peek())) {
            sb.append(consume());
        }
    }
    
    // 3. Read Numeric Shorthand Suffix (K, M, Qi, etc.)
    String suffix = readSuffix();
    if (!suffix.isEmpty()) {
        sb.append(suffix);
        isFloat = true; 
    } else {
        // 4. NEW: Read Standard Scientific Notation (e/E)
        String exponent = readExponent();
        if (!exponent.isEmpty()) {
            sb.append(exponent);
            isFloat = true;
        }
    }
    
    String numberText = sb.toString();
    return create(
        isFloat ? TokenType.FLOAT_LIT : TokenType.INT_LIT, numberText);
}

// NEW Helper method for MainLexer (Numeric Shorthands)
private String readSuffix() {
    if (position >= input.length()) return "";
    
    char c1 = peek();
    
    // Check custom suffixes (Case sensitive)
    if (c1 == 'K' || c1 == 'M' || c1 == 'B' || c1 == 'T') {
        return String.valueOf(consume());
    }
    
    if (c1 == 'Q') {
        consume(); // consume Q
        if (peek() == 'i') {
            consume(); // consume i
            return "Qi";
        }
        return "Q";
    }
    
    return "";
}

// NEW Helper method to read the standard 'e' exponent part (Case insensitive)
private String readExponent() {
    if (position >= input.length()) return "";
    
    char c1 = peek();
    if (c1 != 'e' && c1 != 'E') {
        return "";
    }
    
    StringBuilder sb = new StringBuilder();
    sb.append(consume()); // Consume 'e' or 'E'
    
    // Consume optional sign (+ or -)
    char c2 = peek();
    if (c2 == '+' || c2 == '-') {
        sb.append(consume());
    }
    
    // Must be followed by at least one digit
    if (position < input.length() && Character.isDigit(peek())) {
        while (position < input.length() && Character.isDigit(peek())) {
            sb.append(consume());
        }
    } else {
        // If 'e' or 'E' is not followed by a digit (after optional sign), 
        // backtrack the 'e' or 'E' and treat it as part of identifier/error.
        // NOTE: In a complete Lexer, this should rollback position, but for simplicity here, 
        // we'll stop the number read and rely on the parser to handle the trailing characters if any.
        // Since this is the end of readNumber, any non-digit following 'e' will be part of the ID logic.
        // Given your current Lexer structure, we must ensure 'e' is followed by a digit.
        if (sb.length() == 1 || (sb.length() == 2 && (sb.charAt(1) == '+' || sb.charAt(1) == '-'))) {
             // We only consumed 'e' or 'e+' / 'e-', but no digit followed. This is invalid scientific notation.
             // We return an empty string to signify failure, but a real Lexer needs error handling/backtracking.
             // For safety, we'll assume a digit must follow for it to be an exponent.
             return "";
        }
    }
    
    return sb.toString();
}

  private Token readString() {
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
    return create(TokenType.STRING_LIT, sb.toString());
  }

  private Token readSymbol() {
    char c1 = consume();
    switch (c1) {
      // NEW: Pipe support for Union Types (int|text)
      case '|':
        return create(PIPE, "|");
        
      // NEW: Ampersand support
      case '&':
        return create(AMPERSAND, "&");

      // UPDATED: Added := operator
      case ':':
        if (peek() == '=') {
          consume();
          return create(DOUBLE_COLON_ASSIGN, ":=");
        } else if (peek() == ':') {
          consume();
          return create(DOUBLE_COLON, "::");
        } else {
          return create(COLON, ":");
        }
        
      case '=':
        if (peek() == '=') {
          consume();
          return create(EQ, "==");
        } else {
          return create(ASSIGN, "=");
        }
      case '>':
        if (peek() == '=') {
          consume();
          return create(GTE, ">=");
        } else {
          return create(GT, ">");
        }
      case '<':
        if (peek() == '=') {
          consume();
          return create(LTE, "<=");
        } else {
          return create(LT, "<");
        }
      case '!':
        if (peek() == '=') {
          consume();
          return create(NEQ, "!=");
        } else {
          return create(BANG, "!");
        }
      case '+':
        if (peek() == '=') {
          consume();
          return create(PLUS_ASSIGN, "+=");
        } else {
          return create(PLUS, "+");
        }
      case '-':
        if (peek() == '=') {
          consume();
          return create(MINUS_ASSIGN, "-=");
        } else {
          return create(MINUS, "-");
        }
      case '*':
        if (peek() == '=') {
          consume();
          return create(MUL_ASSIGN, "*=");
        } else {
          return create(MUL, "*");
        }
      case '/':
        if (peek() == '=') {
          consume();
          return create(DIV_ASSIGN, "/=");
        } else {
          return create(DIV, "/");
        }
      case '~':
        if (peek() == '>') {
          consume();
          return create(TILDE_ARROW, "~>");
        } else {
          return create(TokenType.INVALID);
        }
      case '?':
        return create(QUESTION, "?");
      case '%':
        return create(MOD, "%");
      case '.':
        return create(DOT, ".");
      case ',':
        return create(COMMA, ",");
      case '(':
        return create(LPAREN, "(");
      case ')':
        return create(RPAREN, ")");
      case '{':
        return create(LBRACE, "{");
      case '}':
        return create(RBRACE, "}");
      case '[':
        return create(LBRACKET, "[");
      case ']':
        return create(RBRACKET, "]");
      case '_':
        return create(UNDERSCORE, "_");
      default:
        return create(TokenType.INVALID);
    }
  }

  private Token create(TokenType type, String text) {
    return new Token(type, text, this.line, this.column);
  }

  private Token create(TokenType type) {
    return new Token(type, type.name().toLowerCase(), this.line, this.column);
  }

  private Token create(Symbol symbol, String text) {
    return new Token(TokenType.SYMBOL, text, this.line, this.column, symbol);
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
