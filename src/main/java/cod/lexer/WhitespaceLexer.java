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
        int startPos = lexer.getPosition();
        int length = 0;

        while (lexer.getPosition() < lexer.getInput().length && 
               Character.isWhitespace(lexer.peek())) {
            lexer.consume();
            length++;
        }

        char[] source = lexer.getInputArray();
        return new Token(TokenType.WS, source, startPos, length, 
                         startLine, startCol, null, null, null, null);
    }

    public boolean isAtWhitespace() {
        return Character.isWhitespace(lexer.peek());
    }
}