package cod.ast;

import cod.ast.error.ParseError;
import cod.ast.ManualCoderiveLexer.Token;

public class NamingValidator {
    
    public static void validateClassName(String name, Token token) {
        if (!isPascalCase(name)) {
            throw new ParseError(
                "Class name '" + name + "' must use PascalCase (start with uppercase letter)",
                token.line, token.column
            );
        }
    }
    
    public static void validateMethodName(String name, Token token) {
        if (!startsWithLowerCase(name)) {
            throw new ParseError(
                "Method name '" + name + "' must start with lowercase letter",
                token.line, token.column
            );
        }
    }
    
    public static void validateVariableName(String name, Token token) {
    // ONLY prevent PascalCase (class-like names)
    if (isPascalCase(name)) {
        throw new ParseError(
            "Variable/parameter name '" + name + "' cannot use PascalCase (reserved for classes). " +
            "Use camelCase, snake_case, or ALL_CAPS instead",
            token.line, token.column
        );
    }
    // ALL_CAPS and lowercase are always allowed
}

public static void validateParameterName(String name, Token token) {
    // Same rule as variables - no PascalCase
    validateVariableName(name, token);
}
    
    public static void validateConstantName(String name, Token token) {
        if (!isAllCaps(name)) {
            throw new ParseError(
                "Constant name '" + name + "' must use ALL_CAPS with underscores",
                token.line, token.column
            );
        }
    }
    
    public static boolean isPascalCase(String name) {
        return name != null && !name.isEmpty() && 
               Character.isUpperCase(name.charAt(0)) &&
               !isAllCaps(name);
    }
    
    public static boolean startsWithLowerCase(String name) {
        return name != null && !name.isEmpty() && 
               Character.isLowerCase(name.charAt(0));
    }
    
    public static boolean isAllCaps(String name) {
        return name != null && name.matches("[A-Z0-9_]+");
    }
}