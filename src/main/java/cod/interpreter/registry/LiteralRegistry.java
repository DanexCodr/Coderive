package cod.interpreter.registry;

import cod.ast.nodes.*;
import cod.error.ProgramError;
import cod.interpreter.context.ExecutionContext;
import cod.interpreter.Evaluator;
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