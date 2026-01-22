package cod.interpreter;

import cod.interpreter.type.TypeSystem;
import cod.interpreter.type.TypeValue;
import cod.ast.nodes.ExprNode;

import java.math.BigDecimal;
import java.util.List;

public class TypeHandler {
    private final TypeSystem typeSystem;
    
    public TypeHandler(TypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }
    
    // === Type/Value Checking ===
    
    public boolean isNoneValue(Object value) {
        if (value == null) return true;
        if (value instanceof ExprNode && ((ExprNode) value).isNone) return true;
        if (value instanceof String && "none".equals(value)) return true;
        return false;
    }
    
    public boolean isTypeLiteral(String str) {
        return str.equals("int") || str.equals("float") || str.equals("text") || 
               str.equals("bool") || str.equals("type") || str.equals("none") || 
               str.equals("[]") || str.startsWith("[") || 
               str.startsWith("(") || str.contains("|");
    }

 public boolean isTruthy(Object value) {
    if (value == null) return false;
    
    if (value instanceof ExprNode && ((ExprNode) value).isNone) {
        return false;
    }
    
    if (value instanceof Boolean) return (Boolean) value;
    
    if (value instanceof Number) {
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).compareTo(BigDecimal.ZERO) != 0;
        }
        return ((Number) value).doubleValue() != 0.0;
    }
    
    if (value instanceof String) {
        String str = (String) value;
        return !str.isEmpty() && !str.equalsIgnoreCase("false");
    }
    
    if (value instanceof List<?>) {
        return !((List<?>) value).isEmpty();  // Type-safe with wildcard
    }
    
    if (value instanceof cod.range.NaturalArray) {
        return ((cod.range.NaturalArray) value).size() > 0;
    }
    
    return true;
}
    
    // === Type Validation with Special Cases ===
    
    public boolean validateTypeWithNullable(String declaredType, Object value) {
        // Special handling for none values with nullable types
        if (isNoneValue(value) && declaredType.contains("|none")) {
            return true; // none is valid for nullable types
        }
        
        // Use the regular type system validation
        return typeSystem.validateType(declaredType, value);
    }
    
    public boolean isValidForNullableType(String declaredType, Object value) {
        return declaredType.contains("|none") && isNoneValue(value);
    }
    
    // === Type Conversion Helpers ===
    
    public Object wrapUnionType(Object value, String declaredType) {
        if (declaredType.contains("|")) {
            String activeType = typeSystem.getConcreteType(typeSystem.unwrap(value));
            return new TypeValue(value, activeType, declaredType);
        }
        return value;
    }
    
    // === Type Literal Processing ===
    
    public Object processTypeLiteral(String typeLiteral) {
        if (typeLiteral.equals("none")) {
            // Create none value
            ExprNode noneNode = new ExprNode();
            noneNode.isNone = true;
            return noneNode;
        }
        return TypeValue.createTypeValue(typeLiteral);
    }
}