package cod.ir;

import cod.ast.nodes.*;
import cod.math.AutoStackingNumber;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class IRCodec {
    static final int MAGIC = 0xAC0D1EB1;
    static final int VERSION = 1;

    private static final int MAX_DEPTH = 512;
    private static final int MAX_STRING_BYTES = 4 * 1024 * 1024;
    private static final int MAX_COLLECTION_SIZE = 1_000_000;
    private static final int MAX_FIELDS = 1_000;

    private static final byte TAG_NULL = 0;
    private static final byte TAG_STRING = 1;
    private static final byte TAG_BOOL = 2;
    private static final byte TAG_INT = 3;
    private static final byte TAG_LONG = 4;
    private static final byte TAG_DOUBLE = 5;
    private static final byte TAG_ENUM = 6;
    private static final byte TAG_LIST = 7;
    private static final byte TAG_MAP = 8;
    private static final byte TAG_NODE = 9;
    private static final byte TAG_AUTO_STACKING = 10;

    private static final String NODE_PACKAGE_PREFIX = "cod.ast.nodes.";

    private static final Map<Class<?>, List<Field>> FIELD_CACHE = new HashMap<Class<?>, List<Field>>();

    private IRCodec() {}

    static void writeHeader(DataOutput out) throws IOException {
        out.writeInt(MAGIC);
        out.writeInt(VERSION);
    }

    static void readHeader(DataInput in) throws IOException {
        int magic = in.readInt();
        if (magic != MAGIC) {
            throw new IOException("Invalid IR magic number: expected 0x" + Integer.toHexString(MAGIC) +
                    ", got 0x" + Integer.toHexString(magic));
        }

        int version = in.readInt();
        if (version != VERSION) {
            throw new IOException("Unsupported IR version: expected " + VERSION + ", got " + version);
        }
    }

    static void writeValue(DataOutput out, Object value, int depth) throws IOException {
        ensureDepth(depth);

        if (value == null) {
            out.writeByte(TAG_NULL);
            return;
        }

        if (value instanceof String) {
            out.writeByte(TAG_STRING);
            writeString(out, (String) value);
            return;
        }

        if (value instanceof Boolean) {
            out.writeByte(TAG_BOOL);
            out.writeBoolean(((Boolean) value).booleanValue());
            return;
        }

        if (value instanceof Integer) {
            out.writeByte(TAG_INT);
            out.writeInt(((Integer) value).intValue());
            return;
        }

        if (value instanceof Long) {
            out.writeByte(TAG_LONG);
            out.writeLong(((Long) value).longValue());
            return;
        }

        if (value instanceof Float) {
            out.writeByte(TAG_DOUBLE);
            out.writeDouble(((Float) value).doubleValue());
            return;
        }

        if (value instanceof Double) {
            out.writeByte(TAG_DOUBLE);
            out.writeDouble(((Double) value).doubleValue());
            return;
        }

        if (value instanceof Enum) {
            out.writeByte(TAG_ENUM);
            Enum<?> e = (Enum<?>) value;
            writeString(out, e.getDeclaringClass().getName());
            writeString(out, e.name());
            return;
        }

        if (value instanceof AutoStackingNumber) {
            out.writeByte(TAG_AUTO_STACKING);
            writeString(out, value.toString());
            return;
        }

        if (value instanceof List) {
            out.writeByte(TAG_LIST);
            List<?> list = (List<?>) value;
            ensureCollectionSize(list.size(), "list");
            out.writeInt(list.size());
            for (Object item : list) {
                writeValue(out, item, depth + 1);
            }
            return;
        }

        if (value instanceof Map) {
            out.writeByte(TAG_MAP);
            Map<?, ?> map = (Map<?, ?>) value;
            ensureCollectionSize(map.size(), "map");
            out.writeInt(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object key = entry.getKey();
                if (!(key instanceof String)) {
                    throw new IOException("Only String map keys are supported in IR, got: " +
                            (key == null ? "null" : key.getClass().getName()));
                }
                writeString(out, (String) key);
                writeValue(out, entry.getValue(), depth + 1);
            }
            return;
        }

        if (value instanceof ASTNode) {
            writeNode(out, (ASTNode) value, depth + 1);
            return;
        }

        throw new IOException("Unsupported IR value type: " + value.getClass().getName());
    }

    static Object readValue(DataInput in, int depth) throws IOException {
        ensureDepth(depth);
        byte tag = in.readByte();

        switch (tag) {
            case TAG_NULL:
                return null;
            case TAG_STRING:
                return readString(in);
            case TAG_BOOL:
                return Boolean.valueOf(in.readBoolean());
            case TAG_INT:
                return Integer.valueOf(in.readInt());
            case TAG_LONG:
                return Long.valueOf(in.readLong());
            case TAG_DOUBLE:
                return Double.valueOf(in.readDouble());
            case TAG_ENUM:
                return readEnum(in);
            case TAG_LIST:
                return readList(in, depth + 1);
            case TAG_MAP:
                return readMap(in, depth + 1);
            case TAG_NODE:
                return readNode(in, depth + 1);
            case TAG_AUTO_STACKING:
                return AutoStackingNumber.valueOf(readString(in));
            default:
                throw new IOException("Unknown IR tag: " + tag);
        }
    }

    private static void writeNode(DataOutput out, ASTNode node, int depth) throws IOException {
        out.writeByte(TAG_NODE);
        writeString(out, node.getClass().getName());
        List<Field> fields = getSerializableFields(node.getClass());
        ensureCollectionSize(fields.size(), "fields");
        out.writeInt(fields.size());

        for (Field field : fields) {
            writeString(out, field.getName());
            try {
                Object value = field.get(node);
                writeValue(out, value, depth + 1);
            } catch (IllegalAccessException e) {
                throw new IOException("Cannot access field '" + field.getName() + "' of " +
                        node.getClass().getName(), e);
            }
        }
    }

    private static ASTNode readNode(DataInput in, int depth) throws IOException {
        String className = readString(in);
        if (!className.startsWith(NODE_PACKAGE_PREFIX)) {
            throw new IOException("Invalid IR node class: " + className);
        }

        Class<?> rawClass;
        try {
            rawClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IOException("Unknown IR node class: " + className, e);
        }

        if (!ASTNode.class.isAssignableFrom(rawClass)) {
            throw new IOException("IR class is not an ASTNode: " + className);
        }

        @SuppressWarnings("unchecked")
        Class<? extends ASTNode> nodeClass = (Class<? extends ASTNode>) rawClass;

        int fieldCount = in.readInt();
        if (fieldCount < 0 || fieldCount > MAX_FIELDS) {
            throw new IOException("Invalid IR field count: " + fieldCount + " for " + className);
        }

        Map<String, Object> values = new LinkedHashMap<String, Object>();
        for (int i = 0; i < fieldCount; i++) {
            String fieldName = readString(in);
            Object value = readValue(in, depth + 1);
            values.put(fieldName, value);
        }

        ASTNode node = instantiateNode(nodeClass, values);
        applyRemainingFields(node, values);
        return node;
    }

    private static ASTNode instantiateNode(Class<? extends ASTNode> nodeClass, Map<String, Object> values)
            throws IOException {
        if (nodeClass == IdentifierNode.class) {
            return new IdentifierNode((String) values.get("name"));
        }
        if (nodeClass == TextLiteralNode.class) {
            String value = (String) values.get("value");
            Boolean interpolated = (Boolean) values.get("isInterpolated");
            return new TextLiteralNode(value, interpolated != null && interpolated.booleanValue());
        }
        if (nodeClass == BoolLiteralNode.class) {
            Boolean value = (Boolean) values.get("value");
            return new BoolLiteralNode(value != null && value.booleanValue());
        }
        if (nodeClass == IntLiteralNode.class) {
            AutoStackingNumber value = (AutoStackingNumber) values.get("value");
            return new IntLiteralNode(value);
        }
        if (nodeClass == FloatLiteralNode.class) {
            AutoStackingNumber value = (AutoStackingNumber) values.get("value");
            return new FloatLiteralNode(value);
        }
        if (nodeClass == RangeIndexNode.class) {
            return new RangeIndexNode(
                    (ExprNode) values.get("step"),
                    (ExprNode) values.get("start"),
                    (ExprNode) values.get("end")
            );
        }
        if (nodeClass == MultiRangeIndexNode.class) {
            @SuppressWarnings("unchecked")
            List<RangeIndexNode> ranges = (List<RangeIndexNode>) values.get("ranges");
            return new MultiRangeIndexNode(ranges);
        }
        if (nodeClass == ChainedComparisonNode.class) {
            @SuppressWarnings("unchecked")
            List<ExprNode> expressions = (List<ExprNode>) values.get("expressions");
            @SuppressWarnings("unchecked")
            List<String> operators = (List<String>) values.get("operators");
            return new ChainedComparisonNode(expressions, operators);
        }
        if (nodeClass == ValueExprNode.class) {
            return new ValueExprNode(values.get("value"));
        }
        if (nodeClass == ThisNode.class) {
            String className = (String) values.get("className");
            return className == null ? new ThisNode() : new ThisNode(className);
        }

        try {
            Constructor<? extends ASTNode> ctor = nodeClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new IOException("No usable constructor for IR node class: " + nodeClass.getName(), e);
        }
    }

    private static void applyRemainingFields(ASTNode node, Map<String, Object> values) throws IOException {
        List<Field> fields = getSerializableFields(node.getClass());
        for (Field field : fields) {
            if (Modifier.isFinal(field.getModifiers())) {
                continue;
            }
            if (!values.containsKey(field.getName())) {
                continue;
            }
            try {
                field.set(node, values.get(field.getName()));
            } catch (IllegalAccessException e) {
                throw new IOException("Cannot set field '" + field.getName() + "' for " +
                        node.getClass().getName(), e);
            }
        }
    }

    private static Enum<?> readEnum(DataInput in) throws IOException {
        String enumClassName = readString(in);
        String enumName = readString(in);

        Class<?> enumClass;
        try {
            enumClass = Class.forName(enumClassName);
        } catch (ClassNotFoundException e) {
            throw new IOException("Unknown enum class in IR: " + enumClassName, e);
        }

        if (!enumClass.isEnum()) {
            throw new IOException("IR enum class is not enum: " + enumClassName);
        }

        try {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Enum<?> e = Enum.valueOf((Class<? extends Enum>) enumClass, enumName);
            return e;
        } catch (IllegalArgumentException e) {
            throw new IOException("Unknown enum constant '" + enumName + "' for " + enumClassName, e);
        }
    }

    private static List<Object> readList(DataInput in, int depth) throws IOException {
        int size = in.readInt();
        ensureCollectionSize(size, "list");
        List<Object> list = new ArrayList<Object>(size);
        for (int i = 0; i < size; i++) {
            list.add(readValue(in, depth + 1));
        }
        return list;
    }

    private static Map<String, Object> readMap(DataInput in, int depth) throws IOException {
        int size = in.readInt();
        ensureCollectionSize(size, "map");
        Map<String, Object> map = new LinkedHashMap<String, Object>(size);
        for (int i = 0; i < size; i++) {
            String key = readString(in);
            Object value = readValue(in, depth + 1);
            map.put(key, value);
        }
        return map;
    }

    private static void writeString(DataOutput out, String value) throws IOException {
        if (value == null) {
            out.writeInt(-1);
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_STRING_BYTES) {
            throw new IOException("String exceeds IR limit: " + bytes.length + " bytes");
        }
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInput in) throws IOException {
        int len = in.readInt();
        if (len == -1) {
            return null;
        }
        if (len < 0 || len > MAX_STRING_BYTES) {
            throw new IOException("Invalid string length in IR: " + len);
        }
        byte[] bytes = new byte[len];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static List<Field> getSerializableFields(Class<?> clazz) throws IOException {
        List<Field> cached = FIELD_CACHE.get(clazz);
        if (cached != null) {
            return cached;
        }

        List<Field> fields = new ArrayList<Field>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            Field[] declared = current.getDeclaredFields();
            for (Field field : declared) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic()) {
                    continue;
                }
                field.setAccessible(true);
                fields.add(field);
            }
            current = current.getSuperclass();
        }

        Collections.sort(fields, new Comparator<Field>() {
            @Override
            public int compare(Field a, Field b) {
                return a.getName().compareTo(b.getName());
            }
        });

        if (fields.size() > MAX_FIELDS) {
            throw new IOException("Too many serializable fields in " + clazz.getName() + ": " + fields.size());
        }

        FIELD_CACHE.put(clazz, fields);
        return fields;
    }

    private static void ensureDepth(int depth) throws IOException {
        if (depth > MAX_DEPTH) {
            throw new IOException("IR structure exceeds maximum depth of " + MAX_DEPTH);
        }
    }

    private static void ensureCollectionSize(int size, String kind) throws IOException {
        if (size < 0 || size > MAX_COLLECTION_SIZE) {
            throw new IOException("Invalid " + kind + " size in IR: " + size);
        }
    }
}
