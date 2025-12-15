package cod.ast;

import cod.ast.nodes.*;
import java.util.List;

public interface ASTVisitor<T> {
    
    // Program structure
    T visit(ProgramNode node);
    T visit(UnitNode node);
    T visit(UseNode node);
    
    // Type declarations
    T visit(TypeNode node);
    T visit(FieldNode node);
    T visit(MethodNode node);
    T visit(ParamNode node);
    T visit(ConstructorNode node);
    
    // Statements
    T visit(BlockNode node);
    T visit(AssignmentNode node);
    T visit(VarNode node);
    T visit(StmtIfNode node);
    T visit(ExprIfNode node);
    T visit(ForNode node);
    T visit(RangeNode node);
    T visit(OutputNode node);
    T visit(InputNode node);
    T visit(ExitNode node);
    T visit(TupleNode node);
    T visit(ReturnSlotAssignmentNode node);
    T visit(SlotDeclarationNode node);
    T visit(SlotAssignmentNode node);
    T visit(MultipleSlotAssignmentNode node);
    
    // Expressions
    T visit(ExprNode node);
    T visit(BinaryOpNode node);
    T visit(UnaryNode node);
    T visit(TypeCastNode node);
    T visit(MethodCallNode node);
    T visit(ArrayNode node);
    T visit(IndexAccessNode node);
    T visit(EqualityChainNode node);
    T visit(BooleanChainNode node);
    T visit(SlotNode node);
    
    T visit(ASTNode node);
    
    // Utility methods for visiting lists
    List<T> visitList(List<? extends ASTNode> nodes);
    void visitAll(List<? extends ASTNode> nodes);
}