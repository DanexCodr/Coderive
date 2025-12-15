package cod.ast;

import cod.ast.nodes.*;
import java.util.*;

/**
 * Flattens nested expressions to prepare for TAC generation.
 * Transforms deeply nested binary operations into shallower, 
 * more linear forms for better optimization and code generation.
 */
public class ExpressionFlattener extends BaseASTVisitor<ASTNode> {
    
    /**
     * Main entry point for flattening an entire AST
     */
    public ASTNode flatten(ASTNode node) {
        return visit(node);
    }
    
    // === EXPRESSION FLATTENING ===
    
    @Override
    public ASTNode visit(BinaryOpNode node) {
        // First flatten children
        ExprNode left = (ExprNode) dispatch(node.left);
        ExprNode right = (ExprNode) dispatch(node.right);
        
        // Check if this is an associative operation we can flatten
        if (isAssociativeOperation(node.op)) {
            // Collect all operands at this precedence level
            List<ExprNode> operands = new ArrayList<>();
            collectOperands(left, node.op, operands);
            collectOperands(right, node.op, operands);
            
            // If we have more than 2 operands, create a flattened structure
            if (operands.size() > 2) {
                return createFlattenedBinaryChain(operands, node.op);
            }
        }
        
        // Return regular binary node with flattened children
        BinaryOpNode result = new BinaryOpNode();
        result.left = left;
        result.op = node.op;
        result.right = right;
        return result;
    }
    
    @Override
    public ASTNode visit(UnaryNode node) {
        ExprNode operand = (ExprNode) dispatch(node.operand);
        
        // Handle double negatives: !!x → x, -(-x) → x
        if (node.op.equals("!") && operand instanceof UnaryNode) {
            UnaryNode inner = (UnaryNode) operand;
            if (inner.op.equals("!")) {
                return inner.operand; // Remove double negation
            }
        } else if (node.op.equals("-") && operand instanceof UnaryNode) {
            UnaryNode inner = (UnaryNode) operand;
            if (inner.op.equals("-")) {
                return inner.operand; // Remove double negative
            }
        }
        
        UnaryNode result = new UnaryNode();
        result.op = node.op;
        result.operand = operand;
        return result;
    }
    
    @Override
    public ASTNode visit(EqualityChainNode node) {
        EqualityChainNode flattened = new EqualityChainNode();
        flattened.left = (ExprNode) dispatch(node.left);
        flattened.operator = node.operator;
        flattened.isAllChain = node.isAllChain;
        flattened.chainArguments = new ArrayList<>();
        
        for (ExprNode arg : node.chainArguments) {
            flattened.chainArguments.add((ExprNode) dispatch(arg));
        }
        return flattened;
    }
    
    @Override
    public ASTNode visit(BooleanChainNode node) {
        BooleanChainNode flattened = new BooleanChainNode();
        flattened.isAll = node.isAll;
        flattened.expressions = new ArrayList<>();
        
        for (ExprNode expr : node.expressions) {
            flattened.expressions.add((ExprNode) dispatch(expr));
        }
        return flattened;
    }
    
    @Override
    public ASTNode visit(TypeCastNode node) {
        TypeCastNode flattened = new TypeCastNode();
        flattened.targetType = node.targetType;
        flattened.expression = (ExprNode) dispatch(node.expression);
        return flattened;
    }
    
    @Override
    public ASTNode visit(MethodCallNode node) {
        MethodCallNode flattened = new MethodCallNode();
        flattened.name = node.name;
        flattened.qualifiedName = node.qualifiedName;
        flattened.slotNames = new ArrayList<>(node.slotNames);
        flattened.chainType = node.chainType;
        flattened.arguments = new ArrayList<>();
        
        for (ExprNode arg : node.arguments) {
            flattened.arguments.add((ExprNode) dispatch(arg));
        }
        
        if (node.chainArguments != null) {
            flattened.chainArguments = new ArrayList<>();
            for (ExprNode arg : node.chainArguments) {
                flattened.chainArguments.add((ExprNode) dispatch(arg));
            }
        }
        
        return flattened;
    }
    
    @Override
    public ASTNode visit(ArrayNode node) {
        ArrayNode flattened = new ArrayNode();
        flattened.elements = new ArrayList<>();
        
        for (ExprNode elem : node.elements) {
            flattened.elements.add((ExprNode) dispatch(elem));
        }
        return flattened;
    }
    
    @Override
    public ASTNode visit(TupleNode node) {
        TupleNode flattened = new TupleNode();
        flattened.elements = new ArrayList<>();
        
        for (ExprNode elem : node.elements) {
            flattened.elements.add((ExprNode) dispatch(elem));
        }
        return flattened;
    }
    
    @Override
    public ASTNode visit(IndexAccessNode node) {
        IndexAccessNode flattened = new IndexAccessNode();
        flattened.array = (ExprNode) dispatch(node.array);
        flattened.index = (ExprNode) dispatch(node.index);
        return flattened;
    }
    
    @Override
    public ASTNode visit(ExprNode node) {
        // For literals and identifiers, just return a copy
        if (node.value != null || node.isNull || node.name != null) {
            ExprNode copy = new ExprNode();
            copy.value = node.value;
            copy.isNull = node.isNull;
            copy.name = node.name;
            return copy;
        }
        return node;
    }
    
    // === HELPER METHODS ===
    
    private boolean isAssociativeOperation(String op) {
        return op.equals("+") || op.equals("*") || 
               op.equals("&") || op.equals("|") || // For bitwise if supported
               op.equals("&&") || op.equals("||"); // For logical operations
    }
    
    private void collectOperands(ExprNode expr, String targetOp, List<ExprNode> operands) {
        if (expr instanceof BinaryOpNode) {
            BinaryOpNode binExpr = (BinaryOpNode) expr;
            if (binExpr.op.equals(targetOp)) {
                // This is the same operation, recurse
                collectOperands(binExpr.left, targetOp, operands);
                collectOperands(binExpr.right, targetOp, operands);
                return;
            }
        }
        // Not a matching binary op, add as operand
        operands.add(expr);
    }
    
    private ExprNode createFlattenedBinaryChain(List<ExprNode> operands, String op) {
        // Rebuild as right-associative chain for consistent evaluation
        // This creates a balanced tree: (a + b + c + d) → ((a + b) + (c + d))
        return buildBalancedTree(operands, 0, operands.size() - 1, op);
    }
    
    private ExprNode buildBalancedTree(List<ExprNode> operands, int start, int end, String op) {
        if (start == end) {
            return operands.get(start);
        }
        if (start + 1 == end) {
            BinaryOpNode node = new BinaryOpNode();
            node.left = operands.get(start);
            node.op = op;
            node.right = operands.get(end);
            return node;
        }
        
        int mid = (start + end) / 2;
        BinaryOpNode node = new BinaryOpNode();
        node.left = buildBalancedTree(operands, start, mid, op);
        node.op = op;
        node.right = buildBalancedTree(operands, mid + 1, end, op);
        return node;
    }
    
    // === PASS-THROUGH METHODS (statements and declarations) ===
    
    @Override
    public ASTNode visit(AssignmentNode node) {
        AssignmentNode flattened = new AssignmentNode();
        flattened.left = (ExprNode) dispatch(node.left);
        flattened.right = (ExprNode) dispatch(node.right);
        return flattened;
    }
    
    @Override
    public ASTNode visit(VarNode node) {
        VarNode flattened = new VarNode();
        flattened.name = node.name;
        flattened.explicitType = node.explicitType;
        if (node.value != null) {
            flattened.value = (ExprNode) dispatch(node.value);
        }
        return flattened;
    }
    
    @Override
    public ASTNode visit(StmtIfNode node) {
        StmtIfNode flattened = new StmtIfNode();
        flattened.condition = (ExprNode) dispatch(node.condition);
        flattened.thenBlock = (BlockNode) dispatch(node.thenBlock);
        flattened.elseBlock = (BlockNode) dispatch(node.elseBlock);
        return flattened;
    }
    
    @Override
    public ASTNode visit(ForNode node) {
        ForNode flattened = new ForNode();
        flattened.iterator = node.iterator;
        flattened.range = (RangeNode) dispatch(node.range);
        flattened.body = (BlockNode) dispatch(node.body);
        return flattened;
    }
    
    @Override
    public ASTNode visit(RangeNode node) {
        RangeNode flattened = new RangeNode();
        if (node.step != null) {
            flattened.step = (ExprNode) dispatch(node.step);
        }
        flattened.start = (ExprNode) dispatch(node.start);
        flattened.end = (ExprNode) dispatch(node.end);
        return flattened;
    }
    
    @Override
    public ASTNode visit(OutputNode node) {
        OutputNode flattened = new OutputNode();
        flattened.varName = node.varName;
        flattened.arguments = new ArrayList<>();
        for (ExprNode arg : node.arguments) {
            flattened.arguments.add((ExprNode) dispatch(arg));
        }
        return flattened;
    }
    
    @Override
    public ASTNode visit(BlockNode node) {
        BlockNode flattened = new BlockNode();
        flattened.statements = new ArrayList<>();
        for (StmtNode stmt : node.statements) {
            flattened.statements.add((StmtNode) dispatch(stmt));
        }
        return flattened;
    }
    
    // Default pass-through for other node types
    @Override
    public ASTNode visit(ProgramNode node) { return node; }
    @Override
    public ASTNode visit(UnitNode node) { return node; }
    @Override
    public ASTNode visit(UseNode node) { return node; }
    @Override
    public ASTNode visit(TypeNode node) { return node; }
    @Override
    public ASTNode visit(FieldNode node) { return node; }
    @Override
    public ASTNode visit(MethodNode node) { return node; }
    @Override
    public ASTNode visit(ParamNode node) { return node; }
    @Override
    public ASTNode visit(ConstructorNode node) { return node; }
    @Override
    public ASTNode visit(InputNode node) { return node; }
    @Override
    public ASTNode visit(ExitNode node) { return node; }
    @Override
    public ASTNode visit(SlotDeclarationNode node) { return node; }
    @Override
    public ASTNode visit(SlotNode node) { return node; }
    @Override
    public ASTNode visit(SlotAssignmentNode node) {
        SlotAssignmentNode flattened = new SlotAssignmentNode();
        flattened.slotName = node.slotName;
        flattened.value = (ExprNode) dispatch(node.value);
        return flattened;
    }
    
    @Override
    public ASTNode visit(MultipleSlotAssignmentNode node) {
        MultipleSlotAssignmentNode flattened = new MultipleSlotAssignmentNode();
        flattened.assignments = new ArrayList<>();
        for (SlotAssignmentNode assign : node.assignments) {
            flattened.assignments.add((SlotAssignmentNode) dispatch(assign));
        }
        return flattened;
    }
    
    @Override
    public ASTNode visit(ReturnSlotAssignmentNode node) {
        ReturnSlotAssignmentNode flattened = new ReturnSlotAssignmentNode();
        flattened.variableNames = new ArrayList<>(node.variableNames);
        flattened.methodCall = (MethodCallNode) dispatch(node.methodCall);
        return flattened;
    }
}