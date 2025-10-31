package cdrv.interpreter;

import cdrv.debug.DebugSystem;
import java.util.List;

public class TypeSystem {

    // ADD TYPE CONVERSION METHOD
    public Object convertType(Object value, String targetType) {
        try {
            switch (targetType) {
                case "int":
                    if (value instanceof Integer) return value;
                    if (value instanceof Float) return ((Float) value).intValue();
                    if (value instanceof Double) return ((Double) value).intValue();
                    if (value instanceof String) return Integer.parseInt((String) value);
                    return (int) toDouble(value);

                case "float":
                    if (value instanceof Float) return value;
                    return (float) toDouble(value);

                case "string":
                    return String.valueOf(value);

                case "bool":
                    if (value instanceof Boolean) return value;
                    if (value instanceof String) {
                        String str = ((String) value).toLowerCase();
                        return str.equals("true") || str.equals("1") || str.equals("yes");
                    }
                    return toDouble(value) != 0;

                default:
                    return value; // Unknown type, return as-is
            }
        } catch (Exception e) {
            DebugSystem.error("TYPECAST", "Failed to convert " + value + " to " + targetType);
            throw new RuntimeException("Type conversion failed: " + value + " to " + targetType);
        }
    }

    public int compare(Object a, Object b) {
        double da = toDouble(a);
        double db = toDouble(b);
        return Double.compare(da, db);
    }

    public Object addNumbers(Object a, Object b) {
        if (a instanceof Integer && b instanceof Integer) return (Integer) a + (Integer) b;
        double result = toDouble(a) + toDouble(b);
        return result == Math.floor(result) ? (int) result : result;
    }

    public Object subtractNumbers(Object a, Object b) {
        if (a instanceof Integer && b instanceof Integer) return (Integer) a - (Integer) b;
        double result = toDouble(a) - toDouble(b);
        return result == Math.floor(result) ? (int) result : result;
    }

    public Object multiplyNumbers(Object a, Object b) {
        if (a instanceof Integer && b instanceof Integer) return (Integer) a * (Integer) b;
        double result = toDouble(a) * toDouble(b);
        return result == Math.floor(result) ? (int) result : result;
    }

    public Object divideNumbers(Object a, Object b) {
        double result = toDouble(a) / toDouble(b);
        return result == Math.floor(result) ? (int) result : result;
    }

    public Object modulusNumbers(Object a, Object b) {
        if (a instanceof Integer && b instanceof Integer) {
            int intB = (Integer) b;
            if (intB == 0) throw new RuntimeException("Modulus by zero");
            return (Integer) a % intB;
        }

        double da = toDouble(a);
        double db = toDouble(b);
        if (db == 0) throw new RuntimeException("Modulus by zero");

        double result = da % db;
        return result == Math.floor(result) ? (int) result : result;
    }

    public double toDouble(Object val) {
        if (val instanceof Integer) return ((Integer) val).doubleValue();
        if (val instanceof Float) return ((Float) val).doubleValue();
        if (val instanceof Double) return (Double) val;
        if (val instanceof List) {
            // For arrays, return their size
            return ((List<?>) val).size();
        }
        throw new RuntimeException("Cannot convert to number: " + val);
    }

    /**
     * Negates a number value (unary minus operation) Handles different numeric types: Integer,
     * Float, Double Also handles string conversion for numeric strings
     */
    public Object negateNumber(Object operand) {
        DebugSystem.debug(
                "UNARY",
                "Negating: " + operand + " (type: " + operand.getClass().getSimpleName() + ")");

        if (operand instanceof Integer) {
            int value = (Integer) operand;
            return -value;
        } else if (operand instanceof Float) {
            float value = (Float) operand;
            return -value;
        } else if (operand instanceof Double) {
            double value = (Double) operand;
            return -value;
        } else if (operand instanceof String) {
            // Try to parse string as number and negate
            try {
                String str = (String) operand;
                if (str.contains(".")) {
                    return -Double.parseDouble(str);
                } else {
                    int intValue = Integer.parseInt(str);
                    return -intValue;
                }
            } catch (NumberFormatException e) {
                throw new RuntimeException("Cannot negate non-numeric string: " + operand);
            }
        } else {
            // Fallback: convert to double and negate
            try {
                double value = toDouble(operand);
                // Return same type if possible, otherwise return as double
                if (operand instanceof Integer) {
                    return (int) -value;
                } else if (operand instanceof Float) {
                    return (float) -value;
                } else {
                    return -value;
                }
            } catch (Exception e) {
                throw new RuntimeException(
                        "Cannot negate value: "
                                + operand
                                + " (type: "
                                + operand.getClass().getSimpleName()
                                + ")");
            }
        }
    }
}
