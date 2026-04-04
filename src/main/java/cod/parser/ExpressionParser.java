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
    private static final int PREC_CHAIN = 55;
    private static final int PREC_COMPARISON = 60;
    private static final int PREC_TERM = 70;
    private static final int PREC_FACTOR = 80;
    private static final int PREC_UNARY = 90;
    private static final int PREC_CALL = 100;
    private static final int PREC_IS = 40;
    
    private final GlobalRegistry globalRegistry;
    private Set<String> globalFunctionNames;
    private StatementParser statementParser;
    private int bareInferredLambdaDisabledDepth = 0;
    
    public ExpressionParser(ParserContext ctx) {
        this(ctx, null, null);
    }
    
    public ExpressionParser(ParserContext ctx, GlobalRegistry globalRegistry) {
        this(ctx, globalRegistry, null);
    }
    
    public ExpressionParser(ParserContext ctx, GlobalRegistry globalRegistry, StatementParser statementParser) {
        super(ctx);
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
        return new ExpressionParser(isolatedCtx, this.globalRegistry, this.statementParser);
    }

    public ExprNode parseExpr() {
        return attempt(new ParserAction<ExprNode>() {
            @Override
            public ExprNode parse() throws ParseError {
                if (bareInferredLambdaDisabledDepth == 0) {
                    ExprNode inferredLambdaExpr = tryParseBareInferredLambdaExpression();
                    if (inferredLambdaExpr != null) {
                        return inferredLambdaExpr;
                    }
                }
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

    private ExprNode parseConstructorCall() {
        Token classNameToken = now();
        String className = expect(ID).getText();  // Lazy allocation
        expect(LPAREN);

        List<ExprNode> args = new ArrayList<ExprNode>();
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
        
        ConstructorCallNode call = ASTFactory.createConstructorCall(className, args, classNameToken);
        call.argNames = argNames;
        return call;
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

    private void parseNamedArgumentList(List<ExprNode> args, List<String> argNames) {
        do {
            String argName = expect(ID).getText();  // Lazy allocation
            
            expect(COLON);
            ExprNode value = parseExpr();
            
            args.add(value);
            argNames.add(argName);
            
            if (!is(COMMA)) {
                break;
            }
            expect(COMMA);
        } while (!is(RPAREN));
    }

    public MethodCallNode parseMethodCall() {
    return attempt(new ParserAction<MethodCallNode>() {
        @Override
        public MethodCallNode parse() throws ParseError {
            Token nameStartToken = now();
            String qualifiedNameStr = parseQualifiedNameOrKeyword();
            String methodName = qualifiedNameStr;
            if (qualifiedNameStr.contains(".")) {
                methodName = qualifiedNameStr.substring(qualifiedNameStr.lastIndexOf('.') + 1);
            }
            
            MethodCallNode call = ASTFactory.createMethodCall(methodName, qualifiedNameStr, nameStartToken);
            
            if (!qualifiedNameStr.contains(".") && globalFunctionNames != null && 
                globalFunctionNames.contains(methodName)) {
                call.isGlobal = true;
            }
            
            expect(LPAREN);
            
            if (!is(RPAREN)) {
                if (isNamedArgument()) {
                    parseNamedArgumentList(call.arguments, call.argNames);
                } else {
                    call.arguments.add(parseExpr());
                    call.argNames.add(null);
                    
                    while (consume(COMMA)) {
                        call.arguments.add(parseExpr());
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
        Token superToken = now();
        expect(SUPER);
        
        expect(DOT);
        
        Token methodToken = now();
        String methodName;
        
        if (is(methodToken, ID)) {
            methodName = expect(ID).getText();  // Lazy allocation
        } else if (canBeMethod(methodToken)) {
            methodName = expect(KEYWORD).getText();  // Lazy allocation
        } else {
            throw error("Expected method name after 'super.'", methodToken);
        }
        
        MethodCallNode call = ASTFactory.createMethodCall(methodName, "super." + methodName, superToken);
        call.isSuperCall = true;
        call.isGlobal = false;
        
        expect(LPAREN);
        
        if (!is(RPAREN)) {
            if (isNamedArgument()) {
                parseNamedArgumentList(call.arguments, call.argNames);
            } else {
                call.arguments.add(parseExpr());
                call.argNames.add(null);
                while (consume(COMMA)) {
                    call.arguments.add(parseExpr());
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
            String name = expect(KEYWORD).getText();  // Lazy allocation
            
            if (consume(DOT)) {
                StringBuilder fullName = new StringBuilder(name);
                fullName.append(".");
                fullName.append(expect(ID).getText());  // Lazy allocation
                
                while (consume(DOT)) {
                    fullName.append(".");
                    fullName.append(expect(ID).getText());  // Lazy allocation
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

    public IndexAccessNode parseIndexAccessContinuation(ExprNode arrayExpr) {
        Token lbracketToken = expect(LBRACKET);
        
        ExprNode indexExpr;
        
        if (isRangeIndex()) {
            indexExpr = parseRangeIndex();
        } else {
            indexExpr = parseExpr();
            if (consume(COMMA)) {
                List<ExprNode> indices = new ArrayList<ExprNode>();
                indices.add(indexExpr);
                indices.add(parseExpr());
                while (consume(COMMA)) {
                    indices.add(parseExpr());
                }
                expect(RBRACKET);
                indexExpr = ASTFactory.createTuple(indices, lbracketToken);
            } else {
                expect(RBRACKET);
            }
            return ASTFactory.createIndexAccess(arrayExpr, indexExpr, lbracketToken);
        }
        
        return ASTFactory.createIndexAccess(arrayExpr, indexExpr, lbracketToken);
    }

    public ExprNode parseRangeIndex() {
        List<RangeIndexNode> ranges = new ArrayList<RangeIndexNode>();
        
        ranges.add(parseSingleRangeIndex());
        
        while (is(COMMA)) {
            expect(COMMA);
            ranges.add(parseSingleRangeIndex());
        }
        
        expect(RBRACKET);
        
        if (ranges.size() == 1) {
            return ranges.get(0);
        } else {
            return ASTFactory.createMultiRangeIndex(ranges, null);
        }
    }

    private RangeIndexNode parseSingleRangeIndex() {
        ExprNode step = null;
        ExprNode start;
        ExprNode end;
        Token stepToken = null;
        Token rangeToken = null;
        
        start = parseExpr();
        
        if (is(RANGE_DOTDOT)) {
            rangeToken = expect(RANGE_DOTDOT);
        } else if (is(TO)) {
            rangeToken = expect(TO);
        } else {
            throw error("Expected range operator '..' or 'to'");
        }
        
        end = parseExpr();
        
        if (is(RANGE_HASH)) {
            stepToken = expect(RANGE_HASH);
            step = parseExpr();
        } else if (is(BY)) {
            stepToken = expect(BY);
            step = parseExpr();
        }
        
        return ASTFactory.createRangeIndex(step, start, end, stepToken, rangeToken);
    }

    public List<String> parseReturnSlots() {
        expect(LBRACKET);
        List<String> slots = new ArrayList<String>();
        do {
            if (is(ID)) {
                slots.add(expect(ID).getText());  // Lazy allocation
            } else if (is(INT_LIT)) {
                slots.add(expect(INT_LIT).getText());  // Lazy allocation
            } else {
                throw error("Expected slot name or index", now());
            }
        } while (consume(COMMA));
        expect(RBRACKET);
        return slots;
    }
    
    private ExprNode parseIfExpr() {
        Token ifToken = expect(IF);
        ExprNode condition = parseExpr();
        
        ExprNode thenExpr;
        if (is(LBRACE)) {
            expect(LBRACE);
            thenExpr = parseExpr();
            expect(RBRACE);
        } else {
            thenExpr = parseExpr();
        }
        
        Token elseToken = expect(ELSE);
        
        ExprNode elseExpr;
        if (is(LBRACE)) {
            expect(LBRACE);
            elseExpr = parseExpr();
            expect(RBRACE);
        } else {
            elseExpr = parseExpr();
        }
        
        return ASTFactory.createIfExpr(condition, thenExpr, elseExpr, ifToken, elseToken);
    }

    public boolean isLambdaExpression() {
        return attempt(new ParserAction<Boolean>() {
            @Override
            public Boolean parse() throws ParseError {
                return is(LAMBDA);
            }
        });
    }

    public LambdaNode parseLambdaSignature() {
        Token lambdaToken = expect(LAMBDA);
        
        SlotParser slotParser = new SlotParser(this);
        
        expect(LPAREN);
        LambdaParamsParseResult lambdaParams = parseLambdaParameters();
        List<ParamNode> parameters = lambdaParams.parameters;
        
        expect(RPAREN);
        
        // Expression-body lambda, e.g. \() $left + $right
        if (!is(DOUBLE_COLON) && !is(TILDE_ARROW) && !is(LBRACE)) {
            LambdaNode lambda = ASTFactory.createLambda(parameters, null, null, lambdaToken);
            lambda.inferParameters = lambdaParams.inferParameters;
            lambda.expressionBody = parseExprWithoutBareInferredLambda();
            return lambda;
        }
        
        // Parse optional return contract (::)
        List<SlotNode> returnSlots = null;
        if (is(DOUBLE_COLON)) {
            returnSlots = slotParser.parseSlotContract();
        }
        
        // Parse lambda body
        StmtNode body;
        Token tildeArrowToken = null;
        
        if (is(LBRACE)) {
            // Block body - requires statementParser
            if (statementParser == null) {
                throw error("Internal error: statementParser not available for lambda block");
            }
            
            expect(LBRACE);
            BlockNode block = new BlockNode();
            
            while (!is(RBRACE) && !is(EOF)) {
                block.statements.add(statementParser.parseStmt());
            }
            expect(RBRACE);
            body = block;
        } else if (is(TILDE_ARROW)) {
            tildeArrowToken = expect(TILDE_ARROW);
            
            if (is(LBRACE)) {
                // Optional braces around expression(s)
                expect(LBRACE);
                List<SlotAssignmentNode> assignments = slotParser.parseSlotAssignments();
                expect(RBRACE);
                
                // Validate against contract if present
                if (returnSlots != null) {
                    slotParser.validateSlotCount(returnSlots, assignments, tildeArrowToken);
                }
                
                // Wrap in BlockNode
                BlockNode block = new BlockNode();
                if (assignments.size() == 1) {
                    block.statements.add(assignments.get(0));
                } else {
                    block.statements.add(ASTFactory.createMultipleSlotAsmt(assignments, tildeArrowToken));
                }
                body = block;
            } else {
                // Direct expression(s) without braces
                List<SlotAssignmentNode> assignments = slotParser.parseSlotAssignments();
                
                // Validate against contract if present
                if (returnSlots != null) {
                    slotParser.validateSlotCount(returnSlots, assignments, tildeArrowToken);
                }
                
                // Wrap in BlockNode
                BlockNode block = new BlockNode();
                if (assignments.size() == 1) {
                    block.statements.add(assignments.get(0));
                } else {
                    block.statements.add(ASTFactory.createMultipleSlotAsmt(assignments, tildeArrowToken));
                }
                body = block;
            }
        } else {
            // Error: missing ~> or {
            throw error(
                "Expected '~>' or '{' after lambda parameters" +
                (returnSlots != null ? " (return contract requires ~> assignments)" : ""),
                now()
            );
        }
        
        LambdaNode lambda = ASTFactory.createLambda(parameters, returnSlots, body, lambdaToken);
        lambda.inferParameters = lambdaParams.inferParameters;
        return lambda;
    }

    private LambdaParamsParseResult parseLambdaParameters() {
        List<ParamNode> parameters = new ArrayList<ParamNode>();
        boolean inferParameters = false;

        if (!is(RPAREN)) {
            Token current = now();
            boolean underscoreInferMarker =
                (is(UNDERSCORE) || (is(current, ID) && "_".equals(current.getText())))
                    && is(next(), RPAREN);
            if (underscoreInferMarker) {
                consume();
                inferParameters = true;
            } else {
                parameters.add(parseLambdaParameter());
                while (consume(COMMA)) {
                    parameters.add(parseLambdaParameter());
                }
            }
        }

        return new LambdaParamsParseResult(parameters, inferParameters);
    }

    private ExprNode tryParseBareInferredLambdaExpression() {
        return attempt(new ParserAction<ExprNode>() {
            @Override
            public ExprNode parse() throws ParseError {
                if (!is(DOLLAR)) return null;
                Token lambdaToken = now();
                ExprNode expressionBody = parsePrecedence(PREC_ASSIGNMENT);
                if (!containsPlaceholderIdentifier(expressionBody)) {
                    return null;
                }
                LambdaNode lambda = ASTFactory.createLambda(new ArrayList<ParamNode>(), null, null, lambdaToken);
                lambda.inferParameters = true;
                lambda.expressionBody = expressionBody;
                return lambda;
            }
        });
    }

    private ExprNode parseExprWithoutBareInferredLambda() {
        bareInferredLambdaDisabledDepth++;
        try {
            return parseExpr();
        } finally {
            bareInferredLambdaDisabledDepth--;
        }
    }

    private boolean containsPlaceholderIdentifier(ASTNode node) {
        if (node == null) return false;

        if (node instanceof IdentifierNode) {
            String name = ((IdentifierNode) node).name;
            return name != null && name.startsWith("$") && name.length() > 1;
        }
        if (node instanceof LambdaNode) {
            return false;
        }
        if (node instanceof BinaryOpNode) {
            BinaryOpNode n = (BinaryOpNode) node;
            return containsPlaceholderIdentifier(n.left) || containsPlaceholderIdentifier(n.right);
        }
        if (node instanceof UnaryNode) {
            return containsPlaceholderIdentifier(((UnaryNode) node).operand);
        }
        if (node instanceof TypeCastNode) {
            return containsPlaceholderIdentifier(((TypeCastNode) node).expression);
        }
        if (node instanceof MethodCallNode) {
            MethodCallNode n = (MethodCallNode) node;
            if (n.target != null && containsPlaceholderIdentifier(n.target)) return true;
            if (n.arguments != null) {
                for (ExprNode arg : n.arguments) {
                    if (containsPlaceholderIdentifier(arg)) return true;
                }
            }
            return false;
        }
        if (node instanceof PropertyAccessNode) {
            PropertyAccessNode n = (PropertyAccessNode) node;
            return containsPlaceholderIdentifier(n.left) || containsPlaceholderIdentifier(n.right);
        }
        if (node instanceof IndexAccessNode) {
            IndexAccessNode n = (IndexAccessNode) node;
            return containsPlaceholderIdentifier(n.array) || containsPlaceholderIdentifier(n.index);
        }
        if (node instanceof ArrayNode) {
            ArrayNode n = (ArrayNode) node;
            if (n.elements != null) {
                for (ExprNode expr : n.elements) {
                    if (containsPlaceholderIdentifier(expr)) return true;
                }
            }
            return false;
        }
        if (node instanceof TupleNode) {
            TupleNode n = (TupleNode) node;
            if (n.elements != null) {
                for (ExprNode expr : n.elements) {
                    if (containsPlaceholderIdentifier(expr)) return true;
                }
            }
            return false;
        }
        if (node instanceof ExprIfNode) {
            ExprIfNode n = (ExprIfNode) node;
            return containsPlaceholderIdentifier(n.condition)
                || containsPlaceholderIdentifier(n.thenExpr)
                || containsPlaceholderIdentifier(n.elseExpr);
        }
        if (node instanceof BooleanChainNode) {
            BooleanChainNode n = (BooleanChainNode) node;
            if (n.expressions != null) {
                for (ExprNode expr : n.expressions) {
                    if (containsPlaceholderIdentifier(expr)) return true;
                }
            }
            return false;
        }
        if (node instanceof EqualityChainNode) {
            EqualityChainNode n = (EqualityChainNode) node;
            if (containsPlaceholderIdentifier(n.left)) return true;
            if (n.chainArguments != null) {
                for (ExprNode expr : n.chainArguments) {
                    if (containsPlaceholderIdentifier(expr)) return true;
                }
            }
            return false;
        }
        if (node instanceof ChainedComparisonNode) {
            ChainedComparisonNode n = (ChainedComparisonNode) node;
            if (n.expressions != null) {
                for (ExprNode expr : n.expressions) {
                    if (containsPlaceholderIdentifier(expr)) return true;
                }
            }
            return false;
        }
        if (node instanceof ValueExprNode) {
            Object value = ((ValueExprNode) node).getValue();
            return value instanceof ASTNode && containsPlaceholderIdentifier((ASTNode) value);
        }
        return false;
    }

    private static final class LambdaParamsParseResult {
        private final List<ParamNode> parameters;
        private final boolean inferParameters;

        private LambdaParamsParseResult(List<ParamNode> parameters, boolean inferParameters) {
            this.parameters = parameters;
            this.inferParameters = inferParameters;
        }
    }

    private ParamNode parseLambdaParameter() {
        Token paramToken = now();
        
        // Handle tuple destructuring
        if (is(LPAREN)) {
            expect(LPAREN);
            List<String> tupleElements = new ArrayList<String>();
            
            if (!is(RPAREN)) {
                tupleElements.add(expect(ID).getText());  // Lazy allocation
                while (consume(COMMA)) {
                    tupleElements.add(expect(ID).getText());  // Lazy allocation
                }
            }
            expect(RPAREN);
            
            // Create parameter with tuple destructuring info
            ParamNode param = ASTFactory.createParam("_tuple", null, null, true, paramToken);
            param.isTupleDestructuring = true;
            param.tupleElements = tupleElements;
            param.isLambdaParameter = true;
            return param;
        }
        
        // Regular parameter
        String paramName = expect(ID).getText();  // Lazy allocation
        
        // Optional type annotation
        String paramType = null;
        if (consume(COLON)) {
            paramType = parseTypeReference();
        }
        
        // Optional default value
        ExprNode defaultValue = null;
        if (consume(ASSIGN)) {
            defaultValue = parseExpr();
        }
        
        boolean typeInferred = (paramType == null);
        ParamNode param = ASTFactory.createParam(
            paramName, paramType, defaultValue, typeInferred, paramToken
        );
        param.isLambdaParameter = true;
        param.hasDefaultValue = (defaultValue != null);
        
        return param;
    }

    public ExprNode parsePrimaryExpr() {
    return attempt(new ParserAction<ExprNode>() {
        @Override
        public ExprNode parse() throws ParseError {
            ExprNode baseExpr;
            Token startToken = now();
            
            if (startToken == null) {
                throw error("Unexpected end of input in primary expression");
            }
            
            if (is(LAMBDA)) {
                baseExpr = parseLambdaSignature();
            }
            else if (isSuperMethodCall()) {
                baseExpr = parseSuperMethodCall();
            }
            else if (is(SUPER)) {
                Token superToken = expect(SUPER);
                baseExpr = ASTFactory.createSuperExpr(superToken);
            }
            else if (isThisKeyword()) {
                baseExpr = parseThisExpr();
            }
            else if (isConstructorCall() && !isMethodCallFollows()) {
                baseExpr = parseConstructorCall();
            }
            else if (is(LBRACKET)) {
                if (isSlotAccessExpression()) {
                    List<String> slotNames = parseReturnSlots();
                    expect(COLON);
                    MethodCallNode methodCall = parseMethodCall();
                    methodCall.slotNames = slotNames;
                    baseExpr = methodCall;
                } else {
                    baseExpr = parseArrayLiteral();
                }
            }
            else if (isMethodCallFollows()) {
                MethodCallNode methodCall = parseMethodCall();
                baseExpr = methodCall;
            }
            else if (is(IF)) {
                baseExpr = parseIfExpr();
            }
            else if (is(INT_LIT)) {
                Token intToken = expect(INT_LIT);
                String intText = intToken.getText();
                try {
                    int intValue = Integer.parseInt(intText);
                    baseExpr = ASTFactory.createIntLiteral(intValue, intToken);
                } catch (NumberFormatException e1) {
                    try {
                        long longValue = Long.parseLong(intText);
                        baseExpr = ASTFactory.createLongLiteral(longValue, intToken);
                    } catch (NumberFormatException e2) {
                        AutoStackingNumber bigValue = AutoStackingNumber.valueOf(intText);
                        baseExpr = ASTFactory.createFloatLiteral(bigValue, intToken);
                    }
                }
            }
            else if (is(FLOAT_LIT)) {
                Token floatToken = expect(FLOAT_LIT);
                String floatText = floatToken.getText();
                
                Object resolvedValue = resolveFloatLiteralValue(floatText);
                
                if (resolvedValue instanceof AutoStackingNumber) {
                    baseExpr = ASTFactory.createFloatLiteral((AutoStackingNumber)resolvedValue, floatToken);
                } else {
                    try {
                        AutoStackingNumber value = AutoStackingNumber.valueOf(floatText);
                        baseExpr = ASTFactory.createFloatLiteral(value, floatToken);
                    } catch (NumberFormatException e) {
                        throw error("Invalid numeric literal: " + floatText, floatToken);
                    }
                }
            }
            else if (is(TEXT_LIT, INTERPOL)) {
                Token textToken = now();
                
                if (textToken.type == INTERPOL && textToken.hasChildTokens()) {
                    List<ExprNode> parts = new ArrayList<ExprNode>();
                    
                    for (Token part : textToken.childTokens) {
                        if (part.type == TEXT_LIT) {
                            // Text parts are already unquoted by lexer
                            parts.add(ASTFactory.createTextLiteral(part.getText(), part));
                        } else if (part.type == INTERPOL) {
                            if (part.hasChildTokens()) {
                                ParserContext subCtx = new ParserContext(part.childTokens);
                                ExpressionParser subParser = new ExpressionParser(subCtx, globalRegistry, statementParser);
                                ExprNode expr = subParser.parseExpr();
                                parts.add(expr);
                            }
                        }
                    }
                    
                    if (parts.isEmpty()) {
                        baseExpr = ASTFactory.createTextLiteral("", textToken);
                    } else if (parts.size() == 1) {
                        baseExpr = parts.get(0);
                    } else {
                        baseExpr = parts.get(0);
                        for (int i = 1; i < parts.size(); i++) {
                            baseExpr = ASTFactory.createBinaryOp(baseExpr, "+", parts.get(i), textToken);
                        }
                    }
                } else {
                    // Simple text literal - lexer already stripped quotes
                    String text = textToken.getText();
                    
                    if (text.startsWith("|\"") && text.endsWith("\"|")) {
                        baseExpr = handleMultilineTextInterpolation(textToken);
                    } else {
                        baseExpr = ASTFactory.createTextLiteral(text, textToken);
                    }
                }
                consume();
            }
            else if (is(TRUE)) {
                Token trueToken = expect(TRUE);
                baseExpr = ASTFactory.createBoolLiteral(true, trueToken);
            }
            else if (is(FALSE)) {
                Token falseToken = expect(FALSE);
                baseExpr = ASTFactory.createBoolLiteral(false, falseToken);
            }
            else if (is(NONE)) {
                Token noneToken = expect(NONE);
                baseExpr = ASTFactory.createNoneLiteral(noneToken);
            }
            else if (is(INT, TEXT, FLOAT, BOOL, TYPE)) {
                Token typeToken = now();
                String typeName = expect(KEYWORD).getText();
                baseExpr = ASTFactory.createTextLiteral(typeName, typeToken);
            }
            else if (is(ID) || canBeMethod(now())) {
                if (isMethodCallFollows()) {
                    baseExpr = parseMethodCall();
                } else {
                    Token idToken = now();
                    String idName;
                    if (is(idToken, KEYWORD)) {
                        idName = expect(KEYWORD).getText();
                    } else {
                        idName = expect(ID).getText();
                    }
                    baseExpr = ASTFactory.createIdentifier(idName, idToken);
                }
            }
            else if (is(DOLLAR)) {
                Token dollarToken = expect(DOLLAR);
                Token nameToken = expect(ID);
                baseExpr = ASTFactory.createIdentifier("$" + nameToken.getText(), dollarToken);
            }
            else if (is(LPAREN)) {
                if (isTypeCast()) {
                    baseExpr = parseTypeCast();
                } else {
                    Token lparenToken = expect(LPAREN);
                    ExprNode firstExpr = parseExpr();
                    
                    if (is(COMMA)) {
                        List<ExprNode> elements = new ArrayList<ExprNode>();
                        elements.add(firstExpr);
                        
                        while (consume(COMMA)) {
                            elements.add(parseExpr());
                        }
                        
                        if (elements.size() == 1 && !is(RPAREN)) {
                            throw error("Expected expression after comma in tuple");
                        }
                        
                        expect(RPAREN);
                        baseExpr = ASTFactory.createTuple(elements, lparenToken);
                    } else {
                        expect(RPAREN);
                        baseExpr = firstExpr;
                    }
                }
            }
            else {
                throw error("Unexpected token in primary expression: " + startToken.getText() +
                    " (" + getTypeName(startToken.type) + ")", startToken);
            }

            while (is(DOT)) {
                Token dotToken = expect(DOT);
                
                ExprNode property = parsePrimaryExpr();
                
                baseExpr = ASTFactory.createPropertyAccess(baseExpr, property, dotToken);
            }

            while (is(LBRACKET)) {
                baseExpr = parseIndexAccessContinuation(baseExpr);
            }

            return baseExpr;
        }
    });
}

    private ExprNode handleMultilineTextInterpolation(Token token) {
        String fullText = token.getText();  // Lazy allocation
        
        if (!fullText.startsWith("|\"") || !fullText.endsWith("\"|")) {
            return ASTFactory.createTextLiteral(fullText, token);
        }
        
        if (token.hasChildTokens()) {
            return handleInterpolatedTextWithTokens(token);
        }
        
        return parseInterpolatedText(token);
    }

    private ExprNode handleInterpolatedTextWithTokens(Token token) {
        List<Token> exprTokens = token.childTokens;
        
        if (exprTokens == null || exprTokens.isEmpty()) {
            return ASTFactory.createTextLiteral(token.getText(), token);
        }
        
        List<ExprNode> parts = new ArrayList<ExprNode>();
        
        String fullText = token.getText();
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
                        parts.add(ASTFactory.createTextLiteral(currentText.toString(), token));
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
                            ExprNode expr = parsePreTokenizedInterpolation(exprToken);
                            parts.add(expr);
                        } else {
                            parts.add(ASTFactory.createIdentifier(exprToken.getText(), exprToken));
                        }
                    } else {
                        parts.add(ASTFactory.createTextLiteral("", token));
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
            parts.add(ASTFactory.createTextLiteral(currentText.toString(), token));
        }
        
        if (parts.isEmpty()) {
            return ASTFactory.createTextLiteral("", token);
        } else if (parts.size() == 1) {
            return parts.get(0);
        }
        
        ExprNode result = parts.get(0);
        for (int i = 1; i < parts.size(); i++) {
            result = ASTFactory.createBinaryOp(result, "+", parts.get(i), token);
        }
        
        return result;
    }

    private ExprNode parsePreTokenizedInterpolation(Token token) {
        List<Token> exprTokens = token.childTokens;
        
        if (exprTokens == null || exprTokens.isEmpty()) {
            throw error("Interpolation token has no child tokens", token);
        }
        
        ParserContext subCtx = new ParserContext(exprTokens);
        ExpressionParser subParser = new ExpressionParser(subCtx, globalRegistry, statementParser);
        
        ExprNode result = subParser.parseExpr();
        
        if (!subParser.ctx.atEOF()) {
            throw error("Extra tokens in interpolation expression", token);
        }
        
        return result;
    }

    private ExprNode parseInterpolatedText(Token token) {
        String text = token.getText();  // Lazy allocation
        
        if (text.startsWith("\"") && text.endsWith("\"")) {
            text = text.substring(1, text.length() - 1);
        } else if (text.startsWith("|\"") && text.endsWith("\"|")) {
            text = text.substring(2, text.length() - 2);
        }
        
        List<ExprNode> parts = new ArrayList<ExprNode>();
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
                    parts.add(ASTFactory.createTextLiteral(current.toString(), token));
                    current.setLength(0);
                }
                
                int end = text.indexOf('}', pos + 1);
                if (end == -1) {
                    throw error("Unclosed interpolation in text", token);
                }
                
                String exprText = text.substring(pos + 1, end);
                
                ExprNode expr = parseInterpolationExpressionDirectly(exprText, token);
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
            parts.add(ASTFactory.createTextLiteral(current.toString(), token));
        }
        
        if (parts.isEmpty()) {
            return ASTFactory.createTextLiteral("", token);
        } else if (parts.size() == 1) {
            return parts.get(0);
        }
        
        ExprNode result = parts.get(0);
        for (int i = 1; i < parts.size(); i++) {
            result = ASTFactory.createBinaryOp(result, "+", parts.get(i), null);
        }
        
        return result;
    }

    private ExprNode parseInterpolationExpressionDirectly(String exprText, Token textToken) {
        ParserState savedState = getCurrentState();
        
        try {
            cod.lexer.MainLexer tempLexer = new cod.lexer.MainLexer(exprText, true);
            tempLexer.line = textToken.line;
            tempLexer.column = textToken.column + 1;
            
            List<Token> tokens = tempLexer.tokenize();
            
            if (tokens.isEmpty()) {
                return ASTFactory.createTextLiteral("", textToken);
            }
            
            ParserContext subCtx = new ParserContext(tokens);
            ExpressionParser subParser = new ExpressionParser(subCtx, globalRegistry, statementParser);
            
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

    private ExprNode parseThisExpr() {
        Token first = now();
        String className = null;
        
        if (is(first, ID) && is(next(), DOT) && is(next(2), THIS)) {
            Token classNameToken = expect(ID);
            className = classNameToken.getText();  // Lazy allocation
            expect(DOT);
            Token thisToken = expect(THIS);
            return ASTFactory.createThisExpr(className, thisToken);
        }
        
        Token thisToken = expect(THIS);
        return ASTFactory.createThisExpr(null, thisToken);
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

    private ExprNode parsePrecedence(int precedence) {
        ExprNode left = parsePrefix();
        
        while(true) {
            Token op = now();
            if (op == null) break;
            
            int opPrecedence = getPrecedence(op);
            
            if (opPrecedence < precedence) {
                break;
            }
            
            // Handle chained comparisons
            if (opPrecedence == PREC_CHAIN) {
                return parseComparisonChain(left);
            }
            
            if (isComparisonOp(op) && isChainFollows(1)) {
                return parseEqualityChain(left, op.getText());
            }
            
            if (opPrecedence > 0) {
                Token opToken = consume();
                ExprNode right = parsePrecedence(opPrecedence + 1);
                if (is(opToken, IS)) {
                    left = ASTFactory.createBinaryOp(left, IS.toString(), right, opToken);
                } else {
                    left = ASTFactory.createBinaryOp(left, op.getText(), right, opToken);
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

    private ExprNode parseComparisonChain(ExprNode first) {
        List<ExprNode> expressions = new ArrayList<ExprNode>();
        List<String> operators = new ArrayList<String>();
        Token firstToken = now();
        
        expressions.add(first);
        
        while (true) {
            Token opToken = now();
            if (opToken == null) break;
            
            // Check if it's a comparison operator
            if (!isComparisonOp(opToken)) {
                break;
            }
            
            operators.add(opToken.getText());  // Lazy allocation
            consume(); // consume operator
            
            // Parse the next expression with lower precedence to avoid right-associativity
            ExprNode nextExpr = parsePrecedence(PREC_CHAIN - 1);
            expressions.add(nextExpr);
            
            // Look ahead - if next token is another comparison, continue the chain
            Token nextToken = now();
            if (!isComparisonOp(nextToken)) {
                break;
            }
        }
        
        return ASTFactory.createChainedComparison(expressions, operators, firstToken);
    }

    private ExprNode parsePrefix() {
        Token current = now();
        if (current == null) {
            throw error("Unexpected end of input in prefix expression");
        }
        
        if (is(BANG, PLUS, MINUS)) {
            Token opToken = consume();
            ExprNode operand = parsePrecedence(PREC_UNARY);
            return ASTFactory.createUnaryOp(opToken.getText(), operand, opToken);
        }
        
        return parsePrimaryExpr();
    }

    private ExprNode parseBooleanChain() {
        Token typeToken = now();
        boolean isAll = is(typeToken, ALL);
        consume();

        if (is(ID)) {
            Token arrayNameToken = now();
            String arrayName = expect(ID).getText();  // Lazy allocation

            if (isComparisonOp(now())) {
                Token opToken = consume();
                ExprNode right = parsePrecedence(PREC_COMPARISON + 1);                

                List<ExprNode> chainArgs = new ArrayList<ExprNode>();
                chainArgs.add(right);

                ExprNode arrayExpr = ASTFactory.createIdentifier(arrayName, arrayNameToken);
                EqualityChainNode chain = ASTFactory.createEqualityChain(arrayExpr, opToken.getText(), isAll, chainArgs, arrayNameToken, opToken, typeToken);
                return chain;
            } else {
                throw error("Expected comparison operator after 'all/any <arrayName>'");
            }
        } else if (is(LBRACKET)) {
            expect(LBRACKET);
            
            List<ExprNode> expressions = new ArrayList<ExprNode>();

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
            
            BooleanChainNode node = ASTFactory.createBooleanChain(isAll, expressions, typeToken);
            return node;
        } else {
            throw error("Expected array variable or '[' after 'all/any'");
        }
    }

    private ExprNode parseEqualityChain(ExprNode left, String operator) {
        Token leftToken = next(-1);
        
        Token operatorToken = now();
        consume();
        
        Token chainTypeToken = now();
        boolean isAllChain = is(chainTypeToken, ALL);
        
        if (!is(chainTypeToken, ALL, ANY)) {
            throw error("Expected 'all' or 'any' after comparison operator", chainTypeToken);
        }
        
        consume();
        
        List<ExprNode> chainArgs = new ArrayList<ExprNode>();
        
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
            String arrayName = expect(ID).getText();  // Lazy allocation
            ExprNode arrayExpr = ASTFactory.createIdentifier(arrayName, arrayNameToken);
            chainArgs.add(arrayExpr);
        } else {
            throw error("Expected array variable or '[' for array literal after 'all/any'");
        }
        
        EqualityChainNode chain = ASTFactory.createEqualityChain(left, operator, isAllChain, chainArgs, leftToken, operatorToken, chainTypeToken);
        return chain;
    }

    private ExprNode parseChainArgument() {
        if (is(BANG)) {
            Token bangToken = expect(BANG);
            ExprNode arg = parsePrimaryExpr();
            return ASTFactory.createUnaryOp("!", arg, bangToken);
        }
        
        if (is(LPAREN)) {
            return parseArgumentList();
        }
        
        if (is(ID) && is(next(), RBRACKET)) {
            Token idToken = now();
            throw error("Redundant brackets around array variable '" + idToken.getText() + 
                       "'. Use 'any " + idToken.getText() + "' instead of 'any[" + idToken.getText() + "]'", idToken);
        }
        
        return parsePrimaryExpr();
    }

    private ExprNode parseArgumentList() {
        Token lparenToken = expect(LPAREN);
        List<ExprNode> arguments = new ArrayList<ExprNode>();
        if (!is(RPAREN)) {
            arguments.add(parseExpr());
            while (consume(COMMA)) {
                arguments.add(parseExpr());
            }
        }
        expect(RPAREN);
        return ASTFactory.createArgumentList(arguments, lparenToken);
    }

    private ExprNode parseArrayLiteral() {
        Token lbracketToken = expect(LBRACKET);
        
        List<ExprNode> elements = new ArrayList<ExprNode>();
        
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
        return ASTFactory.createArray(elements, lbracketToken);
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

    private RangeNode parseRangeExpression() {
        ExprNode step = null;
        Token stepToken = null;
        Token rangeToken = null;
        
        ExprNode start = parseExpr();
        
        if (is(RANGE_DOTDOT)) {
            rangeToken = expect(RANGE_DOTDOT);
        } else if (is(TO)) {
            rangeToken = expect(TO);
        } else {
            throw error("Expected range operator '..' or 'to'");
        }
        
        ExprNode end = parseExpr();
        
        if (is(BY)) {
            stepToken = expect(BY);
            step = parseExpr();
        } else if (is(RANGE_HASH)) {
            stepToken = expect(RANGE_HASH);
            step = parseExpr();
        }
        
        return ASTFactory.createRange(step, start, end, stepToken, rangeToken);
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
                Token first = now();
                if (!is(first, ID)) return false;
                
                String idName = first.getText();  // Lazy allocation
                
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
                    
                    if (!(is(afterDot, ID) || canBeMethod(afterDot))) {
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
                case EQ: case NEQ: 
                    if (isChainComparison(1)) {
                        return PREC_CHAIN;
                    }
                    return PREC_EQUALITY;
                case LT: case GT: case LTE: case GTE: 
                    if (isChainComparison(1)) {
                        return PREC_CHAIN;
                    }
                    return PREC_COMPARISON;
                case PLUS: case MINUS: return PREC_TERM;
                case MUL: case DIV: case MOD: return PREC_FACTOR;
                case LPAREN: case LBRACKET: return PREC_CALL;
                default: return 0;
            }
        }
        return 0;
    }

    private boolean isChainComparison(int offset) {
        save();
        try {
            // Skip current operator
            Token currentOp = now();
            if (!isComparisonOp(currentOp)) return false;
            
            // Check if there's another comparison operator after an expression
            int pos = getPosition() + offset;
            
            // Parse the next expression (but don't consume)
            if (!isExprStart(next(pos))) return false;
            
            // Move past that expression
            while (pos < tokens.size()) {
                Token t = tokens.get(pos);
                if (t == null) break;
                
                if (is(t, RPAREN, RBRACE, RBRACKET, COMMA)) {
                    break;
                }
                
                if (isComparisonOp(t) && t != currentOp) {
                    return true;
                }
                
                pos++;
            }
            
            return false;
        } finally {
            restore();
        }
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
