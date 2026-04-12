package cod.interpreter.registry;

import cod.ast.node.*;
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
        // Define .size property for Range and NaturalArray
        define("size", 
            new PropertyHandler() {
                @Override
                public Object handle(Object literal, ExecutionContext ctx) {
                    if (literal instanceof Range) {
                        return handleRangeSize((Range) literal, ctx);
                    } else if (literal instanceof NaturalArray) {
                        return handleArraySize((NaturalArray) literal);
                    }
                    // Should never reach here due to type registration
                    throw new ProgramError("Unsupported type for .size");
                }
            },
            Range.class, NaturalArray.class
        );

        define("length",
            new PropertyHandler() {
                @Override
                public Object handle(Object literal, ExecutionContext ctx) {
                    return handleStringLength(literal);
                }
            },
            String.class
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

        define("has",
            new MethodHandler() {
                @Override
                public Object handle(Object literal, List<Object> arguments, ExecutionContext ctx) {
                    return handleStringHas(literal, arguments);
                }
            },
            String.class
        );

        define("find",
            new MethodHandler() {
                @Override
                public Object handle(Object literal, List<Object> arguments, ExecutionContext ctx) {
                    return handleStringFind(literal, arguments);
                }
            },
            String.class
        );

        define("findLast",
            new MethodHandler() {
                @Override
                public Object handle(Object literal, List<Object> arguments, ExecutionContext ctx) {
                    return handleStringFindLast(literal, arguments);
                }
            },
            String.class
        );

        define("replace",
            new MethodHandler() {
                @Override
                public Object handle(Object literal, List<Object> arguments, ExecutionContext ctx) {
                    return handleStringReplace(literal, arguments);
                }
            },
            String.class
        );

        define("split",
            new MethodHandler() {
                @Override
                public Object handle(Object literal, List<Object> arguments, ExecutionContext ctx) {
                    return handleStringSplit(literal, arguments);
                }
            },
            String.class
        );

        define("starts",
            new MethodHandler() {
                @Override
                public Object handle(Object literal, List<Object> arguments, ExecutionContext ctx) {
                    return handleStringStarts(literal, arguments);
                }
            },
            String.class
        );

        define("ends",
            new MethodHandler() {
                @Override
                public Object handle(Object literal, List<Object> arguments, ExecutionContext ctx) {
                    return handleStringEnds(literal, arguments);
                }
            },
            String.class
        );

        define("trim",
            new MethodHandler() {
                @Override
                public Object handle(Object literal, List<Object> arguments, ExecutionContext ctx) {
                    return handleStringTrim(literal, arguments);
                }
            },
            String.class
        );

        define("isEmpty",
            new MethodHandler() {
                @Override
                public Object handle(Object literal, List<Object> arguments, ExecutionContext ctx) {
                    return handleStringIsEmpty(literal, arguments);
                }
            },
            String.class
        );

        define("isBlank",
            new MethodHandler() {
                @Override
                public Object handle(Object literal, List<Object> arguments, ExecutionContext ctx) {
                    return handleStringIsBlank(literal, arguments);
                }
            },
            String.class
        );

        define("repeat",
            new MethodHandler() {
                @Override
                public Object handle(Object literal, List<Object> arguments, ExecutionContext ctx) {
                    return handleStringRepeat(literal, arguments);
                }
            },
            String.class
        );

        define("toUpper",
            new MethodHandler() {
                @Override
                public Object handle(Object literal, List<Object> arguments, ExecutionContext ctx) {
                    return handleStringToUpper(literal, arguments);
                }
            },
            String.class
        );

        define("toLower",
            new MethodHandler() {
                @Override
                public Object handle(Object literal, List<Object> arguments, ExecutionContext ctx) {
                    return handleStringToLower(literal, arguments);
                }
            },
            String.class
        );

        define("isUpper",
            new MethodHandler() {
                @Override
                public Object handle(Object literal, List<Object> arguments, ExecutionContext ctx) {
                    return handleStringIsUpper(literal, arguments);
                }
            },
            String.class
        );
        
        // Future definitions:
        // define("isEmpty", isEmptyHandler, String.class, List.class, NaturalArray.class);
        // define("toUpperCase", toUpperHandler, String.class, TextLiteral.class);
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
     * Handle .size for Range
     */
    private Object handleRangeSize(Range range, ExecutionContext ctx) {
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

    private Object handleStringLength(Object literal) {
        if (!(literal instanceof String)) {
            throw new ProgramError("length is only supported on text values");
        }
        return ((String) literal).length();
    }

    private String asText(Object value) {
        if (value == null) return "none";
        if (value instanceof String) return (String) value;
        if (value instanceof TextLiteral) return ((TextLiteral) value).value;
        return String.valueOf(value);
    }

    private void requireArgCount(String method, List<Object> arguments, int expected) {
        int actual = arguments == null ? 0 : arguments.size();
        if (actual != expected) {
            throw new ProgramError(method + " expects " + expected + " argument(s), got " + actual);
        }
    }

    private String requireStringTarget(Object literal, String method) {
        if (!(literal instanceof String)) {
            throw new ProgramError(method + " is only supported on text values");
        }
        return (String) literal;
    }

    private Object handleStringHas(Object literal, List<Object> arguments) {
        requireArgCount("has", arguments, 1);
        String target = requireStringTarget(literal, "has");
        return target.contains(asText(arguments.get(0)));
    }

    private Object handleStringFind(Object literal, List<Object> arguments) {
        requireArgCount("find", arguments, 1);
        String target = requireStringTarget(literal, "find");
        return target.indexOf(asText(arguments.get(0)));
    }

    private Object handleStringFindLast(Object literal, List<Object> arguments) {
        requireArgCount("findLast", arguments, 1);
        String target = requireStringTarget(literal, "findLast");
        return target.lastIndexOf(asText(arguments.get(0)));
    }

    private Object handleStringReplace(Object literal, List<Object> arguments) {
        requireArgCount("replace", arguments, 2);
        String target = requireStringTarget(literal, "replace");
        return target.replace(asText(arguments.get(0)), asText(arguments.get(1)));
    }

    private Object handleStringSplit(Object literal, List<Object> arguments) {
        requireArgCount("split", arguments, 1);
        String target = requireStringTarget(literal, "split");
        String delimiter = asText(arguments.get(0));
        List<Object> result = new ArrayList<Object>();
        if (delimiter.length() == 0) {
            for (int i = 0; i < target.length(); i++) {
                result.add(String.valueOf(target.charAt(i)));
            }
            return result;
        }

        int from = 0;
        while (true) {
            int at = target.indexOf(delimiter, from);
            if (at < 0) {
                result.add(target.substring(from));
                break;
            }
            result.add(target.substring(from, at));
            from = at + delimiter.length();
        }
        return result;
    }

    private Object handleStringStarts(Object literal, List<Object> arguments) {
        requireArgCount("starts", arguments, 1);
        String target = requireStringTarget(literal, "starts");
        return target.startsWith(asText(arguments.get(0)));
    }

    private Object handleStringEnds(Object literal, List<Object> arguments) {
        requireArgCount("ends", arguments, 1);
        String target = requireStringTarget(literal, "ends");
        return target.endsWith(asText(arguments.get(0)));
    }

    private Object handleStringTrim(Object literal, List<Object> arguments) {
        requireArgCount("trim", arguments, 0);
        String target = requireStringTarget(literal, "trim");
        return target.trim();
    }

    private Object handleStringIsEmpty(Object literal, List<Object> arguments) {
        requireArgCount("isEmpty", arguments, 0);
        String target = requireStringTarget(literal, "isEmpty");
        return target.isEmpty();
    }

    private Object handleStringIsBlank(Object literal, List<Object> arguments) {
        requireArgCount("isBlank", arguments, 0);
        String target = requireStringTarget(literal, "isBlank");
        for (int i = 0; i < target.length(); i++) {
            if (!Character.isWhitespace(target.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private Object handleStringRepeat(Object literal, List<Object> arguments) {
        requireArgCount("repeat", arguments, 1);
        String target = requireStringTarget(literal, "repeat");
        long count = toLong(arguments.get(0));
        if (count < 0) {
            throw new ProgramError("repeat expects a non-negative count, got " + count);
        }
        if (count == 0) return "";
        if (count > Integer.MAX_VALUE) {
            throw new ProgramError("repeat count is too large: " + count);
        }
        long expectedLength;
        try {
            expectedLength = Math.multiplyExact((long) target.length(), count);
        } catch (ArithmeticException e) {
            throw new ProgramError("repeat result is too large");
        }
        if (expectedLength > Integer.MAX_VALUE) {
            throw new ProgramError("repeat result is too large");
        }
        StringBuilder sb = new StringBuilder((int) expectedLength);
        for (int i = 0; i < (int) count; i++) {
            sb.append(target);
        }
        return sb.toString();
    }

    private Object handleStringToUpper(Object literal, List<Object> arguments) {
        requireArgCount("toUpper", arguments, 0);
        String target = requireStringTarget(literal, "toUpper");
        return target.toUpperCase();
    }

    private Object handleStringToLower(Object literal, List<Object> arguments) {
        requireArgCount("toLower", arguments, 0);
        String target = requireStringTarget(literal, "toLower");
        return target.toLowerCase();
    }

    private Object handleStringIsUpper(Object literal, List<Object> arguments) {
        requireArgCount("isUpper", arguments, 0);
        String target = requireStringTarget(literal, "isUpper");
        boolean seenLetter = false;
        for (int i = 0; i < target.length(); i++) {
            char ch = target.charAt(i);
            if (Character.isLetter(ch)) {
                seenLetter = true;
                if (!Character.isUpperCase(ch)) {
                    return false;
                }
            }
        }
        return seenLetter;
    }
    
    @SuppressWarnings("unchecked")
    private List<Object> asConcreteList(Object literal) {
        if (literal instanceof List) {
            return (List<Object>) literal;
        }
        throw new ProgramError("Expected list literal target");
    }

    private NaturalArray asNaturalArray(Object literal) {
        if (literal instanceof NaturalArray) {
            NaturalArray arr = (NaturalArray) literal;
            if (arr.hasPendingUpdates()) {
                arr.commitUpdates();
            }
            return arr;
        }
        return null;
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
        final NaturalArray naturalArray = asNaturalArray(literal);
        List<Object> source = naturalArray == null ? asConcreteList(literal) : null;
        
        if (looksLikeOperatorMap(arguments)) {
            final String op = String.valueOf(arguments.get(0));
            final Object operand = arguments.get(1);
            final TypeHandler typeHandler = ctx.getTypeHandler();
            if (naturalArray != null) {
                return new LazyNaturalArrayMapView(naturalArray, new NaturalArrayMapper() {
                    @Override
                    public Object map(long index, Object value) {
                        return applyOperator(typeHandler, value, op, operand);
                    }
                });
            }
            List<Object> result = new ArrayList<Object>(source.size());
            for (int i = 0; i < source.size(); i++) {
                result.add(applyOperator(typeHandler, source.get(i), op, operand));
            }
            return result;
        }
        
        if (arguments.size() != 1) {
            throw new ProgramError("map callback mode expects exactly one argument");
        }
        final Object callback = arguments.get(0);
        if (naturalArray != null) {
            return new LazyNaturalArrayMapView(naturalArray, new NaturalArrayMapper() {
                @Override
                public Object map(long index, Object value) {
                    return invokeArrayCallback(callback, "map", ctx, value, Integer.valueOf((int) index));
                }
            });
        }
        List<Object> result = new ArrayList<Object>(source.size());
        for (int i = 0; i < source.size(); i++) {
            result.add(invokeArrayCallback(callback, "map", ctx, source.get(i), Integer.valueOf(i)));
        }
        return result;
    }
    
    private Object handleArrayFilter(Object literal, List<Object> arguments, ExecutionContext ctx) {
        if (arguments == null || arguments.isEmpty()) {
            throw new ProgramError("filter expects a callback or (operator, operand)");
        }
        final NaturalArray naturalArray = asNaturalArray(literal);
        List<Object> source = naturalArray == null ? asConcreteList(literal) : null;
        
        if (looksLikeOperatorFilter(arguments)) {
            final String op = String.valueOf(arguments.get(0));
            final Object operand = arguments.get(1);
            final TypeHandler typeHandler = ctx.getTypeHandler();
            if (naturalArray != null) {
                return new LazyNaturalArrayFilterView(naturalArray, new NaturalArrayPredicate() {
                    @Override
                    public boolean test(long index, Object value) {
                        Object comparison = compareWithOperator(typeHandler, value, op, operand);
                        return isTruthy(comparison);
                    }
                });
            }
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
        final Object callback = arguments.get(0);
        if (naturalArray != null) {
            return new LazyNaturalArrayFilterView(naturalArray, new NaturalArrayPredicate() {
                @Override
                public boolean test(long index, Object value) {
                    Object keep = invokeArrayCallback(callback, "filter", ctx, value, Integer.valueOf((int) index));
                    return isTruthy(keep);
                }
            });
        }
        List<Object> result = new ArrayList<Object>();
        for (int i = 0; i < source.size(); i++) {
            Object value = source.get(i);
            Object keep = invokeArrayCallback(callback, "filter", ctx, value, Integer.valueOf(i));
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
        NaturalArray naturalArray = asNaturalArray(literal);
        List<Object> source = naturalArray == null ? asConcreteList(literal) : null;
        long sourceSizeLong = naturalArray != null ? naturalArray.size() : source.size();
        if (sourceSizeLong > Integer.MAX_VALUE) {
            throw new ProgramError(
                "Reduce operation cannot process arrays larger than Integer.MAX_VALUE ("
                    + Integer.MAX_VALUE + ") elements. Current size: " + sourceSizeLong
            );
        }
        int sourceSize = (int) sourceSizeLong;
        if (sourceSize == 0 && arguments.size() < 2) {
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
                accumulator = naturalArray != null ? naturalArray.get(0L) : source.get(0);
                startIndex = 1;
            }
            TypeHandler typeHandler = ctx.getTypeHandler();
            for (int i = startIndex; i < sourceSize; i++) {
                Object next = naturalArray != null ? naturalArray.get((long) i) : source.get(i);
                accumulator = applyOperator(typeHandler, accumulator, op, next);
            }
            return accumulator;
        }
        
        Object accumulator;
        int startIndex;
        if (arguments.size() == 2) {
            accumulator = arguments.get(1);
            startIndex = 0;
        } else {
            accumulator = naturalArray != null ? naturalArray.get(0L) : source.get(0);
            startIndex = 1;
        }
        
        for (int i = startIndex; i < sourceSize; i++) {
            Object value = naturalArray != null ? naturalArray.get((long) i) : source.get(i);
            accumulator = invokeArrayCallback(arguments.get(0), "reduce", ctx, accumulator, value, Integer.valueOf(i));
        }
        return accumulator;
    }

    private interface NaturalArrayMapper {
        Object map(long index, Object value);
    }

    private interface NaturalArrayPredicate {
        boolean test(long index, Object value);
    }

    private static final class LazyNaturalArrayMapView extends AbstractList<Object> {
        private final NaturalArray source;
        private final NaturalArrayMapper mapper;
        private final int size;

        private LazyNaturalArrayMapView(NaturalArray source, NaturalArrayMapper mapper) {
            this.source = source;
            this.mapper = mapper;
            long sourceSize = source.size();
            if (sourceSize > Integer.MAX_VALUE) {
                throw new ProgramError(
                    "Mapped array size exceeds Integer.MAX_VALUE (" + Integer.MAX_VALUE + "): " + sourceSize
                );
            }
            this.size = (int) sourceSize;
        }

        @Override
        public Object get(int index) {
            if (index < 0 || index >= size) {
                throw new ProgramError("Index: " + index + ", Size: " + size);
            }
            Object value = source.get((long) index);
            return mapper.map((long) index, value);
        }

        @Override
        public int size() {
            return size;
        }
    }

    private static final class LazyNaturalArrayFilterView extends AbstractList<Object> {
        private final NaturalArray source;
        private final NaturalArrayPredicate predicate;
        private final List<Long> acceptedSourceIndices;
        private long scanned;
        private final long sourceSize;
        private boolean fullyScanned;

        private LazyNaturalArrayFilterView(NaturalArray source, NaturalArrayPredicate predicate) {
            this.source = source;
            this.predicate = predicate;
            this.acceptedSourceIndices = new ArrayList<Long>();
            this.scanned = 0L;
            this.sourceSize = source.size();
            this.fullyScanned = false;
        }

        @Override
        public Object get(int index) {
            if (index < 0) {
                throw new ProgramError("Negative index: " + index);
            }
            ensureAcceptedIndex(index);
            if (index >= acceptedSourceIndices.size()) {
                throw new ProgramError("Index: " + index + ", Size: " + acceptedSourceIndices.size());
            }
            long sourceIndex = acceptedSourceIndices.get(index).longValue();
            return source.get(sourceIndex);
        }

        @Override
        public int size() {
            scanToEnd();
            return acceptedSourceIndices.size();
        }

        private void ensureAcceptedIndex(int index) {
            while (!fullyScanned && acceptedSourceIndices.size() <= index) {
                scanNext();
            }
        }

        private void scanToEnd() {
            while (!fullyScanned) {
                scanNext();
            }
        }

        private void scanNext() {
            if (fullyScanned) return;
            if (scanned >= sourceSize) {
                fullyScanned = true;
                return;
            }
            Object value = source.get(scanned);
            if (predicate.test(scanned, value)) {
                acceptedSourceIndices.add(Long.valueOf(scanned));
            }
            scanned++;
            if (scanned >= sourceSize) {
                fullyScanned = true;
            }
        }
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
        if (unwrapped instanceof BoolLiteral) return ((BoolLiteral) unwrapped).value;
        if (unwrapped instanceof IntLiteral) return !((IntLiteral) unwrapped).value.isZero();
        if (unwrapped instanceof FloatLiteral) return !((FloatLiteral) unwrapped).value.isZero();
        if (unwrapped instanceof TextLiteral) {
            String str = ((TextLiteral) unwrapped).value;
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
        if (obj instanceof IntLiteral) {
            return ((IntLiteral) obj).value.longValue();
        }
        if (obj instanceof FloatLiteral) {
            return ((FloatLiteral) obj).value.longValue();
        }
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        if (obj instanceof cod.math.AutoStackingNumber) {
            return ((cod.math.AutoStackingNumber) obj).longValue();
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
