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

private static final String NAME = "REPL";

    public static void main(String[] args) {
        System.out.println("Welcome to the Coderive " + NAME + ". Type 'exit' to quit.");
        System.out.println("Special commands: ';reset' to clear state, ';help' for help");
        
        DebugSystem.setLevel(DebugSystem.Level.ERROR);
        DebugSystem.info(NAME, "Starting Coderive " + NAME);
        
        Interpreter interpreter = new Interpreter();
        // UPDATED: Pass null as the token parameter to createType
        ObjectInstance globalInstance = new ObjectInstance(ASTFactory.createType(NAME + "Global", Keyword.LOCAL, null, null));
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
                DebugSystem.info(NAME, "Exiting" + NAME);
                break;
            }
            if (line.equalsIgnoreCase(";reset")) {
                globalLocals.clear();
                globalSlots.clear();
                DebugSystem.info(NAME, "State reset");
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
                    DebugSystem.debug(NAME, "Detected multi-line input");
                    fullInput = readMultiLineInput(scanner, line);
                    if (fullInput == null) {
                        continue;
                    }
                }

                MainLexer lexer = new MainLexer(fullInput);
                List<Token> tokens = lexer.tokenize();
                DebugSystem.debug(NAME, "Tokenized: " + tokens.size() + " tokens");

                MainParser parser = new MainParser(tokens);
                StmtNode astNode = parser.parseSingleLine(); 
                
                if (astNode == null) {
                    DebugSystem.warn(NAME, "No AST generated for input");
                    continue;
                }
                
                DebugSystem.debug(NAME, "Parsed AST node: " + astNode.getClass().getSimpleName());
                
                validateREPLNaming(astNode, globalLocals);

                DebugSystem.debug(NAME, "Evaluating statement");
                Object result = interpreter.evalReplStatement(
                    astNode,
                    globalInstance,
                    globalLocals,
                    globalSlots
                );

                if (astNode instanceof ExprNode && result != null) {
                    System.out.println(String.valueOf(result));
                    DebugSystem.debug(NAME, "Result: " + result);
                }

            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                DebugSystem.error(NAME, "Error: " + e.getMessage());
            }
        }
        scanner.close();
        System.out.println("Exiting " + NAME + ".");
    }
    
    private static void validateREPLNaming(StmtNode stmt, Map<String, Object> locals) {
        DebugSystem.debug(NAME, "Validating naming for: " + stmt.getClass().getSimpleName());
        
        if (stmt instanceof VarNode) {
            VarNode var = (VarNode) stmt;
            String varName = var.name;
            
            if (NamingValidator.isPascalCase(varName)) {
                DebugSystem.error(NAME, "Invalid PascalCase variable: " + varName);
                throw new RuntimeException("Variable name '" + varName + "' cannot use PascalCase (reserved for classes)");
            }
            
            if (NamingValidator.isAllCaps(varName) && var.value == null) {
                DebugSystem.error(NAME, "Constant without value: " + varName);
                throw new RuntimeException("Constant '" + varName + "' must have an initial value");
            }
            
        } else if (stmt instanceof AssignmentNode) {
            AssignmentNode assign = (AssignmentNode) stmt;
            if (assign.left instanceof ExprNode) {
                ExprNode target = (ExprNode) assign.left;
                if (target.name != null) {
                    String varName = target.name;
                    if (NamingValidator.isPascalCase(varName)) {
                        DebugSystem.error(NAME, "Invalid PascalCase assignment: " + varName);
                        throw new RuntimeException("Variable name '" + varName + "' cannot use PascalCase (reserved for classes)");
                    }
                    
                    if (NamingValidator.isAllCaps(varName) && !locals.containsKey(varName)) {
                        DebugSystem.error(NAME, "Assignment to undeclared constant: " + varName);
                        throw new RuntimeException("Cannot assign to undeclared constant '" + varName + "'");
                    }
                }
            }
        }
        
        DebugSystem.debug(NAME, "Naming validation passed");
    }

    private static boolean needsMoreInput(String line) {
        int openBraces = countChar(line, '{');
        int closeBraces = countChar(line, '}');
        return openBraces > closeBraces;
    }

    private static String readMultiLineInput(Scanner scanner, String firstLine) {
        DebugSystem.debug(NAME, "Starting multi-line input collection");
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
        
        DebugSystem.debug(NAME, "Multi-line input complete, length: " + input.length());
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
        System.out.println("Coderive" + NAME + " Help:");
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