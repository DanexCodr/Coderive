package cod.parser;

import cod.ast.ASTFactory;
import cod.ast.nodes.*;
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

    private final ExpressionParser expressionParser;
    private final StatementParser statementParser;
    private final DeclarationParser declarationParser;
    private final Interpreter interpreter;
    
    public enum ProgramType {
    /** Only direct statements - no methods, no classes, no unit */
    SCRIPT,
    
    /** Only methods - no direct code, no classes, no unit */
    METHOD_SCRIPT,
    
    /** Unit declaration with classes only - no direct code, no methods outside classes */
    MODULE
}

    public MainParser(List<Token> tokens) {
        this(tokens, null);
    }
    
    public MainParser(List<Token> tokens, Interpreter interpreter) {
        super(new ParserContext(new ParserState(tokens)), new cod.ast.ASTFactory());
        this.interpreter = interpreter;
        
        GlobalRegistry globalRegistry = interpreter != null ? 
            interpreter.getGlobalRegistry() : null;
        
        this.expressionParser = new ExpressionParser(ctx, this.factory, globalRegistry, null);
        this.statementParser = new StatementParser(ctx, expressionParser);
        this.expressionParser.setStatementParser(this.statementParser);
        this.declarationParser = new DeclarationParser(ctx, statementParser, 
            interpreter != null ? interpreter.getImportResolver() : null);
    }
    
    @Override
    protected BaseParser createIsolatedParser(ParserContext isolatedCtx) {
        return new MainParser(isolatedCtx.getState().getTokens(), interpreter);
    }
    
    public int parseProgram() {
    int programId = factory.createProgram();
    
    // First, check for UNIT declaration
    if (is(UNIT)) {
        factory.getAST().programSetUnit(programId, parseUnit());
        factory.getAST().programSetType(programId, "MODULE");
    } else {
        // No unit - create a default unit
        factory.getAST().programSetUnit(programId, factory.createUnit("default", null));
        factory.getAST().programSetType(programId, "SCRIPT"); // Will be updated after validation
    }

    // Parse USE statements (imports)
    while (is(USE)) {
        int useId = parseUseNode();
        factory.getAST().unitSetImports(factory.getAST().programUnit(programId), useId);
    }

    // Parse everything else at top level
    List<Integer> typesInFile = new java.util.ArrayList<Integer>();
    List<Integer> policiesInFile = new java.util.ArrayList<Integer>();
    List<Integer> topLevelStatements = new java.util.ArrayList<Integer>();
    List<Integer> topLevelMethods = new java.util.ArrayList<Integer>();
    
    while (!is(EOF)) {
        if (declarationParser.isPolicyDeclaration()) {
            int policy = declarationParser.parsePolicy();
            factory.getAST().unitAddPolicy(factory.getAST().programUnit(programId), policy);
            policiesInFile.add(policy);
        } 
        else if (isClassStart() || isClassStartWithoutModifier()) {
            // Save state before attempting to parse as type
            ParserState beforeType = getCurrentState();
            
            int type = declarationParser.parseType();
            if (type != cod.ast.FlatAST.NULL) {
                // Successfully parsed as a type
                factory.getAST().unitAddType(factory.getAST().programUnit(programId), type);
                typesInFile.add(type);
            } else {
                // Not a type - restore state and parse as method
                setState(beforeType);
                int method = declarationParser.parseMethod();
                topLevelMethods.add(method);
            }
        }
        else if (isMethodDeclarationStart()) {
            int method = declarationParser.parseMethod();
            topLevelMethods.add(method);
        }
        else {
            // Must be a statement at top level
            int stmt = statementParser.parseStmt();
            topLevelStatements.add(stmt);
        }
    }
    
    // Now validate the program structure and set final type
    validateProgramStructure(programId, topLevelStatements, topLevelMethods, typesInFile, policiesInFile);
    
    // Add top-level elements to the appropriate places based on program type
    if ("MODULE".equals(factory.getAST().programType(programId))) {
        // Modules already added types and policies during parsing
        // Top-level methods and statements are not allowed (would have thrown error)
    } else if ("METHOD_SCRIPT".equals(factory.getAST().programType(programId))) {
        // Method scripts have an implicit type that contains all methods
        int unitId = factory.getAST().programUnit(programId);
        int methodScriptTypeId = findOrCreateImplicitType(unitId, "__MethodScript__");
        for (int m : toIntArray(topLevelMethods)) {
            factory.getAST().typeAddMethod(methodScriptTypeId, m);
        }
    } else if ("SCRIPT".equals(factory.getAST().programType(programId))) {
        // Scripts have an implicit type that contains all statements
        int unitId2 = factory.getAST().programUnit(programId);
        int scriptTypeId = findOrCreateImplicitType(unitId2, "__Script__");
        for (int s : toIntArray(topLevelStatements)) {
            factory.getAST().typeAddStatement(scriptTypeId, s);
        }
    }
    
    // Validate module-specific rules if this is a module
    if ("MODULE".equals(factory.getAST().programType(programId))) {
        validateModule(programId, typesInFile, policiesInFile);
    }
    
    return programId;
}

    private int findOrCreateImplicitType(int unitId, String typeName) {
        int[] types = factory.getAST().unitTypes(unitId);
        if (types != null) {
            for (int tid : types) {
                if (typeName.equals(factory.getAST().typeName(tid))) {
                    return tid;
                }
            }
        }
        int implicitTypeId = factory.createType(typeName, SHARE, null, null);
        factory.getAST().unitAddType(unitId, implicitTypeId);
        return implicitTypeId;
    }

    private void validateProgramStructure(int programId, 
                                         List<Integer> topLevelStatements,
                                         List<Integer> topLevelMethods,
                                         List<Integer> typesInFile,
                                         List<Integer> policiesInFile) {
        
        String unitName = factory.getAST().unitName(factory.getAST().programUnit(programId));
        boolean hasUnit = unitName != null && !unitName.equals("default");
        boolean hasDirectCode = !topLevelStatements.isEmpty();
        boolean hasMethods = !topLevelMethods.isEmpty();
        boolean hasClasses = !typesInFile.isEmpty();
        
        if (hasUnit) {
            // MODULE validation
            factory.getAST().programSetType(programId, "MODULE");
            
            if (hasDirectCode) {
                throw error("Modules cannot have direct code outside classes.", now());
            }
            if (hasMethods) {
                throw error("Modules cannot have methods outside classes.", now());
            }
            
        } else if (hasDirectCode) {
            // SCRIPT validation
            factory.getAST().programSetType(programId, "SCRIPT");
            
            if (hasMethods) {
                throw error("Scripts cannot contain method declarations.", now());
            }
            if (hasClasses) {
                throw error("Scripts cannot contain class declarations.", now());
            }
            
        } else if (hasMethods) {
            // METHOD_SCRIPT validation
            factory.getAST().programSetType(programId, "METHOD_SCRIPT");
            
            if (hasClasses) {
                throw error("Method scripts cannot contain class declarations.", now());
            }
            
        } else if (hasClasses) {
            // Classes without unit declaration → implicit MODULE
            factory.getAST().programSetType(programId, "MODULE");

        } else {
            // Empty file or just imports - treat as script
            factory.getAST().programSetType(programId, "SCRIPT");
        }
    }

    private void validateModule(int programId, 
                               List<Integer> typesInFile,
                               List<Integer> policiesInFile) {
        int unitId = factory.getAST().programUnit(programId);
        String unitName = factory.getAST().unitName(unitId);
        // Validate unit name against file path
        if (interpreter != null) {
            String filePath = interpreter.getCurrentFilePath();
            if (filePath != null) {
                validateUnitAgainstFilePath(unitName, filePath);
            }
        }

        // Register broadcast if main class specified
        String mainClassName = factory.getAST().unitMainClass(unitId);
        if (!nil(mainClassName) && interpreter != null) {
            try {
                String packageName = extractPackageName(unitName);
                interpreter.getImportResolver().registerBroadcast(packageName, mainClassName);
            } catch (Exception e) { /* Ignore */ }
        }

        // Validate main class exists - TODO: FlatAST migration
        // Validate policy implementations - TODO: FlatAST migration
        // Validate class policies - TODO: FlatAST migration
        for (int typeId : typesInFile) {
            // declarationParser.validateAllPolicyMethods(typeId, programId);
            // declarationParser.validateClassViralPolicies(typeId, programId);
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

    private void validateMainClassExistsInFile(int unitId, List<Integer> typesInFile)  {
    // TODO: migrate to FlatAST API
    return;
  }

    private void validateImplementedPolicies(UnitNode unit, List<TypeNode> types, List<PolicyNode> policies) {
        Map<String, PolicyNode> policyMap = new HashMap<String, PolicyNode>();
        for (PolicyNode policy : policies) {
            policyMap.put(policy.name, policy);
        }
        
        for (TypeNode type : types) {
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

    private int parseUnit() {
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
                mainToken.type != ID || !mainToken.text.equals("main") || 
                colonToken.symbol != COLON) {
                setState(beforeMainCheck);
            } else {
                consume();
                expect(COLON);
                
                mainClassName = parseQualifiedName();
                
                expect(RPAREN);
            }
        }
        
        int unit = factory.createUnit(unitName, unitToken);
        factory.getAST().unitSetMainClass(unit, mainClassName);
        return unit;
    }

    private int parseUseNode() {
        Token useToken = now();
        expect(USE);
        expect(LBRACE);
        List<String> imports = new ArrayList<String>();
        if (!is(RBRACE)) {
            imports.add(parseQualifiedName());
            while (consume(COMMA)) {
                imports.add(parseQualifiedName());
            }
        }
        expect(RBRACE);
        return factory.createUseNode(imports, useToken);
    }

    private static int[] toIntArray(java.util.List<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
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

    public int parseSingleLine() {
        if (is(EOF)) {
            return cod.ast.FlatAST.NULL;
        }

        int stmt = statementParser.parseStmt();

        if (!is(EOF)) {
            Token current = now();
            throw error("Unexpected token after statement: " +
                getTypeName(current.type) + " ('" + current.text + "')", current);
        }
        return stmt;
    }

    /** Compatibility bridge: parse and return a ProgramNode for downstream callers. */
    public cod.ast.nodes.ProgramNode parseProgramNode() {
        int id = parseProgram();
        cod.ast.nodes.ProgramNode node = factory.toProgramNode(id);
        if (node == null) throw new RuntimeException("Internal error: parsed program has no legacy ProgramNode. FlatAST id=" + id);
        return node;
    }

    /** Compatibility: parse a single line and return as StmtNode for REPL. */
    public cod.ast.nodes.StmtNode parseSingleLineNode() {
        int id = parseSingleLine();
        if (id == cod.ast.FlatAST.NULL) return null;
        Object obj = factory.getAST().getLegacyNode(id);
        return (obj instanceof cod.ast.nodes.StmtNode) ? (cod.ast.nodes.StmtNode) obj : null;
    }
}
