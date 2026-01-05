package cod.interpreter.type;

import java.util.Objects;

public class TypeValue {
    
    public final Object value;
    public final String activeType;
    public final String declaredType;

    public TypeValue(Object value, String activeType, String declaredType) {
        this.value = value;
        this.activeType = activeType;
        this.declaredType = declaredType;
    }
    
    @Override
    public String toString() {
        return String.valueOf(value);
    }
    
    // Helper method to check if this is a type value
    public boolean isTypeValue() {
        return "type".equals(activeType);
    }
    
    // Check if a value matches this type (for type checking with 'is')
    public boolean matches(Object otherValue) {
        if (!isTypeValue()) {
            return false;
        }
        
        // The value should be a type signature string
        if (value instanceof String) {
            String typeSignature = (String) value;
            TypeSystem typeSystem = new TypeSystem();
            return typeSystem.validateType(typeSignature, otherValue);
        }
        
        return false;
    }
    
    // NEW: Static factory method for creating type values
    public static TypeValue createTypeValue(String typeSignature) {
        return new TypeValue(typeSignature, "type", "type");
    }
    
    // NEW: Static factory methods for common types
    public static TypeValue intType() {
        return createTypeValue("int");
    }
    
    public static TypeValue floatType() {
        return createTypeValue("float");
    }
    
    public static TypeValue textType() {
        return createTypeValue("text");
    }
    
    public static TypeValue boolType() {
        return createTypeValue("bool");
    }
    
    public static TypeValue dynamicArrayType() {
        return createTypeValue("[]");
    }
    
    public static TypeValue tupleType(String... elementTypes) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < elementTypes.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(elementTypes[i]);
        }
        sb.append(")");
        return createTypeValue(sb.toString());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TypeValue other = (TypeValue) obj;
        return Objects.equals(value, other.value) &&
               Objects.equals(activeType, other.activeType) &&
               Objects.equals(declaredType, other.declaredType);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value, activeType, declaredType);
    }
}
