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
import java.util.Scanner;

public final class CodPTACParityRunner extends BaseRunner {
    
    private final String androidPath = "/storage/emulated/0";
    private final String baseTestPath = "/JavaNIDE/Programming-Language/Coderive/app/src/main/cod/src/main/test/";
    
    private final String[] DEFAULT_CASES = new String[] {
        androidPath + baseTestPath + "Basic.cod",
        androidPath + baseTestPath + "Loop.cod",
        androidPath + baseTestPath + "LazyLoop.cod",
        androidPath + baseTestPath + "Lambda.cod",
        androidPath + baseTestPath + "TailCallOptimization.cod",
        androidPath + baseTestPath + "Import.cod",
        androidPath + baseTestPath + "LinearRecurrenceOptimization.cod"
    };

    @Override
    public void run(String[] args) throws Exception {
        List<String> files = new ArrayList<String>();
        
        if (args != null && args.length > 0) {
            for (String arg : args) {
                if (arg != null && !arg.trim().isEmpty() && !arg.startsWith("-")) {
                    files.add(arg);
                }
            }
        }
        
        if (files.isEmpty()) {
            // Check if Android default path exists
            File lazyLoop = new File(androidPath + baseTestPath + "LazyLoop.cod");
            if (lazyLoop.exists()) {
                System.out.println("Found Android test directory at: " + androidPath + baseTestPath);
                System.out.print("Use Android default tests? (y/n): ");
                System.out.flush();
                
                Scanner scanner = new Scanner(System.in);
                String response = scanner.nextLine().trim().toLowerCase();
                
                if (response.equals("y") || response.equals("yes")) {
                    for (String file : DEFAULT_CASES) {
                        File f = new File(file);
                        if (f.exists()) {
                            files.add(file);
                        }
                    }
                } else {
                    System.out.print("Enter test file path: ");
                    String userFile = scanner.nextLine().trim();
                    if (!userFile.isEmpty()) {
                        files.add(userFile);
                    }
                }
            } else {
                System.out.println("No Android test directory found at: " + androidPath + baseTestPath);
                System.out.print("Enter test file path: ");
                System.out.flush();
                
                Scanner scanner = new Scanner(System.in);
                String userFile = scanner.nextLine().trim();
                if (!userFile.isEmpty()) {
                    files.add(userFile);
                }
            }
        }
        
        if (files.isEmpty()) {
            System.out.println("No test files specified.");
            return;
        }

        int passed = 0;
        int failed = 0;
        List<String> failures = new ArrayList<String>();

        System.out.println();
        System.out.println("CodP-TAC Parity Validation");
        System.out.println("=========================");
        System.out.println("Running " + files.size() + " test(s)...");
        System.out.println();

        for (int i = 0; i < files.size(); i++) {
            String file = files.get(i);
            String shortName = new File(file).getName();
            
            System.out.print("[" + (i + 1) + "/" + files.size() + "] Testing " + shortName + "... ");
            System.out.flush();
            
            try {
                String astOut = runAstPath(file);
                String ptacOut = runCodPTACPath(file);
                
                if (normalize(astOut).equals(normalize(ptacOut))) {
                    System.out.println("PASSED");
                    passed++;
                } else {
                    System.out.println("FAILED");
                    failed++;
                    failures.add(shortName);
                }
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
                failed++;
                failures.add(shortName + " - " + e.getMessage());
            }
            
            System.out.flush();
        }

        System.out.println();
        System.out.println("=========================");
        System.out.println("Results: " + passed + " passed, " + failed + " failed, " + files.size() + " total");
        
        if (!failures.isEmpty()) {
            System.out.println();
            System.out.println("Failed tests:");
            for (String failure : failures) {
                System.out.println("  - " + failure);
            }
        }
        
        System.out.println();
        System.out.println("Parity check complete.");
    }

    private String runAstPath(String file) throws Exception {
        Interpreter interpreter = new Interpreter();
        interpreter.setFilePath(file);
        Program ast = parse(file, interpreter);
        return captureOutput(interpreter, ast);
    }

    private String runCodPTACPath(String file) throws Exception {
        Interpreter interpreter = new Interpreter();
        interpreter.setFilePath(file);
        Program ast = parse(file, interpreter);
        
        if (ast == null || ast.unit == null || ast.unit.types == null || ast.unit.types.isEmpty()) {
            return captureOutput(interpreter, ast);
        }

        String projectRoot = Index.getProjectRoot();
        if (projectRoot == null) {
            projectRoot = new File(".").getAbsoluteFile().getAbsolutePath();
        }
        
        IRManager manager = new IRManager(projectRoot);
        String unitName = ast.unit.name;
        Type entryType = findMainType(ast);
        
        if (entryType == null) {
            return captureOutput(interpreter, ast);
        }
        
        manager.save(unitName, entryType);
        CodPTACArtifact artifact = manager.loadArtifact(unitName, entryType.name);
        
        if (artifact == null) {
            throw new Exception("Failed to load CodP-TAC artifact for: " + file);
        }

        return captureOutputPTAC(artifact, interpreter);
    }

    private Type findMainType(Program ast) {
        if (ast == null || ast.unit == null || ast.unit.types == null || ast.unit.types.isEmpty()) {
            return null;
        }
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

    private String captureOutput(Interpreter interpreter, Program ast) {
        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        PrintStream outReplacement = new PrintStream(outBuffer);
        PrintStream errReplacement = new PrintStream(errBuffer);
        
        try {
            System.setOut(outReplacement);
            System.setErr(errReplacement);
            interpreter.run(ast);
            outReplacement.flush();
            errReplacement.flush();
            
            String output = outBuffer.toString();
            String error = errBuffer.toString();
            
            if (error != null && !error.trim().isEmpty()) {
                output = output + "\n[STDERR]\n" + error;
            }
            return output;
        } finally {
            outReplacement.close();
            errReplacement.close();
            System.setOut(oldOut);
            System.setErr(oldErr);
        }
    }

    private String captureOutputPTAC(CodPTACArtifact artifact, Interpreter interpreter) {
        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        PrintStream outReplacement = new PrintStream(outBuffer);
        PrintStream errReplacement = new PrintStream(errBuffer);
        
        try {
            System.setOut(outReplacement);
            System.setErr(errReplacement);
            new CodPTACExecutor(CodPTACOptions.compileExecuteWithFallback(true))
                .execute(artifact, interpreter);
            outReplacement.flush();
            errReplacement.flush();
            
            String output = outBuffer.toString();
            String error = errBuffer.toString();
            
            if (error != null && !error.trim().isEmpty()) {
                output = output + "\n[STDERR]\n" + error;
            }
            return output;
        } finally {
            outReplacement.close();
            errReplacement.close();
            System.setOut(oldOut);
            System.setErr(oldErr);
        }
    }

    private String normalize(String text) {
        if (text == null) return "";
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.split("\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line.trim()).append("\n");
        }
        return sb.toString().trim();
    }

    public static void main(String[] args) {
        CodPTACParityRunner runner = new CodPTACParityRunner();
        try {
            runner.run(args);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
