package cod.lexer;

import cod.syntax.Symbol;
import static cod.syntax.Symbol.*;

import java.util.*;

public class MainLexer {

  private static final Set<String> KEYWORDS = new HashSet<String>();

  static {
      for (cod.syntax.Keyword keyword : cod.syntax.Keyword.values()) {
          KEYWORDS.add(keyword.toString());
      }
  }

  private final String input;
  private int position = 0;
  private int line = 1;
  private int column = 1;

  public MainLexer(String input) {
    this.input = input;
  }

  public List<Token> tokenize() {
    List<Token> tokens = new ArrayList<Token>();
    while (position < input.length()) {
      skipWhitespaceAndComments();
      if (position < input.length()) {
        tokens.add(scanNextToken());
      }
    }
    tokens.add(create(TokenType.EOF));
    return tokens;
  }

  private void skipWhitespaceAndComments() {
    while (position < input.length()) {
      char c = peek();
      if (Character.isWhitespace(c)) {
        consume();
      } else if (c == '/' && peek(1) == '/') {
        scanLineComment();
      } else if (c == '/' && peek(1) == '*') {
        scanBlockComment();
      } else {
        break;
      }
    }
  }

private Token scanNextToken() {
    char c = peek();

    if (Character.isLetter(c) || c == '_') return readIdentifierOrKeyword();
    if (Character.isDigit(c)) return readNumber();
    
    if (c == '|' && peek(1) == '"') {
        return readMultilineString();
    }
    if (c == '"') {
        return readString();
    }
    return readSymbol();
}

  private Token readIdentifierOrKeyword() {
    int startLine = line;
    int startColumn = column;
    
    StringBuilder sb = new StringBuilder();
    while (position < input.length() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
        sb.append(consume());
    }
    String text = sb.toString();
    
    if (KEYWORDS.contains(text)) {
        return new Token(TokenType.KEYWORD, text, startLine, startColumn);
    }
    
    return new Token(TokenType.ID, text, startLine, startColumn);
  }

  private Token readNumber() {
    int startLine = line;
    int startColumn = column;
    
    StringBuilder sb = new StringBuilder();
    boolean isFloat = false;
    
    while (position < input.length() && Character.isDigit(peek())) {
        sb.append(consume());
    }
    
    if (peek() == '.') {
        isFloat = true;
        sb.append(consume());
        while (position < input.length() && Character.isDigit(peek())) {
            sb.append(consume());
        }
    }
    
    String suffix = readSuffix();
    if (!suffix.isEmpty()) {
        sb.append(suffix);
        isFloat = true; 
    } else {
        String exponent = readExponent();
        if (!exponent.isEmpty()) {
            sb.append(exponent);
            isFloat = true;
        }
    }
    
    String numberText = sb.toString();
    return new Token(
        isFloat ? TokenType.FLOAT_LIT : TokenType.INT_LIT, 
        numberText, 
        startLine, 
        startColumn);
  }

  private String readSuffix() {
    if (position >= input.length()) return "";
    
    char c1 = peek();
    
    if (c1 == 'K' || c1 == 'M' || c1 == 'B' || c1 == 'T') {
        return String.valueOf(consume());
    }
    
    if (c1 == 'Q') {
        consume();
        if (peek() == 'i') {
            consume();
            return "Qi";
        }
        return "Q";
    }
    
    return "";
  }

  private String readExponent() {
    if (position >= input.length()) return "";
    
    char c1 = peek();
    if (c1 != 'e' && c1 != 'E') {
        return "";
    }
    
    StringBuilder sb = new StringBuilder();
    sb.append(consume());
    
    char c2 = peek();
    if (c2 == '+' || c2 == '-') {
        sb.append(consume());
    }
    
    if (position < input.length() && Character.isDigit(peek())) {
        while (position < input.length() && Character.isDigit(peek())) {
            sb.append(consume());
        }
    } else {
        if (sb.length() == 1 || (sb.length() == 2 && (sb.charAt(1) == '+' || sb.charAt(1) == '-'))) {
             return "";
        }
    }
    
    return sb.toString();
  }

  private Token readString() {
    int startLine = line;
    int startColumn = column;
    
    StringBuilder sb = new StringBuilder();
    List<Token> interpolations = new ArrayList<>();
    
    consume();
    
    while (position < input.length() && peek() != '"') {
        if (peek() == '\\') {
            consume();
            if (position >= input.length()) {
                throw new RuntimeException("Unterminated escape sequence at line " + line);
            }
            char escaped = consume();
            switch (escaped) {
                case 'n': sb.append('\n'); break;
                case 't': sb.append('\t'); break;
                case 'r': sb.append('\r'); break;
                case '\\': sb.append('\\'); break;
                case '"': sb.append('"'); break;
                case '{': sb.append('{'); break;
                default: sb.append('\\').append(escaped); break;
            }
        } else if (peek() == '{') {
            int braceLine = line;
            int braceColumn = column;
            consume();
            
            if (peek() == '}') {
                throw new RuntimeException(
                    "Syntax Error: Empty string interpolation at line " + braceLine + 
                    ", column " + braceColumn + "\n" +
                    "Interpolation must contain an expression: {expression}\n" +
                    "If you want a literal '{' character, escape it: \\{"
                );
            }
            
            if (sb.length() > 0) {
                interpolations.add(new Token(TokenType.STRING_LIT, sb.toString(), startLine, startColumn));
                sb = new StringBuilder();
            }
            
            StringBuilder interpolationContent = new StringBuilder();
            int braceDepth = 1;
            
            while (position < input.length() && braceDepth > 0) {
                char ch = peek();
                
                if (ch == '\\') {
                    interpolationContent.append(consume());
                    if (position < input.length()) {
                        interpolationContent.append(consume());
                    }
                } else if (ch == '{') {
                    braceDepth++;
                    interpolationContent.append(consume());
                } else if (ch == '}') {
                    braceDepth--;
                    if (braceDepth > 0) {
                        interpolationContent.append(consume());
                    } else {
                        consume();
                    }
                } else {
                    interpolationContent.append(consume());
                }
            }
            
            if (braceDepth > 0) {
                throw new RuntimeException("Unclosed interpolation at line " + line + ", column " + column);
            }
            
            interpolations.add(new Token(
                TokenType.INTERPOL,
                interpolationContent.toString().trim(),
                line,
                column - interpolationContent.length() - 1
            ));
            
        } else {
            sb.append(consume());
        }
    }
    
    if (position < input.length()) {
        consume();
    } else {
        throw new RuntimeException("Unterminated string at line " + startLine + ", column " + startColumn);
    }
    
    if (sb.length() > 0) {
        interpolations.add(new Token(TokenType.STRING_LIT, sb.toString(), startLine, startColumn));
    }
    
    if (!interpolations.isEmpty()) {
        StringBuilder compositeText = new StringBuilder("\"");
        for (Token part : interpolations) {
            if (part.type == TokenType.STRING_LIT) {
                compositeText.append(part.text);
            } else {
                compositeText.append("{").append(part.text).append("}");
            }
        }
        compositeText.append("\"");
        
        return new Token(
            TokenType.STRING_LIT,
            compositeText.toString(),
            startLine,
            startColumn,
            interpolations
        );
    }
    
    return new Token(TokenType.STRING_LIT, sb.toString(), startLine, startColumn);
}

private Token readMultilineString() {
    int startLine = line;
    int startColumn = column;
    
    if (!(peek() == '|' && peek(1) == '"')) {
        throw new RuntimeException("Invalid multiline string opening at line " + line + ", column " + column);
    }
    
    int baselineColumn = startColumn;
    
    consume();
    consume();
    
    while (position < input.length()) {
        char after = peek();
        if (after == '\n' || after == '\r') {
            break;
        } else if (!Character.isWhitespace(after)) {
            position -= 2;
            line = startLine;
            column = startColumn;
            throw new RuntimeException(
                "After multiline string opening delimiter '|\"', only whitespace allowed on same line. " +
                "String content must start on next line. Found: '" + after + "' at line " + line + ", column " + column
            );
        }
        consume();
    }
    
    if (position < input.length() && peek() == '\n') {
        consume();
        line++;
        column = 1;
    } else if (position < input.length() && peek() == '\r') {
        consume();
        if (position < input.length() && peek() == '\n') {
            consume();
        }
        line++;
        column = 1;
    }
    
    List<Token> interpolations = new ArrayList<Token>();
    StringBuilder currentStringPart = new StringBuilder();
    StringBuilder currentLine = new StringBuilder();
    
    int currentColumnInLine = 1;
    
    while (position < input.length()) {
        char c = peek();
        
        if (c == '"' && peek(1) == '|') {
            validateLineForBaseline(currentLine.toString(), baselineColumn, line);
            
            if (column != baselineColumn) {
throw new RuntimeException(
    "Multiline string closing delimiter '\"|' must align with opening delimiter baseline. " +
    "Opening '|' was at column " + baselineColumn + ", closing '\"' at column " + column + ". " +
    "All content indentation is measured from column " + baselineColumn + ".");
            }
            
            if (currentLine.length() > 0) {
                String strippedLine = stripLeftOfBaseline(currentLine.toString(), baselineColumn, line);
                currentStringPart.append(strippedLine);
                currentLine.setLength(0);
            }
            
            if (currentStringPart.length() > 0) {
                interpolations.add(new Token(TokenType.STRING_LIT, currentStringPart.toString(), startLine, startColumn));
            }
            
            consume();
            consume();
            
            while (position < input.length()) {
                char after = peek();
                if (after == '\n' || after == '\r') break;
                if (Character.isWhitespace(after)) {
                    consume();
                    continue;
                }
                if (after == ')' || after == '.' || after == '+' || after == ',') {
                    break;
                }
                
                position -= 2;
                line = startLine;
                column = startColumn;
                throw new RuntimeException(
                    "After multiline string closing delimiter '\"|', only whitespace or ')', '.', '+', ',' allowed on same line. " +
                    "Found: '" + after + "' at line " + line + ", column " + column
                );
            }
            
            if (!interpolations.isEmpty()) {
                StringBuilder compositeText = new StringBuilder("|\"");
                for (Token part : interpolations) {
                    if (part.type == TokenType.STRING_LIT) {
                        compositeText.append(part.text);
                    } else {
                        compositeText.append("{").append(part.text).append("}");
                    }
                }
                compositeText.append("\"|");
                
                return new Token(
                    TokenType.STRING_LIT,
                    compositeText.toString(),
                    startLine,
                    startColumn,
                    interpolations
                );
            }
            
            StringBuilder result = new StringBuilder();
            for (Token part : interpolations) {
                if (part.type == TokenType.STRING_LIT) {
                    result.append(part.text);
                }
            }
            return new Token(TokenType.STRING_LIT, result.toString(), startLine, startColumn);
        }
        
        char ch = peek();
        
        if (currentColumnInLine < baselineColumn && !Character.isWhitespace(ch) && ch != '\\' && ch != '{') {
            throw new RuntimeException(
                "Multiline string violation at line " + line + ", column " + currentColumnInLine + "\n" +
                "Character '" + ch + "' appears to the left of baseline column " + baselineColumn + "\n" +
                "All content must start at or right of the opening '|' column (column " + baselineColumn + ")\n" +
                "Add " + (baselineColumn - currentColumnInLine) + " more spaces before this character"
            );
        }
        
        if (ch == '\\') {
            consume();
            currentColumnInLine++;
            
            if (position >= input.length()) {
                throw new RuntimeException("Unterminated escape sequence at line " + line);
            }
            char escaped = consume();
            currentColumnInLine++;
            
            currentLine.append('\\').append(escaped);
            
        } else if (ch == '{') {
            int braceLine = line;
            int braceColumn = column;
            consume();
            currentColumnInLine++;
            
            if (currentColumnInLine - 1 < baselineColumn) {
                throw new RuntimeException(
                    "Multiline string violation at line " + line + ", column " + (currentColumnInLine - 1) + "\n" +
                    "Interpolation '{' appears to the left of baseline column " + baselineColumn + "\n" +
                    "All content must start at or right of the opening '|' column (column " + baselineColumn + ")"
                );
            }
            
            int savedPos = position;
            int savedLine = line;
            int savedColumn = column;
            int savedColumnInLine = currentColumnInLine;
            
            boolean foundOnlyWhitespace = true;
            boolean foundClosingBrace = false;
            
            while (position < input.length()) {
                char nextChar = peek();
                
                if (nextChar == '}') {
                    foundClosingBrace = true;
                    break;
                } else if (Character.isWhitespace(nextChar)) {
                    consume();
                    currentColumnInLine++;
                    if (nextChar == '\n' || nextChar == '\r') {
                        if (nextChar == '\n') {
                            line++;
                            column = 1;
                            currentColumnInLine = 1;
                        } else if (nextChar == '\r') {
                            if (position < input.length() && peek() == '\n') {
                                consume();
                                line++;
                                column = 1;
                                currentColumnInLine = 1;
                            }
                        }
                    }
                } else {
                    foundOnlyWhitespace = false;
                    break;
                }
            }
            
            if (foundOnlyWhitespace && foundClosingBrace) {
                throw new RuntimeException(
                    "Syntax Error: Empty string interpolation at line " + braceLine + 
                    ", column " + braceColumn + "\n" +
                    "Interpolation must contain an expression: {expression}\n" +
                    "If you want a literal '{' character, escape it: \\{"
                );
            }
            
            position = savedPos;
            line = savedLine;
            column = savedColumn;
            currentColumnInLine = savedColumnInLine;
            
            if (currentLine.length() > 0) {
                String strippedLine = stripLeftOfBaseline(currentLine.toString(), baselineColumn, line);
                currentStringPart.append(strippedLine);
                currentLine.setLength(0);
            }
            if (currentStringPart.length() > 0) {
                interpolations.add(new Token(TokenType.STRING_LIT, currentStringPart.toString(), startLine, startColumn));
                currentStringPart.setLength(0);
            }
            
            StringBuilder interpolationContent = new StringBuilder();
            int braceDepth = 1;
            
            while (position < input.length() && braceDepth > 0) {
                char interpolationChar = peek();
                
                if (interpolationChar == '\\') {
                    interpolationContent.append(consume());
                    currentColumnInLine++;
                    if (position < input.length()) {
                        interpolationContent.append(consume());
                        currentColumnInLine++;
                    }
                } else if (interpolationChar == '{') {
                    braceDepth++;
                    interpolationContent.append(consume());
                    currentColumnInLine++;
                } else if (interpolationChar == '}') {
                    braceDepth--;
                    if (braceDepth > 0) {
                        interpolationContent.append(consume());
                        currentColumnInLine++;
                    } else {
                        consume();
                        currentColumnInLine++;
                    }
                } else if (interpolationChar == '\n' || interpolationChar == '\r') {
                    if (interpolationChar == '\n') {
                        interpolationContent.append('\n');
                        consume();
                        line++;
                        column = 1;
                        currentColumnInLine = 1;
                    } else if (interpolationChar == '\r') {
                        consume();
                        currentColumnInLine++;
                        if (position < input.length() && peek() == '\n') {
                            interpolationContent.append('\n');
                            consume();
                            line++;
                            column = 1;
                            currentColumnInLine = 1;
                        } else {
                            interpolationContent.append('\r');
                        }
                    }
                } else {
                    interpolationContent.append(consume());
                    currentColumnInLine++;
                }
            }
            
            if (braceDepth > 0) {
                throw new RuntimeException("Unclosed interpolation at line " + line + ", column " + column);
            }
            
            interpolations.add(new Token(
                TokenType.INTERPOL,
                interpolationContent.toString(),
                braceLine,
                braceColumn
            ));
            
        } else if (ch == '}') {
            throw new RuntimeException(
                "Unmatched '}' in multiline string at line " + line + ", column " + column +
                "\nIf you want a literal '}', escape it: \\}"
            );
        } else if (ch == '\n' || ch == '\r') {
            validateLineForBaseline(currentLine.toString(), baselineColumn, line);
            
            String strippedLine = stripLeftOfBaseline(currentLine.toString(), baselineColumn, line);
            currentStringPart.append(strippedLine);
            currentStringPart.append('\n');
            currentLine.setLength(0);
            
            if (ch == '\n') {
                consume();
                line++;
                column = 1;
                currentColumnInLine = 1;
            } else if (ch == '\r') {
                consume();
                if (position < input.length() && peek() == '\n') {
                    consume();
                }
                line++;
                column = 1;
                currentColumnInLine = 1;
            }
            
        } else {
            currentLine.append(consume());
            currentColumnInLine++;
        }
    }
    
    throw new RuntimeException(
        "Unterminated multiline string starting at line " + 
        startLine + ", column " + startColumn
    );
}

private void validateLineForBaseline(String line, int baselineColumn, int lineNumber) {
    for (int i = 0; i < baselineColumn - 1 && i < line.length(); i++) {
        if (!Character.isWhitespace(line.charAt(i))) {
            throw new RuntimeException(
                "Multiline string violation at line " + lineNumber + ", column " + (i + 1) + "\n" +
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

  private Token readSymbol() {
    int startLine = line;
    int startColumn = column;
    
    char c1 = peek();
    
    Token symbolToken = process(startLine, startColumn, c1);
    if (symbolToken != null) {
        return symbolToken;
    }
    
    return new Token(TokenType.INVALID, String.valueOf(c1), startLine, startColumn);
  }

  private Token process(int startLine, int startColumn, char c1) {
    switch (c1) {
      case ':': 
          return processPatterns(startLine, startColumn,
              ":=", DOUBLE_COLON_ASSIGN,
              "::", DOUBLE_COLON,
              ":", COLON
          );
      case '=':
          return processPatterns(startLine, startColumn,
              "==", EQ,
              "=", ASSIGN
          );
      case '>':
          return processPatterns(startLine, startColumn,
              ">=", GTE,
              ">", GT
          );
      case '<':
          return processPatterns(startLine, startColumn,
              "<=", LTE,
              "<", LT
          );
      case '!':
          return processPatterns(startLine, startColumn,
              "!=", NEQ,
              "!", BANG
          );
      case '+':
          return processPatterns(startLine, startColumn,
              "+=", PLUS_ASSIGN,
              "+", PLUS
          );
      case '-':
          return processPatterns(startLine, startColumn,
              "-=", MINUS_ASSIGN,
              "-", MINUS
          );
      case '*':
          return processPatterns(startLine, startColumn,
              "*=", MUL_ASSIGN,
              "*", MUL
          );
      case '/':
          return processPatterns(startLine, startColumn,
              "/=", DIV_ASSIGN,
              "/", DIV
          );
      case '~':
          return processPatterns(startLine, startColumn, "~>", TILDE_ARROW);
      case '|': 
          return processPatterns(startLine, startColumn, "|", PIPE);
      case '&': return processPatterns(startLine, startColumn, "&", AMPERSAND);
      case '?': return processPatterns(startLine, startColumn, "?", QUESTION);
      case '%': return processPatterns(startLine, startColumn, "%", MOD);
      case '.': return processPatterns(startLine, startColumn, ".", DOT);
      case ',': return processPatterns(startLine, startColumn, ",", COMMA);
      case '(': return processPatterns(startLine, startColumn, "(", LPAREN);
      case ')': return processPatterns(startLine, startColumn, ")", RPAREN);
      case '{': return processPatterns(startLine, startColumn, "{", LBRACE);
      case '}': return processPatterns(startLine, startColumn, "}", RBRACE);
      case '[': return processPatterns(startLine, startColumn, "[", LBRACKET);
      case ']': return processPatterns(startLine, startColumn, "]", RBRACKET);
      case '_': return processPatterns(startLine, startColumn, "_", UNDERSCORE);
      default: return null;
    }
  }

  private Token processPatterns(int startLine, int startColumn, Object... patternsAndSymbols) {
    if (patternsAndSymbols.length % 2 != 0) {
        throw new IllegalArgumentException("processPatterns() requires pattern/symbol pairs");
    }
    
    for (int i = 0; i < patternsAndSymbols.length; i += 2) {
        Object patternObj = patternsAndSymbols[i];
        Object symbolObj = patternsAndSymbols[i + 1];
        
        if (!(patternObj instanceof String)) {
            throw new IllegalArgumentException(
                "Pattern at position " + i + " must be String"
            );
        }
        if (!(symbolObj instanceof Symbol)) {
            throw new IllegalArgumentException(
                "Symbol at position " + (i + 1) + " must be Symbol"
            );
        }
        
        String pattern = (String) patternObj;
        Symbol symbol = (Symbol) symbolObj;
        
        if (matches(pattern)) {
            String text = consume(pattern.length());
            return new Token(TokenType.SYMBOL, text, startLine, startColumn, symbol);
        }
    }
    return null;
  }

  private boolean matches(String pattern) {
    if (position + pattern.length() > input.length()) {
        return false;
    }
    for (int i = 0; i < pattern.length(); i++) {
        if (input.charAt(position + i) != pattern.charAt(i)) {
            return false;
        }
    }
    return true;
  }

  private String consume(int length) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < length; i++) {
        result.append(consume());
    }
    return result.toString();
  }

  private Token create(TokenType type) {
    return new Token(type, type.name().toLowerCase(), this.line, this.column);
  }

  private void scanLineComment() {
    while (position < input.length() && peek() != '\n') consume();
  }

  private void scanBlockComment() {
    consume();
    consume();
    while (position < input.length() - 1) {
      if (peek() == '*' && peek(1) == '/') {
        consume();
        consume();
        return;
      }
      consume();
    }
  }

  private char peek() {
    return peek(0);
  }

  private char peek(int offset) {
    return (position + offset >= input.length()) ? '\0' : input.charAt(position + offset);
  }

  private char consume() {
    char c = input.charAt(position++);
    if (c == '\n') {
      line++;
      column = 1;
    } else {
      column++;
    }
    return c;
  }
}