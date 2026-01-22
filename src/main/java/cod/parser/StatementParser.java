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

  public StatementParser(ParserContext ctx, ExpressionParser expressionParser) {
    super(ctx);
    this.expressionParser = expressionParser;
  }

  @Override
  protected BaseParser createIsolatedParser(ParserContext isolatedCtx) {
    return new StatementParser(isolatedCtx, this.expressionParser);
  }

  public StmtNode parseStatement() {
    return attempt(new ParserAction<StmtNode>() {
      @Override
      public StmtNode parse() throws ParseError {
        return parseStatementImpl(null);
      }
    });
  }

  private StmtNode parseStatementImpl(Boolean inheritedStyle) {
    checkIllegalDeclaration();

    Token first = currentToken();

    if (is(first, KEYWORD)) {
        if (is(IF)) return parseIfStatement(inheritedStyle);
        if (is(FOR)) return parseForStatement();
        if (is(EXIT)) return parseExitStatement();
        if (is(SKIP)) return parseSkipStatement();
        if (is(BREAK)) return parseBreakStatement();
    }

    if (is(first, TILDE_ARROW)) return parseSlotAssignment();

    if (is(first, LBRACKET) && isMethodCallStatement()) {
        return parseMethodCallStatement();
    }

    if (isVariableDeclaration()) {
        return parseVariableDeclaration();
    }
    
    if (is(first, ID)) {
        Token second = lookahead(1);

        if (second != null) {
            if (is(first, ID) && is(second, LBRACKET)) {
                if (isIndexAssignment()) return parseIndexAssignment();
            }

            if (is(first, ID) && is(second, ASSIGN)) {
                if (isReturnSlotAssignment()) return parseReturnSlotAssignment();
                return parseSimpleAssignment();
            }

            if (is(first, ID) && is(second, COMMA)) {
                if (isReturnSlotAssignment()) return parseReturnSlotAssignment();
            }
        }

        if (isMethodCallStatement()) return parseMethodCallStatement();
    }

    return parseExpressionStatement();
  }

  private void checkIllegalDeclaration() {
    Token current = currentToken();
    if (current.type != KEYWORD) return;

    if (isTypeStart(current)) return;

    Token idToken = peek(1);
    if (idToken != null && is(idToken, ID)) {
        Token afterId = peek(2);
        if (afterId != null) {
            if (is(afterId, ASSIGN)) {
                throw new ParseError(
                    "Illegally used reserved keyword '"
                        + current.text
                        + "' for declaration of variable '"
                        + idToken.text
                        + "'",
                    current, idToken);
            }
            if (is(afterId, LBRACKET) && peek(3) != null && is(peek(3), RBRACKET)) {
                throw new ParseError(
                    "Illegally used reserved keyword '"
                        + current.text
                        + "' for declaration of variable '"
                        + idToken.text
                        + "'",
                    current, idToken);
            }
        }
    }
  }

  private StmtNode parseSkipStatement() {
    Token skipToken = expect(SKIP);
    
    skipWhitespaceAndComments();
    
    Token nextToken = currentToken();
    
    if (is(nextToken, EOF) || is(nextToken, RBRACE)) {
    } else if (isStatementStart(nextToken)) {
    } else {
        throw new ParseError(
            "Nothing can follow 'skip' in the same statement. " +
            "'skip' must be the complete statement.",
            skipToken);
    }
    
    return ASTFactory.createSkipStatement(skipToken);
  }

  private StmtNode parseBreakStatement() {
    Token breakToken = expect(BREAK);
    
    skipWhitespaceAndComments();
    
    Token nextToken = currentToken();
    
    if (is(nextToken, EOF) || is(nextToken, RBRACE)) {
    } else if (isStatementStart(nextToken)) {
    } else {
        throw new ParseError(
            "Nothing can follow 'break' in the same statement. " +
            "'break' must be the complete statement.",
            breakToken);
    }
    
    return ASTFactory.createBreakStatement(breakToken);
  }

  private boolean isStatementStart(Token token) {
    if (token == null) return false;
    
    if (is(token, KEYWORD)) {
        return is(token, IF, FOR, EXIT, ELSE, ELIF, SKIP, BREAK, SHARE, LOCAL);
    }
    
    if (is(token, ID)) {
        Token next = peek(1);
        if (next != null) return is(next, COLON, ASSIGN, DOUBLE_COLON_ASSIGN, LBRACKET);
    }
    
    if (is(token, TILDE_ARROW)) return true;
    
    if (is(token, LBRACKET)) return isMethodCallStatement();
    
    return false;
  }

  private SlotAssignmentNode parseSingleSlotAssignment() {
    String slotName = null;
    ExprNode value;
    Token colonToken = null;
    
    if (is(currentToken(), ID)) {
        Token afterId = peek(1);
        if (afterId != null && is(afterId, COLON)) {
            slotName = expect(ID).text;
            colonToken = expect(COLON);
            value = expressionParser.parseExpression();
        } else {
            slotName = null;
            value = expressionParser.parseExpression();
        }
    } else {
        slotName = null;
        value = expressionParser.parseExpression();
    }
    
    return ASTFactory.createSlotAssignment(slotName, value, colonToken); 
  }

  private StmtNode parseSlotAssignment() {
    Token tildeArrowToken = expect(TILDE_ARROW);
    List<SlotAssignmentNode> assignments = new ArrayList<SlotAssignmentNode>();
    assignments.add(parseSingleSlotAssignment());
    if (is(COMMA)) {
      while (tryConsume(COMMA)) assignments.add(parseSingleSlotAssignment());
      MultipleSlotAssignmentNode multiAssign = ASTFactory.createMultipleSlotAssignment(assignments, tildeArrowToken);
      return multiAssign;
    } else {
      SlotAssignmentNode assignment = assignments.get(0);
      return assignment;
    }
  }

  private StmtNode parseSimpleAssignment() {
    Token startToken = currentToken();
    
    String idName = null;
    if (isThisExpression()) {
        ExprNode target = parseThisExpression();
        Token assignToken = expect(ASSIGN);
        ExprNode value = expressionParser.parseExpression();
        AssignmentNode assignment = ASTFactory.createAssignment(target, value, false, assignToken);
        return assignment;
    } else if (is(SUPER)) {
        Token superToken = expect(SUPER);
        expect(DOT);
        Token fieldNameToken = currentToken();
        String fieldName = expect(ID).text;
        
        ExprNode target = ASTFactory.createPropertyAccess(
            ASTFactory.createSuperExpression(superToken), 
            fieldName, fieldNameToken);
        
        Token assignToken = expect(ASSIGN);
        ExprNode value = expressionParser.parseExpression();
        AssignmentNode assignment = ASTFactory.createAssignment(target, value, false, assignToken);
        return assignment;
    } else {
        idName = expect(ID).text;
    }
    
    if ("_".equals(idName)) {
        throw new ParseError(
            "Cannot assign to '_'. Underscore is reserved for discard/placeholder.",
            startToken);
    }
    
    ExprNode target = ASTFactory.createIdentifier(idName, startToken);
    Token assignToken = expect(ASSIGN);
    ExprNode value = expressionParser.parseExpression();
    
    AssignmentNode assignment = ASTFactory.createAssignment(target, value, false, assignToken);
    return assignment;
  }

  private boolean isThisExpression() {
    return attempt(new ParserAction<Boolean>() {
        @Override
        public Boolean parse() throws ParseError {
            Token current = currentToken();
            if (is(current, KEYWORD) && is(current, THIS)) {
                return true;
            } else if (is(current,  ID)) {
                if (lookahead(1) != null && is(lookahead(1), DOT)) {
                    Token afterDot = lookahead(2);
                    if (afterDot != null && is(afterDot, KEYWORD) && 
                        is(afterDot, THIS)) {
                        return true;
                    }
                }
            }
            return false;
        }
    });
  }

  private ExprNode parseThisExpression() {
    Token first = currentToken();
    if (is(first, KEYWORD) && is(first, THIS)) {
        Token thisToken = expect(THIS);
        return ASTFactory.createThisExpression(null, thisToken);
    } else if (is(first, ID)) {
        Token classNameToken = first;
        String className = expect(ID).text;
        if (tryConsume(DOT)) {
            Token thisToken = expect(THIS);
            return ASTFactory.createThisExpression(className, thisToken);
        } else {
            restore(); // Go back to the ID token
            return ASTFactory.createIdentifier(className, classNameToken);
        }
    }
    throw error("Expected 'this' or 'ClassName.this'", first);
  }

  private StmtNode parseVariableDeclaration() {
    Token startToken = currentToken();
    String typeName = null;
    boolean isImplicit = false;
    String varName = null;
    ExprNode value = null;

    // Save the current token before consuming it
    Token idToken = currentToken();
    
    if (is(idToken, ID)) {
        varName = expect(ID).text;  // This consumes the token
        
        // Now check what comes after
        if (is(currentToken(), DOUBLE_COLON_ASSIGN)) {
            // Handle implicit typing: name := value
            if (is(idToken, THIS) && !lookaheadIsPropertyAccess()) {
                throw new ParseError(
                    "Cannot declare variable named 'this'",
                    startToken);
            }
            
            if ("_".equals(varName)) {
                throw new ParseError(
                    "Cannot declare variable '_'. Underscore is reserved for discard/placeholder.",
                    startToken);
            }
            isImplicit = true;
            expect(DOUBLE_COLON_ASSIGN);  // Consume the :=
            value = expressionParser.parseExpression();
            
        } else if (is(currentToken(), COLON)) {
            // Handle explicit typing: name: type [= value]
            if (is(idToken, THIS) && !lookaheadIsPropertyAccess()) {
                throw new ParseError(
                    "Cannot declare variable named 'this'",
                    startToken);
            }
            
            if ("_".equals(varName)) {
                throw new ParseError(
                    "Cannot declare variable '_'. Underscore is reserved for discard/placeholder",
                    startToken);
            }
            
            expect(COLON);
            
            if (isTypeStart(currentToken())) {
                typeName = parseTypeReference();
                
                if (tryConsume(ASSIGN)) {
                    value = expressionParser.parseExpression();
                }
            } else {
                throw new ParseError("Expected type after ':' in variable declaration. " +
                                     "For inferred typing use ':=' operator", startToken);
            }
        } else {
            throw new ParseError(
                "Expected variable declaration in format 'name: type', 'name: type = value', or 'name := value'", 
                startToken);
        }
    } else {
        throw new ParseError(
            "Expected variable declaration in format 'name: type', 'name: type = value', or 'name := value'", 
            startToken);
    }

    if (varName != null) {
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

  private boolean lookaheadIsPropertyAccess() {
    return attempt(new ParserAction<Boolean>() {
        @Override
        public Boolean parse() throws ParseError {
            Token current = currentToken();
            if (current == null || current.type != ID) return false;
            
            Token afterDot = lookahead(1);
            if (afterDot == null || afterDot.symbol != DOT) return false;
            
            Token afterDotToken = lookahead(2);
            if (afterDotToken == null) return false;
            
            if (is(afterDotToken, KEYWORD) && is(afterDotToken, THIS)) {
                return true;
            } else if (is(afterDotToken, ID)) {
                Token afterSecondId = lookahead(3);
                if (afterSecondId != null && is(afterSecondId, DOT)) {
                    Token afterSecondDot = lookahead(4);
                    if (afterSecondDot != null && is(afterSecondDot,  KEYWORD) && 
                        is(afterSecondDot, THIS)) {
                        return true;
                    }
                }
                return true;
            }
            
            return false;
        }
    });
  }

private StmtNode parseIfStatement(Boolean inheritedStyle) {
    Token ifToken = expect(IF);
    ExprNode condition = expressionParser.parseExpression();
    StmtIfNode rootIfNode = ASTFactory.createIfStatement(condition, ifToken);

    Boolean currentStyle = inheritedStyle;

    if (!is(LBRACE)) {
        if (lookaheadIsIfOrElif()) {
            throw new ParseError(
                "If statement containing another if or elif must use braces: if condition { ... }",
                currentToken());
        }
    }

    if (is(LBRACE)) {
      expect(LBRACE);
      while (!is(RBRACE)) rootIfNode.thenBlock.statements.add(parseStatementImpl(currentStyle));
      expect(RBRACE);
    } else {
      rootIfNode.thenBlock.statements.add(parseStatementImpl(currentStyle));
    }

    StmtIfNode currentNode = rootIfNode;

    while (is(ELIF)) {
      Token elifToken = expect(ELIF);
      if (currentStyle != null && !currentStyle)
        throw new ParseError("Cannot use 'elif' in an 'else if' style chain", currentToken());
      currentStyle = true;
      ExprNode elifCondition = expressionParser.parseExpression();
      StmtIfNode elifNode = ASTFactory.createIfStatement(elifCondition, elifToken);
      
      if (!is(LBRACE)) {
          if (lookaheadIsIfOrElif()) {
              throw new ParseError(
                  "Elif statement containing another if or elif must use braces: elif condition { ... }",
                  currentToken());
          }
      }
      
      if (is(LBRACE)) {
        expect(LBRACE);
        while (!is(RBRACE)) elifNode.thenBlock.statements.add(parseStatementImpl(currentStyle));
        expect(RBRACE);
      } else {
        elifNode.thenBlock.statements.add(parseStatementImpl(currentStyle));
      }
      currentNode.elseBlock.statements.add(elifNode);
      currentNode = elifNode;
    }

    if (is(ELSE)) {
        // Consume ELSE token first
        expect(ELSE);
            
        // Save state AFTER consuming ELSE
        ParserState savedState = getCurrentState();
        
        // Skip whitespace and comments to check for "if"
        while (getPosition() < tokens.size()) {
            if (is(currentToken(), WS, LINE_COMMENT, BLOCK_COMMENT)) {
                consume();
            } else {
                break;
            }
        }
        
        if (is(IF)) {
            // Handle "else if"
            if (currentStyle != null && currentStyle)
                throw new ParseError("Cannot use 'else if' in an 'elif' style chain", currentToken());
            currentStyle = false;
            currentNode.elseBlock.statements.add(parseIfStatement(currentStyle));
        } else {
            // Reset to position after ELSE keyword (savedState is already after ELSE)
            setState(savedState);
            
            // Skip whitespace and comments again
            skipWhitespaceAndComments();
            
            if (!is(LBRACE)) {
                if (isControlFlowStatementStart(currentToken())) {
                    throw new ParseError(
                        "Else statement containing another control flow statement must use braces: else { ... }",
                        currentToken());
                }
            }
            
            if (is(LBRACE)) {
                expect(LBRACE);
                while (!is(RBRACE)) currentNode.elseBlock.statements.add(parseStatementImpl(currentStyle));
                expect(RBRACE);
            } else {
                currentNode.elseBlock.statements.add(parseStatementImpl(currentStyle));
            }
        }
    }
    return rootIfNode;
}

  private boolean lookaheadIsIfOrElif() {
      ParserState savedState = getCurrentState();
      try {
          skipWhitespaceAndComments();
          
          return isControlFlowStatementStart(currentToken());
      } finally {
          setState(savedState);
      }
  }

  private boolean isControlFlowStatementStart(Token token) {
    if (token == null) return false;
    
    if (is(token, KEYWORD)) {
        return is(token, IF, FOR, ELSE, ELIF);
    }
    return false;
  }

  private StmtNode parseForStatement() {
    Token forToken = expect(FOR);
    
    Token iteratorToken = currentToken();
    String iterator = expect(ID).text;
    ExprNode step = null;
    Token byToken = null;
    Token toToken = null;

    if (is(BY)) {
        byToken = expect(BY);
        
        if (is(MUL) || is(DIV)) {
            Token operatorToken = currentToken();
            String operator = operatorToken.text;
            consume();
            
            if (is(INT_LIT) || is(FLOAT_LIT) || is(ID) || is(PLUS) || is(MINUS)) {
                ExprNode operand = expressionParser.parseExpression();
                ExprNode iteratorRef = ASTFactory.createIdentifier(iterator, iteratorToken);
                step = ASTFactory.createBinaryOp(iteratorRef, operator, operand, operatorToken);
            } else {
                throw new ParseError("Expected number or variable after " + operator, 
                    currentToken());
            }
        } 
        else if (is(PLUS) || is(MINUS)) {
            Token operatorToken = currentToken();
            consume();
            
            if (is(currentToken(), INT_LIT, FLOAT_LIT)) {
                ExprNode operand = expressionParser.parsePrimaryExpression();
                if (is(operatorToken, PLUS)) {
                    step = operand;
                } else {
                    step = ASTFactory.createUnaryOp("-", operand, operatorToken);
                }
            } else {
                step = expressionParser.parseExpression();
            }
        }
        else {
            step = expressionParser.parseExpression();
        }
        
        expect(IN);
    } else {
        step = null;
        expect(IN);
    }

    ExprNode source = expressionParser.parseExpression();
    
    if (is(TO)) {
        toToken = expect(TO);
        ExprNode start = source;
        ExprNode end = expressionParser.parseExpression();
        RangeNode range = ASTFactory.createRange(step, start, end, byToken, toToken);
        ForNode forNode = ASTFactory.createFor(iterator, range, forToken, iteratorToken);
        return parseForLoopBody(forNode);
    } else {
        ForNode forNode = ASTFactory.createFor(iterator, source, forToken, iteratorToken);
        return parseForLoopBody(forNode);
    }
  }

  private StmtNode parseForLoopBody(ForNode forNode) {
    if (!is(LBRACE)) {
        if (isControlFlowStatementStart(currentToken())) {
            throw new ParseError(
                "For statement containing another control flow statement must use braces: for ... { ... }",
                currentToken());
        }
    }

    if (is(LBRACE)) {
        expect(LBRACE); // FIX: Consume the {
        while (!is(RBRACE)) forNode.body.statements.add(parseStatement());
        expect(RBRACE);
    } else {
        forNode.body.statements.add(parseStatement());
    }
    return forNode;
}

  private StmtNode parseExitStatement() {
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
        throw new ParseError(
            "Number of variables (" + varNames.size() + ") does not match number of slots (" + slotNames.size() + ")", currentToken());
    }
    
    ReturnSlotAssignmentNode assignment = ASTFactory.createReturnSlotAssignment(varNames, methodCall, assignToken);
    return assignment;
  }

  private StmtNode parseIndexAssignment() {
    Token arrayVarToken = currentToken();
    ExprNode arrayVar = ASTFactory.createIdentifier(expect(ID).text, arrayVarToken);

    IndexAccessNode indexAccess = expressionParser.parseIndexAccessContinuation(arrayVar);
    while (is(LBRACKET)) {
        indexAccess = expressionParser.parseIndexAccessContinuation(indexAccess);
    }
    Token assignToken = expect(ASSIGN);
    ExprNode value = expressionParser.parseExpression();
    
    AssignmentNode assignment = ASTFactory.createAssignment(indexAccess, value, false, assignToken);
    return assignment;
  }

  private StmtNode parseMethodCallStatement() {
    if (isSymbolAt(0, LBRACKET)) {
      List<String> slotNames = expressionParser.parseReturnSlots();
      expect(COLON);
      MethodCallNode methodCall = expressionParser.parseMethodCall();
      methodCall.slotNames = slotNames;
      return methodCall;
    } else {
      MethodCallNode methodCall = expressionParser.parseMethodCall();
      return methodCall;
    }
  }

  private StmtNode parseExpressionStatement() {
    ExprNode expr = expressionParser.parseExpression();
    return expr;
  }

  private List<String> parseIdList() {
    List<String> ids = new ArrayList<String>();
    ids.add(expect(ID).text);
    while (tryConsume(COMMA)) {
        ids.add(expect(ID).text);
    }
    return ids;
  }

  protected boolean isSlotAssignment() {
    Token first = currentToken();
    if (first == null || first.symbol != TILDE_ARROW) return false;

    Token next = peek(1);
    if (next == null) return false;

    return isExpressionStart(next);
  }

  private boolean isIndexAssignment() {
    return lookahead(new ParserAction<Boolean>() {
        @Override
        public Boolean parse() throws ParseError {
            Token first = currentToken();
            Token second = peek(1);
            
            if (first == null || first.type != ID) return false;
            if (second == null || second.symbol != LBRACKET) return false;
            
            // Skip the ID and [
            consume(); // ID
            consume(); // [
            
            int bracketDepth = 1;
            while (!is(EOF) && bracketDepth > 0) {
                Token t = currentToken();
                if (is(t, LBRACKET)) bracketDepth++;
                else if (is(t, RBRACKET)) bracketDepth--;
                consume();
            }
            
            // Check for nested index access
            while (is(LBRACKET)) {
                consume(); // [
                bracketDepth = 1;
                while (!is(EOF) && bracketDepth > 0) {
                    Token t = currentToken();
                    if (is(t, LBRACKET)) bracketDepth++;
                    else if (is(t, RBRACKET)) bracketDepth--;
                    consume();
                }
            }
            
            return is(ASSIGN);
        }
    });
  }

  private boolean isReturnSlotAssignment() {
    return lookahead(new ParserAction<Boolean>() {
        @Override
        public Boolean parse() throws ParseError {
            ParserState startState = getCurrentState();
            
            // Parse variable list
            if (!is(ID)) return false;
            consume(); // first ID
            
            while (is(COMMA)) {
                consume(); // COMMA
                if (!is(ID)) return false;
                consume(); // ID
            }
            
            // Check for assignment operator
            if (!is(ASSIGN, DOUBLE_COLON_ASSIGN)) return false;
            consume(); // ASSIGN or DOUBLE_COLON_ASSIGN
            
            // Check for slot list
            if (!is(LBRACKET)) return false;
            consume(); // [
            
            if (is(RBRACKET)) {
                consume(); // ]
            } else {
                // First slot
                if (!is(ID, INT_LIT)) return false;
                consume(); // slot
                
                // More slots
                while (is(COMMA)) {
                    consume(); // COMMA
                    if (!is(ID, INT_LIT)) return false;
                    consume(); // slot
                }
                
                if (!is(RBRACKET)) return false;
                consume(); // ]
            }
            
            // Check for colon
            if (!is(COLON)) {
                setState(startState);
                return false;
            }
            
            return true;
        }
    });
  }

  private boolean isMethodCallStatement() {
    return lookahead(new ParserAction<Boolean>() {
        @Override
        public Boolean parse() throws ParseError {
            ParserState startState = getCurrentState();
            
            // Check for slot access prefix
            if (is(LBRACKET)) {
                consume(); // [
                
                if (is(RBRACKET)) {
                    consume(); // ]
                } else {
                    // First slot
                    if (!is(ID, INT_LIT)) {
                        setState(startState);
                        return false;
                    }
                    consume(); // slot
                    
                    // More slots
                    while (is(COMMA)) {
                        consume(); // COMMA
                        if (!is(ID, INT_LIT)) {
                            setState(startState);
                            return false;
                        }
                        consume(); // slot
                    }
                    
                    if (!is(RBRACKET)) {
                        setState(startState);
                        return false;
                    }
                    consume(); // ]
                }
                
                // Must have colon after slot list
                if (!is(COLON)) {
                    setState(startState);
                    return false;
                }
                consume(); // :
            }
            
            // Check for method name
            if (!is(ID)) {
                setState(startState);
                return false;
            }
            consume(); // ID
            
            // Check for dotted method names
            while (is(DOT)) {
                consume(); // DOT
                if (!is(ID)) {
                    setState(startState);
                    return false;
                }
                consume(); // ID
            }
            
            // Must have parentheses
            if (!is(LPAREN)) {
                setState(startState);
                return false;
            }
            
            return true;
        }
    });
  }

  private boolean isVariableDeclaration() {
    Token first = currentToken();
    Token second = peek(1);
    
    if (first == null) return false;

    if (is(first, ID) && second != null && is(second, DOUBLE_COLON_ASSIGN))  return true;
    
    if (is(first, ID) && second != null && is(second, COLON)) {
        Token third = peek(2);
        return third != null && isTypeStart(third);
    }
    
    return false;
  }
}