package cod.runner;

import cod.ast.nodes.ProgramNode;
import cod.compiler.MTOTNativeCompiler;
import cod.compiler.MTOTRegistry;
import cod.compiler.TACCompiler;
import cod.compiler.TACInstruction;
import cod.compiler.TACProgram;
import cod.debug.DebugSystem;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

public class CompilerRunner extends BaseRunner {

    // --- Configuration Constants (Hardcoded) ---
    private static final String ANDROID_PATH = "/storage/emulated/0";
    private static final String INPUT_FILE_DEFINITION = "/JavaNIDE/Programming-Language/Coderive/executables/InteractiveDemo.cod";
    private static final DebugSystem.Level DEBUG_LEVEL = DebugSystem.Level.INFO; // Set to INFO, DEBUG, or TRACE
    private static final TargetMode TARGET_MODE = TargetMode.NATIVE; 
    private static final String ARCHITECTURE = "aarch64"; // Hardcoded architecture for compilation target
    private static final boolean OPTIMIZATION_ENABLED = true; // NEW: Enable constant folding by default for compilation
    private static final boolean SHOW_OPTIMIZATION_INFO = true; // NEW: Show optimization info
    // --- End Configuration ---

    private enum TargetMode {
        TAC,
        NATIVE
    }

    @Override
    public void run(String[] args) throws Exception {
        
        // 1. Setup Configuration
        String defaultInputFilename = ANDROID_PATH + INPUT_FILE_DEFINITION;
        
        RunnerConfig config = new RunnerConfig(defaultInputFilename);
        config.withDebugLevel(DEBUG_LEVEL);

        // Check for command-line override of optimization
        boolean optimizationEnabled = OPTIMIZATION_ENABLED;
        for (String arg : args) {
            if ("--no-opt".equals(arg) || "-O0".equals(arg)) {
                optimizationEnabled = false;
            } else if ("-O".equals(arg) || "--optimize".equals(arg)) {
                optimizationEnabled = true;
            }
        }

        // 2. Configure Debug System
        configureDebugSystem(config.debugLevel);

        DebugSystem.info(LOG_TAG, "Starting Compiler execution...");
        DebugSystem.info(LOG_TAG, "Android Base Path: " + ANDROID_PATH);
        DebugSystem.info(LOG_TAG, "Input file: " + config.inputFilename);
        DebugSystem.info(LOG_TAG, "Target Mode: " + TARGET_MODE);
        DebugSystem.info(LOG_TAG, "Target Architecture: " + ARCHITECTURE);
        DebugSystem.info(LOG_TAG, "Constant Folding: " + (optimizationEnabled ? "ENABLED" : "DISABLED")); // NEW

        // 3. Frontend: Parse Source to AST
        ProgramNode ast = parse(config.inputFilename);
        if (ast == null) throw new RuntimeException("Parsing failed.");

        // NEW: Apply constant folding optimization
        if (optimizationEnabled) {
            DebugSystem.info(LOG_TAG, "Applying constant folding optimization...");
            DebugSystem.startTimer("constant_folding");
            
            ast = optimizeAST(ast, true);
            
            DebugSystem.stopTimer("constant_folding");
            DebugSystem.info(LOG_TAG, "Constant folding completed in " + 
                DebugSystem.getTimerDuration("constant_folding") + " ms");
            
            if (SHOW_OPTIMIZATION_INFO) {
                printOptimizationInfo();
            }
        }

        // 4. Middle-end: Compile AST to TAC
        DebugSystem.info(LOG_TAG, "Generating Three-Address Code...");
        DebugSystem.startTimer("tac_generation");
        TACCompiler tacCompiler = new TACCompiler();
        TACProgram tacProgram = tacCompiler.compile(ast);
        DebugSystem.stopTimer("tac_generation");
        DebugSystem.info(LOG_TAG, "TAC generation completed in " + 
            DebugSystem.getTimerDuration("tac_generation") + " ms");

        // 5. Output Generation
        String outputContent;
        String outputFilename = ANDROID_PATH + "/output.asm"; // NEW: Default output filename
        if (TARGET_MODE == TargetMode.TAC) {
            outputContent = generateTacOutput(tacProgram, optimizationEnabled);
            outputFilename = ANDROID_PATH + "/output.tac"; // For TAC mode
        } else {
            outputContent = generateNativeOutput(tacProgram, optimizationEnabled);
            outputFilename = ANDROID_PATH + "/output.asm"; // For native assembly
        }

        // 6. Write Result to File on /storage/emulated/0
        writeOutputToFile(outputContent, outputFilename);
        
        // 7. Also print truncated output to console
        printTruncatedOutput(outputContent);
        
        // NEW: Print compilation summary
        printCompilationSummary(optimizationEnabled, outputFilename);
    }

    private String generateTacOutput(TACProgram program, boolean optimizationEnabled) {
        StringBuilder sb = new StringBuilder();
        sb.append("; === COD Three-Address Code ===\n\n");
        
        // NEW: Add optimization info comment
        if (optimizationEnabled) {
            sb.append("; Generated with constant folding optimization\n");
            sb.append("; Constant expressions have been evaluated at compile-time\n\n");
        }
        
        for (Map.Entry<String, List<TACInstruction>> entry : program.getMethods().entrySet()) {
            sb.append("method ").append(entry.getKey()).append(":\n");
            List<TACInstruction> code = entry.getValue();
            for (int i = 0; i < code.size(); i++) {
                sb.append(String.format("  %03d: %s\n", i, code.get(i).toString()));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String generateNativeOutput(TACProgram program, boolean optimizationEnabled) {
        DebugSystem.info(LOG_TAG, "Compiling to Native Assembly (" + ARCHITECTURE + ")...");
        DebugSystem.startTimer("native_compilation");
        
        // Use getProfile instead of creating new one
        MTOTRegistry.CPUProfile profile = MTOTRegistry.getProfile(ARCHITECTURE);
        if (profile == null) {
            // Fall back to detected CPU if specified architecture not found
            profile = MTOTRegistry.detectCPU();
            DebugSystem.warn(LOG_TAG, "Architecture " + ARCHITECTURE + " not found, using detected: " + profile.architecture);
        }
        
        MTOTNativeCompiler nativeCompiler = new MTOTNativeCompiler(profile);
        StringBuilder sb = new StringBuilder();
        
        // NEW: Add optimization info comment
        if (optimizationEnabled) {
            sb.append("; Generated with constant folding optimization\n");
            sb.append("; Architecture: ").append(profile.architecture).append("\n");
            sb.append("; Optimizations: Constant expressions folded at compile-time\n\n");
        } else {
            sb.append("; Generated without optimizations\n");
            sb.append("; Architecture: ").append(profile.architecture).append("\n\n");
        }
        
        // Compile each method
        for (Map.Entry<String, List<TACInstruction>> entry : program.getMethods().entrySet()) {
            String asm = nativeCompiler.compileMethodFromTAC(entry.getKey(), entry.getValue());
            sb.append(asm).append("\n\n");
        }
        
        DebugSystem.stopTimer("native_compilation");
        DebugSystem.info(LOG_TAG, "Native compilation completed in " + 
            DebugSystem.getTimerDuration("native_compilation") + " ms");
        
        return sb.toString();
    }
    
    // NEW: Write output to file on /storage/emulated/0
    private void writeOutputToFile(String content, String filename) {
        try {
            DebugSystem.info(LOG_TAG, "Writing output to: " + filename);
            FileOutputStream fos = new FileOutputStream(filename);
            PrintStream ps = new PrintStream(fos);
            ps.print(content);
            ps.close();
            fos.close();
            DebugSystem.info(LOG_TAG, "Output successfully written to " + filename);
            DebugSystem.info(LOG_TAG, "File size: " + content.length() + " bytes");
        } catch (Exception e) {
            DebugSystem.error(LOG_TAG, "Failed to write output file: " + e.getMessage());
            System.err.println("ERROR: Could not write to " + filename + ": " + e.getMessage());
        }
    }
    
    private void printOptimizationInfo() {
        System.out.println("\n=== CONSTANT FOLDING INFORMATION ===");
        System.out.println("Constant folding has been applied to the AST before compilation.");
        System.out.println("\nOptimizations performed:");
        System.out.println("  • Arithmetic constant expressions evaluated");
        System.out.println("  • Boolean chains (any[]/all[]) optimized");
        System.out.println("  • Type casts with constants eliminated");
        System.out.println("  • String concatenation with constants pre-computed");
        System.out.println("\nBenefits for compiled code:");
        System.out.println("  • Smaller generated code size");
        System.out.println("  • Faster execution (no runtime computation for constants)");
        System.out.println("  • More efficient register allocation");
        System.out.println("======================================\n");
    }
    
    private void printCompilationSummary(boolean optimizationEnabled, String outputFilename) {
        System.out.println("\n=== COMPILATION SUMMARY ===");
        System.out.println("Target: " + TARGET_MODE);
        if (TARGET_MODE == TargetMode.NATIVE) {
            System.out.println("Architecture: " + ARCHITECTURE);
        }
        System.out.println("Optimizations: " + (optimizationEnabled ? "Constant folding applied" : "None"));
        System.out.println("Output file: " + outputFilename);
        System.out.println("Status: COMPLETED SUCCESSFULLY");
        System.out.println("============================\n");
    }
    
    private void printTruncatedOutput(String content) {
        final int MAX_DISPLAY_LENGTH = 4000;
        
        System.out.println("\n--- COMPILATION OUTPUT (Preview) ---\n");
        
        if (content.length() > MAX_DISPLAY_LENGTH) {
            String truncated = content.substring(0, MAX_DISPLAY_LENGTH);
            System.out.println(truncated);
            System.out.println("\n... [OUTPUT TRUNCATED - Length: " + content.length() + " chars] ...");
            System.out.println("Full output written to file.");
        } else {
            System.out.println(content);
        }
        System.out.println("\n-----------------------------------\n");
    }

    // Main method remains for execution entry
    public static void main(String[] args) {
        try {
            // Note: args are ignored due to the hardcoded config requirement
            // but we pass them through to check for optimization flags
            new CompilerRunner().run(args);
        } catch (Exception e) {
            System.err.println("Compiler Error: " + e.getMessage());
            // Only print stack trace if DEBUG_LEVEL is high
            if (DEBUG_LEVEL.compareTo(DebugSystem.Level.DEBUG) >= 0) {
                 e.printStackTrace();
            }
            
            // Show usage hints
            System.err.println("\nUsage hints:");
            System.err.println("  CompilerRunner [options]");
            System.err.println("  Options:");
            System.err.println("    --no-opt, -O0  Disable constant folding optimization");
            System.err.println("    -O, --optimize Enable constant folding (default: enabled)");
            System.err.println("    -o <file>      Output file (default: /storage/emulated/0/output.asm)");
        }
    }
}