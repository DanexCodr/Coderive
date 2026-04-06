package cod.ast.nodes;

import cod.ast.VisitorImpl;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Unit extends Base {
    public String name;
    public Use imports;
    public List<Policy> policies = new ArrayList<Policy>();
    public List<Type> types = new ArrayList<Type>();
    public String mainClassName;
    public Map<String, Program> resolvedImports = new HashMap<String, Program>();
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}