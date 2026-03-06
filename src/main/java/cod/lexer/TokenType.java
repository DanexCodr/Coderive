package cod.lexer;

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
  }