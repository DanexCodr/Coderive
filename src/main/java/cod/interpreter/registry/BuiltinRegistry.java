package cod.interpreter.registry;

import cod.ast.nodes.*;
import cod.debug.DebugSystem;
import cod.interpreter.InterpreterVisitor;
import cod.interpreter.io.IOHandler;
import cod.interpreter.type.TypeSystem;
import java.util.*;

public class BuiltinRegistry {
    
    private final Map<String, BuiltinMethod> builtinMethods;
    private final IOHandler ioHandler;
    private final TypeSystem typeSystem;
    
    public BuiltinRegistry(IOHandler ioHandler, TypeSystem typeSystem) {
        this.ioHandler = ioHandler;
        this.typeSystem = typeSystem;
        this.builtinMethods = new HashMap<String, BuiltinMethod>();
        
        // Register builtin methods
        registerBuiltins();
    }
    
    private void registerBuiltins() {
registerMethod("out", new BuiltinMethod() {
    @Override
    public Object execute(MethodCallNode call, InterpreterVisitor visitor) {
        StringBuilder result = new StringBuilder();
        if (call.arguments != null) {
            for (int i = 0; i < call.arguments.size(); i++) {
                Object value = visitor.visit((ASTNode) call.arguments.get(i));
                result.append(String.valueOf(value));
            }
        }
        ioHandler.output(result.toString());
        return null;
    }
    
    @Override
    public String getSignature() {
        return "out(...values)";
    }
});

registerMethod("outln", new BuiltinMethod() {
    @Override
    public Object execute(MethodCallNode call, InterpreterVisitor visitor) {
        StringBuilder result = new StringBuilder();
        if (call.arguments != null) {
            for (int i = 0; i < call.arguments.size(); i++) {
                Object value = visitor.visit((ASTNode) call.arguments.get(i));
                result.append(String.valueOf(value));
            }
        }
        result.append("\n");  // Add newline
        ioHandler.output(result.toString());
        return null;
    }
    
    @Override
    public String getSignature() {
        return "outln(...values)";
    }
});
        
        // Register in() method
        registerMethod("in", new BuiltinMethod() {
            @Override
            public Object execute(MethodCallNode call, InterpreterVisitor visitor) {
                // Determine which signature is being used based on arguments
                int argCount = call.arguments != null ? call.arguments.size() : 0;
                
                String expectedType = "text";  // Default
                String message = "";
                
                if (argCount == 0) {
                    // in() - no arguments
                    expectedType = "text";
                } 
                else if (argCount == 1) {
                    // in(type) - type specified
                    Object typeArg = visitor.visit((ASTNode) call.arguments.get(0));
                    expectedType = String.valueOf(typeArg);
                }
                else if (argCount == 2) {
                    // in(type, message)
                    Object typeArg = visitor.visit((ASTNode) call.arguments.get(0));
                    Object messageArg = visitor.visit((ASTNode) call.arguments.get(1));
                    
                    expectedType = String.valueOf(typeArg);
                    message = String.valueOf(messageArg);
                }
                else if (argCount == 3) {
                    // in(type, message, source)
                    Object typeArg = visitor.visit((ASTNode) call.arguments.get(0));
                    Object messageArg = visitor.visit((ASTNode) call.arguments.get(1));
                    Object sourceArg = visitor.visit((ASTNode) call.arguments.get(2));
                    
                    expectedType = String.valueOf(typeArg);
                    message = String.valueOf(messageArg);
                    String source = String.valueOf(sourceArg);
                    
                    // Currently only "stdin" is supported
                    if (!"stdin".equals(source)) {
                        throw new RuntimeException("Only 'stdin' source is currently supported for input");
                    }
                }
                
                // Output the prompt message if provided
                if (!message.isEmpty()) {
                    ioHandler.output(message);
                }
                
                // Read input based on expected type
                return ioHandler.readInput(expectedType);
            }
            
            @Override
            public String getSignature() {
                return "in() | in(type) | in(type, message) | in(type, message, source)";
            }
        });
        
        // In BuiltinRegistry.java - registerBuiltins() method
registerMethod("timer", new BuiltinMethod() {
    @Override
    public Object execute(MethodCallNode call, InterpreterVisitor visitor) {
        int argCount = call.arguments != null ? call.arguments.size() : 0;
        long nanos = System.nanoTime();
        
        if (argCount == 0) {
            // timer() - default milliseconds
            return nanos / 1_000_000.0;
        } 
        else if (argCount == 1) {
            Object unitArg = visitor.visit((ASTNode) call.arguments.get(0));
            String unit = String.valueOf(unitArg).toLowerCase();
            
            switch (unit) {
                case "ns":
                case "nanos":
                    return (double) nanos;
                    
                case "us":
                case "micros":
                    return nanos / 1_000.0;
                    
                case "ms":
                case "millis":
                    return nanos / 1_000_000.0;
                    
                case "s":
                case "sec":
                case "seconds":
                    return nanos / 1_000_000_000.0;
                    
                default:
                    throw new RuntimeException(
                        "Unknown time unit: '" + unit + "'. " +
                        "Valid units: ns, us, ms, s"
                    );
            }
        }
        else {
            throw new RuntimeException(
                "timer() takes 0 or 1 argument, got " + argCount
            );
        }
    }
    
    @Override
    public String getSignature() {
        return "timer() -> ms | timer(unit) -> time_in_unit";
    }
});
    }
    
    private void registerMethod(String name, BuiltinMethod method) {
        builtinMethods.put(name, method);
        DebugSystem.debug("BUILTIN", "Registered builtin method: " + name + " -> " + method.getSignature());
    }
    
    public boolean isBuiltin(String methodName) {
        return builtinMethods.containsKey(methodName);
    }
    
    public Object executeBuiltin(String methodName, MethodCallNode call, InterpreterVisitor visitor) {
        BuiltinMethod method = builtinMethods.get(methodName);
        if (method == null) {
            throw new RuntimeException("Unknown builtin method: " + methodName);
        }
        
        DebugSystem.debug("BUILTIN", "Executing builtin: " + methodName);
        return method.execute(call, visitor);
    }
    
    public Set<String> getRegisteredBuiltins() {
        return Collections.unmodifiableSet(builtinMethods.keySet());
    }
    
    // Interface for builtin methods
    public interface BuiltinMethod {
        Object execute(MethodCallNode call, InterpreterVisitor visitor);
        String getSignature();
    }
}
