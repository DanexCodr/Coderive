package cod.runner;

import cod.ast.node.Method;
import cod.ast.node.Program;
import cod.ast.node.Type;
import cod.interpreter.Index;
import cod.interpreter.Interpreter;
import cod.ir.IRManager;
import cod.ptac.CodPTACArtifact;
import cod.ptac.CodPTACExecutor;
import cod.ptac.CodPTACOptions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public final class CodPTACParityRunner extends BaseRunner {
    private static final String[] DEFAULT_CASES = new String[] {
        "/home/runner/work/Coderive/Coderive/src/main/cod/src/main/test/Basic.cod",
        "/home/runner/work/Coderive/Coderive/src/main/cod/src/main/test/Loop.cod",
        "/home/runner/work/Coderive/Coderive/src/main/cod/src/main/test/LazyLoop.cod",
        "/home/runner/work/Coderive/Coderive/src/main/cod/src/main/test/Lambda.cod",
        "/home/runner/work/Coderive/Coderive/src/main/cod/src/main/test/TailCallOptimization.cod",
        "/home/runner/work/Coderive/Coderive/src/main/cod/src/main/test/Import.cod",
        "/home/runner/work/Coderive/Coderive/src/main/cod/src/main/test/LinearRecurrenceOptimization.cod"
    };

    @Override
    public void run(String[] args) throws Exception {
        List<String> files = new ArrayList<String>();
        if (args != null && args.length > 0) {
            for (String arg : args) {
                if (arg != null && !arg.trim().isEmpty()) {
                    files.add(arg);
                }
            }
        } else {
            for (String file : DEFAULT_CASES) files.add(file);
        }

        int passed = 0;
        for (String file : files) {
            String astOut = runAstPath(file);
            String ptacOut = runCodPTACPath(file);
            if (!normalize(astOut).equals(normalize(ptacOut))) {
                throw new RuntimeException(
                    "CodP-TAC parity mismatch for " + file + "\nAST:\n" + astOut + "\nPTAC:\n" + ptacOut);
            }
            passed++;
        }

        System.out.println("CodP-TAC parity passed: " + passed + " case(s)");
    }

    private String runAstPath(String file) throws Exception {
        final Interpreter interpreter = new Interpreter();
        interpreter.setFilePath(file);
        final Program ast = parse(file, interpreter);
        return captureOutput(new Runnable() {
            @Override
            public void run() {
                interpreter.run(ast);
            }
        });
    }

    private String runCodPTACPath(String file) throws Exception {
        final Interpreter interpreter = new Interpreter();
        interpreter.setFilePath(file);
        final Program ast = parse(file, interpreter);
        if (ast == null || ast.unit == null || ast.unit.types == null || ast.unit.types.isEmpty()) {
            return captureOutput(new Runnable() {
                @Override
                public void run() {
                    interpreter.run(ast);
                }
            });
        }

        String projectRoot = Index.getProjectRoot();
        if (projectRoot == null) {
            File f = new File("/home/runner/work/Coderive/Coderive");
            projectRoot = f.getAbsolutePath();
        }
        final IRManager manager = new IRManager(projectRoot);
        final String unitName = ast.unit.name;
        final Type entryType = findMainType(ast);
        manager.save(unitName, entryType);
        final CodPTACArtifact artifact = manager.loadArtifact(unitName, entryType.name);
        if (artifact == null) {
            throw new RuntimeException("Failed to load CodP-TAC artifact for parity: " + file);
        }

        return captureOutput(new Runnable() {
            @Override
            public void run() {
                new CodPTACExecutor(CodPTACOptions.compileExecuteWithFallback(true))
                    .execute(artifact, interpreter);
            }
        });
    }

    private Type findMainType(Program ast) {
        for (Type type : ast.unit.types) {
            if (type == null || type.methods == null) continue;
            for (Method method : type.methods) {
                if (method != null && "main".equals(method.methodName)
                    && (method.parameters == null || method.parameters.isEmpty())) {
                    return type;
                }
            }
        }
        return ast.unit.types.get(0);
    }

    private String captureOutput(Runnable runnable) {
        PrintStream oldOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream replacement = new PrintStream(output);
        try {
            System.setOut(replacement);
            runnable.run();
        } finally {
            replacement.flush();
            replacement.close();
            System.setOut(oldOut);
        }
        return output.toString();
    }

    private String normalize(String text) {
        if (text == null) return "";
        return text.replace("\r", "").trim();
    }

    public static void main(String[] args) {
        try {
            new CodPTACParityRunner().run(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
