package cod.semantic;

import cod.ast.nodes.*;
import cod.parser.ProgramType;
import cod.error.ProgramError;

import java.util.List;

/**
 * Validates that a program conforms to the rules of its detected program type.
 * Enforces the three-worlds design:
 * - SCRIPT: Only statements, no methods, no classes
 * - METHOD_SCRIPT: Only methods, no direct code, no classes  
 * - MODULE: Unit + Classes only, no direct code outside methods
 */
public class ProgramValidator {
    
    /**
     * Validates the entire program against its detected program type.
     * 
     * @param program The program to validate
     * @param programType The detected program type
     * @throws ProgramError if validation fails
     */
    public static void validate(ProgramNode program, ProgramType programType) {
        if (program == null) {
            throw new ProgramError("Program cannot be null");
        }
        
        switch (programType) {
            case MODULE:
                validateModule(program);
                break;
            case SCRIPT:
                validateScript(program);
                break;
            case METHOD_SCRIPT:
                validateMethodScript(program);
                break;
            default:
                throw new ProgramError("Unknown program type: " + programType);
        }
    }
    
/**
 * Validates a MODULE program.
 * Rules:
 * 1. Must have unit declaration (not "default")
 * 2. Must contain at least one class
 * 3. Cannot have direct code outside classes
 * 4. Cannot have methods outside classes
 * 5. If main class is specified, it must exist in the module
 */
private static void validateModule(ProgramNode program) {
    // Rule 1: Must have unit declaration (not "default")
    if (program.unit == null || "default".equals(program.unit.name)) {
        throw new ProgramError(
            "Module must start with 'unit' declaration.\n" +
            "Add: unit namespace.name\n" +
            "Before your class definitions."
        );
    }
    
    // Rule 2: Must contain at least one class
    if (program.unit.types == null || program.unit.types.isEmpty()) {
        throw new ProgramError(
            "Module '" + program.unit.name + "' must contain at least one class.\n" +
            "Add a class: ClassName { ... } (package-private) or share ClassName { ... } (public)"
        );
    }
    
    // Rules 3 & 4: Check each class
    for (TypeNode type : program.unit.types) {
        // Check for direct code in classes
        if (type.statements != null && !type.statements.isEmpty()) {
            throw new ProgramError(
                "Modules cannot have direct code outside classes.\n" +
                "Move the code inside a method in class '" + type.name + "'."
            );
        }
        
        // package-private (null) and share are both valid in modules
        
    }
    
    // NEW: Rule 5: Validate main class if specified
    if (program.unit.mainClassName != null && !program.unit.mainClassName.isEmpty()) {
        boolean mainClassFound = false;
        
        for (TypeNode type : program.unit.types) {
            if (type.name.equals(program.unit.mainClassName)) {
                mainClassFound = true;
                
                // Optional: Check if main class has a main method
                boolean hasMainMethod = false;
                if (type.methods != null) {
                    for (MethodNode method : type.methods) {
                        if ("main".equals(method.methodName)) {
                            // Check if it has appropriate parameters
                            // For now, accept any main method - you can refine this later
                            hasMainMethod = true;
                            break;
                        }
                    }
                }
                
                if (!hasMainMethod) {
                    System.err.println("Warning: Specified main class '" + 
                                      program.unit.mainClassName + 
                                      "' does not have a main() method");
                }
                break;
            }
        }
        
        if (!mainClassFound) {
            throw new ProgramError(
                "Specified main class '" + program.unit.mainClassName + 
                "' not found in unit '" + program.unit.name + "'\n" +
                "Available classes: " + getClassNames(program.unit.types)
            );
        }
    }
}

// Helper method to get class names for error messages
private static String getClassNames(List<TypeNode> types) {
    if (types == null || types.isEmpty()) {
        return "(no classes)";
    }
    
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < types.size(); i++) {
        if (i > 0) {
            sb.append(", ");
        }
        sb.append(types.get(i).name);
    }
    return sb.toString();
}

/**
 * Validates a METHOD_SCRIPT program.
 * Rules:
 * 1. Must contain at least one method
 * 2. Cannot have direct code outside methods
 * 3. Cannot have class declarations
 * 4. Cannot have field declarations
 * 5. Should have main() method (warning only)
 */
private static void validateMethodScript(ProgramNode program) {
    boolean hasMethods = false;
    
    for (TypeNode type : program.unit.types) {
        if (type.methods != null && !type.methods.isEmpty()) {
            hasMethods = true;
        }
        
        if (type.statements != null && !type.statements.isEmpty()) {
            throw new ProgramError(
                "Method scripts cannot have direct code outside methods.\n" +
                "Place all code inside method declarations."
            );
        }
        
        // Synthetic type name is OK
        if (type.name != null && !type.name.startsWith("__")) {
            // Check if it's actually a synthetic type created by parser
            // If not, it's an error
            throw new ProgramError(
                "Method scripts cannot contain class declarations.\n" +
                "Found: " + type.name + "\n" +
                "Remove the class or add 'unit' to make it a module."
            );
        }
        
        if (type.fields != null && !type.fields.isEmpty()) {
            throw new ProgramError(
                "Method scripts cannot contain field declarations.\n" +
                "Remove field declarations or add 'unit' to make it a module."
            );
        }
    }
    
    if (!hasMethods) {
        throw new ProgramError("Method script must contain at least one method.");
    }
    
    // Warning for missing main() - optional
    boolean hasMain = false;
    for (TypeNode type : program.unit.types) {
        if (type.methods != null) {
            for (MethodNode method : type.methods) {
                if ("main".equals(method.methodName)) {
                    if (method.parameters == null || method.parameters.isEmpty()) {
                        hasMain = true;
                        break;
                    }
                }
            }
        }
        if (hasMain) break;
    }
    
    if (!hasMain) {
        System.err.println("Warning: Method script should have a 'main()' method");
    }
}
    
    /**
     * Validates a SCRIPT program.
     * Rules:
     * 1. Can have direct code (statements)
     * 2. Cannot have node declarations
     * 3. Cannot have class declarations
     * 4. Cannot have field declarations
     */
    private static void validateScript(ProgramNode program) {
        // Scripts are allowed to have imports
        // Scripts are parsed into a synthetic class with statements
        
        // Check each type (should only be the synthetic __Script__ type)
        for (TypeNode type : program.unit.types) {
            // Rule 2: Cannot have node declarations
            if (type.methods != null && !type.methods.isEmpty()) {
                throw new ProgramError(
                    "Scripts cannot contain node declarations.\n" +
                    "Either:\n" +
                    "1. Remove methods and keep as script, OR\n" +
                    "2. Remove direct code and make it a node script, OR\n" +
                    "3. Add 'unit' and classes to make it a module."
                );
            }
            
            // Rule 3: Cannot have real class declarations (synthetic type is OK)
            if (!type.name.startsWith("__") && type.name != null) {
                // This is a real class name, not allowed in scripts
                throw new ProgramError(
                    "Scripts cannot contain class declarations.\n" +
                    "Found class: " + type.name + "\n" +
                    "Remove the class or add 'unit' to make it a module."
                );
            }
            
            // Rule 4: Cannot have field declarations
            if (type.fields != null && !type.fields.isEmpty()) {
                throw new ProgramError(
                    "Scripts cannot contain field declarations.\n" +
                    "Found fields in type: " + type.name + "\n" +
                    "Remove field declarations or use variables instead."
                );
            }
        }
    }
}