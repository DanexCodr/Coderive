package cod.interpreter;

import cod.ast.nodes.*;
import cod.ast.ImportResolver;
import cod.debug.DebugSystem;
import java.util.*;
import java.io.File;

public class Interpreter {

    Map<String, Object> currentSlots =
            null; // CHANGED: package-private for ExpressionEvaluator access
    Set<String> slotsInCurrentPath =
            new HashSet<>(); // CHANGED: package-private for StatementEvaluator access // Track
                             // slots in current control flow path
    private IOHandler ioHandler =
            new IOHandler(); // REPLACED: Use IOHandler instead of direct Scanner
    private ImportResolver importResolver = new ImportResolver(); // ADD IMPORT RESOLVER
    private TypeSystem typeSystem = new TypeSystem(); // ADD TYPE SYSTEM
    private ExpressionEvaluator exprEvaluator; // ADD EXPRESSION EVALUATOR
    private StatementEvaluator stmtEvaluator; // ADD STATEMENT EVALUATOR

    public Interpreter() {
        this.exprEvaluator =
                new ExpressionEvaluator(typeSystem, this); // INITIALIZE EXPRESSION EVALUATOR
        this.stmtEvaluator =
                new StatementEvaluator(
                        this,
                        exprEvaluator,
                        ioHandler,
                        typeSystem); // INITIALIZE STATEMENT EVALUATOR
    }

    // Java 7 compatible empty map helper
    private Map<String, Object> emptyParamMap() {
        return Collections.<String, Object>emptyMap();
    }

    public ImportResolver getImportResolver() {
        return importResolver;
    }

    public ExpressionEvaluator getExpressionEvaluator() {
        return exprEvaluator;
    }
    
    public StatementEvaluator getStatementEvaluator() {
        return stmtEvaluator;
    }

    public void run(ProgramNode program) {
        DebugSystem.startTimer("program_execution");
        DebugSystem.info("INTERPRETER", "Starting program execution");

        UnitNode unit = program.unit;
        DebugSystem.debug("INTERPRETER", "Processing unit: " + unit.name);
        DebugSystem.debug("INTERPRETER", "Unit imports: " + unit.imports);

        // FIX: Initialize import resolver BEFORE processing types
        initializeImportResolver(unit);

        for (TypeNode type : unit.types) {
            DebugSystem.debug("INTERPRETER", "Processing type: " + type.name);

            for (MethodNode method : type.methods) {
                // Check for valid main() method: name = "main" AND no parameters
                boolean isMainMethod = "main".equals(method.name);
                boolean hasNoParameters = method.parameters.isEmpty();

                if (isMainMethod && hasNoParameters) {
                    DebugSystem.methodEntry("main", emptyParamMap());

                    ObjectInstance obj = new ObjectInstance(type);
                    if (type.constructor != null) {
                        DebugSystem.debug("INTERPRETER", "Executing constructor for " + type.name);
                        evalConstructor(type.constructor, obj);
                    }

                    Object result = evalMethod(method, obj, new HashMap<String, Object>());
                    DebugSystem.methodExit("main", result);

                } else if (isMainMethod && !hasNoParameters) {
                    DebugSystem.warn(
                            "INTERPRETER", "Ignoring main() with parameters: " + method.parameters);
                }
            }
        }

        DebugSystem.stopTimer("program_execution");
        DebugSystem.info("INTERPRETER", "Program execution completed");

        ioHandler.close(); // CLOSE IO HANDLER WHEN DONE
    }

    private void initializeImportResolver(UnitNode unit) {
    DebugSystem.debug("IMPORTS", "=== INITIALIZE IMPORT RESOLVER START ===");
    DebugSystem.debug("IMPORTS", "Initializing import resolver for unit: " + unit.name);

    // UPDATED: Only pre-load imports that were already resolved during AST building
    // Don't try to resolve new imports here - do it lazily when needed
    if (unit.resolvedImports != null && !unit.resolvedImports.isEmpty()) {
        DebugSystem.debug(
                "IMPORTS",
                "Pre-loading " + unit.resolvedImports.size() + " resolved imports from AST");
        for (Map.Entry<String, ProgramNode> entry : unit.resolvedImports.entrySet()) {
            String importName = entry.getKey();
            ProgramNode importedProgram = entry.getValue();
            importResolver.preloadImport(importName, importedProgram);
            DebugSystem.debug("IMPORTS", "Pre-loaded import: " + importName);
        }
    }

    // UPDATED: Just register the import names for later resolution
    if (unit.imports != null && !unit.imports.imports.isEmpty()) {
        DebugSystem.debug("IMPORTS", "Registered " + unit.imports.imports.size() + " imports for lazy resolution");
        for (String importName : unit.imports.imports) {
            importResolver.registerImport(importName);
            DebugSystem.debug("IMPORTS", "Registered import: " + importName);
        }
    }

    DebugSystem.debug("IMPORTS", "=== INITIALIZE IMPORT RESOLVER END ===");
}

    // Add this temporary method to InterpreterRunner.java before interpreter.run(ast)
    private static void debugFileExists(String basePath, String importName) {
        String fileName1 = importName.replace('.', '/') + ".cod";
        String fileName2 = importName + ".cod";

        File file1 = new File(basePath, fileName1);
        File file2 = new File(basePath, fileName2);

        DebugSystem.debug(
                "FILE_CHECK",
                "Checking file: " + file1.getAbsolutePath() + " [exists: " + file1.exists() + "]");
        DebugSystem.debug(
                "FILE_CHECK",
                "Checking file: " + file2.getAbsolutePath() + " [exists: " + file2.exists() + "]");

        // Also check if the directory exists
        File dir = new File(basePath, "cod");
        DebugSystem.debug(
                "FILE_CHECK",
                "Checking directory: "
                        + dir.getAbsolutePath()
                        + " [exists: "
                        + dir.exists()
                        + ", isDirectory: "
                        + dir.isDirectory()
                        + "]");

        if (dir.exists() && dir.isDirectory()) {
            String[] files = dir.list();
            if (files != null) {
                DebugSystem.debug("FILE_CHECK", "Files in cod directory:");
                for (String file : files) {
                    DebugSystem.debug("FILE_CHECK", "  - " + file);
                }
            }
        }
    }

    private void evalConstructor(ConstructorNode constructor, ObjectInstance obj) {
        DebugSystem.methodEntry(constructor.getClass().getSimpleName(), emptyParamMap());

        Map<String, Object> locals = new HashMap<String, Object>();
        for (ParamNode p : constructor.parameters) {
            locals.put(p.name, 0);
            DebugSystem.debug("MEMORY", "Initialized parameter: " + p.name + " = 0");
        }

        for (StatementNode stmt : constructor.body) {
            Object val =
                    stmtEvaluator.evalStmt(
                            stmt, obj, locals, null); // UPDATED: Use StatementEvaluator
            if (stmt instanceof FieldNode) {
                FieldNode field = (FieldNode) stmt;
                obj.fields.put(field.name, val);
                DebugSystem.fieldUpdate(field.name, val);
            }
        }

        DebugSystem.methodExit(constructor.getClass().getSimpleName(), "constructor completed");
    }

    private Object evalMethod(MethodNode method, ObjectInstance obj, Map<String, Object> locals) {
        DebugSystem.methodEntry(method.name, locals);

        // --- MODIFICATION: Reverted to original, efficient slot initialization ---
        // The parser now populates method.returnSlots correctly.
        Map<String, Object> slotValues = new HashMap<String, Object>();
        for (SlotNode s : method.returnSlots) {
            slotValues.put(s.name, null);
            DebugSystem.debug("SLOTS", "Initialized slot: " + s.name + " = null");
        }
List<String> returnSlotNames = new ArrayList<String>();
for (SlotNode slot : method.returnSlots) {
    returnSlotNames.add(slot.name);
}
DebugSystem.debug("METHODS", "Method " + method.name + " has " + method.returnSlots.size() + " return slots: " + returnSlotNames);

        // Keep track of "current method slots" for nested calls
        Map<String, Object> previousSlots = currentSlots;
        currentSlots = slotValues;

        // FIX: Only enable slot path tracking if method has ACTUAL slots
        slotsInCurrentPath.clear();
        boolean hasSlots = !method.returnSlots.isEmpty(); // This check works again
        if (hasSlots) {
            slotsInCurrentPath.addAll(slotValues.keySet());
            DebugSystem.debug(
                    "SLOTS",
                    "Method has " + method.returnSlots.size() + " slots, enabling early return");
        } else {
            DebugSystem.debug("SLOTS", "Method has no slots, early return disabled");
        }

        Object result = null;

        // Process statements
        for (int i = 0; i < method.body.size(); i++) {
            StatementNode stmt = method.body.get(i);
            DebugSystem.trace(
                    "INTERPRETER",
                    "Executing statement "
                            + (i + 1)
                            + "/"
                            + method.body.size()
                            + ": "
                            + stmt.getClass().getSimpleName());
            result =
                    stmtEvaluator.evalStmt(
                            stmt, obj, locals, slotValues); // UPDATED: Use StatementEvaluator

            // FIX: Only check for early return if method has actual slots
            if (hasSlots && shouldReturnEarly(slotValues)) {
                DebugSystem.debug(
                        "SLOTS", "Early return triggered - all slots assigned in current path");
                break;
            }
        }

        currentSlots = previousSlots; // restore previous

        DebugSystem.methodExit(method.name, slotValues);
        return result;
    }

    // Check if all slots in current control flow path are assigned
    boolean shouldReturnEarly(
            Map<String, Object>
                    slotValues) { // CHANGED: package-private for StatementEvaluator access
        for (String slotName : slotsInCurrentPath) {
            if (slotValues.get(slotName) == null) {
                return false; // Found an unassigned slot in current path
            }
        }
        return true; // All slots in current path are assigned
    }

    public Object evalMethodCall(
        MethodCallNode call,
        ObjectInstance obj,
        Map<String, Object> locals) {
    DebugSystem.methodEntry("call:" + call.name, emptyParamMap());
    
    // ADDED: Detailed debugging for method call
    DebugSystem.debug("METHOD_CALL", "=== METHOD CALL ENTERED ===");
    DebugSystem.debug("METHOD_CALL", "call.name: " + call.name);
    DebugSystem.debug("METHOD_CALL", "call.qualifiedName: " + call.qualifiedName);
    DebugSystem.debug("METHOD_CALL", "call.slotNames: " + call.slotNames);
    DebugSystem.debug("METHOD_CALL", "call.arguments: " + call.arguments.size());
    for (int i = 0; i < call.arguments.size(); i++) {
        DebugSystem.debug("METHOD_CALL", "  arg[" + i + "]: " + call.arguments.get(i).getClass().getSimpleName());
    }
    DebugSystem.debug("METHOD_CALL", "Current object type: " + obj.type.name);
    DebugSystem.debug("METHOD_CALL", "Current object fields: " + obj.fields.keySet());

    MethodNode method = null;

    // First, try to find method in current type
    DebugSystem.debug("METHOD_CALL", "Searching for local method: '" + call.name + "'");
    for (int i = 0; i < obj.type.methods.size(); i++) {
        MethodNode candidate = obj.type.methods.get(i);
        DebugSystem.debug("METHOD_CALL", "  Checking local method: " + candidate.name);
        if (candidate.name.equals(call.name)) {
            method = candidate;
            DebugSystem.debug("METHODS", "Found local method: " + call.name);
            break;
        }
    }

    // If not found locally, try to resolve as imported method
    if (method == null) {
        DebugSystem.debug("METHOD_CALL", "Method not found locally, attempting import resolution");
        
        // FIX: Handle cases where the method name itself might be qualified
        String qualifiedMethodName = call.qualifiedName;
        
        // If qualifiedName is null but call.name contains dots, use call.name as qualified
        if (qualifiedMethodName == null && call.name.contains(".")) {
            qualifiedMethodName = call.name;
            DebugSystem.debug("IMPORTS", "Using call.name as qualified name: " + qualifiedMethodName);
        } else if (qualifiedMethodName == null) {
            // Fallback: just use the simple name
            qualifiedMethodName = call.name;
            DebugSystem.debug("IMPORTS", "Using simple name as qualified name: " + qualifiedMethodName);
        } else if (qualifiedMethodName != null) {
            DebugSystem.debug("IMPORTS", "Using existing qualified name: " + qualifiedMethodName);
        }
        
        DebugSystem.debug("IMPORTS", "Attempting to resolve imported method: " + qualifiedMethodName);
        method = resolveImportedMethod(qualifiedMethodName);
        
        if (method != null) {
            DebugSystem.debug("IMPORTS", "Successfully resolved imported method: " + qualifiedMethodName);
            DebugSystem.debug("IMPORTS", "Resolved method details - name: " + method.name + 
                             ", params: " + method.parameters.size() + 
                             ", slots: " + method.returnSlots.size()); // This works again!
        } else {
            DebugSystem.warn("IMPORTS", "Failed to resolve imported method: " + qualifiedMethodName);
            
            // Additional debugging: show what imports are available
            Set<String> availableImports = importResolver.getLoadedImports();
            DebugSystem.debug("IMPORTS", "Available loaded imports: " + availableImports);
            Set<String> registeredImports = importResolver.getRegisteredImports();
            DebugSystem.debug("IMPORTS", "Registered imports: " + registeredImports);
            
            // Show what methods are available in current type for comparison
            DebugSystem.debug("IMPORTS", "Available local methods:");
            for (MethodNode m : obj.type.methods) {
                DebugSystem.debug("IMPORTS", "  - " + m.name);
            }
        }
    }

    if (method == null) {
        DebugSystem.error(
                "IMPORTS",
                "Method not found: "
                        + (call.qualifiedName != null ? call.qualifiedName : call.name));
        // Debug: List available methods in current type
        DebugSystem.debug("METHODS", "Available methods in current type:");
        for (MethodNode m : obj.type.methods) {
            DebugSystem.debug("METHODS", "  - " + m.name);
        }
        
        // Show import status for debugging
        importResolver.debugImportStatus();
        
        throw new RuntimeException(
                "Method not found: "
                        + (call.qualifiedName != null ? call.qualifiedName : call.name));
    }

    Map<String, Object> newLocals = new HashMap<String, Object>();
    DebugSystem.debug("METHODS", "Evaluating " + call.arguments.size() + " arguments");

    // Validate parameter count
    if (call.arguments.size() != method.parameters.size()) {
        throw new RuntimeException(
                "Parameter count mismatch for method " + method.name + 
                ": expected " + method.parameters.size() + 
                ", got " + call.arguments.size());
    }

    for (int i = 0; i < call.arguments.size(); i++) {
        ParamNode param = method.parameters.get(i);
        Object argValue =
                exprEvaluator.evaluate(
                        call.arguments.get(i), obj, locals);
        newLocals.put(param.name, argValue);
        DebugSystem.debug("MEMORY", "Bound parameter: " + param.name + " = " + argValue);
    }

    // --- MODIFICATION: Reverted to original, efficient slot initialization ---
    Map<String, Object> slotValues = new HashMap<String, Object>();
    for (SlotNode s : method.returnSlots) {
        slotValues.put(s.name, null);
        DebugSystem.debug("SLOTS", "Initialized return slot: " + s.name);
    }
    // --- END MODIFICATION (REMOVED PRE-SCAN LOOP) ---


    // Execute method body with access to both locals AND slots
    Map<String, Object> previousSlots = currentSlots;
    currentSlots = slotValues;

    // FIX: Only set up slot path tracking if the called method has slots
    Set<String> previousSlotsInPath = slotsInCurrentPath;
    boolean calledMethodHasSlots = !method.returnSlots.isEmpty(); // This works again
    if (calledMethodHasSlots) {
        slotsInCurrentPath = new HashSet<>(slotValues.keySet());
        DebugSystem.debug("SLOTS", "Called method has " + method.returnSlots.size() + " slots");
    } else {
        slotsInCurrentPath = new HashSet<>(); // Empty for void methods
        DebugSystem.debug("SLOTS", "Called method has no return slots");
    }

    DebugSystem.debug("METHOD_CALL", "Executing method body for: " + method.name);
    for (StatementNode stmt : method.body) {
        stmtEvaluator.evalStmt(
                stmt, obj, newLocals, slotValues);
        // Check for early return in called method
        if (calledMethodHasSlots && shouldReturnEarly(slotValues)) {
            DebugSystem.debug("SLOTS", "Early return triggered in called method");
            break;
        }
    }

    currentSlots = previousSlots;
    slotsInCurrentPath = previousSlotsInPath; // Restore previous slot path

    DebugSystem.methodExit("call:" + call.name, slotValues);
    return slotValues;
}

    private MethodNode resolveImportedMethod(String qualifiedMethodName) {
        DebugSystem.debug("IMPORTS", "resolveImportedMethod called with: " + qualifiedMethodName);

        try {
            MethodNode method = importResolver.findMethod(qualifiedMethodName);
            if (method != null) {
                DebugSystem.debug(
                        "IMPORTS", "Successfully resolved imported method: " + qualifiedMethodName);
                DebugSystem.debug(
                        "IMPORTS",
                        "Method details - params: "
                                + method.parameters.size()
                                + ", slots: "
                                + method.returnSlots.size()); // This works again
                return method;
            } else {
                DebugSystem.warn(
                        "IMPORTS", "Failed to resolve imported method: " + qualifiedMethodName);

                // Debug: List available imports
                Set<String> loadedImports = importResolver.getLoadedImports();
                DebugSystem.debug("IMPORTS", "Loaded imports in resolver: " + loadedImports);
                return null;
            }
        } catch (Exception e) {
            DebugSystem.error(
                    "IMPORTS",
                    "Error resolving imported method "
                            + qualifiedMethodName
                            + ": "
                            + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}