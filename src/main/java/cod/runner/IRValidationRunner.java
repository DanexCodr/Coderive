package cod.runner;

import cod.ast.node.*;
import cod.ir.IRManager;
import cod.ir.IRReader;
import cod.ir.IRWriter;
import cod.interpreter.Interpreter;
import cod.ptac.CodPTACArtifact;
import cod.semantic.ImportResolver;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

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
        CodPTACArtifact artifact = reader.readArtifact(tmp);
        Type loaded = artifact != null ? artifact.typeSnapshot : null;

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

        String projectRoot = cod.interpreter.Index.getProjectRoot();
        if (projectRoot != null) {
            IRManager manager = new IRManager(projectRoot);
            manager.save(program.unit.name, original);
            Type managerLoaded = manager.load(program.unit.name, original.name);
            assertTrue(managerLoaded != null, "IRManager failed to load saved class");
            assertTrue(equalsSafe(original.name, managerLoaded.name), "IRManager loaded wrong class");
            CodPTACArtifact managerArtifact = manager.loadArtifact(program.unit.name, original.name);
            assertTrue(managerArtifact != null, "IRManager failed to load saved CodP-TAC artifact");
            assertTrue(managerArtifact.unit != null, "CodP-TAC unit missing from artifact");
        }

        validateInternalImportIRPath();

        System.out.println("IR validation passed");
    }

    private void validateInternalImportIRPath() throws Exception {
        String internalFile = "/home/runner/work/Coderive/Coderive/src/main/cod/src/main/internal/IntMath.cod";
        String importProbeFile = "/home/runner/work/Coderive/Coderive/src/main/cod/src/main/test/InternalIRImport.cod";

        Interpreter internalInterpreter = new Interpreter();
        internalInterpreter.setFilePath(internalFile);
        Program internalProgram = parse(internalFile, internalInterpreter);
        assertTrue(internalProgram != null, "Internal source parse failed");
        assertTrue(internalProgram.unit != null, "Internal unit is null");
        assertTrue(internalProgram.unit.types != null && !internalProgram.unit.types.isEmpty(),
            "Internal type list is empty");

        Type internalType = internalProgram.unit.types.get(0);
        String projectRoot = cod.interpreter.Index.getProjectRoot();
        assertTrue(projectRoot != null, "Project root not resolved for internal IR validation");

        IRManager manager = new IRManager(projectRoot);
        manager.save(internalProgram.unit.name, internalType);
        CodPTACArtifact artifact = manager.loadArtifact(internalProgram.unit.name, internalType.name);
        assertTrue(artifact != null, "Failed to save/load internal CodP-TAC artifact");

        ImportResolver resolver = new ImportResolver();
        resolver.setCurrentFileDirectory(importProbeFile);
        Type loaded = resolver.resolveImport("internal.IntMath");
        assertTrue(loaded != null, "ImportResolver failed to load internal.IntMath");

        Map<String, Object> cacheStats = resolver.getCacheStats();
        Object bytecodeHits = cacheStats.get("bytecodeCacheHits");
        assertTrue(bytecodeHits instanceof Integer, "Missing bytecode cache hit stat");
        assertTrue(((Integer) bytecodeHits).intValue() > 0,
            "Expected IR bytecode cache hit when loading internal.IntMath");
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
