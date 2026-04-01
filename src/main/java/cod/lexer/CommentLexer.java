package cod.lexer;

import java.util.*;

public class CommentLexer {

    private final MainLexer lexer;
    private final List<Comment> extractedComments;

    public CommentLexer(MainLexer lexer) {
        this.lexer = lexer;
        this.extractedComments = new ArrayList<Comment>();
    }

    public Token scan() {
        if (lexer.peek() == '/' && lexer.peek(1) == '/') {
            return scanLineComment();
        }
        if (lexer.peek() == '/' && lexer.peek(1) == '*') {
            return scanBlockComment();
        }
        return null;
    }

    private Token scanLineComment() {
        int startLine = lexer.line;
        int startCol = lexer.column;
        int startPos = lexer.getPosition();
        int length = 0;

        lexer.consume(); // '/'
        lexer.consume(); // '/'
        length += 2;

        while (lexer.getPosition() < lexer.getInput().length && 
               lexer.peek() != '\n') {
            lexer.consume();
            length++;
        }

        char[] source = lexer.getInputArray();
        Token token = new Token(TokenType.LINE_COMMENT, source, startPos, length,
                                startLine, startCol, null, null, null, null);
        
        String commentText = new String(source, startPos, length);
        extractedComments.add(new Comment(commentText, startLine, startCol, CommentType.LINE));
        return token;
    }

    private Token scanBlockComment() {
        int startLine = lexer.line;
        int startCol = lexer.column;
        int startPos = lexer.getPosition();
        int length = 0;

        lexer.consume(); // '/'
        lexer.consume(); // '*'
        length += 2;

        while (lexer.getPosition() < lexer.getInput().length - 1) {
            if (lexer.peek() == '*' && lexer.peek(1) == '/') {
                lexer.consume(); // '*'
                lexer.consume(); // '/'
                length += 2;
                break;
            }
            lexer.consume();
            length++;
        }

        char[] source = lexer.getInputArray();
        Token token = new Token(TokenType.BLOCK_COMMENT, source, startPos, length,
                                startLine, startCol, null, null, null, null);
        
        String commentText = new String(source, startPos, length);
        extractedComments.add(new Comment(commentText, startLine, startCol, CommentType.BLOCK));
        return token;
    }

    public List<Comment> extractAllComments() {
        extractedComments.clear();
        int savedPos = lexer.getPosition();
        int savedLine = lexer.line;
        int savedCol = lexer.column;

        lexer.setPosition(0);
        lexer.line = 1;
        lexer.column = 1;

        while (lexer.getPosition() < lexer.getInput().length) {
            Token token = scan();
            if (token == null) {
                lexer.consume(); // skip non-comment chars
            }
        }

        lexer.setPosition(savedPos);
        lexer.line = savedLine;
        lexer.column = savedCol;

        return new ArrayList<Comment>(extractedComments);
    }

    public void processWithoutComments(final CommentProcessor processor) {
        int savedPos = lexer.getPosition();
        int savedLine = lexer.line;
        int savedCol = lexer.column;

        lexer.setPosition(0);
        lexer.line = 1;
        lexer.column = 1;

        while (lexer.getPosition() < lexer.getInput().length) {
            Token token = scan();
            if (token == null) {
                processor.process(String.valueOf(lexer.consume()), null);
            }
        }

        lexer.setPosition(savedPos);
        lexer.line = savedLine;
        lexer.column = savedCol;
    }

    public static class Comment {
        public final String text;
        public final int line;
        public final int column;
        public final CommentType type;

        public Comment(String text, int line, int column, CommentType type) {
            this.text = text;
            this.line = line;
            this.column = column;
            this.type = type;
        }
    }

    public enum CommentType {
        LINE, BLOCK
    }

    public interface CommentProcessor {
        void process(String text, CommentType type);
    }
}