package cod.runner;

import cod.runner.BaseRunner;
import cod.ast.nodes.*;
import cod.debug.DebugSystem;
import cod.interpreter.Interpreter;

public class CommandRunner extends BaseRunner {
    
    // Operation modes
    private enum OperationMode {
        INTERPRET,
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
        OperationMode mode = OperationMode.INTERPRET;
        String outputFilename = null;
        boolean showHelp = false;
        
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
            } else if ("--interpret".equals(arg) || "-i".equals(arg)) {
                mode = OperationMode.INTERPRET;
            } else if ("-o".equals(arg)) {
                if (i + 1 < args.length) {
                    outputFilename = args[i + 1];
                    i++;
                } else {
                    System.err.println("Error: -o option requires an output filename.");
                }
            } else if ("--help".equals(arg) || "-h".equals(arg)) {
                showHelp = true;
            }
        }
        
        // Show help and exit if requested
        if (showHelp) {
            printHelp();
            return;
        }
        
        // Set output filename if provided
        if (outputFilename != null) {
            config.withOutputFilename(outputFilename);
        }
        
        // Validate input filename
        if (config.inputFilename == null || config.inputFilename.isEmpty()) {
            printHelp();
            throw new RuntimeException("No input file specified");
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
            case INTERPRET:
                executeInterpretation(ast);
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

    private void executeInterpretation(ProgramNode ast) {
        DebugSystem.info(LOG_TAG, "Starting program interpretation");
        interpreter.run(ast);
        DebugSystem.info(LOG_TAG, "Program interpretation completed");
    }
    
    private void printHelp() {
        System.out.println("Coderive CommandRunner - Multi-target language runner");
        System.out.println();
        System.out.println("Usage: java CommandRunner [options] <input_file.cdrv>");
        System.out.println();
        System.out.println("Operation Modes:");
        System.out.println("  --interpret, -i     Interpret the program (default)");
        System.out.println("  --compile, -c       Compile to native assembly");
        System.out.println("  --compile-bytecode  Compile to bytecode only");
        System.out.println("  --compile-both      Compile to both bytecode and native");
        System.out.println();
        System.out.println("Parser Options:");
        System.out.println("  --manual            Use manual parser (default)");
        System.out.println("  --antlr             Use ANTLR parser");
        System.out.println();
        System.out.println("Output Options:");
        System.out.println("  -o <file>           Output filename for compilation");
        System.out.println("  --print-ast         Print the Abstract Syntax Tree");
        System.out.println();
        System.out.println("Analysis Options:");
        System.out.println("  --no-lint           Disable linting");
        System.out.println("  --stop-on-lint      Stop execution on lint errors");
        System.out.println();
        System.out.println("Debug Options:");
        System.out.println("  --debug             Enable debug output");
        System.out.println("  --trace             Enable trace-level output");
        System.out.println("  --help, -h          Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java CommandRunner program.cdrv --interpret");
        System.out.println("  java CommandRunner program.cdrv --compile -o output.s");
        System.out.println("  java CommandRunner program.cdrv --compile-bytecode --print-ast");
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
