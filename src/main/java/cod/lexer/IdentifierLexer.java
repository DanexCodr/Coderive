package cod.lexer;

import cod.syntax.Keyword;
import java.util.*;

public class IdentifierLexer {

    private final MainLexer lexer;
    private final List<String> extractedIdentifiers;
    private final Set<String> keywords;

    public IdentifierLexer(MainLexer lexer) {
        this.lexer = lexer;
        this.extractedIdentifiers = new ArrayList<String>();
        this.keywords = new HashSet<String>();
        
        for (Keyword keyword : Keyword.values()) {
            keywords.add(keyword.toString());
        }
    }

    public Token scan() {
        char c = lexer.peek();
        if (Character.isLetter(c) || c == '_') {
            return readIdentifierOrKeyword();
        }
        return null;
    }

    private Token readIdentifierOrKeyword() {
        int line = lexer.line;
        int col = lexer.column;

        StringBuilder sb = new StringBuilder();
        while (lexer.getPosition() < lexer.getInput().length() && 
               (Character.isLetterOrDigit(lexer.peek()) || lexer.peek() == '_')) {
            sb.append(lexer.consume());
        }
        String text = sb.toString();

        Keyword keyword = Keyword.fromString(text);
        if (keyword != null) {
            return Token.createKeyword(text, line, col, keyword);
        }

        extractedIdentifiers.add(text);
        return Token.createIdentifier(text, line, col);
    }

    public List<String> extractAllIdentifiers() {
        extractedIdentifiers.clear();
        int savedPos = lexer.getPosition();
        int savedLine = lexer.line;
        int savedCol = lexer.column;

        lexer.setPosition(0);
        lexer.line = 1;
        lexer.column = 1;

        while (lexer.getPosition() < lexer.getInput().length()) {
            Token token = scan();
            if (token == null) {
                lexer.consume();
            }
        }

        lexer.setPosition(savedPos);
        lexer.line = savedLine;
        lexer.column = savedCol;

        return new ArrayList<String>(extractedIdentifiers);
    }

    public boolean isKeyword(String text) {
        return keywords.contains(text);
    }
}