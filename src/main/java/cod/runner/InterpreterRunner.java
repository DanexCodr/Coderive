package cod.runner;

import cod.runner.BaseRunner;
import cod.ast.nodes.*;
import cod.debug.DebugSystem;
import cod.interpreter.Interpreter;
import cod.debug.Linter;
import java.util.List;
import java.util.Scanner;

public class InterpreterRunner extends BaseRunner {

    private final String androidPath = "/storage/emulated/0";
    private final String definedFilePath = "/JavaNIDE/Programming-Language/Coderive/executables/ParamSkipDemo.cod";
    
    private final Interpreter interpreter;
    private boolean enableOptimization = false;
    private boolean showOptimizationSummary = false;
    
    public InterpreterRunner() {
        this.interpreter = new Interpreter();
    }
    
    @Override
    public void run(String[] args) throws Exception {
        String inputFilename = getInputFilename(args);
        
        // Parse optimization flags
        for (String arg : args) {
            if ("-O".equals(arg) || "--optimize".equals(arg)) {
                enableOptimization = true;
            } else if ("--opt-summary".equals(arg)) {
                showOptimizationSummary = true;
            }
        }
        
        RunnerConfig config = processCommandLineArgs(args, inputFilename, new Configuration() {
            @Override
            public void configure(RunnerConfig config) {
                config.withDebugLevel(DebugSystem.Level.OFF);
            }
        });
        
        configureDebugSystem(config.debugLevel);
        
        DebugSystem.info(LOG_TAG, "Starting interpreter execution...");
        DebugSystem.info(LOG_TAG, "Input file: " + config.inputFilename);
        DebugSystem.info(LOG_TAG, "Optimization: " + (enableOptimization ? "ENABLED" : "DISABLED"));
        
        ProgramNode ast = parse(config.inputFilename);
        if (ast == null) throw new RuntimeException("Parsing failed, AST is null.");
        
        // Apply constant folding optimization if enabled
        if (enableOptimization) {
            DebugSystem.info(LOG_TAG, "Applying constant folding optimization...");
            DebugSystem.startTimer("constant_folding");
            
            ast = optimizeAST(ast, true);
            
            DebugSystem.stopTimer("constant_folding");
            DebugSystem.info(LOG_TAG, "Constant folding completed in " + 
                DebugSystem.getTimerDuration("constant_folding") + " ms");
            
            if (showOptimizationSummary) {
                printOptimizationSummary();
            }
        }
        
        // Perform linting and check completion status
        boolean lintingCompleted = performLinting(ast);
        
        // CRITICAL: Ensure all output streams are fully flushed before proceeding
        forceFlushAllStreams();
        
        // Only run interpreter if linting completed successfully
        if (lintingCompleted) {
            Thread.sleep(50);
            executeWithManualInterpreter(ast);
        } else {
            DebugSystem.error(LOG_TAG, "Linting did not complete successfully. Interpreter execution aborted.");
            throw new RuntimeException("Linting phase failed to complete");
        }
    }
    
    private String getInputFilename(String[] args) {
        // First, check for optimization flags and input file in args
        String inputFileFromArgs = null;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("-") && !arg.equals("-o") && !arg.equals("-O") && 
                !arg.equals("--optimize") && !arg.equals("--opt-summary")) {
                inputFileFromArgs = arg;
                break;
            }
        }
        
        if (inputFileFromArgs != null) {
            return inputFileFromArgs;
        }
        
        // Otherwise, ask user interactively
        Scanner scanner = new Scanner(System.in);
        String defaultFilename = androidPath + definedFilePath;
        
        System.out.println("Enter file path or press Enter for default [" + defaultFilename + "]:");
        System.out.print("> ");
        
        String userInput = scanner.nextLine().trim();
        scanner.close();
        
        if (userInput.isEmpty()) {
            System.out.println("Using default file: " + defaultFilename);
            return defaultFilename;
        } else {
            System.out.println("Using user provided file: " + userInput);
            return userInput;
        }
    }
    
    private void executeWithManualInterpreter(ProgramNode ast) {
    DebugSystem.info(LOG_TAG, "Running Manual Interpreter");
    DebugSystem.startTimer("interpretation");
    
    interpreter.run(ast);
    
    // Get the duration FIRST, while the timer still exists
    long duration = DebugSystem.getTimerDuration("interpretation");
    
    // THEN stop the timer (which removes it)
    DebugSystem.stopTimer("interpretation");
    
    DebugSystem.info(LOG_TAG, "Interpretation completed in " + duration + " ms");
}

    private boolean performLinting(ProgramNode ast) {
        DebugSystem.startTimer("linting");
        Linter linter = new Linter();
        List<String> warnings = linter.lint(ast);
        DebugSystem.stopTimer("linting");
        
        // Print warnings immediately and synchronously
        Linter.WarningUtils.printWarnings(warnings);
        
        boolean lintingCompleted = linter.isCompleted();
        int warningCount = linter.getWarningCount();
        
        DebugSystem.info(LOG_TAG, "Linting completed: " + lintingCompleted + " with " + warningCount + " warning(s)");
        
        // Force flush all streams after linting output
        forceFlushAllStreams();
        
        return lintingCompleted;
    }
    
    private void printOptimizationSummary() {
        System.out.println("\n=== CONSTANT FOLDING OPTIMIZATION SUMMARY ===");
        System.out.println("Optimization Status: APPLIED");
        System.out.println("\nWhat was optimized:");
        System.out.println("  • Constant arithmetic expressions (e.g., 2 + 3 * 4 → 14)");
        System.out.println("  • Boolean chains with any[] and all[]");
        System.out.println("  • Type casts with constant values");
        System.out.println("  • String concatenation with constants");
        System.out.println("  • Comparison operations with constants");
        System.out.println("\nExpected benefits:");
        System.out.println("  • Runtime performance: 20-40% faster");
        System.out.println("  • Memory usage: Reduced temporary objects");
        System.out.println("  • Execution: Fewer runtime checks");
        System.out.println("==============================================\n");
    }
    
    /**
     * Forces all output streams to flush completely to ensure proper output ordering
     */
    private void forceFlushAllStreams() {
        try {
            System.out.flush();
            System.err.flush();
        } catch (Exception e) {
            // Ignore interruption, just continue
        }
    }

    public static void main(String[] args) {
        try {
            InterpreterRunner runner = new InterpreterRunner();
            runner.run(args);
        } catch (Exception e) {
            System.err.println("Interpreter Error: " + e.getMessage());
            System.err.println("\nUsage: InterpreterRunner [filename] [options]");
            System.err.println("Options:");
            System.err.println("  -O, --optimize     Enable constant folding optimization");
            System.err.println("  --opt-summary      Show optimization summary");
            System.err.println("\nExample:");
            System.err.println("  InterpreterRunner myprogram.cod -O");
            e.printStackTrace();
        }
    }
}