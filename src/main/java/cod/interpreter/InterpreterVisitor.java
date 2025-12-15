package cod.interpreter;

import static cod.syntax.Keyword.*;
import cod.ast.ASTFactory;
import cod.ast.BaseASTVisitor;
import cod.ast.nodes.*;
import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class InterpreterVisitor extends BaseASTVisitor<Object> {

    private final Interpreter interpreter;
    public final TypeSystem typeSystem;
    private final IOHandler ioHandler;
    private final Stack<ExecutionContext> contextStack = new Stack<ExecutionContext>();
    
    // Scale for BigDecimal operations (consistent with TypeSystem)
    private static final int DECIMAL_SCALE = 20;

    public InterpreterVisitor(Interpreter interpreter, TypeSystem typeSystem, IOHandler ioHandler) {
        this.interpreter = interpreter;
        this.typeSystem = typeSystem;
        this.ioHandler = ioHandler;
    }

    public void pushContext(ExecutionContext context) { contextStack.push(context); }
    public void popContext() { contextStack.pop(); }
    public ExecutionContext getCurrentContext() { return contextStack.peek(); }

@Override
public Object visit(ExprNode node) {
    ExecutionContext ctx = getCurrentContext();

    if (node.name != null) {
        
        if (ctx.locals.containsKey(node.name)) return ctx.locals.get(node.name);
        if (ctx.slotValues != null && ctx.slotValues.containsKey(node.name)) return ctx.slotValues.get(node.name);
        if (ctx.objectInstance.fields.containsKey(node.name)) return ctx.objectInstance.fields.get(node.name);

        throw new RuntimeException("Undefined Variable: " + node.name);
    } 

    if (node.value != null) {
        if (node.value instanceof String) {
            String s = (String) node.value;
            if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) return s.substring(1, s.length() - 1);
            return s;
        }
        
        // FIX: Strip trailing zeros from BigDecimal literals
        if (node.value instanceof BigDecimal) {
            return ((BigDecimal) node.value).stripTrailingZeros();
        }
        
        return node.value;
    }

    return null;
}

   @Override
public Object visit(BinaryOpNode node) {
    Object left = dispatch(node.left);
    Object right = dispatch(node.right);
    
    switch (node.op) {
        case "+": 
        case "+=":
            if (left instanceof String || right instanceof String) {
                Object unwrappedLeft = typeSystem.unwrap(left);
                Object unwrappedRight = typeSystem.unwrap(right);
                
                // FIX: Strip trailing zeros from BigDecimal and use toPlainString()
                if (unwrappedLeft instanceof BigDecimal) {
                    BigDecimal bdLeft = ((BigDecimal) unwrappedLeft).stripTrailingZeros();
                    unwrappedLeft = bdLeft.toPlainString(); // Use plain string, not toString()
                }
                if (unwrappedRight instanceof BigDecimal) {
                    BigDecimal bdRight = ((BigDecimal) unwrappedRight).stripTrailingZeros();
                    unwrappedRight = bdRight.toPlainString(); // Use plain string, not toString()
                }
                
                return String.valueOf(unwrappedLeft) + String.valueOf(unwrappedRight); 
            }
            return typeSystem.addNumbers(left, right);

        case "-":
        case "-=":
            return typeSystem.subtractNumbers(left, right);
        case "*":
        case "*=":
            return typeSystem.multiplyNumbers(left, right);
        case "/":
        case "/=":
            return typeSystem.divideNumbers(left, right);
        case "%":
            return typeSystem.modulusNumbers(left, right);
        case ">":
            return typeSystem.compare(left, right) > 0;
        case "<":
            return typeSystem.compare(left, right) < 0;
        case ">=":
            return typeSystem.compare(left, right) >= 0;
        case "<=":
            return typeSystem.compare(left, right) <= 0;
        case "=":
            return right;
        case "==":
            // Use Object.equals() which handles BigDecimal vs BigDecimal
            return typeSystem.unwrap(left).equals(typeSystem.unwrap(right));
        case "!=":
            return !typeSystem.unwrap(left).equals(typeSystem.unwrap(right));
        default: 
            throw new RuntimeException("Unknown operator: " + node.op);
    }
}

@Override
public Object visit(BooleanChainNode node) {
    boolean isAll = node.isAll;
    
    for (ExprNode expr : node.expressions) {
        Object result = dispatch(expr);
        result = typeSystem.unwrap(result);
        boolean isTruthy = isTruthy(result);
        
        if (isAll) {
            // For "all": if any expression is false, return false
            if (!isTruthy) return false;
        } else {
            // For "any": if any expression is true, return true
            if (isTruthy) return true;
        }
    }
    
    // If we get here:
    // - For "all": all expressions were true, so return true
    // - For "any": no expressions were true, so return false
    return isAll;
}

    @Override
    public Object visit(UnaryNode node) {
        Object operand = dispatch(node.operand);
        
        switch (node.op) {
            case "-": 
                return typeSystem.negateNumber(operand);
            case "+": 
                return operand;
            case "!": 
                return !isTruthy(operand);
            default: 
                throw new RuntimeException("Unknown unary operator: " + node.op);
        }
    }

    @Override
    public Object visit(TypeCastNode node) {
        Object val = dispatch(node.expression);
        if (!typeSystem.validateType(node.targetType, val)) {
            return typeSystem.convertType(val, node.targetType);
        }
        return val;
    }

    @Override
    public Object visit(InputNode node) {
        ExecutionContext ctx = getCurrentContext();
        Object val = ioHandler.readInput(node.targetType);
        
        if (node.variableName != null) {
            ctx.objectInstance.fields.put(node.variableName, val);
            ctx.locals.put(node.variableName, val);
        }
        
        return val;
    }

@Override
public Object visit(ArrayNode node) {
    // Handle [range] -> NaturalArray (keep this!)
    if (node.elements.size() == 1) {
        ExprNode onlyElement = node.elements.get(0);
        if (onlyElement instanceof RangeNode) {
            RangeNode range = (RangeNode) onlyElement;
            return new NaturalArray(range, this);
        }
    }
    
    List<Object> result = new ArrayList<Object>();
    for (ExprNode element : node.elements) {
        // Handle RangeNode specially - DON'T dispatch it!
        if (element instanceof RangeNode) {
            result.add(new NaturalArray((RangeNode) element, this));
        }
        else {
            Object evaluated = dispatch(element);
            
            // Special handling for nested arrays that evaluate to NaturalArray
            if (element instanceof ArrayNode && evaluated instanceof NaturalArray) {
                result.add(evaluated);
            } 
            else {
                result.add(evaluated);
            }
        }
    }
    return result;
}

// Helper updated to accept BigDecimal
private Object convertToAppropriateType(BigDecimal value, Object original) {
    // If original was integer/long and the result is an exact integer, return integer/long
    if ((original instanceof Integer || original instanceof Long) && 
        (value.scale() == 0 || value.stripTrailingZeros().scale() <= 0)) {
        
        // Check if it fits in Integer
        try {
            return value.intValueExact();
        } catch (ArithmeticException e) {
            // Falls back to Long if it exceeds Integer range
            return value.longValue();
        }
    }
    
    // Otherwise, return the precise BigDecimal
    return value;
}

    @Override
    public Object visit(TupleNode node) {
        List<Object> tuple = new ArrayList<Object>();
        for (ExprNode elem : node.elements) tuple.add(dispatch(elem));
        return Collections.unmodifiableList(tuple); 
    }

@Override
public Object visit(IndexAccessNode node) {
    
    // Get array and index objects
    Object arrayObj = dispatch(node.array);
    Object indexObj = dispatch(node.index);
    
    // Unwrap if needed
    arrayObj = typeSystem.unwrap(arrayObj);
    indexObj = typeSystem.unwrap(indexObj);
    
    // --- Handle NaturalArray ---
    if (arrayObj instanceof NaturalArray) {
        NaturalArray natural = (NaturalArray) arrayObj;
        
        // Convert index to long
        long index = toLongIndex(indexObj);
        
        if (index < 0 || index >= natural.size()) {
            throw new RuntimeException("Index out of bounds: " + index + 
                " for NaturalArray of size " + natural.size());
        }
        
        return natural.get(index);
    }
    
    // --- Original handling for traditional lists ---
    if (arrayObj instanceof List) {
        int index = toIntIndex(indexObj);
        
        List<Object> list = (List<Object>) arrayObj;
        
        if (index < 0 || index >= list.size()) {
            throw new RuntimeException("Index out of bounds: " + index + 
                " for array of size " + list.size());
        }
        
        return list.get(index);
    }
    
    throw new RuntimeException("Invalid array access: expected NaturalArray or List, got " + 
        (arrayObj != null ? arrayObj.getClass().getSimpleName() : "null"));
}

/**
 * Convert any numeric object to long for NaturalArray indices.
 */
private long toLongIndex(Object indexObj) {
    if (indexObj == null) {
        throw new RuntimeException("Array index cannot be null");
    }
    
    if (indexObj instanceof Integer) {
        return ((Integer) indexObj).longValue();
    } 
    
    if (indexObj instanceof Long) {
        return (Long) indexObj;
    } 
    
    if (indexObj instanceof Double || indexObj instanceof Float) {
        double d = ((Number) indexObj).doubleValue();
        if (d != Math.floor(d)) {
            throw new RuntimeException("Array index must be integer, got: Double (" + d + ")");
        }
        if (d < Long.MIN_VALUE || d > Long.MAX_VALUE) {
            throw new RuntimeException("Array index out of long range: " + d);
        }
        return (long) d;
    }
    
    // NEW: Handle BigDecimal index precisely
    if (indexObj instanceof BigDecimal) {
        BigDecimal bd = (BigDecimal) indexObj;
        try {
            // Use precise conversion that checks for fraction part
            return bd.longValueExact();
        } catch (ArithmeticException e) {
            throw new RuntimeException("Array index must be an exact integer, got: BigDecimal (" + bd + ")");
        }
    }
    
    throw new RuntimeException("Array index must be integer, got: " + 
        indexObj.getClass().getSimpleName() + " (" + indexObj + ")");
}

/**
 * Convert any numeric object to int for traditional List indices.
 */
private int toIntIndex(Object indexObj) {
    if (indexObj == null) {
        throw new RuntimeException("Array index cannot be null");
    }
    
    if (indexObj instanceof Integer) {
        return (Integer) indexObj;
    } 
    
    if (indexObj instanceof Long) {
        long l = (Long) indexObj;
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new RuntimeException("Array index out of int range: " + l);
        }
        return (int) l;
    }
    
    if (indexObj instanceof Double || indexObj instanceof Float) {
        double d = ((Number) indexObj).doubleValue();
        if (d != Math.floor(d) || d < Integer.MIN_VALUE || d > Integer.MAX_VALUE) {
            throw new RuntimeException("Array index must be integer, got: Double (" + d + ")");
        }
        return (int) d;
    }
    
    // NEW: Handle BigDecimal index precisely
    if (indexObj instanceof BigDecimal) {
        BigDecimal bd = (BigDecimal) indexObj;
        try {
            return bd.intValueExact();
        } catch (ArithmeticException e) {
            throw new RuntimeException("Array index must be an exact integer within int range, got: BigDecimal (" + bd + ")");
        }
    }
    
    throw new RuntimeException("Array index must be integer, got: " + 
        indexObj.getClass().getSimpleName() + " (" + indexObj + ")");
}
    
@Override
public Object visit(AssignmentNode node) {
    ExecutionContext ctx = getCurrentContext();
    Object newValue = dispatch(node.right);

    // Handle Array Index Assignment (LHS is IndexAccessNode)
    if (node.left instanceof IndexAccessNode) {
        IndexAccessNode indexAccess = (IndexAccessNode) node.left;
        
        Object arrayObj = dispatch(indexAccess.array);
        arrayObj = typeSystem.unwrap(arrayObj);
        
        Object indexObj = dispatch(indexAccess.index);
        indexObj = typeSystem.unwrap(indexObj);
        
        // --- Handle NaturalArray ---
        if (arrayObj instanceof NaturalArray) {
            NaturalArray natural = (NaturalArray) arrayObj;
            long index = toLongIndex(indexObj);
            
            if (index < 0 || index >= natural.size()) {
                throw new RuntimeException("Index out of bounds: " + index + 
                    " for NaturalArray of size " + natural.size());
            }
            
            natural.set(index, newValue);
            return newValue;
        }
        
        // --- Handle traditional List ---
        if (arrayObj instanceof List) {
            int intIndex = toIntIndex(indexObj);
            
            List<Object> list = (List<Object>) arrayObj;
            
            if (intIndex < 0 || intIndex >= list.size()) {
                throw new RuntimeException("Array index out of bounds: " + intIndex + 
                    " (array size: " + list.size() + ")");
            }
            
            list.set(intIndex, newValue);
            return newValue;
        }
        
        throw new RuntimeException("Invalid assignment target: Cannot index non-array object. " +
            "Expected NaturalArray or List, got: " + 
            (arrayObj != null ? arrayObj.getClass().getSimpleName() : "null"));
    }
    
    // Handle Variable Assignment (LHS is a variable name)
    else if (node.left.name != null) {
        String varName = node.left.name;

        // REJECT assignment to underscore
        if ("_".equals(varName)) {
            throw new RuntimeException("Cannot assign to '_'. Underscore is reserved for discard/placeholder.");
        }
        
        // 1. Check Locals (REASSIGNMENT only - declarations use := elsewhere)
        if (ctx.locals.containsKey(varName)) {
            if (ctx.localTypes.containsKey(varName)) {
                String type = ctx.localTypes.get(varName);
                if (!typeSystem.validateType(type, newValue)) {
                    throw new RuntimeException("Type mismatch in assignment. Variable '" + 
                        varName + "' expects " + type + ", got: " + newValue);
                }
                if (type.contains("|")) {
                    String activeType = typeSystem.getConcreteType(typeSystem.unwrap(newValue));
                    newValue = new TypedValue(newValue, activeType, type);
                }
            }
            ctx.locals.put(varName, newValue);
            return newValue;
        } 
        
        // 2. Check Object Fields
        if (ctx.objectInstance.fields.containsKey(varName)) {
            ctx.objectInstance.fields.put(varName, newValue);
            return newValue;
        }
        
        // 3. ERROR: Variable doesn't exist (use := for declaration)
        throw new RuntimeException("Cannot assign to undefined variable: " + varName + 
            ". Use ':=' for declaration: " + varName + " := " + node.right);
    }
    
    // Handle Slot Assignment in method returns? (If AssignmentNode is used there)
    // Actually, SlotAssignmentNode is separate, so not here
    
    throw new RuntimeException("Invalid assignment target: " + 
        (node.left != null ? node.left.getClass().getSimpleName() : "null"));
}

    @Override
    public Object visit(SlotDeclarationNode node) {
        return null; 
    }

    @Override
    public Object visit(MethodCallNode node) {
        if (node.chainType != null && node.chainArguments != null) return evaluateConditionalChain(node);
        ExecutionContext ctx = getCurrentContext();
        
        MethodNode method = null;
        if (ctx.objectInstance.type != null) {
             for (MethodNode m : ctx.objectInstance.type.methods) {
                if (m.name.equals(node.name)) { method = m; break; }
            }
        }
        
        Object result = interpreter.evalMethodCall(node, ctx.objectInstance, ctx.locals);
        
        if (node.slotNames != null && !node.slotNames.isEmpty()) {
            if (!(result instanceof Map)) {
                 throw new RuntimeException("Cannot extract slot '" + node.slotNames.get(0) + "'. Method did not return slots.");
            }
            
            Map<String, Object> map = (Map<String, Object>) result;
            String requestedSlot = node.slotNames.get(0);

            if (!map.containsKey(requestedSlot) && method != null && method.returnSlots != null) {
                try {
                    int index = Integer.parseInt(requestedSlot);
                    if (index >= 0 && index < method.returnSlots.size()) {
                        requestedSlot = method.returnSlots.get(index).name;
                    }
                } catch (NumberFormatException e) {
                    // It was a non-numeric name, proceed to standard check.
                }
            }

            if (map.containsKey(requestedSlot)) {
                 return map.get(requestedSlot);
            } else {
                 throw new RuntimeException("Undefined method slot: " + requestedSlot);
            }
        }
        
        return result;
    }

@Override
public Object visit(MultipleSlotAssignmentNode node) {
    ExecutionContext ctx = getCurrentContext();
    
    List<String> declaredSlots = new ArrayList<String>(ctx.slotTypes.keySet());
    
    Object lastValue = null;
    int slotIndex = 0;
    
    for (SlotAssignmentNode assign : node.assignments) {
        Object value = dispatch(assign.value);
        
        String target;
        if (assign.slotName != null && !assign.slotName.isEmpty() && !"_".equals(assign.slotName)) {
            target = assign.slotName;
        } else {
            if (slotIndex < declaredSlots.size()) {
                target = declaredSlots.get(slotIndex);
            } else {
                throw new RuntimeException("Too many positional slot assignments. Method has only " + declaredSlots.size() + " return slots.");
            }
        }
        
        if (ctx.slotValues.containsKey(target)) {
            String declaredType = ctx.slotTypes.get(target);
            
            validateSlotType(ctx, target, value);
            
            if (declaredType.contains("|")) {
                String activeType = typeSystem.getConcreteType(typeSystem.unwrap(value));
                value = new TypedValue(value, activeType, declaredType);
            }
            
            ctx.slotValues.put(target, value);
            ctx.slotsInCurrentPath.add(target);
        } else {
            throw new RuntimeException("Assignment to '" + target + "' failed: Slot is not declared.");
        }
        lastValue = value;
        slotIndex++;
    }
    return lastValue;
}

@Override
public Object visit(SlotAssignmentNode node) {
    ExecutionContext ctx = getCurrentContext();
    Object value = dispatch(node.value);
    String varName = node.slotName;
    
    String slotTarget;
    if (varName != null && !varName.isEmpty() && !"_".equals(varName)) {
        slotTarget = varName;
    } else {
        if (ctx.slotValues != null && !ctx.slotValues.isEmpty()) {
            slotTarget = ctx.slotTypes.keySet().iterator().next();
        } else {
            throw new RuntimeException("Assignment to implicit return failed: Method has no declared return slots.");
        }
    }

    if (ctx.slotValues != null && ctx.slotValues.containsKey(slotTarget)) {
        String declaredType = ctx.slotTypes.get(slotTarget);
        
        validateSlotType(ctx, slotTarget, value);
        
        if (declaredType.contains("|")) {
             String activeType = typeSystem.getConcreteType(typeSystem.unwrap(value));
             value = new TypedValue(value, activeType, declaredType);
        }
        
        ctx.slotValues.put(slotTarget, value);
        ctx.slotsInCurrentPath.add(slotTarget); 
        
    } else {
         throw new RuntimeException("Assignment to '" + (varName != null ? varName : "implicit") + "' failed: Slot '" + slotTarget + "' is not declared.");
    }
    return value;
}

    @Override
    public Object visit(FieldNode node) {
        Object val = node.value != null ? dispatch(node.value) : null;
        getCurrentContext().objectInstance.fields.put(node.name, val);
        return val;
    }

    @Override
    public Object visit(VarNode node) {
        Object val = node.value != null ? dispatch(node.value) : null;
        
        if (node.explicitType != null) {
            String declaredType = node.explicitType;
            
            getCurrentContext().localTypes.put(node.name, declaredType);
            
            if (!typeSystem.validateType(declaredType, val)) {
                 throw new RuntimeException("Type mismatch for variable " + node.name + ". Expected " + declaredType);
            }
            
            if (declaredType.contains("|")) {
                 String activeType = typeSystem.getConcreteType(typeSystem.unwrap(val));
                 val = new TypedValue(val, activeType, declaredType);
            }
        }
        
        getCurrentContext().locals.put(node.name, val);
        return val;
    }

    @Override
    public Object visit(OutputNode node) {
        Object lastVal = null;
        for (ExprNode arg : node.arguments) {
            Object val = dispatch(arg);
            
            val = typeSystem.unwrap(val);
            
            ioHandler.output(val);
            lastVal = val;
        }
        return lastVal;
    }

    @Override
    public Object visit(ExitNode node) {
        throw new Interpreter.EarlyExitException();
    }

    @Override
    public Object visit(StmtIfNode node) {
        Object testObj = dispatch(node.condition);
        testObj = typeSystem.unwrap(testObj); 
        
        boolean test = isTruthy(testObj);
        List<StmtNode> branch = test ? node.thenBlock.statements : node.elseBlock.statements;
        
        ExecutionContext ctx = getCurrentContext();
        Set<String> prevSlots = new HashSet<String>(ctx.slotsInCurrentPath);
        
        for (StmtNode s : branch) {
            dispatch(s);
            if (!ctx.slotsInCurrentPath.isEmpty() && interpreter.shouldReturnEarly(ctx.slotValues, ctx.slotsInCurrentPath)) break;
        }
        ctx.slotsInCurrentPath = prevSlots;
        return null;
    }
    
    @Override
public Object visit(ExprIfNode node) {
    Object condValue = dispatch(node.condition);
    condValue = typeSystem.unwrap(condValue);
    
    if (isTruthy(condValue)) {
        return dispatch(node.thenExpr);
    } else {
        return dispatch(node.elseExpr);
    }
}

// UPDATED: ForNode uses BigDecimal for multiplicative/division loops
@Override
public Object visit(ForNode node) {
    ExecutionContext ctx = getCurrentContext();
    String iter = node.iterator;
    
    Object startObj = dispatch(node.range.start);
    Object endObj = dispatch(node.range.end);
    
    startObj = typeSystem.unwrap(startObj);
    endObj = typeSystem.unwrap(endObj);
    
    // Check if step is multiplicative/division (i * 2, i / 2)
    if (node.range.step != null && node.range.step instanceof BinaryOpNode) {
        BinaryOpNode binOp = (BinaryOpNode) node.range.step;
        
        if (binOp.left instanceof ExprNode && 
            ((ExprNode)binOp.left).name != null && 
            ((ExprNode)binOp.left).name.equals(iter) &&
            (binOp.op.equals("*") || binOp.op.equals("/"))) {
            
            Object rightObj = dispatch(binOp.right);
            rightObj = typeSystem.unwrap(rightObj);
            
            // Use BigDecimal for factor to support precise multiplicative/divisive steps (e.g., *1.01)
            BigDecimal factorBD = typeSystem.toBigDecimal(rightObj);
            
            if (factorBD.compareTo(BigDecimal.ZERO) == 0 && binOp.op.equals("/")) {
                throw new RuntimeException("Division by zero in loop step");
            }
            
            return executeMultiplicativeLoop(ctx, node, startObj, endObj, factorBD, binOp.op);
        }
    }
    
    // Regular additive step
    BigDecimal stepBD = BigDecimal.ONE;
    if (node.range.step != null) {
        Object stepObj = dispatch(node.range.step);
        stepObj = typeSystem.unwrap(stepObj);
        stepBD = typeSystem.toBigDecimal(stepObj);
    } else {
        // Default step based on direction
        BigDecimal startBD = typeSystem.toBigDecimal(startObj);
        BigDecimal endBD = typeSystem.toBigDecimal(endObj);
        stepBD = (startBD.compareTo(endBD) > 0) ? BigDecimal.ONE.negate() : BigDecimal.ONE;
    }
    
    if (stepBD.compareTo(BigDecimal.ZERO) == 0) throw new RuntimeException("Loop step cannot be zero.");
    
    return executeAdditiveLoop(ctx, node, startObj, endObj, stepBD);
}

// UPDATED: executeAdditiveLoop to use BigDecimal
private Object executeAdditiveLoop(ExecutionContext ctx, ForNode node, 
                                   Object startObj, Object endObj, BigDecimal stepBD) {
    String iter = node.iterator;
    
    BigDecimal startBD = typeSystem.toBigDecimal(startObj);
    BigDecimal endBD = typeSystem.toBigDecimal(endObj);
    BigDecimal current = startBD;
    
    boolean increasing = stepBD.compareTo(BigDecimal.ZERO) > 0;
    
    while (true) {
        // Check termination condition using precise comparison
        if (increasing && current.compareTo(endBD) > 0) break;
        if (!increasing && current.compareTo(endBD) < 0) break;
        
        // Store the current value, converting back to Integer/Long if possible
        Object currentValue = convertToAppropriateType(current, startObj);
        
        ctx.locals.put(iter, currentValue);
        if (!ctx.localTypes.containsKey(iter)) {
            // Use INT if it's an exact integer, otherwise FLOAT
            String inferredType = (currentValue instanceof Integer || currentValue instanceof Long) ? INT.toString() : FLOAT.toString();
            ctx.localTypes.put(iter, inferredType);
        }
        
        for (StmtNode s : node.body.statements) {
            dispatch(s);
            if (!ctx.slotsInCurrentPath.isEmpty() && 
                interpreter.shouldReturnEarly(ctx.slotValues, ctx.slotsInCurrentPath)) {
                return null;
            }
        }
        
        // Update iterator precisely
        current = current.add(stepBD);
    }
    return null;
}

// UPDATED: executeMultiplicativeLoop to use BigDecimal
private Object executeMultiplicativeLoop(ExecutionContext ctx, ForNode node,
                                         Object startObj, Object endObj, 
                                         BigDecimal factorBD, String operation) {
    String iter = node.iterator;
    
    BigDecimal startBD = typeSystem.toBigDecimal(startObj);
    BigDecimal endBD = typeSystem.toBigDecimal(endObj);
    BigDecimal current = startBD;
    
    while (true) {
        // Check termination condition based on operation and direction
        boolean shouldContinue = false;
        
        if (operation.equals("*")) {
            if (factorBD.compareTo(BigDecimal.ONE) > 0) {
                // Growing sequence
                shouldContinue = (startBD.compareTo(endBD) < 0) ? current.compareTo(endBD) <= 0 : current.compareTo(endBD) >= 0;
            } else if (factorBD.compareTo(BigDecimal.ZERO) > 0 && factorBD.compareTo(BigDecimal.ONE) < 0) {
                // Shrinking sequence
                shouldContinue = (startBD.compareTo(endBD) > 0) ? current.compareTo(endBD) >= 0 : current.compareTo(endBD) <= 0;
            } else {
                throw new RuntimeException("Unsupported multiplication factor in loops");
            }
        } else if (operation.equals("/")) {
            
            if (factorBD.compareTo(BigDecimal.ONE) > 0) {
                // Shrinking sequence (multiplying by a fraction)
                shouldContinue = (startBD.compareTo(endBD) > 0) ? current.compareTo(endBD) >= 0 : current.compareTo(endBD) <= 0;
            } else if (factorBD.compareTo(BigDecimal.ZERO) > 0 && factorBD.compareTo(BigDecimal.ONE) < 0) {
                // Growing sequence (multiplying by a number > 1)
                shouldContinue = (startBD.compareTo(endBD) < 0) ? current.compareTo(endBD) <= 0 : current.compareTo(endBD) >= 0;
            } else {
                throw new RuntimeException("Unsupported division factor in loops");
            }
        }
        
        if (!shouldContinue) break;
        
        // Store current value
        Object currentValue = convertToAppropriateType(current, startObj);
        
        ctx.locals.put(iter, currentValue);
        if (!ctx.localTypes.containsKey(iter)) {
            String inferredType = (currentValue instanceof Integer || currentValue instanceof Long) ? INT.toString() : FLOAT.toString();
            ctx.localTypes.put(iter, inferredType);
        }
        
        for (StmtNode s : node.body.statements) {
            dispatch(s);
            if (!ctx.slotsInCurrentPath.isEmpty() && 
                interpreter.shouldReturnEarly(ctx.slotValues, ctx.slotsInCurrentPath)) {
                return null;
            }
        }
        
        // Apply precise multiplicative operation
        if (operation.equals("*")) {
            current = current.multiply(factorBD);
        } else if (operation.equals("/")) {
            current = current.divide(factorBD, DECIMAL_SCALE, RoundingMode.HALF_UP);
        }
    }
    return null;
}

    @Override
public Object visit(ReturnSlotAssignmentNode node) {
    ExecutionContext ctx = getCurrentContext();
    Object res = interpreter.evalMethodCall(node.methodCall, ctx.objectInstance, ctx.locals);
    
    if (res instanceof Map) {
        Map<String, Object> map = (Map<String, Object>) res;
        
        MethodNode method = null;
        if (ctx.objectInstance.type != null) {
            for (MethodNode m : ctx.objectInstance.type.methods) {
                if (m.name.equals(node.methodCall.name)) { 
                    method = m; 
                    break; 
                }
            }
        }
        
        for (int i = 0; i < node.variableNames.size(); i++) {
            String slot = node.methodCall.slotNames.get(i);
            
            String requestedSlot = slot;
            if (!map.containsKey(requestedSlot) && method != null && method.returnSlots != null) {
                try {
                    int index = Integer.parseInt(requestedSlot);
                    if (index >= 0 && index < method.returnSlots.size()) {
                        requestedSlot = method.returnSlots.get(index).name;
                    }
                } catch (NumberFormatException e) {
                }
            }
            
            if (map.containsKey(requestedSlot)) {
                Object val = map.get(requestedSlot);
                ctx.locals.put(node.variableNames.get(i), val);
            } else {
                throw new RuntimeException("Missing slot: " + slot + " (resolved as: " + requestedSlot + ")");
            }
        }
    } else {
        throw new RuntimeException("Method did not return slot values");
    }
    return res;
}

    private Object evaluateConditionalChain(MethodCallNode call) {
        boolean result = ALL.toString().equals(call.chainType);
        
        for (ExprNode arg : call.chainArguments) {
            MethodCallNode currentCall = ASTFactory.createMethodCall(call.name, call.qualifiedName);
            currentCall.arguments = new ArrayList<ExprNode>();
            
            if (arg instanceof ArgumentListNode) {
                currentCall.arguments.addAll(((ArgumentListNode) arg).arguments);
            } else {
                currentCall.arguments.add(arg);
            }
            
            boolean negated = false;

            if (arg instanceof UnaryNode && "!".equals(((UnaryNode) arg).op)) {
                negated = true;
            }
            
            Object methodResultObj = dispatch(currentCall);
            methodResultObj = typeSystem.unwrap(methodResultObj); 
            
            boolean methodResult = isTruthy(methodResultObj);
            boolean finalResult = negated ? !methodResult : methodResult;
            
            if (ALL.toString().equals(call.chainType)) {
                if (!finalResult) return false;
            } else { 
                if (finalResult) return true;
            }
        }
        return result;
    }

private boolean isTruthy(Object value) {
    if (value instanceof TypedValue) throw new RuntimeException("isTruthy called on unwrapped value."); 
    
    if (value == null) return false;
    if (value instanceof Boolean) return (Boolean) value;
    if (value instanceof Number) {
        if (value instanceof BigDecimal) return ((BigDecimal) value).compareTo(BigDecimal.ZERO) != 0;
        return ((Number) value).doubleValue() != 0.0;
    }
    if (value instanceof String) {
        String str = (String) value;
        if (str.equalsIgnoreCase("true")) return true;
        if (str.equalsIgnoreCase("false")) return false;
        return !str.isEmpty();
    }
    if (value instanceof List) return !((List) value).isEmpty();
    if (value instanceof NaturalArray) return ((NaturalArray) value).size() > 0;
    return true;
}

    private void validateSlotType(ExecutionContext ctx, String slotName, Object value) {
        if (ctx.slotTypes == null || !ctx.slotTypes.containsKey(slotName) || value == null) return;
        String type = ctx.slotTypes.get(slotName);
        if (!typeSystem.validateType(type, value)) {
             throw new RuntimeException("Type mismatch: " + slotName + " expected " + type);
        }
    }
}