package cod.semantic;

import cod.ast.node.*;
import cod.debug.DebugSystem;
import cod.error.InternalError;
import cod.error.ProgramError;
import cod.interpreter.context.ExecutionContext;
import java.util.*;

public class PolicyResolver {
    
    private final ImportResolver importResolver;
    
    // Cache for flattened policy methods (includes composed policies)
    private Map<String, List<PolicyMethod>> flattenedPolicyCache = 
        new HashMap<String, List<PolicyMethod>>();
    
    // Cache for policy inheritance chains
    private Map<String, List<String>> policyCompositionCache = 
        new HashMap<String, List<String>>();
    
    // Cache for policy validation results per class
    private Map<String, Set<String>> classPolicyCache = 
        new HashMap<String, Set<String>>();
    
    // Cache for policy requirement validation
    private Map<String, Boolean> policyValidationCache = 
        new HashMap<String, Boolean>();
    
    public PolicyResolver(ImportResolver importResolver) {
        if (importResolver == null) {
            throw new InternalError("PolicyResolver constructed with null importResolver");
        }
        this.importResolver = importResolver;
    }
    
    /**
     * Get all methods required by a policy (including composed policies)
     * Now O(1) after first lookup
     */
    public List<PolicyMethod> getFlattenedPolicyMethods(Policy policy) {
        if (policy == null) {
            return new ArrayList<PolicyMethod>();
        }
        
        String policyKey = policy.name;
        
        // Check cache
        if (flattenedPolicyCache.containsKey(policyKey)) {
            DebugSystem.debug("POLICY_CACHE", "Cache hit for policy methods: " + policyKey);
            return flattenedPolicyCache.get(policyKey);
        }
        
        // Build flattened list
        List<PolicyMethod> allMethods = new ArrayList<PolicyMethod>();
        Set<String> visited = new HashSet<String>();
        
        collectPolicyMethodsRecursive(policy, allMethods, visited);
        
        // Store in cache
        flattenedPolicyCache.put(policyKey, Collections.unmodifiableList(allMethods));
        
        DebugSystem.debug("POLICY_CACHE", "Cached " + allMethods.size() + 
            " methods for policy: " + policyKey);
        
        return allMethods;
    }
    
    /**
     * Get complete policy composition chain (including nested compositions)
     */
    public List<String> getPolicyCompositionChain(Policy policy) {
        if (policy == null) {
            return new ArrayList<String>();
        }
        
        String policyKey = policy.name;
        
        // Check cache
        if (policyCompositionCache.containsKey(policyKey)) {
            DebugSystem.debug("POLICY_CACHE", "Cache hit for composition: " + policyKey);
            return policyCompositionCache.get(policyKey);
        }
        
        List<String> chain = new ArrayList<String>();
        Set<String> visited = new HashSet<String>();
        
        collectPolicyCompositionRecursive(policy, chain, visited);
        
        // Store in cache
        policyCompositionCache.put(policyKey, Collections.unmodifiableList(chain));
        
        DebugSystem.debug("POLICY_CACHE", "Cached composition chain of length " + 
            chain.size() + " for policy: " + policyKey);
        
        return chain;
    }
    
    /**
     * Get all policies implemented by a class (including inherited)
     */
    public Set<String> getClassPolicies(Type classType, ExecutionContext ctx) {
        if (classType == null) {
            return new HashSet<String>();
        }
        
        String classKey = classType.name;
        
        // Check cache
        if (classPolicyCache.containsKey(classKey)) {
            DebugSystem.debug("POLICY_CACHE", "Cache hit for class policies: " + classKey);
            return classPolicyCache.get(classKey);
        }
        
        Set<String> allPolicies = new HashSet<String>();
        
        // Add own implemented policies
        if (classType.implementedPolicies != null) {
            allPolicies.addAll(classType.implementedPolicies);
        }
        
        // Add inherited policies from parent classes
        if (classType.extendName != null) {
            Type parent = findParentType(classType, ctx);
            if (parent != null) {
                allPolicies.addAll(getClassPolicies(parent, ctx));
            }
        }
        
        // Store in cache
        classPolicyCache.put(classKey, Collections.unmodifiableSet(allPolicies));
        
        DebugSystem.debug("POLICY_CACHE", "Cached " + allPolicies.size() + 
            " policies for class: " + classKey);
        
        return allPolicies;
    }
    
    /**
     * Validate that a class implements all required policy methods
     * Results cached for O(1) subsequent checks
     */
    public boolean validateClassPolicies(Type classType, ExecutionContext ctx) {
        if (classType == null) {
            return true;
        }
        
        String classKey = classType.name;
        String cacheKey = "validate_" + classKey;
        
        // Check cache
        if (policyValidationCache.containsKey(cacheKey)) {
            DebugSystem.debug("POLICY_CACHE", "Cache hit for validation: " + classKey);
            return policyValidationCache.get(cacheKey);
        }
        
        // Get all policies this class must implement
        Set<String> requiredPolicies = getClassPolicies(classType, ctx);
        
        // Build set of implemented policy methods (for O(1) lookup)
        Set<String> implementedPolicyMethods = new HashSet<String>();
        if (classType.methods != null) {
            for (Method method : classType.methods) {
                if (method.isPolicyMethod) {
                    implementedPolicyMethods.add(method.methodName);
                }
            }
        }
        
        // Check each required policy
        for (String policyName : requiredPolicies) {
            Policy policy = findPolicy(policyName, ctx);
            if (policy == null) {
                DebugSystem.debug("POLICY_CACHE", "Policy not found: " + policyName);
                policyValidationCache.put(cacheKey, false);
                return false;
            }
            
            List<PolicyMethod> requiredMethods = getFlattenedPolicyMethods(policy);
            
            for (PolicyMethod requiredMethod : requiredMethods) {
                if (!implementedPolicyMethods.contains(requiredMethod.methodName)) {
                    DebugSystem.debug("POLICY_CACHE", "Missing method: " + requiredMethod.methodName);
                    policyValidationCache.put(cacheKey, false);
                    return false;
                }
            }
        }
        
        // All good
        policyValidationCache.put(cacheKey, true);
        return true;
    }
    
    /**
     * Find a policy by name (uses ImportResolver with its own cache)
     */
    private Policy findPolicy(String policyName, ExecutionContext ctx) {
        if (importResolver != null) {
            try {
                Policy policy = importResolver.findPolicy(policyName);
                if (policy != null) {
                    return policy;
                }
            } catch (ProgramError e) {
                DebugSystem.debug("POLICY", "Policy not found in imports: " + policyName);
            }
        }
        
        // Check current program (if ctx available)
        if (ctx != null && ctx.objectInstance != null && 
            ctx.objectInstance.type != null && 
            ctx.objectInstance.type.implementedPolicies != null) {
            // This is simplified - in reality you'd need to search current program's policies
        }
        
        return null;
    }
    
    /**
     * Find parent type in inheritance hierarchy
     */
    private Type findParentType(Type childType, ExecutionContext ctx) {
        if (childType.extendName == null) {
            return null;
        }
        
        if (importResolver != null) {
            try {
                return importResolver.findType(childType.extendName);
            } catch (ProgramError e) {
                DebugSystem.debug("POLICY", "Parent type not found: " + childType.extendName);
            }
        }
        
        return null;
    }
    
    /**
     * Recursively collect all methods from a policy and its compositions
     */
    private void collectPolicyMethodsRecursive(Policy policy, 
                                              List<PolicyMethod> allMethods,
                                              Set<String> visited) {
        if (policy == null || visited.contains(policy.name)) {
            return;
        }
        
        visited.add(policy.name);
        
        // First collect from composed policies (to maintain order)
        if (policy.composedPolicies != null) {
            for (String composedName : policy.composedPolicies) {
                Policy composed = findPolicy(composedName, null);
                if (composed != null) {
                    collectPolicyMethodsRecursive(composed, allMethods, visited);
                }
            }
        }
        
        // Then add this policy's methods (they override composed ones)
        if (policy.methods != null) {
            allMethods.addAll(policy.methods);
        }
    }
    
    /**
     * Recursively collect all policy names in composition chain
     */
    private void collectPolicyCompositionRecursive(Policy policy,
                                                  List<String> chain,
                                                  Set<String> visited) {
        if (policy == null || visited.contains(policy.name)) {
            return;
        }
        
        visited.add(policy.name);
        chain.add(policy.name);
        
        if (policy.composedPolicies != null) {
            for (String composedName : policy.composedPolicies) {
                Policy composed = findPolicy(composedName, null);
                if (composed != null) {
                    collectPolicyCompositionRecursive(composed, chain, visited);
                }
            }
        }
    }
    
    /**
     * Clear all caches (for testing/reloading)
     */
    public void clearCaches() {
        flattenedPolicyCache.clear();
        policyCompositionCache.clear();
        classPolicyCache.clear();
        policyValidationCache.clear();
        DebugSystem.debug("POLICY_CACHE", "All policy caches cleared");
    }
    
    /**
     * Get cache statistics
     */
    public Map<String, Integer> getCacheStats() {
        Map<String, Integer> stats = new HashMap<String, Integer>();
        stats.put("flattenedPolicyCache", flattenedPolicyCache.size());
        stats.put("policyCompositionCache", policyCompositionCache.size());
        stats.put("classPolicyCache", classPolicyCache.size());
        stats.put("policyValidationCache", policyValidationCache.size());
        return stats;
    }
}