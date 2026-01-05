package cod.parser;

import cod.ast.ASTFactory;
import cod.error.ParseError;
import cod.ast.nodes.*;

import cod.lexer.Token;
import static cod.lexer.TokenType.*;

import static cod.syntax.Symbol.*;
import static cod.syntax.Keyword.*;

import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

public class ExpressionParser extends BaseParser {
    
    private static final int PREC_ASSIGNMENT = 10;
    private static final int PREC_EQUALITY = 50;
    private static final int PREC_COMPARISON = 60;
    private static final int PREC_TERM = 70;
    private static final int PREC_FACTOR = 80;
    private static final int PREC_UNARY = 90;
    private static final int PREC_CALL = 100;
    private static final int PREC_IS = 40; // NEW: lower precedence than equality
    
    public ExpressionParser(List<Token> tokens, PositionHolder position) {
        super(tokens, position);
    }

    public ExprNode parseExpression() {
    // Check if this is all/any operator OR method call
    if (isKeyword(ALL) || isKeyword(ANY)) {
        // Look ahead to see if it's followed by '(' (method call) or something else (operator)
        Token nextToken = lookahead(1);
        
        if (nextToken != null && nextToken.symbol == LPAREN) {
            // It's a method call: all(...) or any(...)
            return parseMethodCall();
        } else {
            // It's the operator: all arr > 5 or all [...]
            return parseBooleanChain();
        }
    }
    
    return parsePrecedence(PREC_ASSIGNMENT);
}

    private ExprNode parseConstructorCall() {
        Token classNameToken = currentToken();
        String className = consume(ID).text;
        
        consume(LPAREN);
        List<ExprNode> args = new ArrayList<ExprNode>();
        List<String> argNames = new ArrayList<String>();
        
        if (!match(RPAREN)) {
            if (isNamedArgument()) {
                parseNamedArgumentList(args, argNames);
            } else {
                args.add(parseExpression());
                argNames.add(null);
                while (tryConsume(COMMA)) {
                    args.add(parseExpression());
                    argNames.add(null);
                }
            }
        }
        consume(RPAREN);
        
        ConstructorCallNode call = ASTFactory.createConstructorCall(className, args);
        call.argNames = argNames;
        return call;
    }

    private boolean isNamedArgument() {
        int savedPos = getPosition();
        try {
            skipWhitespaceAndComments();
            Token first = currentToken();
            if (first == null || first.type != ID) return false;
            
            Token second = lookahead(1);
            return second != null && second.symbol == COLON;
        } finally {
            position.set(savedPos);
        }
    }

    private void parseNamedArgumentList(List<ExprNode> args, List<String> argNames) {
        do {
            String argName = consume(ID).text;
            consume(COLON);
            ExprNode value = parseExpression();
            
            args.add(value);
            argNames.add(argName);
            
            if (!match(COMMA)) {
                break;
            }
            consume(COMMA);
        } while (!match(RPAREN));
    }

    private void skipWhitespaceAndComments() {
        while (position.get() < tokens.size()) {
            Token t = currentToken();
            if (t.type == WS || t.type == LINE_COMMENT || t.type == BLOCK_COMMENT) {
                consume();
            } else {
                break;
            }
        }
    }

    public MethodCallNode parseMethodCall() {
    String qualifiedNameStr = parseQualifiedNameOrKeyword();  // Changed from parseQualifiedName()
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
        if (isNamedArgument()) {
            parseNamedArgumentList(call.arguments, call.argNames);
        } else {
            call.arguments.add(parseExpression());
            call.argNames.add(null);
            while (tryConsume(COMMA)) {
                call.arguments.add(parseExpression());
                call.argNames.add(null);
            }
        }
    }
    consume(RPAREN);
    return call;
}

private String parseQualifiedNameOrKeyword() {
    Token token = currentToken();
    
    // Check if it's a keyword that can be used as a method name
    if (token.type == KEYWORD && canKeywordBeMethodName(token.text)) {
        String name = consume().text;
        
        // Check for dot notation after keyword (e.g., "in.someMethod")
        if (tryConsume(DOT)) {
            StringBuilder fullName = new StringBuilder(name);
            fullName.append(".");
            // Next part must be ID (can't have keyword.keyword)
            fullName.append(consume(ID).text);
            
            // Continue for more dots
            while (tryConsume(DOT)) {
                fullName.append(".");
                fullName.append(consume(ID).text);
            }
            return fullName.toString();
        }
        
        return name;
    }
    
    // Otherwise, use the existing qualified name parsing (updated to handle keywords after dots)
    return parseQualifiedName();
}

private boolean isRangeIndex() {
    int savedPos = getPosition();
    try {
        // Skip whitespace/comments
        while (position.get() < tokens.size()) {
            Token t = currentToken();
            if (t.type == WS || t.type == LINE_COMMENT || t.type == BLOCK_COMMENT) {
                consume();
            } else {
                break;
            }
        }
        
        // Pattern 1: "by ..."
        if (isKeyword(BY)) return true;
        
        // Pattern 2: Look for expression followed by "to" or comma
        // Try to parse an expression
        try {
            parseExpression();
            
            // Check what follows
            Token after = currentToken();
            if (after != null) {
                // Check for "to" keyword or comma for multiple ranges
                if (isKeyword(TO) || after.symbol == COMMA) {
                    return true;
                }
            }
        } catch (ParseError e) {
            // Not a valid expression
            return false;
        }
        
        return false;
    } finally {
        position.set(savedPos);
    }
}

public IndexAccessNode parseIndexAccessContinuation(ExprNode arrayExpr) {
    consume(LBRACKET);
    
    // Check if this is a range index
    ExprNode indexExpr;
    
    if (isRangeIndex()) {
        indexExpr = parseRangeIndex();
    } else {
        // Regular single index access
        indexExpr = parseExpression();
    }
    
    consume(RBRACKET);
    
    IndexAccessNode indexAccess = ASTFactory.createIndexAccess(arrayExpr, indexExpr);
    return indexAccess;
}

private ExprNode parseRangeIndex() {
    List<RangeIndexNode> ranges = new ArrayList<RangeIndexNode>();
    
    // Parse first range
    ranges.add(parseSingleRangeIndex());
    
    // Parse additional comma-separated ranges
    while (tryConsume(COMMA)) {
        ranges.add(parseSingleRangeIndex());
    }
    
    if (ranges.size() == 1) {
        return ranges.get(0);
    } else {
        return ASTFactory.createMultiRangeIndex(ranges);
    }
}

private RangeIndexNode parseSingleRangeIndex() {
    ExprNode step = null;
    ExprNode start;
    ExprNode end;
    
    // Pattern 1: "by step in start to end"
    if (isKeyword(BY)) {
        consumeKeyword(BY);
        step = parseExpression();
        consumeKeyword(IN);
        start = parseExpression();
        consumeKeyword(TO);
        end = parseExpression();
    }
    // Pattern 2: "start to end"
    else {
        start = parseExpression();
        consumeKeyword(TO);
        end = parseExpression();
    }
    
    return ASTFactory.createRangeIndex(step, start, end);
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
                throw new ParseError("Expected slot name or index, found " + currentToken().text, currentToken().line, currentToken().column);
            }
        } while (tryConsume(COMMA));
        consume(RBRACKET);
        return slots;
    }
    
    private ExprNode parseIfExpression() {
        consumeKeyword(IF);
        ExprNode condition = parseExpression();
        
        ExprNode thenExpr;
        if (match(LBRACE)) {
            consume(LBRACE);
            thenExpr = parseExpression();
            consume(RBRACE);
        } else {
            thenExpr = parseExpression();
        }
        
        consumeKeyword(ELSE);
        
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

public ExprNode parsePrimaryExpression() {
    ExprNode baseExpr;
    Token startToken = currentToken();
    
    if (isConstructorCall() && !isMethodCallFollows()) {
        return parseConstructorCall();
    }

    if (match(LBRACKET)) {
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
            int intValue = Integer.parseInt(intText);
            baseExpr = ASTFactory.createIntLiteral(intValue);
        } catch (NumberFormatException e1) {
            try {
                long longValue = Long.parseLong(intText);
                baseExpr = ASTFactory.createLongLiteral(longValue);
            } catch (NumberFormatException e2) {
                BigDecimal bigDecimalValue = new BigDecimal(intText);
                baseExpr = ASTFactory.createFloatLiteral(bigDecimalValue);
            }
        }
    } else if (match(FLOAT_LIT)) {
        Token floatToken = consume();
        String floatText = floatToken.text;
        
        Object resolvedValue = resolveFloatLiteralValue(floatText);
        
        if (resolvedValue instanceof BigDecimal) {
            baseExpr = ASTFactory.createFloatLiteral((BigDecimal)resolvedValue);
        } else {
            // If resolveFloatLiteralValue returned null (shouldn't happen for valid numbers)
            // or something else, try to parse as plain BigDecimal
            try {
                BigDecimal bigDecimalValue = new BigDecimal(floatText);
                baseExpr = ASTFactory.createFloatLiteral(bigDecimalValue);
            } catch (NumberFormatException e) {
                throw new ParseError("Invalid numeric literal: " + floatText, 
                    floatToken.line, floatToken.column);
            }
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
    } else if (isKeyword(INT) || isKeyword(TEXT) || isKeyword(FLOAT) || isKeyword(BOOL) || isKeyword(TYPE)) {
        // TYPE LITERALS: int, text, float, bool, type as string values
        String typeName = consume().text;
        baseExpr = ASTFactory.createStringLiteral(typeName);
    } else if (match(ID) || (currentToken().type == KEYWORD && canKeywordBeMethodName(currentToken().text))) {
        if (isMethodCallFollows()) {
            baseExpr = parseMethodCall();
        } else {
            String idName = consume().text;
            baseExpr = ASTFactory.createIdentifier(idName);
        }
    } else if (match(LPAREN)) {
        if (isTypeCast()) {
            baseExpr = parseTypeCast();
        } else {
            consume(LPAREN);
            ExprNode firstExpr = parseExpression();
            
            if (match(COMMA)) {
                List<ExprNode> elements = new ArrayList<ExprNode>();
                elements.add(firstExpr);
                
                while (tryConsume(COMMA)) {
                    elements.add(parseExpression());
                }
                
                if (elements.size() == 1 && !match(RPAREN)) {
                    throw new ParseError("Expected expression after comma in tuple but found " + 
                        currentToken().text, currentToken().line, currentToken().column);
                }
                
                consume(RPAREN);
                baseExpr = ASTFactory.createTuple(elements);
            } else {
                consume(RPAREN);
                baseExpr = firstExpr;
            }
        }
    } else {
        throw new ParseError("Unexpected token in primary expression: " + startToken.text +
            " (" + getTypeName(startToken.type) + ")", startToken.line, startToken.column);
    }

    while (match(LBRACKET)) {
        baseExpr = parseIndexAccessContinuation(baseExpr);
    }

    return baseExpr;
}

private Object resolveFloatLiteralValue(String literal) {
    
    String baseValueStr;
    String suffix;
    int exponent = 0;
    
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
            // Try to parse as plain number or scientific notation
            try {
                return new BigDecimal(literal);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    try {
        BigDecimal base = new BigDecimal(baseValueStr);
        BigDecimal multiplier = BigDecimal.TEN.pow(exponent);
        return base.multiply(multiplier);
    } catch (Exception e) {
        // If anything goes wrong (NumberFormatException, ArithmeticException, etc.)
        return null;
    }
}

    private ExprNode parsePrecedence(int precedence) {
        ExprNode left = parsePrefix();
        
        while (true) {
            Token op = currentToken();
            int opPrecedence = getPrecedence(op);
            
            if (opPrecedence < precedence) {
                break;
            }
            
            if (isComparisonOp(op) && isChainFollows(1)) {
                consume();
                return parseEqualityChain(left, op.text);
            }
            
            if (opPrecedence > 0) {
                consume();
                ExprNode right = parsePrecedence(opPrecedence + 1);
                // NEW: Handle 'is' as a special binary operator
                if (op.type == KEYWORD && IS.toString().equals(op.text)) {
                    left = ASTFactory.createBinaryOp(left, "is", right);
                } else {
                    left = ASTFactory.createBinaryOp(left, op.text, right);
                }
                continue;
            }
            
            if (op.symbol == LBRACKET) {
                left = parseIndexAccessContinuation(left);
                continue;
            }
            
            break;
        }
        
        return left;
    }

    private ExprNode parsePrefix() {
        if (match(BANG) || match(PLUS) || match(MINUS)) {
            Token op = consume();
            ExprNode operand = parsePrecedence(PREC_UNARY);
            return ASTFactory.createUnaryOp(op.text, operand);
        }
        
        return parsePrimaryExpression();
    }

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
                        "') ", currentToken().line, currentToken().column);
            }
        } else if (match(LBRACKET)) {
            consume(LBRACKET);
            List<ExprNode> expressions = new ArrayList<ExprNode>();

            if (!match(RBRACKET)) {
                expressions.add(parseExpression());              
                if (!match(COMMA) && !match(RBRACKET)) {
                    throw new ParseError("Boolean chain requires at least two expressions or a comma after the first expression.", currentToken().line, currentToken().column);
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
                    "') ", currentToken().line, currentToken().column);
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
                    "') ", currentToken().line, currentToken().column);
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
            if (isRangeStart()) {
                elements.add(parseRangeExpression());
            } else {
                elements.add(parseExpression());
            }
            
            while (tryConsume(COMMA)) {
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

    private boolean isRangeStart() {
        Token current = currentToken();
        
        if (current.type == KEYWORD && current.text.equals("by")) {
            return true;
        }
        
        int savedPos = getPosition();
        try {
            parseExpression();
            return currentToken().type == KEYWORD && 
                   currentToken().text.equals("to");
        } catch (ParseError e) {
            return false;
        } finally {
            position.set(savedPos);
        }
    }

    private RangeNode parseRangeExpression() {
        ExprNode step = null;
        
        if (isKeyword(BY)) {
            consumeKeyword(BY);
            step = parseExpression();
            consumeKeyword(IN);
        }
        
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

    private boolean isConstructorCall() {
    int savedPos = getPosition();
    try {
        Token first = currentToken();
        if (first == null || first.type != ID) return false;
        
        String idName = first.text;
        
        // CONVENTION: Type names start with uppercase
        if (idName.length() == 0 || Character.isLowerCase(idName.charAt(0))) {
            return false; // lowercase = method/variable
        }
        
        int pos = 1;
        while (peek(pos) != null && 
               (peek(pos).type == WS || 
                peek(pos).type == LINE_COMMENT || 
                peek(pos).type == BLOCK_COMMENT)) {
            pos++;
        }
        
        return peek(pos) != null && peek(pos).symbol == LPAREN;
    } finally {
        position.set(savedPos);
    }
}

    private boolean isMethodCallFollows() {
    int savedPos = getPosition();
    try {
        Token first = lookahead(0);
        
        if (first == null) return false;
        
        boolean isValidName = (first.type == ID) || 
                             (first.type == KEYWORD && canKeywordBeMethodName(first.text));
        
        if (!isValidName) return false;
        
        if (isConstructorCall()) {
            ;
            return false;
        }
        
        int pos = 1;
        while (lookahead(pos) != null && lookahead(pos).symbol == DOT) {
            
            pos++;
            Token afterDot = lookahead(pos);
            
            // FIX: Check if afterDot is ID OR keyword that can be method name
            if (afterDot == null || 
                !(afterDot.type == ID || 
                  (afterDot.type == KEYWORD && canKeywordBeMethodName(afterDot.text)))) {
                return false;
            }
            pos++;
        }
        
        Token afterDots = lookahead(pos);
        
        boolean result = afterDots != null && afterDots.symbol == LPAREN;
        return result;
    } finally {
        position.set(savedPos);
    }
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
        // 1. Must start with (
        if (!isSymbolAt(0, LPAREN)) return false;
        
        // 2. Second token must be a valid start of a type (ID or primitive keyword)
        Token second = lookahead(1);
        if (second == null || !isTypeStart(second)) return false;
        
        int pos = getPosition();
        int parenDepth = 0;
        
        // Limit lookahead to avoid performance issues
        for (int i = 0; i < 50 && pos < tokens.size(); i++) {
            Token t = tokens.get(pos);
            
            // NEW CHECK: If we find a token inside the parens that CANNOT be part of a type
            // (like -, +, *, /, etc.), then this is definitely NOT a cast.
            // We check parenDepth > 0 to ensure we are inside the (...)
            if (parenDepth > 0 && isIllegalTypeToken(t)) {
                return false;
            }

            if (t.symbol == LPAREN) parenDepth++;
            else if (t.symbol == RPAREN) {
                parenDepth--;
                if (parenDepth == 0) {
                    // We found the matching closing paren.
                    // Check if the token immediately AFTER matches the start of an expression
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

    /**
     * Helper to detect tokens that define Math/Logic but are invalid in Type definitions.
     * If these appear inside (...), it's a Grouped Expression, not a Cast.
     */
    private boolean isIllegalTypeToken(Token t) {
        // Allow: ID, Keywords, LBRACKET/RBRACKET (arrays), COMMA/PIPE (unions/tuples), DOT (qualified)
        // Disallow: Math operators, logic operators, literals
        
        if (t.type == INT_LIT || t.type == FLOAT_LIT || t.type == STRING_LIT || t.type == BOOL_LIT) return true;
        
        if (t.symbol == PLUS || t.symbol == MINUS || t.symbol == MUL || 
            t.symbol == DIV || t.symbol == MOD || t.symbol == EQ || 
            t.symbol == NEQ || t.symbol == GT || t.symbol == LT || 
            t.symbol == GTE || t.symbol == LTE) {
            return true;
        }
        
        return false;
    }

    private int getPrecedence(Token token) {
        if (token == null) return 0;
        
        // NEW: 'is' keyword has lower precedence than equality
        if (token.type == KEYWORD && IS.toString().equals(token.text)) {
            return PREC_IS;
        }
        
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