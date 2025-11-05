package cod.runner;

import cod.runner.BaseRunner;
import cod.ast.nodes.*;
import cod.debug.DebugSystem;
import java.io.*;

public class CompilerRunner extends BaseRunner {
    
    // Enum for compiler output mode
    private enum CompilationMode {
        BOTH,
        BYTECODE_ONLY,
        NATIVE_ONLY
    }
    
    private final String androidPath = "/storage/emulated/0";
    private final String definedFilePath = "/JavaNIDE/Programming-Language/Coderive/executables/interactiveDemo.cdrv";
    private final String definedOutputFilePath = "/program.s";
    
    private final CompilationEngine compilationEngine;
    
    public CompilerRunner() {
        this.compilationEngine = new CompilationEngine();
    }

    @Override
    public void run(String[] args) throws Exception {
        String defaultFilename = androidPath + definedFilePath;
        final String defaultOutputFilename = androidPath + definedOutputFilePath;
        
        CompilationMode mode = CompilationMode.NATIVE_ONLY;
        
        // Process command line arguments with anonymous configuration
        RunnerConfig config = processCommandLineArgs(args, defaultFilename, new Configuration() {
            @Override
            public void configure(RunnerConfig config) {
                // Compiler-specific defaults
                config.withParserMode(ParserMode.MANUAL)
                      .withLinting(true)
                      .withPrintAST(false)
                      .withDebugLevel(DebugSystem.Level.INFO)
                      .withOutputFilename(defaultOutputFilename);
            }
        });
        
        // Process compiler-specific arguments
        for (String arg : args) {
            if ("--bytecode".equals(arg)) {
                mode = CompilationMode.BYTECODE_ONLY;
            } else if ("--native".equals(arg)) {
                mode = CompilationMode.NATIVE_ONLY;
            }
        }
        
        // Configure debug system with the specified level
        configureDebugSystem(config.debugLevel);
        
        DebugSystem.info(LOG_TAG, "Starting MTOT compilation pipeline");
        DebugSystem.info(LOG_TAG, "Parser mode: " + config.parserMode);
        DebugSystem.info(LOG_TAG, "Compilation mode: " + mode);
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

        // STAGE 2: LINTING (before compilation, as it should be)
        if (!performLinting(ast, config)) {
            return; // Stop compilation if linting failed
        }

        // STAGE 3: COMPILATION USING COMPILATION ENGINE
        switch (mode) {
            case BYTECODE_ONLY:
                compilationEngine.compileToBytecode(ast, true);
                break;
            case NATIVE_ONLY:
                compilationEngine.compileFullPipeline(ast, config.outputFilename, false);
                break;
            case BOTH:
                compilationEngine.compileFullPipeline(ast, config.outputFilename, true);
                break;
        }
        
        DebugSystem.info("MTOT", "Full compilation pipeline complete.");
    }

    public static void main(String[] args) {
        try {
            CompilerRunner runner = new CompilerRunner();
            runner.run(args);
        } catch (Exception e) {
            DebugSystem.error("MTOT", "Compilation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}