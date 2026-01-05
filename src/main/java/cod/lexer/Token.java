package cod.lexer;

import cod.syntax.Symbol;

public class Token {
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