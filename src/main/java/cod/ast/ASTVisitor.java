package cod.ast;

import cod.ast.nodes.*;
import java.util.ArrayList;
import java.util.List;

public abstract class ASTVisitor<T> implements VisitorImpl<T> {

  protected T defaultVisit(ASTNode n) {
    return null;
  }

  @Override
  public T visit(ProgramNode n) {
    return n.accept(this);
  }

  @Override
  public T visit(UnitNode n) {
    return n.accept(this);
  }

  @Override
  public T visit(UseNode n) {
    return n.accept(this);
  }

  @Override
  public T visit(TypeNode n) {
    return n.accept(this);
  }

  @Override
  public T visit(FieldNode n) {
    return n.accept(this);
  }

  @Override
  public T visit(MethodNode n) {
    return n.accept(this);
  }

  @Override
  public T visit(ParamNode n) {
    return n.accept(this);
  }

  @Override
  public T visit(ConstructorNode n) {
    return n.accept(this);
  }
  
  @Override
  public T visit(ConstructorCallNode n) {
    return n.accept(this);
  }

  @Override
  public T visit(BlockNode n) {
    return n.accept(this);
  }

  @Override
  public T visit(AssignmentNode n) {
    return n.accept(this);
  }

  @Override
  public T visit(VarNode n) {
    return n.accept(this);
  }

  @Override
  public T visit(StmtIfNode n) {
    return n.accept(this);
  }
  
 @Override
  public T visit(ExprIfNode n) {
    return n.accept(this);
  }
  

  @Override
  public T visit(ForNode n) {
    return n.accept(this);
  }
  
  @Override
  public T visit(SkipNode n) {
    return n.accept(this);
  }
  
  @Override
  public T visit(BreakNode n) {
    return n.accept(this);
  }

  @Override
  public T visit(RangeNode n) {
    return n.accept(this);
  }

  @Override
  public T visit(ExitNode n) {
    return n.accept(this);
  }

  @Override
  public T visit(TupleNode n) {
    return n.accept(this);
  }

  @Override
  public T visit(ReturnSlotAssignmentNode n) {
    return n.accept(this);
  }

  @Override
  public T visit(SlotDeclarationNode n) {
    return n.accept(this);
  }

  @Override
  public T visit(SlotAssignmentNode n) {
    return n.accept(this);
  }

  @Override
  public T visit(MultipleSlotAssignmentNode n) {
    return n.accept(this);
  }

  @Override
  public T visit(ExprNode n) {
    return n.accept(this);
  }

@Override
public T visit(BinaryOpNode n) {
    return defaultVisit(n);
}

  @Override
  public T visit(UnaryNode n) {
    return n.accept(this);
  }

  @Override
  public T visit(TypeCastNode n) {
    return n.accept(this);
  }

  @Override
  public T visit(MethodCallNode n) {
    return n.accept(this);
  }

  @Override
  public T visit(ArrayNode n) {
    return n.accept(this);
  }

  @Override
  public T visit(IndexAccessNode n) {
    return n.accept(this);
  }
  
@Override
public T visit(RangeIndexNode n) {
    return n.accept(this);
}

@Override
public T visit(MultiRangeIndexNode n) {
    return n.accept(this);
}

  @Override
  public T visit(EqualityChainNode n) {
    return n.accept(this);
  }

  @Override
  public T visit(BooleanChainNode n) {
    return n.accept(this);
  }

  @Override
  public T visit(SlotNode n) {
    return n.accept(this);
  }

  @Override
  public final T visit(ASTNode n) {
    // This is the entry point - delegate to the n's accept method
    return n.accept(this);
  }

  @Override
  public List<T> visitList(List<? extends ASTNode> nodes) {
    List<T> results = new ArrayList<T>();
    for (ASTNode n : nodes) {
      results.add(visit(n));
    }
    return results;
  }

  @Override
  public void visitAll(List<? extends ASTNode> nodes) {
    for (ASTNode n : nodes) {
      visit(n);
    }
  }

  // Helper method to dispatch via accept() - this is what should be used in InterpreterVisitor
  public T dispatch(ASTNode n) {
    return n.accept(this);
  }
}
