package cod.parser;

import cod.ast.ASTFactory;
import cod.ast.nodes.*;
import cod.error.ParseError;
import cod.lexer.MainLexer.Token;
import cod.semantic.NamingValidator;
import cod.syntax.Keyword;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import static cod.lexer.MainLexer.TokenType.*;
import static cod.syntax.Keyword.*;
import static cod.syntax.Symbol.*;

/**
 * Parses top-level declarations: Types, Methods, and Fields.
 * It requires a reference to the StatementParser to parse method bodies.
 * Uses shared AtomicInteger for automatic position synchronization.
 */
public class DeclarationParser extends BaseParser {

    private final StatementParser statementParser;

    public DeclarationParser(List<Token> tokens, PositionHolder position, StatementParser statementParser) {
        super(tokens, position);
        this.statementParser = statementParser;
    }

    public boolean isConstructorDeclaration() {
        int offset = 0;

        while (lookaheadFrom(0, offset) != null &&
                (lookaheadFrom(0, offset).type == WS ||
                        lookaheadFrom(0, offset).type == LINE_COMMENT ||
                        lookaheadFrom(0, offset).type == BLOCK_COMMENT)) {
            offset++;
        }

        Token first = lookaheadFrom(0, offset);
        if (first == null || !isVisibilityModifier(first)) return false;
        offset++;

        while (lookaheadFrom(0, offset) != null &&
                (lookaheadFrom(0, offset).type == WS ||
                        lookaheadFrom(0, offset).type == LINE_COMMENT ||
                        lookaheadFrom(0, offset).type == BLOCK_COMMENT)) {
            offset++;
        }

        Token nameToken = lookaheadFrom(0, offset);
        if (nameToken == null) return false;

        boolean isThisKeyword = nameToken.type == KEYWORD && "this".equals(nameToken.text);
        boolean isIDNamedThis = nameToken.type == ID && "this".equals(nameToken.text);

        return isThisKeyword || isIDNamedThis;
    }

    public ConstructorNode parseConstructor() {
        Token startToken = currentToken();

        consume(isVisibilityModifier(currentToken()));

        String name = consume().text;
        if (!"this".equals(name)) {
            throw new ParseError("Constructor must be named 'this', found: " + name,
                    startToken.line, startToken.column);
        }

        ConstructorNode constructor = ASTFactory.createConstructor(null, null);

        consume(LPAREN);
        if (!match(RPAREN)) {
            constructor.parameters.add(parseParameter());
            while (tryConsume(COMMA)) {
                constructor.parameters.add(parseParameter());
            }
        }
        consume(RPAREN);

        if (match(TILDE_ARROW)) {
            consume(TILDE_ARROW);
            constructor.body.add(parseConstructorSlotAssignment());
        } else if (match(LBRACE)) {
            consume(LBRACE);
            while (!match(RBRACE)) {
                constructor.body.add(statementParser.parseStatement());
            }
            consume(RBRACE);
        } else {
            throw new ParseError("Constructor must have either '~>' or '{' body",
                    currentToken().line, currentToken().column);
        }

        return constructor;
    }

    private SlotAssignmentNode parseConstructorSlotAssignment() {
        String slotName = null;
        ExprNode value;

        if (currentToken().type == ID) {
            Token afterId = lookahead(1);
            if (afterId != null && afterId.symbol == COLON) {
                slotName = consume(ID).text;
                consume(COLON);
                value = statementParser.expressionParser.parseExpression();
            } else {
                slotName = null;
                value = statementParser.expressionParser.parseExpression();
            }
        } else {
            slotName = null;
            value = statementParser.expressionParser.parseExpression();
        }

        return ASTFactory.createSlotAssignment(slotName, value);
    }

    public TypeNode parseType() {
        Token visibilityToken = currentToken();
        String visibilityText = consume(isVisibilityModifier()).text;

        Keyword visibility;
        if (SHARE.toString().equals(visibilityText)) {
            visibility = Keyword.SHARE;
        } else if (LOCAL.toString().equals(visibilityText)) {
            visibility = Keyword.LOCAL;
        } else {
            throw new ParseError(
                    "Internal parser error: isVisibilityModifier() returned true for non-visibility keyword: '" + visibilityText + "'",
                    visibilityToken.line, visibilityToken.column);
        }

        Token typeNameToken = currentToken();
        String typeName = consume(ID).text;

        NamingValidator.validateClassName(typeName, typeNameToken);

        // NEW: Parse optional inheritance
        String extendName = null;
        if (isKeyword(IS)) {
            consumeKeyword(IS);
            extendName = parseQualifiedName(); // Could be "ParentClass" or "package.ParentClass"
        }

        TypeNode type = ASTFactory.createType(typeName, visibility, extendName);

        consume(LBRACE);
        while (!match(RBRACE)) {
            if (isFieldDeclaration()) {
                type.fields.add(parseField());
            } else if (isConstructorDeclaration()) {
                ConstructorNode constructor = parseConstructor();
                type.constructors.add(constructor);
            } else if (isMethodDeclaration()) {
                MethodNode method = parseMethod();
                method.associatedClass = type.name;
                type.methods.add(method);
            } else {
                type.statements.add(statementParser.parseStatement());
            }
        }
        consume(RBRACE);
        return type;
    }

    public MethodNode parseMethod() {
    Token startToken = currentToken();

    boolean isBuiltin = false;
    Keyword visibility = Keyword.SHARE;

    if (isKeyword(BUILTIN)) {
        consumeKeyword(BUILTIN);
        isBuiltin = true;
        visibility = Keyword.SHARE;
    } else if (isVisibilityModifier()) {
        Token visibilityToken = currentToken();
        String visibilityText = consume().text;

        if (SHARE.toString().equals(visibilityText)) {
            visibility = Keyword.SHARE;
        } else if (LOCAL.toString().equals(visibilityText)) {
            visibility = Keyword.LOCAL;
        } else {
            throw new ParseError(
                    "Internal parser error: isVisibilityModifier() returned true for non-visibility keyword: '" + visibilityText + "'",
                    visibilityToken.line, visibilityToken.column);
        }
    }

    // CHANGED: Allow both ID and certain keywords as method names
    String methodName;
    if (currentToken().type == KEYWORD && canKeywordBeMethodName(currentToken().text)) {
        methodName = currentToken().text;
        consume();
    } else if (currentToken().type == ID) {
        methodName = consume(ID).text;
    } else {
        throw new ParseError("Expected method name (identifier or allowed keyword)",
                currentToken().line, currentToken().column);
    }

    NamingValidator.validateMethodName(methodName, startToken);

    // We initialize with empty slots first, populate them later if '::' exists
    MethodNode method = ASTFactory.createMethod(methodName, visibility, null);
    method.isBuiltin = isBuiltin;

    // Parse parameters
    consume(LPAREN);
    
    // SPECIAL HANDLING FOR BUILTIN METHODS
    if (isBuiltin) {
        // For builtin methods, skip everything inside parentheses
        // (could be documentation, not real parameters)
        int parenDepth = 1;
        while (!match(EOF) && parenDepth > 0) {
            Token t = currentToken();
            if (t.symbol == LPAREN) {
                parenDepth++;
            } else if (t.symbol == RPAREN) {
                parenDepth--;
                if (parenDepth == 0) {
                    consume(RPAREN);
                    break;
                }
            }
            consume();
        }
    } else {
        // Regular method: parse actual parameters
        if (!match(RPAREN)) {
            method.parameters.add(parseParameter());
            while (tryConsume(COMMA)) {
                method.parameters.add(parseParameter());
            }
        }
        consume(RPAREN);
    }

    // Check for Slot Contract '::'
    if (isSlotDeclaration()) {
        method.returnSlots = parseSlotContractList();
    } else {
        method.returnSlots = new ArrayList<SlotNode>();
    }

    // --- CRITICAL FIX: Skip whitespace/comments before checking for ~> or { ---
    skipWhitespaceAndComments();

    // --- BUILTIN METHOD HANDLING ---
    if (isBuiltin) {
        // Builtin methods can have documentation after the signature
        // Skip any tokens until we reach a new method/field or end of type
        while (position.get() < tokens.size()) {
            Token current = currentToken();
            
            // Stop at tokens that indicate the start of something else
            if (current.symbol == RBRACE ||  // End of type
                isVisibilityModifier(current) ||  // Start of new method/field
                current.type == KEYWORD && (BUILTIN.toString().equals(current.text) || 
                                           SHARE.toString().equals(current.text) || 
                                           LOCAL.toString().equals(current.text))) {
                break;
            }
            
            // Skip the token
            consume();
        }
        
        // Validate that builtin methods don't have a body
        if (match(TILDE_ARROW) || match(LBRACE)) {
            Token current = currentToken();
            throw new ParseError(
                    "Builtin method '" + methodName + "' cannot have a body. " +
                            "Builtin methods are only declarations, not implementations.\n" +
                            "Remove '~>' or '{...}' after builtin method signature.",
                    current.line, current.column);
        }

        return method;
    }

    // --- REGULAR METHOD CONTINUES BELOW ---

    // Check for inline return (~>) or block ({)
    if (match(TILDE_ARROW)) {
        // Inline return
        consume(TILDE_ARROW);

        // Skip whitespace before parsing slot assignments
        skipWhitespaceAndComments();

        // Parse multiple slot assignments for inline return
        List<SlotAssignmentNode> slotAssignments = new ArrayList<>();

        // Parse first slot assignment
        slotAssignments.add(parseSingleSlotAssignment());

        // Parse additional comma-separated slot assignments
        while (tryConsume(COMMA)) {
            skipWhitespaceAndComments(); // Skip whitespace after comma
            slotAssignments.add(parseSingleSlotAssignment());
        }

        // Add to method body
        if (slotAssignments.size() == 1) {
            method.body.add(slotAssignments.get(0));
        } else {
            MultipleSlotAssignmentNode multiAssign = ASTFactory.createMultipleSlotAssignment(slotAssignments);
            method.body.add(multiAssign);
        }

    } else if (match(LBRACE)) {
        // Block return
        consume(LBRACE);
        while (!match(RBRACE)) {
            method.body.add(statementParser.parseStatement());
        }
        consume(RBRACE);
    } else {
        // Error: Method must have either ~> or {
        Token current = currentToken();
        throw new ParseError(
                "Expected '~>' or '{' after method signature, but found " +
                        getTypeName(current.type) + " ('" + current.text + "')", currentToken().line, currentToken().column);
    }

    return method;
}

    private SlotAssignmentNode parseSingleSlotAssignment() {
        String slotName = null;
        ExprNode value;

        // Check for named slot: "slotName: expression"
        if (currentToken().type == ID) {
            Token afterId = lookahead(1);
            if (afterId != null && afterId.symbol == COLON) {
                // Named slot: "sum: a + b"
                slotName = consume(ID).text;
                consume(COLON);
                value = statementParser.expressionParser.parseExpression();
            } else {
                // Positional slot: just an expression
                slotName = null;
                value = statementParser.expressionParser.parseExpression();
            }
        } else {
            // Positional slot: just an expression
            slotName = null;
            value = statementParser.expressionParser.parseExpression();
        }

        return ASTFactory.createSlotAssignment(slotName, value);
    }

    public List<SlotNode> parseSlotContractList() {
        consume(DOUBLE_COLON);

        List<SlotNode> slots = new ArrayList<SlotNode>();

        boolean firstSlot = true;
        boolean isNamedMode = false;
        int index = 0;

        do {
            String name;
            String type;

            if (firstSlot) {
                if (currentToken().type == ID) {
                    // Named slot: name: type
                    isNamedMode = true;
                    name = consume(ID).text;
                    consume(COLON);
                    type = parseTypeReference();
                } else {
                    // Unnamed slot: just type
                    isNamedMode = false;
                    name = String.valueOf(index);
                    type = parseTypeReference();
                }
                firstSlot = false;
            } else {
                if (isNamedMode) {
                    if (currentToken().type != ID) {
                        throw new ParseError("Mixed slot declaration styles not allowed. Expected name for slot.", currentToken().line, currentToken().column);
                    }
                    name = consume(ID).text;
                    consume(COLON);
                    type = parseTypeReference();
                } else {
                    if (currentToken().type == ID) {
                        throw new ParseError("Mixed slot declaration styles not allowed. Found name '" + currentToken().text + "' in unnamed slot list.", currentToken().line, currentToken().column);
                    }
                    name = String.valueOf(index);
                    type = parseTypeReference();
                }
            }

            slots.add(ASTFactory.createSlot(type, name));
            index++;

        } while (tryConsume(COMMA));

        return slots;
    }

    public FieldNode parseField() {
        Token startToken = currentToken();

        // Parse optional visibility
        Keyword visibility = null;
        if (isVisibilityModifier()) {
            Token visToken = consume();
            if (SHARE.toString().equals(visToken.text)) {
                visibility = SHARE;
            } else if (LOCAL.toString().equals(visToken.text)) {
                visibility = LOCAL;
            }
        }

        // Parse name (FIRST!)
        String fieldName = consume(ID).text;

        // REJECT underscore as field name
        if ("_".equals(fieldName)) {
            throw new ParseError(
                    "Field name cannot be '_'. Underscore is reserved for discard/placeholder.",
                    startToken.line, startToken.column
            );
        }

        // Parse colon
        consume(COLON);

        // Parse type (SECOND!)
        String fieldType = parseTypeReference();

        // Validate name
        if (NamingValidator.isAllCaps(fieldName)) {
            NamingValidator.validateConstantName(fieldName, startToken);
        } else {
            NamingValidator.validateVariableName(fieldName, startToken);
        }

        // Create field
        FieldNode field = ASTFactory.createField(fieldName, fieldType);
        if (visibility != null) {
            // Store visibility if your AST supports it
            // field.visibility = visibility; // Uncomment if FieldNode has this field
        }

        // Parse optional initializer
        if (tryConsume(ASSIGN)) {
            field.value = statementParser.expressionParser.parseExpression();
        }

        return field;
    }

    public ParamNode parseParameter() {
        Token startToken = currentToken();
        String name = consume(ID).text;

        // NEW: Check for := syntax (inferred from literal)
        if (match(DOUBLE_COLON_ASSIGN)) {
            consume(DOUBLE_COLON_ASSIGN);

            // Parse default value (must be a literal for inference)
            ExprNode defaultValue = statementParser.expressionParser.parsePrimaryExpression();

            // NEW: Validate it's a literal (not method call or complex expression)
            if (!isSimpleLiteral(defaultValue)) {
                throw new ParseError(
                        "Parameter inference (:=) can only be used with literals. " +
                                "Use explicit typing for expressions: " + name + ": Type = expression",
                        startToken.line, startToken.column
                );
            }

            // Infer type from literal
            String inferredType = inferTypeFromLiteral(defaultValue);
            if (inferredType == null) {
                throw new ParseError(
                        "Cannot infer parameter type from literal. Use explicit typing: " +
                                name + ": Type = " + defaultValue,
                        startToken.line, startToken.column
                );
            }

            // Validate parameter name
            NamingValidator.validateParameterName(name, startToken);

            // Create param with inferred type
            ParamNode param = ASTFactory.createParam(name, inferredType, defaultValue, true);
            param.hasDefaultValue = true;
            return param;
        }

        // Original: name: Type [= expression]
        consume(COLON);
        String type = parseTypeReference();

        ExprNode defaultValue = null;
        if (tryConsume(ASSIGN)) {
            // This can be ANY expression (literal OR method call)
            defaultValue = statementParser.expressionParser.parseExpression();
        }

        NamingValidator.validateParameterName(name, startToken);
        ParamNode param = ASTFactory.createParam(name, type, defaultValue, false);
        if (defaultValue != null) {
            param.hasDefaultValue = true;
        }
        return param;
    }

    private boolean isSimpleLiteral(ExprNode expr) {
        // Check if expression is a SIMPLE literal

        // 1. Basic literals (int, float, text, bool, null)
        if (expr instanceof ExprNode) {
            ExprNode e = (ExprNode) expr;
            if (e.value != null || e.isNull) {
                return e.name == null; // Not an identifier
            }
        }

        // 2. Array literals
        if (expr instanceof ArrayNode) {
            ArrayNode arr = (ArrayNode) expr;

            // Check if it's a range NaturalArray (could be [1 to 10] or [by 2 in 1 to 10])
            if (arr.elements.size() == 1 && arr.elements.get(0) instanceof RangeNode) {
                RangeNode range = (RangeNode) arr.elements.get(0);
                // Check start and end are simple, step could be null or simple
                return isSimpleLiteral(range.start) &&
                        isSimpleLiteral(range.end) &&
                        (range.step == null || isSimpleLiteral(range.step));
            }

            // Regular array - check all elements
            for (ExprNode elem : arr.elements) {
                if (!isSimpleLiteral(elem)) return false;
            }
            return true;
        }

        // 3. RangeNode (if parsed separately somehow)
        if (expr instanceof RangeNode) {
            RangeNode range = (RangeNode) expr;
            return isSimpleLiteral(range.start) &&
                    isSimpleLiteral(range.end) &&
                    (range.step == null || isSimpleLiteral(range.step));
        }

        // 4. Tuple literals
        if (expr instanceof TupleNode) {
            TupleNode tuple = (TupleNode) expr;
            for (ExprNode elem : tuple.elements) {
                if (!isSimpleLiteral(elem)) return false;
            }
            return true;
        }

        return false;
    }

    private String inferTypeFromLiteral(ExprNode expr) {
        if (expr == null) return null;

        // 1. Basic ExprNode with value
        if (expr instanceof ExprNode) {
            ExprNode e = (ExprNode) expr;

            if (e.isNull) return null; // Can't infer from null

            Object value = e.value;
            if (value instanceof Integer || value instanceof Long) return "int";
            if (value instanceof Float || value instanceof Double || value instanceof BigDecimal) return "float";
            if (value instanceof String) return "text";
            if (value instanceof Boolean) return "bool";
        }

        // 2. Array literals
        if (expr instanceof ArrayNode) {
            ArrayNode arr = (ArrayNode) expr;
            if (arr.elements.isEmpty()) return null; // Can't infer empty array

            // Check if it's a single range (NaturalArray)
            if (arr.elements.size() == 1 && arr.elements.get(0) instanceof RangeNode) {
                return "[]"; // NaturalArray is treated as dynamic array []
            }

            // Try to infer from first element
            String elementType = inferTypeFromLiteral(arr.elements.get(0));
            if (elementType != null) {
                return "[" + elementType + "]";
            }
            return null;
        }

        // 3. RangeNode directly (if somehow parsed separately)
        if (expr instanceof RangeNode) {
            return "[]"; // NaturalArray as dynamic array
        }

        // 4. Tuple literals
        if (expr instanceof TupleNode) {
            TupleNode tuple = (TupleNode) expr;
            if (tuple.elements.isEmpty()) return null;

            StringBuilder sb = new StringBuilder("(");
            for (int i = 0; i < tuple.elements.size(); i++) {
                String elemType = inferTypeFromLiteral(tuple.elements.get(i));
                if (elemType == null) return null; // Can't infer if any element unknown

                if (i > 0) sb.append(",");
                sb.append(elemType);
            }
            sb.append(")");
            return sb.toString();
        }

        return null;
    }

    // --- Lookahead Methods ---

    public boolean isSlotDeclaration() {
        // [CHANGED]: Checks for :: instead of ~:
        return isSymbolAt(0, DOUBLE_COLON);
    }

    private boolean isMethodDeclaration() {
        int offset = 0;

        // Skip leading whitespace
        while (lookaheadFrom(0, offset) != null &&
                (lookaheadFrom(0, offset).type == WS ||
                        lookaheadFrom(0, offset).type == LINE_COMMENT ||
                        lookaheadFrom(0, offset).type == BLOCK_COMMENT)) {
            offset++;
        }

        // 1. Skip modifiers (builtin / share / local)
        Token first = lookaheadFrom(0, offset);
        if (first == null) return false;

        if (BUILTIN.toString().equals(first.text) ||
                SHARE.toString().equals(first.text) ||
                LOCAL.toString().equals(first.text)) {
            offset++;
        }

        // Skip whitespace after modifier
        while (lookaheadFrom(0, offset) != null &&
                (lookaheadFrom(0, offset).type == WS ||
                        lookaheadFrom(0, offset).type == LINE_COMMENT ||
                        lookaheadFrom(0, offset).type == BLOCK_COMMENT)) {
            offset++;
        }

        // 2. Check for method ID name OR Allowed Keyword (FIX)
        Token nameToken = lookaheadFrom(0, offset);
        if (nameToken == null) return false;

        // FIX: Allow specific keywords to be method names (e.g. "in")
        boolean isValidName = (nameToken.type == ID) || 
                              (nameToken.type == KEYWORD && canKeywordBeMethodName(nameToken.text));

        if (!isValidName) return false;

        offset++; // Skip the name

        // Skip whitespace after name
        while (lookaheadFrom(0, offset) != null &&
                (lookaheadFrom(0, offset).type == WS ||
                        lookaheadFrom(0, offset).type == LINE_COMMENT ||
                        lookaheadFrom(0, offset).type == BLOCK_COMMENT)) {
            offset++;
        }

        // 3. Check for opening parenthesis
        Token parenToken = lookaheadFrom(0, offset);
        if (parenToken == null || parenToken.symbol != LPAREN) return false;

        // If we have "share name (", it's a method.
        return true;
    }

    private boolean isFieldDeclaration() {
        int currentPos = getPosition();
        int p = currentPos;

        // Skip optional visibility modifier (local/share)
        if (p < tokens.size() && isVisibilityModifier(tokens.get(p))) {
            p++;
        }

        if (p >= tokens.size()) return false;

        // Pattern: name: type
        // 1. Must start with ID (field name)
        if (tokens.get(p).type != ID) {
            return false;
        }

        p++; // Skip the field name

        // 2. Must have colon after name
        if (p >= tokens.size() || tokens.get(p).symbol != COLON) {
            return false;
        }

        p++; // Skip colon

        // 3. Must have a valid type after colon
        if (p >= tokens.size() || !isTypeStart(tokens.get(p))) {
            return false;
        }

        // 4. Check it's not a method call
        // After "name: type", check what comes next
        int afterTypePos = skipType(p);
        if (afterTypePos == -1) return false;

        // If after type we see '(', it's a method parameter list
        if (afterTypePos < tokens.size() && tokens.get(afterTypePos).symbol == LPAREN) {
            return false; // It's a method declaration
        }

        // Otherwise, it's a field declaration
        return true;
    }

    private boolean isVisibilityModifier(Token token) {
        if (token == null) return false;
        return (token.type == KEYWORD && (token.text.equals(SHARE.toString()) || token.text.equals(LOCAL.toString())));
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
}