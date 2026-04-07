package cod.parser;

import cod.ast.ASTFactory;
import cod.semantic.NamingValidator;
import cod.error.ParseError;
import cod.ast.node.*;

import java.util.ArrayList;
import java.util.List;

import cod.lexer.Token;
import static cod.lexer.TokenType.*;
import cod.parser.context.*;

import static cod.syntax.Symbol.*;
import static cod.syntax.Keyword.*;

public class StatementParser extends BaseParser {

  public final ExpressionParser expressionParser;
  private final SlotParser slotParser;

  public StatementParser(ParserContext ctx, ExpressionParser expressionParser) {
    super(ctx);
    this.expressionParser = expressionParser;
    this.slotParser = new SlotParser(this);
  }

  @Override
  protected BaseParser createIsolatedParser(ParserContext isolatedCtx) {
    ExpressionParser isolatedExprParser = new ExpressionParser(
        isolatedCtx, 
        this.expressionParser.getGlobalRegistry(),
        null
    );
    StatementParser isolatedStmtParser = new StatementParser(isolatedCtx, isolatedExprParser);
    isolatedExprParser.setStatementParser(isolatedStmtParser);
    return isolatedStmtParser;
  }

  public Stmt parseStmt() {
    return attempt(
        new ParserAction<Stmt>() {
          @Override
          public Stmt parse() throws ParseError {
            return parseStmtInternal();
          }
        });
  }

  private Stmt parseStmtInternal() {
    checkIllegalDeclaration();

    Token first = now();

    if (is(first, KEYWORD)) {
      if (is(IF)) return parseIfStmt();
      if (is(FOR)) return parseForStmt();
      if (is(EXIT)) return parseExitStmt();
      if (is(SKIP)) return parseSkipStmt();
      if (is(BREAK)) return parseBreakStmt();
      if (is(SUPER)) return parseSimpleAssignment();
    }

    if (is(first, TRUE, FALSE)) {
      Token second = next();
      if (is(second, LBRACE)) {
        throw error(
            "Missing 'if' keyword. Use 'if "
                + first.getText()
                + " { ... }' instead of '"
                + first.getText()
                + " { ... }'",
            first);
      }
    }

    if (is(first, ID)) {
      Token second = next();
      
      // Index assignment FIRST
      if (is(first, ID) && is(second, LBRACKET)) {
        if (isIndexAssignment()) return parseIndexAssignment();
      }

      // Variable declaration
      if (isVariableDeclaration()) {
        return parseVariableDeclaration();
      }

      // Simple assignment
      if (is(first, ID) && is(second, ASSIGN)) {
        if (isReturnSlotAssignment()) return parseReturnSlotAssignment();
        return parseSimpleAssignment();
      }

      // Return slot assignment with commas
      if (is(first, ID) && is(second, COMMA)) {
        if (isReturnSlotAssignment()) return parseReturnSlotAssignment();
      }

      // Method call
      if (isMethodCallStmt()) return parseMethodCallStmt();
      
      // Error case for ID followed by LBRACE
      if (is(second, LBRACE)) {
        String varName = first.getText();
        if (!varName.equals("_") && Character.isLowerCase(varName.charAt(0))) {
          Token third = next(2);
          if (!is(third, RBRACE)) {
            throw error(
                "Unexpected block after variable '"
                    + varName
                    + "'. "
                    + "Did you mean 'if "
                    + varName
                    + " { ... }'?",
                first);
          }
        }
      }
    }

    if (is(first, TILDE_ARROW)) return parseSlotAssignment();

    if (is(first, LBRACKET) && isMethodCallStmt()) {
      return parseMethodCallStmt();
    }

    return parseExprStmt();
  }

  private void checkIllegalDeclaration() {
    Token current = now();
    if (current.type != KEYWORD) return;

    if (is(current, IF, ELSE, ELIF, FOR, EXIT, SKIP, BREAK)) return;

    if (isTypeStart(current)) return;

    Token idToken = next();
    if (!is(idToken, ID)) return;

    Token afterId = next(2);
    if (is(afterId, ASSIGN) || (is(afterId, LBRACKET) && is(next(3), RBRACKET))) {
      throw error(
          "Illegally used reserved keyword '"
              + current.getText()
              + "' for declaration of variable '"
              + idToken.getText()
              + "'",
          current,
          idToken);
    }
  }

  private Stmt parseSkipStmt() {
    Token skipToken = expect(SKIP);

    Token nextToken = now();

    if (!(any(is(nextToken, EOF), is(nextToken, RBRACE)) || isStmtStart(nextToken))) {
      throw error(
          "Nothing can follow 'skip' in the same statement. "
              + "'skip' must be the complete statement.",
          skipToken);
    }

    return ASTFactory.createSkipStmt(skipToken);
  }

  private Stmt parseBreakStmt() {
    Token breakToken = expect(BREAK);

    Token nextToken = now();

    if (!(any(is(nextToken, EOF), is(nextToken, RBRACE)) || isStmtStart(nextToken))) {
      throw error(
          "Nothing can follow 'break' in the same statement. "
              + "'break' must be the complete statement.",
          breakToken);
    }

    return ASTFactory.createBreakStmt(breakToken);
  }
  
  private boolean isStmtStart(Token token) {
    if (nil(token)) return false;

    if (is(token, ID)) return is(next(), COLON, ASSIGN, DOUBLE_COLON_ASSIGN, LBRACKET);

    if (is(token, TILDE_ARROW)) return true;

    if (is(token, LBRACKET)) return isMethodCallStmt();

    return is(token, IF, FOR, EXIT, ELSE, ELIF, SKIP, BREAK, SHARE, LOCAL);
  }

  private Stmt parseSlotAssignment() {
    Token tildeArrowToken = expect(TILDE_ARROW);
    return slotParser.parseSlotAssignmentsAsStmt(tildeArrowToken);
  }

  private Stmt parseSimpleAssignment() {
    Token startToken = now();

    String idName = null;
    if (isThisExpr()) {
        Expr target = parseThisExpr();
        Token assignToken = expect(ASSIGN);
        Expr value = expressionParser.parseExpr();
        Assignment assignment = ASTFactory.createAsmt(target, value, false, assignToken);
        return assignment;
    } else if (is(SUPER)) {
        Token superToken = expect(SUPER);
        Token dotToken = expect(DOT);
        Token fieldNameToken = now();
        String fieldName = expect(ID).getText();
        
        Expr fieldNode = ASTFactory.createIdentifier(fieldName, fieldNameToken);
        
        Expr target = ASTFactory.createPropertyAccess(
            ASTFactory.createSuperExpr(superToken), 
            fieldNode, 
            dotToken
        );

        Token assignToken = expect(ASSIGN);
        Expr value = expressionParser.parseExpr();
        Assignment assignment = ASTFactory.createAsmt(target, value, false, assignToken);
        return assignment;
    } else {
        idName = expect(ID).getText();
    }

    if ("_".equals(idName)) {
        throw error(
            "Cannot assign to '_'. Underscore is reserved for discard/placeholder.", startToken);
    }

    Expr target = ASTFactory.createIdentifier(idName, startToken);
    Token assignToken = expect(ASSIGN);
    Expr value = expressionParser.parseExpr();

    Assignment assignment = ASTFactory.createAsmt(target, value, false, assignToken);
    return assignment;
  }

  private boolean isThisExpr() {
    return attempt(
        new ParserAction<Boolean>() {
          @Override
          public Boolean parse() throws ParseError {
            Token current = now();
            if (is(current, THIS)) {
              return true;
            } else if (is(current, ID)) {
              if (is(next(), DOT)) {
                Token afterDot = next(2);
                if (is(afterDot, THIS)) return true;
              }
            }
            return false;
          }
        });
  }

  private Expr parseThisExpr() {
    Token first = now();
    if (is(first, THIS)) {
      Token thisToken = expect(THIS);
      return ASTFactory.createThisExpr(null, thisToken);
    } else if (is(first, ID)) {
      Token classNameToken = first;
      String className = expect(ID).getText();
      if (consume(DOT)) {
        Token thisToken = expect(THIS);
        return ASTFactory.createThisExpr(className, thisToken);
      } else {
        restore();
        return ASTFactory.createIdentifier(className, classNameToken);
      }
    }
    throw error("Expected 'this' or 'ClassName.this'", first);
  }

  private Stmt parseVariableDeclaration() {
    Token startToken = now();
    String typeName = null;
    boolean isImplicit = false;
    String varName = null;
    Expr value = null;

    if (is(ID)) {
      varName = expect(ID).getText();

      if (is(DOUBLE_COLON_ASSIGN)) {
        NamingValidator.validateVariableDeclarationName(varName, startToken);
        isImplicit = true;
        expect(DOUBLE_COLON_ASSIGN);
        value = expressionParser.parseExpr();

      } else if (is(COLON)) {
        NamingValidator.validateVariableDeclarationName(varName, startToken);

        expect(COLON);

        if (isTypeStart(now())) {
          typeName = parseTypeReference();

          if (consume(ASSIGN)) {
            value = expressionParser.parseExpr();
          }
        } else {
          throw error(
              "Expected type after ':' in variable declaration. "
                  + "For inferred typing use ':=' operator",
              startToken);
        }
      } else {
        throw error(
            "Expected variable declaration in format 'name: type', 'name: type = value', or 'name := value'",
            startToken);
      }
    } else {
      throw error(
          "Expected variable declaration in format 'name: type', 'name: type = value', or 'name := value'",
          startToken);
    }

    if (!nil(varName)) {
      NamingValidator.validateVariableOrConstantName(varName, startToken);
      if (NamingValidator.isAllCaps(varName) && value == null) {
        throw error("Constant '" + varName + "' must have an initial value", startToken);
      }
    }

    Var varNode = ASTFactory.createVar(varName, value, startToken);
    varNode.explicitType = isImplicit ? null : typeName;
    return varNode;
  }

  private boolean nextIsPropertyAccess() {
    save();
    try {
      if (is(ID) && is(next(), DOT) && is(next(2), THIS)) {
        return true;
      }

      if (is(ID) && is(next(), DOT) && is(next(2), ID) && is(next(3), DOT) && is(next(4), THIS)) {
        return true;
      }

      return false;
    } finally {
      restore();
    }
  }

  private Stmt parseIfStmt() {
    Token ifToken = expect(IF);

    Expr condition = expressionParser.parseExpr();

    StmtIf rootIfNode = ASTFactory.createIfStmt(condition, ifToken);

    parseControlFlowBlock(rootIfNode.thenBlock);

    StmtIf currentNode = rootIfNode;

    while (is(ELIF)) {
      Token elifToken = expect(ELIF);
      Expr elifCondition = expressionParser.parseExpr();
      StmtIf elifNode = ASTFactory.createIfStmt(elifCondition, elifToken);

      parseControlFlowBlock(elifNode.thenBlock);
      currentNode.elseBlock.statements.add(elifNode);
      currentNode = elifNode;
    }

    if (is(ELSE)) {
      expect(ELSE);

      ParserState savedState = getCurrentState();

      if (is(IF)) {
        currentNode.elseBlock.statements.add(parseIfStmt());
      } else {
        setState(savedState);

        if (is(LBRACE)) {
          expect(LBRACE);
          while (!is(RBRACE) && !is(EOF)) {
            currentNode.elseBlock.statements.add(parseStmtInternal());
          }
          expect(RBRACE);
        } else {
          Token next = now();
          if (!(is(next, EOF) || is(next, RBRACE)) && !isInControlFlow(next)) {
            currentNode.elseBlock.statements.add(parseStmtInternal());
          }
        }
      }
    }

    return rootIfNode;
  }

  private void parseControlFlowBlock(Block block) {
    if (is(LBRACE)) {
      expect(LBRACE);
      while (!is(RBRACE) && !is(EOF)) {
        block.statements.add(parseStmtInternal());
      }
      expect(RBRACE);
    } else {
      Token next = now();
      if (!(is(next, EOF) || is(next, RBRACE)) && !isInControlFlow(next)) {
        block.statements.add(parseStmtInternal());
      }
    }
  }

  private boolean isInControlFlow(Token token) {
    return is(token, IF, FOR, ELSE, ELIF, EXIT, SKIP, BREAK);
  }

  private Stmt parseForStmt() {
    Token forToken = expect(FOR);
    Token iteratorToken = now();
    String iterator = expect(ID).getText();
    if (NamingValidator.isAllCaps(iterator)) {
      throw error("Loop iterator '" + iterator + "' cannot use ALL_CAPS (reserved for constants)", iteratorToken);
    }
    
    // Expect 'of' for both array iteration and range loops
    expect(OF);
    
    // Parse the source/start expression
    Expr source = expressionParser.parseExpr();

    // Check if this is a range loop (has .. or to after the start expression)
    if (is(RANGE_DOTDOT)) {
        Token rangeToken = expect(RANGE_DOTDOT);
        Expr end = expressionParser.parseExpr();
        
        // Check for optional step after end
        Expr step = null;
        Token stepToken = null;
        if (is(RANGE_HASH)) {
            stepToken = expect(RANGE_HASH);
            step = parseStepExpr(iterator, iteratorToken);
        } else if (is(BY)) {
            stepToken = expect(BY);
            step = parseStepExpr(iterator, iteratorToken);
        }
        
        Range range = ASTFactory.createRange(step, source, end, stepToken, rangeToken);
        For forNode = ASTFactory.createFor(iterator, range, forToken, iteratorToken);
        return parseForLoopBody(forNode);
    } 
    else if (is(TO)) {
        Token rangeToken = expect(TO);
        Expr end = expressionParser.parseExpr();
        
        // Check for optional step after end
        Expr step = null;
        Token stepToken = null;
        if (is(BY)) {
            stepToken = expect(BY);
            step = parseStepExpr(iterator, iteratorToken);
        }
        
        Range range = ASTFactory.createRange(step, source, end, stepToken, rangeToken);
        For forNode = ASTFactory.createFor(iterator, range, forToken, iteratorToken);
        return parseForLoopBody(forNode);
    } 
    else {
        // This is array iteration - source is the array
        For forNode = ASTFactory.createFor(iterator, source, forToken, iteratorToken);
        return parseForLoopBody(forNode);
    }
  }

  private Expr parseStepExpr(String iterator, Token iteratorToken) {
    if (is(MUL) || is(DIV)) {
      Token operatorToken = now();
      String operator = operatorToken.getText();
      consume();
      Expr operand = expressionParser.parseExpr();
      Expr iteratorRef = ASTFactory.createIdentifier(iterator, iteratorToken);
      return ASTFactory.createBinaryOp(iteratorRef, operator, operand, operatorToken);
    } else if (is(PLUS) || is(MINUS)) {
      Token operatorToken = now();
      consume();
      if (is(INT_LIT, FLOAT_LIT)) {
        Expr operand = expressionParser.parsePrimaryExpr();
        if (is(operatorToken, PLUS)) {
          return operand;
        }
        return ASTFactory.createUnaryOp("-", operand, operatorToken);
      } else {
        return expressionParser.parseExpr();
      }
    } else {
      return expressionParser.parseExpr();
    }
  }

  private Stmt parseForLoopBody(For forNode) {
    parseControlFlowBlock(forNode.body);
    return forNode;
  }

  private Stmt parseExitStmt() {
    Token exitToken = expect(EXIT);
    Exit exit = ASTFactory.createExit(exitToken);
    return exit;
  }

  private Stmt parseReturnSlotAssignment() {
    List<String> varNames = parseIdList();

    Token assignToken = null;
    if (is(DOUBLE_COLON_ASSIGN)) {
        assignToken = expect(DOUBLE_COLON_ASSIGN);
    } else {
        assignToken = expect(ASSIGN);
    }

    List<String> slotNames = expressionParser.parseReturnSlots();
    expect(COLON);
    
    // Check if this is a lambda expression
    if (expressionParser.isLambdaExpression()) {
        Lambda lambda = expressionParser.parseLambdaSignature();
        
        // Validate slot count if needed
        if (!lambda.returnSlots.isEmpty() && lambda.returnSlots.size() != slotNames.size()) {
            throw error(
                "Number of slot names (" + slotNames.size() + 
                ") does not match number of lambda return slots (" + 
                lambda.returnSlots.size() + ")", assignToken);
        }
        
        ReturnSlotAssignment assignment =
            ASTFactory.createReturnSlotAsmt(varNames, lambda, assignToken);
        return assignment;
    }
    // Existing method call handling
    MethodCall methodCall = expressionParser.parseMethodCall();
    methodCall.slotNames = slotNames;

    for (String varName : varNames) {
        if ("_".equals(varName)) {
            continue;
        }
    }

    if (varNames.size() != slotNames.size()) {
        throw error(
            "Number of variables (" + varNames.size() + 
            ") does not match number of slots (" + slotNames.size() + ")");
    }

    ReturnSlotAssignment assignment =
        ASTFactory.createReturnSlotAsmt(varNames, methodCall, assignToken);
    return assignment;
  }

  private Stmt parseIndexAssignment() {
    Token arrayVarToken = now();
    Expr arrayVar = ASTFactory.createIdentifier(expect(ID).getText(), arrayVarToken);

    // Parse the entire index expression using the expression parser
    Expr indexAccess = expressionParser.parseIndexAccessContinuation(arrayVar);

    // Handle any additional chained indexing (e.g., matrix[1 to 2][0..1])
    while (is(LBRACKET)) {
      indexAccess = expressionParser.parseIndexAccessContinuation(indexAccess);
    }

    Token assignToken = expect(ASSIGN);
    Expr value = expressionParser.parseExpr();

    return ASTFactory.createAsmt(indexAccess, value, false, assignToken);
  }

  private Stmt parseMethodCallStmt() {
    if (is(next(0), LBRACKET)) {
        List<String> slotNames = expressionParser.parseReturnSlots();
        expect(COLON);
        MethodCall methodCall = expressionParser.parseMethodCall();
        methodCall.slotNames = slotNames;
        return methodCall;
    }
    MethodCall methodCall = expressionParser.parseMethodCall();
    return methodCall;
  }

  private Stmt parseExprStmt() {
    Expr expr = expressionParser.parseExpr();
    return expr;
  }

  private List<String> parseIdList() {
    List<String> ids = new ArrayList<String>();
    ids.add(expect(ID).getText());
    while (consume(COMMA)) {
      ids.add(expect(ID).getText());
    }
    return ids;
  }

  protected boolean isSlotAssignment() {
    Token first = now();
    if (nil(first) || first.symbol != TILDE_ARROW) return false;

    Token next = next();
    if (nil(next)) return false;

    return isExprStart(next);
  }

  private void bracketDepth() {
        int depth = 1;
        while (!is(EOF) && depth > 0) {
        if (is(LBRACKET)) depth++;
        else if (is(RBRACKET)) depth--;
        consume();
      }
  }

  private boolean isIndexAssignment() {
    save();
    try {
      if (!is(ID) || !is(next(), LBRACKET)) return false;

      consume(); // ID
      consume(); // LBRACKET

      bracketDepth();

      while (is(LBRACKET)) {
        consume();
        bracketDepth();
      }

      return is(ASSIGN);
    } finally {
      restore();
    }
  }

  private boolean isReturnSlotAssignment() {
    save();
    try {
        if (!is(ID)) return false;
        consume(); // First ID

        while (is(COMMA)) {
            consume();
            if (!is(ID)) return false;
            consume();
        }

        if (!is(ASSIGN, DOUBLE_COLON_ASSIGN)) return false;
        consume();

        if (!is(LBRACKET)) return false;
        consume();

        if (!is(RBRACKET)) {
            if (!is(ID, INT_LIT)) return false;
            consume();
            while (is(COMMA)) {
                consume();
                if (!is(ID, INT_LIT)) return false;
                consume();
            }
            if (!is(RBRACKET)) return false;
        }
        consume(); // RBRACKET

        if (!is(COLON)) return false;
        consume(); // COLON

        // Check if it's a lambda expression
        if (is(LAMBDA)) {
            return true;
        }

        // Existing method call check
        if (!is(ID)) return false;
        consume();

        while (is(DOT)) {
            consume();
            if (!is(ID)) return false;
            consume();
        }

        return is(LPAREN);
    } finally {
        restore();
    }
  }

  private boolean isMethodCallStmt() {
    save();
    try {
      if (is(LBRACKET)) {
        consume();
        if (!is(RBRACKET)) {
          if (!is(ID, INT_LIT)) return false;
          consume();
          while (is(COMMA)) {
            consume();
            if (!is(ID, INT_LIT)) return false;
            consume();
          }
          if (!is(RBRACKET)) return false;
        }
        consume(); // RBRACKET
        if (!is(COLON)) return false;
        consume(); // COLON
      }

      if (!is(ID)) return false;
      consume();

      while (is(DOT)) {
        consume();
        if (!is(ID)) return false;
        consume();
      }

      return is(LPAREN);
    } finally {
      restore();
    }
  }

  private boolean isVariableDeclaration() {
    Token first = now();
    Token second = next();

    if (is(first, ID) && is(second, DOUBLE_COLON_ASSIGN)) return true;

    if (is(first, ID) && is(second, COLON)) {
      Token third = next(2);
      return isTypeStart(third);
    }

    return false;
  }
}
