package cod.interpreter;

import static cod.syntax.Keyword.*;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class TypeSystem {

    // Scale for BigDecimal operations (can be adjusted for higher precision)
    private static final int DECIMAL_SCALE = 20;

    public Object unwrap(Object obj) {
        if (obj instanceof TypedValue) {
            return ((TypedValue) obj).value;
        }
        return obj;
    }

    public Object addNumbers(Object a, Object b) {
        a = unwrap(a); b = unwrap(b);
        if (a instanceof List || b instanceof List) throw new RuntimeException("Cannot add arrays");
        if (a instanceof Integer && b instanceof Integer) return (Integer)a + (Integer)b;
        
        // Use BigDecimal for high precision addition
        BigDecimal bdA = toBigDecimal(a);
        BigDecimal bdB = toBigDecimal(b);
        return bdA.add(bdB);
    }
    public Object subtractNumbers(Object a, Object b) {
        a = unwrap(a); b = unwrap(b);
        if (a instanceof List || b instanceof List) throw new RuntimeException("Cannot operate on arrays");
        if (a instanceof Integer && b instanceof Integer) return (Integer)a - (Integer)b;
        
        // Use BigDecimal for high precision subtraction
        BigDecimal bdA = toBigDecimal(a);
        BigDecimal bdB = toBigDecimal(b);
        return bdA.subtract(bdB);
    }
    public Object multiplyNumbers(Object a, Object b) {
        a = unwrap(a); b = unwrap(b);
        if (a instanceof List || b instanceof List) throw new RuntimeException("Cannot operate on arrays");
        if (a instanceof Integer && b instanceof Integer) return (Integer)a * (Integer)b;
        
        // Use BigDecimal for high precision multiplication
        BigDecimal bdA = toBigDecimal(a);
        BigDecimal bdB = toBigDecimal(b);
        return bdA.multiply(bdB);
    }
    public Object divideNumbers(Object a, Object b) {
        a = unwrap(a); b = unwrap(b);
        if (a instanceof List || b instanceof List) throw new RuntimeException("Cannot operate on arrays");
        
        // Use BigDecimal for high precision division
        BigDecimal bdA = toBigDecimal(a);
        BigDecimal bdB = toBigDecimal(b);
        
        if (bdB.compareTo(BigDecimal.ZERO) == 0) {
            throw new RuntimeException("Division by zero");
        }
        
        // Use a fixed scale for division to prevent ArithmeticException
        return bdA.divide(bdB, DECIMAL_SCALE, RoundingMode.HALF_UP);
    }
    public Object modulusNumbers(Object a, Object b) {
        a = unwrap(a); b = unwrap(b);
        if (a instanceof List || b instanceof List) throw new RuntimeException("Cannot operate on arrays");
        // Modulus is less common for floats, fall back to double for simplicity 
        // as BigDecimal modulus is complex and rarely used in range-like contexts.
        if (a instanceof Integer && b instanceof Integer) return (Integer)a % (Integer)b;
        return toDouble(a) % toDouble(b);
    }
    public Object negateNumber(Object a) {
        a = unwrap(a);
        if (a instanceof List) throw new RuntimeException("Cannot operate on arrays");
        if (a instanceof Integer) return -(Integer)a;
        
        if (a instanceof BigDecimal) return ((BigDecimal) a).negate();
        return -toDouble(a);
    }

    public double toDouble(Object o) {
        o = unwrap(o);
        if (o instanceof Integer) return ((Integer)o).doubleValue();
        if (o instanceof Long) return ((Long)o).doubleValue();  
        if (o instanceof Float) return ((Float)o).doubleValue();
        if (o instanceof Double) return (Double)o;
        if (o instanceof BigDecimal) return ((BigDecimal) o).doubleValue(); // NEW: Handle BigDecimal
        if (o instanceof Boolean) return ((Boolean)o) ? 1.0 : 0.0;
        if (o instanceof String) {
            try {
                return Double.parseDouble((String)o);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Cannot convert string '" + o + "' to number");
            }
        }
        throw new RuntimeException("Cannot convert " + o + " to number");
    }
    
    // NEW: High precision conversion method
    public BigDecimal toBigDecimal(Object o) {
        o = unwrap(o);
        if (o instanceof Integer || o instanceof Long) return new BigDecimal(o.toString());
        if (o instanceof Float || o instanceof Double) return new BigDecimal(o.toString());
        if (o instanceof BigDecimal) return (BigDecimal) o;
        if (o instanceof Boolean) return ((Boolean)o) ? BigDecimal.ONE : BigDecimal.ZERO;
        if (o instanceof String) {
            try {
                return new BigDecimal((String)o);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Cannot convert string '" + o + "' to BigDecimal");
            }
        }
        throw new RuntimeException("Cannot convert " + o + " to BigDecimal");
    }

    public int compare(Object a, Object b) {
        a = unwrap(a); b = unwrap(b);
        if (a instanceof String || b instanceof String) return String.valueOf(a).compareTo(String.valueOf(b));
        
        // Use BigDecimal comparison for high precision
        if (a instanceof BigDecimal || b instanceof BigDecimal || 
            a instanceof Double || b instanceof Double ||
            a instanceof Float || b instanceof Float) {
            
            BigDecimal bdA = toBigDecimal(a);
            BigDecimal bdB = toBigDecimal(b);
            return bdA.compareTo(bdB);
        }
        
        // Fallback to integer comparison if both are integers/longs
        double valA = toDouble(a); double valB = toDouble(b);
        return Double.compare(valA, valB);
    }

    public Object convertType(Object value, String targetType) {
        value = unwrap(value);
        
        // Convert from BigDecimal to target type
        if (value instanceof BigDecimal) {
            BigDecimal bd = (BigDecimal) value;
            if (targetType.equals(INT.toString())) return bd.intValue();
            if (targetType.equals(FLOAT.toString())) return bd.doubleValue();
            if (targetType.equals(TEXT.toString())) {
                // FIX: Use toPlainString() not toString()
                bd = bd.stripTrailingZeros();
                return bd.toPlainString();
            }
        }
        
        if (targetType.equals(INT.toString())) return (int) toDouble(value);
        if (targetType.equals(FLOAT.toString())) return toDouble(value);
        
        // FIXED TEXT CONVERSION
        if (targetType.equals(TEXT.toString())) {
            if (value instanceof BigDecimal) {
                // Already handled above, but keep for safety
                BigDecimal bd = (BigDecimal) value;
                bd = bd.stripTrailingZeros();
                return bd.toPlainString();
            }
            if (value instanceof Double) {
                // Handle Double to avoid scientific notation
                Double d = (Double) value;
                BigDecimal bd = new BigDecimal(d);
                bd = bd.stripTrailingZeros();
                return bd.toPlainString();
            }
            if (value instanceof Float) {
                // Handle Float to avoid scientific notation
                Float f = (Float) value;
                BigDecimal bd = new BigDecimal(f);
                bd = bd.stripTrailingZeros();
                return bd.toPlainString();
            }
            return String.valueOf(value);
        }
        
        if (targetType.equals(BOOL.toString())) {
            if (value instanceof Boolean) return value;
            if (value instanceof String) {
                String strVal = ((String)value).toLowerCase().trim();
                if (strVal.equals("true")) return true;
                if (strVal.equals("false")) return false;
                try {
                    return Double.parseDouble(strVal) != 0.0;
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Cannot convert string '" + value + "' to boolean");
                }
            }
            return toDouble(value) != 0.0;
        }
        
        throw new RuntimeException("Cannot convert value '" + value + "' to type '" + targetType + "'");
    }
        
    public String getConcreteType(Object value) {
        if (value instanceof Integer) return INT.toString();
        if (value instanceof Long) return INT.toString();
        if (value instanceof String) return TEXT.toString();
        if (value instanceof Float || value instanceof Double) return FLOAT.toString();
        if (value instanceof BigDecimal) {
            // Check if it's an integer BigDecimal
            BigDecimal bd = (BigDecimal) value;
            if (bd.scale() <= 0 || bd.stripTrailingZeros().scale() <= 0) {
                return INT.toString(); // Integer BigDecimal
            }
            return FLOAT.toString(); // Fractional BigDecimal
        }
        if (value instanceof Boolean) return BOOL.toString();
        if (value instanceof List) return "list"; 
        if (value instanceof NaturalArray) return "list"; // Treat NaturalArray as list type
        return "unknown";
    }

    public boolean validateType(String typeSig, Object value) {
        String typeSigTrimmed = typeSig.trim();
        if (typeSigTrimmed.contains("|")) {
            if (!isTypeStructurallyValid(typeSigTrimmed)) {
                throw new RuntimeException("Union type contains illegal keywords. Only explicit types allowed.");
            }
        }
        if (value instanceof TypedValue) {
            TypedValue tv = (TypedValue) value;
            return validateTypeInternal(typeSig, tv.value, tv.activeType);
        }
        String concreteType = getConcreteType(value);
        return validateTypeInternal(typeSig, value, concreteType);
    }

   private boolean validateTypeInternal(String typeSig, Object rawValue, String concreteType) {
    if (typeSig == null) return true;
    String type = typeSig.trim();

    if (type.equals(ANY.toString())) return true;

    if (rawValue == null) return false;

    // 1. Unions
    List<String> unionParts = splitTopLevel(type, '|');
    if (unionParts.size() > 1) {
        for (String part : unionParts) {
            if (validateTypeInternal(part, rawValue, concreteType)) return true;
        }
        return false;
    }

    // 2. Arrays - CHANGED: Check for [type] instead of type[]
    if (type.startsWith("[") && type.endsWith("]")) {
        if (!(rawValue instanceof List || rawValue instanceof NaturalArray)) return false;
        
        // Special case: empty brackets [] (dynamic array) accepts any array
        if (type.equals("[]")) {
            return true; // [] accepts any array type
        }
        
        String baseType = type.substring(1, type.length() - 1);
        
        // For NaturalArray, just check the bounds/properties, not every element
        if (rawValue instanceof NaturalArray) return true; 
        
        List<?> list = (List<?>) rawValue;
        for (Object item : list) {
            if (!validateType(baseType, item)) return false; 
        }
        return true;
    }

    // 3. Groups/Tuples
    if (type.startsWith("(") && type.endsWith(")")) {
        String content = type.substring(1, type.length() - 1);
        List<String> tupleTypes = splitTopLevel(content, ',');
        if (tupleTypes.size() == 1) return validateTypeInternal(tupleTypes.get(0), rawValue, concreteType);
        if (!(rawValue instanceof List)) return false;
        List<?> list = (List<?>) rawValue;
        if (list.size() != tupleTypes.size()) return false;
        for (int i = 0; i < tupleTypes.size(); i++) {
            if (!validateType(tupleTypes.get(i), list.get(i))) return false;
        }
        return true;
    }

    // 4. Primitives
    return type.equals(concreteType) || checkPrimitiveMatch(type, rawValue);
}

private boolean isTypeStructurallyValid(String typeSig) {
    List<String> unionParts = splitTopLevel(typeSig, '|');
    if (unionParts.size() > 1) {
        for (String part : unionParts) if (!isTypeStructurallyValid(part)) return false;
        return true;
    }
    String type = typeSig.trim();
    if (type.isEmpty()) return false;
    
    // CHANGED: Check for [type] instead of type[]
    if (type.startsWith("[") && type.endsWith("]")) {
        String inner = type.substring(1, type.length() - 1);
        // Special case: [] is valid (dynamic array)
        if (inner.isEmpty()) return true;
        return isTypeStructurallyValid(inner);
    }
    
    if (type.startsWith("(") && type.endsWith(")")) {
        String content = type.substring(1, type.length() - 1);
        List<String> parts = splitTopLevel(content, ',');
        for (String part : parts) if (!isTypeStructurallyValid(part)) return false; 
        return true;
    }
    
    if (type.equals(INT.toString()) || type.equals(FLOAT.toString()) || 
        type.equals(TEXT.toString()) || type.equals(BOOL.toString()) || type.equals(ANY.toString())) {
        return true;
    }
    if (Character.isUpperCase(type.charAt(0))) return true;
    return false;
}

private boolean checkPrimitiveMatch(String type, Object rawValue) {
    // Handle array types with [type] notation
    if (type.startsWith("[") && type.endsWith("]")) {
        // Special case: empty brackets [] (dynamic array)
        if (type.equals("[]")) {
            // [] accepts any array
            return rawValue instanceof List || rawValue instanceof NaturalArray;
        }
        
        // Regular [type] - for primitive matching, just check if it's an array
        // The actual element type validation happens in validateTypeInternal
        return rawValue instanceof List || rawValue instanceof NaturalArray;
    }
    
    if (type == INT.toString()) return rawValue instanceof Integer || rawValue instanceof Long;
    else if (type == TEXT.toString()) return rawValue instanceof String;
    else if (type == FLOAT.toString()) return rawValue instanceof Float || rawValue instanceof Double || rawValue instanceof BigDecimal;
    else if (type == BOOL.toString()) return rawValue instanceof Boolean;
    return false; 
}

    private List<String> splitTopLevel(String input, char delimiter) {
        List<String> parts = new ArrayList<String>();
        int parenDepth = 0;
        StringBuilder current = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c == '(') parenDepth++;
            else if (c == ')') parenDepth--;
            if (c == delimiter && parenDepth == 0) {
                parts.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString().trim());
        return parts;
    }
}