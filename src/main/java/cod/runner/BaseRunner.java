package cod.runner;

import cod.ast.node.*;
import cod.debug.DebugSystem;
import cod.interpreter.Interpreter;
import cod.semantic.ImportResolver;
import cod.interpreter.Index;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.File;
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

    protected static final String
    LOG_TAG = "RUNNER",
    PARSER = "PARSER",
    IR = "IR",
    NATIVE = "NATIVE",
    INTERPRETER = "INTERPRETER",
    AST = "AST";
    
    public static void out(String s) {
        System.out.println(s);
    }
    
    public static void outE(String err) {
        System.err.println(err);
    }
    
    public static void out() {
        System.out.println();
    }
    
    public static void outE() {
        System.err.println();
    }
    
    public Program parse(String filename, Interpreter interpreter) throws Exception {
        DebugSystem.debug(LOG_TAG, "Loading source file: " + filename);
        
        // Use Java 7 Files API to read entire file
        String sourceCode = new String(
            Files.readAllBytes(Paths.get(filename)), 
            StandardCharsets.UTF_8
        );
        
        DebugSystem.debug(LOG_TAG, "Source length: " + sourceCode.length() + " chars");
        DebugSystem.debug(PARSER, "Tokenizing...");
        
        MainLexer lexer = new MainLexer(sourceCode);
        List<Token> tokens = lexer.tokenize();

        DebugSystem.debug(PARSER, "Generated " + tokens.size() + " tokens");
        
        DebugSystem.debug(PARSER, "Parsing...");
        
        MainParser parser = new MainParser(tokens, interpreter);
        Program ast = parser.parseProgram();
        
        DebugSystem.debug(PARSER, "Parsing completed successfully");
       
        return ast;
    }

    protected void configureDebugSystem(DebugSystem.Level level) {
        DebugSystem.setLevel(level);
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

    protected RunnerConfig processArgs(String[] args, String defaultInputFilename, Configuration configCallback) {
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
                    outE("Error: -o option requires an output filename.");
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

    protected RunnerConfig processArgs(String[] args, String defaultInputFilename) {
        return processArgs(args, defaultInputFilename, null);
    }
    
    // ========== INDEX GENERATION METHODS ==========
    
    /**
     * Generate indexes for all units in the program.
     * This should be called before execution to enable O(1) import resolution.
     * 
     * @param ast the parsed program AST
     * @param interpreter the interpreter instance
     */
    protected void generateIndexes(Program ast, Interpreter interpreter) {
        if (ast == null || ast.unit == null) {
            DebugSystem.debug("INDEX", "No program or unit, skipping index generation");
            return;
        }
        
        DebugSystem.debug("INDEX", "=== Starting index generation ===");
        
        ImportResolver resolver = interpreter.getImportResolver();
        String srcMainRoot = resolver.getSrcMainRoot();
        
        if (srcMainRoot == null) {
            DebugSystem.debug("INDEX", "No src/main root found, skipping index generation");
            DebugSystem.debug("INDEX", "Current file directory: " + resolver.getCurrentFileDirectory());
            return;
        }
        
        DebugSystem.debug("INDEX", "Using src/main root: " + srcMainRoot);
        
        // Generate index for the main unit
        if (ast.unit.name != null && !ast.unit.name.equals("default")) {
            generateIndexForUnit(ast.unit.name, srcMainRoot);
        }
        
        // Generate indexes for imported units
        if (ast.unit.imports != null && ast.unit.imports.imports != null) {
            DebugSystem.debug("INDEX", "Processing imports: " + ast.unit.imports.imports);
            
            for (String importName : ast.unit.imports.imports) {
                // Parse unit name from import (format: unit.Class)
                String[] parts = importName.split("\\.");
                if (parts.length > 0) {
                    String unitName = parts[0];
                    DebugSystem.debug("INDEX", "Generating index for imported unit: " + unitName);
                    generateIndexForUnit(unitName, srcMainRoot);
                }
            }
        }
        
        // Also generate indexes for any units found in the loaded programs cache
        for (String unitName : resolver.getLoadedImports()) {
            generateIndexForUnit(unitName, srcMainRoot);
        }
        
        DebugSystem.debug("INDEX", "=== Index generation complete ===");
        
        // Log cache statistics
        if (DebugSystem.getLevel().compareTo(DebugSystem.Level.DEBUG) >= 0) {
            DebugSystem.debug("INDEX", "Import resolver cache stats: " + resolver.getCacheStats());
        }
    }
    
    /**
     * Generate index for a specific unit.
     * 
     * @param unitName the name of the unit
     * @param srcMainRoot the root src/main directory path
     */
    private void generateIndexForUnit(String unitName, String srcMainRoot) {
        if (unitName == null || unitName.isEmpty()) {
            return;
        }
        
        String unitPath = srcMainRoot + "/" + unitName;
        File unitDir = new File(unitPath);
        
        if (!unitDir.exists()) {
            DebugSystem.debug("INDEX", "Unit directory does not exist: " + unitPath);
            return;
        }
        
        if (!unitDir.isDirectory()) {
            DebugSystem.debug("INDEX", "Path is not a directory: " + unitPath);
            return;
        }
        
        // Check if index exists and is up to date
        Index index = Index.load(unitName);
        boolean needsUpdate = (index == null || index.isStale(unitPath));
        
        if (needsUpdate) {
            DebugSystem.debug("INDEX", "Generating fresh index for unit: " + unitName);
            index = new Index(unitName);
            
            if (index.refresh(unitPath)) {
                if (index.save()) {
                    DebugSystem.debug("INDEX", "Generated index for unit: " + unitName + 
                                     " (" + index.size() + " classes)");
                    
                    // Log class names for debugging
                    if (DebugSystem.getLevel().compareTo(DebugSystem.Level.TRACE) >= 0) {
                        DebugSystem.trace("INDEX", "Classes in " + unitName + ": " + index.getClassNames());
                    }
                } else {
                    DebugSystem.warn("INDEX", "Failed to save index for unit: " + unitName);
                }
            } else {
                DebugSystem.debug("INDEX", "No .cod files found in unit: " + unitName);
            }
        } else {
            DebugSystem.debug("INDEX", "Index is up to date for unit: " + unitName + 
                             " (" + index.size() + " classes)");
        }
    }
    
    /**
     * Generate index for a unit using a specific path (alternative to using srcMainRoot).
     * 
     * @param unitName the name of the unit
     * @param unitPath the absolute path to the unit directory
     */
    protected void generateIndexForUnitWithPath(String unitName, String unitPath) {
        if (unitName == null || unitName.isEmpty()) {
            return;
        }
        
        if (unitPath == null || unitPath.isEmpty()) {
            return;
        }
        
        File unitDir = new File(unitPath);
        
        if (!unitDir.exists() || !unitDir.isDirectory()) {
            DebugSystem.debug("INDEX", "Invalid unit path: " + unitPath);
            return;
        }
        
        Index index = Index.load(unitName);
        boolean needsUpdate = (index == null || index.isStale(unitPath));
        
        if (needsUpdate) {
            DebugSystem.debug("INDEX", "Generating index for unit: " + unitName + " at " + unitPath);
            index = new Index(unitName);
            
            if (index.refresh(unitPath)) {
                index.save();
                DebugSystem.debug("INDEX", "Generated index for unit: " + unitName + 
                                 " (" + index.size() + " classes)");
            }
        } else {
            DebugSystem.debug("INDEX", "Index up to date for unit: " + unitName);
        }
    }
    
    /**
     * Force regenerate all indexes in the project.
     * This is useful after significant code changes.
     * 
     * @param srcMainRoot the root src/main directory path
     */
    protected void regenerateAllIndexes(String srcMainRoot) {
        if (srcMainRoot == null || srcMainRoot.isEmpty()) {
            DebugSystem.debug("INDEX", "Cannot regenerate: no src/main root");
            return;
        }
        
        File rootDir = new File(srcMainRoot);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            DebugSystem.debug("INDEX", "Cannot regenerate: src/main root not found: " + srcMainRoot);
            return;
        }
        
        DebugSystem.debug("INDEX", "=== Regenerating all indexes in: " + srcMainRoot + " ===");
        
        File[] units = rootDir.listFiles();
        if (units != null) {
            int generated = 0;
            for (File unit : units) {
                if (unit.isDirectory()) {
                    String unitName = unit.getName();
                    Index index = new Index(unitName);
                    if (index.refresh(unit.getAbsolutePath())) {
                        if (index.save()) {
                            generated++;
                            DebugSystem.debug("INDEX", "Regenerated index for: " + unitName);
                        }
                    }
                }
            }
            DebugSystem.debug("INDEX", "Regenerated " + generated + " indexes");
        }
        
        DebugSystem.debug("INDEX", "=== Index regeneration complete ===");
    }
    
    /**
     * Clear all index caches.
     */
    protected void clearIndexCaches(Interpreter interpreter) {
        if (interpreter != null && interpreter.getImportResolver() != null) {
            interpreter.getImportResolver().clearCache();
            DebugSystem.debug("INDEX", "Cleared import resolver caches");
        }
    }

    public abstract void run(String[] args) throws Exception;
}