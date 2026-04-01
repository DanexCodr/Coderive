package cod.lexer;

import java.util.*;

public class NumberLexer {

    private final MainLexer lexer;
    private final List<NumberValue> extractedNumbers;

    public NumberLexer(MainLexer lexer) {
        this.lexer = lexer;
        this.extractedNumbers = new ArrayList<NumberValue>();
    }

    public Token scan() {
        if (Character.isDigit(lexer.peek())) {
            return readNumber();
        }
        return null;
    }

    private Token readNumber() {
        int startLine = lexer.line;
        int startCol = lexer.column;
        int startPos = lexer.getPosition();
        int length = 0;
        boolean isFloat = false;

        // Read integer part
        while (lexer.getPosition() < lexer.getInput().length && 
               Character.isDigit(lexer.peek())) {
            lexer.consume();
            length++;
        }

        // Check for decimal point
        if (lexer.peek() == '.' && lexer.peek(1) != '.') {
            isFloat = true;
            lexer.consume();
            length++;

            // Read fractional part
            while (lexer.getPosition() < lexer.getInput().length && 
                   Character.isDigit(lexer.peek())) {
                lexer.consume();
                length++;
            }
        }

        // Check for scientific notation
        if (lexer.peek() == 'e' || lexer.peek() == 'E') {
            isFloat = true;
            lexer.consume();
            length++;

            // Optional sign
            if (lexer.peek() == '+' || lexer.peek() == '-') {
                lexer.consume();
                length++;
            }

            // Exponent digits
            if (Character.isDigit(lexer.peek())) {
                while (lexer.getPosition() < lexer.getInput().length && 
                       Character.isDigit(lexer.peek())) {
                    lexer.consume();
                    length++;
                }
            }
        }

        // Check for numeric suffixes
        if (lexer.getPosition() < lexer.getInput().length) {
            char c = lexer.peek();
            if (c == 'K' || c == 'M' || c == 'B' || c == 'T') {
                isFloat = true;
                lexer.consume();
                length++;
            } else if (c == 'Q') {
                isFloat = true;
                lexer.consume();
                length++;
                if (lexer.peek() == 'i') {
                    lexer.consume();
                    length++;
                }
            }
        }

        char[] source = lexer.getInputArray();
        
        // Store extracted number
        String numberText = new String(source, startPos, length);
        extractedNumbers.add(new NumberValue(numberText, isFloat, startLine, startCol));

        return Token.createNumber(source, startPos, length, isFloat, startLine, startCol);
    }

    public List<NumberValue> extractAllNumbers() {
        extractedNumbers.clear();
        int savedPos = lexer.getPosition();
        int savedLine = lexer.line;
        int savedCol = lexer.column;

        lexer.setPosition(0);
        lexer.line = 1;
        lexer.column = 1;

        while (lexer.getPosition() < lexer.getInput().length) {
            Token token = scan();
            if (token == null) {
                lexer.consume();
            }
        }

        lexer.setPosition(savedPos);
        lexer.line = savedLine;
        lexer.column = savedCol;

        return new ArrayList<NumberValue>(extractedNumbers);
    }

    public static class NumberValue {
        public final String text;
        public final boolean isFloat;
        public final int line;
        public final int column;

        public NumberValue(String text, boolean isFloat, int line, int column) {
            this.text = text;
            this.isFloat = isFloat;
            this.line = line;
            this.column = column;
        }
    }
}