package cod.interpreter.context;

import cod.ast.nodes.TypeNode;
import java.util.HashMap;
import java.util.Map;

public class ObjectInstance {
    public TypeNode type;
    public Map<String, Object> fields = new HashMap<String, Object>();

    public ObjectInstance(TypeNode type) {
        this.type = type;
    }
}
