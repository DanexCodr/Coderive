package cod.interpreter;

import cod.ast.nodes.*;
import cod.debug.DebugSystem;
import java.util.*;

public class StatementEvaluator {
    private Interpreter interpreter;
    private ExpressionEvaluator exprEvaluator;
    private IOHandler ioHandler;
    private TypeSystem typeSystem;

    public StatementEvaluator(Interpreter interpreter, ExpressionEvaluator exprEvaluator,
                            IOHandler ioHandler, TypeSystem typeSystem) {
        this.interpreter = interpreter;
        this.exprEvaluator = exprEvaluator;
        this.ioHandler = ioHandler;
        this.typeSystem = typeSystem;
    }

    public Object evalStmt(StatementNode stmt, ObjectInstance obj, Map<String, Object> locals, Map<String, Object> slotValues) {
        DebugSystem.trace("INTERPRETER", "evalStmt: " + stmt.getClass().getSimpleName());

        // --- MODIFICATION: Removed SlotDeclarationNode case ---
        if (stmt instanceof SlotAssignmentNode) {
            SlotAssignmentNode assign = (SlotAssignmentNode) stmt;
            Object value = exprEvaluator.evaluate(assign.value, obj, locals);
            String varName = assign.slotName;

            if (slotValues != null && slotValues.containsKey(varName)) {
                Object oldValue = slotValues.get(varName);
                slotValues.put(varName, value);
                DebugSystem.slotUpdate(varName, oldValue, value);
            } else {
                 DebugSystem.warn("SLOTS", "Assignment to '" + varName + "' ignored (not a declared slot).");
            }
            return value;
        // --- END MODIFICATION ---

        } else if (stmt instanceof InputNode) {
            // Handle input: name = (type) input
            InputNode input = (InputNode) stmt;
            Object inputValue = ioHandler.readInput(input.targetType);

            // --- Enforce ~ for slots (NO CHANGE) ---
            if (slotValues != null && slotValues.containsKey(input.variableName)) {
                 throw new RuntimeException("Cannot assign to slot '" + input.variableName + "' using '=' (from input). Use '~> " + input.variableName + " ...' syntax instead.");
            } else {
                obj.fields.put(input.variableName, inputValue);
                DebugSystem.fieldUpdate(input.variableName, inputValue);
            }
            locals.put(input.variableName, inputValue);
            return inputValue;

        } else if (stmt instanceof AssignmentNode) {
            // NEW: Handle AssignmentNode for both variable and array assignments
            AssignmentNode assignment = (AssignmentNode) stmt;
            Object value = exprEvaluator.evaluate(assignment.right, obj, locals);

            // Handle slot extraction for method calls with slot casting
            if (assignment.right instanceof MethodCallNode) {
                MethodCallNode methodCall = (MethodCallNode) assignment.right;
                if (methodCall.slotNames != null && !methodCall.slotNames.isEmpty() && value instanceof Map) {
                    Map<String, Object> slotReturns = (Map<String, Object>) value;
                    String slotName = methodCall.slotNames.get(0);
                    if (slotReturns.containsKey(slotName)) {
                        value = slotReturns.get(slotName);
                        DebugSystem.debug("SLOTS", "Extracted slot '" + slotName + "' = " + value + " for assignment");
                    }
                }
            }

            if (assignment.left instanceof IndexAccessNode) {
                // Array element assignment: arr[index] = value
                IndexAccessNode indexAccess = (IndexAccessNode) assignment.left;
                Object arrayObj = exprEvaluator.evaluate(indexAccess.array, obj, locals);
                Object indexObj = exprEvaluator.evaluate(indexAccess.index, obj, locals);

                if (arrayObj instanceof List && indexObj instanceof Integer) {
                    List<Object> list = (List<Object>) arrayObj;
                    int index = (Integer) indexObj;

                    if (index >= 0 && index < list.size()) {
                        list.set(index, value);
                        DebugSystem.debug("ARRAYS", "Array assignment: [" + index + "] = " + value);
                    } else {
                        throw new RuntimeException("Array index out of bounds: " + index + " for array size " + list.size());
                    }
                } else {
                    throw new RuntimeException("Invalid array assignment - array: " + arrayObj.getClass() + ", index: " + indexObj.getClass());
                }
            } else if (assignment.left instanceof ExprNode) {
                // Variable assignment: x = value
                ExprNode target = (ExprNode) assignment.left;
                if (target.name != null) {
                    String varName = target.name;

                    // --- Enforce ~ for slots (NO CHANGE) ---
                    if (slotValues != null && slotValues.containsKey(varName)) {
                        throw new RuntimeException("Cannot assign to slot '" + varName + "' using '='. Use '~ " + varName + " ...' syntax instead.");
                    // --- END ---

                    } else if (locals.containsKey(varName)) { // Check if it's a local variable
                        locals.put(varName, value);
                        DebugSystem.debug("MEMORY", "Assignment to local: " + varName + " = " + value);
                    } else { // Assume it's a field if not a slot or local
                        obj.fields.put(varName, value);
                        DebugSystem.fieldUpdate(varName, value);
                    }
                    // Ensure locals map is updated if it was assigned there
                    if (locals.containsKey(varName)) locals.put(varName, value);

                    DebugSystem.debug("MEMORY", "Assignment: " + varName + " = " + value);
                }
            }

            return value;

        } else if (stmt instanceof FieldNode) {
            FieldNode f = (FieldNode) stmt;

            // ADD DEBUGGING FOR METHOD CALLS IN FIELD ASSIGNMENTS
            if (f.value instanceof MethodCallNode) {
                MethodCallNode methodCall = (MethodCallNode) f.value;
                DebugSystem.debug("FIELD_ASSIGNMENT", "=== FIELD ASSIGNMENT WITH METHOD CALL ===");
                DebugSystem.debug("FIELD_ASSIGNMENT", "Field name: " + f.name);
                // ... (rest of debug lines) ...
            }

            // Regular field assignment (field declarations with initial values)
            Object val = f.value != null ? exprEvaluator.evaluate(f.value, obj, locals) : null;

            // Handle slot extraction in field assignments
            // ... (slot extraction logic) ...

            // --- Enforce ~ for slots (NO CHANGE) ---
            if (slotValues != null && slotValues.containsKey(f.name)) {
                 throw new RuntimeException("Cannot assign to slot '" + f.name + "' using '=' (in field declaration). Use '~> " + f.name + " ...' syntax instead.");
            } else {
                obj.fields.put(f.name, val);
                DebugSystem.fieldUpdate(f.name, val);
            }

            // Field declarations do not create local variables
            // locals.put(f.name, val); // REMOVED
            return val;

        } else if (stmt instanceof VarNode) {
            VarNode var = (VarNode) stmt;
            Object val = var.value != null ? exprEvaluator.evaluate(var.value, obj, locals) : null;

            // Handle slot extraction in variable declarations
             // ... (slot extraction logic) ...

            // Check if variable already exists (shadowing or error?)
            // For now, allow simple shadowing/reassignment in the same scope
            if (locals.containsKey(var.name)) {
                 DebugSystem.warn("SCOPE", "Local variable '" + var.name + "' is being redeclared/shadowed.");
            }
             
            // --- Enforce ~ for slots (NO CHANGE) ---
            if (slotValues != null && slotValues.containsKey(var.name)) {
                throw new RuntimeException("Cannot declare variable '" + var.name + "' because it conflicts with a return slot. Use '~> " + var.name + " ...' to assign to the slot.");
            }
            // --- END ---

            locals.put(var.name, val);
            DebugSystem.debug("MEMORY", "Local variable declared: " + var.name + " = " + val);
            return val;

        } else if (stmt instanceof OutputNode) {
            // ... (OutputNode logic remains the same) ...
             OutputNode output = (OutputNode) stmt;
            Object lastVal = null;

            for (ExprNode arg : output.arguments) {
                if (arg instanceof MethodCallNode) {
                    MethodCallNode methodCall = (MethodCallNode) arg;
                    Object result = interpreter.evalMethodCall(methodCall, obj, locals);

                    if (result instanceof Map) {
                        Map<String, Object> slotReturns = (Map<String, Object>) result;

                        if (output.varName != null) {
                            // Named output: output varName = method()
                            lastVal = slotReturns.get(output.varName);
                        } else {
                            // Direct method call in output: output [slots]:method()
                            if (methodCall.slotNames != null && !methodCall.slotNames.isEmpty()) {
                                // Build output from the specific slots requested
                                if (methodCall.slotNames.size() == 1) {
                                    // Single slot
                                    lastVal = slotReturns.get(methodCall.slotNames.get(0));
                                } else {
                                    // Multiple slots - join with comma space
                                    StringBuilder sb = new StringBuilder();
                                    for (int i = 0; i < methodCall.slotNames.size(); i++) {
                                        if (i > 0) sb.append(", ");
                                        String slotName = methodCall.slotNames.get(i);
                                        Object value = slotReturns.get(slotName);
                                        sb.append(value != null ? value.toString() : "null");
                                    }
                                    lastVal = sb.toString();
                                }
                            } else {
                                // No slot casting - use all slots
                                if (slotReturns.size() == 1) {
                                    lastVal = slotReturns.values().iterator().next();
                                } else {
                                    // Join all slot values
                                    StringBuilder sb = new StringBuilder();
                                    boolean first = true;
                                    for (Object value : slotReturns.values()) {
                                        if (!first) sb.append(", ");
                                        sb.append(value != null ? value.toString() : "null");
                                        first = false;
                                    }
                                    lastVal = sb.toString();
                                }
                            }
                        }
                    } else {
                        lastVal = result;
                    }
                } else {
                    lastVal = exprEvaluator.evaluate(arg, obj, locals);
                }
            }

            ioHandler.output(lastVal);
            return lastVal;

        } else if (stmt instanceof IfNode) {
             // Basic If/Else block scope is handled implicitly by executing statements
             // No explicit map creation/destruction needed here unless 'var' inside if/else
             // should *only* exist there (which would require more complex scope handling)
            IfNode ifn = (IfNode) stmt;
            boolean test = Boolean.TRUE.equals(exprEvaluator.evaluate(ifn.condition, obj, locals));
            DebugSystem.debug("CONTROL", "If condition = " + test);

            Set<String> previousSlotsInPath = new HashSet<>(interpreter.slotsInCurrentPath);

            List<StatementNode> branch = test ? ifn.thenBlock.statements : ifn.elseBlock.statements;
            for (StatementNode s : branch) {
                evalStmt(s, obj, locals, slotValues);
                if (!interpreter.slotsInCurrentPath.isEmpty() && interpreter.shouldReturnEarly(slotValues)) {
                    break;
                }
            }

            interpreter.slotsInCurrentPath = previousSlotsInPath;
            return null;

        } else if (stmt instanceof ForNode) {
            ForNode f = (ForNode) stmt;
            RangeNode range = f.range;
            String iteratorName = f.iterator; // Store iterator name

            Object startObj = exprEvaluator.evaluate(range.start, obj, locals);
            Object endObj = exprEvaluator.evaluate(range.end, obj, locals);
            double start = typeSystem.toDouble(startObj);
            double end = typeSystem.toDouble(endObj);

            Set<String> previousSlotsInPath = new HashSet<>(interpreter.slotsInCurrentPath);

            boolean isTransformativeStep = isTransformativeStep(range.step, iteratorName);
            DebugSystem.debug("FOR_LOOP", "Transformative step detected: " + isTransformativeStep + " for step: " + (range.step != null ? range.step.getClass().getSimpleName() : "null"));

            double stepValue = 1.0;
            if (!isTransformativeStep) {
                if (range.step == null) {
                    stepValue = (start > end) ? -1.0 : 1.0;
                    DebugSystem.debug("FOR_LOOP", "Smart default step applied: " + stepValue);
                } else {
                    Map<String, Object> tempLocals = new HashMap<>(locals);
                    // *** CRITICAL: Use iteratorName consistently ***
                    tempLocals.put(iteratorName, start); // Use start value for initial step eval
                    Object stepObj = exprEvaluator.evaluate(range.step, obj, tempLocals);
                    stepValue = typeSystem.toDouble(stepObj);
                }

                if (stepValue == 0) {
                    throw new RuntimeException("Step cannot be zero in for loop. Potentially cause an infinite loop.");
                }
                if ((start < end && stepValue < 0) || (start > end && stepValue > 0)) {
                    throw new RuntimeException("Step direction would cause infinite loop: start=" +
                                             start + ", end=" + end + ", step=" + stepValue);
                }
            }

            // --- SCOPE HANDLING: Save previous value and check existence ---
            Object originalValue = null;
            boolean existedBefore = locals.containsKey(iteratorName);
            if (existedBefore) {
                originalValue = locals.get(iteratorName);
                DebugSystem.debug("SCOPE", "Saving existing value of '" + iteratorName + "': " + originalValue);
            } else {
                DebugSystem.debug("SCOPE", "Iterator '" + iteratorName + "' is new to this scope.");
            }

            // Initialize the iterator for the loop
            double current = start;
            locals.put(iteratorName, (int) current);
            DebugSystem.debug("SCOPE", "Initialized loop iterator '" + iteratorName + "' = " + (int)current);

            // Use try-finally to ensure scope restoration
            try {
                // Remove maxIterations check for final version
                // int iteration = 0;
                // long maxIterations = 50;

                // while (iteration < maxIterations) {
                while(true) { // Loop indefinitely until break
                    // iteration++; // Remove if maxIterations is removed

                    boolean shouldTerminate;
                    double effectiveStepDirection = isTransformativeStep ? (end >= start ? 1.0 : -1.0) : stepValue;

                    if (effectiveStepDirection > 0) { // Counting up
                        shouldTerminate = current > end;
                    } else { // Counting down
                        shouldTerminate = current < end;
                    }

                    if (shouldTerminate) {
                         DebugSystem.debug("SCOPE", "Loop termination condition met for '" + iteratorName + "'.");
                        break; // Exit the while loop
                    }

                    // --- Execute Body ---
                    // Create a *copy* of locals for the body to prevent modifications
                    // IF strict block scoping for variables declared *inside* the loop is needed.
                    // For now, allow body to modify outer locals, but the iterator itself is managed.
                     DebugSystem.debug("SCOPE", "Entering loop body for '" + iteratorName + "' = " + (int)current);
                    for (StatementNode s : f.body.statements) {
                        evalStmt(s, obj, locals, slotValues); // Pass the main locals map
                        if (!interpreter.slotsInCurrentPath.isEmpty() && interpreter.shouldReturnEarly(slotValues)) {
                             DebugSystem.debug("SCOPE", "Early return triggered inside loop for '" + iteratorName + "'.");
                            break;
                        }
                    }
                    if (!interpreter.slotsInCurrentPath.isEmpty() && interpreter.shouldReturnEarly(slotValues)) {
                         DebugSystem.debug("SCOPE", "Propagating early return break for '" + iteratorName + "'.");
                        break; // Break the outer while loop as well
                    }
                    DebugSystem.debug("SCOPE", "Exiting loop body for '" + iteratorName + "' = " + (int)current);


                    // --- Apply Step ---
                    if (isTransformativeStep) {
                        Map<String, Object> tempLocals = new HashMap<>(locals);
                        // *** CRITICAL: Use iteratorName consistently ***
                        tempLocals.put(iteratorName, current);

                        Object stepObj = exprEvaluator.evaluate(range.step, obj, tempLocals);
                        double newValue = typeSystem.toDouble(stepObj);

                        DebugSystem.debug("FOR_LOOP", "Transformative step: " + current + " -> " + newValue);

                        if (newValue == current && current != end) {
                            throw new RuntimeException("Loop step expression resulted in no change, causing infinite loop: " + current + " -> " + newValue);
                        }
                        current = newValue;
                    } else {
                        DebugSystem.debug("FOR_LOOP", "Additive step: " + current + " + " + stepValue + " = " + (current + stepValue));
                        current += stepValue;
                    }

                    // --- Update Iterator within Loop Scope ---
                     // *** CRITICAL: Use iteratorName consistently ***
                    locals.put(iteratorName, (int) current);
                     DebugSystem.debug("SCOPE", "Updated loop iterator '" + iteratorName + "' = " + (int)current);

                } // End while loop

                // Remove maxIterations check
                // if (iteration >= maxIterations) {
                //    throw new RuntimeException("For loop exceeded maximum iteration count (" + maxIterations + ") - possible infinite loop");
                // }

            } finally {
                // --- SCOPE HANDLING: Restore old state ---
                if (existedBefore) {
                    locals.put(iteratorName, originalValue);
                    DebugSystem.debug("SCOPE", "Restored '" + iteratorName + "' to previous value: " + originalValue);
                } else {
                    locals.remove(iteratorName);
                    DebugSystem.debug("SCOPE", "Removed loop-specific iterator '" + iteratorName + "'.");
                }
            }

            interpreter.slotsInCurrentPath = previousSlotsInPath;
            return null;

        } else if (stmt instanceof MethodCallNode) {
            // ... (MethodCallNode as statement remains the same) ...
             MethodCallNode call = (MethodCallNode) stmt;
            Object result = interpreter.evalMethodCall(call, obj, locals);

            // Handle slot assignments if the call is used as a statement
            if (call.slotNames != null && !call.slotNames.isEmpty() && result instanceof Map) {
                Map<String, Object> slotReturns = (Map<String, Object>) result;
                DebugSystem.debug("SLOTS", "Statement method call returned slots: " + slotReturns);

                for (String slotName : call.slotNames) {
                    if (slotReturns.containsKey(slotName)) {
                        Object slotValue = slotReturns.get(slotName);
                        // Assign ONLY to slots declared in the *current* method scope
                        if (slotValues != null && slotValues.containsKey(slotName)) {
                            Object oldValue = slotValues.get(slotName);
                            slotValues.put(slotName, slotValue);
                            DebugSystem.slotUpdate(slotName, oldValue, slotValue);
                        } else {
                            // Don't create local vars from slots automatically
                            DebugSystem.debug("SLOTS", "Slot '" + slotName + "' from statement call ignored (not a local slot).");
                        }
                    }
                }
            }
            return result; // Statement calls still return a value, even if unused
    } else if (stmt instanceof ReturnSlotAssignmentNode) {
     ReturnSlotAssignmentNode assignment = (ReturnSlotAssignmentNode) stmt;

    Object result = interpreter.evalMethodCall(assignment.methodCall, obj, locals);

    if (result instanceof Map) {
        Map<String, Object> slotReturns = (Map<String, Object>) result;

        for (int i = 0; i < assignment.variableNames.size(); i++) {
            String varName = assignment.variableNames.get(i);
            String slotName = assignment.methodCall.slotNames.get(i);

            if (slotReturns.containsKey(slotName)) {
                Object slotValue = slotReturns.get(slotName);
                
                if (slotValues != null && slotValues.containsKey(varName)) {
                     throw new RuntimeException("Cannot assign to variable '" + varName + "' because it conflicts with a return slot.");
                }

                if (locals.containsKey(varName)) {
                   DebugSystem.warn("SCOPE", "Variable '" + varName + "' from slot assignment is shadowing/reassigning existing local.");
                }
                locals.put(varName, slotValue);
                DebugSystem.debug("SLOTS", "Assigned slot '" + slotName + "' to local '" + varName + "' = " + slotValue);
            } else {
                DebugSystem.warn("SLOTS", "Slot '" + slotName + "' not found in method return for assignment to '" + varName + "'.");
                locals.put(varName, null);
            }
        }
    }
    return result;
    }else if (stmt instanceof SlotAssignmentNode) {
    SlotAssignmentNode assign = (SlotAssignmentNode) stmt;
    Object value = exprEvaluator.evaluate(assign.value, obj, locals);
    String varName = assign.slotName;

    // --- NEW: Handle implicit returns ---
    if ("return".equals(varName)) {
        // This is an implicit return from a single-expression method
        if (slotValues != null && !slotValues.isEmpty()) {
            // Assign to the first declared slot (most common case for single return)
            String firstSlot = slotValues.keySet().iterator().next();
            Object oldValue = slotValues.get(firstSlot);
            slotValues.put(firstSlot, value);
            DebugSystem.slotUpdate(firstSlot, oldValue, value);
            DebugSystem.debug("RETURN", "Implicit return assigned to slot: " + firstSlot + " = " + value);
        } else {
            DebugSystem.warn("RETURN", "Implicit return ignored - no slots declared");
        }
        return value;
    }
    
    // Regular slot assignment
    if (slotValues != null && slotValues.containsKey(varName)) {
        Object oldValue = slotValues.get(varName);
        slotValues.put(varName, value);
        DebugSystem.slotUpdate(varName, oldValue, value);
    } else {
         DebugSystem.warn("SLOTS", "Assignment to '" + varName + "' ignored (not a declared slot).");
    }
    return value;
} else if (stmt instanceof MultipleSlotAssignmentNode) {
    MultipleSlotAssignmentNode multiAssign = (MultipleSlotAssignmentNode) stmt;
    
    // Validate assignment count matches declared slots
    List<String> declaredSlots = new ArrayList<String>();
    if (slotValues != null) {
        declaredSlots.addAll(slotValues.keySet());
    }
    
    if (multiAssign.assignments.size() != declaredSlots.size()) {
        throw new RuntimeException("Return assignment count mismatch: declared " + 
                                  declaredSlots.size() + " slots but assigned " + 
                                  multiAssign.assignments.size() + " values");
    }
    
    // Count named assignments
    int namedCount = 0;
    List<String> assignedNames = new ArrayList<String>();
    for (SlotAssignmentNode assign : multiAssign.assignments) {
        if (assign.slotName != null) {
            namedCount++;
            assignedNames.add(assign.slotName);
        }
    }
    
    // Handle mixed assignments - validate names match declared slots
    if (namedCount > 0 && namedCount < multiAssign.assignments.size()) {
        // Mixed assignments - validate all named ones match declared slots
        for (String assignedName : assignedNames) {
            if (!declaredSlots.contains(assignedName)) {
                throw new RuntimeException("Assignment to undeclared slot: " + assignedName);
            }
        }
        
        // Execute mixed assignments - named ones by name, unnamed ones by position
        Object lastValue = null;
        int unnamedIndex = 0;
        
        for (SlotAssignmentNode assign : multiAssign.assignments) {
            Object value = exprEvaluator.evaluate(assign.value, obj, locals);
            
            if (assign.slotName != null) {
                // Named assignment
                if (slotValues != null && slotValues.containsKey(assign.slotName)) {
                    Object oldValue = slotValues.get(assign.slotName);
                    slotValues.put(assign.slotName, value);
                    DebugSystem.slotUpdate(assign.slotName, oldValue, value);
                }
            } else {
                // Unnamed assignment - assign by position
                if (unnamedIndex < declaredSlots.size()) {
                    String slotName = declaredSlots.get(unnamedIndex);
                    // Skip if this slot was already assigned by name
                    while (unnamedIndex < declaredSlots.size() && 
                           assignedNames.contains(declaredSlots.get(unnamedIndex))) {
                        unnamedIndex++;
                    }
                    if (unnamedIndex < declaredSlots.size()) {
                        slotName = declaredSlots.get(unnamedIndex);
                        if (slotValues != null && slotValues.containsKey(slotName)) {
                            Object oldValue = slotValues.get(slotName);
                            slotValues.put(slotName, value);
                            DebugSystem.slotUpdate(slotName, oldValue, value);
                        }
                        unnamedIndex++;
                    }
                }
            }
            lastValue = value;
        }
        
        return lastValue;
    }
    
    // Original logic for all-named or all-unnamed assignments
    Object lastValue = null;
    if (namedCount == 0) {
        // All unnamed - assign in declaration order
        for (int i = 0; i < multiAssign.assignments.size(); i++) {
            SlotAssignmentNode assign = multiAssign.assignments.get(i);
            String slotName = declaredSlots.get(i);
            Object value = exprEvaluator.evaluate(assign.value, obj, locals);
            
            if (slotValues != null && slotValues.containsKey(slotName)) {
                Object oldValue = slotValues.get(slotName);
                slotValues.put(slotName, value);
                DebugSystem.slotUpdate(slotName, oldValue, value);
            }
            lastValue = value;
        }
    } else {
        // All named - assign by name
        for (SlotAssignmentNode assign : multiAssign.assignments) {
            Object value = exprEvaluator.evaluate(assign.value, obj, locals);
            
            if (slotValues != null && slotValues.containsKey(assign.slotName)) {
                Object oldValue = slotValues.get(assign.slotName);
                slotValues.put(assign.slotName, value);
                DebugSystem.slotUpdate(assign.slotName, oldValue, value);
            }
            lastValue = value;
        }
    }
    
    return lastValue;
    } else if (stmt instanceof ExprNode) {
             // Evaluate expression as a statement (value discarded unless it's a method call)
            return exprEvaluator.evaluate((ExprNode) stmt, obj, locals);
        }

        return null; // Default return for statements
    }

    // --- Helper Methods ---
    // (isTransformativeStep, containsIteratorVariable, isIdentityOperation, isIterator, isLiteralOne, isAssignmentOperator remain the same)

    private boolean isTransformativeStep(ExprNode stepExpr, String iterator) {
        if (stepExpr == null) return false;
        if (stepExpr instanceof BinaryOpNode) {
            BinaryOpNode binOp = (BinaryOpNode) stepExpr;
            if (isAssignmentOperator(binOp.op)) {
                return isIterator(binOp.left, iterator);
            } else {
                return containsIteratorVariable(binOp.left, iterator) ||
                       containsIteratorVariable(binOp.right, iterator);
            }
        } else if (stepExpr instanceof UnaryNode) {
            return containsIteratorVariable(((UnaryNode)stepExpr).operand, iterator);
        } else if (stepExpr.name != null) {
            // 'by steps' is additive, 'by i' is transformative
            return stepExpr.name.equals(iterator);
        }
        return false;
    }

    private boolean containsIteratorVariable(ExprNode stepExpr, String iterator) {
        if (stepExpr == null) return false;
        if (stepExpr instanceof BinaryOpNode) {
            BinaryOpNode binOp = (BinaryOpNode) stepExpr;
            return containsIteratorVariable(binOp.left, iterator) || containsIteratorVariable(binOp.right, iterator);
        } else if (stepExpr instanceof UnaryNode) {
            return containsIteratorVariable(((UnaryNode)stepExpr).operand, iterator);
        } else if (stepExpr.name != null) {
            return stepExpr.name.equals(iterator);
        }
        return false;
    }

    private boolean isIdentityOperation(ExprNode stepExpr, String iterator) {
        if (stepExpr instanceof BinaryOpNode) {
            BinaryOpNode binOp = (BinaryOpNode) stepExpr;
            if (binOp.op.equals("*") || binOp.op.equals("/")) {
                return (isIterator(binOp.left, iterator) && isLiteralOne(binOp.right)) ||
                       (isIterator(binOp.right, iterator) && isLiteralOne(binOp.left));
            }
        } else if (stepExpr instanceof UnaryNode) {
            UnaryNode unary = (UnaryNode) stepExpr;
            if (unary.op.equals("+")) {
                return isIterator(unary.operand, iterator);
            }
        }
        return false;
    }

    private boolean isIterator(ExprNode expr, String iterator) {
        return expr != null && expr.name != null && expr.name.equals(iterator);
    }

    private boolean isLiteralOne(ExprNode expr) {
        if (expr != null && expr.value != null) {
            if (expr.value instanceof Integer) {
                return ((Integer)expr.value) == 1;
            } else if (expr.value instanceof Double) {
                return ((Double)expr.value) == 1.0;
            }
        } else if (expr instanceof UnaryNode) {
            UnaryNode unary = (UnaryNode) expr;
            if (unary.op.equals("+")) {
                return isLiteralOne(unary.operand);
            }
        }
        return false;
    }

    private boolean isAssignmentOperator(String op) {
        return op.equals("=") || op.equals("+=") || op.equals("-=") ||
               op.equals("*=") || op.equals("/=");
    }
}