package cod.semantic;

import cod.ast.nodes.*;
import cod.error.ParseError;
import cod.lexer.Token;

public final class ReturnContractValidator {

  private ReturnContractValidator() {}

  public static void validateMethodReturnContract(Method method, Type currentClass, Token startToken) {
    if (method == null || method.returnSlots == null || method.returnSlots.isEmpty()) {
      return;
    }

    validateNoStatementsAfterReturn(new Block(method.body), method.methodName, startToken);

    boolean hasSlotAssignments = false;
    for (Stmt stmt : method.body) {
      if (hasSlotAssignmentsInBody(stmt)) {
        hasSlotAssignments = true;
        break;
      }
    }

    if (!hasSlotAssignments) {
      String context = "";
      if (currentClass != null) {
        context = "class '" + currentClass.name + "' ";
      } else if (method.associatedClass != null && !method.associatedClass.isEmpty()) {
        context = "class '" + method.associatedClass + "' ";
      }

      throw error(
          "Method '" + method.methodName + "' of " + context + "has return contract (::) but no ~> assignments in body.\n"
              + "Use '~> slot: value' or '~> value' to return values.",
          startToken);
    }
  }

  private static boolean hasSlotAssignmentsInBody(Stmt stmt) {
    if (stmt instanceof SlotAssignment || stmt instanceof MultipleSlotAssignment) {
      return true;
    }
    if (stmt instanceof Block) {
      Block block = (Block) stmt;
      for (Stmt child : block.statements) {
        if (hasSlotAssignmentsInBody(child)) {
          return true;
        }
      }
    }
    if (stmt instanceof StmtIf) {
      StmtIf ifNode = (StmtIf) stmt;
      if (hasSlotAssignmentsInBody(ifNode.thenBlock)) {
        return true;
      }
      if (ifNode.elseBlock != null && hasSlotAssignmentsInBody(ifNode.elseBlock)) {
        return true;
      }
    }
    if (stmt instanceof For) {
      For forNode = (For) stmt;
      if (hasSlotAssignmentsInBody(forNode.body)) {
        return true;
      }
    }
    return false;
  }

  private static void validateNoStatementsAfterReturn(Block block, String methodName, Token startToken) {
    if (block == null || block.statements == null) {
      return;
    }

    boolean foundReturn = false;
    int returnIndex = -1;

    for (int i = 0; i < block.statements.size(); i++) {
      Stmt stmt = block.statements.get(i);

      if (stmt instanceof SlotAssignment || stmt instanceof MultipleSlotAssignment) {
        if (foundReturn) {
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

    if (foundReturn && returnIndex < block.statements.size() - 1) {
      Stmt deadCodeStmt = block.statements.get(returnIndex + 1);
      Token deadCodeToken = deadCodeStmt.getSourceSpan() != null ?
          deadCodeStmt.getSourceSpan().getErrorToken() : startToken;

      throw error(
          "Method '" + methodName + "' has return contract (::) but has dead code after ~>.\n" +
              "The ~> statement is the return point - any code after it will never execute.\n" +
              "Remove the dead code or move it before the ~> statement.",
          deadCodeToken);
    }

    for (Stmt stmt : block.statements) {
      if (stmt instanceof Block) {
        validateNoStatementsAfterReturn((Block) stmt, methodName, startToken);
      } else if (stmt instanceof StmtIf) {
        StmtIf ifNode = (StmtIf) stmt;
        validateNoStatementsAfterReturn(ifNode.thenBlock, methodName, startToken);
        if (ifNode.elseBlock != null) {
          validateNoStatementsAfterReturn(ifNode.elseBlock, methodName, startToken);
        }
      } else if (stmt instanceof For) {
        For forNode = (For) stmt;
        validateNoStatementsAfterReturn(forNode.body, methodName, startToken);
      }
    }
  }

  private static ParseError error(String message, Token token) {
    if (token != null) {
      return new ParseError(message, token);
    }
    return new ParseError(message, 1, 1);
  }
}
