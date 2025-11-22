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

        if (stmt instanceof SlotAssignmentNode) {
            SlotAssignmentNode assign = (SlotAssignmentNode) stmt;
            Object value = exprEvaluator.evaluate(assign.value, obj, locals);
            String varName = assign.slotName;

            // --- NEW: Handle implicit returns ---
            if ("return".equals(varName)) {
                if (slotValues != null && !slotValues.isEmpty()) {
                    // Assign to the first declared slot
                    String firstSlot = slotValues.keySet().iterator().next();
                    
                    // Validate Type
                    validateSlotType(firstSlot, value);
                    
                    Object oldValue = slotValues.get(firstSlot);
                    slotValues.put(firstSlot, value);
                    DebugSystem.slotUpdate(firstSlot, oldValue, value);
                }
                return value;
            }
            
            // Check if slot exists
            if (slotValues != null && slotValues.containsKey(varName)) {
                // Enforce: If slot name is auto-generated (starts with digit), user cannot assign to it by name
                if (Character.isDigit(varName.charAt(0))) {
                     throw new RuntimeException("Cannot assign to unnamed slot '" + varName + "' by name. Use positional assignment.");
                }

                // Validate Type
                validateSlotType(varName, value);

                Object oldValue = slotValues.get(varName);
                slotValues.put(varName, value);
                DebugSystem.slotUpdate(varName, oldValue, value);
            } else {
                 // CHANGED: Warn -> Exception
                 throw new RuntimeException("Assignment to '" + varName + "' failed: Slot is not declared in method signature.");
            }
            return value;

        } else if (stmt instanceof MultipleSlotAssignmentNode) {
            MultipleSlotAssignmentNode multiAssign = (MultipleSlotAssignmentNode) stmt;
            
            // Validate assignment count matches declared slots
            List<String> declaredSlots = new ArrayList<String>();
            if (slotValues != null) {
                // Sort keys if they look like indices "0", "1" to ensure positional correctness
                List<String> keys = new ArrayList<>(slotValues.keySet());
                Collections.sort(keys, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                         if (Character.isDigit(o1.charAt(0)) && Character.isDigit(o2.charAt(0))) {
                             try {
                                 return Integer.valueOf(o1).compareTo(Integer.valueOf(o2));
                             } catch (NumberFormatException e) {}
                         }
                         return o1.compareTo(o2);
                    }
                });
                declaredSlots.addAll(keys);
            }
            
            if (multiAssign.assignments.size() > declaredSlots.size()) {
                throw new RuntimeException("Too many return assignments: declared " + 
                                          declaredSlots.size() + " slots but assigned " + 
                                          multiAssign.assignments.size() + " values");
            }
            
            Object lastValue = null;
            int slotIndex = 0;

            for (SlotAssignmentNode assign : multiAssign.assignments) {
                Object value = exprEvaluator.evaluate(assign.value, obj, locals);
                String targetSlotName = null;

                if (assign.slotName != null) {
                    // Named assignment
                    targetSlotName = assign.slotName;
                } else {
                    // Positional assignment
                    if (slotIndex < declaredSlots.size()) {
                        targetSlotName = declaredSlots.get(slotIndex);
                    }
                }

                if (targetSlotName != null && slotValues.containsKey(targetSlotName)) {
                    // Enforce restriction: If doing Named assignment to Unnamed slot
                    if (assign.slotName != null && Character.isDigit(targetSlotName.charAt(0))) {
                         throw new RuntimeException("Cannot use named assignment '" + assign.slotName + "' for an unnamed slot.");
                    }
                    
                    // Validate Type
                    validateSlotType(targetSlotName, value);

                    Object oldValue = slotValues.get(targetSlotName);
                    slotValues.put(targetSlotName, value);
                    DebugSystem.slotUpdate(targetSlotName, oldValue, value);
                } else {
                     throw new RuntimeException("Slot '" + (targetSlotName != null ? targetSlotName : "index " + slotIndex) + "' not found.");
                }
                
                lastValue = value;
                slotIndex++;
            }
            
            return lastValue;
            
        } else if (stmt instanceof InputNode) {
            // Handle input: name = (type) input
            InputNode input = (InputNode) stmt;
            Object inputValue = ioHandler.readInput(input.targetType);

            if (slotValues != null && slotValues.containsKey(input.variableName)) {
                 throw new RuntimeException("Cannot assign to slot '" + input.variableName + "' using '=' (from input). Use '~> " + input.variableName + " ...' syntax instead.");
            } else {
                obj.fields.put(input.variableName, inputValue);
                DebugSystem.fieldUpdate(input.variableName, inputValue);
            }
            locals.put(input.variableName, inputValue);
            return inputValue;

        } else if (stmt instanceof AssignmentNode) {
            AssignmentNode assignment = (AssignmentNode) stmt;
            Object value = exprEvaluator.evaluate(assignment.right, obj, locals);

            if (assignment.right instanceof MethodCallNode) {
                MethodCallNode methodCall = (MethodCallNode) assignment.right;
                if (methodCall.slotNames != null && !methodCall.slotNames.isEmpty() && value instanceof Map) {
                    Map<String, Object> slotReturns = (Map<String, Object>) value;
                    String slotName = methodCall.slotNames.get(0);
                    
                    // Handle auto-numbered slots in calls: [0]:method()
                    if (!slotReturns.containsKey(slotName) && slotReturns.containsKey("0")) {
                         // If user asked for [name] but we have [0], checks fail unless mapped.
                         // But per requirements, user should use [0] if unnamed.
                    }
                    
                    if (slotReturns.containsKey(slotName)) {
                        value = slotReturns.get(slotName);
                    }
                }
            }

            if (assignment.left instanceof IndexAccessNode) {
                IndexAccessNode indexAccess = (IndexAccessNode) assignment.left;
                Object arrayObj = exprEvaluator.evaluate(indexAccess.array, obj, locals);
                Object indexObj = exprEvaluator.evaluate(indexAccess.index, obj, locals);

                if (arrayObj instanceof List && indexObj instanceof Integer) {
                    List<Object> list = (List<Object>) arrayObj;
                    int index = (Integer) indexObj;

                    if (index >= 0 && index < list.size()) {
                        list.set(index, value);
                    } else {
                        throw new RuntimeException("Array index out of bounds: " + index + " for array size " + list.size());
                    }
                }
            } else if (assignment.left instanceof ExprNode) {
                ExprNode target = (ExprNode) assignment.left;
                if (target.name != null) {
                    String varName = target.name;

                    if (slotValues != null && slotValues.containsKey(varName)) {
                        throw new RuntimeException("Cannot assign to slot '" + varName + "' using '='. Use '~ " + varName + " ...' syntax instead.");
                    } else if (locals.containsKey(varName)) { 
                        locals.put(varName, value);
                    } else { 
                        obj.fields.put(varName, value);
                    }
                    if (locals.containsKey(varName)) locals.put(varName, value);
                }
            }

            return value;

        } else if (stmt instanceof FieldNode) {
            FieldNode f = (FieldNode) stmt;
            Object val = f.value != null ? exprEvaluator.evaluate(f.value, obj, locals) : null;

            if (slotValues != null && slotValues.containsKey(f.name)) {
                 throw new RuntimeException("Cannot assign to slot '" + f.name + "' using '=' (in field declaration). Use '~> " + f.name + " ...' syntax instead.");
            } else {
                obj.fields.put(f.name, val);
            }
            return val;

        } else if (stmt instanceof VarNode) {
            VarNode var = (VarNode) stmt;
            Object val = var.value != null ? exprEvaluator.evaluate(var.value, obj, locals) : null;

            if (slotValues != null && slotValues.containsKey(var.name)) {
                throw new RuntimeException("Cannot declare variable '" + var.name + "' because it conflicts with a return slot. Use '~> " + var.name + " ...' to assign to the slot.");
            }

            locals.put(var.name, val);
            return val;

        } else if (stmt instanceof OutputNode) {
             OutputNode output = (OutputNode) stmt;
            Object lastVal = null;

            for (ExprNode arg : output.arguments) {
                if (arg instanceof MethodCallNode) {
                    MethodCallNode methodCall = (MethodCallNode) arg;
                    Object result = interpreter.evalMethodCall(methodCall, obj, locals);

                    if (result instanceof Map) {
                        Map<String, Object> slotReturns = (Map<String, Object>) result;

                        if (output.varName != null) {
                            lastVal = slotReturns.get(output.varName);
                        } else {
                            if (methodCall.slotNames != null && !methodCall.slotNames.isEmpty()) {
                                if (methodCall.slotNames.size() == 1) {
                                    lastVal = slotReturns.get(methodCall.slotNames.get(0));
                                } else {
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
                                if (slotReturns.size() == 1) {
                                    lastVal = slotReturns.values().iterator().next();
                                } else {
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
            IfNode ifn = (IfNode) stmt;
            boolean test = Boolean.TRUE.equals(exprEvaluator.evaluate(ifn.condition, obj, locals));

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
            String iteratorName = f.iterator;

            Object startObj = exprEvaluator.evaluate(range.start, obj, locals);
            Object endObj = exprEvaluator.evaluate(range.end, obj, locals);
            double start = typeSystem.toDouble(startObj);
            double end = typeSystem.toDouble(endObj);

            Set<String> previousSlotsInPath = new HashSet<>(interpreter.slotsInCurrentPath);

            boolean isTransformativeStep = isTransformativeStep(range.step, iteratorName);
            double stepValue = 1.0;
            if (!isTransformativeStep) {
                if (range.step == null) {
                    stepValue = (start > end) ? -1.0 : 1.0;
                } else {
                    Map<String, Object> tempLocals = new HashMap<>(locals);
                    tempLocals.put(iteratorName, start); 
                    Object stepObj = exprEvaluator.evaluate(range.step, obj, tempLocals);
                    stepValue = typeSystem.toDouble(stepObj);
                }

                if (stepValue == 0) {
                    throw new RuntimeException("Step cannot be zero in for loop.");
                }
            }

            Object originalValue = null;
            boolean existedBefore = locals.containsKey(iteratorName);
            if (existedBefore) {
                originalValue = locals.get(iteratorName);
            }

            double current = start;
            locals.put(iteratorName, (int) current);

            try {
                while(true) {
                    boolean shouldTerminate;
                    double effectiveStepDirection = isTransformativeStep ? (end >= start ? 1.0 : -1.0) : stepValue;

                    if (effectiveStepDirection > 0) { 
                        shouldTerminate = current > end;
                    } else { 
                        shouldTerminate = current < end;
                    }

                    if (shouldTerminate) break;

                    for (StatementNode s : f.body.statements) {
                        evalStmt(s, obj, locals, slotValues); 
                        if (!interpreter.slotsInCurrentPath.isEmpty() && interpreter.shouldReturnEarly(slotValues)) {
                            break;
                        }
                    }
                    if (!interpreter.slotsInCurrentPath.isEmpty() && interpreter.shouldReturnEarly(slotValues)) {
                        break; 
                    }

                    if (isTransformativeStep) {
                        Map<String, Object> tempLocals = new HashMap<>(locals);
                        tempLocals.put(iteratorName, current);
                        Object stepObj = exprEvaluator.evaluate(range.step, obj, tempLocals);
                        double newValue = typeSystem.toDouble(stepObj);
                        if (newValue == current && current != end) {
                            throw new RuntimeException("Loop step expression resulted in no change, causing infinite loop");
                        }
                        current = newValue;
                    } else {
                        current += stepValue;
                    }
                    locals.put(iteratorName, (int) current);
                } 

            } finally {
                if (existedBefore) {
                    locals.put(iteratorName, originalValue);
                } else {
                    locals.remove(iteratorName);
                }
            }

            interpreter.slotsInCurrentPath = previousSlotsInPath;
            return null;

        } else if (stmt instanceof MethodCallNode) {
             MethodCallNode call = (MethodCallNode) stmt;
            Object result = interpreter.evalMethodCall(call, obj, locals);

            if (call.slotNames != null && !call.slotNames.isEmpty() && result instanceof Map) {
                Map<String, Object> slotReturns = (Map<String, Object>) result;

                for (String slotName : call.slotNames) {
                    if (slotReturns.containsKey(slotName)) {
                        Object slotValue = slotReturns.get(slotName);
                        if (slotValues != null && slotValues.containsKey(slotName)) {
                            validateSlotType(slotName, slotValue); // Validate Type
                            Object oldValue = slotValues.get(slotName);
                            slotValues.put(slotName, slotValue);
                            DebugSystem.slotUpdate(slotName, oldValue, slotValue);
                        }
                    }
                }
            }
            return result; 
            
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

                        locals.put(varName, slotValue);
                    } else {
                        // Handle named assignment to unnamed slot index
                         if (slotReturns.containsKey("0") && Character.isDigit(slotName.charAt(0))) {
                             // Fallback? No, user must call by index if slots are unnamed
                         }
                         // CHANGED: Warn -> Exception
                        throw new RuntimeException("Runtime Error: Slot '" + slotName + "' requested but not found in method return values.");
                    }
                }
            }
            return result;
            
        } else if (stmt instanceof ExprNode) {
            return exprEvaluator.evaluate((ExprNode) stmt, obj, locals);
        }

        return null; 
    }
    
    // --- VALIDATION HELPER ---
    private void validateSlotType(String slotName, Object value) {
        if (interpreter.currentSlotTypes == null || !interpreter.currentSlotTypes.containsKey(slotName)) {
            return; // Should not happen if logic is correct
        }
        
        String expectedType = interpreter.currentSlotTypes.get(slotName);
        if (value == null) return; // Null allowed?
        
        boolean isValid = false;
        if (expectedType.equals("int")) {
            isValid = value instanceof Integer;
        } else if (expectedType.equals("float")) {
            isValid = value instanceof Float || value instanceof Double;
        } else if (expectedType.equals("string")) {
            isValid = value instanceof String;
        } else if (expectedType.equals("bool")) {
            isValid = value instanceof Boolean;
        } else {
            // For custom class types, we might check instance type name
            // Since we don't have full class metadata here easily, we might relax or check map
            if (value instanceof ObjectInstance) {
                isValid = ((ObjectInstance)value).type.name.equals(expectedType);
            } else {
                isValid = true; // Allow custom types for now if not primitive
            }
        }
        
        if (!isValid) {
            throw new RuntimeException("Type mismatch for slot '" + slotName + 
                                     "'. Expected " + expectedType + 
                                     " but got " + value.getClass().getSimpleName());
        }
    }

    // --- Helper Methods ---

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

    private boolean isIterator(ExprNode expr, String iterator) {
        return expr != null && expr.name != null && expr.name.equals(iterator);
    }

    private boolean isAssignmentOperator(String op) {
        return op.equals("=") || op.equals("+=") || op.equals("-=") ||
               op.equals("*=") || op.equals("/=");
    }
}