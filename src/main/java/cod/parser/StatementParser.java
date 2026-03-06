package cod.parser;

import cod.ast.ASTFactory;
import cod.semantic.NamingValidator;
import cod.error.ParseError;
import cod.ast.nodes.*;

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

  public StmtNode parseStmt() {
    return attempt(
        new ParserAction<StmtNode>() {
          @Override
          public StmtNode parse() throws ParseError {
            return parseStmtInternal();
          }
        });
  }

  private StmtNode parseStmtInternal() {
    checkIllegalDeclaration();

    Token first = now();

    if (is(first, KEYWORD)) {
      if (is(IF)) return parseIfStmt();
      if (is(FOR)) return parseForStmt();
      if (is(EXIT)) return parseExitStmt();
      if (is(SKIP)) return parseSkipStmt();
      if (is(BREAK)) return parseBreakStmt();
    }

    if (is(first, TRUE, FALSE)) {
      Token second = next();
      if (is(second, LBRACE)) {
        throw error(
            "Missing 'if' keyword. Use 'if "
                + first.text
                + " { ... }' instead of '"
                + first.text
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
        String varName = first.text;
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

    if (isTypeStart(current)) return;

    Token idToken = next();
    if (!is(idToken, ID)) return;

    Token afterId = next(2);
    if (is(afterId, ASSIGN) || (is(afterId, LBRACKET) && is(next(3), RBRACKET))) {
      throw error(
          "Illegally used reserved keyword '"
              + current.text
              + "' for declaration of variable '"
              + idToken.text
              + "'",
          current,
          idToken);
    }
  }

  private StmtNode parseSkipStmt() {
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

  private StmtNode parseBreakStmt() {
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

  private StmtNode parseSlotAssignment() {
    Token tildeArrowToken = expect(TILDE_ARROW);
    return slotParser.parseSlotAssignmentsAsStmt(tildeArrowToken);
  }

  private StmtNode parseSimpleAssignment() {
    Token startToken = now();

    String idName = null;
    if (isThisExpr()) {
        ExprNode target = parseThisExpr();
        Token assignToken = expect(ASSIGN);
        ExprNode value = expressionParser.parseExpr();
        AssignmentNode assignment = ASTFactory.createAsmt(target, value, false, assignToken);
        return assignment;
    } else if (is(SUPER)) {
        Token superToken = expect(SUPER);
        Token dotToken = expect(DOT);
        Token fieldNameToken = now();
        String fieldName = expect(ID).text;
        
        ExprNode fieldNode = ASTFactory.createIdentifier(fieldName, fieldNameToken);
        
        ExprNode target = ASTFactory.createPropertyAccess(
            ASTFactory.createSuperExpr(superToken), 
            fieldNode, 
            dotToken
        );

        Token assignToken = expect(ASSIGN);
        ExprNode value = expressionParser.parseExpr();
        AssignmentNode assignment = ASTFactory.createAsmt(target, value, false, assignToken);
        return assignment;
    } else {
        idName = expect(ID).text;
    }

    if ("_".equals(idName)) {
        throw error(
            "Cannot assign to '_'. Underscore is reserved for discard/placeholder.", startToken);
    }

    ExprNode target = ASTFactory.createIdentifier(idName, startToken);
    Token assignToken = expect(ASSIGN);
    ExprNode value = expressionParser.parseExpr();

    AssignmentNode assignment = ASTFactory.createAsmt(target, value, false, assignToken);
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

  private ExprNode parseThisExpr() {
    Token first = now();
    if (is(first, THIS)) {
      Token thisToken = expect(THIS);
      return ASTFactory.createThisExpr(null, thisToken);
    } else if (is(first, ID)) {
      Token classNameToken = first;
      String className = expect(ID).text;
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

  private StmtNode parseVariableDeclaration() {
    Token startToken = now();
    String typeName = null;
    boolean isImplicit = false;
    String varName = null;
    ExprNode value = null;

    if (is(ID)) {
      varName = expect(ID).text;

      if (is(DOUBLE_COLON_ASSIGN)) {
        if (is(THIS) && !nextIsPropertyAccess()) {
          throw error("Cannot declare variable named 'this'", startToken);
        }

        if ("_".equals(varName)) {
          throw error(
              "Cannot declare variable '_'. Underscore is reserved for discard/placeholder.",
              startToken);
        }
        isImplicit = true;
        expect(DOUBLE_COLON_ASSIGN);
        value = expressionParser.parseExpr();

      } else if (is(COLON)) {
        if (is(THIS) && !nextIsPropertyAccess()) {
          throw error("Cannot declare variable named 'this'", startToken);
        }

        if ("_".equals(varName)) {
          throw error(
              "Cannot declare variable '_'. Underscore is reserved for discard/placeholder",
              startToken);
        }

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
      if (NamingValidator.isAllCaps(varName)) {
        NamingValidator.validateConstantName(varName, startToken);
      } else {
        NamingValidator.validateVariableName(varName, startToken);
      }
    }

    VarNode varNode = ASTFactory.createVar(varName, value, startToken);
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

  private StmtNode parseIfStmt() {
    Token ifToken = expect(IF);

   
    ExprNode condition = expressionParser.parseExpr();

    StmtIfNode rootIfNode = ASTFactory.createIfStmt(condition, ifToken);

    parseControlFlowBlock(rootIfNode.thenBlock);

    StmtIfNode currentNode = rootIfNode;

    while (is(ELIF)) {
      Token elifToken = expect(ELIF);
      ExprNode elifCondition = expressionParser.parseExpr();
      StmtIfNode elifNode = ASTFactory.createIfStmt(elifCondition, elifToken);

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

  private void parseControlFlowBlock(BlockNode block) {
   

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

  private StmtNode parseForStmt() {
    Token forToken = expect(FOR);
    Token iteratorToken = now();
    String iterator = expect(ID).text;
    
    // Expect 'of' for both array iteration and range loops
    expect(OF);
    
    // Parse the source/start expression
    ExprNode source = expressionParser.parseExpr();

    // Check if this is a range loop (has .. or to after the start expression)
    if (is(RANGE_DOTDOT)) {
        Token rangeToken = expect(RANGE_DOTDOT);
        ExprNode end = expressionParser.parseExpr();
        
        // Check for optional step after end
        ExprNode step = null;
        Token stepToken = null;
        if (is(RANGE_HASH)) {
            stepToken = expect(RANGE_HASH);
            step = parseStepExpr(iterator, iteratorToken);
        } else if (is(BY)) {
            stepToken = expect(BY);
            step = parseStepExpr(iterator, iteratorToken);
        }
        
        RangeNode range = ASTFactory.createRange(step, source, end, stepToken, rangeToken);
        ForNode forNode = ASTFactory.createFor(iterator, range, forToken, iteratorToken);
        return parseForLoopBody(forNode);
    } 
    else if (is(TO)) {
        Token rangeToken = expect(TO);
        ExprNode end = expressionParser.parseExpr();
        
        // Check for optional step after end
        ExprNode step = null;
        Token stepToken = null;
        if (is(BY)) {
            stepToken = expect(BY);
            step = parseStepExpr(iterator, iteratorToken);
        }
        
        RangeNode range = ASTFactory.createRange(step, source, end, stepToken, rangeToken);
        ForNode forNode = ASTFactory.createFor(iterator, range, forToken, iteratorToken);
        return parseForLoopBody(forNode);
    } 
    else {
        // This is array iteration - source is the array
        ForNode forNode = ASTFactory.createFor(iterator, source, forToken, iteratorToken);
        return parseForLoopBody(forNode);
    }
}

  private ExprNode parseStepExpr(String iterator, Token iteratorToken) {
    if (is(MUL) || is(DIV)) {
      Token operatorToken = now();
      String operator = operatorToken.text;
      consume();
      ExprNode operand = expressionParser.parseExpr();
      ExprNode iteratorRef = ASTFactory.createIdentifier(iterator, iteratorToken);
      return ASTFactory.createBinaryOp(iteratorRef, operator, operand, operatorToken);
    } else if (is(PLUS) || is(MINUS)) {
      Token operatorToken = now();
      consume();
      if (is(INT_LIT, FLOAT_LIT)) {
        ExprNode operand = expressionParser.parsePrimaryExpr();
        if (is(operatorToken, PLUS)) {
          return operand;
        } else {
          return ASTFactory.createUnaryOp("-", operand, operatorToken);
        }
      } else {
        return expressionParser.parseExpr();
      }
    } else {
      return expressionParser.parseExpr();
    }
  }

  private StmtNode parseForLoopBody(ForNode forNode) {
    parseControlFlowBlock(forNode.body);
    return forNode;
  }

  private StmtNode parseExitStmt() {
    Token exitToken = expect(EXIT);
    ExitNode exit = ASTFactory.createExit(exitToken);
    return exit;
  }

  private StmtNode parseReturnSlotAssignment() {
    List<String> varNames = parseIdList();

    Token assignToken = null;
    if (is(DOUBLE_COLON_ASSIGN)) {
      assignToken = expect(DOUBLE_COLON_ASSIGN);
    } else {
      assignToken = expect(ASSIGN);
    }

    List<String> slotNames = expressionParser.parseReturnSlots();
    expect(COLON);
    MethodCallNode methodCall = expressionParser.parseMethodCall();
    methodCall.slotNames = slotNames;

    for (String varName : varNames) {
      if ("_".equals(varName)) {
        continue;
      }
    }

    if (varNames.size() != slotNames.size()) {
      throw error(
          "Number of variables ("
              + varNames.size()
              + ") does not match number of slots ("
              + slotNames.size()
              + ")");
    }

    ReturnSlotAssignmentNode assignment =
        ASTFactory.createReturnSlotAsmt(varNames, methodCall, assignToken);
    return assignment;
  }

  private StmtNode parseIndexAssignment() {
    Token arrayVarToken = now();
    ExprNode arrayVar = ASTFactory.createIdentifier(expect(ID).text, arrayVarToken);

    // Parse the entire index expression using the expression parser
    ExprNode indexAccess = expressionParser.parseIndexAccessContinuation(arrayVar);

    // Handle any additional chained indexing (e.g., matrix[1 to 2][0..1])
    while (is(LBRACKET)) {
      indexAccess = expressionParser.parseIndexAccessContinuation(indexAccess);
    }

    Token assignToken = expect(ASSIGN);
    ExprNode value = expressionParser.parseExpr();

    return ASTFactory.createAsmt(indexAccess, value, false, assignToken);
  }

  private StmtNode parseMethodCallStmt() {
    if (is(next(0), LBRACKET)) {
        List<String> slotNames = expressionParser.parseReturnSlots();
        expect(COLON);
        MethodCallNode methodCall = expressionParser.parseMethodCall();
        methodCall.slotNames = slotNames;
        return methodCall;
    } else {
        MethodCallNode methodCall = expressionParser.parseMethodCall();
        // This will be handled as a single-slot call if the method has one slot
        // The interpreter will determine this during execution
        return methodCall;
    }
}

  private StmtNode parseExprStmt() {
    ExprNode expr = expressionParser.parseExpr();
    return expr;
  }

  private List<String> parseIdList() {
    List<String> ids = new ArrayList<String>();
    ids.add(expect(ID).text);
    while (consume(COMMA)) {
      ids.add(expect(ID).text);
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

      return is(COLON);
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