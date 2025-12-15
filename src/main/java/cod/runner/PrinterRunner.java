// PrinterRunner.java
package cod.runner;

import cod.runner.BaseRunner;
import cod.ast.nodes.*;
import cod.debug.DebugSystem;
import cod.ast.ASTPrinter;
import java.util.Scanner;

public class PrinterRunner extends BaseRunner {

    private final String androidPath = "/storage/emulated/0";
    private final String definedFilePath = "/JavaNIDE/Programming-Language/Coderive/executables/InteractiveDemo.cod";
    
    private boolean showOptimized = false;
    private boolean showBoth = false;
    private boolean showDiff = false;
    
    @Override
    public void run(String[] args) throws Exception {
        DebugSystem.info(LOG_TAG, "Starting PrinterRunner");
        
        String inputFilename = getInputFilename(args);
        
        for (String arg : args) {
            if ("--optimized".equals(arg) || "-O".equals(arg)) {
                showOptimized = true;
                DebugSystem.debug(LOG_TAG, "Show optimized mode");
            } else if ("--both".equals(arg) || "-B".equals(arg)) {
                showBoth = true;
                DebugSystem.debug(LOG_TAG, "Show both mode");
            } else if ("--diff".equals(arg) || "-D".equals(arg)) {
                showDiff = true;
                showBoth = true;
                DebugSystem.debug(LOG_TAG, "Show diff mode");
            } else if ("--help".equals(arg) || "-h".equals(arg)) {
                printHelp();
                return;
            }
        }
        
        RunnerConfig config = processCommandLineArgs(args, inputFilename, new Configuration() {
            @Override
            public void configure(RunnerConfig config) {
                config.withDebugLevel(DebugSystem.Level.DEBUG);
            }
        });
        
        configureDebugSystem(config.debugLevel);
        
        DebugSystem.info(LOG_TAG, "Starting AST Printer execution...");
        DebugSystem.info(LOG_TAG, "Input file: " + config.inputFilename);
        
        if (showOptimized) {
            DebugSystem.info(LOG_TAG, "Mode: Showing OPTIMIZED AST");
        } else if (showBoth) {
            DebugSystem.info(LOG_TAG, "Mode: Showing BOTH original and optimized AST");
            if (showDiff) {
                DebugSystem.info(LOG_TAG, "Mode: Also showing DIFFERENCES");
            }
        } else {
            DebugSystem.info(LOG_TAG, "Mode: Showing ORIGINAL AST");
        }
        
        ProgramNode ast = parse(config.inputFilename);
        if (ast == null) {
            DebugSystem.error(LOG_TAG, "Parsing failed, AST is null");
            throw new RuntimeException("Parsing failed, AST is null.");
        }
        DebugSystem.info(LOG_TAG, "AST parsed successfully");
        
        DebugSystem.info(LOG_TAG, "Printing Abstract Syntax Tree:");
        
        if (showBoth) {
            printBothASTs(ast, showDiff);
        } else if (showOptimized) {
            printOptimizedAST(ast);
        } else {
            printOriginalAST(ast);
        }
        
        DebugSystem.info(LOG_TAG, "AST printing completed");
    }
    
    private void printOriginalAST(ProgramNode ast) {
        System.out.println("\n=== ORIGINAL AST (No Optimizations) ===");
        System.out.println("This is the AST as parsed from source code.");
        System.out.println("Constant expressions are not evaluated.");
        System.out.println("========================================\n");
        DebugSystem.debug(LOG_TAG, "Printing original AST");
        ASTPrinter.print(ast);
    }
    
    private void printOptimizedAST(ProgramNode ast) {
        System.out.println("\n=== OPTIMIZED AST (With Constant Folding) ===");
        System.out.println("Constant folding has been applied:");
        System.out.println("• Arithmetic expressions evaluated");
        System.out.println("• Boolean chains optimized");
        System.out.println("• Type casts eliminated where possible");
        System.out.println("=============================================\n");
        
        DebugSystem.startTimer("optimization");
        DebugSystem.debug(LOG_TAG, "Optimizing AST");
        ProgramNode optimized = optimizeAST(ast, true);
        DebugSystem.stopTimer("optimization");
        
        DebugSystem.debug(LOG_TAG, "Printing optimized AST");
        ASTPrinter.print(optimized);
        
        System.out.println("\n[Optimization completed in " + 
            DebugSystem.getTimerDuration("optimization") + " ms]");
    }

// Alternative using char array (slightly more efficient for single characters)
private void printBothASTs(ProgramNode originalAst, boolean showDiff) {
    System.out.println("\n=== COMPARISON: Original vs Optimized AST ===");
    System.out.println("Shows the effect of constant folding optimization.");
    System.out.println("================================================\n");
    
    System.out.println("=== ORIGINAL AST ===");
    System.out.println("(As parsed from source)\n");
    DebugSystem.debug(LOG_TAG, "Printing original AST for comparison");
    ASTPrinter.print(originalAst);
    
    System.out.println();
    for (int i = 0; i < 60; i++) System.out.print("=");
    System.out.println("\n");
    
    System.out.println("=== OPTIMIZED AST ===");
    System.out.println("(After constant folding)\n");
    
    DebugSystem.startTimer("optimization");
    DebugSystem.debug(LOG_TAG, "Optimizing AST for comparison");
    ProgramNode optimizedAst = optimizeAST(originalAst, true);
    DebugSystem.stopTimer("optimization");
    
    ASTPrinter.print(optimizedAst);
    
    System.out.println("\n[Optimization time: " + 
        DebugSystem.getTimerDuration("optimization") + " ms]");
    
    if (showDiff) {
        System.out.println();
        for (int i = 0; i < 60; i++) System.out.print("=");
        System.out.println("\n");
        System.out.println("=== KEY DIFFERENCES ===");
        DebugSystem.debug(LOG_TAG, "Printing AST differences");
        printASTDifferences(originalAst, optimizedAst);
    }
    
    System.out.println("\n=== END OF COMPARISON ===");
}
    
    private void printASTDifferences(ProgramNode original, ProgramNode optimized) {
        System.out.println("\nSummary of changes made by constant folding:");
        System.out.println("(This is a conceptual summary - actual AST nodes differ)");
        System.out.println();
        
        System.out.println("Examples of what constant folding does:");
        System.out.println("  • 2 + 3 * 4  →  14");
        System.out.println("  • any[true, false, true]  →  true");
        System.out.println("  • (int)3.14  →  3");
        System.out.println("  • \"Hello \" + \"World\"  →  \"Hello World\"");
        System.out.println("  • x == any[1, 2, 3] (if x is constant) → true/false");
        System.out.println();
        
        System.out.println("Benefits:");
        System.out.println("  • Runtime computation eliminated");
        System.out.println("  • Smaller AST (fewer nodes)");
        System.out.println("  • Better performance during interpretation/compilation");
        
        System.out.println("\n[Note: Detailed node-by-node comparison not implemented]");
        DebugSystem.debug(LOG_TAG, "Printed AST differences (conceptual)");
    }
    
    private String getInputFilename(String[] args) {
        for (String arg : args) {
            if (!arg.startsWith("-") && 
                !arg.equals("--optimized") && 
                !arg.equals("--both") && 
                !arg.equals("--diff") && 
                !arg.equals("--help") &&
                !arg.equals("-O") && 
                !arg.equals("-B") && 
                !arg.equals("-D") && 
                !arg.equals("-h")) {
                DebugSystem.debug(LOG_TAG, "Found input file in args: " + arg);
                return arg;
            }
        }
        
        Scanner scanner = new Scanner(System.in);
        String defaultFilename = androidPath + definedFilePath;
        
        System.out.println("Enter file path or press Enter for default [" + defaultFilename + "]:");
        System.out.print("> ");
        
        String userInput = scanner.nextLine().trim();
        scanner.close();
        
        if (userInput.isEmpty()) {
            System.out.println("Using default file: " + defaultFilename);
            DebugSystem.info(LOG_TAG, "Using default file: " + defaultFilename);
            return defaultFilename;
        } else {
            System.out.println("Using user provided file: " + userInput);
            DebugSystem.info(LOG_TAG, "Using user file: " + userInput);
            return userInput;
        }
    }
    
    private void printHelp() {
        System.out.println("AST Printer - Display Abstract Syntax Trees");
        System.out.println("Usage: PrinterRunner [filename] [options]\n");
        System.out.println("Options:");
        System.out.println("  -O, --optimized    Show AST after constant folding optimization");
        System.out.println("  -B, --both         Show both original and optimized AST");
        System.out.println("  -D, --diff         Show differences between original and optimized");
        System.out.println("  -h, --help         Show this help message");
        System.out.println("\nExamples:");
        System.out.println("  PrinterRunner program.cod               # Show original AST");
        System.out.println("  PrinterRunner program.cod -O            # Show optimized AST");
        System.out.println("  PrinterRunner program.cod -B            # Show both");
        System.out.println("  PrinterRunner program.cod -B -D         # Show both with diff");
        System.out.println("\nNote: Default file is used if no filename provided.");
        DebugSystem.info(LOG_TAG, "Printed help message");
    }

    public static void main(String[] args) {
        try {
            DebugSystem.setLevel(DebugSystem.Level.INFO);
            DebugSystem.info("PRINTER_RUNNER", "Starting PrinterRunner");
            PrinterRunner runner = new PrinterRunner();
            runner.run(args);
        } catch (Exception e) {
            System.err.println("AST Printer Error: " + e.getMessage());
            DebugSystem.error("PRINTER_RUNNER", "Error: " + e.getMessage());
            System.err.println("\nUse --help for usage information.");
            
            if (DebugSystem.getLevel().compareTo(DebugSystem.Level.DEBUG) >= 0) {
                e.printStackTrace();
            }
        }
    }
}