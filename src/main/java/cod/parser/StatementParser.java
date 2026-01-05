package cod.parser;

import cod.ast.ASTFactory;
import cod.semantic.NamingValidator;
import cod.error.ParseError;
import cod.ast.nodes.*;

import java.util.ArrayList;
import java.util.List;

import cod.lexer.Token;
import static cod.lexer.TokenType.*;

import static cod.syntax.Symbol.*;
import static cod.syntax.Keyword.*;

public class StatementParser extends BaseParser {

  public final ExpressionParser expressionParser;

  public StatementParser(
      List<Token> tokens, PositionHolder position, ExpressionParser expressionParser) {
    super(tokens, position);
    this.expressionParser = expressionParser;
  }

  public StmtNode parseStatement() {
    return parseStatement(null);
  }

  private StmtNode parseStatement(Boolean inheritedStyle) {
    checkIllegalDeclaration();

    Token first = currentToken();

    if (first.type == KEYWORD) {
        String text = first.text;
        if (IF.toString().equals(text)) return parseIfStatement(inheritedStyle);
        if (FOR.toString().equals(text)) return parseForStatement();
        if (EXIT.toString().equals(text)) return parseExitStatement();
        if (SKIP.toString().equals(text)) return parseSkipStatement();
        if (BREAK.toString().equals(text)) return parseBreakStatement();
    }

    if (first.symbol == TILDE_ARROW) return parseSlotAssignment();

    if (first.symbol == LBRACKET && isMethodCallStatement()) {
        return parseMethodCallStatement();
    }

    if (isVariableDeclaration()) {
        return parseVariableDeclaration();
    }
    
    if (first.type == ID) {
        Token second = lookahead(1);

        if (second != null) {
            if (first.type == ID && second.symbol == LBRACKET) {
                if (isIndexAssignment()) return parseIndexAssignment();
            }

            if (first.type == ID && second.symbol == ASSIGN) {
                if (isReturnSlotAssignment()) return parseReturnSlotAssignment();
                return parseSimpleAssignment();
            }

            if (first.type == ID && second.symbol == COMMA) {
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
    if (idToken != null && idToken.type == ID) {
        Token afterId = peek(2);
        if (afterId != null) {
            if (afterId.symbol == ASSIGN) {
                throw new ParseError(
                    "Illegally used reserved keyword '"
                        + current.text
                        + "' for declaration of variable '"
                        + idToken.text
                        + "'",
                    current.line,
                    idToken.column);
            }
            if (afterId.symbol == LBRACKET && peek(3) != null && peek(3).symbol == RBRACKET) {
                throw new ParseError(
                    "Illegally used reserved keyword '"
                        + current.text
                        + "' for declaration of variable '"
                        + idToken.text
                        + "'",
                    current.line,
                    idToken.column);
            }
        }
    }
}

private StmtNode parseSkipStatement() {
    Token skipToken = currentToken();
    
    consumeKeyword(SKIP);
    
    skipWhitespaceAndComments();
    
    Token nextToken = currentToken();
    
    if (nextToken.type == EOF || nextToken.symbol == RBRACE) {
    } else if (isStatementStart(nextToken)) {
    } else {
        throw new ParseError(
            "Nothing can follow 'skip' in the same statement. " +
            "'skip' must be the complete statement.",
            skipToken.line,
            skipToken.column
        );
    }
    
    return ASTFactory.createSkipStatement();
}

private StmtNode parseBreakStatement() {
    Token breakToken = currentToken();
    
    consumeKeyword(BREAK);
    
    skipWhitespaceAndComments();
    
    Token nextToken = currentToken();
    
    if (nextToken.type == EOF || nextToken.symbol == RBRACE) {
    } else if (isStatementStart(nextToken)) {
    } else {
        throw new ParseError(
            "Nothing can follow 'break' in the same statement. " +
            "'break' must be the complete statement.",
            breakToken.line,
            breakToken.column
        );
    }
    
    return ASTFactory.createBreakStatement();
}

private boolean isStatementStart(Token token) {
    if (token == null) return false;
    
    if (token.type == KEYWORD) {
        String text = token.text;
        return text.equals(IF.toString()) || 
               text.equals(FOR.toString()) || 
               text.equals(EXIT.toString()) ||
               text.equals(ELSE.toString()) ||
               text.equals(ELIF.toString()) ||
               text.equals(SKIP.toString()) ||
               text.equals(BREAK.toString()) ||
               text.equals(SHARE.toString()) ||
               text.equals(LOCAL.toString());
    }
    
    if (token.type == ID) {
        Token next = lookahead(1);
        if (next != null) {
            return next.symbol == COLON || 
                   next.symbol == ASSIGN || 
                   next.symbol == DOUBLE_COLON_ASSIGN ||
                   next.symbol == LBRACKET;
        }
    }
    
    if (token.symbol == TILDE_ARROW) return true;
    
    if (token.symbol == LBRACKET) {
        return isMethodCallStatement();
    }
    
    return false;
}

private void skipWhitespaceAndComments() {
    while (position.get() < tokens.size()) {
        Token t = currentToken();
        if (t.type == WS || t.type == LINE_COMMENT || t.type == BLOCK_COMMENT) {
            consume();
        } else {
            break;
        }
    }
}

private boolean isStatementTerminator(Token token) {
    if (token == null) return true;
    if (token.type == EOF) return true;
    
    if (token.symbol == RBRACE) return true;
    
    if (token.type == KEYWORD) {
        String text = token.text;
        return text.equals(IF.toString()) || 
               text.equals(FOR.toString()) || 
               text.equals(EXIT.toString()) ||
               text.equals(ELSE.toString()) ||
               text.equals(ELIF.toString()) ||
               text.equals(SKIP.toString()) ||
               text.equals(BREAK.toString()) ||
               text.equals(SHARE.toString()) ||
               text.equals(LOCAL.toString());
    }
    
    if (token.type == ID) {
        Token next = lookahead(1);
        if (next != null) {
            if (next.symbol == COLON || next.symbol == ASSIGN || 
                next.symbol == DOUBLE_COLON_ASSIGN) {
                return true;
            }
        }
    }
    
    if (token.symbol == TILDE_ARROW) return true;
    
    if (token.symbol == LBRACKET) {
        if (isMethodCallStatement()) {
            return true;
        }
    }
    
    return false;
}

  private SlotAssignmentNode parseSingleSlotAssignment() {
    String slotName = null;
    ExprNode value;
    
    if (currentToken().type == ID) {
        Token afterId = lookahead(1);
        if (afterId != null && afterId.symbol == COLON) {
            slotName = consume(ID).text;
            consume(COLON);
            value = expressionParser.parseExpression();
        } else {
            slotName = null;
            value = expressionParser.parseExpression();
        }
    } else {
        slotName = null;
        value = expressionParser.parseExpression();
    }
    
    return ASTFactory.createSlotAssignment(slotName, value); 
}

  private StmtNode parseSlotAssignment() {
    consume(TILDE_ARROW);
    List<SlotAssignmentNode> assignments = new ArrayList<SlotAssignmentNode>();
    assignments.add(parseSingleSlotAssignment());
    if (match(COMMA)) {
      while (tryConsume(COMMA)) assignments.add(parseSingleSlotAssignment());
      MultipleSlotAssignmentNode multiAssign = ASTFactory.createMultipleSlotAssignment(assignments);
      return multiAssign;
    } else {
      SlotAssignmentNode assignment = assignments.get(0);
      return assignment;
    }
  }

private StmtNode parseSimpleAssignment() {
    Token startToken = currentToken();
    String idName = consume(ID).text;
    
    if ("_".equals(idName)) {
        throw new ParseError(
            "Cannot assign to '_'. Underscore is reserved for discard/placeholder.",
            startToken.line, startToken.column
        );
    }
    
    ExprNode target = ASTFactory.createIdentifier(idName);
    consume(ASSIGN);
    ExprNode value = expressionParser.parseExpression();
    
    // NEW: Create assignment with isDeclaration = false
    AssignmentNode assignment = ASTFactory.createAssignment(target, value, false);
    return assignment;
}

private StmtNode parseVariableDeclaration() {
    Token startToken = currentToken();
    String typeName = null;
    boolean isImplicit = false;
    String varName = null;
    ExprNode value = null;

    // Check for varName := value
    if (currentToken().type == ID && lookahead(1) != null && 
        lookahead(1).symbol == DOUBLE_COLON_ASSIGN) {
        
        varName = consume(ID).text;
        
        if ("_".equals(varName)) {
            throw new ParseError(
                "Cannot declare variable '_'. Underscore is reserved for discard/placeholder.",
                startToken.line, startToken.column
            );
        }
        
        consume(DOUBLE_COLON_ASSIGN);
        isImplicit = true;
        value = expressionParser.parseExpression();
        
    }
    // Check for varName: type [= value]
    else if (currentToken().type == ID && lookahead(1) != null && lookahead(1).symbol == COLON) {
        
        varName = consume(ID).text;
        
        if ("_".equals(varName)) {
            throw new ParseError(
                "Cannot declare variable '_'. Underscore is reserved for discard/placeholder",
                startToken.line, startToken.column
            );
        }
        
        consume(COLON);
        
        if (isTypeStart(currentToken())) {
            typeName = parseTypeReference();
            
            if (tryConsume(ASSIGN)) {
                value = expressionParser.parseExpression();
            }
        } else {
            throw new ParseError("Expected type after ':' in variable declaration. " +
                                 "For inferred typing use ':=' operator", startToken.line, startToken.column);
        }
        
    } else {
        throw new ParseError(
            "Expected variable declaration in format 'name: type', 'name: type = value', or 'name := value'", 
            startToken.line, startToken.column);
    }

    if (varName != null) {
        if (NamingValidator.isAllCaps(varName)) {
            NamingValidator.validateConstantName(varName, startToken);
        } else {
            NamingValidator.validateVariableName(varName, startToken);
        }
    }

    VarNode varNode = ASTFactory.createVar(varName, value);
    varNode.explicitType = isImplicit ? null : typeName;
    return varNode;
}

  private StmtNode parseIfStatement(Boolean inheritedStyle) {
    consumeKeyword(IF);
    ExprNode condition = expressionParser.parseExpression();
    StmtIfNode rootIfNode = ASTFactory.createIfStatement(condition);

    Boolean currentStyle = inheritedStyle;

    if (!match(LBRACE)) {
        if (lookaheadIsIfOrElif()) {
            throw new ParseError(
                "If statement containing another if or elif must use braces: if condition { ... }",
                currentToken().line, currentToken().column
            );
        }
    }

    if (match(LBRACE)) {
      consume(LBRACE);
      while (!match(RBRACE)) rootIfNode.thenBlock.statements.add(parseStatement(currentStyle));
      consume(RBRACE);
    } else {
      rootIfNode.thenBlock.statements.add(parseStatement(currentStyle));
    }

    StmtIfNode currentNode = rootIfNode;

    while (isKeyword(ELIF)) {
      consumeKeyword(ELIF);
      if (currentStyle != null && !currentStyle)
        throw new ParseError("Cannot use 'elif' in an 'else if' style chain", currentToken().line, currentToken().column);
      currentStyle = true;
      ExprNode elifCondition = expressionParser.parseExpression();
      StmtIfNode elifNode = ASTFactory.createIfStatement(elifCondition);
      
      if (!match(LBRACE)) {
          if (lookaheadIsIfOrElif()) {
              throw new ParseError(
                  "Elif statement containing another if or elif must use braces: elif condition { ... }",
                  currentToken().line, currentToken().column
              );
          }
      }
      
      if (match(LBRACE)) {
        consume(LBRACE);
        while (!match(RBRACE)) elifNode.thenBlock.statements.add(parseStatement(currentStyle));
        consume(RBRACE);
      } else {
        elifNode.thenBlock.statements.add(parseStatement(currentStyle));
      }
      currentNode.elseBlock.statements.add(elifNode);
      currentNode = elifNode;
    }

    if (isKeyword(ELSE)) {
        Token elseToken = currentToken();
        consumeKeyword(ELSE);
        
        int savedPos = getPosition();
        
        while (position.get() < tokens.size()) {
            Token t = currentToken();
            if (t.type == WS || t.type == LINE_COMMENT || t.type == BLOCK_COMMENT) {
                consume();
            } else {
                break;
            }
        }
        
        if (isKeyword(IF)) {
            if (currentStyle != null && currentStyle)
                throw new ParseError("Cannot use 'else if' in an 'elif' style chain", currentToken().line, currentToken().column);
            currentStyle = false;
            currentNode.elseBlock.statements.add(parseIfStatement(currentStyle));
        } else {
            position.set(savedPos);
            
            if (isKeyword(IF)) {
                if (currentStyle != null && currentStyle)
                    throw new ParseError("Cannot use 'else if' in an 'elif' style chain", currentToken().line, currentToken().column);
                currentStyle = false;
                currentNode.elseBlock.statements.add(parseIfStatement(currentStyle));
            } else {
                if (!match(LBRACE)) {
                    if (isControlFlowStatementStart(currentToken())) {
                        throw new ParseError(
                            "Else statement containing another control flow statement must use braces: else { ... }",
                            currentToken().line, currentToken().column
                        );
                    }
                }
                
                if (match(LBRACE)) {
                    consume(LBRACE);
                    while (!match(RBRACE)) currentNode.elseBlock.statements.add(parseStatement(currentStyle));
                    consume(RBRACE);
                } else {
                    currentNode.elseBlock.statements.add(parseStatement(currentStyle));
                }
            }
        }
    }
    return rootIfNode;
}

  private boolean lookaheadIsIfOrElif() {
      int savedPos = getPosition();
      try {
          while (position.get() < tokens.size()) {
              Token t = currentToken();
              if (t.type == WS || t.type == LINE_COMMENT || t.type == BLOCK_COMMENT) {
                  consume();
              } else {
                  break;
              }
          }
          
          return isControlFlowStatementStart(currentToken());
      } finally {
          position.set(savedPos);
      }
  }

private boolean isControlFlowStatementStart(Token token) {
    if (token == null) return false;
    
    if (token.type == KEYWORD) {
        String text = token.text;
        return text.equals(IF.toString()) || 
               text.equals(FOR.toString()) ||
               text.equals(ELSE.toString()) ||
               text.equals(ELIF.toString());
    }
    return false;
}

  private StmtNode parseForStatement() {
    consumeKeyword(FOR);
    String iterator = consume(ID).text;
    ExprNode step = null;

    if (isKeyword(BY)) {
        consumeKeyword(BY);
        
        if (match(MUL) || match(DIV)) {
            Token operator = consume();
            
            if (match(INT_LIT) || match(FLOAT_LIT) || match(ID) || match(PLUS) || match(MINUS)) {
                ExprNode operand = expressionParser.parseExpression();
                ExprNode iteratorRef = ASTFactory.createIdentifier(iterator);
                step = ASTFactory.createBinaryOp(iteratorRef, operator.text, operand);
            } else {
                throw new ParseError("Expected number or variable after " + operator.text, 
                    currentToken().line, currentToken().column);
            }
        } 
        else if (match(PLUS) || match(MINUS)) {
            Token operator = consume();
            
            if (peek(0).type == INT_LIT || peek(0).type == FLOAT_LIT) {
                ExprNode operand = expressionParser.parsePrimaryExpression();
                if (operator.symbol == PLUS) {
                    step = operand;
                } else {
                    step = ASTFactory.createUnaryOp("-", operand);
                }
            } else {
                step = expressionParser.parseExpression();
            }
        }
        else {
            step = expressionParser.parseExpression();
        }
        
        consumeKeyword(IN);
    } else {
        step = null;
        consumeKeyword(IN);
    }

    // Parse the iteration source
    ExprNode source = expressionParser.parseExpression();
    
    // Check if it's a range (has TO keyword)
    if (isKeyword(TO)) {
        // Traditional range: source to end
        ExprNode start = source;
        consumeKeyword(TO);
        ExprNode end = expressionParser.parseExpression();
        RangeNode range = ASTFactory.createRange(step, start, end);
        ForNode forNode = ASTFactory.createFor(iterator, range);
        return parseForLoopBody(forNode);
    } else {
        // Array-based iteration
        ForNode forNode = ASTFactory.createFor(iterator, source); // NEW: pass source as second param
        return parseForLoopBody(forNode);
    }
}

private StmtNode parseForLoopBody(ForNode forNode) {
    if (!match(LBRACE)) {
        if (isControlFlowStatementStart(currentToken())) {
            throw new ParseError(
                "For statement containing another control flow statement must use braces: for ... { ... }",
                currentToken().line, currentToken().column
            );
        }
    }

    if (match(LBRACE)) {
        consume(LBRACE);
        while (!match(RBRACE)) forNode.body.statements.add(parseStatement());
        consume(RBRACE);
    } else {
        forNode.body.statements.add(parseStatement());
    }
    return forNode;
}

  private StmtNode parseExitStatement() {
    consumeKeyword(EXIT);
    ExitNode exit = ASTFactory.createExit();
    return exit;
  }

  private StmtNode parseReturnSlotAssignment() {
    List<String> varNames = parseIdList();
    
    if (match(DOUBLE_COLON_ASSIGN)) {
        consume(DOUBLE_COLON_ASSIGN);
    } else {
        consume(ASSIGN);
    }
    
    List<String> slotNames = expressionParser.parseReturnSlots();
    consume(COLON);
    MethodCallNode methodCall = expressionParser.parseMethodCall();
    methodCall.slotNames = slotNames;
    
    for (String varName : varNames) {
        if ("_".equals(varName)) {
            continue;
        }
    }
    
    if (varNames.size() != slotNames.size()) {
        throw new ParseError(
            "Number of variables (" + varNames.size() + ") does not match number of slots (" + slotNames.size() + ")", currentToken().line, currentToken().column);
    }
    
    ReturnSlotAssignmentNode assignment = ASTFactory.createReturnSlotAssignment(varNames, methodCall);
    return assignment;
}

  private StmtNode parseIndexAssignment() {
    ExprNode arrayVar = ASTFactory.createIdentifier(consume(ID).text);

    IndexAccessNode indexAccess = expressionParser.parseIndexAccessContinuation(arrayVar);
    while (match(LBRACKET)) {
        indexAccess = expressionParser.parseIndexAccessContinuation(indexAccess);
    }
    consume(ASSIGN);
    ExprNode value = expressionParser.parseExpression();
    
    // NEW: Index assignments are never declarations (isDeclaration = false)
    AssignmentNode assignment = ASTFactory.createAssignment(indexAccess, value, false);
    return assignment;
}

  private StmtNode parseMethodCallStatement() {
    if (isSymbolAt(0, LBRACKET)) {
      List<String> slotNames = expressionParser.parseReturnSlots();
      consume(COLON);
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
    ids.add(consume(ID).text);
    while (tryConsume(COMMA)) ids.add(consume(ID).text);
    return ids;
  }

  protected boolean isSlotAssignment() {
    Token first = lookahead(0);
    if (first == null || first.symbol != TILDE_ARROW) return false;

    Token next = lookahead(1);
    if (next == null) return false;

    return isExpressionStart(next);
  }

  private boolean isIndexAssignment() {
    Token first = lookahead(0);
    Token second = lookahead(1);

    if (first == null || first.type == ID) {
    } else {
         return false;
    }

    if (second == null || second.symbol != LBRACKET) return false;

    int pos = getPosition() + 2;
    int bracketDepth = 1;

    while (pos < tokens.size() && bracketDepth > 0) {
      Token t = tokens.get(pos);
      if (t.symbol == LBRACKET) bracketDepth++;
      else if (t.symbol == RBRACKET) bracketDepth--;
      pos++;
    }

    if (bracketDepth == 0) {
      while (pos < tokens.size() && tokens.get(pos).symbol == LBRACKET) {
        pos++;
        bracketDepth = 1;
        while (pos < tokens.size() && bracketDepth > 0) {
          Token t = tokens.get(pos);
          if (t.symbol == LBRACKET) bracketDepth++;
          else if (t.symbol == RBRACKET) bracketDepth--;
          pos++;
        }
        if (bracketDepth != 0) return false;
      }
      return pos < tokens.size() && tokens.get(pos).symbol == ASSIGN;
    }

    return false;
  }

private boolean isReturnSlotAssignment() {
    int p = getPosition();

    Token t = tokens.get(p++);
    if (t.type != ID) return false;
    
    while (p < tokens.size() && tokens.get(p).symbol == COMMA) {
        p++;
        if (p >= tokens.size() || tokens.get(p).type != ID) return false;
        p++;
    }

    if (p >= tokens.size()) return false;
    
    if (tokens.get(p).symbol != ASSIGN && tokens.get(p).symbol != DOUBLE_COLON_ASSIGN) return false;
    
    p++;

    if (p >= tokens.size() || tokens.get(p).symbol != LBRACKET) return false;
    p++;

    if (p >= tokens.size()) return false;
    Token firstSlot = tokens.get(p);
    if (firstSlot.type == ID || firstSlot.type == INT_LIT) {
        p++;
    } else if (firstSlot.symbol != RBRACKET) {
        return false;
    }

    while (p < tokens.size() && tokens.get(p).symbol == COMMA) {
        p++;
        if (p >= tokens.size()) return false;
        Token nextSlot = tokens.get(p);
        if (nextSlot.type == ID || nextSlot.type == INT_LIT) {
            p++;
        } else {
            return false;
        }
    }

    if (p >= tokens.size() || tokens.get(p).symbol != RBRACKET) return false;
    p++;

    return p < tokens.size() && tokens.get(p).symbol == COLON;
}

  private boolean isMethodCallStatement() {
    int p = getPosition();

    if (isSymbolAt(0, LBRACKET)) {
      p++;
      if (p >= tokens.size() || (tokens.get(p).type != ID && tokens.get(p).type != INT_LIT))
        return false;
      p++;
      while (p < tokens.size() && tokens.get(p).symbol == COMMA) {
        p++;
        if (p >= tokens.size() || (tokens.get(p).type != ID && tokens.get(p).type != INT_LIT))
          return false;
        p++;
      }
      if (p >= tokens.size() || tokens.get(p).symbol != RBRACKET) return false;
      p++;
      if (p >= tokens.size() || tokens.get(p).symbol != COLON) return false;
      p++;
    }

    if (p >= tokens.size() || tokens.get(p).type != ID) return false;
    p++;
    while (p < tokens.size() && tokens.get(p).symbol == DOT) {
      p++;
      if (p >= tokens.size() || tokens.get(p).type != ID) return false;
      p++;
    }

    return p < tokens.size() && tokens.get(p).symbol == LPAREN;
  }

private boolean isVariableDeclaration() {
    Token first = lookahead(0);
    Token second = lookahead(1);
    
    if (first == null) return false;

    if (first.type == ID && second != null && second.symbol == DOUBLE_COLON_ASSIGN) {
        return true;
    }
    
    if (first.type == ID && second != null && second.symbol == COLON) {
        int pos = getPosition() + 2;
        if (pos < tokens.size()) {
            Token third = tokens.get(pos);
            return isTypeStart(third);
        }
    }
    
    return false;
}
}