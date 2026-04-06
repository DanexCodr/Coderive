package cod.semantic;

import cod.ast.node.*;
import cod.error.ParseError;
import cod.lexer.Token;

import java.util.*;

import static cod.semantic.ObjectValidator.nil;

public final class PolicyValidator {

  private final ImportResolver importResolver;
  private final Map<String, Policy> availablePolicies = new HashMap<>();

  public PolicyValidator(ImportResolver importResolver) {
    this.importResolver = importResolver;
  }

  public ImportResolver getImportResolver() {
    return importResolver;
  }

  public void registerLocalPolicy(Policy policy) {
    if (!nil(policy) && !nil(policy.name)) {
      availablePolicies.put(policy.name, policy);
    }
  }

  public void validatePolicyComposition(Policy policy, Token errorToken) {
    if (nil(policy, policy.composedPolicies) || policy.composedPolicies.isEmpty()) {
      return;
    }

    Set<String> visited = new HashSet<>();
    visited.add(policy.name);

    for (String composedName : policy.composedPolicies) {
      checkForCompositionCycle(composedName, visited, errorToken);
    }
  }

  public void validateClassViralPolicies(Type type, Program currentProgram) {
    if (nil(type, type.extendName)) {
      return;
    }

    Set<String> requiredMethods = getAllMethodsRequiredByAncestors(type, currentProgram);

    for (String methodName : requiredMethods) {
      boolean implementsMethod = false;

      if (!nil(type.methods)) {
        for (Method method : type.methods) {
          if (method.methodName.equals(methodName) && method.isPolicyMethod) {
            implementsMethod = true;
            break;
          }
        }
      }

      if (!implementsMethod) {
        String requiringAncestor = getRequiringAncestorWithPolicy(methodName, type, currentProgram);

        Token errorToken = null;
        if (type.parentToken != null) {
          errorToken = type.parentToken;
        } else if (type.extendToken != null) {
          errorToken = type.extendToken;
        }

        if (!nil(requiringAncestor)) {
          if (errorToken != null) {
            throw error(
                "Class '"
                    + type.name
                    + "' inherits from '" + type.extendName + "'\n" +
                    "The ancestor " + requiringAncestor + " requires policy method '" + methodName + "'\n" +
                    "Add: policy " + methodName + "(...) { ... } inside the class",
                errorToken);
          } else {
            throw error(
                "Class '"
                    + type.name
                    + "' inherits from '" + type.extendName + "'\n" +
                    "The ancestor " + requiringAncestor + " requires policy method '" + methodName + "'\n" +
                    "Add: policy " + methodName + "(...) { ... } inside the class",
                null);
          }
        } else {
          if (errorToken != null) {
            throw error(
                "Class '"
                    + type.name
                    + "' inherits from '" + type.extendName + "' which requires policy method '"
                    + methodName
                    + "'\n"
                    + "Add: policy " + methodName + "(...) { ... } inside the class",
                errorToken);
          } else {
            throw error(
                "Class '"
                    + type.name
                    + "' inherits from '" + type.extendName + "' which requires policy method '"
                    + methodName
                    + "'\n"
                    + "Add: policy " + methodName + "(...) { ... } inside the class",
                null);
          }
        }
      }
    }
  }

  public void validateAllPolicyMethods(Type type, Program currentProgram) {
    if (nil(type, type.methods)) {
      return;
    }

    for (Method method : type.methods) {
      if (method.isPolicyMethod) {
        validatePolicyMethod(type, method, currentProgram);
      }
    }
  }

  private Policy findPolicy(String policyName) {
    if (availablePolicies.containsKey(policyName)) {
      return availablePolicies.get(policyName);
    }

    if (!nil(importResolver)) {
      try {
        return importResolver.findPolicy(policyName);
      } catch (Exception e) {
      }
    }

    if (policyName.contains(".")) {
      return null;
    }

    return null;
  }

  private Type findClassByName(String className, Program currentProgram) {
    if (!nil(currentProgram, currentProgram.unit, currentProgram.unit.types)) {
      for (Type type : currentProgram.unit.types) {
        if (type.name.equals(className)) {
          return type;
        }
      }
    }

    if (!nil(importResolver)) {
      return importResolver.findType(className);
    }

    return null;
  }

  private List<PolicyMethod> getAllPolicyMethods(Policy policy) {
    if (nil(policy)) {
      return new ArrayList<>();
    }

    List<PolicyMethod> allMethods = new ArrayList<>();
    Set<String> visitedPolicies = new HashSet<>();

    collectPolicyMethodsViaComposition(policy, allMethods, visitedPolicies);

    return allMethods;
  }

  private void collectPolicyMethodsViaComposition(
      Policy policy, List<PolicyMethod> allMethods, Set<String> visited) {
    if (nil(policy) || visited.contains(policy.name)) {
      return;
    }

    visited.add(policy.name);

    if (!nil(policy.composedPolicies)) {
      for (String composedName : policy.composedPolicies) {
        Policy composed = findPolicy(composedName);
        if (!nil(composed)) {
          collectPolicyMethodsViaComposition(composed, allMethods, visited);
        }
      }
    }

    if (policy.methods != null) {
      allMethods.addAll(policy.methods);
    }
  }

  private List<String> getAllAffectingPolicies(Type currentClass, Program currentProgram) {
    List<String> allPolicies = new ArrayList<>();
    if (nil(currentClass)) {
      return allPolicies;
    }

    Set<String> visitedClasses = new HashSet<>();
    collectAffectingPoliciesRecursive(currentClass, allPolicies, visitedClasses, currentProgram);

    return allPolicies;
  }

  private void collectAffectingPoliciesRecursive(
      Type type, List<String> allPolicies, Set<String> visited, Program currentProgram) {
    if (nil(type) || visited.contains(type.name)) {
      return;
    }

    visited.add(type.name);

    if (!nil(type.extendName)) {
      Type parent = findClassByName(type.extendName, currentProgram);
      if (!nil(parent)) {
        collectAffectingPoliciesRecursive(parent, allPolicies, visited, currentProgram);
      }
    }

    if (!nil(type.implementedPolicies)) {
      for (String policyName : type.implementedPolicies) {
        if (!allPolicies.contains(policyName)) {
          allPolicies.add(policyName);
        }
      }
    }
  }

  private List<String> getAncestorPolicies(Type currentClass, Program currentProgram) {
    List<String> ancestorPolicies = new ArrayList<>();
    if (nil(currentClass, currentClass.extendName)) {
      return ancestorPolicies;
    }

    List<String> allAffecting = getAllAffectingPolicies(currentClass, currentProgram);

    if (!nil(currentClass.implementedPolicies)) {
      for (String ownPolicy : currentClass.implementedPolicies) {
        allAffecting.remove(ownPolicy);
      }
    }

    return allAffecting;
  }

  private String getRequiringAncestorWithPolicy(String methodName, Type currentClass,
                                               Program currentProgram) {
    if (nil(currentClass, currentClass.extendName)) {
      return null;
    }

    Type current = findClassByName(currentClass.extendName, currentProgram);
    while (!nil(current)) {
      if (!nil(current.implementedPolicies)) {
        for (String policyName : current.implementedPolicies) {
          Policy policy = findPolicy(policyName);
          if (!nil(policy)) {
            List<PolicyMethod> allRequiredMethods = getAllPolicyMethods(policy);
            for (PolicyMethod policyMethod : allRequiredMethods) {
              if (policyMethod.methodName.equals(methodName)) {
                return current.name + " (implements policy '" + policyName + "')";
              }
            }
          }
        }
      }

      if (!nil(current.extendName)) {
        current = findClassByName(current.extendName, currentProgram);
      } else {
        current = null;
      }
    }

    return null;
  }

  private boolean isMethodRequiredFromAncestorPolicy(
      String methodName, Type currentClass, Program currentProgram) {
    List<String> ancestorPolicies = getAncestorPolicies(currentClass, currentProgram);

    for (String policyName : ancestorPolicies) {
      Policy policy = findPolicy(policyName);
      if (!nil(policy)) {
        List<PolicyMethod> allRequiredMethods = getAllPolicyMethods(policy);
        for (PolicyMethod policyMethod : allRequiredMethods) {
          if (policyMethod.methodName.equals(methodName)) {
            return true;
          }
        }
      }
    }

    return false;
  }

  private Set<String> getAllMethodsRequiredByAncestors(
      Type currentClass, Program currentProgram) {
    Set<String> requiredMethods = new HashSet<>();
    List<String> ancestorPolicies = getAncestorPolicies(currentClass, currentProgram);

    for (String policyName : ancestorPolicies) {
      Policy policy = findPolicy(policyName);
      if (!nil(policy)) {
        List<PolicyMethod> allRequiredMethods = getAllPolicyMethods(policy);
        for (PolicyMethod method : allRequiredMethods) {
          requiredMethods.add(method.methodName);
        }
      }
    }

    return requiredMethods;
  }

  private void checkForCompositionCycle(String composedName, Set<String> visited, Token errorToken) {
    if (visited.contains(composedName)) {
      throw error(
          "Circular composition detected in policies: "
              + String.join(" -> ", visited)
              + " -> "
              + composedName,
          errorToken);
    }

    visited.add(composedName);

    Policy composed = findPolicy(composedName);
    if (!nil(composed) && !nil(composed.composedPolicies)) {
      for (String nestedComposed : composed.composedPolicies) {
        checkForCompositionCycle(nestedComposed, visited, errorToken);
      }
    }

    visited.remove(composedName);
  }

  private boolean areTypesCompatible(String implType, String policyType) {
    if (nil(implType, policyType)) {
      return false;
    }

    if (implType.equals(policyType)) {
      return true;
    }

    if (policyType.contains("|")) {
      String[] policyUnion = policyType.split("\\|");
      for (String unionMember : policyUnion) {
        if (unionMember.trim().equals(implType)) {
          return true;
        }
      }
    }

    if (policyType.equals("[]") && implType.startsWith("[") && implType.endsWith("]")) {
      return true;
    }

    if (policyType.equals("type") && !nil(implType)) {
      return true;
    }

    return false;
  }

  private void validatePolicyMethod(Type currentClass, Method method, Program currentProgram) {
    if (nil(currentClass)) {
      throw error(
          "Policy method '" + method.methodName + "' can only be declared in a class",
          null);
    }

    boolean isRequiredByOwnPolicy = false;
    String requiringPolicy = null;
    PolicyMethod requiredSignature = null;

    if (!nil(currentClass.implementedPolicies)) {
      for (String policyName : currentClass.implementedPolicies) {
        Policy policy = findPolicy(policyName);
        if (!nil(policy)) {
          List<PolicyMethod> allRequiredMethods = getAllPolicyMethods(policy);
          for (PolicyMethod policyMethod : allRequiredMethods) {
            if (policyMethod.methodName.equals(method.methodName)) {
              isRequiredByOwnPolicy = true;
              requiringPolicy = policyName;
              requiredSignature = policyMethod;
              break;
            }
          }
          if (isRequiredByOwnPolicy) break;
        }
      }
    }

    boolean isRequiredByAncestor = false;
    String requiringAncestor = null;

    if (!isRequiredByOwnPolicy) {
      isRequiredByAncestor =
          isMethodRequiredFromAncestorPolicy(method.methodName, currentClass, currentProgram);
      if (isRequiredByAncestor) {
        requiringAncestor = getRequiringAncestorWithPolicy(method.methodName, currentClass, currentProgram);

        if (!nil(requiringAncestor)) {
          String ancestorName = requiringAncestor.split(" ")[0];
          Type ancestor = findClassByName(ancestorName, currentProgram);
          if (!nil(ancestor) && !nil(ancestor.implementedPolicies)) {
            for (String policyName : ancestor.implementedPolicies) {
              Policy policy = findPolicy(policyName);
              if (!nil(policy)) {
                List<PolicyMethod> allRequiredMethods = getAllPolicyMethods(policy);
                for (PolicyMethod policyMethod : allRequiredMethods) {
                  if (policyMethod.methodName.equals(method.methodName)) {
                    requiredSignature = policyMethod;
                    requiringPolicy = policyName;
                    break;
                  }
                }
              }
              if (requiredSignature != null) break;
            }
          }
        }
      }
    }

    if (!isRequiredByOwnPolicy && !isRequiredByAncestor) {
      throw error(
          "Method '"
              + method.methodName
              + "' is not required by any implemented policy or ancestor's policies. "
              + "Remove 'policy' keyword or add to a policy.",
          null);
    }

    if (requiredSignature != null) {
      if (method.parameters.size() != requiredSignature.parameters.size()) {
        String errorSource =
            isRequiredByAncestor
                ? "ancestor '" + requiringAncestor + "'"
                : "policy '" + requiringPolicy + "'";

        throw error(
            "Policy method '"
                + method.methodName
                + "' signature mismatch for "
                + errorSource
                + ": expected "
                + requiredSignature.parameters.size()
                + " parameters, got "
                + method.parameters.size(),
            null);
      }

      for (int i = 0; i < method.parameters.size(); i++) {
        Param implParam = method.parameters.get(i);
        Param policyParam = requiredSignature.parameters.get(i);

        if (!areTypesCompatible(implParam.type, policyParam.type)) {
          String errorSource =
              isRequiredByAncestor
                  ? "ancestor '" + requiringAncestor + "'"
                  : "policy '" + requiringPolicy + "'";

          throw error(
              "Policy method '"
                  + method.methodName
                  + "' parameter type mismatch for parameter "
                  + (i + 1)
                  + " '"
                  + policyParam.name
                  + "' in "
                  + errorSource
                  + ": expected "
                  + policyParam.type
                  + ", got "
                  + implParam.type,
              null);
        }
      }

      if (!nil(requiredSignature.returnSlots) && !requiredSignature.returnSlots.isEmpty()) {
        if (nil(method.returnSlots) || method.returnSlots.isEmpty()) {
          // Intentionally allowed: implementing method may omit explicit return slot contract.
        } else if (method.returnSlots.size() != requiredSignature.returnSlots.size()) {
          String errorSource =
              isRequiredByAncestor
                  ? "ancestor '" + requiringAncestor + "'"
                  : "policy '" + requiringPolicy + "'";

          throw error(
              "Policy method '"
                  + method.methodName
                  + "' return slot mismatch for "
                  + errorSource
                  + ": expected "
                  + requiredSignature.returnSlots.size()
                  + " slots, got "
                  + method.returnSlots.size(),
              null);
        } else {
          for (int i = 0; i < method.returnSlots.size(); i++) {
            Slot implSlot = method.returnSlots.get(i);
            Slot policySlot = requiredSignature.returnSlots.get(i);

            if (!areTypesCompatible(implSlot.type, policySlot.type)) {
              String errorSource =
                  isRequiredByAncestor
                      ? "ancestor '" + requiringAncestor + "'"
                      : "policy '" + requiringPolicy + "'";

              throw error(
                  "Policy method '"
                      + method.methodName
                      + "' return slot type mismatch for slot "
                      + (i + 1)
                      + " in "
                      + errorSource
                      + ": expected "
                      + policySlot.type
                      + ", got "
                      + implSlot.type,
                  null);
            }
          }
        }
      }
    }
  }

  private static ParseError error(String message, Token token) {
    if (token != null) {
      return new ParseError(message, token);
    }
    return new ParseError(message, 1, 1);
  }
}
