package cod.runner;

import cod.ast.nodes.*;
import cod.ir.IRReader;
import cod.ir.IRWriter;
import cod.math.AutoStackingNumber;
import cod.syntax.Keyword;

import java.io.File;
import java.io.IOException;
import cod.ast.nodes.Field;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class IRCodecBenchmarkRunner {
    private static final int WARMUP_ITERATIONS = 50;
    private static final int MEASURE_ITERATIONS = 200;

    public static void main(String[] args) {
        try {
            Type sample = createSampleType();

            File irFile = new File("/tmp/coderive-ir-codec-benchmark.codb");
            IRWriter writer = new IRWriter();
            IRReader reader = new IRReader();

            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                writer.write(irFile, sample);
                reader.read(irFile);
                ReflectionNodeWalker.walk(sample);
            }

            long visitorNanos = benchmarkVisitor(writer, reader, irFile, sample);
            long reflectionNanos = benchmarkReflection(sample);

            double visitorMs = visitorNanos / 1_000_000.0;
            double reflectionMs = reflectionNanos / 1_000_000.0;
            double ratio = visitorNanos == 0 ? 0.0 : ((double) reflectionNanos / (double) visitorNanos);

            System.out.println("IR codec benchmark (" + MEASURE_ITERATIONS + " iterations)");
            System.out.println("visitor serialize+deserialize: " + String.format("%.3f", visitorMs) + " ms");
            System.out.println("reflection node walk        : " + String.format("%.3f", reflectionMs) + " ms");
            System.out.println("reflection/visitor ratio    : " + String.format("%.3f", ratio) + "x");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static long benchmarkVisitor(IRWriter writer, IRReader reader, File file, Type sample) throws IOException {
        long start = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            writer.write(file, sample);
            reader.read(file);
        }
        return System.nanoTime() - start;
    }

    private static long benchmarkReflection(Type sample) {
        long start = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            ReflectionNodeWalker.walk(sample);
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

    private static final class ReflectionNodeWalker {
        private ReflectionNodeWalker() {}

        static int walk(Object root) {
            return walk(root, Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>()));
        }

        private static int walk(Object value, java.util.Set<Object> seen) {
            if (value == null) {
                return 1;
            }

            if (isLeaf(value)) {
                return 1;
            }

            if (!seen.add(value)) {
                return 0;
            }

            int count = 1;
            Class<?> cls = value.getClass();

            if (value instanceof List) {
                for (Object item : (List<?>) value) {
                    count += walk(item, seen);
                }
                return count;
            }

            if (value instanceof Map) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                    count += walk(entry.getKey(), seen);
                    count += walk(entry.getValue(), seen);
                }
                return count;
            }

            while (cls != null && cls != Object.class) {
                for (java.lang.reflect.Field f : cls.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers())) {
                        continue;
                    }
                    try {
                        AccessibleObject.setAccessible(new AccessibleObject[]{f}, true);
                        count += walk(f.get(value), seen);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
                cls = cls.getSuperclass();
            }

            return count;
        }

        private static boolean isLeaf(Object value) {
            return value instanceof String
                    || value instanceof Number
                    || value instanceof Boolean
                    || value instanceof Character
                    || value instanceof Enum<?>;
        }
    }
}
