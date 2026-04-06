package cod.parser;

import cod.ast.ASTFactory;
import cod.ast.nodes.*;
import cod.error.ParseError;
import cod.interpreter.Interpreter;
import cod.interpreter.registry.GlobalRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
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
            ("local".equals(currentToken.getText()) || "share".equals(currentToken.getText()))) {
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
    validateProgramStructure(program, topLevelStatements, topLevelMethods, typesInFile, policiesInFile);
    
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
        validateModule(program, typesInFile, policiesInFile);
    }
    
    return program;
}

    private boolean isTopLevelMethodDeclaration() {
        return next(new ParserAction<Boolean>() {
            @Override
            public Boolean parse() throws ParseError {
                ParserState savedState = getCurrentState();
                try {
                    if (is(SHARE, LOCAL)) {
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

    private void validateProgramStructure(Program program, 
                                         List<Stmt> topLevelStatements,
                                         List<Method> topLevelMethods,
                                         List<Type> typesInFile,
                                         List<Policy> policiesInFile) {
        
        boolean hasUnit = program.unit.name != null && !program.unit.name.equals(DEFAULT_UNIT_NAME);
        
        List<Stmt> actualStatements = new ArrayList<Stmt>();
        for (Stmt stmt : topLevelStatements) {
            if (stmt instanceof Block) {
                Block block = (Block) stmt;
                if (!block.statements.isEmpty()) {
                    actualStatements.add(stmt);
                }
            } else if (stmt != null) {
                actualStatements.add(stmt);
            }
        }
        
        boolean hasDirectCode = !actualStatements.isEmpty();
        boolean hasMethods = !topLevelMethods.isEmpty();
        boolean hasClasses = !typesInFile.isEmpty();
        boolean hasPolicies = !policiesInFile.isEmpty();
        boolean hasMainClassDeclaration = !nil(program.unit.mainClassName);
        boolean isSelfBroadcast = hasMainClassDeclaration && SELF_BROADCAST_NAME.equals(program.unit.mainClassName);
        
        if (hasDirectCode) {
            if (hasMethods) {
                throw error("Cannot mix method declarations with direct code. Use a class or unit.", now());
            }
            if (hasClasses || hasPolicies) {
                throw error("Cannot mix class/policy declarations with direct script code in the same file.", now());
            }
            if (hasUnit && !isSelfBroadcast) {
                throw error(
                    "Script unit declarations are only allowed with self broadcast: unit <name> (main: this)",
                    now()
                );
            }
            program.programType = ProgramType.SCRIPT;
            return;
        }
        
        if (hasMethods) {
            if (!hasUnit) {
                throw error("Static modules with top-level methods must declare a unit.", now());
            }
            if (isSelfBroadcast) {
                throw error("Self broadcast (main: this) is only valid for script files with direct code.", now());
            }
            program.programType = ProgramType.STATIC_MODULE;
            return;
        }
        
        if (hasClasses || hasPolicies) {
            if (hasUnit && isSelfBroadcast) {
                throw error("Self broadcast (main: this) is only valid for script files with direct code.", now());
            }
            program.programType = ProgramType.MODULE;
            return;
        }
        
        if (hasUnit) {
            if (isSelfBroadcast) {
                program.programType = ProgramType.SCRIPT;
            } else {
                program.programType = ProgramType.STATIC_MODULE;
            }
            return;
        }
        
        program.programType = ProgramType.SCRIPT;
    }

    private void validateModule(Program program, 
                               List<Type> typesInFile,
                               List<Policy> policiesInFile) {
        if (interpreter != null) {
            String filePath = interpreter.getCurrentFilePath();
            if (filePath != null) {
                validateUnitAgainstFilePath(program.unit.name, filePath);
            }
        }

        if (!nil(program.unit.mainClassName) && interpreter != null) {
            try {
                String packageName = extractPackageName(program.unit.name);
                interpreter.getImportResolver().registerBroadcast(
                    packageName, program.unit.mainClassName
                );
            } catch (Exception e) {
                // Ignore
            }
        }

        validateMainClassExistsInFile(program.unit, typesInFile);
        validateImplementedPolicies(program.unit, typesInFile, policiesInFile);
        
        for (Type type : typesInFile) {
            declarationParser.validateAllPolicyMethods(type, program);
            declarationParser.validateClassViralPolicies(type, program);
        }
    }

    private void validateUnitAgainstFilePath(String unitName, String filePath) {
        if (nil(filePath, unitName)) return;
        
        if (unitName.isEmpty()) {
            throw error("Unit name cannot be empty");
        }
        
        if (!filePath.endsWith(".cod")) {
            throw error("File must have .cod extension");
        }
        
        String dirName = extractDirNameNoFileIO(filePath);
        if (dirName.isEmpty()) {
            validateFileInCurrentDirectory(unitName, filePath);
            return;
        }
        
        String firstUnitPart;
        int dotIndex = unitName.indexOf('.');
        if (dotIndex != -1) {
            firstUnitPart = unitName.substring(0, dotIndex);
        } else {
            firstUnitPart = unitName;
        }
        
        if (!firstUnitPart.isEmpty() && !dirName.isEmpty() && 
            firstUnitPart.charAt(0) != dirName.charAt(0)) {
            throw error(
                "Unit name '" + unitName + "' doesn't match directory '" + dirName + "'");
        }
        
        int lengthDiff = Math.abs(firstUnitPart.length() - dirName.length());
        if (lengthDiff > 3) {
            throw error(
                "Unit name '" + unitName + "' doesn't match directory '" + dirName + "'");
        }
        
        if (!firstUnitPart.isEmpty() && !dirName.isEmpty() &&
            firstUnitPart.charAt(firstUnitPart.length() - 1) != dirName.charAt(dirName.length() - 1)) {
            throw error(
                "Unit name '" + unitName + "' doesn't match directory '" + dirName + "'");
        }
        
        if (quickPathMatchCheck(unitName, filePath)) {
            return;
        }
        
        String expectedUnit = calculateExpectedUnit(filePath);
        if (!unitName.equals(expectedUnit)) {
            throw error(
                "Unit name '" + unitName + "' doesn't match directory structure");
        }
    }

    private String extractDirNameNoFileIO(String filePath) {
        int len = filePath.length();
        
        if (len > 4 && filePath.endsWith(".cod")) {
            len -= 4;
        }
        
        int lastSeparator = -1;
        for (int i = len - 1; i >= 0; i--) {
            char c = filePath.charAt(i);
            if (c == '/' || c == '\\') {
                lastSeparator = i;
                break;
            }
        }
        
        if (lastSeparator == -1) {
            return "";
        }
        
        int prevSeparator = -1;
        for (int i = lastSeparator - 1; i >= 0; i--) {
            char c = filePath.charAt(i);
            if (c == '/' || c == '\\') {
                prevSeparator = i;
                break;
            }
        }
        
        if (prevSeparator == -1) {
            return filePath.substring(0, lastSeparator);
        }
        
        return filePath.substring(prevSeparator + 1, lastSeparator);
    }

    private void validateFileInCurrentDirectory(String unitName, String filePath) {
        if (unitName.isEmpty()) {
            throw error("Unit name cannot be empty");
        }
        
        if (unitName.contains(" ")) {
            throw error("Unit name cannot contain spaces: '" + unitName + "'");
        }
    }

    private boolean quickPathMatchCheck(String unitName, String filePath) {
        String unitAsPath = unitName.replace('.', '/');
        
        String pathWithoutExt = filePath;
        if (filePath.endsWith(".cod")) {
            pathWithoutExt = filePath.substring(0, filePath.length() - 4);
        }
        
        pathWithoutExt = pathWithoutExt.replace('\\', '/');
        
        if (pathWithoutExt.endsWith("/" + unitAsPath) || 
            pathWithoutExt.equals(unitAsPath)) {
            return true;
        }
        
        int lastSlash = pathWithoutExt.lastIndexOf('/');
        if (lastSlash != -1) {
            String parentPath = pathWithoutExt.substring(0, lastSlash);
            if (parentPath.endsWith("/" + unitAsPath) || 
                parentPath.equals(unitAsPath)) {
                return true;
            }
        }
        
        return false;
    }

    private String calculateExpectedUnit(String filePath) {
        String normalized = filePath.replace('\\', '/');
        
        String srcMain = "src/main/";
        int srcMainIndex = normalized.indexOf(srcMain);
        
        if (srcMainIndex == -1) {
            return "";
        }
        
        String relative = normalized.substring(srcMainIndex + srcMain.length());
        
        if (relative.endsWith(".cod")) {
            relative = relative.substring(0, relative.length() - 4);
        }
        
        String[] parts = relative.split("/");
        List<String> unitParts = new ArrayList<String>();
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;
            
            boolean looksLikeFileName = part.length() > 0 && Character.isUpperCase(part.charAt(0));
            
            if (i == parts.length - 1 && looksLikeFileName) {
                continue;
            }
            
            unitParts.add(part);
        }
        
        if (unitParts.isEmpty()) {
            return "";
        }
        
        StringBuilder unitName = new StringBuilder();
        for (int i = 0; i < unitParts.size(); i++) {
            if (i > 0) unitName.append(".");
            unitName.append(unitParts.get(i));
        }
        
        return unitName.toString();
    }

    private void validateMainClassExistsInFile(Unit unit, List<Type> typesInFile) {
        if (unit.mainClassName == null || unit.mainClassName.isEmpty()) {
            return;
        }
        
        boolean classFound = false;
        for (Type type : typesInFile) {
            if (unit.mainClassName.equals(type.name)) {
                classFound = true;
                
                boolean hasMainMethod = false;
                for (Method method : type.methods) {
                    if (method.methodName.equals("main")) {
                        hasMainMethod = true;
                        break;
                    }
                }
                
                if (!hasMainMethod) {
                    throw error(
                        "[MODULE] Broadcasted class '" + unit.mainClassName + 
                        "' must have a main() method");
                }
                break;
            }
        }
        
        if (!classFound) {
            throw error(
                "[MODULE] Cannot broadcast undefined class '" + unit.mainClassName + "'\n" +
                "Define " + unit.mainClassName + " in this file before broadcasting it\n" +
                "Example:\n" +
                "  unit " + unit.name + " (main: " + unit.mainClassName + ")\n" +
                "  \n" +
                "  " + unit.mainClassName + " {\n" +
                "      share main() {\n" +
                "          // Your code here\n" +
                "      }\n" +
                "  }");
        }
    }

    private void validateImplementedPolicies(Unit unit, List<Type> types, List<Policy> policies) {
        Map<String, Policy> policyMap = new HashMap<String, Policy>();
        for (Policy policy : policies) {
            policyMap.put(policy.name, policy);
        }
        
        for (Type type : types) {
            for (String policyName : type.implementedPolicies) {
                if (!policyMap.containsKey(policyName)) {
                    throw error(
                        "Class '" + type.name + "' implements undefined policy '" + policyName + "'\n" +
                        "Available policies: " + policyMap.keySet());
                }
            }
        }
    }

    private String extractPackageName(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isEmpty()) {
            return "";
        }
        
        int lastDot = qualifiedName.lastIndexOf('.');
        if (lastDot > 0) {
            return qualifiedName.substring(0, lastDot);
        }
        return qualifiedName;
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
            
            if (is(first, LOCAL, SHARE, BUILTIN, POLICY)) {
                Token second = next();
                if (is(second, ID) || canBeMethod(second)) {
                    Token third = next(2);
                    return is(third, LPAREN);
                }
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
