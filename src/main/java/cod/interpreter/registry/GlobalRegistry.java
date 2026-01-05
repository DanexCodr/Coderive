package cod.interpreter.registry;

import cod.ast.nodes.*;
import cod.debug.DebugSystem;
import cod.interpreter.InterpreterVisitor;
import cod.interpreter.io.IOHandler;
import cod.interpreter.type.TypeSystem;
import java.util.*;

public class GlobalRegistry {
    
    private final Map<String, GlobalFunction> globalFunctions;
    private final BuiltinRegistry builtinRegistry;
    private final TypeSystem typeSystem;
    private final IOHandler ioHandler;
    
    public GlobalRegistry(IOHandler ioHandler, TypeSystem typeSystem, BuiltinRegistry builtinRegistry) {
        this.ioHandler = ioHandler;
        this.typeSystem = typeSystem;
        this.builtinRegistry = builtinRegistry;
        this.globalFunctions = new HashMap<String, GlobalFunction>();
        
        registerGlobalFunctions();
    }
    
    private void registerGlobalFunctions() {
        registerGlobal("out", new GlobalFunction() {
            @Override
            public Object execute(List<ExprNode> arguments, InterpreterVisitor visitor) {
                StringBuilder result = new StringBuilder();
                if (arguments != null) {
                    for (ExprNode arg : arguments) {
                        Object value = visitor.visit((ASTNode) arg);
                        result.append(String.valueOf(value));
                    }
                }
                ioHandler.output(result.toString());
                return null;
            }
            
            @Override
            public String getSignature() {
                return "out(...values) -> void";
            }
        });
        
        registerGlobal("outln", new GlobalFunction() {
            @Override
            public Object execute(List<ExprNode> arguments, InterpreterVisitor visitor) {
                StringBuilder result = new StringBuilder();
                if (arguments != null) {
                    for (ExprNode arg : arguments) {
                        Object value = visitor.visit((ASTNode) arg);
                        result.append(String.valueOf(value));
                    }
                }
                result.append("\n");
                ioHandler.output(result.toString());
                return null;
            }
            
            @Override
            public String getSignature() {
                return "outln(...values) -> void";
            }
        });
        
        registerGlobal("in", new GlobalFunction() {
            @Override
            public Object execute(List<ExprNode> arguments, InterpreterVisitor visitor) {
                int argCount = arguments != null ? arguments.size() : 0;
                
                String expectedType = "text";
                String message = "";
                
                if (argCount == 0) {
                    expectedType = "text";
                } 
                else if (argCount == 1) {
                    Object typeArg = visitor.visit((ASTNode) arguments.get(0));
                    expectedType = String.valueOf(typeArg);
                }
                else if (argCount == 2) {
                    Object typeArg = visitor.visit((ASTNode) arguments.get(0));
                    Object messageArg = visitor.visit((ASTNode) arguments.get(1));
                    
                    expectedType = String.valueOf(typeArg);
                    message = String.valueOf(messageArg);
                }
                else if (argCount == 3) {
                    Object typeArg = visitor.visit((ASTNode) arguments.get(0));
                    Object messageArg = visitor.visit((ASTNode) arguments.get(1));
                    Object sourceArg = visitor.visit((ASTNode) arguments.get(2));
                    
                    expectedType = String.valueOf(typeArg);
                    message = String.valueOf(messageArg);
                    String source = String.valueOf(sourceArg);
                    
                    if (!"stdin".equals(source)) {
                        throw new RuntimeException("Only 'stdin' source is currently supported for input");
                    }
                }
                
                if (!message.isEmpty()) {
                    ioHandler.output(message);
                }
                
                return ioHandler.readInput(expectedType);
            }
            
            @Override
            public String getSignature() {
                return "in() | in(type) | in(type, message) | in(type, message, source) -> value";
            }
        });
        
        // In registerGlobalFunctions() method
registerGlobal("timer", new GlobalFunction() {
    @Override
    public Object execute(List<ExprNode> arguments, InterpreterVisitor visitor) {
        // Same implementation logic
        int argCount = arguments != null ? arguments.size() : 0;
        long nanos = System.nanoTime();
        
        if (argCount == 0) return nanos / 1_000_000.0;
        
        Object unitArg = visitor.visit((ASTNode) arguments.get(0));
        String unit = String.valueOf(unitArg).toLowerCase();
        
        switch (unit) {
            case "ns": return (double) nanos;
            case "us": return nanos / 1_000.0;
            case "ms": return nanos / 1_000_000.0;
            case "s": return nanos / 1_000_000_000.0;
            default:
                throw new RuntimeException("Unknown time unit: " + unit);
        }
    }
    
    @Override
    public String getSignature() {
        return "timer() -> ms | timer(unit) -> time_in_unit";
    }
});
    }
    
    private void registerGlobal(String name, GlobalFunction function) {
        globalFunctions.put(name, function);
        DebugSystem.debug("GLOBAL", "Registered global function: " + name + " -> " + function.getSignature());
    }
    
    public boolean isGlobal(String functionName) {
        return globalFunctions.containsKey(functionName) || 
               builtinRegistry.isBuiltin(functionName);
    }
    
    public Object executeGlobal(String functionName, List<ExprNode> arguments, InterpreterVisitor visitor) {
        GlobalFunction globalFunc = globalFunctions.get(functionName);
        if (globalFunc != null) {
            DebugSystem.debug("GLOBAL", "Executing global function: " + functionName);
            return globalFunc.execute(arguments, visitor);
        }
        
        if (builtinRegistry.isBuiltin(functionName)) {
            MethodCallNode dummyCall = new MethodCallNode();
            dummyCall.name = functionName;
            dummyCall.arguments = arguments;
            DebugSystem.debug("GLOBAL", "Executing builtin as global: " + functionName);
            return builtinRegistry.executeBuiltin(functionName, dummyCall, visitor);
        }
        
        throw new RuntimeException("Unknown global function: " + functionName);
    }
    
    public Set<String> getGlobalFunctions() {
        Set<String> allFunctions = new HashSet<String>(globalFunctions.keySet());
        allFunctions.addAll(builtinRegistry.getRegisteredBuiltins());
        return Collections.unmodifiableSet(allFunctions);
    }
    
    public interface GlobalFunction {
        Object execute(List<ExprNode> arguments, InterpreterVisitor visitor);
        String getSignature();
    }
}