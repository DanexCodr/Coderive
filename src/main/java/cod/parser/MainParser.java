package cod.parser;

import cod.ast.ASTFactory;
import cod.ast.node.*;
import cod.error.ParseError;
import cod.interpreter.Interpreter;
import cod.interpreter.registry.GlobalRegistry;
import cod.semantic.ModuleValidator;
import java.util.ArrayList;
import java.util.List;
import cod.lexer.Token;
import static cod.lexer.TokenType.*;
import cod.parser.context.*;
import static cod.syntax.Keyword.*;
import static cod.syntax.Symbol.*;

public class MainParser extends BaseParser {
    private static final String DEFAULT_UNIT_NAME = "default";
    private static final String SELF_BROADCAST_NAME = "this";

    private final ExpressionParser expressionParser;
    private final StatementParser statementParser;
    private final DeclarationParser declarationParser;
    private final Interpreter interpreter;
    
    public enum ProgramType {
    /** Only direct statements - no methods, no classes, no unit */
    SCRIPT,
    
    /** Unit-based static module with top-level methods/fields and classes */
    STATIC_MODULE,
    
    /** Legacy module mode */
    MODULE
}

    public MainParser(List<Token> tokens) {
        this(tokens, null);
    }
    
    public MainParser(List<Token> tokens, Interpreter interpreter) {
        super(new ParserContext(new ParserState(tokens)));
        this.interpreter = interpreter;
        
        GlobalRegistry globalRegistry = interpreter != null ? 
            interpreter.getGlobalRegistry() : null;
        
        this.expressionParser = new ExpressionParser(ctx, globalRegistry, null);
        this.statementParser = new StatementParser(ctx, expressionParser);
        this.expressionParser.setStatementParser(this.statementParser);
        this.declarationParser = new DeclarationParser(ctx, statementParser, 
            interpreter != null ? interpreter.getImportResolver() : null);
    }
    
    @Override
    protected BaseParser createIsolatedParser(ParserContext isolatedCtx) {
        return new MainParser(isolatedCtx.getState().getTokens(), interpreter);
    }
    
    public Program parseProgram() {
    Program program = ASTFactory.createProgram();
    
    // UNIT declaration is optional (required only for static modules)
    if (is(UNIT)) {
        program.unit = parseUnit();
    } else {
        program.unit = ASTFactory.createUnit(DEFAULT_UNIT_NAME, (Token) null);
    }

    // Parse USE statements (imports)
    while (is(USE)) {
        if (program.unit.imports == null) {
            program.unit.imports = parseUseNode();
        } else {
            Use additionalImports = parseUseNode();
            program.unit.imports.imports.addAll(additionalImports.imports);
        }
    }

    // Parse everything else at top level
    List<Type> typesInFile = new ArrayList<>();
    List<Policy> policiesInFile = new ArrayList<>();
    List<Stmt> topLevelStatements = new ArrayList<>();
    List<Method> topLevelMethods = new ArrayList<>();
    List<Field> topLevelFields = new ArrayList<>();
    
    while (!is(EOF)) {
        Token currentToken = now();
        
        // DIRECT CHECK: If we see "local" or "share" text, try to parse as method first
        if (currentToken != null && 
            ("local".equals(currentToken.getText())
                || "share".equals(currentToken.getText())
                || "unsafe".equals(currentToken.getText()))) {
            ParserState savedState = getCurrentState();
            try {
                Method method = declarationParser.parseMethod();
                topLevelMethods.add(method);
                continue;
            } catch (ParseError e) {
                // Not a valid method, restore state and continue
                setState(savedState);
            }
        }
        
        // Check for method declarations
        if (isMethodDeclarationStart() || isTopLevelMethodDeclaration()) {
            Method method = declarationParser.parseMethod();
            topLevelMethods.add(method);
            continue;
        }
        
        if (isTopLevelFieldDeclaration()) {
            Field field = declarationParser.parseField();
            topLevelFields.add(field);
            continue;
        }
        
        if (declarationParser.isPolicyDeclaration()) {
            Policy policy = declarationParser.parsePolicy();
            program.unit.policies.add(policy);
            policiesInFile.add(policy);
            continue;
        }
        
        if (isClassStart() || isClassStartWithoutModifier()) {
            ParserState beforeType = getCurrentState();
            
            Type type = declarationParser.parseType();
            if (type != null) {
                program.unit.types.add(type);
                typesInFile.add(type);
                continue;
            } else {
                setState(beforeType);
                Method method = declarationParser.parseMethod();
                topLevelMethods.add(method);
                continue;
            }
        }
        
        // Must be a statement at top level
        Stmt stmt = statementParser.parseStmt();
        topLevelStatements.add(stmt);
    }
    
    // Now validate the program structure and set final type
    program.programType = ModuleValidator.determineProgramType(
        program,
        topLevelStatements,
        topLevelMethods,
        typesInFile,
        policiesInFile,
        now()
    );
    
    // Add top-level elements to the appropriate places based on program type
    if (program.programType == ProgramType.STATIC_MODULE) {
        Type staticModuleType = findOrCreateImplicitType(program.unit, "__StaticModule__");
        staticModuleType.methods.addAll(topLevelMethods);
        staticModuleType.fields.addAll(topLevelFields);
    } else if (program.programType == ProgramType.SCRIPT) {
        Type scriptType = findOrCreateImplicitType(program.unit, "__Script__");
        scriptType.statements.addAll(topLevelStatements);
    }
    
    // Validate module-specific rules if this is a module
    if (program.programType == ProgramType.STATIC_MODULE || program.programType == ProgramType.MODULE) {
        ModuleValidator.validateModule(
            program,
            typesInFile,
            policiesInFile,
            interpreter,
            declarationParser,
            now()
        );
    }
    
    return program;
}

    private boolean isTopLevelMethodDeclaration() {
        return next(new ParserAction<Boolean>() {
            @Override
            public Boolean parse() throws ParseError {
                ParserState savedState = getCurrentState();
                try {
                    if (is(SHARE, LOCAL, UNSAFE)) {
                        consume();
                    }
                    while (is(BUILTIN, POLICY, UNSAFE)) {
                        consume();
                    }
                    
                    Token nameToken = now();
                    if (!is(nameToken, ID) && !canBeMethod(nameToken)) {
                        return false;
                    }
                    consume();
                    
                    if (!is(LPAREN)) {
                        return false;
                    }
                    consume();
                    
                    int parenDepth = 1;
                    while (!is(EOF) && parenDepth > 0) {
                        if (is(LPAREN)) parenDepth++;
                        else if (is(RPAREN)) parenDepth--;
                        consume();
                    }
                    
                    if (is(DOUBLE_COLON)) {
                        return true;
                    }
                    
                    return is(TILDE_ARROW, LBRACE);
                } finally {
                    setState(savedState);
                }
            }
        });
    }

    private Type findOrCreateImplicitType(Unit unit, String typeName) {
        for (Type type : unit.types) {
            if (type.name.equals(typeName)) {
                return type;
            }
        }
        Type implicitType = ASTFactory.createType(typeName, SHARE, null, null);
        unit.types.add(implicitType);
        return implicitType;
    }

    private Unit parseUnit() {
        Token unitToken = now();
        expect(UNIT);
        String unitName = parseQualifiedName();
        
        String mainClassName = null;
        if (is(LPAREN)) {
            ParserState beforeMainCheck = getCurrentState();
            
            expect(LPAREN);
            
            Token mainToken = now();
            Token colonToken = next();
            
            if (nil(mainToken, colonToken) || 
                mainToken.type != ID || !mainToken.getText().equals("main") || 
                colonToken.symbol != COLON) {
                setState(beforeMainCheck);
            } else {
                consume();
                expect(COLON);
                
                Token mainTargetToken = now();
                if (is(mainTargetToken, THIS) || (is(mainTargetToken, ID) && SELF_BROADCAST_NAME.equals(mainTargetToken.getText()))) {
                    mainClassName = consume().getText();
                } else {
                    mainClassName = parseQualifiedName();
                }
                
                expect(RPAREN);
            }
        }
        
        Unit unit = ASTFactory.createUnit(unitName, unitToken);
        unit.mainClassName = mainClassName;
        return unit;
    }

    private Use parseUseNode() {
        Token useToken = now();
        expect(USE);
        expect(LBRACE);
        List<String> imports = new ArrayList<String>();
        if (!is(RBRACE)) {
            imports.add(parseUseImportSpec());
            while (consume(COMMA)) {
                imports.add(parseUseImportSpec());
            }
        }
        expect(RBRACE);
        return ASTFactory.createUseNode(imports, useToken);
    }

    private String parseUseImportSpec() {
        StringBuilder spec = new StringBuilder();
        int parenDepth = 0;
        int bracketDepth = 0;
        
        while (!is(EOF)) {
            if (parenDepth == 0 && bracketDepth == 0 && (is(COMMA) || is(RBRACE))) {
                break;
            }
            Token token = consume();
            if (token == null || token.type == EOF) {
                break;
            }
            if (token.symbol == LPAREN) parenDepth++;
            else if (token.symbol == RPAREN && parenDepth > 0) parenDepth--;
            else if (token.symbol == LBRACKET) bracketDepth++;
            else if (token.symbol == RBRACKET && bracketDepth > 0) bracketDepth--;
            
            spec.append(token.getText());
        }
        
        String value = spec.toString();
        if (value.isEmpty()) {
            throw error("Expected import spec inside use block (e.g. unit.Class, unit.method(*), unit.*, unit.**, unit.FIELD).");
        }
        return value;
    }

    private boolean isTopLevelFieldDeclaration() {
        return next(new ParserAction<Boolean>() {
            @Override
            public Boolean parse() throws ParseError {
                ParserState savedState = getCurrentState();
                try {
                    if (is(SHARE, LOCAL)) {
                        consume();
                    }
                    
                    if (!is(ID)) {
                        return false;
                    }
                    consume();
                    
                    if (!is(COLON)) {
                        return false;
                    }
                    
                    return !is(next(), COLON);
                } finally {
                    setState(savedState);
                }
            }
        });
    }

    private boolean isMethodDeclarationStart() {
        ParserState savedState = getCurrentState();
        try {
            Token first = now();
            if (first == null) return false;
            
            int offset = 0;
            Token token = next(offset);

            if (is(token, LOCAL, SHARE, UNSAFE, BUILTIN, POLICY)) {
                offset++;
                token = next(offset);
            }
            while (is(token, BUILTIN, POLICY, UNSAFE)) {
                offset++;
                token = next(offset);
            }
            if (is(token, ID) || canBeMethod(token)) {
                Token afterName = next(offset + 1);
                return is(afterName, LPAREN);
            }
            return false;
        } finally {
            setState(savedState);
        }
    }

    public Stmt parseSingleLine() {
        if (is(EOF)) {
            return null;
        }

        Stmt stmt = statementParser.parseStmt();

        if (!is(EOF)) {
            Token current = now();
            throw error("Unexpected token after statement: " +
                getTypeName(current.type) + " ('" + current.getText() + "')", current);
        }
        return stmt;
    }
}
