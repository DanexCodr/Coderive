package cod.lexer;

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