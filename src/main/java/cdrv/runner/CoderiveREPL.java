package cdrv.runner;

import cdrv.ast.ASTFactory;
import cdrv.ast.ManualCoderiveLexer;
import cdrv.ast.ManualCoderiveParser;
import cdrv.ast.nodes.*;
import cdrv.debug.DebugSystem;
import cdrv.interpreter.Interpreter;
import cdrv.interpreter.ObjectInstance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * A Read-Eval-Print-Loop (REPL) for the Coderive language.
 *
 * This class provides an interactive command-line shell that uses the
 * ManualCoderiveLexer, ManualCoderiveParser, and the Interpreter
 * to execute Coderive code one line at a time.
 */
public class CoderiveREPL {

    public static void main(String[] args) {
        System.out.println("Welcome to the Coderive REPL. Type 'exit' to quit.");
        System.out.println("Special commands: ';reset' to clear state, ';help' for help\n");
        
        // 1. Set up persistent state
        Interpreter interpreter = new Interpreter();
        ObjectInstance globalInstance = new ObjectInstance(ASTFactory.createType("REPLGlobal", "local", null));
        Map<String, Object> globalLocals = new HashMap<>();
        Map<String, Object> globalSlots = new HashMap<>();
        
        DebugSystem.setLevel(DebugSystem.Level.ERROR);

        // 2. Start the Read-Eval-Print-Loop
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print(">> ");
            System.out.flush();
            String line = scanner.nextLine().trim();

            if (line.isEmpty()) {
                continue;
            }
            
            // Handle special REPL commands
            if (line.equalsIgnoreCase(";exit") || line.equalsIgnoreCase(";quit")) {
                break;
            }
            if (line.equalsIgnoreCase(";reset")) {
                globalLocals.clear();
                globalSlots.clear();
                System.out.println("State reset.");
                continue;
            }
            if (line.equalsIgnoreCase(";help")) {
                printHelp();
                continue;
            }

            try {
                // 3. Check for multi-line input
                String fullInput = line;
                if (needsMoreInput(line)) {
                    fullInput = readMultiLineInput(scanner, line);
                    if (fullInput == null) {
                        continue; // User cancelled multi-line input
                    }
                }

                // 4. READ: Lex the input
                ManualCoderiveLexer lexer = new ManualCoderiveLexer(fullInput);
                List<ManualCoderiveLexer.Token> tokens = lexer.tokenize();

                // 5. EVAL (Part 1: Parse)
                ManualCoderiveParser parser = new ManualCoderiveParser(tokens);
                StatementNode astNode = parser.parseSingleLine(); 
                
                if (astNode == null) {
                    continue;
                }

                // 6. EVAL (Part 2: Execute)
                Object result = interpreter.getStatementEvaluator().evalStmt(
                    astNode,
                    globalInstance,
                    globalLocals,
                    globalSlots
                );

                // 7. PRINT
                if (astNode instanceof ExprNode && result != null) {
                    System.out.println(String.valueOf(result));
                }

            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }

        scanner.close();
        System.out.println("Exiting REPL.");
    }

    /**
     * Checks if the input line needs continuation (unclosed braces, etc.)
     */
    private static boolean needsMoreInput(String line) {
        // Simple check: if we have unmatched braces, we need more input
        int openBraces = countChar(line, '{');
        int closeBraces = countChar(line, '}');
        return openBraces > closeBraces;
    }

    /**
     * Reads multi-line input until all braces are balanced
     */
    private static String readMultiLineInput(Scanner scanner, String firstLine) {
        StringBuilder input = new StringBuilder(firstLine);
        int braceBalance = countChar(firstLine, '{') - countChar(firstLine, '}');
        
        System.out.print(" . . ");
        while (braceBalance > 0) {
            String line = scanner.nextLine().trim();
            input.append(" ").append(line); // Add space between lines
            
            braceBalance += countChar(line, '{') - countChar(line, '}');
            
            if (braceBalance <= 0) {
                break;
            }
            System.out.print(" . . ");
        }
        
        return input.toString();
    }

    /**
     * Counts occurrences of a character in a string
     */
    private static int countChar(String str, char ch) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) {
                count++;
            }
        }
        return count;
    }

    /**
     * Prints REPL help information
     */
    private static void printHelp() {
        System.out.println("Coderive REPL Help:");
        System.out.println("  Expressions: 5 + 3, x * 2, \"hello\" + \" world\"");
        System.out.println("  Variables:   x = 10, name = \"Alice\"");
        System.out.println("  Output:      output \"Hello\"");
        System.out.println("  Slots:       ~ result 42");
        System.out.println("  Control:     if x > 5 { output \"big\" }");
        System.out.println("  Loops:       for i in 1 to 5 { output i }");
        System.out.println("  Multi-line:  Use { } for blocks (auto-detected)");
        System.out.println("  Commands:    ;help, ;reset, ;exit/;quit");
        System.out.println();
    }
}