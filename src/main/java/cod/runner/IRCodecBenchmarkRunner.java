package cod.runner;

import cod.ast.nodes.*;
import cod.ir.IRWriter;
import cod.math.AutoStackingNumber;
import cod.syntax.Keyword;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class IRCodecBenchmarkRunner {
    private static final int WARMUP_ITERATIONS = 50;
    private static final int MEASURE_ITERATIONS = 200;

    public static void main(String[] args) {
        try {
            Type sample = createSampleType();

            File tempDir = new File(System.getProperty("java.io.tmpdir"));
            File irFile = new File(tempDir, "coderive-ir-codec-benchmark.codb");
            File reflectionFile = new File(tempDir, "coderive-reflection-codec-benchmark.codb");
            IRWriter writer = new IRWriter();
            ReflectionSerializer reflectionSerializer = new ReflectionSerializer();

            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                writer.write(irFile, sample);
                reflectionSerializer.write(reflectionFile, sample);
            }

            long visitorNanos = benchmarkVisitorSerialization(writer, irFile, sample);
            long reflectionNanos = benchmarkReflectionSerialization(reflectionSerializer, reflectionFile, sample);

            double visitorMs = visitorNanos / 1_000_000.0;
            double reflectionMs = reflectionNanos / 1_000_000.0;
            double ratio = visitorNanos == 0 ? 0.0 : ((double) reflectionNanos / (double) visitorNanos);

            System.out.println("IR codec benchmark (" + MEASURE_ITERATIONS + " iterations)");
            System.out.println("visitor serialize to bytes    : " + String.format("%.3f", visitorMs) + " ms");
            System.out.println("reflection serialize to bytes : " + String.format("%.3f", reflectionMs) + " ms");
            System.out.println("reflection/visitor ratio      : " + String.format("%.3f", ratio) + "x");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static long benchmarkVisitorSerialization(IRWriter writer, File file, Type sample) throws IOException {
        long start = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            writer.write(file, sample);
        }
        return System.nanoTime() - start;
    }

    private static long benchmarkReflectionSerialization(ReflectionSerializer serializer, File file, Type sample)
            throws IOException {
        long start = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            serializer.write(file, sample);
        }
        return System.nanoTime() - start;
    }

    private static Type createSampleType() {
        Type type = new Type();
        type.name = "BenchType";
        type.visibility = Keyword.SHARE;

        for (int i = 0; i < 20; i++) {
            Field field = new Field();
            field.name = "field" + i;
            field.type = "Int";
            field.visibility = Keyword.LOCAL;
            field.value = new IntLiteral(i);
            type.fields.add(field);
        }

        for (int m = 0; m < 15; m++) {
            Method method = new Method();
            method.methodName = "method" + m;
            method.associatedClass = type.name;
            method.visibility = Keyword.SHARE;

            for (int p = 0; p < 4; p++) {
                Param param = new Param();
                param.name = "p" + p;
                param.type = "Int";
                method.parameters.add(param);
            }

            Slot returnSlot = new Slot();
            returnSlot.name = "result";
            returnSlot.type = "Int";
            method.returnSlots.add(returnSlot);

            method.body.add(createMethodBody(m));
            type.methods.add(method);
        }

        type.statements.add(createMethodBody(999));
        return type;
    }

    private static Stmt createMethodBody(int seed) {
        Block block = new Block();

        Var var = new Var();
        var.name = "x" + seed;
        var.explicitType = "Int";
        var.value = new BinaryOp(new IntLiteral(seed), "+", new IntLiteral(seed + 1));
        block.statements.add(var);

        Assignment assignment = new Assignment();
        assignment.left = new Identifier(var.name);
        assignment.right = new BinaryOp(new Identifier(var.name), "*", new IntLiteral(2));
        block.statements.add(assignment);

        MethodCall call = new MethodCall();
        call.name = "print";
        call.arguments.add(new TextLiteral("seed=" + seed));
        call.arguments.add(new FloatLiteral(AutoStackingNumber.valueOf("1.5")));
        block.statements.add(call);

        ReturnSlotAssignment ret = new ReturnSlotAssignment();
        ret.variableNames.add("result");
        MethodCall compute = new MethodCall();
        compute.name = "compute";
        compute.arguments.add(new Identifier(var.name));
        ret.methodCall = compute;
        block.statements.add(ret);

        return block;
    }

    private static final class ReflectionSerializer {
        private static final int MAGIC = 0xAC0D1EB1;
        private static final int VERSION = 1;

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

        private ReflectionSerializer() {}

        void write(File file, Type type) throws IOException {
            if (file == null) {
                throw new IOException("IR target file is null");
            }
            if (type == null) {
                throw new IOException("IR type is null");
            }

            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("Failed to create IR directory: " + parent.getAbsolutePath());
            }

            DataOutputStream out = null;
            try {
                out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
                out.writeInt(MAGIC);
                out.writeInt(VERSION);
                writeValue(out, type, new IdentityHashMap<Object, Boolean>());
                out.flush();
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException ignored) {}
                }
            }
        }

        private void writeValue(DataOutputStream out, Object value, IdentityHashMap<Object, Boolean> seen)
                throws IOException {
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

            if (value instanceof Enum<?>) {
                out.writeByte(TAG_ENUM);
                Enum<?> enumValue = (Enum<?>) value;
                writeString(out, enumValue.getDeclaringClass().getName());
                writeString(out, enumValue.name());
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
                out.writeInt(list.size());
                for (Object item : list) {
                    writeValue(out, item, seen);
                }
                return;
            }

            if (value instanceof Map) {
                out.writeByte(TAG_MAP);
                Map<?, ?> map = (Map<?, ?>) value;
                out.writeInt(map.size());
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    Object key = entry.getKey();
                    if (!(key instanceof String)) {
                        throw new IOException("Only String map keys are supported in reflection benchmark codec");
                    }
                    writeString(out, (String) key);
                    writeValue(out, entry.getValue(), seen);
                }
                return;
            }

            if (!(value instanceof Base)) {
                out.writeByte(TAG_STRING);
                writeString(out, String.valueOf(value));
                return;
            }

            if (seen.put(value, Boolean.TRUE) != null) {
                out.writeByte(TAG_NULL);
                return;
            }

            out.writeByte(TAG_NODE);
            Class<?> cls = value.getClass();
            writeString(out, cls.getName());
            List<java.lang.reflect.Field> fields = collectInstanceFields(cls);
            out.writeInt(fields.size());
            for (java.lang.reflect.Field field : fields) {
                writeString(out, field.getName());
                try {
                    AccessibleObject.setAccessible(new AccessibleObject[]{field}, true);
                    writeValue(out, field.get(value), seen);
                } catch (IllegalAccessException e) {
                    throw new IOException("Failed to read field " + field.getName() + " from " + cls.getName(), e);
                }
            }
        }

        private List<java.lang.reflect.Field> collectInstanceFields(Class<?> cls) {
            List<java.lang.reflect.Field> fields = new ArrayList<java.lang.reflect.Field>();
            Class<?> cur = cls;
            while (cur != null && cur != Object.class) {
                for (java.lang.reflect.Field f : cur.getDeclaredFields()) {
                    if (!Modifier.isStatic(f.getModifiers())) {
                        fields.add(f);
                    }
                }
                cur = cur.getSuperclass();
            }
            fields.sort(Comparator.comparing(java.lang.reflect.Field::getName));
            return fields;
        }

        private void writeString(DataOutputStream out, String value) throws IOException {
            if (value == null) {
                out.writeInt(-1);
                return;
            }
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            out.writeInt(bytes.length);
            out.write(bytes);
        }
    }

}
