// BaseRunner.java
package cod.runner;

import cod.ast.nodes.*;
import cod.debug.DebugSystem;
import java.io.*;
import java.util.List;

import cod.lexer.MainLexer;
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
        List<MainLexer.Token> tokens = lexer.tokenize();
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
    DebugSystem.enableCategory("PERF");
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

    // ==================== CONSTANT FOLDING INTEGRATION ====================
    
    protected <T extends ASTNode> T optimizeAST(T ast, boolean enableOptimization) {
        if (!enableOptimization) {
            DebugSystem.debug(LOG_TAG, "Skipping constant folding (disabled)");
            return ast;
        }
        
        DebugSystem.debug(LOG_TAG, "Starting constant folding optimization...");
        DebugSystem.startTimer("constant_folding");
        
        try {
            Class<?> optimizerClass = Class.forName("cod.ast.Optimizer");
            java.lang.reflect.Method foldMethod = optimizerClass.getMethod("foldConstants", ASTNode.class);
            
            @SuppressWarnings("unchecked")
            T optimized = (T) foldMethod.invoke(null, ast);
            
            DebugSystem.stopTimer("constant_folding");
            long duration = DebugSystem.getTimerDuration("constant_folding");
            
            DebugSystem.info(LOG_TAG, "Constant folding completed in " + duration + " ms");
            
            if (DebugSystem.getLevel().compareTo(DebugSystem.Level.DEBUG) >= 0) {
                printOptimizationDebugInfo(ast, optimized);
            }
            
            return optimized;
            
        } catch (ClassNotFoundException e) {
            DebugSystem.warn(LOG_TAG, "Optimizer class not found. Constant folding disabled.");
            DebugSystem.warn(LOG_TAG, "Make sure Optimizer.java is in the classpath.");
            return ast;
        } catch (NoSuchMethodException e) {
            DebugSystem.warn(LOG_TAG, "Optimizer.foldConstants method not found.");
            return ast;
        } catch (Exception e) {
            DebugSystem.error(LOG_TAG, "Constant folding failed: " + e.getMessage());
            if (DebugSystem.getLevel().compareTo(DebugSystem.Level.DEBUG) >= 0) {
                e.printStackTrace();
            }
            return ast;
        }
    }
    
    protected <T extends ASTNode> T optimizeAST(T ast, boolean constantFolding, boolean otherOptimizations) {
        if (!constantFolding && !otherOptimizations) {
            return ast;
        }
        
        DebugSystem.debug(LOG_TAG, "Starting optimizations...");
        DebugSystem.startTimer("optimizations");
        
        T result = ast;
        
        if (constantFolding) {
            result = optimizeAST(result, true);
        }
        
        if (otherOptimizations) {
            DebugSystem.debug(LOG_TAG, "Other optimizations requested but not implemented yet");
        }
        
        DebugSystem.stopTimer("optimizations");
        DebugSystem.info(LOG_TAG, "All optimizations completed in " + 
            DebugSystem.getTimerDuration("optimizations") + " ms");
        
        return result;
    }
    
    private void printOptimizationDebugInfo(ASTNode original, ASTNode optimized) {
        if (original == optimized) {
            DebugSystem.debug(LOG_TAG, "No changes made by constant folding");
            return;
        }
        
        DebugSystem.debug(LOG_TAG, "AST modified by constant folding");
        
        int originalEstimate = estimateNodeCount(original);
        int optimizedEstimate = estimateNodeCount(optimized);
        
        if (optimizedEstimate < originalEstimate) {
            int reduction = ((originalEstimate - optimizedEstimate) * 100) / originalEstimate;
            DebugSystem.debug(LOG_TAG, "Estimated size reduction: " + reduction + "%");
            DebugSystem.debug(LOG_TAG, "Original: ~" + originalEstimate + " nodes");
            DebugSystem.debug(LOG_TAG, "Optimized: ~" + optimizedEstimate + " nodes");
        }
        
        try {
            Class<?> optimizerClass = Class.forName("cod.ast.Optimizer");
            java.lang.reflect.Method getStatsMethod = optimizerClass.getMethod("getStats");
            Object stats = getStatsMethod.invoke(null);
            
            if (stats != null) {
                Class<?> statsClass = stats.getClass();
                java.lang.reflect.Method getTotalMethod = statsClass.getMethod("getTotalOptimizations");
                Integer totalOpts = (Integer) getTotalMethod.invoke(stats);
                
                if (totalOpts != null && totalOpts > 0) {
                    DebugSystem.debug(LOG_TAG, "Total optimizations applied: " + totalOpts);
                }
            }
        } catch (Exception e) {
            // Ignore
        }
    }
    
    private int estimateNodeCount(ASTNode node) {
        if (node == null) return 0;
        
        int count = 1;
        
        if (node instanceof ProgramNode) {
            ProgramNode prog = (ProgramNode) node;
            if (prog.unit != null) count += estimateNodeCount(prog.unit);
        } else if (node instanceof UnitNode) {
            UnitNode unit = (UnitNode) node;
            if (unit.imports != null) count += estimateNodeCount(unit.imports);
            for (TypeNode type : unit.types) {
                count += estimateNodeCount(type);
            }
        } else if (node instanceof TypeNode) {
            TypeNode type = (TypeNode) node;
            for (FieldNode field : type.fields) {
                count += estimateNodeCount(field);
            }
            for (MethodNode method : type.methods) {
                count += estimateNodeCount(method);
            }
            for (StmtNode stmt : type.statements) {
                count += estimateNodeCount(stmt);
            }
        } else if (node instanceof MethodNode) {
            MethodNode method = (MethodNode) node;
            for (ParamNode param : method.parameters) {
                count += estimateNodeCount(param);
            }
            for (StmtNode stmt : method.body) {
                count += estimateNodeCount(stmt);
            }
        } else if (node instanceof BlockNode) {
            BlockNode block = (BlockNode) node;
            for (StmtNode stmt : block.statements) {
                count += estimateNodeCount(stmt);
            }
        } else if (node instanceof StmtIfNode) {
            StmtIfNode ifNode = (StmtIfNode) node;
            if (ifNode.condition != null) count += estimateNodeCount(ifNode.condition);
            count += estimateNodeCount(ifNode.thenBlock);
            count += estimateNodeCount(ifNode.elseBlock);
        } else if (node instanceof BinaryOpNode) {
            BinaryOpNode binOp = (BinaryOpNode) node;
            if (binOp.left != null) count += estimateNodeCount(binOp.left);
            if (binOp.right != null) count += estimateNodeCount(binOp.right);
        } else if (node instanceof UnaryNode) {
            UnaryNode unary = (UnaryNode) node;
            if (unary.operand != null) count += estimateNodeCount(unary.operand);
        } else if (node instanceof MethodCallNode) {
            MethodCallNode call = (MethodCallNode) node;
            for (ExprNode arg : call.arguments) {
                count += estimateNodeCount(arg);
            }
            if (call.chainArguments != null) {
                for (ExprNode arg : call.chainArguments) {
                    count += estimateNodeCount(arg);
                }
            }
        } else if (node instanceof EqualityChainNode) {
            EqualityChainNode chain = (EqualityChainNode) node;
            if (chain.left != null) count += estimateNodeCount(chain.left);
            for (ExprNode arg : chain.chainArguments) {
                count += estimateNodeCount(arg);
            }
        } else if (node instanceof BooleanChainNode) {
            BooleanChainNode boolChain = (BooleanChainNode) node;
            for (ExprNode expr : boolChain.expressions) {
                count += estimateNodeCount(expr);
            }
        }
        
        return count;
    }
    
    protected boolean isConstantFoldingAvailable() {
        try {
            Class.forName("cod.ast.Optimizer");
            DebugSystem.debug(LOG_TAG, "Constant folding available");
            return true;
        } catch (ClassNotFoundException e) {
            DebugSystem.debug(LOG_TAG, "Constant folding NOT available");
            return false;
        }
    }
    
    protected String getOptimizationInfo() {
        StringBuilder info = new StringBuilder();
        
        if (isConstantFoldingAvailable()) {
            info.append("Available optimizations:\n");
            info.append("  • Constant folding (-O, --optimize)\n");
            info.append("    - Evaluates constant expressions at compile time\n");
            info.append("    - Optimizes boolean chains (any[]/all[])\n");
            info.append("    - Eliminates unnecessary type casts\n");
            info.append("    - Pre-computes string concatenation\n");
        } else {
            info.append("Optimizations: NOT AVAILABLE\n");
            info.append("Make sure Optimizer.java and ConstantFolder.java are compiled.\n");
        }
        
        return info.toString();
    }

    public abstract void run(String[] args) throws Exception;
}