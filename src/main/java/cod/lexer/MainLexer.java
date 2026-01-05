package cod.lexer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cod.syntax.Keyword.*;
import cod.syntax.Symbol;
import static cod.syntax.Symbol.*;

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
    StringBuilder sb = new StringBuilder();
    while (position < input.length() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
        sb.append(consume());
    }
    String text = sb.toString();
    
    if (KEYWORDS.contains(text)) {
        return create(TokenType.KEYWORD, text);
    }
    
    return create(TokenType.ID, text);
  }

  private Token readNumber() {
    StringBuilder sb = new StringBuilder();
    boolean isFloat = false;
    
    // 1. Read integer part
    while (position < input.length() && Character.isDigit(peek())) {
        sb.append(consume());
    }
    
    // 2. Read fractional part
    if (peek() == '.') {
        isFloat = true;
        sb.append(consume());
        while (position < input.length() && Character.isDigit(peek())) {
            sb.append(consume());
        }
    }
    
    // 3. Read Numeric Shorthand Suffix (K, M, Qi, etc.)
    String suffix = readSuffix();
    if (!suffix.isEmpty()) {
        sb.append(suffix);
        isFloat = true; 
    } else {
        // 4. NEW: Read Standard Scientific Notation (e/E)
        String exponent = readExponent();
        if (!exponent.isEmpty()) {
            sb.append(exponent);
            isFloat = true;
        }
    }
    
    String numberText = sb.toString();
    return create(
        isFloat ? TokenType.FLOAT_LIT : TokenType.INT_LIT, numberText);
  }

  private String readSuffix() {
    if (position >= input.length()) return "";
    
    char c1 = peek();
    
    // Check custom suffixes (Case sensitive)
    if (c1 == 'K' || c1 == 'M' || c1 == 'B' || c1 == 'T') {
        return String.valueOf(consume());
    }
    
    if (c1 == 'Q') {
        consume(); // consume Q
        if (peek() == 'i') {
            consume(); // consume i
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
    sb.append(consume()); // Consume 'e' or 'E'
    
    // Consume optional sign (+ or -)
    char c2 = peek();
    if (c2 == '+' || c2 == '-') {
        sb.append(consume());
    }
    
    // Must be followed by at least one digit
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
    StringBuilder sb = new StringBuilder();
    consume(); // Consume opening quote
    while (position < input.length() && peek() != '"') {
        if (peek() == '\\') {
            consume(); // consume the backslash
            char escaped = consume();
            switch (escaped) {
                case 'n': sb.append('\n'); break;
                case 't': sb.append('\t'); break;
                case 'r': sb.append('\r'); break;
                case '\\': sb.append('\\'); break;
                case '"': sb.append('"'); break;
                default: sb.append('\\').append(escaped); break;
            }
        } else {
            sb.append(consume());
        }
    }
    if (position < input.length()) consume(); // Consume closing quote
    return create(TokenType.STRING_LIT, sb.toString());
  }

  // FINAL VERSION: | column determines baseline
  private Token readMultilineString() {
    int startLine = line;
    int startColumn = column;  // | position = BASELINE
    
    // Check opening delimiter "|" 
    if (!(peek() == '|' && peek(1) == '"')) {
        throw new RuntimeException("Invalid multiline string opening at line " + line + ", column " + column);
    }
    
    // The | character's column IS the baseline
    int baseColumn = startColumn;
    
    // Consume opening delimiter
    consume(); // Consume |
    consume(); // Consume "
    
    // Allow ONLY whitespace after opening delimiter on same line
    while (position < input.length()) {
        char after = peek();
        if (after == '\n' || after == '\r') {
            break; // End of line reached, valid
        } else if (!Character.isWhitespace(after)) {
            position -= 2; // Go back to | position
            line = startLine;
            column = startColumn;
            throw new RuntimeException(
                "After multiline string opening delimiter '|\"', only whitespace allowed on same line. " +
                "String content must start on next line. Found: '" + after + "' at line " + line + ", column " + column
            );
        }
        consume(); // Skip whitespace
    }
    
    // Skip the formatting newline after |"
    if (position < input.length() && peek() == '\n') {
        consume();
        line++;
        column = 1;
    } else if (position < input.length() && peek() == '\r') {
        consume(); // \r
        if (position < input.length() && peek() == '\n') {
            consume(); // \n in \r\n
        }
        line++;
        column = 1;
    }
    
    List<String> contentLines = new ArrayList<String>();
    
    while (position < input.length()) {
        char c = peek();
        
        // Check for closing delimiter "|
        if (c == '"' && peek(1) == '|') {
            // CRITICAL: Closing " must align with opening | baseline
            if (column != baseColumn) {
                throw new RuntimeException(
                    "Multiline string closing delimiter '\"|' must align with opening delimiter baseline. " +
                    "Opening '|' was at column " + baseColumn + ", closing '\"' at column " + column + ". " +
                    "All content indentation is measured from column " + baseColumn + "."
                );
            }
            
            // Build dedented result relative to baseline
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < contentLines.size(); i++) {
                if (i > 0) result.append('\n');
                result.append(contentLines.get(i));
            }
            
            // Consume closing delimiter
            consume(); // Consume "
            consume(); // Consume |
            
            // Allow: whitespace OR ) . + , after closing delimiter
            while (position < input.length()) {
                char after = peek();
                if (after == '\n' || after == '\r') break;
                if (Character.isWhitespace(after)) {
                    consume(); // Skip whitespace
                    continue;
                }
                // These specific connectors are allowed after "|
                if (after == ')' || after == '.' || after == '+' || after == ',') {
                    break; // Stop validation - these are valid
                }
                
                // Anything else is an error
                position -= 2; // Go back to " position
                line = startLine;
                column = startColumn;
                throw new RuntimeException(
                    "After multiline string closing delimiter '\"|', only whitespace or ')', '.', '+', ',' allowed on same line. " +
                    "Found: '" + after + "' at line " + line + ", column " + column
                );
            }
            
            return new Token(TokenType.STRING_LIT, result.toString(), startLine, startColumn);
        }
        
        // Read a content line
        int lineStartColumn = column;
        StringBuilder lineBuilder = new StringBuilder();
        
        while (position < input.length()) {
            char ch = peek();
            if (ch == '\n' || ch == '\r') break;
            
            if (ch == '\\') {
                // Handle escape sequences
                consume();
                if (position >= input.length()) {
                    throw new RuntimeException("Unterminated escape sequence at line " + line);
                }
                char escaped = consume();
                switch (escaped) {
                    case 'n': lineBuilder.append('\n'); break;
                    case 't': lineBuilder.append('\t'); break;
                    case 'r': lineBuilder.append('\r'); break;
                    case '\\': lineBuilder.append('\\'); break;
                    case '"': lineBuilder.append('"'); break;
                    case '|': lineBuilder.append('|'); break;
                    default: lineBuilder.append('\\').append(escaped); break;
                }
            } else {
                lineBuilder.append(consume());
            }
        }
        
        String rawLine = lineBuilder.toString();
        StringBuilder processedLine = new StringBuilder();
        
        // Calculate leading whitespace
        int leadingWhitespace = 0;
        while (leadingWhitespace < rawLine.length() && 
               Character.isWhitespace(rawLine.charAt(leadingWhitespace))) {
            leadingWhitespace++;
        }
        
        // Visual column of first non-whitespace
        int visualColumn = lineStartColumn + leadingWhitespace;
        
        // Check if line is properly indented (must be at or after baseline)
        if (visualColumn < baseColumn) {
            throw new RuntimeException(
                "Multiline string content must be indented to at least baseline column " + baseColumn + ". " +
                "Line starts at column " + lineStartColumn + " with " + leadingWhitespace + 
                " whitespace = column " + visualColumn + " which is before baseline."
            );
        }
        
        // Calculate relative indentation (beyond baseline)
        int relativeIndent = visualColumn - baseColumn;
        
        // Add relative indentation spaces
        for (int i = 0; i < relativeIndent; i++) {
            processedLine.append(' ');
        }
        
        // Add actual content (after removing leading whitespace)
        if (leadingWhitespace < rawLine.length()) {
            processedLine.append(rawLine.substring(leadingWhitespace));
        }
        
        contentLines.add(processedLine.toString());
        
        // Handle line ending
        if (position < input.length()) {
            char lineEnd = peek();
            if (lineEnd == '\n') {
                consume();
                line++;
                column = 1;
            } else if (lineEnd == '\r') {
                consume();
                if (position < input.length() && peek() == '\n') {
                    consume();
                }
                line++;
                column = 1;
            }
        }
    }
    
    throw new RuntimeException(
        "Unterminated multiline string starting at line " + 
        startLine + ", column " + startColumn
    );
  }

  private Token readSymbol() {
    char c1 = peek();
    
    switch (c1) {
      case ':': 
          return process(
              ":=", DOUBLE_COLON_ASSIGN,
              "::", DOUBLE_COLON,
              ":", COLON
          );
      case '=':
          return process(
              "==", EQ,
              "=", ASSIGN
          );
      case '>':
          return process(
              ">=", GTE,
              ">", GT
          );
      case '<':
          return process(
              "<=", LTE,
              "<", LT
          );
      case '!':
          return process(
              "!=", NEQ,
              "!", BANG
          );
      case '+':
          return process(
              "+=", PLUS_ASSIGN,
              "+", PLUS
          );
      case '-':
          return process(
              "-=", MINUS_ASSIGN,
              "-", MINUS
          );
      case '*':
          return process(
              "*=", MUL_ASSIGN,
              "*", MUL
          );
      case '/':
          return process(
              "/=", DIV_ASSIGN,
              "/", DIV
          );
      case '~':
          return process("~>", TILDE_ARROW);
      case '|': 
          // Already handled in scanNextToken for multiline strings
          return process("|", PIPE);
      case '&': return process("&", AMPERSAND);
      case '?': return process("?", QUESTION);
      case '%': return process("%", MOD);
      case '.': return process(".", DOT);
      case ',': return process(",", COMMA);
      case '(': return process("(", LPAREN);
      case ')': return process(")", RPAREN);
      case '{': return process("{", LBRACE);
      case '}': return process("}", RBRACE);
      case '[': return process("[", LBRACKET);
      case ']': return process("]", RBRACKET);
      case '_': return process("_", UNDERSCORE);
      default: return create(TokenType.INVALID);
    }
  }

  private Token process(Object... patternsAndSymbols) {
    // Must have an even number of arguments (pattern, symbol pairs)
    if (patternsAndSymbols.length % 2 != 0) {
        throw new IllegalArgumentException("process() requires pattern/symbol pairs");
    }
    
    for (int i = 0; i < patternsAndSymbols.length; i += 2) {
        Object patternObj = patternsAndSymbols[i];
        Object symbolObj = patternsAndSymbols[i + 1];
        
        // Validate types - Java 7 style
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
            return create(symbol, text);
        }
    }
    return create(TokenType.INVALID);
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

  private Token create(TokenType type, String text) {
    return new Token(type, text, this.line, this.column);
  }

  private Token create(TokenType type) {
    return new Token(type, type.name().toLowerCase(), this.line, this.column);
  }

  private Token create(Symbol symbol, String text) {
    return new Token(TokenType.SYMBOL, text, this.line, this.column, symbol);
  }

  private void scanLineComment() {
    while (position < input.length() && peek() != '\n') consume();
  }

  private void scanBlockComment() {
    consume();
    consume(); // consume '/*'
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