package cod.semantic;

import cod.error.ParseError;
import cod.lexer.MainLexer.Token;

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
    // REJECT underscore as variable name (only parameter, not variable)
    if ("_".equals(name)) {
        throw new ParseError(
            "Underscore '_' is reserved for discard/placeholder in parameters and cannot be used as a variable name",
            token.line, token.column
        );
    }

    if (isPascalCase(name)) {
        throw new ParseError(
            "Variable/parameter name '" + name + "' cannot use PascalCase (reserved for classes). " +
            "Use camelCase, snake_case, or ALL_CAPS instead",
            token.line, token.column
        );
    }
}


public static void validateParameterName(String name, Token token) {
    // ALLOW underscore as parameter name
    if ("_".equals(name)) {
        return; // Valid as parameter name (means discard)
    }
    
    if (isPascalCase(name)) {
        throw new ParseError(
            "Parameter name '" + name + "' cannot use PascalCase (reserved for classes). " +
            "Use camelCase, snake_case, or ALL_CAPS instead",
            token.line, token.column
        );
    }
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