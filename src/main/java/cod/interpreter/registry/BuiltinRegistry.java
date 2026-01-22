package cod.interpreter.registry;

import cod.ast.nodes.*;
import cod.debug.DebugSystem;
import cod.interpreter.InterpreterVisitor;
import cod.interpreter.io.IOHandler;
import java.util.*;

public class BuiltinRegistry {
    
    private final Map<String, BuiltinMethod> builtinMethods;
    
    @SuppressWarnings("unused")
    private final IOHandler ioHandler;
    
    public BuiltinRegistry(IOHandler ioHandler) {
        this.ioHandler = ioHandler;
        this.builtinMethods = new HashMap<String, BuiltinMethod>();
        
        // Register builtin methods
        registerBuiltins();
    }
    
    private void registerBuiltins() {
        
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