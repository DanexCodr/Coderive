package cod.parser;

import cod.ast.ASTFactory;
import cod.ast.node.*;
import cod.error.ParseError;
import cod.parser.context.*;
import cod.semantic.ImportResolver;
import cod.semantic.NamingValidator;
import cod.semantic.PolicyValidator;
import cod.semantic.ReturnContractValidator;
import cod.syntax.Keyword;
import java.util.*;

import cod.lexer.Token;
import static cod.lexer.TokenType.*;
import static cod.syntax.Keyword.*;
import static cod.syntax.Symbol.*;

public class DeclarationParser extends BaseParser {

  private final StatementParser statementParser;
  private final SlotParser slotParser;
  private final PolicyValidator policyValidator;

  private Type currentParsingClass = null;
  public DeclarationParser(
      ParserContext ctx, StatementParser statementParser, ImportResolver importResolver) {
    super(ctx);
    this.statementParser = statementParser;
    this.slotParser = new SlotParser(this);
    this.policyValidator = new PolicyValidator(importResolver);
  }

  @Override
  protected BaseParser createIsolatedParser(ParserContext isolatedCtx) {
    return new DeclarationParser(isolatedCtx, this.statementParser, this.policyValidator.getImportResolver());
  }
  
  public StatementParser getStatementParser() {
    return statementParser;
  }

  private void setCurrentParsingClass(Type type) {
    currentParsingClass = type;
  }

  private Type getCurrentParsingClass() {
    return currentParsingClass;
  }


  public void validateClassViralPolicies(Type type, Program currentProgram) {
    policyValidator.validateClassViralPolicies(type, currentProgram);
  }

  public void validateAllPolicyMethods(Type type, Program currentProgram) {
    policyValidator.validateAllPolicyMethods(type, currentProgram);
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

  public Constructor parseConstructor() {
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

    Constructor constructor = ASTFactory.createConstructor(null, null, thisToken);

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

  private MethodCall parseSuperConstructorCall() {
    Token superToken = now();
    expect(SUPER);

    String zuper = SUPER.toString();
    MethodCall superCall = ASTFactory.createMethodCall(zuper, zuper, superToken);
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

  private void parseNamedArgumentList(List<Expr> args, List<String> argNames) {
    do {
      String argName = expect(ID).getText();
      expect(COLON);
      Expr value = statementParser.expressionParser.parseExpr();

      args.add(value);
      argNames.add(argName);

      if (!is(COMMA)) {
        break;
      }
      expect(COMMA);
    } while (!is(RPAREN));
  }

  public Type parseType() {
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

    Type type = ASTFactory.createType(typeName, visibility, extendName, typeNameToken);
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
            Constructor constructor = parseConstructor();
            type.constructors.add(constructor);
        } else if (isMethodDeclaration()) {
            Method method = parseMethod();
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

  public Policy parsePolicy() {
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

    NamingValidator.validatePolicyName(policyName, nameToken);

    List<String> composedPolicies = new ArrayList<String>();
    if (is(WITH)) {
      expect(WITH);
      composedPolicies.add(parseQualifiedName());

      while (consume(COMMA)) {
        composedPolicies.add(parseQualifiedName());
      }
    }

    Policy policy = ASTFactory.createPolicy(policyName, visibility, nameToken);
    policy.composedPolicies = composedPolicies;

    if (!is(LBRACE)) {
      throw error("Expected '{' after policy name");
    }
    expect(LBRACE);

    while (!is(RBRACE)) {
      if (isPolicyMethodDeclarationStart()) {
        PolicyMethod method = parsePolicyMethod();
        policy.methods.add(method);
      } else if (!is(RBRACE)) {
        Token current = now();
        throw error(
            "Policy can only contain method declarations and cannot have a body, found: " + current.getText(),
            current);
      }
    }

    expect(RBRACE);

    policyValidator.registerLocalPolicy(policy);
    policyValidator.validatePolicyComposition(policy, nameToken);

    return policy;
  }

  public PolicyMethod parsePolicyMethod() {
    Token methodNameToken = now();
    String methodName;

    if (canBeMethod(now())) {
      methodName = consume().getText();
    } else if (is(ID)) {
      methodName = expect(ID).getText();
    } else {
      throw error("Expected method name in policy declaration");
    }

    NamingValidator.validatePolicyMethodName(methodName, methodNameToken);

    PolicyMethod method = ASTFactory.createPolicyMethod(methodName, methodNameToken);
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

  public Method parseMethod() {
    Token startToken = now();

    boolean isBuiltin = false;
    Keyword visibility = Keyword.SHARE;
    boolean isPolicyMethod = false;
    Token visibilityToken = null;

    if (is(POLICY)) {
        expect(POLICY);
        isPolicyMethod = true;

        Type currentClass = getCurrentParsingClass();
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

    Method method = ASTFactory.createMethod(methodName, visibility, null, nameToken);
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
        method.returnSlots = new ArrayList<Slot>();
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

        List<SlotAssignment> slotAssignments = slotParser.parseSlotAssignments();

        if (slotAssignments.size() == 1) {
            method.body.add(slotAssignments.get(0));
        } else {
            MultipleSlotAssignment multiAssign =
                ASTFactory.createMultipleSlotAsmt(slotAssignments, tildeArrowToken);
            method.body.add(multiAssign);
        }

    } else if (is(LBRACE)) {
        expect(LBRACE);
        while (!is(RBRACE)) {
            method.body.add(statementParser.parseStmt());
        }
        expect(RBRACE);
        
        ReturnContractValidator.validateMethodReturnContract(method, currentParsingClass, startToken);
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

  public List<Slot> parseSlotContractList() {
    return slotParser.parseSlotContract();
  }

  public Field parseField() {
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

    expect(COLON);

    String fieldType = parseTypeReference();

    NamingValidator.validateFieldName(fieldName, startToken);

    Field field = ASTFactory.createField(fieldName, fieldType, fieldNameToken);
    if (visibility != null) {
       field.visibility = visibility;
    }

    if (consume(ASSIGN)) {
      field.value = statementParser.expressionParser.parseExpr();
    }

    return field;
  }

  public Param parseParameter() {
    Token startToken = now();
    String name = expect(ID).getText();

    if (is(DOUBLE_COLON_ASSIGN)) {
      expect(DOUBLE_COLON_ASSIGN);

      Expr defaultValue = statementParser.expressionParser.parsePrimaryExpr();

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

      Param param = ASTFactory.createParam(name, inferredType, defaultValue, true, startToken);
      param.hasDefaultValue = true;
      return param;
    }

    expect(COLON);
    String type = parseTypeReference();

    Expr defaultValue = null;
    if (consume(ASSIGN)) {
      defaultValue = statementParser.expressionParser.parseExpr();
    }

    NamingValidator.validateParameterName(name, startToken);
    Param param = ASTFactory.createParam(name, type, defaultValue, false, startToken);
    if (defaultValue != null) {
      param.hasDefaultValue = true;
    }
    return param;
  }

  private boolean isSimpleLiteral(Expr expr) {
    if (expr == null) return false;
    
    if (expr instanceof IntLiteral ||
        expr instanceof FloatLiteral ||
        expr instanceof BoolLiteral ||
        expr instanceof NoneLiteral) {
      return true;
    }
    
    if (expr instanceof TextLiteral) {
      return true;
    }

    if (expr instanceof Array) {
      Array arr = (Array) expr;

      if (arr.elements.size() == 1 && arr.elements.get(0) instanceof Range) {
        Range range = (Range) arr.elements.get(0);
        return isSimpleLiteral(range.start)
            && isSimpleLiteral(range.end)
            && (nil(range.step) || isSimpleLiteral(range.step));
      }

      for (Expr elem : arr.elements) {
        if (!isSimpleLiteral(elem)) return false;
      }
      return true;
    }

    if (expr instanceof Range) {
      Range range = (Range) expr;
      return isSimpleLiteral(range.start)
          && isSimpleLiteral(range.end)
          && (nil(range.step) || isSimpleLiteral(range.step));
    }

    if (expr instanceof Tuple) {
      Tuple tuple = (Tuple) expr;
      for (Expr elem : tuple.elements) {
        if (!isSimpleLiteral(elem)) return false;
      }
      return true;
    }

    return false;
  }

  private String inferTypeFromLiteral(Expr expr) {
    if (nil(expr)) return null;

    if (expr instanceof IntLiteral) return INT.toString();
    if (expr instanceof FloatLiteral) return FLOAT.toString();
    if (expr instanceof BoolLiteral) return BOOL.toString();
    if (expr instanceof TextLiteral) return TEXT.toString();
    if (expr instanceof NoneLiteral) return null;

    if (expr instanceof Array) {
      Array arr = (Array) expr;
      if (arr.elements.isEmpty()) return null;

      if (arr.elements.size() == 1 && arr.elements.get(0) instanceof Range) {
        return "[]";
      }

      String elementType = inferTypeFromLiteral(arr.elements.get(0));
      if (elementType != null) {
        return "[" + elementType + "]";
      }
      return null;
    }

    if (expr instanceof Range) {
      return "[]";
    }

    if (expr instanceof Tuple) {
      Tuple tuple = (Tuple) expr;
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
