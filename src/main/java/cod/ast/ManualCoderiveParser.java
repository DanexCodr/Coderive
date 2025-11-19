package cod.ast;

import cod.ast.error.ParseError;
import cod.ast.nodes.*;
import java.util.ArrayList;
import java.util.List;
import cod.ast.ManualCoderiveLexer.Token;
import static cod.ast.ManualCoderiveLexer.TokenType.*;
import static cod.Constants.*;

/**
 * A self-contained, ANTLR-free recursive descent parser.
 * Implements the "annotation-style" slot declaration.
 */
public class ManualCoderiveParser {
    private final List<Token> tokens;
    private int position = 0;

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
        if (match(UNIT)) {
            program.unit = parseUnit();
        } else {
            program.unit = ASTFactory.createUnit("default");
        }

        while (!match(Token.EOF)) {
            if (isVisibilityModifier() || isSlotDeclaration()) { // Allow type or method
                program.unit.types.add(parseType());
            } else {
                 throw new ParseError("Expected type declaration (share/local) or EOF but found " +
                         getTypeName(currentToken().type) + " ('" + currentToken().text +
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
            throw new ParseError("Unexpected token after statement: " + 
                getTypeName(current.type) + " ('" + current.text + "')" +
                " at line " + current.line + ":" + current.column);
        }
        return stmt;
    }
    // --- END NEW METHOD ---


    private UnitNode parseUnit() {
        Token unitToken = currentToken();
        consume(UNIT);
        String unitName = parseQualifiedName();
        UnitNode unit = ASTFactory.createUnit(unitName);
        setNodePosition(unit, unitToken);
        
        if (match(GET)) {
            unit.imports = parseGetNode();
        }
        return unit;
    }

    private GetNode parseGetNode() {
        Token getToken = currentToken();
        consume(GET);
        consume(LBRACE);
        List<String> imports = new ArrayList<String>();
        if (!match(RBRACE)) {
            imports.add(parseQualifiedName());
            while (tryConsume(COMMA)) {
                imports.add(parseQualifiedName());
            }
        }
        consume(RBRACE);
        GetNode getNode = ASTFactory.createGetNode(imports);
        setNodePosition(getNode, getToken);
        return getNode;
    }

    private TypeNode parseType() {
        Token visibilityToken = currentToken();
        String visibility = consume(isVisibilityModifier()).text;
        Token typeNameToken = currentToken();
        String typeName = consume(ID).text;
        
        // ENFORCE CLASS NAMING
        NamingValidator.validateClassName(typeName, typeNameToken);
        
        String extendName = null;
        if (tryConsume(EXTEND)) {
            extendName = parseQualifiedName();
        }

        TypeNode type = ASTFactory.createType(typeName, visibility, extendName);
        setNodePosition(type, visibilityToken);
        
        consume(LBRACE);
        while (!match(RBRACE)) {
            if (isMethodDeclaration() || isSlotDeclaration()) {
                MethodNode method = parseMethod();
                type.methods.add(method);
            } else if (isFieldDeclaration()) {
                type.fields.add(parseField());
            } else {
                type.statements.add(parseStatement());
            }
        }
        consume(RBRACE);
        return type;
    }

    private MethodNode parseMethod() {
        Token startToken = currentToken();
        
        List<String> returnSlots = null;
        if (isSlotDeclaration()) {
            consume(TILDE_BAR);
            returnSlots = parseIdList();
        }

        boolean isBuiltin = false;
        String visibility = share; // DEFAULT to share
        
        if (tryConsume(BUILTIN)) {
            isBuiltin = true;
            // Builtin methods are automatically shareable
            visibility = share;
        } else if (isVisibilityModifier()) {
            visibility = consume().text;
        }
        
        String methodName = consume(ID).text;
        
        NamingValidator.validateMethodName(methodName, startToken);

        MethodNode method = ASTFactory.createMethod(methodName, visibility, returnSlots);
        method.isBuiltin = isBuiltin;
        setNodePosition(method, startToken);

        consume(LPAREN);
        if (!match(RPAREN)) {
            method.parameters.add(parseParameter());
            while (tryConsume(COMMA)) {
                method.parameters.add(parseParameter());
            }
        }
        consume(RPAREN);

        if (match(TILDE_ARROW)) {
            consume(TILDE_ARROW);
            ExprNode returnExpr = parseExpression();
            
            SlotAssignmentNode returnStmt = ASTFactory.createSlotAssignment("return", returnExpr);
            method.body.add(returnStmt);
        } else {
            consume(LBRACE);
            while (!match(RBRACE)) {
                method.body.add(parseStatement());
            }
            consume(RBRACE);
        }
        
        return method;
    }

    private FieldNode parseField() {
        Token startToken = currentToken();
        String fieldType = parseTypeReference();
        String fieldName = consume(ID).text;
        
        // ENFORCE FIELD NAMING
        if (NamingValidator.isAllCaps(fieldName)) {
            NamingValidator.validateConstantName(fieldName, startToken);
        } else {
            NamingValidator.validateVariableName(fieldName, startToken);
        }

        FieldNode field = ASTFactory.createField(fieldName, fieldType);
        setNodePosition(field, startToken);

        if (tryConsume(ASSIGN)) {
            field.value = parseExpression();
        }
        return field;
    }

    // --- Statement Parsers ---

    private StatementNode parseStatement() {
        return parseStatement(null);
    }

    private StatementNode parseStatement(Boolean inheritedStyle) {
        // --- ORDER MATTERS FOR AMBIGUITY RESOLUTION ---

        // 1. Keywords first (Ensure FOR is checked early)
        if (match(IF)) return parseIfStatement(inheritedStyle);
        if (match(FOR)) return parseForStatement();
        if (match(OUTPUT)) return parseOutputStatement();

        // 2. Slot Assignment (NEW: Check for ~> before variable declarations)
        if (isSlotAssignment()) return parseSlotAssignment();

        // 3. Variable Declaration (Check using predicate AFTER keywords)
        if (isVariableDeclaration()) return parseVariableDeclaration();

        // 4. Assignments (check specific cases using predicates before general expression)
        if (isInputAssignment()) return parseInputAssignment();
        if (isReturnSlotAssignment()) return parseReturnSlotAssignment();
        if (isIndexAssignment()) return parseIndexAssignment();
        if (isSimpleAssignment()) return parseSimpleAssignment();

        // 5. Method Call Statement (Check before general expression)
        if (isMethodCallStatement()) {
            return parseMethodCallStatement();
        }

        // 6. Fallback: Other Expression Statements (if allowed)
        return parseExpressionStatement();
    }

    private List<SlotAssignmentNode> parseMultipleSlotAssignments() {
        List<SlotAssignmentNode> assignments = new ArrayList<SlotAssignmentNode>();
        
        // Parse first assignment
        assignments.add(parseSingleSlotAssignment());
        
        // Parse additional comma-separated assignments
        while (tryConsume(COMMA)) {
            assignments.add(parseSingleSlotAssignment());
        }
        
        return assignments;
    }

    private SlotAssignmentNode parseSingleSlotAssignment() {
        Token startToken = currentToken();
        
        // Check if this is a named assignment (slotName expr) or unnamed (expr)
        String slotName = null;
        ExprNode value;
        
        // Simple heuristic: if we have ID followed by something that looks like an expression start
        if (peek(0).type == ID && 
            (peek(1).type == ID || 
             peek(1).type == INT_LIT ||
             peek(1).type == FLOAT_LIT ||
             peek(1).type == STRING_LIT ||
             peek(1).type == BOOL_LIT ||
             peek(1).type == LPAREN)) {
            // This looks like a named assignment: slotName expr
            slotName = consume(ID).text;
            value = parseExpression();
        } else {
            // Unnamed assignment: just expr
            value = parseExpression();
        }
        
        SlotAssignmentNode assignment = ASTFactory.createSlotAssignment(slotName, value);
        setNodePosition(assignment, startToken);
        return assignment;
    }

    private StatementNode parseSlotAssignment() {
        Token startToken = currentToken();
        consume(TILDE_ARROW);  // Consume ~>
        
        // Check if this is a multiple assignment by looking ahead for comma
        // We need to parse the first element and THEN check if there's a comma
        List<SlotAssignmentNode> assignments = new ArrayList<SlotAssignmentNode>();
        
        // Parse first assignment
        assignments.add(parseSingleSlotAssignment());
        
        // Now check if there are more comma-separated assignments
        if (match(COMMA)) {
            // Parse the rest of the assignments
            while (tryConsume(COMMA)) {
                assignments.add(parseSingleSlotAssignment());
            }
            
            MultipleSlotAssignmentNode multiAssign = ASTFactory.createMultipleSlotAssignment(assignments);
            setNodePosition(multiAssign, startToken);
            return multiAssign;
        } else {
            // Single assignment - return the first one we parsed
            SlotAssignmentNode assignment = assignments.get(0);
            setNodePosition(assignment, startToken);
            return assignment;
        }
    }

    private StatementNode parseSimpleAssignment() {
        Token startToken = currentToken();
        ExprNode target = ASTFactory.createIdentifier(consume(ID).text);
        consume(ASSIGN);
        ExprNode value = parseExpression();
        AssignmentNode assignment = ASTFactory.createAssignment(target, value);
        setNodePosition(assignment, startToken);
        return assignment;
    }

    private StatementNode parseVariableDeclaration() {
        Token startToken = currentToken();
        String typeName = null;
        if (tryConsume(VAR)) {
            typeName = "var";
        } else if (isTypeStart(currentToken())) {
            typeName = parseTypeReference();
        } else {
             throw new ParseError("Internal Error: Expected 'var' or type name at start of variable declaration.");
        }

        Token varNameToken = currentToken();
        String varName = consume(ID).text;
        
        // ENFORCE VARIABLE/CONSTANT NAMING
        if (NamingValidator.isAllCaps(varName)) {
            // This is a constant
            NamingValidator.validateConstantName(varName, varNameToken);
        } else {
            // This is a regular variable
            NamingValidator.validateVariableName(varName, varNameToken);
        }

        VarNode varNode = ASTFactory.createVar(varName, null);
        setNodePosition(varNode, startToken);
        varNode.explicitType = typeName;

        if (tryConsume(ASSIGN)) {
            varNode.value = parseExpression();
        }
        return varNode;
    }

    private StatementNode parseIfStatement() {
        return parseIfStatement(null); // Start with no inherited style
    }

    private StatementNode parseIfStatement(Boolean inheritedStyle) {
        Token startToken = currentToken();
        consume(IF);
        ExprNode condition = parseExpression();
        IfNode rootIfNode = ASTFactory.createIf(condition);
        setNodePosition(rootIfNode, startToken);

        // Use inherited style if provided, otherwise we'll detect from the first branch we encounter
        Boolean currentStyle = inheritedStyle;
        
        // Handle then block (single statement or brace block)
        if (match(LBRACE)) {
            consume(LBRACE);
            while(!match(RBRACE)) {
                rootIfNode.thenBlock.statements.add(parseStatement(currentStyle));
            }
            consume(RBRACE);
        } else {
            // Single statement
            rootIfNode.thenBlock.statements.add(parseStatement(currentStyle));
        }

        IfNode currentNode = rootIfNode;
        
        // Handle ELIF branches
        while (tryConsume(ELIF)) {
            if (currentStyle != null && !currentStyle) {
                throw new ParseError("Cannot use 'elif' in an 'else if' style chain");
            }
            currentStyle = true;
            
            ExprNode elifCondition = parseExpression();
            IfNode elifNode = ASTFactory.createIf(elifCondition);

            if (match(LBRACE)) {
                consume(LBRACE);
                while(!match(RBRACE)) {
                    elifNode.thenBlock.statements.add(parseStatement(currentStyle));
                }
                consume(RBRACE);
            } else {
                elifNode.thenBlock.statements.add(parseStatement(currentStyle));
            }

            currentNode.elseBlock.statements.add(elifNode);
            currentNode = elifNode;
        }

        // Handle ELSE branch
        if (tryConsume(ELSE)) {
            if (match(IF)) {
                if (currentStyle != null && currentStyle) {
                    throw new ParseError("Cannot use 'else if' in an 'elif' style chain");
                }
                currentStyle = false;
                
                // Pass the current style down to nested if
                currentNode.elseBlock.statements.add(parseIfStatement(currentStyle));
            } else {
                // Regular ELSE block
                if (match(LBRACE)) {
                    consume(LBRACE);
                    while(!match(RBRACE)) {
                        currentNode.elseBlock.statements.add(parseStatement(currentStyle));
                    }
                    consume(RBRACE);
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
        consume(FOR);
        String iterator = consume(ID).text;

        ExprNode by = null; // Initialize by expression

        if (tryConsume(BY)) {
            // --- Using the logic structure from the OLD WORKING version ---

            // 1. Check specific MULTIPLICATIVE prefixes
            if (match(MUL, DIV)) {
                Token operator = consume(); // Consume '*' or '/'
                ExprNode operand = parseExpression(); // Parse the expression after the operator
                ExprNode iteratorRef = ASTFactory.createIdentifier(iterator); // Node for iterator 'i'
                // Create 'i * operand' or 'i / operand' node
                by = ASTFactory.createBinaryOp(iteratorRef, operator.text, operand);

            // 2. Check specific ADDITIVE prefixes (only simple literals)
            } else if (match(PLUS, MINUS)) {
                Token operator = consume(); // Consume '+' or '-'
                // Look ahead to see if it's followed by a literal for the simple case
                if (peek(0).type == INT_LIT || peek(0).type == FLOAT_LIT) {
                    ExprNode operand = parsePrimaryExpression(); // Consume only the literal
                    if (operator.type == PLUS) {
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
                    ExprNode target = ASTFactory.createIdentifier(consume(ID).text);
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

            consume(IN); // Consume IN after the BY clause
        } else {
            // No BY clause
            by = null; // Let interpreter handle default step
            consume(IN); // Consume IN
        }

        // Parse start and end expressions
        ExprNode start = parseExpression();
        consume(TO);
        ExprNode end = parseExpression();

        // Create AST nodes
        RangeNode range = ASTFactory.createRange(by, start, end);
        ForNode forNode = ASTFactory.createFor(iterator, range);
        setNodePosition(forNode, startToken);

        // Parse loop body
        consume(LBRACE);
        while (!match(RBRACE)) {
            forNode.body.statements.add(parseStatement());
        }
        consume(RBRACE);
        return forNode;
    }

    private StatementNode parseOutputStatement() {
        Token startToken = currentToken();
        consume(OUTPUT);
        OutputNode output = ASTFactory.createOutput();
        setNodePosition(output, startToken);
        output.arguments.add(parseExpression());
        return output;
    }

    private StatementNode parseInputAssignment() {
        Token startToken = currentToken();
        String varName = consume(ID).text;
        consume(ASSIGN);
        consume(LPAREN);
        String type = parseTypeReference();
        consume(RPAREN);
        consume(INPUT);
        InputNode input = ASTFactory.createInput(type, varName);
        setNodePosition(input, startToken);
        return input;
    }

    private StatementNode parseReturnSlotAssignment() {
        Token startToken = currentToken();
        List<String> varNames = parseIdList();
        consume(ASSIGN);
        List<String> slotNames = parseReturnSlots();
        consume(COLON);
        MethodCallNode methodCall = parseMethodCall();
        methodCall.slotNames = slotNames;

        if (varNames.size() != slotNames.size()) {
            throw new ParseError("Number of variables (" + varNames.size() +
                                   ") does not match number of slots (" + slotNames.size() + ")");
        }

        ReturnSlotAssignmentNode assignment = ASTFactory.createReturnSlotAssignment(varNames, methodCall);
        setNodePosition(assignment, startToken);
        return assignment;
    }

    private StatementNode parseIndexAssignment() {
        Token startToken = currentToken();
        ExprNode arrayVar = ASTFactory.createIdentifier(consume(ID).text);
        IndexAccessNode indexAccess = parseIndexAccessContinuation(arrayVar);
        while(match(LBRACKET)) {
            indexAccess = parseIndexAccessContinuation(indexAccess);
        }
        consume(ASSIGN);
        ExprNode value = parseExpression();
        AssignmentNode assignment = ASTFactory.createAssignment(indexAccess, value);
        setNodePosition(assignment, startToken);
        return assignment;
    }

    private StatementNode parseMethodCallStatement() {
        Token startToken = currentToken();
        if (peek(0).type == LBRACKET) {
            List<String> slotNames = parseReturnSlots();
            consume(COLON);
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
        while (match(EQ, NEQ, GT, LT, GTE, LTE)) {
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
        while (match(PLUS, MINUS)) {
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
        while (match(MUL, DIV, MOD)) {
            Token op = consume();
            ExprNode right = parseUnaryExpression();
            left = ASTFactory.createBinaryOp(left, op.text, right);
            setNodePosition(left, startToken);
        }
        return left;
    }

     private ExprNode parseUnaryExpression() {
        Token startToken = currentToken();
        if (match(PLUS, MINUS)) {
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
        consume(LBRACKET);
        List<ExprNode> elements = new ArrayList<ExprNode>();
        if (!match(RBRACKET)) {
            elements.add(parseExpression());
            while (tryConsume(COMMA)) {
                elements.add(parseExpression());
            }
        }
        consume(RBRACKET);
        ArrayNode array = ASTFactory.createArray(elements);
        setNodePosition(array, startToken);
        return array;
    }

    private ExprNode parsePrimaryExpression() {
        ExprNode baseExpr;
        Token startToken = currentToken();

        if (match(LBRACKET)) {
            baseExpr = parseArrayLiteral();
        } else if (match(INT_LIT)) {
            baseExpr = ASTFactory.createIntLiteral(Integer.parseInt(consume().text));
            setNodePosition(baseExpr, startToken);
        } else if (match(FLOAT_LIT)) {
            baseExpr = ASTFactory.createFloatLiteral(Float.parseFloat(consume().text));
            setNodePosition(baseExpr, startToken);
        } else if (match(STRING_LIT)) {
            Token stringToken = consume();
            baseExpr = ASTFactory.createStringLiteral(stringToken.text);
            setNodePosition(baseExpr, stringToken);
        } else if (match(BOOL_LIT)) {
            baseExpr = ASTFactory.createBoolLiteral(Boolean.parseBoolean(consume().text));
            setNodePosition(baseExpr, startToken);
        } else if (match(ID)) {
             if (isMethodCallFollows()) {
                 baseExpr = parseMethodCall();
             } else {
                 baseExpr = ASTFactory.createIdentifier(consume().text);
                 setNodePosition(baseExpr, startToken);
             }
        } else if (match(LPAREN)) {
             if (isTypeCast()) {
                 baseExpr = parseTypeCast();
             } else {
                 consume(LPAREN);
                 baseExpr = parseExpression();
                 consume(RPAREN);
                 setNodePosition(baseExpr, startToken);
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

        consume(LPAREN);
        if (!match(RPAREN)) {
            call.arguments.add(parseExpression());
            while (tryConsume(COMMA)) {
                call.arguments.add(parseExpression());
            }
        }
        consume(RPAREN);
        return call;
    }

     private IndexAccessNode parseIndexAccessContinuation(ExprNode arrayExpr) {
         Token startToken = currentToken();
         consume(LBRACKET);
         ExprNode indexExpr = parseExpression();
         consume(RBRACKET);
         IndexAccessNode access = ASTFactory.createIndexAccess(arrayExpr, indexExpr);
         setNodePosition(access, startToken);
         return access;
     }

    private ExprNode parseTypeCast() {
        Token startToken = currentToken();
        consume(LPAREN);
        String type = parseTypeReference();
        consume(RPAREN);
        ExprNode expressionToCast = parseUnaryExpression();
        TypeCastNode cast = ASTFactory.createTypeCast(type, expressionToCast);
        setNodePosition(cast, startToken);
        return cast;
    }

    private List<String> parseReturnSlots() {
        consume(LBRACKET);
        List<String> slots = parseIdList();
        consume(RBRACKET);
        return slots;
    }

     private List<String> parseIdList() {
         List<String> ids = new ArrayList<String>();
         ids.add(consume(ID).text);
         while (tryConsume(COMMA)) {
             ids.add(consume(ID).text);
         }
         return ids;
     }

    private ParamNode parseParameter() {
        Token startToken = currentToken();
        String type = parseTypeReference();
        Token paramNameToken = currentToken();
        String name = consume(ID).text;
        
        // ENFORCE: No PascalCase for parameters
        NamingValidator.validateParameterName(name, paramNameToken);
        
        ParamNode param = ASTFactory.createParam(name, type);
        setNodePosition(param, startToken);
        return param;
    }

    // --- Predicate Methods ---

    // --- PREDICATES MODIFIED ---
    private boolean isSlotDeclaration() {
        try {
            return tokens.get(position).type == TILDE_BAR;
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }

    private boolean isSlotAssignment() {
        try {
            // Must be '~>' followed by something valid
            if (tokens.get(position).type != TILDE_ARROW) return false;
            if (position + 1 >= tokens.size()) return false;
            
            // Check if followed by ID or expression start
            int nextPos = position + 1;
            Token nextToken = tokens.get(nextPos);
            
            // Valid if followed by ID, literal, or expression start
            return nextToken.type == ID || 
                   nextToken.type == INT_LIT ||
                   nextToken.type == FLOAT_LIT ||
                   nextToken.type == STRING_LIT ||
                   nextToken.type == BOOL_LIT ||
                   nextToken.type == LPAREN ||
                   nextToken.type == LBRACKET ||
                   nextToken.type == PLUS ||
                   nextToken.type == MINUS;
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }

    private boolean isTypeCast() {
        int p = position; try { if (tokens.get(p++).type != LPAREN) return false; if (!isTypeStart(tokens.get(p))) return false; p++; while (p < tokens.size() && tokens.get(p).type == LBRACKET) { if (p + 1 < tokens.size() && tokens.get(p + 1).type == RBRACKET) { p += 2; } else { return false; } } if (p >= tokens.size() || tokens.get(p++).type != RPAREN) return false; if (p >= tokens.size()) return false; int nt = tokens.get(p).type; return nt == LBRACKET || nt == INT_LIT || nt == FLOAT_LIT || nt == STRING_LIT || nt == BOOL_LIT || nt == ID || nt == LPAREN || nt == PLUS || nt == MINUS; } catch (IndexOutOfBoundsException e) { return false; }
    }

    private boolean isMethodDeclaration() {
        int p = position; 
        try { 
            // Check for slot declaration (~|)
            if (tokens.get(p).type == TILDE_BAR) { 
                p++; // Skip past ~|
                while(p < tokens.size() && (tokens.get(p).type == ID || tokens.get(p).type == COMMA)) {
                    p++; // Skip past id list
                }
            }
            
            // Check for builtin modifier OR visibility modifier
            if (tokens.get(p).type == BUILTIN || isVisibilityModifier(tokens.get(p))) {
                p++; 
            }
            
            // Must have ID (method name)
            if (p >= tokens.size() || tokens.get(p).type != ID) return false; 
            p++; 
            
            // Must have LPAREN for parameters
            if (p >= tokens.size() || tokens.get(p).type != LPAREN) return false;
            p++;
            
            // After parameters, must have either TILDE_ARROW or LBRACE
            // Skip parameters and RPAREN
            int parenDepth = 1;
            while (p < tokens.size() && parenDepth > 0) {
                if (tokens.get(p).type == LPAREN) parenDepth++;
                else if (tokens.get(p).type == RPAREN) parenDepth--;
                p++;
            }
            
            // Now check what comes after parameters
            if (p >= tokens.size()) return false;
            
            // Valid if followed by ~> (single expression) or { (block)
            return tokens.get(p).type == TILDE_ARROW || 
                   tokens.get(p).type == LBRACE;
        } catch (IndexOutOfBoundsException e) { 
            return false; 
        }
    }

    private boolean isFieldDeclaration() {
        int p = position; try { if (!isTypeStart(tokens.get(p))) return false; p++; while (p < tokens.size() && tokens.get(p).type == LBRACKET) { if (p + 1 < tokens.size() && tokens.get(p + 1).type == RBRACKET) { p += 2; } else { return false; } } if (p >= tokens.size() || tokens.get(p).type != ID) return false; p++; return p >= tokens.size() || tokens.get(p).type != LPAREN; } catch (IndexOutOfBoundsException e) { return false; }
    }

    private boolean isInputAssignment() {
         int p = position; try { if (tokens.get(p++).type != ID) return false; if (tokens.get(p++).type != ASSIGN) return false; if (tokens.get(p++).type != LPAREN) return false; if (!isTypeStart(tokens.get(p))) return false; p++; while (p < tokens.size() && tokens.get(p).type == LBRACKET) { if (p + 1 < tokens.size() && tokens.get(p + 1).type == RBRACKET) { p += 2; } else { return false; } } if (p >= tokens.size() || tokens.get(p++).type != RPAREN) return false; return p < tokens.size() && tokens.get(p).type == INPUT; } catch (IndexOutOfBoundsException e) { return false; }
     }

     // --- Fixed Simple Assignment Predicate ---
     private boolean isSimpleAssignment() {
         int p = position;
         try {
             if (tokens.get(p).type != ID) return false; // Must start with ID
             // Check NOT index assignment: token after ID should not be LBRACKET
             if (p + 1 < tokens.size() && tokens.get(p + 1).type == LBRACKET) return false;
             // Check IS followed by ASSIGN
             if (p + 1 >= tokens.size() || tokens.get(p + 1).type != ASSIGN) return false;
             // Check NOT input assignment: token after ASSIGN is not LPAREN
             if (p + 2 < tokens.size() && tokens.get(p + 2).type == LPAREN) return false;
             // REMOVED check for LBRACKET after ASSIGN

             // If it passed all the above, it's a simple assignment
             return true;
         } catch (IndexOutOfBoundsException e) {
             return false;
         }
     }

    private boolean isIndexAssignment() {
        int p = position; try { if (tokens.get(p++).type != ID) return false; if (p >= tokens.size() || tokens.get(p++).type != LBRACKET) return false; int bd = 1; while (p < tokens.size() && bd > 0) { if (tokens.get(p).type == LBRACKET) bd++; else if (tokens.get(p).type == RBRACKET) bd--; p++; } if (bd != 0) return false; while (p < tokens.size() && tokens.get(p).type == LBRACKET) { p++; bd = 1; while (p < tokens.size() && bd > 0) { if (tokens.get(p).type == LBRACKET) bd++; else if (tokens.get(p).type == RBRACKET) bd--; p++; } if (bd != 0) return false; } return p < tokens.size() && tokens.get(p).type == ASSIGN; } catch (IndexOutOfBoundsException e) { return false; }
    }

     private boolean isMethodCallFollows() {
         int p = position; try { if (tokens.get(p).type != ID) return false; p++; while(p < tokens.size() && tokens.get(p).type == DOT) { p++; if (p >= tokens.size() || tokens.get(p).type != ID) return false; p++; } return p < tokens.size() && tokens.get(p).type == LPAREN; } catch (IndexOutOfBoundsException e) { return false; }
     }

    private boolean isReturnSlotAssignment() {
        int p = position; try { if (tokens.get(p++).type != ID) return false; while (p < tokens.size() && tokens.get(p).type == COMMA) { p++; if (p >= tokens.size() || tokens.get(p++).type != ID) return false; } if (p >= tokens.size() || tokens.get(p++).type != ASSIGN) return false; if (p >= tokens.size() || tokens.get(p++).type != LBRACKET) return false; if (p >= tokens.size() || tokens.get(p++).type != ID) return false; while (p < tokens.size() && tokens.get(p).type == COMMA) { p++; if (p >= tokens.size() || tokens.get(p++).type != ID) return false; } if (p >= tokens.size() || tokens.get(p++).type != RBRACKET) return false; return p < tokens.size() && tokens.get(p).type == COLON; } catch (IndexOutOfBoundsException e) { return false; }
    }

    private boolean isVariableDeclaration() {
         int p = position; try { Token first = tokens.get(p); if (first.type == VAR) { return p + 1 < tokens.size() && tokens.get(p + 1).type == ID; } else if (isTypeStart(first)) { p++; while (p < tokens.size() && tokens.get(p).type == LBRACKET) { if (p + 1 < tokens.size() && tokens.get(p + 1).type == RBRACKET) { p += 2; } else { return false; } } return p < tokens.size() && tokens.get(p).type == ID; } return false; } catch (IndexOutOfBoundsException e) { return false; }
    }

    private boolean isAssignmentInByClause() {
        int p = position; try { if (tokens.get(p).type != ID) return false; int nt = tokens.get(p + 1).type; return nt == ASSIGN || nt == PLUS_ASSIGN || nt == MINUS_ASSIGN || nt == MUL_ASSIGN || nt == DIV_ASSIGN; } catch (IndexOutOfBoundsException e) { return false; }
    }

    private boolean isMethodCallStatement() {
        int p = position; try { if (tokens.get(p).type == LBRACKET) { p++; if (tokens.get(p).type != ID) return false; p++; while(p < tokens.size() && tokens.get(p).type == COMMA) { p+=2; } if (p >= tokens.size() || tokens.get(p).type != RBRACKET) return false; p++; if (p >= tokens.size() || tokens.get(p).type != COLON) return false; p++; } if (tokens.get(p).type != ID) return false; p++; while(p < tokens.size() && tokens.get(p).type == DOT) { p++; if (p >= tokens.size() || tokens.get(p).type != ID) return false; p++; } return p < tokens.size() && tokens.get(p).type == LPAREN; } catch (IndexOutOfBoundsException e) { return false; }
    }

    // --- Utility and Helper Methods ---

    private String parseQualifiedName() {
        StringBuilder name = new StringBuilder();
        name.append(consume(ID).text);
        while (tryConsume(DOT)) {
            name.append(".");
            name.append(consume(ID).text);
        }
        return name.toString();
    }

    private String parseTypeReference() {
        StringBuilder typeName = new StringBuilder();
        if (isTypeStart(currentToken())) {
            typeName.append(consume().text);
        } else {
            Token current = currentToken();
            throw new ParseError("Expected type name (int, string, float, bool, or ID) but got " +
                getTypeName(current.type) + " ('" + current.text + "')" +
                 " at line " + current.line + ":" + current.column);
        }
        while (match(LBRACKET)) {
             consume(LBRACKET);
             consume(RBRACKET);
             typeName.append("[]");
        }
        return typeName.toString();
    }

    private boolean isTypeKeyword(int type) {
        return type == INT || type == STRING ||
               type == FLOAT || type == BOOL;
    }

     private boolean isTypeStart(Token token) {
         if (token == null) return false;
         return isTypeKeyword(token.type) || token.type == ID;
     }

    private boolean isVisibilityModifier() {
        return match(SHARE, LOCAL);
    }

     private boolean isVisibilityModifier(Token token) {
          if (token == null) return false;
         return token.type == SHARE || token.type == LOCAL;
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
        throw new ParseError("Expected " + getTypeName(expectedType) + " but found " +
                getTypeName(token.type) + " ('" + token.text + "') at line " + token.line + ":" + token.column);
    }

    private Token consume(boolean condition) {
         if (condition) return consume();
         Token current = currentToken();
         throw new ParseError("Consumption condition not met at: " + current.text +
             " (" + getTypeName(current.type) + ")" +
             " at line " + current.line + ":" + current.column);
    }

    private boolean tryConsume(int expectedType) {
        if (match(expectedType)) {
            consume(expectedType);
            return true;
        }
        return false;
    }

    private String getTypeName(int type) {
        return ManualCoderiveLexer.TokenType.getName(type);
    }
}