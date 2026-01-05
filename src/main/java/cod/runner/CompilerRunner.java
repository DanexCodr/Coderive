// CompilerRunner.java
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
    private static final DebugSystem.Level DEBUG_LEVEL = DebugSystem.Level.INFO;
    private static final TargetMode TARGET_MODE = TargetMode.NATIVE; 
    private static final String ARCHITECTURE = "aarch64";
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

        // 2. Configure Debug System
        configureDebugSystem(config.debugLevel);

        DebugSystem.info(LOG_TAG, "Starting Compiler execution...");
        DebugSystem.info(LOG_TAG, "Android Base Path: " + ANDROID_PATH);
        DebugSystem.info(LOG_TAG, "Input file: " + config.inputFilename);
        DebugSystem.info(LOG_TAG, "Target Mode: " + TARGET_MODE);
        DebugSystem.info(LOG_TAG, "Target Architecture: " + ARCHITECTURE);

        // 3. Frontend: Parse Source to AST
        ProgramNode ast = parse(config.inputFilename);
        if (ast == null) throw new RuntimeException("Parsing failed.");

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
        String outputFilename = ANDROID_PATH + "/output.asm";
        if (TARGET_MODE == TargetMode.TAC) {
            outputContent = generateTacOutput(tacProgram);
            outputFilename = ANDROID_PATH + "/output.tac";
        } else {
            outputContent = generateNativeOutput(tacProgram);
            outputFilename = ANDROID_PATH + "/output.asm";
        }

        // 6. Write Result to File on /storage/emulated/0
        writeOutputToFile(outputContent, outputFilename);
        
        // 7. Also print truncated output to console
        printTruncatedOutput(outputContent);
        
        printCompilationSummary(outputFilename);
    }

    private String generateTacOutput(TACProgram program) {
        StringBuilder sb = new StringBuilder();
        sb.append("; === COD Three-Address Code ===\n\n");
        
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

    private String generateNativeOutput(TACProgram program) {
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
        
        sb.append("; Generated assembly for architecture: ").append(profile.architecture).append("\n\n");
        
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
    
    private void printCompilationSummary(String outputFilename) {
        System.out.println("\n=== COMPILATION SUMMARY ===");
        System.out.println("Target: " + TARGET_MODE);
        if (TARGET_MODE == TargetMode.NATIVE) {
            System.out.println("Architecture: " + ARCHITECTURE);
        }
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

    public static void main(String[] args) {
        try {
            new CompilerRunner().run(args);
        } catch (Exception e) {
            System.err.println("Compiler Error: " + e.getMessage());
            
            if (DEBUG_LEVEL.compareTo(DebugSystem.Level.DEBUG) >= 0) {
                 e.printStackTrace();
            }
            
            System.err.println("\nUsage hints:");
            System.err.println("  CompilerRunner [options]");
            System.err.println("  Options:");
            System.err.println("    -o <file>      Output file (default: /storage/emulated/0/output.asm)");
        }
    }
}