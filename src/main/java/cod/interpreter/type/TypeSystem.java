package cod.interpreter.type;

import cod.ast.nodes.ExprNode;
import cod.range.NaturalArray;
import static cod.syntax.Keyword.*;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class TypeSystem {

    private static final int DECIMAL_SCALE = 20;

    // NEW: Helper to check if value is none
    private boolean isNoneValue(Object obj) {
        if (obj == null) return true;
        if (obj instanceof ExprNode && ((ExprNode) obj).isNone) return true;
        if (obj instanceof String && "none".equals(obj)) return true;
        if (obj instanceof TypeValue) {
            TypeValue tv = (TypeValue) obj;
            return tv.value == null || isNoneValue(tv.value);
        }
        return false;
    }

    // NEW: Helper to create a none value
    private Object createNoneValue() {
        cod.ast.nodes.ExprNode noneNode = new cod.ast.nodes.ExprNode();
        noneNode.isNone = true;
        return noneNode;
    }

    public Object unwrap(Object obj) {
        if (obj instanceof TypeValue) {
            return ((TypeValue) obj).value;
        }
        // NEW: Handle none literals
        if (obj instanceof cod.ast.nodes.ExprNode && ((cod.ast.nodes.ExprNode) obj).isNone) {
            return null;
        }
        return obj;
    }

    public Object addNumbers(Object a, Object b) {
        a = unwrap(a); b = unwrap(b);
        if (a instanceof List || b instanceof List) throw new RuntimeException("Cannot add arrays");
        
        if (a instanceof Integer && b instanceof Integer) return (Integer)a + (Integer)b;
        
        BigDecimal bdA = toBigDecimal(a);
        BigDecimal bdB = toBigDecimal(b);
        return bdA.add(bdB);
    }
    
    public Object subtractNumbers(Object a, Object b) {
        a = unwrap(a); b = unwrap(b);
        if (a instanceof List || b instanceof List) throw new RuntimeException("Cannot operate on arrays");
        
        if (a instanceof Integer && b instanceof Integer) return (Integer)a - (Integer)b;
        
        BigDecimal bdA = toBigDecimal(a);
        BigDecimal bdB = toBigDecimal(b);
        return bdA.subtract(bdB);
    }
    
    public Object multiplyNumbers(Object a, Object b) {
        a = unwrap(a); 
        b = unwrap(b);
        
        // Handle array multiplication
        if (isArray(a) || isArray(b)) {
            return multiplyArrayOrScalar(a, b);
        }
        
        // Handle string multiplication (repetition)
        if ((a instanceof String && isNumeric(b)) || (b instanceof String && isNumeric(a))) {
            return multiplyString(a, b);
        }
        
        if (a instanceof Integer && b instanceof Integer) {
            Object result = (Integer)a * (Integer)b;
            return result;
        }
        
        BigDecimal bdA = toBigDecimal(a);
        BigDecimal bdB = toBigDecimal(b);
        Object result = bdA.multiply(bdB);
        return result;
    }
    
    private boolean isArray(Object obj) {
        return obj instanceof List || obj instanceof NaturalArray;
    }
    
    private boolean isNumeric(Object obj) {
        return obj instanceof Integer || obj instanceof Long || 
               obj instanceof Float || obj instanceof Double || 
               obj instanceof BigDecimal;
    }
    
    private Object multiplyArrayOrScalar(Object a, Object b) {
        boolean aIsArray = isArray(a);
        boolean bIsArray = isArray(b);
        
        // Array * Array (element-wise)
        if (aIsArray && bIsArray) {
            return multiplyArrays(a, b);
        }
        
        // Array * Scalar (broadcast)
        if (aIsArray) {
            return multiplyArrayByScalar(a, b);
        }
        
        // Scalar * Array
        if (bIsArray) {
            return multiplyArrayByScalar(b, a);
        }
        
        throw new RuntimeException("Cannot multiply: " + a + " * " + b);
    }
    
    private Object multiplyArrays(Object a, Object b) {
        List<Object> listA = toList(a);
        List<Object> listB = toList(b);
        
        if (listA.size() != listB.size()) {
            throw new RuntimeException("Arrays must be same size for element-wise multiplication");
        }
        
        List<Object> result = new ArrayList<Object>();
        for (int i = 0; i < listA.size(); i++) {
            Object elemA = listA.get(i);
            Object elemB = listB.get(i);
            result.add(multiplyNumbers(elemA, elemB));
        }
        
        return result;
    }
    
    private Object multiplyArrayByScalar(Object array, Object scalar) {
        List<Object> list = toList(array);
        List<Object> result = new ArrayList<Object>();
        
        for (Object elem : list) {
            result.add(multiplyNumbers(elem, scalar));
        }
        
        // Try to preserve NaturalArray if possible
        if (array instanceof NaturalArray && scalar instanceof Integer) {
            NaturalArray natural = (NaturalArray) array;
            try {
                return multiplyNaturalArrayByScalar(natural, (Integer) scalar);
            } catch (Exception e) {
                // Fall back to list
            }
        }
        
        return result;
    }
    
    private Object multiplyNaturalArrayByScalar(NaturalArray natural, int scalar) {
        // For now, just convert to list and multiply
        // Could optimize later to create new NaturalArray
        List<Object> result = new ArrayList<Object>();
        for (Object elem : natural.toList()) {
            result.add(multiplyNumbers(elem, scalar));
        }
        return result;
    }
    
    private Object multiplyString(Object a, Object b) {
        String str = null;
        int repeat = 0;
        
        if (a instanceof String && isNumeric(b)) {
            str = (String) a;
            repeat = toInt(b);
        } else if (b instanceof String && isNumeric(a)) {
            str = (String) b;
            repeat = toInt(a);
        }
        
        if (str != null) {
            if (repeat < 0) {
                throw new RuntimeException("Cannot repeat string negative times");
            }
            if (repeat == 0) return "";
            
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < repeat; i++) {
                sb.append(str);
            }
            return sb.toString();
        }
        
        throw new RuntimeException("String multiplication requires String * Integer");
    }
    
    private int toInt(Object obj) {
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Long) return ((Long) obj).intValue();
        if (obj instanceof BigDecimal) return ((BigDecimal) obj).intValue();
        throw new RuntimeException("Cannot convert to int: " + obj);
    }

    @SuppressWarnings("unchecked")    
    private List<Object> toList(Object obj) {
        if (obj instanceof List) {
            return (List<Object>) obj;
        }
        if (obj instanceof NaturalArray) {
            return ((NaturalArray) obj).toList();
        }
        throw new RuntimeException("Cannot convert to list: " + obj.getClass());
    }
    
    public Object divideNumbers(Object a, Object b) {
        a = unwrap(a); b = unwrap(b);
        if (a instanceof List || b instanceof List) throw new RuntimeException("Cannot operate on arrays");
        
        BigDecimal bdA = toBigDecimal(a);
        BigDecimal bdB = toBigDecimal(b);
        
        if (bdB.compareTo(BigDecimal.ZERO) == 0) {
            throw new RuntimeException("Division by zero");
        }
        
        return bdA.divide(bdB, DECIMAL_SCALE, RoundingMode.HALF_UP);
    }
    
    public Object modulusNumbers(Object a, Object b) {
        a = unwrap(a); b = unwrap(b);
        if (a instanceof List || b instanceof List) throw new RuntimeException("Cannot operate on arrays");
        
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
        if (o instanceof BigDecimal) return ((BigDecimal) o).doubleValue();
        if (o instanceof Boolean) return ((Boolean)o) ? 1.0 : 0.0;
        if (o instanceof String) {
            String s = (String) o;
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Cannot convert string '" + o + "' to number");
            }
        }
        throw new RuntimeException("Cannot convert " + o + " to number");
    }

    public BigDecimal toBigDecimal(Object o) {
        o = unwrap(o);
        if (o instanceof Integer || o instanceof Long) return new BigDecimal(o.toString());
        if (o instanceof Float || o instanceof Double) return new BigDecimal(o.toString());
        if (o instanceof BigDecimal) return (BigDecimal) o;
        if (o instanceof Boolean) return ((Boolean)o) ? BigDecimal.ONE : BigDecimal.ZERO;
        if (o instanceof String) {
            String s = (String) o;
            try {
                return new BigDecimal(s);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Cannot convert string '" + o + "' to BigDecimal");
            }
        }
        throw new RuntimeException("Cannot convert " + o + " to BigDecimal");
    }

    public int compare(Object a, Object b) {
        a = unwrap(a); b = unwrap(b);
        
        // Handle none/null comparisons
        boolean aIsNone = isNoneValue(a);
        boolean bIsNone = isNoneValue(b);
        if (aIsNone && bIsNone) return 0;
        if (aIsNone) return -1;
        if (bIsNone) return 1;
        
        if (a instanceof String || b instanceof String) {
            return String.valueOf(a).compareTo(String.valueOf(b));
        }
        
        if (a instanceof BigDecimal || b instanceof BigDecimal || 
            a instanceof Double || b instanceof Double ||
            a instanceof Float || b instanceof Float) {
            
            BigDecimal bdA = toBigDecimal(a);
            BigDecimal bdB = toBigDecimal(b);
            return bdA.compareTo(bdB);
        }
        
        double valA = toDouble(a); double valB = toDouble(b);
        return Double.compare(valA, valB);
    }

    public Object convertType(Object value, String targetType) {
        value = unwrap(value);
    
        if (targetType.equals(TYPE.toString())) {
            if (value instanceof TypeValue && ((TypeValue) value).isTypeValue()) {
                return value;
            }
            if (value instanceof String) {
                String str = (String) value;
                if (isValidTypeSignature(str)) {
                    return TypeValue.createTypeValue(str);
                }
            }
            throw new RuntimeException("Cannot convert '" + value + "' to type");
        }
        
        // NEW: Handle conversion to "none" type
        if (targetType.equals("none")) {
            // Always return null for "none" type
            return createNoneValue();
        }
        
        if (value instanceof BigDecimal) {
            BigDecimal bd = (BigDecimal) value;
            if (targetType.equals(INT.toString())) return bd.intValue();
            if (targetType.equals(FLOAT.toString())) return bd.doubleValue();
            if (targetType.equals(TEXT.toString())) {
                bd = bd.stripTrailingZeros();
                return bd.toPlainString();
            }
        }
        
        if (targetType.equals(INT.toString())) return (int) toDouble(value);
        if (targetType.equals(FLOAT.toString())) return toDouble(value);
        
        if (targetType.equals(TEXT.toString())) {
            if (value instanceof BigDecimal) {
                BigDecimal bd = (BigDecimal) value;
                bd = bd.stripTrailingZeros();
                return bd.toPlainString();
            }
            if (value instanceof Double) {
                Double d = (Double) value;
                BigDecimal bd = new BigDecimal(d);
                bd = bd.stripTrailingZeros();
                return bd.toPlainString();
            }
            if (value instanceof Float) {
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
        if (value instanceof TypeValue) {
            TypeValue tv = (TypeValue) value;
            if (tv.isTypeValue()) {
                return TYPE.toString();
            }
            return tv.activeType;
        }
        
        // NEW: Handle none/null values
        if (value == null) return "none";
        if (value instanceof cod.ast.nodes.ExprNode && ((cod.ast.nodes.ExprNode) value).isNone) {
            return "none";
        }
        
        if (value instanceof Integer) return INT.toString();
        if (value instanceof Long) return INT.toString();
        if (value instanceof String) return TEXT.toString();
        if (value instanceof Float || value instanceof Double) return FLOAT.toString();
        if (value instanceof BigDecimal) {
            BigDecimal bd = (BigDecimal) value;
            if (bd.scale() <= 0 || bd.stripTrailingZeros().scale() <= 0) {
                return INT.toString();
            }
            return FLOAT.toString();
        }
        if (value instanceof Boolean) return BOOL.toString();
        if (value instanceof List) return "list"; 
        if (value instanceof NaturalArray) return "list";
        return "unknown";
    }

    public boolean validateType(String typeSig, Object value) {
        String typeSigTrimmed = typeSig.trim();
        if (typeSigTrimmed.contains("|")) {
            if (!isTypeStructurallyValid(typeSigTrimmed)) {
                throw new RuntimeException("Union type contains illegal keywords. Only explicit types allowed.");
            }
        }
        if (value instanceof TypeValue) {
            TypeValue tv = (TypeValue) value;
            return validateTypeInternal(typeSig, tv.value, tv.activeType);
        }
        String concreteType = getConcreteType(value);
        return validateTypeInternal(typeSig, value, concreteType);
    }
    
    public boolean areEqual(Object a, Object b) {
        a = unwrap(a);
        b = unwrap(b);
        
        // Handle none/null comparisons
        boolean aIsNone = isNoneValue(a);
        boolean bIsNone = isNoneValue(b);
        if (aIsNone && bIsNone) return true;
        if (aIsNone || bIsNone) return false;
        
        if (a == null) return b == null;
        if (b == null) return false;
        
        if (a instanceof Number && b instanceof Number) {
            BigDecimal bdA = toBigDecimal(a);
            BigDecimal bdB = toBigDecimal(b);
            return bdA.compareTo(bdB) == 0;
        }
        
        return a.equals(b);
    }

    private boolean validateTypeInternal(String typeSig, Object rawValue, String concreteType) {
        if (typeSig == null) return true;
        String type = typeSig.trim();

        if (type.equals(ANY.toString())) return true;
        
        // NEW: Check for none value
        boolean isNoneValue = isNoneValue(rawValue);
        if (isNoneValue) {
            // none only validates against "none" type or nullable unions
            if (type.equals("none")) return true;
            // Check if it's a nullable union like "int|none"
            if (type.contains("|")) {
                List<String> unionParts = splitTopLevel(type, '|');
                for (String part : unionParts) {
                    if (part.equals("none")) return true;
                }
            }
            return false;
        }

        List<String> unionParts = splitTopLevel(type, '|');
        if (unionParts.size() > 1) {
            for (String part : unionParts) {
                if (validateTypeInternal(part, rawValue, concreteType)) return true;
            }
            return false;
        }

        // NEW: Early check for "none" type
        if (type.equals("none")) {
            return isNoneValue;
        }

        if (type.startsWith("[") && type.endsWith("]")) {
            if (!(rawValue instanceof List || rawValue instanceof NaturalArray)) return false;
            
            if (type.equals("[]")) return true;
            
            String baseType = type.substring(1, type.length() - 1);
            
            if (rawValue instanceof NaturalArray) return true; 
            
            List<?> list = (List<?>) rawValue;
            for (Object item : list) {
                if (!validateType(baseType, item)) return false; 
            }
            return true;
        }

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

        if (type.equals(TYPE.toString())) {
            if (rawValue instanceof TypeValue) {
                TypeValue tv = (TypeValue) rawValue;
                return tv.isTypeValue();
            }
            if (rawValue instanceof String) {
                String str = (String) rawValue;
                return isValidTypeSignature(str);
            }
            return false;
        }
        
        return type.equals(concreteType) || checkPrimitiveMatch(type, rawValue);
    }

    private boolean isValidTypeSignature(String str) {
        if (str.isEmpty()) return false;
        
        // UPDATED: Added "none" to valid primitive types
        if (str.equals("int") || str.equals("float") || str.equals("text") || 
            str.equals("bool") || str.equals("type") || str.equals("none")) {
            return true;
        }
        
        if (str.startsWith("[") && str.endsWith("]")) {
            String inner = str.substring(1, str.length() - 1);
            if (inner.isEmpty()) return true;
            return isValidTypeSignature(inner);
        }
        
        if (str.startsWith("(") && str.endsWith(")")) {
            String inner = str.substring(1, str.length() - 1);
            if (inner.isEmpty()) return false;
            
            List<String> parts = splitTopLevel(inner, ',');
            for (String part : parts) {
                if (!isValidTypeSignature(part.trim())) {
                    return false;
                }
            }
            return true;
        }
        
        if (str.contains("|")) {
            List<String> parts = splitTopLevel(str, '|');
            for (String part : parts) {
                if (!isValidTypeSignature(part.trim())) {
                    return false;
                }
            }
            return true;
        }
        
        if (Character.isUpperCase(str.charAt(0))) {
            return true;
        }
        
        return false;
    }

    private boolean isTypeStructurallyValid(String typeSig) {
        List<String> unionParts = splitTopLevel(typeSig, '|');
        if (unionParts.size() > 1) {
            for (String part : unionParts) if (!isTypeStructurallyValid(part)) return false;
            return true;
        }
        String type = typeSig.trim();
        if (type.isEmpty()) return false;
        
        if (type.startsWith("[") && type.endsWith("]")) {
            String inner = type.substring(1, type.length() - 1);
            if (inner.isEmpty()) return true;
            return isTypeStructurallyValid(inner);
        }
        
        if (type.startsWith("(") && type.endsWith(")")) {
            String content = type.substring(1, type.length() - 1);
            List<String> parts = splitTopLevel(content, ',');
            for (String part : parts) if (!isTypeStructurallyValid(part)) return false; 
            return true;
        }
        
        // UPDATED: Added "none" to structurally valid types
        if (type.equals(INT.toString()) || type.equals(FLOAT.toString()) || 
            type.equals(TEXT.toString()) || type.equals(BOOL.toString()) || 
            type.equals(ANY.toString()) || type.equals("none")) {
            return true;
        }
        if (Character.isUpperCase(type.charAt(0))) return true;
        return false;
    }

    private boolean checkPrimitiveMatch(String type, Object rawValue) {
        if (type.startsWith("[") && type.endsWith("]")) {
            if (type.equals("[]")) {
                return rawValue instanceof List || rawValue instanceof NaturalArray;
            }
            return rawValue instanceof List || rawValue instanceof NaturalArray;
        }
        
        if (type.equals(INT.toString())) return rawValue instanceof Integer || rawValue instanceof Long;
        else if (type.equals(TEXT.toString())) return rawValue instanceof String;
        else if (type.equals(FLOAT.toString())) return rawValue instanceof Float || rawValue instanceof Double || rawValue instanceof BigDecimal;
        else if (type.equals(BOOL.toString())) return rawValue instanceof Boolean;
        else if (type.equals("none")) {  // NEW
            return isNoneValue(rawValue);
        }
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