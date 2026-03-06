package cod.interpreter.handler;

import cod.ast.nodes.*;
import cod.error.InternalError;
import cod.error.ProgramError;
import cod.math.AutoStackingNumber;
import cod.interpreter.context.ExecutionContext;
import cod.interpreter.InterpreterVisitor;
import cod.range.MultiRangeSpec;
import cod.range.NaturalArray;
import cod.range.RangeSpec;

public class ExpressionHandler {
    private final TypeHandler typeSystem;
    private final InterpreterVisitor dispatcher;
    
    public ExpressionHandler(TypeHandler typeSystem, InterpreterVisitor dispatcher) {
        if (typeSystem == null) {
            throw new InternalError("ExpressionHandler constructed with null typeSystem");
        }
        if (dispatcher == null) {
            throw new InternalError("ExpressionHandler constructed with null dispatcher");
        }
        this.typeSystem = typeSystem;
        this.dispatcher = dispatcher;
    }
    
    // === Core Expression Evaluation ===
    
    public Object handleBinaryOp(BinaryOpNode node, ExecutionContext ctx) {
    if (node == null) {
        throw new InternalError("handleBinaryOp called with null node");
    }
    if (ctx == null) {
        throw new InternalError("handleBinaryOp called with null context");
    }
    
    try {
        Object left = dispatcher.dispatch(node.left);
        Object right = dispatcher.dispatch(node.right);
        Object result = null;

        switch (node.op) {
            case "+":
            case "+=":
                if (left instanceof String || right instanceof String ||
                    left instanceof TextLiteralNode || right instanceof TextLiteralNode) {
                    
                    // === FIX: Force materialization before string conversion ===
                    Object unwrappedLeft = typeSystem.unwrap(left);
                    Object unwrappedRight = typeSystem.unwrap(right);
                    
                    if (unwrappedLeft instanceof NaturalArray) {
                        NaturalArray arr = (NaturalArray) unwrappedLeft;
                        if (arr.hasPendingUpdates()) {
                            arr.commitUpdates();
                        }
                    }
                    
                    if (unwrappedRight instanceof NaturalArray) {
                        NaturalArray arr = (NaturalArray) unwrappedRight;
                        if (arr.hasPendingUpdates()) {
                            arr.commitUpdates();
                        }
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
                throw new ProgramError("Unknown operator: " + node.op);
        }
        return result;
    } catch (ProgramError e) {
        throw e;
    } catch (Exception e) {
        throw new InternalError("Binary operation failed: " + node.op, e);
    }
}
    
    public Object handleUnaryOp(UnaryNode node, ExecutionContext ctx) {
        if (node == null) {
            throw new InternalError("handleUnaryOp called with null node");
        }
        if (ctx == null) {
            throw new InternalError("handleUnaryOp called with null context");
        }
        
        try {
            Object operand = dispatcher.dispatch(node.operand);

            switch (node.op) {
                case "-":
                    return typeSystem.negateNumber(operand);
                case "+":
                    return operand;
                case "!":
                    return !typeSystem.isTruthy(operand);
                default:
                    throw new ProgramError("Unknown unary operator: " + node.op);
            }
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Unary operation failed: " + node.op, e);
        }
    }
    
    public Object handleTypeCast(TypeCastNode node, ExecutionContext ctx) {
        if (node == null) {
            throw new InternalError("handleTypeCast called with null node");
        }
        if (ctx == null) {
            throw new InternalError("handleTypeCast called with null context");
        }
        
        try {
            Object val = dispatcher.dispatch(node.expression);
            if (!typeSystem.validateType(node.targetType, val)) {
                return typeSystem.convertType(val, node.targetType);
            }
            return val;
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Type cast failed to " + node.targetType, e);
        }
    }
    
    public Object handleBooleanChain(BooleanChainNode node, ExecutionContext ctx) {
        if (node == null) {
            throw new InternalError("handleBooleanChain called with null node");
        }
        if (ctx == null) {
            throw new InternalError("handleBooleanChain called with null context");
        }
        
        try {
            boolean isAll = node.isAll;

            for (ExprNode expr : node.expressions) {
                Object result = dispatcher.dispatch(expr);
                result = typeSystem.unwrap(result);
                boolean isTruthy = typeSystem.isTruthy(result);

                if (isAll) {
                    if (!isTruthy) return false;
                } else {
                    if (isTruthy) return true;
                }
            }

            return isAll;
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Boolean chain evaluation failed", e);
        }
    }
    
    public Object handleEqualityChain(EqualityChainNode node, ExecutionContext ctx) {
        if (node == null) {
            throw new InternalError("handleEqualityChain called with null node");
        }
        if (ctx == null) {
            throw new InternalError("handleEqualityChain called with null context");
        }
        
        try {
            Object leftValue = dispatcher.dispatch(node.left);
            leftValue = typeSystem.unwrap(leftValue);

            for (ExprNode arg : node.chainArguments) {
                Object rightValue = dispatcher.dispatch(arg);
                rightValue = typeSystem.unwrap(rightValue);

                boolean comparisonResult;
                switch (node.operator) {
                    case "==":
                        comparisonResult = typeSystem.areEqual(leftValue, rightValue);
                        break;
                    case "!=":
                        comparisonResult = !typeSystem.areEqual(leftValue, rightValue);
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
                        break;
                    default:
                        throw new ProgramError(
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
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Equality chain evaluation failed", e);
        }
    }
    
    // === Type/Value Conversion ===
    
    public long toLong(Object obj) {
        try {
            if (obj instanceof AutoStackingNumber) {
                return ((AutoStackingNumber) obj).longValue();
            }
            if (obj instanceof IntLiteralNode) {
                return ((IntLiteralNode) obj).value.longValue();
            }
            if (obj instanceof Integer) return ((Integer) obj).longValue();
            if (obj instanceof Long) return (Long) obj;
            throw new ProgramError("Cannot convert to long: " + 
                (obj != null ? obj.getClass().getSimpleName() + " with value " + obj : "null"));
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Long conversion failed", e);
        }
    }
    
    public long toLongIndex(Object indexObj) {
        if (indexObj == null) {
            throw new ProgramError("Array index cannot be null");
        }

        try {
            if (indexObj instanceof AutoStackingNumber) {
                AutoStackingNumber num = (AutoStackingNumber) indexObj;
                try {
                    return num.longValue();
                } catch (ArithmeticException e) {
                    throw new ProgramError(
                        "Array index must be an exact integer, got: " + num);
                }
            }

            if (indexObj instanceof IntLiteralNode) {
                return ((IntLiteralNode) indexObj).value.longValue();
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
                    throw new ProgramError("Array index must be integer, got: Double (" + d + ")");
                }
                return (long) d;
            }
            
            if (indexObj instanceof RangeSpec) {
                throw new ProgramError("Cannot use range as index for single element access");
            }
            
            if (indexObj instanceof MultiRangeSpec) {
                throw new ProgramError("Cannot use multi-range as index for single element access");
            }

            throw new ProgramError(
                "Array index must be integer, got: " + 
                (indexObj != null ? indexObj.getClass().getSimpleName() : "null"));
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Index conversion failed", e);
        }
    }
    
    public int toIntIndex(Object indexObj) {
        if (indexObj == null) {
            throw new ProgramError("Array index cannot be null");
        }
        
        try {
            if (indexObj instanceof AutoStackingNumber) {
                return (int) ((AutoStackingNumber) indexObj).longValue();
            }
            
            if (indexObj instanceof IntLiteralNode) {
                long val = ((IntLiteralNode) indexObj).value.longValue();
                if (val > Integer.MAX_VALUE || val < Integer.MIN_VALUE) {
                    throw new ProgramError("Index out of range for integer array: " + val);
                }
                return (int) val;
            }
            
            if (indexObj instanceof Integer) return (Integer) indexObj;
            if (indexObj instanceof Long) {
                long val = (Long) indexObj;
                if (val > Integer.MAX_VALUE || val < Integer.MIN_VALUE) {
                    throw new ProgramError("Long index " + val + " cannot fit in int");
                }
                return (int) val;
            }

            throw new ProgramError("Array index must be integer, got: " +
                (indexObj != null ? indexObj.getClass().getSimpleName() : "null"));
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Int index conversion failed", e);
        }
    }
    
    public long calculateStep(RangeSpec range) {
        if (range == null) {
            throw new InternalError("calculateStep called with null range");
        }
        
        try {
            if (range.step != null) {
                return toLongIndex(range.step);
            } else {
                long start = toLongIndex(range.start);
                long end = toLongIndex(range.end);
                if (start == end) return 1L;
                return (start < end) ? 1L : -1L;
            }
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Step calculation failed", e);
        }
    }
    
    // === Type Checking ===
    
    private Object handleIsOperator(Object leftValue, Object rightValue) {
        try {
            leftValue = typeSystem.unwrap(leftValue);
            rightValue = typeSystem.unwrap(rightValue);
            
            if (rightValue instanceof TypeHandler.Value) {
                TypeHandler.Value typeVal = (TypeHandler.Value) rightValue;
                if (typeVal.isTypeValue()) {
                    return typeVal.matches(leftValue);
                }
            }
            
            if (rightValue instanceof String) {
                String typeString = (String) rightValue;
                
                if (typeSystem.isTypeLiteral(typeString)) {
                    String leftType = typeSystem.getConcreteType(leftValue);
                    return typeString.equals(leftType);
                }
                
                if (typeString.startsWith("[") || typeString.startsWith("(") || typeString.contains("|")) {
                    return typeSystem.validateType(typeString, leftValue);
                }
            }

            if (rightValue instanceof TextLiteralNode) {
                String typeString = ((TextLiteralNode) rightValue).value;
                
                if (typeSystem.isTypeLiteral(typeString)) {
                    String leftType = typeSystem.getConcreteType(leftValue);
                    return typeString.equals(leftType);
                }
                
                if (typeString.startsWith("[") || typeString.startsWith("(") || typeString.contains("|")) {
                    return typeSystem.validateType(typeString, leftValue);
                }
            }
            
            return typeSystem.areEqual(leftValue, rightValue);
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("'is' operator evaluation failed", e);
        }
    }
}