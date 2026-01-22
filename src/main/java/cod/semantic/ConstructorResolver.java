package cod.semantic;

import cod.ast.nodes.*;
import cod.ast.ASTFactory;
import cod.interpreter.*;
import cod.interpreter.context.*;
import cod.interpreter.type.*;
import static cod.syntax.Keyword.*;

import java.util.*;

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
                return new TypeValue(argValue, activeType, param.type);
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
    
       // NEW: Validate that a class implements all required policy methods from ancestors (VIRAL)
    public void validateViralPolicies(TypeNode type, ExecutionContext ctx) {
        if (type == null) {
            return;
        }
        
        // Get all policies from ancestors
        List<String> ancestorPolicies = getAncestorPolicies(type, ctx);
        
        // For each ancestor policy, check if this class implements all required methods
        for (String policyName : ancestorPolicies) {
            PolicyNode policy = findPolicy(policyName, ctx);
            if (policy != null) {
                List<PolicyMethodNode> requiredMethods = getAllPolicyMethods(policy);
                
                for (PolicyMethodNode requiredMethod : requiredMethods) {
                    // Check if this class implements this method
                    boolean implementsMethod = false;
                    
                    // Check in class methods
                    if (type.methods != null) {
                        for (MethodNode classMethod : type.methods) {
                            if (classMethod.methodName.equals(requiredMethod.methodName) && 
                                classMethod.isPolicyMethod) {
                                implementsMethod = true;
                                break;
                            }
                        }
                    }
                    
                    if (!implementsMethod) {
                        // Find which ancestor requires this
                        String requiringAncestor = findRequiringAncestor(type, policyName, ctx);
                        
                        throw new ConstructorResolutionException(
                            type.name,
                            "Missing policy method implementation: '" + requiredMethod.methodName + 
                            "' required by ancestor '" + requiringAncestor + 
                            "' which implements policy '" + policyName + "'",
                            Collections.<String>emptyList()
                        );
                    }
                }
            }
        }
    }
    
    // NEW: Get all policies from ancestors (excluding this class's own)
    private List<String> getAncestorPolicies(TypeNode type, ExecutionContext ctx) {
        List<String> ancestorPolicies = new ArrayList<String>();
        if (type == null || type.extendName == null) {
            return ancestorPolicies;
        }
        
        TypeNode current = findParentType(type, ctx);
        while (current != null) {
            if (current.implementedPolicies != null) {
                for (String policyName : current.implementedPolicies) {
                    if (!ancestorPolicies.contains(policyName)) {
                        ancestorPolicies.add(policyName);
                    }
                }
            }
            
            if (current.extendName != null) {
                current = findParentType(current, ctx);
            } else {
                current = null;
            }
        }
        
        return ancestorPolicies;
    }
    
    // NEW: Find which ancestor first implemented a policy
    private String findRequiringAncestor(TypeNode child, String policyName, ExecutionContext ctx) {
        TypeNode current = findParentType(child, ctx);
        
        while (current != null) {
            if (current.implementedPolicies != null && 
                current.implementedPolicies.contains(policyName)) {
                return current.name;
            }
            
            if (current.extendName != null) {
                current = findParentType(current, ctx);
            } else {
                current = null;
            }
        }
        
        return "unknown ancestor";
    }
    
    // NEW: Find a policy (helper method)
    private PolicyNode findPolicy(String policyName, ExecutionContext ctx) {
        // Try ImportResolver first
        if (interpreter != null && interpreter.getImportResolver() != null) {
            PolicyNode policy = interpreter.getImportResolver().findPolicy(policyName);
            if (policy != null) {
                return policy;
            }
        }
        
        // Check current program
        if (interpreter != null && interpreter.getCurrentProgram() != null) {
            ProgramNode program = interpreter.getCurrentProgram();
            if (program.unit != null && program.unit.policies != null) {
                for (PolicyNode policy : program.unit.policies) {
                    if (policy.name.equals(policyName)) {
                        return policy;
                    }
                }
            }
        }
        
        return null;
    }
    
    // NEW: Get all methods from a policy (including composed)
    private List<PolicyMethodNode> getAllPolicyMethods(PolicyNode policy) {
        if (policy == null) {
            return new ArrayList<PolicyMethodNode>();
        }
        
        List<PolicyMethodNode> allMethods = new ArrayList<PolicyMethodNode>();
        Set<String> visited = new HashSet<String>();
        
        collectPolicyMethodsRecursive(policy, allMethods, visited);
        
        return allMethods;
    }
    
    // NEW: Recursively collect policy methods
    private void collectPolicyMethodsRecursive(PolicyNode policy, List<PolicyMethodNode> allMethods, 
                                              Set<String> visited) {
        if (policy == null || visited.contains(policy.name)) {
            return;
        }
        
        visited.add(policy.name);
        
        // Collect from composed policies
        if (policy.composedPolicies != null) {
            for (String composedName : policy.composedPolicies) {
                PolicyNode composed = findPolicy(composedName, null);
                if (composed != null) {
                    collectPolicyMethodsRecursive(composed, allMethods, visited);
                }
            }
        }
        
        // Add local methods
        if (policy.methods != null) {
            allMethods.addAll(policy.methods);
        }
    }
    
    // UPDATED: Create instance with viral policy validation
    private ObjectInstance createInstance(TypeNode type, 
                                         ConstructorMatch match,
                                         ConstructorCallNode call,
                                         ExecutionContext ctx) {
        ObjectInstance obj = new ObjectInstance(type);
        
        // NEW: Validate viral policies before creating instance
        validateViralPolicies(type, ctx);
        
        // Initialize all inherited fields in order from root to leaf
        List<TypeNode> inheritanceChain = getInheritanceChain(type, ctx);
        for (TypeNode chainType : inheritanceChain) {
            initializeFields(chainType, obj);
        }
        
        ExecutionContext constrCtx = new ExecutionContext(obj, match.argumentValues, null, null);
        
        for (ParamNode param : match.constructor.parameters) {
            constrCtx.localTypes.put(param.name, param.type);
        }
        
        // Check for super constructor call at the beginning of constructor body
        boolean explicitSuperCalled = false;
        
        // Look for super constructor call (MethodCallNode with isSuperCall = true)
        if (!match.constructor.body.isEmpty() && match.constructor.body.get(0) instanceof MethodCallNode) {
            MethodCallNode firstCall = (MethodCallNode) match.constructor.body.get(0);
            if (firstCall.isSuperCall) {
                explicitSuperCalled = true;
                // Call parent constructor with the provided arguments
                invokeSuperConstructor(type, firstCall, obj, ctx, constrCtx);
            }
        }
        
        // If no explicit super call and we have inheritance, call default parent constructor
        if (!explicitSuperCalled && type.extendName != null) {
            TypeNode parentType = findParentType(type, ctx);
            if (parentType != null) {
                // Find default constructor for parent
                ConstructorMatch parentMatch = findDefaultConstructor(parentType);
                if (parentMatch != null) {
                    invokeParentConstructorSilently(parentType, parentMatch, obj, ctx, constrCtx);
                }
            }
        }
        
        visitor.pushContext(constrCtx);
        try {
            // Skip the first statement if it was a super constructor call
            int startIndex = explicitSuperCalled ? 1 : 0;
            for (int i = startIndex; i < match.constructor.body.size(); i++) {
                StmtNode stmt = match.constructor.body.get(i);
                visitor.dispatch(stmt);
            }
        } finally {
            visitor.popContext();
        }
        
        return obj;
    }


// NEW: Invoke super constructor explicitly
private void invokeSuperConstructor(TypeNode childType, MethodCallNode superCall, 
                                    ObjectInstance childObj, ExecutionContext childCtx,
                                    ExecutionContext constrCtx) {
    TypeNode parentType = findParentType(childType, childCtx);
    if (parentType == null) {
        throw new ConstructorResolutionException(
            childType.name,
            "Cannot call super constructor: parent type not found",
            Collections.<String>emptyList()
        );
    }
    
    // Convert MethodCallNode to ConstructorCallNode for parent
    ConstructorCallNode parentCall = new ConstructorCallNode();
    parentCall.className = parentType.name;
    parentCall.arguments = superCall.arguments;
    parentCall.argNames = superCall.argNames;
    
    // Find matching constructor in parent
    ConstructorMatch parentMatch = findBestMatchingConstructor(parentType, parentCall, childCtx);
    if (parentMatch == null) {
        throw new ConstructorResolutionException(
            childType.name,
            "No matching parent constructor found for arguments",
            getConstructorSignatures(parentType.constructors)
        );
    }
    
    // Create parent context and execute parent constructor
    ExecutionContext parentConstrCtx = new ExecutionContext(childObj, parentMatch.argumentValues, null, null);
    
    for (ParamNode param : parentMatch.constructor.parameters) {
        parentConstrCtx.localTypes.put(param.name, param.type);
    }
    
    visitor.pushContext(parentConstrCtx);
    try {
        for (StmtNode stmt : parentMatch.constructor.body) {
            visitor.dispatch(stmt);
        }
    } finally {
        visitor.popContext();
        visitor.pushContext(constrCtx); // Restore child constructor context
    }
}

// NEW: Invoke default parent constructor silently (without explicit call)
private void invokeParentConstructorSilently(TypeNode parentType, ConstructorMatch parentMatch,
                                            ObjectInstance childObj, ExecutionContext childCtx,
                                            ExecutionContext constrCtx) {
    ExecutionContext parentConstrCtx = new ExecutionContext(childObj, parentMatch.argumentValues, null, null);
    
    for (ParamNode param : parentMatch.constructor.parameters) {
        parentConstrCtx.localTypes.put(param.name, param.type);
    }
    
    visitor.pushContext(parentConstrCtx);
    try {
        for (StmtNode stmt : parentMatch.constructor.body) {
            visitor.dispatch(stmt);
        }
    } finally {
        visitor.popContext();
        visitor.pushContext(constrCtx); // Restore child constructor context
    }
}

// NEW: Find default constructor for a type
private ConstructorMatch findDefaultConstructor(TypeNode type) {
    if (type.constructors == null || type.constructors.isEmpty()) {
        ConstructorNode defaultConstructor = createDefaultConstructor(type);
        return new ConstructorMatch(defaultConstructor, Collections.<String, Object>emptyMap(), 0);
    }
    
    // Look for constructor with no parameters
    for (ConstructorNode constructor : type.constructors) {
        if (constructor.parameters == null || constructor.parameters.isEmpty()) {
            return new ConstructorMatch(constructor, Collections.<String, Object>emptyMap(), 0);
        }
    }
    
    return null;
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
                // UPDATED: Pass null as the token parameter to createIdentifier
                ExprNode fieldTarget = ASTFactory.createIdentifier(field.name, null);
                // UPDATED: Pass null as the token parameter to createAssignment
                AssignmentNode fieldInit = ASTFactory.createAssignment(fieldTarget, field.value, false, null);
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
        
        // NEW: Extract field name if using "this.field" syntax
        String actualFieldName = fieldName;
        if (fieldName != null && fieldName.startsWith("this.")) {
            actualFieldName = fieldName.substring(5); // Remove "this."
        }
        
        // Check if current type has the field
        for (FieldNode field : type.fields) {
            if (field.name.equals(actualFieldName)) {
                return ctx.objectInstance.fields.get(actualFieldName);
            }
        }
        
        // Check parent recursively
        if (type.extendName != null) {
            TypeNode parent = findParentType(type, ctx);
            return getFieldFromHierarchy(parent, actualFieldName, ctx);
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
    
        // NEW: Helper to get inheritance chain
    private List<TypeNode> getInheritanceChain(TypeNode type, ExecutionContext ctx) {
        List<TypeNode> chain = new ArrayList<TypeNode>();
        if (type == null) {
            return chain;
        }
        
        // Add parent first (root to leaf order)
        if (type.extendName != null) {
            TypeNode parent = findParentType(type, ctx);
            if (parent != null) {
                chain.addAll(getInheritanceChain(parent, ctx));
            }
        }
        
        // Add this type
        chain.add(type);
        
        return chain;
    }
    
    // Existing findParentType method
    public TypeNode findParentType(TypeNode childType, ExecutionContext ctx) {
        if (childType.extendName == null) {
            return null;
        }
        
        // Try to find parent via ImportResolver
        if (interpreter != null && interpreter.getImportResolver() != null) {
            TypeNode parent = interpreter.getImportResolver().findType(childType.extendName);
            if (parent != null) {
                return parent;
            }
        }
        
        // Check current program
        if (interpreter != null && interpreter.getCurrentProgram() != null) {
            ProgramNode program = interpreter.getCurrentProgram();
            if (program.unit != null && program.unit.types != null) {
                for (TypeNode type : program.unit.types) {
                    if (type.name.equals(childType.extendName)) {
                        return type;
                    }
                }
            }
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
    
    @SuppressWarnings("serial")
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