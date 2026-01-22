package cod.parser;

import cod.ast.ASTFactory;
import cod.ast.nodes.*;
import cod.error.ParseError;
import cod.interpreter.Interpreter;
import cod.syntax.Symbol;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import cod.lexer.Token;
import static cod.lexer.TokenType.*;
import cod.parser.context.*;
import cod.parser.program.*;
import static cod.syntax.Keyword.*;
import static cod.syntax.Symbol.*;

public class MainParser extends BaseParser {

    private final ExpressionParser expressionParser;
    private final StatementParser statementParser;
    private final DeclarationParser declarationParser;
    private final Interpreter interpreter;

    public MainParser(List<Token> tokens) {
        this(tokens, null);
    }
    
    public MainParser(List<Token> tokens, Interpreter interpreter) {
        super(new ParserContext(new ParserState(tokens)));
        this.expressionParser = new ExpressionParser(ctx);
        this.statementParser = new StatementParser(ctx, expressionParser);
        this.declarationParser = new DeclarationParser(ctx, statementParser, 
            interpreter != null ? interpreter.getImportResolver() : null);
        this.interpreter = interpreter;
    }
    
    @Override
    protected BaseParser createIsolatedParser(ParserContext isolatedCtx) {
        return new MainParser(isolatedCtx.getState().getTokens(), interpreter);
    }
    
    public ProgramNode parseProgram() {
        ParserState startState = getCurrentState();
        
        ProgramType programType = detectProgramType();
        
        // Reset to beginning using ParserState
        setState(startState.withPosition(0));
        
        ProgramNode program = null;
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
                    getLine(), getColumn());
        }
        
        program.programType = programType;
        
        cod.semantic.ProgramValidator.validate(program, programType);
        
        return program;
    }

    private ProgramNode parseModuleProgram() {
        ProgramNode program = ASTFactory.createProgram();
        program.unit = parseUnit();
        
        // Validate unit against file path if interpreter has file path
        if (interpreter != null) {
            String filePath = interpreter.getCurrentFilePath();
            if (filePath != null) {
                validateUnitAgainstFilePath(program.unit.name, filePath);
            }
        }
        
        if (program.unit.mainClassName != null && interpreter != null) {
            try {
                String packageName = extractPackageName(program.unit.name);
                interpreter.getImportResolver().registerBroadcast(
                    packageName, program.unit.mainClassName
                );
            } catch (Exception e) {
                // Ignore if interpreter is not available
            }
        }

        while (is(USE)) {
            if (program.unit.imports == null) {
                program.unit.imports = parseUseNode();
            } else {
                UseNode additionalImports = parseUseNode();
                program.unit.imports.imports.addAll(additionalImports.imports);
            }
        }

        List<TypeNode> typesInFile = new ArrayList<TypeNode>();
        List<PolicyNode> policiesInFile = new ArrayList<PolicyNode>();
        
        while (!is(EOF)) {
            skipWhitespaceAndComments();
            
            // Check what type of declaration this is
            if (declarationParser.isPolicyDeclaration()) {
                // Parse policy declaration
                PolicyNode policy = declarationParser.parsePolicy();
                program.unit.policies.add(policy);
                policiesInFile.add(policy);
            } 
            else if (isClassStart() || isClassStartWithoutModifier()) {
                // Parse class declaration
                TypeNode type = declarationParser.parseType();
                program.unit.types.add(type);
                typesInFile.add(type);
                
                // Validate that the class implements all required policy methods
                validateClassImplementsPolicies(type, policiesInFile, program);
            } 
            else {
                Token current = currentToken();
                if (current != null) {
                    throw new ParseError(
                        "Modules can only contain class or policy declarations after imports. " +
                        "Found: " + current.text + " (" + getTypeName(current.type) + ")",
                        current);
                }
            }
        }
        
        validateMainClassExistsInFile(program.unit, typesInFile);
        
        // Validate that policies referenced in class 'with' clauses exist
        validateImplementedPolicies(program.unit, typesInFile, policiesInFile);
        
        // Validate viral policies for each type
        for (TypeNode type : typesInFile) {
            // Validate all policy methods in this type
            declarationParser.validateAllPolicyMethods(type, program);
            // Validate viral policies inheritance
            declarationParser.validateClassViralPolicies(type, program);
        }
        
        return program;
    }

private void validateUnitAgainstFilePath(String unitName, String filePath) {
    if (filePath == null || unitName == null) return;
    
    // === PHASE 0: INSTANT O(1) CHECKS ===
    
    // 1. Basic sanity checks
    if (unitName.isEmpty()) {
        throw new ParseError("Unit name cannot be empty", getLine(), getColumn());
    }
    
    if (!filePath.endsWith(".cod")) {
        throw new ParseError("File must have .cod extension", getLine(), getColumn());
    }
    
    // === PHASE 1: EXTRACT DIR NAME (optimized, no File I/O) ===
    String dirName = extractDirNameNoFileIO(filePath);
    if (dirName.isEmpty()) {
        // File in current directory - skip directory checks
        validateFileInCurrentDirectory(unitName, filePath);
        return;
    }
    
    // === PHASE 2: ULTRA-FAST CHECKS (your original logic) ===
    
    // Get first part of unit name (for dotted or single-word)
    String firstUnitPart;
    int dotIndex = unitName.indexOf('.');
    if (dotIndex != -1) {
        firstUnitPart = unitName.substring(0, dotIndex);
    } else {
        firstUnitPart = unitName;
    }
    
    // 1. First character check (your original logic)
    if (!firstUnitPart.isEmpty() && !dirName.isEmpty() && 
        firstUnitPart.charAt(0) != dirName.charAt(0)) {
        throw new ParseError(
            "Unit name '" + unitName + "' doesn't match directory '" + dirName + "'",
            getLine(), getColumn());
    }
    
    // 2. Length sanity (your original logic)
    int lengthDiff = Math.abs(firstUnitPart.length() - dirName.length());
    if (lengthDiff > 3) {
        throw new ParseError(
            "Unit name '" + unitName + "' doesn't match directory '" + dirName + "'",
            getLine(), getColumn());
    }
    
    // 3. Last character check (your original logic)
    if (!firstUnitPart.isEmpty() && !dirName.isEmpty() &&
        firstUnitPart.charAt(firstUnitPart.length() - 1) != dirName.charAt(dirName.length() - 1)) {
        throw new ParseError(
            "Unit name '" + unitName + "' doesn't match directory '" + dirName + "'",
            getLine(), getColumn());
    }
    
    // === PHASE 3: FULL VALIDATION (only if fast checks pass) ===
    
    if (quickPathMatchCheck(unitName, filePath)) {
        return;
    }
    
    String expectedUnit = calculateExpectedUnit(filePath);
    if (!unitName.equals(expectedUnit)) {
        throw new ParseError(
            "Unit name '" + unitName + "' doesn't match directory structure",
            getLine(), getColumn());
    }
}

private String extractDirNameNoFileIO(String filePath) {
    int len = filePath.length();
    
    // Skip .cod extension if present
    if (len > 4 && filePath.endsWith(".cod")) {
        len -= 4;
    }
    
    // Find last separator
    int lastSeparator = -1;
    for (int i = len - 1; i >= 0; i--) {
        char c = filePath.charAt(i);
        if (c == '/' || c == '\\') {
            lastSeparator = i;
            break;
        }
    }
    
    if (lastSeparator == -1) {
        return ""; // No directory
    }
    
    // Find previous separator
    int prevSeparator = -1;
    for (int i = lastSeparator - 1; i >= 0; i--) {
        char c = filePath.charAt(i);
        if (c == '/' || c == '\\') {
            prevSeparator = i;
            break;
        }
    }
    
    if (prevSeparator == -1) {
        // Directory is from start to last separator
        return filePath.substring(0, lastSeparator);
    }
    
    // Directory is between separators
    return filePath.substring(prevSeparator + 1, lastSeparator);
}

private void validateFileInCurrentDirectory(String unitName, String filePath) {
    // File is in current directory (no parent directory)
    // For test files, we can skip strict validation
    // Just do basic format check
    if (unitName.isEmpty()) {
        throw new ParseError("Unit name cannot be empty", getLine(), getColumn());
    }
    
    // Optional: Check if unit looks reasonable
    if (unitName.contains(" ")) {
        throw new ParseError("Unit name cannot contain spaces: '" + unitName + "'", 
            getLine(), getColumn());
    }
}

    private boolean quickPathMatchCheck(String unitName, String filePath) {
        // Convert unit to path format
        String unitAsPath = unitName.replace('.', '/');
        
        // Remove .cod extension
        String pathWithoutExt = filePath;
        if (filePath.endsWith(".cod")) {
            pathWithoutExt = filePath.substring(0, filePath.length() - 4);
        }
        
        // Normalize separators
        pathWithoutExt = pathWithoutExt.replace('\\', '/');
        
        // Check for direct match
        if (pathWithoutExt.endsWith("/" + unitAsPath) || 
            pathWithoutExt.equals(unitAsPath)) {
            return true;
        }
        
        // Check if file is in unit directory
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
        
        // Find src/main/ in the path
        String srcMain = "src/main/";
        int srcMainIndex = normalized.indexOf(srcMain);
        
        if (srcMainIndex == -1) {
            // File not under src/main/, return empty string (will fail validation)
            return "";
        }
        
        // Get path relative to src/main/
        String relative = normalized.substring(srcMainIndex + srcMain.length());
        
        // Remove .cod extension
        if (relative.endsWith(".cod")) {
            relative = relative.substring(0, relative.length() - 4);
        }
        
        // Split into components
        String[] parts = relative.split("/");
        List<String> unitParts = new ArrayList<String>();
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;
            
            // Check if this looks like a file name (PascalCase for classes/policies)
            boolean looksLikeFileName = part.length() > 0 && Character.isUpperCase(part.charAt(0));
            
            if (i == parts.length - 1 && looksLikeFileName) {
                // Last component and looks like a class name, skip it
                // This handles cases like Serializable.cod, Main.cod
                continue;
            }
            
            unitParts.add(part);
        }
        
        // Join with dots
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

    private void validateClassImplementsPolicies(TypeNode type, List<PolicyNode> policies, ProgramNode program) {
        if (type == null || type.implementedPolicies == null || type.implementedPolicies.isEmpty()) {
            return;
        }
        
        // Build map of available policies (local + imported)
        Map<String, PolicyNode> policyMap = new HashMap<String, PolicyNode>();
        
        // Add local policies
        for (PolicyNode policy : policies) {
            policyMap.put(policy.name, policy);
        }
        
        // Try to get imported policies via ImportResolver
        if (interpreter != null && interpreter.getImportResolver() != null) {
            for (String policyName : type.implementedPolicies) {
                if (!policyMap.containsKey(policyName)) {
                    try {
                        PolicyNode importedPolicy = interpreter.getImportResolver().findPolicy(policyName);
                        if (importedPolicy != null) {
                            policyMap.put(policyName, importedPolicy);
                        }
                    } catch (Exception e) {
                        // Policy not found in imports, will be caught below
                    }
                }
            }
        }
        
        for (String policyName : type.implementedPolicies) {
            PolicyNode policy = policyMap.get(policyName);
            
            if (policy == null) {
                // Policy not found - will be caught by validateImplementedPolicies
                continue;
            }
            
            // Get all methods required by this policy (including composed ones)
            List<PolicyMethodNode> requiredMethods = getAllPolicyMethods(policy, program);
            
            // Check each required method
            for (PolicyMethodNode requiredMethod : requiredMethods) {
                boolean implementsMethod = false;
                
                // Check if class implements this method with 'policy' keyword
                if (type.methods != null) {
                    for (MethodNode classMethod : type.methods) {
                        if (classMethod.methodName.equals(requiredMethod.methodName) && 
                            classMethod.isPolicyMethod) {
                            // Method exists and is marked as policy implementation
                            implementsMethod = true;
                            break;
                        }
                    }
                }
                
                if (!implementsMethod) {
                    // Get the token for this specific policy declaration
                    Token policyToken = null;
                    if (type.policyTokens != null) {
                        policyToken = type.policyTokens.get(policyName);
                    }
                    
                    if (policyToken != null) {
                        // Point to the exact location of the policy name in 'with Accept'
                        throw new ParseError(
                            "Class '" + type.name + "' claims to implement policy '" + 
                            policyName + "'\n" +
                            "Policy '" + policyName + "' requires method '" + requiredMethod.methodName + "'\n" +
                            "Add: policy " + requiredMethod.methodName + "(...) { ... } inside the class",
                            policyToken);
                    } else {
                        // Fallback: use current position
                        throw new ParseError(
                            "Class '" + type.name + "' claims to implement policy '" + 
                            policyName + "'\n" +
                            "Policy '" + policyName + "' requires method '" + requiredMethod.methodName + "'\n" +
                            "Add: policy " + requiredMethod.methodName + "(...) { ... } inside the class",
                            getLine(), getColumn());
                    }
                }
            }
        }
    }

    // Helper method to get all methods from a policy (including composed policies)
    private List<PolicyMethodNode> getAllPolicyMethods(PolicyNode policy, ProgramNode program) {
        List<PolicyMethodNode> allMethods = new ArrayList<PolicyMethodNode>();
        if (policy == null) {
            return allMethods;
        }
        
        Set<String> visited = new HashSet<String>();
        collectPolicyMethodsRecursive(policy, allMethods, visited, program);
        return allMethods;
    }

    // Recursive method to collect methods from composed policies
    private void collectPolicyMethodsRecursive(PolicyNode policy, List<PolicyMethodNode> allMethods, 
                                              Set<String> visited, ProgramNode program) {
        if (policy == null || visited.contains(policy.name)) {
            return;
        }
        
        visited.add(policy.name);
        
        // Collect from composed policies first
        if (policy.composedPolicies != null) {
            for (String composedName : policy.composedPolicies) {
                // Try to find the composed policy
                PolicyNode composedPolicy = null;
                
                // Check local policies first
                if (program.unit.policies != null) {
                    for (PolicyNode p : program.unit.policies) {
                        if (p.name.equals(composedName)) {
                            composedPolicy = p;
                            break;
                        }
                    }
                }
                
                // Check imported policies
                if (composedPolicy == null && interpreter != null && interpreter.getImportResolver() != null) {
                    try {
                        composedPolicy = interpreter.getImportResolver().findPolicy(composedName);
                    } catch (Exception e) {
                        // Policy not found
                    }
                }
                
                if (composedPolicy != null) {
                    collectPolicyMethodsRecursive(composedPolicy, allMethods, visited, program);
                }
            }
        }
        
        // Add local methods
        if (policy.methods != null) {
            allMethods.addAll(policy.methods);
        }
    }

    private void validateImplementedPolicies(UnitNode unit, List<TypeNode> types, List<PolicyNode> policies) {
        // Build map of available policies
        Map<String, PolicyNode> policyMap = new HashMap<String, PolicyNode>();
        for (PolicyNode policy : policies) {
            policyMap.put(policy.name, policy);
        }
        
        // Check each class's implemented policies
        for (TypeNode type : types) {
            for (String policyName : type.implementedPolicies) {
                if (!policyMap.containsKey(policyName)) {
                    throw new ParseError(
                        "Class '" + type.name + "' implements undefined policy '" + policyName + "'\n" +
                        "Available policies: " + policyMap.keySet(),
                        getLine(), getColumn());
                }
            }
        }
    }

    private UnitNode parseUnit() {
        Token unitToken = currentToken();
        expect(UNIT);
        String unitName = parseQualifiedName();
        
        String mainClassName = null;
        if (is(LPAREN)) {
            ParserState beforeMainCheck = getCurrentState();
            
            expect(LPAREN);
            
            Token mainToken = currentToken();
            Token colonToken = lookahead(1);
            
            if (mainToken == null || colonToken == null || 
                mainToken.type != ID || !mainToken.text.equals("main") || 
                colonToken.symbol != COLON) {
                // Not a main declaration, roll back
                setState(beforeMainCheck);
            } else {
                consume(); // "main"
                expect(COLON);
                
                mainClassName = parseQualifiedName();
                
                expect(RPAREN);
            }
        }
        
        UnitNode unit = ASTFactory.createUnit(unitName, unitToken);
        unit.mainClassName = mainClassName;
        
        if (is(USE)) {
            unit.imports = parseUseNode();
        }
        return unit;
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

    private void validateMainClassExistsInFile(UnitNode unit, List<TypeNode> typesInFile) {
        if (unit.mainClassName == null || unit.mainClassName.isEmpty()) {
            return;
        }
        
        boolean classFound = false;
        for (TypeNode type : typesInFile) {
            if (unit.mainClassName.equals(type.name)) {
                classFound = true;
                
                boolean hasMainMethod = false;
                for (MethodNode method : type.methods) {
                    if (method.methodName.equals("main")) {
                        hasMainMethod = true;
                        break;
                    }
                }
                
                if (!hasMainMethod) {
                    throw new ParseError(
                        "[MODULE] Broadcasted class '" + unit.mainClassName + 
                        "' must have a main() method",
                        getLine(), getColumn());
                }
                break;
            }
        }
        
        if (!classFound) {
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
                getLine(), getColumn());
        }
    }

    private UseNode parseUseNode() {
        Token useToken = currentToken();
        expect(USE);
        expect(LBRACE);
        List<String> imports = new ArrayList<String>();
        if (!is(RBRACE)) {
            imports.add(parseQualifiedName());
            while (tryConsume(COMMA)) {
                imports.add(parseQualifiedName());
            }
        }
        expect(RBRACE);
        UseNode useNode = ASTFactory.createUseNode(imports, useToken);
        return useNode;
    }
    
    private ProgramType detectProgramType() {
        ProgramTypeScanner scanner = new ProgramTypeScanner(getCurrentState().getTokens());
        return scanner.scan();
    }

    private ProgramNode parseScriptProgram() {
        ProgramNode program = ASTFactory.createProgram();
        program.unit = ASTFactory.createUnit("default", null);

        while (is(USE)) {
            if (program.unit.imports == null) {
                program.unit.imports = parseUseNode();
            } else {
                UseNode additionalImports = parseUseNode();
                program.unit.imports.imports.addAll(additionalImports.imports);
            }
        }

        TypeNode scriptType = ASTFactory.createType("__Script__", SHARE, null, null);

        while (!is(EOF)) {
            if (isVisibilityModifier() || isClassStartWithoutModifier() || isMethodDeclarationStart()) {
                throw new ParseError("Scripts cannot contain method or class declarations", getLine(), getColumn());
            }
            scriptType.statements.add(statementParser.parseStatement());
        }
        
        program.unit.types.add(scriptType);
        return program;
    }
    
    private ProgramNode parseMethodScriptProgram() {
        ProgramNode program = ASTFactory.createProgram();
        program.unit = ASTFactory.createUnit("default", null);

        while (is(USE)) {
            if (program.unit.imports == null) {
                program.unit.imports = parseUseNode();
            } else {
                UseNode additionalImports = parseUseNode();
                program.unit.imports.imports.addAll(additionalImports.imports);
            }
        }

        TypeNode methodScriptType = ASTFactory.createType("__MethodScript__", SHARE, null, null);

        while (!is(EOF)) {
            if (!isMethodDeclarationStart()) {
                if (isVisibilityModifier() || isClassStartWithoutModifier()) {
                    throw new ParseError("Method scripts cannot contain class declarations", getLine(), getColumn());
                }
                throw new ParseError("Method scripts can only contain method declarations", getLine(), getColumn());
            }
            methodScriptType.methods.add(declarationParser.parseMethod());
        }
        
        program.unit.types.add(methodScriptType);
        return program;
    }

    private boolean isMethodDeclarationStart() {
        ParserState savedState = getCurrentState();
        skipWhitespaceAndComments();
        
        try {
            Token first = currentToken();
            if (first == null) return false;
            
            if (is(first, KEYWORD)) {
                if (is(first, LOCAL, SHARE)) {
                    Token second = lookahead(1);
                    if (second != null && is(second, ID)) {
                        Token third = lookahead(2);
                        if (third != null && is(third, LPAREN)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } finally {
            setState(savedState);
        }
    }

    // === STATE-BASED SKIP METHODS ===
    
    private void skipMethodDeclaration() {
        boolean isBuiltin = false;
        if (is(KEYWORD) && is(BUILTIN)) {
            isBuiltin = true;
            consume();
        }
        
        if (isVisibilityModifier()) {
            consume();
        }
        
        if (is(ID)) consume();
        
        if (is(LPAREN)) {
            expect(LPAREN);
            skipUntil(RPAREN);
        }
        
        if (is(DOUBLE_COLON)) {
            expect(DOUBLE_COLON);
            skipSlotContract();
        }
        
        if (isBuiltin) {
            return;
        }
        
        if (is(TILDE_ARROW)) {
            expect(TILDE_ARROW);
            
            skipSlotAssignment();
            
            while (tryConsume(COMMA)) {
                skipSlotAssignment();
            }
            
        } else if (is(LBRACE)) {
            expect(LBRACE);
            skipUntil(RBRACE);
        }
    }

    private void skipSlotAssignment() {
        if (is(ID)) {
            Token next = lookahead(1);
            if (next != null && is(next, COLON)) {
                consume();
                consume();
            }
        }
        skipExpression();
    }
    
    private void skipSlotContract() {
        do {
            if (is(ID) && isSymbolAt(1, COLON)) {
                consume();
                expect(COLON);
            }
            skipTypeReference();
        } while (tryConsume(COMMA));
    }
    
    private void skipStatement() {
        Token current = currentToken();
        
        if (is(current, KEYWORD)) {
            if (is(current, IF)) {
                skipIfStatement();
            } else if (is(current, FOR)) {
                skipForStatement();
            } else if (is(current, EXIT)) {
                consume();
            } else if (is(current, SHARE, LOCAL)) {
                // Check if this is a method declaration using state
                ParserState testState = getCurrentState().skipWhitespaceAndComments();
                testState = testState.advance().skipWhitespaceAndComments(); // Skip modifier
                
                if (testState.currentToken() != null && is(testState.currentToken(), ID)) {
                    testState = testState.advance().skipWhitespaceAndComments();
                    if (testState.currentToken() != null && is(testState.currentToken(), LPAREN)) {
                        // It's a method declaration
                        skipMethodDeclaration();
                    } else {
                        consume();
                    }
                } else {
                    consume();
                }
            } else {
                consume();
            }
        } else if (is(current, ID)) {
            skipUntilStatementEnd();
        } else {
            consume();
        }
    }

    private void skipForStatement() {
        expect(FOR);
        
        if (is(ID)) consume();
        
        if (is(BY)) {
            expect(BY);
            skipExpression();
        }
        
        if (is(IN)) {
            expect(IN);
        }
        
        skipExpression();
        
        if (is(TO)) {
            expect(TO);
        } else {
            return;
        }
        
        skipExpression();
        
        if (is(LBRACE)) {
            expect(LBRACE);
            skipUntil(RBRACE);
        } else {
            skipStatement();
        }
    }
    
    private void skipIfStatement() {
        expect(IF);
        skipExpression();
        
        if (is(LBRACE)) {
            expect(LBRACE);
            skipUntil(RBRACE);
        } else {
            skipStatement();
        }
        
        while (is(ELIF) || is(ELSE)) {
            consume();
            if (is(IF)) {
                expect(IF);
                skipExpression();
            }
            
            if (is(LBRACE)) {
                expect(LBRACE);
                skipUntil(RBRACE);
            } else {
                skipStatement();
            }
        }
    }
    
    private void skipExpression() {
        int braceDepth = 0;
        int parenDepth = 0;
        int bracketDepth = 0;

        while (!is(EOF)) {
            Token t = currentToken();
            
            if (is(t, LBRACE)) braceDepth++;
            else if (is(t, RBRACE)) braceDepth--;
            else if (is(t, LPAREN)) parenDepth++;
            else if (is(t, RPAREN)) parenDepth--;
            else if (is(t, LBRACKET)) bracketDepth++;
            else if (is(t,  RBRACKET)) bracketDepth--;

            if (braceDepth < 0 || parenDepth < 0 || bracketDepth < 0) {
                return;
            }

            if (braceDepth == 0 && parenDepth == 0 && bracketDepth == 0) {
                if (is(t, COMMA)) return;
                
                if (is(t, KEYWORD)) {
                    if (is(t, IF, FOR, EXIT, ELSE, ELIF, SHARE, LOCAL, UNIT)) return;
                }
            }

            consume();
        }
    }
    
    private void skipUntil(Symbol symbol) {
        while (!is(EOF) && !is(symbol)) {
            Token t = currentToken();
            if (is(t, LBRACE, LPAREN, LBRACKET)) {
                consume();
                skipUntilMatching(t.symbol);
            } else {
                consume();
            }
        }
        if (is(symbol)) consume();
    }
    
    private void skipUntilMatching(Symbol opening) {
        Symbol closing;
        if (is(opening, LBRACE)) closing = RBRACE;
        else if (is(opening, LPAREN)) closing = RPAREN;
        else if (is(opening, LBRACKET)) closing = RBRACKET;
        else return;
        
        skipUntil(closing);
    }
    
    private void skipUntilStatementEnd() {
        while (!is(EOF)) {
            Token t = currentToken();
            
            if (is(t, RBRACE) ||
                (is(t, KEYWORD) && 
                 is(t, ELSE, ELIF, IF, FOR, EXIT))) {
                break;
            }
            
            if (is(t, LBRACE, LPAREN, LBRACKET)) {
                consume();
                skipUntilMatching(t.symbol);
            } else {
                consume();
            }
        }
    }
    
    private void skipTypeReference() {
        if (is(LBRACKET)) {
            expect(LBRACKET);
            if (!is(RBRACKET)) {
                skipTypeReference();
            }
            expect(RBRACKET);
        } else if (is(LPAREN)) {
            expect(LPAREN);
            skipTypeReference();
            while (tryConsume(COMMA)) {
                skipTypeReference();
            }
            expect(RPAREN);
        } else if (isTypeStart(currentToken())) {
            consume();
        }
        
        // Skip union
        while (tryConsume(PIPE)) {
            skipTypeReference();
        }
    }

    public StmtNode parseSingleLine() {
        if (is(EOF)) {
            return null;
        }

        StmtNode stmt = statementParser.parseStatement();

        if (!is(EOF)) {
            Token current = currentToken();
            throw new ParseError("Unexpected token after statement: " +
                getTypeName(current.type) + " ('" + current.text + "')", current);
        }
        return stmt;
    }
}