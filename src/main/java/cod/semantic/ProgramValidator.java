package cod.semantic;

import cod.ast.nodes.*;
import cod.parser.ProgramType;
import cod.error.ParseError;

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
     * @throws ParseError if validation fails
     */
    public static void validate(ProgramNode program, ProgramType programType) {
        if (program == null) {
            throw new ParseError("Program cannot be null");
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
                throw new ParseError("Unknown program type: " + programType);
        }
    }
    
    /**
     * Validates a MODULE program.
     * Rules:
     * 1. Must have unit declaration
     * 2. Must contain at least one class
     * 3. Cannot have direct code outside classes
     * 4. Cannot have methods outside classes
     */
    private static void validateModule(ProgramNode program) {
        // Rule 1: Must have unit declaration (not "default")
        if (program.unit == null || "default".equals(program.unit.name)) {
            throw new ParseError(
                "Module must start with 'unit' declaration.\n" +
                "Add: unit namespace.name\n" +
                "Before your class definitions."
            );
        }
        
        // Rule 2: Must contain at least one class
        if (program.unit.types == null || program.unit.types.isEmpty()) {
            throw new ParseError(
                "Module '" + program.unit.name + "' must contain at least one class.\n" +
                "Add a class: share ClassName { ... }"
            );
        }
        
        // Rules 3 & 4: Check each class
        for (TypeNode type : program.unit.types) {
            // Check for direct code in classes
            if (type.statements != null && !type.statements.isEmpty()) {
                throw new ParseError(
                    "Modules cannot have direct code outside classes.\n" +
                    "Move the code inside a method in class '" + type.name + "'."
                );
            }
            
            // Note: Methods in classes are validated by DeclarationParser
        }
    }
    
    /**
     * Validates a SCRIPT program.
     * Rules:
     * 1. Can have direct code (statements)
     * 2. Cannot have method declarations
     * 3. Cannot have class declarations
     * 4. Cannot have field declarations
     */
    private static void validateScript(ProgramNode program) {
        // Scripts are allowed to have imports
        // Scripts are parsed into a synthetic class with statements
        
        // Check each type (should only be the synthetic __Script__ type)
        for (TypeNode type : program.unit.types) {
            // Rule 2: Cannot have method declarations
            if (type.methods != null && !type.methods.isEmpty()) {
                throw new ParseError(
                    "Scripts cannot contain method declarations.\n" +
                    "Either:\n" +
                    "1. Remove methods and keep as script, OR\n" +
                    "2. Remove direct code and make it a method script, OR\n" +
                    "3. Add 'unit' and classes to make it a module."
                );
            }
            
            // Rule 3: Cannot have real class declarations (synthetic type is OK)
            if (!type.name.startsWith("__") && type.name != null) {
                // This is a real class name, not allowed in scripts
                throw new ParseError(
                    "Scripts cannot contain class declarations.\n" +
                    "Found class: " + type.name + "\n" +
                    "Remove the class or add 'unit' to make it a module."
                );
            }
            
            // Rule 4: Cannot have field declarations
            if (type.fields != null && !type.fields.isEmpty()) {
                throw new ParseError(
                    "Scripts cannot contain field declarations.\n" +
                    "Found fields in type: " + type.name + "\n" +
                    "Remove field declarations or use variables instead."
                );
            }
        }
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
            throw new ParseError(
                "Method scripts cannot have direct code outside methods.\n" +
                "Place all code inside method declarations."
            );
        }
        
        // Synthetic type name is OK
        if (type.name != null && !type.name.startsWith("__")) {
            // Check if it's actually a synthetic type created by parser
            // If not, it's an error
            throw new ParseError(
                "Method scripts cannot contain class declarations.\n" +
                "Found: " + type.name + "\n" +
                "Remove the class or add 'unit' to make it a module."
            );
        }
        
        if (type.fields != null && !type.fields.isEmpty()) {
            throw new ParseError(
                "Method scripts cannot contain field declarations.\n" +
                "Remove field declarations or add 'unit' to make it a module."
            );
        }
    }
    
    if (!hasMethods) {
        throw new ParseError("Method script must contain at least one method.");
    }
    
    // Warning for missing main() - optional
    boolean hasMain = false;
    for (TypeNode type : program.unit.types) {
        if (type.methods != null) {
            for (MethodNode method : type.methods) {
                if ("main".equals(method.name)) {
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
}