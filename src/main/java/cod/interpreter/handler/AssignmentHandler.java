package cod.interpreter.handler;

import cod.ast.nodes.*;
import cod.error.InternalError;
import cod.error.ProgramError;
import cod.range.NaturalArray;
import cod.range.MultiRangeSpec;
import cod.range.RangeSpec;
import cod.interpreter.context.ExecutionContext;
import cod.interpreter.*;
import cod.semantic.ConstructorResolver;

import java.util.*;

public class AssignmentHandler {
    private final TypeHandler typeSystem;
    private final Interpreter interpreter;
    private final ExpressionHandler expressionHandler;
    private final InterpreterVisitor dispatcher;
    
    public AssignmentHandler(TypeHandler typeSystem, Interpreter interpreter, 
                           ExpressionHandler expressionHandler, InterpreterVisitor dispatcher) {
        if (typeSystem == null) {
            throw new InternalError("AssignmentHandler constructed with null typeSystem");
        }
        if (interpreter == null) {
            throw new InternalError("AssignmentHandler constructed with null interpreter");
        }
        if (expressionHandler == null) {
            throw new InternalError("AssignmentHandler constructed with null expressionHandler");
        }
        if (dispatcher == null) {
            throw new InternalError("AssignmentHandler constructed with null dispatcher");
        }
        
        this.typeSystem = typeSystem;
        this.interpreter = interpreter;
        this.expressionHandler = expressionHandler;
        this.dispatcher = dispatcher;
    }
    
    // === Core Assignment Logic ===
    
    public Object handleAssignment(AssignmentNode node, ExecutionContext ctx) {
        if (node == null) {
            throw new InternalError("handleAssignment called with null node");
        }
        if (ctx == null) {
            throw new InternalError("handleAssignment called with null context");
        }
        
        try {
            Object newValue = dispatcher.dispatch(node.right);
            
            if (node.left instanceof IndexAccessNode) {
                return handleIndexAssignment((IndexAccessNode) node.left, newValue, ctx);
            } else if (node.left instanceof ExprNode) {
                return handleVariableAssignment((ExprNode) node.left, newValue, ctx);
            }
            
            throw new ProgramError("Invalid assignment target");
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Assignment failed", e);
        }
    }
    
    // In AssignmentHandler.java - optimized slot handling

public Object handleSlotAssignment(SlotAssignmentNode node, ExecutionContext ctx) {
    if (node == null) {
        throw new InternalError("handleSlotAssignment called with null node");
    }
    if (ctx == null) {
        throw new InternalError("handleSlotAssignment called with null context");
    }
    
    try {
        Object value = dispatcher.dispatch(node.value);
        String varName = node.slotName;
        
        if (varName != null && !varName.isEmpty() && !"_".equals(varName)) {
            return assignToSlot(varName, value, ctx);
        } else {
            // Implicit slot - use first available slot
            if (ctx.getSlotCount() > 0) {
                String slotTarget = ctx.getSlotName(0); // O(1) with new optimization
                return assignToSlot(slotTarget, value, ctx);
            } else {
                return handleRegularAssignment(varName, value, ctx);
            }
        }
    } catch (ProgramError e) {
        throw e;
    } catch (Exception e) {
        throw new InternalError("Slot assignment failed", e);
    }
}

public Object handleMultipleSlotAssignment(MultipleSlotAssignmentNode node, ExecutionContext ctx) {
    if (node == null) {
        throw new InternalError("handleMultipleSlotAssignment called with null node");
    }
    if (ctx == null) {
        throw new InternalError("handleMultipleSlotAssignment called with null context");
    }
    
    try {
        Object lastValue = null;
        int slotIndex = 0;
        
        for (SlotAssignmentNode assign : node.assignments) {
            Object value = dispatcher.dispatch(assign.value);
            String target = determineTargetOptimized(assign, ctx, slotIndex);
            
            assignToSlot(target, value, ctx);
            lastValue = value;
            slotIndex++;
        }
        return lastValue;
    } catch (ProgramError e) {
        throw e;
    } catch (Exception e) {
        throw new InternalError("Multiple slot assignment failed", e);
    }
}

// NEW: O(1) target determination
private String determineTargetOptimized(SlotAssignmentNode assign, ExecutionContext ctx, int slotIndex) {
    String varName = assign.slotName;
    
    if (varName != null && !varName.isEmpty() && !"_".equals(varName)) {
        return varName;
    } else {
        if (slotIndex < ctx.getSlotCount()) {
            return ctx.getSlotName(slotIndex); // O(1)
        } else {
            throw new ProgramError("Too many positional slot assignments. Expected at most " + 
                ctx.getSlotCount() + ", got " + (slotIndex + 1));
        }
    }
}

// NEW: O(1) slot assignment
private Object assignToSlot(String slotTarget, Object value, ExecutionContext ctx) {
    try {
        if (ctx.hasSlot(slotTarget)) { // O(1)
            String declaredType = ctx.getSlotType(slotTarget); // O(1)
            validateAssignmentType(declaredType, value, slotTarget);
            
            value = typeSystem.wrapUnionType(value, declaredType);
            
            ctx.setSlotValue(slotTarget, value); // O(1)
            ctx.markSlotAssigned(slotTarget); // O(1)
            return value;
        } else {
            throw new ProgramError("Assignment to slot '" + slotTarget + "' failed: slot not declared");
        }
    } catch (ProgramError e) {
        throw e;
    } catch (Exception e) {
        throw new InternalError("Slot assignment failed for: " + slotTarget, e);
    }
}
    
    // === Assignment Implementation Methods ===
    
    @SuppressWarnings("unchecked")
    private Object handleIndexAssignment(IndexAccessNode indexAccess, Object newValue, ExecutionContext ctx) {
        try {
            Object arrayObj = dispatcher.dispatch(indexAccess.array);
            arrayObj = typeSystem.unwrap(arrayObj);
            Object indexObj = dispatcher.dispatch(indexAccess.index);
            indexObj = typeSystem.unwrap(indexObj);
            
            if (indexObj instanceof RangeSpec) {
                return assignRange(arrayObj, (RangeSpec) indexObj, newValue);
            }
            
            if (indexObj instanceof MultiRangeSpec) {
                return assignMultiRange(arrayObj, (MultiRangeSpec) indexObj, newValue);
            }
            
            if (arrayObj instanceof NaturalArray) {
                NaturalArray natural = (NaturalArray) arrayObj;
                long index = expressionHandler.toLongIndex(indexObj);
                natural.set(index, newValue);
                return newValue;
            }
            
            if (arrayObj instanceof List) {
                int intIndex = expressionHandler.toIntIndex(indexObj);
                List<Object> list = (List<Object>) arrayObj;
                list.set(intIndex, newValue);
                return newValue;
            }
            
            throw new ProgramError("Invalid array assignment target: " +
                (arrayObj != null ? arrayObj.getClass().getSimpleName() : "null"));
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Index assignment failed", e);
        }
    }
    
    private Object handleVariableAssignment(ExprNode target, Object newValue, ExecutionContext ctx) {
        try {
            if (target instanceof PropertyAccessNode) {
                PropertyAccessNode prop = (PropertyAccessNode) target;
                
                if (prop.left instanceof ThisNode) {
                    if (prop.right instanceof IdentifierNode) {
                        IdentifierNode right = (IdentifierNode) prop.right;
                        return assignToField(right.name, newValue, ctx);
                    }
                } else if (prop.left instanceof SuperNode) {
                    if (prop.right instanceof IdentifierNode) {
                        IdentifierNode right = (IdentifierNode) prop.right;
                        return assignToSuperField(right.name, newValue, ctx);
                    }
                }
                
                throw new ProgramError("Invalid property assignment target");
            }
            
            if (target instanceof IdentifierNode) {
                IdentifierNode id = (IdentifierNode) target;
                return assignToVariableScoped(id.name, newValue, ctx);
            }
            
            throw new ProgramError("Invalid assignment target: " + 
                (target != null ? target.getClass().getSimpleName() : "null"));
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Variable assignment failed", e);
        }
    }
    
    private Object handleRegularAssignment(String varName, Object value, ExecutionContext ctx) {
        if (varName == null || "_".equals(varName)) {
            throw new ProgramError("Invalid assignment: cannot assign to '_'");
        }
        
        return assignToVariableScoped(varName, value, ctx);
    }
    
    public Object assignToVariableScoped(String varName, Object newValue, ExecutionContext ctx) {
    // First check all scopes for existing variable
    for (int i = ctx.getScopeDepth() - 1; i >= 0; i--) {
        Map<String, Object> scope = ctx.getLocalsStack().get(i);
        if (scope.containsKey(varName)) {
            return updateVariableInScope(varName, newValue, scope, i, ctx);
        }
    }
    
    // Then check object fields
    if (ctx.objectInstance != null && ctx.objectInstance.type != null) {
        Object fieldValue = interpreter.getConstructorResolver()
            .getFieldFromHierarchy(ctx.objectInstance.type, varName, ctx);
        if (fieldValue != null) {
            ctx.objectInstance.fields.put(varName, newValue);
            return newValue;
        }
    }
    
    // If not found anywhere, this is an error - variable must be declared with := first
    throw new ProgramError("Variable '" + varName + "' not declared. Use ':=' for declaration, or declare it first.");
}
    
    private Object updateVariableInScope(String varName, Object newValue, 
                                       Map<String, Object> scope, int scopeIndex, 
                                       ExecutionContext ctx) {
        try {
            String declaredType = null;
            if (scopeIndex < ctx.getLocalTypesStack().size()) {
                Map<String, String> typeScope = ctx.getLocalTypesStack().get(scopeIndex);
                declaredType = typeScope.get(varName);
            }
            
            if (declaredType != null) {
                validateAssignmentType(declaredType, newValue, varName);
                newValue = typeSystem.wrapUnionType(newValue, declaredType);
            }
            
            scope.put(varName, newValue);
            return newValue;
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Variable update failed for: " + varName, e);
        }
    }
    
    private Object assignToField(String fieldName, Object newValue, ExecutionContext ctx) {
        try {
            Object existingField = interpreter.getConstructorResolver()
                .getFieldFromHierarchy(ctx.objectInstance.type, fieldName, ctx);
            if (existingField != null) {
                ctx.objectInstance.fields.put(fieldName, newValue);
                return newValue;
            } else {
                throw new ProgramError("Cannot assign to undefined field via this: " + fieldName);
            }
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Field assignment failed for: " + fieldName, e);
        }
    }
    
    private Object assignToSuperField(String fieldName, Object newValue, ExecutionContext ctx) {
        try {
            if (ctx.objectInstance == null || ctx.objectInstance.type == null) {
                throw new ProgramError("Cannot assign to 'super." + fieldName + "' outside of object context");
            }
            
            if (ctx.objectInstance.type.extendName == null) {
                throw new ProgramError("Cannot assign to 'super." + fieldName + "' - no parent class");
            }
            
            ConstructorResolver resolver = interpreter.getConstructorResolver();
            TypeNode parentType = resolver.findParentType(ctx.objectInstance.type, ctx);
            
            if (parentType == null) {
                throw new ProgramError("Parent class not found for 'super." + fieldName + "'");
            }
            
            Object existingField = resolver.getFieldFromHierarchy(parentType, fieldName, ctx);
            if (existingField != null) {
                ctx.objectInstance.fields.put(fieldName, newValue);
                return newValue;
            } else {
                throw new ProgramError("Cannot assign to undefined field via super: " + fieldName);
            }
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Super field assignment failed for: " + fieldName, e);
        }
    }
    
    // === Helper Methods ===
    
    private void validateAssignmentType(String declaredType, Object value, String name) {
        if (!typeSystem.validateTypeWithNullable(declaredType, value)) {
            throw new ProgramError(
                "Type mismatch in assignment for " + name + 
                ". Expected " + declaredType + 
                ", got " + typeSystem.getConcreteType(value));
        }
    }
    
    // === Range/Multi-Range Assignment Methods ===
    
    @SuppressWarnings("unchecked")
    private Object assignRange(Object array, RangeSpec range, Object value) {
        try {
            if (array instanceof NaturalArray) {
                NaturalArray natural = (NaturalArray) array;
                natural.setRange(range, value);
                return value;
            } else if (array instanceof List) {
                List<Object> list = (List<Object>) array;
                setListRange(list, range, value);
                return value;
            }
            throw new ProgramError("Cannot assign range to " + 
                (array != null ? array.getClass().getSimpleName() : "null"));
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Range assignment failed", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private Object assignMultiRange(Object array, MultiRangeSpec multiRange, Object value) {
        try {
            if (array instanceof NaturalArray) {
                NaturalArray natural = (NaturalArray) array;
                natural.setMultiRange(multiRange, value);
                return value;
            } else if (array instanceof List) {
                List<Object> list = (List<Object>) array;
                setListMultiRange(list, multiRange, value);
                return value;
            }
            throw new ProgramError("Cannot assign multi-range to " + 
                (array != null ? array.getClass().getSimpleName() : "null"));
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Multi-range assignment failed", e);
        }
    }
    
    private void setListRange(List<Object> list, RangeSpec range, Object value) {
        try {
            long start = expressionHandler.toLongIndex(range.start);
            long end = expressionHandler.toLongIndex(range.end);
            long step = expressionHandler.calculateStep(range);
            
            if (start < 0) start = list.size() + start;
            if (end < 0) end = list.size() + end;
            
            if (step > 0) {
                for (long i = start; i <= end && i < list.size(); i += step) {
                    list.set((int) i, value);
                }
            } else if (step < 0) {
                for (long i = start; i >= end && i >= 0; i += step) {
                    list.set((int) i, value);
                }
            } else {
                throw new InternalError("Step cannot be zero - should have been caught earlier");
            }
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("List range assignment failed", e);
        }
    }
    
    private void setListMultiRange(List<Object> list, MultiRangeSpec multiRange, Object value) {
        try {
            for (RangeSpec range : multiRange.ranges) {
                setListRange(list, range, value);
            }
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("List multi-range assignment failed", e);
        }
    }
}