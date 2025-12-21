package cod.ast;

import cod.ast.nodes.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Constant Folding Visitor - Optimizes expressions by evaluating constant subexpressions at compile time.
 * Implements ASTVisitor<ASTNode> to transform the AST.
 * 
 * UPDATED: Can work with flattened expressions for better optimization.
 */
public class ConstantFolder extends BaseASTVisitor<ASTNode> {
    
    private final boolean useFlattening;
    
    public ConstantFolder() {
        this(false);
    }
    
    public ConstantFolder(boolean useFlattening) {
        this.useFlattening = useFlattening;
    }
    
    @Override
    public ASTNode visit(BinaryOpNode node) {
        // First, recursively fold children
        ExprNode left = (ExprNode) dispatch(node.left);
        ExprNode right = (ExprNode) dispatch(node.right);
        
        // Apply flattening if enabled
        BinaryOpNode toEvaluate;
        if (useFlattening && isAssociativeOperation(node.op)) {
            // Try to flatten and collect constants
            List<ExprNode> operands = new ArrayList<>();
            List<Object> constantValues = new ArrayList<>();
            List<ExprNode> variableOperands = new ArrayList<>();
            
            collectAndSeparateOperands(left, node.op, operands, constantValues, variableOperands);
            collectAndSeparateOperands(right, node.op, operands, constantValues, variableOperands);
            
            // If we have multiple constants, combine them
            if (constantValues.size() > 1) {
                Object combined = combineConstants(constantValues, node.op);
                if (combined != null) {
                    // Replace multiple constants with one combined constant
                    constantValues.clear();
                    constantValues.add(combined);
                }
            }
            
            // Rebuild expression with combined constants
            if (constantValues.size() == 1 && variableOperands.isEmpty()) {
                // All operands were constants
                return createConstantNode(constantValues.get(0));
            } else if (constantValues.size() == 1 && variableOperands.size() == 1) {
                // One constant, one variable - create simple binary op
                toEvaluate = new BinaryOpNode();
                toEvaluate.left = variableOperands.get(0);
                toEvaluate.op = node.op;
                toEvaluate.right = createConstantNode(constantValues.get(0));
            } else {
                // Rebuild flattened structure
                return rebuildFlattenedExpression(variableOperands, constantValues, node.op);
            }
        } else {
            toEvaluate = new BinaryOpNode();
            toEvaluate.left = left;
            toEvaluate.op = node.op;
            toEvaluate.right = right;
        }
        
        // If both are constant literals, try to evaluate
        if (isConstantLiteral(toEvaluate.left) && isConstantLiteral(toEvaluate.right)) {
            Object leftVal = toEvaluate.left.value;
            Object rightVal = toEvaluate.right.value;
            String op = toEvaluate.op;
            
            try {
                Object result = evaluateBinaryOp(leftVal, rightVal, op, toEvaluate);
                if (result != null) {
                    stats.incrementConstantExpressions();
                    return createConstantNode(result);
                }
            } catch (ArithmeticException e) {
                // Division by zero, overflow, etc. - keep original expression
                // The runtime will catch it
            }
        }
        
        // Return new node with folded children
        return toEvaluate;
    }
    
    // Helper method for flattening-aware constant collection
    private void collectAndSeparateOperands(ExprNode expr, String targetOp, 
                                           List<ExprNode> allOperands,
                                           List<Object> constants,
                                           List<ExprNode> variables) {
        if (expr instanceof BinaryOpNode) {
            BinaryOpNode binExpr = (BinaryOpNode) expr;
            if (binExpr.op.equals(targetOp)) {
                // This is the same operation, recurse
                collectAndSeparateOperands(binExpr.left, targetOp, allOperands, constants, variables);
                collectAndSeparateOperands(binExpr.right, targetOp, allOperands, constants, variables);
                return;
            }
        }
        
        // Not a matching binary op, add as operand
        allOperands.add(expr);
        if (isConstantLiteral(expr)) {
            constants.add(expr.value);
        } else {
            variables.add(expr);
        }
    }
    
    private Object combineConstants(List<Object> constants, String op) {
        if (constants.isEmpty()) return null;
        
        Object result = constants.get(0);
        for (int i = 1; i < constants.size(); i++) {
            result = evaluateBinaryOp(result, constants.get(i), op, null);
            if (result == null) {
                return null; // Can't combine these constants
            }
        }
        return result;
    }
    
    private ExprNode rebuildFlattenedExpression(List<ExprNode> variables, 
                                               List<Object> constants, 
                                               String op) {
        List<ExprNode> allOperands = new ArrayList<>();
        allOperands.addAll(variables);
        for (Object constant : constants) {
            allOperands.add(createConstantNode(constant));
        }
        
        // Rebuild as right-associative chain
        return buildBalancedTree(allOperands, 0, allOperands.size() - 1, op);
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
    
    private boolean isAssociativeOperation(String op) {
        return op.equals("+") || op.equals("*");
    }
    
    // ... [REST OF THE ORIGINAL ConstantFolder CODE REMAINS THE SAME] ...
    // The visit methods for other node types remain unchanged from your original
    
    @Override
    public ASTNode visit(BooleanChainNode node) {
        BooleanChainNode folded = new BooleanChainNode();
        folded.isAll = node.isAll;
        folded.expressions = new ArrayList<>();
        
        boolean allConstants = true;
        List<Object> constantValues = new ArrayList<>();
        
        // First, fold all expressions
        for (ExprNode expr : node.expressions) {
            ExprNode foldedExpr = (ExprNode) dispatch(expr);
            folded.expressions.add(foldedExpr);
            
            if (isConstantLiteral(foldedExpr)) {
                constantValues.add(foldedExpr.value);
            } else {
                allConstants = false;
            }
        }
        
        // If all expressions are constants, evaluate the boolean chain
        if (allConstants && !constantValues.isEmpty()) {
            boolean result;
            
            if (node.isAll) {
                // all[]: All must be true
                result = true;
                for (Object val : constantValues) {
                    if (val instanceof Boolean) {
                        if (!(Boolean) val) {
                            result = false;
                            break;
                        }
                    } else {
                        // Non-boolean in boolean chain - can't fold
                        return folded;
                    }
                }
            } else {
                // any[]: At least one must be true
                result = false;
                for (Object val : constantValues) {
                    if (val instanceof Boolean) {
                        if ((Boolean) val) {
                            result = true;
                            break;
                        }
                    } else {
                        // Non-boolean in boolean chain - can't fold
                        return folded;
                    }
                }
            }
            
            // Create a boolean constant with the result
            stats.incrementBooleanChains();
            return createConstantNode(result);
        }
        
        return folded;
    }
    
    @Override
    public ASTNode visit(EqualityChainNode node) {
        EqualityChainNode folded = new EqualityChainNode();
        folded.left = (ExprNode) dispatch(node.left);
        folded.operator = node.operator;
        folded.isAllChain = node.isAllChain;
        folded.chainArguments = new ArrayList<>();
        
        // Check if left side is constant
        boolean leftConstant = isConstantLiteral(folded.left);
        List<Object> rightConstants = new ArrayList<>();
        boolean allRightConstants = true;
        
        // Fold all chain arguments
        for (ExprNode arg : node.chainArguments) {
            ExprNode foldedArg = (ExprNode) dispatch(arg);
            folded.chainArguments.add(foldedArg);
            
            if (isConstantLiteral(foldedArg)) {
                rightConstants.add(foldedArg.value);
            } else {
                allRightConstants = false;
            }
        }
        
        // If both left and all right arguments are constants, evaluate the chain
        if (leftConstant && allRightConstants && !rightConstants.isEmpty()) {
            Object leftVal = folded.left.value;
            boolean result;
            
            if (node.isAllChain) {
                // all[] chain: All comparisons must be true
                result = true;
                for (Object rightVal : rightConstants) {
                    boolean comparison = evaluateComparison(leftVal, rightVal, node.operator);
                    if (!comparison) {
                        result = false;
                        break;
                    }
                }
            } else {
                // any[] chain: At least one comparison must be true
                result = false;
                for (Object rightVal : rightConstants) {
                    boolean comparison = evaluateComparison(leftVal, rightVal, node.operator);
                    if (comparison) {
                        result = true;
                        break;
                    }
                }
            }
            
            // Create a boolean constant with the result
            stats.incrementConstantExpressions();
            return createConstantNode(result);
        }
        
        return folded;
    }
    
    @Override
    public ASTNode visit(UnaryNode node) {
        ExprNode operand = (ExprNode) dispatch(node.operand);
        
        if (isConstantLiteral(operand)) {
            Object val = operand.value;
            String op = node.op;
            
            Object result = evaluateUnaryOp(val, op, node);
            if (result != null) {
                stats.incrementConstantExpressions();
                return createConstantNode(result);
            }
        }
        
        UnaryNode folded = new UnaryNode();
        folded.op = node.op;
        folded.operand = operand;
        return folded;
    }
    
    @Override
    public ASTNode visit(TypeCastNode node) {
        ExprNode expr = (ExprNode) dispatch(node.expression);
        
        if (isConstantLiteral(expr)) {
            Object val = expr.value;
            String targetType = node.targetType;
            
            try {
                Object result = evaluateTypeCast(val, targetType, node);
                if (result != null) {
                    stats.incrementTypeCasts();
                    return createConstantNode(result);
                }
            } catch (ClassCastException | NumberFormatException e) {
                // Invalid cast - keep original
            }
        }
        
        TypeCastNode folded = new TypeCastNode();
        folded.targetType = node.targetType;
        folded.expression = expr;
        return folded;
    }
    
    @Override
    public ASTNode visit(ExprNode node) {
        // For literals, just return a copy
        if (node.value != null || node.isNull) {
            ExprNode copy = new ExprNode();
            copy.value = node.value;
            copy.isNull = node.isNull;
            copy.name = node.name;
            return copy;
        }
        
        // For identifiers, cannot fold
        ExprNode copy = new ExprNode();
        copy.name = node.name;
        return copy;
    }
    
    @Override
    public ASTNode visit(ArrayNode node) {
        ArrayNode folded = new ArrayNode();
        folded.elements = new ArrayList<>();
        
        for (ExprNode elem : node.elements) {
            folded.elements.add((ExprNode) dispatch(elem));
        }
        return folded;
    }
    
    @Override
    public ASTNode visit(TupleNode node) {
        TupleNode folded = new TupleNode();
        folded.elements = new ArrayList<>();
        
        for (ExprNode elem : node.elements) {
            folded.elements.add((ExprNode) dispatch(elem));
        }
        return folded;
    }
    
    @Override
    public ASTNode visit(MethodCallNode node) {
        // Method calls are not constant-foldable (unless they're pure builtins,
        // but that's a more advanced optimization)
        MethodCallNode folded = new MethodCallNode();
        folded.name = node.name;
        folded.qualifiedName = node.qualifiedName;
        folded.slotNames = new ArrayList<>(node.slotNames);
        folded.chainType = node.chainType;
        folded.arguments = new ArrayList<>();
        
        for (ExprNode arg : node.arguments) {
            folded.arguments.add((ExprNode) dispatch(arg));
        }
        
        if (node.chainArguments != null) {
            folded.chainArguments = new ArrayList<>();
            for (ExprNode arg : node.chainArguments) {
                folded.chainArguments.add((ExprNode) dispatch(arg));
            }
        }
        
        return folded;
    }
    
    @Override
    public ASTNode visit(IndexAccessNode node) {
        IndexAccessNode folded = new IndexAccessNode();
        folded.array = (ExprNode) dispatch(node.array);
        folded.index = (ExprNode) dispatch(node.index);
        return folded;
    }
    
    // Helper methods for evaluation
    
    private boolean evaluateComparison(Object left, Object right, String operator) {
        if (left == null || right == null) {
            switch (operator) {
                case "==": return left == right;
                case "!=": return left != right;
                default: return false;
            }
        }
        
        if (left instanceof Number && right instanceof Number) {
            double l = ((Number) left).doubleValue();
            double r = ((Number) right).doubleValue();
            
            switch (operator) {
                case "==": return Math.abs(l - r) < 1e-12;
                case "!=": return Math.abs(l - r) >= 1e-12;
                case "<": return l < r;
                case ">": return l > r;
                case "<=": return l <= r;
                case ">=": return l >= r;
                default: return false;
            }
        }
        
        if (left instanceof Boolean && right instanceof Boolean) {
            boolean l = (Boolean) left;
            boolean r = (Boolean) right;
            
            switch (operator) {
                case "==": return l == r;
                case "!=": return l != r;
                default: return false;
            }
        }
        
        if (left instanceof String && right instanceof String) {
            String l = (String) left;
            String r = (String) right;
            
            switch (operator) {
                case "==": return l.equals(r);
                case "!=": return !l.equals(r);
                default: return false;
            }
        }
        
        return false;
    }
    
    private Object evaluateBinaryOp(Object left, Object right, String op, BinaryOpNode node) {
        // Handle numeric operations
        if (left instanceof Number && right instanceof Number) {
            double l = ((Number) left).doubleValue();
            double r = ((Number) right).doubleValue();
            
            switch (op) {
                case "+": return l + r;
                case "-": return l - r;
                case "*": return l * r;
                case "/": 
                    if (r == 0) throw new ArithmeticException("Division by zero");
                    return l / r;
                case "%": return l % r;
                case "<": return l < r;
                case ">": return l > r;
                case "<=": return l <= r;
                case ">=": return l >= r;
                case "==": return Math.abs(l - r) < 1e-12;
                case "!=": return Math.abs(l - r) >= 1e-12;
                default: return null;
            }
        }
        
        // Handle boolean operations (only equality for booleans in your language)
        if (left instanceof Boolean && right instanceof Boolean) {
            boolean l = (Boolean) left;
            boolean r = (Boolean) right;
            
            switch (op) {
                case "==": return l == r;
                case "!=": return l != r;
                default: return null;
            }
        }
        
        // Handle string concatenation
        if (op.equals("+") && (left instanceof String || right instanceof String)) {
            return left.toString() + right.toString();
        }
        
        // Handle equality for null
        if (left == null || right == null) {
            switch (op) {
                case "==": return left == right;
                case "!=": return left != right;
                default: return null;
            }
        }
        
        return null;
    }
    
    private Object evaluateUnaryOp(Object val, String op, UnaryNode node) {
        if (val instanceof Number) {
            double num = ((Number) val).doubleValue();
            switch (op) {
                case "+": return +num;
                case "-": return -num;
                default: return null;
            }
        }
        
        if (val instanceof Boolean && op.equals("!")) {
            return !(Boolean) val;
        }
        
        return null;
    }
    
    private Object evaluateTypeCast(Object val, String targetType, TypeCastNode node) {
        if (val == null) return null;
        
        targetType = targetType.toLowerCase();
        
        try {
            switch (targetType) {
                case "int":
                    if (val instanceof Number) return ((Number) val).intValue();
                    if (val instanceof String) return Integer.parseInt((String) val);
                    break;
                    
                case "float":
                    if (val instanceof Number) return ((Number) val).floatValue();
                    if (val instanceof String) return Float.parseFloat((String) val);
                    break;
                    
                case "text":
                    return val.toString();
                    
                case "bool":
                    if (val instanceof Boolean) return val;
                    if (val instanceof Number) return ((Number) val).doubleValue() != 0;
                    if (val instanceof String) return Boolean.parseBoolean((String) val);
                    break;
            }
        } catch (NumberFormatException e) {
            // Invalid cast, keep original
        }
        
        return null;
    }
    
    private boolean isConstantLiteral(ExprNode node) {
        return node != null && (node.value != null || node.isNull);
    }
    
    private ExprNode createConstantNode(Object value) {
        ExprNode node = new ExprNode();
        
        if (value == null) {
            node.isNull = true;
        } else if (value instanceof Integer) {
            node.value = value;
        } else if (value instanceof Float) {
            node.value = value;
        } else if (value instanceof Double) {
            node.value = ((Double) value).floatValue();
        } else if (value instanceof Boolean) {
            node.value = value;
        } else if (value instanceof String) {
            node.value = value;
        }
        
        return node;
    }
    
    // Pass-through methods for statements
    
    @Override
    public ASTNode visit(AssignmentNode node) {
        AssignmentNode folded = new AssignmentNode();
        folded.left = (ExprNode) dispatch(node.left);
        folded.right = (ExprNode) dispatch(node.right);
        return folded;
    }
    
    @Override
    public ASTNode visit(VarNode node) {
        VarNode folded = new VarNode();
        folded.name = node.name;
        folded.explicitType = node.explicitType;
        if (node.value != null) {
            folded.value = (ExprNode) dispatch(node.value);
        }
        return folded;
    }
    
    @Override
    public ASTNode visit(StmtIfNode node) {
        StmtIfNode folded = new StmtIfNode();
        folded.condition = (ExprNode) dispatch(node.condition);
        folded.thenBlock = (BlockNode) dispatch(node.thenBlock);
        folded.elseBlock = (BlockNode) dispatch(node.elseBlock);
        return folded;
    }
    
    @Override
    public ASTNode visit(ForNode node) {
        ForNode folded = new ForNode();
        folded.iterator = node.iterator;
        folded.range = (RangeNode) dispatch(node.range);
        folded.body = (BlockNode) dispatch(node.body);
        return folded;
    }
    
    @Override
    public ASTNode visit(RangeNode node) {
        RangeNode folded = new RangeNode();
        if (node.step != null) {
            folded.step = (ExprNode) dispatch(node.step);
        }
        folded.start = (ExprNode) dispatch(node.start);
        folded.end = (ExprNode) dispatch(node.end);
        return folded;
    }
    
    @Override
    public ASTNode visit(BlockNode node) {
        BlockNode folded = new BlockNode();
        folded.statements = new ArrayList<>();
        for (StmtNode stmt : node.statements) {
            folded.statements.add((StmtNode) dispatch(stmt));
        }
        return folded;
    }
    
    @Override
    public ASTNode visit(SlotAssignmentNode node) {
        SlotAssignmentNode folded = new SlotAssignmentNode();
        folded.slotName = node.slotName;
        folded.value = (ExprNode) dispatch(node.value);
        return folded;
    }
    
    @Override
    public ASTNode visit(MultipleSlotAssignmentNode node) {
        MultipleSlotAssignmentNode folded = new MultipleSlotAssignmentNode();
        folded.assignments = new ArrayList<>();
        for (SlotAssignmentNode assign : node.assignments) {
            folded.assignments.add((SlotAssignmentNode) dispatch(assign));
        }
        return folded;
    }
    
    @Override
    public ASTNode visit(ReturnSlotAssignmentNode node) {
        ReturnSlotAssignmentNode folded = new ReturnSlotAssignmentNode();
        folded.variableNames = new ArrayList<>(node.variableNames);
        folded.methodCall = (MethodCallNode) dispatch(node.methodCall);
        return folded;
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
    public ASTNode visit(ExitNode node) { return node; }
    @Override
    public ASTNode visit(SlotDeclarationNode node) { return node; }
    @Override
    public ASTNode visit(SlotNode node) { return node; }
    
    // Statistics tracking
    private static OptimizationStats stats = new OptimizationStats();
    
    public static OptimizationStats getStats() {
        return stats;
    }
}