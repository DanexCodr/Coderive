package cod.runner;

import cod.runner.BaseRunner;
import cod.ast.node.*;
import cod.debug.DebugSystem;
import cod.interpreter.Interpreter;
import cod.debug.Linter;
import cod.interpreter.Index;
import cod.ir.IRManager;
import cod.ptac.CodPTACArtifact;
import cod.ptac.CodPTACExecutor;
import cod.ptac.CodPTACOptions;

import java.io.File;
import java.util.List;
import java.util.Scanner;

/*
This class is used by the language developer for testing. Rigorously tested in an Android phone.
For production use CommandRunner.
*/

public class TestRunner extends BaseRunner {

    private final String TEST_FILE = "HelloWorldModule";
    
    private final String androidPath = "/storage/emulated/0";
    private final String definedFilePath =
        "/JavaNIDE/Programming-Language/Coderive/app/src/main/cod/demo/src/main/test/" + TEST_FILE + ".cod";
    private final String consoleRelativePath =
        "src/main/cod/demo/src/main/test/" + TEST_FILE + ".cod";
    private final String NAME = "TEST";
    private final DebugSystem.Level level = DebugSystem.Level.OFF;

    private final Interpreter interpreter;
    private IRManager irManager;
    private final CodPTACOptions ptacOptions;

    public TestRunner() {
        this.interpreter = new Interpreter();
        this.ptacOptions = CodPTACOptions.current();
    }

    @Override
    public void run(String[] args) throws Exception {
        String inputFilename = getInputFilename(args);

        // Validate file path is under src/main/
        validateSourceFilePath(inputFilename);

        RunnerConfig config =
            processArgs(
                args,
                inputFilename,
                new Configuration() {
                    @Override
                    public void configure(RunnerConfig config) {
                        // Enable DEBUG to see index generation logs
                        config.withDebugLevel(level);
                    }
                });

        configureDebugSystem(config.debugLevel);

        DebugSystem.info(
            NAME + LOG_TAG, 
            "Starting interpreter execution...\nInput file: " + config.inputFilename);
            
        DebugSystem.startTimer("exec");

        // Set file path on interpreter BEFORE parsing
        interpreter.setFilePath(config.inputFilename);

        // Parse with interpreter for unit validation
        Program ast = parse(config.inputFilename, interpreter);

        // Initialize IR manager after parsing (project root is now known)
        initializeIRManager();

        // Perform linting and check completion status
        boolean lintingCompleted = performLinting(ast);

        // Ensure all output streams are fully flushed before proceeding
        flushStreams();

        // Only run interpreter if linting completed successfully
        if (lintingCompleted) {
            executeWithManualInterpreter(ast);
        } else {
            DebugSystem.error(
                NAME + LOG_TAG, 
                "Linting did not complete successfully. Interpreter execution aborted.");
            throw new RuntimeException("Linting phase failed to complete");
        }
        System.out.println("\n" + "-----------------------------");
        System.out.println("Execution completed! Duration: " + DebugSystem.stopTimer("exec") + "ms");
    }

    /**
     * Initialize IR manager after project root is known
     */
    private void initializeIRManager() {
        String srcMainRoot = interpreter.getImportResolver().getSrcMainRoot();
        if (srcMainRoot != null) {
            String projectRoot = Index.getProjectRoot();
            if (projectRoot != null) {
                this.irManager = new IRManager(projectRoot);
                DebugSystem.debug(NAME + LOG_TAG, "IR manager initialized with root: " + projectRoot);
            }
        }
    }

    // Validate source file is under src/main/
    private void validateSourceFilePath(String filePath) {
        String normalized = filePath.replace('\\', '/');
        if (!normalized.contains("/src/main/")) {
            DebugSystem.warn(
                NAME + LOG_TAG,
                "Source file not under src/main/: "
                    + filePath
                    + "\nUnit declaration validation may fail."
                    + "\nExpected structure: src/main/<unit>/<file>.cod");
        }
    }

    private String getInputFilename(String[] args) {
        // First, check for input file in args
        String inputFileFromArgs = null;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("-") && !arg.equals("-o")) {
                inputFileFromArgs = arg;
                break;
            }
        }

        if (inputFileFromArgs != null) {
            return inputFileFromArgs;
        }

        String defaultFilename = androidPath + definedFilePath;
        String consoleFilename =
            new File(System.getProperty("user.dir"), consoleRelativePath).getAbsolutePath();
        File consoleFile = new File(consoleFilename);

        // Prefer local console default if it exists
        if (consoleFile.exists() && consoleFile.isFile()) {
            out("Using console default file: " + consoleFilename);
            return consoleFilename;
        }

        // Otherwise, ask user interactively
        Scanner scanner = new Scanner(System.in);

        out("Enter file path or press Enter for default [" + defaultFilename + "]");
        out("Or type 'console' to use local default [" + consoleFilename + "]");
        System.out.print("> ");

        String userInput = scanner.nextLine().trim();

        if (userInput.isEmpty()) {
            System.out.println("Using default file: " + defaultFilename);
            return defaultFilename;
        } else if ("console".equalsIgnoreCase(userInput)) {
            System.out.println("Using console file: " + consoleFilename);
            return consoleFilename;
        } else {
            out("Using user provided file: " + userInput);
            return userInput;
        }
    }

    private void executeWithManualInterpreter(Program ast) {
        DebugSystem.info(NAME + LOG_TAG, "Running Interpreter");
        
        // Generate indexes before execution for O(1) import resolution
        DebugSystem.info(NAME + LOG_TAG, "Generating indexes...");
        generateIndexes(ast, interpreter);
        
        // Compile to IR after index generation
        if (irManager != null && ast != null && ast.unit != null) {
            DebugSystem.info(NAME + LOG_TAG, "Generating IR...");
            compileToBytecode(ast);
        }
        
        // ========== DEBUG: Check if index was generated ==========
        DebugSystem.info(NAME + LOG_TAG, "Checking for generated indexes...");
        
        // Get the unit name from the AST
        String unitName = null;
        if (ast != null && ast.unit != null) {
            unitName = ast.unit.name;
            DebugSystem.info(NAME + LOG_TAG, "Main unit name: " + unitName);
        }
        
        // Try to load index for the main unit
        if (unitName != null && !unitName.equals("default")) {
            Index idx = Index.load(unitName);
            if (idx != null) {
                DebugSystem.info(NAME + LOG_TAG, "✓ Index found for unit '" + unitName + 
                                 "' with " + idx.size() + " classes");
                if (DebugSystem.getLevel().compareTo(DebugSystem.Level.TRACE) >= 0) {
                    DebugSystem.trace(NAME + LOG_TAG, "  Classes: " + idx.getClassNames());
                }
            } else {
                DebugSystem.warn(NAME + LOG_TAG, "✗ No index found for unit '" + unitName + "'");
            }
        }
        
        // Also check for imported units
        if (ast != null && ast.unit != null && ast.unit.imports != null) {
            for (String importName : ast.unit.imports.imports) {
                String[] parts = importName.split("\\.");
                if (parts.length > 0) {
                    String importedUnit = parts[0];
                    Index importedIdx = Index.load(importedUnit);
                    if (importedIdx != null) {
                        DebugSystem.info(NAME + LOG_TAG, "✓ Index found for imported unit '" + 
                                         importedUnit + "' with " + importedIdx.size() + " classes");
                    } else {
                        DebugSystem.warn(NAME + LOG_TAG, "✗ No index found for imported unit '" + 
                                         importedUnit + "'");
                    }
                }
            }
        }
        
        // Check src/idx directory using project root
        String projectRoot = Index.getProjectRoot();
        if (projectRoot != null) {
            File idxDir = new File(projectRoot + File.separator + "src" + File.separator + "idx");
            if (idxDir.exists() && idxDir.isDirectory()) {
                DebugSystem.info(NAME + LOG_TAG, "Index directory exists at: " + idxDir.getAbsolutePath());
                String[] files = idxDir.list();
                if (files != null && files.length > 0) {
                    DebugSystem.info(NAME + LOG_TAG, "  Contents: " + java.util.Arrays.toString(files));
                } else {
                    DebugSystem.warn(NAME + LOG_TAG, "  Index directory is empty");
                }
            } else {
                DebugSystem.info(NAME + LOG_TAG, "Index directory will be created at: " + idxDir.getAbsolutePath());
            }
            
            // Check bytecode directory
            File binDir = new File(projectRoot + File.separator + "src" + File.separator + "bin");
            if (binDir.exists() && binDir.isDirectory()) {
                DebugSystem.info(NAME + LOG_TAG, "Bytecode directory exists at: " + binDir.getAbsolutePath());
                String[] files = binDir.list();
                if (files != null && files.length > 0) {
                    DebugSystem.info(NAME + LOG_TAG, "  Contents: " + java.util.Arrays.toString(files));
                }
            } else {
                DebugSystem.info(NAME + LOG_TAG, "Bytecode directory will be created at: " + binDir.getAbsolutePath());
            }
        } else {
            DebugSystem.warn(NAME + LOG_TAG, "Project root not set, cannot verify directories");
            DebugSystem.warn(NAME + LOG_TAG, "  Current working directory: " + new File(".").getAbsolutePath());
        }
        
        DebugSystem.startTimer("interpretation");
        if (ptacOptions.isCompileExecuteEnabled() && irManager != null && ast != null && ast.unit != null) {
            Type entryType = findMainType(ast);
            if (entryType != null) {
                CodPTACArtifact artifact = irManager.loadArtifact(ast.unit.name, entryType.name);
                if (artifact == null) {
                    irManager.save(ast.unit.name, entryType);
                    artifact = irManager.loadArtifact(ast.unit.name, entryType.name);
                }
                if (artifact != null) {
                    DebugSystem.info(NAME + LOG_TAG, "Executing using CodP-TAC executor");
                    new CodPTACExecutor(ptacOptions).execute(artifact, interpreter);
                    double duration = DebugSystem.stopTimer("interpretation");
                    DebugSystem.info(NAME + LOG_TAG, String.format("Interpretation completed in %.3f ms", duration));
                    return;
                }
            }
        }

        interpreter.run(ast);
        double duration = DebugSystem.stopTimer("interpretation");
        
        DebugSystem.info(NAME + LOG_TAG, String.format("Interpretation completed in %.3f ms", duration));
    }
    
    /**
     * Compile all classes in the program to .codb IR files
     */
    private void compileToBytecode(Program ast) {
        if (ast == null || ast.unit == null || irManager == null) {
            return;
        }
        
        String unitName = ast.unit.name;
        if (unitName == null || unitName.equals("default")) {
            return;
        }
        
        int compiled = 0;
        for (Type type : ast.unit.types) {
            try {
                irManager.save(unitName, type);
                compiled++;
                DebugSystem.debug(NAME + LOG_TAG, "Compiled CodP-TAC artifact: " + type.name + " → " + type.name + ".codb");
            } catch (Exception e) {
                DebugSystem.warn(NAME + LOG_TAG, "Failed to compile " + type.name + ": " + e.getMessage());
            }
        }
        
        if (compiled > 0) {
            DebugSystem.info(NAME + LOG_TAG, "Compiled " + compiled + " class(es) to IR");
        }
    }

    private boolean performLinting(Program ast) {
        DebugSystem.startTimer("linting");
        Linter linter = new Linter();
        List<String> warnings = linter.lint(ast);
        DebugSystem.stopTimer("linting");

        // Print warnings immediately and synchronously
        Linter.WarningUtils.printWarnings(warnings);
        boolean lintingCompleted = linter.isCompleted();
        int warningCount = linter.getWarningCount();

        DebugSystem.info(
            NAME + LOG_TAG,
            "Linting completed: " + lintingCompleted + " with " + warningCount + " warning(s)");

        // Force flush all streams after linting output
        flushStreams();

        return lintingCompleted;
    }

    /** Forces all output streams to flush completely to ensure proper output ordering */
    private void flushStreams() {
        try {
            System.out.flush();
            System.err.flush();
        } catch (Exception e) {
            // Ignore interruption, just continue
        }
    }

    public static void main(String[] args) {
        try {
            new TestRunner().run(args);
        } catch (Exception e) {
            e.printStackTrace();
            outE();
            outE("Usage: TestRunner [filename] [options]");
            outE("Options:");
            outE("  -o <file>      Output file");
            outE("Environment flags:");
            outE("  COD_PTAC_MODE=interpreter|compile-only|compile-execute");
            outE("  COD_PTAC_FALLBACK=true|false");
            outE();
            outE("Example:");
            outE("  TestRunner myprogram.cod");
            outE();
        }
    }

    private Type findMainType(Program ast) {
        if (ast == null || ast.unit == null || ast.unit.types == null) return null;
        for (Type type : ast.unit.types) {
            if (type == null || type.methods == null) continue;
            for (Method method : type.methods) {
                if (method != null && "main".equals(method.methodName)
                    && (method.parameters == null || method.parameters.isEmpty())) {
                    return type;
                }
            }
        }
        return !ast.unit.types.isEmpty() ? ast.unit.types.get(0) : null;
    }
}
