package cod.interpreter;

import cod.ast.ASTVisitor;
import cod.ast.nodes.*;
import cod.range.NaturalArray;
import cod.range.MultiRangeSpec;
import cod.range.RangeSpec;
import cod.range.formula.*;
import cod.range.pattern.*;
import cod.interpreter.context.*;
import cod.interpreter.exception.*;
import cod.interpreter.type.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import static cod.syntax.Keyword.*;
import cod.semantic.ConstructorResolver;

public class InterpreterVisitor extends ASTVisitor<Object> {

    enum PatternType {
        CONDITIONAL,
        SIMPLE_ASSIGNMENT,
        SEQUENCE
    }

    class PatternResult {
        public final PatternType type;
        public final Object pattern;
        
        public PatternResult(PatternType type, Object pattern) {
            this.type = type;
            this.pattern = pattern;
        }
    }

    private final Interpreter interpreter;
    public final TypeSystem typeSystem;
    private final Stack<ExecutionContext> contextStack = new Stack<ExecutionContext>();
    private final ExpressionHandler expressionHandler;
    private final AssignmentHandler assignmentHandler;
    private final TypeHandler typeHandler;  // Add TypeHandler reference

    private static final int DECIMAL_SCALE = 20;
    
    public InterpreterVisitor(Interpreter interpreter, TypeSystem typeSystem) {
        this.interpreter = interpreter;
        this.typeSystem = typeSystem;
        this.expressionHandler = new ExpressionHandler(typeSystem, this);
        this.assignmentHandler = new AssignmentHandler(typeSystem, interpreter, expressionHandler, this);
        this.typeHandler = new TypeHandler(typeSystem);  // Initialize TypeHandler
    }

    public void pushContext(ExecutionContext context) {
        contextStack.push(context);
    }

    public void popContext() {
        contextStack.pop();
    }

    public ExecutionContext getCurrentContext() {
        return contextStack.peek();
    }

    // Helper to create none value
    private Object createNoneValue() {
        ExprNode noneNode = new ExprNode();
        noneNode.isNone = true;
        return noneNode;
    }

    @Override
    public Object visit(ExprNode node) {
        ExecutionContext ctx = getCurrentContext();

        // Handle property access like "this.field" or "ClassName.this.field" or "super.field"
        if (node.isPropertyAccess && node.propertyName != null) {
            if (ctx.objectInstance == null) {
                throw new RuntimeException("Cannot access property '" + node.propertyName + "' outside of object context");
            }
            
            // Handle super.property access
            if (node.isSuper) {
                if (ctx.objectInstance.type == null || ctx.objectInstance.type.extendName == null) {
                    throw new RuntimeException("Cannot access 'super." + node.propertyName + "' - no parent class");
                }
                
                // Find parent type
                TypeNode parentType = interpreter.getConstructorResolver()
                    .findParentType(ctx.objectInstance.type, ctx);
                
                if (parentType == null) {
                    throw new RuntimeException("Parent class not found for 'super." + node.propertyName + "'");
                }
                
                // Get the field value from the parent
                Object fieldValue = interpreter.getConstructorResolver()
                    .getFieldFromHierarchy(parentType, node.propertyName, ctx);
                
                if (fieldValue == null) {
                    throw new RuntimeException("Undefined field in parent: " + node.propertyName);
                }
                
                return fieldValue;
            }
            
            // Check for qualified "ClassName.this.field"
            if (node.thisClassName != null) {
                if (ctx.objectInstance.type == null || 
                    !node.thisClassName.equals(ctx.objectInstance.type.name)) {
                    throw new RuntimeException(
                        "Cannot access '" + node.thisClassName + ".this." + node.propertyName + 
                        "' in current context. Current object is of type: " + 
                        (ctx.objectInstance.type != null ? ctx.objectInstance.type.name : "null")
                    );
                }
            }
            
            // Get the field value from the object
            Object fieldValue = interpreter.getConstructorResolver()
                .getFieldFromHierarchy(ctx.objectInstance.type, node.propertyName, ctx);
            
            if (fieldValue == null) {
                throw new RuntimeException("Undefined field: " + node.propertyName);
            }
            
            return fieldValue;
        }

        // Handle "this" expression
        if (node.isThis) {
            if (ctx.objectInstance == null) {
                throw new RuntimeException("Cannot use 'this' outside of an object context");
            }
            
            // Check for qualified "ClassName.this"
            if (node.thisClassName != null) {
                if (ctx.objectInstance.type == null || 
                    !node.thisClassName.equals(ctx.objectInstance.type.name)) {
                    throw new RuntimeException(
                        "Cannot access '" + node.thisClassName + ".this' in current context. " +
                        "Current object is of type: " + 
                        (ctx.objectInstance.type != null ? ctx.objectInstance.type.name : "null")
                    );
                }
            }
            
            // Return the current object instance
            return ctx.objectInstance;
        }
        
        // Handle "super" expression
        if (node.isSuper) {
            if (ctx.objectInstance == null) {
                throw new RuntimeException("Cannot use 'super' outside of an object context");
            }
            
            if (ctx.objectInstance.type == null || ctx.objectInstance.type.extendName == null) {
                throw new RuntimeException("Cannot use 'super' - no parent class");
            }
            
            // Return the parent object instance (or at least the current instance for context)
            return ctx.objectInstance;
        }

        if (node.name != null) {
            // FIXED: Correct scoping order
            // 1. Check locals FIRST (most specific scope)
            if (ctx.locals.containsKey(node.name)) {
                Object val = ctx.locals.get(node.name);
                return val;
            }
            
            // 2. Check slot values (method return slots)
            if (ctx.slotValues != null && ctx.slotValues.containsKey(node.name)) {
                Object val = ctx.slotValues.get(node.name);
                return val;
            }
            
            // 3. Check if accessing field via "this.varName" syntax (old style)
            if (node.name.startsWith("this.") && ctx.objectInstance != null) {
                String fieldName = node.name.substring(5); // Remove "this."
                Object fieldValue = interpreter.getConstructorResolver()
                    .getFieldFromHierarchy(ctx.objectInstance.type, fieldName, ctx);
                if (fieldValue != null) {
                    return fieldValue;
                }
            }

            // 4. Check fields directly (instance scope)
            if (ctx.objectInstance != null && ctx.objectInstance.type != null) {
                Object fieldValue =
                    interpreter
                        .getConstructorResolver()
                        .getFieldFromHierarchy(ctx.objectInstance.type, node.name, ctx);
                if (fieldValue != null) {
                    return fieldValue;
                }
            }
            
            throw new RuntimeException("Undefined Variable: " + node.name);
        }

        if (node.value != null) {
            if (node.value instanceof String) {
                String s = (String) node.value;
                if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
                    // String literal
                    String result = s.substring(1, s.length() - 1);
                    return result;
                } else {
                    // Check if this is a type literal - Use TypeHandler
                    if (typeHandler.isTypeLiteral(s)) {
                        // Use TypeHandler to process type literal
                        return typeHandler.processTypeLiteral(s);
                    }
                    // Regular identifier string
                    return s;
                }
            }

            if (node.value instanceof BigDecimal) {
                BigDecimal result = ((BigDecimal) node.value).stripTrailingZeros();
                return result;
            }
            return node.value;
        }
        
        // Handle isNone flag
        if (node.isNone) {
            return createNoneValue();
        }
        
        return null;
    }

    @Override
    public Object visit(ConstructorCallNode node) {
        return interpreter.getConstructorResolver().resolveAndCreate(node, getCurrentContext());
    }

    @Override
    public Object visit(ConstructorNode node) {
        return null;
    }

    @Override
    public Object visit(BinaryOpNode node) {
        return expressionHandler.handleBinaryOp(node, getCurrentContext());
    }

    @Override
    public Object visit(BooleanChainNode node) {
        return expressionHandler.handleBooleanChain(node, getCurrentContext());
    }

    @Override
    public Object visit(EqualityChainNode node) {
        return expressionHandler.handleEqualityChain(node, getCurrentContext());
    }

    @Override
    public Object visit(UnaryNode node) {
        return expressionHandler.handleUnaryOp(node, getCurrentContext());
    }

    @Override
    public Object visit(TypeCastNode node) {
        return expressionHandler.handleTypeCast(node, getCurrentContext());
    }

    @Override
    public Object visit(ArrayNode node) {
        if (node.elements.size() == 1) {
            ExprNode onlyElement = node.elements.get(0);
            if (onlyElement instanceof RangeNode) {
                RangeNode range = (RangeNode) onlyElement;
                return new NaturalArray(range, this);
            }
        }

        List<Object> result = new ArrayList<Object>();
        for (ExprNode element : node.elements) {
            if (element instanceof RangeNode) {
                result.add(new NaturalArray((RangeNode) element, this));
            } else {
                Object evaluated = dispatch(element);
                
                // Convert type strings to TypeValue for type arrays
                if (evaluated instanceof String && typeHandler.isTypeLiteral((String) evaluated)) {
                    evaluated = TypeValue.createTypeValue((String) evaluated);
                }
                
                result.add(evaluated);
            }
        }
        return result;
    }

    @Override
    public Object visit(TupleNode node) {
        List<Object> tuple = new ArrayList<Object>();
        for (ExprNode elem : node.elements) tuple.add(dispatch(elem));
        return Collections.unmodifiableList(tuple);
    }

    @Override
    public Object visit(RangeIndexNode node) {
        // Create a range specification object
        Object step = node.step != null ? dispatch(node.step) : null;
        Object start = dispatch(node.start);
        Object end = dispatch(node.end);
        
        // Create a RangeSpec object
        return new RangeSpec(step, start, end);
    }

    @Override
    public Object visit(MultiRangeIndexNode node) {
        List<RangeSpec> ranges = new ArrayList<RangeSpec>();
        for (RangeIndexNode rangeNode : node.ranges) {
            ranges.add((RangeSpec) visit(rangeNode));
        }
        return new MultiRangeSpec(ranges);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object visit(IndexAccessNode node) {
        Object arrayObj = dispatch(node.array);
        Object indexObj = dispatch(node.index);

        arrayObj = typeSystem.unwrap(arrayObj);
        indexObj = typeSystem.unwrap(indexObj);

        // Handle range indexing
        if (indexObj instanceof RangeSpec) {
            return applyRangeIndex(arrayObj, (RangeSpec) indexObj);
        }
        
        // Handle multi-range indexing
        if (indexObj instanceof MultiRangeSpec) {
            return applyMultiRangeIndex(arrayObj, (MultiRangeSpec) indexObj);
        }

        // Original code for regular index access
        if (arrayObj instanceof NaturalArray) {
            NaturalArray natural = (NaturalArray) arrayObj;
            long index = expressionHandler.toLongIndex(indexObj);
            // NaturalArray handles its own bounds checking (including negative indices)
            return natural.get(index);
        }

        if (arrayObj instanceof List) {
            List<Object> list = (List<Object>) arrayObj;
            if (indexObj instanceof BigDecimal) {
                int index = ((BigDecimal) indexObj).intValue();
                if (index < 0 || index >= list.size()) {
                    throw new RuntimeException(
                        "Index out of bounds: " + index + " for array of size " + list.size());
                }
                return list.get(index);
            } else {
                int index = expressionHandler.toIntIndex(indexObj);
                if (index < 0 || index >= list.size()) {
                    throw new RuntimeException(
                        "Index out of bounds: " + index + " for array of size " + list.size());
                }
                return list.get(index);
            }
        }

        throw new RuntimeException(
            "Invalid array access: expected NaturalArray or List, got "
                + (arrayObj != null ? arrayObj.getClass().getSimpleName() : "null"));
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
        throw new RuntimeException("Cannot apply range index to " + 
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
        throw new RuntimeException("Cannot apply multi-range index to " + 
            (array != null ? array.getClass().getSimpleName() : "null"));
    }

    private List<Object> getListRange(List<Object> list, RangeSpec range) {
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
            throw new RuntimeException("Step cannot be zero");
        }
        return result;
    }

    private List<Object> getListMultiRange(List<Object> list, MultiRangeSpec multiRange) {
        List<Object> result = new ArrayList<Object>();
        for (RangeSpec range : multiRange.ranges) {
            result.addAll(getListRange(list, range));
        }
        return result;
    }

    @Override
    public Object visit(AssignmentNode node) {
        return assignmentHandler.handleAssignment(node, getCurrentContext());
    }

    @Override
    public Object visit(SlotDeclarationNode node) {
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object visit(MethodCallNode node) {
        // Handle super method calls
        if (node.isSuperCall) {
            return handleSuperMethodCall(node);
        }
        
        if (interpreter.getGlobalRegistry().isGlobal(node.name)) {
            return interpreter.getGlobalRegistry().executeGlobal(node.name, node.arguments, this);
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
                    if (ctx.locals.containsKey(receiver)) {
                        Object receiverObj = ctx.locals.get(receiver);
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

        if (method == null) throw new RuntimeException("Method not found: " + node.name);

        if (method.isBuiltin) return interpreter.handleBuiltinMethod(method, node);

        Object result = interpreter.evalMethodCall(node, ctx.objectInstance, ctx.locals, method);

        if (node.slotNames != null && !node.slotNames.isEmpty()) {
            if (!(result instanceof Map)) throw new RuntimeException("Method did not return slots.");

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

            if (map.containsKey(requestedSlot)) return map.get(requestedSlot);
            else throw new RuntimeException("Undefined method slot: " + requestedSlot);
        }

        return result;
    }
    
    // Handle super.method() calls
    @SuppressWarnings("unchecked")
    private Object handleSuperMethodCall(MethodCallNode node) {
        ExecutionContext ctx = getCurrentContext();
        
        if (ctx.objectInstance == null || ctx.objectInstance.type == null) {
            throw new RuntimeException("Cannot call 'super." + node.name + "' outside of object context");
        }
        
        if (ctx.objectInstance.type.extendName == null) {
            throw new RuntimeException("Cannot call 'super." + node.name + "' - no parent class");
        }
        
        // Find parent type
        ConstructorResolver resolver = interpreter.getConstructorResolver();
        TypeNode parentType = resolver.findParentType(ctx.objectInstance.type, ctx);
        
        if (parentType == null) {
            throw new RuntimeException("Parent class not found for 'super." + node.name + "'");
        }
        
        // Find method in parent (not current class)
        MethodNode method = resolver.findMethodInHierarchy(parentType, node.name, ctx);
        
        if (method == null) {
            throw new RuntimeException("Method '" + node.name + "' not found in parent class");
        }
        
        if (method.isBuiltin) {
            return interpreter.handleBuiltinMethod(method, node);
        }
        
        // Evaluate the method call in parent context
        Object result = interpreter.evalMethodCall(node, ctx.objectInstance, ctx.locals, method);
        
        if (node.slotNames != null && !node.slotNames.isEmpty()) {
            if (!(result instanceof Map)) throw new RuntimeException("Method did not return slots.");

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

            if (map.containsKey(requestedSlot)) return map.get(requestedSlot);
            else throw new RuntimeException("Undefined method slot: " + requestedSlot);
        }

        return result;
    }

    @Override
    public Object visit(MultipleSlotAssignmentNode node) {
        return assignmentHandler.handleMultipleSlotAssignment(node, getCurrentContext());
    }

    @Override
    public Object visit(SlotAssignmentNode node) {
        return assignmentHandler.handleSlotAssignment(node, getCurrentContext());
    }

    @Override
    public Object visit(FieldNode node) {
        ExecutionContext ctx = getCurrentContext();
        Object val = node.value != null ? dispatch(node.value) : null;
        if (ctx.objectInstance.type != null) {
            Object existingField =
                interpreter
                    .getConstructorResolver()
                    .getFieldFromHierarchy(ctx.objectInstance.type, node.name, ctx);
            if (existingField != null) throw new RuntimeException("Cannot redeclare field: " + node.name);
        }
        ctx.objectInstance.fields.put(node.name, val);
        return val;
    }

    @Override
    public Object visit(VarNode node) {
        Object val = node.value != null ? dispatch(node.value) : null;
        
        if (node.explicitType != null) {
            String declaredType = node.explicitType;
            getCurrentContext().localTypes.put(node.name, declaredType);
            
            // Special handling for type variables
            if (TYPE.toString().equals(declaredType)) {
                // Convert string type literals to TypeValue
                if (val instanceof String) {
                    String typeStr = (String) val;
                    if (typeHandler.isTypeLiteral(typeStr)) {
                        val = TypeValue.createTypeValue(typeStr);
                    }
                }
            }
            
            // If no value provided and type is nullable, default to none
            if (val == null && declaredType.contains("|none")) {
                val = createNoneValue();
            }
            
            // Special handling for none values with nullable types
            if (!typeSystem.validateType(declaredType, val)) {
                if (typeHandler.isNoneValue(val) && declaredType.contains("|none")) {
                    // none is valid for nullable types
                    val = createNoneValue();
                } else {
                    throw new RuntimeException("Type mismatch for " + node.name + ". Expected " + declaredType);
                }
            }
            
            if (declaredType.contains("|")) {
                String activeType = typeSystem.getConcreteType(typeSystem.unwrap(val));
                val = new TypeValue(val, activeType, declaredType);
            }
        }
        
        getCurrentContext().locals.put(node.name, val);
        return val;
    }

    @Override
    public Object visit(ExitNode node) {
        throw new EarlyExitException();
    }

    @Override
    public Object visit(StmtIfNode node) {
        Object testObj = dispatch(node.condition);
        boolean test = typeHandler.isTruthy(typeSystem.unwrap(testObj));
        List<StmtNode> branch = test ? node.thenBlock.statements : node.elseBlock.statements;

        ExecutionContext ctx = getCurrentContext();
        Set<String> prevSlots = new HashSet<String>(ctx.slotsInCurrentPath);

        for (StmtNode s : branch) {
            dispatch(s);
            if (!ctx.slotsInCurrentPath.isEmpty()
                && interpreter.shouldReturnEarly(ctx.slotValues, ctx.slotsInCurrentPath)) break;
        }
        ctx.slotsInCurrentPath = prevSlots;
        return null;
    }

    @Override
    public Object visit(ExprIfNode node) {
        Object condValue = dispatch(node.condition);
        if (typeHandler.isTruthy(typeSystem.unwrap(condValue))) return dispatch(node.thenExpr);
        else return dispatch(node.elseExpr);
    }

    @Override
    public Object visit(ForNode node) {
        // FIRST: Collect ALL patterns in the loop body
        List<PatternResult> allPatterns = new ArrayList<PatternResult>();
        
        // Check for conditional patterns (if statements)
        for (StmtNode stmt : node.body.statements) {
            if (stmt instanceof StmtIfNode) {
                StmtIfNode ifStmt = (StmtIfNode) stmt;
                ConditionalPattern pattern = extractConditionalPattern(ifStmt, node.iterator);
                if (pattern != null && pattern.isOptimizable()) {
                    allPatterns.add(new PatternResult(PatternType.CONDITIONAL, pattern));
                }
            }
        }
        
        // Check for simple assignment patterns
        if (node.body.statements.size() == 1 && node.body.statements.get(0) instanceof AssignmentNode) {
            AssignmentNode assign = (AssignmentNode) node.body.statements.get(0);
            AssignmentPattern pattern = AssignmentPattern.extract(assign, node.iterator);
            if (pattern != null) {
                allPatterns.add(new PatternResult(PatternType.SIMPLE_ASSIGNMENT, pattern));
            }
        }
        
        // Check for sequence patterns (2-statement sequences)
        if (node.body.statements.size() == 2) {
            AssignmentPattern.StatementSequencePattern seqPattern = 
                AssignmentPattern.extractSequence(node.body.statements, node.iterator);
            if (seqPattern != null && seqPattern.isOptimizable()) {
                allPatterns.add(new PatternResult(PatternType.SEQUENCE, seqPattern));
            }
        }
        
        // If we found patterns, try to apply them
        if (!allPatterns.isEmpty()) {
            return applyPatterns(node, allPatterns);
        }
        
        // If no patterns matched or not optimizable, fall back to normal execution
        return executeForLoopNormally(node);
    }

    private ExprNode substituteVariable(ExprNode expr, String varName, ExprNode replacement) {
    if (expr == null) return null;
    
    // If this expression IS the variable we're replacing
    if (expr instanceof ExprNode && varName.equals(((ExprNode) expr).name)) {
        return replacement;
    }
    
    // Recursively substitute in binary operations
    if (expr instanceof BinaryOpNode) {
        BinaryOpNode binOp = (BinaryOpNode) expr;
        BinaryOpNode newBinOp = new BinaryOpNode();
        newBinOp.op = binOp.op;
        newBinOp.left = substituteVariable(binOp.left, varName, replacement);
        newBinOp.right = substituteVariable(binOp.right, varName, replacement);
        return newBinOp;
    }
    
    // Recursively substitute in unary operations
    if (expr instanceof UnaryNode) {
        UnaryNode unary = (UnaryNode) expr;
        UnaryNode newUnary = new UnaryNode();
        newUnary.op = unary.op;
        newUnary.operand = substituteVariable(unary.operand, varName, replacement);
        return newUnary;
    }
    
    // For MethodCallNode, substitute in arguments
    if (expr instanceof MethodCallNode) {
        MethodCallNode call = (MethodCallNode) expr;
        MethodCallNode newCall = new MethodCallNode();
        newCall.name = call.name;
        newCall.qualifiedName = call.qualifiedName;
        newCall.slotNames = call.slotNames;
        newCall.isSuperCall = call.isSuperCall; // Preserve super call flag
        
        if (call.arguments != null) {
            newCall.arguments = new java.util.ArrayList<ExprNode>();
            for (ExprNode arg : call.arguments) {
                newCall.arguments.add(substituteVariable(arg, varName, replacement));
            }
        }
        return newCall;
    }
    
    // For IndexAccessNode, substitute in array and index
    if (expr instanceof IndexAccessNode) {
        IndexAccessNode access = (IndexAccessNode) expr;
        IndexAccessNode newAccess = new IndexAccessNode();
        newAccess.array = substituteVariable(access.array, varName, replacement);
        newAccess.index = substituteVariable(access.index, varName, replacement);
        return newAccess;
    }
    
    // For TypeCastNode, substitute in expression
    if (expr instanceof TypeCastNode) {
        TypeCastNode cast = (TypeCastNode) expr;
        TypeCastNode newCast = new TypeCastNode();
        newCast.targetType = cast.targetType;
        newCast.expression = substituteVariable(cast.expression, varName, replacement);
        return newCast;
    }
    
    // For ArrayNode, substitute in elements
    if (expr instanceof ArrayNode) {
        ArrayNode array = (ArrayNode) expr;
        ArrayNode newArray = new ArrayNode();
        newArray.elements = new java.util.ArrayList<ExprNode>();
        for (ExprNode elem : array.elements) {
            newArray.elements.add(substituteVariable(elem, varName, replacement));
        }
        return newArray;
    }
    
    // For TupleNode, substitute in elements
    if (expr instanceof TupleNode) {
        TupleNode tuple = (TupleNode) expr;
        TupleNode newTuple = new TupleNode();
        newTuple.elements = new java.util.ArrayList<ExprNode>();
        for (ExprNode elem : tuple.elements) {
            newTuple.elements.add(substituteVariable(elem, varName, replacement));
        }
        return newTuple;
    }
    
    // For other node types, return as-is (no substitution needed)
    return expr;
}

    private Object executeForLoopNormally(ForNode node) {
        ExecutionContext ctx = getCurrentContext();
        String iter = node.iterator;

        if (node.range != null) {
            return executeRangeLoop(ctx, node, iter);
        } else if (node.arraySource != null) {
            Object arrayObj = dispatch(node.arraySource);
            arrayObj = typeSystem.unwrap(arrayObj);
            return executeArrayLoop(ctx, node, iter, arrayObj);
        }
        throw new RuntimeException("Invalid for loop");
    }
    
    private Object applyPatterns(ForNode node, List<PatternResult> patterns) {
        // Get the array object
        Object arrayObj = null;
        
        // Determine which array we're working with from the patterns
        for (PatternResult result : patterns) {
            switch (result.type) {
                case CONDITIONAL:
                    ConditionalPattern condPattern = (ConditionalPattern) result.pattern;
                    if (condPattern.arrayExpression != null) {
                        arrayObj = dispatch(condPattern.arrayExpression);
                    }
                    break;
                case SIMPLE_ASSIGNMENT:
                    AssignmentPattern assignPattern = (AssignmentPattern) result.pattern;
                    if (assignPattern.array != null) {
                        arrayObj = dispatch(assignPattern.array);
                    }
                    break;
                case SEQUENCE:
                    AssignmentPattern.StatementSequencePattern seqPattern = 
                        (AssignmentPattern.StatementSequencePattern) result.pattern;
                    if (seqPattern.targetArray != null) {
                        arrayObj = dispatch(seqPattern.targetArray);
                    }
                    break;
            }
            if (arrayObj != null) break;
        }
        
        if (!(arrayObj instanceof NaturalArray)) {
            return executeForLoopNormally(node);
        }
        
        NaturalArray arr = (NaturalArray) arrayObj;
        
        // Determine loop bounds
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
            return executeForLoopNormally(node);
        }
        
        long min = Math.min(start, end);
        long max = Math.max(start, end);
        
        // Apply ALL patterns to the array
        for (PatternResult result : patterns) {
            switch (result.type) {
                case CONDITIONAL:
                    applyConditionalPattern(arr, (ConditionalPattern) result.pattern, min, max, node.iterator);
                    break;
                case SIMPLE_ASSIGNMENT:
                    applyAssignmentPattern(arr, (AssignmentPattern) result.pattern, min, max, node.iterator);
                    break;
                case SEQUENCE:
                    applySequencePattern(arr, (AssignmentPattern.StatementSequencePattern) result.pattern, min, max, node.iterator);
                    break;
            }
        }
        
        // Optimization applied! No actual iteration needed.
        return arr;
    }

    private void applyConditionalPattern(NaturalArray arr, ConditionalPattern pattern, long min, long max, String iterator) {
        if (pattern.hasElif()) {
            // Multi-branch formula
            MultiBranchFormula formula = new MultiBranchFormula(
                min, max, iterator,
                pattern.condition, pattern.thenExpr,
                pattern.elifConditions, pattern.elifExpressions,
                pattern.elseExpr
            );
            arr.addMultiBranchFormula(formula);
        } else {
            // Regular conditional formula
            ConditionalFormula formula = new ConditionalFormula(
                min, max, iterator,
                pattern.condition, pattern.thenExpr, pattern.elseExpr
            );
            arr.addConditionalFormula(formula);
        }
    }

    private void applyAssignmentPattern(NaturalArray arr, AssignmentPattern pattern, long min, long max, String iterator) {
        // LoopFormula formula = new LoopFormula(min, max, pattern.expression, iterator);
        // TO-DO: Actual Implementation
    }

    private void applySequencePattern(NaturalArray arr, 
                                     AssignmentPattern.StatementSequencePattern pattern, 
                                     long min, long max, String iterator) {
        // Substitute the temp variable with its expression
        ExprNode substitutedExpr = substituteVariable(
            pattern.finalExpr, 
            pattern.tempVar, 
            pattern.tempExpr
        );
        
        // Create and add the loop formula
        LoopFormula formula = new LoopFormula(min, max, substitutedExpr, iterator);
        arr.addLoopFormula(formula);
    }

    @SuppressWarnings("unchecked")
    private Object executeArrayLoop(
        ExecutionContext ctx, ForNode node, String iter, Object arrayObj) {
        if (arrayObj instanceof NaturalArray) {
            NaturalArray natural = (NaturalArray) arrayObj;
            long size = natural.size();
            for (long i = 0; i < size; i++) {
                Object currentValue = natural.get(i);
                ctx.locals.put(iter, currentValue);
                try {
                    executeLoopBody(ctx, node);
                } catch (BreakLoopException e) {
                    break;  // Break out of the loop when BreakLoopException is thrown
                }
            }
        } else if (arrayObj instanceof List) {
            List<Object> list = (List<Object>) arrayObj;
            for (Object currentValue : list) {
                ctx.locals.put(iter, currentValue);
                try {
                    executeLoopBody(ctx, node);
                } catch (BreakLoopException e) {
                    break;  // Break out of the loop when BreakLoopException is thrown
                }
            }
        } else {
            throw new RuntimeException("Cannot iterate over: " + arrayObj.getClass().getSimpleName());
        }
        return null;
    }

    private Object executeRangeLoop(ExecutionContext ctx, ForNode node, String iter) {
        Object startObj = dispatch(node.range.start);
        Object endObj = dispatch(node.range.end);
        startObj = typeSystem.unwrap(startObj);
        endObj = typeSystem.unwrap(endObj);

        if (node.range.step != null && node.range.step instanceof BinaryOpNode) {
            BinaryOpNode binOp = (BinaryOpNode) node.range.step;
            if (binOp.left instanceof ExprNode
                && ((ExprNode) binOp.left).name.equals(iter)
                && (binOp.op.equals("*") || binOp.op.equals("/"))) {
                Object rightObj = dispatch(binOp.right);
                rightObj = typeSystem.unwrap(rightObj);
                BigDecimal factorBD = typeSystem.toBigDecimal(rightObj);
                validateFactor(factorBD, binOp.op);
                return executeMultiplicativeLoop(ctx, node, startObj, endObj, factorBD, binOp.op);
            }
        }

        BigDecimal stepBD;
        if (node.range.step != null) {
            Object stepObj = dispatch(node.range.step);
            stepBD = typeSystem.toBigDecimal(typeSystem.unwrap(stepObj));
        } else {
            BigDecimal startBD = typeSystem.toBigDecimal(startObj);
            BigDecimal endBD = typeSystem.toBigDecimal(endObj);
            stepBD = (startBD.compareTo(endBD) > 0) ? BigDecimal.ONE.negate() : BigDecimal.ONE;
        }

        if (stepBD.compareTo(BigDecimal.ZERO) == 0)
            throw new RuntimeException("Loop step cannot be zero.");

        return executeAdditiveLoop(ctx, node, startObj, endObj, stepBD);
    }

    private Object executeAdditiveLoop(
        ExecutionContext ctx, ForNode node, Object startObj, Object endObj, BigDecimal stepBD) {
        BigDecimal startBD = typeSystem.toBigDecimal(startObj);
        BigDecimal endBD = typeSystem.toBigDecimal(endObj);
        BigDecimal current = startBD;
        boolean increasing = stepBD.compareTo(BigDecimal.ZERO) > 0;

        while (shouldContinueAdditive(current, endBD, stepBD, increasing)) {
            try {
                executeIteration(ctx, node, current, startObj);
            } catch (BreakLoopException e) {
                break;  // Break out of the loop when BreakLoopException is thrown
            }
            current = current.add(stepBD);
        }
        return null;
    }

    private Object executeMultiplicativeLoop(
        ExecutionContext ctx,
        ForNode node,
        Object startObj,
        Object endObj,
        BigDecimal factorBD,
        String operation) {
        BigDecimal startBD = typeSystem.toBigDecimal(startObj);
        BigDecimal endBD = typeSystem.toBigDecimal(endObj);
        BigDecimal current = startBD;

        while (shouldContinueMultiplicative(current, startBD, endBD, factorBD, operation)) {
            try {
                executeIteration(ctx, node, current, startObj);
            } catch (BreakLoopException e) {
                break;  // Break out of the loop when BreakLoopException is thrown
            }
            if (operation.equals("*")) current = current.multiply(factorBD);
            else current = current.divide(factorBD, DECIMAL_SCALE, RoundingMode.HALF_UP);
        }
        return null;
    }

    private void executeIteration(
        ExecutionContext ctx, ForNode node, BigDecimal current, Object startObj) {
        String iter = node.iterator;
        Object currentValue = convertToAppropriateType(current, startObj);
        ctx.locals.put(iter, currentValue);
        if (!ctx.localTypes.containsKey(iter)) {
            String inferredType =
                (currentValue instanceof Integer || currentValue instanceof Long)
                    ? INT.toString()
                    : FLOAT.toString();
            ctx.localTypes.put(iter, inferredType);
        }
        executeLoopBody(ctx, node);
    }

    private void executeLoopBody(ExecutionContext ctx, ForNode node) {
        for (StmtNode s : node.body.statements) {
            try {
                dispatch(s);
            } catch (SkipIterationException e) {
                break;
            } catch (BreakLoopException e) {
                throw e;
            }

            if (!ctx.slotsInCurrentPath.isEmpty()
                && interpreter.shouldReturnEarly(ctx.slotValues, ctx.slotsInCurrentPath)) return;
        }
    }

    private boolean shouldContinueAdditive(
        BigDecimal current, BigDecimal end, BigDecimal step, boolean increasing) {
        return increasing ? current.compareTo(end) <= 0 : current.compareTo(end) >= 0;
    }

    private void validateFactor(BigDecimal factor, String operation) {
        if (factor.compareTo(BigDecimal.ZERO) <= 0)
            throw new RuntimeException("Factor must be positive");
    }

    private boolean shouldContinueMultiplicative(
        BigDecimal current, BigDecimal start, BigDecimal end, BigDecimal factor, String operation) {
        int startEndComparison = start.compareTo(end);
        if (operation.equals("*")) {
            return factor.compareTo(BigDecimal.ONE) > 0
                ? (startEndComparison < 0 ? current.compareTo(end) <= 0 : current.compareTo(end) >= 0)
                : (startEndComparison > 0 ? current.compareTo(end) >= 0 : current.compareTo(end) <= 0);
        } else {
            return factor.compareTo(BigDecimal.ONE) > 0
                ? (startEndComparison > 0 ? current.compareTo(end) >= 0 : current.compareTo(end) <= 0)
                : (startEndComparison < 0 ? current.compareTo(end) <= 0 : current.compareTo(end) >= 0);
        }
    }

    private Object convertToAppropriateType(BigDecimal value, Object original) {
        if ((original instanceof Integer || original instanceof Long) && value.scale() == 0) {
            try {
                return value.intValueExact();
            } catch (ArithmeticException e) {
                return value.longValue();
            }
        }
        return value;
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
    @SuppressWarnings("unchecked")
    public Object visit(ReturnSlotAssignmentNode node) {
        ExecutionContext ctx = getCurrentContext();
        Object res = interpreter.evalMethodCall(node.methodCall, ctx.objectInstance, ctx.locals, null);

        if (res instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) res;
            MethodNode method = null;
            if (ctx.objectInstance.type != null) {
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
                    }
                }

                if (map.containsKey(requestedSlot)) {
                    ctx.locals.put(node.variableNames.get(i), map.get(requestedSlot));
                } else {
                    throw new RuntimeException("Missing slot: " + slot);
                }
            }
        } else throw new RuntimeException("Method did not return slot values");
        return res;
    }

    private ConditionalPattern extractConditionalPattern(StmtIfNode ifStmt, String iterator) {
        return ConditionalPattern.extract(ifStmt, iterator);
    }
}