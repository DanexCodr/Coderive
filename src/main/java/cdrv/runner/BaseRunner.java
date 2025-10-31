package cdrv.runner;

import cdrv.ast.*;
import cdrv.ast.nodes.*;
import cdrv.debug.DebugSystem;
import cdrv.ast.ASTPrinter;
import java.io.*;
import java.util.List;
import java.util.Map;

// ANTLR-specific imports
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.Recognizer;

// Manual-specific imports
import cdrv.ast.ManualCoderiveLexer;
import cdrv.ast.ManualCoderiveParser;

// Linter import
import cdrv.debug.Linter;

public abstract class BaseRunner {
    
    // Enum for parser mode
    public enum ParserMode {
        ANTLR,
        MANUAL
    }

    // Configuration class for runner options - now with builder pattern
    public static class RunnerConfig {
        public ParserMode parserMode = ParserMode.MANUAL;
        public boolean printAST = false;
        public boolean enableLinting = true;
        public boolean stopOnLintError = false;
        public String inputFilename;
        public String outputFilename;
        public DebugSystem.Level debugLevel = DebugSystem.Level.INFO;
        
        public RunnerConfig(String inputFilename) {
            this.inputFilename = inputFilename;
        }
        
        // Builder-style methods for fluent configuration
        public RunnerConfig withParserMode(ParserMode mode) {
            this.parserMode = mode;
            return this;
        }
        
        public RunnerConfig withPrintAST(boolean printAST) {
            this.printAST = printAST;
            return this;
        }
        
        public RunnerConfig withLinting(boolean enableLinting) {
            this.enableLinting = enableLinting;
            return this;
        }
        
        public RunnerConfig withStopOnLintError(boolean stopOnLintError) {
            this.stopOnLintError = stopOnLintError;
            return this;
        }
        
        public RunnerConfig withOutputFilename(String outputFilename) {
            this.outputFilename = outputFilename;
            return this;
        }
        
        public RunnerConfig withDebugLevel(DebugSystem.Level debugLevel) {
            this.debugLevel = debugLevel;
            return this;
        }
    }

    // Configuration interface for anonymous inner class style
    public interface Configuration {
        void configure(RunnerConfig config);
    }

    protected static final String LOG_TAG = "RUNNER";

    /**
     * Common method to parse a source file using either ANTLR or manual parser
     */
    protected ProgramNode parseSourceFile(String filename, ParserMode mode) throws Exception {
        switch (mode) {
            case ANTLR:
                return parseWithAntlr(filename);
            case MANUAL:
                return parseWithManual(filename);
            default:
                throw new IllegalArgumentException("Unknown parser mode: " + mode);
        }
    }

    /**
     * Parse using ANTLR parser
     */
    private ProgramNode parseWithAntlr(String filename) throws Exception {
        DebugSystem.debug(LOG_TAG, "Loading source file for ANTLR: " + filename);
        InputStream is = new FileInputStream(filename);
        
        ANTLRInputStream input = new ANTLRInputStream(is);
        CoderiveLexer lexer = new CoderiveLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CoderiveParser parser = new CoderiveParser(tokens);

        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                  int line, int charPositionInLine, String msg,
                                  RecognitionException e) {
                String error = "Syntax error at line " + line + ":" + charPositionInLine + " - " + msg;
                DebugSystem.error("PARSER", error);
                throw new RuntimeException(error);
            }
        });

        DebugSystem.debug("PARSER", "Building ANTLR parse tree...");
        CoderiveParser.ProgramContext programContext = parser.program();
        DebugSystem.debug("PARSER", "ANTLR parse tree built successfully");

        ASTBuilder builder = new ASTBuilder();
        ProgramNode ast = builder.build(programContext);
        
        return ast;
    }

    /**
     * Parse using manual parser
     */
    private ProgramNode parseWithManual(String filename) throws Exception {
        DebugSystem.debug(LOG_TAG, "Loading source file for Manual Parser: " + filename);
        InputStream is = new FileInputStream(filename);
        
        String sourceCode = readFileToString(is);
        
        DebugSystem.debug("PARSER", "Tokenizing with ManualLexer...");
        ManualCoderiveLexer lexer = new ManualCoderiveLexer(sourceCode);
        List<ManualCoderiveLexer.Token> tokens = lexer.tokenize();
        
        DebugSystem.debug("PARSER", "Parsing with ManualParser...");
        ManualCoderiveParser parser = new ManualCoderiveParser(tokens);
        
        ProgramNode ast = parser.parseProgram();
        DebugSystem.debug("PARSER", "Manual parsing completed successfully");
        
        return ast;
    }

    /**
     * Common file reading utility
     */
    protected String readFileToString(InputStream is) throws Exception {
        java.util.Scanner scanner = new java.util.Scanner(is).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    /**
     * Common debug system configuration
     */
    protected void configureDebugSystem(DebugSystem.Level level) {
        DebugSystem.setLevel(level);
        DebugSystem.enableCategory("BYTECODE");
        DebugSystem.enableCategory("MTOT");
        DebugSystem.enableCategory(LOG_TAG);
        DebugSystem.enableCategory("PARSER");
        DebugSystem.enableCategory("INTERPRETER");
        DebugSystem.enableCategory("AST");
        DebugSystem.setShowTimestamp(true);
    }

/**
 * Print AST if enabled in config with proper output synchronization
 */
protected void printASTIfEnabled(ProgramNode ast, RunnerConfig config) {
    if (config.printAST) {
        // Ensure any pending stderr output is flushed before printing AST to stdout
        System.err.flush();
        
        DebugSystem.info("AST", "Printing Abstract Syntax Tree:");
        ASTPrinter printer = new ASTPrinter();
        ASTPrinter.print(ast);
        
        // Flush stdout after AST printing
        System.out.flush();
        
        DebugSystem.debug("AST", "AST printing completed");
    }
}

/**
 * Perform linting if enabled in config
 */
protected boolean performLinting(ProgramNode ast, RunnerConfig config) {
    if (!config.enableLinting) {
        DebugSystem.debug("LINTER", "Linting disabled by configuration");
        return true;
    }
    
    DebugSystem.startTimer("linting");
    Linter linter = new Linter();
    List<String> warnings = linter.lint(ast);
    DebugSystem.stopTimer("linting");
    
    // Print warnings immediately and completely
    Linter.WarningUtils.printWarnings(warnings);
    
    // Force complete output synchronization
    System.out.flush();
    System.err.flush();
    
    if (!warnings.isEmpty() && config.stopOnLintError) {
        DebugSystem.error(LOG_TAG, "Execution stopped due to lint errors.");
        return false;
    }
    
    return true;
}

    /**
     * Common method to extract filename from arguments
     */
    protected String extractFilenameFromArgs(String[] args, String defaultFilename) {
        for (String arg : args) {
            if (!arg.startsWith("--") && !arg.equals("-o")) {
                return arg;
            }
        }
        return defaultFilename;
    }

    /**
     * Common method to process command line arguments and create configuration
     * Now supports Configuration callback
     */
    protected RunnerConfig processCommandLineArgs(String[] args, String defaultInputFilename, Configuration configCallback) {
        RunnerConfig config = new RunnerConfig(defaultInputFilename);
        
        // Apply command line arguments first
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--antlr".equals(arg)) {
                config.parserMode = ParserMode.ANTLR;
            } else if ("--manual".equals(arg)) {
                config.parserMode = ParserMode.MANUAL;
            } else if ("--print-ast".equals(arg)) {
                config.printAST = true;
            } else if ("--no-lint".equals(arg)) {
                config.enableLinting = false;
            } else if ("--stop-on-lint".equals(arg)) {
                config.stopOnLintError = true;
            } else if ("--debug".equals(arg)) {
                config.debugLevel = DebugSystem.Level.DEBUG;
            } else if ("--trace".equals(arg)) {
                config.debugLevel = DebugSystem.Level.TRACE;
            } else if ("-o".equals(arg)) {
                if (i + 1 < args.length) {
                    config.outputFilename = args[i + 1];
                    i++;
                } else {
                    System.err.println("Error: -o option requires an output filename.");
                }
            }
        }
        
        // Extract filename if not already set by -o processing
        if (config.inputFilename == null) {
            config.inputFilename = extractFilenameFromArgs(args, defaultInputFilename);
        }
        
        // Apply configuration callback if provided
        if (configCallback != null) {
            configCallback.configure(config);
        }
        
        return config;
    }

    // Overloaded version for backward compatibility
    protected RunnerConfig processCommandLineArgs(String[] args, String defaultInputFilename) {
        return processCommandLineArgs(args, defaultInputFilename, null);
    }

    /**
     * Abstract method to be implemented by specific runners
     */
    public abstract void run(String[] args) throws Exception;
}