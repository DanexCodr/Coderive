package cod.interpreter;

import cod.ast.nodes.*;
import cod.ast.ImportResolver;
import cod.debug.DebugSystem;
import java.util.*;
import java.io.File;

public class Interpreter {

    Map<String, Object> currentSlots = null; 
    Map<String, String> currentSlotTypes = null; // NEW: Track slot types for validation
    Set<String> slotsInCurrentPath = new HashSet<>(); 
    
    private IOHandler ioHandler = new IOHandler(); 
    private ImportResolver importResolver = new ImportResolver();
    private TypeSystem typeSystem = new TypeSystem();
    private ExpressionEvaluator exprEvaluator;
    private StatementEvaluator stmtEvaluator;

    public Interpreter() {
        this.exprEvaluator = new ExpressionEvaluator(typeSystem, this);
        this.stmtEvaluator = new StatementEvaluator(this, exprEvaluator, ioHandler, typeSystem);
    }

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

        initializeImportResolver(unit);

        for (TypeNode type : unit.types) {
            DebugSystem.debug("INTERPRETER", "Processing type: " + type.name);

            for (MethodNode method : type.methods) {
                boolean isMainMethod = "main".equals(method.name);
                boolean hasNoParameters = method.parameters.isEmpty();

                if (isMainMethod && hasNoParameters) {
                    DebugSystem.methodEntry("main", emptyParamMap());

                    ObjectInstance obj = new ObjectInstance(type);
                    if (type.constructor != null) {
                        evalConstructor(type.constructor, obj);
                    }

                    Object result = evalMethod(method, obj, new HashMap<String, Object>());
                    DebugSystem.methodExit("main", result);

                } else if (isMainMethod && !hasNoParameters) {
                    DebugSystem.warn("INTERPRETER", "Ignoring main() with parameters: " + method.parameters);
                }
            }
        }

        DebugSystem.stopTimer("program_execution");
        DebugSystem.info("INTERPRETER", "Program execution completed");
        ioHandler.close(); 
    }

    private void initializeImportResolver(UnitNode unit) {
        if (unit.resolvedImports != null && !unit.resolvedImports.isEmpty()) {
            for (Map.Entry<String, ProgramNode> entry : unit.resolvedImports.entrySet()) {
                importResolver.preloadImport(entry.getKey(), entry.getValue());
            }
        }
        if (unit.imports != null && !unit.imports.imports.isEmpty()) {
            for (String importName : unit.imports.imports) {
                importResolver.registerImport(importName);
            }
        }
    }

    private void evalConstructor(ConstructorNode constructor, ObjectInstance obj) {
        DebugSystem.methodEntry(constructor.getClass().getSimpleName(), emptyParamMap());

        Map<String, Object> locals = new HashMap<String, Object>();
        for (ParamNode p : constructor.parameters) {
            locals.put(p.name, 0);
        }

        for (StatementNode stmt : constructor.body) {
            Object val = stmtEvaluator.evalStmt(stmt, obj, locals, null);
            if (stmt instanceof FieldNode) {
                FieldNode field = (FieldNode) stmt;
                obj.fields.put(field.name, val);
            }
        }

        DebugSystem.methodExit(constructor.getClass().getSimpleName(), "constructor completed");
    }

    private Object evalMethod(MethodNode method, ObjectInstance obj, Map<String, Object> locals) {
        DebugSystem.methodEntry(method.name, locals);

        // Initialize slot values and types
        Map<String, Object> slotValues = new HashMap<String, Object>();
        Map<String, String> slotTypes = new HashMap<String, String>();
        
        for (SlotNode s : method.returnSlots) {
            slotValues.put(s.name, null);
            slotTypes.put(s.name, s.type); // Store type for validation
            DebugSystem.debug("SLOTS", "Initialized slot: " + s.name + " (Type: " + s.type + ") = null");
        }

        // Push context
        Map<String, Object> previousSlots = currentSlots;
        Map<String, String> previousSlotTypes = currentSlotTypes;
        currentSlots = slotValues;
        currentSlotTypes = slotTypes;

        slotsInCurrentPath.clear();
        boolean hasSlots = !method.returnSlots.isEmpty(); 
        if (hasSlots) {
            slotsInCurrentPath.addAll(slotValues.keySet());
        }

        Object result = null;

        // Process statements
        for (int i = 0; i < method.body.size(); i++) {
            StatementNode stmt = method.body.get(i);
            result = stmtEvaluator.evalStmt(stmt, obj, locals, slotValues);

            if (hasSlots && shouldReturnEarly(slotValues)) {
                DebugSystem.debug("SLOTS", "Early return triggered - all slots assigned in current path");
                break;
            }
        }

        // Pop context
        currentSlots = previousSlots;
        currentSlotTypes = previousSlotTypes;

        DebugSystem.methodExit(method.name, slotValues);
        return result; // Usually null unless simple return
    }

    boolean shouldReturnEarly(Map<String, Object> slotValues) { 
        for (String slotName : slotsInCurrentPath) {
            if (slotValues.get(slotName) == null) {
                return false; 
            }
        }
        return true; 
    }

    public Object evalMethodCall(MethodCallNode call, ObjectInstance obj, Map<String, Object> locals) {
        MethodNode method = null;

        for (int i = 0; i < obj.type.methods.size(); i++) {
            MethodNode candidate = obj.type.methods.get(i);
            if (candidate.name.equals(call.name)) {
                method = candidate;
                break;
            }
        }

        if (method == null) {
            String qualifiedMethodName = call.qualifiedName != null ? call.qualifiedName : call.name;
            method = resolveImportedMethod(qualifiedMethodName);
        }

        if (method == null) {
            throw new RuntimeException("Method not found: " + (call.qualifiedName != null ? call.qualifiedName : call.name));
        }

        if (method.isBuiltin) {
            return handleBuiltinMethod(method, call, obj, locals);
        }

        Map<String, Object> newLocals = new HashMap<String, Object>();
        if (call.arguments.size() != method.parameters.size()) {
            throw new RuntimeException("Parameter count mismatch for method " + method.name);
        }

        for (int i = 0; i < call.arguments.size(); i++) {
            ParamNode param = method.parameters.get(i);
            Object argValue = exprEvaluator.evaluate(call.arguments.get(i), obj, locals);
            newLocals.put(param.name, argValue);
        }

        // Initialize context for called method
        Map<String, Object> slotValues = new HashMap<String, Object>();
        Map<String, String> slotTypes = new HashMap<String, String>();
        
        for (SlotNode s : method.returnSlots) {
            slotValues.put(s.name, null);
            slotTypes.put(s.name, s.type);
        }

        Map<String, Object> previousSlots = currentSlots;
        Map<String, String> previousSlotTypes = currentSlotTypes;
        currentSlots = slotValues;
        currentSlotTypes = slotTypes;

        Set<String> previousSlotsInPath = slotsInCurrentPath;
        boolean calledMethodHasSlots = !method.returnSlots.isEmpty();
        if (calledMethodHasSlots) {
            slotsInCurrentPath = new HashSet<>(slotValues.keySet());
        } else {
            slotsInCurrentPath = new HashSet<>();
        }

        for (StatementNode stmt : method.body) {
            stmtEvaluator.evalStmt(stmt, obj, newLocals, slotValues);
            if (calledMethodHasSlots && shouldReturnEarly(slotValues)) {
                break;
            }
        }

        currentSlots = previousSlots;
        currentSlotTypes = previousSlotTypes;
        slotsInCurrentPath = previousSlotsInPath;

        return slotValues;
    }

    private Object handleBuiltinMethod(MethodNode method, MethodCallNode call, ObjectInstance obj, Map<String, Object> locals) {
        switch (method.name) {
            case "outa":
                return handleSysOuta(call, obj, locals);
            default:
                throw new RuntimeException("Unknown builtin method: " + method.name);
        }
    }

    private Object handleSysOuta(MethodCallNode call, ObjectInstance obj, Map<String, Object> locals) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < call.arguments.size(); i++) {
            Object value = exprEvaluator.evaluate(call.arguments.get(i), obj, locals);
            result.append(String.valueOf(value));
        }
        ioHandler.output(result.toString());
        return null;
    }

    private MethodNode resolveImportedMethod(String qualifiedMethodName) {
        try {
            return importResolver.findMethod(qualifiedMethodName);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}