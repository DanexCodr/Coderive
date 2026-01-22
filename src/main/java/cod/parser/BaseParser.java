package cod.parser;

import cod.error.ParseError;
import cod.lexer.Token;
import cod.lexer.TokenType;
import static cod.lexer.TokenType.*;
import cod.parser.context.*;
import cod.semantic.TokenValidator;
import cod.syntax.Keyword;
import static cod.syntax.Keyword.*;
import cod.syntax.Symbol;
import static cod.syntax.Symbol.*;

import java.util.List;

/** Base class for all parser components with ParserState integration. */
public abstract class BaseParser {
  protected final ParserContext ctx;
  protected final List<Token> tokens;

  public BaseParser(ParserContext ctx) {
    this.ctx = ctx;
    this.tokens = ctx.getTokens();
  }
  
    protected boolean is(Symbol... sb) {
    return is(currentToken(), sb);
  }
  
  protected boolean is(Keyword... kw) {
    return is(currentToken(), kw);
  }
  
  protected boolean is(TokenType... type) {
    return is(currentToken(), type);
  }
  
  protected boolean is(Token tk, Symbol... sb) {
    return TokenValidator.is(tk, sb);
  }
  
  protected boolean is(Token tk, Keyword... kw) {
    return TokenValidator.is(tk, kw);
  }

  protected boolean is(Token tk, TokenType...  type) {
    return TokenValidator.is(tk, type);
  }
  
  // === EXPECTATION HELPERS ===

  /**
   * Expect a specific token type. Consumes the token if it matches.
   * @throws ParseError if current token doesn't match expected type
   */
  protected Token expect(TokenType expectedType) {
    return ctx.expect(expectedType);
  }

  /**
   * Expect a specific symbol. Consumes the token if it matches.
   * @throws ParseError if current token doesn't match expected symbol
   */
  protected Token expect(Symbol expectedSymbol) {
    return ctx.expect(expectedSymbol);
  }

  /**
   * Expect a specific keyword. Consumes the token if it matches.
   * @throws ParseError if current token doesn't match expected keyword
   */
  protected Token expect(Keyword expectedKeyword) {
    return ctx.expect(expectedKeyword);
  }

  // === PARSER ACTION INTERFACE ===

  public interface ParserAction<T> {
    T parse() throws ParseError;
  }

  // === STATE MANAGEMENT HELPERS ===

  /** Execute a parsing action with state isolation. Returns both the result and the new state. */
  protected <T> ParseResult<T> withIsolatedState(ParserAction<T> action) {
    ParserState savedState = ctx.getState();
    try {
      T result = action.parse();
      return ParseResult.success(result, ctx.getState());
    } catch (ParseError e) {
      ctx.setState(savedState);
      throw e;
    }
  }

  /**
   * Try a parsing action, returning to original state on failure. Manual backtracking
   * implementation for Java 7.
   */
  protected <T> T attempt(final ParserAction<T> action) {
    ctx.save(); // Save current state
    try {
      T result = action.parse();
      ctx.commit(); // Success - discard saved state
      return result;
    } catch (ParseError e) {
      ctx.restore(); // Failure - restore saved state
      throw e;
    }
  }

  /** Try a parsing action, returning null on failure (no exception thrown). */
  protected <T> T tryParse(ParserAction<T> action) {
    ctx.save(); // Save current state
    try {
      T result = action.parse();
      ctx.commit(); // Success - discard saved state
      return result;
    } catch (ParseError e) {
      ctx.restore(); // Failure - restore saved state
      return null;
    }
  }

  /** Check if a parsing action would succeed without actually parsing. */
  protected boolean lookahead(ParserAction<Boolean> action) {
    ctx.save(); // Save current state
    try {
      Boolean result = action.parse();
      ctx.restore(); // Always restore for lookahead
      return result != null && result;
    } catch (ParseError e) {
      ctx.restore(); // Failure - restore saved state
      return false;
    }
  }

  /** Execute parser action in isolated context without affecting main parser. */
  protected <T> T parseInIsolation(ParserAction<T> action) {
    ParserState originalState = ctx.getState();
    
    try {
      T result = action.parse();
      return result;
    } finally {
      // Always restore original state
      ctx.setState(originalState);
    }
  }

  /** Override in subclasses to create appropriate parser type. */
  protected BaseParser createIsolatedParser(ParserContext isolatedCtx) {
    // Default implementation - subclasses should override
    return new BaseParser(isolatedCtx) {
      // Minimal implementation for lookahead
    };
  }

  protected boolean isClassStartWithoutModifier() {
    Token current = currentToken();
    if (current == null || current.type != ID) return false;

    String name = current.text;
    if (name.length() == 0 || !Character.isUpperCase(name.charAt(0))) {
        return false;
    }

    Token next = lookahead(1);
    if (next == null) return false;

    // Check for: ClassName with Policy { ... }
    if (is(next, KEYWORD) && is(next, WITH)) {
        Token afterWith = lookahead(2);
        if (afterWith != null && is(afterWith, ID)) {
            Token afterPolicy = lookahead(3);
            return afterPolicy != null && is(afterPolicy, LBRACE);
        }
    }
    
    if (is(next, LBRACE)) return true;

    if (is(next, KEYWORD) && is(next, IS)) {
        Token afterIs = lookahead(2);
        if (afterIs != null && is(afterIs, ID)) {
            Token afterParent = lookahead(3);
            return afterParent != null && is(afterParent, LBRACE);
        }
    }

    return false;
  }

  // Method to check for 'with' keyword
  protected boolean isWithKeyword() {
    Token token = currentToken();
    return token != null && is(token, KEYWORD) && is(token, WITH);
  }

  /** Skip whitespace and comments, updating the parser state. */
  protected void skipWhitespaceAndComments() {
    ctx.setState(ctx.getState().skipWhitespaceAndComments());
  }

  /** Get current state with whitespace skipped. */
  protected ParserState getSkippedState() {
    return ctx.getState().skipWhitespaceAndComments();
  }

  // === DELEGATE METHODS ===

  protected Token currentToken() {
    return ctx.current();
  }

  protected Token consume() {
    return ctx.consume();
  }

  protected boolean tryConsume(TokenType type) {
    return ctx.tryConsume(type);
  }

  protected Token peek(int offset) {
    return ctx.peek(offset);
  }

  protected boolean tryConsume(Symbol expectedSymbol) {
    if (is(expectedSymbol)) {
      consume();
      return true;
    }
    return false;
  }

  // === BACKTRACKING ===

  protected void save() {
    ctx.save();
  }

  protected void restore() {
    ctx.restore();
  }

  protected void commit() {
    ctx.commit();
  }

  // === ERROR REPORTING ===

  protected ParseError error(String message) {
    return new ParseError(message, ctx.getLine(), ctx.getColumn());
  }

  protected ParseError error(String message, Token token) {
    return new ParseError(message, token.line, token.column);
  }
  
  protected String getTypeName(TokenType type) {
    return type.toString();
  }

  // === TYPE PARSING ===

  protected boolean isTypeKeyword(Token token) {
    return is(token, INT, TEXT, FLOAT, BOOL, TYPE);
  }

  protected boolean isTypeStart(Token token) {
    if (token == null) return false;
    return isTypeKeyword(token) || is(token, ID) || is(token, LPAREN, LBRACKET);
  }

  protected String parseQualifiedName() {
    StringBuilder name = new StringBuilder();

    // First part - must be ID
    name.append(expect(ID).text);

    while (tryConsume(DOT)) {
      name.append(".");

      Token next = currentToken();
      if (is(next, ID)) {
        name.append(expect(ID).text);
      } else if (is(next, KEYWORD) && canKeywordBeMethodName(next)) {
        name.append(expect(KEYWORD).text);
      } else {
        throw error("Expected identifier or method keyword after '.'");
      }
    }
    return name.toString();
  }

  protected boolean canKeywordBeMethodName(Token token) {
    return is(token, IN, ALL, ANY);
  }

  protected String parseTypeReference() {
    StringBuilder type = new StringBuilder();

    if (is(LBRACKET)) {
      expect(LBRACKET);
      if (is(RBRACKET)) {
        expect(RBRACKET);
        type.append("[]");
      } else {
        String inner = parseTypeReference();
        expect(RBRACKET);
        type.append("[").append(inner).append("]");
      }
    } else if (is(LPAREN)) {
      type.append(parseGroupedType());
    } else {
      Token typeToken = currentToken();
      if (isTypeStart(typeToken) && typeToken.symbol != LBRACKET) {
        String typeName = consume().text;
        type.append(typeName);
      } else {
        throw error("Expected type name");
      }
    }

    if (tryConsume(QUESTION)) {
      return type.toString() + "|none";
    }

    while (is(PIPE)) {
      expect(PIPE);
      type.append("|");
      type.append(parseTypeReference());
    }

    return type.toString();
  }

  private String parseGroupedType() {
    expect(LPAREN);
    StringBuilder group = new StringBuilder("(");

    group.append(parseTypeReference());

    while (tryConsume(COMMA)) {
      group.append(",");
      group.append(parseTypeReference());
    }

    expect(RPAREN);
    group.append(")");
    return group.toString();
  }

  // === VISIBILITY MODIFIER ===

  protected boolean isVisibilityModifier() {
    return is(SHARE, LOCAL);
  }

  protected boolean isVisibilityModifier(Token token) {
    if (token == null) return false;
    return is(token, KEYWORD) && is(token, SHARE, LOCAL);
  }

  // === POSITION ACCESS ===

  public int getPosition() {
    return ctx.getPosition();
  }

  public int getLine() {
    return ctx.getLine();
  }

  public int getColumn() {
    return ctx.getColumn();
  }

  public ParserState getCurrentState() {
    return ctx.getState();
  }

  public void setState(ParserState state) {
    ctx.setState(state);
  }

  // === LOOKAHEAD HELPERS ===

  protected Token lookahead(int n) {
    return peek(n);
  }

  protected Token lookahead() {
    return peek(1);
  }

  protected boolean isSymbolAt(int offset, Symbol symbol) {
    Token token = peek(offset);
    return token != null && is(token, SYMBOL) && is(token, symbol);
  }

  // === EXPRESSION START CHECK ===

  protected boolean isExpressionStart(Token t) {
    if (t == null) return false;

    if (is(t, WS, LINE_COMMENT, BLOCK_COMMENT)) {
      return false;
    }
    return is(t, INT_LIT, FLOAT_LIT, STRING_LIT, BOOL_LIT, ID)
        || is(t, LPAREN, LBRACKET, BANG, PLUS, MINUS)
        || (is(t, KEYWORD) && is(t, NONE, TRUE, FALSE, SUPER, THIS));
  }

  // === CLASS START CHECK ===

  protected boolean isClassStart() {
    Token current = currentToken();
    if (current == null) return false;

    if (isVisibilityModifier()) {
      return true;
    }

    return isClassStartWithoutModifier();
  }

  /** Check if we're at the start of a statement. */
  protected boolean isStatementStart() {
    Token token = currentToken();
    if (token == null) return false;

    if (is(token, KEYWORD)) {
      return is(token, IF, FOR, EXIT, ELSE, ELIF, SKIP, BREAK, SHARE, LOCAL);
    }

    if (is(token, ID)) {
      Token next = lookahead(1);
      if (next != null) {
        return is(next, COLON, ASSIGN, DOUBLE_COLON_ASSIGN, LBRACKET);
      }
    }

    if (is(token, TILDE_ARROW)) return true;

    return false;
  }
}