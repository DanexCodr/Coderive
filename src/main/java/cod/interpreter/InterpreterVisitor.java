package cod.interpreter;

import cod.ast.ASTFactory;
import cod.ast.ASTVisitor;
import cod.ast.nodes.*;
import cod.debug.DebugSystem;
import cod.error.InternalError;
import cod.error.ProgramError;
import cod.math.AutoStackingNumber;
import cod.range.NaturalArray;
import cod.range.MultiRangeSpec;
import cod.range.RangeSpec;
import cod.range.formula.*;
import cod.range.pattern.*;
import cod.interpreter.registry.LiteralRegistry;
import cod.interpreter.context.*;
import cod.interpreter.exception.*;
import cod.interpreter.handler.*;
import java.util.*;
import static cod.syntax.Keyword.*;
import cod.semantic.ConstructorResolver;

public class InterpreterVisitor extends ASTVisitor<Object> implements Evaluator {

    enum PatternType {
        CONDITIONAL,
        SEQUENCE
    }

    class PatternResult {
        public final PatternType type;
        public final Object pattern;
        
        public PatternResult(PatternType type, Object pattern) {
            if (type == null) {
                throw new InternalError("PatternResult constructed with null type");
            }
            this.type = type;
            this.pattern = pattern;
        }
    }

    private final Interpreter interpreter;
    public final TypeHandler typeSystem;
    private final Stack<ExecutionContext> contextStack = new Stack<ExecutionContext>();
    private final ExpressionHandler expressionHandler;
    private final AssignmentHandler assignmentHandler;
    
    private final LiteralRegistry literalRegistry;  

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
        this.expressionHandler = new ExpressionHandler(typeSystem, this);
        this.assignmentHandler = new AssignmentHandler(typeSystem, interpreter, expressionHandler, this);
    }
    
    // Implement Evaluator interface
    @Override
    public Object evaluate(ExprNode node, ExecutionContext ctx) {
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
    public Object evaluate(StmtNode node, ExecutionContext ctx) {
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

    private Object createNoneValue() {
        return new NoneLiteralNode();
    }

    @Override
    public Object visit(ProgramNode n) {
        return defaultVisit(n);
    }

    @Override
    public Object visit(UnitNode n) {
        return defaultVisit(n);
    }

    @Override
    public Object visit(UseNode n) {
        return defaultVisit(n);
    }

    @Override
    public Object visit(TypeNode n) {
        return defaultVisit(n);
    }

    @Override
    public Object visit(FieldNode n) {
        if (n == null) {
            throw new InternalError("visit(FieldNode) called with null node");
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
        ctx.objectInstance.fields.put(n.name, val);
        return val;
    }

    @Override
    public Object visit(MethodNode n) {
        return defaultVisit(n);
    }

    @Override
    public Object visit(ParamNode n) {
        return defaultVisit(n);
    }

    @Override
    public Object visit(ConstructorNode n) {
        return defaultVisit(n);
    }

    @Override
    public Object visit(ConstructorCallNode node) {
        if (node == null) {
            throw new InternalError("visit(ConstructorCallNode) called with null node");
        }
        
        try {
            return interpreter.getConstructorResolver().resolveAndCreate(node, getCurrentContext());
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Constructor resolution failed", e);
        }
    }

    @Override
    public Object visit(PolicyNode n) {
        return defaultVisit(n);
    }

    @Override
    public Object visit(PolicyMethodNode n) {
        return defaultVisit(n);
    }

    @Override
    public Object visit(BlockNode node) {
        if (node == null) {
            throw new InternalError("visit(BlockNode) called with null node");
        }
        
        ExecutionContext ctx = getCurrentContext();
        ctx.pushScope();
        
        try {
            for (StmtNode stmt : node.statements) {
                dispatch(stmt);
            }
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
    public Object visit(AssignmentNode node) {
        if (node == null) {
            throw new InternalError("visit(AssignmentNode) called with null node");
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
    public Object visit(VarNode node) {
        if (node == null) {
            throw new InternalError("visit(VarNode) called with null node");
        }
        
        try {
            Object val = node.value != null ? dispatch(node.value) : null;
            
            ExecutionContext ctx = getCurrentContext();
            
            // Handle array type conversion for [text] = [int range]
            if (node.explicitType != null && node.explicitType.startsWith("[") && 
                node.explicitType.endsWith("]") && val instanceof NaturalArray) {
                
                NaturalArray arr = (NaturalArray) val;
                String expectedElementType = node.explicitType.substring(1, node.explicitType.length() - 1);
                String actualElementType = arr.getElementType();
                
                // If expected is [text] but actual is not text, create a converting wrapper
                if (expectedElementType.equals("text") && !actualElementType.equals("text")) {
                    // Create a new NaturalArray with conversion enabled
                    RangeNode range = getRangeFromArray(arr);
                    if (range != null) {
                        val = new NaturalArray(range, this, ctx, node.explicitType);
                    }
                }
            }
            
            ctx.setVariable(node.name, val);
            
            if (node.explicitType != null) {
                String declaredType = node.explicitType;
                ctx.setVariableType(node.name, declaredType);
                
                if (TYPE.toString().equals(declaredType)) {
                    if (val instanceof String) {
                        String typeStr = (String) val;
                        if (typeSystem.isTypeLiteral(typeStr)) {
                            val = TypeHandler.Value.createTypeValue(typeStr);
                            ctx.setVariable(node.name, val);
                        }
                    } else if (val instanceof TextLiteralNode) {
                        String typeStr = ((TextLiteralNode) val).value;
                        if (typeSystem.isTypeLiteral(typeStr)) {
                            val = TypeHandler.Value.createTypeValue(typeStr);
                            ctx.setVariable(node.name, val);
                        }
                    }
                }
                
                if (val == null && declaredType.contains("|none")) {
                    val = createNoneValue();
                    ctx.setVariable(node.name, val);
                }
                
                if (!typeSystem.validateType(declaredType, val)) {
                    if (typeSystem.isNoneValue(val) && declaredType.contains("|none")) {
                        val = createNoneValue();
                        ctx.setVariable(node.name, val);
                    } else {
                        throw new ProgramError("Type mismatch for " + node.name + ". Expected " + declaredType);
                    }
                }
                
                if (declaredType.contains("|")) {
                    String activeType = typeSystem.getConcreteType(typeSystem.unwrap(val));
                    val = new TypeHandler.Value(val, activeType, declaredType);
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

    // Helper method to extract RangeNode from NaturalArray
    private RangeNode getRangeFromArray(NaturalArray arr) {
        try {
            java.lang.reflect.Field rangeField = NaturalArray.class.getDeclaredField("baseRange");
            rangeField.setAccessible(true);
            return (RangeNode) rangeField.get(arr);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Object visit(StmtIfNode node) {
        if (node == null) {
            throw new InternalError("visit(StmtIfNode) called with null node");
        }
        
        try {
            Object testObj = dispatch(node.condition);
            boolean test = typeSystem.isTruthy(typeSystem.unwrap(testObj));
            
            ExecutionContext ctx = getCurrentContext();
            
            int originalDepth = ctx.getScopeDepth();
            
            try {
                ctx.pushScope();
                List<StmtNode> statements = test ? node.thenBlock.statements : node.elseBlock.statements;
                for (StmtNode s : statements) {
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
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("If statement execution failed", e);
        }
    }
    
    @Override
    public Object visit(ExprIfNode node) {
        if (node == null) {
            throw new InternalError("visit(ExprIfNode) called with null node");
        }
        
        try {
            Object condValue = dispatch(node.condition);
            if (typeSystem.isTruthy(typeSystem.unwrap(condValue))) {
                return dispatch(node.thenExpr);
            } else {
                return dispatch(node.elseExpr);
            }
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("If expression execution failed", e);
        }
    }

    @Override
    public Object visit(ForNode node) {
        if (node == null) {
            throw new InternalError("visit(ForNode) called with null node");
        }
        
        ExecutionContext ctx = getCurrentContext();
        
        int originalDepth = ctx.getScopeDepth();
        
        try {
            ctx.pushScope();
            
            OutputAwarePattern.OutputPattern outputPattern = 
                OutputAwarePattern.extract(node, node.iterator);
            
            if (outputPattern.isOptimizable) {
                try {
                    return executeOutputAwareLoop(node, outputPattern);
                } catch (Exception e) {
                    DebugSystem.debug("OPTIMIZER", 
                        "Output-aware pattern failed, falling back: " + e.getMessage());
                }
            }
            
            List<PatternResult> allPatterns = new ArrayList<PatternResult>();
            
            for (StmtNode stmt : node.body.statements) {
                if (stmt instanceof StmtIfNode) {
                    StmtIfNode ifStmt = (StmtIfNode) stmt;
                    ConditionalPattern pattern = extractConditionalPattern(ifStmt, node.iterator);
                    if (pattern != null && pattern.isOptimizable()) {
                        allPatterns.add(new PatternResult(PatternType.CONDITIONAL, pattern));
                    }
                }
            }
            
            SequencePattern.Pattern seqPattern = 
                SequencePattern.extract(node.body.statements, node.iterator);
            if (seqPattern != null && seqPattern.isOptimizable()) {
                allPatterns.add(new PatternResult(PatternType.SEQUENCE, seqPattern));
            }
            
            if (!allPatterns.isEmpty()) {
                try {
                    return applyPatterns(node, allPatterns);
                } catch (ProgramError e) {
                    throw e;
                } catch (Exception e) {
                    throw new InternalError("Pattern optimization failed, falling back to normal execution", e);
                }
            }
            
            if (node.range != null) {
                return executeRangeLoop(ctx, node, node.iterator);
            } else if (node.arraySource != null) {
                Object arrayObj = dispatch(node.arraySource);
                arrayObj = typeSystem.unwrap(arrayObj);
                return executeArrayLoop(ctx, node, node.iterator, arrayObj);
            }
            throw new ProgramError("Invalid for loop: neither range nor array source specified");
            
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("For loop execution failed", e);
        } finally {
            while (ctx.getScopeDepth() > originalDepth) {
                ctx.popScope();
            }
        }
    }

    @Override
    public Object visit(SkipNode node) {
        throw new SkipIterationException();
    }

    @Override
    public Object visit(BreakNode node) {
        throw new BreakLoopException();
    }

    @Override
    public Object visit(RangeNode n) {
        return defaultVisit(n);
    }

    @Override
    public Object visit(ExitNode node) {
        throw new EarlyExitException();
    }

    @Override
    public Object visit(TupleNode node) {
        if (node == null) {
            throw new InternalError("visit(TupleNode) called with null node");
        }
        
        try {
            List<Object> tuple = new ArrayList<Object>();
            for (ExprNode elem : node.elements) {
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
public Object visit(ReturnSlotAssignmentNode node) {
    if (node == null) {
        throw new InternalError("visit(ReturnSlotAssignmentNode) called with null node");
    }
    
    ExecutionContext ctx = getCurrentContext();
    
    Map<String, Object> allLocals = new HashMap<>();
    for (int i = 0; i < ctx.getScopeDepth(); i++) {
        Map<String, Object> scope = ctx.getScope(i);
        if (scope != null) {
            allLocals.putAll(scope);
        }
    }
    
    try {
        Object res = interpreter.evalMethodCall(node.methodCall, ctx.objectInstance, allLocals, null);

        if (res instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) res;
            MethodNode method = null;
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
            // NEW: Handle case where method returns a single value directly
            // This happens when calling a single-slot method without []: syntax
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

    @Override
    public Object visit(SlotAssignmentNode node) {
        if (node == null) {
            throw new InternalError("visit(SlotAssignmentNode) called with null node");
        }
        
        try {
            return assignmentHandler.handleSlotAssignment(node, getCurrentContext());
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Slot assignment failed", e);
        }
    }

    @Override
    public Object visit(SlotDeclarationNode n) {
        return defaultVisit(n);
    }

    @Override
    public Object visit(MultipleSlotAssignmentNode node) {
        if (node == null) {
            throw new InternalError("visit(MultipleSlotAssignmentNode) called with null node");
        }
        
        try {
            return assignmentHandler.handleMultipleSlotAssignment(node, getCurrentContext());
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Multiple slot assignment failed", e);
        }
    }

    @Override
    public Object visit(IdentifierNode node) {
        if (node == null) {
            throw new InternalError("visit(IdentifierNode) called with null node");
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
        
        throw new ProgramError("Undefined variable: " + name);
    }

    @Override
    public Object visit(IntLiteralNode node) {
        if (node == null) {
            throw new InternalError("visit(IntLiteralNode) called with null node");
        }
        return node.value;
    }

    @Override
    public Object visit(FloatLiteralNode node) {
        if (node == null) {
            throw new InternalError("visit(FloatLiteralNode) called with null node");
        }
        return node.value;
    }

    @Override
    public Object visit(TextLiteralNode node) {
        if (node == null) {
            throw new InternalError("visit(TextLiteralNode) called with null node");
        }
        
        String text = node.value;
        
        if (typeSystem.isTypeLiteral(text)) {
            return typeSystem.processTypeLiteral(text);
        }
        
        if (text.startsWith("\"") && text.endsWith("\"") && text.length() >= 2) {
            return text.substring(1, text.length() - 1);
        }
        
        return text;
    }

    @Override
    public Object visit(BoolLiteralNode node) {
        if (node == null) {
            throw new InternalError("visit(BoolLiteralNode) called with null node");
        }
        return node.value;
    }

    @Override
    public Object visit(NoneLiteralNode node) {
        return node;
    }

    @Override
    public Object visit(ThisNode node) {
        if (node == null) {
            throw new InternalError("visit(ThisNode) called with null node");
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
    public Object visit(SuperNode node) {
        if (node == null) {
            throw new InternalError("visit(SuperNode) called with null node");
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
    public Object visit(PropertyAccessNode node) {
        if (node == null) {
            throw new InternalError("visit(PropertyAccessNode) called with null node");
        }
        
        ExecutionContext ctx = getCurrentContext();
        
        try {
            Object leftObj = dispatch(node.left);
            leftObj = typeSystem.unwrap(leftObj);
            
            if (node.right instanceof IdentifierNode) {
                IdentifierNode right = (IdentifierNode) node.right;
                String propertyName = right.name;
                
                if (literalRegistry.hasProperty(leftObj, propertyName)) {
                    return literalRegistry.handleProperty(leftObj, propertyName, ctx);
                }
            }
            
            if (leftObj instanceof NaturalArray) {
                NaturalArray natural = (NaturalArray) leftObj;
                if (natural.hasPendingUpdates()) {
                    natural.commitUpdates();
                }
            }
            
            if (node.left instanceof SuperNode) {
                return handleSuperPropertyAccess(node, ctx);
            }
            
            if (node.left instanceof ThisNode) {
                return handleThisPropertyAccess(node, ctx);
            }
            
            if (leftObj instanceof ObjectInstance) {
                ObjectInstance instance = (ObjectInstance) leftObj;
                
                if (node.right instanceof IdentifierNode) {
                    IdentifierNode right = (IdentifierNode) node.right;
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

    private Object handleSuperPropertyAccess(PropertyAccessNode node, ExecutionContext ctx) {
        if (ctx.objectInstance == null || ctx.objectInstance.type == null) {
            throw new ProgramError("Cannot access 'super' outside of object context");
        }
        
        if (ctx.objectInstance.type.extendName == null) {
            throw new ProgramError("Cannot access 'super' - no parent class");
        }
        
        try {
            TypeNode parentType = interpreter.getConstructorResolver()
                .findParentType(ctx.objectInstance.type, ctx);
            
            if (parentType == null) {
                throw new ProgramError("Parent class not found");
            }
            
            if (node.right instanceof IdentifierNode) {
                IdentifierNode right = (IdentifierNode) node.right;
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

    private Object handleThisPropertyAccess(PropertyAccessNode node, ExecutionContext ctx) {
        if (ctx.objectInstance == null || ctx.objectInstance.type == null) {
            throw new ProgramError("Cannot access 'this' outside of object context");
        }
        
        try {
            if (node.left instanceof ThisNode) {
                ThisNode left = (ThisNode) node.left;
                if (left.className != null && 
                    !left.className.equals(ctx.objectInstance.type.name)) {
                    throw new ProgramError(
                        "Cannot access '" + left.className + ".this' in current context. " +
                        "Current object is of type: " + ctx.objectInstance.type.name
                    );
                }
            }
            
            if (node.right instanceof IdentifierNode) {
                IdentifierNode right = (IdentifierNode) node.right;
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
    public Object visit(BinaryOpNode node) {
        if (node == null) {
            throw new InternalError("visit(BinaryOpNode) called with null node");
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
    public Object visit(UnaryNode node) {
        if (node == null) {
            throw new InternalError("visit(UnaryNode) called with null node");
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
    public Object visit(TypeCastNode node) {
        if (node == null) {
            throw new InternalError("visit(TypeCastNode) called with null node");
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
public Object visit(MethodCallNode node) {
    if (node == null) {
        throw new InternalError("visit(MethodCallNode) called with null node");
    }
    
    try {
        if (node.isSuperCall) {
            return handleSuperMethodCall(node);
        }
        
        List<Object> evaluatedArgs = new ArrayList<Object>();
        for (ExprNode arg : node.arguments) {
            Object argValue = dispatch(arg);
            evaluatedArgs.add(typeSystem.unwrap(argValue));
        }
        
        if (node.isGlobal) {
            return interpreter.getGlobalRegistry().executeGlobal(node.name, evaluatedArgs);
        }

        ExecutionContext ctx = getCurrentContext();
        MethodNode method = null;

        if (ctx.currentClass != null) {
            method =
                interpreter
                    .getConstructorResolver()
                    .findMethodInHierarchy(ctx.currentClass, node.name, ctx);
        }

        if (method == null && ctx.objectInstance != null && ctx.objectInstance.type != null) {
            method =
                interpreter
                    .getConstructorResolver()
                    .findMethodInHierarchy(ctx.objectInstance.type, node.name, ctx);
        }

        if (method == null) {
            String qName = node.qualifiedName;
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
                                qName = objInst.type.name + "." + methodName;
                            }
                        }
                    }
                }
            }
            if (qName == null) qName = node.name;
            method = interpreter.getImportResolver().findMethod(qName);
        }

        if (method == null) {
            throw new ProgramError("Method not found: " + node.name);
        }

        // NEW: Check if this is a single-slot call (no []: syntax but method has one slot)
        boolean hasSingleSlot = method.returnSlots != null && method.returnSlots.size() == 1;
        if (node.slotNames.isEmpty() && hasSingleSlot) {
            node.isSingleSlotCall = true;
            // Set the slot name to the single slot's name for proper handling
            node.slotNames.add(method.returnSlots.get(0).name);
        }

        if (method.isBuiltin) {
            MethodCallNode evaluatedCall = new MethodCallNode();
            evaluatedCall.name = node.name;
            evaluatedCall.arguments = new ArrayList<ExprNode>();
            for (Object val : evaluatedArgs) {
                evaluatedCall.arguments.add(new ValueExprNode(val));
            }
            evaluatedCall.slotNames = node.slotNames;
            evaluatedCall.qualifiedName = node.qualifiedName;
            evaluatedCall.target = node.target;
            evaluatedCall.isSuperCall = node.isSuperCall;
            evaluatedCall.isSingleSlotCall = node.isSingleSlotCall;
            
            return interpreter.handleBuiltinMethod(method, evaluatedCall);
        }

        Map<String, Object> methodLocals = new HashMap<String, Object>();
        Map<String, String> methodLocalTypes = new HashMap<String, String>();

        int argCount = evaluatedArgs.size();
        int paramCount = method.parameters != null ? method.parameters.size() : 0;

        for (int i = 0; i < paramCount; i++) {
            ParamNode param = method.parameters.get(i);
            Object argValue = null;

            if (i < argCount) {
                argValue = evaluatedArgs.get(i);
            } else {
                if (param.hasDefaultValue) {
                    ExecutionContext defaultCtx = new ExecutionContext(ctx.objectInstance, new HashMap<String, Object>(), null, null, typeSystem);
                    pushContext(defaultCtx);
                    try {
                        argValue = dispatch(param.defaultValue);
                    } finally {
                        popContext();
                    }
                } else {
                    throw new ProgramError(
                        "Missing argument for parameter '" + param.name + 
                        "'. Expected " + paramCount + " arguments, got " + argCount);
                }
            }

            String paramType = param.type;

            if (!typeSystem.validateType(paramType, argValue)) {
                if (paramType.equals(TEXT.toString())) {
                    argValue = typeSystem.convertType(argValue, paramType);
                } else {
                    throw new ProgramError(
                        "Argument type mismatch for parameter " + param.name + 
                        ". Expected " + paramType + ", got: " + typeSystem.getConcreteType(argValue));
                }
            }

            if (paramType.contains("|")) {
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

        Map<String, Object> slotValues = new LinkedHashMap<String, Object>();
        Map<String, String> slotTypes = new LinkedHashMap<String, String>();
        if (method.returnSlots != null) {
            for (SlotNode s : method.returnSlots) {
                slotValues.put(s.name, null);
                slotTypes.put(s.name, s.type);
            }
        }

        ExecutionContext methodCtx = new ExecutionContext(ctx.objectInstance, methodLocals, slotValues, slotTypes, typeSystem);
        
        for (Map.Entry<String, String> entry : methodLocalTypes.entrySet()) {
            methodCtx.setVariableType(entry.getKey(), entry.getValue());
        }
        
        methodCtx.objectInstance = ctx.objectInstance;
        
        if (method.associatedClass != null) {
            TypeNode classType = findTypeByName(method.associatedClass);
            if (classType != null) {
                methodCtx.currentClass = classType;
            }
        }
        
        if (ctx.objectInstance != null && ctx.objectInstance.type != null && methodCtx.currentClass == null) {
            TypeNode classType = findTypeByName(ctx.objectInstance.type.name);
            if (classType != null) {
                methodCtx.currentClass = classType;
            }
        }

        pushContext(methodCtx);
        boolean calledMethodHasSlots = method.returnSlots != null && !method.returnSlots.isEmpty();
        Object methodResult = null;

        try {
            if (method.body != null) {
                for (StmtNode stmt : method.body) {
                    visit(stmt);
                    
                    if (calledMethodHasSlots && interpreter.shouldReturnEarly(slotValues, methodCtx.slotsInCurrentPath)) {
                        break;
                    }
                }
            }
        } catch (EarlyExitException e) {
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Method call execution failed: " + node.name, e);
        } finally {
            popContext();
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
                        // Not an index
                    }
                }

                if (map.containsKey(requestedSlot)) {
                    // NEW: For single-slot calls, return the value directly
                    if (node.isSingleSlotCall) {
                        return map.get(requestedSlot);
                    }
                    return map.get(requestedSlot);
                } else {
                    throw new ProgramError("Undefined method slot: " + requestedSlot);
                }
            } else if (calledMethodHasSlots) {
                return slotValues;
            }
        }

        // Default: return whatever the method produced
        return methodResult != null ? methodResult : slotValues;
    } catch (ProgramError e) {
        throw e;
    } catch (Exception e) {
        throw new InternalError("Method call failed: " + node.name, e);
    }
}

    private TypeNode findTypeByName(String className) {
        ProgramNode currentProgram = interpreter.getCurrentProgram();
        if (currentProgram != null && currentProgram.unit != null && currentProgram.unit.types != null) {
            for (TypeNode t : currentProgram.unit.types) {
                if (t.name.equals(className)) {
                    return t;
                }
            }
        }
        return null;
    }

    @Override
    public Object visit(ArrayNode node) {
        if (node == null) {
            throw new InternalError("visit(ArrayNode) called with null node");
        }
        
        try {
            if (node.elements.size() == 1) {
                ExprNode onlyElement = node.elements.get(0);
                if (onlyElement instanceof RangeNode) {
                    RangeNode range = (RangeNode) onlyElement;
                    
                    // Just create the array - type checking happens in VarNode
                    return new NaturalArray(range, this, getCurrentContext());
                }
            }

            // Regular array literal handling
            List<Object> result = new ArrayList<Object>();
            for (ExprNode element : node.elements) {
                if (element instanceof RangeNode) {
                    result.add(new NaturalArray((RangeNode) element, this, getCurrentContext()));
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
                    } else if (evaluated instanceof TextLiteralNode) {
                        String str = ((TextLiteralNode) evaluated).value;
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

@SuppressWarnings("unchecked")
@Override
public Object visit(IndexAccessNode node) {
    if (node == null) {
        throw new InternalError("visit(IndexAccessNode) called with null node");
    }
    
    try {
        Object arrayObj = dispatch(node.array);
        arrayObj = typeSystem.unwrap(arrayObj);
        
        // === FIX: Force materialization BEFORE index access ===
        if (arrayObj instanceof NaturalArray) {
            NaturalArray natural = (NaturalArray) arrayObj;
            if (natural.hasPendingUpdates()) {
                natural.commitUpdates(); // This evaluates all pending formulas
            }
        }
        
        Object indexObj = dispatch(node.index);
        indexObj = typeSystem.unwrap(indexObj);

        if (indexObj instanceof RangeSpec) {
            return applyRangeIndex(arrayObj, (RangeSpec) indexObj);
        }
        
        if (indexObj instanceof MultiRangeSpec) {
            return applyMultiRangeIndex(arrayObj, (MultiRangeSpec) indexObj);
        }

        if (arrayObj instanceof NaturalArray) {
            NaturalArray natural = (NaturalArray) arrayObj;
            long index = expressionHandler.toLongIndex(indexObj);
            
            // Use the conversion-aware get method
            if (natural.needsConversion()) {
                return natural.get(index, true);
            } else {
                return natural.get(index);
            }
        }

        if (arrayObj instanceof List) {
            List<Object> list = (List<Object>) arrayObj;
            if (indexObj instanceof AutoStackingNumber) {
                int index = (int) ((AutoStackingNumber) indexObj).longValue();
                if (index < 0 || index >= list.size()) {
                    throw new ProgramError(
                        "Index out of bounds: " + index + " for array of size " + list.size());
                }
                return list.get(index);
            } else {
                int index = expressionHandler.toIntIndex(indexObj);
                if (index < 0 || index >= list.size()) {
                    throw new ProgramError(
                        "Index out of bounds: " + index + " for array of size " + list.size());
                }
                return list.get(index);
            }
        }

        throw new ProgramError(
            "Invalid array access: expected NaturalArray or List, got "
                + (arrayObj != null ? arrayObj.getClass().getSimpleName() : "null"));
    } catch (ProgramError e) {
        throw e;
    } catch (Exception e) {
        throw new InternalError("Index access failed", e);
    }
}

    @Override
    public Object visit(RangeIndexNode node) {
        if (node == null) {
            throw new InternalError("visit(RangeIndexNode) called with null node");
        }
        
        try {
            Object step = node.step != null ? dispatch(node.step) : null;
            Object start = dispatch(node.start);
            Object end = dispatch(node.end);
            
            return new RangeSpec(step, start, end);
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Range index creation failed", e);
        }
    }

    @Override
    public Object visit(MultiRangeIndexNode node) {
        if (node == null) {
            throw new InternalError("visit(MultiRangeIndexNode) called with null node");
        }
        
        try {
            List<RangeSpec> ranges = new ArrayList<RangeSpec>();
            for (RangeIndexNode rangeNode : node.ranges) {
                ranges.add((RangeSpec) visit(rangeNode));
            }
            return new MultiRangeSpec(ranges);
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Multi-range index creation failed", e);
        }
    }

    @Override
    public Object visit(EqualityChainNode node) {
        if (node == null) {
            throw new InternalError("visit(EqualityChainNode) called with null node");
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
    public Object visit(BooleanChainNode node) {
        if (node == null) {
            throw new InternalError("visit(BooleanChainNode) called with null node");
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
    public Object visit(SlotNode n) {
        return defaultVisit(n);
    }

    @Override
    public Object visit(LambdaNode node) {
        return node;
    }

    @SuppressWarnings("unchecked")
    private Object handleSuperMethodCall(MethodCallNode node) {
        ExecutionContext ctx = getCurrentContext();
        
        if (ctx.objectInstance == null || ctx.objectInstance.type == null) {
            throw new ProgramError("Cannot call 'super." + node.name + "' outside of object context");
        }
        
        if (ctx.objectInstance.type.extendName == null) {
            throw new ProgramError("Cannot call 'super." + node.name + "' - no parent class");
        }
        
        try {
            ConstructorResolver resolver = interpreter.getConstructorResolver();
            TypeNode parentType = resolver.findParentType(ctx.objectInstance.type, ctx);
            
            if (parentType == null) {
                throw new ProgramError("Parent class not found for 'super." + node.name + "'");
            }
            
            MethodNode method = resolver.findMethodInHierarchy(parentType, node.name, ctx);
            
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
                    }
                }

                if (map.containsKey(requestedSlot)) {
                    return map.get(requestedSlot);
                } else {
                    throw new ProgramError("Undefined method slot: " + requestedSlot);
                }
            }

            return result;
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Super method call failed: " + node.name, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Object applyRangeIndex(Object array, RangeSpec range) {
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
    private Object applyMultiRangeIndex(Object array, MultiRangeSpec multiRange) {
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

    private List<Object> getListRange(List<Object> list, RangeSpec range) {
        try {
            long start, end;
            
            start = expressionHandler.toLongIndex(range.start);
            if (start < 0) start = list.size() + start;
            
            end = expressionHandler.toLongIndex(range.end);
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

    private List<Object> getListMultiRange(List<Object> list, MultiRangeSpec multiRange) {
        try {
            List<Object> result = new ArrayList<Object>();
            for (RangeSpec range : multiRange.ranges) {
                result.addAll(getListRange(list, range));
            }
            return result;
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("List multi-range extraction failed", e);
        }
    }

    private Object applyPatterns(ForNode node, List<PatternResult> patterns) {
        if (node == null) {
            throw new InternalError("applyPatterns called with null node");
        }
        if (patterns == null) {
            throw new InternalError("applyPatterns called with null patterns");
        }
        
        try {
            Object arrayObj = null;
            
            for (PatternResult result : patterns) {
                if (result.type == PatternType.SEQUENCE) {
                    SequencePattern.Pattern seqPattern = (SequencePattern.Pattern) result.pattern;
                    if (seqPattern.targetArray != null) {
                        arrayObj = dispatch(seqPattern.targetArray);
                    }
                } else if (result.type == PatternType.CONDITIONAL) {
                    ConditionalPattern condPattern = (ConditionalPattern) result.pattern;
                    if (condPattern.array != null) {
                        arrayObj = dispatch(condPattern.array);
                    }
                }
                if (arrayObj != null) break;
            }
            
            if (!(arrayObj instanceof NaturalArray)) {
                DebugSystem.debug("OPTIMIZER", "Array not optimizable, falling back to normal execution");
                return executeForLoopNormally(node);
            }
            
            NaturalArray arr = (NaturalArray) arrayObj;
            
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
            
            for (PatternResult result : patterns) {
                if (result.type == PatternType.SEQUENCE) {
                    applySequencePattern(arr, (SequencePattern.Pattern) result.pattern, min, max, node.iterator);
                } else if (result.type == PatternType.CONDITIONAL) {
                    applyConditionalPattern(arr, (ConditionalPattern) result.pattern, min, max, node.iterator);
                }
            }
            
            return arr;
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
            List<ExprNode> conditions = new ArrayList<ExprNode>();
            List<List<StmtNode>> branchStatements = new ArrayList<List<StmtNode>>();
            
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

    private Object executeForLoopNormally(ForNode node) {
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
        ExecutionContext ctx, ForNode node, String iter, Object arrayObj) {
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

    private Object executeRangeLoop(ExecutionContext ctx, ForNode node, String iter) {
        try {
            Object startObj = dispatch(node.range.start);
            Object endObj = dispatch(node.range.end);
            startObj = typeSystem.unwrap(startObj);
            endObj = typeSystem.unwrap(endObj);

            if (node.range.step != null && node.range.step instanceof BinaryOpNode) {
                BinaryOpNode binOp = (BinaryOpNode) node.range.step;
                if (binOp.left instanceof IdentifierNode
                    && ((IdentifierNode) binOp.left).name.equals(iter)
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
        ExecutionContext ctx, ForNode node, Object startObj, Object endObj, AutoStackingNumber step) {
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
        ForNode node,
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
        ExecutionContext ctx, ForNode node, AutoStackingNumber current, Object startObj) {
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
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Loop iteration failed", e);
        }
    }

    private void executeLoopBody(ExecutionContext ctx, ForNode node) {
        try {
            for (StmtNode s : node.body.statements) {
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
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Loop body execution failed", e);
        }
    }
    
    private Object executeOutputAwareLoop(ForNode node, OutputAwarePattern.OutputPattern pattern) {
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

    private NaturalArray createArrayFromOutputPattern(ForNode node, Object computation, ExecutionContext ctx) {
        if (computation instanceof SequencePattern.Pattern) {
            SequencePattern.Pattern seqPattern = (SequencePattern.Pattern) computation;
            
            RangeNode range = node.range;
            if (range == null && node.arraySource != null) {
                Object sourceObj = dispatch(node.arraySource);
                sourceObj = typeSystem.unwrap(sourceObj);
                
                if (sourceObj instanceof NaturalArray) {
                    NaturalArray sourceArr = (NaturalArray) sourceObj;
                    long size = sourceArr.size();
                    
                    ExprNode start = ASTFactory.createIntLiteral(0, null);
                    ExprNode end = ASTFactory.createIntLiteral((int)(size - 1), null);
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
            
            RangeNode range = node.range;
            if (range == null && node.arraySource != null) {
                Object sourceObj = dispatch(node.arraySource);
                sourceObj = typeSystem.unwrap(sourceObj);
                
                if (sourceObj instanceof NaturalArray) {
                    NaturalArray sourceArr = (NaturalArray) sourceObj;
                    long size = sourceArr.size();
                    
                    ExprNode start = ASTFactory.createIntLiteral(0, null);
                    ExprNode end = ASTFactory.createIntLiteral((int)(size - 1), null);
                    range = ASTFactory.createRange(null, start, end, null, null);
                }
            }
            
            if (range == null) {
                throw new ProgramError("Cannot create array from pattern: no range specified");
            }
            
            NaturalArray arr = new NaturalArray(range, this, ctx);
            
            List<ExprNode> conditions = new ArrayList<ExprNode>();
            List<List<StmtNode>> branchStatements = new ArrayList<List<StmtNode>>();
            
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

    private void executeOutputRangeLoop(ExecutionContext ctx, ForNode node, 
                                       NaturalArray arr, List<MethodCallNode> outputCalls) {
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
                
                for (MethodCallNode outputCall : outputCalls) {
                    MethodCallNode evalCall = new MethodCallNode();
                    evalCall.name = outputCall.name;
                    evalCall.arguments = new ArrayList<ExprNode>();
                    
                    for (ExprNode arg : outputCall.arguments) {
                        if (arg instanceof IdentifierNode && 
                            "_".equals(((IdentifierNode) arg).name)) {
                            evalCall.arguments.add(new ValueExprNode(value));
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

    private void executeOutputArrayLoop(ExecutionContext ctx, ForNode node,
                                       NaturalArray arr, List<MethodCallNode> outputCalls) {
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
                
                for (MethodCallNode outputCall : outputCalls) {
                    MethodCallNode evalCall = new MethodCallNode();
                    evalCall.name = outputCall.name;
                    evalCall.arguments = new ArrayList<ExprNode>();
                    
                    for (ExprNode arg : outputCall.arguments) {
                        if (arg instanceof IdentifierNode && 
                            "_".equals(((IdentifierNode) arg).name)) {
                            evalCall.arguments.add(new ValueExprNode(value));
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

    private long calculateRangeStep(RangeNode range) {
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
             original instanceof IntLiteralNode) && value.fitsInStacks(1)) {
            try {
                return (int) value.longValue();
            } catch (ArithmeticException e) {
                return value.longValue();
            }
        }
        return value;
    }

    private ConditionalPattern extractConditionalPattern(StmtIfNode ifStmt, String iterator) {
        try {
            return ConditionalPattern.extract(ifStmt, iterator);
        } catch (Exception e) {
            DebugSystem.debug("OPTIMIZER", "Failed to extract conditional pattern: " + e.getMessage());
            return null;
        }
    }
}