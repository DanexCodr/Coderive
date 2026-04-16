package cod.lexer;

import java.util.HashMap;
import java.util.Map;

public enum TokenType {
    KEYWORD,
    INT_LIT,
    FLOAT_LIT,
    TEXT_LIT,
    BOOL_LIT,
    ID,
    SYMBOL,
    EOF,
    INVALID,
    LINE_COMMENT,
    BLOCK_COMMENT,
    WS,
    INTERPOL;
    
      @Override
  public String toString() {
    return name().toLowerCase();
  }

  public enum Keyword {
    SHARE,
    LOCAL,
    UNIT,
    USE,
    IS,
    THIS,
    SUPER,
    IF,
    ELSE,
    ELIF,
    OF,
    FOR,
    BREAK,
    SKIP,
    TO,
    BY,
    INT,
    TEXT,
    FLOAT,
    BOOL,
    TYPE,
    POLICY,
    WITH,
    BUILTIN,
    ALL,
    ANY,
    EXIT,
    NONE,
    TRUE,
    FALSE,
    GET,
    SET,
    UNSAFE,
    I8,
    I16,
    I32,
    I64,
    U8,
    U16,
    U32,
    U64,
    F32,
    F64;

    private static final Map<String, Keyword> STRING_TO_KEYWORD = new HashMap<>();

    static {
      for (Keyword keyword : values()) {
        STRING_TO_KEYWORD.put(keyword.toString(), keyword);
      }
    }

    public static Keyword fromString(String text) {
      return STRING_TO_KEYWORD.get(text.toLowerCase());
    }

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }

  public enum Symbol {
    EQ,
    ASSIGN,
    GT,
    GTE,
    LT,
    LTE,
    NEQ,
    BANG,
    PLUS,
    PLUS_ASSIGN,
    MINUS,
    MINUS_ASSIGN,
    MUL,
    MUL_ASSIGN,
    DIV,
    DIV_ASSIGN,
    LAMBDA,
    MOD,
    DOUBLE_COLON,
    DOUBLE_COLON_ASSIGN,
    TILDE_ARROW,
    COLON,
    DOT,
    COMMA,
    LPAREN,
    RPAREN,
    LBRACE,
    RBRACE,
    LBRACKET,
    RBRACKET,
    PIPE,
    QUESTION,
    AMPERSAND,
    DOLLAR,
    UNDERSCORE,
    RANGE_DOTDOT,
    RANGE_HASH
  }
}
