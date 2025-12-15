package cod.interpreter;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

public class ExecutionContext {
    public ObjectInstance objectInstance;
    public Map<String, Object> locals;
    public Map<String, Object> slotValues;
    public Map<String, String> slotTypes;
    // NEW: Map to track the explicit type declared for local variables
    public Map<String, String> localTypes;
    public Set<String> slotsInCurrentPath;

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
        
        if (slotValues != null && !slotValues.isEmpty()) {
            this.slotsInCurrentPath.addAll(slotValues.keySet());
        }
    }
}