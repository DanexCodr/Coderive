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
        int line = lexer.line;
        int col = lexer.column;

        StringBuilder sb = new StringBuilder();
        boolean isFloat = false;

        // Read integer part
        while (lexer.getPosition() < lexer.getInput().length() && 
               Character.isDigit(lexer.peek())) {
            sb.append(lexer.consume());
        }

        // Check for decimal point
        if (lexer.peek() == '.' && lexer.peek(1) != '.') {
            isFloat = true;
            sb.append(lexer.consume()); // consume the '.'

            // Read fractional part
            while (lexer.getPosition() < lexer.getInput().length() && 
                   Character.isDigit(lexer.peek())) {
                sb.append(lexer.consume());
            }
        }

        // Check for scientific notation
        if (lexer.peek() == 'e' || lexer.peek() == 'E') {
            isFloat = true;
            sb.append(lexer.consume()); // consume 'e' or 'E'

            // Optional sign
            if (lexer.peek() == '+' || lexer.peek() == '-') {
                sb.append(lexer.consume());
            }

            // Exponent digits
            if (Character.isDigit(lexer.peek())) {
                while (lexer.getPosition() < lexer.getInput().length() && 
                       Character.isDigit(lexer.peek())) {
                    sb.append(lexer.consume());
                }
            }
        }

        // Check for numeric suffixes
        String suffix = readSuffix();
        if (!suffix.isEmpty()) {
            sb.append(suffix);
            isFloat = true;
        }

        String numberText = sb.toString();
        
        // Store extracted number
        extractedNumbers.add(new NumberValue(numberText, isFloat, line, col));

        return Token.createNumber(numberText, isFloat, line, col);
    }

    private String readSuffix() {
        if (lexer.getPosition() >= lexer.getInput().length()) return "";

        char c1 = lexer.peek();

        if (c1 == 'K' || c1 == 'M' || c1 == 'B' || c1 == 'T') {
            return String.valueOf(lexer.consume());
        }

        if (c1 == 'Q') {
            lexer.consume();
            if (lexer.peek() == 'i') {
                lexer.consume();
                return "Qi";
            }
            return "Q";
        }

        return "";
    }

    public List<NumberValue> extractAllNumbers() {
        extractedNumbers.clear();
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