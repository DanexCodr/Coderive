package cod.interpreter.registry;

import cod.ast.nodes.*;
import cod.debug.DebugSystem;
import cod.error.InternalError;
import cod.error.ProgramError;
import cod.math.AutoStackingNumber;

import java.util.*;

public class BuiltinRegistry {
    
    private final Map<String, BuiltinMethod> builtinMethods;
    
    public BuiltinRegistry() {
        this.builtinMethods = new HashMap<String, BuiltinMethod>();
        registerBuiltins();
    }
    
    private String asString(Object arg) {
        if (arg instanceof String) {
            return (String) arg;
        }
        if (arg instanceof TextLiteralNode) {
            String text = ((TextLiteralNode) arg).value;
            if (text.startsWith("\"") && text.endsWith("\"") && text.length() >= 2) {
                return text.substring(1, text.length() - 1);
            }
            return text;
        }
        return String.valueOf(arg);
    }
    
    private void registerBuiltins() {
        registerMethod("timer", new BuiltinMethod() {
            @Override
            public Object execute(List<Object> arguments) {
                try {
                    int argCount = arguments != null ? arguments.size() : 0;
                    long nanos = System.nanoTime();
                    
                    double result;
                    if (argCount == 0) {
                        result = nanos / 1_000_000.0;
                    } else if (argCount == 1) {
                        String unit = asString(arguments.get(0)).toLowerCase();
                        
                        if ("ns".equals(unit) || "nanos".equals(unit)) {
                            return AutoStackingNumber.fromLong(nanos);
                        } else if ("us".equals(unit) || "micros".equals(unit)) {
                            result = nanos / 1_000.0;
                        } else if ("ms".equals(unit) || "millis".equals(unit)) {
                            result = nanos / 1_000_000.0;
                        } else if ("s".equals(unit) || "sec".equals(unit) || "seconds".equals(unit)) {
                            result = nanos / 1_000_000_000.0;
                        } else {
                            throw new ProgramError(
                                "Unknown time unit: '" + unit + "'. Valid units: ns, us, ms, s"
                            );
                        }
                    } else {
                        throw new ProgramError("timer() takes 0 or 1 argument, got " + argCount);
                    }
                    
                    // Round to 9 decimal places (nanosecond precision)
                    double rounded = Math.round(result * 1_000_000_000.0) / 1_000_000_000.0;
                    
                    return AutoStackingNumber.fromDouble(rounded);
                    
                } catch (ProgramError e) {
                    throw e;
                } catch (Exception e) {
                    throw new InternalError("timer() builtin execution failed", e);
                }
            }
            
            @Override
            public String getSignature() {
                return "timer() -> ms | timer(unit) -> time_in_unit";
            }
        });
    }
    
    private void registerMethod(String name, BuiltinMethod method) {
        if (name == null || name.isEmpty()) {
            throw new InternalError("registerMethod called with null/empty name");
        }
        if (method == null) {
            throw new InternalError("registerMethod called with null method");
        }
        
        builtinMethods.put(name, method);
        DebugSystem.debug("BUILTIN", "Registered builtin method: " + name + " -> " + method.getSignature());
    }
    
    public boolean isBuiltin(String methodName) {
        return methodName != null && builtinMethods.containsKey(methodName);
    }
    
    public Object executeBuiltin(String methodName, List<Object> arguments) {
        if (methodName == null) {
            throw new InternalError("executeBuiltin called with null methodName");
        }
        
        BuiltinMethod method = builtinMethods.get(methodName);
        if (method == null) {
            throw new ProgramError("Unknown builtin method: " + methodName);
        }
        
        DebugSystem.debug("BUILTIN", "Executing builtin: " + methodName);
        try {
            return method.execute(arguments);
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Builtin execution failed: " + methodName, e);
        }
    }
    
    public Set<String> getRegisteredBuiltins() {
        return Collections.unmodifiableSet(builtinMethods.keySet());
    }
    
    public interface BuiltinMethod {
        Object execute(List<Object> arguments);
        String getSignature();
    }
}