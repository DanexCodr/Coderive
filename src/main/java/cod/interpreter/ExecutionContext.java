package cod.interpreter;

import cod.ast.nodes.TypeNode;
import java.util.*;

public class ExecutionContext {
    public ObjectInstance objectInstance;
    public Map<String, Object> locals;
    public Map<String, Object> slotValues;
    public Map<String, String> slotTypes;
    public Map<String, String> localTypes;
    public Set<String> slotsInCurrentPath;
    // NEW: Track which class the currently executing method belongs to
    public TypeNode currentClass;

    public ExecutionContext(ObjectInstance objectInstance, 
                          Map<String, Object> locals, 
                          Map<String, Object> slotValues,
                          Map<String, String> slotTypes) {
        this.objectInstance = objectInstance;
        this.locals = locals;
        this.slotValues = slotValues;
        this.slotTypes = slotTypes;
        
        // Initialize the NEW map
        this.localTypes = new HashMap<String, String>();

        this.slotsInCurrentPath = new HashSet<String>();
        this.currentClass = null; // Initialize as null
        
        if (slotValues != null && !slotValues.isEmpty()) {
            this.slotsInCurrentPath.addAll(slotValues.keySet());
        }
    }
    
    // NEW: Constructor with currentClass
    public ExecutionContext(ObjectInstance objectInstance, 
                          Map<String, Object> locals, 
                          Map<String, Object> slotValues,
                          Map<String, String> slotTypes,
                          TypeNode currentClass) {
        this.objectInstance = objectInstance;
        this.locals = locals;
        this.slotValues = slotValues;
        this.slotTypes = slotTypes;
        this.localTypes = new HashMap<String, String>();
        this.slotsInCurrentPath = new HashSet<String>();
        this.currentClass = currentClass;
        
        if (slotValues != null && !slotValues.isEmpty()) {
            this.slotsInCurrentPath.addAll(slotValues.keySet());
        }
    }
}