package cod.interpreter.registry;

import cod.ast.nodes.*;
import cod.error.ProgramError;
import cod.interpreter.context.ExecutionContext;
import cod.interpreter.Evaluator;
import cod.math.AutoStackingNumber;
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

        // ── String .length property ──────────────────────────────────────────
        define("length",
            new PropertyHandler() {
                @Override
                public Object handle(Object literal, ExecutionContext ctx) {
                    return asText(literal).length();
                }
            },
            String.class
        );

        // ── String methods ───────────────────────────────────────────────────

        // .upper() – converts text to uppercase
        define("upper",
            new MethodHandler() {
                @Override
                public Object handle(Object literal, List<Object> args, ExecutionContext ctx) {
                    if (args != null && !args.isEmpty()) {
                        throw new ProgramError("upper() takes no arguments");
                    }
                    return asText(literal).toUpperCase();
                }
            },
            String.class
        );

        // .lower() – converts text to lowercase
        define("lower",
            new MethodHandler() {
                @Override
                public Object handle(Object literal, List<Object> args, ExecutionContext ctx) {
                    if (args != null && !args.isEmpty()) {
                        throw new ProgramError("lower() takes no arguments");
                    }
                    return asText(literal).toLowerCase();
                }
            },
            String.class
        );

        // .trim() – removes leading and trailing whitespace
        define("trim",
            new MethodHandler() {
                @Override
                public Object handle(Object literal, List<Object> args, ExecutionContext ctx) {
                    if (args != null && !args.isEmpty()) {
                        throw new ProgramError("trim() takes no arguments");
                    }
                    return asText(literal).trim();
                }
            },
            String.class
        );

        // .reversed() – reverses the text
        define("reversed",
            new MethodHandler() {
                @Override
                public Object handle(Object literal, List<Object> args, ExecutionContext ctx) {
                    if (args != null && !args.isEmpty()) {
                        throw new ProgramError("reversed() takes no arguments");
                    }
                    return new StringBuilder(asText(literal)).reverse().toString();
                }
            },
            String.class
        );

        // .contains(sub) – true if text contains the substring
        define("contains",
            new MethodHandler() {
                @Override
                public Object handle(Object literal, List<Object> args, ExecutionContext ctx) {
                    if (args == null || args.size() != 1) {
                        throw new ProgramError("contains() requires exactly 1 argument");
                    }
                    return asText(literal).contains(asText(args.get(0)));
                }
            },
            String.class
        );

        // .startsWith(prefix) – true if text starts with prefix
        define("startsWith",
            new MethodHandler() {
                @Override
                public Object handle(Object literal, List<Object> args, ExecutionContext ctx) {
                    if (args == null || args.size() != 1) {
                        throw new ProgramError("startsWith() requires exactly 1 argument");
                    }
                    return asText(literal).startsWith(asText(args.get(0)));
                }
            },
            String.class
        );

        // .endsWith(suffix) – true if text ends with suffix
        define("endsWith",
            new MethodHandler() {
                @Override
                public Object handle(Object literal, List<Object> args, ExecutionContext ctx) {
                    if (args == null || args.size() != 1) {
                        throw new ProgramError("endsWith() requires exactly 1 argument");
                    }
                    return asText(literal).endsWith(asText(args.get(0)));
                }
            },
            String.class
        );

        // .replace(old, new) – replaces all occurrences of old with new
        define("replace",
            new MethodHandler() {
                @Override
                public Object handle(Object literal, List<Object> args, ExecutionContext ctx) {
                    if (args == null || args.size() != 2) {
                        throw new ProgramError("replace() requires exactly 2 arguments");
                    }
                    return asText(literal).replace(asText(args.get(0)), asText(args.get(1)));
                }
            },
            String.class
        );

        // ── Number methods ───────────────────────────────────────────────────

        // .abs() – absolute value
        define("abs",
            new MethodHandler() {
                @Override
                public Object handle(Object literal, List<Object> args, ExecutionContext ctx) {
                    if (args != null && !args.isEmpty()) {
                        throw new ProgramError("abs() takes no arguments");
                    }
                    return numberAbs(literal);
                }
            },
            Integer.class, Long.class, Double.class, Float.class, AutoStackingNumber.class
        );

        // .pow(n) – raises the number to the power n
        define("pow",
            new MethodHandler() {
                @Override
                public Object handle(Object literal, List<Object> args, ExecutionContext ctx) {
                    if (args == null || args.size() != 1) {
                        throw new ProgramError("pow() requires exactly 1 argument");
                    }
                    if (literal instanceof AutoStackingNumber) {
                        int exp = (int) toLong(args.get(0));
                        return ((AutoStackingNumber) literal).pow(exp);
                    }
                    double base = toDouble(literal);
                    double exp  = toDouble(args.get(0));
                    double result = Math.pow(base, exp);
                    if (literal instanceof Integer || literal instanceof Long) {
                        long longResult = (long) result;
                        if (longResult <= Integer.MAX_VALUE && longResult >= Integer.MIN_VALUE) {
                            return (int) longResult;
                        }
                        return longResult;
                    }
                    return result;
                }
            },
            Integer.class, Long.class, Double.class, Float.class, AutoStackingNumber.class
        );

        // .sqrt() – square root
        define("sqrt",
            new MethodHandler() {
                @Override
                public Object handle(Object literal, List<Object> args, ExecutionContext ctx) {
                    if (args != null && !args.isEmpty()) {
                        throw new ProgramError("sqrt() takes no arguments");
                    }
                    double val = toDouble(literal);
                    if (val < 0) {
                        throw new ProgramError("sqrt() requires a non-negative number");
                    }
                    return AutoStackingNumber.fromDouble(Math.sqrt(val));
                }
            },
            Integer.class, Long.class, Double.class, Float.class, AutoStackingNumber.class
        );
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
        if (obj instanceof AutoStackingNumber) {
            return ((AutoStackingNumber) obj).longValue();
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
     * Convert an object to a Java String, stripping surrounding quotes from text literals.
     */
    private String asText(Object obj) {
        if (obj == null) {
            return "none";
        }
        if (obj instanceof String) {
            return (String) obj;
        }
        if (obj instanceof TextLiteralNode) {
            String text = ((TextLiteralNode) obj).value;
            if (text.startsWith("\"") && text.endsWith("\"") && text.length() >= 2) {
                return text.substring(1, text.length() - 1);
            }
            return text;
        }
        return String.valueOf(obj);
    }
    
    /**
     * Convert an object to double for numeric operations.
     */
    private double toDouble(Object obj) {
        if (obj == null) {
            throw new ProgramError("Cannot convert null to number");
        }
        if (obj instanceof Double) {
            return (Double) obj;
        }
        if (obj instanceof Float) {
            return ((Float) obj).doubleValue();
        }
        if (obj instanceof Integer) {
            return ((Integer) obj).doubleValue();
        }
        if (obj instanceof Long) {
            return ((Long) obj).doubleValue();
        }
        if (obj instanceof AutoStackingNumber) {
            return ((AutoStackingNumber) obj).doubleValue();
        }
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        if (obj instanceof String) {
            try {
                return Double.parseDouble((String) obj);
            } catch (NumberFormatException e) {
                throw new ProgramError("Cannot convert string to number: " + obj);
            }
        }
        throw new ProgramError("Cannot convert to number: " + obj.getClass().getSimpleName());
    }
    
    /**
     * Return the absolute value, preserving the original numeric type.
     */
    private Object numberAbs(Object obj) {
        if (obj instanceof AutoStackingNumber) {
            return ((AutoStackingNumber) obj).abs();
        }
        if (obj instanceof Integer) {
            return Math.abs((Integer) obj);
        }
        if (obj instanceof Long) {
            return Math.abs((Long) obj);
        }
        if (obj instanceof Double) {
            return Math.abs((Double) obj);
        }
        if (obj instanceof Float) {
            return Math.abs((Float) obj);
        }
        if (obj instanceof Number) {
            double d = ((Number) obj).doubleValue();
            return Math.abs(d);
        }
        throw new ProgramError("abs() requires a numeric value");
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