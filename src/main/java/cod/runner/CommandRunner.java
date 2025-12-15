package cod.runner;

import cod.runner.BaseRunner;
import cod.ast.nodes.*;
import cod.debug.DebugSystem;
import cod.interpreter.Interpreter;

public class CommandRunner extends BaseRunner {
    
    private final Interpreter interpreter;
    private boolean enableOptimization = false;
    private boolean showOptimizationStats = false;
    
    public CommandRunner() {
        this.interpreter = new Interpreter();
    }
    
    @Override
    public void run(String[] args) throws Exception {
        String outputFilename = null;
        enableOptimization = false;
        showOptimizationStats = false;
        
        RunnerConfig config = processCommandLineArgs(args, null, new Configuration() {
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
                    System.err.println("Error: -o option requires an output filename.");
                }
            } else if ("-O".equals(arg) || "--optimize".equals(arg)) {
                enableOptimization = true;
                DebugSystem.info(LOG_TAG, "Constant folding optimization enabled");
            } else if ("--opt-stats".equals(arg)) {
                showOptimizationStats = true;
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
        
        DebugSystem.info(LOG_TAG, "Starting CommandRunner execution");
        DebugSystem.info(LOG_TAG, "Input file: " + config.inputFilename);
        DebugSystem.info(LOG_TAG, "Optimization: " + (enableOptimization ? "ENABLED" : "DISABLED"));
        
        if (config.inputFilename == null || config.inputFilename.isEmpty()) {
            throw new RuntimeException("No input file specified. Usage: CommandRunner <filename> [options]");
        }
        
        DebugSystem.startTimer("parsing");
        ProgramNode ast = parse(config.inputFilename);
        if (ast == null) {
            throw new RuntimeException("Parsing failed, AST is null.");
        }
        DebugSystem.stopTimer("parsing");
        DebugSystem.info(LOG_TAG, "AST built successfully");
        
        // Apply constant folding optimization if enabled
        if (enableOptimization) {
            DebugSystem.startTimer("optimization");
            DebugSystem.info(LOG_TAG, "Applying constant folding optimization...");
            
            ast = optimizeAST(ast, true);
            
            DebugSystem.stopTimer("optimization");
            DebugSystem.info(LOG_TAG, "Optimization completed");
            
            if (showOptimizationStats) {
                printOptimizationSummary();
            }
        }
        
        executeInterpretation(ast);
        
        DebugSystem.info(LOG_TAG, "CommandRunner execution completed");
    }
    
    private void executeInterpretation(ProgramNode ast) {
        DebugSystem.info(LOG_TAG, "Starting program interpretation");
        interpreter.run(ast);
        DebugSystem.info(LOG_TAG, "Program interpretation completed");
    }
    
    private void printOptimizationSummary() {
        System.out.println("\n=== CONSTANT FOLDING SUMMARY ===");
        System.out.println("Constant folding has been applied to the AST.");
        System.out.println("Benefits:");
        System.out.println("  • Constant expressions evaluated at compile time");
        System.out.println("  • Runtime performance improved");
        System.out.println("  • Boolean chains (any[]/all[]) optimized");
        System.out.println("  • Type casts eliminated where possible");
        System.out.println("===============================\n");
    }
    
    private void printHelp() {
        System.out.println("Coderive CommandRunner - Execute Coderive programs");
        System.out.println("Usage: CommandRunner <filename> [options]\n");
        System.out.println("Options:");
        System.out.println("  -i, --interpret     Interpret the program (default)");
        System.out.println("  -O, --optimize      Enable constant folding optimization");
        System.out.println("  --opt-stats         Show optimization summary");
        System.out.println("  -o <file>           Write output to file");
        System.out.println("  --debug             Enable debug output");
        System.out.println("  --trace             Enable trace-level debugging");
        System.out.println("  --quiet             Only show errors");
        System.out.println("  -h, --help          Show this help message");
        System.out.println("\nExamples:");
        System.out.println("  CommandRunner program.cod");
        System.out.println("  CommandRunner program.cod -O");
        System.out.println("  CommandRunner program.cod -O --opt-stats");
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