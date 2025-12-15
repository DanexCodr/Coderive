package cod.parser;

import cod.error.ParseError;

import cod.syntax.Keyword;
import static cod.syntax.Keyword.*;
import cod.syntax.Symbol;
import static cod.syntax.Symbol.*;

import java.util.List;

import cod.lexer.MainLexer.Token;
import cod.lexer.MainLexer.TokenType;
import static cod.lexer.MainLexer.TokenType.*;

/**
 * Base class for all parser components, handling token stream management and
 * common utility methods. Uses shared PositionHolder for position synchronization.
 */
public abstract class BaseParser {
    protected final List<Token> tokens;
    protected final PositionHolder position;

    public BaseParser(List<Token> tokens, PositionHolder position) {
        this.tokens = tokens;
        this.position = position;
    }
    
    // --- Token Management ---

    protected Token currentToken() {
        int pos = position.get();
        if (pos >= tokens.size()) {
            // Return synthetic EOF token
            if (tokens.isEmpty()) {
                return new Token(EOF, "EOF", 1, 1);
            }
            Token last = tokens.get(tokens.size() - 1);
            return new Token(EOF, "EOF", last.line, last.column + 1);
        }
        return tokens.get(pos);
    }

    // UPDATED: Faster, safer bounds checking
    protected Token peek(int offset) {
        int targetPos = position.get() + offset;
        return (targetPos >= 0 && targetPos < tokens.size()) ?
                tokens.get(targetPos) : null;
    }

    // NEW: Fast lookahead helpers
    protected Token lookahead(int n) {
        return peek(n);
    }

    protected Token lookahead() {
        return peek(1);
    }
    
    // NEW: Helper for scanning loops (moved from DeclarationParser)
    protected Token lookaheadFrom(int baseOffset, int additionalOffset) {
        int pos = getPosition() + baseOffset + additionalOffset;
        return pos >= 0 && pos < tokens.size() ? tokens.get(pos) : null;
    }

    protected boolean match(Symbol expectedSymbol) {
        Token current = currentToken();
        return current.type == TokenType.SYMBOL && current.symbol == expectedSymbol;
    }

    protected boolean match(TokenType... types) {
        Token current = currentToken();
        if (current.type == EOF) {
            for (TokenType type : types) { 
                if (type == EOF) return true;
            }
            return false;
        }
        for (TokenType type : types) {
            if (current.type == type) return true;
        }
        return false;
    }

    protected Token consume() {
        Token token = currentToken();
        if (token.type != EOF && position.get() < tokens.size()) {
            position.up();
        }
        return token;
    }

    protected Token consume(TokenType expectedType) {
        Token token = currentToken();
        if (token.type == expectedType) {
            if (token.type != EOF && position.get() < tokens.size()) {
                position.up();
            }
            return token;
        }
        throw new ParseError("Expected " + getTypeName(expectedType) + " but found " +
                getTypeName(token.type) + " ('" + token.text + "') at line " + token.line + ":" + token.column);
    }

    protected Token consume(Symbol expectedSymbol) {
        Token token = currentToken();
        if (token.type == TokenType.SYMBOL && token.symbol == expectedSymbol) {
            if (token.type != EOF && position.get() < tokens.size()) {
                position.up();
            }
            return token;
        }
        throw new ParseError("Expected " + expectedSymbol + " but found " +
                getTypeName(token.type) + " ('" + token.text + "') at line " + token.line + ":" + token.column);
    }
    
    protected Token consume(boolean condition) {
         if (condition) return consume();
         Token current = currentToken();
         throw new ParseError("Consumption condition not met at: " + current.text +
             " (" + getTypeName(current.type) + ")" +
             " at line " + current.line + ":" + current.column);
    }

    protected boolean tryConsume(Symbol expectedSymbol) {
        if (match(expectedSymbol)) {
            consume(expectedSymbol);
            return true;
        }
        return false;
    }

    protected boolean tryConsume(TokenType expectedType) {
        if (match(expectedType)) {
            consume(expectedType);
            return true;
        }
        return false;
    }
    
    // --- Keyword and Symbol Helpers ---

    protected boolean isKeyword(Keyword expectedKeyword) {
        Token current = currentToken();
        return current.type == KEYWORD && current.text.equals(expectedKeyword.toString());
    }
    
    protected boolean isKeywordAt(int offset, Keyword expectedKeyword) {
        Token token = peek(offset);
        return token != null && token.type == KEYWORD && token.text.equals(expectedKeyword.toString());
    }

    protected void consumeKeyword(Keyword expectedKeyword) {
        Token token = currentToken();
        if (token.type == KEYWORD && token.text.equals(expectedKeyword.toString())) {
            if (position.get() < tokens.size()) {
                position.up();
            }
            return;
        }
        throw new ParseError("Expected keyword '" + expectedKeyword.toString() + "' but found " +
                getTypeName(token.type) + " ('" + token.text + "') at line " + token.line + ":" + token.column);
    }
    
    protected boolean isSymbolAt(int offset, Symbol symbol) {
        Token token = peek(offset);
        return token != null && token.type == SYMBOL && token.symbol == symbol;
    }
    
    protected String getTypeName(TokenType type) {
        return type.toString();
    }
    
    // NEW: Helper to check if a token starts an expression (moved from ExpressionParser)
    protected boolean isExpressionStart(Token t) {
    if (t == null) return false;
    
    // Skip whitespace and comments - look for the next real token
    if (t.type == WS || t.type == LINE_COMMENT || t.type == BLOCK_COMMENT) {
        return false; // This tells the caller to skip this token
    }
    
    String text = t.text;
    return t.type == INT_LIT || t.type == FLOAT_LIT || 
           t.type == STRING_LIT || t.type == BOOL_LIT ||
           t.type == ID || t.symbol == LPAREN ||
           t.symbol == LBRACKET || t.symbol == BANG ||
           t.symbol == PLUS || t.symbol == MINUS ||
           (t.type == KEYWORD && (
               NULL.toString().equals(text) || 
               TRUE.toString().equals(text) || 
               FALSE.toString().equals(text) ||
               INPUT.toString().equals(text)
           ));
}
    
    // --- Generic Grammar Helpers ---
    
    protected String parseQualifiedName() {
        StringBuilder name = new StringBuilder();
        name.append(consume(ID).text);
        while (tryConsume(DOT)) {
            name.append(".");
            name.append(consume(ID).text);
        }
        return name.toString();
    }
    
    protected boolean isTypeKeyword(String text) {
        // Includes ARRAY keyword for declaration purposes
        return text.equals(INT.toString()) || text.equals(TEXT.toString()) ||
               text.equals(FLOAT.toString()) || text.equals(BOOL.toString()); 
    }

protected boolean isTypeStart(Token token) {
    if (token == null) return false;
    // REMOVED: var keyword from type checking
    // Checks for (int, text, bool, float, ID, LPAREN)
    // LBRACKET for prefix array types e.g., [int]
    return isTypeKeyword(token.text) || token.type == ID || token.symbol == LPAREN || token.symbol == LBRACKET;
}
     
    protected boolean skipBrackets(int startOffset) {
        int p = position.get() + startOffset;
        while (p < tokens.size() && isSymbolAt(p - position.get(), LBRACKET)) {
            if (p + 1 >= tokens.size() || !isSymbolAt(p + 1 - position.get(), RBRACKET)) {
                return false;
            }
            p += 2;
        }
        return true;
    }
    
/**
 * Skips a complete type definition starting at startPos (relative to tokens list, not parser position).
 * Handles prefix arrays [int], groups (int, int), and unions int|float.
 * Returns the index AFTER the type, or -1 if not a valid type.
 */
protected int skipType(int startPos) {
    int pos = startPos;
    if (pos >= tokens.size()) return -1;
    
    Token t = tokens.get(pos);
    
    // 1. Handle Prefix Array: [Type] or []
    if (t.symbol == LBRACKET) {
        pos++;
        if (pos >= tokens.size()) return -1;
        
        if (tokens.get(pos).symbol == RBRACKET) {
            // Empty brackets []
            pos++;
        } else {
            // Recursive skip for inner type [int]
            pos = skipType(pos);
            if (pos == -1 || pos >= tokens.size()) return -1;
            
            if (tokens.get(pos).symbol == RBRACKET) {
                pos++;
            } else {
                return -1;
            }
        }
    } 
    // 2. Handle Group: (Type, Type)
    else if (t.symbol == LPAREN) {
        pos++;
        while (true) {
            pos = skipType(pos);
            if (pos == -1 || pos >= tokens.size()) return -1;
            
            Token sep = tokens.get(pos);
            if (sep.symbol == COMMA) {
                pos++;
            } else if (sep.symbol == RPAREN) {
                pos++;
                break;
            } else {
                return -1;
            }
        }
    } 
    // 3. Handle Primitive or ID
    else if (isTypeKeyword(t.text) || t.type == ID) {
        pos++;
    } 
    else {
        return -1;
    }
    
    // 4. Handle Unions (|)
    if (pos < tokens.size() && tokens.get(pos).symbol == PIPE) {
        pos++; // Consume |
        return skipType(pos); // Recurse for next part
    }

    return pos;
}

    /**
 * Entry point for parsing types. 
 * Handles complex structures like: (int, text) | [bool]
 */
protected String parseTypeReference() {
    StringBuilder type = new StringBuilder();
    
    // 1. Parse the first type component
    if (match(LBRACKET)) {
        // Prefix Array handling [Type] or []
        consume(LBRACKET);
        if (match(RBRACKET)) {
            // Empty brackets [] -> dynamic array
            consume(RBRACKET);
            type.append("[]"); // Just [] for dynamic arrays
        } else {
            // Recursively parse inner type
            String inner = parseTypeReference();
            consume(RBRACKET);
            // Keep [type] notation
            type.append("[").append(inner).append("]");
        }
    } else if (match(LPAREN)) {
        type.append(parseGroupedType());
    } else {
        Token typeToken = currentToken();
        if (isTypeStart(typeToken) && typeToken.symbol != LBRACKET) {
            String typeName = consume().text;
            type.append(typeName);
        } else {
            Token current = currentToken();
            throw new ParseError("Expected type name but got " +
                getTypeName(current.type) + " ('" + current.text + "')" +
                " at line " + current.line + ":" + current.column);
        }
    }
    
    // 2. Handle Union types (|)
    while (match(PIPE)) {
        consume(PIPE);
        type.append("|");
        type.append(parseTypeReference());
    }
    
    return type.toString();
}

    /**
     * Handles Grouped Types: (Type, Type)
     */
    private String parseGroupedType() {
        consume(LPAREN);
        StringBuilder group = new StringBuilder("(");
        
        // Recursively parse the first element
        group.append(parseTypeReference()); 
        
        // Parse subsequent elements
        while (tryConsume(COMMA)) {
            group.append(",");
            group.append(parseTypeReference());
        }
        
        consume(RPAREN);
        group.append(")");
        return group.toString();
    }
    
    protected boolean isVisibilityModifier() {
        return isKeyword(SHARE) || isKeyword(LOCAL);
    }
    
    // --- Position Access (for debugging) ---

    /**
     * Gets the current shared position (for debugging/logging only)
     */
    public int getPosition() {
        return this.position.get();
    }
}
