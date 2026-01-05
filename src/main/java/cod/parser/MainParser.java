package cod.parser;

import cod.ast.ASTFactory;
import cod.ast.nodes.*;
import cod.error.ParseError;
import cod.interpreter.Interpreter;
import cod.syntax.Symbol;
import java.util.ArrayList;
import java.util.List;
import cod.lexer.Token;
import static cod.lexer.TokenType.*;
import static cod.syntax.Keyword.*;
import static cod.syntax.Symbol.*;

/**
 * The main parser entry point, responsible for the overall program structure.
 * Uses shared PositionHolder for automatic position synchronization across all parsers.
 * Now includes program type detection and validation for the three-worlds design.
 */
public class MainParser extends BaseParser {

    private final ExpressionParser expressionParser;
    private final StatementParser statementParser;
    private final DeclarationParser declarationParser;
    private final Interpreter interpreter;

    public MainParser(List<Token> tokens) {
        this(tokens, null);
    }
    
    public MainParser(List<Token> tokens, Interpreter interpreter) {
        // Initialize BaseParser with shared PositionHolder
        super(tokens, new PositionHolder(0));
        
        // All parsers share the same position counter
        this.expressionParser = new ExpressionParser(tokens, this.position);
        this.statementParser = new StatementParser(tokens, this.position, expressionParser);
        this.declarationParser = new DeclarationParser(tokens, this.position, statementParser);
        this.interpreter = interpreter;
    }
    
    private Interpreter getInterpreter() {
        return interpreter;
    }
    
public ProgramNode parseProgram() {
    // Detect program type first
    ProgramType programType = detectProgramType();
    
    // Reset position for actual parsing
    position.set(0);
    
    // Parse based on program type
    ProgramNode program = null;
    // REMOVE THE TRY-CATCH BLOCK - let errors propagate naturally
    switch (programType) {
        case MODULE:
            program = parseModuleProgram();
            break;
        case SCRIPT:
            program = parseScriptProgram();
            break;
        case METHOD_SCRIPT:
            program = parseMethodScriptProgram();
            break;
        default:
            throw new ParseError("Unknown program type: " + programType, 
                currentToken().line, currentToken().column);
    }
    
    // Set program type
    program.programType = programType;
    
    // Validate program structure
    cod.semantic.ProgramValidator.validate(program, programType);
    
    return program;
}

    /**
     * Entry method to parse a single line statement (e.g., for REPL/debugging).
     */
    public StmtNode parseSingleLine() {
        if (match(EOF)) {
            return null;
        }

        // Automatic position sharing - no synchronization needed!
        StmtNode stmt = statementParser.parseStatement();

        if (!match(EOF)) {
            Token current = currentToken();
            throw new ParseError("Unexpected token after statement: " +
                getTypeName(current.type) + " ('" + current.text + "')", current.line, current.column);
        }
        return stmt;
    }

private UnitNode parseUnit() {
    consumeKeyword(UNIT);
    String unitName = parseQualifiedName();
    
    // NEW: Parse optional main class specification
    String mainClassName = null;
    if (match(LPAREN)) {
        consume(LPAREN);
        
        // Expect "main:" identifier followed by colon
        Token mainToken = currentToken();
        Token colonToken = lookahead(1);
        
        if (mainToken == null || colonToken == null || 
            mainToken.type != ID || !"main".equals(mainToken.text) || 
            colonToken.symbol != COLON) {
            throw new ParseError(
                "Expected 'main:' in unit declaration",
                mainToken != null ? mainToken.line : currentToken().line,
                mainToken != null ? mainToken.column : currentToken().column
            );
        }
        
        consume(); // consume "main" (as ID)
        consume(COLON);
        
        mainClassName = parseQualifiedName();
        
        consume(RPAREN);
    }
    
    UnitNode unit = ASTFactory.createUnit(unitName);
    unit.mainClassName = mainClassName;
    
    if (isKeyword(USE)) {
        unit.imports = parseUseNode();
    }
    return unit;
}

private void skipOptionalMainClass() {
    consume(LPAREN);
    
    // Skip "main: ClassName" pattern
    // Look for: any token "main", then ":", then identifiers/dots, then ")"
    while (!match(RPAREN) && !match(EOF)) {
        consume();
    }
    if (match(RPAREN)) {
        consume(RPAREN);
    }
}

    private UseNode parseUseNode() {
        consumeKeyword(USE);
        consume(LBRACE);
        List<String> imports = new ArrayList<String>();
        if (!match(RBRACE)) {
            imports.add(parseQualifiedName());
            while (tryConsume(COMMA)) {
                imports.add(parseQualifiedName());
            }
        }
        consume(RBRACE);
        UseNode getNode = ASTFactory.createUseNode(imports);
        return getNode;
    }

    private TypeNode parseTypeDelegation() {
        // Automatic position sharing - no synchronization needed!
        return declarationParser.parseType();
    }
    
    // =========================================================================
    // NEW METHODS FOR THREE-WORLDS DESIGN
    // =========================================================================

/**
 * Detects the program type based on file structure.
 */
private ProgramType detectProgramType() {
    // Save current position
    int savedPos = position.get();
    position.set(0);
    
    try {
        boolean hasUnit = false;
        boolean hasDirectCode = false;
        boolean hasMethods = false;
        boolean hasClasses = false;
        
        // Track if we're inside imports (skip them)
        boolean skippingImports = false;
        
        // Scan tokens
        while (!match(EOF)) {
            Token current = currentToken();
            
            // Skip imports (use keyword)
            if (current.type == KEYWORD && USE.toString().equals(current.text)) {
                skippingImports = true;
                consume(); // consume "use"
                if (match(LBRACE)) {
                    consume(LBRACE);
                    skipUntil(RBRACE);
                }
                continue;
            }
            
// Check for unit declaration
if (!hasUnit && current.type == KEYWORD && UNIT.toString().equals(current.text)) {
    hasUnit = true;
    consume(); // consume "unit"
    // Skip unit name
    if (match(ID)) {
        parseQualifiedName();
    }
    
    // NEW: Skip optional (main: ClassName)
    if (match(LPAREN)) {
        skipOptionalMainClass();
    }
    continue;
}
            
            // After unit, we should only see class declarations
            if (hasUnit) {
                // Check for class declaration (visibility modifier OR just class name)
                if (isClassStart()) {
                    hasClasses = true;
                    // Skip the class declaration
                    skipTypeDeclaration();
                    continue;
                }
                // Anything else after unit is an error
                if (!skippingImports) {
                    hasDirectCode = true; // This will cause validation error
                }
                consume();
                continue;
            }
            
            // No unit yet, check for other patterns
            
            // Check for method declaration (BOTH syntaxes)
            if (isMethodDeclarationStart()) {
                hasMethods = true;
                skipMethodDeclaration();
                continue;
            }
            
            // --- CRITICAL FIX: Handle ~> as part of method, not direct code ---
            if (current.symbol == TILDE_ARROW) {
                // This is likely a method's return arrow
                // Look back to see if we might have missed a method declaration
                // For simplicity, treat it as part of a method
                hasMethods = true;
                consume(); // skip ~>
                // Skip the slot assignment expression
                skipExpression();
                continue;
            }
            
            // Check for class declaration (without unit - error)
            if (isClassStart()) {
                hasClasses = true;
                skipTypeDeclaration();
                continue;
            }
            
            // Check for direct code/statements
            if (looksLikeDirectCode(current)) {
                hasDirectCode = true;
                skipStatement();
                continue;
            }
            
            // Skip other tokens
            consume();
        }
        
        // Apply detection rules
        ProgramType result = determineProgramType(hasUnit, hasDirectCode, hasMethods, hasClasses);
        return result;
        
    } finally {
        // Restore position
        position.set(savedPos);
    }
}

    /**
     * Parse a MODULE program (has unit declaration).
     */
    private ProgramNode parseModuleProgram() {
    ProgramNode program = ASTFactory.createProgram();

    // Must have unit
    program.unit = parseUnit();
    
    // NEW: Register broadcast if specified
    if (program.unit.mainClassName != null) {
        try {
            // Extract package name from unit name
            String packageName = extractPackageName(program.unit.name);
            interpreter.getImportResolver().registerBroadcast(
                packageName, program.unit.mainClassName
            );
        } catch (Exception e) {
        }
    }

    // Parse imports
    while (isKeyword(USE)) {
        if (program.unit.imports == null) {
            program.unit.imports = parseUseNode();
        } else {
            UseNode additionalImports = parseUseNode();
            program.unit.imports.imports.addAll(additionalImports.imports);
        }
    }

    // Parse classes only - UPDATED to handle optional visibility
    List<TypeNode> typesInFile = new ArrayList<TypeNode>();
    while (!match(EOF)) {
        if (isVisibilityModifier() || isClassStartWithoutModifier()) {
            TypeNode type = parseTypeDelegation();
            program.unit.types.add(type);
            typesInFile.add(type);
        } else {
            throw new ParseError("Modules can only contain class declarations after imports", 
                currentToken().line, currentToken().column);
        }
    }
    
    // NEW: Validate that broadcasted main class exists in this file
    validateMainClassExistsInFile(program.unit, typesInFile);
    
    return program;
}

private String extractPackageName(String qualifiedName) {
    if (qualifiedName == null || qualifiedName.isEmpty()) {
        return "";
    }
    
    // Extract package from qualified name like "my.app.subpackage"
    int lastDot = qualifiedName.lastIndexOf('.');
    if (lastDot > 0) {
        return qualifiedName.substring(0, lastDot);
    }
    return qualifiedName;
}

private void validateMainClassExistsInFile(UnitNode unit, List<TypeNode> typesInFile) {
    if (unit.mainClassName == null || unit.mainClassName.isEmpty()) {
        return; // No main class specified, no validation needed
    }
    
    boolean classFound = false;
    for (TypeNode type : typesInFile) {
        if (type.name.equals(unit.mainClassName)) {
            classFound = true;
            
            // Additional check: make sure it has a main() method
            boolean hasMainMethod = false;
            for (MethodNode method : type.methods) {
                if ("main".equals(method.methodName)) {
                    hasMainMethod = true;
                    break;
                }
            }
            
            if (!hasMainMethod) {
                // ADD PROGRAM TYPE CONTEXT DIRECTLY HERE
                throw new ParseError(
                    "[MODULE] Broadcasted class '" + unit.mainClassName + 
                    "' must have a main() method",
                    currentToken().line, currentToken().column
                );
            }
            break;
        }
    }
    
    if (!classFound) {
        // ADD PROGRAM TYPE CONTEXT DIRECTLY HERE
        throw new ParseError(
            "[MODULE] Cannot broadcast undefined class '" + unit.mainClassName + "'\n" +
            "Define " + unit.mainClassName + " in this file before broadcasting it\n" +
            "Example:\n" +
            "  unit " + unit.name + " (main: " + unit.mainClassName + ")\n" +
            "  \n" +
            "  " + unit.mainClassName + " {\n" +
            "      share main() {\n" +
            "          // Your code here\n" +
            "      }\n" +
            "  }",
            currentToken().line, currentToken().column
        );
    }
}
    
    /**
     * Parse a SCRIPT program (direct code only).
     */
    private ProgramNode parseScriptProgram() {
        ProgramNode program = ASTFactory.createProgram();
        program.unit = ASTFactory.createUnit("default");

        // Parse imports
        while (isKeyword(USE)) {
            if (program.unit.imports == null) {
                program.unit.imports = parseUseNode();
            } else {
                UseNode additionalImports = parseUseNode();
                program.unit.imports.imports.addAll(additionalImports.imports);
            }
        }

        // Create synthetic type to hold statements
        TypeNode scriptType = ASTFactory.createType("__Script__", SHARE, null);

        // Parse statements directly
        while (!match(EOF)) {
            if (isVisibilityModifier() || isClassStartWithoutModifier() || isMethodDeclarationStart()) {  // <<< UPDATED
                throw new ParseError("Scripts cannot contain method or class declarations", currentToken().line, currentToken().column);
            }
            scriptType.statements.add(statementParser.parseStatement());
        }
        
        program.unit.types.add(scriptType);
        return program;
    }
    
    /**
     * Parse a METHOD_SCRIPT program (methods only).
     */
    private ProgramNode parseMethodScriptProgram() {
        ProgramNode program = ASTFactory.createProgram();
        program.unit = ASTFactory.createUnit("default");

        // Parse imports
        while (isKeyword(USE)) {
            if (program.unit.imports == null) {
                program.unit.imports = parseUseNode();
            } else {
                UseNode additionalImports = parseUseNode();
                program.unit.imports.imports.addAll(additionalImports.imports);
            }
        }

        // Create synthetic type to hold methods
        TypeNode methodScriptType = ASTFactory.createType("__MethodScript__", SHARE, null);

        // Parse method declarations only
        while (!match(EOF)) {
            if (!isMethodDeclarationStart()) {
                // Check if it's a class (with or without visibility) - that's also an error
                if (isVisibilityModifier() || isClassStartWithoutModifier()) {  // <<< UPDATED
                    throw new ParseError("Method scripts cannot contain class declarations", currentToken().line, currentToken().column);
                }
                throw new ParseError("Method scripts can only contain method declarations", currentToken().line, currentToken().column);
            }
            methodScriptType.methods.add(declarationParser.parseMethod());
        }
        
        program.unit.types.add(methodScriptType);
        return program;
    }

    // =========================================================================
    // HELPER METHODS FOR PROGRAM TYPE DETECTION
    // =========================================================================
    
    private boolean isMethodDeclarationStart() {
    Token first = currentToken();
    if (first == null) return false;
    
    // Must be local or share keyword
    if (first.type == KEYWORD) {
        String text = first.text;
        if (LOCAL.toString().equals(text) || SHARE.toString().equals(text)) {
            Token second = lookahead(1);
            if (second != null && second.type == ID) {
                Token third = lookahead(2);
                // Method if followed by '('
                if (third != null && third.symbol == LPAREN) {
                    return true;
                }
            }
        }
    }
    return false;
}
    
    private boolean looksLikeDirectCode(Token token) {
    if (token == null) return false;
    
    // --- FIX: ~> is NOT direct code, it's part of method ---
    if (token.symbol == TILDE_ARROW) {
        return false; // This is method return syntax
    }
    
    // Keywords that start statements (NOT method declarations)
    if (token.type == KEYWORD) {
        String text = token.text;
        
        // Check if this is a method declaration start
        if ("local".equals(text) || "share".equals(text)) {
            // Look ahead to see if it's a method
            Token next1 = lookahead(1);
            Token next2 = lookahead(2);
            if (next1 != null && next1.type == ID && 
                next2 != null && next2.symbol == LPAREN) {
                return false; // It's a method declaration, NOT direct code
            }
        }
        
        // Only these keywords are direct code
        return
               text.equals("if") ||
               text.equals("for") ||
               text.equals("exit");
    }
    
    // Variable declaration/assignment
    if (token.type == ID) {
        Token next = lookahead(1);
        if (next != null) {
            // name := value
            if (next.symbol == DOUBLE_COLON_ASSIGN) return true;
            // name = value
            if (next.symbol == ASSIGN) return true;
            // name: type or name: type = value
            if (next.symbol == COLON) return true;
        }
        return true; // Could be method call or variable reference
    }
    
    // Expression start
    return isExpressionStart(token);
}

private void skipTypeDeclaration() {
    // Skip optional visibility modifier
    if (isVisibilityModifier()) {
        consume();
    }
    
    // Skip type name
    if (match(ID)) consume();
    
    // Skip 'is' and base type if present
    if (isKeyword(IS)) {
        consumeKeyword(IS);
        parseQualifiedName();
    }
    
    // Skip opening brace
    if (match(LBRACE)) consume(LBRACE);
    
    // Skip everything until matching closing brace
    int braceDepth = 1;
    while (!match(EOF) && braceDepth > 0) {
        Token t = currentToken();
        if (t.symbol == LBRACE) braceDepth++;
        else if (t.symbol == RBRACE) braceDepth--;
        consume();
    }
}
    
    private void skipMethodDeclaration() {
    // Check for builtin modifier
    boolean isBuiltin = false;
    if (match(KEYWORD) && BUILTIN.toString().equals(currentToken().text)) {
        isBuiltin = true;
        consume(); // skip "builtin"
    }
    
    // Skip modifier (share/local)
    if (isVisibilityModifier()) {
        consume();
    }
    
    // Skip method name
    if (match(ID)) consume();
    
    // Skip parameters
    if (match(LPAREN)) {
        consume(LPAREN);
        skipUntil(RPAREN);
    }
    
    // Skip return slots if present
    if (match(DOUBLE_COLON)) {
        consume(DOUBLE_COLON);
        skipSlotContract();
    }
    
    // --- NEW: Builtin methods have no body ---
    if (isBuiltin) {
        return; // Builtin methods end here
    }
    
    // Handle both ~> and { syntax (only for non-builtin methods)
    if (match(TILDE_ARROW)) {
        consume(TILDE_ARROW);
        
        // Skip slot assignments properly
        skipSlotAssignment();
        
        // Skip additional comma-separated slot assignments
        while (tryConsume(COMMA)) {
            skipSlotAssignment();
        }
        
    } else if (match(LBRACE)) {
        consume(LBRACE);
        skipUntil(RBRACE);
    }
}

// NEW helper method
private void skipSlotAssignment() {
    // Skip optional slot name and colon
    if (match(ID)) {
        Token next = lookahead(1);
        if (next != null && next.symbol == COLON) {
            consume(); // skip name
            consume(); // skip colon
        }
    }
    // Skip the expression
    skipExpression();
}
    
    private void skipSlotContract() {
        do {
            // Skip name: type or just type
            if (match(ID) && isSymbolAt(1, COLON)) {
                consume(); // name
                consume(COLON);
            }
            // Skip type
            skipTypeReference();
        } while (tryConsume(COMMA));
    }
    
    private void skipStatement() {
    Token current = currentToken();
    
    if (current.type == KEYWORD) {
        String text = current.text;
        if ("if".equals(text)) {
            skipIfStatement();
        } else if ("for".equals(text)) {
            skipForStatement();
        } else if ("exit".equals(text)) {
            consume(); // exit
        } else if ("local".equals(text) || "share".equals(text)) {
            // Check if this is a method declaration
            Token next = lookahead(1);
            if (next != null && next.type == ID) {
                Token afterNext = lookahead(2);
                if (afterNext != null && afterNext.symbol == LPAREN) {
                    // It's a method declaration
                    skipMethodDeclaration();
                } else {
                    // Not a method, just skip the keyword
                    consume();
                }
            } else {
                consume();
            }
        } else {
            // Skip other keywords
            consume();
        }
    } else if (current.type == ID) {
        // Skip until statement end
        skipUntilStatementEnd();
    } else {
        // Expression or other
        consume();
    }
}

private void skipForStatement() {
    // Skip "for"
    consumeKeyword(FOR);
    
    // Skip iterator name
    if (match(ID)) consume();
    
    // Skip optional "by"
    if (isKeyword(BY)) {
        consumeKeyword(BY);
        // Skip step expression
        skipExpression();
    }
    
    // Skip "in"
    if (isKeyword(IN)) {
        consumeKeyword(IN);
    }
    
    // Skip start expression
    skipExpression();
    
    // Skip "to"
    if (isKeyword(TO)) {
        consumeKeyword(TO);
    } else {
        // If we don't find "to", just return
        return;
    }
    
    // Skip end expression
    skipExpression();
    
    // Skip body (could be block or single statement)
    if (match(LBRACE)) {
        consume(LBRACE);
        skipUntil(RBRACE);
    } else {
        skipStatement();
    }
}
    
    private void skipIfStatement() {
        consumeKeyword(IF);
        skipExpression();
        
        // Skip then block
        if (match(LBRACE)) {
            consume(LBRACE);
            skipUntil(RBRACE);
        } else {
            skipStatement();
        }
        
        // Skip else/elif if present
        while (isKeyword(ELIF) || isKeyword(ELSE)) {
            consume();
            if (isKeyword(IF)) {
                consumeKeyword(IF);
                skipExpression();
            }
            
            if (match(LBRACE)) {
                consume(LBRACE);
                skipUntil(RBRACE);
            } else {
                skipStatement();
            }
        }
    }
    
    private void skipExpression() {
    // UPDATED: Now tracks nesting and handles Commas/Keywords correctly
    int braceDepth = 0;
    int parenDepth = 0;
    int bracketDepth = 0;

    while (!match(EOF)) {
        Token t = currentToken();
        
        // 1. Handle Nesting
        if (t.symbol == LBRACE) braceDepth++;
        else if (t.symbol == RBRACE) braceDepth--;
        else if (t.symbol == LPAREN) parenDepth++;
        else if (t.symbol == RPAREN) parenDepth--;
        else if (t.symbol == LBRACKET) bracketDepth++;
        else if (t.symbol == RBRACKET) bracketDepth--;

        // If we closed a nesting level that we didn't open in this context (e.g. end of method body), stop.
        if (braceDepth < 0 || parenDepth < 0 || bracketDepth < 0) {
            return;
        }

        // 2. Check Stop Conditions (Only at depth 0)
        if (braceDepth == 0 && parenDepth == 0 && bracketDepth == 0) {
            // Stop at COMMA (necessary for slot lists: ~> a, b)
            if (t.symbol == COMMA) {
                return;
            }
            
            // Stop at keywords that start new statements or declarations
            if (t.type == KEYWORD) {
                String text = t.text;
                if (text.equals(IF.toString()) || 
                    text.equals(FOR.toString()) || 
                    text.equals(EXIT.toString()) ||
                    text.equals(ELSE.toString()) ||
                    text.equals(ELIF.toString()) ||
                    // CRITICAL FIX: Stop at method/class modifiers
                    text.equals("share") || 
                    text.equals("local") ||
                    text.equals("unit")) {
                    return;
                }
            }
        }

        consume();
    }
}
    
    private void skipUntil(Symbol symbol) {
        while (!match(EOF) && !match(symbol)) {
            Token t = currentToken();
            if (t.symbol == LBRACE || t.symbol == LPAREN || t.symbol == LBRACKET) {
                // Skip nested
                consume();
                skipUntilMatching(t.symbol);
            } else {
                consume();
            }
        }
        if (match(symbol)) consume();
    }
    
    private void skipUntilMatching(Symbol opening) {
        Symbol closing;
        if (opening == LBRACE) closing = RBRACE;
        else if (opening == LPAREN) closing = RPAREN;
        else if (opening == LBRACKET) closing = RBRACKET;
        else return;
        
        skipUntil(closing);
    }
    
    private void skipUntilStatementEnd() {
        while (!match(EOF)) {
            Token t = currentToken();
            
            // Statement end markers - NO SEMICOLON IN YOUR LANGUAGE
            if (t.symbol == RBRACE ||
                (t.type == KEYWORD && 
                 (t.text.equals(ELSE.toString()) || t.text.equals("elif") ||
                  t.text.equals("if") || t.text.equals("for") ||
                  t.text.equals("exit")))) {
                break;
            }
            
            // Skip nested structures
            if (t.symbol == LBRACE || t.symbol == LPAREN || t.symbol == LBRACKET) {
                consume();
                skipUntilMatching(t.symbol);
            } else {
                consume();
            }
        }
    }
    
    private void skipTypeReference() {
        if (match(LBRACKET)) {
            consume(LBRACKET);
            if (!match(RBRACKET)) {
                skipTypeReference();
            }
            consume(RBRACKET);
        } else if (match(LPAREN)) {
            consume(LPAREN);
            skipTypeReference();
            while (tryConsume(COMMA)) {
                skipTypeReference();
            }
            consume(RPAREN);
        } else if (isTypeStart(currentToken())) {
            consume();
        }
        
        // Skip union
        while (tryConsume(PIPE)) {
            skipTypeReference();
        }
    }
    
    private ProgramType determineProgramType(boolean hasUnit, boolean hasDirectCode, 
                                       boolean hasMethods, boolean hasClasses) {
    
    // Rule 1: Has unit → MUST be Module
    if (hasUnit) {
        if (hasDirectCode) {
            throw new ParseError("[MODULE] Modules cannot have direct code outside classes.", 
                currentToken().line, currentToken().column);
        }
        if (hasMethods && !hasClasses) {
            throw new ParseError("[MODULE] Modules cannot have methods outside classes.", 
                currentToken().line, currentToken().column);
        }
        return ProgramType.MODULE;
    }
    
    // Rule 2: Has direct code → Script
    if (hasDirectCode) {
        if (hasMethods) {
            throw new ParseError("[SCRIPT] Cannot mix direct code and method declarations.\n" +
                               "Either:\n" +
                               "1. Remove methods and keep as script, OR\n" +
                               "2. Remove direct code and make it a method script, OR\n" +
                               "3. Add 'unit' and classes to make it a module.", 
                currentToken().line, currentToken().column);
        }
        if (hasClasses) {
            throw new ParseError("[SCRIPT] Scripts cannot contain class declarations.", 
                currentToken().line, currentToken().column);
        }
        return ProgramType.SCRIPT;
    }
    
    // Rule 3: Has methods → Method Script
    if (hasMethods) {
        if (hasClasses) {
            throw new ParseError("[METHOD_SCRIPT] Method scripts cannot contain class declarations.", 
                currentToken().line, currentToken().column);
        }
        return ProgramType.METHOD_SCRIPT;
    }
    
    // Rule 4: Has classes without unit → ERROR
    if (hasClasses) {
        throw new ParseError("[UNKNOWN] Classes require 'unit' declaration.\n" +
                           "Add: unit namespace.name\n" +
                           "Before your class definitions.", 
            currentToken().line, currentToken().column);
    }
    
    // Empty file or unrecognized
    throw new ParseError("[UNKNOWN] Empty file or unrecognized structure", 1, 1);
}
}