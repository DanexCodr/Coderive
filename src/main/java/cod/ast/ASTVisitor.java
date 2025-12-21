package cod.ast;

import cod.ast.nodes.*;
import java.util.List;

public interface ASTVisitor<T> {
    
    // Program structure
    T visit(ProgramNode n);
    T visit(UnitNode n);
    T visit(UseNode n);
    
    // Type declarations
    T visit(TypeNode n);
    T visit(FieldNode n);
    T visit(MethodNode n);
    T visit(ParamNode n);
    T visit(ConstructorNode n);
    T visit(ConstructorCallNode n);
    
    // Statements
    T visit(BlockNode n);
    T visit(AssignmentNode n);
    T visit(VarNode n);
    T visit(StmtIfNode n);
    T visit(ExprIfNode n);
    T visit(ForNode n);
    T visit(SkipNode n);
    T visit(BreakNode n);
    T visit(RangeNode n);
    T visit(ExitNode n);
    T visit(TupleNode n);
    T visit(ReturnSlotAssignmentNode n);
    T visit(SlotDeclarationNode n);
    T visit(SlotAssignmentNode n);
    T visit(MultipleSlotAssignmentNode n);
    
    // Expressions
    T visit(ExprNode n);
    T visit(BinaryOpNode n);
    T visit(UnaryNode n);
    T visit(TypeCastNode n);
    T visit(MethodCallNode n);
    T visit(ArrayNode n);
    T visit(IndexAccessNode n);
    T visit(EqualityChainNode n);
    T visit(BooleanChainNode n);
    T visit(SlotNode n);
    
    T visit(ASTNode n);
    
    // Utility methods for visiting lists
    List<T> visitList(List<? extends ASTNode> n);
    void visitAll(List<? extends ASTNode> n);
}