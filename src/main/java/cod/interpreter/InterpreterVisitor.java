package cod.interpreter;

import cod.ast.ASTFactory;
import cod.ast.ASTVisitor;
import cod.ast.node.*;
import cod.debug.DebugSystem;
import cod.error.InternalError;
import cod.error.ProgramError;
import cod.math.AutoStackingNumber;
import cod.range.*;
import cod.range.formula.*;
import cod.range.pattern.*;
import cod.interpreter.registry.*;
import cod.interpreter.context.*;
import cod.interpreter.exception.*;
import cod.interpreter.handler.*;
import java.util.*;
import static cod.lexer.TokenType.Keyword.*;
import cod.semantic.ConstructorResolver;
import cod.semantic.NamingValidator;

public class InterpreterVisitor extends ASTVisitor<Object> implements Evaluator {
    private static final String SELF_CALL_PLACEHOLDER = "<~";
    private static final String SELF_CALL_LAMBDA_OWNER = "self-call lambda";
    private static final double SELF_CALL_LEVEL_FLOAT_EPSILON = 1e-12d;

    // Tail-call trampolining intentionally uses this internal signal to unwind Java frames
    // without allocating wrapper result objects through every visitor return path.
    // This favors lower allocation overhead over exception cost in non-tail paths.

    enum PatternType {
        CONDITIONAL,
        SEQUENCE,
        LINEAR_RECURRENCE
    }

    class PatternResult {
        public final PatternType type;
        public final Object pattern;
        public final Expr targetArray;
        
        public PatternResult(PatternType type, Object pattern, Expr targetArray) {
            if (type == null) {
                throw new InternalError("PatternResult constructed with null type");
            }
            this.type = type;
            this.pattern = pattern;
            this.targetArray = targetArray;
        }
    }

    private static class LinearRecurrencePattern {
        public final Expr targetArray;
        public final int order;
        public final AutoStackingNumber[] coefficientsByLag;
        public final AutoStackingNumber constantTerm;
        public final long recurrenceStart;
        public final long seedStart;
        public final AutoStackingNumber[] seedValues;

        LinearRecurrencePattern(
            Expr targetArray,
            int order,
            AutoStackingNumber[] coefficientsByLag,
            AutoStackingNumber constantTerm,
            long recurrenceStart,
            long seedStart,
            AutoStackingNumber[] seedValues
        ) {
            this.targetArray = targetArray;
            this.order = order;
            this.coefficientsByLag = coefficientsByLag;
            this.constantTerm = constantTerm;
            this.recurrenceStart = recurrenceStart;
            this.seedStart = seedStart;
            this.seedValues = seedValues;
        }
    }

    private final Interpreter interpreter;
    public final TypeHandler typeSystem;
    private final Stack<ExecutionContext> contextStack = new Stack<ExecutionContext>();
    private final ExpressionHandler expressionHandler;
    private final AssignmentHandler assignmentHandler;
    private final LiteralRegistry literalRegistry;
    private final ContextHandler contextHandler;
    private final LambdaInvokingHandler lambdaInvokingHandler;
    private final ArrayOperationHandler arrayOperationHandler;
    private final PatternHandler patternHandler;
    private final LoopOptimizationHandler loopOptimizationHandler;
    
    // ========== SIMPLE LOOP OPTIMIZATION CONSTANTS ==========
    private static final int LAZY_THRESHOLD = 10;  // From your data: 10+ iterations = worth it
    private static final int MAX_SUPPORTED_LAG = 64;

    public InterpreterVisitor(Interpreter interpreter, TypeHandler typeSystem, 
                              LiteralRegistry literalRegistry) {
        if (interpreter == null) {
            throw new InternalError("InterpreterVisitor constructed with null interpreter");
        }
        if (typeSystem == null) {
            throw new InternalError("InterpreterVisitor constructed with null typeSystem");
        }
        if (literalRegistry == null) {
            throw new InternalError("InterpreterVisitor constructed with null literalRegistry");
        }
        
        this.interpreter = interpreter;
        this.typeSystem = typeSystem;
        this.literalRegistry = literalRegistry;
        this.contextHandler = new ContextHandler(interpreter);
        this.expressionHandler = new ExpressionHandler(typeSystem, this);
        this.assignmentHandler = new AssignmentHandler(typeSystem, interpreter, expressionHandler, this);
        this.arrayOperationHandler =
            new ArrayOperationHandler(this, interpreter, typeSystem, expressionHandler, contextHandler);
        this.patternHandler =
            new PatternHandler(this, typeSystem, expressionHandler, arrayOperationHandler);
        this.loopOptimizationHandler =
            new LoopOptimizationHandler(this, typeSystem, expressionHandler, arrayOperationHandler, patternHandler);
        this.lambdaInvokingHandler = new LambdaInvokingHandler(typeSystem, this);
    }
    
    // Implement Evaluator interface
    @Override
    public Object evaluate(Expr node, ExecutionContext ctx) {
        if (node == null) {
            throw new InternalError("evaluate called with null node");
        }
        if (ctx == null) {
            throw new InternalError("evaluate called with null context");
        }
        
        pushContext(ctx);
        try {
            return dispatch(node);
        } finally {
            popContext();
        }
    }

    @Override
    public Object evaluate(Stmt node, ExecutionContext ctx) {
        if (node == null) {
            throw new InternalError("evaluate called with null node");
        }
        if (ctx == null) {
            throw new InternalError("evaluate called with null context");
        }
        
        pushContext(ctx);
        try {
            return dispatch(node);
        } finally {
            popContext();
        }
    }
    
    @Override
    public Object invokeLambda(Object callback, List<Object> arguments, ExecutionContext ctx, String ownerMethod) {
        if (ctx == null) {
            throw new InternalError("invokeLambda called with null context");
        }
        return lambdaInvokingHandler.invokeLambdaCallback(callback, arguments, ctx, ownerMethod);
    }

    public void pushContext(ExecutionContext context) {
        if (context == null) {
            throw new InternalError("pushContext called with null context");
        }
        contextStack.push(context);
        ExecutionContext.setCurrentContext(context);
    }

    public void popContext() {
        if (contextStack.isEmpty()) {
            throw new InternalError("popContext called on empty stack");
        }
        contextStack.pop();
        if (!contextStack.isEmpty()) {
            ExecutionContext.setCurrentContext(contextStack.peek());
        } else {
            ExecutionContext.clearCurrentContext();
        }
    }

    public ExecutionContext getCurrentContext() {
        if (contextStack.isEmpty()) {
            throw new InternalError("getCurrentContext called on empty stack");
        }
        return contextStack.peek();
    }
    
    public boolean isContextStackEmpty() {
        return contextStack.isEmpty();
    }

    public boolean shouldReturnEarly(Map<String, Object> slotValues, Set<String> slotsInCurrentPath) {
        return interpreter.shouldReturnEarly(slotValues, slotsInCurrentPath);
    }

    private Object createNoneValue() {
        return contextHandler.createNoneValue();
    }

    @Override
    public Object visit(Program n) {
        return defaultVisit(n);
    }

    @Override
    public Object visit(Unit n) {
        return defaultVisit(n);
    }

    @Override
    public Object visit(Use n) {
        return defaultVisit(n);
    }

    @Override
    public Object visit(Type n) {
        return defaultVisit(n);
    }

    @Override
    public Object visit(Field n) {
        if (n == null) {
            throw new InternalError("visit(Field) called with null node");
        }
        
        ExecutionContext ctx = getCurrentContext();
        Object val = n.value != null ? dispatch(n.value) : null;
        
        if (ctx.objectInstance.type != null) {
            Object existingField =
                interpreter
                    .getConstructorResolver()
                    .getFieldFromHierarchy(ctx.objectInstance.type, n.name, ctx);
            if (existingField != null) {
                throw new ProgramError("Cannot redeclare field: " + n.name);
            }
        }
        ctx.setObjectField(n.name, val);
        return val;
    }

    @Override
    public Object visit(Method n) {
        return defaultVisit(n);
    }

    @Override
    public Object visit(Param n) {
        return defaultVisit(n);
    }

    @Override
    public Object visit(Constructor n) {
        return defaultVisit(n);
    }

    @Override
    public Object visit(ConstructorCall node) {
        if (node == null) {
            throw new InternalError("visit(ConstructorCall) called with null node");
        }
        
        try {
            ExecutionContext ctx = getCurrentContext();
            Type targetType = null;
            try {
                targetType = interpreter.getImportResolver().findType(node.className);
            } catch (ProgramError ignore) {
                Program currentProgram = interpreter.getCurrentProgram();
                if (currentProgram != null
                    && currentProgram.unit != null
                    && currentProgram.unit.types != null) {
                    for (Type localType : currentProgram.unit.types) {
                        if (localType != null && node.className.equals(localType.name)) {
                            targetType = localType;
                            break;
                        }
                    }
                }
                if (targetType == null) {
                    throw ignore;
                }
            }
            if (targetType != null
                && targetType.isUnsafe
                && !isUnsafeExecutionContext(ctx)
                && !ExecutionContext.isUnsafeCommitAllowed()) {
                throw new ProgramError(
                    "Unsafe class '" + targetType.name + "' cannot be constructed in a safe context. Use safe("
                        + targetType.name
                        + "(...)).");
            }
            return interpreter.getConstructorResolver().resolveAndCreate(node, getCurrentContext());
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Constructor resolution failed", e);
        }
    }

    @Override
    public Object visit(Policy n) {
        return defaultVisit(n);
    }

    @Override
    public Object visit(PolicyMethod n) {
        return defaultVisit(n);
    }

    @Override
    public Object visit(Block node) {
        if (node == null) {
            throw new InternalError("visit(Block) called with null node");
        }
        
        ExecutionContext ctx = getCurrentContext();
        ctx.pushScope();
        
        try {
            for (Stmt stmt : node.statements) {
                dispatch(stmt);
                // Early return check: stop executing nested block statements once required
                // return slots for the current path are assigned.
                if (!ctx.slotsInCurrentPath.isEmpty()
                    && interpreter.shouldReturnEarly(ctx.getSlotValues(), ctx.slotsInCurrentPath)) {
                    break;
                }
            }
        } catch (TailCallSignal e) {
            throw e;
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Block execution failed", e);
        } finally {
            ctx.popScope();
        }
        
        return null;
    }

    @Override
    public Object visit(Assignment node) {
        if (node == null) {
            throw new InternalError("visit(Assignment) called with null node");
        }
        
        try {
            return assignmentHandler.handleAssignment(node, getCurrentContext());
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Assignment failed", e);
        }
    }

    @Override
    public Object visit(Var node) {
        if (node == null) {
            throw new InternalError("visit(Var) called with null node");
        }
        
        try {
            ExecutionContext ctx = getCurrentContext();
            if (NamingValidator.isAllCaps(node.name)) {
                if (node.value == null) {
                    throw new ProgramError("Constant '" + node.name + "' must have an initial value");
                }
                if (contextHandler.isVariableDeclaredInAnyScope(ctx, node.name)) {
                    throw new ProgramError("Cannot reassign constant '" + node.name + "'");
                }
            }

            Object val = node.value != null ? dispatch(node.value) : null;
            
            // Handle array type conversion for [text] = [int range]
            if (node.explicitType != null && node.explicitType.startsWith("[") && 
                node.explicitType.endsWith("]") && val instanceof NaturalArray) {
                
                NaturalArray arr = (NaturalArray) val;
                String expectedElementType = node.explicitType.substring(1, node.explicitType.length() - 1);
                String actualElementType = arr.getElementType();
                
                // If expected is [text] but actual is not text, create a converting wrapper
                if (expectedElementType.equals("text") && !actualElementType.equals("text")) {
                    // Create a new NaturalArray with conversion enabled
                    Range range = contextHandler.getRangeFromArray(arr);
                    if (range != null) {
                        val = new NaturalArray(range, this, ctx, node.explicitType);
                    }
                }
            }
            
            ctx.setVariable(node.name, val);
            
            if (node.explicitType != null) {
                String declaredType = node.explicitType;
                String resolvedDeclaredType = resolveVariableTypeAliasIfAny(declaredType, ctx);
                ctx.setVariableType(node.name, resolvedDeclaredType);
                
                if (TYPE.toString().equals(declaredType)) {
                    if (val instanceof String) {
                        String typeStr = (String) val;
                        if (typeSystem.isTypeLiteral(typeStr)) {
                            val = TypeHandler.Value.createTypeValue(typeStr);
                            ctx.setVariable(node.name, val);
                        }
                    } else if (val instanceof TextLiteral) {
                        String typeStr = ((TextLiteral) val).value;
                        if (typeSystem.isTypeLiteral(typeStr)) {
                            val = TypeHandler.Value.createTypeValue(typeStr);
                            ctx.setVariable(node.name, val);
                        }
                    }
                }
                
                if (val == null && resolvedDeclaredType.contains("|none")) {
                    val = createNoneValue();
                    ctx.setVariable(node.name, val);
                }
                
                if (!typeSystem.validateType(resolvedDeclaredType, val)) {
                    if (typeSystem.isNoneValue(val) && resolvedDeclaredType.contains("|none")) {
                        val = createNoneValue();
                        ctx.setVariable(node.name, val);
                    } else {
                        throw new ProgramError("Type mismatch for " + node.name + ". Expected " + resolvedDeclaredType);
                    }
                }
                
                if (resolvedDeclaredType != null && resolvedDeclaredType.indexOf('|') >= 0) {
                    String activeType = typeSystem.getConcreteType(typeSystem.unwrap(val));
                    val = new TypeHandler.Value(val, activeType, resolvedDeclaredType);
                    ctx.setVariable(node.name, val);
                }
            }
            
            return val;
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Variable declaration failed: " + node.name, e);
        }
    }

    // Helper method to extract Range from NaturalArray
    private Range getRangeFromArray(NaturalArray arr) {
        return contextHandler.getRangeFromArray(arr);
    }

    private Type resolveInternalRangeSpecType() {
        return contextHandler.resolveInternalRangeSpecType();
    }

    private Type resolveInternalMultiRangeSpecType() {
        return contextHandler.resolveInternalMultiRangeSpecType();
    }

    private boolean isVariableDeclaredInAnyScope(ExecutionContext ctx, String name) {
        return contextHandler.isVariableDeclaredInAnyScope(ctx, name);
    }

    private String resolveVariableTypeAliasIfAny(String declaredType, ExecutionContext ctx) {
        if (declaredType == null || typeSystem.isTypeLiteral(declaredType)) {
            return declaredType;
        }
        if (ctx == null) {
            return declaredType;
        }

        AliasLookupResult alias = findTypeAliasValue(declaredType, ctx);
        if (alias == null || !alias.found) {
            return declaredType;
        }

        if (!NamingValidator.isAllCaps(declaredType)) {
            throw new ProgramError(
                "Type alias '" + declaredType + "' must be declared as a constant name (ALL_CAPS). " +
                "Declare it like: BYTE: type = u8");
        }

        Object rawAliasValue = alias.value;
        if (rawAliasValue instanceof TypeHandler.Value && ((TypeHandler.Value) rawAliasValue).isTypeValue()) {
            Object typeSig = ((TypeHandler.Value) rawAliasValue).value;
            if (typeSig instanceof String && typeSystem.isTypeLiteral((String) typeSig)) {
                return (String) typeSig;
            }
        }
        Object aliasValue = typeSystem.unwrap(rawAliasValue);
        if (aliasValue instanceof String && typeSystem.isTypeLiteral((String) aliasValue)) {
            return (String) aliasValue;
        }
        if (aliasValue instanceof TextLiteral) {
            String literal = ((TextLiteral) aliasValue).value;
            if (typeSystem.isTypeLiteral(literal)) {
                return literal;
            }
        }

        throw new ProgramError(
            "Type alias constant '" + declaredType + "' must hold a type value. " +
            "Declare it like: BYTE: type = u8");
    }

    private AliasLookupResult findTypeAliasValue(String aliasName, ExecutionContext ctx) {
        for (int i = ctx.getScopeDepth() - 1; i >= 0; i--) {
            Map<String, Object> scope = ctx.getLocalsStack().get(i);
            if (scope.containsKey(aliasName)) {
                return new AliasLookupResult(true, scope.get(aliasName));
            }
        }
        Map<String, Object> slots = ctx.getSlotValues();
        if (slots != null && slots.containsKey(aliasName)) {
            return new AliasLookupResult(true, slots.get(aliasName));
        }
        return new AliasLookupResult(false, null);
    }

    private static final class AliasLookupResult {
        private final boolean found;
        private final Object value;

        private AliasLookupResult(boolean found, Object value) {
            this.found = found;
            this.value = value;
        }
    }

    @Override
    public Object visit(StmtIf node) {
        if (node == null) {
            throw new InternalError("visit(StmtIf) called with null node");
        }
        
        try {
            Object testObj = dispatch(node.condition);
            boolean test = typeSystem.isTruthy(typeSystem.unwrap(testObj));
            
            ExecutionContext ctx = getCurrentContext();
            
            int originalDepth = ctx.getScopeDepth();
            
            try {
                ctx.pushScope();
                List<Stmt> statements = test ? node.thenBlock.statements : node.elseBlock.statements;
                for (Stmt s : statements) {
                    dispatch(s);
                    if (!ctx.slotsInCurrentPath.isEmpty()
                        && interpreter.shouldReturnEarly(ctx.getSlotValues(), ctx.slotsInCurrentPath)) break;
                }
            } finally {
                while (ctx.getScopeDepth() > originalDepth) {
                    ctx.popScope();
                }
            }
            
            return null;
        } catch (SkipIterationException e) {
            throw e;
        } catch (BreakLoopException e) {
            throw e;
        } catch (TailCallSignal e) {
            throw e;
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("If statement execution failed", e);
        }
    }
    
    @Override
    public Object visit(ExprIf node) {
        if (node == null) {
            throw new InternalError("visit(ExprIf) called with null node");
        }
        
        try {
            Object condValue = dispatch(node.condition);
            if (typeSystem.isTruthy(typeSystem.unwrap(condValue))) {
                return dispatch(node.thenExpr);
            }
            return dispatch(node.elseExpr);
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("If expression execution failed", e);
        }
    }

    // ========== UPDATED FOR NODE WITH SIMPLE LOOP DECISION ==========
    @Override
    public Object visit(For node) {
        return loopOptimizationHandler.executeForLoop(node);
    }

    // ========== SIMPLE LOOP DECISION METHODS ==========
    
    /**
     * Simple decision: Should we try lazy execution?
     * Based on your data: 10+ iterations is worth it
     */
    private boolean shouldUseLazyExecution(long loopSize, boolean hasSideEffects) {
        // If we can't determine size, be conservative
        if (loopSize < 0) {
            return false;
        }
        
        // Small loop: only lazy if no side effects
        if (loopSize < LAZY_THRESHOLD) {
            return !hasSideEffects;
        }
        
        // Large loop: always worth trying
        return true;
    }
    
    /**
     * Quick estimate of loop size (doesn't need to be perfect)
     */
    private long estimateLoopSize(For node, ExecutionContext ctx) {
        try {
            if (node.range != null) {
                // Range loop: we can calculate exactly
                Object startObj = dispatch(node.range.start);
                Object endObj = dispatch(node.range.end);
                
                startObj = typeSystem.unwrap(startObj);
                endObj = typeSystem.unwrap(endObj);
                
                AutoStackingNumber start = typeSystem.toAutoStackingNumber(startObj);
                AutoStackingNumber end = typeSystem.toAutoStackingNumber(endObj);
                
                AutoStackingNumber step;
                if (node.range.step != null) {
                    Object stepObj = dispatch(node.range.step);
                    step = typeSystem.toAutoStackingNumber(typeSystem.unwrap(stepObj));
                } else {
                    step = (start.compareTo(end) > 0) ? 
                        AutoStackingNumber.minusOne(1) : AutoStackingNumber.one(1);
                }
                
                if (step.isZero()) return 0;
                
                AutoStackingNumber diff = end.subtract(start);
                AutoStackingNumber steps = diff.divide(step);
                AutoStackingNumber size = steps.add(AutoStackingNumber.one(1));
                
                return size.longValue();
                
            } else if (node.arraySource != null) {
                // Array loop: get size from array
                Object arrayObj = dispatch(node.arraySource);
                arrayObj = typeSystem.unwrap(arrayObj);
                
                if (arrayObj instanceof NaturalArray) {
                    NaturalArray arr = (NaturalArray) arrayObj;
                    // Force materialization to get accurate size
                    if (arr.hasPendingUpdates()) {
                        arr.commitUpdates();
                    }
                    return arr.size();
                } else if (arrayObj instanceof List) {
                    return ((List<?>) arrayObj).size();
                }
            }
        } catch (Exception e) {
                DebugSystem.debug("LOOP", "Failed to estimate size: " + e.getMessage());
            
        }
        
        return -1; // Unknown size
    }
    
    /**
     * Quick side effect detection (simple version)
     */
    private boolean hasSideEffects(Block body) {
        if (body == null || body.statements == null) return false;
        
        for (Stmt stmt : body.statements) {
            if (stmt instanceof MethodCall) {
                MethodCall call = (MethodCall) stmt;
                // out(), outs(), in() are side effects
                if ("out".equals(call.name) || "outs".equals(call.name) || "in".equals(call.name)) {
                    return true;
                }
                // Any method call could have side effects
                return true;
            }
            
            // Check nested blocks
            if (stmt instanceof StmtIf) {
                StmtIf ifStmt = (StmtIf) stmt;
                if (hasSideEffects(ifStmt.thenBlock) || hasSideEffects(ifStmt.elseBlock)) {
                    return true;
                }
            }
            
            // Nested loops definitely have side effects (complex)
            if (stmt instanceof For) {
                return true;
            }
            
            // Assignments to properties could be side effects
            if (stmt instanceof Assignment) {
                Assignment assign = (Assignment) stmt;
                if (assign.left instanceof PropertyAccess) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Try optimized execution, return null if not possible
     */
    private Object tryOptimizedExecution(For node, int loopId) {
        // Try output-aware pattern first
        OutputAwarePattern.OutputPattern outputPattern = 
            OutputAwarePattern.extract(node, node.iterator);
        
        if (outputPattern.isOptimizable) {
            try {
                Object result = executeOutputAwareLoop(node, outputPattern);
                ArrayTracker.markLoopOptimized(loopId);
                return result;
            } catch (Exception e) {
                DebugSystem.debug("OPTIMIZER", "Output pattern failed: " + e.getMessage());
            }
        }

        // Try multi-array sequence chain pattern
        List<PatternResult> multiArrayPatterns = extractMultiArraySequencePatterns(node);
        if (!multiArrayPatterns.isEmpty()) {
            try {
                Object result = applyPatterns(node, multiArrayPatterns);
                ArrayTracker.markLoopOptimized(loopId);
                return result;
            } catch (Exception e) {
                DebugSystem.debug("OPTIMIZER", "Multi-array pattern failed: " + e.getMessage());
            }
        }

        // Try automatic linear recurrence pattern (generic)
        LinearRecurrencePattern recurrencePattern = extractLinearRecurrencePattern(node);
        if (recurrencePattern != null) {
            try {
                List<PatternResult> patterns = new ArrayList<PatternResult>();
                patterns.add(new PatternResult(PatternType.LINEAR_RECURRENCE, recurrencePattern, recurrencePattern.targetArray));
                Object result = applyPatterns(node, patterns);
                ArrayTracker.markLoopOptimized(loopId);
                return result;
            } catch (Exception e) {
                DebugSystem.debug("OPTIMIZER", "Linear recurrence pattern failed: " + e.getMessage());
            }
        }
        
        // Try sequence pattern
        SequencePattern.Pattern seqPattern = 
            SequencePattern.extract(node.body.statements, node.iterator);
        if (seqPattern != null && seqPattern.isOptimizable()) {
            try {
                List<PatternResult> patterns = new ArrayList<PatternResult>();
                patterns.add(new PatternResult(PatternType.SEQUENCE, seqPattern, seqPattern.targetArray));
                Object result = applyPatterns(node, patterns);
                ArrayTracker.markLoopOptimized(loopId);
                return result;
            } catch (Exception e) {
                DebugSystem.debug("OPTIMIZER", "Sequence pattern failed: " + e.getMessage());
            }
        }
        
        // Try conditional patterns
        List<PatternResult> allPatterns = new ArrayList<PatternResult>();
        for (Stmt stmt : node.body.statements) {
            if (stmt instanceof StmtIf) {
                StmtIf ifStmt = (StmtIf) stmt;
                List<ConditionalPattern> patterns = extractConditionalPatterns(ifStmt, node.iterator);
                for (ConditionalPattern pattern : patterns) {
                    if (pattern != null && pattern.isOptimizable()) {
                        allPatterns.add(new PatternResult(PatternType.CONDITIONAL, pattern, pattern.array));
                    }
                }
            }
        }
        
        if (!allPatterns.isEmpty()) {
            try {
                Object result = applyPatterns(node, allPatterns);
                ArrayTracker.markLoopOptimized(loopId);
                return result;
            } catch (Exception e) {
                DebugSystem.debug("OPTIMIZER", "Conditional pattern failed: " + e.getMessage());
            }
        }
        
        return null;
    }

    private LinearRecurrencePattern extractLinearRecurrencePattern(For node) {
        if (node == null || node.body == null || node.body.statements == null) {
            return null;
        }
        if (node.body.statements.size() != 1) {
            return null;
        }
        if (!(node.body.statements.get(0) instanceof Assignment)) {
            return null;
        }
        Assignment assign = (Assignment) node.body.statements.get(0);
        if (!(assign.left instanceof IndexAccess)) {
            return null;
        }
        IndexAccess leftAccess = (IndexAccess) assign.left;
        if (!(leftAccess.array instanceof Identifier) || !(leftAccess.index instanceof Identifier)) {
            return null;
        }
        String iter = node.iterator;
        Identifier idx = (Identifier) leftAccess.index;
        if (!iter.equals(idx.name)) {
            return null;
        }

        Object resolved = dispatch(leftAccess.array);
        resolved = typeSystem.unwrap(resolved);
        if (!(resolved instanceof NaturalArray)) {
            return null;
        }
        NaturalArray targetArray = (NaturalArray) resolved;

        Set<String> deps = new HashSet<String>();
        collectIndexedArrayRefs(assign.right, iter, deps);
        String targetName = ((Identifier) leftAccess.array).name;
        if (!deps.contains(targetName)) {
            return null;
        }
        for (String dep : deps) {
            if (!targetName.equals(dep)) {
                return null;
            }
        }

        // Index 0 is intentionally unused; coefficient for lag k is stored at coeff[k].
        AutoStackingNumber[] coeff = new AutoStackingNumber[MAX_SUPPORTED_LAG + 1];
        for (int i = 0; i < coeff.length; i++) coeff[i] = AutoStackingNumber.fromLong(0L);
        AutoStackingNumber[] constant = new AutoStackingNumber[]{AutoStackingNumber.fromLong(0L)};
        if (!collectLinearTerms(assign.right, targetName, iter, coeff, constant, AutoStackingNumber.fromLong(1L))) {
            return null;
        }

        int maxLag = 0;
        boolean hasAnyLag = false;
        for (int lag = 1; lag < coeff.length; lag++) {
            if (!coeff[lag].isZero()) {
                hasAnyLag = true;
                if (lag > maxLag) maxLag = lag;
            }
        }
        if (!hasAnyLag || maxLag <= 0) {
            return null;
        }

        int order = maxLag;
        AutoStackingNumber[] coeffByLag = new AutoStackingNumber[order];
        for (int lag = 1; lag <= order; lag++) {
            coeffByLag[lag - 1] = coeff[lag];
        }

        long[] bounds = resolveLoopBounds(node);
        if (bounds == null) {
            return null;
        }
        long min = bounds[0];
        long max = bounds[1];
        long recurrenceStart = min;
        if (recurrenceStart < order) {
            recurrenceStart = order;
        }
        if (recurrenceStart > max) {
            return null;
        }

        AutoStackingNumber[] seed = new AutoStackingNumber[order];
        long seedStart = recurrenceStart - order;
        for (int i = 0; i < order; i++) {
            long idxSeed = seedStart + i;
            Object vObj = targetArray.get(idxSeed);
            AutoStackingNumber v = typeSystem.toAutoStackingNumber(vObj);
            if (v == null) {
                return null;
            }
            seed[i] = v;
        }

        return new LinearRecurrencePattern(
            leftAccess.array,
            order,
            coeffByLag,
            constant[0],
            recurrenceStart,
            seedStart,
            seed
        );
    }

    private boolean collectLinearTerms(
        Expr expr,
        String targetArrayName,
        String iterator,
        AutoStackingNumber[] coeffByLag,
        AutoStackingNumber[] constant,
        AutoStackingNumber sign
    ) {
        if (expr == null) return false;

        if (expr instanceof BinaryOp) {
            BinaryOp bin = (BinaryOp) expr;
            if ("+".equals(bin.op)) {
                return collectLinearTerms(bin.left, targetArrayName, iterator, coeffByLag, constant, sign) &&
                       collectLinearTerms(bin.right, targetArrayName, iterator, coeffByLag, constant, sign);
            }
            if ("-".equals(bin.op)) {
                return collectLinearTerms(bin.left, targetArrayName, iterator, coeffByLag, constant, sign) &&
                       collectLinearTerms(bin.right, targetArrayName, iterator, coeffByLag, constant, sign.multiply(AutoStackingNumber.fromLong(-1L)));
            }
            if ("*".equals(bin.op)) {
                TermRef ref = extractIndexedTargetTerm(bin.left, targetArrayName, iterator);
                AutoStackingNumber scalar = toNumericLiteral(bin.right);
                if (ref == null || scalar == null) {
                    ref = extractIndexedTargetTerm(bin.right, targetArrayName, iterator);
                    scalar = toNumericLiteral(bin.left);
                }
                if (ref != null && scalar != null) {
                    AutoStackingNumber c = sign.multiply(scalar);
                    coeffByLag[ref.lag] = coeffByLag[ref.lag].add(c);
                    return true;
                }
                return false;
            }
            return false;
        }

        TermRef ref = extractIndexedTargetTerm(expr, targetArrayName, iterator);
        if (ref != null) {
            coeffByLag[ref.lag] = coeffByLag[ref.lag].add(sign);
            return true;
        }

        AutoStackingNumber literal = toNumericLiteral(expr);
        if (literal != null) {
            constant[0] = constant[0].add(sign.multiply(literal));
            return true;
        }

        return false;
    }

    private static class TermRef {
        final int lag;
        TermRef(int lag) { this.lag = lag; }
    }

    private TermRef extractIndexedTargetTerm(Expr expr, String targetArrayName, String iterator) {
        if (!(expr instanceof IndexAccess)) {
            return null;
        }
        IndexAccess access = (IndexAccess) expr;
        if (!(access.array instanceof Identifier)) {
            return null;
        }
        String arrayName = ((Identifier) access.array).name;
        if (!targetArrayName.equals(arrayName)) {
            return null;
        }
        int lag = extractLag(access.index, iterator);
        if (lag <= 0 || lag > MAX_SUPPORTED_LAG) {
            return null;
        }
        return new TermRef(lag);
    }

    private int extractLag(Expr indexExpr, String iterator) {
        if (indexExpr instanceof BinaryOp) {
            BinaryOp bin = (BinaryOp) indexExpr;
            if ("-".equals(bin.op) && bin.left instanceof Identifier &&
                iterator.equals(((Identifier) bin.left).name)) {
                AutoStackingNumber n = toNumericLiteral(bin.right);
                if (n == null) return -1;
                long lag = n.longValue();
                if (lag <= 0 || lag > Integer.MAX_VALUE) return -1;
                return (int) lag;
            }
        }
        return -1;
    }

    private AutoStackingNumber toNumericLiteral(Expr expr) {
        if (expr instanceof IntLiteral) {
            return ((IntLiteral) expr).value;
        }
        if (expr instanceof FloatLiteral) {
            return ((FloatLiteral) expr).value;
        }
        if (expr instanceof Unary) {
            Unary unary = (Unary) expr;
            if ("-".equals(unary.op)) {
                AutoStackingNumber inner = toNumericLiteral(unary.operand);
                if (inner == null) return null;
                return AutoStackingNumber.fromLong(0L).subtract(inner);
            }
            if ("+".equals(unary.op)) {
                return toNumericLiteral(unary.operand);
            }
        }
        return null;
    }

    private long[] resolveLoopBounds(For node) {
        if (node == null) return null;
        if (node.range != null) {
            Object startObj = dispatch(node.range.start);
            Object endObj = dispatch(node.range.end);
            long start = expressionHandler.toLong(startObj);
            long end = expressionHandler.toLong(endObj);
            return new long[]{Math.min(start, end), Math.max(start, end)};
        }
        if (node.arraySource != null) {
            Object sourceObj = dispatch(node.arraySource);
            sourceObj = typeSystem.unwrap(sourceObj);
            if (sourceObj instanceof NaturalArray) {
                NaturalArray sourceArr = (NaturalArray) sourceObj;
                if (sourceArr.size() > 0) {
                    return new long[]{0L, sourceArr.size() - 1L};
                }
            } else if (sourceObj instanceof List) {
                List<?> list = (List<?>) sourceObj;
                if (!list.isEmpty()) {
                    return new long[]{0L, list.size() - 1L};
                }
            }
        }
        return null;
    }

    private List<PatternResult> extractMultiArraySequencePatterns(For node) {
        List<PatternResult> results = new ArrayList<PatternResult>();
        if (node == null || node.body == null || node.body.statements == null) {
            return results;
        }

        List<Stmt> statements = node.body.statements;
        if (statements.size() < 2) {
            return results;
        }

        List<String> orderedTargets = new ArrayList<String>();
        List<Assignment> orderedAssignments = new ArrayList<Assignment>();

        for (Stmt stmt : statements) {
            if (!(stmt instanceof Assignment)) {
                return new ArrayList<PatternResult>();
            }

            Assignment assign = (Assignment) stmt;
            if (assign.isDeclaration || !(assign.left instanceof IndexAccess)) {
                return new ArrayList<PatternResult>();
            }

            IndexAccess indexAccess = (IndexAccess) assign.left;
            if (!(indexAccess.array instanceof Identifier) || !(indexAccess.index instanceof Identifier)) {
                return new ArrayList<PatternResult>();
            }

            Identifier index = (Identifier) indexAccess.index;
            if (!node.iterator.equals(index.name)) {
                return new ArrayList<PatternResult>();
            }

            String targetName = ((Identifier) indexAccess.array).name;
            if (orderedTargets.contains(targetName)) {
                return new ArrayList<PatternResult>();
            }

            orderedTargets.add(targetName);
            orderedAssignments.add(assign);
        }

        for (int i = 0; i < orderedAssignments.size(); i++) {
            Assignment assign = orderedAssignments.get(i);
            IndexAccess indexAccess = (IndexAccess) assign.left;
            Identifier targetArray = (Identifier) indexAccess.array;

            Set<String> refs = new HashSet<String>();
            collectIndexedArrayRefs(assign.right, node.iterator, refs);

            for (String ref : refs) {
                int refIndex = orderedTargets.indexOf(ref);
                if (refIndex == -1 || refIndex > i) {
                    return new ArrayList<PatternResult>();
                }
            }

            List<SequencePattern.Step> steps = new ArrayList<SequencePattern.Step>();
            steps.add(new SequencePattern.Step(null, assign.right));
            SequencePattern.Pattern pattern = new SequencePattern.Pattern(steps, targetArray, node.iterator);
            results.add(new PatternResult(PatternType.SEQUENCE, pattern, targetArray));
        }

        return results;
    }

    private void collectIndexedArrayRefs(Expr expr, String iterator, Set<String> refs) {
        if (expr == null || refs == null) {
            return;
        }

        if (expr instanceof IndexAccess) {
            IndexAccess access = (IndexAccess) expr;
            if (access.array instanceof Identifier && access.index instanceof Identifier) {
                Identifier idx = (Identifier) access.index;
                if (iterator.equals(idx.name)) {
                    refs.add(((Identifier) access.array).name);
                }
            }
            collectIndexedArrayRefs(access.array, iterator, refs);
            collectIndexedArrayRefs(access.index, iterator, refs);
            return;
        }

        if (expr instanceof BinaryOp) {
            BinaryOp bin = (BinaryOp) expr;
            collectIndexedArrayRefs(bin.left, iterator, refs);
            collectIndexedArrayRefs(bin.right, iterator, refs);
            return;
        }

        if (expr instanceof Unary) {
            collectIndexedArrayRefs(((Unary) expr).operand, iterator, refs);
            return;
        }

        if (expr instanceof MethodCall) {
            MethodCall call = (MethodCall) expr;
            if (call.arguments != null) {
                for (Expr arg : call.arguments) {
                    collectIndexedArrayRefs(arg, iterator, refs);
                }
            }
            return;
        }

        if (expr instanceof TypeCast) {
            collectIndexedArrayRefs(((TypeCast) expr).expression, iterator, refs);
            return;
        }

        if (expr instanceof PropertyAccess) {
            PropertyAccess prop = (PropertyAccess) expr;
            collectIndexedArrayRefs(prop.left, iterator, refs);
            collectIndexedArrayRefs(prop.right, iterator, refs);
            return;
        }

        if (expr instanceof Tuple) {
            Tuple tuple = (Tuple) expr;
            if (tuple.elements != null) {
                for (Expr elem : tuple.elements) {
                    collectIndexedArrayRefs(elem, iterator, refs);
                }
            }
            return;
        }

        if (expr instanceof Array) {
            Array array = (Array) expr;
            if (array.elements != null) {
                for (Expr elem : array.elements) {
                    collectIndexedArrayRefs(elem, iterator, refs);
                }
            }
        }
    }

    @Override
    public Object visit(Skip node) {
        throw new SkipIterationException();
    }

    @Override
    public Object visit(Break node) {
        throw new BreakLoopException();
    }

    @Override
    public Object visit(Range n) {
        return defaultVisit(n);
    }

    @Override
    public Object visit(Exit node) {
        throw new EarlyExitException();
    }

    @Override
    public Object visit(Tuple node) {
        if (node == null) {
            throw new InternalError("visit(Tuple) called with null node");
        }
        
        try {
            List<Object> tuple = new ArrayList<Object>();
            for (Expr elem : node.elements) {
                tuple.add(dispatch(elem));
            }
            return Collections.unmodifiableList(tuple);
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Tuple creation failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object visit(ReturnSlotAssignment node) {
        if (node == null) {
            throw new InternalError("visit(ReturnSlotAssignment) called with null node");
        }
        
        ExecutionContext ctx = getCurrentContext();
        
        Map<String, Object> allLocals = new HashMap<String, Object>();
        for (int i = 0; i < ctx.getScopeDepth(); i++) {
            Map<String, Object> scope = ctx.getScope(i);
            if (scope != null) {
                allLocals.putAll(scope);
            }
        }
        
        try {
            if (node.lambda != null) {
                return evaluateLambdaAssignment(node, ctx, allLocals);
            }
            
            Object res = interpreter.evalMethodCall(node.methodCall, ctx.objectInstance, allLocals, null);

            if (res instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) res;
                Method method = null;
                if (ctx.objectInstance != null && ctx.objectInstance.type != null) {
                    method =
                        interpreter
                            .getConstructorResolver()
                            .findMethodInHierarchy(ctx.objectInstance.type, node.methodCall.name, ctx);
                }

                for (int i = 0; i < node.variableNames.size(); i++) {
                    String slot = node.methodCall.slotNames.get(i);
                    String requestedSlot = slot;
                    
                    if (!map.containsKey(requestedSlot) && method != null && method.returnSlots != null) {
                        try {
                            int index = Integer.parseInt(requestedSlot);
                            if (index >= 0 && index < method.returnSlots.size()) {
                                requestedSlot = method.returnSlots.get(index).name;
                            }
                        } catch (NumberFormatException e) {
                            // Not an index
                        }
                    }

                    if (map.containsKey(requestedSlot)) {
                        Object value = map.get(requestedSlot);
                        
                        if (value instanceof NaturalArray) {
                            NaturalArray arr = (NaturalArray) value;
                            if (arr.hasPendingUpdates()) {
                                arr.commitUpdates();
                            }
                        }
                        
                        ctx.setVariable(node.variableNames.get(i), value);
                    } else {
                        throw new ProgramError("Missing slot: " + slot + " (tried as: " + requestedSlot + ")");
                    }
                }
            } else {
                // Handle case where method returns a single value directly
                if (node.variableNames.size() == 1) {
                    ctx.setVariable(node.variableNames.get(0), res);
                } else {
                    throw new ProgramError("Method did not return slot values");
                }
            }
            return res;
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Return slot assignment failed", e);
        }
    }

    private Object evaluateLambdaAssignment(
        ReturnSlotAssignment node,
        ExecutionContext parentCtx,
        Map<String, Object> allLocals) {
        
        Lambda lambda = node.lambda;
        if (lambda == null) {
            throw new ProgramError("Lambda assignment missing lambda expression");
        }
        
        List<Param> params = lambda.parameters != null ? lambda.parameters : new ArrayList<Param>();
        if (lambda.inferParameters && params.isEmpty()) {
            params = inferLambdaParamsFromPlaceholders(lambda);
            if (params.isEmpty()) {
                throw new ProgramError(
                    "Inferred lambda parameters require named placeholders like $item or $left in the body");
            }
        }
        
        List<Slot> lambdaSlots =
            lambda.returnSlots != null ? lambda.returnSlots : new ArrayList<Slot>();
        if (lambdaSlots.isEmpty()) {
            throw new ProgramError(
                "Lambda assignment requires a return contract (::) to map values to variables");
        }
        
        if (lambdaSlots.size() != node.variableNames.size()) {
            throw new ProgramError(
                "Number of assigned variables (" + node.variableNames.size()
                    + ") does not match lambda return slots (" + lambdaSlots.size() + ")");
        }

        Map<String, Object> initialLambdaLocals = new HashMap<String, Object>(allLocals);
        List<Object> activeParamValues = new ArrayList<Object>();
        for (Param param : params) {
            if (param == null || param.name == null) continue;
            
            Object boundValue = null;
            boolean found = false;
            
            if (initialLambdaLocals.containsKey(param.name)) {
                boundValue = initialLambdaLocals.get(param.name);
                found = true;
            } else if (param.hasDefaultValue && param.defaultValue != null) {
                ExecutionContext defaultCtx = new ExecutionContext(
                    parentCtx.objectInstance,
                    initialLambdaLocals,
                    null,
                    null,
                    typeSystem
                );
                pushContext(defaultCtx);
                try {
                    boundValue = visit((Base) param.defaultValue);
                    found = true;
                } finally {
                    popContext();
                }
            }
            
            if (!found) {
                throw new ProgramError(
                    "Missing value for lambda parameter '" + param.name + "'. "
                        + "Declare a local variable with that name or provide a default value.");
            }
            
            if (param.type != null && !typeSystem.validateType(param.type, boundValue)) {
                throw new ProgramError(
                    "Lambda parameter type mismatch for '" + param.name + "'. Expected "
                        + param.type + ", got: " + typeSystem.getConcreteType(boundValue));
            }
            
            initialLambdaLocals.put(param.name, boundValue);
            activeParamValues.add(boundValue);
        }

        while (true) {
            Map<String, Object> slotValues = new LinkedHashMap<String, Object>();
            Map<String, String> slotTypes = new LinkedHashMap<String, String>();
            for (Slot slot : lambdaSlots) {
                slotValues.put(slot.name, null);
                slotTypes.put(slot.name, slot.type);
            }

            Map<String, Object> lambdaLocals = new HashMap<String, Object>(allLocals);
            for (int i = 0; i < params.size(); i++) {
                Param param = params.get(i);
                if (param == null || param.name == null) continue;
                Object paramValue = i < activeParamValues.size() ? activeParamValues.get(i) : null;
                lambdaLocals.put(param.name, paramValue);
            }

            LambdaClosure lambdaClosure =
                new LambdaClosure(
                    lambda,
                    lambdaLocals,
                    parentCtx.objectInstance,
                    parentCtx.currentClass,
                    parentCtx.currentLambdaClosure,
                    Collections.<Object>emptyList());

            ExecutionContext lambdaCtx =
                new ExecutionContext(parentCtx.objectInstance, lambdaLocals, slotValues, slotTypes, typeSystem);
            lambdaCtx.currentClass = parentCtx.currentClass;
            lambdaCtx.currentMethodName = parentCtx.currentMethodName;
            lambdaCtx.currentLambdaClosure = lambdaClosure;

            List<Object> nextTailArgs = null;

            pushContext(lambdaCtx);
            try {
                if (lambda.body != null) {
                    visit((Base) lambda.body);
                }
            } catch (TailCallSignal tailCallSignal) {
                if (tailCallSignal.lambdaClosure != null && tailCallSignal.lambdaClosure == lambdaClosure) {
                    nextTailArgs = tailCallSignal.arguments;
                } else {
                    throw tailCallSignal;
                }
            } catch (EarlyExitException e) {
                // normal lambda early exit
            } finally {
                popContext();
            }

            if (nextTailArgs != null) {
                activeParamValues = new ArrayList<Object>(nextTailArgs);
                continue;
            }

            Object result = slotValues;
            for (int i = 0; i < node.variableNames.size(); i++) {
                String varName = node.variableNames.get(i);
                if ("_".equals(varName)) continue;

                String slotName = lambdaSlots.get(i).name;
                if (!slotValues.containsKey(slotName)) {
                    throw new ProgramError("Missing slot: " + slotName);
                }
                parentCtx.setVariable(varName, slotValues.get(slotName));
            }

            return result;
        }
    }

    @Override
    public Object visit(SlotAssignment node) {
        if (node == null) {
            throw new InternalError("visit(SlotAssignment) called with null node");
        }
        
        ExecutionContext ctx = getCurrentContext();
        TailCallSignal tailCallSignal = buildTailCallSignalForSlotAssignment(node, ctx);
        if (tailCallSignal != null) {
            throw tailCallSignal;
        }

        try {
            return assignmentHandler.handleSlotAssignment(node, ctx);
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Slot assignment failed", e);
        }
    }

    @Override
    public Object visit(SlotDeclaration n) {
        return defaultVisit(n);
    }

    @Override
    public Object visit(MultipleSlotAssignment node) {
        if (node == null) {
            throw new InternalError("visit(MultipleSlotAssignment) called with null node");
        }
        
        try {
            return assignmentHandler.handleMultipleSlotAssignment(node, getCurrentContext());
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Multiple slot assignment failed", e);
        }
    }

    private TailCallSignal buildTailCallSignalForSlotAssignment(SlotAssignment node, ExecutionContext ctx) {
        if (node == null || ctx == null || !(node.value instanceof MethodCall)) return null;
        MethodCall methodCall = (MethodCall) node.value;
        if (!methodCall.isSelfCall) return null;

        List<Object> evaluatedArgs = evaluateMethodCallArguments(methodCall);
        Integer resolvedLevel = resolveSelfCallLevelValue(methodCall, ctx);

        if (ctx.currentLambdaClosure != null) {
            if (resolvedLevel != null && resolvedLevel.intValue() != 0) {
                // Tail-call trampoline only applies to same-closure self calls.
                // Parent/grandparent calls switch closure targets, so they are not TCO-safe here.
                return null;
            }
            return TailCallSignal.forLambda(ctx.currentLambdaClosure, evaluatedArgs);
        }

        if (resolvedLevel != null) {
            // <~N(...) levels are lambda-only; method contexts are validated in method-call resolution.
            return null;
        }
        if (ctx.currentMethodName == null || ctx.currentMethodName.isEmpty()) {
            return null;
        }
        return TailCallSignal.forMethod(ctx.currentMethodName, evaluatedArgs);
    }

    private List<Object> evaluateMethodCallArguments(MethodCall methodCall) {
        List<Object> evaluatedArgs = new ArrayList<Object>();
        if (methodCall == null || methodCall.arguments == null) {
            return evaluatedArgs;
        }
        for (Expr arg : methodCall.arguments) {
            Object argValue = dispatch(arg);
            evaluatedArgs.add(typeSystem.unwrap(argValue));
        }
        return evaluatedArgs;
    }

    private boolean isUnsafeExecutionContext(ExecutionContext ctx) {
        if (ctx == null) return false;
        if (ctx.currentClass != null && ctx.currentClass.isUnsafe) {
            return true;
        }
        Method currentMethod = resolveCurrentContextMethod(ctx);
        return currentMethod != null && currentMethod.isUnsafe;
    }

    private Method resolveCurrentContextMethod(ExecutionContext ctx) {
        if (ctx == null || ctx.currentMethodName == null || ctx.currentMethodName.isEmpty()) {
            return null;
        }
        Type searchType = ctx.currentClass;
        if (searchType == null && ctx.objectInstance != null) {
            searchType = ctx.objectInstance.type;
        }
        if (searchType == null) {
            return null;
        }
        return interpreter.getConstructorResolver().findMethodInHierarchy(searchType, ctx.currentMethodName, ctx);
    }

    private Method resolveMethodForCall(MethodCall node, ExecutionContext ctx) {
        Method method = null;
        String callName = node.name;
        String callQualifiedName = node.qualifiedName;

        if (ctx.currentClass != null) {
            method = interpreter.getConstructorResolver().findMethodInHierarchy(ctx.currentClass, callName, ctx);
        }

        if (method == null && ctx.objectInstance != null && ctx.objectInstance.type != null) {
            method = interpreter.getConstructorResolver().findMethodInHierarchy(ctx.objectInstance.type, callName, ctx);
        }

        if (method == null) {
            String qName = callQualifiedName;
            if (qName != null && qName.contains(".")) {
                String[] parts = qName.split("\\.");
                if (parts.length == 2) {
                    String receiver = parts[0];
                    String methodName = parts[1];
                    if (ctx.locals().containsKey(receiver)) {
                        Object receiverObj = ctx.locals().get(receiver);
                        if (receiverObj instanceof ObjectInstance) {
                            ObjectInstance objInst = (ObjectInstance) receiverObj;
                            if (objInst.type != null) {
                                Method instanceMethod = interpreter
                                    .getConstructorResolver()
                                    .findMethodInHierarchy(objInst.type, methodName, ctx);
                                if (instanceMethod != null) {
                                    return instanceMethod;
                                }
                                qName = objInst.type.name + "." + methodName;
                            }
                        }
                    } else {
                        Method receiverTypeMethod = findMethodOnReceiverType(receiver, methodName);
                        if (receiverTypeMethod != null) {
                            return receiverTypeMethod;
                        }
                    }
                }
            }
            if (qName == null) qName = callName;
            method = interpreter.getImportResolver().findMethod(qName);
        }

        return method;
    }

    private Method findMethodOnReceiverType(String receiverTypeName, String methodName) {
        if (receiverTypeName == null || methodName == null) {
            return null;
        }

        Type receiverType = null;
        try {
            receiverType = interpreter.getImportResolver().findType(receiverTypeName);
        } catch (ProgramError ignored) {
            Program currentProgram = interpreter.getCurrentProgram();
            if (currentProgram != null
                && currentProgram.unit != null
                && currentProgram.unit.types != null) {
                for (Type localType : currentProgram.unit.types) {
                    if (localType != null && receiverTypeName.equals(localType.name)) {
                        receiverType = localType;
                        break;
                    }
                }
            }
        }

        if (receiverType == null || receiverType.methods == null) {
            if (receiverType == null
                && receiverTypeName.length() > 0
                && Character.isUpperCase(receiverTypeName.charAt(0))) {
                String lowerUnitName = receiverTypeName.toLowerCase(Locale.ENGLISH);
                try {
                    receiverType = interpreter.getImportResolver().resolveImport(
                        lowerUnitName + "." + receiverTypeName);
                } catch (Exception ignored) {
                    // Keep searching through other fallbacks.
                }
            }
        }

        if (receiverType == null || receiverType.methods == null) {
            return null;
        }
        for (Method method : receiverType.methods) {
            if (method != null && methodName.equals(method.methodName)) {
                return method;
            }
        }
        return null;
    }

    private Object executeSafeCommit(MethodCall node, ExecutionContext ctx) {
        if (isUnsafeExecutionContext(ctx)) {
            throw new ProgramError(
                "safe() is not allowed inside unsafe classes or methods; these contexts already have permission to execute unsafe code");
        }
        if (node.arguments == null || node.arguments.size() != 1) {
            throw new ProgramError("safe() expects exactly one argument");
        }

        Expr argument = node.arguments.get(0);
        boolean unsafeTarget = false;

        if (argument instanceof MethodCall) {
            Method targetMethod = resolveMethodForCall((MethodCall) argument, ctx);
            unsafeTarget = targetMethod != null && targetMethod.isUnsafe;
        } else if (argument instanceof ConstructorCall) {
            Type targetType = null;
            String className = ((ConstructorCall) argument).className;
            try {
                targetType = interpreter.getImportResolver().findType(className);
            } catch (ProgramError ignore) {
                Program currentProgram = interpreter.getCurrentProgram();
                if (currentProgram != null
                    && currentProgram.unit != null
                    && currentProgram.unit.types != null) {
                    for (Type localType : currentProgram.unit.types) {
                        if (localType != null && className.equals(localType.name)) {
                            targetType = localType;
                            break;
                        }
                    }
                }
                if (targetType == null) {
                    throw ignore;
                }
            }
            unsafeTarget = targetType != null && targetType.isUnsafe;
        }

        if (!unsafeTarget) {
            throw new ProgramError(
                "safe() requires an unsafe method call or unsafe class constructor as its argument, but the provided expression is not marked unsafe");
        }

        ExecutionContext.enterUnsafeCommitAllowance();
        try {
            return dispatch(argument);
        } finally {
            ExecutionContext.exitUnsafeCommitAllowance();
        }
    }

    @Override
    public Object visit(Identifier node) {
        if (node == null) {
            throw new InternalError("visit(Identifier) called with null node");
        }
        
        ExecutionContext ctx = getCurrentContext();
        String name = node.name;
        
        Object val = ctx.getVariable(name);
        if (val != null) {
            return val;
        }
        
        if (ctx.getSlotValues() != null && ctx.getSlotValues().containsKey(name)) {
            return ctx.getSlotValues().get(name);
        }

        if (ctx.objectInstance != null && ctx.objectInstance.type != null) {
            Object fieldValue = interpreter.getConstructorResolver()
                .getFieldFromHierarchy(ctx.objectInstance.type, name, ctx);
            if (fieldValue != null) {
                return fieldValue;
            }
        }

        Field importedField = interpreter.getImportResolver().findField(name);
        if (importedField != null) {
            if (importedField.value != null) {
                return dispatch(importedField.value);
            }
            return null;
        }
        
        throw new ProgramError("Undefined variable: " + name);
    }

    @Override
    public Object visit(IntLiteral node) {
        if (node == null) {
            throw new InternalError("visit(IntLiteral) called with null node");
        }
        return node.value;
    }

    @Override
    public Object visit(FloatLiteral node) {
        if (node == null) {
            throw new InternalError("visit(FloatLiteral) called with null node");
        }
        return node.value;
    }

@Override
public Object visit(TextLiteral node) {
    if (node == null) {
        throw new InternalError("visit(TextLiteral) called with null node");
    }
    
    String text = node.value;
    
    if (typeSystem.isTypeLiteral(text)) {
        return typeSystem.processTypeLiteral(text);
    }
    
    return text;
}

    @Override
    public Object visit(BoolLiteral node) {
        if (node == null) {
            throw new InternalError("visit(BoolLiteral) called with null node");
        }
        return node.value;
    }

    @Override
    public Object visit(NoneLiteral node) {
        return null;
    }

    @Override
    public Object visit(This node) {
        if (node == null) {
            throw new InternalError("visit(This) called with null node");
        }
        
        ExecutionContext ctx = getCurrentContext();
        
        if (ctx.objectInstance == null) {
            throw new ProgramError("Cannot use 'this' outside of an object context");
        }
        
        if (node.className != null) {
            if (ctx.objectInstance.type == null || 
                !node.className.equals(ctx.objectInstance.type.name)) {
                throw new ProgramError(
                    "Cannot access '" + node.className + ".this' in current context. " +
                    "Current object is of type: " + 
                    (ctx.objectInstance.type != null ? ctx.objectInstance.type.name : "null")
                );
            }
        }
        
        return ctx.objectInstance;
    }

    @Override
    public Object visit(Super node) {
        if (node == null) {
            throw new InternalError("visit(Super) called with null node");
        }
        
        ExecutionContext ctx = getCurrentContext();
        
        if (ctx.objectInstance == null) {
            throw new ProgramError("Cannot use 'super' outside of an object context");
        }
        
        if (ctx.objectInstance.type == null || ctx.objectInstance.type.extendName == null) {
            throw new ProgramError("Cannot use 'super' - no parent class");
        }
        
        return ctx.objectInstance;
    }
    
    @Override
    public Object visit(PropertyAccess node) {
        if (node == null) {
            throw new InternalError("visit(PropertyAccess) called with null node");
        }
        
        ExecutionContext ctx = getCurrentContext();
        
        try {
            if (node.left instanceof Identifier && node.right instanceof Identifier) {
                String leftName = ((Identifier) node.left).name;
                String rightName = ((Identifier) node.right).name;
                Field importedField = interpreter.getImportResolver().findField(leftName + "." + rightName);
                if (importedField != null) {
                    if (importedField.value != null) {
                        return dispatch(importedField.value);
                    }
                    return null;
                }
            }

            Object leftObj = dispatch(node.left);
            leftObj = typeSystem.unwrap(leftObj);
            
            if (node.right instanceof Identifier) {
                Identifier right = (Identifier) node.right;
                String propertyName = right.name;
                
                if (literalRegistry.hasProperty(leftObj, propertyName)) {
                    return literalRegistry.handleProperty(leftObj, propertyName, ctx);
                }
            }
            
            if (node.right instanceof MethodCall) {
                MethodCall literalMethod = (MethodCall) node.right;
                String methodName = literalMethod.name;
                if (literalRegistry.hasMethod(leftObj, methodName)) {
                    List<Object> evaluatedArgs = new ArrayList<Object>();
                    if (literalMethod.arguments != null) {
                        for (Expr arg : literalMethod.arguments) {
                            Object argValue = dispatch(arg);
                            evaluatedArgs.add(typeSystem.unwrap(argValue));
                        }
                    }
                    return literalRegistry.handleMethod(leftObj, methodName, evaluatedArgs, ctx);
                }
            }
            
            if (leftObj instanceof NaturalArray) {
                NaturalArray natural = (NaturalArray) leftObj;
                if (natural.hasPendingUpdates()) {
                    natural.commitUpdates();
                }
            }
            
            if (node.left instanceof Super) {
                return handleSuperPropertyAccess(node, ctx);
            }
            
            if (node.left instanceof This) {
                return handleThisPropertyAccess(node, ctx);
            }
            
            if (leftObj instanceof ObjectInstance) {
                ObjectInstance instance = (ObjectInstance) leftObj;
                
                if (node.right instanceof Identifier) {
                    Identifier right = (Identifier) node.right;
                    String fieldName = right.name;
                    
                    Object fieldValue = interpreter.getConstructorResolver()
                        .getFieldFromHierarchy(instance.type, fieldName, ctx);
                        
                    if (fieldValue == null) {
                        throw new ProgramError("Undefined field: " + fieldName);
                    }
                    
                    return fieldValue;
                }
            }
            
            throw new ProgramError("Invalid property access");
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Property access failed", e);
        }
    }

    private Object handleSuperPropertyAccess(PropertyAccess node, ExecutionContext ctx) {
        if (ctx.objectInstance == null || ctx.objectInstance.type == null) {
            throw new ProgramError("Cannot access 'super' outside of object context");
        }
        
        if (ctx.objectInstance.type.extendName == null) {
            throw new ProgramError("Cannot access 'super' - no parent class");
        }
        
        try {
            Type parentType = interpreter.getConstructorResolver()
                .findParentType(ctx.objectInstance.type, ctx);
            
            if (parentType == null) {
                throw new ProgramError("Parent class not found");
            }
            
            if (node.right instanceof Identifier) {
                Identifier right = (Identifier) node.right;
                String fieldName = right.name;
                
                Object fieldValue = interpreter.getConstructorResolver()
                    .getFieldFromHierarchy(parentType, fieldName, ctx);
                
                if (fieldValue == null) {
                    throw new ProgramError("Undefined field in parent: " + fieldName);
                }
                
                return fieldValue;
            }
            
            throw new ProgramError("Invalid super property access");
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Super property access failed", e);
        }
    }

    private Object handleThisPropertyAccess(PropertyAccess node, ExecutionContext ctx) {
        if (ctx.objectInstance == null || ctx.objectInstance.type == null) {
            throw new ProgramError("Cannot access 'this' outside of object context");
        }
        
        try {
            if (node.left instanceof This) {
                This left = (This) node.left;
                if (left.className != null && 
                    !left.className.equals(ctx.objectInstance.type.name)) {
                    throw new ProgramError(
                        "Cannot access '" + left.className + ".this' in current context. " +
                        "Current object is of type: " + ctx.objectInstance.type.name
                    );
                }
            }
            
            if (node.right instanceof Identifier) {
                Identifier right = (Identifier) node.right;
                String fieldName = right.name;
                
                Object fieldValue = interpreter.getConstructorResolver()
                    .getFieldFromHierarchy(ctx.objectInstance.type, fieldName, ctx);
                
                if (fieldValue == null) {
                    throw new ProgramError("Undefined field: " + fieldName);
                }
                
                return fieldValue;
            }
            
            throw new ProgramError("Invalid this property access");
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("This property access failed", e);
        }
    }

    @Override
    public Object visit(BinaryOp node) {
        if (node == null) {
            throw new InternalError("visit(BinaryOp) called with null node");
        }
        
        try {
            return expressionHandler.handleBinaryOp(node, getCurrentContext());
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Binary operation failed: " + node.op, e);
        }
    }

    @Override
    public Object visit(Unary node) {
        if (node == null) {
            throw new InternalError("visit(Unary) called with null node");
        }
        
        try {
            return expressionHandler.handleUnaryOp(node, getCurrentContext());
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Unary operation failed: " + node.op, e);
        }
    }

    @Override
    public Object visit(TypeCast node) {
        if (node == null) {
            throw new InternalError("visit(TypeCast) called with null node");
        }
        
        try {
            return expressionHandler.handleTypeCast(node, getCurrentContext());
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Type cast failed to " + node.targetType, e);
        }
    }

    @SuppressWarnings("unchecked")
@Override
public Object visit(MethodCall node) {
    if (node == null) {
        throw new InternalError("visit(MethodCall) called with null node");
    }
    
    try {
        // Handle super calls first
        if (node.isSuperCall) {
            return handleSuperMethodCall(node);
        }
        
        ExecutionContext ctx = getCurrentContext();
        String callName = node.name;
        String callQualifiedName = node.qualifiedName;

        if (node.isSelfCall) {
            Integer requestedLevel = resolveSelfCallLevelValue(node, ctx);
            if (requestedLevel != null) {
                LambdaClosure targetClosure = resolveSelfCallClosure(ctx, requestedLevel.intValue());
                List<Object> evaluatedArgs = evaluateMethodCallArguments(node);
                return invokeLambdaCallback(targetClosure, evaluatedArgs, ctx, SELF_CALL_LAMBDA_OWNER);
            }
            if (ctx.currentLambdaClosure != null) {
                List<Object> evaluatedArgs = evaluateMethodCallArguments(node);
                return invokeLambdaCallback(ctx.currentLambdaClosure, evaluatedArgs, ctx, SELF_CALL_LAMBDA_OWNER);
            }
            if (ctx.currentMethodName != null && !ctx.currentMethodName.isEmpty()) {
                callName = ctx.currentMethodName;
                callQualifiedName = ctx.currentMethodName;
            } else {
                throw new ProgramError(
                    "'<~(...)' can only be used inside a method or lambda body.");
            }
        }

        if (ctx != null && callQualifiedName != null && callQualifiedName.contains(".")) {
            String[] parts = callQualifiedName.split("\\.");
            if (parts.length == 2) {
                String receiverName = parts[0];
                String methodName = parts[1];
                Object receiverValue = ctx.getVariable(receiverName);
                receiverValue = typeSystem.unwrap(receiverValue);
                if (literalRegistry.hasMethod(receiverValue, methodName)) {
                    List<Object> evaluatedArgs = evaluateMethodCallArguments(node);
                    return literalRegistry.handleMethod(receiverValue, methodName, evaluatedArgs, ctx);
                }
            }
        }

        if ("safe".equals(callName) && (callQualifiedName == null || "safe".equals(callQualifiedName))) {
            return executeSafeCommit(node, ctx);
        }
        
        // ========== FIX: Evaluate arguments with special handling for ValueExpr ==========
        List<Object> evaluatedArgs = new ArrayList<Object>();
        if (node.arguments != null) {
            for (Expr arg : node.arguments) {
                Object argValue;
                if (arg instanceof ValueExpr) {
                    // ValueExpr already contains the actual value - extract it directly
                    argValue = ((ValueExpr) arg).getValue();
                    DebugSystem.debug("METHOD_CALL", "ValueExpr argument extracted: " + argValue);
                } else {
                    argValue = dispatch(arg);
                }
                evaluatedArgs.add(typeSystem.unwrap(argValue));
            }
        }
        
        // Check global functions first
        GlobalRegistry globalRegistry = interpreter.getGlobalRegistry();
        if (globalRegistry != null && globalRegistry.isGlobal(callName)) {
            DebugSystem.debug("GLOBAL", "Executing global function: " + callName + 
                              " with args: " + evaluatedArgs);
            return globalRegistry.executeGlobal(callName, evaluatedArgs);
        }
        
        // Try to find method in current class hierarchy
        Method method = null;
        ObjectInstance invocationInstance = ctx.objectInstance;
        if (ctx.currentClass != null) {
            method = interpreter
                .getConstructorResolver()
                .findMethodInHierarchy(ctx.currentClass, callName, ctx);
        }

        // If not found, try from object instance
        if (method == null && ctx.objectInstance != null && ctx.objectInstance.type != null) {
            method = interpreter
                .getConstructorResolver()
                .findMethodInHierarchy(ctx.objectInstance.type, callName, ctx);
        }

        // If still not found, try imported methods
        if (method == null) {
            String qName = callQualifiedName;
            if (qName != null && qName.contains(".")) {
                String[] parts = qName.split("\\.");
                if (parts.length == 2) {
                    String receiver = parts[0];
                    String methodName = parts[1];
                    if (ctx.locals().containsKey(receiver)) {
                        Object receiverObj = ctx.locals().get(receiver);
                        if (receiverObj instanceof ObjectInstance) {
                            ObjectInstance objInst = (ObjectInstance) receiverObj;
                            if (objInst.type != null) {
                                Method instanceMethod = interpreter
                                    .getConstructorResolver()
                                    .findMethodInHierarchy(objInst.type, methodName, ctx);
                                if (instanceMethod != null) {
                                    method = instanceMethod;
                                    invocationInstance = objInst;
                                }
                                qName = objInst.type.name + "." + methodName;
                            }
                        }
                    } else {
                        Method receiverTypeMethod = findMethodOnReceiverType(receiver, methodName);
                        if (receiverTypeMethod != null) {
                            method = receiverTypeMethod;
                        }
                    }
                }
            }
            if (method == null) {
                if (qName == null) qName = callName;
                method = interpreter.getImportResolver().findMethod(qName);
            }
        }

        // If method not found after all attempts, throw error
        if (method == null) {
            throw new ProgramError("Method not found: " + callName);
        }

        if (method.isUnsafe && !isUnsafeExecutionContext(ctx) && !ExecutionContext.isUnsafeCommitAllowed()) {
            throw new ProgramError(
                "Unsafe method '" + method.methodName + "' cannot be called in a safe context. Use safe("
                    + callName
                    + "(...)).");
        }

        // Check if this is a single-slot call
        boolean hasSingleSlot = method.returnSlots != null && method.returnSlots.size() == 1;
        if (node.slotNames.isEmpty() && hasSingleSlot) {
            node.isSingleSlotCall = true;
            node.slotNames.add(method.returnSlots.get(0).name);
        }

        // Handle builtin methods
        if (method.isBuiltin) {
            MethodCall evaluatedCall = new MethodCall();
            evaluatedCall.name = callName;
            evaluatedCall.arguments = new ArrayList<Expr>();
            for (Object val : evaluatedArgs) {
                evaluatedCall.arguments.add(new ValueExpr(val));
            }
            evaluatedCall.slotNames = node.slotNames;
            evaluatedCall.qualifiedName = callQualifiedName;
            evaluatedCall.target = node.target;
            evaluatedCall.isSuperCall = node.isSuperCall;
            evaluatedCall.isSingleSlotCall = node.isSingleSlotCall;
            evaluatedCall.isSelfCall = node.isSelfCall;
            evaluatedCall.selfCallLevel = node.selfCallLevel;
            evaluatedCall.selfCallLevelConstantName = node.selfCallLevelConstantName;
            
            return interpreter.handleBuiltinMethod(method, evaluatedCall);
        }

        boolean calledMethodHasSlots = method.returnSlots != null && !method.returnSlots.isEmpty();
        List<Object> activeMethodArgs = new ArrayList<Object>(evaluatedArgs);

        while (true) {
            // Prepare method locals with parameter values
            Map<String, Object> methodLocals = new HashMap<String, Object>();
            Map<String, String> methodLocalTypes = new HashMap<String, String>();

            int argCount = activeMethodArgs.size();
            int paramCount = method.parameters != null ? method.parameters.size() : 0;

            for (int i = 0; i < paramCount; i++) {
                Param param = method.parameters.get(i);
                Object argValue = null;

                if (i < argCount) {
                    argValue = activeMethodArgs.get(i);
                } else {
                    if (param.hasDefaultValue) {
                        ExecutionContext defaultCtx = new ExecutionContext(
                            invocationInstance,
                            new HashMap<String, Object>(),
                            null,
                            null,
                            typeSystem
                        );
                        defaultCtx.currentMethodName = callName;
                        pushContext(defaultCtx);
                        try {
                            argValue = dispatch(param.defaultValue);
                        } finally {
                            popContext();
                        }
                    } else {
                        throw new ProgramError(
                            "Missing argument for parameter '" + param.name
                                + "'. Expected " + paramCount + " arguments, got " + argCount);
                    }
                }

                String paramType = param.type;

                if (!typeSystem.validateType(paramType, argValue)) {
                    if (paramType.equals(TEXT.toString())) {
                        argValue = typeSystem.convertType(argValue, paramType);
                    } else {
                        throw new ProgramError(
                            "Argument type mismatch for parameter " + param.name
                                + ". Expected " + paramType + ", got: "
                                + typeSystem.getConcreteType(argValue));
                    }
                }

                if (paramType != null && paramType.indexOf('|') >= 0) {
                    String activeType = typeSystem.getConcreteType(typeSystem.unwrap(argValue));
                    argValue = new TypeHandler.Value(argValue, activeType, paramType);
                }

                methodLocals.put(param.name, argValue);
                methodLocalTypes.put(param.name, paramType);
            }

            if (argCount > paramCount) {
                throw new ProgramError(
                    "Too many arguments: expected " + paramCount + ", got " + argCount);
            }

            // Setup slot values for method return
            Map<String, Object> slotValues = new LinkedHashMap<String, Object>();
            Map<String, String> slotTypes = new LinkedHashMap<String, String>();
            if (method.returnSlots != null) {
                for (Slot s : method.returnSlots) {
                    slotValues.put(s.name, null);
                    slotTypes.put(s.name, s.type);
                }
            }

            // Create method execution context
            ExecutionContext methodCtx = new ExecutionContext(
                invocationInstance,
                methodLocals,
                slotValues,
                slotTypes,
                typeSystem
            );

            for (Map.Entry<String, String> entry : methodLocalTypes.entrySet()) {
                methodCtx.setVariableType(entry.getKey(), entry.getValue());
            }

            methodCtx.objectInstance = invocationInstance;

            if (method.associatedClass != null) {
                Type classType = findTypeByName(method.associatedClass);
                if (classType != null) {
                    methodCtx.currentClass = classType;
                }
            }

            if (invocationInstance != null && invocationInstance.type != null
                && methodCtx.currentClass == null) {
                Type classType = findTypeByName(invocationInstance.type.name);
                if (classType != null) {
                    methodCtx.currentClass = classType;
                }
            }
            methodCtx.currentMethodName = callName;
            methodCtx.currentLambdaClosure = null;

            // Execute method body
            pushContext(methodCtx);
            Object methodResult = null;
            List<Object> nextTailArgs = null;

            try {
                if (method.body != null) {
                    for (Stmt stmt : method.body) {
                        visit(stmt);

                        if (calledMethodHasSlots
                            && interpreter.shouldReturnEarly(slotValues, methodCtx.slotsInCurrentPath)) {
                            break;
                        }
                    }
                }
            } catch (TailCallSignal tailCallSignal) {
                if (tailCallSignal.methodName != null && tailCallSignal.methodName.equals(callName)) {
                    nextTailArgs = tailCallSignal.arguments;
                } else {
                    throw tailCallSignal;
                }
            } catch (EarlyExitException e) {
                // Normal exit - method completed
            } catch (ProgramError e) {
                throw e;
            } catch (Exception e) {
                throw new InternalError("Method call execution failed: " + callName, e);
            } finally {
                popContext();
            }

            if (nextTailArgs != null) {
                activeMethodArgs = nextTailArgs;
                continue;
            }

            // Handle return value based on call type
            if (node.slotNames != null && !node.slotNames.isEmpty()) {
                if (!(methodResult instanceof Map) && calledMethodHasSlots) {
                    methodResult = slotValues;
                }

                if (methodResult instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) methodResult;
                    String requestedSlot = node.slotNames.get(0);

                    if (!map.containsKey(requestedSlot) && method != null && method.returnSlots != null) {
                        try {
                            int index = Integer.parseInt(requestedSlot);
                            if (index >= 0 && index < method.returnSlots.size()) {
                                requestedSlot = method.returnSlots.get(index).name;
                            }
                        } catch (NumberFormatException e) {
                            // Not an index, keep original slot name
                        }
                    }

                    if (map.containsKey(requestedSlot)) {
                        return map.get(requestedSlot);
                    }
                    throw new ProgramError("Undefined method slot: " + requestedSlot);
                } else if (calledMethodHasSlots) {
                    return slotValues;
                }
            }

            // Default: return whatever the method produced
            return methodResult != null ? methodResult : slotValues;
        }
        
    } catch (ProgramError e) {
        throw e;
    } catch (Exception e) {
        throw new InternalError("Method call failed: " + node.name, e);
    }
}

    private LambdaClosure resolveSelfCallClosure(ExecutionContext ctx, int level) {
        // Parser-level checks reject negative literals, but runtime validation is still required
        // for non-parser entry paths (deserialized/constructed ASTs).
        if (level < 0) {
            throw new ProgramError("Self-call level cannot be negative: " + level);
        }
        if (ctx.currentLambdaClosure == null) {
            throw new ProgramError(
                "'<~" + level + "(...)' is only valid inside lambda bodies.");
        }

        LambdaClosure closure = ctx.currentLambdaClosure;
        for (int i = 0; i < level; i++) {
            closure = closure.parentClosure;
            if (closure == null) {
                throw new ProgramError(
                    "Lambda self-call level '<~" + level + "(...)' is out of range for current nesting.");
            }
        }
        return closure;
    }

    private Integer resolveSelfCallLevelValue(MethodCall node, ExecutionContext ctx) {
        if (node == null) return null;
        if (node.selfCallLevel != null) return node.selfCallLevel;
        if (node.selfCallLevelConstantName == null) return null;

        String constantName = node.selfCallLevelConstantName;
        Object levelValue = ctx != null ? ctx.getVariable(constantName) : null;

        if (levelValue == null && ctx != null && ctx.getSlotValues() != null
            && ctx.getSlotValues().containsKey(constantName)) {
            levelValue = ctx.getSlotValues().get(constantName);
        }

        if (levelValue == null && ctx != null && ctx.objectInstance != null && ctx.objectInstance.type != null) {
            levelValue =
                interpreter
                    .getConstructorResolver()
                    .getFieldFromHierarchy(ctx.objectInstance.type, constantName, ctx);
        }

        if (levelValue == null) {
            throw new ProgramError("Undefined self-call level constant: " + constantName);
        }

        Object unwrapped = typeSystem.unwrap(levelValue);
        long levelLong;
        try {
            if (unwrapped instanceof AutoStackingNumber) {
                // Level constants are expected to be small integers in normal usage.
                // longValue() also enforces integer-only semantics (fails on fractional values).
                levelLong = ((AutoStackingNumber) unwrapped).longValue();
            } else if (unwrapped instanceof Number) {
                if (unwrapped instanceof Double || unwrapped instanceof Float) {
                    double numeric = ((Number) unwrapped).doubleValue();
                    double fractional = Math.abs(numeric % 1.0d);
                    if (fractional > SELF_CALL_LEVEL_FLOAT_EPSILON
                        && Math.abs(fractional - 1.0d) > SELF_CALL_LEVEL_FLOAT_EPSILON) {
                        throw new ProgramError(
                            "Self-call level constant '" + constantName + "' must be an integer value");
                    }
                }
                levelLong = ((Number) unwrapped).longValue();
            } else {
                throw new ProgramError(
                    "Self-call level constant '" + constantName + "' must be int, got: "
                        + typeSystem.getConcreteType(unwrapped));
            }
        } catch (ArithmeticException e) {
            throw new ProgramError(
                "Self-call level constant '" + constantName + "' must be an integer value");
        }

        if (levelLong < 0) {
            throw new ProgramError(
                "Self-call level constant '" + constantName + "' cannot be negative: " + levelLong);
        }

        if (levelLong > Integer.MAX_VALUE) {
            throw new ProgramError(
                "Self-call level constant '" + constantName + "' is out of supported range: " + levelLong);
        }

        return Integer.valueOf((int) levelLong);
    }

    private Type findTypeByName(String className) {
        Program currentProgram = interpreter.getCurrentProgram();
        if (currentProgram != null && currentProgram.unit != null && currentProgram.unit.types != null) {
            for (Type t : currentProgram.unit.types) {
                if (t.name.equals(className)) {
                    return t;
                }
            }
        }
        return null;
    }

    @Override
    public Object visit(Array node) {
        if (node == null) {
            throw new InternalError("visit(Array) called with null node");
        }
        
        try {
            if (node.elements.size() == 1) {
                Expr onlyElement = node.elements.get(0);
                if (onlyElement instanceof Range) {
                    Range range = (Range) onlyElement;
                    
                    // Just create the array - type checking happens in Var
                    return new NaturalArray(range, this, getCurrentContext());
                }
            }
            
            if (node.elements.size() > 1 && allElementsAreRanges(node.elements)) {
                return buildDimensionArray(node.elements, 0);
            }

            // Regular array literal handling
            List<Object> result = new ArrayList<Object>();
            for (Expr element : node.elements) {
                if (element instanceof Range) {
                    result.add(new NaturalArray((Range) element, this, getCurrentContext()));
                } else {
                    Object evaluated = dispatch(element);
                    
                    if (evaluated instanceof NaturalArray) {
                        NaturalArray arr = (NaturalArray) evaluated;
                        if (arr.hasPendingUpdates()) {
                            arr.commitUpdates();
                        }
                    }
                    
                    if (evaluated instanceof String && typeSystem.isTypeLiteral((String) evaluated)) {
                        evaluated = TypeHandler.Value.createTypeValue((String) evaluated);
                    } else if (evaluated instanceof TextLiteral) {
                        String str = ((TextLiteral) evaluated).value;
                        if (typeSystem.isTypeLiteral(str)) {
                            evaluated = TypeHandler.Value.createTypeValue(str);
                        }
                    }
                    
                    result.add(evaluated);
                }
            }
            return result;
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Array creation failed", e);
        }
    }
    
    private boolean allElementsAreRanges(List<Expr> elements) {
        if (elements == null || elements.isEmpty()) return false;
        for (Expr element : elements) {
            if (!(element instanceof Range)) {
                return false;
            }
        }
        return true;
    }
    
    private Object buildDimensionArray(List<Expr> ranges, int dimension) {
        Range currentRange = (Range) ranges.get(dimension);
        NaturalArray currentNatural = new NaturalArray(currentRange, this, getCurrentContext());
        if (dimension == ranges.size() - 1) {
            return currentNatural;
        }
        
        long length = currentNatural.size();
        if (length > Integer.MAX_VALUE) {
            throw new ProgramError("Dimension size too large for nested ND array literal: " + length + " (max " + Integer.MAX_VALUE + ")");
        }
        
        List<Object> result = new ArrayList<Object>((int) length);
        for (int i = 0; i < (int) length; i++) {
            result.add(buildDimensionArray(ranges, dimension + 1));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object visit(IndexAccess node) {
        return arrayOperationHandler.visitIndexAccess(node);
    }

    @Override
    public Object visit(RangeIndex node) {
        return arrayOperationHandler.visitRangeIndex(node);
    }

    @Override
    public Object visit(MultiRangeIndex node) {
        return arrayOperationHandler.visitMultiRangeIndex(node);
    }

    @Override
    public Object visit(EqualityChain node) {
        if (node == null) {
            throw new InternalError("visit(EqualityChain) called with null node");
        }
        
        try {
            return expressionHandler.handleEqualityChain(node, getCurrentContext());
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Equality chain evaluation failed", e);
        }
    }
    
@Override
public Object visit(ChainedComparison node) {
    if (node == null) {
        throw new InternalError("visit(ChainedComparison) called with null node");
    }
    
    try {
        return expressionHandler.handleChainedComparison(node, getCurrentContext());
    } catch (ProgramError e) {
        throw e;
    } catch (Exception e) {
        throw new InternalError("Chained comparison execution failed", e);
    }
}

    @Override
    public Object visit(BooleanChain node) {
        if (node == null) {
            throw new InternalError("visit(BooleanChain) called with null node");
        }
        
        try {
            return expressionHandler.handleBooleanChain(node, getCurrentContext());
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Boolean chain evaluation failed", e);
        }
    }

    @Override
    public Object visit(Slot n) {
        return defaultVisit(n);
    }

    @Override
    public Object visit(Lambda node) {
        return lambdaInvokingHandler.createLambdaClosure(node, getCurrentContext());
    }
    
    private Object invokeLambdaCallback(
        Object callbackObj,
        List<Object> args,
        ExecutionContext parentCtx,
        String ownerMethod) {
        return lambdaInvokingHandler.invokeLambdaCallback(callbackObj, args, parentCtx, ownerMethod);
    }

    private List<Param> resolveLambdaParameters(Lambda lambda) {
        if (lambda == null) {
            return new ArrayList<Param>();
        }
        List<Param> params =
            lambda.parameters != null ? lambda.parameters : new ArrayList<Param>();
        if (!params.isEmpty()) {
            return params;
        }
        if (!lambda.inferParameters) {
            return params;
        }

        List<Param> inferred = inferLambdaParamsFromPlaceholders(lambda);
        return inferred;
    }

    private List<Object> mergeBoundAndIncomingLambdaArgs(List<Object> boundArgs, List<Object> incomingArgs) {
        if ((boundArgs == null || boundArgs.isEmpty()) && (incomingArgs == null || incomingArgs.isEmpty())) {
            return Collections.<Object>emptyList();
        }
        List<Object> combined = new ArrayList<Object>();
        if (boundArgs != null && !boundArgs.isEmpty()) {
            combined.addAll(boundArgs);
        }
        if (incomingArgs != null && !incomingArgs.isEmpty()) {
            combined.addAll(incomingArgs);
        }
        return combined;
    }

    private boolean shouldAutoCurry(List<Param> params, List<Object> values) {
        if (params == null || params.isEmpty()) return false;
        int requiredCount = 0;
        for (Param param : params) {
            if (param == null) continue;
            if (!param.hasDefaultValue) {
                requiredCount++;
            }
        }
        return values.size() < requiredCount;
    }

    private LambdaClosure createCurriedLambdaClosure(
        LambdaClosure closure,
        List<Object> boundArgs) {

        return new LambdaClosure(
            closure.lambda,
            closure.capturedLocals,
            closure.objectInstance,
            closure.currentClass,
            closure.parentClosure,
            boundArgs);
    }

    private Map<String, Object> bindLambdaArguments(
        List<Param> params,
        List<Object> values,
        LambdaClosure closure,
        String ownerMethod) {

        Map<String, Object> lambdaLocals = new HashMap<String, Object>(closure.capturedLocals);
        for (int i = 0; i < params.size(); i++) {
            Param param = params.get(i);
            if (param == null || param.name == null) continue;

            Object boundValue = resolveLambdaArgumentValue(i, param, values, closure, lambdaLocals, ownerMethod);
            validateLambdaArgumentType(param, boundValue);
            lambdaLocals.put(param.name, boundValue);
        }
        return lambdaLocals;
    }

    private Object resolveLambdaArgumentValue(
        int index,
        Param param,
        List<Object> values,
        LambdaClosure closure,
        Map<String, Object> lambdaLocals,
        String ownerMethod) {

        if (index < values.size()) {
            return values.get(index);
        }
        if (param.hasDefaultValue && param.defaultValue != null) {
            return evaluateLambdaDefaultValue(param, closure, lambdaLocals);
        }
        throw new ProgramError(
            "Missing value for lambda parameter '" + param.name + "' in " + ownerMethod + " callback");
    }

    private Object evaluateLambdaDefaultValue(
        Param param,
        LambdaClosure closure,
        Map<String, Object> lambdaLocals) {

        ExecutionContext defaultCtx =
            new ExecutionContext(closure.objectInstance, lambdaLocals, null, null, typeSystem);
        defaultCtx.currentClass = closure.currentClass;
        defaultCtx.currentLambdaClosure = closure;
        pushContext(defaultCtx);
        try {
            return visit((Base) param.defaultValue);
        } finally {
            popContext();
        }
    }

    private void validateLambdaArgumentType(Param param, Object boundValue) {
        if (param.type != null && !typeSystem.validateType(param.type, boundValue)) {
            throw new ProgramError(
                "Lambda parameter type mismatch for '" + param.name + "'. Expected "
                    + param.type + ", got: " + typeSystem.getConcreteType(boundValue));
        }
    }

    private Object evaluateLambdaExpressionBody(
        Lambda lambda,
        LambdaClosure closure,
        Map<String, Object> lambdaLocals) {

        ExecutionContext exprCtx =
            new ExecutionContext(closure.objectInstance, lambdaLocals, null, null, typeSystem);
        exprCtx.currentClass = closure.currentClass;
        exprCtx.currentLambdaClosure = closure;
        pushContext(exprCtx);
        try {
            return dispatch(lambda.expressionBody);
        } finally {
            popContext();
        }
    }

    private void bindPositionalInferredPlaceholderAliases(
        Map<String, Object> lambdaLocals,
        List<Object> values) {

        if (values == null || values.isEmpty()) return;
        Object first = values.get(0);
        putIfAbsent(lambdaLocals, "$item", first);
        putIfAbsent(lambdaLocals, "$left", first);
        putIfAbsent(lambdaLocals, "$acc", first);
        putIfAbsent(lambdaLocals, "$value", first);

        if (values.size() > 1) {
            Object second = values.get(1);
            putIfAbsent(lambdaLocals, "$index", second);
            putIfAbsent(lambdaLocals, "$right", second);
            putIfAbsent(lambdaLocals, "$next", second);
        }
        if (values.size() > 2) {
            Object third = values.get(2);
            putIfAbsent(lambdaLocals, "$index", third);
            putIfAbsent(lambdaLocals, "$position", third);
        }
    }

    private void putIfAbsent(Map<String, Object> lambdaLocals, String name, Object value) {
        if (!lambdaLocals.containsKey(name)) {
            lambdaLocals.put(name, value);
        }
    }

    private Object evaluateLambdaBlockBody(
        Lambda lambda,
        LambdaClosure closure,
        Map<String, Object> lambdaLocals) {

        List<Slot> lambdaSlots =
            lambda.returnSlots != null ? lambda.returnSlots : new ArrayList<Slot>();
        if (lambdaSlots.isEmpty()) {
            throw new ProgramError(
                "Lambda with explicit body requires a return contract (::). "
                    + "Use expression body syntax for implicit return values.");
        }

        Map<String, Object> slotValues = new LinkedHashMap<String, Object>();
        Map<String, String> slotTypes = new LinkedHashMap<String, String>();
        for (Slot slot : lambdaSlots) {
            slotValues.put(slot.name, null);
            slotTypes.put(slot.name, slot.type);
        }

        ExecutionContext lambdaCtx =
            new ExecutionContext(closure.objectInstance, lambdaLocals, slotValues, slotTypes, typeSystem);
        lambdaCtx.currentClass = closure.currentClass;
        lambdaCtx.currentLambdaClosure = closure;
        pushContext(lambdaCtx);
        try {
            if (lambda.body != null) {
                visit((Base) lambda.body);
            }
        } catch (EarlyExitException e) {
            // normal lambda early exit
        } finally {
            popContext();
        }

        if (lambdaSlots.size() == 1) {
            return slotValues.get(lambdaSlots.get(0).name);
        }
        return slotValues;
    }
    
    private List<Param> inferLambdaParamsFromPlaceholders(Lambda lambda) {
        if (lambda == null) {
            return new ArrayList<Param>();
        }
        LinkedHashSet<String> names = new LinkedHashSet<String>();
        
        if (lambda.expressionBody != null) {
            collectPlaceholderNames(lambda.expressionBody, names);
        } else if (lambda.body != null) {
            collectPlaceholderNames(lambda.body, names);
        }
        
        List<Param> params = new ArrayList<Param>();
        for (String name : names) {
            Param param = new Param();
            param.name = name;
            param.type = null;
            param.typeInferred = true;
            param.isLambdaParameter = true;
            params.add(param);
        }
        return params;
    }
    
    private void collectPlaceholderNames(Base node, LinkedHashSet<String> names) {
        if (node == null) return;
        
        if (node instanceof Identifier) {
            String name = ((Identifier) node).name;
            if (name != null && name.startsWith("$") && name.length() > 1) {
                names.add(name);
            }
            return;
        }
        
        if (node instanceof BinaryOp) {
            BinaryOp n = (BinaryOp) node;
            collectPlaceholderNames(n.left, names);
            collectPlaceholderNames(n.right, names);
            return;
        }
        if (node instanceof Unary) {
            collectPlaceholderNames(((Unary) node).operand, names);
            return;
        }
        if (node instanceof TypeCast) {
            collectPlaceholderNames(((TypeCast) node).expression, names);
            return;
        }
        if (node instanceof MethodCall) {
            MethodCall n = (MethodCall) node;
            if (n.arguments != null) {
                for (Expr arg : n.arguments) {
                    collectPlaceholderNames(arg, names);
                }
            }
            if (n.target != null) {
                collectPlaceholderNames(n.target, names);
            }
            return;
        }
        if (node instanceof PropertyAccess) {
            PropertyAccess n = (PropertyAccess) node;
            collectPlaceholderNames(n.left, names);
            collectPlaceholderNames(n.right, names);
            return;
        }
        if (node instanceof IndexAccess) {
            IndexAccess n = (IndexAccess) node;
            collectPlaceholderNames(n.array, names);
            collectPlaceholderNames(n.index, names);
            return;
        }
        if (node instanceof Array) {
            Array n = (Array) node;
            if (n.elements != null) {
                for (Expr elem : n.elements) {
                    collectPlaceholderNames(elem, names);
                }
            }
            return;
        }
        if (node instanceof Tuple) {
            Tuple n = (Tuple) node;
            if (n.elements != null) {
                for (Expr elem : n.elements) {
                    collectPlaceholderNames(elem, names);
                }
            }
            return;
        }
        if (node instanceof ExprIf) {
            ExprIf n = (ExprIf) node;
            collectPlaceholderNames(n.condition, names);
            collectPlaceholderNames(n.thenExpr, names);
            collectPlaceholderNames(n.elseExpr, names);
            return;
        }
        if (node instanceof BooleanChain) {
            BooleanChain n = (BooleanChain) node;
            if (n.expressions != null) {
                for (Expr expr : n.expressions) {
                    collectPlaceholderNames(expr, names);
                }
            }
            return;
        }
        if (node instanceof EqualityChain) {
            EqualityChain n = (EqualityChain) node;
            collectPlaceholderNames(n.left, names);
            if (n.chainArguments != null) {
                for (Expr expr : n.chainArguments) {
                    collectPlaceholderNames(expr, names);
                }
            }
            return;
        }
        if (node instanceof ChainedComparison) {
            ChainedComparison n = (ChainedComparison) node;
            if (n.expressions != null) {
                for (Expr expr : n.expressions) {
                    collectPlaceholderNames(expr, names);
                }
            }
            return;
        }
        if (node instanceof ValueExpr) {
            Object value = ((ValueExpr) node).getValue();
            if (value instanceof Base) {
                collectPlaceholderNames((Base) value, names);
            }
            return;
        }
        if (node instanceof Lambda) {
            // Nested lambdas infer their own placeholders independently.
            return;
        }
        
        if (node instanceof Block) {
            Block n = (Block) node;
            if (n.statements != null) {
                for (Stmt stmt : n.statements) {
                    collectPlaceholderNames(stmt, names);
                }
            }
            return;
        }
        if (node instanceof SlotAssignment) {
            collectPlaceholderNames(((SlotAssignment) node).value, names);
            return;
        }
        if (node instanceof MultipleSlotAssignment) {
            MultipleSlotAssignment n = (MultipleSlotAssignment) node;
            if (n.assignments != null) {
                for (SlotAssignment asg : n.assignments) {
                    collectPlaceholderNames(asg, names);
                }
            }
            return;
        }
        if (node instanceof Assignment) {
            Assignment n = (Assignment) node;
            collectPlaceholderNames(n.left, names);
            collectPlaceholderNames(n.right, names);
            return;
        }
        if (node instanceof Var) {
            collectPlaceholderNames(((Var) node).value, names);
            return;
        }
        if (node instanceof ReturnSlotAssignment) {
            ReturnSlotAssignment n = (ReturnSlotAssignment) node;
            collectPlaceholderNames(n.methodCall, names);
            collectPlaceholderNames(n.lambda, names);
            return;
        }
    }

    @SuppressWarnings("unchecked")
    private Object handleSuperMethodCall(MethodCall node) {
        ExecutionContext ctx = getCurrentContext();
        
        if (ctx.objectInstance == null || ctx.objectInstance.type == null) {
            throw new ProgramError("Cannot call 'super." + node.name + "' outside of object context");
        }
        
        if (ctx.objectInstance.type.extendName == null) {
            throw new ProgramError("Cannot call 'super." + node.name + "' - no parent class");
        }
        
        try {
            ConstructorResolver resolver = interpreter.getConstructorResolver();
            Type parentType = resolver.findParentType(ctx.objectInstance.type, ctx);
            
            if (parentType == null) {
                throw new ProgramError("Parent class not found for 'super." + node.name + "'");
            }
            
            Method method = resolver.findMethodInHierarchy(parentType, node.name, ctx);
            
            if (method == null) {
                throw new ProgramError("Method '" + node.name + "' not found in parent class");
            }
            
            if (method.isBuiltin) {
                return interpreter.handleBuiltinMethod(method, node);
            }
            
            Object result = interpreter.evalMethodCall(node, ctx.objectInstance, ctx.locals(), method);
            
            if (node.slotNames != null && !node.slotNames.isEmpty()) {
                if (!(result instanceof Map)) {
                    throw new ProgramError("Method did not return slots.");
                }

                Map<String, Object> map = (Map<String, Object>) result;
                String requestedSlot = node.slotNames.get(0);

                if (!map.containsKey(requestedSlot) && method != null && method.returnSlots != null) {
                    try {
                        int index = Integer.parseInt(requestedSlot);
                        if (index >= 0 && index < method.returnSlots.size()) {
                            requestedSlot = method.returnSlots.get(index).name;
                        }
                    } catch (NumberFormatException e) {
                        // Not an index
                    }
                }

                if (map.containsKey(requestedSlot)) {
                    return map.get(requestedSlot);
                }
                throw new ProgramError("Undefined method slot: " + requestedSlot);
            }

            return result;
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Super method call failed: " + node.name, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Object applyRangeIndex(Object array, Object range) {
        if (array instanceof NaturalArray) {
            NaturalArray natural = (NaturalArray) array;
            return natural.getRange(range);
        } else if (array instanceof List) {
            List<Object> list = (List<Object>) array;
            return getListRange(list, range);
        }
        throw new ProgramError("Cannot apply range index to " + 
            (array != null ? array.getClass().getSimpleName() : "null"));
    }

    @SuppressWarnings("unchecked")
    private Object applyMultiRangeIndex(Object array, Object multiRange) {
        if (array instanceof NaturalArray) {
            NaturalArray natural = (NaturalArray) array;
            return natural.getMultiRange(multiRange);
        } else if (array instanceof List) {
            List<Object> list = (List<Object>) array;
            return getListMultiRange(list, multiRange);
        }
        throw new ProgramError("Cannot apply multi-range index to " + 
            (array != null ? array.getClass().getSimpleName() : "null"));
    }

    @SuppressWarnings("unchecked")
    private Object applyTupleIndices(Object array, List<?> indices) {
        Object current = array;
        for (Object rawIndex : indices) {
            Object indexObj = typeSystem.unwrap(rawIndex);
            if (RangeObjects.isRangeSpec(indexObj)) {
                current = applyRangeIndex(current, indexObj);
                continue;
            }
            if (RangeObjects.isMultiRangeSpec(indexObj)) {
                current = applyMultiRangeIndex(current, indexObj);
                continue;
            }
            if (current instanceof NaturalArray) {
                NaturalArray natural = (NaturalArray) current;
                long idx = expressionHandler.toLongIndex(indexObj);
                current = natural.needsConversion() ? natural.get(idx, true) : natural.get(idx);
                continue;
            }
            if (current instanceof List) {
                List<Object> list = (List<Object>) current;
                int idx = expressionHandler.toIntIndex(indexObj);
                if (idx < 0 || idx >= list.size()) {
                    throw new ProgramError("Index out of bounds: " + idx + " for array of size " + list.size());
                }
                current = list.get(idx);
                continue;
            }
            throw new ProgramError("Invalid array access during multidimensional indexing: expected NaturalArray or List, got "
                + (current != null ? current.getClass().getSimpleName() : "null"));
        }
        return current;
    }

    private List<Object> getListRange(List<Object> list, Object range) {
        try {
            long start, end;
            
            start = expressionHandler.toLongIndex(RangeObjects.getStart(range));
            if (start < 0) start = list.size() + start;
            
            end = expressionHandler.toLongIndex(RangeObjects.getEnd(range));
            if (end < 0) end = list.size() + end;
            
            long step = expressionHandler.calculateStep(range);
            
            List<Object> result = new ArrayList<Object>();
            if (step > 0) {
                for (long i = start; i <= end && i < list.size(); i += step) {
                    result.add(list.get((int) i));
                }
            } else if (step < 0) {
                for (long i = start; i >= end && i >= 0; i += step) {
                    result.add(list.get((int) i));
                }
            } else {
                throw new InternalError("Step cannot be zero - should have been caught earlier");
            }
            return result;
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("List range extraction failed", e);
        }
    }

    private List<Object> getListMultiRange(List<Object> list, Object multiRange) {
        try {
            List<Object> result = new ArrayList<Object>();
            for (Object range : RangeObjects.getRanges(multiRange)) {
                result.addAll(getListRange(list, range));
            }
            return result;
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("List multi-range extraction failed", e);
        }
    }

    private String applyStringRangeIndex(String text, Object range) {
        try {
            long start = expressionHandler.toLongIndex(RangeObjects.getStart(range));
            long end = expressionHandler.toLongIndex(RangeObjects.getEnd(range));
            long step = expressionHandler.calculateStep(range);

            int length = text.length();
            start = normalizeTextIndex(start, length);
            end = normalizeTextIndex(end, length);

            if (start < 0 || start >= length) {
                throw new ProgramError("Range start index out of bounds: " + start + " for text of length " + length);
            }
            if (end < 0 || end >= length) {
                throw new ProgramError("Range end index out of bounds: " + end + " for text of length " + length);
            }
            if (step == 0) {
                throw new ProgramError("Range step cannot be zero");
            }

            StringBuilder result = new StringBuilder();
            if (step > 0) {
                for (long i = start; i <= end; i += step) {
                    result.append(text.charAt((int) i));
                }
            } else {
                for (long i = start; i >= end; i += step) {
                    result.append(text.charAt((int) i));
                }
            }
            return result.toString();
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("String range extraction failed", e);
        }
    }

    private int normalizeTextIndex(int index, int length) {
        return (int) normalizeTextIndex((long) index, length);
    }

    private long normalizeTextIndex(long index, int length) {
        if (index < 0) {
            return length + index;
        }
        return index;
    }

    private Object applyPatterns(For node, List<PatternResult> patterns) {
        if (node == null) {
            throw new InternalError("applyPatterns called with null node");
        }
        if (patterns == null) {
            throw new InternalError("applyPatterns called with null patterns");
        }
        
        try {
            List<NaturalArray> targetArrays = new ArrayList<NaturalArray>();
            List<List<PatternResult>> groupedPatterns = new ArrayList<List<PatternResult>>();
            Map<Integer, Integer> arrayIdToGroupIndex = new HashMap<Integer, Integer>();
            
            for (PatternResult result : patterns) {
                if (result == null || result.targetArray == null) {
                    continue;
                }
                
                Object resolvedArray = dispatch(result.targetArray);
                resolvedArray = typeSystem.unwrap(resolvedArray);
                
                if (!(resolvedArray instanceof NaturalArray)) {
                    DebugSystem.debug("OPTIMIZER", "Array not optimizable, falling back to normal execution");
                    return executeForLoopNormally(node);
                }
                
                NaturalArray naturalArray = (NaturalArray) resolvedArray;
                int arrayId = naturalArray.getArrayId();
                Integer existingGroup = arrayIdToGroupIndex.get(arrayId);
                int groupIndex = existingGroup != null ? existingGroup : -1;
                
                if (groupIndex == -1) {
                    targetArrays.add(naturalArray);
                    List<PatternResult> newGroup = new ArrayList<PatternResult>();
                    newGroup.add(result);
                    groupedPatterns.add(newGroup);
                    arrayIdToGroupIndex.put(arrayId, targetArrays.size() - 1);
                } else {
                    groupedPatterns.get(groupIndex).add(result);
                }
            }
            
            if (targetArrays.isEmpty()) {
                DebugSystem.debug("OPTIMIZER", "No target arrays found, falling back to normal execution");
                return executeForLoopNormally(node);
            }
            
            long start = 0, end = 0;
            boolean boundsFound = false;
            
            if (node.range != null) {
                Object startObj = dispatch(node.range.start);
                Object endObj = dispatch(node.range.end);
                start = expressionHandler.toLong(startObj);
                end = expressionHandler.toLong(endObj);
                boundsFound = true;
            } else if (node.arraySource != null) {
                Object sourceObj = dispatch(node.arraySource);
                if (sourceObj instanceof NaturalArray) {
                    NaturalArray sourceArr = (NaturalArray) sourceObj;
                    if (sourceArr.size() > 0) {
                        start = 0;
                        end = sourceArr.size() - 1;
                        boundsFound = true;
                    }
                }
            }
            
            if (!boundsFound) {
                DebugSystem.debug("OPTIMIZER", "Could not determine bounds, falling back to normal execution");
                return executeForLoopNormally(node);
            }
            
            long min = Math.min(start, end);
            long max = Math.max(start, end);
            
            for (int arrayIndex = 0; arrayIndex < targetArrays.size(); arrayIndex++) {
                NaturalArray arr = targetArrays.get(arrayIndex);
                List<PatternResult> arrayPatterns = groupedPatterns.get(arrayIndex);
                
                for (PatternResult result : arrayPatterns) {
                    if (result.type == PatternType.SEQUENCE) {
                        applySequencePattern(arr, (SequencePattern.Pattern) result.pattern, min, max, node.iterator);
                    } else if (result.type == PatternType.CONDITIONAL) {
                        applyConditionalPattern(arr, (ConditionalPattern) result.pattern, min, max, node.iterator);
                    } else if (result.type == PatternType.LINEAR_RECURRENCE) {
                        applyLinearRecurrencePattern(arr, (LinearRecurrencePattern) result.pattern, min, max, node.iterator);
                    }
                }
            }
            
            // Preserve backward behavior by returning the last processed optimized target array.
            return targetArrays.get(targetArrays.size() - 1);
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Pattern application failed, falling back to normal execution", e);
        }
    }

    private void applyConditionalPattern(NaturalArray arr, ConditionalPattern pattern, 
                                    long min, long max, String iterator) {
        if (pattern == null) {
            throw new InternalError("applyConditionalPattern called with null pattern");
        }
        if (arr == null) {
            throw new InternalError("applyConditionalPattern called with null array");
        }
        
        try {
            List<Expr> conditions = new ArrayList<Expr>();
            List<List<Stmt>> branchStatements = new ArrayList<List<Stmt>>();
            
            for (ConditionalPattern.Branch branch : pattern.branches) {
                conditions.add(branch.condition);
                branchStatements.add(branch.statements);
            }
            
            ConditionalFormula formula = new ConditionalFormula(
                min, max, iterator,
                conditions,
                branchStatements,
                pattern.elseStatements
            );
            arr.addConditionalFormula(formula);
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Failed to apply conditional pattern", e);
        }
    }

    private void applySequencePattern(NaturalArray arr, 
                                     SequencePattern.Pattern pattern, 
                                     long min, long max, String iterator) {
        if (pattern == null) {
            throw new InternalError("applySequencePattern called with null pattern");
        }
        if (arr == null) {
            throw new InternalError("applySequencePattern called with null array");
        }
        
        try {
            SequenceFormula formula;
            
            if (pattern.isSimple()) {
                formula = SequenceFormula.createSimple(min, max, pattern.getFinalExpression(), iterator);
            } else {
                formula = SequenceFormula.createFromSequence(
                    min, max, iterator,
                    pattern.getTempVarNames(),
                    pattern.getTempExpressions(),
                    pattern.getFinalExpression()
                );
            }
            
            arr.addSequenceFormula(formula);
            
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Failed to apply sequence pattern", e);
        }
    }

    private void applyLinearRecurrencePattern(
        NaturalArray arr,
        LinearRecurrencePattern pattern,
        long min,
        long max,
        String iterator
    ) {
        if (arr == null) {
            throw new InternalError("applyLinearRecurrencePattern called with null array");
        }
        if (pattern == null) {
            throw new InternalError("applyLinearRecurrencePattern called with null pattern");
        }
        try {
            long start = Math.max(min, pattern.seedStart);
            long end = max;
            if (end < start) {
                return;
            }
            LinearRecurrenceFormula formula = new LinearRecurrenceFormula(
                start,
                end,
                pattern.recurrenceStart,
                pattern.coefficientsByLag,
                pattern.constantTerm,
                pattern.seedValues,
                pattern.seedStart
            );
            arr.addLinearRecurrenceFormula(formula);
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Failed to apply linear recurrence pattern", e);
        }
    }

    private Object executeForLoopNormally(For node) {
        ExecutionContext ctx = getCurrentContext();
        String iter = node.iterator;

        try {
            if (node.range != null) {
                return executeRangeLoop(ctx, node, iter);
            } else if (node.arraySource != null) {
                Object arrayObj = dispatch(node.arraySource);
                arrayObj = typeSystem.unwrap(arrayObj);
                return executeArrayLoop(ctx, node, iter, arrayObj);
            }
            throw new ProgramError("Invalid for loop");
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Normal loop execution failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Object executeArrayLoop(
        ExecutionContext ctx, For node, String iter, Object arrayObj) {
        try {
            if (arrayObj instanceof NaturalArray) {
                NaturalArray natural = (NaturalArray) arrayObj;
                long size = natural.size();
                for (long i = 0; i < size; i++) {
                    Object currentValue = natural.get(i);
                    ctx.setVariable(iter, currentValue);
                    try {
                        executeLoopBody(ctx, node);
                    } catch (BreakLoopException e) {
                        break;
                    }
                }
            } else if (arrayObj instanceof List) {
                List<Object> list = (List<Object>) arrayObj;
                for (Object currentValue : list) {
                    ctx.setVariable(iter, currentValue);
                    try {
                        executeLoopBody(ctx, node);
                    } catch (BreakLoopException e) {
                        break;
                    }
                }
            } else {
                throw new ProgramError("Cannot iterate over: " + 
                    (arrayObj != null ? arrayObj.getClass().getSimpleName() : "null"));
            }
            return null;
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Array loop execution failed", e);
        }
    }

    private Object executeRangeLoop(ExecutionContext ctx, For node, String iter) {
        try {
            Object startObj = dispatch(node.range.start);
            Object endObj = dispatch(node.range.end);
            startObj = typeSystem.unwrap(startObj);
            endObj = typeSystem.unwrap(endObj);

            if (node.range.step != null && node.range.step instanceof BinaryOp) {
                BinaryOp binOp = (BinaryOp) node.range.step;
                if (binOp.left instanceof Identifier
                    && ((Identifier) binOp.left).name.equals(iter)
                    && (binOp.op.equals("*") || binOp.op.equals("/"))) {
                    Object rightObj = dispatch(binOp.right);
                    rightObj = typeSystem.unwrap(rightObj);
                    AutoStackingNumber factor = typeSystem.toAutoStackingNumber(rightObj);
                    validateFactor(factor, binOp.op);
                    return executeMultiplicativeLoop(ctx, node, startObj, endObj, factor, binOp.op);
                }
            }

            AutoStackingNumber step;
            if (node.range.step != null) {
                Object stepObj = dispatch(node.range.step);
                step = typeSystem.toAutoStackingNumber(typeSystem.unwrap(stepObj));
            } else {
                AutoStackingNumber start = typeSystem.toAutoStackingNumber(startObj);
                AutoStackingNumber end = typeSystem.toAutoStackingNumber(endObj);
                step = (start.compareTo(end) > 0) ? AutoStackingNumber.minusOne(1) : AutoStackingNumber.one(1);
            }

            if (step.isZero()) {
                throw new ProgramError("Loop step cannot be zero.");
            }

            return executeAdditiveLoop(ctx, node, startObj, endObj, step);
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Range loop execution failed", e);
        }
    }

    private Object executeAdditiveLoop(
        ExecutionContext ctx, For node, Object startObj, Object endObj, AutoStackingNumber step) {
        try {
            AutoStackingNumber start = typeSystem.toAutoStackingNumber(startObj);
            AutoStackingNumber end = typeSystem.toAutoStackingNumber(endObj);
            AutoStackingNumber current = start;
            boolean increasing = step.isPositive();

            while (shouldContinueAdditive(current, end, step, increasing)) {
                try {
                    executeIteration(ctx, node, current, startObj);
                } catch (BreakLoopException e) {
                    break;
                }
                current = current.add(step);
            }
            return null;
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Additive loop execution failed", e);
        }
    }

    private Object executeMultiplicativeLoop(
        ExecutionContext ctx,
        For node,
        Object startObj,
        Object endObj,
        AutoStackingNumber factor,
        String operation) {
        try {
            AutoStackingNumber start = typeSystem.toAutoStackingNumber(startObj);
            AutoStackingNumber end = typeSystem.toAutoStackingNumber(endObj);
            AutoStackingNumber current = start;

            while (shouldContinueMultiplicative(current, start, end, factor, operation)) {
                try {
                    executeIteration(ctx, node, current, startObj);
                } catch (BreakLoopException e) {
                    break;
                }
                if (operation.equals("*")) {
                    current = current.multiply(factor);
                } else {
                    current = current.divide(factor);
                }
            }
            return null;
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Multiplicative loop execution failed", e);
        }
    }

    private void executeIteration(
        ExecutionContext ctx, For node, AutoStackingNumber current, Object startObj) {
        try {
            String iter = node.iterator;
            Object currentValue = convertToAppropriateType(current, startObj);
            ctx.setVariable(iter, currentValue);
            if (ctx.getVariableType(iter) == null) {
                String inferredType = (current.fitsInStacks(1) && 
                    (current.getWords()[0] & 0x7FFFFFFFFFFFFFFFL) < Long.MAX_VALUE)
                    ? INT.toString() : FLOAT.toString();
                ctx.setVariableType(iter, inferredType);
            }
            executeLoopBody(ctx, node);
        } catch (BreakLoopException e) {
            throw e;
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Loop iteration failed", e);
        }
    }

    private void executeLoopBody(ExecutionContext ctx, For node) {
        try {
            for (Stmt s : node.body.statements) {
                try {
                    dispatch(s);
                } catch (SkipIterationException e) {
                    break;
                } catch (BreakLoopException e) {
                    throw e;
                }

                if (!ctx.slotsInCurrentPath.isEmpty()
                    && interpreter.shouldReturnEarly(ctx.getSlotValues(), ctx.slotsInCurrentPath)) return;
            }
        } catch (BreakLoopException e) {
            throw e;
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Loop body execution failed", e);
        }
    }
    
    private Object executeOutputAwareLoop(For node, OutputAwarePattern.OutputPattern pattern) {
        ExecutionContext ctx = getCurrentContext();
        
        try {
            NaturalArray arr = createArrayFromOutputPattern(node, pattern.computation, ctx);
            
            ctx.enterOptimizedLoop();
            
            if (node.range != null) {
                executeOutputRangeLoop(ctx, node, arr, pattern.outputCalls);
            } else if (node.arraySource != null) {
                executeOutputArrayLoop(ctx, node, arr, pattern.outputCalls);
            }
            return arr;            
        } finally {
            ctx.exitOptimizedLoop();
        }
    }

    private NaturalArray createArrayFromOutputPattern(For node, Object computation, ExecutionContext ctx) {
        if (computation instanceof SequencePattern.Pattern) {
            SequencePattern.Pattern seqPattern = (SequencePattern.Pattern) computation;
            
            Range range = node.range;
            if (range == null && node.arraySource != null) {
                Object sourceObj = dispatch(node.arraySource);
                sourceObj = typeSystem.unwrap(sourceObj);
                
                if (sourceObj instanceof NaturalArray) {
                    NaturalArray sourceArr = (NaturalArray) sourceObj;
                    long size = sourceArr.size();
                    
                    Expr start = ASTFactory.createIntLiteral(0, null);
                    Expr end = ASTFactory.createIntLiteral((int)(size - 1), null);
                    range = ASTFactory.createRange(null, start, end, null, null);
                }
            }
            
            if (range == null) {
                throw new ProgramError("Cannot create array from pattern: no range specified");
            }
            
            NaturalArray arr = new NaturalArray(range, this, ctx);
            
            if (seqPattern.isSimple()) {
                SequenceFormula formula = SequenceFormula.createSimple(
                    0, arr.size() - 1, 
                    seqPattern.getFinalExpression(), 
                    node.iterator
                );
                arr.addSequenceFormula(formula);
            } else {
                SequenceFormula formula = SequenceFormula.createFromSequence(
                    0, arr.size() - 1, node.iterator,
                    seqPattern.getTempVarNames(),
                    seqPattern.getTempExpressions(),
                    seqPattern.getFinalExpression()
                );
                arr.addSequenceFormula(formula);
            }
            
            return arr;
            
        } else if (computation instanceof ConditionalPattern) {
            ConditionalPattern condPattern = (ConditionalPattern) computation;
            
            Range range = node.range;
            if (range == null && node.arraySource != null) {
                Object sourceObj = dispatch(node.arraySource);
                sourceObj = typeSystem.unwrap(sourceObj);
                
                if (sourceObj instanceof NaturalArray) {
                    NaturalArray sourceArr = (NaturalArray) sourceObj;
                    long size = sourceArr.size();
                    
                    Expr start = ASTFactory.createIntLiteral(0, null);
                    Expr end = ASTFactory.createIntLiteral((int)(size - 1), null);
                    range = ASTFactory.createRange(null, start, end, null, null);
                }
            }
            
            if (range == null) {
                throw new ProgramError("Cannot create array from pattern: no range specified");
            }
            
            NaturalArray arr = new NaturalArray(range, this, ctx);
            
            List<Expr> conditions = new ArrayList<Expr>();
            List<List<Stmt>> branchStatements = new ArrayList<List<Stmt>>();
            
            for (ConditionalPattern.Branch branch : condPattern.branches) {
                conditions.add(branch.condition);
                branchStatements.add(branch.statements);
            }
            
            ConditionalFormula formula = new ConditionalFormula(
                0, arr.size() - 1, node.iterator,
                conditions,
                branchStatements,
                condPattern.elseStatements
            );
            arr.addConditionalFormula(formula);
            
            return arr;
        }
        
        throw new ProgramError("Unknown computation pattern type");
    }

    private void executeOutputRangeLoop(ExecutionContext ctx, For node, 
                                       NaturalArray arr, List<MethodCall> outputCalls) {
        try {
            Object startObj = dispatch(node.range.start);
            Object endObj = dispatch(node.range.end);
            startObj = typeSystem.unwrap(startObj);
            endObj = typeSystem.unwrap(endObj);
            
            long start = expressionHandler.toLong(startObj);
            long end = expressionHandler.toLong(endObj);
            long step = calculateRangeStep(node.range);
            
            for (long i = start; i <= end; i += step) {
                Object value = arr.get(i);
                
                arr.recordOutput(i, value);
                
                ctx.setVariable(node.iterator, value);
                
                for (MethodCall outputCall : outputCalls) {
                    MethodCall evalCall = new MethodCall();
                    evalCall.name = outputCall.name;
                    evalCall.arguments = new ArrayList<Expr>();
                    
                    for (Expr arg : outputCall.arguments) {
                        if (arg instanceof Identifier && 
                            "_".equals(((Identifier) arg).name)) {
                            evalCall.arguments.add(new ValueExpr(value));
                        } else {
                            evalCall.arguments.add(arg);
                        }
                    }
                    
                    dispatch(evalCall);
                }
            }
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Output range loop execution failed", e);
        }
    }

    private void executeOutputArrayLoop(ExecutionContext ctx, For node,
                                       NaturalArray arr, List<MethodCall> outputCalls) {
        try {
            Object sourceObj = dispatch(node.arraySource);
            sourceObj = typeSystem.unwrap(sourceObj);
            
            long size = 0;
            if (sourceObj instanceof NaturalArray) {
                size = ((NaturalArray) sourceObj).size();
            } else if (sourceObj instanceof List) {
                size = ((List<?>) sourceObj).size();
            } else {
                throw new ProgramError("Cannot iterate over: " + 
                    (sourceObj != null ? sourceObj.getClass().getSimpleName() : "null"));
            }
            
            for (long i = 0; i < size; i++) {
                Object value = arr.get(i);
                
                arr.recordOutput(i, value);
                
                ctx.setVariable(node.iterator, value);
                
                for (MethodCall outputCall : outputCalls) {
                    MethodCall evalCall = new MethodCall();
                    evalCall.name = outputCall.name;
                    evalCall.arguments = new ArrayList<Expr>();
                    
                    for (Expr arg : outputCall.arguments) {
                        if (arg instanceof Identifier && 
                            "_".equals(((Identifier) arg).name)) {
                            evalCall.arguments.add(new ValueExpr(value));
                        } else {
                            evalCall.arguments.add(arg);
                        }
                    }
                    
                    dispatch(evalCall);
                }
            }
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Output array loop execution failed", e);
        }
    }

    private long calculateRangeStep(Range range) {
        if (range == null) {
            return 1L;
        }
        
        if (range.step != null) {
            Object stepObj = dispatch(range.step);
            return expressionHandler.toLong(stepObj);
        }
        
        Object startObj = dispatch(range.start);
        Object endObj = dispatch(range.end);
        long start = expressionHandler.toLong(startObj);
        long end = expressionHandler.toLong(endObj);
        
        return (start < end) ? 1L : -1L;
    }

    private boolean shouldContinueAdditive(
        AutoStackingNumber current, AutoStackingNumber end, AutoStackingNumber step, boolean increasing) {
        return increasing ? current.compareTo(end) <= 0 : current.compareTo(end) >= 0;
    }

    private void validateFactor(AutoStackingNumber factor, String operation) {
        if (factor.compareTo(AutoStackingNumber.zero(1)) <= 0) {
            throw new ProgramError("Factor must be positive");
        }
    }

    private boolean shouldContinueMultiplicative(
        AutoStackingNumber current, AutoStackingNumber start, AutoStackingNumber end, 
        AutoStackingNumber factor, String operation) {
        int startEndComparison = start.compareTo(end);
        if (operation.equals("*")) {
            return factor.compareTo(AutoStackingNumber.one(1)) > 0
                ? (startEndComparison < 0 ? current.compareTo(end) <= 0 : current.compareTo(end) >= 0)
                : (startEndComparison > 0 ? current.compareTo(end) >= 0 : current.compareTo(end) <= 0);
        } else {
            return factor.compareTo(AutoStackingNumber.one(1)) > 0
                ? (startEndComparison > 0 ? current.compareTo(end) >= 0 : current.compareTo(end) <= 0)
                : (startEndComparison < 0 ? current.compareTo(end) <= 0 : current.compareTo(end) >= 0);
        }
    }

    private Object convertToAppropriateType(AutoStackingNumber value, Object original) {
        if ((original instanceof Integer || original instanceof Long || 
             original instanceof IntLiteral) && value.fitsInStacks(1)) {
            try {
                return (int) value.longValue();
            } catch (ArithmeticException e) {
                return value.longValue();
            }
        }
        return value;
    }

    private List<ConditionalPattern> extractConditionalPatterns(StmtIf ifStmt, String iterator) {
        try {
            return ConditionalPattern.extractAll(ifStmt, iterator);
        } catch (Exception e) {
            DebugSystem.debug("OPTIMIZER", "Failed to extract conditional pattern: " + e.getMessage());
            return new ArrayList<ConditionalPattern>();
        }
    }
}
