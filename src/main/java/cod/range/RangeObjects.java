package cod.range;

import cod.ast.node.Type;
import cod.error.ProgramError;
import cod.interpreter.context.ObjectInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class RangeObjects {
    private static final String RANGE_MARKER = "__coderiveRangeSpec";
    private static final String MULTI_MARKER = "__coderiveMultiRangeSpec";
    private static final String STEP_FIELD = "step";
    private static final String START_FIELD = "start";
    private static final String END_FIELD = "end";
    private static final String RANGES_FIELD = "ranges";

    private RangeObjects() {}

    public static ObjectInstance createRangeSpec(Type type, Object step, Object start, Object end) {
        ObjectInstance instance = new ObjectInstance(type);
        instance.fields.put(RANGE_MARKER, Boolean.TRUE);
        instance.fields.put(STEP_FIELD, step);
        instance.fields.put(START_FIELD, start);
        instance.fields.put(END_FIELD, end);
        return instance;
    }

    public static ObjectInstance createMultiRangeSpec(Type type, List<Object> ranges) {
        ObjectInstance instance = new ObjectInstance(type);
        instance.fields.put(MULTI_MARKER, Boolean.TRUE);
        // Defensive copy to preserve immutability of runtime multi-range objects.
        instance.fields.put(RANGES_FIELD, new ArrayList<Object>(ranges != null ? ranges : Collections.<Object>emptyList()));
        return instance;
    }

    public static boolean isRangeSpec(Object value) {
        if (!(value instanceof ObjectInstance)) return false;
        ObjectInstance instance = (ObjectInstance) value;
        Object markerValue = instance.fields.get(RANGE_MARKER);
        if (markerValue instanceof Boolean) return ((Boolean) markerValue).booleanValue();
        return instance.fields.containsKey(START_FIELD)
            && instance.fields.containsKey(END_FIELD)
            && instance.fields.containsKey(STEP_FIELD);
    }

    public static boolean isMultiRangeSpec(Object value) {
        if (!(value instanceof ObjectInstance)) return false;
        ObjectInstance instance = (ObjectInstance) value;
        if (hasMarker(instance.fields, MULTI_MARKER)) return true;
        return instance.fields.get(RANGES_FIELD) instanceof List<?>;
    }

    public static Object getStep(Object range) {
        return asRange(range).fields.get(STEP_FIELD);
    }

    public static Object getStart(Object range) {
        return asRange(range).fields.get(START_FIELD);
    }

    public static Object getEnd(Object range) {
        return asRange(range).fields.get(END_FIELD);
    }

    @SuppressWarnings("unchecked")
    public static List<Object> getRanges(Object multiRange) {
        ObjectInstance instance = asMultiRange(multiRange);
        Object rangesObj = instance.fields.get(RANGES_FIELD);
        if (!(rangesObj instanceof List<?>)) {
            throw new ProgramError("Invalid MultiRangeSpec: ranges must be a list");
        }
        List<Object> ranges = (List<Object>) rangesObj;
        for (Object range : ranges) {
            if (!isRangeSpec(range)) {
                throw new ProgramError("Invalid MultiRangeSpec: ranges must contain RangeSpec values");
            }
        }
        return ranges;
    }

    private static ObjectInstance asRange(Object range) {
        if (!isRangeSpec(range)) {
            throw new ProgramError("Expected internal.range.RangeSpec value");
        }
        return (ObjectInstance) range;
    }

    private static ObjectInstance asMultiRange(Object multiRange) {
        if (!isMultiRangeSpec(multiRange)) {
            throw new ProgramError("Expected internal.range.MultiRangeSpec value");
        }
        return (ObjectInstance) multiRange;
    }

    private static boolean hasMarker(Map<String, Object> fields, String marker) {
        Object markerValue = fields.get(marker);
        return markerValue instanceof Boolean && ((Boolean) markerValue).booleanValue();
    }
}
