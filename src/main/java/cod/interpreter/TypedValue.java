// In TypedValue.java
package cod.interpreter;

public class TypedValue {
    
    // The raw data being stored (e.g., Integer(5), String("hello"), or List)
    public final Object value;
    
    // The specific type active in this instance (e.g., "int" or "text")
    public final String activeType;
    
    // The full declared Union type signature (e.g., "int|text" or "(float,int)|bool")
    public final String declaredType;

    public TypedValue(Object value, String activeType, String declaredType) {
        this.value = value;
        this.activeType = activeType;
        this.declaredType = declaredType;
    }
    
    // ADD THIS toString() METHOD BACK
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}