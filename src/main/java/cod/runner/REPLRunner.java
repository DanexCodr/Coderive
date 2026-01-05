// REPLRunner.java
package cod.runner;

import cod.ast.ASTFactory;
import cod.semantic.NamingValidator;
import cod.ast.nodes.*;
import cod.debug.DebugSystem;

import cod.interpreter.Interpreter;
import cod.interpreter.context.ObjectInstance;

import cod.lexer.*;
import cod.parser.MainParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import cod.syntax.Keyword;

public class REPLRunner {

    public static void main(String[] args) {
        System.out.println("Welcome to the Coderive REPL. Type 'exit' to quit.");
        System.out.println("Special commands: ';reset' to clear state, ';help' for help");
        
        DebugSystem.setLevel(DebugSystem.Level.ERROR);
        DebugSystem.info("REPL", "Starting Coderive REPL");
        
        Interpreter interpreter = new Interpreter();
        ObjectInstance globalInstance = new ObjectInstance(ASTFactory.createType("REPLGlobal", Keyword.LOCAL, null));
        Map<String, Object> globalLocals = new HashMap<String, Object>();
        Map<String, Object> globalSlots = new HashMap<String, Object>();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print(">> ");
            System.out.flush();
            String line = scanner.nextLine().trim();

            if (line.isEmpty()) {
                continue;
            }
            
            if (line.equalsIgnoreCase(";exit") || line.equalsIgnoreCase(";quit")) {
                DebugSystem.info("REPL", "Exiting REPL");
                break;
            }
            if (line.equalsIgnoreCase(";reset")) {
                globalLocals.clear();
                globalSlots.clear();
                DebugSystem.info("REPL", "State reset");
                System.out.println("State reset.");
                continue;
            }
            if (line.equalsIgnoreCase(";help")) {
                printHelp();
                continue;
            }

            try {
                String fullInput = line;
                if (needsMoreInput(line)) {
                    DebugSystem.debug("REPL", "Detected multi-line input");
                    fullInput = readMultiLineInput(scanner, line);
                    if (fullInput == null) {
                        continue;
                    }
                }

                MainLexer lexer = new MainLexer(fullInput);
                List<Token> tokens = lexer.tokenize();
                DebugSystem.debug("REPL", "Tokenized: " + tokens.size() + " tokens");

                MainParser parser = new MainParser(tokens);
                StmtNode astNode = parser.parseSingleLine(); 
                
                if (astNode == null) {
                    DebugSystem.warn("REPL", "No AST generated for input");
                    continue;
                }
                
                DebugSystem.debug("REPL", "Parsed AST node: " + astNode.getClass().getSimpleName());
                
                validateREPLNaming(astNode, globalLocals);

                DebugSystem.debug("REPL", "Evaluating statement");
                Object result = interpreter.evalReplStatement(
                    astNode,
                    globalInstance,
                    globalLocals,
                    globalSlots
                );

                if (astNode instanceof ExprNode && result != null) {
                    System.out.println(String.valueOf(result));
                    DebugSystem.debug("REPL", "Result: " + result);
                }

            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                DebugSystem.error("REPL", "Error: " + e.getMessage());
            }
        }
        scanner.close();
        System.out.println("Exiting REPL.");
    }
    
    private static void validateREPLNaming(StmtNode stmt, Map<String, Object> locals) {
        DebugSystem.debug("REPL", "Validating naming for: " + stmt.getClass().getSimpleName());
        
        if (stmt instanceof VarNode) {
            VarNode var = (VarNode) stmt;
            String varName = var.name;
            
            if (NamingValidator.isPascalCase(varName)) {
                DebugSystem.error("REPL", "Invalid PascalCase variable: " + varName);
                throw new RuntimeException("Variable name '" + varName + "' cannot use PascalCase (reserved for classes)");
            }
            
            if (NamingValidator.isAllCaps(varName) && var.value == null) {
                DebugSystem.error("REPL", "Constant without value: " + varName);
                throw new RuntimeException("Constant '" + varName + "' must have an initial value");
            }
            
        } else if (stmt instanceof AssignmentNode) {
            AssignmentNode assign = (AssignmentNode) stmt;
            if (assign.left instanceof ExprNode) {
                ExprNode target = (ExprNode) assign.left;
                if (target.name != null) {
                    String varName = target.name;
                    if (NamingValidator.isPascalCase(varName)) {
                        DebugSystem.error("REPL", "Invalid PascalCase assignment: " + varName);
                        throw new RuntimeException("Variable name '" + varName + "' cannot use PascalCase (reserved for classes)");
                    }
                    
                    if (NamingValidator.isAllCaps(varName) && !locals.containsKey(varName)) {
                        DebugSystem.error("REPL", "Assignment to undeclared constant: " + varName);
                        throw new RuntimeException("Cannot assign to undeclared constant '" + varName + "'");
                    }
                }
            }
        }
        
        DebugSystem.debug("REPL", "Naming validation passed");
    }

    private static boolean needsMoreInput(String line) {
        int openBraces = countChar(line, '{');
        int closeBraces = countChar(line, '}');
        return openBraces > closeBraces;
    }

    private static String readMultiLineInput(Scanner scanner, String firstLine) {
        DebugSystem.debug("REPL", "Starting multi-line input collection");
        StringBuilder input = new StringBuilder(firstLine);
        int braceBalance = countChar(firstLine, '{') - countChar(firstLine, '}');
        
        System.out.print(" . . ");
        while (braceBalance > 0) {
            String line = scanner.nextLine().trim();
            input.append(" ").append(line);
            
            braceBalance += countChar(line, '{') - countChar(line, '}');
            
            if (braceBalance <= 0) {
                break;
            }
            System.out.print(" . . ");
        }
        
        DebugSystem.debug("REPL", "Multi-line input complete, length: " + input.length());
        return input.toString();
    }

    private static int countChar(String str, char ch) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) {
                count++;
            }
        }
        return count;
    }

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