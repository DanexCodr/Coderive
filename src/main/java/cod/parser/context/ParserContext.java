package cod.parser.context;

import cod.lexer.Token;
import cod.lexer.TokenType;
import cod.error.ParseError;
import cod.semantic.TokenValidator;
import cod.syntax.*;
import java.util.List;
import java.util.Stack;

public final class ParserContext {
  private ParserState state;
  private final Stack<ParserState> backtrackStack = new Stack<>();

  public ParserContext(ParserState initialState) {
    this.state = initialState;
  }

  public ParserContext(List<Token> tokens) {
    this.state = new ParserState(tokens);
  }

  // === CORE METHODS ===

  public Token current() {
    return state.currentToken();
  }

  public Token consume() {
    Token token = current();
    state = state.advance();
    return token;
  }
  
  private boolean is(Token tk, Symbol... sb) {
    return TokenValidator.is(tk, sb);
  }
  
  private boolean is(Token tk, Keyword... kw) {
    return TokenValidator.is(tk, kw);
  }

  private boolean is(Token tk, TokenType...  type) {
    return TokenValidator.is(tk, type);
  }

  public Token expect(TokenType expected) throws ParseError {
    Token token = current();
    if (token != null && is(token, expected)) {
      state = state.advance();
      return token;
    }
    throw new ParseError(
        "Expected "
            + expected
            + ", got "
            + (token != null ? token.type : "EOF")
            + (token != null && token.text != null ? " ('" + token.text + "')" : ""),
        token != null ? token.line : state.getLine(),
        token != null ? token.column : state.getColumn());
  }

  public Token expect(Symbol expected) throws ParseError {
    Token token = state.currentToken();
    if (token != null && is(token, TokenType.SYMBOL) && is(token, expected)) {
      state = state.advance();
      return token;
    }
    throw new ParseError(
        "Expected symbol '"
            + expected
            + "', got "
            + (token != null ? token.type : "EOF")
            + (token != null && token.text != null ? " ('" + token.text + "')" : ""),
        token != null ? token.line : state.getLine(),
        token != null ? token.column : state.getColumn());
  }

  public Token expect(Keyword expected) throws ParseError {
    Token token = state.currentToken();
    if (token != null
        && is(token, TokenType.KEYWORD)
        && is(token, expected)) {
      state = state.advance();
      return token;
    }
    throw new ParseError(
        "Expected keyword '"
            + expected
            + (token != null ? "', got " + token.type + " ('" + token.text + "')" : "', got EOF"),
        token);
  }

  public boolean tryConsume(TokenType expected) {
    Token token = state.currentToken();
    if (token != null && is(token, expected)) {
      state = state.advance();
      return true;
    }
    return false;
  }

  // === BACKTRACKING ===

  public void save() {
    backtrackStack.push(state);
  }

  public void restore() {
    if (!backtrackStack.isEmpty()) {
      state = backtrackStack.pop();
    }
  }

// CRITICAL: Never change to backtrackStack.clear()
  public void commit() {
    if (!backtrackStack.isEmpty()) {
        backtrackStack.pop(); // Just pop, don't clear all
    }
}

  // === LOOKAHEAD ===

  public Token peek(int offset) {
    return state.peek(offset);
  }

  // === STATE MANAGEMENT ===

  public ParserState getState() {
    return state;
  }

  public void setState(ParserState newState) {
    this.state = newState;
  }

  public void reset() {
    this.state = new ParserState(state.getTokens());
    this.backtrackStack.clear();
  }

  public void resetTo(int position) {
    this.state = state.withPosition(position);
    this.backtrackStack.clear();
  }

  // === UTILITIES ===

  public int getPosition() {
    return state.getPosition();
  }

  public int getLine() {
    return state.getLine();
  }

  public int getColumn() {
    return state.getColumn();
  }

  public boolean hasMore() {
    return state.hasMore();
  }

  public boolean atEOF() {
    return state.atEOF();
  }

  public List<Token> getTokens() {
    return state.getTokens();
  }

  @Override
  public String toString() {
    return state.toString();
  }
}
