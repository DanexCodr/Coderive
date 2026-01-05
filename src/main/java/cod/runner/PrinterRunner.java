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
    
    @Override
    public void run(String[] args) throws Exception {
        DebugSystem.info(LOG_TAG, "Starting PrinterRunner");
        
        String inputFilename = getInputFilename(args);
        
        for (String arg : args) {
            if ("--help".equals(arg) || "-h".equals(arg)) {
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
        
        ProgramNode ast = parse(config.inputFilename);
        if (ast == null) {
            DebugSystem.error(LOG_TAG, "Parsing failed, AST is null");
            throw new RuntimeException("Parsing failed, AST is null.");
        }
        DebugSystem.info(LOG_TAG, "AST parsed successfully");
        
        DebugSystem.info(LOG_TAG, "Printing Abstract Syntax Tree:");
        
        printOriginalAST(ast);
        
        DebugSystem.info(LOG_TAG, "AST printing completed");
    }
    
    private void printOriginalAST(ProgramNode ast) {
        System.out.println("\n=== ABSTRACT SYNTAX TREE ===");
        System.out.println("This is the AST as parsed from source code.");
        System.out.println("========================================\n");
        DebugSystem.debug(LOG_TAG, "Printing AST");
        ASTPrinter.print(ast);
    }
    
    private String getInputFilename(String[] args) {
        for (String arg : args) {
            if (!arg.startsWith("-") && !arg.equals("--help") && !arg.equals("-h")) {
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
        System.out.println("  -h, --help         Show this help message");
        System.out.println("\nExamples:");
        System.out.println("  PrinterRunner program.cod               # Show AST");
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