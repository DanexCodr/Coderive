package cod.parser;

import cod.error.ParseError;
import cod.ast.ASTFactory;
import cod.ast.nodes.*;

import static cod.syntax.Keyword.*;
import cod.syntax.Symbol;
import static cod.syntax.Symbol.*;

import java.util.ArrayList;
import java.util.List;

import static cod.lexer.MainLexer.Token;
import static cod.lexer.MainLexer.TokenType.*;

/**
 * The main parser entry point, responsible for the overall program structure.
 * Uses shared PositionHolder for automatic position synchronization across all parsers.
 * Now includes program type detection and validation for the three-worlds design.
 */
public class MainParser extends BaseParser {

    private final ExpressionParser expressionParser;
    private final StatementParser statementParser;
    private final DeclarationParser declarationParser;

    public MainParser(List<Token> tokens) {
        // Initialize BaseParser with shared PositionHolder
        super(tokens, new PositionHolder(0));
        
        // All parsers share the same position counter - NO synchronization needed!
        this.expressionParser = new ExpressionParser(tokens, this.position);
        this.statementParser = new StatementParser(tokens, this.position, expressionParser);
        this.declarationParser = new DeclarationParser(tokens, this.position, statementParser);
    }

public ProgramNode parseProgram() {
    // Detect program type first
    ProgramType programType = detectProgramType();
    
    // Reset position for actual parsing
    position.set(0);
    
    // Parse based on program type
    ProgramNode program = null;
    try {
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
                throw new ParseError("Unknown program type: " + programType);
        }
    } catch (ParseError e) {
        // Add program type context to error
        throw new ParseError("[" + programType + "] " + e.getMessage(), 
                            e.getLine(), e.getColumn());
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
                getTypeName(current.type) + " ('" + current.text + "')" +
                " at line " + current.line + ":" + current.column);
        }
        return stmt;
    }

    private UnitNode parseUnit() {
        consumeKeyword(UNIT);
        String unitName = parseQualifiedName();
        UnitNode unit = ASTFactory.createUnit(unitName);

        if (isKeyword(USE)) {
            unit.imports = parseUseNode();
        }
        return unit;
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
    
    // Turn off debug output for cleaner test results
    // System.err.println("[DEBUG] Starting program type detection");
    
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
                continue;
            }
            
            // After unit, we should only see class declarations
            if (hasUnit) {
                // Check for class declaration (visibility modifier)
                if (isVisibilityModifier()) {
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
            if (isVisibilityModifier()) {
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
        // System.err.println("[DEBUG] Final detection: " + result);
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

        // Parse imports
        while (isKeyword(USE)) {
            if (program.unit.imports == null) {
                program.unit.imports = parseUseNode();
            } else {
                UseNode additionalImports = parseUseNode();
                program.unit.imports.imports.addAll(additionalImports.imports);
            }
        }

        // Parse classes only
        while (!match(EOF)) {
            if (isVisibilityModifier()) {
                program.unit.types.add(parseTypeDelegation());
            } else {
                throw new ParseError("Modules can only contain class declarations after imports");
            }
        }
        
        return program;
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
            if (isVisibilityModifier() || isMethodDeclarationStart()) {
                throw new ParseError("Scripts cannot contain method or class declarations");
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
                throw new ParseError("Method scripts can only contain method declarations");
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
        return text.equals("output") || 
               text.equals("if") ||
               text.equals("for") ||
               text.equals("exit") ||
               text.equals("input");
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
        // Skip visibility modifier
        consume();
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
    // Skip modifier (share/local)
    consume();
    
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
    
    // Handle both ~> and { syntax
    if (match(TILDE_ARROW)) {
        consume(TILDE_ARROW);
        
        // --- FIX: Skip slot assignments properly ---
        // Skip first slot assignment
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
        } else if ("output".equals(text)) {
            skipOutputStatement();
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
    
    private void skipOutputStatement() {
        consumeKeyword(OUTPUT);
        skipExpression();
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
                    text.equals(OUTPUT.toString()) ||
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
                  t.text.equals("output") || t.text.equals("exit")))) {
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
            throw new ParseError("Modules cannot have direct code outside classes.");
        }
        if (hasMethods && !hasClasses) {
            throw new ParseError("Modules cannot have methods outside classes.");
        }
        return ProgramType.MODULE;
    }
    
    // Rule 2: Has direct code → Script
    if (hasDirectCode) {
        if (hasMethods) {
            throw new ParseError("Cannot mix direct code and method declarations.\n" +
                               "Either:\n" +
                               "1. Remove methods and keep as script, OR\n" +
                               "2. Remove direct code and make it a method script, OR\n" +
                               "3. Add 'unit' and classes to make it a module.");
        }
        if (hasClasses) {
            throw new ParseError("Scripts cannot contain class declarations.");
        }
        return ProgramType.SCRIPT;
    }
    
    // Rule 3: Has methods → Method Script
    if (hasMethods) {
        if (hasClasses) {
            throw new ParseError("Method scripts cannot contain class declarations.");
        }
        return ProgramType.METHOD_SCRIPT;
    }
    
    // Rule 4: Has classes without unit → ERROR
    if (hasClasses) {
        throw new ParseError("Classes require 'unit' declaration.\n" +
                           "Add: unit namespace.name\n" +
                           "Before your class definitions.");
    }
    
    // Empty file or unrecognized
    throw new ParseError("Empty file or unrecognized structure");
}
}