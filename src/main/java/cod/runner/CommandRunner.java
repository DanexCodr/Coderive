// CommandRunner.java
package cod.runner;

import cod.ast.node.*;
import cod.debug.DebugSystem;
import cod.interpreter.Interpreter;
import cod.interpreter.Index;
import cod.ir.IRManager;
import cod.ptac.CodPTACArtifact;
import cod.ptac.CodPTACExecutor;
import cod.ptac.CodPTACOptions;

public class CommandRunner extends BaseRunner {

    private final Interpreter interpreter;
    private IRManager irManager;
    private final CodPTACOptions ptacOptions;

    private static final String NAME = "COMMAND";

    public CommandRunner() {
        this.interpreter = new Interpreter();
        this.ptacOptions = CodPTACOptions.current();
    }

    @Override
    public void run(String[] args) throws Exception {
        // Check for compile command first
        if (args.length > 0 && "compile".equals(args[0])) {
            handleCompileCommand(args);
            return;
        }
        
        String outputFilename = null;

        RunnerConfig config =
            processArgs(
                args,
                null,
                new Configuration() {
                    @Override
                    public void configure(RunnerConfig config) {
                        config.withDebugLevel(DebugSystem.Level.INFO);
                    }
                });

        // Parse command-line arguments
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--interpret".equals(arg) || "-i".equals(arg)) {
                // Default mode, do nothing
            } else if ("-o".equals(arg)) {
                if (i + 1 < args.length) {
                    outputFilename = args[i + 1];
                    i++;
                } else {
                    outE("Error: -o option requires an output filename.");
                }
            } else if ("--debug".equals(arg)) {
                config.debugLevel = DebugSystem.Level.DEBUG;
            } else if ("--trace".equals(arg)) {
                config.debugLevel = DebugSystem.Level.TRACE;
            } else if ("--quiet".equals(arg)) {
                config.debugLevel = DebugSystem.Level.ERROR;
            } else if ("--help".equals(arg) || "-h".equals(arg)) {
                printHelp();
                return;
            }
        }

        if (outputFilename != null) {
            config.withOutputFilename(outputFilename);
        }

        configureDebugSystem(config.debugLevel);

        DebugSystem.info(NAME + LOG_TAG, "Starting CommandRunner execution");
        DebugSystem.info(NAME + LOG_TAG, "Input file: " + config.inputFilename);

        if (config.inputFilename == null || config.inputFilename.isEmpty()) {
            throw new RuntimeException(
                "No input file specified. Usage: CommandRunner <filename> [options]");
        }

        DebugSystem.startTimer("exec");
        try {
            // Set file path on interpreter BEFORE parsing
            interpreter.setFilePath(config.inputFilename);

            DebugSystem.startTimer("parsing");
            Program ast = parse(config.inputFilename, interpreter);
            if (ast == null) {
                throw new RuntimeException("Parsing failed, AST is null.");
            }
            DebugSystem.stopTimer("parsing");
            DebugSystem.info(NAME + LOG_TAG, "AST built successfully");
            
            // Initialize IR manager
            initializeIRManager();

            executeInterpretation(ast);

            DebugSystem.info(NAME + LOG_TAG, "CommandRunner execution completed");
        } finally {
            System.out.println("\n-----------------------------");
            System.out.println("Execution completed! Duration: " + DebugSystem.stopTimer("exec") + "ms");
        }
    }
    
    /**
     * Handle the "compile" command
     */
    private void handleCompileCommand(String[] args) throws Exception {
        if (args.length < 2) {
            outE("Error: No source file specified for compilation");
            outE("Usage: CommandRunner compile <filename>");
            return;
        }
        
        String sourceFile = args[1];
        DebugSystem.info(NAME + LOG_TAG, "Compiling: " + sourceFile);
        
        Interpreter tempInterpreter = new Interpreter();
        tempInterpreter.setFilePath(sourceFile);
        Program ast = parse(sourceFile, tempInterpreter);
        
        if (ast == null) {
            outE("Error: Failed to parse source file");
            return;
        }
        
        // Initialize IR manager for compilation
        String srcMainRoot = tempInterpreter.getImportResolver().getSrcMainRoot();
        if (srcMainRoot != null) {
            String projectRoot = Index.getProjectRoot();
            if (projectRoot != null) {
                IRManager bm = new IRManager(projectRoot);
                
                int compiled = 0;
                for (Type type : ast.unit.types) {
                    bm.save(ast.unit.name, type);
                    System.out.println("Compiled (CodP-TAC artifact): " + type.name + " → " + type.name + ".codb");
                    compiled++;
                }
                
                System.out.println("Compilation complete: " + compiled + " class(es) compiled");
            } else {
                outE("Error: Could not determine project root");
            }
        } else {
            outE("Error: Could not find src/main/ structure");
        }
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

    private void executeInterpretation(Program ast) {
        DebugSystem.info(NAME + LOG_TAG, "Starting program interpretation");

        boolean hasImports =
            ast != null
                && ast.unit != null
                && ast.unit.imports != null
                && ast.unit.imports.imports != null
                && !ast.unit.imports.imports.isEmpty();

        if (hasImports) {
            DebugSystem.info(NAME + LOG_TAG, "Generating indexes...");
            generateIndexes(ast, interpreter);

            if (irManager != null) {
                DebugSystem.info(NAME + LOG_TAG, "Generating IR...");
                compileToBytecode(ast);
            }
        } else {
            DebugSystem.debug(NAME + LOG_TAG, "Skipping index/IR generation (no imports)");
        }
        
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
                    DebugSystem.info(NAME + LOG_TAG, "Program interpretation completed");
                    return;
                }
            }
        }

        interpreter.run(ast);
        DebugSystem.info(NAME + LOG_TAG, "Program interpretation completed");
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

    private void printHelp() {
        out("Coderive CommandRunner - Execute Coderive programs");
        out("Usage: CommandRunner <filename> [options]");
        out("       CommandRunner compile <filename>");
        out();
        out("Options:");
        out("  -i, --interpret     Interpret the program (default)");
        out("  -o <file>           Write output to file");
        out("  --debug             Enable debug output");
        out("  --trace             Enable trace-level debugging");
        out("  --quiet             Only show errors");
        out("  -h, --help          Show this help message");
        out();
        out("Commands:");
        out("  compile <file>      Compile source to bytecode (.codb)");
        out("Environment flags:");
        out("  COD_PTAC_MODE=interpreter|compile-only|compile-execute");
        out("  COD_PTAC_FALLBACK=true|false");
        out();
        out("Examples:");
        out("  CommandRunner program.cod");
        out("  CommandRunner program.cod -o output.txt");
        out("  CommandRunner compile program.cod");
    }

    public static void main(String[] args) {
        try {
            CommandRunner runner = new CommandRunner();
            runner.run(args);
        } catch (Exception e) {
            DebugSystem.error(NAME + LOG_TAG, "Execution failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
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
