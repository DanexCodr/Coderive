package cod.interpreter.registry;

import cod.ast.nodes.*;
import cod.error.ProgramError;
import cod.interpreter.context.ExecutionContext;
import cod.interpreter.Evaluator;
import cod.interpreter.handler.TypeHandler;
import cod.range.NaturalArray;
import java.util.*;

/**
 * Registry for handling property access and method calls on literal values.
 * Provides extension points for adding behaviors to literals like ranges, numbers, strings, etc.
 */
public class LiteralRegistry {
    
    // Property handlers: property name -> handler
    private final Map<String, PropertyHandler> propertyHandlers;
    
    // Track which types support which properties: property name -> set of types
    private final Map<String, Set<Class<?>>> propertyTypes;
    
    // Method handlers: method name -> handler
    private final Map<String, MethodHandler> methodHandlers;
    
    // Track which types support which methods: method name -> set of types
    private final Map<String, Set<Class<?>>> methodTypes;
    
    // Evaluator for evaluating expressions
    private Evaluator evaluator;
    
    public LiteralRegistry(Evaluator evaluator) {
        this.evaluator = evaluator;
        this.propertyHandlers = new HashMap<String, PropertyHandler>();
        this.propertyTypes = new HashMap<String, Set<Class<?>>>();
        this.methodHandlers = new HashMap<String, MethodHandler>();
        this.methodTypes = new HashMap<String, Set<Class<?>>>();
        
        registerBuiltInHandlers();
    }
    
    /**
     * Set the evaluator after construction (for two-step initialization)
     */
    public void setEvaluator(Evaluator evaluator) {
        if (evaluator == null) {
            throw new IllegalArgumentException("Cannot set null evaluator");
        }
        this.evaluator = evaluator;
    }
    
    /**
     * Register all built-in literal handlers
     */
    private void registerBuiltInHandlers() {
        // Define .size property for RangeNode and NaturalArray
        define("size", 
            new PropertyHandler() {
                @Override
                public Object handle(Object literal, ExecutionContext ctx) {
                    if (literal instanceof RangeNode) {
                        return handleRangeSize((RangeNode) literal, ctx);
                    } else if (literal instanceof NaturalArray) {
                        return handleArraySize((NaturalArray) literal);
                    }
                    // Should never reach here due to type registration
                    throw new ProgramError("Unsupported type for .size");
                }
            },
            RangeNode.class, NaturalArray.class
        );
        
        define("map",
            new MethodHandler() {
                @Override
                public Object handle(Object literal, List<Object> arguments, ExecutionContext ctx) {
                    return handleArrayMap(literal, arguments, ctx);
                }
            },
            NaturalArray.class, List.class
        );
        
        define("filter",
            new MethodHandler() {
                @Override
                public Object handle(Object literal, List<Object> arguments, ExecutionContext ctx) {
                    return handleArrayFilter(literal, arguments, ctx);
                }
            },
            NaturalArray.class, List.class
        );
        
        define("reduce",
            new MethodHandler() {
                @Override
                public Object handle(Object literal, List<Object> arguments, ExecutionContext ctx) {
                    return handleArrayReduce(literal, arguments, ctx);
                }
            },
            NaturalArray.class, List.class
        );
        
        // Future definitions:
        // define("isEmpty", isEmptyHandler, String.class, List.class, NaturalArray.class);
        // define("toUpperCase", toUpperHandler, String.class, TextLiteralNode.class);
    }
    
    /**
     * Define a property for multiple types
     */
    public void define(String propertyName, PropertyHandler handler, Class<?>... types) {
        // Store the handler
        propertyHandlers.put(propertyName, handler);
        
        // Store the types that support this property
        Set<Class<?>> typeSet = new HashSet<Class<?>>();
        for (Class<?> type : types) {
            typeSet.add(type);
        }
        propertyTypes.put(propertyName, typeSet);
    }
    
    /**
     * Define a method for multiple types
     */
    public void define(String methodName, MethodHandler handler, Class<?>... types) {
        // Store the handler
        methodHandlers.put(methodName, handler);
        
        // Store the types that support this method
        Set<Class<?>> typeSet = new HashSet<Class<?>>();
        for (Class<?> type : types) {
            typeSet.add(type);
        }
        methodTypes.put(methodName, typeSet);
    }
    
    /**
     * Check if a literal has a property with the given name
     */
    public boolean hasProperty(Object literal, String propertyName) {
        if (literal == null || propertyName == null) return false;
        
        Set<Class<?>> types = propertyTypes.get(propertyName);
        if (types == null) return false;
        
        for (Class<?> type : types) {
            if (type.isAssignableFrom(literal.getClass())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if a literal has a method with the given name
     */
    public boolean hasMethod(Object literal, String methodName) {
        if (literal == null || methodName == null) return false;
        
        Set<Class<?>> types = methodTypes.get(methodName);
        if (types == null) return false;
        
        for (Class<?> type : types) {
            if (type.isAssignableFrom(literal.getClass())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Handle property access on a literal
     */
    public Object handleProperty(Object literal, String propertyName, ExecutionContext ctx) {
        if (literal == null) {
            throw new ProgramError("Cannot access property '" + propertyName + "' on null");
        }
        
        // Ensure evaluator is set
        if (evaluator == null) {
            throw new ProgramError("LiteralRegistry not properly initialized - missing evaluator");
        }
        
        // Check if this literal type supports the property
        if (!hasProperty(literal, propertyName)) {
            throw new ProgramError(
                "Property '" + propertyName + "' not supported by " + 
                literal.getClass().getSimpleName()
            );
        }
        
        // Get and execute handler
        PropertyHandler handler = propertyHandlers.get(propertyName);
        if (handler == null) {
            throw new ProgramError("No handler defined for property: " + propertyName);
        }
        
        return handler.handle(literal, ctx);
    }
    
    /**
     * Handle method call on a literal
     */
    public Object handleMethod(Object literal, String methodName, List<Object> arguments, ExecutionContext ctx) {
        if (literal == null) {
            throw new ProgramError("Cannot call method '" + methodName + "' on null");
        }
        
        // Ensure evaluator is set
        if (evaluator == null) {
            throw new ProgramError("LiteralRegistry not properly initialized - missing evaluator");
        }
        
        // Check if this literal type supports the method
        if (!hasMethod(literal, methodName)) {
            throw new ProgramError(
                "Method '" + methodName + "' not supported by " + 
                literal.getClass().getSimpleName()
            );
        }
        
        // Get and execute handler
        MethodHandler handler = methodHandlers.get(methodName);
        if (handler == null) {
            throw new ProgramError("No handler defined for method: " + methodName);
        }
        
        return handler.handle(literal, arguments, ctx);
    }
    
    /**
     * Handle .size for RangeNode
     */
    private Object handleRangeSize(RangeNode range, ExecutionContext ctx) {
        try {
            Object start = evaluator.evaluate(range.start, ctx);
            Object end = evaluator.evaluate(range.end, ctx);
            
            long startVal = toLong(start);
            long endVal = toLong(end);
            
            long size;
            if (range.step != null) {
                Object step = evaluator.evaluate(range.step, ctx);
                long stepVal = toLong(step);
                if (stepVal == 0) {
                    size = 0;
                } else {
                    size = Math.abs(endVal - startVal) / Math.abs(stepVal) + 1;
                }
            } else {
                size = Math.abs(endVal - startVal) + 1;
            }
            
            return (size <= Integer.MAX_VALUE) ? (int) size : size;
            
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new ProgramError("Failed to calculate range size: " + e.getMessage());
        }
    }
    
    /**
     * Handle .size for NaturalArray
     */
    private Object handleArraySize(NaturalArray array) {
        if (array.hasPendingUpdates()) {
            array.commitUpdates();
        }
        long size = array.size();
        return (size <= Integer.MAX_VALUE) ? (int) size : size;
    }
    
    @SuppressWarnings("unchecked")
    private List<Object> asList(Object literal) {
        if (literal instanceof NaturalArray) {
            NaturalArray arr = (NaturalArray) literal;
            if (arr.hasPendingUpdates()) {
                arr.commitUpdates();
            }
            return arr.toList();
        }
        if (literal instanceof List) {
            return (List<Object>) literal;
        }
        throw new ProgramError("Expected array literal target");
    }
    
    private Object invokeArrayCallback(Object callbackObj, String methodName, ExecutionContext ctx, Object... args) {
        List<Object> callbackArgs = new ArrayList<Object>();
        if (args != null) {
            for (Object arg : args) {
                callbackArgs.add(arg);
            }
        }
        return evaluator.invokeLambda(callbackObj, callbackArgs, ctx, methodName);
    }
    
    private Object handleArrayMap(Object literal, List<Object> arguments, ExecutionContext ctx) {
        if (arguments == null || arguments.isEmpty()) {
            throw new ProgramError("map expects a callback or (operator, operand)");
        }
        List<Object> source = asList(literal);
        
        if (looksLikeOperatorMap(arguments)) {
            String op = String.valueOf(arguments.get(0));
            Object operand = arguments.get(1);
            TypeHandler typeHandler = ctx.getTypeHandler();
            List<Object> result = new ArrayList<Object>(source.size());
            for (int i = 0; i < source.size(); i++) {
                Object value = source.get(i);
                result.add(applyOperator(typeHandler, value, op, operand));
            }
            return result;
        }
        
        if (arguments.size() != 1) {
            throw new ProgramError("map callback mode expects exactly one argument");
        }
        
        List<Object> result = new ArrayList<Object>(source.size());
        for (int i = 0; i < source.size(); i++) {
            Object value = source.get(i);
            Object mapped = invokeArrayCallback(arguments.get(0), "map", ctx, value, i);
            result.add(mapped);
        }
        return result;
    }
    
    private Object handleArrayFilter(Object literal, List<Object> arguments, ExecutionContext ctx) {
        if (arguments == null || arguments.isEmpty()) {
            throw new ProgramError("filter expects a callback or (operator, operand)");
        }
        List<Object> source = asList(literal);
        
        if (looksLikeOperatorFilter(arguments)) {
            String op = String.valueOf(arguments.get(0));
            Object operand = arguments.get(1);
            TypeHandler typeHandler = ctx.getTypeHandler();
            List<Object> result = new ArrayList<Object>();
            for (int i = 0; i < source.size(); i++) {
                Object value = source.get(i);
                Object comparison = compareWithOperator(typeHandler, value, op, operand);
                if (isTruthy(comparison)) {
                    result.add(value);
                }
            }
            return result;
        }
        
        if (arguments.size() != 1) {
            throw new ProgramError("filter callback mode expects exactly one argument");
        }
        
        List<Object> result = new ArrayList<Object>();
        for (int i = 0; i < source.size(); i++) {
            Object value = source.get(i);
            Object keep = invokeArrayCallback(arguments.get(0), "filter", ctx, value, i);
            if (isTruthy(keep)) {
                result.add(value);
            }
        }
        return result;
    }
    
    private Object handleArrayReduce(Object literal, List<Object> arguments, ExecutionContext ctx) {
        if (arguments == null || arguments.isEmpty() || arguments.size() > 2) {
            throw new ProgramError("reduce expects callback/op and optional initial value");
        }
        List<Object> source = asList(literal);
        if (source.isEmpty() && arguments.size() < 2) {
            throw new ProgramError("reduce on empty array requires an initial value");
        }
        
        if (isOperatorReduce(arguments.get(0))) {
            String op = String.valueOf(arguments.get(0));
            Object accumulator;
            int startIndex;
            if (arguments.size() == 2) {
                accumulator = arguments.get(1);
                startIndex = 0;
            } else {
                accumulator = source.get(0);
                startIndex = 1;
            }
            TypeHandler typeHandler = ctx.getTypeHandler();
            for (int i = startIndex; i < source.size(); i++) {
                accumulator = applyOperator(typeHandler, accumulator, op, source.get(i));
            }
            return accumulator;
        }
        
        Object accumulator;
        int startIndex;
        if (arguments.size() == 2) {
            accumulator = arguments.get(1);
            startIndex = 0;
        } else {
            accumulator = source.get(0);
            startIndex = 1;
        }
        
        for (int i = startIndex; i < source.size(); i++) {
            Object value = source.get(i);
            accumulator = invokeArrayCallback(arguments.get(0), "reduce", ctx, accumulator, value, i);
        }
        return accumulator;
    }
    
    private boolean looksLikeOperatorMap(List<Object> arguments) {
        return arguments.size() == 2 && isOperatorReduce(arguments.get(0));
    }
    
    private boolean looksLikeOperatorFilter(List<Object> arguments) {
        if (arguments.size() != 2) return false;
        String op = String.valueOf(arguments.get(0));
        return "==".equals(op) || "!=".equals(op) || ">".equals(op) || "<".equals(op) || ">=".equals(op) || "<=".equals(op);
    }
    
    private boolean isOperatorReduce(Object opObj) {
        String op = String.valueOf(opObj);
        return "+".equals(op) || "-".equals(op) || "*".equals(op) || "/".equals(op);
    }
    
    private Object applyOperator(TypeHandler typeHandler, Object left, String op, Object right) {
        if ("+".equals(op)) return typeHandler.addNumbers(left, right);
        if ("-".equals(op)) return typeHandler.subtractNumbers(left, right);
        if ("*".equals(op)) return typeHandler.multiplyNumbers(left, right);
        if ("/".equals(op)) return typeHandler.divideNumbers(left, right);
        throw new ProgramError("Unsupported operator for array method: " + op);
    }
    
    private Object compareWithOperator(TypeHandler typeHandler, Object left, String op, Object right) {
        int cmp = typeHandler.compare(left, right);
        if ("==".equals(op)) return cmp == 0;
        if ("!=".equals(op)) return cmp != 0;
        if (">".equals(op)) return cmp > 0;
        if ("<".equals(op)) return cmp < 0;
        if (">=".equals(op)) return cmp >= 0;
        if ("<=".equals(op)) return cmp <= 0;
        throw new ProgramError("Unsupported comparison operator for filter: " + op);
    }
    
    private boolean isTruthy(Object value) {
        Object unwrapped = value;
        if (value instanceof TypeHandler.Value) {
            unwrapped = ((TypeHandler.Value) value).value;
        }
        if (unwrapped == null) return false;
        if (unwrapped instanceof Boolean) return ((Boolean) unwrapped).booleanValue();
        if (unwrapped instanceof Number) return ((Number) unwrapped).doubleValue() != 0.0;
        if (unwrapped instanceof String) {
            String str = (String) unwrapped;
            return !str.isEmpty() && !"false".equalsIgnoreCase(str);
        }
        if (unwrapped instanceof List) return !((List<?>) unwrapped).isEmpty();
        if (unwrapped instanceof NaturalArray) return ((NaturalArray) unwrapped).size() > 0;
        if (unwrapped instanceof BoolLiteralNode) return ((BoolLiteralNode) unwrapped).value;
        if (unwrapped instanceof IntLiteralNode) return !((IntLiteralNode) unwrapped).value.isZero();
        if (unwrapped instanceof FloatLiteralNode) return !((FloatLiteralNode) unwrapped).value.isZero();
        if (unwrapped instanceof TextLiteralNode) {
            String str = ((TextLiteralNode) unwrapped).value;
            return !str.isEmpty() && !"false".equalsIgnoreCase(str);
        }
        return true;
    }
    
    /**
     * Convert an object to long for numeric operations
     */
    private long toLong(Object obj) {
        if (obj == null) {
            throw new ProgramError("Cannot convert null to number");
        }
        
        if (obj instanceof Integer) {
            return ((Integer) obj).longValue();
        }
        if (obj instanceof Long) {
            return (Long) obj;
        }
        if (obj instanceof IntLiteralNode) {
            return ((IntLiteralNode) obj).value.longValue();
        }
        if (obj instanceof FloatLiteralNode) {
            return ((FloatLiteralNode) obj).value.longValue();
        }
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        if (obj instanceof String) {
            try {
                return Long.parseLong((String) obj);
            } catch (NumberFormatException e) {
                throw new ProgramError("Cannot convert string to number: " + obj);
            }
        }
        
        throw new ProgramError("Cannot convert to number: " + obj.getClass().getSimpleName());
    }
    
    /**
     * Clear all registered handlers
     */
    public void clear() {
        propertyHandlers.clear();
        propertyTypes.clear();
        methodHandlers.clear();
        methodTypes.clear();
    }
    
    // ========== Handler Interfaces ==========
    
    /**
     * Handler for property access on literals
     */
    public interface PropertyHandler {
        Object handle(Object literal, ExecutionContext ctx);
    }
    
    /**
     * Handler for method calls on literals
     */
    public interface MethodHandler {
        Object handle(Object literal, List<Object> arguments, ExecutionContext ctx);
    }
}
