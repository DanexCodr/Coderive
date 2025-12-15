package cod.ast;

import cod.ast.nodes.*;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseASTVisitor<T> implements ASTVisitor<T> {

  protected T defaultVisit(ASTNode node) {
    return null;
  }

  @Override
  public T visit(ProgramNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(UnitNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(UseNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(TypeNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(FieldNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(MethodNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(ParamNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(ConstructorNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(BlockNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(AssignmentNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(VarNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(StmtIfNode node) {
    return node.accept(this);
  }
  
 @Override
  public T visit(ExprIfNode node) {
    return node.accept(this);
  }
  

  @Override
  public T visit(ForNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(RangeNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(OutputNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(InputNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(ExitNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(TupleNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(ReturnSlotAssignmentNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(SlotDeclarationNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(SlotAssignmentNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(MultipleSlotAssignmentNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(ExprNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(BinaryOpNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(UnaryNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(TypeCastNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(MethodCallNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(ArrayNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(IndexAccessNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(EqualityChainNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(BooleanChainNode node) {
    return node.accept(this);
  }

  @Override
  public T visit(SlotNode node) {
    return node.accept(this);
  }

  @Override
  public final T visit(ASTNode node) {
    // This is the entry point - delegate to the node's accept method
    return node.accept(this);
  }

  @Override
  public List<T> visitList(List<? extends ASTNode> nodes) {
    List<T> results = new ArrayList<T>();
    for (ASTNode node : nodes) {
      results.add(visit(node));
    }
    return results;
  }

  @Override
  public void visitAll(List<? extends ASTNode> nodes) {
    for (ASTNode node : nodes) {
      visit(node);
    }
  }

  // Helper method to dispatch via accept() - this is what should be used in InterpreterVisitor
  public T dispatch(ASTNode node) {
    return node.accept(this);
  }
}
