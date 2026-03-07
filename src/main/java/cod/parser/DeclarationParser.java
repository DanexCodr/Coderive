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

  private int currentParsingClassId = cod.ast.FlatAST.NULL;
  private Map<String, Integer> availablePolicies = new HashMap<String, Integer>();

  public DeclarationParser(
      ParserContext ctx, StatementParser statementParser, ImportResolver importResolver) {
    super(ctx, statementParser.getFactory());
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

  private void setCurrentParsingClassId(int typeId) {
    currentParsingClassId = typeId;
  }

  private int getCurrentParsingClassId() {
    return currentParsingClassId;
  }

  private PolicyNode findPolicy(String policyName) {
    // TODO: migrate to FlatAST API
    return null;
  }

  private TypeNode findClassByName(String className, ProgramNode currentProgram) {
    // TODO: migrate to FlatAST API
    return null;
  }

  private List<PolicyMethodNode> getAllPolicyMethods(PolicyNode policy) {
    // TODO: migrate to FlatAST API
    return new java.util.ArrayList<PolicyMethodNode>();
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
        if (composed != null) {
          collectPolicyMethodsViaComposition(composed, allMethods, visited);
        }
      }
    }

    if (policy.methods != null) {
      allMethods.addAll(policy.methods);
    }
  }

  private List<String> getAllAffectingPolicies(TypeNode currentClass, ProgramNode currentProgram) {
    // TODO: migrate to FlatAST API
    return new java.util.ArrayList<String>();
  }

  private void collectAffectingPoliciesRecursive(
      int type, List<String> allPolicies, Set<String> visited, ProgramNode currentProgram) {
    // TODO: migrate to FlatAST API
    return;
  }

  private List<String> getAncestorPolicies(TypeNode currentClass, ProgramNode currentProgram) {
    // TODO: migrate to FlatAST API
    return new java.util.ArrayList<String>();
  }

  private String getRequiringAncestorWithPolicy(String methodName, TypeNode currentClass, 
                                               ProgramNode currentProgram) {
    // TODO: migrate to FlatAST API
    return null;
  }

  private boolean isMethodRequiredFromAncestorPolicy(
      String methodName, int currentClass, ProgramNode currentProgram) {
    // TODO: migrate to FlatAST API
    return false;
  }

  private Set<String> getAllMethodsRequiredByAncestors(
      int currentClass, ProgramNode currentProgram) {
    // TODO: migrate to FlatAST API
    return new java.util.HashSet<String>();
  }

  private void validatePolicyComposition(PolicyNode policy) {
    // TODO: migrate to FlatAST API
    return;
  }

  private void checkForCompositionCycle(
      String currentPolicy, String composedName, Set<String> visited) {
    // TODO: migrate to FlatAST API
    return;
  }

  private boolean areTypesCompatible(String implType, String policyType) {
    // TODO: migrate to FlatAST API
    return true;
  }

  private void validatePolicyMethod(MethodNode method, ProgramNode currentProgram) {
    // TODO: migrate to FlatAST API
    return;
  }

  public void validateClassViralPolicies(int type, ProgramNode currentProgram) {
    // TODO: migrate to FlatAST API
    return;
  }

  public void validateAllPolicyMethods(int type, ProgramNode currentProgram) {
    // TODO: migrate to FlatAST API
    return;
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

  public int parseConstructor() {
    Token startToken = now();
    Token thisToken = null;

    if (is(SHARE, LOCAL)) {
      consume();
    }

    thisToken = now();
    Token current = consume();
    if (!is(current, THIS)) {
      throw error(
          "Constructor must be named 'this', found: " + current.text, startToken);
    }

    int constructorId = factory.createConstructor(new java.util.ArrayList<Integer>(), new java.util.ArrayList<Integer>(), thisToken);

    expect(LPAREN);
    if (!is(RPAREN)) {
      factory.getAST().constructorAddParam(constructorId, parseParameter());
      while (consume(COMMA)) {
        factory.getAST().constructorAddParam(constructorId, parseParameter());
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
      factory.getAST().constructorAddBodyStmt(constructorId, parseSuperConstructorCall());
    }

    if (is(LBRACE)) {
      expect(LBRACE);
      while (!is(RBRACE)) {
        factory.getAST().constructorAddBodyStmt(constructorId, statementParser.parseStmt());
      }
      expect(RBRACE);
    } else if (!hasSuperCall) {
      throw error(
          "Constructor must have a body: this(...) { ... } or this(...) super(...) { ... }");
    }

    return constructorId;
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

  private int parseSuperConstructorCall() {
    Token superToken = now();
    expect(SUPER);

    String zuper = SUPER.toString();
    int superCall = factory.createMethodCall(zuper, zuper, superToken);
    factory.getAST().methodCallSetIsSuper(superCall, true);
    expect(LPAREN);

    if (!is(RPAREN)) {
      if (isNamedArgument()) {
        do {
            String argName = expect(ID).text;
            expect(COLON);
            int argValue = statementParser.expressionParser.parseExpr();
            factory.getAST().methodCallAddArg(superCall, argValue, argName);
            if (!is(COMMA)) break;
            expect(COMMA);
        } while (!is(RPAREN));
      } else {
        factory.getAST().methodCallAddArg(superCall, statementParser.expressionParser.parseExpr(), null);
        while (consume(COMMA)) {
          factory.getAST().methodCallAddArg(superCall, statementParser.expressionParser.parseExpr(), null);
        }
      }
    }

    expect(RPAREN);

    return superCall;
  }

  private static int[] toIntArray(List<Integer> list) {
    int[] arr = new int[list.size()];
    for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
    return arr;
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

  private void parseNamedArgumentList(List<Integer> args, List<String> argNames) {
    do {
      String argName = expect(ID).text;
      expect(COLON);
      int value = statementParser.expressionParser.parseExpr();

      args.add(value);
      argNames.add(argName);

      if (!is(COMMA)) {
        break;
      }
      expect(COMMA);
    } while (!is(RPAREN));
  }

  public int parseType() {
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
                    + currentVisibility.text
                    + "'",
                visibilityToken);
        }
    }

    Token typeNameToken = now();
    String typeName = expect(ID).text;

    // Check if this is actually a method declaration (has parentheses after name)
    if (is(LPAREN)) {
        // This is a method, not a class - restore and let parseMethod handle it
        restore();
        return cod.ast.FlatAST.NULL; // Signal that this isn't a type
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

    int typeId = factory.createType(typeName, visibility, extendName, typeNameToken);
    for (String impl : implementedPolicies) {
      factory.getAST().typeAddImplementedPolicy(typeId, impl);
    }
    // type.policyTokens, extendToken, parentToken not yet in FlatAST - TODO

    setCurrentParsingClassId(typeId);

    expect(LBRACE);
    while (!is(RBRACE)) {
        if (isFieldDeclaration()) {
            factory.getAST().typeAddField(typeId, parseField());
        } else if (isConstructorDeclaration()) {
            int constructor = parseConstructor();
            factory.getAST().typeAddConstructor(typeId, constructor);
        } else if (isMethodDeclaration()) {
            int method = parseMethod();
            factory.getAST().methodSetAssociatedClass(method, factory.getAST().typeName(typeId));
            factory.getAST().typeAddMethod(typeId, method);
        } else {
            factory.getAST().typeAddStatement(typeId, statementParser.parseStmt());
        }
    }

    setCurrentParsingClassId(cod.ast.FlatAST.NULL);

    expect(RBRACE);
    return typeId;
}

  public int parsePolicy() {
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
    String policyName = expect(ID).text;

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

    int policyId = factory.createPolicy(policyName, visibility, nameToken);
    factory.getAST().policySetComposed(policyId, composedPolicies.toArray(new String[composedPolicies.size()]));

    if (!is(LBRACE)) {
      throw error("Expected '{' after policy name");
    }
    expect(LBRACE);

    while (!is(RBRACE)) {
      
      if (isPolicyMethodDeclarationStart()) {
        int method = parsePolicyMethod();
        factory.getAST().policyAddMethod(policyId, method);
      } else if (!is(RBRACE)) {
        Token current = now();
        throw error(
            "Policy can only contain method declarations and cannot have a body, found: " + current.text,
            current);
      }
    }

    expect(RBRACE);

    availablePolicies.put(policyName, policyId);

    // TODO: re-enable with FlatAST API: validatePolicyComposition(...)

    return policyId;
  }

  public int parsePolicyMethod() {
    Token methodNameToken = now();
    String methodName;

    if (canBeMethod(now())) {
      methodName = consume().text;
    } else if (is(ID)) {
      methodName = expect(ID).text;
    } else {
      throw error("Expected method name in policy declaration");
    }

    if (methodName.length() > 0 && !Character.isLowerCase(methodName.charAt(0))) {
      throw error(
          "Method name '" + methodName + "' must start with a lowercase letter",
          methodNameToken);
    }

    int policyMethodId = factory.createPolicyMethod(methodName, methodNameToken);
    if (!is(LPAREN)) {
      throw error("Expected '(' after method name");
    }
    expect(LPAREN);

    if (!is(RPAREN)) {
      factory.getAST().policyMethodAddParam(policyMethodId, parseParameter());
      while (consume(COMMA)) {
        factory.getAST().policyMethodAddParam(policyMethodId, parseParameter());
      }
    }

    if (!is(RPAREN)) {
      throw error("Expected ')' after parameters");
    }
    expect(RPAREN);

    if (isSlotDeclaration()) {
      List<Integer> pmSlots = slotParser.parseSlotContract();
      for (int slot : toIntArray(pmSlots)) {
        factory.getAST().policyMethodAddReturnSlot(policyMethodId, slot);
      }
    }

    return policyMethodId;
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

  public int parseMethod() {
    Token startToken = now();

    boolean isBuiltin = false;
    Keyword visibility = Keyword.SHARE;
    boolean isPolicyMethod = false;
    Token visibilityToken = null;

    if (is(POLICY)) {
      expect(POLICY);
      isPolicyMethod = true;

      int currentClass = getCurrentParsingClassId();
      if (currentClass != cod.ast.FlatAST.NULL) {
        visibility = factory.getAST().typeVisibility(currentClass);
      } else {
        visibility = Keyword.SHARE;
      }

    } else if (is(BUILTIN)) {
      expect(BUILTIN);
      isBuiltin = true;
      visibility = Keyword.SHARE;
    } else if (is(SHARE, LOCAL)) {
      visibilityToken = now();
      Token currentVisibility= consume();

      if (is(currentVisibility, SHARE)) {
        visibility = Keyword.SHARE;
      } else if (is(currentVisibility, LOCAL)) {
        visibility = Keyword.LOCAL;
      } else {
        throw error(
            "Internal parser error: isVisibilityModifier() returned true for non-visibility keyword: '"
                + currentVisibility.text
                + "'",
            visibilityToken);
      }
    }

    String methodName;
    Token nameToken = now();
    if (canBeMethod(now())) {
      methodName = now().text;
      consume();
    } else if (is(ID)) {
      methodName = expect(ID).text;
    } else {
      throw error(
          "Expected method name (identifier or allowed keyword)");
    }

    NamingValidator.validateMethodName(methodName, startToken);

    int methodId = factory.createMethod(methodName, visibility, new java.util.ArrayList<Integer>(), nameToken);
    factory.getAST().methodSetIsBuiltin(methodId, isBuiltin);
    factory.getAST().methodSetIsPolicyMethod(methodId, isPolicyMethod);

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
        factory.getAST().methodAddParam(methodId, parseParameter());
        while (consume(COMMA)) {
          factory.getAST().methodAddParam(methodId, parseParameter());
        }
      }
      expect(RPAREN);
    }

    if (isSlotDeclaration()) {
      List<Integer> retSlots = slotParser.parseSlotContract();
      factory.getAST().methodSetReturnSlots(methodId, toIntArray(retSlots));
    } else {
      factory.getAST().methodSetReturnSlots(methodId, new int[0]);
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

      return methodId;
    }

    if (is(TILDE_ARROW)) {
      Token tildeArrowToken = now();
      expect(TILDE_ARROW);

      
      List<Integer> slotAssignments = slotParser.parseSlotAssignments();

      if (slotAssignments.size() == 1) {
        factory.getAST().methodAddBodyStmt(methodId, slotAssignments.get(0));
      } else {
        int multiAssignId = factory.createMultipleSlotAsmt(slotAssignments, tildeArrowToken);
        factory.getAST().methodAddBodyStmt(methodId, multiAssignId);
      }

    } else if (is(LBRACE)) {
      expect(LBRACE);
      while (!is(RBRACE)) {
        factory.getAST().methodAddBodyStmt(methodId, statementParser.parseStmt());
      }
      expect(RBRACE);
    } else {
      Token current = now();
      throw error(
          "Expected '~>' or '{' after method signature, but found "
              + getTypeName(current.type)
              + " ('"
              + current.text
              + "')",
          current);
    }

    return methodId;
  }

  public List<Integer> parseSlotContractList() {
    return slotParser.parseSlotContract();
  }

  public int parseField() {
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
    String fieldName = expect(ID).text;

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

    int fieldId = factory.createField(fieldName, fieldType, fieldNameToken);
    if (visibility != null) {
       factory.getAST().fieldSetVisibility(fieldId, visibility);
    }

    if (consume(ASSIGN)) {
      factory.getAST().fieldSetValue(fieldId, statementParser.expressionParser.parseExpr());
    }

    return fieldId;
  }

  public int parseParameter() {
    Token startToken = now();
    String name = expect(ID).text;

    if (is(DOUBLE_COLON_ASSIGN)) {
      expect(DOUBLE_COLON_ASSIGN);

      int defaultValue = statementParser.expressionParser.parsePrimaryExpr();

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

      int paramId = factory.createParam(name, inferredType, defaultValue, true, startToken);
      return paramId;
    }

    expect(COLON);
    String type = parseTypeReference();

    int defaultValue = cod.ast.FlatAST.NULL;
    if (consume(ASSIGN)) {
      defaultValue = statementParser.expressionParser.parseExpr();
    }

    NamingValidator.validateParameterName(name, startToken);
    int paramId = factory.createParam(name, type, defaultValue, false, startToken);
    if (defaultValue != cod.ast.FlatAST.NULL) {
    }
    return paramId;
  }

  private boolean isSimpleLiteral(int expr)  {
    // TODO: migrate to FlatAST API
    return true;
  }

  private String inferTypeFromLiteral(int expr)  {
    // TODO: migrate to FlatAST API
    if (expr == cod.ast.FlatAST.NULL) return null;
    cod.ast.NodeKind kind = factory.getAST().kind(expr);
    if (kind == cod.ast.NodeKind.INT_LITERAL) return "int";
    if (kind == cod.ast.NodeKind.FLOAT_LITERAL) return "float";
    if (kind == cod.ast.NodeKind.BOOL_LITERAL) return "bool";
    if (kind == cod.ast.NodeKind.TEXT_LITERAL) return "text";
    if (kind == cod.ast.NodeKind.ARRAY) return "[]";
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