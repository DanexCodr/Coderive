package cod.runner;

import cod.ast.ASTFactory;
import cod.semantic.NamingValidator;
import cod.ast.nodes.*;
import cod.debug.DebugSystem;

import cod.interpreter.Interpreter;
import cod.interpreter.context.ObjectInstance;

import cod.lexer.*;
import cod.parser.MainParser;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import cod.syntax.Keyword;

public class REPLRunner {

private static final String NAME = "REPL";
    
    private static Interpreter interpreter = new Interpreter();
    private static ObjectInstance globalInstance = new ObjectInstance(ASTFactory.createType(NAME + "Global", Keyword.LOCAL, null, null));
    private static Map<String, Object> globalLocals = new HashMap<String, Object>();
    private static Map<String, Object> globalSlots = new HashMap<String, Object>();

    public static void main(String[] args) {
        out("Welcome to the Coderive " + NAME + ". Type 'exit' to quit.");
        out("Special commands: ';reset' to clear state, ';help' for help");
        
        DebugSystem.setLevel(DebugSystem.Level.ERROR);
        DebugSystem.info(NAME, "Starting Coderive " + NAME);
        
        // ✅ NO REINITIALIZATION HERE! Use the static fields that are already initialized
        
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
                // ✅ Clear maps instead of creating new ones
                globalLocals.clear();
                globalSlots.clear();
                DebugSystem.info(NAME, "State reset");
                out("State reset.");
                continue;
            }
            if (line.equalsIgnoreCase(";help")) {
                printHelp();
                continue;
            }

            try {
                // Use the SAME eval() method as the API!
                String result = eval(line);
                if (result != null && !result.isEmpty()) {
                    out(result);
                }
            } catch (Exception e) {
                outE("Error: " + e.getMessage());
                DebugSystem.error(NAME, "Error: " + e.getMessage());
            }
        }
        scanner.close();
        out("Exiting " + NAME + ".");
    }
    
    /**
     * Evaluates a single line of Coderive code and returns the result as a string.
     * This method is PURE - no Scanner, no System.exit(), no side effects.
     * Works for BOTH terminal REPL and web API!
     * 
     * @param line The Coderive code to evaluate
     * @return The result of evaluation, or error message
     */
    public static String eval(String line) {
        // Capture ALL output (System.out and System.err)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        
        // Redirect output to our capture stream
        System.setOut(ps);
        System.setErr(ps);
        
        try {
            if (line == null || line.trim().isEmpty()) {
                return "";
            }
            
            line = line.trim();
            
            // Handle special commands
            if (line.equalsIgnoreCase(";reset")) {
                globalLocals.clear();
                globalSlots.clear();
                return "State reset.";
            }
            if (line.equalsIgnoreCase(";help")) {
                return getHelpText();
            }
            if (line.equalsIgnoreCase(";exit") || line.equalsIgnoreCase(";quit")) {
                return "";  // Silent exit for API
            }
            
            // Tokenize and parse
            MainLexer lexer = new MainLexer(line);
            List<Token> tokens = lexer.tokenize();
            
            if (tokens.isEmpty()) {
                return "";
            }
            
            MainParser parser = new MainParser(tokens);
            StmtNode astNode = parser.parseSingleLine();
            
            if (astNode == null) {
                return "";
            }
            
            // Validate naming
            validateREPLNaming(astNode, globalLocals);
            
            // Evaluate
            Object result = interpreter.evalReplStatement(
                astNode,
                globalInstance,
                globalLocals,
                globalSlots
            );
            
            // Get any captured output
            String output = baos.toString();
            
            // Return result if it's an expression
            if (astNode instanceof ExprNode && result != null) {
                return output + String.valueOf(result);
            }
            
            return output;
            
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        } finally {
            // Restore original output streams
            System.setOut(oldOut);
            System.setErr(oldErr);
        }
    }
    
    /**
     * Resets the REPL state (clears variables and slots)
     */
    public static void reset() {
        globalLocals.clear();
        globalSlots.clear();
    }
    
    /**
     * Returns help text as a string
     */
    private static String getHelpText() {
        StringBuilder help = new StringBuilder();
        help.append("Coderive REPL Help:\n");
        help.append("  Expressions: 5 + 3, x * 2, \"hello\" + \" world\"\n");
        help.append("  Variables:   x = 10, name = \"Alice\"\n");
        help.append("  Output:      out(\"Hello\")\n");
        help.append("  Slots:       ~> result: 42\n");
        help.append("  Control:     if x > 5 { out(\"big\") }\n");
        help.append("  Loops:       for i in 1 to 5 { out(i) }\n");
        help.append("  Multi-line:  Use { } for blocks\n");
        help.append("  Commands:    ;help, ;reset, ;exit/;quit");
        return help.toString();
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
            
            // Handle different left-hand side expressions
            if (assign.left instanceof IdentifierNode) {
                IdentifierNode target = (IdentifierNode) assign.left;
                String varName = target.name;
                
                if (NamingValidator.isPascalCase(varName)) {
                    DebugSystem.error(NAME, "Invalid PascalCase assignment: " + varName);
                    throw new RuntimeException("Variable name '" + varName + "' cannot use PascalCase (reserved for classes)");
                }
                
                if (NamingValidator.isAllCaps(varName) && !locals.containsKey(varName)) {
                    DebugSystem.error(NAME, "Assignment to undeclared constant: " + varName);
                    throw new RuntimeException("Cannot assign to undeclared constant '" + varName + "'");
                }
                
            } else if (assign.left instanceof PropertyAccessNode) {
                PropertyAccessNode prop = (PropertyAccessNode) assign.left;
                
                // Check if it's a field assignment (this.field or super.field)
                if (prop.left instanceof ThisNode || prop.left instanceof SuperNode) {
                    if (prop.right instanceof IdentifierNode) {
                        IdentifierNode field = (IdentifierNode) prop.right;
                        String fieldName = field.name;
                        
                        if (NamingValidator.isPascalCase(fieldName)) {
                            DebugSystem.error(NAME, "Invalid PascalCase field assignment: " + fieldName);
                            throw new RuntimeException("Field name '" + fieldName + "' cannot use PascalCase (reserved for classes)");
                        }
                    }
                }
                
            } else if (assign.left instanceof IndexAccessNode) {
                // Array assignment - the array variable itself should be validated
                IndexAccessNode indexAccess = (IndexAccessNode) assign.left;
                if (indexAccess.array instanceof IdentifierNode) {
                    IdentifierNode arrayVar = (IdentifierNode) indexAccess.array;
                    String arrayName = arrayVar.name;
                    
                    if (NamingValidator.isPascalCase(arrayName)) {
                        DebugSystem.error(NAME, "Invalid PascalCase array variable: " + arrayName);
                        throw new RuntimeException("Array variable name '" + arrayName + "' cannot use PascalCase (reserved for classes)");
                    }
                }
            }
            
        } else if (stmt instanceof ForNode) {
            ForNode forNode = (ForNode) stmt;
            String iterator = forNode.iterator;
            
            // Validate iterator name
            if (NamingValidator.isPascalCase(iterator)) {
                DebugSystem.error(NAME, "Invalid PascalCase loop iterator: " + iterator);
                throw new RuntimeException("Loop iterator '" + iterator + "' cannot use PascalCase (reserved for classes)");
            }
            
            if (NamingValidator.isAllCaps(iterator)) {
                DebugSystem.error(NAME, "Loop iterator cannot be ALL_CAPS: " + iterator);
                throw new RuntimeException("Loop iterator '" + iterator + "' should not use ALL_CAPS (reserved for constants)");
            }
        }
        
        DebugSystem.debug(NAME, "Naming validation passed");
    }

    private static void printHelp() {
        out("Coderive " + NAME + " Help:");
        out("  Expressions: 5 + 3, x * 2, \"hello\" + \" world\"");
        out("  Variables:   x = 10, name = \"Alice\"");
        out("  Output:      out(\"Hello\")");
        out("  Slots:       ~> result: 42");
        out("  Control:     if x > 5 { out(\"big\") }");
        out("  Loops:       for i in 1 to 5 { out(i) }");
        out("  Multi-line:  Use { } for blocks (auto-detected)");
        out("  Commands:    ;help, ;reset, ;exit/;quit");
        out();
    }
    
    public static void out(String s) {
      System.out.println(s);
    }
    
    public static void outE(String err) {
      System.err.println(err);
    }
    
    public static void out() {
      System.out.println();
    }
    
    public static void outE() {
      System.err.println();
    }
}