package cod.interpreter.context;

import cod.ast.node.Lambda;
import cod.ast.node.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LambdaClosure {
    public final Lambda lambda;
    public final Map<String, Object> capturedLocals;
    public final ObjectInstance objectInstance;
    public final Type currentClass;
    public final LambdaClosure parentClosure;
    public final List<Object> boundArguments;
    
    public LambdaClosure(
        Lambda lambda,
        Map<String, Object> capturedLocals,
        ObjectInstance objectInstance,
        Type currentClass,
        LambdaClosure parentClosure,
        List<Object> boundArguments) {
        
        this.lambda = lambda;
        this.capturedLocals =
            capturedLocals != null ? new HashMap<String, Object>(capturedLocals) : new HashMap<String, Object>();
        this.objectInstance = objectInstance;
        this.currentClass = currentClass;
        this.parentClosure = parentClosure;
        this.boundArguments =
            boundArguments != null
                ? new ArrayList<Object>(boundArguments)
                : Collections.<Object>emptyList();
    }
}
