package cod.lexer;

public class WhitespaceLexer {

    private final MainLexer lexer;

    public WhitespaceLexer(MainLexer lexer) {
        this.lexer = lexer;
    }

    public Token scan() {
        if (Character.isWhitespace(lexer.peek())) {
            return scanWhitespace();
        }
        return null;
    }

    private Token scanWhitespace() {
        int startLine = lexer.line;
        int startCol = lexer.column;
        StringBuilder text = new StringBuilder();

        while (lexer.getPosition() < lexer.getInput().length() && 
               Character.isWhitespace(lexer.peek())) {
            text.append(lexer.consume());
        }

        return new Token(TokenType.WS, text.toString(), startLine, startCol, 
                        null, null, null, null);
    }

    public boolean isAtWhitespace() {
        return Character.isWhitespace(lexer.peek());
    }
}