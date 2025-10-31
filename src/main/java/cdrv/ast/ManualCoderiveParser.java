package cdrv.ast;

import cdrv.ast.nodes.*;
import java.util.ArrayList;
import java.util.List;
import cdrv.ast.ManualCoderiveLexer.Token;
import static cdrv.Constants.*;

/**
 * A self-contained, ANTLR-free recursive descent parser.
 * Implements the "annotation-style" slot declaration.
 */
public class ManualCoderiveParser {
    private final List<Token> tokens;
    private int position = 0;

    // --- Exception Class ---
    private static class ParseException extends RuntimeException {
        public ParseException(String message) { super(message); }
    }

    // --- Constructor ---
    public ManualCoderiveParser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // --- Helper method to set source position ---
    private void setNodePosition(ASTNode node, Token token) {
        if (node != null && token != null) {
            node.setSourcePosition(token.line, token.column);
        }
    }

    // --- Main Parsing Methods ---

    public ProgramNode parseProgram() {
        ProgramNode program = ASTFactory.createProgram();
        if (match(ManualCoderiveLexer.UNIT)) {
            program.unit = parseUnit();
        } else {
            program.unit = ASTFactory.createUnit("default");
        }

        while (!match(Token.EOF)) {
            if (isVisibilityModifier() || isSlotDeclaration()) { // Allow type or method
                program.unit.types.add(parseType());
            } else {
                 throw new ParseException("Expected type declaration (share/local) or EOF but found " +
                         ManualCoderiveLexer.getTypeName(currentToken().type) + " ('" + currentToken().text +
                         "') at line " + currentToken().line + ":" + currentToken().column);
            }
        }
        return program;
    }

    // --- NEW METHOD FOR REPL ---
    /**
     * Parses a single statement or expression from the token stream.
     * Used by the REPL.
     */
    public StatementNode parseSingleLine() {
        if (match(Token.EOF)) {
            return null; // Empty line
        }
        
        StatementNode stmt = parseStatement();
        
        // After parsing one statement, we expect the end of the input.
        if (!match(Token.EOF)) {
            Token current = currentToken();
            throw new ParseException("Unexpected token after statement: " + 
                ManualCoderiveLexer.getTypeName(current.type) + " ('" + current.text + "')" +
                " at line " + current.line + ":" + current.column);
        }
        return stmt;
    }
    // --- END NEW METHOD ---


    private UnitNode parseUnit() {
        Token unitToken = currentToken();
        consume(ManualCoderiveLexer.UNIT);
        String unitName = parseQualifiedName();
        UnitNode unit = ASTFactory.createUnit(unitName);
        setNodePosition(unit, unitToken);
        
        if (match(ManualCoderiveLexer.GET)) {
            unit.imports = parseGetNode();
        }
        return unit;
    }

    private GetNode parseGetNode() {
        Token getToken = currentToken();
        consume(ManualCoderiveLexer.GET);
        consume(ManualCoderiveLexer.LBRACE);
        List<String> imports = new ArrayList<String>();
        if (!match(ManualCoderiveLexer.RBRACE)) {
            imports.add(parseQualifiedName());
            while (tryConsume(ManualCoderiveLexer.COMMA)) {
                imports.add(parseQualifiedName());
            }
        }
        consume(ManualCoderiveLexer.RBRACE);
        GetNode getNode = ASTFactory.createGetNode(imports);
        setNodePosition(getNode, getToken);
        return getNode;
    }

    private TypeNode parseType() {
        Token visibilityToken = currentToken();
        String visibility = consume(isVisibilityModifier()).text;
        Token typeNameToken = currentToken();
        String typeName = consume(ManualCoderiveLexer.ID).text;
        String extendName = null;
        if (tryConsume(ManualCoderiveLexer.EXTEND)) {
            extendName = parseQualifiedName();
        }

        TypeNode type = ASTFactory.createType(typeName, visibility, extendName);
        setNodePosition(type, visibilityToken);
        
        consume(ManualCoderiveLexer.LBRACE);
        while (!match(ManualCoderiveLexer.RBRACE)) {
             // --- MODIFICATION: Check for slot decl before method decl ---
             if (isMethodDeclaration() || isSlotDeclaration()) {
             // --- END MODIFICATION ---
                 MethodNode method = parseMethod();
                 type.methods.add(method);
             } else if (isFieldDeclaration()) {
                 type.fields.add(parseField());
             } else {
                 type.statements.add(parseStatement());
             }
        }
        consume(ManualCoderiveLexer.RBRACE);
        return type;
    }


    private MethodNode parseMethod() {
        Token startToken = currentToken();
        
        // --- MODIFICATION: Parse "annotation-style" slot declaration ---
        List<String> returnSlots = null;
        if (isSlotDeclaration()) {
            consume(ManualCoderiveLexer.TILDE_BAR);
            returnSlots = parseIdList();
        }
        // --- END MODIFICATION ---

        String visibility = share;
        if (isVisibilityModifier()) {
             visibility = consume().text;
        }
        String methodName = consume(ManualCoderiveLexer.ID).text;

        // --- MODIFICATION: Pass parsed slots to factory ---
        MethodNode method = ASTFactory.createMethod(methodName, visibility, returnSlots);
        setNodePosition(method, startToken);
        // --- END MODIFICATION ---

        consume(ManualCoderiveLexer.LPAREN);
        if (!match(ManualCoderiveLexer.RPAREN)) {
            method.parameters.add(parseParameter());
            while (tryConsume(ManualCoderiveLexer.COMMA)) {
                method.parameters.add(parseParameter());
            }
        }
        consume(ManualCoderiveLexer.RPAREN);

        consume(ManualCoderiveLexer.LBRACE);
        while (!match(ManualCoderiveLexer.RBRACE)) {
            method.body.add(parseStatement());
        }
        consume(ManualCoderiveLexer.RBRACE);
        return method;
    }


    private FieldNode parseField() {
        Token startToken = currentToken();
        String fieldType = parseTypeReference();
        String fieldName = consume(ManualCoderiveLexer.ID).text;
        FieldNode field = ASTFactory.createField(fieldName, fieldType);
        setNodePosition(field, startToken);

        if (tryConsume(ManualCoderiveLexer.ASSIGN)) {
            field.value = parseExpression();
        }
        return field;
    }

    // --- Statement Parsers ---

// --- REVISED Statement Parser Order ---
private StatementNode parseStatement() {
    return parseStatement(null);
}

private StatementNode parseStatement(Boolean inheritedStyle) {
    // --- ORDER MATTERS FOR AMBIGUITY RESOLUTION ---

    // 1. Keywords first (Ensure FOR is checked early)
    if (match(ManualCoderiveLexer.IF)) return parseIfStatement(inheritedStyle);
    if (match(ManualCoderiveLexer.FOR)) return parseForStatement();
    if (match(ManualCoderiveLexer.OUTPUT)) return parseOutputStatement();

    // --- MODIFICATION: Slot *declaration* removed, *assignment* remains ---
    // if (isSlotDeclaration()) return parseSlotDeclaration(); // REMOVED
    if (isSlotAssignment()) return parseSlotAssignment();
    // --- END MODIFICATION ---

    // 2. Variable Declaration (Check using predicate AFTER keywords)
    if (isVariableDeclaration()) return parseVariableDeclaration();

    // 3. Assignments (check specific cases using predicates before general expression)
    if (isInputAssignment()) return parseInputAssignment();
    if (isReturnSlotAssignment()) return parseReturnSlotAssignment();
    if (isIndexAssignment()) return parseIndexAssignment();
    if (isSimpleAssignment()) return parseSimpleAssignment(); // <-- Fix applied in predicate

    // 4. Method Call Statement (Check before general expression)
    if (isMethodCallStatement()) {
        return parseMethodCallStatement();
    }

    // 5. Fallback: Other Expression Statements (if allowed)
    return parseExpressionStatement();
}

    // --- MODIFICATION: Removed parseSlotDeclaration ---
    
    private StatementNode parseSlotAssignment() {
        Token startToken = currentToken();
        consume(ManualCoderiveLexer.TILDE);
        String slotName = consume(ManualCoderiveLexer.ID).text;
        ExprNode value = parseExpression(); // Parses the 'a + b' part
        SlotAssignmentNode assignment = ASTFactory.createSlotAssignment(slotName, value);
        setNodePosition(assignment, startToken);
        return assignment;
    }

    private StatementNode parseSimpleAssignment() {
        Token startToken = currentToken();
        ExprNode target = ASTFactory.createIdentifier(consume(ManualCoderiveLexer.ID).text);
        consume(ManualCoderiveLexer.ASSIGN);
        ExprNode value = parseExpression();
        AssignmentNode assignment = ASTFactory.createAssignment(target, value);
        setNodePosition(assignment, startToken);
        return assignment;
    }

    // --- Corrected Variable Declaration Parsing ---
    private StatementNode parseVariableDeclaration() {
        Token startToken = currentToken();
        String typeName = null;
        if (tryConsume(ManualCoderiveLexer.VAR)) {
            typeName = "var"; // Indicate implicit typing
        } else if (isTypeStart(currentToken())) {
            typeName = parseTypeReference(); // Consume the type name (int, string, ID, int[], etc.)
        } else {
             throw new ParseException("Internal Error: Expected 'var' or type name at start of variable declaration.");
        }

        String varName = consume(ManualCoderiveLexer.ID).text;
        VarNode varNode = ASTFactory.createVar(varName, null);
        setNodePosition(varNode, startToken);
        varNode.explicitType = typeName; // Assumes VarNode has this field

        if (tryConsume(ManualCoderiveLexer.ASSIGN)) {
            varNode.value = parseExpression();
        }
        return varNode;
    }

private StatementNode parseIfStatement() {
    return parseIfStatement(null); // Start with no inherited style
}

private StatementNode parseIfStatement(Boolean inheritedStyle) {
    Token startToken = currentToken();
    consume(ManualCoderiveLexer.IF);
    ExprNode condition = parseExpression();
    IfNode rootIfNode = ASTFactory.createIf(condition);
    setNodePosition(rootIfNode, startToken);

    // Use inherited style if provided, otherwise we'll detect from the first branch we encounter
    Boolean currentStyle = inheritedStyle;
    
    // Handle then block (single statement or brace block)
    if (match(ManualCoderiveLexer.LBRACE)) {
        consume(ManualCoderiveLexer.LBRACE);
        while(!match(ManualCoderiveLexer.RBRACE)) {
            rootIfNode.thenBlock.statements.add(parseStatement(currentStyle));
        }
        consume(ManualCoderiveLexer.RBRACE);
    } else {
        // Single statement
        rootIfNode.thenBlock.statements.add(parseStatement(currentStyle));
    }

    IfNode currentNode = rootIfNode;
    
    // Handle ELIF branches
    while (tryConsume(ManualCoderiveLexer.ELIF)) {
        if (currentStyle != null && !currentStyle) {
            throw new ParseException("Cannot use 'elif' in an 'else if' style chain");
        }
        currentStyle = true;
        
        ExprNode elifCondition = parseExpression();
        IfNode elifNode = ASTFactory.createIf(elifCondition);

        if (match(ManualCoderiveLexer.LBRACE)) {
            consume(ManualCoderiveLexer.LBRACE);
            while(!match(ManualCoderiveLexer.RBRACE)) {
                elifNode.thenBlock.statements.add(parseStatement(currentStyle));
            }
            consume(ManualCoderiveLexer.RBRACE);
        } else {
            elifNode.thenBlock.statements.add(parseStatement(currentStyle));
        }

        currentNode.elseBlock.statements.add(elifNode);
        currentNode = elifNode;
    }

    // Handle ELSE branch
    if (tryConsume(ManualCoderiveLexer.ELSE)) {
        if (match(ManualCoderiveLexer.IF)) {
            if (currentStyle != null && currentStyle) {
                throw new ParseException("Cannot use 'else if' in an 'elif' style chain");
            }
            currentStyle = false;
            
            // Pass the current style down to nested if
            currentNode.elseBlock.statements.add(parseIfStatement(currentStyle));
        } else {
            // Regular ELSE block
            if (match(ManualCoderiveLexer.LBRACE)) {
                consume(ManualCoderiveLexer.LBRACE);
                while(!match(ManualCoderiveLexer.RBRACE)) {
                    currentNode.elseBlock.statements.add(parseStatement(currentStyle));
                }
                consume(ManualCoderiveLexer.RBRACE);
            } else {
                currentNode.elseBlock.statements.add(parseStatement(currentStyle));
            }
        }
    }
    
    return rootIfNode;
}

    // --- Method using the structure from your previously working version ---
    private StatementNode parseForStatement() {
        Token startToken = currentToken();
        consume(ManualCoderiveLexer.FOR);
        String iterator = consume(ManualCoderiveLexer.ID).text;

        ExprNode by = null; // Initialize by expression

        if (tryConsume(ManualCoderiveLexer.BY)) {
            // --- Using the logic structure from the OLD WORKING version ---

            // 1. Check specific MULTIPLICATIVE prefixes
            if (match(ManualCoderiveLexer.MUL, ManualCoderiveLexer.DIV)) {
                Token operator = consume(); // Consume '*' or '/'
                ExprNode operand = parseExpression(); // Parse the expression after the operator
                ExprNode iteratorRef = ASTFactory.createIdentifier(iterator); // Node for iterator 'i'
                // Create 'i * operand' or 'i / operand' node
                by = ASTFactory.createBinaryOp(iteratorRef, operator.text, operand);

            // 2. Check specific ADDITIVE prefixes (only simple literals)
            } else if (match(ManualCoderiveLexer.PLUS, ManualCoderiveLexer.MINUS)) {
                Token operator = consume(); // Consume '+' or '-'
                // Look ahead to see if it's followed by a literal for the simple case
                if (peek(0).type == ManualCoderiveLexer.INT_LIT || peek(0).type == ManualCoderiveLexer.FLOAT_LIT) {
                    ExprNode operand = parsePrimaryExpression(); // Consume only the literal
                    if (operator.type == ManualCoderiveLexer.PLUS) {
                        by = operand; // +1 becomes just 1
                    } else {
                        by = ASTFactory.createUnaryOp("-", operand); // -1 becomes UnaryOp
                    }
                } else {
                     // If not followed by literal, assume it's part of a full expression.
                     position--; // Backtrack (Put the operator back)
                     by = parseExpression(); // Parse as general expression
                }

            // 3. If NOT a specific prefix, THEN check for assignment or regular expr
            } else {
                if (isAssignmentInByClause()) { // Checks for 'ID op= expr'
                    // Parses 'by ID (=|+=|-=|*=|/=) expr'
                    ExprNode target = ASTFactory.createIdentifier(consume(ManualCoderiveLexer.ID).text);
                     if (!target.name.equals(iterator)) {
                         // Warning or error depending on language rules
                         System.err.println("[Parser Warning] Variable in 'by' assignment ("+target.name+") doesn't match iterator ("+iterator+").");
                     }
                    Token assignOp = consume(); // Consume operator
                    ExprNode value = parseExpression(); // Parse the right side
                    by = ASTFactory.createBinaryOp(target, assignOp.text, value); // Create assignment node
                } else {
                    // 4. Fallback: Parses 'by expr' (like 'by 2' or 'by steps')
                    by = parseExpression();
                }
            }
            // --- End OLD WORKING logic structure ---

            consume(ManualCoderiveLexer.IN); // Consume IN after the BY clause
        } else {
            // No BY clause
            by = null; // Let interpreter handle default step
            consume(ManualCoderiveLexer.IN); // Consume IN
        }

        // Parse start and end expressions
        ExprNode start = parseExpression();
        consume(ManualCoderiveLexer.TO);
        ExprNode end = parseExpression();

        // Create AST nodes
        RangeNode range = ASTFactory.createRange(by, start, end);
        ForNode forNode = ASTFactory.createFor(iterator, range);
        setNodePosition(forNode, startToken);

        // Parse loop body
        consume(ManualCoderiveLexer.LBRACE);
        while (!match(ManualCoderiveLexer.RBRACE)) {
            forNode.body.statements.add(parseStatement());
        }
        consume(ManualCoderiveLexer.RBRACE);
        return forNode;
    }


    private StatementNode parseOutputStatement() {
        Token startToken = currentToken();
        consume(ManualCoderiveLexer.OUTPUT);
        OutputNode output = ASTFactory.createOutput();
        setNodePosition(output, startToken);
        output.arguments.add(parseExpression());
        return output;
    }


    private StatementNode parseInputAssignment() {
        Token startToken = currentToken();
        String varName = consume(ManualCoderiveLexer.ID).text;
        consume(ManualCoderiveLexer.ASSIGN);
        consume(ManualCoderiveLexer.LPAREN);
        String type = parseTypeReference();
        consume(ManualCoderiveLexer.RPAREN);
        consume(ManualCoderiveLexer.INPUT);
        InputNode input = ASTFactory.createInput(type, varName);
        setNodePosition(input, startToken);
        return input;
    }

    private StatementNode parseReturnSlotAssignment() {
        Token startToken = currentToken();
        List<String> varNames = parseIdList();
        consume(ManualCoderiveLexer.ASSIGN);
        List<String> slotNames = parseReturnSlots();
        consume(ManualCoderiveLexer.COLON);
        MethodCallNode methodCall = parseMethodCall();
        methodCall.slotNames = slotNames;

        if (varNames.size() != slotNames.size()) {
            throw new ParseException("Number of variables (" + varNames.size() +
                                   ") does not match number of slots (" + slotNames.size() + ")");
        }

        ReturnSlotAssignmentNode assignment = ASTFactory.createReturnSlotAssignment(varNames, methodCall);
        setNodePosition(assignment, startToken);
        return assignment;
    }


    private StatementNode parseIndexAssignment() {
        Token startToken = currentToken();
        ExprNode arrayVar = ASTFactory.createIdentifier(consume(ManualCoderiveLexer.ID).text);
        IndexAccessNode indexAccess = parseIndexAccessContinuation(arrayVar);
        while(match(ManualCoderiveLexer.LBRACKET)) {
            indexAccess = parseIndexAccessContinuation(indexAccess);
        }
        consume(ManualCoderiveLexer.ASSIGN);
        ExprNode value = parseExpression();
        AssignmentNode assignment = ASTFactory.createAssignment(indexAccess, value);
        setNodePosition(assignment, startToken);
        return assignment;
    }


    private StatementNode parseMethodCallStatement() {
        Token startToken = currentToken();
        if (peek(0).type == ManualCoderiveLexer.LBRACKET) {
            List<String> slotNames = parseReturnSlots();
            consume(ManualCoderiveLexer.COLON);
            MethodCallNode methodCall = parseMethodCall();
            methodCall.slotNames = slotNames;
            setNodePosition(methodCall, startToken);
            return methodCall;
        } else {
            MethodCallNode methodCall = parseMethodCall();
            setNodePosition(methodCall, startToken);
            return methodCall;
        }
    }

    private StatementNode parseExpressionStatement() {
         Token startToken = currentToken();
         ExprNode expr = parseExpression();
         setNodePosition(expr, startToken);
         return expr;
    }


    // --- Expression Parsers ---

    private ExprNode parseExpression() { return parseComparisonExpression(); }

    private ExprNode parseComparisonExpression() {
        Token startToken = currentToken();
        ExprNode left = parseAdditiveExpression();
        while (match(ManualCoderiveLexer.EQ, ManualCoderiveLexer.NEQ, ManualCoderiveLexer.GT, ManualCoderiveLexer.LT, ManualCoderiveLexer.GTE, ManualCoderiveLexer.LTE)) {
            Token op = consume();
            ExprNode right = parseAdditiveExpression();
            left = ASTFactory.createBinaryOp(left, op.text, right);
            setNodePosition(left, startToken);
        }
        return left;
    }

    private ExprNode parseAdditiveExpression() {
        Token startToken = currentToken();
        ExprNode left = parseMultiplicativeExpression();
        while (match(ManualCoderiveLexer.PLUS, ManualCoderiveLexer.MINUS)) {
            Token op = consume();
            ExprNode right = parseMultiplicativeExpression();
            left = ASTFactory.createBinaryOp(left, op.text, right);
            setNodePosition(left, startToken);
        }
        return left;
    }

    private ExprNode parseMultiplicativeExpression() {
        Token startToken = currentToken();
        ExprNode left = parseUnaryExpression();
        while (match(ManualCoderiveLexer.MUL, ManualCoderiveLexer.DIV, ManualCoderiveLexer.MOD)) {
            Token op = consume();
            ExprNode right = parseUnaryExpression();
            left = ASTFactory.createBinaryOp(left, op.text, right);
            setNodePosition(left, startToken);
        }
        return left;
    }

     private ExprNode parseUnaryExpression() {
        Token startToken = currentToken();
        if (match(ManualCoderiveLexer.PLUS, ManualCoderiveLexer.MINUS)) {
            Token op = consume();
            ExprNode operand = parsePrimaryExpression();
            UnaryNode unary = ASTFactory.createUnaryOp(op.text, operand);
            setNodePosition(unary, startToken);
            return unary;
        }
        return parsePrimaryExpression();
    }


    private ExprNode parseArrayLiteral() {
        Token startToken = currentToken();
        consume(ManualCoderiveLexer.LBRACKET);
        List<ExprNode> elements = new ArrayList<ExprNode>();
        if (!match(ManualCoderiveLexer.RBRACKET)) {
            elements.add(parseExpression());
            while (tryConsume(ManualCoderiveLexer.COMMA)) {
                elements.add(parseExpression());
            }
        }
        consume(ManualCoderiveLexer.RBRACKET);
        ArrayNode array = ASTFactory.createArray(elements);
        setNodePosition(array, startToken);
        return array;
    }

    private ExprNode parsePrimaryExpression() {
        ExprNode baseExpr;
        Token startToken = currentToken();

        if (match(ManualCoderiveLexer.LBRACKET)) {
            baseExpr = parseArrayLiteral();
        } else if (match(ManualCoderiveLexer.INT_LIT)) {
            baseExpr = ASTFactory.createIntLiteral(Integer.parseInt(consume().text));
            setNodePosition(baseExpr, startToken);
        } else if (match(ManualCoderiveLexer.FLOAT_LIT)) {
            baseExpr = ASTFactory.createFloatLiteral(Float.parseFloat(consume().text));
            setNodePosition(baseExpr, startToken);
        } else if (match(ManualCoderiveLexer.STRING_LIT)) {
            Token stringToken = consume();
            baseExpr = ASTFactory.createStringLiteral(stringToken.text);
            setNodePosition(baseExpr, stringToken);
        } else if (match(ManualCoderiveLexer.BOOL_LIT)) {
            baseExpr = ASTFactory.createBoolLiteral(Boolean.parseBoolean(consume().text));
            setNodePosition(baseExpr, startToken);
        } else if (match(ManualCoderiveLexer.ID)) {
             if (isMethodCallFollows()) {
                 baseExpr = parseMethodCall();
             } else {
                 baseExpr = ASTFactory.createIdentifier(consume().text);
                 setNodePosition(baseExpr, startToken);
             }
        } else if (match(ManualCoderiveLexer.LPAREN)) {
             if (isTypeCast()) {
                 baseExpr = parseTypeCast();
             } else {
                 consume(ManualCoderiveLexer.LPAREN);
                 baseExpr = parseExpression();
                 consume(ManualCoderiveLexer.RPAREN);
                 setNodePosition(baseExpr, startToken);
             }
        } else {
            throw new ParseException("Unexpected token in primary expression: " + startToken.text +
                " (" + ManualCoderiveLexer.getTypeName(startToken.type) + ")" +
                " at line " + startToken.line + ":" + startToken.column);
        }

        while (match(ManualCoderiveLexer.LBRACKET)) {
             baseExpr = parseIndexAccessContinuation(baseExpr);
        }

        return baseExpr;
    }


    // --- Sub-Expression and Grammar Rule Parsers ---

    private MethodCallNode parseMethodCall() {
        Token startToken = currentToken();
        String qualifiedNameStr = parseQualifiedName();
        String methodName = qualifiedNameStr;
        if (qualifiedNameStr.contains(".")) {
             methodName = qualifiedNameStr.substring(qualifiedNameStr.lastIndexOf('.') + 1);
        }
        MethodCallNode call = ASTFactory.createMethodCall(methodName, qualifiedNameStr);
        setNodePosition(call, startToken);

        consume(ManualCoderiveLexer.LPAREN);
        if (!match(ManualCoderiveLexer.RPAREN)) {
            call.arguments.add(parseExpression());
            while (tryConsume(ManualCoderiveLexer.COMMA)) {
                call.arguments.add(parseExpression());
            }
        }
        consume(ManualCoderiveLexer.RPAREN);
        return call;
    }

     private IndexAccessNode parseIndexAccessContinuation(ExprNode arrayExpr) {
         Token startToken = currentToken();
         consume(ManualCoderiveLexer.LBRACKET);
         ExprNode indexExpr = parseExpression();
         consume(ManualCoderiveLexer.RBRACKET);
         IndexAccessNode access = ASTFactory.createIndexAccess(arrayExpr, indexExpr);
         setNodePosition(access, startToken);
         return access;
     }


    private ExprNode parseTypeCast() {
        Token startToken = currentToken();
        consume(ManualCoderiveLexer.LPAREN);
        String type = parseTypeReference();
        consume(ManualCoderiveLexer.RPAREN);
        ExprNode expressionToCast = parseUnaryExpression();
        TypeCastNode cast = ASTFactory.createTypeCast(type, expressionToCast);
        setNodePosition(cast, startToken);
        return cast;
    }


    private List<String> parseReturnSlots() {
        consume(ManualCoderiveLexer.LBRACKET);
        List<String> slots = parseIdList();
        consume(ManualCoderiveLexer.RBRACKET);
        return slots;
    }

     private List<String> parseIdList() {
         List<String> ids = new ArrayList<String>();
         ids.add(consume(ManualCoderiveLexer.ID).text);
         while (tryConsume(ManualCoderiveLexer.COMMA)) {
             ids.add(consume(ManualCoderiveLexer.ID).text);
         }
         return ids;
     }

    private ParamNode parseParameter() {
        Token startToken = currentToken();
        String type = parseTypeReference();
        String name = consume(ManualCoderiveLexer.ID).text;
        ParamNode param = ASTFactory.createParam(name, type);
        setNodePosition(param, startToken);
        return param;
    }

    // --- Predicate Methods ---

    // --- PREDICATES MODIFIED ---
    private boolean isSlotDeclaration() {
        try {
            return tokens.get(position).type == ManualCoderiveLexer.TILDE_BAR;
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }

    private boolean isSlotAssignment() {
        try {
            // Must be just '~' followed by an ID and then not an assignment operator
            // This distinguishes `~ result a + b` from `~|`
            if (tokens.get(position).type != ManualCoderiveLexer.TILDE) return false;
            if (position + 1 >= tokens.size()) return false;
            return tokens.get(position + 1).type == ManualCoderiveLexer.ID;
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }
    // --- END PREDICATES MODIFIED ---

    private boolean isTypeCast() {
        int p = position; try { if (tokens.get(p++).type != ManualCoderiveLexer.LPAREN) return false; if (!isTypeStart(tokens.get(p))) return false; p++; while (p < tokens.size() && tokens.get(p).type == ManualCoderiveLexer.LBRACKET) { if (p + 1 < tokens.size() && tokens.get(p + 1).type == ManualCoderiveLexer.RBRACKET) { p += 2; } else { return false; } } if (p >= tokens.size() || tokens.get(p++).type != ManualCoderiveLexer.RPAREN) return false; if (p >= tokens.size()) return false; int nt = tokens.get(p).type; return nt == ManualCoderiveLexer.LBRACKET || nt == ManualCoderiveLexer.INT_LIT || nt == ManualCoderiveLexer.FLOAT_LIT || nt == ManualCoderiveLexer.STRING_LIT || nt == ManualCoderiveLexer.BOOL_LIT || nt == ManualCoderiveLexer.ID || nt == ManualCoderiveLexer.LPAREN || nt == ManualCoderiveLexer.PLUS || nt == ManualCoderiveLexer.MINUS; } catch (IndexOutOfBoundsException e) { return false; }
    }
    private boolean isMethodDeclaration() {
        int p = position; try { 
            // --- MODIFIED PREDICATE: Now checks for slot decl OR visibility ---
            if (tokens.get(p).type == ManualCoderiveLexer.TILDE_BAR) { 
                p++; // Skip past ~|
                while(p < tokens.size() && (tokens.get(p).type == ManualCoderiveLexer.ID || tokens.get(p).type == ManualCoderiveLexer.COMMA)) {
                    p++; // Skip past id list
                }
            }
            // ---
            if (isVisibilityModifier(tokens.get(p))) p++; 
            if (p >= tokens.size() || tokens.get(p).type != ManualCoderiveLexer.ID) return false; p++; 
            return p < tokens.size() && tokens.get(p).type == ManualCoderiveLexer.LPAREN; 
        } catch (IndexOutOfBoundsException e) { return false; }
    }
    private boolean isFieldDeclaration() {
        int p = position; try { if (!isTypeStart(tokens.get(p))) return false; p++; while (p < tokens.size() && tokens.get(p).type == ManualCoderiveLexer.LBRACKET) { if (p + 1 < tokens.size() && tokens.get(p + 1).type == ManualCoderiveLexer.RBRACKET) { p += 2; } else { return false; } } if (p >= tokens.size() || tokens.get(p).type != ManualCoderiveLexer.ID) return false; p++; return p >= tokens.size() || tokens.get(p).type != ManualCoderiveLexer.LPAREN; } catch (IndexOutOfBoundsException e) { return false; }
    }
    private boolean isInputAssignment() {
         int p = position; try { if (tokens.get(p++).type != ManualCoderiveLexer.ID) return false; if (tokens.get(p++).type != ManualCoderiveLexer.ASSIGN) return false; if (tokens.get(p++).type != ManualCoderiveLexer.LPAREN) return false; if (!isTypeStart(tokens.get(p))) return false; p++; while (p < tokens.size() && tokens.get(p).type == ManualCoderiveLexer.LBRACKET) { if (p + 1 < tokens.size() && tokens.get(p + 1).type == ManualCoderiveLexer.RBRACKET) { p += 2; } else { return false; } } if (p >= tokens.size() || tokens.get(p++).type != ManualCoderiveLexer.RPAREN) return false; return p < tokens.size() && tokens.get(p).type == ManualCoderiveLexer.INPUT; } catch (IndexOutOfBoundsException e) { return false; }
     }
     // --- Fixed Simple Assignment Predicate ---
     private boolean isSimpleAssignment() {
         int p = position;
         try {
             if (tokens.get(p).type != ManualCoderiveLexer.ID) return false; // Must start with ID
             // Check NOT index assignment: token after ID should not be LBRACKET
             if (p + 1 < tokens.size() && tokens.get(p + 1).type == ManualCoderiveLexer.LBRACKET) return false;
             // Check IS followed by ASSIGN
             if (p + 1 >= tokens.size() || tokens.get(p + 1).type != ManualCoderiveLexer.ASSIGN) return false;
             // Check NOT input assignment: token after ASSIGN is not LPAREN
             if (p + 2 < tokens.size() && tokens.get(p + 2).type == ManualCoderiveLexer.LPAREN) return false;
             // REMOVED check for LBRACKET after ASSIGN

             // If it passed all the above, it's a simple assignment
             return true;
         } catch (IndexOutOfBoundsException e) {
             return false;
         }
     }
    private boolean isIndexAssignment() {
        int p = position; try { if (tokens.get(p++).type != ManualCoderiveLexer.ID) return false; if (p >= tokens.size() || tokens.get(p++).type != ManualCoderiveLexer.LBRACKET) return false; int bd = 1; while (p < tokens.size() && bd > 0) { if (tokens.get(p).type == ManualCoderiveLexer.LBRACKET) bd++; else if (tokens.get(p).type == ManualCoderiveLexer.RBRACKET) bd--; p++; } if (bd != 0) return false; while (p < tokens.size() && tokens.get(p).type == ManualCoderiveLexer.LBRACKET) { p++; bd = 1; while (p < tokens.size() && bd > 0) { if (tokens.get(p).type == ManualCoderiveLexer.LBRACKET) bd++; else if (tokens.get(p).type == ManualCoderiveLexer.RBRACKET) bd--; p++; } if (bd != 0) return false; } return p < tokens.size() && tokens.get(p).type == ManualCoderiveLexer.ASSIGN; } catch (IndexOutOfBoundsException e) { return false; }
    }
     private boolean isMethodCallFollows() {
         int p = position; try { if (tokens.get(p).type != ManualCoderiveLexer.ID) return false; p++; while(p < tokens.size() && tokens.get(p).type == ManualCoderiveLexer.DOT) { p++; if (p >= tokens.size() || tokens.get(p).type != ManualCoderiveLexer.ID) return false; p++; } return p < tokens.size() && tokens.get(p).type == ManualCoderiveLexer.LPAREN; } catch (IndexOutOfBoundsException e) { return false; }
     }
    private boolean isReturnSlotAssignment() {
        int p = position; try { if (tokens.get(p++).type != ManualCoderiveLexer.ID) return false; while (p < tokens.size() && tokens.get(p).type == ManualCoderiveLexer.COMMA) { p++; if (p >= tokens.size() || tokens.get(p++).type != ManualCoderiveLexer.ID) return false; } if (p >= tokens.size() || tokens.get(p++).type != ManualCoderiveLexer.ASSIGN) return false; if (p >= tokens.size() || tokens.get(p++).type != ManualCoderiveLexer.LBRACKET) return false; if (p >= tokens.size() || tokens.get(p++).type != ManualCoderiveLexer.ID) return false; while (p < tokens.size() && tokens.get(p).type == ManualCoderiveLexer.COMMA) { p++; if (p >= tokens.size() || tokens.get(p++).type != ManualCoderiveLexer.ID) return false; } if (p >= tokens.size() || tokens.get(p++).type != ManualCoderiveLexer.RBRACKET) return false; return p < tokens.size() && tokens.get(p).type == ManualCoderiveLexer.COLON; } catch (IndexOutOfBoundsException e) { return false; }
    }
    private boolean isVariableDeclaration() {
         int p = position; try { Token first = tokens.get(p); if (first.type == ManualCoderiveLexer.VAR) { return p + 1 < tokens.size() && tokens.get(p + 1).type == ManualCoderiveLexer.ID; } else if (isTypeStart(first)) { p++; while (p < tokens.size() && tokens.get(p).type == ManualCoderiveLexer.LBRACKET) { if (p + 1 < tokens.size() && tokens.get(p + 1).type == ManualCoderiveLexer.RBRACKET) { p += 2; } else { return false; } } return p < tokens.size() && tokens.get(p).type == ManualCoderiveLexer.ID; } return false; } catch (IndexOutOfBoundsException e) { return false; }
    }
    private boolean isAssignmentInByClause() {
        int p = position; try { if (tokens.get(p).type != ManualCoderiveLexer.ID) return false; int nt = tokens.get(p + 1).type; return nt == ManualCoderiveLexer.ASSIGN || nt == ManualCoderiveLexer.PLUS_ASSIGN || nt == ManualCoderiveLexer.MINUS_ASSIGN || nt == ManualCoderiveLexer.MUL_ASSIGN || nt == ManualCoderiveLexer.DIV_ASSIGN; } catch (IndexOutOfBoundsException e) { return false; }
    }
    private boolean isMethodCallStatement() {
        int p = position; try { if (tokens.get(p).type == ManualCoderiveLexer.LBRACKET) { p++; if (tokens.get(p).type != ManualCoderiveLexer.ID) return false; p++; while(p < tokens.size() && tokens.get(p).type == ManualCoderiveLexer.COMMA) { p+=2; } if (p >= tokens.size() || tokens.get(p).type != ManualCoderiveLexer.RBRACKET) return false; p++; if (p >= tokens.size() || tokens.get(p).type != ManualCoderiveLexer.COLON) return false; p++; } if (tokens.get(p).type != ManualCoderiveLexer.ID) return false; p++; while(p < tokens.size() && tokens.get(p).type == ManualCoderiveLexer.DOT) { p++; if (p >= tokens.size() || tokens.get(p).type != ManualCoderiveLexer.ID) return false; p++; } return p < tokens.size() && tokens.get(p).type == ManualCoderiveLexer.LPAREN; } catch (IndexOutOfBoundsException e) { return false; }
    }

    // --- Utility and Helper Methods ---

    private String parseQualifiedName() {
        StringBuilder name = new StringBuilder();
        name.append(consume(ManualCoderiveLexer.ID).text);
        while (tryConsume(ManualCoderiveLexer.DOT)) {
            name.append(".");
            name.append(consume(ManualCoderiveLexer.ID).text);
        }
        return name.toString();
    }

    private String parseTypeReference() {
        StringBuilder typeName = new StringBuilder();
        if (isTypeStart(currentToken())) {
            typeName.append(consume().text);
        } else {
            Token current = currentToken();
            throw new ParseException("Expected type name (int, string, float, bool, or ID) but got " +
                ManualCoderiveLexer.getTypeName(current.type) + " ('" + current.text + "')" +
                 " at line " + current.line + ":" + current.column);
        }
        while (match(ManualCoderiveLexer.LBRACKET)) {
             consume(ManualCoderiveLexer.LBRACKET);
             consume(ManualCoderiveLexer.RBRACKET);
             typeName.append("[]");
        }
        return typeName.toString();
    }


    private boolean isTypeKeyword(int type) {
        return type == ManualCoderiveLexer.INT || type == ManualCoderiveLexer.STRING ||
               type == ManualCoderiveLexer.FLOAT || type == ManualCoderiveLexer.BOOL;
    }

     private boolean isTypeStart(Token token) {
         if (token == null) return false;
         return isTypeKeyword(token.type) || token.type == ManualCoderiveLexer.ID;
     }

    private boolean isVisibilityModifier() {
        return match(ManualCoderiveLexer.SHARE, ManualCoderiveLexer.LOCAL);
    }
     private boolean isVisibilityModifier(Token token) {
          if (token == null) return false;
         return token.type == ManualCoderiveLexer.SHARE || token.type == ManualCoderiveLexer.LOCAL;
     }

    // --- Token Stream Navigation ---

    private Token currentToken() {
        return (position >= tokens.size()) ? tokens.get(tokens.size() - 1) : tokens.get(position);
    }

    private Token peek(int offset) {
         int targetPos = position + offset;
         if (targetPos >= tokens.size()) return tokens.get(tokens.size() - 1);
         if (targetPos < 0) { return null; }
        return tokens.get(targetPos);
    }

    private boolean match(int... types) {
        Token current = currentToken();
        if (current.type == Token.EOF) {
            for (int type : types) { if (type == Token.EOF) return true; }
            return false;
        }
        for (int type : types) {
            if (current.type == type) return true;
        }
        return false;
    }

    private Token consume() {
        Token token = currentToken();
        if (token.type != Token.EOF) position++;
        return token;
    }

    private Token consume(int expectedType) {
        Token token = currentToken();
        if (token.type == expectedType) {
            if (token.type != Token.EOF) position++;
            return token;
        }
        throw new ParseException("Expected " + ManualCoderiveLexer.getTypeName(expectedType) + " but found " +
                ManualCoderiveLexer.getTypeName(token.type) + " ('" + token.text + "') at line " + token.line + ":" + token.column);
    }

    private Token consume(boolean condition) {
         if (condition) return consume();
         Token current = currentToken();
         throw new ParseException("Consumption condition not met at: " + current.text +
             " (" + ManualCoderiveLexer.getTypeName(current.type) + ")" +
             " at line " + current.line + ":" + current.column);
    }

    private boolean tryConsume(int expectedType) {
        if (match(expectedType)) {
            consume(expectedType);
            return true;
        }
        return false;
    }
}