package cod.interpreter.handler;

import cod.ast.nodes.*;
import cod.error.InternalError;
import cod.error.ProgramError;
import cod.math.AutoStackingNumber;
import cod.range.NaturalArray;
import static cod.syntax.Keyword.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TypeHandler {
    
    // === TypeHandler Value Class ===
    public static class Value {
        public final Object value;
        public final String activeType;
        public final String declaredType;

        public Value(Object value, String activeType, String declaredType) {
            this.value = value;
            this.activeType = activeType;
            this.declaredType = declaredType;
        }
        
        @Override
        public String toString() {
            return String.valueOf(value);
        }
        
        public boolean isTypeValue() {
            return "type".equals(activeType);
        }
        
        public boolean matches(Object otherValue) {
            if (!isTypeValue()) {
                return false;
            }
            
            if (value instanceof String) {
                String typeSignature = (String) value;
                TypeHandler typeSystem = new TypeHandler();
                return typeSystem.validateType(typeSignature, otherValue);
            }
            
            return false;
        }
        
        public static Value createTypeValue(String typeSignature) {
            return new Value(typeSignature, "type", "type");
        }
        
        public static Value intType() {
            return createTypeValue("int");
        }
        
        public static Value floatType() {
            return createTypeValue("float");
        }
        
        public static Value textType() {
            return createTypeValue("text");
        }
        
        public static Value boolType() {
            return createTypeValue("bool");
        }
        
        public static Value dynamicArrayType() {
            return createTypeValue("[]");
        }
        
        public static Value tupleType(String... elementTypes) {
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
            Value other = (Value) obj;
            return Objects.equals(value, other.value) &&
                   Objects.equals(activeType, other.activeType) &&
                   Objects.equals(declaredType, other.declaredType);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(value, activeType, declaredType);
        }
    }
    
    // AutoStackingNumber constants
    private static final AutoStackingNumber ZERO = AutoStackingNumber.valueOf("0");
    private static final AutoStackingNumber ONE = AutoStackingNumber.valueOf("1");

    // Helper to check if value is none
    public boolean isNoneValue(Object obj) {
        if (obj == null) return true;
        if (obj instanceof NoneLiteralNode) return true;
        if (obj instanceof String && "none".equals(obj)) return true;
        if (obj instanceof Value) {
            Value tv = (Value) obj;
            return tv.value == null || isNoneValue(tv.value);
        }
        return false;
    }

    public Object unwrap(Object obj) {
        if (obj instanceof Value) {
            return ((Value) obj).value;
        }
        if (obj instanceof NoneLiteralNode) {
            return null;
        }
        return obj;
    }

    // === TypeHandler/Value Checking ===
    
    public boolean isTruthy(Object value) {
        if (value == null) return false;
        
        if (value instanceof BoolLiteralNode) {
            return ((BoolLiteralNode) value).value;
        }
        
        if (value instanceof IntLiteralNode) {
            return !((IntLiteralNode) value).value.isZero();
        }
        
        if (value instanceof FloatLiteralNode) {
            return !((FloatLiteralNode) value).value.isZero();
        }
        
        if (value instanceof TextLiteralNode) {
            String str = ((TextLiteralNode) value).value;
            return !str.isEmpty() && !str.equalsIgnoreCase("false");
        }
        
        if (value instanceof Boolean) {
            return ((Boolean) value) != false;
        } 
        
        if (value instanceof Number) {
            return ((Number) value).doubleValue() != 0.0;
        }
        
        if (value instanceof String) {
            String str = (String) value;
            return !str.isEmpty() && !str.equalsIgnoreCase("false");
        }
        
        if (value instanceof List<?>) {
            return !((List<?>) value).isEmpty();
        }
        
        if (value instanceof NaturalArray) {
            return ((NaturalArray) value).size() > 0;
        }
        
        if (value instanceof AutoStackingNumber) {
            return !((AutoStackingNumber) value).isZero();
        }
        
        throw new InternalError(
            "Unhandled type in truthy check: " +
            value.getClass().getName() + " with value: " + value
        );
    }
    
    public boolean isTypeLiteral(String str) {
        return str.equals("int") || str.equals("float") || str.equals("text") || 
               str.equals("bool") || str.equals("type") || str.equals("none") || 
               str.equals("[]") || str.startsWith("[") || 
               str.startsWith("(") || str.contains("|");
    }
    
    public Object processTypeLiteral(String typeLiteral) {
        if (typeLiteral.equals("none")) {
            return new NoneLiteralNode();
        }
        return Value.createTypeValue(typeLiteral);
    }
    
    // === TypeHandler Validation with Special Cases ===
    
    public boolean validateTypeWithNullable(String declaredType, Object value) {
        if (isNoneValue(value) && declaredType.contains("|none")) {
            return true;
        }
        return validateType(declaredType, value);
    }
    
    public boolean isValidForNullableType(String declaredType, Object value) {
        return declaredType.contains("|none") && isNoneValue(value);
    }
    
    // === TypeHandler Conversion Helpers ===
    
    public Object wrapUnionType(Object value, String declaredType) {
        if (declaredType.contains("|")) {
            String activeType = getConcreteType(unwrap(value));
            return new Value(value, activeType, declaredType);
        }
        return value;
    }
    
    // === Convert to AutoStackingNumber ===
    
    public AutoStackingNumber toAutoStackingNumber(Object o) {
        o = unwrap(o);
        
        if (o instanceof AutoStackingNumber) {
            return (AutoStackingNumber) o;
        }
        if (o instanceof IntLiteralNode) {
            return ((IntLiteralNode) o).value;
        }
        if (o instanceof FloatLiteralNode) {
            return ((FloatLiteralNode) o).value;
        }
        if (o instanceof Integer || o instanceof Long) {
            return AutoStackingNumber.fromLong(((Number) o).longValue());
        }
        if (o instanceof Float || o instanceof Double) {
            return AutoStackingNumber.fromDouble(((Number) o).doubleValue());
        }
        if (o instanceof Boolean) {
            return ((Boolean) o) ? ONE : ZERO;
        }
        if (o instanceof BoolLiteralNode) {
            return ((BoolLiteralNode) o).value ? ONE : ZERO;
        }
        if (o instanceof String) {
            String s = (String) o;
            try {
                return AutoStackingNumber.valueOf(s);
            } catch (NumberFormatException e) {
                throw new ProgramError("Cannot convert string '" + s + "' to number");
            }
        }
        if (o instanceof TextLiteralNode) {
            String s = ((TextLiteralNode) o).value;
            try {
                return AutoStackingNumber.valueOf(s);
            } catch (NumberFormatException e) {
                throw new ProgramError("Cannot convert string '" + s + "' to number");
            }
        }
        
        throw new InternalError(
            "Cannot convert to AutoStackingNumber: " + 
            (o != null ? o.getClass().getName() + " with value " + o : "null")
        );
    }
    
    public long toLong(Object o) {
        AutoStackingNumber num = toAutoStackingNumber(o);
        return num.longValue();
    }
    
    public double toDouble(Object o) {
        AutoStackingNumber num = toAutoStackingNumber(o);
        return num.doubleValue();
    }
    
    // === Arithmetic Operations ===
    
    public Object addNumbers(Object a, Object b) {
        a = unwrap(a); 
        b = unwrap(b);
        
        if (isArray(a) || isArray(b)) {
            return applyArrayOperation(a, b, "+");
        }
        
        if (a instanceof String || b instanceof String ||
            a instanceof TextLiteralNode || b instanceof TextLiteralNode) {
            return String.valueOf(a) + String.valueOf(b);
        }
        
        AutoStackingNumber numA = toAutoStackingNumber(a);
        AutoStackingNumber numB = toAutoStackingNumber(b);
        return numA.add(numB);
    }
    
    public Object subtractNumbers(Object a, Object b) {
        a = unwrap(a); 
        b = unwrap(b);
        
        if (isArray(a) || isArray(b)) {
            return applyArrayOperation(a, b, "-");
        }
        
        AutoStackingNumber numA = toAutoStackingNumber(a);
        AutoStackingNumber numB = toAutoStackingNumber(b);
        return numA.subtract(numB);
    }
    
    public Object multiplyNumbers(Object a, Object b) {
        a = unwrap(a); 
        b = unwrap(b);
        
        if (isArray(a) || isArray(b)) {
            return applyArrayOperation(a, b, "*");
        }
        
        // Handle string multiplication (repetition)
        if ((a instanceof TextLiteralNode && isNumeric(b)) || 
            (b instanceof TextLiteralNode && isNumeric(a))) {
            return multiplyString(a, b);
        }
        
        if (a instanceof String && isNumeric(b)) {
            return multiplyString(a, b);
        }
        
        if (b instanceof String && isNumeric(a)) {
            return multiplyString(a, b);
        }
        
        AutoStackingNumber numA = toAutoStackingNumber(a);
        AutoStackingNumber numB = toAutoStackingNumber(b);
        return numA.multiply(numB);
    }
    
    private boolean isArray(Object obj) {
        return obj instanceof List || obj instanceof NaturalArray;
    }
    
    private boolean isNumeric(Object obj) {
        return obj instanceof AutoStackingNumber ||
               obj instanceof IntLiteralNode ||
               obj instanceof FloatLiteralNode ||
               obj instanceof Integer || obj instanceof Long || 
               obj instanceof Float || obj instanceof Double;
    }
    
    private Object applyArrayOperation(Object a, Object b, String op) {
        boolean aIsArray = isArray(a);
        boolean bIsArray = isArray(b);
        
        if (aIsArray && bIsArray) {
            return applyArrayArrayOperation(a, b, op);
        }
        
        if (aIsArray) {
            return applyArrayScalarOperation(a, b, op);
        }
        
        if (bIsArray) {
            return applyArrayScalarOperation(b, a, op);
        }
        
        throw new InternalError(
            "Invalid state in applyArrayOperation: neither a nor b is array. " +
            "a=" + (a != null ? a.getClass().getName() : "null") + 
            ", b=" + (b != null ? b.getClass().getName() : "null")
        );
    }
    
    private Object applyArrayArrayOperation(Object a, Object b, String op) {
        List<Object> listA = toList(a);
        List<Object> listB = toList(b);
        
        int sizeA = listA.size();
        int sizeB = listB.size();
        int resultSize;
        
        if (sizeA == sizeB) {
            resultSize = sizeA;
        } else if (sizeA == 1) {
            resultSize = sizeB;
        } else if (sizeB == 1) {
            resultSize = sizeA;
        } else if (canBroadcastNestedWithVector(listA, listB)) {
            List<Object> result = new ArrayList<Object>(sizeA);
            for (Object elemA : listA) {
                result.add(applyScalarOperation(elemA, listB, op));
            }
            return result;
        } else if (canBroadcastNestedWithVector(listB, listA)) {
            List<Object> result = new ArrayList<Object>(sizeB);
            for (Object elemB : listB) {
                result.add(applyScalarOperation(listA, elemB, op));
            }
            return result;
        } else {
            throw new ProgramError(
                "Arrays are not broadcast-compatible for '" + op + "'. " +
                "Left size: " + sizeA + ", Right size: " + sizeB
            );
        }
        
        List<Object> result = new ArrayList<Object>();
        for (int i = 0; i < resultSize; i++) {
            Object elemA = listA.get(sizeA == 1 ? 0 : i);
            Object elemB = listB.get(sizeB == 1 ? 0 : i);
            result.add(applyScalarOperation(elemA, elemB, op));
        }
        
        return result;
    }
    
    private boolean canBroadcastNestedWithVector(List<Object> nestedCandidate, List<Object> vectorCandidate) {
        if (nestedCandidate.isEmpty()) return false;
        for (Object element : nestedCandidate) {
            if (!(element instanceof List || element instanceof NaturalArray)) {
                return false;
            }
            List<Object> inner = toList(element);
            int innerSize = inner.size();
            int vectorSize = vectorCandidate.size();
            boolean sameSize = innerSize == vectorSize;
            boolean innerBroadcastable = innerSize == 1;
            boolean vectorBroadcastable = vectorSize == 1;
            if (!sameSize && !innerBroadcastable && !vectorBroadcastable) {
                return false;
            }
        }
        return true;
    }
    
    private Object applyArrayScalarOperation(Object array, Object scalar, String op) {
        List<Object> list = toList(array);
        List<Object> result = new ArrayList<Object>();

        for (Object elem : list) {
            result.add(applyScalarOperation(elem, scalar, op));
        }
        
        return result;
    }

    private Object applyScalarOperation(Object a, Object b, String op) {
        if (isArray(a) || isArray(b)) {
            return applyArrayOperation(a, b, op);
        }
        
        if ("+".equals(op)) {
            if (a instanceof String || b instanceof String ||
                a instanceof TextLiteralNode || b instanceof TextLiteralNode) {
                return String.valueOf(a) + String.valueOf(b);
            }
            AutoStackingNumber numA = toAutoStackingNumber(a);
            AutoStackingNumber numB = toAutoStackingNumber(b);
            return numA.add(numB);
        }
        
        if ("-".equals(op)) {
            AutoStackingNumber numA = toAutoStackingNumber(a);
            AutoStackingNumber numB = toAutoStackingNumber(b);
            return numA.subtract(numB);
        }
        
        if ("*".equals(op)) {
            return multiplyScalars(a, b);
        }
        
        if ("/".equals(op)) {
            AutoStackingNumber numA = toAutoStackingNumber(a);
            AutoStackingNumber numB = toAutoStackingNumber(b);
            if (numB.isZero()) {
                throw new ProgramError("Division by zero");
            }
            return numA.divide(numB);
        }
        
        throw new InternalError("Unsupported array operation: " + op);
    }

    private Object multiplyScalars(Object a, Object b) {
        if ((a instanceof TextLiteralNode && isNumeric(b)) || 
            (b instanceof TextLiteralNode && isNumeric(a))) {
            return multiplyString(a, b);
        }
        
        if (a instanceof String && isNumeric(b)) {
            return multiplyString(a, b);
        }
        
        if (b instanceof String && isNumeric(a)) {
            return multiplyString(a, b);
        }
        
        AutoStackingNumber numA = toAutoStackingNumber(a);
        AutoStackingNumber numB = toAutoStackingNumber(b);
        return numA.multiply(numB);
    }
    
    private Object multiplyString(Object a, Object b) {
        String str = null;
        int repeat = 0;
        
        if (a instanceof TextLiteralNode && isNumeric(b)) {
            str = ((TextLiteralNode) a).value;
            repeat = (int) toAutoStackingNumber(b).longValue();
        } else if (b instanceof TextLiteralNode && isNumeric(a)) {
            str = ((TextLiteralNode) b).value;
            repeat = (int) toAutoStackingNumber(a).longValue();
        } else if (a instanceof String && isNumeric(b)) {
            str = (String) a;
            repeat = (int) toAutoStackingNumber(b).longValue();
        } else if (b instanceof String && isNumeric(a)) {
            str = (String) b;
            repeat = (int) toAutoStackingNumber(a).longValue();
        } else {
            throw new InternalError(
                "Invalid state in multiplyString: neither argument is string/numeric pair. " +
                "a=" + (a != null ? a.getClass().getName() : "null") + 
                ", b=" + (b != null ? b.getClass().getName() : "null")
            );
        }
        
        if (str == null) {
            throw new InternalError("String multiplication failed to identify string operand");
        }
        
        if (repeat < 0) {
            throw new ProgramError("Cannot repeat string negative times: " + repeat);
        }
        
        if (repeat == 0) return "";
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < repeat; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
    
    @SuppressWarnings("unchecked")    
    private List<Object> toList(Object obj) {
        if (obj instanceof List) {
            return (List<Object>) obj;
        }
        if (obj instanceof NaturalArray) {
            return ((NaturalArray) obj).toList();
        }
        throw new InternalError(
            "Cannot convert to list: " + 
            (obj != null ? obj.getClass().getName() : "null")
        );
    }
    
    public Object divideNumbers(Object a, Object b) {
        a = unwrap(a); 
        b = unwrap(b);
        
        if (isArray(a) || isArray(b)) {
            return applyArrayOperation(a, b, "/");
        }
        
        AutoStackingNumber numA = toAutoStackingNumber(a);
        AutoStackingNumber numB = toAutoStackingNumber(b);
        
        if (numB.isZero()) {
            throw new ProgramError("Division by zero");
        }
        
        return numA.divide(numB);
    }
    
    public Object modulusNumbers(Object a, Object b) {
        a = unwrap(a); 
        b = unwrap(b);
        
        if (a instanceof List || b instanceof List) {
            throw new ProgramError("Cannot use modulus '%' on arrays");
        }
        
        AutoStackingNumber numA = toAutoStackingNumber(a);
        AutoStackingNumber numB = toAutoStackingNumber(b);
        
        if (numB.isZero()) {
            throw new ProgramError("Modulus by zero");
        }
        
        return numA.remainder(numB);
    }
    
    public Object negateNumber(Object a) {
        a = unwrap(a);
        
        if (a instanceof List) {
            throw new ProgramError("Cannot negate an array");
        }
        
        AutoStackingNumber num = toAutoStackingNumber(a);
        return num.negate();
    }
    
    public int compare(Object a, Object b) {
        a = unwrap(a); 
        b = unwrap(b);
        
        boolean aIsNone = isNoneValue(a);
        boolean bIsNone = isNoneValue(b);
        if (aIsNone && bIsNone) return 0;
        if (aIsNone) return -1;
        if (bIsNone) return 1;
        
        // Handle strings
        if (a instanceof TextLiteralNode || b instanceof TextLiteralNode ||
            a instanceof String || b instanceof String) {
            String strA = a instanceof TextLiteralNode ? ((TextLiteralNode) a).value : String.valueOf(a);
            String strB = b instanceof TextLiteralNode ? ((TextLiteralNode) b).value : String.valueOf(b);
            return strA.compareTo(strB);
        }
        
        // Handle numbers
        AutoStackingNumber numA = toAutoStackingNumber(a);
        AutoStackingNumber numB = toAutoStackingNumber(b);
        return numA.compareTo(numB);
    }

    public Object convertType(Object value, String targetType) {
        value = unwrap(value);
        
        if (value instanceof NaturalArray) {
            NaturalArray arr = (NaturalArray) value;
            if (arr.hasPendingUpdates()) {
                arr.commitUpdates();
            }
        }
    
        if (targetType.equals(TYPE.toString())) {
            if (value instanceof Value && ((Value) value).isTypeValue()) {
                return value;
            }
            if (value instanceof String) {
                String str = (String) value;
                if (isValidTypeSignature(str)) {
                    return Value.createTypeValue(str);
                }
            }
            if (value instanceof TextLiteralNode) {
                String str = ((TextLiteralNode) value).value;
                if (isValidTypeSignature(str)) {
                    return Value.createTypeValue(str);
                }
            }
            throw new ProgramError("Cannot convert '" + value + "' to type");
        }
        
        if (targetType.equals("none")) {
            return new NoneLiteralNode();
        }
        
        if (value instanceof FloatLiteralNode) {
            AutoStackingNumber num = ((FloatLiteralNode) value).value;
            if (targetType.equals(INT.toString())) {
                try {
                    return num.longValue();
                } catch (ArithmeticException e) {
                    throw new ProgramError("Cannot convert float to int without loss: " + num);
                }
            }
            if (targetType.equals(FLOAT.toString())) return num;
            if (targetType.equals(TEXT.toString())) {
                return num.toString();
            }
        }
        
        if (value instanceof IntLiteralNode) {
            AutoStackingNumber num = ((IntLiteralNode) value).value;
            if (targetType.equals(INT.toString())) return num.longValue();
            if (targetType.equals(FLOAT.toString())) return num;
            if (targetType.equals(TEXT.toString())) return num.toString();
        }
        
        if (value instanceof BoolLiteralNode) {
            boolean val = ((BoolLiteralNode) value).value;
            if (targetType.equals(BOOL.toString())) return val;
            if (targetType.equals(INT.toString())) return val ? 1 : 0;
            if (targetType.equals(FLOAT.toString())) return val ? ONE : ZERO;
            if (targetType.equals(TEXT.toString())) return String.valueOf(val);
        }
        
        if (value instanceof AutoStackingNumber) {
            AutoStackingNumber num = (AutoStackingNumber) value;
            if (targetType.equals(INT.toString())) return num.longValue();
            if (targetType.equals(FLOAT.toString())) return num;
            if (targetType.equals(TEXT.toString())) return num.toString();
        }
        
        if (value instanceof TextLiteralNode) {
            String str = ((TextLiteralNode) value).value;
            if (targetType.equals(TEXT.toString())) return str;
            if (targetType.equals(INT.toString())) {
                try {
                    return Integer.parseInt(str);
                } catch (NumberFormatException e) {
                    throw new ProgramError("Cannot convert string '" + str + "' to int");
                }
            }
            if (targetType.equals(FLOAT.toString())) {
                try {
                    return AutoStackingNumber.valueOf(str);
                } catch (NumberFormatException e) {
                    throw new ProgramError("Cannot convert string '" + str + "' to float");
                }
            }
            if (targetType.equals(BOOL.toString())) {
                String lower = str.toLowerCase().trim();
                if (lower.equals("true")) return true;
                if (lower.equals("false")) return false;
                throw new ProgramError("Cannot convert string '" + str + "' to boolean");
            }
        }
        
        if (targetType.equals(INT.toString())) return (int) toDouble(value);
        if (targetType.equals(FLOAT.toString())) return toAutoStackingNumber(value);
        
        if (targetType.equals(TEXT.toString())) {
            if (value instanceof AutoStackingNumber) {
                return value.toString();
            }
            if (value instanceof Double) {
                Double d = (Double) value;
                return AutoStackingNumber.fromDouble(d).toString();
            }
            if (value instanceof Float) {
                Float f = (Float) value;
                return AutoStackingNumber.fromDouble(f).toString();
            }
            return String.valueOf(value);
        }
        
        if (targetType.equals(BOOL.toString())) {
            if (value instanceof Boolean) return value;
            if (value instanceof BoolLiteralNode) return ((BoolLiteralNode) value).value;
            if (value instanceof AutoStackingNumber) {
                return !((AutoStackingNumber) value).isZero();
            }
            if (value instanceof String) {
                String strVal = ((String)value).toLowerCase().trim();
                if (strVal.equals("true")) return true;
                if (strVal.equals("false")) return false;
                try {
                    return Double.parseDouble(strVal) != 0.0;
                } catch (NumberFormatException e) {
                    throw new ProgramError("Cannot convert string '" + value + "' to boolean");
                }
            }
            if (value instanceof TextLiteralNode) {
                String strVal = ((TextLiteralNode) value).value.toLowerCase().trim();
                if (strVal.equals("true")) return true;
                if (strVal.equals("false")) return false;
                try {
                    return Double.parseDouble(strVal) != 0.0;
                } catch (NumberFormatException e) {
                    throw new ProgramError("Cannot convert string '" + strVal + "' to boolean");
                }
            }
            return toDouble(value) != 0.0;
        }
        
        throw new InternalError(
            "Unhandled type conversion: value=" + value + 
            " (type=" + (value != null ? value.getClass().getName() : "null") + 
            "), targetType=" + targetType
        );
    }
        
    public String getConcreteType(Object value) {
    if (value instanceof Value) {
        Value tv = (Value) value;
        if (tv.isTypeValue()) {
            return TYPE.toString();
        }
        return tv.activeType;
    }
    
    if (value == null) return "none";
    if (value instanceof NoneLiteralNode) return "none";
    
    if (value instanceof NaturalArray) {
        NaturalArray arr = (NaturalArray) value;
        if (arr.hasPendingUpdates()) {
            arr.commitUpdates();
        }
        // Return the element type of the array, not "list"
        return arr.getElementType();
    }
    
    if (value instanceof IntLiteralNode) return INT.toString();
    if (value instanceof FloatLiteralNode) return FLOAT.toString();
    if (value instanceof TextLiteralNode) return TEXT.toString();
    if (value instanceof BoolLiteralNode) return BOOL.toString();
    
    if (value instanceof AutoStackingNumber) {
        AutoStackingNumber num = (AutoStackingNumber) value;
        // Check if it's an integer (no fractional part)
        if (num.fitsInStacks(1) && (num.getWords()[0] & 0x7FFFFFFFFFFFFFFFL) < Long.MAX_VALUE) {
            return INT.toString();
        }
        return FLOAT.toString();
    }
    
    if (value instanceof Integer) return INT.toString();
    if (value instanceof Long) return INT.toString();
    if (value instanceof String) return TEXT.toString();
    if (value instanceof Float || value instanceof Double) return FLOAT.toString();
    if (value instanceof Boolean) return BOOL.toString();
    if (value instanceof List) return "list"; 
    
    throw new InternalError("Unknown type for value: " + value + " (" + 
        (value != null ? value.getClass().getName() : "null") + ")");
}

    public boolean validateType(String typeSig, Object value) {
        String typeSigTrimmed = typeSig.trim();
        if (typeSigTrimmed.contains("|")) {
            if (!isTypeStructurallyValid(typeSigTrimmed)) {
                throw new ProgramError("Union type contains illegal keywords: " + typeSig);
            }
        }
        if (value instanceof Value) {
            Value tv = (Value) value;
            return validateTypeInternal(typeSig, tv.value, tv.activeType);
        }
        String concreteType = getConcreteType(value);
        return validateTypeInternal(typeSig, value, concreteType);
    }
    
    public boolean areEqual(Object a, Object b) {
        a = unwrap(a);
        b = unwrap(b);
        
        boolean aIsNone = isNoneValue(a);
        boolean bIsNone = isNoneValue(b);
        if (aIsNone && bIsNone) return true;
        if (aIsNone || bIsNone) return false;
        
        if (a == null) return b == null;
        if (b == null) return false;
        
        // Handle numbers
        if (isNumeric(a) && isNumeric(b)) {
            AutoStackingNumber numA = toAutoStackingNumber(a);
            AutoStackingNumber numB = toAutoStackingNumber(b);
            return numA.compareTo(numB) == 0;
        }
        
        if (a instanceof IntLiteralNode && b instanceof IntLiteralNode) {
            return ((IntLiteralNode) a).value.compareTo(((IntLiteralNode) b).value) == 0;
        }
        
        if (a instanceof FloatLiteralNode && b instanceof FloatLiteralNode) {
            return ((FloatLiteralNode) a).value.compareTo(((FloatLiteralNode) b).value) == 0;
        }
        
        if (a instanceof TextLiteralNode && b instanceof TextLiteralNode) {
            return ((TextLiteralNode) a).value.equals(((TextLiteralNode) b).value);
        }
        
        if (a instanceof BoolLiteralNode && b instanceof BoolLiteralNode) {
            return ((BoolLiteralNode) a).value == ((BoolLiteralNode) b).value;
        }
        
        return a.equals(b);
    }

    private boolean validateTypeInternal(String typeSig, Object rawValue, String concreteType) {
        if (typeSig == null) return true;
        String type = typeSig.trim();

        if (type.equals(ANY.toString())) return true;
        
        boolean isNoneValue = isNoneValue(rawValue);
        if (isNoneValue) {
            if (type.equals("none")) return true;
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
            if (rawValue instanceof Value) {
                Value tv = (Value) rawValue;
                return tv.isTypeValue();
            }
            if (rawValue instanceof String) {
                String str = (String) rawValue;
                return isValidTypeSignature(str);
            }
            if (rawValue instanceof TextLiteralNode) {
                String str = ((TextLiteralNode) rawValue).value;
                return isValidTypeSignature(str);
            }
            return false;
        }
        
        return type.equals(concreteType) || checkPrimitiveMatch(type, rawValue);
    }

    private boolean isValidTypeSignature(String str) {
        if (str == null || str.isEmpty()) return false;
        
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
        
        if (type.equals(INT.toString())) {
            return rawValue instanceof IntLiteralNode ||
                   rawValue instanceof Integer || 
                   rawValue instanceof Long ||
                   (rawValue instanceof AutoStackingNumber && 
                    ((AutoStackingNumber) rawValue).fitsInStacks(1));
        } else if (type.equals(TEXT.toString())) {
            return rawValue instanceof TextLiteralNode ||
                   rawValue instanceof String;
        } else if (type.equals(FLOAT.toString())) {
            return rawValue instanceof FloatLiteralNode ||
                   rawValue instanceof Float || 
                   rawValue instanceof Double || 
                   rawValue instanceof AutoStackingNumber;
        } else if (type.equals(BOOL.toString())) {
            return rawValue instanceof BoolLiteralNode ||
                   rawValue instanceof Boolean;
        } else if (type.equals("none")) {
            return isNoneValue(rawValue);
        }
        return false; 
    }

    private List<String> splitTopLevel(String input, char delimiter) {
        List<String> parts = new ArrayList<String>();
        int parenDepth = 0;
        int bracketDepth = 0;
        StringBuilder current = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c == '(') parenDepth++;
            else if (c == ')') parenDepth--;
            else if (c == '[') bracketDepth++;
            else if (c == ']') bracketDepth--;
            
            if (c == delimiter && parenDepth == 0 && bracketDepth == 0) {
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
