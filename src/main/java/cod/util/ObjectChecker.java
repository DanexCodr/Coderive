package cod.util;

import cod.lexer.*;
import cod.syntax.*;

public final class ObjectChecker {
  private ObjectChecker() {}

  // TokenType check - FIXED
  public static boolean is(Token token, TokenType... types) {
    if (token == null) return false;
    if (token.type == null) return false;
    for (TokenType type : types) {
      if (token.type == type) return true;
    }
    return false;
  }

  // Symbol check - FIXED
  public static boolean is(Token token, Symbol... symbols) {
    if (token == null) return false;
    if (token.type != TokenType.SYMBOL) return false;
    if (token.symbol == null) return false;
    for (Symbol symbol : symbols) {
      if (token.symbol == symbol) return true;
    }
    return false;
  }

  // Keyword check - FIXED
  public static boolean is(Token token, Keyword... keywords) {
    if (token == null) return false;
    if (token.type != TokenType.KEYWORD) return false;
    if (token.keyword == null) return false;
    for (Keyword keyword : keywords) {
      if (token.keyword == keyword) return true;
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
      throw new IllegalArgumentException("nil() of ObjectChecker requires at least one object.");
    for (Object obj : objects) {
      if (obj == null) return true;
    }
    return false;
  }
}
