package cod.ast.nodes;

import cod.ast.VisitorImpl;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UnitNode extends ASTNode {
    public String name;
    public UseNode imports;
    public List<PolicyNode> policies = new ArrayList<PolicyNode>();
    public List<TypeNode> types = new ArrayList<TypeNode>();
    public String mainClassName;
    public Map<String, ProgramNode> resolvedImports = new HashMap<String, ProgramNode>();
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}