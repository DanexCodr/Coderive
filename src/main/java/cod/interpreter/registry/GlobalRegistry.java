package cod.interpreter.registry;

import cod.ast.node.*;
import cod.debug.DebugSystem;
import cod.error.InternalError;
import cod.error.ProgramError;
import cod.interpreter.handler.IOHandler;
import cod.interpreter.context.ExecutionContext;
import cod.math.AutoStackingNumber;
import cod.range.NaturalArray;

import java.util.*;

public class GlobalRegistry {
    
    private final Map<String, GlobalFunction> globalFunctions;
    private final BuiltinRegistry builtinRegistry;
    private final IOHandler ioHandler;
    
    public GlobalRegistry(IOHandler ioHandler, BuiltinRegistry builtinRegistry) {
        if (ioHandler == null) {
            throw new InternalError("GlobalRegistry constructed with null ioHandler");
        }
        if (builtinRegistry == null) {
            throw new InternalError("GlobalRegistry constructed with null builtinRegistry");
        }
        
        this.ioHandler = ioHandler;
        this.builtinRegistry = builtinRegistry;
        this.globalFunctions = new HashMap<String, GlobalFunction>();
        
        registerGlobalFunctions();
    }
    
    /**
     * Extract a string value from an argument (works with both raw strings and AST nodes)
     */
    private String asString(Object arg) {
        if (arg instanceof String) {
            return (String) arg;
        }
        if (arg instanceof TextLiteral) {
            String text = ((TextLiteral) arg).value;
            if (text.startsWith("\"") && text.endsWith("\"") && text.length() >= 2) {
                return text.substring(1, text.length() - 1);
            }
            return text;
        }
        return String.valueOf(arg);
    }
    
    // Helper to auto-commit arrays before outs
    private void autoCommitArrays(List<Object> arguments) {
        if (arguments == null) return;
        for (Object arg : arguments) {
            if (arg instanceof NaturalArray) {
                NaturalArray arr = (NaturalArray) arg;
                if (arr.hasPendingUpdates()) {
                    arr.commitUpdates();
                }
            }
        }
    }
    
    // ========== THREAD LOCAL CONTEXT HELPERS ==========
    private boolean isInOptimizedLoop() {
        ExecutionContext ctx = ExecutionContext.getCurrentContext();
        return ctx != null && ctx.isInOptimizedLoop();
    }
    
    private void recordOptimizedOutput(List<Object> arguments) {
        ExecutionContext ctx = ExecutionContext.getCurrentContext();
        if (ctx != null && ctx.isInOptimizedLoop()) {
            for (Object arg : arguments) {
                ctx.recordOptimizedOutput(arg);
            }
        }
    }
    
    private void registerGlobalFunctions() {
        // out() function - prints with newline
        registerGlobal("out", new GlobalFunction() {
            @Override
            public Object execute(List<Object> arguments) {
                try {
                    // Check if we're in an optimized loop
                    if (isInOptimizedLoop()) {
                        // Don't outs now - just record
                        recordOptimizedOutput(arguments);
                        return null;
                    }
                    
                    // Normal outs path
                    autoCommitArrays(arguments);
                    
                    if (arguments == null || arguments.isEmpty()) {
                        ioHandler.outs("\n");
                        return null;
                    }
                    
                    if (arguments.size() == 1) {
                        ioHandler.outs(asString(arguments.get(0)) + "\n");
                        return null;
                    }
                    
                    StringBuilder result = new StringBuilder();
                    for (Object arg : arguments) {
                        result.append(asString(arg)).append("\n");
                    }
                    ioHandler.outs(result.toString());
                    return null;
                } catch (ProgramError e) {
                    throw e;
                } catch (Exception e) {
                    throw new InternalError("out() execution failed", e);
                }
            }
            
            @Override
            public String getSignature() {
                return "out(value) -> value\\n | out(v1, v2, ...) -> each on new line";
            }
        });

        // outs() function - prints with spaces
        registerGlobal("outs", new GlobalFunction() {
            @Override
            public Object execute(List<Object> arguments) {
                try {
                    // Check if we're in an optimized loop
                    if (isInOptimizedLoop()) {
                        // Don't outs now - just record
                        recordOptimizedOutput(arguments);
                        return null;
                    }
                    
                    // Normal outs path
                    autoCommitArrays(arguments);
                    
                    if (arguments == null || arguments.isEmpty()) {
                        ioHandler.outs("");
                        return null;
                    }
                    
                    if (arguments.size() == 1) {
                        ioHandler.outs(asString(arguments.get(0)));
                        return null;
                    }
                    
                    StringBuilder result = new StringBuilder();
                    for (int i = 0; i < arguments.size(); i++) {
                        result.append(asString(arguments.get(i)));
                        if (i < arguments.size() - 1) {
                            result.append(" ");
                        }
                    }
                    ioHandler.outs(result.toString());
                    return null;
                } catch (ProgramError e) {
                    throw e;
                } catch (Exception e) {
                    throw new InternalError("outs() execution failed", e);
                }
            }
            
            @Override
            public String getSignature() {
                return "outs(value) -> value | outs(v1, v2, ...) -> auto-spaced";
            }
        });
        
        registerGlobal("in", new GlobalFunction() {
            @Override
            public Object execute(List<Object> arguments) {
                try {
                    int argCount = arguments != null ? arguments.size() : 0;
                    
                    String expectedType = "text";
                    String message = "";
                    
                    if (argCount == 0) {
                        expectedType = "text";
                    } else if (argCount == 1) {
                        expectedType = asString(arguments.get(0));
                    } else if (argCount == 2) {
                        expectedType = asString(arguments.get(0));
                        message = asString(arguments.get(1));
                    } else if (argCount == 3) {
                        expectedType = asString(arguments.get(0));
                        message = asString(arguments.get(1));
                        String source = asString(arguments.get(2));
                        
                        if (!"stdin".equals(source)) {
                            throw new ProgramError("Only 'stdin' source is currently supported for input");
                        }
                    } else {
                        throw new ProgramError("in() takes 0-3 arguments, got " + argCount);
                    }
                    
                    if (!message.isEmpty()) {
                        ioHandler.outs(message);
                    }
                    
                    return ioHandler.readInput(expectedType);
                } catch (ProgramError e) {
                    throw e;
                } catch (Exception e) {
                    throw new InternalError("in() execution failed", e);
                }
            }
            
            @Override
            public String getSignature() {
                return "in() | in(type) | in(type, message) | in(type, message, source) -> value";
            }
        });
        
        registerGlobal("timer", new GlobalFunction() {
            @Override
            public Object execute(List<Object> arguments) {
                try {
                    int argCount = arguments != null ? arguments.size() : 0;
                    long nanos = System.nanoTime();
                    
                    double result;
                    if (argCount == 0) {
                        // Default: return milliseconds with 9 decimal places
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
                    // Multiply by 1e9, round, then divide back
                    double rounded = Math.round(result * 1_000_000_000.0) / 1_000_000_000.0;
                    
                    // Convert to AutoStackingNumber
                    return AutoStackingNumber.fromDouble(rounded);
                    
                } catch (ProgramError e) {
                    throw e;
                } catch (Exception e) {
                    throw new InternalError("timer() execution failed", e);
                }
            }
            
            @Override
            public String getSignature() {
                return "timer() -> ms | timer(unit) -> time_in_unit";
            }
        });
    }
    
    private void registerGlobal(String name, GlobalFunction function) {
        if (name == null || name.isEmpty()) {
            throw new InternalError("registerGlobal called with null/empty name");
        }
        if (function == null) {
            throw new InternalError("registerGlobal called with null function");
        }
        
        globalFunctions.put(name, function);
        DebugSystem.debug("GLOBAL", "Registered global function: " + name + " -> " + function.getSignature());
    }
    
    public boolean isGlobal(String functionName) {
        if (functionName == null) {
            return false;
        }
        return globalFunctions.containsKey(functionName) || builtinRegistry.isBuiltin(functionName);
    }
    
    public Object executeGlobal(String functionName, List<Object> arguments) {
        if (functionName == null) {
            throw new InternalError("executeGlobal called with null functionName");
        }
        
        GlobalFunction globalFunc = globalFunctions.get(functionName);
        if (globalFunc != null) {
            DebugSystem.debug("GLOBAL", "Executing global function: " + functionName + " with args: " + arguments);
            try {
                return globalFunc.execute(arguments);
            } catch (ProgramError e) {
                throw e;
            } catch (Exception e) {
                throw new InternalError("Global function execution failed: " + functionName, e);
            }
        }
        
        if (builtinRegistry.isBuiltin(functionName)) {
            DebugSystem.debug("GLOBAL", "Executing builtin as global: " + functionName);
            try {
                return builtinRegistry.executeBuiltin(functionName, arguments);
            } catch (ProgramError e) {
                throw e;
            } catch (Exception e) {
                throw new InternalError("Builtin execution as global failed: " + functionName, e);
            }
        }
        
        throw new ProgramError("Unknown global function: " + functionName);
    }
    
    public Set<String> getGlobalFunctionNames() {
        Set<String> names = new HashSet<String>();
        names.addAll(globalFunctions.keySet());
        names.addAll(builtinRegistry.getRegisteredBuiltins());
        return Collections.unmodifiableSet(names);
    }
    
    public interface GlobalFunction {
        Object execute(List<Object> arguments);
        String getSignature();
    }
}