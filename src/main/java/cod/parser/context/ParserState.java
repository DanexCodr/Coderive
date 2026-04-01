package cod.parser.context;

import cod.lexer.Token;
import java.util.List;
import java.util.Objects;

public final class ParserState {
  private final List<Token> tokens;
  private final int position;
  private final int line;
  private final int column;

  private transient Token currentTokenCache;

  public ParserState(List<Token> tokens) {
    this(tokens, 0, 1, 1);
  }

  private ParserState(List<Token> tokens, int position, int line, int column) {
    this.tokens = Objects.requireNonNull(tokens, "tokens cannot be null");
    this.position = position;
    this.line = line;
    this.column = column;
    updateCurrentTokenCache();
  }

  private void updateCurrentTokenCache() {
    if (position >= 0 && position < tokens.size()) {
      currentTokenCache = tokens.get(position);
    } else {
      currentTokenCache = null;
    }
  }

  public ParserState advance() {
    if (position >= tokens.size()) {
      return this;
    }

    Token current = now();
    if (current == null) {
      return this;
    }

    int newPosition = position + 1;
    int newLine = current.line;
    int newColumn = current.column + current.getLength();

    if (newPosition < tokens.size()) {
      Token next = tokens.get(newPosition);
      newLine = next.line;
      newColumn = next.column;
    }

    return new ParserState(tokens, newPosition, newLine, newColumn);
  }

  public ParserState withPosition(int newPosition) {
    if (newPosition < 0 || newPosition > tokens.size()) {
      throw new IllegalArgumentException("Invalid position: " + newPosition);
    }

    if (newPosition == position) {
      return this;
    }

    int newLine = 1;
    int newColumn = 1;

    if (newPosition < tokens.size()) {
      Token token = tokens.get(newPosition);
      newLine = token.line;
      newColumn = token.column;
    } else if (!tokens.isEmpty()) {
      Token lastToken = tokens.get(tokens.size() - 1);
      newLine = lastToken.line;
      newColumn = lastToken.column + lastToken.getLength();
    }

    return new ParserState(tokens, newPosition, newLine, newColumn);
  }

  public ParserState withPositionAndLineCol(int newPosition, int newLine, int newColumn) {
    return new ParserState(tokens, newPosition, newLine, newColumn);
  }

  public Token now() {
    return currentTokenCache;
  }

  public Token next(int offset) {
    int targetPos = position + offset;
    return targetPos >= 0 && targetPos < tokens.size() ? tokens.get(targetPos) : null;
  }

  public boolean hasMore() {
    return position < tokens.size();
  }

  public boolean atEOF() {
    return !hasMore();
  }

  public List<Token> getTokens() {
    return tokens;
  }

  public int getPosition() {
    return position;
  }

  public int getLine() {
    return line;
  }

  public int getColumn() {
    return column;
  }

  public ParserState copy() {
    return new ParserState(tokens, position, line, column);
  }

  @Override
  public String toString() {
    Token current = now();
    return String.format(
        "ParserState[pos=%d, line=%d, col=%d, current=%s]",
        position, line, column, current != null ? "'" + current.getText() + "'" : "EOF");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ParserState that = (ParserState) o;
    return position == that.position
        && line == that.line
        && column == that.column
        && tokens.equals(that.tokens);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tokens, position, line, column);
  }
}