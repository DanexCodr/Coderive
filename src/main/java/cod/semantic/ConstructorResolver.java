package cod.semantic;

import cod.ast.nodes.*;
import cod.interpreter.*;
import cod.interpreter.context.*;
import cod.interpreter.type.*;
import static cod.syntax.Keyword.*;

import java.util.*;
import java.math.BigDecimal;

public class ConstructorResolver {
    private final TypeSystem typeSystem;
    private final InterpreterVisitor visitor;
    private final Interpreter interpreter;
    
    public ConstructorResolver(TypeSystem typeSystem, InterpreterVisitor visitor, Interpreter interpreter) {
        this.typeSystem = typeSystem;
        this.visitor = visitor;
        this.interpreter = interpreter;
    }
    
    public ObjectInstance resolveAndCreate(ConstructorCallNode call, ExecutionContext ctx) {
        TypeNode type = findType(call.className, ctx);
        if (type == null) {
            throw new ConstructorResolutionException(
                call.className, 
                "Type not found",
                Collections.<String>emptyList()
            );
        }
        
        // Validate inheritance hierarchy
        validateInheritanceHierarchy(type, ctx);
        
        ConstructorMatch match = findBestMatchingConstructor(type, call, ctx);
        if (match == null) {
            throw new ConstructorResolutionException(
                type.name, 
                "No matching constructor found for arguments",
                getConstructorSignatures(type.constructors)
            );
        }
        
        return createInstance(type, match, call, ctx);
    }
    
    // NEW: Validate inheritance hierarchy to prevent circular inheritance
    private void validateInheritanceHierarchy(TypeNode type, ExecutionContext ctx) {
        if (type.extendName == null) {
            return;
        }
        
        Set<String> visited = new HashSet<String>();
        visited.add(type.name);
        
        String current = type.extendName;
        while (current != null) {
            if (visited.contains(current)) {
                throw new ConstructorResolutionException(
                    type.name,
                    "Circular inheritance detected: " + 
                    String.join(" -> ", visited) + " -> " + current,
                    Collections.<String>emptyList()
                );
            }
            
            visited.add(current);
            
            // Find the next base class
            TypeNode nextType = findType(current, ctx);
            if (nextType == null || nextType.extendName == null) {
                break;
            }
            
            current = nextType.extendName;
        }
    }
    
    // NEW: Find parent type
    private TypeNode findParentType(TypeNode childType, ExecutionContext ctx) {
        if (childType.extendName == null) {
            return null;
        }
        return findType(childType.extendName, ctx);
    }
    
    // NEW: Get complete inheritance chain
    private List<TypeNode> getInheritanceChain(TypeNode type, ExecutionContext ctx) {
        List<TypeNode> chain = new ArrayList<TypeNode>();
        chain.add(type);
        
        TypeNode current = type;
        while (current.extendName != null) {
            TypeNode parent = findParentType(current, ctx);
            if (parent == null) {
                break;
            }
            chain.add(0, parent); // Add parent to beginning (so chain is [root, ..., child])
            current = parent;
        }
        
        return chain;
    }
    
    private TypeNode findType(String className, ExecutionContext ctx) {
    // 1. Check if this is the main class of the current unit
    if (interpreter != null) {
        ProgramNode currentProgram = interpreter.getCurrentProgram();
        if (currentProgram != null && currentProgram.unit != null) {
            // Check if it's the specified main class
            if (className.equals(currentProgram.unit.mainClassName)) {
                // Search in current program's types
                if (currentProgram.unit.types != null) {
                    for (TypeNode type : currentProgram.unit.types) {
                        if (type.name.equals(className)) {
                            return type;
                        }
                    }
                }
            }
        }
    }
    
    // 2. Check current object's type
    if (ctx.objectInstance != null && ctx.objectInstance.type != null && 
        ctx.objectInstance.type.name.equals(className)) {
        return ctx.objectInstance.type;
    }
    
    // 3. Check current program
    ProgramNode currentProgram = interpreter.getCurrentProgram();
    if (currentProgram != null && currentProgram.unit != null && currentProgram.unit.types != null) {
        for (TypeNode type : currentProgram.unit.types) {
            if (type.name.equals(className)) {
                return type;
            }
        }
    }
    
    // 4. Use ImportResolver to find type (NEW - consistent with method resolution)
    ImportResolver resolver = interpreter.getImportResolver();
    TypeNode type = resolver.findType(className);
    
    if (type != null) {
        return type;
    }
    
    // 5. Also check if it's a fully qualified name in loaded imports
    // (e.g., "lang.Sys" where className is "lang.Sys", not just "Sys")
    if (resolver != null) {
        for (String loadedImport : resolver.getLoadedImports()) {
            if (loadedImport.endsWith("." + className) || loadedImport.equals(className)) {
                try {
                    ProgramNode program = resolver.resolveImport(loadedImport);
                    if (program != null && program.unit != null && program.unit.types != null) {
                        for (TypeNode t : program.unit.types) {
                            if (t.name.equals(className) || 
                                loadedImport.endsWith("." + t.name)) {
                                return t;
                            }
                        }
                    }
                } catch (Exception e) {
                    continue;
                }
            }
        }
    }
    
    return null;
}
    
    private ConstructorMatch findBestMatchingConstructor(TypeNode type, 
                                                       ConstructorCallNode call,
                                                       ExecutionContext ctx) {
        if (type.constructors == null || type.constructors.isEmpty()) {
            ConstructorNode defaultConstructor = createDefaultConstructor(type);
            return new ConstructorMatch(defaultConstructor, Collections.<String, Object>emptyMap(), 0);
        }
        
        List<ConstructorMatch> candidates = new ArrayList<ConstructorMatch>();
        
        for (ConstructorNode constructor : type.constructors) {
            ConstructorMatch match = tryMatchConstructor(constructor, call, ctx);
            if (match != null) {
                candidates.add(match);
            }
        }
        
        if (candidates.isEmpty()) {
            return null;
        }
        
        return selectBestMatch(candidates);
    }
    
    private ConstructorMatch tryMatchConstructor(ConstructorNode constructor,
                                               ConstructorCallNode call,
                                               ExecutionContext ctx) {
        if (call.argNames != null && !call.argNames.isEmpty() && call.argNames.get(0) != null) {
            return tryMatchNamedConstructor(constructor, call, ctx);
        } else {
            return tryMatchPositionalConstructor(constructor, call, ctx);
        }
    }
    
    private ConstructorMatch tryMatchNamedConstructor(ConstructorNode constructor,
                                                    ConstructorCallNode call,
                                                    ExecutionContext ctx) {
        Map<String, Object> argumentValues = new HashMap<String, Object>();
        Map<String, ExprNode> providedArgs = new HashMap<String, ExprNode>();
        int conversionScore = 0;
        
        for (int i = 0; i < call.arguments.size(); i++) {
            providedArgs.put(call.argNames.get(i), call.arguments.get(i));
        }
        
        for (ParamNode param : constructor.parameters) {
            if (providedArgs.containsKey(param.name)) {
                ExprNode argExpr = providedArgs.get(param.name);
                Object argValue = evaluateArgument(argExpr, param, ctx);
                
                if (argValue == null) {
                    return null;
                }
                
                if (!typeSystem.validateType(param.type, argValue)) {
                    return null;
                }
                
                argumentValues.put(param.name, argValue);
                
                if (typeSystem.getConcreteType(typeSystem.unwrap(argValue)).equals("text") && 
                    !param.type.equals("text")) {
                    conversionScore++;
                }
            } else if (param.hasDefaultValue) {
                Object defaultValue = visitor.dispatch(param.defaultValue);
                argumentValues.put(param.name, defaultValue);
                conversionScore++;
            } else {
                return null;
            }
        }
        
        for (String argName : call.argNames) {
            boolean found = false;
            for (ParamNode param : constructor.parameters) {
                if (param.name.equals(argName)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return null;
            }
        }
        
        return new ConstructorMatch(constructor, argumentValues, conversionScore);
    }
    
    private ConstructorMatch tryMatchPositionalConstructor(ConstructorNode constructor,
                                                         ConstructorCallNode call,
                                                         ExecutionContext ctx) {
        Map<String, Object> argumentValues = new HashMap<String, Object>();
        int conversionScore = 0;
        int argIndex = 0;
        int paramIndex = 0;
        
        while (paramIndex < constructor.parameters.size()) {
            ParamNode param = constructor.parameters.get(paramIndex);
            
            if (argIndex < call.arguments.size()) {
                ExprNode argExpr = call.arguments.get(argIndex);
                
                if (isUnderscore(argExpr)) {
                    if (!param.hasDefaultValue) {
                        return null;
                    }
                    Object defaultValue = visitor.dispatch(param.defaultValue);
                    argumentValues.put(param.name, defaultValue);
                    conversionScore++;
                } else {
                    Object argValue = evaluateArgument(argExpr, param, ctx);
                    
                    if (argValue == null) {
                        return null;
                    }
                    
                    if (!typeSystem.validateType(param.type, argValue)) {
                        return null;
                    }
                    
                    argumentValues.put(param.name, argValue);
                    
                    if (typeSystem.getConcreteType(typeSystem.unwrap(argValue)).equals("text") && 
                        !param.type.equals("text")) {
                        conversionScore++;
                    }
                }
                argIndex++;
            } else if (param.hasDefaultValue) {
                Object defaultValue = visitor.dispatch(param.defaultValue);
                argumentValues.put(param.name, defaultValue);
                conversionScore++;
            } else {
                return null;
            }
            
            paramIndex++;
        }
        
        if (argIndex < call.arguments.size()) {
            return null;
        }
        
        return new ConstructorMatch(constructor, argumentValues, conversionScore);
    }
    
    private Object evaluateArgument(ExprNode argExpr, ParamNode param, ExecutionContext ctx) {
        ExecutionContext savedCtx = visitor.getCurrentContext();
        visitor.pushContext(ctx);
        
        try {
            Object argValue = visitor.dispatch(argExpr);
            
            if (!typeSystem.validateType(param.type, argValue)) {
                if (param.type.equals(TEXT.toString())) {
                    return typeSystem.convertType(argValue, param.type);
                }
                return null;
            }
            
            if (param.type.contains("|")) {
                String activeType = typeSystem.getConcreteType(typeSystem.unwrap(argValue));
                return new TypedValue(argValue, activeType, param.type);
            }
            
            return argValue;
        } finally {
            visitor.popContext();
            if (savedCtx != null) {
                visitor.pushContext(savedCtx);
            }
        }
    }
    
    private boolean isUnderscore(ExprNode expr) {
        return expr instanceof ExprNode && "_".equals(((ExprNode) expr).name);
    }
    
    private ConstructorMatch selectBestMatch(List<ConstructorMatch> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }
        
        ConstructorMatch bestMatch = candidates.get(0);
        int bestScore = bestMatch.conversionScore;
        int bestArgCount = bestMatch.argumentValues.size();
        
        for (int i = 1; i < candidates.size(); i++) {
            ConstructorMatch current = candidates.get(i);
            int currentScore = current.conversionScore;
            int currentArgCount = current.argumentValues.size();
            
            // Prefer fewer conversions
            if (currentScore < bestScore) {
                bestMatch = current;
                bestScore = currentScore;
                bestArgCount = currentArgCount;
            } 
            // If equal conversions, prefer more arguments (fewer defaults)
            else if (currentScore == bestScore && currentArgCount > bestArgCount) {
                bestMatch = current;
                bestArgCount = currentArgCount;
            }
        }
        
        return bestMatch;
    }
    
    private ObjectInstance createInstance(TypeNode type, 
                                         ConstructorMatch match,
                                         ConstructorCallNode call,
                                         ExecutionContext ctx) {
        ObjectInstance obj = new ObjectInstance(type);
        
        // NEW: Initialize all inherited fields in order from root to leaf
        List<TypeNode> inheritanceChain = getInheritanceChain(type, ctx);
        for (TypeNode chainType : inheritanceChain) {
            initializeFields(chainType, obj);
        }
        
        ExecutionContext constrCtx = new ExecutionContext(obj, match.argumentValues, null, null);
        
        for (ParamNode param : match.constructor.parameters) {
            constrCtx.localTypes.put(param.name, param.type);
        }
        
        visitor.pushContext(constrCtx);
        try {
            for (StmtNode stmt : match.constructor.body) {
                visitor.dispatch(stmt);
            }
        } finally {
            visitor.popContext();
        }
        
        return obj;
    }
    
    private void initializeFields(TypeNode type, ObjectInstance obj) {
        for (FieldNode field : type.fields) {
            if (field.value != null) {
                Object defaultValue = visitor.dispatch(field.value);
                obj.fields.put(field.name, defaultValue);
            } else {
                String fieldType = field.type;
                if (fieldType.contains(INT.toString())) {
                    obj.fields.put(field.name, 0);
                } else if (fieldType.contains(FLOAT.toString())) {
                    obj.fields.put(field.name, 0.0);
                } else if (fieldType.contains(TEXT.toString())) {
                    obj.fields.put(field.name, "");
                } else if (fieldType.contains(BOOL.toString())) {
                    obj.fields.put(field.name, false);
                } else {
                    obj.fields.put(field.name, null);
                }
            }
        }
    }
    
    private ConstructorNode createDefaultConstructor(TypeNode type) {
        ConstructorNode defaultConstructor = new ConstructorNode();
        defaultConstructor.parameters = new ArrayList<ParamNode>();
        defaultConstructor.body = new ArrayList<StmtNode>();
        
        for (FieldNode field : type.fields) {
            if (field.value != null) {
                SlotAssignmentNode fieldInit = new SlotAssignmentNode();
                fieldInit.slotName = field.name;
                fieldInit.value = field.value;
                defaultConstructor.body.add(fieldInit);
            }
        }
        
        return defaultConstructor;
    }
    
    private List<String> getConstructorSignatures(List<ConstructorNode> constructors) {
        List<String> signatures = new ArrayList<String>();
        if (constructors == null || constructors.isEmpty()) {
            signatures.add("() [default constructor]");
            return signatures;
        }
        
        for (ConstructorNode c : constructors) {
            StringBuilder sb = new StringBuilder("(");
            for (int i = 0; i < c.parameters.size(); i++) {
                if (i > 0) sb.append(", ");
                ParamNode p = c.parameters.get(i);
                sb.append(p.name).append(": ").append(p.type);
                if (p.hasDefaultValue) {
                    sb.append(" = default");
                }
            }
            sb.append(")");
            signatures.add(sb.toString());
        }
        return signatures;
    }
    
    // NEW: Helper to get field from inheritance hierarchy
    public Object getFieldFromHierarchy(TypeNode type, String fieldName, ExecutionContext ctx) {
        if (type == null) return null;
        
        // Check current type's fields
        TypeNode fieldOwner = findFieldOwnerInHierarchy(type, fieldName, ctx);
        return fieldOwner != null ? ctx.objectInstance.fields.get(fieldName) : null;
    }
    
    // NEW: Find which type in hierarchy owns this field
    private TypeNode findFieldOwnerInHierarchy(TypeNode type, String fieldName, ExecutionContext ctx) {
        if (type == null) return null;
        
        // Check if current type has the field
        for (FieldNode field : type.fields) {
            if (field.name.equals(fieldName)) {
                return type;
            }
        }
        
        // Check parent recursively
        if (type.extendName != null) {
            TypeNode parent = findParentType(type, ctx);
            return findFieldOwnerInHierarchy(parent, fieldName, ctx);
        }
        
        return null;
    }
    
    // NEW: Find method in inheritance hierarchy
    public MethodNode findMethodInHierarchy(TypeNode type, String methodName, ExecutionContext ctx) {
        if (type == null) return null;
        
        // Check current type's methods
        for (MethodNode method : type.methods) {
            if (method.methodName.equals(methodName)) {
                return method;
            }
        }
        
        // Check parent recursively
        if (type.extendName != null) {
            TypeNode parent = findParentType(type, ctx);
            return findMethodInHierarchy(parent, methodName, ctx);
        }
        
        return null;
    }
    
    private static class ConstructorMatch {
        final ConstructorNode constructor;
        final Map<String, Object> argumentValues;
        final int conversionScore;
        
        ConstructorMatch(ConstructorNode constructor, Map<String, Object> argumentValues, int conversionScore) {
            this.constructor = constructor;
            this.argumentValues = argumentValues;
            this.conversionScore = conversionScore;
        }
    }
    
    public static class ConstructorResolutionException extends RuntimeException {
        private final List<String> availableSignatures;
        
        public ConstructorResolutionException(String className, String message, List<String> signatures) {
            super("Constructor resolution error for " + className + ": " + message);
            this.availableSignatures = signatures;
        }
        
        @Override
        public String getMessage() {
            StringBuilder sb = new StringBuilder(super.getMessage());
            if (availableSignatures != null && !availableSignatures.isEmpty()) {
                sb.append("\nAvailable constructors:");
                for (String sig : availableSignatures) {
                    sb.append("\n  ").append(sig);
                }
            }
            return sb.toString();
        }
    }
}