// BaseRunner.java
package cod.runner;

import cod.ast.nodes.*;
import cod.debug.DebugSystem;
import java.io.*;
import java.util.List;

import cod.lexer.*;
import cod.parser.MainParser;

public abstract class BaseRunner {

    public static class RunnerConfig {
        public String inputFilename;
        public String outputFilename;
        public DebugSystem.Level debugLevel = DebugSystem.Level.INFO;
        
        public RunnerConfig(String inputFilename) {
            this.inputFilename = inputFilename;
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

    public interface Configuration {
        void configure(RunnerConfig config);
    }

    protected static final String LOG_TAG = "RUNNER";

    public ProgramNode parse(String filename) throws Exception {
        DebugSystem.debug(LOG_TAG, "Loading source file: " + filename);
        InputStream is = new FileInputStream(filename);
        
        String sourceCode = readFileToString(is);
        DebugSystem.debug(LOG_TAG, "Source length: " + sourceCode.length() + " chars");
        
        DebugSystem.debug("PARSER", "Tokenizing...");
        MainLexer lexer = new MainLexer(sourceCode);
        List<Token> tokens = lexer.tokenize();
        DebugSystem.debug("PARSER", "Generated " + tokens.size() + " tokens");
        
        DebugSystem.debug("PARSER", "Parsing...");
        MainParser parser = new MainParser(tokens);
        
        ProgramNode ast = parser.parseProgram();
        DebugSystem.debug("PARSER", "Parsing completed successfully");
        
        return ast;
    }

    protected String readFileToString(InputStream is) throws Exception {
        java.util.Scanner scanner = new java.util.Scanner(is).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    protected void configureDebugSystem(DebugSystem.Level level) {
        DebugSystem.setLevel(level);
        DebugSystem.enableCategory("BYTECODE");
        DebugSystem.enableCategory("MTOT");
        DebugSystem.enableCategory(LOG_TAG);
        DebugSystem.enableCategory("PARSER");
        DebugSystem.enableCategory("INTERPRETER");
        DebugSystem.enableCategory("AST");
        DebugSystem.setShowTimestamp(true);
        DebugSystem.info(LOG_TAG, "DebugSystem configured to level: " + level);
    }

    protected String extractFilenameFromArgs(String[] args, String defaultFilename) {
        for (String arg : args) {
            if (!arg.startsWith("--") && !arg.equals("-o")) {
                DebugSystem.debug(LOG_TAG, "Extracted filename from args: " + arg);
                return arg;
            }
        }
        DebugSystem.debug(LOG_TAG, "Using default filename: " + defaultFilename);
        return defaultFilename;
    }

    protected RunnerConfig processCommandLineArgs(String[] args, String defaultInputFilename, Configuration configCallback) {
        DebugSystem.debug(LOG_TAG, "Processing command line args, count: " + args.length);
        RunnerConfig config = new RunnerConfig(defaultInputFilename);
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--debug".equals(arg)) {
                config.debugLevel = DebugSystem.Level.DEBUG;
                DebugSystem.debug(LOG_TAG, "Set debug level to DEBUG");
            } else if ("--trace".equals(arg)) {
                config.debugLevel = DebugSystem.Level.TRACE;
                DebugSystem.trace(LOG_TAG, "Set debug level to TRACE");
            } else if ("-o".equals(arg)) {
                if (i + 1 < args.length) {
                    config.outputFilename = args[i + 1];
                    i++;
                    DebugSystem.debug(LOG_TAG, "Set output filename: " + config.outputFilename);
                } else {
                    System.err.println("Error: -o option requires an output filename.");
                    DebugSystem.error(LOG_TAG, "-o option missing filename");
                }
            }
        }
        
        if (config.inputFilename == null) {
            config.inputFilename = extractFilenameFromArgs(args, defaultInputFilename);
        }
        
        if (configCallback != null) {
            configCallback.configure(config);
        }
        
        DebugSystem.debug(LOG_TAG, "Config: input=" + config.inputFilename + 
            ", output=" + config.outputFilename + ", level=" + config.debugLevel);
        
        return config;
    }

    protected RunnerConfig processCommandLineArgs(String[] args, String defaultInputFilename) {
        return processCommandLineArgs(args, defaultInputFilename, null);
    }

    public abstract void run(String[] args) throws Exception;
}