package cod.lexer;

import java.util.*;

public class StringLexer {

    private final MainLexer lexer;
    private final List<String> extractedStrings;

    public StringLexer(MainLexer lexer) {
        this.lexer = lexer;
        this.extractedStrings = new ArrayList<String>();
    }

    public Token scan() {
        if (lexer.peek() == '|' && lexer.peek(1) == '"') {
            return readMultilineText();
        }
        if (lexer.peek() == '"') {
            return readText();
        }
        return null;
    }

    private Token readText() {
        int startLine = lexer.line;
        int startCol = lexer.column;

        StringBuilder sb = new StringBuilder();
        List<Token> parts = new ArrayList<Token>();
        List<Token> childTokens = new ArrayList<Token>();

        boolean isMultiline = false;

        // Check if this is a multiline string (|")
        if (lexer.peek() == '|' && lexer.peek(1) == '"') {
            isMultiline = true;
            lexer.consume(); // consume '|'
            lexer.consume(); // consume '"'
        } else {
            lexer.consume(); // consume regular opening quote
        }

        while (lexer.getPosition() < lexer.getInput().length()) {
            char c = lexer.peek();

            // Check for newline in regular string
            if (!isMultiline && (c == '\n' || c == '\r')) {
                throw new RuntimeException(
                    "Syntax Error: Unterminated string at line " + startLine + 
                    ", column " + startCol
                );
            }

            // Check for closing quote
            if (!isMultiline && c == '"') {
                lexer.consume(); // consume closing quote
                break;
            } else if (isMultiline && c == '"' && lexer.peek(1) == '|') {
                lexer.consume(); // consume '"'
                lexer.consume(); // consume '|'
                break;
            }

            // Handle escape sequences
            if (c == '\\') {
                lexer.consume(); // consume backslash
                if (lexer.getPosition() >= lexer.getInput().length()) {
                    throw new RuntimeException(
                        "Syntax Error: Unterminated escape sequence at line " + lexer.line
                    );
                }
                char escaped = lexer.consume();
                switch (escaped) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case '\\': sb.append('\\'); break;
                    case '"': sb.append('"'); break;
                    case '{': sb.append('{'); break;
                    default: sb.append('\\').append(escaped); break;
                }
                continue;
            }

            // Handle interpolation
            if (c == '{' && !isMultiline) {
                int braceLine = lexer.line;
                int braceColumn = lexer.column;
                lexer.consume(); // consume '{'

                // Save current text part
                if (sb.length() > 0) {
                    Token textToken = Token.createTextLiteral(sb.toString(), startLine, startCol);
                    parts.add(textToken);
                    childTokens.add(textToken);
                    sb = new StringBuilder();
                }

                // Parse interpolation expression
                StringBuilder exprBuilder = new StringBuilder();
                int braceDepth = 1;
                
                while (lexer.getPosition() < lexer.getInput().length() && braceDepth > 0) {
                    char ch = lexer.peek();
                    
                    if (ch == '{') {
                        braceDepth++;
                        exprBuilder.append(lexer.consume());
                    } else if (ch == '}') {
                        braceDepth--;
                        if (braceDepth > 0) {
                            exprBuilder.append(lexer.consume());
                        } else {
                            lexer.consume(); // consume closing '}'
                        }
                    } else {
                        exprBuilder.append(lexer.consume());
                    }
                }
                
                // Tokenize the expression
                String exprText = exprBuilder.toString().trim();
                MainLexer exprLexer = new MainLexer(exprText, true);
                List<Token> exprTokens = exprLexer.tokenize();
                
                // Create INTERPOL token with child tokens
                Token interpToken = Token.createInterpolation(braceLine, braceColumn, exprTokens);
                parts.add(interpToken);
                childTokens.addAll(exprTokens);
                
                continue;
            }

            // Regular character
            sb.append(lexer.consume());
        }

        // Add any remaining text part
        if (sb.length() > 0) {
            Token textToken = Token.createTextLiteral(sb.toString(), startLine, startCol);
            parts.add(textToken);
            childTokens.add(textToken);
        }

        // Build full text representation
        StringBuilder fullText = new StringBuilder(isMultiline ? "|\"" : "\"");
        for (Token part : parts) {
            if (part.type == TokenType.TEXT_LIT) {
                fullText.append(part.text);
            } else if (part.type == TokenType.INTERPOL) {
                if (part.hasChildTokens()) {
                    StringBuilder exprBuilder = new StringBuilder();
                    for (Token exprToken : part.childTokens) {
                        exprBuilder.append(exprToken.text);
                    }
                    fullText.append("{").append(exprBuilder.toString()).append("}");
                } else {
                    fullText.append("{").append(part.text).append("}");
                }
            }
        }
        fullText.append(isMultiline ? "\"|" : "\"");
        
        extractedStrings.add(fullText.toString());

        // If no interpolations, return a simple text literal
        if (parts.size() == 1 && parts.get(0).type == TokenType.TEXT_LIT) {
            return parts.get(0);
        }

        return new Token(
            TokenType.INTERPOL,
            fullText.toString(),
            startLine,
            startCol,
            null, null, parts, null
        );
    }

    private Token readMultilineText() {
        int startLine = lexer.line;
        int startCol = lexer.column;
        
        if (!(lexer.peek() == '|' && lexer.peek(1) == '"')) {
            throw new RuntimeException("Invalid multiline text opening at line " + startLine + ", column " + startCol);
        }
        
        int baselineColumn = startCol;
        
        lexer.consume(); // consume '|'
        lexer.consume(); // consume '"'
        
        // Skip whitespace after opening delimiter
        while (lexer.getPosition() < lexer.getInput().length()) {
            char after = lexer.peek();
            if (after == '\n' || after == '\r') {
                break;
            } else if (!Character.isWhitespace(after)) {
                // Reset position and throw error
                throw new RuntimeException(
                    "After multiline text opening delimiter '|\"', only whitespace allowed on same line. " +
                    "Text content must start on next line. Found: '" + after + "' at line " + startLine + ", column " + startCol
                );
            }
            lexer.consume();
        }
        
        // Skip to next line
        if (lexer.getPosition() < lexer.getInput().length() && lexer.peek() == '\n') {
            lexer.consume();
        } else if (lexer.getPosition() < lexer.getInput().length() && lexer.peek() == '\r') {
            lexer.consume();
            if (lexer.getPosition() < lexer.getInput().length() && lexer.peek() == '\n') {
                lexer.consume();
            }
        }
        
        List<Token> interpolations = new ArrayList<Token>();
        List<Token> childTokens = new ArrayList<Token>();
        StringBuilder currentTextPart = new StringBuilder();
        StringBuilder currentLine = new StringBuilder();
        
        int currentColumnInLine = 1;
        
        while (lexer.getPosition() < lexer.getInput().length()) {
            char c = lexer.peek();
            
            // Check for closing delimiter
            if (c == '"' && lexer.peek(1) == '|') {
                validateLineForBaseline(currentLine.toString(), baselineColumn, lexer.line);
                
                if (lexer.column != baselineColumn) {
                    throw new RuntimeException(
                        "Multiline text closing delimiter '\"|' must align with opening delimiter baseline. " +
                        "Opening '|' was at column " + baselineColumn + ", closing '\"' at column " + lexer.column + ". " +
                        "All content indentation is measured from column " + baselineColumn + ".");
                }
                
                if (currentLine.length() > 0) {
                    String strippedLine = stripLeftOfBaseline(currentLine.toString(), baselineColumn, lexer.line);
                    currentTextPart.append(strippedLine);
                    currentLine.setLength(0);
                }
                
                // Add any remaining text part
                if (currentTextPart.length() > 0) {
                    Token textToken = Token.createTextLiteral(currentTextPart.toString(), startLine, startCol);
                    childTokens.add(textToken);
                    interpolations.add(textToken);
                }
                
                lexer.consume(); // consume '"'
                lexer.consume(); // consume '|'
                
                // Build final text
                StringBuilder finalText = new StringBuilder("|\"");
                for (Token part : interpolations) {
                    if (part.type == TokenType.TEXT_LIT) {
                        finalText.append(part.text);
                    } else if (part.type == TokenType.INTERPOL) {
                        if (part.hasChildTokens()) {
                            StringBuilder exprBuilder = new StringBuilder();
                            for (Token exprToken : part.childTokens) {
                                exprBuilder.append(exprToken.text);
                            }
                            finalText.append("{").append(exprBuilder.toString()).append("}");
                        } else {
                            finalText.append("{").append(part.text).append("}");
                        }
                    }
                }
                finalText.append("\"|");
                
                extractedStrings.add(finalText.toString());
                
                // If no interpolations, return text literal
                if (interpolations.size() == 1 && interpolations.get(0).type == TokenType.TEXT_LIT) {
                    return Token.createTextLiteral(finalText.toString(), startLine, startCol);
                }
                
                return new Token(
                    TokenType.INTERPOL,
                    finalText.toString(),
                    startLine,
                    startCol,
                    null, null, interpolations, null
                );
            }
            
            // Check for content to left of baseline
            if (currentColumnInLine < baselineColumn && !Character.isWhitespace(c) && c != '\\' && c != '{') {
                throw new RuntimeException(
                    "Multiline text violation at line " + lexer.line + ", column " + currentColumnInLine + "\n" +
                    "Character '" + c + "' appears to the left of baseline column " + baselineColumn + "\n" +
                    "All content must start at or right of the opening '|' column (column " + baselineColumn + ")"
                );
            }
            
            // Handle escapes
            if (c == '\\') {
                lexer.consume();
                currentColumnInLine++;
                
                if (lexer.getPosition() >= lexer.getInput().length()) {
                    throw new RuntimeException("Unterminated escape sequence at line " + lexer.line);
                }
                char escaped = lexer.consume();
                currentColumnInLine++;
                
                currentLine.append('\\').append(escaped);
                
            } else if (c == '{') {
                // Handle interpolation in multiline string
                int braceLine = lexer.line;
                int braceColumn = lexer.column;
                lexer.consume();
                currentColumnInLine++;
                
                // Save current text part
                if (currentLine.length() > 0) {
                    String strippedLine = stripLeftOfBaseline(currentLine.toString(), baselineColumn, braceLine);
                    currentTextPart.append(strippedLine);
                    currentLine.setLength(0);
                }
                if (currentTextPart.length() > 0) {
                    Token textToken = Token.createTextLiteral(currentTextPart.toString(), startLine, startCol);
                    childTokens.add(textToken);
                    interpolations.add(textToken);
                    currentTextPart.setLength(0);
                }
                
                // Parse interpolation expression
                StringBuilder exprBuilder = new StringBuilder();
                int braceDepth = 1;
                
                while (lexer.getPosition() < lexer.getInput().length() && braceDepth > 0) {
                    char ch = lexer.peek();
                    
                    if (ch == '\\') {
                        exprBuilder.append(lexer.consume());
                        currentColumnInLine++;
                        if (lexer.getPosition() < lexer.getInput().length()) {
                            exprBuilder.append(lexer.consume());
                            currentColumnInLine++;
                        }
                    } else if (ch == '{') {
                        braceDepth++;
                        exprBuilder.append(lexer.consume());
                        currentColumnInLine++;
                    } else if (ch == '}') {
                        braceDepth--;
                        if (braceDepth > 0) {
                            exprBuilder.append(lexer.consume());
                            currentColumnInLine++;
                        } else {
                            lexer.consume(); // consume closing '}'
                            currentColumnInLine++;
                        }
                    } else if (ch == '\n' || ch == '\r') {
                        if (ch == '\n') {
                            exprBuilder.append('\n');
                            lexer.consume();
                            lexer.line++;
                            lexer.column = 1;
                            currentColumnInLine = 1;
                        } else if (ch == '\r') {
                            lexer.consume();
                            currentColumnInLine++;
                            if (lexer.getPosition() < lexer.getInput().length() && lexer.peek() == '\n') {
                                exprBuilder.append('\n');
                                lexer.consume();
                                lexer.line++;
                                lexer.column = 1;
                                currentColumnInLine = 1;
                            } else {
                                exprBuilder.append('\r');
                            }
                        }
                    } else {
                        exprBuilder.append(lexer.consume());
                        currentColumnInLine++;
                    }
                }
                
                // Tokenize expression
                String exprText = exprBuilder.toString().trim();
                MainLexer exprLexer = new MainLexer(exprText, true);
                List<Token> exprTokens = exprLexer.tokenize();
                
                Token interpToken = Token.createInterpolation(braceLine, braceColumn, exprTokens);
                childTokens.addAll(exprTokens);
                interpolations.add(interpToken);
                
            } else if (c == '\n' || c == '\r') {
                validateLineForBaseline(currentLine.toString(), baselineColumn, lexer.line);
                
                String strippedLine = stripLeftOfBaseline(currentLine.toString(), baselineColumn, lexer.line);
                currentTextPart.append(strippedLine);
                currentTextPart.append('\n');
                currentLine.setLength(0);
                
                if (c == '\n') {
                    lexer.consume();
                    lexer.line++;
                    lexer.column = 1;
                    currentColumnInLine = 1;
                } else if (c == '\r') {
                    lexer.consume();
                    if (lexer.getPosition() < lexer.getInput().length() && lexer.peek() == '\n') {
                        lexer.consume();
                        lexer.line++;
                        lexer.column = 1;
                    }
                    currentColumnInLine = 1;
                }
                
            } else {
                currentLine.append(lexer.consume());
                currentColumnInLine++;
            }
        }
        
        throw new RuntimeException(
            "Unterminated multiline text starting at line " + startLine + 
            ", column " + startCol
        );
    }

    // Helper methods for multiline text
    private void validateLineForBaseline(String line, int baselineColumn, int lineNumber) {
        for (int i = 0; i < baselineColumn - 1 && i < line.length(); i++) {
            if (!Character.isWhitespace(line.charAt(i))) {
                throw new RuntimeException(
                    "Multiline text violation at line " + lineNumber + ", column " + (i + 1) + "\n" +
                    "Character '" + line.charAt(i) + "' appears to the left of baseline column " + baselineColumn + "\n" +
                    "All content must start at or right of the opening '|' column"
                );
            }
        }
    }

    private String stripLeftOfBaseline(String line, int baselineColumn, int lineNumber) {
        validateLineForBaseline(line, baselineColumn, lineNumber);
        
        if (line.length() >= baselineColumn) {
            return line.substring(baselineColumn - 1);
        } else {
            return "";
        }
    }

    public List<String> extractAllStrings() {
        extractedStrings.clear();
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

        return new ArrayList<String>(extractedStrings);
    }
}