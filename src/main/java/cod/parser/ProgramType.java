package cod.parser;

/**
 * Enum representing the three program types in the language.
 * Determined by the structure of the source file.
 */
public enum ProgramType {
    /** Only direct statements - no methods, no classes, no unit */
    SCRIPT,
    
    /** Only methods - no direct code, no classes, no unit */
    METHOD_SCRIPT,
    
    /** Unit declaration with classes only - no direct code, no methods outside classes */
    MODULE
}