package cod.interpreter.context;

import cod.ast.nodes.Type;

import java.util.HashMap;
import java.util.Map;

public class ObjectInstance {
    public Type type;
    public Map<String, Object> fields = new HashMap<String, Object>();

    public ObjectInstance(Type type) {
        this.type = type;
        // fields initialized as empty HashMap by default
    }
    
    @Override
    public String toString() {
        if (type == null) {
            return "ObjectInstance[type=null]";
        }
        return "ObjectInstance[" + type.name + "]";
    }
}