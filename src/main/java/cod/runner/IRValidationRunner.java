package cod.runner;

import cod.ast.nodes.*;
import cod.ir.IRManager;
import cod.ir.IRReader;
import cod.ir.IRWriter;
import cod.interpreter.Interpreter;

import java.io.File;
import java.util.ArrayList;

public class IRValidationRunner extends BaseRunner {
    @Override
    public void run(String[] args) throws Exception {
        String file = "/home/runner/work/Coderive/Coderive/src/main/cod/src/main/test/Import.cod";
        if (args != null && args.length > 0 && args[0] != null && !args[0].isEmpty()) {
            file = args[0];
        }

        Interpreter interpreter = new Interpreter();
        interpreter.setFilePath(file);
        Program program = parse(file, interpreter);

        if (program == null || program.unit == null || program.unit.types == null || program.unit.types.isEmpty()) {
            throw new RuntimeException("No parsed type available for IR validation");
        }

        Type original = program.unit.types.get(0);
        File tmp = new File("/tmp/coderive-ir-validation.codb");

        IRWriter writer = new IRWriter();
        IRReader reader = new IRReader();
        writer.write(tmp, original);
        Type loaded = reader.read(tmp);

        assertTrue(loaded != null, "Loaded type is null");
        assertTrue(equalsSafe(original.name, loaded.name), "Type name mismatch");
        assertTrue(loaded.methods != null, "Loaded methods null");
        assertTrue(original.methods != null, "Original methods null");
        assertTrue(original.methods.size() == loaded.methods.size(), "Method count mismatch");

        if (!original.methods.isEmpty()) {
            Method om = original.methods.get(0);
            Method lm = loaded.methods.get(0);
            assertTrue(equalsSafe(om.methodName, lm.methodName), "First method name mismatch");
            int ob = om.body == null ? 0 : om.body.size();
            int lb = lm.body == null ? 0 : lm.body.size();
            assertTrue(ob == lb, "First method body size mismatch");
        }

        String projectRoot = cod.util.Index.getProjectRoot();
        if (projectRoot != null) {
            IRManager manager = new IRManager(projectRoot);
            manager.save(program.unit.name, original);
            Type managerLoaded = manager.load(program.unit.name, original.name);
            assertTrue(managerLoaded != null, "IRManager failed to load saved class");
            assertTrue(equalsSafe(original.name, managerLoaded.name), "IRManager loaded wrong class");
        }

        System.out.println("IR validation passed");
    }

    private static boolean equalsSafe(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new RuntimeException(message);
        }
    }

    public static void main(String[] args) {
        try {
            new IRValidationRunner().run(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
