package cod.semantic;

import cod.lexer.*;
import cod.lexer.TokenType.Keyword;
import cod.lexer.TokenType.Symbol;

public final class ObjectValidator {
  private ObjectValidator() {}

  // TokenType check - optimized with direct type comparison
  public static boolean is(Token token, TokenType... types) {
    if (token == null) return false;
    TokenType tokenType = token.type;
    if (tokenType == null) return false;
    for (TokenType type : types) {
      if (tokenType == type) return true;
    }
    return false;
  }

  // Symbol check - fast direct symbol comparison
  public static boolean is(Token token, Symbol... symbols) {
    if (token == null) return false;
    if (token.type != TokenType.SYMBOL) return false;
    Symbol tokenSymbol = token.symbol;
    if (tokenSymbol == null) return false;
    for (Symbol symbol : symbols) {
      if (tokenSymbol == symbol) return true;
    }
    return false;
  }

  // Keyword check - fast direct keyword comparison
  public static boolean is(Token token, Keyword... keywords) {
    if (token == null) return false;
    if (token.type != TokenType.KEYWORD) return false;
    Keyword tokenKeyword = token.keyword;
    if (tokenKeyword == null) return false;
    for (Keyword keyword : keywords) {
      if (tokenKeyword == keyword) return true;
    }
    return false;
  }
  
  public static boolean any(boolean... values) {
    if (values == null) return false;
    for (boolean bool : values) {
      if (bool) return true;
    }
    return false;
  }

  public static boolean nil(Object... objects) {
    if (objects == null || objects.length == 0)
      throw new IllegalArgumentException("nil() of ObjectValidator requires at least one object.");
    for (Object obj : objects) {
      if (obj == null) return true;
    }
    return false;
  }
}
