package cod.parser;

import cod.ast.ASTFactory;
import cod.error.ParseError;
import cod.ast.nodes.*;
import cod.interpreter.registry.GlobalRegistry;
import cod.lexer.Token;
import static cod.lexer.TokenType.*;
import cod.math.AutoStackingNumber;
import cod.parser.context.*;
import static cod.syntax.Symbol.*;
import static cod.syntax.Keyword.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ExpressionParser extends BaseParser {
    
    private static final int PREC_ASSIGNMENT = 10;
    private static final int PREC_EQUALITY = 50;
    private static final int PREC_COMPARISON = 60;
    private static final int PREC_TERM = 70;
    private static final int PREC_FACTOR = 80;
    private static final int PREC_UNARY = 90;
    private static final int PREC_CALL = 100;
    private static final int PREC_IS = 40;
    
    private final GlobalRegistry globalRegistry;
    private Set<String> globalFunctionNames;
    private StatementParser statementParser;
    
    public ExpressionParser(ParserContext ctx, ASTFactory factory) {
        this(ctx, factory, null, null);
    }
    
    public ExpressionParser(ParserContext ctx, ASTFactory factory, GlobalRegistry globalRegistry) {
        this(ctx, factory, globalRegistry, null);
    }
    
    public ExpressionParser(ParserContext ctx, ASTFactory factory, GlobalRegistry globalRegistry, StatementParser statementParser) {
        super(ctx, factory);
        this.globalRegistry = globalRegistry;
        this.statementParser = statementParser;
        if (globalRegistry != null) {
            this.globalFunctionNames = globalRegistry.getGlobalFunctionNames();
        }
    }
    
    public void setStatementParser(StatementParser statementParser) {
        this.statementParser = statementParser;
    }
    
    @Override
    protected BaseParser createIsolatedParser(ParserContext isolatedCtx) {
        return new ExpressionParser(isolatedCtx, this.factory, this.globalRegistry, this.statementParser);
    }

    public int parseExpr() {
        return attempt(new ParserAction<Integer>() {
            @Override
            public Integer parse() throws ParseError {
                if (is(ALL, ANY)) {
                    Token nextToken = next();
                    if (is(nextToken, LPAREN)) {
                        return parseMethodCall();
                    } else {
                        return parseBooleanChain();
                    }
                }
                return parsePrecedence(PREC_ASSIGNMENT);
            }
        });
    }

    private int parseConstructorCall() {
        Token classNameToken = now();
        String className = expect(ID).text;

        List<Integer> args = new ArrayList<Integer>();
        List<String> argNames = new ArrayList<String>();
        
        if (!is(RPAREN)) {
            if (isNamedArgument()) {
                parseNamedArgumentList(args, argNames);
            } else {
                args.add(parseExpr());
                argNames.add(null);
                while (consume(COMMA)) {
                    args.add(parseExpr());
                    argNames.add(null);
                }
            }
        }
        expect(RPAREN);
        
        int call = factory.createConstructorCall(className, args, classNameToken);
        {
            String[] namesArr = argNames.toArray(new String[argNames.size()]);
            factory.getAST().constructorCallSetArgs(call, factory.getAST().constructorCallArgs(call), namesArr);
        }
        return call;
    }

    private void parseNamedArgumentListForCall(int callId) {
        do {
            String argName = expect(ID).text;
            expect(COLON);
            int valueId = parseExpr();
            factory.getAST().methodCallAddArg(callId, valueId, argName);
            if (!is(COMMA)) {
                break;
            }
            expect(COMMA);
        } while (!is(RPAREN));
    }

        private boolean isNamedArgument() {
        save();
        try {

            Token first = now();
            if (!is(first, ID)) return false;
            
            Token second = next();
            return is(second, COLON);
        } finally {
            restore();
        }
    }

    private void parseNamedArgumentList(List<Integer> args, List<String> argNames) {
        do {
            String argName = expect(ID).text;
            
            expect(COLON);
            int value = parseExpr();
            
            args.add(value);
            argNames.add(argName);
            
            if (!is(COMMA)) {
                break;
            }
            expect(COMMA);
        } while (!is(RPAREN));
    }

    public int parseMethodCall() {
        return attempt(new ParserAction<Integer>() {
            @Override
            public Integer parse() throws ParseError {
                Token nameStartToken = now();
                String qualifiedNameStr = parseQualifiedNameOrKeyword();
                String methodName = qualifiedNameStr;
                if (qualifiedNameStr.contains(".")) {
                    methodName = qualifiedNameStr.substring(qualifiedNameStr.lastIndexOf('.') + 1);
                }
                int call = factory.createMethodCall(methodName, qualifiedNameStr, nameStartToken);
                
                if (!qualifiedNameStr.contains(".") && globalFunctionNames != null && 
                    globalFunctionNames.contains(methodName)) {
                    factory.getAST().methodCallSetIsGlobal(call, true);
                }
                
                expect(LPAREN);
                
                if (!is(RPAREN)) {
                    if (isNamedArgument()) {
                        parseNamedArgumentListForCall(call);
                    } else {
                        factory.getAST().methodCallAddArg(call, parseExpr(), null);
                        while (consume(COMMA)) {
                            factory.getAST().methodCallAddArg(call, parseExpr(), null);
                        }
                    }
                }
                expect(RPAREN);
                return call;
            }
        });
    }

    private int parseSuperMethodCall() {
        Token superToken = now();
        expect(SUPER);
        
        expect(DOT);
        
        Token methodToken = now();
        String methodName;
        
        if (is(methodToken, ID)) {
            methodName = expect(ID).text;
        } else if (canBeMethod(methodToken)) {
            methodName = expect(KEYWORD).text;
        } else {
            throw error("Expected method name after 'super.'", methodToken);
        }
        
        int call = factory.createMethodCall(methodName, "super." + methodName, superToken);
        factory.getAST().methodCallSetIsSuper(call, true);
        factory.getAST().methodCallSetIsGlobal(call, false);
        
        expect(LPAREN);
        
        if (!is(RPAREN)) {
            if (isNamedArgument()) {
                parseNamedArgumentListForCall(call);
            } else {
                factory.getAST().methodCallAddArg(call, parseExpr(), null);
                while (consume(COMMA)) {
                    factory.getAST().methodCallAddArg(call, parseExpr(), null);
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
                if (!is(SUPER)) return false;
                expect(SUPER);
                
                if (!is(DOT)) return false;
                expect(DOT);
                
                Token nameToken = now();
                boolean isValidName = (is(nameToken, ID) || canBeMethod(nameToken));
                if (!isValidName) return false;
                
                consume();
                
                return is(LPAREN);
            }
        });
    }

    private String parseQualifiedNameOrKeyword() {
        Token token = now();
        
        if (canBeMethod(token)) {
            String name = expect(KEYWORD).text;
            
            if (consume(DOT)) {
                StringBuilder fullName = new StringBuilder(name);
                fullName.append(".");
                fullName.append(expect(ID).text);
                
                while (consume(DOT)) {
                    fullName.append(".");
                    fullName.append(expect(ID).text);
                }
                return fullName.toString();
            }
            
            return name;
        }
        
        return parseQualifiedName();
    }

    public boolean isRangeIndex() {
        save();
        try {

            
            if (!isExprStart(now())) return false;
            parseExpr();

            
            if (!is(RANGE_DOTDOT) && !is(TO)) return false;
            
            return true;
        } catch (ParseError e) {
            return false;
        } finally {
            restore();
        }
    }

    public int parseIndexAccessContinuation(int arrayId) {
        Token lbracketToken = expect(LBRACKET);
        
        int indexId;
        
        if (isRangeIndex()) {
            indexId = parseRangeIndex();
        } else {
            indexId = parseExpr();
            expect(RBRACKET);
            return factory.createIndexAccess(arrayId, indexId, lbracketToken);
        }
        
        return factory.createIndexAccess(arrayId, indexId, lbracketToken);
    }

    public int parseRangeIndex() {
        List<Integer> ranges = new ArrayList<Integer>();
        
        ranges.add(parseSingleRangeIndex());
       
        
        while (is(COMMA)) {
            expect(COMMA);

            ranges.add(parseSingleRangeIndex());

        }
        
        expect(RBRACKET);
        
        if (ranges.size() == 1) {
            return ranges.get(0);
        } else {
            return factory.createMultiRangeIndex(ranges, null);
        }
    }

    private int parseSingleRangeIndex() {
        int stepId = cod.ast.FlatAST.NULL;
        int startId;
        int endId;
        Token stepToken = null;
        Token rangeToken = null;
        
        startId = parseExpr();
       
        
        if (is(RANGE_DOTDOT)) {
            rangeToken = expect(RANGE_DOTDOT);
        } else if (is(TO)) {
            rangeToken = expect(TO);
        } else {
            throw error("Expected range operator '..' or 'to'");
        }
        
       
        
        endId = parseExpr();
       
        
        if (is(RANGE_HASH)) {
            stepToken = expect(RANGE_HASH);
            stepId = parseExpr();

        } else if (is(BY)) {
            stepToken = expect(BY);
            stepId = parseExpr();

        }
        
        return factory.createRangeIndex(stepId, startId, endId, stepToken, rangeToken);
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
                throw error("Expected slot name or index", now());
            }
        } while (consume(COMMA));
        expect(RBRACKET);
        return slots;
    }
    
    private int parseIfExpr() {
        Token ifToken = expect(IF);
        int condId = parseExpr();
        
        int thenId;
        if (is(LBRACE)) {
            expect(LBRACE);
            thenId = parseExpr();
            expect(RBRACE);
        } else {
            thenId = parseExpr();
        }
        
        Token elseToken = expect(ELSE);
        
        int elseId;
        if (is(LBRACE)) {
            expect(LBRACE);
            elseId = parseExpr();
            expect(RBRACE);
        } else {
            elseId = parseExpr();
        }
        
        return factory.createIfExpr(condId, thenId, elseId, ifToken, elseToken);
    }

    private int parseLambdaSignature() {
        Token lambdaToken = expect(LAMBDA);
        
        SlotParser slotParser = new SlotParser(this);
        
        expect(LPAREN);
        
        List<Integer> parameters = new ArrayList<Integer>();
        
        // Parse parameters if any
        if (!is(RPAREN)) {
            parameters.add(parseLambdaParameter());
            
            while (consume(COMMA)) {
    
                parameters.add(parseLambdaParameter());
            }
        }
        
        expect(RPAREN);
       
        
        // Parse optional return contract (::)
        List<Integer> returnSlots = null;
        if (is(DOUBLE_COLON)) {
            returnSlots = slotParser.parseSlotContract();

        }
        
        // Parse lambda body
        int bodyId = cod.ast.FlatAST.NULL;
        Token tildeArrowToken = null;
        
        if (is(LBRACE)) {
            // Block body - requires statementParser
            if (statementParser == null) {
                throw error("Internal error: statementParser not available for lambda block");
            }
            
            expect(LBRACE);
            int block = factory.createBlock(null);
            
            while (!is(RBRACE) && !is(EOF)) {
                factory.getAST().blockAddStmt(block, statementParser.parseStmt());
    
            }
            expect(RBRACE);
            bodyId = block;
        } else if (is(TILDE_ARROW)) {
            tildeArrowToken = expect(TILDE_ARROW);

            
            if (is(LBRACE)) {
                // Optional braces around expression(s)
                expect(LBRACE);
                List<Integer> assignments = slotParser.parseSlotAssignments();
                expect(RBRACE);
                
                // Validate against contract if present
                if (returnSlots != null) {
                    slotParser.validateSlotCount(returnSlots, assignments, tildeArrowToken);
                }
                
                // Wrap in BlockNode
                int block = factory.createBlock(null);
                if (assignments.size() == 1) {
                    factory.getAST().blockAddStmt(block, assignments.get(0));
                } else {
                    factory.getAST().blockAddStmt(block, factory.createMultipleSlotAsmt(assignments, tildeArrowToken));
                }
                bodyId = block;
            } else {
                // Direct expression(s) without braces
                List<Integer> assignments = slotParser.parseSlotAssignments();
                
                // Validate against contract if present
                if (returnSlots != null) {
                    slotParser.validateSlotCount(returnSlots, assignments, tildeArrowToken);
                }
                
                // Wrap in BlockNode
                int block = factory.createBlock(null);
                if (assignments.size() == 1) {
                    factory.getAST().blockAddStmt(block, assignments.get(0));
                } else {
                    factory.getAST().blockAddStmt(block, factory.createMultipleSlotAsmt(assignments, tildeArrowToken));
                }
                bodyId = block;
            }
        } else {
            // Error: missing ~> or {
            throw error(
                "Expected '~>' or '{' after lambda parameters" +
                (returnSlots != null ? " (return contract requires ~> assignments)" : ""),
                now()
            );
        }
        
        return factory.createLambda(parameters, returnSlots, bodyId, lambdaToken);
    }

    private int parseLambdaParameter() {
        Token paramToken = now();
        
        // Handle tuple destructuring
        if (is(LPAREN)) {
            expect(LPAREN);
            List<String> tupleElements = new ArrayList<String>();
            
            if (!is(RPAREN)) {
                tupleElements.add(expect(ID).text);
                while (consume(COMMA)) {
        
                    tupleElements.add(expect(ID).text);
                }
            }
            expect(RPAREN);
            
            // Create parameter with tuple destructuring info
            int param = factory.createParam("_tuple", null, cod.ast.FlatAST.NULL, true, paramToken);
            factory.getAST().paramSetTupleDestructuring(param, true, tupleElements.toArray(new String[tupleElements.size()]));
            factory.getAST().paramSetIsLambda(param, true);
            return param;
        }
        
        // Regular parameter
        String paramName = expect(ID).text;
        
        // Optional type annotation
        String paramType = null;
        if (consume(COLON)) {
            paramType = parseTypeReference();
        }
        
        // Optional default value
        int defaultValue = cod.ast.FlatAST.NULL;
        if (consume(ASSIGN)) {
            defaultValue = parseExpr();
        }
        
        boolean typeInferred = (paramType == null);
        int param = factory.createParam(
            paramName, paramType, defaultValue, typeInferred, paramToken
        );
        factory.getAST().paramSetIsLambda(param, true);
        
        return param;
    }

    public int parsePrimaryExpr() {
    return attempt(new ParserAction<Integer>() {
        @Override
        public Integer parse() throws ParseError {
            int baseId;
            Token startToken = now();
            
            if (startToken == null) {
                throw error("Unexpected end of input in primary expression");
            }
            
            if (is(LAMBDA)) {
                baseId = parseLambdaSignature();
            }
            else if (isSuperMethodCall()) {
                baseId = parseSuperMethodCall();
            }
            else if (is(SUPER)) {
                Token superToken = expect(SUPER);
                baseId = factory.createSuperExpr(superToken);
            }
            else if (isThisKeyword()) {
                baseId = parseThisExpr();
            }
            else if (isConstructorCall() && !isMethodCallFollows()) {
                baseId = parseConstructorCall();
            }
            else if (is(LBRACKET)) {
                if (isSlotAccessExpression()) {
                    List<String> slotNames = parseReturnSlots();
                    expect(COLON);
                    int methodCall = parseMethodCall();
                    factory.getAST().methodCallSetSlotNames(methodCall, slotNames.toArray(new String[slotNames.size()]));
                    baseId = methodCall;
                } else {
                    baseId = parseArrayLiteral();
                }
            }
            // NEW: Handle method calls without slot brackets
            else if (isMethodCallFollows()) {
                int methodCall = parseMethodCall();
                // The interpreter will handle single-slot optimization
                baseId = methodCall;
            }
            else if (is(IF)) {
                baseId = parseIfExpr();
            }
            else if (is(INT_LIT)) {
                Token intToken = expect(INT_LIT);
                String intText = intToken.text;
                try {
                    int intValue = Integer.parseInt(intText);
                    baseId = factory.createIntLiteral(intValue, intToken);
                } catch (NumberFormatException e1) {
                    try {
                        long longValue = Long.parseLong(intText);
                        baseId = factory.createLongLiteral(longValue, intToken);
                    } catch (NumberFormatException e2) {
                        AutoStackingNumber bigValue = AutoStackingNumber.valueOf(intText);
                        baseId = factory.createFloatLiteral(bigValue, intToken);
                    }
                }
            }
            else if (is(FLOAT_LIT)) {
    Token floatToken = expect(FLOAT_LIT);
    String floatText = floatToken.text;
    
    // First try to resolve as a suffixed number (K, M, B, T, Q, Qi)
    Object resolvedValue = resolveFloatLiteralValue(floatText);
    
    if (resolvedValue instanceof AutoStackingNumber) {
        baseId = factory.createFloatLiteral((AutoStackingNumber)resolvedValue, floatToken);
    } else {
        // If resolveFloatLiteralValue returned null, try direct parsing
        // This will now handle regular floats like "0.0" correctly
        try {
            AutoStackingNumber value = AutoStackingNumber.valueOf(floatText);
            baseId = factory.createFloatLiteral(value, floatToken);
        } catch (NumberFormatException e) {
            throw error("Invalid numeric literal: " + floatText, floatToken);
        }
    }
}
            else if (is(TEXT_LIT, INTERPOL)) {
                Token textToken = now();
                
                if (textToken.type == INTERPOL && textToken.hasChildTokens()) {
                    List<Integer> parts = new ArrayList<Integer>();
                    
                    for (Token part : textToken.childTokens) {
                        if (part.type == TEXT_LIT) {
                            parts.add(factory.createTextLiteral(part.text, part));
                        } else if (part.type == INTERPOL) {
                            if (part.hasChildTokens()) {
                                ParserContext subCtx = new ParserContext(part.childTokens);
                                ExpressionParser subParser = new ExpressionParser(subCtx, factory, globalRegistry, statementParser);
                                int expr = subParser.parseExpr();
                                parts.add(expr);
                            }
                        }
                    }
                    
                    if (parts.isEmpty()) {
                        baseId = factory.createTextLiteral("", textToken);
                    } else if (parts.size() == 1) {
                        baseId = parts.get(0);
                    } else {
                        baseId = parts.get(0);
                        for (int i = 1; i < parts.size(); i++) {
                            baseId = factory.createBinaryOp(baseId, "+", parts.get(i), textToken);
                        }
                    }
                } else {
                    String text = textToken.text;
                    if (text.startsWith("|\"") && text.endsWith("\"|")) {
                        baseId = handleMultilineTextInterpolation(textToken);
                    } else {
                        baseId = factory.createTextLiteral(text, textToken);
                    }
                }
                consume();
            }
            else if (is(TRUE)) {
                Token trueToken = expect(TRUE);
                baseId = factory.createBoolLiteral(true, trueToken);
            }
            else if (is(FALSE)) {
                Token falseToken = expect(FALSE);
                baseId = factory.createBoolLiteral(false, falseToken);
            }
            else if (is(NONE)) {
                Token noneToken = expect(NONE);
                baseId = factory.createNoneLiteral(noneToken);
            }
            else if (is(INT, TEXT, FLOAT, BOOL, TYPE)) {
                Token typeToken = now();
                String typeName = expect(KEYWORD).text;
                baseId = factory.createTextLiteral(typeName, typeToken);
            }
            else if (is(ID) || canBeMethod(now())) {
                if (isMethodCallFollows()) {
                    baseId = parseMethodCall();
                } else {
                    Token idToken = now();
                    String idName;
                    if (is(idToken, KEYWORD)) {
                        idName = expect(KEYWORD).text;
                    } else {
                        idName = expect(ID).text;
                    }
                    baseId = factory.createIdentifier(idName, idToken);
                }
            }
            else if (is(LPAREN)) {
                if (isTypeCast()) {
                    baseId = parseTypeCast();
                } else {
                    Token lparenToken = expect(LPAREN);
                    int firstExpr = parseExpr();
                    
                    if (is(COMMA)) {
                        List<Integer> elements = new ArrayList<Integer>();
                        elements.add(firstExpr);
                        
                        while (consume(COMMA)) {
                
                            elements.add(parseExpr());
                
                        }
                        
                        if (elements.size() == 1 && !is(RPAREN)) {
                            throw error("Expected expression after comma in tuple");
                        }
                        
                        expect(RPAREN);
                        baseId = factory.createTuple(elements, lparenToken);
                    } else {
                        expect(RPAREN);
                        baseId = firstExpr;
                    }
                }
            }
            else {
                throw error("Unexpected token in primary expression: " + startToken.text +
                    " (" + getTypeName(startToken.type) + ")", startToken);
            }

            while (is(DOT)) {
                Token dotToken = expect(DOT);
    
                
                int propId = parsePrimaryExpr();
                
                baseId = factory.createPropertyAccess(baseId, propId, dotToken);
            }

            while (is(LBRACKET)) {
                baseId = parseIndexAccessContinuation(baseId);
            }

            return baseId;
        }
    });
}

    private int handleMultilineTextInterpolation(Token token) {
        String fullText = token.text;
        
        if (!fullText.startsWith("|\"") || !fullText.endsWith("\"|")) {
            return factory.createTextLiteral(fullText, token);
        }
        
        if (token.hasChildTokens()) {
            return handleInterpolatedTextWithTokens(token);
        }
        
        return parseInterpolatedText(token);
    }

    private int handleInterpolatedTextWithTokens(Token token) {
        List<Token> exprTokens = token.childTokens;
        
        if (exprTokens == null || exprTokens.isEmpty()) {
            return factory.createTextLiteral(token.text, token);
        }
        
        List<Integer> parts = new ArrayList<Integer>();
        
        String fullText = token.text;
        if (fullText.startsWith("\"") && fullText.endsWith("\"")) {
            fullText = fullText.substring(1, fullText.length() - 1);
        } else if (fullText.startsWith("|\"") && fullText.endsWith("\"|")) {
            fullText = fullText.substring(2, fullText.length() - 2);
        }
        
        int braceCount = 0;
        StringBuilder currentText = new StringBuilder();
        boolean inEscape = false;
        
        for (int i = 0; i < fullText.length(); i++) {
            char c = fullText.charAt(i);
            
            if (inEscape) {
                currentText.append('\\').append(c);
                inEscape = false;
            } else if (c == '\\') {
                inEscape = true;
            } else if (c == '{') {
                if (braceCount == 0) {
                    if (currentText.length() > 0) {
                        parts.add(factory.createTextLiteral(currentText.toString(), token));
                        currentText.setLength(0);
                    }
                    braceCount++;
                } else {
                    currentText.append(c);
                }
            } else if (c == '}') {
                if (braceCount == 1) {
                    if (!exprTokens.isEmpty()) {
                        Token exprToken = exprTokens.get(0);
                        if (exprToken.childTokens != null && !exprToken.childTokens.isEmpty()) {
                            int expr = parsePreTokenizedInterpolation(exprToken);
                            parts.add(expr);
                        } else {
                            parts.add(factory.createIdentifier(exprToken.text, exprToken));
                        }
                    } else {
                        parts.add(factory.createTextLiteral("", token));
                    }
                    braceCount = 0;
                } else {
                    currentText.append(c);
                }
            } else {
                currentText.append(c);
            }
        }
        
        if (currentText.length() > 0) {
            parts.add(factory.createTextLiteral(currentText.toString(), token));
        }
        
        if (parts.isEmpty()) {
            return factory.createTextLiteral("", token);
        } else if (parts.size() == 1) {
            return parts.get(0);
        }
        
        int result = parts.get(0);
        for (int i = 1; i < parts.size(); i++) {
            result = factory.createBinaryOp(result, "+", parts.get(i), token);
        }
        
        return result;
    }

    private int parsePreTokenizedInterpolation(Token token) {
        List<Token> exprTokens = token.childTokens;
        
        if (exprTokens == null || exprTokens.isEmpty()) {
            throw error("Interpolation token has no child tokens", token);
        }
        
        ParserContext subCtx = new ParserContext(exprTokens);
        ExpressionParser subParser = new ExpressionParser(subCtx, factory, globalRegistry, statementParser);
        
        int result = subParser.parseExpr();
        
        if (!subParser.ctx.atEOF()) {
            throw error("Extra tokens in interpolation expression", token);
        }
        
        return result;
    }

    private int parseInterpolatedText(Token token) {
        String text = token.text;
        
        if (text.startsWith("\"") && text.endsWith("\"")) {
            text = text.substring(1, text.length() - 1);
        } else if (text.startsWith("|\"") && text.endsWith("\"|")) {
            text = text.substring(2, text.length() - 2);
        }
        
        List<Integer> parts = new ArrayList<Integer>();
        StringBuilder current = new StringBuilder();
        int pos = 0;
        boolean inEscape = false;
        
        while (pos < text.length()) {
            char c = text.charAt(pos);
            
            if (inEscape) {
                switch (c) {
                    case 'n': current.append('\n'); break;
                    case 't': current.append('\t'); break;
                    case 'r': current.append('\r'); break;
                    case '\\': current.append('\\'); break;
                    case '"': current.append('"'); break;
                    case '{': current.append('{'); break;
                    default: current.append('\\').append(c); break;
                }
                inEscape = false;
                pos++;
                continue;
            }
            
            if (c == '\\') {
                inEscape = true;
                pos++;
            } else if (c == '{') {
                if (current.length() > 0) {
                    parts.add(factory.createTextLiteral(current.toString(), token));
                    current.setLength(0);
                }
                
                int end = text.indexOf('}', pos + 1);
                if (end == -1) {
                    throw error("Unclosed interpolation in text", token);
                }
                
                String exprText = text.substring(pos + 1, end);
                
                int expr = parseInterpolationExpressionDirectly(exprText, token);
                parts.add(expr);
                
                pos = end + 1;
            } else {
                current.append(c);
                pos++;
            }
        }
        
        if (inEscape) {
            current.append('\\');
        }
        
        if (current.length() > 0) {
            parts.add(factory.createTextLiteral(current.toString(), token));
        }
        
        if (parts.isEmpty()) {
            return factory.createTextLiteral("", token);
        } else if (parts.size() == 1) {
            return parts.get(0);
        }
        
        int result = parts.get(0);
        for (int i = 1; i < parts.size(); i++) {
            result = factory.createBinaryOp(result, "+", parts.get(i), null);
        }
        
        return result;
    }

    private int parseInterpolationExpressionDirectly(String exprText, Token textToken) {
        ParserState savedState = getCurrentState();
        
        try {
            cod.lexer.MainLexer tempLexer = new cod.lexer.MainLexer(exprText, true);
            tempLexer.line = textToken.line;
            tempLexer.column = textToken.column + 1;
            
            List<Token> tokens = tempLexer.tokenize();
            
            if (tokens.isEmpty()) {
                return factory.createTextLiteral("", textToken);
            }
            
            ParserContext subCtx = new ParserContext(tokens);
            ExpressionParser subParser = new ExpressionParser(subCtx, factory, globalRegistry, statementParser);
            
            return subParser.parseExpr();
        } finally {
            setState(savedState);
        }
    }

    private boolean isThisKeyword() {
        Token current = now();
        if (is(current, THIS)) {
            return true;
        } else if (is(current, ID)) {
            if (is(next(), DOT)) {
                Token afterDot = next(2);
                if (is(afterDot, THIS)) {
                    return true;
                }
            }
        }
        return false;
    }

    private int parseThisExpr() {
        Token first = now();
        String className = null;
        
        if (is(first, ID) && is(next(), DOT) && is(next(2), THIS)) {
            
            Token classNameToken = expect(ID);
            className = classNameToken.text;
            expect(DOT);
            Token thisToken = expect(THIS);
            return factory.createThisExpr(className, thisToken);
        }
        
        Token thisToken = expect(THIS);
        return factory.createThisExpr(null, thisToken);
    }

    private Object resolveFloatLiteralValue(String literal) {
    // If it contains a decimal point, let the main valueOf handle it
    if (literal.contains(".")) {
        return null; // Let the fallback to AutoStackingNumber.valueOf() handle it
    }
    
    String baseValueStr;
    String suffix;
    int exponent = 0;
    
    // Handle Qi suffix
    if (literal.endsWith("Qi")) {
        suffix = "Qi";
        baseValueStr = literal.substring(0, literal.length() - 2);
        exponent = 18;
    } else {
        char lastChar = literal.charAt(literal.length() - 1);
        
        if (lastChar == 'K' || lastChar == 'M' || lastChar == 'B' || lastChar == 'T' || lastChar == 'Q') {
            suffix = String.valueOf(lastChar);
            baseValueStr = literal.substring(0, literal.length() - 1);
            
            if ("K".equals(suffix)) exponent = 3;
            else if ("M".equals(suffix)) exponent = 6;
            else if ("B".equals(suffix)) exponent = 9;
            else if ("T".equals(suffix)) exponent = 12;
            else if ("Q".equals(suffix)) exponent = 15;
        } else {
            // No suffix - let the main valueOf handle it
            return null;
        }
    }

    try {
        AutoStackingNumber base = AutoStackingNumber.valueOf(baseValueStr);
        AutoStackingNumber multiplier = AutoStackingNumber.fromLong(10).pow(exponent);
        return base.multiply(multiplier);
    } catch (Exception e) {
        return null;
    }
}

    private int parsePrecedence(int precedence) {
        int left = parsePrefix();
        
        while(true) {
            Token op = now();
            if (op == null) break;
            
            int opPrecedence = getPrecedence(op);
            
            if (opPrecedence < precedence) {
                break;
            }
            
            if (isComparisonOp(op) && isChainFollows(1)) {
                return parseEqualityChain(left, op.text);
            }
            
            if (opPrecedence > 0) {
                Token opToken = consume();
                int right = parsePrecedence(opPrecedence + 1);
                if (is(opToken, IS)) {
                    left = factory.createBinaryOp(left, IS.toString(), right, opToken);
                } else {
                    left = factory.createBinaryOp(left, op.text, right, opToken);
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

    private int parsePrefix() {
        Token current = now();
        if (current == null) {
            throw error("Unexpected end of input in prefix expression");
        }
        
        if (is(BANG, PLUS, MINUS)) {
            Token opToken = consume();
            int operand = parsePrecedence(PREC_UNARY);
            return factory.createUnaryOp(opToken.text, operand, opToken);
        }
        
        return parsePrimaryExpr();
    }

    private int parseBooleanChain() {
        Token typeToken = now();
        boolean isAll = is(typeToken, ALL);
        consume();

        if (is(ID)) {
            Token arrayNameToken = now();
            String arrayName = expect(ID).text;

            if (isComparisonOp(now())) {
                Token opToken = consume();
                int right = parsePrecedence(PREC_COMPARISON + 1);                

                List<Integer> chainArgs = new ArrayList<Integer>();
                chainArgs.add(right);

                int arrayExpr = factory.createIdentifier(arrayName, arrayNameToken);
                int chain = factory.createEqualityChain(arrayExpr, opToken.text, isAll, chainArgs, arrayNameToken, opToken, typeToken);
                return chain;
            } else {
                throw error("Expected comparison operator after 'all/any <arrayName>'");
            }
        } else if (is(LBRACKET)) {
            expect(LBRACKET);
            
            List<Integer> expressions = new ArrayList<Integer>();

            if (!is(RBRACKET)) {
                expressions.add(parseExpr());
                
    
                
                if (!is(COMMA, RBRACKET)) {
                    throw error("Boolean chain requires at least two expressions or a comma after the first expression.");
                }                 
                while (consume(COMMA)) {
        
                    expressions.add(parseExpr());
        
                }
            }
            expect(RBRACKET);
            
            int node = factory.createBooleanChain(isAll, expressions, typeToken);
            return node;
        } else {
            throw error("Expected array variable or '[' after 'all/any'");
        }
    }

    private int parseEqualityChain(int left, String operator) {
        Token leftToken = next(-1);
        
        Token operatorToken = now();
        consume();
        
        Token chainTypeToken = now();
        boolean isAllChain = is(chainTypeToken, ALL);
        
        if (!is(chainTypeToken, ALL, ANY)) {
            throw error("Expected 'all' or 'any' after comparison operator", chainTypeToken);
        }
        
        consume();
        
        List<Integer> chainArgs = new ArrayList<Integer>();
        
        if (is(LBRACKET)) {
            expect(LBRACKET);
            if (!is(RBRACKET)) {
                chainArgs.add(parseChainArgument());
                while (consume(COMMA)) {
                    chainArgs.add(parseChainArgument());
                }
            }
            expect(RBRACKET);
        } else if (is(ID)) {
            Token arrayNameToken = now();
            String arrayName = expect(ID).text;
            int arrayExpr = factory.createIdentifier(arrayName, arrayNameToken);
            chainArgs.add(arrayExpr);
        } else {
            throw error("Expected array variable or '[' for array literal after 'all/any'");
        }
        
        int chain = factory.createEqualityChain(left, operator, isAllChain, chainArgs, leftToken, operatorToken, chainTypeToken);
        return chain;
    }

    private int parseChainArgument() {
        if (is(BANG)) {
            Token bangToken = expect(BANG);
            int arg = parsePrimaryExpr();
            return factory.createUnaryOp("!", arg, bangToken);
        }
        
        if (is(LPAREN)) {
            return parseArgumentList();
        }
        
        if (is(ID) && is(next(), RBRACKET)) {
            Token idToken = now();
            throw error("Redundant brackets around array variable '" + idToken.text + 
                       "'. Use 'any " + idToken.text + "' instead of 'any[" + idToken.text + "]'", idToken);
        }
        
        return parsePrimaryExpr();
    }

    private int parseArgumentList() {
        Token lparenToken = expect(LPAREN);
        List<Integer> arguments = new ArrayList<Integer>();
        if (!is(RPAREN)) {
            arguments.add(parseExpr());
            while (consume(COMMA)) {
                arguments.add(parseExpr());
            }
        }
        expect(RPAREN);
        return factory.createArgumentList(arguments, lparenToken);
    }

    private int parseArrayLiteral() {
        Token lbracketToken = expect(LBRACKET);
        
        List<Integer> elements = new ArrayList<Integer>();
        
        if (!is(RBRACKET)) {
            if (isRangeStart()) {
                elements.add(parseRangeExpression());
            } else {
                elements.add(parseExpr());
            }
            
            while (consume(COMMA)) {
    
                
                if (isRangeStart()) {
                    elements.add(parseRangeExpression());
                } else {
                    elements.add(parseExpr());
                }
            }
        }
        
        expect(RBRACKET);
        return factory.createArray(elements, lbracketToken);
    }

    private boolean isRangeStart() {
        save();
        try {

            
            if (!isExprStart(now())) return false;
            parseExpr();

            
            if (is(RANGE_DOTDOT) || is(TO)) return true;
            
            return false;
        } catch (ParseError e) {
            return false;
        } finally {
            restore();
        }
    }

    private int parseRangeExpression() {
        int stepId = cod.ast.FlatAST.NULL;
        Token stepToken = null;
        Token rangeToken = null;
        
        int startId = parseExpr();
       
        
        if (is(RANGE_DOTDOT)) {
            rangeToken = expect(RANGE_DOTDOT);
        } else if (is(TO)) {
            rangeToken = expect(TO);
        } else {
            throw error("Expected range operator '..' or 'to'");
        }
        
       
        int endId = parseExpr();
       
        
        if (is(BY)) {
            stepToken = expect(BY);
            stepId = parseExpr();

        } else if (is(RANGE_HASH)) {
            stepToken = expect(RANGE_HASH);
            stepId = parseExpr();

        }
        
        return factory.createRange(stepId, startId, endId, stepToken, rangeToken);
    }

    private int parseTypeCast() {
        Token lparenToken = expect(LPAREN);
        String type = parseTypeReference();    
        expect(RPAREN);
        int expressionToCast = parsePrecedence(PREC_UNARY);
        return factory.createTypeCast(type, expressionToCast, lparenToken);
    }

    private boolean isConstructorCall() {
        return attempt(new ParserAction<Boolean>() {
            @Override
            public Boolean parse() throws ParseError {
                Token first = now();
                if (!is(first, ID)) return false;
                
                String idName = first.text;
                
                if (idName.length() == 0 || Character.isLowerCase(idName.charAt(0))) {
                    return false;
                }
                
                int pos = 1;
                while (is(next(pos), WS, LINE_COMMENT, BLOCK_COMMENT)) {
                    pos++;
                }
                
                return is(next(pos), LPAREN);
            }
        });
    }

    private boolean isMethodCallFollows() {
        return attempt(new ParserAction<Boolean>() {
            @Override
            public Boolean parse() throws ParseError {
                Token first = now();
                
                boolean isValidName = (is(first, ID) || canBeMethod(first));
                
                if (!isValidName) return false;
                
                if (isConstructorCall()) {
                    return false;
                }
                
                int pos = 1;
                while (is(next(pos), DOT)) {
                    pos++;
                    Token afterDot = next(pos);
                    
                    if (!(is(afterDot, ID) ||  canBeMethod(afterDot))) {
                        return false;
                    }
                    pos++;
                }
                
                Token afterDots = next(pos);
                return is(afterDots, LPAREN);
            }
        });
    }

    private boolean isSlotAccessExpression() {
        if (!is(next(0), LBRACKET)) return false;

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
            return is(t, COLON);
        }
        return false;
    }

    private boolean isTypeCast() {
        if (!is(next(0), LPAREN)) return false;
        
        Token second = next();
        if (!isTypeStart(second)) return false;
        
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
                        return isExprStart(afterParen);
                    }
                    return false;
                }
            }
            pos++;
        }
        return false;
    }

    private boolean isIllegalTypeToken(Token t) {
        if (is(t, INT_LIT, FLOAT_LIT, TEXT_LIT, BOOL_LIT)) return true;
        
        if (is(t, PLUS, MINUS, MUL, DIV, MOD, EQ, NEQ, GT, LT, GTE, LTE)) return true;
        
        return false;
    }

    private int getPrecedence(Token token) {
        if (nil(token)) return 0;
        
        if (is(token, IS)) return PREC_IS;
        
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
        Token next = next(offset);
        if (next == null) return false;
        
        if (is(next, ALL, ANY)) {
            Token after = next(offset + 1);
            return is(after, LBRACKET) || is(after, ID);
        }
        return false;
    }

    private boolean isComparisonOp(Token t) {
        if (t == null) return false;
        return is(t, EQ, NEQ, GT, LT, GTE, LTE);
    }
    
    public GlobalRegistry getGlobalRegistry() {
        return globalRegistry;
    }
}