package cod.interpreter;

import cod.ast.nodes.*;
import cod.range.NaturalArray;
import cod.range.MultiRangeSpec;
import cod.range.RangeSpec;
import cod.interpreter.context.ExecutionContext;
import cod.interpreter.type.TypeSystem;
import cod.semantic.ConstructorResolver;

import java.util.*;

public class AssignmentHandler {
    private final TypeSystem typeSystem;
    private final Interpreter interpreter;
    private final ExpressionHandler expressionHandler;
    private final InterpreterVisitor dispatcher;
    private final TypeHandler typeHandler;  // Add TypeHandler reference
    
    public AssignmentHandler(TypeSystem typeSystem, Interpreter interpreter, 
                           ExpressionHandler expressionHandler, InterpreterVisitor dispatcher) {
        this.typeSystem = typeSystem;
        this.interpreter = interpreter;
        this.expressionHandler = expressionHandler;
        this.dispatcher = dispatcher;
        this.typeHandler = new TypeHandler(typeSystem);  // Initialize TypeHandler
    }
    
    // === Core Assignment Logic ===
    
    public Object handleAssignment(AssignmentNode node, ExecutionContext ctx) {
        Object newValue = dispatcher.dispatch(node.right);
        
        if (node.left instanceof IndexAccessNode) {
            return handleIndexAssignment((IndexAccessNode) node.left, newValue, ctx);
        } else if (node.left instanceof ExprNode) {
            return handleVariableAssignment((ExprNode) node.left, newValue, ctx);
        }
        
        throw new RuntimeException("Invalid assignment target");
    }
    
    public Object handleSlotAssignment(SlotAssignmentNode node, ExecutionContext ctx) {
        Object value = dispatcher.dispatch(node.value);
        String varName = node.slotName;
        
        if (varName != null && !varName.isEmpty() && !"_".equals(varName)) {
            return assignToSlot(varName, value, ctx);
        } else {
            // Try to use first declared slot
            if (ctx.slotValues != null && !ctx.slotValues.isEmpty()) {
                String slotTarget = ctx.slotTypes.keySet().iterator().next();
                return assignToSlot(slotTarget, value, ctx);
            } else {
                // If no slots, treat as regular variable assignment
                return handleRegularAssignment(varName, value, ctx);
            }
        }
    }
    
    public Object handleMultipleSlotAssignment(MultipleSlotAssignmentNode node, ExecutionContext ctx) {
        List<String> declaredSlots = new ArrayList<>(ctx.slotTypes.keySet());
        Object lastValue = null;
        int slotIndex = 0;
        
        for (SlotAssignmentNode assign : node.assignments) {
            Object value = dispatcher.dispatch(assign.value);
            String target = determineTarget(assign, declaredSlots, slotIndex);
            
            assignToSlot(target, value, ctx);
            lastValue = value;
            slotIndex++;
        }
        return lastValue;
    }
    
    // === Assignment Implementation Methods ===
    
    @SuppressWarnings("unchecked")
    private Object handleIndexAssignment(IndexAccessNode indexAccess, Object newValue, ExecutionContext ctx) {
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
        
        // Regular index assignment
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
        
        throw new RuntimeException("Invalid assignment target");
    }
    
    private Object handleVariableAssignment(ExprNode target, Object newValue, ExecutionContext ctx) {
        if (target.isThis) {
            throw new RuntimeException("Cannot assign to 'this' itself");
        }
        
        if (target.isSuper) {
            throw new RuntimeException("Cannot assign to 'super' itself");
        }
        
        if (target.name != null) {
            return assignToVariable(target.name, newValue, ctx);
        }
        
        // NEW: Handle super.field assignments
        if (target.isPropertyAccess && target.isSuper && target.propertyName != null) {
            return assignToSuperField(target.propertyName, newValue, ctx);
        }
        
        // Handle this.field assignments (existing)
        if (target.isPropertyAccess && target.isThis && target.propertyName != null) {
            return assignToField(target.propertyName, newValue, ctx);
        }
        
        throw new RuntimeException("Invalid assignment target");
    }
    
    private Object handleRegularAssignment(String varName, Object value, ExecutionContext ctx) {
        if (varName == null || "_".equals(varName)) {
            throw new RuntimeException("Invalid assignment: cannot assign to '_'");
        }
        
        // Check if assigning to a field via "this.x"
        if (ctx.objectInstance != null && varName.startsWith("this.")) {
            String fieldName = varName.substring(5);
            return assignToField(fieldName, value, ctx);
        }
        
        // NEW: Check if assigning to a field via "super.x"
        if (ctx.objectInstance != null && varName.startsWith("super.")) {
            String fieldName = varName.substring(6);
            return assignToSuperField(fieldName, value, ctx);
        }
        
        // Regular local variable assignment
        ctx.locals.put(varName, value);
        return value;
    }
    
    private Object assignToVariable(String varName, Object newValue, ExecutionContext ctx) {
        // Check for "this.field" syntax
        if (varName.startsWith("this.") && ctx.objectInstance != null) {
            String fieldName = varName.substring(5);
            return assignToField(fieldName, newValue, ctx);
        }
        
        // NEW: Check for "super.field" syntax
        if (varName.startsWith("super.") && ctx.objectInstance != null) {
            String fieldName = varName.substring(6);
            return assignToSuperField(fieldName, newValue, ctx);
        }
        
        if ("_".equals(varName)) {
            throw new RuntimeException("Cannot assign to '_'");
        }
        
        if (ctx.locals.containsKey(varName)) {
            return assignToLocal(varName, newValue, ctx);
        }
        
        // Check if it's a field
        if (ctx.objectInstance != null && ctx.objectInstance.type != null) {
            Object fieldValue = interpreter.getConstructorResolver()
                .getFieldFromHierarchy(ctx.objectInstance.type, varName, ctx);
            if (fieldValue != null) {
                ctx.objectInstance.fields.put(varName, newValue);
                return newValue;
            }
        }
        
        throw new RuntimeException("Cannot assign to undefined variable: " + varName);
    }
    
    private Object assignToLocal(String varName, Object newValue, ExecutionContext ctx) {
        if (ctx.localTypes.containsKey(varName)) {
            String type = ctx.localTypes.get(varName);
            validateAssignmentType(type, newValue, varName);
            
            // Use TypeHandler's wrapUnionType method
            newValue = typeHandler.wrapUnionType(newValue, type);
        }
        
        ctx.locals.put(varName, newValue);
        return newValue;
    }
    
    private Object assignToField(String fieldName, Object newValue, ExecutionContext ctx) {
        // Validate field exists
        Object existingField = interpreter.getConstructorResolver()
            .getFieldFromHierarchy(ctx.objectInstance.type, fieldName, ctx);
        if (existingField != null) {
            ctx.objectInstance.fields.put(fieldName, newValue);
            return newValue;
        } else {
            throw new RuntimeException("Cannot assign to undefined field via this: " + fieldName);
        }
    }
    
    // NEW: Assign to parent class field via super
    private Object assignToSuperField(String fieldName, Object newValue, ExecutionContext ctx) {
        if (ctx.objectInstance == null || ctx.objectInstance.type == null) {
            throw new RuntimeException("Cannot assign to 'super." + fieldName + "' outside of object context");
        }
        
        if (ctx.objectInstance.type.extendName == null) {
            throw new RuntimeException("Cannot assign to 'super." + fieldName + "' - no parent class");
        }
        
        // Find parent type
        ConstructorResolver resolver = interpreter.getConstructorResolver();
        TypeNode parentType = resolver.findParentType(ctx.objectInstance.type, ctx);
        
        if (parentType == null) {
            throw new RuntimeException("Parent class not found for 'super." + fieldName + "'");
        }
        
        // Check if field exists in parent hierarchy
        Object existingField = resolver.getFieldFromHierarchy(parentType, fieldName, ctx);
        if (existingField != null) {
            // Field exists in parent, assign to it
            ctx.objectInstance.fields.put(fieldName, newValue);
            return newValue;
        } else {
            throw new RuntimeException("Cannot assign to undefined field via super: " + fieldName);
        }
    }
    
    private Object assignToSlot(String slotTarget, Object value, ExecutionContext ctx) {
        if (ctx.slotValues != null && ctx.slotValues.containsKey(slotTarget)) {
            String declaredType = ctx.slotTypes.get(slotTarget);
            validateAssignmentType(declaredType, value, slotTarget);
            
            // Use TypeHandler's wrapUnionType method
            value = typeHandler.wrapUnionType(value, declaredType);
            
            ctx.slotValues.put(slotTarget, value);
            ctx.slotsInCurrentPath.add(slotTarget);
            return value;
        } else {
            throw new RuntimeException("Assignment to slot '" + slotTarget + "' failed.");
        }
    }
    
    // === Helper Methods ===
    
    private String determineTarget(SlotAssignmentNode assign, List<String> declaredSlots, int slotIndex) {
        String varName = assign.slotName;
        
        if (varName != null && !varName.isEmpty() && !"_".equals(varName)) {
            return varName;
        } else {
            if (slotIndex < declaredSlots.size()) {
                return declaredSlots.get(slotIndex);
            } else {
                throw new RuntimeException("Too many positional slot assignments.");
            }
        }
    }
    
    private void validateAssignmentType(String declaredType, Object value, String name) {
        // Use TypeHandler's validateTypeWithNullable method instead of custom validation
        if (!typeHandler.validateTypeWithNullable(declaredType, value)) {
            throw new RuntimeException("Type mismatch in assignment for " + name);
        }
    }
    
    // === Range/Multi-Range Assignment Methods ===
    
    @SuppressWarnings("unchecked")
    private Object assignRange(Object array, RangeSpec range, Object value) {
        if (array instanceof NaturalArray) {
            NaturalArray natural = (NaturalArray) array;
            natural.setRange(range, value);
            return value;
        } else if (array instanceof List) {
            List<Object> list = (List<Object>) array;
            setListRange(list, range, value);
            return value;
        }
        throw new RuntimeException("Cannot assign range to " + 
            (array != null ? array.getClass().getSimpleName() : "null"));
    }
    
    @SuppressWarnings("unchecked")
    private Object assignMultiRange(Object array, MultiRangeSpec multiRange, Object value) {
        if (array instanceof NaturalArray) {
            NaturalArray natural = (NaturalArray) array;
            natural.setMultiRange(multiRange, value);
            return value;
        } else if (array instanceof List) {
            List<Object> list = (List<Object>) array;
            setListMultiRange(list, multiRange, value);
            return value;
        }
        throw new RuntimeException("Cannot assign multi-range to " + 
            (array != null ? array.getClass().getSimpleName() : "null"));
    }
    
    private void setListRange(List<Object> list, RangeSpec range, Object value) {
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
            throw new RuntimeException("Step cannot be zero");
        }
    }
    
    private void setListMultiRange(List<Object> list, MultiRangeSpec multiRange, Object value) {
        for (RangeSpec range : multiRange.ranges) {
            setListRange(list, range, value);
        }
    }
}