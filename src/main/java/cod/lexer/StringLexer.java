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

    // Helper method to create text literal token without quotes
    private Token createTextLiteralWithoutQuotes(char[] source, int start, int length, int line, int col) {
        return Token.createTextLiteral(source, start, length, line, col);
    }

    private Token readText() {
        int startLine = lexer.line;
        int startCol = lexer.column;
        int startPos = lexer.getPosition();
        int length = 0;
        
        List<Token> parts = new ArrayList<Token>();
        List<Token> childTokens = new ArrayList<Token>();
        
        boolean isMultiline = false;

        // Check if this is a multiline string (|")
        if (lexer.peek() == '|' && lexer.peek(1) == '"') {
            isMultiline = true;
            lexer.consume(); // consume '|'
            lexer.consume(); // consume '"'
            length += 2;
        } else {
            lexer.consume(); // consume regular opening quote
            length++;
        }
        
        int textStart = lexer.getPosition();
        int textLength = 0;
        
        while (lexer.getPosition() < lexer.getInput().length) {
            char c = lexer.peek();
            
            // Check for newline in regular string
            if (!isMultiline && c == '\n') {
                throw new RuntimeException(
                    "Syntax Error: Unterminated string at line " + startLine + 
                    ", column " + startCol
                );
            }
            
            // Check for closing quote
            if (!isMultiline && c == '"') {
                lexer.consume(); // consume closing quote
                length++;
                break;
            } else if (isMultiline && c == '"' && lexer.peek(1) == '|') {
                lexer.consume(); // consume '"'
                lexer.consume(); // consume '|'
                length += 2;
                break;
            }
            
            // Handle escape sequences
            if (c == '\\') {
                // If we have accumulated text, create a token for it
                if (textLength > 0) {
                    char[] source = lexer.getInputArray();
                    Token textToken = createTextLiteralWithoutQuotes(source, textStart, textLength, startLine, startCol);
                    parts.add(textToken);
                    childTokens.add(textToken);
                    textStart = lexer.getPosition();
                    textLength = 0;
                }
                
                lexer.consume(); // consume backslash
                length++;
                
                if (lexer.getPosition() >= lexer.getInput().length) {
                    throw new RuntimeException(
                        "Syntax Error: Unterminated escape sequence at line " + lexer.line
                    );
                }
                char escaped = lexer.consume();
                length++;
                
                // Convert escape sequence to actual character
                char actualChar;
                switch (escaped) {
                    case 'n': actualChar = '\n'; break;
                    case 't': actualChar = '\t'; break;
                    case 'r': actualChar = '\r'; break;
                    case '\\': actualChar = '\\'; break;
                    case '"': actualChar = '"'; break;
                    case '{': actualChar = '{'; break;
                    default: actualChar = escaped; break;
                }
                
                // Add escaped character as a text literal (already without quotes)
                String escapedStr = String.valueOf(actualChar);
                Token escapedToken = Token.createTextLiteral(escapedStr, lexer.line, lexer.column - 1);
                parts.add(escapedToken);
                childTokens.add(escapedToken);
                
                textStart = lexer.getPosition();
                textLength = 0;
                continue;
            }
            
            // Handle interpolation
            if (c == '{' && !isMultiline) {
                // If we have accumulated text, create a token for it
                if (textLength > 0) {
                    char[] source = lexer.getInputArray();
                    Token textToken = createTextLiteralWithoutQuotes(source, textStart, textLength, startLine, startCol);
                    parts.add(textToken);
                    childTokens.add(textToken);
                }
                
                int braceLine = lexer.line;
                int braceColumn = lexer.column;
                lexer.consume(); // consume '{'
                length++;
                
                // Parse interpolation expression
                int exprStart = lexer.getPosition();
                int exprLength = 0;
                int braceDepth = 1;
                
                while (lexer.getPosition() < lexer.getInput().length && braceDepth > 0) {
                    char ch = lexer.peek();
                    
                    if (ch == '{') {
                        braceDepth++;
                        lexer.consume();
                        exprLength++;
                    } else if (ch == '}') {
                        braceDepth--;
                        if (braceDepth > 0) {
                            lexer.consume();
                            exprLength++;
                        } else {
                            lexer.consume(); // consume closing '}'
                            exprLength++;
                        }
                    } else {
                        lexer.consume();
                        exprLength++;
                    }
                }
                
                // Create INTERPOL token with child tokens
                char[] source = lexer.getInputArray();
                char[] exprSlice = Arrays.copyOfRange(source, exprStart, exprStart + exprLength - 1);
                MainLexer exprLexer = new MainLexer(new String(exprSlice), true);
                List<Token> exprTokens = exprLexer.tokenize();
                
                Token interpToken = Token.createInterpolation(braceLine, braceColumn, exprTokens);
                parts.add(interpToken);
                childTokens.addAll(exprTokens);
                
                textStart = lexer.getPosition();
                textLength = 0;
                continue;
            }
            
            // Regular character
            lexer.consume();
            textLength++;
            length++;
        }
        
        // Add any remaining text part
        if (textLength > 0) {
            char[] source = lexer.getInputArray();
            Token textToken = createTextLiteralWithoutQuotes(source, textStart, textLength, startLine, startCol);
            parts.add(textToken);
            childTokens.add(textToken);
        }
        
        char[] fullSource = lexer.getInputArray();
        
        // Build full text representation (for backward compatibility)
        StringBuilder fullText = new StringBuilder(isMultiline ? "|\"" : "\"");
        for (Token part : parts) {
            if (part.type == TokenType.TEXT_LIT) {
                fullText.append(part.getText());
            } else if (part.type == TokenType.INTERPOL) {
                if (part.hasChildTokens()) {
                    StringBuilder exprBuilder = new StringBuilder();
                    for (Token exprToken : part.childTokens) {
                        exprBuilder.append(exprToken.getText());
                    }
                    fullText.append("{").append(exprBuilder.toString()).append("}");
                } else {
                    fullText.append("{").append(part.getText()).append("}");
                }
            }
        }
        fullText.append(isMultiline ? "\"|" : "\"");
        
        extractedStrings.add(fullText.toString());
        
        // If no interpolations, return a simple text literal
        if (parts.size() == 1 && parts.get(0).type == TokenType.TEXT_LIT) {
            return Token.createTextLiteral(fullSource, startPos, length, startLine, startCol);
        }
        
        return new Token(
            TokenType.INTERPOL,
            fullSource, startPos, length,
            startLine, startCol,
            null, null, parts, null
        );
    }

    private Token readMultilineText() {
        int startLine = lexer.line;
        int startCol = lexer.column;
        int startPos = lexer.getPosition();
        
        if (!(lexer.peek() == '|' && lexer.peek(1) == '"')) {
            throw new RuntimeException("Invalid multiline text opening at line " + startLine + ", column " + startCol);
        }
        
        int baselineColumn = startCol;
        int length = 0;
        
        lexer.consume(); // consume '|'
        lexer.consume(); // consume '"'
        length += 2;
        
        // Skip whitespace after opening delimiter
        while (lexer.getPosition() < lexer.getInput().length) {
            char after = lexer.peek();
            if (after == '\n') {
                break;
            } else if (!Character.isWhitespace(after)) {
                throw new RuntimeException(
                    "After multiline text opening delimiter '|\"', only whitespace allowed on same line. " +
                    "Text content must start on next line. Found: '" + after + "' at line " + startLine + ", column " + startCol
                );
            }
            lexer.consume();
            length++;
        }
        
        // Skip to next line
        if (lexer.getPosition() < lexer.getInput().length && lexer.peek() == '\n') {
            lexer.consume();
            length++;
            lexer.line++;
            lexer.column = 1;
        }
        
        List<Token> interpolations = new ArrayList<Token>();
        List<Token> childTokens = new ArrayList<Token>();
        
        int currentColumnInLine = 1;
        StringBuilder currentLine = new StringBuilder();
        
        while (lexer.getPosition() < lexer.getInput().length) {
            char c = lexer.peek();
            
            // Check for closing delimiter
            if (c == '"' && lexer.peek(1) == '|') {
                // Add any remaining text
                if (currentLine.length() > 0) {
                    // Strip baseline indentation
                    int skip = 0;
                    for (int i = 0; i < currentLine.length() && skip < baselineColumn - 1; i++) {
                        char ch = currentLine.charAt(i);
                        if (ch == ' ' || ch == '\t') {
                            skip++;
                        } else {
                            break;
                        }
                    }
                    if (skip < currentLine.length()) {
                        String lineText = currentLine.substring(skip);
                        // Create text token without quotes (multiline content doesn't have quotes)
                        Token textToken = Token.createTextLiteral(lineText, startLine, startCol);
                        interpolations.add(textToken);
                        childTokens.add(textToken);
                    }
                }
                
                lexer.consume(); // consume '"'
                lexer.consume(); // consume '|'
                length += 2;
                
                char[] fullSource = lexer.getInputArray();
                StringBuilder finalText = new StringBuilder("|\"");
                for (Token part : interpolations) {
                    if (part.type == TokenType.TEXT_LIT) {
                        finalText.append(part.getText());
                    } else if (part.type == TokenType.INTERPOL) {
                        if (part.hasChildTokens()) {
                            StringBuilder exprBuilder = new StringBuilder();
                            for (Token exprToken : part.childTokens) {
                                exprBuilder.append(exprToken.getText());
                            }
                            finalText.append("{").append(exprBuilder.toString()).append("}");
                        } else {
                            finalText.append("{").append(part.getText()).append("}");
                        }
                    }
                }
                finalText.append("\"|");
                
                extractedStrings.add(finalText.toString());
                
                if (interpolations.size() == 1 && interpolations.get(0).type == TokenType.TEXT_LIT) {
                    return Token.createTextLiteral(fullSource, startPos, length, startLine, startCol);
                }
                
                return new Token(
                    TokenType.INTERPOL,
                    fullSource, startPos, length,
                    startLine, startCol,
                    null, null, interpolations, null
                );
            }
            
            // Handle newlines
            if (c == '\n') {
                // Process the current line
                if (currentLine.length() > 0) {
                    // Strip baseline indentation
                    int skip = 0;
                    for (int i = 0; i < currentLine.length() && skip < baselineColumn - 1; i++) {
                        char ch = currentLine.charAt(i);
                        if (ch == ' ' || ch == '\t') {
                            skip++;
                        } else {
                            break;
                        }
                    }
                    if (skip < currentLine.length()) {
                        String lineText = currentLine.substring(skip);
                        Token textToken = Token.createTextLiteral(lineText, startLine, startCol);
                        interpolations.add(textToken);
                        childTokens.add(textToken);
                    }
                }
                
                // Add newline as actual newline character
                Token newlineToken = Token.createTextLiteral("\n", lexer.line, lexer.column);
                interpolations.add(newlineToken);
                childTokens.add(newlineToken);
                
                // Consume newline
                lexer.consume();
                length++;
                lexer.line++;
                lexer.column = 1;
                currentColumnInLine = 1;
                
                // Reset for next line
                currentLine.setLength(0);
                continue;
            }
            
            // Handle content to the left of baseline (error)
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
                length++;
                currentColumnInLine++;
                
                if (lexer.getPosition() >= lexer.getInput().length) {
                    throw new RuntimeException("Unterminated escape sequence at line " + lexer.line);
                }
                char escaped = lexer.consume();
                length++;
                currentColumnInLine++;
                
                // Convert escape sequences to actual characters
                char actualChar;
                switch (escaped) {
                    case 'n': actualChar = '\n'; break;
                    case 't': actualChar = '\t'; break;
                    case 'r': actualChar = '\r'; break;
                    case '\\': actualChar = '\\'; break;
                    case '"': actualChar = '"'; break;
                    case '{': actualChar = '{'; break;
                    default: actualChar = escaped; break;
                }
                currentLine.append(actualChar);
                continue;
            }
            
            // Handle interpolation
            if (c == '{') {
                // Add current text first
                if (currentLine.length() > 0) {
                    // Strip baseline from current line segment
                    int skip = 0;
                    for (int i = 0; i < currentLine.length() && skip < baselineColumn - 1; i++) {
                        char ch = currentLine.charAt(i);
                        if (ch == ' ' || ch == '\t') {
                            skip++;
                        } else {
                            break;
                        }
                    }
                    if (skip < currentLine.length()) {
                        String lineText = currentLine.substring(skip);
                        Token textToken = Token.createTextLiteral(lineText, startLine, startCol);
                        interpolations.add(textToken);
                        childTokens.add(textToken);
                    }
                    currentLine.setLength(0);
                }
                
                int braceLine = lexer.line;
                int braceColumn = lexer.column;
                lexer.consume();
                length++;
                currentColumnInLine++;
                
                // Parse interpolation expression
                StringBuilder exprBuilder = new StringBuilder();
                int braceDepth = 1;
                
                while (lexer.getPosition() < lexer.getInput().length && braceDepth > 0) {
                    char ch = lexer.peek();
                    
                    if (ch == '\\') {
                        exprBuilder.append(lexer.consume());
                        if (lexer.getPosition() < lexer.getInput().length) {
                            exprBuilder.append(lexer.consume());
                        }
                    } else if (ch == '{') {
                        braceDepth++;
                        exprBuilder.append(lexer.consume());
                    } else if (ch == '}') {
                        braceDepth--;
                        if (braceDepth > 0) {
                            exprBuilder.append(lexer.consume());
                        } else {
                            lexer.consume(); // consume closing '}'
                        }
                    } else if (ch == '\n') {
                        exprBuilder.append('\n');
                        lexer.consume();
                        lexer.line++;
                        lexer.column = 1;
                        currentColumnInLine = 1;
                    } else {
                        exprBuilder.append(lexer.consume());
                        currentColumnInLine++;
                    }
                }
                
                // Tokenize the expression
                String exprText = exprBuilder.toString();
                MainLexer exprLexer = new MainLexer(exprText, true);
                List<Token> exprTokens = exprLexer.tokenize();
                
                Token interpToken = Token.createInterpolation(braceLine, braceColumn, exprTokens);
                childTokens.addAll(exprTokens);
                interpolations.add(interpToken);
                
            } else {
                // Regular character
                lexer.consume();
                currentLine.append(c);
                length++;
                currentColumnInLine++;
            }
        }
        
        throw new RuntimeException(
            "Unterminated multiline text starting at line " + startLine + 
            ", column " + startCol
        );
    }
    
    public List<String> extractAllStrings() {
        extractedStrings.clear();
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
        
        return new ArrayList<String>(extractedStrings);
    }
}