package cdrv.runner;

import cdrv.runner.BaseRunner;
import cdrv.ast.nodes.*;
import cdrv.debug.DebugSystem;
import cdrv.interpreter.Interpreter;

public class InterpreterRunner extends BaseRunner {

    private final String androidPath = "/storage/emulated/0";
    private final String definedFilePath = "/JavaNIDE/Programming-Language/Coderive/executables/InteractiveDemo.cdrv";
    
    private final Interpreter interpreter;
    
    public InterpreterRunner() {
        this.interpreter = new Interpreter();
    }
    
    @Override
    public void run(String[] args) throws Exception {
        String defaultFilename = androidPath + definedFilePath;
        
        // Process command line arguments with anonymous configuration
        RunnerConfig config = processCommandLineArgs(args, defaultFilename, new Configuration() {
            @Override
            public void configure(RunnerConfig config) {
                // Interpreter-specific defaults
                config.withParserMode(ParserMode.MANUAL)
                      .withLinting(true)
                      .withPrintAST(false)
                      .withDebugLevel(DebugSystem.Level.INFO);
            }
        });
        
        // Configure debug system with the specified level
        configureDebugSystem(config.debugLevel);
        
        DebugSystem.info(LOG_TAG, "Starting interpreter execution");
        DebugSystem.info(LOG_TAG, "Parser mode: " + config.parserMode);
        DebugSystem.info(LOG_TAG, "Input file: " + config.inputFilename);
        DebugSystem.info(LOG_TAG, "Print AST: " + config.printAST);
        DebugSystem.info(LOG_TAG, "Enable linting: " + config.enableLinting);
        
        // Parse source file
        DebugSystem.startTimer("parsing_and_ast");
        ProgramNode ast = parseSourceFile(config.inputFilename, config.parserMode);
        if (ast == null) {
            throw new RuntimeException("Parsing failed, AST is null.");
        }
        DebugSystem.stopTimer("parsing_and_ast");
        DebugSystem.info(LOG_TAG, "AST built successfully");
        
        // Boolean flag to ensure sequential execution after linting
        boolean lintingCompleted = false;
        
        // Linting - COMPLETE BEFORE doing anything else
        if (config.enableLinting) {
            boolean lintingPassed = performLinting(ast, config);
            if (!lintingPassed) {
                return; // Stop execution if linting failed
            }
            // Add a slight delay after linting
            try {
                Thread.sleep(100); // 100ms delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                DebugSystem.warn(LOG_TAG, "Delay after linting was interrupted");
            }
            // Set flag to indicate linting is completely finished
            lintingCompleted = true;
        } else {
            // If linting is disabled, mark as completed to proceed
            lintingCompleted = true;
        }
        
        // Only proceed if linting has completed (either successfully or was disabled)
        if (lintingCompleted) {
            // Print AST - ONLY AFTER linting is completely finished
            printASTIfEnabled(ast, config);
            
            // Execute using Interpreter directly
            DebugSystem.info(LOG_TAG, "Starting program interpretation");
            interpreter.run(ast);
            DebugSystem.info(LOG_TAG, "Program interpretation completed");
        }
    }

    public static void main(String[] args) {
        try {
            InterpreterRunner runner = new InterpreterRunner();
            runner.run(args);
        } catch (Exception e) {
            DebugSystem.error("INTERPRETER", "Execution failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}