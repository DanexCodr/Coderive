package cod.parser;

import cod.ast.ASTFactory;
import cod.error.ParseError;
import cod.ast.nodes.*;

import cod.lexer.MainLexer;
import cod.lexer.Token;
import static cod.lexer.TokenType.*;
import cod.parser.context.*;

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
    private static final int PREC_IS = 40;
    
    public ExpressionParser(ParserContext ctx) {
        super(ctx);
    }
    
    @Override
    protected BaseParser createIsolatedParser(ParserContext isolatedCtx) {
        return new ExpressionParser(isolatedCtx);
    }

    public ExprNode parseExpression() {
        return attempt(new ParserAction<ExprNode>() {
            @Override
            public ExprNode parse() throws ParseError {
                if (is(ALL, ANY)) {
                    Token nextToken = lookahead(1);
                    if (nextToken != null && is(nextToken, LPAREN)) {
                        return parseMethodCall();
                    } else {
                        return parseBooleanChain();
                    }
                }
                return parsePrecedence(PREC_ASSIGNMENT);
            }
        });
    }

    private ExprNode parseConstructorCall() {
        Token classNameToken = currentToken();
        String className = expect(ID).text;

        List<ExprNode> args = new ArrayList<ExprNode>();
        List<String> argNames = new ArrayList<String>();
        
        if (!is(RPAREN)) {
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
        expect(RPAREN);
        
        ConstructorCallNode call = ASTFactory.createConstructorCall(className, args, classNameToken);
        call.argNames = argNames;
        return call;
    }

    private boolean isNamedArgument() {
        save();
        try {
            skipWhitespaceAndComments();
            Token first = currentToken();
            if (first == null || first.type != ID) return false;
            
            Token second = lookahead(1);
            return second != null && is(second, COLON);
        } finally {
            restore();
        }
    }

    private void parseNamedArgumentList(List<ExprNode> args, List<String> argNames) {
        do {
            String argName = expect(ID).text;
            
            expect(COLON);
            ExprNode value = parseExpression();
            
            args.add(value);
            argNames.add(argName);
            
            if (!is(COMMA)) {
                break;
            }
            expect(COMMA);
        } while (!is(RPAREN));
    }

    protected void skipWhitespaceAndComments() {
        super.skipWhitespaceAndComments();
    }

    public MethodCallNode parseMethodCall() {
        return attempt(new ParserAction<MethodCallNode>() {
            @Override
            public MethodCallNode parse() throws ParseError {
                Token nameStartToken = currentToken();
                String qualifiedNameStr = parseQualifiedNameOrKeyword();
                String methodName = qualifiedNameStr;
                if (qualifiedNameStr.contains(".")) {
                    methodName = qualifiedNameStr.substring(qualifiedNameStr.lastIndexOf('.') + 1);
                }
                MethodCallNode call = ASTFactory.createMethodCall(methodName, qualifiedNameStr, nameStartToken);
                
                expect(LPAREN);
                
                if (!is(RPAREN)) {
                    if (isNamedArgument()) {
                        parseNamedArgumentList(call.arguments, call.argNames);
                    } else {
                        // Boolean chains will now be parsed as normal expressions
                        call.arguments.add(parseExpression());
                        call.argNames.add(null);
                        while (tryConsume(COMMA)) {
                            call.arguments.add(parseExpression());
                            call.argNames.add(null);
                        }
                    }
                }
                expect(RPAREN);
                return call;
            }
        });
    }

    private MethodCallNode parseSuperMethodCall() {
        Token superToken = currentToken();
        expect(SUPER);
        
        expect(DOT);
        
        Token methodToken = currentToken();
        String methodName;
        
        if (is(methodToken, ID)) {
            methodName = expect(ID).text;
        } else if (is(methodToken, KEYWORD) && canKeywordBeMethodName(methodToken)) {
            methodName = expect(KEYWORD).text;
        } else {
            throw error("Expected method name after 'super.'", methodToken);
        }
        
        MethodCallNode call = ASTFactory.createMethodCall(methodName, "super." + methodName, superToken);
        call.isSuperCall = true;
        
        expect(LPAREN);
        
        if (!is(RPAREN)) {
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
        expect(RPAREN);
        
        return call;
    }

    private boolean isSuperMethodCall() {
        return attempt(new ParserAction<Boolean>() {
            @Override
            public Boolean parse() throws ParseError {
                while (true) {
                    if (is(currentToken(), WS, LINE_COMMENT, BLOCK_COMMENT)) {
                        consume();
                    } else {
                        break;
                    }
                }
                
                if (!is(SUPER)) return false;
                expect(SUPER);
                
                while (true) {
                    if (is(currentToken(), WS, LINE_COMMENT, BLOCK_COMMENT)) {
                        consume();
                    } else {
                        break;
                    }
                }
                
                if (!is(DOT)) return false;
                expect(DOT);
                
                while (true) {
                    if (is(currentToken(), WS,  LINE_COMMENT, BLOCK_COMMENT)) {
                        consume();
                    } else {
                        break;
                    }
                }
                
                Token nameToken = currentToken();
                if (nameToken == null) return false;
                
                boolean isValidName = (is(nameToken, ID)) || 
                                    (is(nameToken, KEYWORD) && canKeywordBeMethodName(nameToken));
                if (!isValidName) return false;
                
                consume();
                
                while (true) {
                    if (is(currentToken(), WS, LINE_COMMENT, BLOCK_COMMENT)) {
                        consume();
                    } else {
                        break;
                    }
                }
                
                return is(LPAREN);
            }
        });
    }

    private String parseQualifiedNameOrKeyword() {
        Token token = currentToken();
        
        if (is(token, KEYWORD) && canKeywordBeMethodName(token)) {
            String name = expect(KEYWORD).text;
            
            if (tryConsume(DOT)) {
                StringBuilder fullName = new StringBuilder(name);
                fullName.append(".");
                fullName.append(expect(ID).text);
                
                while (tryConsume(DOT)) {
                    fullName.append(".");
                    fullName.append(expect(ID).text);
                }
                return fullName.toString();
            }
            
            return name;
        }
        
        return parseQualifiedName();
    }

    private boolean isRangeIndex() {
        // Check token pattern without consuming
        int pos = getPosition();
        
        // Skip whitespace
        while (pos < tokens.size()) {
            if (is(tokens.get(pos), WS,  LINE_COMMENT, BLOCK_COMMENT)) {
                pos++;
            } else {
                break;
            }
        }
        
        if (pos >= tokens.size()) return false;
        
        // Check for BY
        if (is(tokens.get(pos), KEYWORD) && is(tokens.get(pos), BY)) {
            return true;
        }
        
        // Skip past what could be an expression
        // This is simplified - just skip some tokens
        int depth = 0;
        while (pos < tokens.size()) {
            Token t = tokens.get(pos);
            
            if (is(t, LPAREN, LBRACKET)) {
                depth++;
            } else if (is(t, RPAREN, RBRACKET)) {
                if (depth > 0) depth--;
                else break; // Found closing bracket without opening
            } else if (depth == 0) {
                // At top level, check for TO or COMMA
                if (is(t, KEYWORD) && is(t, TO)) {
                    return true;
                } else if (is(t, COMMA)) {
                    return true;
                } else if (is(t, RBRACKET)) {
                    // Reached the closing bracket without finding TO or COMMA
                    break;
                }
            }
            pos++;
        }
        
        return false;
    }

    public IndexAccessNode parseIndexAccessContinuation(ExprNode arrayExpr) {
        Token lbracketToken = expect(LBRACKET);
        
        ExprNode indexExpr;
        
        if (isRangeIndex()) {
            indexExpr = parseRangeIndex();
        } else {
            indexExpr = parseExpression();
        }
        
        expect(RBRACKET);
        
        IndexAccessNode indexAccess = ASTFactory.createIndexAccess(arrayExpr, indexExpr, lbracketToken);
        return indexAccess;
    }

    private ExprNode parseRangeIndex() {
        List<RangeIndexNode> ranges = new ArrayList<RangeIndexNode>();
        
        Token firstLbracketToken = currentToken();
        ranges.add(parseSingleRangeIndex());
        
        while (tryConsume(COMMA)) {
            ranges.add(parseSingleRangeIndex());
        }
        
        if (ranges.size() == 1) {
            return ranges.get(0);
        } else {
            return ASTFactory.createMultiRangeIndex(ranges, firstLbracketToken);
        }
    }

    private RangeIndexNode parseSingleRangeIndex() {
        ExprNode step = null;
        ExprNode start;
        ExprNode end;
        Token byToken = null;
        Token toToken = null;
        
        if (is(BY)) {
            byToken = expect(BY);
            step = parseExpression();
            expect(IN);
            start = parseExpression();
            toToken = expect(TO);
            end = parseExpression();
        } else {
            start = parseExpression();
            toToken = expect(TO);
            end = parseExpression();
        }
        
        return ASTFactory.createRangeIndex(step, start, end, byToken, toToken);
    }

    public List<String> parseReturnSlots() {
        expect(LBRACKET);
        List<String> slots = new ArrayList<String>();
        do {
            if (is(ID)) {
                slots.add(expect(ID).text);
            } else if (is(INT_LIT)) {
                slots.add(expect(INT_LIT).text);
            } else {
                throw error("Expected slot name or index", currentToken());
            }
        } while (tryConsume(COMMA));
        expect(RBRACKET);
        return slots;
    }
    
    private ExprNode parseIfExpression() {
        Token ifToken = expect(IF);
        ExprNode condition = parseExpression();
        
        ExprNode thenExpr;
        if (is(LBRACE)) {
            expect(LBRACE);
            thenExpr = parseExpression();
            expect(RBRACE);
        } else {
            thenExpr = parseExpression();
        }
        
        Token elseToken = expect(ELSE);
        
        ExprNode elseExpr;
        if (is(LBRACE)) {
            expect(LBRACE);
            elseExpr = parseExpression();
            expect(RBRACE);
        } else {
            elseExpr = parseExpression();
        }
        
        return ASTFactory.createIfExpression(condition, thenExpr, elseExpr, ifToken, elseToken);
    }

    public ExprNode parsePrimaryExpression() {
    return attempt(new ParserAction<ExprNode>() {
        @Override
        public ExprNode parse() throws ParseError {
            ExprNode baseExpr;
            Token startToken = currentToken();
            
            if (isSuperMethodCall()) {
                return parseSuperMethodCall();
            }
            
            if (is(SUPER)) {
                Token superToken = expect(SUPER);
                return ASTFactory.createSuperExpression(superToken);
            }
            
            if (isThisKeyword()) {
                baseExpr = parseThisExpression();
            } else if (isConstructorCall() && !isMethodCallFollows()) {
                return parseConstructorCall();
            } else if (is(LBRACKET)) {
                if (isSlotAccessExpression()) {
                    List<String> slotNames = parseReturnSlots();
                    expect(COLON);
                    MethodCallNode methodCall = parseMethodCall();
                    methodCall.slotNames = slotNames;
                    baseExpr = methodCall;
                } else {
                    baseExpr = parseArrayLiteral();
                }
            } else if (is(IF)) {
                return parseIfExpression();
            } else if (is(INT_LIT)) {
                Token intToken = expect(INT_LIT);
                String intText = intToken.text;
                try {
                    int intValue = Integer.parseInt(intText);
                    baseExpr = ASTFactory.createIntLiteral(intValue, intToken);
                } catch (NumberFormatException e1) {
                    try {
                        long longValue = Long.parseLong(intText);
                        baseExpr = ASTFactory.createLongLiteral(longValue, intToken);
                    } catch (NumberFormatException e2) {
                        BigDecimal bigDecimalValue = new BigDecimal(intText);
                        baseExpr = ASTFactory.createFloatLiteral(bigDecimalValue, intToken);
                    }
                }
            } else if (is(FLOAT_LIT)) {
                Token floatToken = expect(FLOAT_LIT);
                String floatText = floatToken.text;
                
                Object resolvedValue = resolveFloatLiteralValue(floatText);
                
                if (resolvedValue instanceof BigDecimal) {
                    baseExpr = ASTFactory.createFloatLiteral((BigDecimal)resolvedValue, floatToken);
                } else {
                    try {
                        BigDecimal bigDecimalValue = new BigDecimal(floatText);
                        baseExpr = ASTFactory.createFloatLiteral(bigDecimalValue, floatToken);
                    } catch (NumberFormatException e) {
                        throw error("Invalid numeric literal: " + floatText, floatToken);
                    }
                }
            } else if (is(STRING_LIT)) {
                Token stringToken = currentToken();
                
                if (stringToken.interpolations != null && !stringToken.interpolations.isEmpty()) {
                    baseExpr = parseInterpolatedString(stringToken);
                } else {
                    baseExpr = ASTFactory.createStringLiteral(stringToken.text, stringToken);
                }
                expect(STRING_LIT);
            } else if (is(TRUE)) {
                Token trueToken = expect(TRUE);
                baseExpr = ASTFactory.createBoolLiteral(true, trueToken);
            } else if (is(FALSE)) {
                Token falseToken = expect(FALSE);
                baseExpr = ASTFactory.createBoolLiteral(false, falseToken);
            } else if (is(NONE)) {
                Token noneToken = expect(NONE);
                baseExpr = ASTFactory.createNoneLiteral(noneToken);
            } else if (is(INT, TEXT, FLOAT, BOOL, TYPE)) {
                Token typeToken = currentToken();
                String typeName = expect(KEYWORD).text;
                baseExpr = ASTFactory.createStringLiteral(typeName, typeToken);
            } else if (is(ID) || (is(currentToken(), KEYWORD) && canKeywordBeMethodName(currentToken()))) {
                if (isMethodCallFollows()) {
                    baseExpr = parseMethodCall();
                } else {
                    Token idToken = currentToken();
                    String idName;
                    if (is(idToken, KEYWORD)) {
                        idName = expect(KEYWORD).text;
                    } else {
                        idName = expect(ID).text;
                    }
                    baseExpr = ASTFactory.createIdentifier(idName, idToken);
                }
            } else if (is(LPAREN)) {
                if (isTypeCast()) {
                    baseExpr = parseTypeCast();
                } else {
                    Token lparenToken = expect(LPAREN);
                    ExprNode firstExpr = parseExpression();
                    
                    if (is(COMMA)) {
                        List<ExprNode> elements = new ArrayList<ExprNode>();
                        elements.add(firstExpr);
                        
                        while (tryConsume(COMMA)) {
                            elements.add(parseExpression());
                        }
                        
                        if (elements.size() == 1 && !is(RPAREN)) {
                            throw error("Expected expression after comma in tuple", currentToken());
                        }
                        
                        expect(RPAREN);
                        baseExpr = ASTFactory.createTuple(elements, lparenToken);
                    } else {
                        expect(RPAREN);
                        baseExpr = firstExpr;
                    }
                }
          } else {
    // Get previous token for debugging
    Token prevToken = null;
    int currentPos = getPosition();
    if (currentPos > 0) {
        prevToken = tokens.get(currentPos - 1);
    }
    
    System.err.println("DEBUG: parsePrimaryExpression failing at line " + 
                      startToken.line + ":" + startToken.column + 
                      ", token=" + startToken + 
                      ", type=" + startToken.type + 
                      ", text='" + startToken.text + "'");
    System.err.println("DEBUG: Previous token: " + prevToken);
    System.err.println("DEBUG: Current position: " + currentPos + " of " + tokens.size());
    
    // Also print next few tokens for context
    System.err.println("DEBUG: Next few tokens:");
    for (int i = 0; i < 5 && (currentPos + i) < tokens.size(); i++) {
        Token t = tokens.get(currentPos + i);
        System.err.println("  [" + i + "] " + t);
    }
    
    throw error("Unexpected token in primary expression: " + startToken.text +
        " (" + getTypeName(startToken.type) + ")", startToken);
}

            baseExpr = parsePropertyAccessChain(baseExpr);

            while (is(LBRACKET)) {
                baseExpr = parseIndexAccessContinuation(baseExpr);
            }

            return baseExpr;
        }
    });
}

    private ExprNode parseInterpolatedString(Token token) {
        List<ExprNode> parts = new ArrayList<ExprNode>();
        
        for (Token part : token.interpolations) {
            if (is(part, STRING_LIT)) {
                if (!part.text.isEmpty()) {
                    parts.add(ASTFactory.createStringLiteral(part.text, part));
                }
            } else if (is(part, INTERPOL)) {
                ExprNode expr = parseInterpolationExpression(part);
                parts.add(expr);
            }
        }
        
        if (parts.isEmpty()) {
            return ASTFactory.createStringLiteral("", token);
        } else if (parts.size() == 1) {
            return parts.get(0);
        }
        
        ExprNode result = parts.get(0);
        for (int i = 1; i < parts.size(); i++) {
            result = ASTFactory.createBinaryOp(result, "+", parts.get(i), null);
        }
        
        return result;
    }

    private ExprNode parseInterpolationExpression(Token interpolationToken) {
        ParserState savedState = getCurrentState();
        
        try {
            MainLexer lexer = new MainLexer(interpolationToken.text);
            List<Token> exprTokens = lexer.tokenize();
            
            if (!exprTokens.isEmpty() && is(exprTokens.get(exprTokens.size() - 1), EOF)) {
                exprTokens = exprTokens.subList(0, exprTokens.size() - 1);
            }

            ParserState interpolationState = new ParserState(exprTokens);
            ParserContext tempCtx = new ParserContext(interpolationState);
            ExpressionParser tempParser = new ExpressionParser(tempCtx);
            
            return tempParser.parseExpression();
            
        } catch (Exception e) {
            throw error("Invalid interpolation expression: {" + interpolationToken.text + "}", interpolationToken);
        } finally {
            setState(savedState);
        }
    }

    private ExprNode parsePropertyAccessChain(ExprNode baseExpr) {
        ExprNode current = baseExpr;
        
        while (is(DOT)) {
            Token dotToken = expect(DOT);
            
            if (is(ID) || (is(currentToken(), KEYWORD) && canKeywordBeMethodName(currentToken()))) {
                Token propertyToken = currentToken();
                String propertyName;
                if (is(propertyToken, KEYWORD)) {
                    propertyName = expect(KEYWORD).text;
                } else {
                    propertyName = expect(ID).text;
                }
                
                if (lookahead(1) != null && is(lookahead(1), LPAREN)) {
                    break;
                } else {
                    current = ASTFactory.createPropertyAccess(current, propertyName, dotToken);
                }
            } else {
                throw error("Expected field name or method after '.'", currentToken());
            }
        }
        
        return current;
    }

    private boolean isThisKeyword() {
        Token current = currentToken();
        if (is(current, KEYWORD) && is(current, THIS)) {
            return true;
        } else if (is(current, ID)) {
            if (lookahead(1) != null && is(lookahead(1), DOT)) {
                Token afterDot = lookahead(2);
                if (afterDot != null && is(afterDot, KEYWORD) && 
                    is(afterDot, THIS)) {
                    return true;
                }
            }
        }
        return false;
    }

    private ExprNode parseThisExpression() {
        Token first = currentToken();
        String className = null;
        
        if (is(first, ID) && lookahead(1) != null && 
            is(lookahead(1), DOT) && lookahead(2) != null &&
            is(lookahead(2), KEYWORD) && is(lookahead(2), THIS)) {
            
            Token classNameToken = expect(ID);
            className = classNameToken.text;
            expect(DOT);
            Token thisToken = expect(THIS);
            return ASTFactory.createThisExpression(className, thisToken);
        }
        
        Token thisToken = expect(THIS);
        return ASTFactory.createThisExpression(null, thisToken);
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
                return parseEqualityChain(left, op.text);
            }
            
            if (opPrecedence > 0) {
                Token opToken = consume();
                ExprNode right = parsePrecedence(opPrecedence + 1);
                if (is(opToken, KEYWORD) && is(opToken, IS)) {
                    left = ASTFactory.createBinaryOp(left, IS.toString(), right, opToken);
                } else {
                    left = ASTFactory.createBinaryOp(left, op.text, right, opToken);
                }
                continue;
            }
            
            if (is(op, LBRACKET)) {
                left = parseIndexAccessContinuation(left);
                continue;
            }
            
            break;
        }
        
        return left;
    }

    private ExprNode parsePrefix() {
        if (is(BANG, PLUS, MINUS)) {
            Token opToken = consume();
            ExprNode operand = parsePrecedence(PREC_UNARY);
            return ASTFactory.createUnaryOp(opToken.text, operand, opToken);
        }
        
        return parsePrimaryExpression();
    }

private ExprNode parseBooleanChain() {
    Token typeToken = currentToken();
    boolean isAll = is(typeToken, ALL);
    consume();

    if (is(ID)) {
        Token arrayNameToken = currentToken();
        String arrayName = expect(ID).text;

        if (isComparisonOp(currentToken())) {
            Token opToken = consume();
            ExprNode right = parsePrecedence(PREC_COMPARISON + 1);                

            List<ExprNode> chainArgs = new ArrayList<ExprNode>();
            chainArgs.add(right);

            ExprNode arrayExpr = ASTFactory.createIdentifier(arrayName, arrayNameToken);
            EqualityChainNode chain = ASTFactory.createEqualityChain(arrayExpr, opToken.text, isAll, chainArgs, arrayNameToken, opToken, typeToken);
            return chain;
        } else {
            throw new ParseError("Expected comparison operator after 'all/any <arrayName>'", currentToken());
        }
    } else if (is(LBRACKET)) {
        // CONSUME the opening bracket!
        expect(LBRACKET);
        
        List<ExprNode> expressions = new ArrayList<ExprNode>();

        if (!is(RBRACKET)) {
            expressions.add(parseExpression());
            
            // Skip whitespace and comments
            skipWhitespaceAndComments();
            
            if (!is(COMMA, RBRACKET)) {
                throw new ParseError("Boolean chain requires at least two expressions or a comma after the first expression.", currentToken());
            }                 
            while (tryConsume(COMMA)) {
                skipWhitespaceAndComments();
                expressions.add(parseExpression());
                skipWhitespaceAndComments();
            }
        }
        expect(RBRACKET);
        
        BooleanChainNode node = ASTFactory.createBooleanChain(isAll, expressions, typeToken);
        return node;
    } else {
        throw new ParseError("Expected array variable or '[' after 'all/any'", currentToken());
    }
}

    private ExprNode parseEqualityChain(ExprNode left, String operator) {
    // Get the left expression token (token before the operator)
    Token leftToken = lookahead(-1);
    
    // Consume the comparison operator (==, !=, etc.)
    Token operatorToken = currentToken();
    consume();
    
    // Now we should be at 'all' or 'any'
    Token chainTypeToken = currentToken();
    boolean isAllChain = is(chainTypeToken, ALL);
    
    if (!is(chainTypeToken, ALL, ANY)) {
        throw new ParseError("Expected 'all' or 'any' after comparison operator", chainTypeToken);
    }
    
    consume(); // Consume 'all' or 'any'
    
    List<ExprNode> chainArgs = new ArrayList<ExprNode>();
    
    if (is(LBRACKET)) {
        expect(LBRACKET);
        if (!is(RBRACKET)) {
            chainArgs.add(parseChainArgument());
            while (tryConsume(COMMA)) {
                chainArgs.add(parseChainArgument());
            }
        }
        expect(RBRACKET);
    } else if (is(ID)) {
        Token arrayNameToken = currentToken();
        String arrayName = expect(ID).text;
        ExprNode arrayExpr = ASTFactory.createIdentifier(arrayName, arrayNameToken);
        chainArgs.add(arrayExpr);
    } else {
        throw new ParseError("Expected array variable or '[' for array literal after 'all/any'", currentToken());
    }
    
    EqualityChainNode chain = ASTFactory.createEqualityChain(left, operator, isAllChain, chainArgs, leftToken, operatorToken, chainTypeToken);
    return chain;
}

    private ExprNode parseChainArgument() {
        if (is(BANG)) {
            Token bangToken = expect(BANG);
            ExprNode arg = parsePrimaryExpression();
            return ASTFactory.createUnaryOp("!", arg, bangToken);
        }
        
        if (is(LPAREN)) {
            return parseArgumentList();
        }
        
        if (is(ID) && isSymbolAt(1, RBRACKET)) {
            Token idToken = currentToken();
            throw error("Redundant brackets around array variable '" + idToken.text + 
                       "'. Use 'any " + idToken.text + "' instead of 'any[" + idToken.text + "]'", idToken);
        }
        
        return parsePrimaryExpression();
    }

    private ExprNode parseArgumentList() {
        Token lparenToken = expect(LPAREN);
        List<ExprNode> arguments = new ArrayList<ExprNode>();
        if (!is(RPAREN)) {
            arguments.add(parseExpression());
            while (tryConsume(COMMA)) {
                arguments.add(parseExpression());
            }
        }
        expect(RPAREN);
        return ASTFactory.createArgumentList(arguments, lparenToken);
    }

    private ExprNode parseArrayLiteral() {
    Token lbracketToken = expect(LBRACKET);
    
    List<ExprNode> elements = new ArrayList<ExprNode>();
    
    if (!is(RBRACKET)) {
        // Check if this is a range expression
        if (isRangeStart()) {
            elements.add(parseRangeExpression());
        } else {
            elements.add(parseExpression());
        }
        
        // Check for more elements (comma-separated)
        while (tryConsume(COMMA)) {
            skipWhitespaceAndComments();
            
            if (isRangeStart()) {
                elements.add(parseRangeExpression());
            } else {
                elements.add(parseExpression());
            }
        }
    }
    
    expect(RBRACKET);
    return ASTFactory.createArray(elements, lbracketToken);
}

    private boolean isRangeStart() {
    // Save state to check without consuming
    ParserState savedState = getCurrentState();
    
    try {
        skipWhitespaceAndComments();
        
        if (is(BY)) {
            // Pattern: BY expr IN expr TO expr
            consume(); // BY
            skipWhitespaceAndComments();
            
            // Check if next is an expression start
            if (!isExpressionStart(currentToken())) {
                return false;
            }
            // Skip the expression
            try {
                parseExpression();
            } catch (ParseError e) {
                return false;
            }
            
            skipWhitespaceAndComments();
            if (!is(IN)) return false;
            consume(); // IN
            
            skipWhitespaceAndComments();
            if (!isExpressionStart(currentToken())) {
                return false;
            }
            // Skip the expression  
            try {
                parseExpression();
            } catch (ParseError e) {
                return false;
            }
            
            skipWhitespaceAndComments();
            if (!is(TO)) return false;
            
            return true;
        } else {
            // Pattern: expr TO expr
            if (!isExpressionStart(currentToken())) {
                return false;
            }
            // Skip first expression
            try {
                parseExpression();
            } catch (ParseError e) {
                return false;
            }
            
            skipWhitespaceAndComments();
            return is(TO);
        }
    } finally {
        setState(savedState);
    }
}

    private RangeNode parseRangeExpression() {
    ExprNode step = null;
    Token byToken = null;
    Token toToken = null;
    
    if (is(BY)) {
        byToken = expect(BY);
        step = parseExpression();
        skipWhitespaceAndComments();
        expect(IN);
        skipWhitespaceAndComments();
    }
    
    ExprNode start = parseExpression();
    skipWhitespaceAndComments();
    toToken = expect(TO);
    skipWhitespaceAndComments();
    ExprNode end = parseExpression();
    
    return ASTFactory.createRange(step, start, end, byToken, toToken);
}

    private ExprNode parseTypeCast() {
        Token lparenToken = expect(LPAREN);
        String type = parseTypeReference();    
        expect(RPAREN);
        ExprNode expressionToCast = parsePrecedence(PREC_UNARY);
        return ASTFactory.createTypeCast(type, expressionToCast, lparenToken);
    }

    private boolean isConstructorCall() {
        return attempt(new ParserAction<Boolean>() {
            @Override
            public Boolean parse() throws ParseError {
                Token first = currentToken();
                if (first == null || first.type != ID) return false;
                
                String idName = first.text;
                
                if (idName.length() == 0 || Character.isLowerCase(idName.charAt(0))) {
                    return false;
                }
                
                int pos = 1;
                while (peek(pos) != null && 
                       (is(peek(pos), WS, LINE_COMMENT, BLOCK_COMMENT))) {
                    pos++;
                }
                
                return peek(pos) != null && is(peek(pos), LPAREN);
            }
        });
    }

    private boolean isMethodCallFollows() {
        return attempt(new ParserAction<Boolean>() {
            @Override
            public Boolean parse() throws ParseError {
                Token first = currentToken();
                
                if (first == null) return false;
                
                boolean isValidName = (is(first, ID)) || 
                                     (is(first, KEYWORD) && canKeywordBeMethodName(first));
                
                if (!isValidName) return false;
                
                if (isConstructorCall()) {
                    return false;
                }
                
                int pos = 1;
                while (lookahead(pos) != null && is(lookahead(pos), DOT)) {
                    pos++;
                    Token afterDot = lookahead(pos);
                    
                    if (afterDot == null || 
                        !(is(afterDot, ID) || 
                          (is(afterDot, KEYWORD) && canKeywordBeMethodName(afterDot)))) {
                        return false;
                    }
                    pos++;
                }
                
                Token afterDots = lookahead(pos);
                return afterDots != null && is(afterDots, LPAREN);
            }
        });
    }

    private boolean isSlotAccessExpression() {
        if (!isSymbolAt(0, LBRACKET)) return false;

        int pos = getPosition() + 1;
        int depth = 1;

        while (pos < tokens.size() && depth > 0) {
            Token t = tokens.get(pos);
            if (is(t, LBRACKET)) depth++;
            else if (is(t, RBRACKET)) depth--;
            pos++;
        }

        if (depth == 0 && pos < tokens.size()) {
            Token t = tokens.get(pos);
            return t != null && is(t, COLON);
        }
        return false;
    }

    private boolean isTypeCast() {
        if (!isSymbolAt(0, LPAREN)) return false;
        
        Token second = lookahead(1);
        if (second == null || !isTypeStart(second)) return false;
        
        int pos = getPosition();
        int parenDepth = 0;
        
        for (int i = 0; i < 50 && pos < tokens.size(); i++) {
            Token t = tokens.get(pos);
            
            if (parenDepth > 0 && isIllegalTypeToken(t)) {
                return false;
            }

            if (is(t, LPAREN)) parenDepth++;
            else if (is(t, RPAREN)) {
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

    private boolean isIllegalTypeToken(Token t) {
        if (is(t, INT_LIT, FLOAT_LIT, STRING_LIT, BOOL_LIT)) return true;
        
        if (is(t, PLUS, MINUS, MUL, DIV, MOD, EQ, NEQ, GT, LT, GTE, LTE)) return true;
        
        return false;
    }

    private int getPrecedence(Token token) {
        if (token == null) return 0;
        
        if (is(token, KEYWORD) && is(token, IS)) return PREC_IS;
        
        if (is(token, SYMBOL)) {
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
        
        if (is(next, KEYWORD) && is(next, ALL, ANY)) {
            Token after = peek(offset + 1);
            return after != null && (is(after, LBRACKET) || is(after, ID));
        }
        return false;
    }

    private boolean isComparisonOp(Token t) {
        return t != null && t.symbol != null &&
               (is(t, EQ,NEQ, GT, LT, GTE, LTE));
    }
    
    public boolean isExpressionStart() {
        Token token = currentToken();
        if (token == null) return false;
        
        return isExpressionStart(token);
    }
    
    public ExprNode tryParseExpression() {
        return tryParse(new ParserAction<ExprNode>() {
            @Override
            public ExprNode parse() throws ParseError {
                return parseExpression();
            }
        });
    }
    
    public ExprNode tryParsePrimaryExpression() {
        return tryParse(new ParserAction<ExprNode>() {
            @Override
            public ExprNode parse() throws ParseError {
                return parsePrimaryExpression();
            }
        });
    }
}