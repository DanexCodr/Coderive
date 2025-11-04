package cdrv.runner;

import cdrv.ast.nodes.ProgramNode;
import cdrv.compiler.BytecodeCompiler;
import cdrv.compiler.BytecodeProgram;
import cdrv.compiler.BytecodeInstruction;
import cdrv.compiler.MTOTNativeCompiler;
import cdrv.compiler.MTOTRegistry;
import cdrv.debug.DebugSystem;

import java.io.*;
import java.util.List;
import java.util.Map;

public class CompilationEngine {
    private static final String LOG_TAG = "COMPILATION_ENGINE";
    
    private final BytecodeCompiler bytecodeCompiler;
    private MTOTNativeCompiler nativeCompiler;
    
    public CompilationEngine() {
        this.bytecodeCompiler = new BytecodeCompiler();
    }
    
    public BytecodeProgram compileToBytecode(ProgramNode ast, boolean disassemble) {
        DebugSystem.info(LOG_TAG, "Starting bytecode compilation");
        DebugSystem.startTimer("bytecode_compilation");
        
        BytecodeProgram bytecode = bytecodeCompiler.compile(ast);
        
        if (disassemble) {
            bytecode.disassemble();
        }
        
        DebugSystem.stopTimer("bytecode_compilation");
        DebugSystem.info(LOG_TAG, "Bytecode compilation completed");
        return bytecode;
    }
    
    public void compileToNative(BytecodeProgram bytecode, String outputFilename) {
        DebugSystem.info(LOG_TAG, "Starting native compilation");
        DebugSystem.startTimer("native_compilation");
        
        if (nativeCompiler == null) {
            MTOTRegistry.CPUProfile cpu = MTOTRegistry.detectCPU();
            DebugSystem.info("MTOT", "Detected CPU: " + cpu.architecture);
            nativeCompiler = new MTOTNativeCompiler(cpu);
        }
        
        for (Map.Entry<String, List<BytecodeInstruction>> entry : bytecode.getMethods().entrySet()) {
            String methodName = entry.getKey();
            List<BytecodeInstruction> methodBytecode = entry.getValue();
            String assembly = nativeCompiler.compileMethodFromBytecode(methodName, methodBytecode);
            bytecode.addNativeMethod(methodName, assembly);
        }
        
        DebugSystem.stopTimer("native_compilation");
        
        if (outputFilename != null) {
            writeNativeAssembly(bytecode, outputFilename);
        } else {
            printNativeAssembly(bytecode);
        }
        
        DebugSystem.info(LOG_TAG, "Native compilation completed");
    }
    
    public void compileFullPipeline(ProgramNode ast, String outputFilename, boolean disassemble) {
        BytecodeProgram bytecode = compileToBytecode(ast, disassemble);
        compileToNative(bytecode, outputFilename);
    }
    
    private void writeNativeAssembly(BytecodeProgram bytecode, String outputFilename) {
        DebugSystem.info(LOG_TAG, "Writing native assembly to " + outputFilename);
        try {
            PrintWriter out = new PrintWriter(new FileOutputStream(outputFilename));
            try {
                for (Map.Entry<String, String> entry : bytecode.getNativeMethods().entrySet()) {
                    out.println(entry.getValue());
                }
            } finally {
                out.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write output file: " + e.getMessage(), e);
        }
    }
    
    private void printNativeAssembly(BytecodeProgram bytecode) {
        for (Map.Entry<String, String> entry : bytecode.getNativeMethods().entrySet()) {
            System.out.println(entry.getValue());
        }
    }
}