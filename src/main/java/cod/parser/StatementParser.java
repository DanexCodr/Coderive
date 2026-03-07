package cod.parser;

import cod.ast.ASTFactory;
import cod.ast.FlatAST;
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
    super(ctx, expressionParser.getFactory());
    this.expressionParser = expressionParser;
    this.slotParser = new SlotParser(this);
  }

    @Override
    protected BaseParser createIsolatedParser(ParserContext isolatedCtx) {
        ExpressionParser isolatedExprParser = new ExpressionParser(
            isolatedCtx,
            this.factory,
            this.expressionParser.getGlobalRegistry(),
            null
        );
        StatementParser isolatedStmtParser = new StatementParser(isolatedCtx, isolatedExprParser);
        isolatedExprParser.setStatementParser(isolatedStmtParser);
        return isolatedStmtParser;
    }

  public int parseStmt() {
    return attempt(
        new ParserAction<Integer>() {
          @Override
          public Integer parse() throws ParseError {
            return parseStmtInternal();
          }
        });
  }

  private int parseStmtInternal() {
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

  private int parseSkipStmt() {
    Token skipToken = expect(SKIP);

   

    Token nextToken = now();

    if (!(any(is(nextToken, EOF), is(nextToken, RBRACE)) || isStmtStart(nextToken))) {
      throw error(
          "Nothing can follow 'skip' in the same statement. "
              + "'skip' must be the complete statement.",
          skipToken);
    }

    return factory.createSkipStmt(skipToken);
  }

  private int parseBreakStmt() {
    Token breakToken = expect(BREAK);

   

    Token nextToken = now();

    if (!(any(is(nextToken, EOF), is(nextToken, RBRACE)) || isStmtStart(nextToken))) {
      throw error(
          "Nothing can follow 'break' in the same statement. "
              + "'break' must be the complete statement.",
          breakToken);
    }

    return factory.createBreakStmt(breakToken);
  }
  
  private boolean isStmtStart(Token token) {
    if (nil(token)) return false;

    if (is(token, ID)) return is(next(), COLON, ASSIGN, DOUBLE_COLON_ASSIGN, LBRACKET);

    if (is(token, TILDE_ARROW)) return true;

    if (is(token, LBRACKET)) return isMethodCallStmt();

    return is(token, IF, FOR, EXIT, ELSE, ELIF, SKIP, BREAK, SHARE, LOCAL);
  }

  private int parseSlotAssignment() {
    Token tildeArrowToken = expect(TILDE_ARROW);
    return slotParser.parseSlotAssignmentsAsStmt(tildeArrowToken);
  }

  private int parseSimpleAssignment() {
    Token startToken = now();

    String idName = null;
    if (isThisExpr()) {
        int target = parseThisExpr();
        Token assignToken = expect(ASSIGN);
        int value = expressionParser.parseExpr();
        int assignment = factory.createAsmt(target, value, false, assignToken);
        return assignment;
    } else if (is(SUPER)) {
        Token superToken = expect(SUPER);
        Token dotToken = expect(DOT);
        Token fieldNameToken = now();
        String fieldName = expect(ID).text;
        
        int fieldNode = factory.createIdentifier(fieldName, fieldNameToken);
        
        int target = factory.createPropertyAccess(
            factory.createSuperExpr(superToken), 
            fieldNode, 
            dotToken
        );

        Token assignToken = expect(ASSIGN);
        int value = expressionParser.parseExpr();
        int assignment = factory.createAsmt(target, value, false, assignToken);
        return assignment;
    } else {
        idName = expect(ID).text;
    }

    if ("_".equals(idName)) {
        throw error(
            "Cannot assign to '_'. Underscore is reserved for discard/placeholder.", startToken);
    }

    int target = factory.createIdentifier(idName, startToken);
    Token assignToken = expect(ASSIGN);
    int value = expressionParser.parseExpr();

    int assignment = factory.createAsmt(target, value, false, assignToken);
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

  private int parseThisExpr() {
    Token first = now();
    if (is(first, THIS)) {
      Token thisToken = expect(THIS);
      return factory.createThisExpr(null, thisToken);
    } else if (is(first, ID)) {
      Token classNameToken = first;
      String className = expect(ID).text;
      if (consume(DOT)) {
        Token thisToken = expect(THIS);
        return factory.createThisExpr(className, thisToken);
      } else {
        restore();
        return factory.createIdentifier(className, classNameToken);
      }
    }
    throw error("Expected 'this' or 'ClassName.this'", first);
  }

  private int parseVariableDeclaration() {
    Token startToken = now();
    String typeName = null;
    boolean isImplicit = false;
    String varName = null;
    int value = cod.ast.FlatAST.NULL;

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

    int varId = factory.createVar(varName, value, startToken);
    if (!isImplicit && typeName != null) factory.getAST().varSetExplicitType(varId, typeName);
    return varId;
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

  private int parseIfStmt() {
    Token ifToken = expect(IF);

   
    int condition = expressionParser.parseExpr();

    int rootIfId = factory.createIfStmt(condition, ifToken);

    parseControlFlowBlock(factory.getAST().stmtIfThen(rootIfId));

    int currentNodeId = rootIfId;

    while (is(ELIF)) {
      Token elifToken = expect(ELIF);
      int elifCondition = expressionParser.parseExpr();
      int elifNodeId = factory.createIfStmt(elifCondition, elifToken);

      parseControlFlowBlock(factory.getAST().stmtIfThen(elifNodeId));
      factory.getAST().blockAddStmt(factory.getAST().stmtIfElse(currentNodeId), elifNodeId);
      currentNodeId = elifNodeId;
    }

    if (is(ELSE)) {
      expect(ELSE);

      ParserState savedState = getCurrentState();
     

      if (is(IF)) {
        factory.getAST().blockAddStmt(factory.getAST().stmtIfElse(currentNodeId), parseIfStmt());
      } else {
        setState(savedState);
       

        if (is(LBRACE)) {
          expect(LBRACE);
         
          while (!is(RBRACE) && !is(EOF)) {
            factory.getAST().blockAddStmt(factory.getAST().stmtIfElse(currentNodeId), parseStmtInternal());
           
          }
          expect(RBRACE);
        } else {
          Token next = now();
          if (!(is(next, EOF) || is(next, RBRACE)) && !isInControlFlow(next)) {
            factory.getAST().blockAddStmt(factory.getAST().stmtIfElse(currentNodeId), parseStmtInternal());
          }
        }
      }
    }

    return rootIfId;
  }

  private void parseControlFlowBlock(int blockId) {
   

    if (is(LBRACE)) {
      expect(LBRACE);
     
      while (!is(RBRACE) && !is(EOF)) {
        factory.getAST().blockAddStmt(blockId, parseStmtInternal());
       
      }
      expect(RBRACE);
    } else {
      Token next = now();
      if (!(is(next, EOF) || is(next, RBRACE)) && !isInControlFlow(next)) {
        factory.getAST().blockAddStmt(blockId, parseStmtInternal());
      }
    }
  }

  private boolean isInControlFlow(Token token) {
    return is(token, IF, FOR, ELSE, ELIF, EXIT, SKIP, BREAK);
  }

  private int parseForStmt() {
    Token forToken = expect(FOR);
    Token iteratorToken = now();
    String iterator = expect(ID).text;
    
    // Expect 'of' for both array iteration and range loops
    expect(OF);
    
    // Parse the source/start expression
    int source = expressionParser.parseExpr();

    // Check if this is a range loop (has .. or to after the start expression)
    if (is(RANGE_DOTDOT)) {
        Token rangeToken = expect(RANGE_DOTDOT);
        int end = expressionParser.parseExpr();
        
        // Check for optional step after end
        int step = cod.ast.FlatAST.NULL;
        Token stepToken = null;
        if (is(RANGE_HASH)) {
            stepToken = expect(RANGE_HASH);
            step = parseStepExpr(iterator, iteratorToken);
        } else if (is(BY)) {
            stepToken = expect(BY);
            step = parseStepExpr(iterator, iteratorToken);
        }
        
        int range = factory.createRange(step, source, end, stepToken, rangeToken);
        int forNodeId = factory.createFor(iterator, range, forToken, iteratorToken);
        return parseForLoopBody(forNodeId);
    } 
    else if (is(TO)) {
        Token rangeToken = expect(TO);
        int end = expressionParser.parseExpr();
        
        // Check for optional step after end
        int step = cod.ast.FlatAST.NULL;
        Token stepToken = null;
        if (is(BY)) {
            stepToken = expect(BY);
            step = parseStepExpr(iterator, iteratorToken);
        }
        
        int range = factory.createRange(step, source, end, stepToken, rangeToken);
        int forNodeId = factory.createFor(iterator, range, forToken, iteratorToken);
        return parseForLoopBody(forNodeId);
    } 
    else {
        // This is array iteration - source is the array
        int forNodeId = factory.createForArray(iterator, source, forToken, iteratorToken);
        return parseForLoopBody(forNodeId);
    }
}

  private int parseStepExpr(String iterator, Token iteratorToken) {
    if (is(MUL) || is(DIV)) {
      Token operatorToken = now();
      String operator = operatorToken.text;
      consume();
      int operand = expressionParser.parseExpr();
      int iteratorRef = factory.createIdentifier(iterator, iteratorToken);
      return factory.createBinaryOp(iteratorRef, operator, operand, operatorToken);
    } else if (is(PLUS) || is(MINUS)) {
      Token operatorToken = now();
      consume();
      if (is(INT_LIT, FLOAT_LIT)) {
        int operand = expressionParser.parsePrimaryExpr();
        if (is(operatorToken, PLUS)) {
          return operand;
        } else {
          return factory.createUnaryOp("-", operand, operatorToken);
        }
      } else {
        return expressionParser.parseExpr();
      }
    } else {
      return expressionParser.parseExpr();
    }
  }

  private int parseForLoopBody(int forNodeId) {
    parseControlFlowBlock(factory.getAST().forBody(forNodeId));
    return forNodeId;
  }

  private int parseExitStmt() {
    Token exitToken = expect(EXIT);
    int exit = factory.createExit(exitToken);
    return exit;
  }

  private int parseReturnSlotAssignment() {
    List<String> varNames = parseIdList();

    Token assignToken = null;
    if (is(DOUBLE_COLON_ASSIGN)) {
      assignToken = expect(DOUBLE_COLON_ASSIGN);
    } else {
      assignToken = expect(ASSIGN);
    }

    List<String> slotNames = expressionParser.parseReturnSlots();
    expect(COLON);
    int methodCall = expressionParser.parseMethodCall();
    factory.getAST().methodCallSetSlotNames(methodCall, slotNames.toArray(new String[slotNames.size()]));

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

    return factory.createReturnSlotAsmt(varNames, methodCall, assignToken);
  }

  private int parseIndexAssignment() {
    Token arrayVarToken = now();
    int arrayVar = factory.createIdentifier(expect(ID).text, arrayVarToken);

    // Parse the entire index expression using the expression parser
    int indexAccess = expressionParser.parseIndexAccessContinuation(arrayVar);

    // Handle any additional chained indexing (e.g., matrix[1 to 2][0..1])
    while (is(LBRACKET)) {
      indexAccess = expressionParser.parseIndexAccessContinuation(indexAccess);
    }

    Token assignToken = expect(ASSIGN);
    int value = expressionParser.parseExpr();

    return factory.createAsmt(indexAccess, value, false, assignToken);
  }

  private int parseMethodCallStmt() {
    if (is(next(0), LBRACKET)) {
        List<String> slotNames = expressionParser.parseReturnSlots();
        expect(COLON);
        int methodCall = expressionParser.parseMethodCall();
        factory.getAST().methodCallSetSlotNames(methodCall, slotNames.toArray(new String[slotNames.size()]));
        return methodCall;
    } else {
        int methodCall = expressionParser.parseMethodCall();
        // This will be handled as a single-slot call if the method has one slot
        // The interpreter will determine this during execution
        return methodCall;
    }
}

  private int parseExprStmt() {
    int expr = expressionParser.parseExpr();
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