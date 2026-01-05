// InterpreterRunner.java
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
    private final String definedFilePath = "/JavaNIDE/Programming-Language/Coderive/executables/LazyLoop.cod";
    
    private final Interpreter interpreter;
    
    public InterpreterRunner() {
        this.interpreter = new Interpreter();
    }
    
    @Override
    public void run(String[] args) throws Exception {
        String inputFilename = getInputFilename(args);
        
        RunnerConfig config = processCommandLineArgs(args, inputFilename, new Configuration() {
            @Override
            public void configure(RunnerConfig config) {
                config.withDebugLevel(DebugSystem.Level.OFF);
            }
        });
        
        configureDebugSystem(config.debugLevel);
        
        DebugSystem.info(LOG_TAG, "Starting interpreter execution...");
        DebugSystem.info(LOG_TAG, "Input file: " + config.inputFilename);
        
        ProgramNode ast = parse(config.inputFilename);
        if (ast == null) throw new RuntimeException("Parsing failed, AST is null.");
        
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
            e.printStackTrace();
            System.err.println("\nUsage: InterpreterRunner [filename] [options]");
            System.err.println("Options:");
            System.err.println("  -o <file>      Output file");
            System.err.println("\nExample:");
            System.err.println("  InterpreterRunner myprogram.cod");
        }
    }
}