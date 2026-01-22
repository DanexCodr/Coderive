package cod.parser;

import cod.ast.ASTFactory;
import cod.ast.nodes.*;
import cod.error.ParseError;
import cod.parser.context.*;
import cod.semantic.ImportResolver;
import cod.semantic.NamingValidator;
import cod.syntax.Keyword;
import java.math.BigDecimal;
import java.util.*;

import cod.lexer.Token;
import static cod.lexer.TokenType.*;
import static cod.syntax.Keyword.*;
import static cod.syntax.Symbol.*;

public class DeclarationParser extends BaseParser {

  private final StatementParser statementParser;
  private final ImportResolver importResolver;

  private TypeNode currentParsingClass = null;
  private Map<String, PolicyNode> availablePolicies = new HashMap<String, PolicyNode>();

  public DeclarationParser(
      ParserContext ctx, StatementParser statementParser, ImportResolver importResolver) {
    super(ctx);
    this.statementParser = statementParser;
    this.importResolver = importResolver;
  }

  @Override
  protected BaseParser createIsolatedParser(ParserContext isolatedCtx) {
    return new DeclarationParser(isolatedCtx, this.statementParser, this.importResolver);
  }

  // Helper methods for tracking current class
  private void setCurrentParsingClass(TypeNode type) {
    currentParsingClass = type;
  }

  private TypeNode getCurrentParsingClass() {
    return currentParsingClass;
  }

  // Enhanced findPolicy method that checks both local and imported policies
  private PolicyNode findPolicy(String policyName) {
    // 1. Check locally defined policies (in current file)
    if (availablePolicies.containsKey(policyName)) {
      return availablePolicies.get(policyName);
    }

    // 2. Check imported policies via ImportResolver
    if (importResolver != null) {
      try {
        return importResolver.findPolicy(policyName);
      } catch (Exception e) {
        // Policy not found in imports, continue to check other sources
      }
    }

    // 3. Check if it's a fully qualified name with unit prefix
    if (policyName.contains(".")) {
      // Try to find it as a type (might be a class, not a policy)
      // For now, return null and let caller handle the error
      return null;
    }

    return null;
  }

  // Find a class by name (for checking parent classes)
  private TypeNode findClassByName(String className, ProgramNode currentProgram) {
    // Check current program first
    if (currentProgram != null
        && currentProgram.unit != null
        && currentProgram.unit.types != null) {
      for (TypeNode type : currentProgram.unit.types) {
        if (type.name.equals(className)) {
          return type;
        }
      }
    }

    // Check imported types via ImportResolver
    if (importResolver != null) {
      return importResolver.findType(className);
    }

    return null;
  }

  // Get all methods from a policy including composed policies
  private List<PolicyMethodNode> getAllPolicyMethods(PolicyNode policy) {
    if (policy == null) {
      return new ArrayList<PolicyMethodNode>();
    }

    List<PolicyMethodNode> allMethods = new ArrayList<PolicyMethodNode>();
    Set<String> visitedPolicies = new HashSet<String>();

    // Recursively collect methods via composition only
    collectPolicyMethodsViaComposition(policy, allMethods, visitedPolicies);

    return allMethods;
  }

  // Recursive method to collect all methods from policy composition
  private void collectPolicyMethodsViaComposition(
      PolicyNode policy, List<PolicyMethodNode> allMethods, Set<String> visited) {
    if (policy == null || visited.contains(policy.name)) {
      return;
    }

    visited.add(policy.name);

    // Collect from composed policies (composition only, NO inheritance)
    if (policy.composedPolicies != null) {
      for (String composedName : policy.composedPolicies) {
        PolicyNode composed = findPolicy(composedName);
        if (composed != null) {
          collectPolicyMethodsViaComposition(composed, allMethods, visited);
        }
      }
    }

    // Add local methods
    if (policy.methods != null) {
      allMethods.addAll(policy.methods);
    }
  }

  // Get ALL policies that affect this class (including from ancestors - VIRAL)
  private List<String> getAllAffectingPolicies(TypeNode currentClass, ProgramNode currentProgram) {
    List<String> allPolicies = new ArrayList<String>();
    if (currentClass == null) {
      return allPolicies;
    }

    Set<String> visitedClasses = new HashSet<String>();
    collectAffectingPoliciesRecursive(currentClass, allPolicies, visitedClasses, currentProgram);

    return allPolicies;
  }

  // Recursively collect policies from inheritance chain (VIRAL)
  private void collectAffectingPoliciesRecursive(
      TypeNode type, List<String> allPolicies, Set<String> visited, ProgramNode currentProgram) {
    if (type == null || visited.contains(type.name)) {
      return;
    }

    visited.add(type.name);

    // First check parent (so parent policies come first in the list)
    if (type.extendName != null) {
      TypeNode parent = findClassByName(type.extendName, currentProgram);
      if (parent != null) {
        collectAffectingPoliciesRecursive(parent, allPolicies, visited, currentProgram);
      }
    }

    // Add this class's own policies
    if (type.implementedPolicies != null) {
      for (String policyName : type.implementedPolicies) {
        if (!allPolicies.contains(policyName)) {
          allPolicies.add(policyName);
        }
      }
    }
  }

  // Get ONLY ancestor policies (VIRAL - policies from parents/grandparents)
  private List<String> getAncestorPolicies(TypeNode currentClass, ProgramNode currentProgram) {
    List<String> ancestorPolicies = new ArrayList<String>();
    if (currentClass == null || currentClass.extendName == null) {
      return ancestorPolicies;
    }

    // Get all affecting policies
    List<String> allAffecting = getAllAffectingPolicies(currentClass, currentProgram);

    // Remove this class's own policies
    if (currentClass.implementedPolicies != null) {
      for (String ownPolicy : currentClass.implementedPolicies) {
        allAffecting.remove(ownPolicy);
      }
    }

    return allAffecting;
  }

  // Get detailed information about which ancestor and policy requires a method
  private String getRequiringAncestorWithPolicy(String methodName, TypeNode currentClass, 
                                               ProgramNode currentProgram) {
    if (currentClass == null || currentClass.extendName == null) {
      return null;
    }

    TypeNode current = findClassByName(currentClass.extendName, currentProgram);
    while (current != null) {
      if (current.implementedPolicies != null) {
        for (String policyName : current.implementedPolicies) {
          PolicyNode policy = findPolicy(policyName);
          if (policy != null) {
            List<PolicyMethodNode> allRequiredMethods = getAllPolicyMethods(policy);
            for (PolicyMethodNode policyMethod : allRequiredMethods) {
              if (policyMethod.methodName.equals(methodName)) {
                return current.name + " (implements policy '" + policyName + "')";
              }
            }
          }
        }
      }

      if (current.extendName != null) {
        current = findClassByName(current.extendName, currentProgram);
      } else {
        current = null;
      }
    }

    return null;
  }

  private boolean isMethodRequiredFromAncestorPolicy(
      String methodName, TypeNode currentClass, ProgramNode currentProgram) {
    List<String> ancestorPolicies = getAncestorPolicies(currentClass, currentProgram);

    // Check each ancestor policy
    for (String policyName : ancestorPolicies) {
      PolicyNode policy = findPolicy(policyName);
      if (policy != null) {
        List<PolicyMethodNode> allRequiredMethods = getAllPolicyMethods(policy);
        for (PolicyMethodNode policyMethod : allRequiredMethods) {
          if (policyMethod.methodName.equals(methodName)) {
            return true;
          }
        }
      }
    }

    return false;
  }

  // Get all method names required by ancestor policies (VIRAL)
  private Set<String> getAllMethodsRequiredByAncestors(
      TypeNode currentClass, ProgramNode currentProgram) {
    Set<String> requiredMethods = new HashSet<String>();
    List<String> ancestorPolicies = getAncestorPolicies(currentClass, currentProgram);

    for (String policyName : ancestorPolicies) {
      PolicyNode policy = findPolicy(policyName);
      if (policy != null) {
        List<PolicyMethodNode> allRequiredMethods = getAllPolicyMethods(policy);
        for (PolicyMethodNode method : allRequiredMethods) {
          requiredMethods.add(method.methodName);
        }
      }
    }

    return requiredMethods;
  }

  // Validate policy composition to prevent cycles
  private void validatePolicyComposition(PolicyNode policy) {
    if (policy == null || policy.composedPolicies == null || policy.composedPolicies.isEmpty()) {
      return;
    }

    Set<String> visited = new HashSet<String>();
    visited.add(policy.name);

    for (String composedName : policy.composedPolicies) {
      checkForCompositionCycle(policy.name, composedName, visited);
    }
  }

  // Check for composition cycles
  private void checkForCompositionCycle(
      String currentPolicy, String composedName, Set<String> visited) {
    if (visited.contains(composedName)) {
      throw new ParseError(
          "Circular composition detected in policies: "
              + String.join(" -> ", visited)
              + " -> "
              + composedName,
          getLine(),
          getColumn());
    }

    visited.add(composedName);

    // Check composed policy's dependencies
    PolicyNode composed = findPolicy(composedName);
    if (composed != null && composed.composedPolicies != null) {
      for (String nestedComposed : composed.composedPolicies) {
        checkForCompositionCycle(currentPolicy, nestedComposed, visited);
      }
    }

    visited.remove(composedName);
  }

  // Check type compatibility for policy method validation
  private boolean areTypesCompatible(String implType, String policyType) {
    if (implType == null || policyType == null) {
      return false;
    }

    // If types are exactly equal
    if (implType.equals(policyType)) {
      return true;
    }

    // Handle union types (e.g., "Int|none" is compatible with "Int")
    if (policyType.contains("|")) {
      String[] policyUnion = policyType.split("\\|");
      for (String unionMember : policyUnion) {
        if (unionMember.trim().equals(implType)) {
          return true;
        }
      }
    }

    // Handle array wildcard (e.g., "[Int]" is compatible with "[]")
    if (policyType.equals("[]") && implType.startsWith("[") && implType.endsWith("]")) {
      return true; // Policy accepts any array
    }

    // Handle generic type compatibility (basic)
    if (policyType.equals("type") && implType != null) {
      return true; // 'type' accepts any type
    }

    return false;
  }

  // UPDATED: Enhanced policy method validation with VIRAL checking
  private void validatePolicyMethod(MethodNode method, ProgramNode currentProgram) {
    TypeNode currentClass = getCurrentParsingClass();

    if (currentClass == null) {
      throw new ParseError(
          "Policy method '" + method.methodName + "' can only be declared in a class",
          getLine(),
          getColumn());
    }

    // Check 1: Is this method required by this class's own policies?
    boolean isRequiredByOwnPolicy = false;
    String requiringPolicy = null;
    PolicyMethodNode requiredSignature = null;

    if (currentClass.implementedPolicies != null) {
      for (String policyName : currentClass.implementedPolicies) {
        PolicyNode policy = findPolicy(policyName);
        if (policy != null) {
          List<PolicyMethodNode> allRequiredMethods = getAllPolicyMethods(policy);
          for (PolicyMethodNode policyMethod : allRequiredMethods) {
            if (policyMethod.methodName.equals(method.methodName)) {
              isRequiredByOwnPolicy = true;
              requiringPolicy = policyName;
              requiredSignature = policyMethod;
              break;
            }
          }
          if (isRequiredByOwnPolicy) break;
        }
      }
    }

    // Check 2: Is this method required by ancestor's policies (VIRAL inheritance)?
    boolean isRequiredByAncestor = false;
    String requiringAncestor = null;

    if (!isRequiredByOwnPolicy) {
      isRequiredByAncestor =
          isMethodRequiredFromAncestorPolicy(method.methodName, currentClass, currentProgram);
      if (isRequiredByAncestor) {
        requiringAncestor = getRequiringAncestorWithPolicy(method.methodName, currentClass, currentProgram);

        // Find the policy signature from the ancestor
        if (requiringAncestor != null) {
          // Extract class name from the detailed string
          String ancestorName = requiringAncestor.split(" ")[0]; // Get "Node" from "Node (implements policy 'Accept')"
          TypeNode ancestor = findClassByName(ancestorName, currentProgram);
          if (ancestor != null && ancestor.implementedPolicies != null) {
            for (String policyName : ancestor.implementedPolicies) {
              PolicyNode policy = findPolicy(policyName);
              if (policy != null) {
                List<PolicyMethodNode> allRequiredMethods = getAllPolicyMethods(policy);
                for (PolicyMethodNode policyMethod : allRequiredMethods) {
                  if (policyMethod.methodName.equals(method.methodName)) {
                    requiredSignature = policyMethod;
                    requiringPolicy = policyName;
                    break;
                  }
                }
              }
              if (requiredSignature != null) break;
            }
          }
        }
      }
    }

    // If neither own policy nor ancestor policy requires it, error
    if (!isRequiredByOwnPolicy && !isRequiredByAncestor) {
      throw new ParseError(
          "Method '"
              + method.methodName
              + "' is not required by any implemented policy or ancestor's policies. "
              + "Remove 'policy' keyword or add to a policy.",
          getLine(),
          getColumn());
    }

    // Now validate the signature against what's required
    if (requiredSignature != null) {
      // Validate parameter count
      if (method.parameters.size() != requiredSignature.parameters.size()) {
        String errorSource =
            isRequiredByAncestor
                ? "ancestor '" + requiringAncestor + "'"
                : "policy '" + requiringPolicy + "'";

        throw new ParseError(
            "Policy method '"
                + method.methodName
                + "' signature mismatch for "
                + errorSource
                + ": expected "
                + requiredSignature.parameters.size()
                + " parameters, got "
                + method.parameters.size(),
            getLine(),
            getColumn());
      }

      // Validate parameter types
      for (int i = 0; i < method.parameters.size(); i++) {
        ParamNode implParam = method.parameters.get(i);
        ParamNode policyParam = requiredSignature.parameters.get(i);

        if (!areTypesCompatible(implParam.type, policyParam.type)) {
          String errorSource =
              isRequiredByAncestor
                  ? "ancestor '" + requiringAncestor + "'"
                  : "policy '" + requiringPolicy + "'";

          throw new ParseError(
              "Policy method '"
                  + method.methodName
                  + "' parameter type mismatch for parameter "
                  + (i + 1)
                  + " '"
                  + policyParam.name
                  + "' in "
                  + errorSource
                  + ": expected "
                  + policyParam.type
                  + ", got "
                  + implParam.type,
              getLine(),
              getColumn());
        }
      }

      // Validate return slot count if policy declares them
      if (requiredSignature.returnSlots != null && !requiredSignature.returnSlots.isEmpty()) {
        if (method.returnSlots == null || method.returnSlots.isEmpty()) {
          // Implementation can omit return slots? Your design decision
          // For now, allow it but warn
        } else if (method.returnSlots.size() != requiredSignature.returnSlots.size()) {
          String errorSource =
              isRequiredByAncestor
                  ? "ancestor '" + requiringAncestor + "'"
                  : "policy '" + requiringPolicy + "'";

          throw new ParseError(
              "Policy method '"
                  + method.methodName
                  + "' return slot mismatch for "
                  + errorSource
                  + ": expected "
                  + requiredSignature.returnSlots.size()
                  + " slots, got "
                  + method.returnSlots.size(),
              getLine(),
              getColumn());
        } else {
          // Validate return slot types
          for (int i = 0; i < method.returnSlots.size(); i++) {
            SlotNode implSlot = method.returnSlots.get(i);
            SlotNode policySlot = requiredSignature.returnSlots.get(i);

            if (!areTypesCompatible(implSlot.type, policySlot.type)) {
              String errorSource =
                  isRequiredByAncestor
                      ? "ancestor '" + requiringAncestor + "'"
                      : "policy '" + requiringPolicy + "'";

              throw new ParseError(
                  "Policy method '"
                      + method.methodName
                      + "' return slot type mismatch for slot "
                      + (i + 1)
                      + " in "
                      + errorSource
                      + ": expected "
                      + policySlot.type
                      + ", got "
                      + implSlot.type,
                  getLine(),
                  getColumn());
            }
          }
        }
      }
    }

    // If this is required by ancestor, give a specific message
    if (isRequiredByAncestor && requiringAncestor != null) {
      // You could add a debug message or warning here
      // e.g., "Note: Implementing policy method required by ancestor '" + requiringAncestor + "'"
    }
  }

  // UPDATED: Validate that a class implements all required methods from ancestor policies
  public void validateClassViralPolicies(TypeNode type, ProgramNode currentProgram) {
    if (type == null || type.extendName == null) {
      return; // No parent, no viral policies
    }

    // Get all methods required by ancestor policies
    Set<String> requiredMethods = getAllMethodsRequiredByAncestors(type, currentProgram);

    // Check if this class implements all required methods
    for (String methodName : requiredMethods) {
      boolean implementsMethod = false;

      // Check in class methods
      if (type.methods != null) {
        for (MethodNode method : type.methods) {
          if (method.methodName.equals(methodName) && method.isPolicyMethod) {
            implementsMethod = true;
            break;
          }
        }
      }

      if (!implementsMethod) {
        // Get detailed information about which ancestor requires this
        String requiringAncestor = getRequiringAncestorWithPolicy(methodName, type, currentProgram);
        
        // Use the parent class name token if available
        int line = getLine();
        int column = getColumn();
        
        if (type.parentToken != null) {
          // Point to the parent class name in 'ChildNode is Node'
          line = type.parentToken.line;
          column = type.parentToken.column;
        } else if (type.extendToken != null) {
          // Fallback to 'is' keyword
          line = type.extendToken.line;
          column = type.extendToken.column;
        }
        
        if (requiringAncestor != null) {
          throw new ParseError(
              "Class '"
                  + type.name
                  + "' inherits from '" + type.extendName + "'\n" +
              "The ancestor " + requiringAncestor + " requires policy method '" + methodName + "'\n" +
              "Add: policy " + methodName + "(...) { ... } inside the class",
              line, column
          );
        } else {
          throw new ParseError(
              "Class '"
                  + type.name
                  + "' inherits from '" + type.extendName + "' which requires policy method '"
                  + methodName
                  + "'\n"
                  + "Add: policy " + methodName + "(...) { ... } inside the class",
              line, column
          );
        }
      }
    }
  }

  public void validateAllPolicyMethods(TypeNode type, ProgramNode currentProgram) {
    if (type == null || type.methods == null) {
      return;
    }

    // Save current parsing class
    TypeNode savedClass = getCurrentParsingClass();
    setCurrentParsingClass(type);

    try {
      for (MethodNode method : type.methods) {
        if (method.isPolicyMethod) {
          validatePolicyMethod(method, currentProgram);
        }
      }
    } finally {
      // Restore current parsing class
      setCurrentParsingClass(savedClass);
    }
  }

  private boolean lookaheadOffset(int offset) {
    ParserState currentState = getCurrentState();
    Token token = currentState.peek(offset);
    return token != null && is(token, WS, LINE_COMMENT, BLOCK_COMMENT);
  }

  public boolean isConstructorDeclaration() {
    return lookahead(
        new ParserAction<Boolean>() {
          @Override
          public Boolean parse() throws ParseError {
            int offset = 0;

            while (lookaheadOffset(offset)) offset++;

            Token first = lookahead(offset);
            if (first == null || !isVisibilityModifier(first)) return false;
            offset++;

            while (lookaheadOffset(offset)) offset++;

            Token nameToken = lookahead(offset);
            if (nameToken == null) return false;

            boolean thisAsKeywordOrIDName =
                (is(nameToken, KEYWORD) && is(nameToken, THIS))
                    || (is(nameToken, ID) && is(nameToken, THIS));

            if (!thisAsKeywordOrIDName) return false;

            offset++;
            while (lookaheadOffset(offset)) offset++;

            Token parenToken = lookahead(offset);
            return parenToken != null && is(parenToken, LPAREN);
          }
        });
  }

  public ConstructorNode parseConstructor() {
    Token startToken = currentToken();
    Token thisToken = null;

    if (isVisibilityModifier()) {
      consume();
    }

    thisToken = currentToken();
    Token current = consume();
    if (!is(current, THIS)) {
      throw new ParseError(
          "Constructor must be named 'this', found: " + current.text, startToken);
    }

    ConstructorNode constructor = ASTFactory.createConstructor(null, null, thisToken);

    expect(LPAREN);
    if (!is(RPAREN)) {
      constructor.parameters.add(parseParameter());
      while (tryConsume(COMMA)) {
        constructor.parameters.add(parseParameter());
      }
    }
    expect(RPAREN);

    if (isSlotDeclaration()) {
      throw new ParseError(
          "Constructors cannot have return slots contracts (:: syntax). "
              + "Remove '::' and return type declarations from constructor.",
          getLine(),
          getColumn());
    }

    if (is(TILDE_ARROW)) {
      throw new ParseError(
          "Constructors cannot use inline return (~>) syntax. "
              + "Use a block body: this(...) { ... }",
          getLine(),
          getColumn());
    }

    boolean hasSuperCall = false;
    if (looksLikeSuperConstructorCall()) {
      hasSuperCall = true;
      constructor.body.add(parseSuperConstructorCall());
    }

    if (is(LBRACE)) {
      expect(LBRACE);
      while (!is(RBRACE)) {
        constructor.body.add(statementParser.parseStatement());
      }
      expect(RBRACE);
    } else if (!hasSuperCall) {
      throw new ParseError(
          "Constructor must have a body: this(...) { ... } or this(...) super(...) { ... }",
          getLine(),
          getColumn());
    }

    return constructor;
  }

  private boolean looksLikeSuperConstructorCall() {
    return attempt(
        new ParserAction<Boolean>() {
          @Override
          public Boolean parse() throws ParseError {
            skipWhitespaceAndComments();

            if (!is(SUPER)) return false;
            expect(SUPER);

            skipWhitespaceAndComments();

            return is(LPAREN);
          }
        });
  }

  private MethodCallNode parseSuperConstructorCall() {
    Token superToken = currentToken();
    expect(SUPER);

    String zuper = SUPER.toString();
    MethodCallNode superCall = ASTFactory.createMethodCall(zuper, zuper, superToken);
    superCall.isSuperCall = true;
    expect(LPAREN);

    if (!is(RPAREN)) {
      if (isNamedArgument()) {
        parseNamedArgumentList(superCall.arguments, superCall.argNames);
      } else {
        superCall.arguments.add(statementParser.expressionParser.parseExpression());
        superCall.argNames.add(null);
        while (tryConsume(COMMA)) {
          superCall.arguments.add(statementParser.expressionParser.parseExpression());
          superCall.argNames.add(null);
        }
      }
    }

    expect(RPAREN);

    return superCall;
  }

  private boolean isNamedArgument() {
    return attempt(
        new ParserAction<Boolean>() {
          @Override
          public Boolean parse() throws ParseError {
            ParserState savedState = getSkippedState();
            setState(savedState);

            Token first = currentToken();
            if (first == null || first.type != ID) return false;

            Token second = lookahead(1);
            return second != null && is(second, COLON);
          }
        });
  }

  private void parseNamedArgumentList(List<ExprNode> args, List<String> argNames) {
    do {
      String argName = expect(ID).text;
      expect(COLON);
      ExprNode value = statementParser.expressionParser.parseExpression();

      args.add(value);
      argNames.add(argName);

      if (!is(COMMA)) {
        break;
      }
      expect(COMMA);
    } while (!is(RPAREN));
  }

  public TypeNode parseType() {

    Keyword visibility = null;
    Token visibilityToken = null;

    if (isVisibilityModifier()) {
      visibilityToken = currentToken();
      Token currentVisibility = consume();
      if (is(currentVisibility, SHARE)) {
        visibility = Keyword.SHARE;
      } else if (is(currentVisibility, LOCAL)) {
        visibility = Keyword.LOCAL;
      } else {
        throw new ParseError(
            "Internal parser error: isVisibilityModifier() returned true for non-visibility keyword: '"
                + currentVisibility.text
                + "'",
            visibilityToken);
      }
    }

    Token typeNameToken = currentToken();
    String typeName = expect(ID).text;

    NamingValidator.validateClassName(typeName, typeNameToken);

    String extendName = null;
    Token extendToken = null; // The 'is' keyword
    Token parentToken = null; // The parent class name token
    
    if (is(IS)) {
      extendToken = currentToken(); // Save the 'is' token
      expect(IS);
      
      parentToken = currentToken(); // Save the parent class name token
      extendName = parseQualifiedName();
    }

    List<String> implementedPolicies = new ArrayList<String>();
    java.util.Map<String, Token> policyTokens = new java.util.HashMap<String, Token>();
    
    while (isWithKeyword()) {
      expect(WITH);
      
      Token policyToken = currentToken();
      String policyName = parseQualifiedName();
      implementedPolicies.add(policyName);
      policyTokens.put(policyName, policyToken);

      while (tryConsume(COMMA)) {
        policyToken = currentToken();
        policyName = parseQualifiedName();
        implementedPolicies.add(policyName);
        policyTokens.put(policyName, policyToken);
      }
    }

    TypeNode type = ASTFactory.createType(typeName, visibility, extendName, typeNameToken);
    type.implementedPolicies = implementedPolicies;
    type.policyTokens = policyTokens;
    type.extendToken = extendToken;
    type.parentToken = parentToken;

    // Set current parsing class
    setCurrentParsingClass(type);

    expect(LBRACE);
    while (!is(RBRACE)) {
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

    // Clear current parsing class
    setCurrentParsingClass(null);

    expect(RBRACE);
    return type;
  }

  // Policy parsing with composition ONLY (no inheritance)
  public PolicyNode parsePolicy() {
    Keyword visibility = null;
    Token visibilityToken = null;
    if (isVisibilityModifier()) {
      visibilityToken = consume();
      if (is(visibilityToken, SHARE)) {
        visibility = Keyword.SHARE;
      } else if (is(visibilityToken, LOCAL)) {
        visibility = Keyword.LOCAL;
      }
      skipWhitespaceAndComments();
    }

    if (!is(POLICY)) {
      throw new ParseError("Expected 'policy' keyword", getLine(), getColumn());
    }
    expect(POLICY);

    Token nameToken = currentToken();
    if (nameToken == null || nameToken.type != ID) {
      throw new ParseError("Expected policy name after 'policy' keyword", getLine(), getColumn());
    }

    String policyName = expect(ID).text;

    if (policyName.length() > 0 && !Character.isUpperCase(policyName.charAt(0))) {
      throw new ParseError(
          "Policy name '" + policyName + "' must start with an uppercase letter",
          nameToken);
    }

    // ONLY allow 'with' for composition, NOT 'is' for inheritance
    List<String> composedPolicies = new ArrayList<String>();
    if (is(WITH)) {
      expect(WITH);
      composedPolicies.add(parseQualifiedName());

      while (tryConsume(COMMA)) {
        composedPolicies.add(parseQualifiedName());
      }
    }

    PolicyNode policy = ASTFactory.createPolicy(policyName, visibility, nameToken);
    policy.composedPolicies = composedPolicies;

    if (!is(LBRACE)) {
      throw new ParseError("Expected '{' after policy name", getLine(), getColumn());
    }
    expect(LBRACE);

    while (!is(RBRACE)) {
      skipWhitespaceAndComments();

      if (isPolicyMethodDeclarationStart()) {
        PolicyMethodNode method = parsePolicyMethod();
        policy.methods.add(method);
      } else if (!is(RBRACE)) {
        Token current = currentToken();
        throw new ParseError(
            "Policy can only contain method declarations and cannot have a body, found: " + current.text,
            current);
      }
    }

    expect(RBRACE);

    // Store the policy for later validation
    availablePolicies.put(policyName, policy);

    // Validate composition (check for cycles)
    validatePolicyComposition(policy);

    return policy;
  }

  public PolicyMethodNode parsePolicyMethod() {
    Token methodNameToken = currentToken();
    String methodName;

    if (is(currentToken(), KEYWORD) && canKeywordBeMethodName(currentToken())) {
      methodName = consume().text;
    } else if (is(currentToken(), ID)) {
      methodName = expect(ID).text;
    } else {
      throw new ParseError("Expected method name in policy declaration", getLine(), getColumn());
    }

    if (methodName.length() > 0 && !Character.isLowerCase(methodName.charAt(0))) {
      throw new ParseError(
          "Method name '" + methodName + "' must start with a lowercase letter",
          methodNameToken);
    }

    PolicyMethodNode method = ASTFactory.createPolicyMethod(methodName, methodNameToken);
    if (!is(LPAREN)) {
      throw new ParseError("Expected '(' after method name", getLine(), getColumn());
    }
    expect(LPAREN);

    if (!is(RPAREN)) {
      method.parameters.add(parseParameter());
      while (tryConsume(COMMA)) {
        method.parameters.add(parseParameter());
      }
    }

    if (!is(RPAREN)) {
      throw new ParseError("Expected ')' after parameters", getLine(), getColumn());
    }
    expect(RPAREN);

    if (isSlotDeclaration()) {
      method.returnSlots = parseSlotContractList();
    }

    return method;
  }

  public boolean isPolicyMethodDeclarationStart() {
    return attempt(
        new ParserAction<Boolean>() {
          @Override
          public Boolean parse() throws ParseError {
            ParserState savedState = getCurrentState();
            try {
              skipWhitespaceAndComments();

              if (is(currentToken(), KEYWORD) && (is(currentToken(), BUILTIN, SHARE, LOCAL))) {
                return false;
              }

              Token nameToken = currentToken();
              if (nameToken == null) return false;

              boolean isValidName =
                  (is(nameToken, ID))
                      || (is(nameToken, KEYWORD) && canKeywordBeMethodName(nameToken));

              if (!isValidName) return false;

              consume();
              skipWhitespaceAndComments();

              return is(LPAREN);
            } finally {
              setState(savedState);
            }
          }
        });
  }

  public boolean isPolicyDeclaration() {
    return attempt(
        new ParserAction<Boolean>() {
          @Override
          public Boolean parse() throws ParseError {
            ParserState savedState = getCurrentState();
            try {
              skipWhitespaceAndComments();

              if (isVisibilityModifier()) {
                consume();
                skipWhitespaceAndComments();
              }

              if (!is(POLICY)) {
                return false;
              }

              consume();
              skipWhitespaceAndComments();

              return is(ID);
            } finally {
              setState(savedState);
            }
          }
        });
  }

  public MethodNode parseMethod() {
    Token startToken = currentToken();

    boolean isBuiltin = false;
    Keyword visibility = Keyword.SHARE;
    boolean isPolicyMethod = false;
    Token visibilityToken = null;

    if (is(POLICY)) {
      expect(POLICY);
      isPolicyMethod = true;

      // Policy methods inherit visibility from class
      TypeNode currentClass = getCurrentParsingClass();
      if (currentClass != null) {
        visibility = currentClass.visibility;
      } else {
        // Default to SHARE if no class context
        visibility = Keyword.SHARE;
      }

    } else if (is(BUILTIN)) {
      expect(BUILTIN);
      isBuiltin = true;
      visibility = Keyword.SHARE;
    } else if (isVisibilityModifier()) {
      visibilityToken = currentToken();
      Token currentVisibility= consume();

      if (is(currentVisibility, SHARE)) {
        visibility = Keyword.SHARE;
      } else if (is(currentVisibility, LOCAL)) {
        visibility = Keyword.LOCAL;
      } else {
        throw new ParseError(
            "Internal parser error: isVisibilityModifier() returned true for non-visibility keyword: '"
                + currentVisibility.text
                + "'",
            visibilityToken);
      }
    }

    String methodName;
    Token nameToken = currentToken();
    if (is(currentToken(), KEYWORD) && canKeywordBeMethodName(currentToken())) {
      methodName = currentToken().text;
      consume();
    } else if (is(currentToken(), ID)) {
      methodName = expect(ID).text;
    } else {
      throw new ParseError(
          "Expected method name (identifier or allowed keyword)", getLine(), getColumn());
    }

    NamingValidator.validateMethodName(methodName, startToken);

    MethodNode method = ASTFactory.createMethod(methodName, visibility, null, nameToken);
    method.isBuiltin = isBuiltin;
    method.isPolicyMethod = isPolicyMethod;

    expect(LPAREN);

    if (isBuiltin) {
      int parenDepth = 1;
      while (!is(EOF) && parenDepth > 0) {
        Token t = currentToken();
        if (is(t, LPAREN)) {
          parenDepth++;
        } else if (is(t, RPAREN)) {
          parenDepth--;
          if (parenDepth == 0) {
            expect(RPAREN);
            break;
          }
        }
        consume();
      }
    } else {
      if (!is(RPAREN)) {
        method.parameters.add(parseParameter());
        while (tryConsume(COMMA)) {
          method.parameters.add(parseParameter());
        }
      }
      expect(RPAREN);
    }

    if (isSlotDeclaration()) {
      method.returnSlots = parseSlotContractList();
    } else {
      method.returnSlots = new ArrayList<SlotNode>();
    }

    skipWhitespaceAndComments();

    // Validate policy method implementation
    // Note: This validation will be done later in MainParser when we have access to the current
    // program

    if (isBuiltin) {
      while (getPosition() < tokens.size()) {
        Token current = currentToken();

        if (is(current, RBRACE)
            || isVisibilityModifier(current)
            || is(current, KEYWORD) && is(current, BUILTIN, SHARE, LOCAL, POLICY)) {
          break;
        }

        consume();
      }

      if (is(TILDE_ARROW, LBRACE)) {
        Token current = currentToken();
        throw new ParseError(
            "Builtin method '"
                + methodName
                + "' cannot have a body. "
                + "Builtin methods are only declarations, not implementations.\n"
                + "Remove '~>' or '{...}' after builtin method signature.",
            current);
      }

      return method;
    }

    if (is(TILDE_ARROW)) {
      Token tildeArrowToken = currentToken();
      expect(TILDE_ARROW);

      skipWhitespaceAndComments();

      List<SlotAssignmentNode> slotAssignments = new ArrayList<SlotAssignmentNode>();

      slotAssignments.add(parseSingleSlotAssignment());

      while (tryConsume(COMMA)) {
        skipWhitespaceAndComments();
        slotAssignments.add(parseSingleSlotAssignment());
      }

      if (slotAssignments.size() == 1) {
        method.body.add(slotAssignments.get(0));
      } else {
        MultipleSlotAssignmentNode multiAssign =
            ASTFactory.createMultipleSlotAssignment(slotAssignments, tildeArrowToken);
        method.body.add(multiAssign);
      }

    } else if (is(LBRACE)) {
      expect(LBRACE);
      while (!is(RBRACE)) {
        method.body.add(statementParser.parseStatement());
      }
      expect(RBRACE);
    } else {
      Token current = currentToken();
      throw new ParseError(
          "Expected '~>' or '{' after method signature, but found "
              + getTypeName(current.type)
              + " ('"
              + current.text
              + "')",
          getLine(),
          getColumn());
    }

    return method;
  }

  private SlotAssignmentNode parseSingleSlotAssignment() {
    String slotName = null;
    ExprNode value;
    Token colonToken = null;

    if (is(currentToken(), ID)) {
      Token afterId = lookahead(1);
      if (afterId != null && is(afterId, COLON)) {
        slotName = expect(ID).text;
        colonToken = currentToken();
        expect(COLON);
        value = statementParser.expressionParser.parseExpression();
      } else {
        slotName = null;
        value = statementParser.expressionParser.parseExpression();
      }
    } else {
      slotName = null;
      value = statementParser.expressionParser.parseExpression();
    }

    return ASTFactory.createSlotAssignment(slotName, value, colonToken);
  }

  public List<SlotNode> parseSlotContractList() {
    expect(DOUBLE_COLON);

    List<SlotNode> slots = new ArrayList<SlotNode>();

    boolean firstSlot = true;
    boolean isNamedMode = false;
    int index = 0;

    do {
      String name;
      String type;
      Token nameToken = null;

      if (firstSlot) {
        if (is(currentToken(), ID)) {
          isNamedMode = true;
          nameToken = currentToken();
          name = expect(ID).text;
          expect(COLON);
          type = parseTypeReference();
        } else {
          isNamedMode = false;
          name = String.valueOf(index);
          type = parseTypeReference();
        }
        firstSlot = false;
      } else {
        if (isNamedMode) {
          if (currentToken().type != ID) {
            throw new ParseError(
                "Mixed slot declaration styles not allowed. Expected name for slot.",
                getLine(),
                getColumn());
          }
          nameToken = currentToken();
          name = expect(ID).text;
          expect(COLON);
          type = parseTypeReference();
        } else {
          if (is(currentToken(), ID)) {
            throw new ParseError(
                "Mixed slot declaration styles not allowed. Found name '"
                    + currentToken().text
                    + "' in unnamed slot list.",
                getLine(),
                getColumn());
          }
          name = String.valueOf(index);
          type = parseTypeReference();
        }
      }

      slots.add(ASTFactory.createSlot(type, name, nameToken));
      index++;

    } while (tryConsume(COMMA));

    return slots;
  }

  public FieldNode parseField() {
    Token startToken = currentToken();

    Keyword visibility = null;
    Token visibilityToken = null;
    if (isVisibilityModifier()) {
      visibilityToken = consume();  // consume visibility modifier
      if (is(visibilityToken, SHARE)) {
        visibility = SHARE;
      } else if (is(visibilityToken, LOCAL)) {
        visibility = LOCAL;
      }
    }

    Token fieldNameToken = currentToken();
    String fieldName = expect(ID).text;

    if (fieldName.equals(UNDERSCORE.toString())) {
      throw new ParseError(
          "Field name cannot be '_'. Underscore is reserved for discard/placeholder.",
          startToken);
    }

    expect(COLON);

    String fieldType = parseTypeReference();

    if (NamingValidator.isAllCaps(fieldName)) {
      NamingValidator.validateConstantName(fieldName, startToken);
    } else {
      NamingValidator.validateVariableName(fieldName, startToken);
    }

    FieldNode field = ASTFactory.createField(fieldName, fieldType, fieldNameToken);
    if (visibility != null) {
      // Handle visibility if needed
    }

    if (tryConsume(ASSIGN)) {
      field.value = statementParser.expressionParser.parseExpression();
    }

    return field;
}

  public ParamNode parseParameter() {
    Token startToken = currentToken();
    String name = expect(ID).text;

    if (is(DOUBLE_COLON_ASSIGN)) {
      expect(DOUBLE_COLON_ASSIGN);

      ExprNode defaultValue = statementParser.expressionParser.parsePrimaryExpression();

      if (!isSimpleLiteral(defaultValue)) {
        throw new ParseError(
            "Parameter inference (:=) can only be used with literals. "
                + "Use explicit typing for expressions: "
                + name
                + ": Type = expression",
            startToken);
      }

      String inferredType = inferTypeFromLiteral(defaultValue);
      if (inferredType == null) {
        throw new ParseError(
            "Cannot infer parameter type from literal. Use explicit typing: "
                + name
                + ": Type = "
                + defaultValue,
            startToken);
      }

      NamingValidator.validateParameterName(name, startToken);

      ParamNode param = ASTFactory.createParam(name, inferredType, defaultValue, true, startToken);
      param.hasDefaultValue = true;
      return param;
    }

    expect(COLON);
    String type = parseTypeReference();

    ExprNode defaultValue = null;
    if (tryConsume(ASSIGN)) {
      defaultValue = statementParser.expressionParser.parseExpression();
    }

    NamingValidator.validateParameterName(name, startToken);
    ParamNode param = ASTFactory.createParam(name, type, defaultValue, false, startToken);
    if (defaultValue != null) {
      param.hasDefaultValue = true;
    }
    return param;
  }

  private boolean isSimpleLiteral(ExprNode expr) {
    if (expr instanceof ExprNode) {
      ExprNode e = (ExprNode) expr;
      if (e.value != null || e.isNone) {
        return e.name == null;
      }
    }

    if (expr instanceof ArrayNode) {
      ArrayNode arr = (ArrayNode) expr;

      if (arr.elements.size() == 1 && arr.elements.get(0) instanceof RangeNode) {
        RangeNode range = (RangeNode) arr.elements.get(0);
        return isSimpleLiteral(range.start)
            && isSimpleLiteral(range.end)
            && (range.step == null || isSimpleLiteral(range.step));
      }

      for (ExprNode elem : arr.elements) {
        if (!isSimpleLiteral(elem)) return false;
      }
      return true;
    }

    if (expr instanceof RangeNode) {
      RangeNode range = (RangeNode) expr;
      return isSimpleLiteral(range.start)
          && isSimpleLiteral(range.end)
          && (range.step == null || isSimpleLiteral(range.step));
    }

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

    if (expr instanceof ExprNode) {
      ExprNode e = (ExprNode) expr;

      if (e.isNone) return null;

      Object value = e.value;
      if (value instanceof Integer || value instanceof Long) return INT.toString();
      if (value instanceof Float || value instanceof Double || value instanceof BigDecimal)
        return FLOAT.toString();
      if (value instanceof String) return TEXT.toString();
      if (value instanceof Boolean) return BOOL.toString();
    }

    if (expr instanceof ArrayNode) {
      ArrayNode arr = (ArrayNode) expr;
      if (arr.elements.isEmpty()) return null;

      if (arr.elements.size() == 1 && arr.elements.get(0) instanceof RangeNode) {
        return "[]";
      }

      String elementType = inferTypeFromLiteral(arr.elements.get(0));
      if (elementType != null) {
        return "[" + elementType + "]";
      }
      return null;
    }

    if (expr instanceof RangeNode) {
      return "[]";
    }

    if (expr instanceof TupleNode) {
      TupleNode tuple = (TupleNode) expr;
      if (tuple.elements.isEmpty()) return null;

      StringBuilder sb = new StringBuilder("(");
      for (int i = 0; i < tuple.elements.size(); i++) {
        String elemType = inferTypeFromLiteral(tuple.elements.get(i));
        if (elemType == null) return null;

        if (i > 0) sb.append(",");
        sb.append(elemType);
      }
      sb.append(")");
      return sb.toString();
    }

    return null;
  }

  public boolean isSlotDeclaration() {
    return isSymbolAt(0, DOUBLE_COLON);
  }

  private boolean isMethodDeclaration() {
    return lookahead(
        new ParserAction<Boolean>() {
          @Override
          public Boolean parse() throws ParseError {
            int offset = 0;

            while (lookaheadOffset(offset)) offset++;

            Token first = lookahead(offset);
            if (first == null) return false;

            // Check for 'policy' keyword for policy methods
            if (is(first, POLICY)) {
              offset++;
              while (lookaheadOffset(offset)) offset++;
            } else if (is(first, BUILTIN, SHARE, LOCAL)) {
              offset++;
              while (lookaheadOffset(offset)) offset++;
            }

            Token nameToken = lookahead(offset);
            if (nameToken == null) return false;

            boolean isValidName =
                (is(nameToken, ID))
                    || (is(nameToken, KEYWORD) && canKeywordBeMethodName(nameToken));

            if (!isValidName) return false;

            offset++;
            while (lookaheadOffset(offset)) offset++;

            Token parenToken = lookahead(offset);
            return parenToken != null && is(parenToken, LPAREN);
          }
        });
  }

  private boolean isFieldDeclaration() {
    return lookahead(
        new ParserAction<Boolean>() {
          @Override
          public Boolean parse() throws ParseError {
            if (isVisibilityModifier()) {
              consume();  // consume visibility modifier
            }

            if (!is(ID)) {
                return false;
            }
            expect(ID);

            if (!is(COLON)) {
                return false;
            }
            expect(COLON);

            if (!isTypeStart(currentToken())) {
                return false;
            }

            String type = parseTypeReference();
            if (type == null) {
                return false;
            }

            if (is(LPAREN)) {
                return false;
            }

            return true;
          }
        });
}
}