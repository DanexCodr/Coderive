package cod.interpreter.handler;

import cod.ast.node.*;
import cod.error.InternalError;
import cod.error.ProgramError;
import cod.math.AutoStackingNumber;
import cod.range.NaturalArray;
import static cod.syntax.Keyword.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.AbstractList;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;

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

    public static class PointerValue {
        public final Object container;
        public final long index;
        public final String pointedType;

        public PointerValue(Object container, long index, String pointedType) {
            this.container = container;
            this.index = index;
            this.pointedType = pointedType;
        }

        @Override
        public String toString() {
            return "&" + pointedType + "@" + index;
        }
    }
    
    // AutoStackingNumber constants
    private static final AutoStackingNumber ZERO = AutoStackingNumber.valueOf("0");
    private static final AutoStackingNumber ONE = AutoStackingNumber.valueOf("1");
    private static final int LAZY_ARRAY_MEMO_MAX_SIZE = 8192;
    private static final String[] UNSAFE_NUMERIC_TYPES = {
        "i8", "i16", "i32", "i64", "u8", "u16", "u32", "u64", "f32", "f64"
    };

    public boolean isPointerType(String type) {
        return type != null && type.startsWith("*") && type.length() > 1;
    }

    public boolean isSizedArrayType(String type) {
        if (type == null) return false;
        int l = type.lastIndexOf('[');
        int r = type.lastIndexOf(']');
        if (l <= 0 || r != type.length() - 1) return false;
        String sizePart = type.substring(l + 1, r).trim();
        if (sizePart.isEmpty()) return false;
        for (int i = 0; i < sizePart.length(); i++) {
            if (!Character.isDigit(sizePart.charAt(i))) return false;
        }
        return true;
    }

    public String getSizedArrayElementType(String type) {
        if (!isSizedArrayType(type)) return null;
        return type.substring(0, type.lastIndexOf('['));
    }

    public int getSizedArrayLength(String type) {
        if (!isSizedArrayType(type)) return -1;
        String sizePart = type.substring(type.lastIndexOf('[') + 1, type.length() - 1).trim();
        try {
            return Integer.parseInt(sizePart);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // Helper to check if value is none
    public boolean isNoneValue(Object obj) {
        if (obj == null) return true;
        if (obj instanceof NoneLiteral) return true;
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
        if (obj instanceof NoneLiteral) {
            return null;
        }
        return obj;
    }

    // === TypeHandler/Value Checking ===
    
    public boolean isTruthy(Object value) {
        if (value == null) return false;
        
        if (value instanceof BoolLiteral) {
            return ((BoolLiteral) value).value;
        }
        
        if (value instanceof IntLiteral) {
            return !((IntLiteral) value).value.isZero();
        }
        
        if (value instanceof FloatLiteral) {
            return !((FloatLiteral) value).value.isZero();
        }
        
        if (value instanceof TextLiteral) {
            String str = ((TextLiteral) value).value;
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
               isPointerType(str) || isSizedArrayType(str) ||
               isUnsafeNumericType(str) ||
               str.equals("[]") || str.startsWith("[") || 
               str.startsWith("(") || str.contains("|");
    }

    public boolean isUnsafeNumericType(String type) {
        if (type == null) return false;
        for (String unsafeType : UNSAFE_NUMERIC_TYPES) {
            if (unsafeType.equals(type)) {
                return true;
            }
        }
        return false;
    }

    public Object normalizeForDeclaredType(String declaredType, Object value) {
        if (declaredType == null) return value;
        String normalized = declaredType.trim();
        if (!isUnsafeNumericType(normalized)) {
            return value;
        }
        Object converted = convertType(value, normalized);
        return new Value(converted, normalized, normalized);
    }
    
    public Object processTypeLiteral(String typeLiteral) {
        if (typeLiteral.equals("none")) {
            return new NoneLiteral();
        }
        return Value.createTypeValue(typeLiteral);
    }

    private String normalizeTypeSignature(String typeSig) {
        if (typeSig == null) return null;
        String trimmed = typeSig.trim();
        if (isSizedArrayType(trimmed)) {
            String inner = normalizeTypeSignature(getSizedArrayElementType(trimmed));
            return "[" + inner + "]";
        }
        return trimmed;
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
        if (o instanceof IntLiteral) {
            return ((IntLiteral) o).value;
        }
        if (o instanceof FloatLiteral) {
            return ((FloatLiteral) o).value;
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
        if (o instanceof BoolLiteral) {
            return ((BoolLiteral) o).value ? ONE : ZERO;
        }
        if (o instanceof String) {
            String s = (String) o;
            try {
                return AutoStackingNumber.valueOf(s);
            } catch (NumberFormatException e) {
                throw new ProgramError("Cannot convert string '" + s + "' to number");
            }
        }
        if (o instanceof TextLiteral) {
            String s = ((TextLiteral) o).value;
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

    private boolean tryFastLongInto(Object o, long[] out, int index) {
        if (o instanceof Integer || o instanceof Long || o instanceof Short || o instanceof Byte) {
            out[index] = ((Number) o).longValue();
            return true;
        }
        if (o instanceof IntLiteral) {
            try {
                out[index] = ((IntLiteral) o).value.longValue();
                return true;
            } catch (ArithmeticException ignored) {
                return false;
            }
        }
        if (o instanceof AutoStackingNumber) {
            try {
                out[index] = ((AutoStackingNumber) o).longValue();
                return true;
            } catch (ArithmeticException ignored) {
                return false;
            }
        }
        return false;
    }

    private long[] getFastLongPair(Object a, Object b) {
        long[] pair = new long[2];
        if (!tryFastLongInto(a, pair, 0)) {
            return null;
        }
        if (!tryFastLongInto(b, pair, 1)) {
            return null;
        }
        return pair;
    }
    
    // === Arithmetic Operations ===
    
    public Object addNumbers(Object a, Object b) {
        a = unwrap(a); 
        b = unwrap(b);
        
        if (isArray(a) || isArray(b)) {
            return applyArrayOperation(a, b, "+");
        }
        return addScalars(a, b);
    }
    
    private Object addScalars(Object a, Object b) {
        
        if (a instanceof String || b instanceof String ||
            a instanceof TextLiteral || b instanceof TextLiteral) {
            return String.valueOf(a) + String.valueOf(b);
        }

        long[] fastPair = getFastLongPair(a, b);
        if (fastPair != null) {
            long av = fastPair[0];
            long bv = fastPair[1];
            long sum = av + bv;
            if (((av ^ sum) & (bv ^ sum)) >= 0) {
                return AutoStackingNumber.fromLong(sum);
            }
            return AutoStackingNumber.fromDouble((double) av + (double) bv);
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
        return subtractScalars(a, b);
    }
    
    private Object subtractScalars(Object a, Object b) {

        long[] fastPair = getFastLongPair(a, b);
        if (fastPair != null) {
            long av = fastPair[0];
            long bv = fastPair[1];
            long diff = av - bv;
            if (((av ^ bv) & (av ^ diff)) >= 0) {
                return AutoStackingNumber.fromLong(diff);
            }
            return AutoStackingNumber.fromDouble((double) av - (double) bv);
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
        return multiplyScalars(a, b);
    }
    
    private Object multiplyScalars(Object a, Object b) {
        
        // Handle string multiplication (repetition)
        if ((a instanceof TextLiteral && isNumeric(b)) || 
            (b instanceof TextLiteral && isNumeric(a))) {
            return multiplyString(a, b);
        }
        
        if (a instanceof String && isNumeric(b)) {
            return multiplyString(a, b);
        }
        
        if (b instanceof String && isNumeric(a)) {
            return multiplyString(a, b);
        }

        long[] fastPair = getFastLongPair(a, b);
        if (fastPair != null) {
            long av = fastPair[0];
            long bv = fastPair[1];
            if (av == 0L || bv == 0L) {
                return AutoStackingNumber.fromLong(0L);
            }
            if (!isLongMultiplicationOverflow(av, bv)) {
                long product = av * bv;
                return AutoStackingNumber.fromLong(product);
            }
            return AutoStackingNumber.fromDouble((double) av * (double) bv);
        }
        
        AutoStackingNumber numA = toAutoStackingNumber(a);
        AutoStackingNumber numB = toAutoStackingNumber(b);
        return numA.multiply(numB);
    }
    
    private boolean isLongMultiplicationOverflow(long a, long b) {
        if (a > 0) {
            if (b > 0) return a > Long.MAX_VALUE / b;
            if (b < 0) return b < Long.MIN_VALUE / a;
            return false;
        }
        if (a < 0) {
            if (b > 0) return a < Long.MIN_VALUE / b;
            if (b < 0) return a != 0 && b < Long.MAX_VALUE / a;
            return false;
        }
        return false;
    }
    
    private boolean isArray(Object obj) {
        return obj instanceof List || obj instanceof NaturalArray;
    }
    
    private boolean isNumeric(Object obj) {
        return obj instanceof AutoStackingNumber ||
               obj instanceof IntLiteral ||
               obj instanceof FloatLiteral ||
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
        int sizeA = getArrayLikeSize(a);
        int sizeB = getArrayLikeSize(b);
        int resultSize;
        int opCode = resolveArrayOpCode(op);
        
        if (sizeA == sizeB) {
            resultSize = sizeA;
        } else if (sizeA == 1) {
            resultSize = sizeB;
        } else if (sizeB == 1) {
            resultSize = sizeA;
        } else {
            List<Object> listA = toList(a);
            List<Object> listB = toList(b);
            if (canBroadcastNestedWithVector(listA, listB)) {
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
        }

        List<Object> result = new ArrayList<Object>(resultSize);
        for (int i = 0; i < resultSize; i++) {
            Object elemA = getArrayLikeElement(a, sizeA == 1 ? 0 : i);
            Object elemB = getArrayLikeElement(b, sizeB == 1 ? 0 : i);
            if (isArray(elemA) || isArray(elemB)) {
                result.add(applyArrayOperation(elemA, elemB, op));
            } else {
                result.add(applyScalarByOpCode(elemA, elemB, opCode));
            }
        }
        
        return result;
    }
    
    private int getArrayLikeSize(Object obj) {
        if (obj instanceof List) {
            return ((List<?>) obj).size();
        }
        if (obj instanceof NaturalArray) {
            long size = ((NaturalArray) obj).size();
            if (size > Integer.MAX_VALUE) {
                throw new ProgramError("Array too large to materialize operation result: " + size);
            }
            return (int) size;
        }
        throw new InternalError(
            "Cannot get array-like size: " +
            (obj != null ? obj.getClass().getName() : "null")
        );
    }
    
    private Object getArrayLikeElement(Object obj, int index) {
        if (obj instanceof List) {
            return ((List<?>) obj).get(index);
        }
        if (obj instanceof NaturalArray) {
            return ((NaturalArray) obj).get(index);
        }
        throw new InternalError(
            "Cannot get array-like element: " +
            (obj != null ? obj.getClass().getName() : "null")
        );
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
        int opCode = resolveArrayOpCode(op);
        if (array instanceof NaturalArray) {
            NaturalArray natural = (NaturalArray) array;
            long sizeLong = natural.size();
            if (sizeLong > Integer.MAX_VALUE) {
                throw new ProgramError("Array too large for scalar operation: " + sizeLong);
            }
            int size = (int) sizeLong;
            if (!natural.isMutable() && !natural.hasPendingUpdates()) {
                return new LazyNaturalArrayScalarResult(natural, scalar, op, opCode, size);
            }
            List<Object> result = new ArrayList<Object>(size);
            for (int i = 0; i < size; i++) {
                Object elem = natural.get(i);
                if (isArray(elem)) {
                    result.add(applyArrayOperation(elem, scalar, op));
                } else {
                    result.add(applyScalarByOpCode(elem, scalar, opCode));
                }
            }
            return result;
        }

        List<Object> list = toList(array);
        List<Object> result = new ArrayList<Object>(list.size());
        for (Object elem : list) {
            if (isArray(elem)) {
                result.add(applyArrayOperation(elem, scalar, op));
            } else {
                result.add(applyScalarByOpCode(elem, scalar, opCode));
            }
        }
        
        return result;
    }

    private final class LazyNaturalArrayScalarResult extends AbstractList<Object> implements RandomAccess {
        private final NaturalArray source;
        private final Object scalar;
        private final String op;
        private final int opCode;
        private final int size;
        private List<Object> materialized;
        private final Object[] memoValues;
        private final boolean[] memoComputed;

        private LazyNaturalArrayScalarResult(NaturalArray source, Object scalar, String op, int opCode, int size) {
            this.source = source;
            this.scalar = scalar;
            this.op = op;
            this.opCode = opCode;
            this.size = size;
            if (size <= LAZY_ARRAY_MEMO_MAX_SIZE) {
                this.memoValues = new Object[size];
                this.memoComputed = new boolean[size];
            } else {
                this.memoValues = null;
                this.memoComputed = null;
            }
        }

        @Override
        public Object get(int index) {
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
            }
            if (materialized != null) {
                return materialized.get(index);
            }
            if (memoComputed != null && memoComputed[index]) {
                return memoValues[index];
            }
            Object elem = source.get(index);
            Object computed;
            if (isArray(elem)) {
                computed = applyArrayOperation(elem, scalar, op);
            } else {
                computed = applyScalarByOpCode(elem, scalar, opCode);
            }
            if (memoComputed != null) {
                memoValues[index] = computed;
                memoComputed[index] = true;
            }
            return computed;
        }

        @Override
        public int size() {
            if (materialized != null) {
                return materialized.size();
            }
            return size;
        }

        @Override
        public Object set(int index, Object element) {
            return ensureMaterialized().set(index, element);
        }

        @Override
        public void add(int index, Object element) {
            ensureMaterialized().add(index, element);
        }

        @Override
        public Object remove(int index) {
            return ensureMaterialized().remove(index);
        }

        private List<Object> ensureMaterialized() {
            if (materialized != null) {
                return materialized;
            }
            List<Object> eager = new ArrayList<Object>(size);
            for (int i = 0; i < size; i++) {
                eager.add(get(i));
            }
            materialized = eager;
            return materialized;
        }
    }

    private Object applyScalarOperation(Object a, Object b, String op) {
        if (isArray(a) || isArray(b)) {
            return applyArrayOperation(a, b, op);
        }
        return applyScalarByOpCode(a, b, resolveArrayOpCode(op));
    }
    
    private int resolveArrayOpCode(String op) {
        if ("+".equals(op)) return 1;
        if ("-".equals(op)) return 2;
        if ("*".equals(op)) return 3;
        if ("/".equals(op)) return 4;
        if ("%".equals(op)) return 5;
        throw new InternalError("Unsupported array operation: " + op);
    }
    
    private Object applyScalarByOpCode(Object a, Object b, int opCode) {
        switch (opCode) {
            case 1: return addScalars(a, b);
            case 2: return subtractScalars(a, b);
            case 3: return multiplyScalars(a, b);
            case 4: return divideScalars(a, b);
            case 5: return modulusScalars(a, b);
            default: throw new InternalError("Unsupported scalar op code: " + opCode);
        }
    }
    
    private Object multiplyString(Object a, Object b) {
        String str = null;
        int repeat = 0;
        
        if (a instanceof TextLiteral && isNumeric(b)) {
            str = ((TextLiteral) a).value;
            repeat = (int) toAutoStackingNumber(b).longValue();
        } else if (b instanceof TextLiteral && isNumeric(a)) {
            str = ((TextLiteral) b).value;
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
        return divideScalars(a, b);
    }
    
    private Object divideScalars(Object a, Object b) {
        long[] fastPair = getFastLongPair(a, b);
        if (fastPair != null) {
            long av = fastPair[0];
            long bv = fastPair[1];
            if (bv == 0L) {
                throw new ProgramError("Division by zero");
            }
            if (av % bv == 0L) {
                return AutoStackingNumber.fromLong(av / bv);
            }
            return AutoStackingNumber.fromDouble((double) av / (double) bv);
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
        return modulusScalars(a, b);
    }
    
    private Object modulusScalars(Object a, Object b) {
        long[] fastPair = getFastLongPair(a, b);
        if (fastPair != null) {
            long av = fastPair[0];
            long bv = fastPair[1];
            if (bv == 0L) {
                throw new ProgramError("Modulus by zero");
            }
            return AutoStackingNumber.fromLong(av % bv);
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
        if (a instanceof TextLiteral || b instanceof TextLiteral ||
            a instanceof String || b instanceof String) {
            String strA = a instanceof TextLiteral ? ((TextLiteral) a).value : String.valueOf(a);
            String strB = b instanceof TextLiteral ? ((TextLiteral) b).value : String.valueOf(b);
            return strA.compareTo(strB);
        }

        long[] fastPair = getFastLongPair(a, b);
        if (fastPair != null) {
            long av = fastPair[0];
            long bv = fastPair[1];
            return av < bv ? -1 : (av == bv ? 0 : 1);
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
            if (value instanceof TextLiteral) {
                String str = ((TextLiteral) value).value;
                if (isValidTypeSignature(str)) {
                    return Value.createTypeValue(str);
                }
            }
            throw new ProgramError("Cannot convert '" + value + "' to type");
        }
        
        if (targetType.equals("none")) {
            return new NoneLiteral();
        }

        if (isUnsafeNumericType(targetType)) {
            return convertUnsafeNumeric(value, targetType);
        }

        if (isPointerType(targetType)) {
            Object unwrapped = unwrap(value);
            if (unwrapped instanceof PointerValue) {
                PointerValue pointer = (PointerValue) unwrapped;
                String expectedPointedType = normalizeTypeSignature(targetType.substring(1));
                String actualPointedType = normalizeTypeSignature(pointer.pointedType);
                if (expectedPointedType.equals(actualPointedType)) {
                    return pointer;
                }
            }
            throw new ProgramError("Cannot convert '" + value + "' to pointer type " + targetType);
        }
        
        if (value instanceof FloatLiteral) {
            AutoStackingNumber num = ((FloatLiteral) value).value;
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
        
        if (value instanceof IntLiteral) {
            AutoStackingNumber num = ((IntLiteral) value).value;
            if (targetType.equals(INT.toString())) return num.longValue();
            if (targetType.equals(FLOAT.toString())) return num;
            if (targetType.equals(TEXT.toString())) return num.toString();
        }
        
        if (value instanceof BoolLiteral) {
            boolean val = ((BoolLiteral) value).value;
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
        
        if (value instanceof TextLiteral) {
            String str = ((TextLiteral) value).value;
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
            if (value instanceof BoolLiteral) return ((BoolLiteral) value).value;
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
            if (value instanceof TextLiteral) {
                String strVal = ((TextLiteral) value).value.toLowerCase().trim();
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

    private Object convertUnsafeNumeric(Object value, String targetType) {
        if (targetType.equals("f32")) {
            double numeric = toDouble(value);
            return AutoStackingNumber.fromDouble((double) ((float) numeric));
        }
        if (targetType.equals("f64")) {
            double numeric = toDouble(value);
            return AutoStackingNumber.fromDouble(numeric);
        }
        BigInteger integral = toIntegralBigInteger(value);
        return wrapIntegerUnsafe(integral, targetType);
    }

    private BigInteger toIntegralBigInteger(Object value) {
        Object unwrapped = unwrap(value);
        if (unwrapped instanceof IntLiteral) {
            return new BigInteger(((IntLiteral) unwrapped).value.toString());
        }
        if (unwrapped instanceof FloatLiteral) {
            AutoStackingNumber n = ((FloatLiteral) unwrapped).value;
            return new BigDecimal(n.toString()).toBigInteger();
        }
        if (unwrapped instanceof AutoStackingNumber) {
            return new BigDecimal(((AutoStackingNumber) unwrapped).toString()).toBigInteger();
        }
        if (unwrapped instanceof Integer || unwrapped instanceof Long) {
            return BigInteger.valueOf(((Number) unwrapped).longValue());
        }
        if (unwrapped instanceof Float || unwrapped instanceof Double) {
            return BigDecimal.valueOf(((Number) unwrapped).doubleValue()).toBigInteger();
        }
        throw new ProgramError(
            "Unsafe numeric types require int or float values, got: " + getConcreteType(unwrapped));
    }

    private AutoStackingNumber wrapIntegerUnsafe(BigInteger value, String targetType) {
        int bits = 64;
        boolean signed = true;
        if (targetType.equals("i8")) bits = 8;
        else if (targetType.equals("i16")) bits = 16;
        else if (targetType.equals("i32")) bits = 32;
        else if (targetType.equals("i64")) bits = 64;
        else if (targetType.equals("u8")) { bits = 8; signed = false; }
        else if (targetType.equals("u16")) { bits = 16; signed = false; }
        else if (targetType.equals("u32")) { bits = 32; signed = false; }
        else if (targetType.equals("u64")) { bits = 64; signed = false; }

        BigInteger modulus = BigInteger.ONE.shiftLeft(bits);
        BigInteger wrapped = value.mod(modulus);
        if (signed) {
            BigInteger signBoundary = BigInteger.ONE.shiftLeft(bits - 1);
            if (wrapped.compareTo(signBoundary) >= 0) {
                wrapped = wrapped.subtract(modulus);
            }
        }
        return AutoStackingNumber.valueOf(wrapped.toString());
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
    if (value instanceof NoneLiteral) return "none";
    
    if (value instanceof NaturalArray) {
        NaturalArray arr = (NaturalArray) value;
        if (arr.hasPendingUpdates()) {
            arr.commitUpdates();
        }
        // Return the element type of the array, not "list"
        return arr.getElementType();
    }

    if (value instanceof PointerValue) {
        return "*" + ((PointerValue) value).pointedType;
    }
    
    if (value instanceof IntLiteral) return INT.toString();
    if (value instanceof FloatLiteral) return FLOAT.toString();
    if (value instanceof TextLiteral) return TEXT.toString();
    if (value instanceof BoolLiteral) return BOOL.toString();
    
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
        String typeSigTrimmed = normalizeTypeSignature(typeSig);
        if (typeSigTrimmed == null) {
            return true;
        }
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
        
        if (a instanceof IntLiteral && b instanceof IntLiteral) {
            return ((IntLiteral) a).value.compareTo(((IntLiteral) b).value) == 0;
        }
        
        if (a instanceof FloatLiteral && b instanceof FloatLiteral) {
            return ((FloatLiteral) a).value.compareTo(((FloatLiteral) b).value) == 0;
        }
        
        if (a instanceof TextLiteral && b instanceof TextLiteral) {
            return ((TextLiteral) a).value.equals(((TextLiteral) b).value);
        }
        
        if (a instanceof BoolLiteral && b instanceof BoolLiteral) {
            return ((BoolLiteral) a).value == ((BoolLiteral) b).value;
        }
        
        return a.equals(b);
    }

    private boolean validateTypeInternal(String typeSig, Object rawValue, String concreteType) {
        if (typeSig == null) return true;
        String type = normalizeTypeSignature(typeSig);

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

        if (isPointerType(type)) {
            if (isNoneValue(rawValue)) return false;
            Object unwrapped = unwrap(rawValue);
            if (!(unwrapped instanceof PointerValue)) return false;
            PointerValue pointer = (PointerValue) unwrapped;
            String pointedType = normalizeTypeSignature(pointer.pointedType);
            String expectedPointedType = normalizeTypeSignature(type.substring(1));
            return expectedPointedType.equals(pointedType);
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
            if (rawValue instanceof TextLiteral) {
                String str = ((TextLiteral) rawValue).value;
                return isValidTypeSignature(str);
            }
            return false;
        }
        
        return type.equals(concreteType) || checkPrimitiveMatch(type, rawValue);
    }

    private boolean isValidTypeSignature(String str) {
        if (str == null || str.isEmpty()) return false;

        if (isPointerType(str)) {
            return isValidTypeSignature(str.substring(1));
        }
        if (isSizedArrayType(str)) {
            return isValidTypeSignature(getSizedArrayElementType(str));
        }
        
        if (str.equals("int") || str.equals("float") || str.equals("text") || 
            str.equals("bool") || str.equals("type") || str.equals("none") ||
            isUnsafeNumericType(str)) {
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

        if (isPointerType(type)) {
            return isTypeStructurallyValid(type.substring(1));
        }
        if (isSizedArrayType(type)) {
            return isTypeStructurallyValid(getSizedArrayElementType(type));
        }
        
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
            type.equals(ANY.toString()) || type.equals("none") ||
            isUnsafeNumericType(type)) {
            return true;
        }
        if (Character.isUpperCase(type.charAt(0))) return true;
        return false;
    }

    private boolean checkPrimitiveMatch(String type, Object rawValue) {
        if (isPointerType(type)) {
            Object unwrapped = unwrap(rawValue);
            return unwrapped instanceof PointerValue;
        }
        
        if (type.startsWith("[") && type.endsWith("]")) {
            if (type.equals("[]")) {
                return rawValue instanceof List || rawValue instanceof NaturalArray;
            }
            return rawValue instanceof List || rawValue instanceof NaturalArray;
        }
        
        if (type.equals(INT.toString())) {
            return rawValue instanceof IntLiteral ||
                   rawValue instanceof Integer || 
                   rawValue instanceof Long ||
                   (rawValue instanceof AutoStackingNumber && 
                    ((AutoStackingNumber) rawValue).fitsInStacks(1));
        } else if (type.equals(TEXT.toString())) {
            return rawValue instanceof TextLiteral ||
                   rawValue instanceof String;
        } else if (type.equals(FLOAT.toString())) {
            return rawValue instanceof FloatLiteral ||
                   rawValue instanceof Float || 
                   rawValue instanceof Double || 
                   rawValue instanceof AutoStackingNumber;
        } else if (type.equals(BOOL.toString())) {
            return rawValue instanceof BoolLiteral ||
                   rawValue instanceof Boolean;
        } else if (type.equals("none")) {
            return isNoneValue(rawValue);
        } else if (isUnsafeNumericType(type)) {
            return rawValue instanceof IntLiteral
                || rawValue instanceof FloatLiteral
                || rawValue instanceof Integer
                || rawValue instanceof Long
                || rawValue instanceof Float
                || rawValue instanceof Double
                || rawValue instanceof AutoStackingNumber
                || (rawValue instanceof Value && isUnsafeNumericType(((Value) rawValue).activeType));
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
