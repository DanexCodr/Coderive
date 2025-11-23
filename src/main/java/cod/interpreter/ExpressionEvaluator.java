package cod.interpreter;

import cod.ast.nodes.*;
import cod.ast.ASTFactory;
import cod.debug.DebugSystem;
import java.util.*;

public class ExpressionEvaluator {
    private TypeSystem typeSystem;
    private Interpreter interpreter;

    public ExpressionEvaluator(TypeSystem typeSystem, Interpreter interpreter) {
        this.typeSystem = typeSystem;
        this.interpreter = interpreter;
    }

        public Object evaluate(ExprNode expr, ObjectInstance obj, Map<String, Object> locals) {
        DebugSystem.trace("EXPRESSIONS", "Evaluating: " + expr.getClass().getSimpleName());

        // --- NEW: Handles 'if all:(A, B)' and 'if any: A, B' ---
        if (expr instanceof BooleanChainNode) {
            return evaluateBooleanChain((BooleanChainNode) expr, obj, locals);
        }
        // ... (UnaryNode, EqualityChainNode, TypeCastNode, ArrayNode, IndexAccessNode) ...

        if (expr instanceof UnaryNode) {
            UnaryNode unary = (UnaryNode) expr;
            Object operand = evaluate(unary.operand, obj, locals);
            DebugSystem.debug("EXPRESSIONS", "Unary operation: " + unary.op + " " + operand);

            switch (unary.op) {
                case "-":
                    return typeSystem.negateNumber(operand);
                case "+":
                    return operand;
                case "!":
                    return !isTruthy(operand);
                default:
                    throw new RuntimeException("Unknown unary operator: " + unary.op);
            }
        }

        if (expr instanceof EqualityChainNode) {
            return evaluateEqualityChain((EqualityChainNode) expr, obj, locals);
        }

        if (expr instanceof TypeCastNode) {
            TypeCastNode cast = (TypeCastNode) expr;
            Object value = evaluate(cast.expression, obj, locals);
            DebugSystem.debug("TYPECAST", "Casting " + value + " to " + cast.targetType);
            return typeSystem.convertType(value, cast.targetType);

        } else if (expr instanceof ArrayNode) {
            ArrayNode arrayNode = (ArrayNode) expr;
            List<Object> array = new ArrayList<Object>();
            for (ExprNode elem : arrayNode.elements) {
                array.add(evaluate(elem, obj, locals));
            }
            DebugSystem.debug("ARRAYS", "Created array with " + array.size() + " elements");
            return array;

        } else if (expr instanceof IndexAccessNode) {
            IndexAccessNode indexNode = (IndexAccessNode) expr;
            Object arrayObj = evaluate(indexNode.array, obj, locals);
            Object indexObj = evaluate(indexNode.index, obj, locals);

            if (arrayObj instanceof List && indexObj instanceof Integer) {
                List<Object> list = (List<Object>) arrayObj;
                int index = (Integer) indexObj;

                if (index >= 0 && index < list.size()) {
                    Object value = list.get(index);
                    DebugSystem.debug("ARRAYS", "Array access: [" + index + "] = " + value);
                    return value;
                } else {
                    throw new RuntimeException(
                            "Array index out of bounds: "
                                    + index
                                    + " for array size "
                                    + list.size());
                }
            } else {
                throw new RuntimeException(
                        "Invalid array access - array: "
                                + arrayObj.getClass()
                                + ", index: "
                                + indexObj.getClass());
            }

       } else if (expr instanceof BinaryOpNode) {
            BinaryOpNode binOp = (BinaryOpNode) expr;
            
            boolean isForLoopStep = isForLoopStepEvaluation(binOp, obj, locals);
            
            if (isAssignmentOperator(binOp.op) && !isForLoopStep) {
                throw new RuntimeException("Assignment operator '" + binOp.op + "' cannot be used in expressions - use statements instead");
            }
            
            Object left = evaluate(binOp.left, obj, locals);
            Object right = evaluate(binOp.right, obj, locals);
            DebugSystem.debug(
                    "EXPRESSIONS", "Binary operation: " + left + " " + binOp.op + " " + right);

            switch (binOp.op) {
                case "+":
                    return (left instanceof String || right instanceof String)
                            ? String.valueOf(left) + right
                            : typeSystem.addNumbers(left, right);
                case "-":
                    return typeSystem.subtractNumbers(left, right);
                case "*":
                    return typeSystem.multiplyNumbers(left, right);
                case "/":
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
                case "==":
                    return left.equals(right);
                case "!=":
                    return !left.equals(right);
                case "=":
                    return right;
                case "+=":
                    return typeSystem.addNumbers(left, right);
                case "-=":
                    return typeSystem.subtractNumbers(left, right);
                case "*=":
                    return typeSystem.multiplyNumbers(left, right);
                case "/=":
                    return typeSystem.divideNumbers(left, right);
                default:
                    throw new RuntimeException("Unknown operator: " + binOp.op);
            }
        } else if (expr instanceof MethodCallNode) {
            MethodCallNode call = (MethodCallNode) expr;
            
            if (call.chainType != null && call.chainArguments != null) {
                return evaluateConditionalChain(call, obj, locals);
            }
    
            DebugSystem.debug("EXPR_METHOD_CALL", "=== EXPRESSION METHOD CALL ===");
            DebugSystem.debug("EXPR_METHOD_CALL", "call.name: " + call.name);
            DebugSystem.debug("EXPR_METHOD_CALL", "call.qualifiedName: " + call.qualifiedName);
            DebugSystem.debug("EXPR_METHOD_CALL", "call.slotNames: " + call.slotNames);
            DebugSystem.debug("EXPR_METHOD_CALL", "call.arguments: " + call.arguments.size());
            for (int i = 0; i < call.arguments.size(); i++) {
                DebugSystem.debug("EXPR_METHOD_CALL", "  arg[" + i + "]: " + call.arguments.get(i).getClass().getSimpleName());
            }

            Object result = interpreter.evalMethodCall(call, obj, locals);

            if (call.slotNames != null && !call.slotNames.isEmpty() && result instanceof Map) {
                Map<String, Object> slotReturns = (Map<String, Object>) result;
                for (String slotName : call.slotNames) {
                    if (slotReturns.containsKey(slotName)) {
                        Object slotValue = slotReturns.get(slotName);
                        DebugSystem.debug(
                                "SLOTS",
                                "Slot '" + slotName + "' extracted in expression (read-only)");
                    }
                }
            }

            return result;
        } else if (expr.name != null) {
            Object value = null;
            if (locals.containsKey(expr.name)) {
                value = locals.get(expr.name);
                DebugSystem.trace("MEMORY", "Found local: " + expr.name + " = " + value);
            } else if (interpreter.currentSlots != null
                    && interpreter.currentSlots.containsKey(expr.name)) {
                value = interpreter.currentSlots.get(expr.name);
                DebugSystem.trace("MEMORY", "Found slot: " + expr.name + " = " + value);
            } else if (obj.fields.containsKey(expr.name)) {
                value = obj.fields.get(expr.name);
                DebugSystem.trace("MEMORY", "Found field: " + expr.name + " = " + value);
            } else {
                throw new RuntimeException("Undefined Variable: " + expr.name);
            }
            return value;
        } else if (expr.value != null) {
            DebugSystem.trace("EXPRESSIONS", "Literal value: " + expr.value);
            if (expr.value instanceof String) {
                String s = (String) expr.value;
                if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2)
                    s = s.substring(1, s.length() - 1);
                return s;
            }
            return expr.value;
        }
        return null;
    }

    private Object evaluateEqualityChain(EqualityChainNode chain, ObjectInstance obj, Map<String, Object> locals) {
        
        // --- NEW: Handle Array-based check (e.g., 'all scores >= 60') ---
        Object leftObj = evaluate(chain.left, obj, locals);
        
        List<Object> targetValues;
        
        if (leftObj instanceof List) {
            // Case 1: Array-based check (e.g., 'all scores >= 60')
            targetValues = (List<Object>) leftObj;
        } else {
            // Case 2: Standard check (e.g., 'x == any[1, 2, 3]') or Reverse Check (e.g., 'any[1, 2, 3] == x')
            // In both these cases, the "target" of the comparison is the single leftObj
            targetValues = new ArrayList<Object>();
            targetValues.add(leftObj);
        }
        
        boolean isAllChain = chain.isAllChain;
        boolean result = isAllChain;
        
        DebugSystem.debug("EQUALITY_CHAIN", "Starting " + (isAllChain ? "all" : "any") + " chain with operator: " + chain.operator);
        
        // If it's an array check, the chain arguments must have a single value (e.g., 60)
        // If it's a standard check, the targetValues has a single value (e.g., x)
        
        List<ExprNode> comparisonValues = chain.chainArguments;
        
        if (targetValues.size() > 1 && comparisonValues.size() > 1) {
            throw new RuntimeException("Ambiguous chain: Cannot compare array against array/list of values in an equality chain.");
        }
        
        // --- NEW Logic for array iteration (Case 1) ---
        if (targetValues.size() > 1) {
             Object rightValue = comparisonValues.size() == 1 ? evaluate(comparisonValues.get(0), obj, locals) : null;
             
             for (Object targetValue : targetValues) {
                 boolean currentResult = compareValues(targetValue, chain.operator, rightValue);

                 if (isAllChain) {
                    result = result && currentResult;
                    if (!result) {
                        DebugSystem.debug("EQUALITY_CHAIN", "ALL array chain short-circuited at false");
                        break;
                    }
                } else {
                    result = result || currentResult;
                    if (result) {
                        DebugSystem.debug("EQUALITY_CHAIN", "ANY array chain short-circuited at true");
                        break;
                    }
                }
             }
             
        } else {
            // --- Original Logic for single-value check (Case 2 and Reverse) ---
            Object fixedLeftValue = targetValues.get(0);
            
            for (ExprNode chainArg : comparisonValues) {
                Object rightValue = evaluate(chainArg, obj, locals);
                boolean currentResult = compareValues(fixedLeftValue, chain.operator, rightValue);
                
                if (isAllChain) {
                    result = result && currentResult;
                    if (!result) {
                        DebugSystem.debug("EQUALITY_CHAIN", "ALL chain short-circuited at false");
                        break;
                    }
                } else {
                    result = result || currentResult;
                    if (result) {
                        DebugSystem.debug("EQUALITY_CHAIN", "ANY chain short-circuited at true");
                        break;
                    }
                }
            }
        }
        
        DebugSystem.debug("EQUALITY_CHAIN", "Equality chain result: " + result);
        return result;
    }
    
    // --- NEW: Comparison helper method to clean up chain logic ---
    private boolean compareValues(Object leftValue, String operator, Object rightValue) {
        switch (operator) {
            case "==":
                return leftValue.equals(rightValue);
            case "!=":
                return !leftValue.equals(rightValue);
            case ">":
                return typeSystem.compare(leftValue, rightValue) > 0;
            case "<":
                return typeSystem.compare(leftValue, rightValue) < 0;
            case ">=":
                return typeSystem.compare(leftValue, rightValue) >= 0;
            case "<=":
                return typeSystem.compare(leftValue, rightValue) <= 0;
            default:
                throw new RuntimeException("Unsupported operator in equality chain: " + operator);
        }
    }

    private Object evaluateBooleanChain(BooleanChainNode chain, ObjectInstance obj, Map<String, Object> locals) {
        boolean isAll = chain.isAll;
        boolean result = isAll; 
        
        DebugSystem.debug("BOOL_CHAIN", "Starting " + (isAll ? "all" : "any") + " boolean chain");

        if (isAll) {
             result = true;
             for (ExprNode e : chain.expressions) {
                 Object val = evaluate(e, obj, locals);
                 if (!isTruthy(val)) {
                     DebugSystem.debug("BOOL_CHAIN", "ALL chain short-circuited (found false)");
                     result = false;
                     break;
                 }
             }
        } else {
             result = false;
             for (ExprNode e : chain.expressions) {
                 Object val = evaluate(e, obj, locals);
                 if (isTruthy(val)) {
                     DebugSystem.debug("BOOL_CHAIN", "ANY chain short-circuited (found true)");
                     result = true;
                     break;
                 }
             }
        }
        return result;
    }

    private Object evaluateConditionalChain(MethodCallNode call, ObjectInstance obj, Map<String, Object> locals) {
        boolean isAllChain = "all".equals(call.chainType);
        boolean result = isAllChain;
        
        DebugSystem.debug("CHAIN", "Starting " + call.chainType + " chain for method: " + call.name);
        
        for (ExprNode chainArg : call.chainArguments) {
            MethodCallNode singleCall = ASTFactory.createMethodCall(call.name, call.qualifiedName);
            singleCall.arguments = new ArrayList<ExprNode>();
            
            boolean currentResult;
            if (chainArg instanceof UnaryNode && "!".equals(((UnaryNode)chainArg).op)) {
                UnaryNode unary = (UnaryNode) chainArg;
                singleCall.arguments.add(unary.operand);
                Object methodResult = interpreter.evalMethodCall(singleCall, obj, locals);
                currentResult = !isTruthy(methodResult);
                DebugSystem.debug("CHAIN", "Negated argument: !" + unary.operand + " = " + currentResult);
            } else {
                singleCall.arguments.add(chainArg);
                Object methodResult = interpreter.evalMethodCall(singleCall, obj, locals);
                currentResult = isTruthy(methodResult);
                DebugSystem.debug("CHAIN", "Regular argument: " + chainArg + " = " + currentResult);
            }
            
            if (isAllChain) {
                result = result && currentResult;
                if (!result) {
                    DebugSystem.debug("CHAIN", "ALL chain short-circuited at false");
                    break;
                }
            } else {
                result = result || currentResult;
                if (result) {
                    DebugSystem.debug("CHAIN", "ANY chain short-circuited at true");
                    break;
                }
            }
        }
        
        DebugSystem.debug("CHAIN", "Conditional chain result: " + result + " for " + call.chainType);
        return result;
    }

    private boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).doubleValue() != 0.0;
        if (value instanceof String) return !((String) value).isEmpty();
        if (value instanceof List) return !((List) value).isEmpty();
        if (value instanceof Map) return !((Map) value).isEmpty();
        return true;
    }
    
    private boolean isAssignmentOperator(String op) {
        return op.equals("=") || op.equals("+=") || op.equals("-=") || 
               op.equals("*=") || op.equals("/=");
    }
    
    private boolean isForLoopStepEvaluation(BinaryOpNode binOp, ObjectInstance obj, Map<String, Object> locals) {
        DebugSystem.debug("FOR_LOOP_STEP", "Checking if binary op is for loop step: " + binOp.op);
        return true;
    }
}