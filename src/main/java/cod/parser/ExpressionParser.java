package cod.parser;

import cod.ast.ASTFactory;
import cod.error.ParseError;
import cod.ast.nodes.*;

import cod.lexer.MainLexer.Token;
import static cod.lexer.MainLexer.TokenType.*;

import static cod.syntax.Symbol.*;
import static cod.syntax.Keyword.*;

import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

/**
 * Predictive parser for expressions using Pratt precedence.
 * Replaces complex lookahead with clean precedence-based parsing.
 * Maintains 100% API compatibility with the previous implementation.
 */
public class ExpressionParser extends BaseParser {
    
    // Precedence levels (higher = tighter binding)
    private static final int PREC_ASSIGNMENT = 10;
    private static final int PREC_EQUALITY = 50;
    private static final int PREC_COMPARISON = 60;
    private static final int PREC_TERM = 70;
    private static final int PREC_FACTOR = 80;
    private static final int PREC_UNARY = 90;
    private static final int PREC_CALL = 100;
    
    public ExpressionParser(List<Token> tokens, PositionHolder position) {
        super(tokens, position);
    }

    // === PUBLIC API METHODS (SAME SIGNATURES AS BEFORE) ===
    
    public ExprNode parseExpression() {
        if (isKeyword(ALL) || isKeyword(ANY)) {
            return parseBooleanChain();
        }
        return parsePrecedence(PREC_ASSIGNMENT);
    }

    public MethodCallNode parseMethodCall() {
        String qualifiedNameStr = parseQualifiedName();
        String methodName = qualifiedNameStr;
        if (qualifiedNameStr.contains(".")) {
             methodName = qualifiedNameStr.substring(qualifiedNameStr.lastIndexOf('.') + 1);
        }
        MethodCallNode call = ASTFactory.createMethodCall(methodName, qualifiedNameStr);
        
        consume(LPAREN);
        
        if (isKeyword(ALL) || isKeyword(ANY)) {
            return parseConditionalChainCall(call);
        }
        
        if (!match(RPAREN)) {
            call.arguments.add(parseExpression());
            while (tryConsume(COMMA)) {
                call.arguments.add(parseExpression());
            }
        }
        consume(RPAREN);
        return call;
    }

    public IndexAccessNode parseIndexAccessContinuation(ExprNode arrayExpr) {
        consume(LBRACKET);
        ExprNode indexExpr = parseExpression();
        consume(RBRACKET);
        return ASTFactory.createIndexAccess(arrayExpr, indexExpr);
    }

    public List<String> parseReturnSlots() {
        consume(LBRACKET);
        List<String> slots = new ArrayList<String>();
        do {
            if (match(ID)) {
                slots.add(consume(ID).text);
            } else if (match(INT_LIT)) {
                slots.add(consume(INT_LIT).text);
            } else {
                throw new ParseError("Expected slot name or index, found " + currentToken().text);
            }
        } while (tryConsume(COMMA));
        consume(RBRACKET);
        return slots;
    }
    
    private ExprNode parseIfExpression() {
    consumeKeyword(IF);
    ExprNode condition = parseExpression();
    
    // Parse then expression (can be in braces or not)
    ExprNode thenExpr;
    if (match(LBRACE)) {
        consume(LBRACE);
        thenExpr = parseExpression();
        consume(RBRACE);
    } else {
        thenExpr = parseExpression();
    }
    
    // ELSE is REQUIRED for expressions
    consumeKeyword(ELSE);
    
    // Parse else expression
    ExprNode elseExpr;
    if (match(LBRACE)) {
        consume(LBRACE);
        elseExpr = parseExpression();
        consume(RBRACE);
    } else {
        elseExpr = parseExpression();
    }
    
    return ASTFactory.createIfExpression(condition, thenExpr, elseExpr);
}

    // === CRITICAL METHOD: Called by StatementParser ===
public ExprNode parsePrimaryExpression() {
    ExprNode baseExpr;
    Token startToken = currentToken();

    if (match(LBRACKET)) {
        // FIX: Distinguish between [Array] and [Slot]:Call
        if (isSlotAccessExpression()) {
            List<String> slotNames = parseReturnSlots();
            consume(COLON);
            MethodCallNode methodCall = parseMethodCall();
            methodCall.slotNames = slotNames;
            baseExpr = methodCall;
        } else {
            baseExpr = parseArrayLiteral();
            }
       } else if (isKeyword(IF)) {
    return parseIfExpression();
} else if (match(INT_LIT)) {
    String intText = consume().text;
    try {
        // 1. Try int first (Fast path 1)
        int intValue = Integer.parseInt(intText);
        baseExpr = ASTFactory.createIntLiteral(intValue);
    } catch (NumberFormatException e1) {
        try {
            // 2. Try long (Fast path 2)
            long longValue = Long.parseLong(intText);
            baseExpr = ASTFactory.createLongLiteral(longValue);
        } catch (NumberFormatException e2) {
            // 3. Fallback: Too big for long. Use BigDecimal.
            BigDecimal bigDecimalValue = new BigDecimal(intText);
            baseExpr = ASTFactory.createFloatLiteral(bigDecimalValue);
        }
    }
} else if (match(FLOAT_LIT)) {
        Token floatToken = consume();
        String floatText = floatToken.text;
        
        Object resolvedValue = resolveFloatLiteralValue(floatText);
        
        if (resolvedValue instanceof BigDecimal) {
             // Case A: Shorthand (already resolved as BigDecimal)
             baseExpr = ASTFactory.createFloatLiteral((BigDecimal)resolvedValue);
        } else {
             // Case B: Regular Float Literal (e.g., 3.14)
             // FIX: Convert directly from string to BigDecimal for max precision
             BigDecimal bigDecimalValue = new BigDecimal(floatText);
             baseExpr = ASTFactory.createFloatLiteral(bigDecimalValue);
        }
        
    } else if (match(STRING_LIT)) {
        Token stringToken = consume();
        baseExpr = ASTFactory.createStringLiteral(stringToken.text);
        
    } else if (isKeyword(TRUE)) {
        consumeKeyword(TRUE);
        baseExpr = ASTFactory.createBoolLiteral(true);
        
    } else if (isKeyword(FALSE)) {
        consumeKeyword(FALSE);
        baseExpr = ASTFactory.createBoolLiteral(false);
         
    } else if (isKeyword(NULL)) {
        consumeKeyword(NULL);
        baseExpr = ASTFactory.createNullLiteral();
    
    // Handle INPUT as an expression token
    } else if (isKeyword(INPUT)) {
        consumeKeyword(INPUT);
        // Create input node with default "text" type and no variable binding (expression mode)
        baseExpr = ASTFactory.createInput(TEXT.toString(), null);
         
} else if (match(ID)) {
    if (isMethodCallFollows()) {
        baseExpr = parseMethodCall();
    } else {
        String idName = consume().text;
        // Allow underscore as identifier (will be validated in interpreter)
        baseExpr = ASTFactory.createIdentifier(idName);
    }
} else if (match(LPAREN)) {
        if (isTypeCast()) {
            baseExpr = parseTypeCast();
        } else {
            consume(LPAREN);
            ExprNode firstExpr = parseExpression();
            
            // Handle Tuples (expressions separated by commas)
            if (match(COMMA)) {
                List<ExprNode> elements = new ArrayList<ExprNode>();
                elements.add(firstExpr);
                
                // Consume the first comma and continue to parse subsequent elements
                while (tryConsume(COMMA)) {
                    elements.add(parseExpression());
                }
                
                // Check if the tuple is empty or terminated prematurely
                if (elements.size() == 1 && !match(RPAREN)) {
                     // This catches cases like (expr, )
                     throw new ParseError("Expected expression after comma in tuple but found " + 
                        currentToken().text, currentToken().line, currentToken().column);
                }
                
                consume(RPAREN);
                baseExpr = ASTFactory.createTuple(elements);
            } else {
                // No comma found, it was a simple grouped expression (a+b)
                consume(RPAREN);
                baseExpr = firstExpr; 
            }
        }
    } else {
        throw new ParseError("Unexpected token in primary expression: " + startToken.text +
            " (" + getTypeName(startToken.type) + ")" +
            " at line " + startToken.line + ":" + startToken.column);
    }

    while (match(LBRACKET)) {
        baseExpr = parseIndexAccessContinuation(baseExpr);
    }

    return baseExpr;
}

    // === PRIVATE IMPLEMENTATION (PREDICTIVE PARSING) ===

    private ExprNode parsePrecedence(int precedence) {
        ExprNode left = parsePrefix();
        
        while (true) {
            Token op = currentToken();
            int opPrecedence = getPrecedence(op);
            
            if (opPrecedence < precedence) {
                break;
            }
            
            // Special case: boolean chain (x == all[...])
            if (isComparisonOp(op) && isChainFollows(1)) {
                consume();
                return parseEqualityChain(left, op.text);
            }
            
            // Regular binary operator
            if (opPrecedence > 0) {
                consume();
                ExprNode right = parsePrecedence(opPrecedence + 1);
                left = ASTFactory.createBinaryOp(left, op.text, right);
                continue;
            }
            
            // Index access
            if (op.symbol == LBRACKET) {
                left = parseIndexAccessContinuation(left);
                continue;
            }
            
            break;
        }
        
        return left;
    }

    private ExprNode parsePrefix() {
        // Handle unary operators first
        if (match(BANG) || match(PLUS) || match(MINUS)) {
            Token op = consume();
            ExprNode operand = parsePrecedence(PREC_UNARY);
            return ASTFactory.createUnaryOp(op.text, operand);
        }
        
        // Then parse primary expression
        return parsePrimaryExpression();
    }

    // === BOOLEAN CHAINS (KEPT FROM ORIGINAL) ===

    private ExprNode parseBooleanChain() {
        Token typeToken = consume();
        boolean isAll = typeToken.text.equals(ALL.toString());

        if (match(ID)) {
            String arrayName = consume().text;

            if (isComparisonOp(currentToken())) {
                Token op = consume();
                ExprNode right = parsePrecedence(PREC_COMPARISON + 1);                

                List<ExprNode> chainArgs = new ArrayList<ExprNode>();
                chainArgs.add(right);

                ExprNode arrayExpr = ASTFactory.createIdentifier(arrayName);
                EqualityChainNode chain = ASTFactory.createEqualityChain(arrayExpr, op.text, isAll, chainArgs);
                return chain;
            } else {
                throw new ParseError("Expected comparison operator after 'all/any <arrayName>' but found " +
                        getTypeName(currentToken().type) + " ('" + currentToken().text +
                        "') at line " + currentToken().line + ":" + currentToken().column);
            }
        } else if (match(LBRACKET)) {
            consume(LBRACKET);
            List<ExprNode> expressions = new ArrayList<ExprNode>();

            if (!match(RBRACKET)) {
                expressions.add(parseExpression());              
                if (!match(COMMA) && !match(RBRACKET)) {
                    throw new ParseError("Boolean chain requires at least two expressions or a comma after the first expression.");
                }                 
                while (tryConsume(COMMA)) {
                    expressions.add(parseExpression());
                }
            }
            consume(RBRACKET);
            
            BooleanChainNode node = ASTFactory.createBooleanChain(isAll, expressions);
            return node;
        } else {
            throw new ParseError("Expected array variable or '[' after 'all/any' but found " +
                    getTypeName(currentToken().type) + " ('" + currentToken().text +
                    "') at line " + currentToken().line + ":" + currentToken().column);
        }
    }

    private ExprNode parseEqualityChain(ExprNode left, String operator) {
        Token chainTypeToken = consume();
        boolean isAllChain = chainTypeToken.text.equals(ALL.toString());
        
        List<ExprNode> chainArgs = new ArrayList<ExprNode>();
        
        if (match(LBRACKET)) {
            consume(LBRACKET);
            if (!match(RBRACKET)) {
                chainArgs.add(parseChainArgument());
                while (tryConsume(COMMA)) {
                    chainArgs.add(parseChainArgument());
                }
            }
            consume(RBRACKET);
        } else if (match(ID)) {
            String arrayName = consume(ID).text;
            ExprNode arrayExpr = ASTFactory.createIdentifier(arrayName);
            chainArgs.add(arrayExpr);
        } else {
            throw new ParseError("Expected array variable or '[' for array literal after 'all/any' but found " +
                    getTypeName(currentToken().type) + " ('" + currentToken().text +
                    "') at line " + currentToken().line + ":" + currentToken().column);
        }
        
        EqualityChainNode chain = ASTFactory.createEqualityChain(left, operator, isAllChain, chainArgs);
        return chain;
    }

    private ExprNode parseChainArgument() {
        if (match(BANG)) {
            consume(BANG);
            ExprNode arg = parsePrimaryExpression();
            return ASTFactory.createUnaryOp("!", arg);
        }
        
        if (match(LPAREN)) {
            return parseArgumentList();
        }
        
        if (match(ID) && isSymbolAt(1, RBRACKET)) {
            Token idToken = currentToken();
            throw new ParseError("Redundant brackets around array variable '" + idToken.text + 
                               "'. Use 'any " + idToken.text + "' instead of 'any[" + idToken.text + "]'",
                               idToken.line, idToken.column);
        }
        
        return parsePrimaryExpression();
    }

    private ExprNode parseArgumentList() {
        consume(LPAREN);
        List<ExprNode> arguments = new ArrayList<ExprNode>();
        if (!match(RPAREN)) {
            arguments.add(parseExpression());
            while (tryConsume(COMMA)) {
                arguments.add(parseExpression());
            }
        }
        consume(RPAREN);
        return ASTFactory.createArgumentList(arguments);
    }

    private MethodCallNode parseConditionalChainCall(MethodCallNode call) {
        Token chainTypeToken = consume();
        boolean isAllChain = chainTypeToken.text.equals(ALL.toString());
        call.chainType = isAllChain ? ALL.toString() : ANY.toString();
        
        consume(LBRACKET);
        
        List<ExprNode> chainArgs = new ArrayList<ExprNode>();
        if (!match(RBRACKET)) {
            chainArgs.add(parseChainArgument());
            while (tryConsume(COMMA)) {
                chainArgs.add(parseChainArgument());
            }
        }
        
        consume(RBRACKET);
        
        call.chainArguments = chainArgs;
        return call;
    }

    private ExprNode parseArrayLiteral() {
    consume(LBRACKET);
    List<ExprNode> elements = new ArrayList<ExprNode>();
    
    if (!match(RBRACKET)) {
        // Check if this is a range
        if (isRangeStart()) {
            elements.add(parseRangeExpression());
        } else {
            elements.add(parseExpression());
        }
        
        while (tryConsume(COMMA)) {
            // Check if next is a range
            if (isRangeStart()) {
                elements.add(parseRangeExpression());
            } else {
                elements.add(parseExpression());
            }
        }
    }
    
    consume(RBRACKET);
    return ASTFactory.createArray(elements);
}

// Add this helper method:
private boolean isRangeStart() {
    Token current = currentToken();
    
    // Case 1: "by" step "in" start "to" end
    if (current.type == KEYWORD && current.text.equals("by")) {
        return true;
    }
    
    // Case 2: start "to" end (no 'by')
    // Try to parse an expression, then check for "to"
    int savedPos = getPosition();
    try {
        parseExpression(); // Try to parse start
        return currentToken().type == KEYWORD && 
               currentToken().text.equals("to");
    } catch (ParseError e) {
        return false;
    } finally {
        position.set(savedPos); // Reset position
    }
}

// Add this method (similar to StatementParser.parseForStatement range parsing):
private RangeNode parseRangeExpression() {
    ExprNode step = null;
    
    // Check for "by step"
    if (isKeyword(BY)) {
        consumeKeyword(BY);
        step = parseExpression();
        consumeKeyword(IN);
    }
    
    // Parse "start to end"
    ExprNode start = parseExpression();
    consumeKeyword(TO);
    ExprNode end = parseExpression();
    
    return ASTFactory.createRange(step, start, end);
}

    private ExprNode parseTypeCast() {
        consume(LPAREN);
        String type = parseTypeReference();    
        consume(RPAREN);
        ExprNode expressionToCast = parsePrecedence(PREC_UNARY);
        return ASTFactory.createTypeCast(type, expressionToCast);
    }
    
// NEW Helper method to resolve numeric shorthands or standard scientific notation into BigDecimal.
private Object resolveFloatLiteralValue(String literal) {
    String baseValueStr;
    String suffix;
    int exponent = 0;
    
    // --- 1. Check for Custom Shorthands (K, M, Qi, etc.) ---
    if (literal.endsWith("Qi")) {
        suffix = "Qi";
        baseValueStr = literal.substring(0, literal.length() - 2);
        exponent = 18;
    } else {
        char lastChar = literal.charAt(literal.length() - 1);
        
        if (lastChar == 'K' || lastChar == 'M' || lastChar == 'B' || lastChar == 'T' || lastChar == 'Q') {
            suffix = String.valueOf(lastChar);
            baseValueStr = literal.substring(0, literal.length() - 1);
            
            switch (suffix) {
                case "K": exponent = 3; break;
                case "M": exponent = 6; break;
                case "B": exponent = 9; break;
                case "T": exponent = 12; break;
                case "Q": exponent = 15; break;
                default: break; 
            }
        } else {
            // 2. Standard Scientific Notation ('e' or 'E')
            if (literal.contains("e") || literal.contains("E")) {
                 try {
                     // BigDecimal's constructor handles this standard format directly
                     return new BigDecimal(literal);
                 } catch (NumberFormatException e) {
                     // Fall through to general error
                 }
            }
            
            // If no shorthand and no 'e', return null (it's a plain float)
            return null;
        }
    }

    // --- 3. Calculation for Custom Shorthands (if reached) ---
    if (exponent > 0) {
        try {
            BigDecimal base = new BigDecimal(baseValueStr);
            BigDecimal multiplier = BigDecimal.TEN.pow(exponent);
            return base.multiply(multiplier);
        } catch (NumberFormatException e) {
            // Fall through to general error
        }
    }
    
    throw new ParseError("Invalid numeric literal format: " + literal, 
        currentToken().line, currentToken().column);
}

    // === ORIGINAL LOOKAHEAD HELPERS (KEPT FOR COMPATIBILITY) ===

    private boolean isMethodCallFollows() {
        Token first = lookahead(0);
        if (first == null || first.type != ID) return false;
        
        int pos = 1;
        while (lookahead(pos) != null && lookahead(pos).symbol == DOT) {
            pos++;
            if (lookahead(pos) == null || lookahead(pos).type != ID) return false;
            pos++;
        }
        
        Token afterDots = lookahead(pos);
        return afterDots != null && afterDots.symbol == LPAREN;
    }

    private boolean isSlotAccessExpression() {
        if (!isSymbolAt(0, LBRACKET)) return false;

        int pos = getPosition() + 1;
        int depth = 1;

        while (pos < tokens.size() && depth > 0) {
            Token t = tokens.get(pos);
            if (t.symbol == LBRACKET) depth++;
            else if (t.symbol == RBRACKET) depth--;
            pos++;
        }

        if (depth == 0 && pos < tokens.size()) {
             return tokens.get(pos).symbol == COLON;
        }
        return false;
    }

    private boolean isTypeCast() {
        if (!isSymbolAt(0, LPAREN)) return false;
        Token second = lookahead(1);
        if (second == null || !isTypeStart(second)) return false;
        
        int pos = getPosition();
        int parenDepth = 0;
        
        for (int i = 0; i < 20 && pos < tokens.size(); i++) {
            Token t = tokens.get(pos);
            if (t.symbol == LPAREN) parenDepth++;
            else if (t.symbol == RPAREN) {
                parenDepth--;
                if (parenDepth == 0) {
                    if (pos + 1 < tokens.size()) {
                        Token afterParen = tokens.get(pos + 1);
                        return isExpressionStart(afterParen);
                    }
                    return false;
                }
            }
            pos++;
        }
        return false;
    }

    // === UTILITY METHODS ===

    private int getPrecedence(Token token) {
        if (token == null) return 0;
        
        if (token.type == SYMBOL) {
            switch (token.symbol) {
                case EQ: case NEQ: return PREC_EQUALITY;
                case LT: case GT: case LTE: case GTE: return PREC_COMPARISON;
                case PLUS: case MINUS: return PREC_TERM;
                case MUL: case DIV: case MOD: return PREC_FACTOR;
                case LPAREN: case LBRACKET: return PREC_CALL;
                default: return 0;
            }
        }
        return 0;
    }

    private boolean isChainFollows(int offset) {
        Token next = peek(offset);
        if (next == null) return false;
        
        if (next.type == KEYWORD && 
            (next.text.equals(ALL.toString()) || next.text.equals(ANY.toString()))) {
            Token after = peek(offset + 1);
            return after != null && (after.symbol == LBRACKET || after.type == ID);
        }
        return false;
    }

    private boolean isComparisonOp(Token t) {
        return t != null && t.symbol != null &&
               (t.symbol == EQ || t.symbol == NEQ || 
                t.symbol == GT || t.symbol == LT || 
                t.symbol == GTE || t.symbol == LTE);
    }
}