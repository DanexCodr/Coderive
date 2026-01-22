package cod.ast.nodes;

import cod.ast.VisitorImpl;
import cod.syntax.Keyword;

import java.util.ArrayList;
import java.util.List;

public class PolicyNode extends ASTNode {
    public String name;
    public Keyword visibility;
    public List<PolicyMethodNode> methods = new ArrayList<PolicyMethodNode>();
    public String sourceUnit;
    
    // ONLY composition, NO inheritance
    public List<String> composedPolicies = new ArrayList<String>();

    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
    
    // Helper method to get display name
    public String getDisplayName() {
        if (sourceUnit != null && !sourceUnit.isEmpty()) {
            return sourceUnit + "." + name;
        }
        return name;
    }
    
    // Check if this policy composes another policy
    public boolean composesPolicy(String policyName) {
        return composedPolicies != null && composedPolicies.contains(policyName);
    }
    
    // Get all dependencies (only composition)
    public List<String> getAllDependencies() {
        List<String> dependencies = new ArrayList<String>();
        if (composedPolicies != null) {
            dependencies.addAll(composedPolicies);
        }
        return dependencies;
    }
}