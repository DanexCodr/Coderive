package cod.interpreter;

import cod.ast.nodes.*;
import cod.interpreter.context.ExecutionContext;
import cod.interpreter.type.TypeSystem;
import cod.interpreter.type.TypeValue;
import cod.range.MultiRangeSpec;
import cod.range.RangeSpec;

import java.math.BigDecimal;

public class ExpressionHandler {
    private final TypeSystem typeSystem;
    private final InterpreterVisitor dispatcher;
    private final TypeHandler typeHandler;
    
    public ExpressionHandler(TypeSystem typeSystem, InterpreterVisitor dispatcher) {
        this.typeSystem = typeSystem;
        this.dispatcher = dispatcher;
        this.typeHandler = new TypeHandler(typeSystem);  // Initialize TypeHandler
    }
    
    // === Core Expression Evaluation ===
    
    public Object handleBinaryOp(BinaryOpNode node, ExecutionContext ctx) {
        Object left = dispatcher.dispatch(node.left);
        Object right = dispatcher.dispatch(node.right);
        Object result = null;

        switch (node.op) {
            case "+":
            case "+=":
                if (left instanceof String || right instanceof String) {
                    Object unwrappedLeft = typeSystem.unwrap(left);
                    Object unwrappedRight = typeSystem.unwrap(right);

                    if (unwrappedLeft instanceof BigDecimal) {
                        BigDecimal bdLeft = ((BigDecimal) unwrappedLeft).stripTrailingZeros();
                        unwrappedLeft = bdLeft.toPlainString();
                    }
                    if (unwrappedRight instanceof BigDecimal) {
                        BigDecimal bdRight = ((BigDecimal) unwrappedRight).stripTrailingZeros();
                        unwrappedRight = bdRight.toPlainString();
                    }

                    result = String.valueOf(unwrappedLeft) + String.valueOf(unwrappedRight);
                } else {
                    result = typeSystem.addNumbers(left, right);
                }
                break;

            case "*":
            case "*=":
                result = typeSystem.multiplyNumbers(left, right);
                break;

            case "-":
            case "-=":
                result = typeSystem.subtractNumbers(left, right);
                break;

            case "/":
            case "/=":
                result = typeSystem.divideNumbers(left, right);
                break;

            case "%":
                result = typeSystem.modulusNumbers(left, right);
                break;

            case ">":
                result = typeSystem.compare(left, right) > 0;
                break;

            case "<":
                result = typeSystem.compare(left, right) < 0;
                break;

            case ">=":
                result = typeSystem.compare(left, right) >= 0;
                break;

            case "<=":
                result = typeSystem.compare(left, right) <= 0;
                break;

            case "=":
                result = right;
                break;

            case "==":
                result = typeSystem.areEqual(left, right);
                break;

            case "!=":
                result = !typeSystem.areEqual(left, right);
                break;
                
            case "is": {
                return handleIsOperator(left, right);
            }

            default:
                throw new RuntimeException("Unknown operator: " + node.op);
        }
        return result;
    }
    
    public Object handleUnaryOp(UnaryNode node, ExecutionContext ctx) {
        Object operand = dispatcher.dispatch(node.operand);

        switch (node.op) {
            case "-":
                return typeSystem.negateNumber(operand);
            case "+":
                return operand;
            case "!":
                return !typeHandler.isTruthy(operand);  // Use TypeHandler.isTruthy()
            default:
                throw new RuntimeException("Unknown unary operator: " + node.op);
        }
    }
    
    public Object handleTypeCast(TypeCastNode node, ExecutionContext ctx) {
        Object val = dispatcher.dispatch(node.expression);
        if (!typeSystem.validateType(node.targetType, val)) {
            return typeSystem.convertType(val, node.targetType);
        }
        return val;
    }
    
    public Object handleBooleanChain(BooleanChainNode node, ExecutionContext ctx) {
        boolean isAll = node.isAll;

        for (ExprNode expr : node.expressions) {
            Object result = dispatcher.dispatch(expr);
            result = typeSystem.unwrap(result);
            boolean isTruthy = typeHandler.isTruthy(result);  // Use TypeHandler.isTruthy()

            if (isAll) {
                if (!isTruthy) return false;
            } else {
                if (isTruthy) return true;
            }
        }

        return isAll;
    }
    
    public Object handleEqualityChain(EqualityChainNode node, ExecutionContext ctx) {
        Object leftValue = dispatcher.dispatch(node.left);
        leftValue = typeSystem.unwrap(leftValue);

        for (ExprNode arg : node.chainArguments) {
            Object rightValue = dispatcher.dispatch(arg);
            rightValue = typeSystem.unwrap(rightValue);

            boolean comparisonResult;
            switch (node.operator) {
                case "==":
                    comparisonResult = typeSystem.unwrap(leftValue).equals(typeSystem.unwrap(rightValue));
                    break;
                case "!=":
                    comparisonResult = !typeSystem.unwrap(leftValue).equals(typeSystem.unwrap(rightValue));
                    break;
                case ">":
                    comparisonResult = typeSystem.compare(leftValue, rightValue) > 0;
                    break;
                case "<":
                    comparisonResult = typeSystem.compare(leftValue, rightValue) < 0;
                    break;
                case ">=":
                    comparisonResult = typeSystem.compare(leftValue, rightValue) >= 0;
                    break;
                case "<=":
                    comparisonResult = typeSystem.compare(leftValue, rightValue) <= 0;
                default:
                    throw new RuntimeException(
                        "Unknown comparison operator in equality chain: " + node.operator);
            }

            if (node.isAllChain) {
                if (!comparisonResult) {
                    return false;
                }
            } else {
                if (comparisonResult) {
                    return true;
                }
            }
        }

        return node.isAllChain;
    }
    
    // === Type/Value Conversion ===
    
    public long toLong(Object obj) {
        if (obj instanceof Integer) return ((Integer) obj).longValue();
        if (obj instanceof Long) return (Long) obj;
        if (obj instanceof BigDecimal) return ((BigDecimal) obj).longValue();
        throw new RuntimeException("Cannot convert to long: " + obj);
    }
    
    public long toLongIndex(Object indexObj) {
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
            return (long) d;
        }

        if (indexObj instanceof BigDecimal) {
            BigDecimal bd = (BigDecimal) indexObj;
            try {
                return bd.longValueExact();
            } catch (ArithmeticException e) {
                throw new RuntimeException(
                    "Array index must be an exact integer, got: BigDecimal (" + bd + ")");
            }
        }
        
        if (indexObj instanceof RangeSpec || indexObj instanceof MultiRangeSpec) {
            throw new RuntimeException("Cannot use range as index for non-range indexing");
        }

        throw new RuntimeException(
            "Array index must be integer, got: " + indexObj.getClass().getSimpleName());
    }
    
    public int toIntIndex(Object indexObj) {
        if (indexObj == null) {
            throw new RuntimeException("Array index cannot be null");
        }
        
        if (indexObj instanceof Integer) return (Integer) indexObj;
        if (indexObj instanceof Long) return ((Long) indexObj).intValue();

        if (indexObj instanceof BigDecimal) {
            return ((BigDecimal) indexObj).intValue();
        }

        throw new RuntimeException("Array index must be integer");
    }
    
    public long calculateStep(RangeSpec range) {
        if (range.step != null) {
            return toLongIndex(range.step);
        } else {
            long start = toLongIndex(range.start);
            long end = toLongIndex(range.end);
            if (start == end) return 1L;
            return (start < end) ? 1L : -1L;
        }
    }
    
    // === Type Checking ===
    
    private Object handleIsOperator(Object leftValue, Object rightValue) {
        leftValue = typeSystem.unwrap(leftValue);
        rightValue = typeSystem.unwrap(rightValue);
        
        // Case 1: Right side is a type value (TypeValue with activeType == "type")
        if (rightValue instanceof TypeValue) {
            TypeValue typeVal = (TypeValue) rightValue;
            if (typeVal.isTypeValue()) {
                return typeVal.matches(leftValue);
            }
        }
        
        // Case 2: Right side is a type string
        if (rightValue instanceof String) {
            String typeString = (String) rightValue;
            
            // Check if it's a built-in type name - use TypeHandler.isTypeLiteral()
            if (typeHandler.isTypeLiteral(typeString)) {
                String leftType = typeSystem.getConcreteType(leftValue);
                return typeString.equals(leftType);
            }
            
            // Check if it's a complex type signature
            if (typeString.startsWith("[") || typeString.startsWith("(") || typeString.contains("|")) {
                // Use the type system to validate
                return typeSystem.validateType(typeString, leftValue);
            }
        }
        
        // Case 3: Regular comparison (fallback)
        return typeSystem.areEqual(leftValue, rightValue);
    }
}