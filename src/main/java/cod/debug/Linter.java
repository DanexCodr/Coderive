package cod.debug;

import cod.semantic.NamingValidator;
import cod.ast.nodes.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A static analyzer for the Coderive language.
 * It walks the AST to find potential bugs and style issues,
 * such as unused variables or methods.
 *
 * This class is built to be Java 7 compatible.
 */
public class Linter {

    private final List<String> warnings;
    private boolean completed;
    
    // --- State for Type-level checks ---
    private Set<String> definedMethods;
    private Set<String> calledMethods;
    private Map<String, MethodNode> methodMap; // To check visibility

    // --- State for Method-level checks ---
    private Set<String> definedVariables;
    private Set<String> usedVariables;

    public Linter() {
        this.warnings = new ArrayList<String>();
        this.completed = false;
    }

    /**
     * Lints the given AST ProgramNode and returns a list of warnings.
     * @param program The root of the AST.
     * @return A list of warning messages.
     */
    public List<String> lint(ProgramNode program) {
        warnings.clear();
        completed = false;
        
        try {
            if (program.unit != null) {
                visitUnit(program.unit);
            }
            completed = true;
        } catch (Exception e) {
            System.err.println("Linting failed with error: " + e.getMessage());
            e.printStackTrace();
            completed = false;
        }
        
        return warnings;
    }

    /**
     * Checks if the linting process completed successfully
     * @return true if linting completed, false otherwise
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * Gets the number of warnings found during linting
     * @return number of warnings
     */
    public int getWarningCount() {
        return warnings.size();
    }

    private void checkNamingConventions(TypeNode type) {
        // --- FIX: Skip PascalCase check for synthetic types ---
        if (type.name.equals("__MethodScript__") || 
            type.name.equals("__Script__") ||
            (type.name.startsWith("__") && type.name.endsWith("__"))) {
            // Skip PascalCase check for synthetic/internal types
            return;
        }
        
        // Check type name
        if (!NamingValidator.isPascalCase(type.name)) {
            addWarning(type.name, "TYPE", "Type name '" + type.name + "' should use PascalCase");
        }
        
        // Check method names
        for (MethodNode method : type.methods) {
            if (!NamingValidator.startsWithLowerCase(method.methodName)) {
                addWarning(type.name, method.methodName, "Method '" + method.methodName + "' should start with lowercase letter");
            }
        }
        
        // Check field names
        for (FieldNode field : type.fields) {
            if (NamingValidator.isPascalCase(field.name)) {
                addWarning(type.name, "FIELD", "Field '" + field.name + "' should not use PascalCase (reserved for classes)");
            }
        }
        
        // Check for ALL_CAPS fields that aren't actually constants
        for (FieldNode field : type.fields) {
            if (NamingValidator.isAllCaps(field.name) && field.value == null) {
                addWarning(type.name, "FIELD", "Field '" + field.name + "' uses ALL_CAPS but has no initial value - is this meant to be a constant?");
            }
        }
    }

    private void visitUnit(UnitNode unit) {
        for (TypeNode type : unit.types) {
            visitType(type);
        }
    }

    private void visitType(TypeNode type) {
        // Initialize sets for this type
        this.definedMethods = new HashSet<String>();
        this.calledMethods = new HashSet<String>();
        this.methodMap = new HashMap<String, MethodNode>();

        // Check naming conventions
        checkNamingConventions(type);

        // First pass: Find all defined methods in this type
        for (MethodNode method : type.methods) {
            definedMethods.add(method.methodName);
            methodMap.put(method.methodName, method);
        }
        
        // "main" is a special entry point, so it's always considered "used"
        if (definedMethods.contains("main")) {
            calledMethods.add("main");
        }

        // Second pass: Visit all methods to find calls and analyze bodies
        for (MethodNode method : type.methods) {
            visitMethod(method, type.name);
        }

        // Third pass: Analyze the results for this type
        for (String methodName : definedMethods) {
            if (!calledMethods.contains(methodName)) {
                MethodNode method = methodMap.get(methodName);
                if ("local".equals(method.visibility)) {
                    addWarning(
                        type.name, 
                        methodName, 
                        "Method '" + methodName + "' is 'local' and is never called."
                    );
                }
            }
        }
    }

    private void visitMethod(MethodNode method, String typeName) {
        // Initialize sets for this method
        this.definedVariables = new HashSet<String>();
        this.usedVariables = new HashSet<String>();

        // Add parameters to both sets (parameters are defined and "used" by the caller)
        for (ParamNode param : method.parameters) {
            definedVariables.add(param.name);
            usedVariables.add(param.name); 
        }

        // Add return slots (they are implicitly defined)
        for (SlotNode slot : method.returnSlots) {
            definedVariables.add(slot.name);
        }

        // Visit all statements in the method body
        for (StmtNode stmt : method.body) {
            visitStatement(stmt);
        }

        // Analyze the results for this method
        for (String varName : definedVariables) {
            if (!usedVariables.contains(varName)) {
                // Don't warn about unused slots, that's a different kind of check
                boolean isSlot = false;
                for (SlotNode slot : method.returnSlots) {
                    if (slot.name.equals(varName)) {
                        isSlot = true;
                        break;
                    }
                }
                
                if (!isSlot) {
                    addWarning(
                        typeName, 
                        method.methodName, 
                        "Local variable '" + varName + "' is defined but its value is never used."
                    );
                }
            }
        }
    }

    // --- Statement Visitor ---

    private void visitStatement(StmtNode stmt) {
        if (stmt == null) {
            return;
        }

        if (stmt instanceof VarNode) {
            VarNode var = (VarNode) stmt;
            definedVariables.add(var.name);
            visitExpression(var.value);

        } else if (stmt instanceof AssignmentNode) {
            AssignmentNode assign = (AssignmentNode) stmt;
            visitExpression(assign.right);
            visitExpression(assign.left);

        } else if (stmt instanceof SlotAssignmentNode) {
            SlotAssignmentNode assign = (SlotAssignmentNode) stmt;
            visitExpression(assign.value);
            if (assign.slotName != null && !"_".equals(assign.slotName)) {
                usedVariables.add(assign.slotName);
            }
            
        } else if (stmt instanceof MultipleSlotAssignmentNode) {
            MultipleSlotAssignmentNode multiAssign = (MultipleSlotAssignmentNode) stmt;
            for (SlotAssignmentNode assign : multiAssign.assignments) {
                visitStatement(assign);
            }
            
        } else if (stmt instanceof StmtIfNode) {
            StmtIfNode ifNode = (StmtIfNode) stmt;
            visitExpression(ifNode.condition);
            for (StmtNode s : ifNode.thenBlock.statements) {
                visitStatement(s);
            }
            for (StmtNode s : ifNode.elseBlock.statements) {
                visitStatement(s);
            }

        } else if (stmt instanceof ExprIfNode) {
            ExprIfNode ifExpr = (ExprIfNode) stmt;
            visitExpression(ifExpr.condition);
            visitExpression(ifExpr.thenExpr);
            visitExpression(ifExpr.elseExpr);
            
        } else if (stmt instanceof ForNode) {
            ForNode forNode = (ForNode) stmt;
            
            definedVariables.add(forNode.iterator);
            
            if (forNode.range != null) {
                visitExpression(forNode.range.start);
                visitExpression(forNode.range.end);
                if (forNode.range.step != null) {
                    visitExpression(forNode.range.step);
                }
            } else if (forNode.arraySource != null) {
                visitExpression(forNode.arraySource);
            }
            
            for (StmtNode s : forNode.body.statements) {
                visitStatement(s);
            }

        } else if (stmt instanceof ReturnSlotAssignmentNode) {
            ReturnSlotAssignmentNode assign = (ReturnSlotAssignmentNode) stmt;
            visitExpression(assign.methodCall);
            for (String varName : assign.variableNames) {
                definedVariables.add(varName);
            }
            
        } else if (stmt instanceof MethodCallNode) {
            visitExpression((ExprNode) stmt);

        } else if (stmt instanceof ExprNode) {
            visitExpression((ExprNode) stmt);
        }
    }

    // --- Expression Visitor ---

    private void visitExpression(ExprNode expr) {
        if (expr == null) {
            return;
        }

        // Handle different expression types
        if (expr instanceof IdentifierNode) {
            IdentifierNode id = (IdentifierNode) expr;
            usedVariables.add(id.name);
            
        } else if (expr instanceof BinaryOpNode) {
            BinaryOpNode binOp = (BinaryOpNode) expr;
            visitExpression(binOp.left);
            visitExpression(binOp.right);
            
        } else if (expr instanceof UnaryNode) {
            UnaryNode unary = (UnaryNode) expr;
            visitExpression(unary.operand);

        } else if (expr instanceof TypeCastNode) {
            TypeCastNode cast = (TypeCastNode) expr;
            visitExpression(cast.expression);
            
        } else if (expr instanceof ArrayNode) {
            ArrayNode array = (ArrayNode) expr;
            for (ExprNode elem : array.elements) {
                visitExpression(elem);
            }

        } else if (expr instanceof IndexAccessNode) {
            IndexAccessNode access = (IndexAccessNode) expr;
            visitExpression(access.array);
            visitExpression(access.index);

        } else if (expr instanceof RangeIndexNode) {
            RangeIndexNode range = (RangeIndexNode) expr;
            if (range.step != null) visitExpression(range.step);
            visitExpression(range.start);
            visitExpression(range.end);
            
        } else if (expr instanceof MultiRangeIndexNode) {
            MultiRangeIndexNode multiRange = (MultiRangeIndexNode) expr;
            for (RangeIndexNode range : multiRange.ranges) {
                visitExpression(range);
            }
            
        } else if (expr instanceof MethodCallNode) {
            MethodCallNode call = (MethodCallNode) expr;
            
            // This method is "called"
            // We only check for local calls, not qualified 'lib.math.sqrt'
            if (call.qualifiedName == null || !call.qualifiedName.contains(".")) {
                calledMethods.add(call.name);
            }

            // All arguments are "used"
            for (ExprNode arg : call.arguments) {
                visitExpression(arg);
            }

        } else if (expr instanceof PropertyAccessNode) {
            PropertyAccessNode prop = (PropertyAccessNode) expr;
            visitExpression(prop.left);
            visitExpression(prop.right);
            
        } else if (expr instanceof EqualityChainNode) {
            EqualityChainNode chain = (EqualityChainNode) expr;
            visitExpression(chain.left);
            for (ExprNode arg : chain.chainArguments) {
                visitExpression(arg);
            }
            
        } else if (expr instanceof BooleanChainNode) {
            BooleanChainNode chain = (BooleanChainNode) expr;
            for (ExprNode e : chain.expressions) {
                visitExpression(e);
            }
            
        } else if (expr instanceof ThisNode) {
            // 'this' is always considered used when accessed
            // No variable to track
            
        } else if (expr instanceof SuperNode) {
            // 'super' is always considered used when accessed
            // No variable to track
            
        } else if (expr instanceof IntLiteralNode ||
                   expr instanceof FloatLiteralNode ||
                   expr instanceof TextLiteralNode ||
                   expr instanceof BoolLiteralNode ||
                   expr instanceof NoneLiteralNode) {
            // Literals don't reference variables
            // Nothing to track
            
        } else if (expr instanceof LambdaNode) {
            // Lambda parameters are defined variables
            LambdaNode lambda = (LambdaNode) expr;
            for (ParamNode param : lambda.parameters) {
                definedVariables.add(param.name);
                usedVariables.add(param.name); // Parameters are used when lambda is called
            }
        }
    }

    // --- Helper ---
    
    private void addWarning(String typeName, String methodName, String message) {
        // We don't have line numbers on the nodes yet, so we'll just
        // provide the type and method context.
        warnings.add("Warning [" + typeName + "." + methodName + "]: " + message);
    }

    /**
     * Inner utility class for consistent warning output formatting
     */
    public static class WarningUtils {
        
        /**
         * Prints warnings to stderr in a consistent format
         * @param warnings List of warning messages to print
         */
        public static void printWarnings(List<String> warnings) {
            if (!warnings.isEmpty()) {
                System.err.println("---- Linter found " + warnings.size() + " issue(s) ----");
                for (String warning : warnings) {
                    System.err.println(warning);
                }
                System.err.println("----------------------------------");
            }
        }
        
        /**
         * Prints warnings with a custom header
         * @param warnings List of warning messages to print
         * @param header Custom header text
         */
        public static void printWarnings(List<String> warnings, String header) {
            if (!warnings.isEmpty()) {
                System.err.println("---- " + header + " (" + warnings.size() + " issue(s)) ----");
                for (String warning : warnings) {
                    System.err.println(warning);
                }
                System.err.println("----------------------------------");
            }
        }
    }
}