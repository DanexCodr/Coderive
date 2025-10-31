package cdrv.runner;

import cdrv.runner.BaseRunner;
import cdrv.ast.nodes.*;
import cdrv.debug.DebugSystem;
import java.io.*;
import java.util.List;
import java.util.Map;

// Bytecode compiler imports
import cdrv.compiler.BytecodeCompiler;
import cdrv.compiler.BytecodeProgram;
import cdrv.compiler.BytecodeInstruction;
import cdrv.compiler.MTOTNativeCompiler;
import cdrv.compiler.MTOTRegistry;

public class CompilerRunner extends BaseRunner {
    
    // Enum for compiler output mode
    private enum CompilationMode {
        BOTH,
        BYTECODE_ONLY,
        NATIVE_ONLY
    }

    @Override
    public void run(String[] args) throws Exception {
        String defaultFilename = "/storage/emulated/0/JavaNIDE/Programming-Language/Coderive/executables/interactiveDemo.cdrv";
        final String defaultOutputFilename = "/storage/emulated/0/program.s";
        
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

        // STAGE 3: BYTECODE COMPILATION
        DebugSystem.startTimer("bytecode_compilation");
        BytecodeCompiler compiler = new BytecodeCompiler();
        BytecodeProgram bytecode = compiler.compile(ast);
        DebugSystem.stopTimer("bytecode_compilation");

        if (mode == CompilationMode.BYTECODE_ONLY || mode == CompilationMode.BOTH) {
            bytecode.disassemble();
        }
        
        if (mode == CompilationMode.BYTECODE_ONLY) {
            DebugSystem.info(LOG_TAG, "Bytecode-only compilation complete.");
            return;
        }

        // STAGE 4: NATIVE COMPILATION
        DebugSystem.startTimer("native_compilation");
        MTOTRegistry.CPUProfile cpu = MTOTRegistry.detectCPU();
        DebugSystem.info("MTOT", "Detected CPU: " + cpu.architecture);
        MTOTNativeCompiler nativeCompiler = new MTOTNativeCompiler(cpu);

        for (Map.Entry<String, List<BytecodeInstruction>> entry : bytecode.getMethods().entrySet()) {
            String methodName = entry.getKey();
            List<BytecodeInstruction> methodBytecode = entry.getValue();
            String assembly = nativeCompiler.compileMethodFromBytecode(methodName, methodBytecode);
            bytecode.addNativeMethod(methodName, assembly);
        }
        DebugSystem.stopTimer("native_compilation");

        if (mode == CompilationMode.NATIVE_ONLY || mode == CompilationMode.BOTH) {
            DebugSystem.info(LOG_TAG, "Writing native assembly to " + config.outputFilename);
            try (PrintWriter out = new PrintWriter(new FileOutputStream(config.outputFilename))) {
                for (Map.Entry<String, String> entry : bytecode.getNativeMethods().entrySet()) {
                    out.println(entry.getValue());
                }
            }
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