package cod.parser;

import cod.ast.ASTFactory;
import cod.ast.nodes.*;
import cod.error.ParseError;
import cod.parser.context.*;
import cod.semantic.ImportResolver;
import cod.semantic.NamingValidator;
import cod.syntax.Keyword;
import java.util.*;

import cod.lexer.Token;
import static cod.lexer.TokenType.*;
import static cod.syntax.Keyword.*;
import static cod.syntax.Symbol.*;

public class DeclarationParser extends BaseParser {

  private final StatementParser statementParser;
  private final ImportResolver importResolver;
  private final SlotParser slotParser;

  private TypeNode currentParsingClass = null;
  private Map<String, PolicyNode> availablePolicies = new HashMap<String, PolicyNode>();

  public DeclarationParser(
      ParserContext ctx, StatementParser statementParser, ImportResolver importResolver) {
    super(ctx);
    this.statementParser = statementParser;
    this.importResolver = importResolver;
    this.slotParser = new SlotParser(this);
  }

  @Override
  protected BaseParser createIsolatedParser(ParserContext isolatedCtx) {
    return new DeclarationParser(isolatedCtx, this.statementParser, this.importResolver);
  }
  
  public StatementParser getStatementParser() {
    return statementParser;
  }

  private void setCurrentParsingClass(TypeNode type) {
    currentParsingClass = type;
  }

  private TypeNode getCurrentParsingClass() {
    return currentParsingClass;
  }

  private PolicyNode findPolicy(String policyName) {
    if (availablePolicies.containsKey(policyName)) {
      return availablePolicies.get(policyName);
    }

    if (!nil(importResolver)) {
      try {
        return importResolver.findPolicy(policyName);
      } catch (Exception e) {
      }
    }

    if (policyName.contains(".")) {
      return null;
    }

    return null;
  }

  private TypeNode findClassByName(String className, ProgramNode currentProgram) {
    if (!nil(currentProgram, currentProgram.unit, currentProgram.unit.types)) {
      for (TypeNode type : currentProgram.unit.types) {
        if (type.name.equals(className)) {
          return type;
        }
      }
    }

    if (!nil(importResolver)) {
      return importResolver.findType(className);
    }

    return null;
  }

  private List<PolicyMethodNode> getAllPolicyMethods(PolicyNode policy) {
    if (nil(policy)) {
      return new ArrayList<PolicyMethodNode>();
    }

    List<PolicyMethodNode> allMethods = new ArrayList<PolicyMethodNode>();
    Set<String> visitedPolicies = new HashSet<String>();

    collectPolicyMethodsViaComposition(policy, allMethods, visitedPolicies);

    return allMethods;
  }

  private void collectPolicyMethodsViaComposition(
      PolicyNode policy, List<PolicyMethodNode> allMethods, Set<String> visited) {
    if (nil(policy) || visited.contains(policy.name)) {
      return;
    }

    visited.add(policy.name);

    if (!nil(policy.composedPolicies)) {
      for (String composedName : policy.composedPolicies) {
        PolicyNode composed = findPolicy(composedName);
        if (!nil(composed)) {
          collectPolicyMethodsViaComposition(composed, allMethods, visited);
        }
      }
    }

    if (policy.methods != null) {
      allMethods.addAll(policy.methods);
    }
  }

  private List<String> getAllAffectingPolicies(TypeNode currentClass, ProgramNode currentProgram) {
    List<String> allPolicies = new ArrayList<String>();
    if (nil(currentClass)) {
      return allPolicies;
    }

    Set<String> visitedClasses = new HashSet<String>();
    collectAffectingPoliciesRecursive(currentClass, allPolicies, visitedClasses, currentProgram);

    return allPolicies;
  }

  private void collectAffectingPoliciesRecursive(
      TypeNode type, List<String> allPolicies, Set<String> visited, ProgramNode currentProgram) {
    if (nil(type) || visited.contains(type.name)) {
      return;
    }

    visited.add(type.name);

    if (!nil(type.extendName)) {
      TypeNode parent = findClassByName(type.extendName, currentProgram);
      if (!nil(parent)) {
        collectAffectingPoliciesRecursive(parent, allPolicies, visited, currentProgram);
      }
    }

    if (!nil(type.implementedPolicies)) {
      for (String policyName : type.implementedPolicies) {
        if (!allPolicies.contains(policyName)) {
          allPolicies.add(policyName);
        }
      }
    }
  }

  private List<String> getAncestorPolicies(TypeNode currentClass, ProgramNode currentProgram) {
    List<String> ancestorPolicies = new ArrayList<String>();
    if (nil(currentClass, currentClass.extendName)) {
      return ancestorPolicies;
    }

    List<String> allAffecting = getAllAffectingPolicies(currentClass, currentProgram);

    if (!nil(currentClass.implementedPolicies)) {
      for (String ownPolicy : currentClass.implementedPolicies) {
        allAffecting.remove(ownPolicy);
      }
    }

    return allAffecting;
  }

  private String getRequiringAncestorWithPolicy(String methodName, TypeNode currentClass, 
                                               ProgramNode currentProgram) {
    if (nil(currentClass, currentClass.extendName)) {
      return null;
    }

    TypeNode current = findClassByName(currentClass.extendName, currentProgram);
    while (!nil(current)) {
      if (!nil(current.implementedPolicies)) {
        for (String policyName : current.implementedPolicies) {
          PolicyNode policy = findPolicy(policyName);
          if (!nil(policy)) {
            List<PolicyMethodNode> allRequiredMethods = getAllPolicyMethods(policy);
            for (PolicyMethodNode policyMethod : allRequiredMethods) {
              if (policyMethod.methodName.equals(methodName)) {
                return current.name + " (implements policy '" + policyName + "')";
              }
            }
          }
        }
      }

      if (!nil(current.extendName)) {
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

    for (String policyName : ancestorPolicies) {
      PolicyNode policy = findPolicy(policyName);
      if (!nil(policy)) {
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

  private Set<String> getAllMethodsRequiredByAncestors(
      TypeNode currentClass, ProgramNode currentProgram) {
    Set<String> requiredMethods = new HashSet<String>();
    List<String> ancestorPolicies = getAncestorPolicies(currentClass, currentProgram);

    for (String policyName : ancestorPolicies) {
      PolicyNode policy = findPolicy(policyName);
      if (!nil(policy)) {
        List<PolicyMethodNode> allRequiredMethods = getAllPolicyMethods(policy);
        for (PolicyMethodNode method : allRequiredMethods) {
          requiredMethods.add(method.methodName);
        }
      }
    }

    return requiredMethods;
  }

  private void validatePolicyComposition(PolicyNode policy) {
    if (nil(policy, policy.composedPolicies) || policy.composedPolicies.isEmpty()) {
      return;
    }

    Set<String> visited = new HashSet<String>();
    visited.add(policy.name);

    for (String composedName : policy.composedPolicies) {
      checkForCompositionCycle(policy.name, composedName, visited);
    }
  }

  private void checkForCompositionCycle(
      String currentPolicy, String composedName, Set<String> visited) {
    if (visited.contains(composedName)) {
      throw error(
          "Circular composition detected in policies: "
              + String.join(" -> ", visited)
              + " -> "
              + composedName);
    }

    visited.add(composedName);

    PolicyNode composed = findPolicy(composedName);
    if (!nil(composed) && !nil(composed.composedPolicies)) {
      for (String nestedComposed : composed.composedPolicies) {
        checkForCompositionCycle(currentPolicy, nestedComposed, visited);
      }
    }

    visited.remove(composedName);
  }

  private boolean areTypesCompatible(String implType, String policyType) {
    if (nil(implType, policyType)) {
      return false;
    }

    if (implType.equals(policyType)) {
      return true;
    }

    if (policyType.contains("|")) {
      String[] policyUnion = policyType.split("\\|");
      for (String unionMember : policyUnion) {
        if (unionMember.trim().equals(implType)) {
          return true;
        }
      }
    }

    if (policyType.equals("[]") && implType.startsWith("[") && implType.endsWith("]")) {
      return true;
    }

    if (policyType.equals("type") && !nil(implType)) {
      return true;
    }

    return false;
  }

  private void validatePolicyMethod(MethodNode method, ProgramNode currentProgram) {
    TypeNode currentClass = getCurrentParsingClass();

    if (nil(currentClass)) {
      throw error(
          "Policy method '" + method.methodName + "' can only be declared in a class");
    }

    boolean isRequiredByOwnPolicy = false;
    String requiringPolicy = null;
    PolicyMethodNode requiredSignature = null;

    if (!nil(currentClass.implementedPolicies)) {
      for (String policyName : currentClass.implementedPolicies) {
        PolicyNode policy = findPolicy(policyName);
        if (!nil(policy)) {
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

    boolean isRequiredByAncestor = false;
    String requiringAncestor = null;

    if (!isRequiredByOwnPolicy) {
      isRequiredByAncestor =
          isMethodRequiredFromAncestorPolicy(method.methodName, currentClass, currentProgram);
      if (isRequiredByAncestor) {
        requiringAncestor = getRequiringAncestorWithPolicy(method.methodName, currentClass, currentProgram);

        if (!nil(requiringAncestor)) {
          String ancestorName = requiringAncestor.split(" ")[0];
          TypeNode ancestor = findClassByName(ancestorName, currentProgram);
          if (!nil(ancestor) && !nil(ancestor.implementedPolicies)) {
            for (String policyName : ancestor.implementedPolicies) {
              PolicyNode policy = findPolicy(policyName);
              if (!nil(policy)) {
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

    if (!isRequiredByOwnPolicy && !isRequiredByAncestor) {
      throw error(
          "Method '"
              + method.methodName
              + "' is not required by any implemented policy or ancestor's policies. "
              + "Remove 'policy' keyword or add to a policy.");
    }

    if (requiredSignature != null) {
      if (method.parameters.size() != requiredSignature.parameters.size()) {
        String errorSource =
            isRequiredByAncestor
                ? "ancestor '" + requiringAncestor + "'"
                : "policy '" + requiringPolicy + "'";

        throw error(
            "Policy method '"
                + method.methodName
                + "' signature mismatch for "
                + errorSource
                + ": expected "
                + requiredSignature.parameters.size()
                + " parameters, got "
                + method.parameters.size());
      }

      for (int i = 0; i < method.parameters.size(); i++) {
        ParamNode implParam = method.parameters.get(i);
        ParamNode policyParam = requiredSignature.parameters.get(i);

        if (!areTypesCompatible(implParam.type, policyParam.type)) {
          String errorSource =
              isRequiredByAncestor
                  ? "ancestor '" + requiringAncestor + "'"
                  : "policy '" + requiringPolicy + "'";

          throw error(
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
                  + implParam.type);
        }
      }

      if (!nil(requiredSignature.returnSlots) && !requiredSignature.returnSlots.isEmpty()) {
        if (nil(method.returnSlots) || method.returnSlots.isEmpty()) {
        } else if (method.returnSlots.size() != requiredSignature.returnSlots.size()) {
          String errorSource =
              isRequiredByAncestor
                  ? "ancestor '" + requiringAncestor + "'"
                  : "policy '" + requiringPolicy + "'";

          throw error(
              "Policy method '"
                  + method.methodName
                  + "' return slot mismatch for "
                  + errorSource
                  + ": expected "
                  + requiredSignature.returnSlots.size()
                  + " slots, got "
                  + method.returnSlots.size());
        } else {
          for (int i = 0; i < method.returnSlots.size(); i++) {
            SlotNode implSlot = method.returnSlots.get(i);
            SlotNode policySlot = requiredSignature.returnSlots.get(i);

            if (!areTypesCompatible(implSlot.type, policySlot.type)) {
              String errorSource =
                  isRequiredByAncestor
                      ? "ancestor '" + requiringAncestor + "'"
                      : "policy '" + requiringPolicy + "'";

              throw error(
                  "Policy method '"
                      + method.methodName
                      + "' return slot type mismatch for slot "
                      + (i + 1)
                      + " in "
                      + errorSource
                      + ": expected "
                      + policySlot.type
                      + ", got "
                      + implSlot.type);
            }
          }
        }
      }
    }
  }

  public void validateClassViralPolicies(TypeNode type, ProgramNode currentProgram) {
    if (nil(type, type.extendName)) {
      return;
    }

    Set<String> requiredMethods = getAllMethodsRequiredByAncestors(type, currentProgram);

    for (String methodName : requiredMethods) {
      boolean implementsMethod = false;

      if (!nil(type.methods)) {
        for (MethodNode method : type.methods) {
          if (method.methodName.equals(methodName) && method.isPolicyMethod) {
            implementsMethod = true;
            break;
          }
        }
      }

      if (!implementsMethod) {
        String requiringAncestor = getRequiringAncestorWithPolicy(methodName, type, currentProgram);
        
        Token errorToken = null;
        if (type.parentToken != null) {
          errorToken = type.parentToken;
        } else if (type.extendToken != null) {
          errorToken = type.extendToken;
        }
        
        if (!nil(requiringAncestor)) {
          if (errorToken != null) {
            throw error(
                "Class '"
                    + type.name
                    + "' inherits from '" + type.extendName + "'\n" +
                "The ancestor " + requiringAncestor + " requires policy method '" + methodName + "'\n" +
                "Add: policy " + methodName + "(...) { ... } inside the class",
                errorToken);
          } else {
            throw error(
                "Class '"
                    + type.name
                    + "' inherits from '" + type.extendName + "'\n" +
                "The ancestor " + requiringAncestor + " requires policy method '" + methodName + "'\n" +
                "Add: policy " + methodName + "(...) { ... } inside the class");
          }
        } else {
          if (errorToken != null) {
            throw error(
                "Class '"
                    + type.name
                    + "' inherits from '" + type.extendName + "' which requires policy method '"
                    + methodName
                    + "'\n"
                    + "Add: policy " + methodName + "(...) { ... } inside the class",
                errorToken);
          } else {
            throw error(
                "Class '"
                    + type.name
                    + "' inherits from '" + type.extendName + "' which requires policy method '"
                    + methodName
                    + "'\n"
                    + "Add: policy " + methodName + "(...) { ... } inside the class");
          }
        }
      }
    }
  }

  public void validateAllPolicyMethods(TypeNode type, ProgramNode currentProgram) {
    if (nil(type, type.methods)) {
      return;
    }

    TypeNode savedClass = getCurrentParsingClass();
    setCurrentParsingClass(type);

    try {
      for (MethodNode method : type.methods) {
        if (method.isPolicyMethod) {
          validatePolicyMethod(method, currentProgram);
        }
      }
    } finally {
      setCurrentParsingClass(savedClass);
    }
  }
  
  private boolean wsComments(int offset) {
    return is(next(offset), WS, LINE_COMMENT, BLOCK_COMMENT);
  }

  public boolean isConstructorDeclaration() {
    return next(
        new ParserAction<Boolean>() {
          @Override
          public Boolean parse() throws ParseError {
            int offset = 0;

            while (wsComments(offset)) offset++;

            Token first = next(offset);
            if (!is(first, SHARE, LOCAL)) return false;
            offset++;

            while (wsComments(offset)) offset++;

            Token nameToken = next(offset);
            boolean thisAsKeywordOrIDName = is(nameToken, THIS);

            if (!thisAsKeywordOrIDName) return false;

            offset++;
            while (wsComments(offset)) offset++;

            Token parenToken = next(offset);
            return is(parenToken, LPAREN);
          }
        });
  }

  public ConstructorNode parseConstructor() {
    Token startToken = now();
    Token thisToken = null;

    if (is(SHARE, LOCAL)) {
      consume();
    }

    thisToken = now();
    Token current = consume();
    if (!is(current, THIS)) {
      throw error(
          "Constructor must be named 'this', found: " + current.getText(), startToken);
    }

    ConstructorNode constructor = ASTFactory.createConstructor(null, null, thisToken);

    expect(LPAREN);
    if (!is(RPAREN)) {
      constructor.parameters.add(parseParameter());
      while (consume(COMMA)) {
        constructor.parameters.add(parseParameter());
      }
    }
    expect(RPAREN);

    if (isSlotDeclaration()) {
      throw error(
          "Constructors cannot have return slots contracts (:: syntax). "
              + "Remove '::' and return type declarations from constructor.");
    }

    if (is(TILDE_ARROW)) {
      throw error(
          "Constructors cannot use inline return (~>) syntax. "
              + "Use a block body: this(...) { ... }");
    }

    boolean hasSuperCall = false;
    if (looksLikeSuperConstructorCall()) {
      hasSuperCall = true;
      constructor.body.add(parseSuperConstructorCall());
    }

    if (is(LBRACE)) {
      expect(LBRACE);
      while (!is(RBRACE)) {
        constructor.body.add(statementParser.parseStmt());
      }
      expect(RBRACE);
    } else if (!hasSuperCall) {
      throw error(
          "Constructor must have a body: this(...) { ... } or this(...) super(...) { ... }");
    }

    return constructor;
  }

  private boolean looksLikeSuperConstructorCall() {
    return attempt(
        new ParserAction<Boolean>() {
          @Override
          public Boolean parse() throws ParseError {
            if (!is(SUPER)) return false;
            expect(SUPER);
            return is(LPAREN);
          }
        });
  }

  private MethodCallNode parseSuperConstructorCall() {
    Token superToken = now();
    expect(SUPER);

    String zuper = SUPER.toString();
    MethodCallNode superCall = ASTFactory.createMethodCall(zuper, zuper, superToken);
    superCall.isSuperCall = true;
    expect(LPAREN);

    if (!is(RPAREN)) {
      if (isNamedArgument()) {
        parseNamedArgumentList(superCall.arguments, superCall.argNames);
      } else {
        superCall.arguments.add(statementParser.expressionParser.parseExpr());
        superCall.argNames.add(null);
        while (consume(COMMA)) {
          superCall.arguments.add(statementParser.expressionParser.parseExpr());
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
            Token first = now();
            if (!is(first, ID)) return false;

            Token second = next();
            return is(second, COLON);
          }
        });
  }

  private void parseNamedArgumentList(List<ExprNode> args, List<String> argNames) {
    do {
      String argName = expect(ID).getText();
      expect(COLON);
      ExprNode value = statementParser.expressionParser.parseExpr();

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

    if (is(SHARE, LOCAL)) {
        visibilityToken = now();
        Token currentVisibility = consume();
        if (is(currentVisibility, SHARE)) {
            visibility = Keyword.SHARE;
        } else if (is(currentVisibility, LOCAL)) {
            visibility = Keyword.LOCAL;
        } else {
            throw error(
                "Internal parser error: isVisibilityModifier() returned true for non-visibility keyword: '"
                    + currentVisibility.getText()
                    + "'",
                visibilityToken);
        }
    }

    Token typeNameToken = now();
    String typeName = expect(ID).getText();

    // Check if this is actually a method declaration (has parentheses after name)
    if (is(LPAREN)) {
        // This is a method, not a class - restore and let parseMethod handle it
        restore();
        return null; // Signal that this isn't a type
    }

    NamingValidator.validateClassName(typeName, typeNameToken);

    String extendName = null;
    Token extendToken = null;
    Token parentToken = null;
    
    if (is(IS)) {
        extendToken = now();
        expect(IS);
        
        parentToken = now();
        extendName = parseQualifiedName();
    }

    List<String> implementedPolicies = new ArrayList<String>();
    java.util.Map<String, Token> policyTokens = new java.util.HashMap<String, Token>();
    
    while (is(WITH)) {
        expect(WITH);
        
        Token policyToken = now();
        String policyName = parseQualifiedName();
        implementedPolicies.add(policyName);
        policyTokens.put(policyName, policyToken);

        while (consume(COMMA)) {
            policyToken = now();
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
            type.statements.add(statementParser.parseStmt());
        }
    }

    setCurrentParsingClass(null);

    expect(RBRACE);
    return type;
}

  public PolicyNode parsePolicy() {
    Keyword visibility = null;
    Token visibilityToken = null;
    if (is(SHARE, LOCAL)) {
      visibilityToken = consume();
      if (is(visibilityToken, SHARE)) {
        visibility = Keyword.SHARE;
      } else if (is(visibilityToken, LOCAL)) {
        visibility = Keyword.LOCAL;
      }
    }

    if (!is(POLICY)) {
      throw error("Expected 'policy' keyword");
    }
    expect(POLICY);

    Token nameToken = now();
    String policyName = expect(ID).getText();

    if (policyName.length() > 0 && !Character.isUpperCase(policyName.charAt(0))) {
      throw error(
          "Policy name '" + policyName + "' must start with an uppercase letter",
          nameToken);
    }

    List<String> composedPolicies = new ArrayList<String>();
    if (is(WITH)) {
      expect(WITH);
      composedPolicies.add(parseQualifiedName());

      while (consume(COMMA)) {
        composedPolicies.add(parseQualifiedName());
      }
    }

    PolicyNode policy = ASTFactory.createPolicy(policyName, visibility, nameToken);
    policy.composedPolicies = composedPolicies;

    if (!is(LBRACE)) {
      throw error("Expected '{' after policy name");
    }
    expect(LBRACE);

    while (!is(RBRACE)) {
      if (isPolicyMethodDeclarationStart()) {
        PolicyMethodNode method = parsePolicyMethod();
        policy.methods.add(method);
      } else if (!is(RBRACE)) {
        Token current = now();
        throw error(
            "Policy can only contain method declarations and cannot have a body, found: " + current.getText(),
            current);
      }
    }

    expect(RBRACE);

    availablePolicies.put(policyName, policy);

    validatePolicyComposition(policy);

    return policy;
  }

  public PolicyMethodNode parsePolicyMethod() {
    Token methodNameToken = now();
    String methodName;

    if (canBeMethod(now())) {
      methodName = consume().getText();
    } else if (is(ID)) {
      methodName = expect(ID).getText();
    } else {
      throw error("Expected method name in policy declaration");
    }

    if (methodName.length() > 0 && !Character.isLowerCase(methodName.charAt(0))) {
      throw error(
          "Method name '" + methodName + "' must start with a lowercase letter",
          methodNameToken);
    }

    PolicyMethodNode method = ASTFactory.createPolicyMethod(methodName, methodNameToken);
    if (!is(LPAREN)) {
      throw error("Expected '(' after method name");
    }
    expect(LPAREN);

    if (!is(RPAREN)) {
      method.parameters.add(parseParameter());
      while (consume(COMMA)) {
        method.parameters.add(parseParameter());
      }
    }

    if (!is(RPAREN)) {
      throw error("Expected ')' after parameters");
    }
    expect(RPAREN);

    if (isSlotDeclaration()) {
      method.returnSlots = slotParser.parseSlotContract();
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
              if (is(BUILTIN, SHARE, LOCAL)) {
                return false;
              }

              Token nameToken = now();

              boolean isValidName = is(nameToken, ID) || canBeMethod(nameToken);

              if (!isValidName) return false;

              consume();
              
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
              if (is(SHARE, LOCAL)) {
                consume();
              }

              if (!is(POLICY)) {
                return false;
              }

              consume();
              
              return is(ID);
            } finally {
              setState(savedState);
            }
          }
        });
  }

  // Recursive method to check for slot assignments in nested blocks
  private boolean hasSlotAssignmentsInBody(StmtNode stmt) {
    if (stmt instanceof SlotAssignmentNode || stmt instanceof MultipleSlotAssignmentNode) {
      return true;
    }
    if (stmt instanceof BlockNode) {
      BlockNode block = (BlockNode) stmt;
      for (StmtNode child : block.statements) {
        if (hasSlotAssignmentsInBody(child)) {
          return true;
        }
      }
    }
    if (stmt instanceof StmtIfNode) {
      StmtIfNode ifNode = (StmtIfNode) stmt;
      if (hasSlotAssignmentsInBody(ifNode.thenBlock)) {
        return true;
      }
      if (ifNode.elseBlock != null && hasSlotAssignmentsInBody(ifNode.elseBlock)) {
        return true;
      }
    }
    if (stmt instanceof ForNode) {
      ForNode forNode = (ForNode) stmt;
      if (hasSlotAssignmentsInBody(forNode.body)) {
        return true;
      }
    }
    return false;
  }

  // ========== DEAD CODE VALIDATION FOR ~> ==========
  
  private void validateNoStatementsAfterReturn(BlockNode block, String methodName, Token startToken) {
    if (block == null || block.statements == null) {
      return;
    }
    
    boolean foundReturn = false;
    int returnIndex = -1;
    
    for (int i = 0; i < block.statements.size(); i++) {
      StmtNode stmt = block.statements.get(i);
      
      if (stmt instanceof SlotAssignmentNode || stmt instanceof MultipleSlotAssignmentNode) {
        if (foundReturn) {
          // Multiple ~> statements in same block
          throw error(
              "Method '" + methodName + "' has return contract (::) but contains multiple ~> statements in the same block.\n" +
              "Only one ~> statement is allowed per block. Use comma-separated assignments:\n" +
              "  ~> slot1: value1, slot2: value2",
              startToken);
        }
        foundReturn = true;
        returnIndex = i;
      }
    }
    
    // If we found a ~> statement, check if there's anything after it
    if (foundReturn && returnIndex < block.statements.size() - 1) {
      StmtNode deadCodeStmt = block.statements.get(returnIndex + 1);
      Token deadCodeToken = deadCodeStmt.getSourceSpan() != null ? 
                            deadCodeStmt.getSourceSpan().getErrorToken() : startToken;
      
      throw error(
          "Method '" + methodName + "' has return contract (::) but has dead code after ~>.\n" +
          "The ~> statement is the return point - any code after it will never execute.\n" +
          "Remove the dead code or move it before the ~> statement.",
          deadCodeToken);
    }
    
    // Recursively validate nested blocks
    for (StmtNode stmt : block.statements) {
      if (stmt instanceof BlockNode) {
        validateNoStatementsAfterReturn((BlockNode) stmt, methodName, startToken);
      } else if (stmt instanceof StmtIfNode) {
        StmtIfNode ifNode = (StmtIfNode) stmt;
        validateNoStatementsAfterReturn(ifNode.thenBlock, methodName, startToken);
        if (ifNode.elseBlock != null) {
          validateNoStatementsAfterReturn(ifNode.elseBlock, methodName, startToken);
        }
      } else if (stmt instanceof ForNode) {
        ForNode forNode = (ForNode) stmt;
        validateNoStatementsAfterReturn(forNode.body, methodName, startToken);
      }
    }
  }
  // ========== END DEAD CODE VALIDATION ==========

  public MethodNode parseMethod() {
    Token startToken = now();

    boolean isBuiltin = false;
    Keyword visibility = Keyword.SHARE;
    boolean isPolicyMethod = false;
    Token visibilityToken = null;

    if (is(POLICY)) {
        expect(POLICY);
        isPolicyMethod = true;

        TypeNode currentClass = getCurrentParsingClass();
        if (!nil(currentClass)) {
            visibility = currentClass.visibility;
        } else {
            visibility = Keyword.SHARE;
        }

    } else if (is(BUILTIN)) {
        expect(BUILTIN);
        isBuiltin = true;
        visibility = Keyword.SHARE;
    } else if (is(SHARE, LOCAL)) {
        visibilityToken = now();
        Token currentVisibility = consume();

        if (is(currentVisibility, SHARE)) {
            visibility = Keyword.SHARE;
        } else if (is(currentVisibility, LOCAL)) {
            visibility = Keyword.LOCAL;
        } else {
            throw error(
                "Internal parser error: isVisibilityModifier() returned true for non-visibility keyword: '"
                    + currentVisibility.getText()
                    + "'",
                visibilityToken);
        }
    }

    String methodName;
    Token nameToken = now();
    if (canBeMethod(now())) {
        methodName = now().getText();
        consume();
    } else if (is(ID)) {
        methodName = expect(ID).getText();
    } else {
        throw error(
            "Expected method name (identifier or allowed keyword)");
    }

    NamingValidator.validateMethodName(methodName, startToken);

    MethodNode method = ASTFactory.createMethod(methodName, visibility, null, nameToken);
    method.isBuiltin = isBuiltin;
    method.isPolicyMethod = isPolicyMethod;

    expect(LPAREN);

    if (isBuiltin) {
        int parenDepth = 1;
        while (!is(EOF) && parenDepth > 0) {
            Token t = now();
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
            while (consume(COMMA)) {
                method.parameters.add(parseParameter());
            }
        }
        expect(RPAREN);
    }

    // Parse slot contract if present (:: syntax)
    if (isSlotDeclaration()) {
        method.returnSlots = slotParser.parseSlotContract();
    } else {
        method.returnSlots = new ArrayList<SlotNode>();
    }

    if (isBuiltin) {
        while (getPosition() < tokens.size()) {
            Token current = now();

            if (is(current, RBRACE)
                || is(current, SHARE, LOCAL, BUILTIN, POLICY)) {
                break;
            }

            consume();
        }

        if (is(TILDE_ARROW, LBRACE)) {
            Token current = now();
            throw error(
                "Builtin method '"
                    + methodName
                    + "' cannot have a body. "
                    + "Builtin methods are only declarations, not implementations.\n"
                    + "Remove '~>' or '{...}' after builtin method signature.",
                current);
        }

        return method;
    }

    // Parse method body
    if (is(TILDE_ARROW)) {
        Token tildeArrowToken = now();
        expect(TILDE_ARROW);

        List<SlotAssignmentNode> slotAssignments = slotParser.parseSlotAssignments();

        if (slotAssignments.size() == 1) {
            method.body.add(slotAssignments.get(0));
        } else {
            MultipleSlotAssignmentNode multiAssign =
                ASTFactory.createMultipleSlotAsmt(slotAssignments, tildeArrowToken);
            method.body.add(multiAssign);
        }

    } else if (is(LBRACE)) {
        expect(LBRACE);
        while (!is(RBRACE)) {
            method.body.add(statementParser.parseStmt());
        }
        expect(RBRACE);
        
 // Validate dead code after ~> if method has return contract
if (method.returnSlots != null && !method.returnSlots.isEmpty()) {
    validateNoStatementsAfterReturn(new BlockNode(method.body), methodName, startToken);
}
        
        // Validate that if there's a return contract, the body has ~> assignments
        if (method.returnSlots != null && !method.returnSlots.isEmpty()) {
            boolean hasSlotAssignments = false;
            for (StmtNode stmt : method.body) {
                if (hasSlotAssignmentsInBody(stmt)) {
                    hasSlotAssignments = true;
                    break;
                }
            }
            if (!hasSlotAssignments) {
                String context = "";
                if (currentParsingClass != null) {
                    context = "class '" + currentParsingClass.name + "' ";
                } else if (method.associatedClass != null && !method.associatedClass.isEmpty()) {
                    context = "class '" + method.associatedClass + "' ";
                }
                
                throw error(
                    "Method '" + methodName + "' of " + context + "has return contract (::) but no ~> assignments in body.\n"
                    + "Use '~> slot: value' or '~> value' to return values.",
                    startToken);
            }
        }
    } else {
        Token current = now();
        throw error(
            "Expected '~>' or '{' after method signature, but found "
                + getTypeName(current.type)
                + " ('"
                + current.getText()
                + "')",
            current);
    }

    return method;
}

  public List<SlotNode> parseSlotContractList() {
    return slotParser.parseSlotContract();
  }

  public FieldNode parseField() {
    Token startToken = now();

    Keyword visibility = null;
    Token visibilityToken = null;
    if (is(SHARE, LOCAL)) {
      visibilityToken = consume();
      if (is(visibilityToken, SHARE)) {
        visibility = SHARE;
      } else if (is(visibilityToken, LOCAL)) {
        visibility = LOCAL;
      }
    }

    Token fieldNameToken = now();
    String fieldName = expect(ID).getText();

    if (fieldName.equals("_")) {
      throw error(
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
       field.visibility = visibility;
    }

    if (consume(ASSIGN)) {
      field.value = statementParser.expressionParser.parseExpr();
    }

    return field;
  }

  public ParamNode parseParameter() {
    Token startToken = now();
    String name = expect(ID).getText();

    if (is(DOUBLE_COLON_ASSIGN)) {
      expect(DOUBLE_COLON_ASSIGN);

      ExprNode defaultValue = statementParser.expressionParser.parsePrimaryExpr();

      if (!isSimpleLiteral(defaultValue)) {
        throw error(
            "Parameter inference (:=) can only be used with literals. "
                + "Use explicit typing for expressions: "
                + name
                + ": Type = expression",
            startToken);
      }

      String inferredType = inferTypeFromLiteral(defaultValue);
      if (inferredType == null) {
        throw error(
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
    if (consume(ASSIGN)) {
      defaultValue = statementParser.expressionParser.parseExpr();
    }

    NamingValidator.validateParameterName(name, startToken);
    ParamNode param = ASTFactory.createParam(name, type, defaultValue, false, startToken);
    if (defaultValue != null) {
      param.hasDefaultValue = true;
    }
    return param;
  }

  private boolean isSimpleLiteral(ExprNode expr) {
    if (expr == null) return false;
    
    if (expr instanceof IntLiteralNode ||
        expr instanceof FloatLiteralNode ||
        expr instanceof BoolLiteralNode ||
        expr instanceof NoneLiteralNode) {
      return true;
    }
    
    if (expr instanceof TextLiteralNode) {
      return true;
    }

    if (expr instanceof ArrayNode) {
      ArrayNode arr = (ArrayNode) expr;

      if (arr.elements.size() == 1 && arr.elements.get(0) instanceof RangeNode) {
        RangeNode range = (RangeNode) arr.elements.get(0);
        return isSimpleLiteral(range.start)
            && isSimpleLiteral(range.end)
            && (nil(range.step) || isSimpleLiteral(range.step));
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
          && (nil(range.step) || isSimpleLiteral(range.step));
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
    if (nil(expr)) return null;

    if (expr instanceof IntLiteralNode) return INT.toString();
    if (expr instanceof FloatLiteralNode) return FLOAT.toString();
    if (expr instanceof BoolLiteralNode) return BOOL.toString();
    if (expr instanceof TextLiteralNode) return TEXT.toString();
    if (expr instanceof NoneLiteralNode) return null;

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
    return is(DOUBLE_COLON);
  }

  private boolean isMethodDeclaration() {
    return next(
        new ParserAction<Boolean>() {
          @Override
          public Boolean parse() throws ParseError {
            int offset = 0;

            while (wsComments(offset)) offset++;

            Token first = next(offset);
            if (nil(first)) return false;

            if (is(first, SHARE, LOCAL, BUILTIN, POLICY)) {
              offset++;
              while (wsComments(offset)) offset++;
            }

            Token nameToken = next(offset);
            
            boolean isValidName = is(nameToken, ID) || canBeMethod(nameToken);

            if (!isValidName) return false;

            offset++;
            while (wsComments(offset)) offset++;

            Token parenToken = next(offset);
            return is(parenToken, LPAREN);
          }
        });
  }

  private boolean isFieldDeclaration() {
    return next(
        new ParserAction<Boolean>() {
          @Override
          public Boolean parse() throws ParseError {
            if (is(SHARE, LOCAL)) {
              consume();
            }

            if (!is(ID)) {
                return false;
            }
            expect(ID);

            if (!is(COLON)) {
                return false;
            }
            expect(COLON);

            if (!isTypeStart(now())) {
                return false;
            }

            String type = parseTypeReference();
            if (nil(type)) {
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