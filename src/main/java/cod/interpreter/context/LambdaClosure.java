package cod.interpreter.context;

import cod.ast.nodes.Lambda;
import cod.ast.nodes.Type;
import java.util.HashMap;
import java.util.Map;

public class LambdaClosure {
    public final Lambda lambda;
    public final Map<String, Object> capturedLocals;
    public final ObjectInstance objectInstance;
    public final Type currentClass;
    
    public LambdaClosure(
        Lambda lambda,
        Map<String, Object> capturedLocals,
        ObjectInstance objectInstance,
        Type currentClass) {
        
        this.lambda = lambda;
        this.capturedLocals =
            capturedLocals != null ? new HashMap<String, Object>(capturedLocals) : new HashMap<String, Object>();
        this.objectInstance = objectInstance;
        this.currentClass = currentClass;
    }
}
