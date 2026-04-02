package cod.interpreter.context;

import cod.ast.nodes.LambdaNode;
import cod.ast.nodes.TypeNode;
import java.util.HashMap;
import java.util.Map;

public class LambdaClosure {
    public final LambdaNode lambda;
    public final Map<String, Object> capturedLocals;
    public final ObjectInstance objectInstance;
    public final TypeNode currentClass;
    
    public LambdaClosure(
        LambdaNode lambda,
        Map<String, Object> capturedLocals,
        ObjectInstance objectInstance,
        TypeNode currentClass) {
        
        this.lambda = lambda;
        this.capturedLocals =
            capturedLocals != null ? new HashMap<String, Object>(capturedLocals) : new HashMap<String, Object>();
        this.objectInstance = objectInstance;
        this.currentClass = currentClass;
    }
}
