package cdrv.runner;

import cdrv.runner.BaseRunner;
import cdrv.ast.nodes.*;
import cdrv.debug.DebugSystem;
import cdrv.interpreter.Interpreter;

public class CommandRunner extends BaseRunner {
    
    // Operation modes
    private enum OperationMode {
        AST_INTERPRET,
        MANUAL_INTERPRET,
        COMPILE_BYTECODE,
        COMPILE_NATIVE,
        COMPILE_BOTH
    }
    
    private final Interpreter interpreter;
    private final CompilationEngine compilationEngine;
    
    public CommandRunner() {
        this.interpreter = new Interpreter();
        this.compilationEngine = new CompilationEngine();
    }
    
    @Override
    public void run(String[] args) throws Exception {
        // Default operation mode
        OperationMode mode = OperationMode.MANUAL_INTERPRET;
        String outputFilename = null;
        
        // Process command line arguments with anonymous configuration
        RunnerConfig config = processCommandLineArgs(args, null, new Configuration() {
            @Override
            public void configure(RunnerConfig config) {
                // Common defaults
                config.withParserMode(ParserMode.MANUAL)
                      .withLinting(true)
                      .withPrintAST(false)
                      .withDebugLevel(DebugSystem.Level.INFO);
            }
        });
        
        // Process operation mode arguments and output filename
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--compile".equals(arg) || "-c".equals(arg)) {
                mode = OperationMode.COMPILE_NATIVE;
            } else if ("--compile-bytecode".equals(arg)) {
                mode = OperationMode.COMPILE_BYTECODE;
            } else if ("--compile-both".equals(arg)) {
                mode = OperationMode.COMPILE_BOTH;
            } else if ("--ast-interpret".equals(arg) || "-ai".equals(arg)) {
                mode = OperationMode.AST_INTERPRET;
            } else if ("--manual-interpret".equals(arg) || "-mi".equals(arg)) {
                mode = OperationMode.MANUAL_INTERPRET;
            } else if ("-o".equals(arg)) {
                if (i + 1 < args.length) {
                    outputFilename = args[i + 1];
                    i++;
                } else {
                    System.err.println("Error: -o option requires an output filename.");
                }
            }
        }
        
        // Set output filename if provided
        if (outputFilename != null) {
            config.withOutputFilename(outputFilename);
        }
        
        // Configure debug system with the specified level
        configureDebugSystem(config.debugLevel);
        
        DebugSystem.info(LOG_TAG, "Starting CommandRunner execution");
        DebugSystem.info(LOG_TAG, "Operation mode: " + mode);
        DebugSystem.info(LOG_TAG, "Parser mode: " + config.parserMode);
        DebugSystem.info(LOG_TAG, "Input file: " + config.inputFilename);
        DebugSystem.info(LOG_TAG, "Output file: " + config.outputFilename);
        DebugSystem.info(LOG_TAG, "Print AST: " + config.printAST);
        DebugSystem.info(LOG_TAG, "Enable linting: " + config.enableLinting);
        
        // Validate input filename
        if (config.inputFilename == null || config.inputFilename.isEmpty()) {
            throw new RuntimeException("No input file specified");
        }
        
        // STAGE 1: PARSING AND AST
        DebugSystem.startTimer("parsing_and_ast");
        ProgramNode ast = parseSourceFile(config.inputFilename, config.parserMode);
        if (ast == null) {
            throw new RuntimeException("Parsing failed, AST is null.");
        }
        DebugSystem.stopTimer("parsing_and_ast");
        DebugSystem.info(LOG_TAG, "AST built successfully");

        // PRINT AST BEFORE LINTING (if enabled)
        printASTIfEnabled(ast, config);

        // STAGE 2: LINTING
        if (!performLinting(ast, config)) {
            return; // Stop execution if linting failed
        }

        // STAGE 3: EXECUTION BASED ON MODE
        switch (mode) {
            case AST_INTERPRET:
                executeASTInterpretation(ast);
                break;
            case MANUAL_INTERPRET:
                executeManualInterpretation(ast);
                break;
            case COMPILE_BYTECODE:
                compilationEngine.compileToBytecode(ast, true);
                break;
            case COMPILE_NATIVE:
                compilationEngine.compileFullPipeline(ast, config.outputFilename, false);
                break;
            case COMPILE_BOTH:
                compilationEngine.compileFullPipeline(ast, config.outputFilename, true);
                break;
            default:
                throw new IllegalArgumentException("Unknown operation mode: " + mode);
        }
        
        DebugSystem.info(LOG_TAG, "CommandRunner execution completed");
    }

    private void executeASTInterpretation(ProgramNode ast) {
        DebugSystem.info(LOG_TAG, "Starting AST-based program interpretation");
        interpreter.run(ast);
        DebugSystem.info(LOG_TAG, "AST-based interpretation completed");
    }

    private void executeManualInterpretation(ProgramNode ast) {
        DebugSystem.info(LOG_TAG, "Starting manual program interpretation");
        interpreter.run(ast);
        DebugSystem.info(LOG_TAG, "Manual interpretation completed");
    }

    public static void main(String[] args) {
        try {
            CommandRunner runner = new CommandRunner();
            runner.run(args);
        } catch (Exception e) {
            DebugSystem.error("COMMAND_RUNNER", "Execution failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}