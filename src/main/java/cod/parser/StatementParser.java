package cod.parser;

import cod.ast.ASTFactory;
import cod.semantic.NamingValidator;
import cod.error.ParseError;
import cod.ast.nodes.*;

import java.util.ArrayList;
import java.util.List;

import cod.lexer.MainLexer.Token;
import static cod.lexer.MainLexer.TokenType.*;

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

    // 1. Keyword statements (fast path)
    if (first.type == KEYWORD) {
        String text = first.text;
        if (IF.toString().equals(text)) return parseIfStatement(inheritedStyle);
        if (FOR.toString().equals(text)) return parseForStatement();
        if (OUTPUT.toString().equals(text)) return parseOutputStatement();
        if (EXIT.toString().equals(text)) return parseExitStatement();
    }

    // 2. Symbol statements (Slot Assignment)
    if (first.symbol == TILDE_ARROW) return parseSlotAssignment();

    // 3. Method Call Statement with return slots (starts with [)
    if (first.symbol == LBRACKET && isMethodCallStatement()) {
        return parseMethodCallStatement();
    }

    // 4. Reassignment and Method Call (declarations handled above)
    if (isVariableDeclaration()) {
        return parseVariableDeclaration();
    }
    
    // 5. Declaration, Assignment, or Method Call
    if (first.type == ID) {
        Token second = lookahead(1);

        if (second != null) {
            // Case A: ID [ ... (Index Assignment only)
            if (first.type == ID && second.symbol == LBRACKET) {
                if (isIndexAssignment()) return parseIndexAssignment();
            }

            // Case B: ID = ... (Simple reassignment)
            if (first.type == ID && second.symbol == ASSIGN) {
                // Check specific assignments before falling back to simple
                if (isReturnSlotAssignment()) return parseReturnSlotAssignment();
                if (isInputAssignment()) return parseInputAssignment();
                return parseSimpleAssignment();
            }

            // Case C: ID , ... (Multi-var return slot)
            if (first.type == ID && second.symbol == COMMA) {
                if (isReturnSlotAssignment()) return parseReturnSlotAssignment();
            }
        }

        // Fallback for Calls (f())
        if (isMethodCallStatement()) return parseMethodCallStatement();
    }

    // 6. Everything else (Expression Statement)
    return parseExpressionStatement();
}

  private void checkIllegalDeclaration() {
    Token current = currentToken();
    if (current.type != KEYWORD) return;

    // REMOVED: VAR keyword check
    // Only check if keyword looks like a type start
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

  private SlotAssignmentNode parseSingleSlotAssignment() {
    String slotName = null;
    ExprNode value;
    
    // Check for named slot: "slotName: expression"
    if (currentToken().type == ID) {
        Token afterId = lookahead(1);
        if (afterId != null && afterId.symbol == COLON) {
            // Named slot: "sum: a + b"
            slotName = consume(ID).text;
            consume(COLON);
            value = expressionParser.parseExpression();
        } else {
            // Positional slot: just an expression
            slotName = null;
            value = expressionParser.parseExpression();
        }
    } else {
        // Positional slot: just an expression
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
    
    // REJECT assignment to underscore
    if ("_".equals(idName)) {
        throw new ParseError(
            "Cannot assign to '_'. Underscore is reserved for discard/placeholder.",
            startToken.line, startToken.column
        );
    }
    
    ExprNode target = ASTFactory.createIdentifier(idName);
    consume(ASSIGN);
    ExprNode value = expressionParser.parseExpression();
    AssignmentNode assignment = ASTFactory.createAssignment(target, value);
    return assignment;
}

private StmtNode parseVariableDeclaration() {
    Token startToken = currentToken();
    String typeName = null;
    boolean isImplicit = false;
    String varName = null;
    ExprNode value = null;

    // Case 1: Implicit Typing (ID := Expression)
    if (currentToken().type == ID && lookahead(1) != null && 
        lookahead(1).symbol == DOUBLE_COLON_ASSIGN) {
        
        varName = consume(ID).text;
        
        // REJECT underscore
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
    // Case 2: Explicit Typing (ID : Type [ = Expression])
    else if (currentToken().type == ID && lookahead(1) != null && 
             lookahead(1).symbol == COLON) {
        
        varName = consume(ID).text;
        
        // REJECT underscore
        if ("_".equals(varName)) {
            throw new ParseError(
                "Cannot declare variable '_'. Underscore is reserved for discard/placeholder.",
                startToken.line, startToken.column
            );
        }
        
        consume(COLON);
        
        // MUST have a type after colon
        if (isTypeStart(currentToken())) {
            typeName = parseTypeReference();
            
            // Check for optional assignment
            if (tryConsume(ASSIGN)) {
                value = expressionParser.parseExpression();
            }
        } else {
            throw new ParseError("Expected type after ':' in variable declaration. " +
                                 "For inferred typing use ':=' operator.");
        }
        
    } else {
        // Fallback for unrecognized pattern that wasn't correctly caught by lookahead
        throw new ParseError(
            "Expected variable declaration in format 'name: type', 'name: type = value', or 'name := value'.");
    }

    // Validate variable name (common to both cases)
    if (varName != null) {
        if (NamingValidator.isAllCaps(varName)) {
            NamingValidator.validateConstantName(varName, startToken);
        } else {
            NamingValidator.validateVariableName(varName, startToken);
        }
    }

    // Create and return the VarNode
    VarNode varNode = ASTFactory.createVar(varName, value);
    varNode.explicitType = isImplicit ? null : typeName;
    return varNode;
}

  private StmtNode parseIfStatement(Boolean inheritedStyle) {
    consumeKeyword(IF);
    ExprNode condition = expressionParser.parseExpression();
    StmtIfNode rootIfNode = ASTFactory.createIfStatement(condition);

    Boolean currentStyle = inheritedStyle;

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
        throw new ParseError("Cannot use 'elif' in an 'else if' style chain");
      currentStyle = true;
      ExprNode elifCondition = expressionParser.parseExpression();
      StmtIfNode elifNode = ASTFactory.createIfStatement(elifCondition);
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
      consumeKeyword(ELSE);
      if (isKeyword(IF)) {
        if (currentStyle != null && currentStyle)
          throw new ParseError("Cannot use 'else if' in an 'elif' style chain");
        currentStyle = false;
        currentNode.elseBlock.statements.add(parseIfStatement(currentStyle));
      } else {
        if (match(LBRACE)) {
          consume(LBRACE);
          while (!match(RBRACE)) currentNode.elseBlock.statements.add(parseStatement(currentStyle));
          consume(RBRACE);
        } else {
          currentNode.elseBlock.statements.add(parseStatement(currentStyle));
        }
      }
    }
    return rootIfNode;
  }

// For loop parsing with multiplicative (*2, /2) and additive (+1, -2) step expressions
  private StmtNode parseForStatement() {
    consumeKeyword(FOR);
    String iterator = consume(ID).text;
    ExprNode step = null;

    if (isKeyword(BY)) {
        consumeKeyword(BY);
        
        // KEEP: Multiplicative and division operators (*2, /2)
        if (match(MUL) || match(DIV)) {
            Token operator = consume();
            
            // Handle *2, /2, *stepVar, /stepVar
            if (match(INT_LIT) || match(FLOAT_LIT) || match(ID) || match(PLUS) || match(MINUS)) {
                ExprNode operand = expressionParser.parseExpression();
                // Create a binary operation like i * 2 or i / 2
                ExprNode iteratorRef = ASTFactory.createIdentifier(iterator);
                step = ASTFactory.createBinaryOp(iteratorRef, operator.text, operand);
            } else {
                throw new ParseError("Expected number or variable after " + operator.text, 
                    currentToken().line, currentToken().column);
            }
        } 
        // KEEP: Additive operators with optional sign
        else if (match(PLUS) || match(MINUS)) {
            Token operator = consume();
            
            if (peek(0).type == INT_LIT || peek(0).type == FLOAT_LIT) {
                // Handle +1, -2, etc.
                ExprNode operand = expressionParser.parsePrimaryExpression();
                if (operator.symbol == PLUS) {
                    step = operand;  // Just the number (e.g., 1)
                } else {
                    // Create unary negative (e.g., -2)
                    step = ASTFactory.createUnaryOp("-", operand);
                }
            } else {
                // Just a simple expression (could be variable)
                step = expressionParser.parseExpression();
            }
        }
        // Simple numeric or variable step
        else {
            step = expressionParser.parseExpression();
        }
        
        consumeKeyword(IN);
    } else {
        step = null;
        consumeKeyword(IN);
    }

    ExprNode start = expressionParser.parseExpression();
    consumeKeyword(TO);
    ExprNode end = expressionParser.parseExpression();

    RangeNode range = ASTFactory.createRange(step, start, end);
    ForNode forNode = ASTFactory.createFor(iterator, range);

    consume(LBRACE);
    while (!match(RBRACE)) forNode.body.statements.add(parseStatement());
    consume(RBRACE);
    return forNode;
  }

  private StmtNode parseOutputStatement() {
    consumeKeyword(OUTPUT);
    OutputNode output = ASTFactory.createOutput();
    output.arguments.add(expressionParser.parseExpression());
    return output;
  }

  private StmtNode parseInputAssignment() {
  Token startToken = currentToken();
    String varName = consume(ID).text;
    consume(ASSIGN);
    String type;
    if (match(LPAREN)) {
      consume(LPAREN);
      type = parseTypeReference();
      consume(RPAREN);
    } else {
      throw new ParseError(
          "Input assignment requires an explicit type grouping, e.g., var = (int)input",
          startToken.line,
          startToken.column);
    }
    consumeKeyword(INPUT);
    InputNode input = ASTFactory.createInput(type, varName);
    return input;
  }

  private StmtNode parseExitStatement() {
    consumeKeyword(EXIT);
    ExitNode exit = ASTFactory.createExit();
    return exit;
  }

  private StmtNode parseReturnSlotAssignment() {
    List<String> varNames = parseIdList();
    
    // Accept both = and := for return slot assignment
    if (match(DOUBLE_COLON_ASSIGN)) {
        consume(DOUBLE_COLON_ASSIGN);
    } else {
        consume(ASSIGN);
    }
    
    List<String> slotNames = expressionParser.parseReturnSlots();
    consume(COLON);
    MethodCallNode methodCall = expressionParser.parseMethodCall();
    methodCall.slotNames = slotNames;
    
    // Validate variable names (allow underscore for discard)
    for (String varName : varNames) {
        if ("_".equals(varName)) {
            continue; // Underscore is allowed here - it means discard this slot
        }
        // Validate other names normally
    }
    
    if (varNames.size() != slotNames.size()) {
        throw new ParseError(
            "Number of variables (" + varNames.size() + ") does not match number of slots (" + slotNames.size() + ")");
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
    AssignmentNode assignment = ASTFactory.createAssignment(indexAccess, value);
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

  // --- Lookahead Methods (OPTIMIZED) ---

  protected boolean isSlotAssignment() {
    Token first = lookahead(0);
    if (first == null || first.symbol != TILDE_ARROW) return false;

    Token next = lookahead(1);
    if (next == null) return false;

    // Check if the next token is a possible start of an expression/literal
    return isExpressionStart(next);
  }

  // SIMPLIFIED: Uses simpler token-based lookahead instead of complex loop and exception catching
  private boolean isIndexAssignment() {
    Token first = lookahead(0);
    Token second = lookahead(1);

    if (first == null || first.type == ID) {
    // Continue to next
    } else {
         return false;
    }

    if (second == null || second.symbol != LBRACKET) return false;

    // REMOVED: Case 1 (Empty brackets) check.
    // Now only checks if it looks like an index access (name[...)

    // Case 2: Index access: name[expr]... = value
    int pos = getPosition() + 2; // Start after ID [
    int bracketDepth = 1;

    // Look for the matching closing bracket for the first index
    while (pos < tokens.size() && bracketDepth > 0) {
      Token t = tokens.get(pos);
      if (t.symbol == LBRACKET) bracketDepth++;
      else if (t.symbol == RBRACKET) bracketDepth--;
      pos++;
    }

    // If we found a matching bracket
    if (bracketDepth == 0) {
      // Check for subsequent index accesses or assignment
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

    // 1. Variable list (ID, ID...)
    Token t = tokens.get(p++);
    if (t.type != ID) return false;
    
    while (p < tokens.size() && tokens.get(p).symbol == COMMA) {
        p++; // Skip comma
        if (p >= tokens.size() || tokens.get(p).type != ID) return false;
        p++; // Skip ID
    }

    // 2. Assignment ( = OR := )
    if (p >= tokens.size()) return false;
    
    // --- FIX START: Allow both ASSIGN and DOUBLE_COLON_ASSIGN ---
    if (tokens.get(p).symbol != ASSIGN && tokens.get(p).symbol != DOUBLE_COLON_ASSIGN) return false;
    // --- FIX END ---
    
    p++;

    // 3. Slot list ([...])
    if (p >= tokens.size() || tokens.get(p).symbol != LBRACKET) return false;
    p++;

    // Check for at least one slot
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

    // 4. Colon (:)
    return p < tokens.size() && tokens.get(p).symbol == COLON;
}

  // Original logic was sufficient, minor cleanup
  private boolean isInputAssignment() {
    int p = getPosition();

    if (tokens.get(p++).type != ID) return false;
    if (p >= tokens.size() || tokens.get(p).symbol != ASSIGN) return false;
    p++;
    if (p >= tokens.size() || tokens.get(p).symbol != LPAREN) return false;
    p++;

    int parenBalance = 1;
    while (p < tokens.size() && parenBalance > 0) {
      Token t = tokens.get(p);
      if (t.symbol == LPAREN) parenBalance++;
      else if (t.symbol == RPAREN) parenBalance--;
      p++;
    }

    if (parenBalance != 0) return false; // Type grouping must close

    return p < tokens.size() && isKeywordAt(p - getPosition(), INPUT);
  }

  // Original logic was sufficient, minor cleanup
  private boolean isMethodCallStatement() {
    int p = getPosition();

    if (isSymbolAt(0, LBRACKET)) {
      // Check for [slots] : call()
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

    // Check for qualified name/ID
    if (p >= tokens.size() || tokens.get(p).type != ID) return false;
    p++;
    while (p < tokens.size() && tokens.get(p).symbol == DOT) {
      p++;
      if (p >= tokens.size() || tokens.get(p).type != ID) return false;
      p++;
    }

    // Must be followed by (
    return p < tokens.size() && tokens.get(p).symbol == LPAREN;
  }

private boolean isVariableDeclaration() {
    Token first = lookahead(0);
    Token second = lookahead(1);
    
    if (first == null) return false;

    // Case 1: name := value (implicit typing)
    if (first.type == ID && second != null && second.symbol == DOUBLE_COLON_ASSIGN) {
        return true;
    }
    
    // Case 2: name: type or name: type = value
    if (first.type == ID && second != null && second.symbol == COLON) {
        int pos = getPosition() + 2; // Skip ID and COLON
        if (pos < tokens.size()) {
            Token third = tokens.get(pos);
            return isTypeStart(third);
        }
    }
    
    return false;
}
}