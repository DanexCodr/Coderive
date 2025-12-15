package cod.interpreter;

import cod.syntax.Keyword;
import static cod.syntax.Keyword.*;
import cod.ast.nodes.*;
import cod.semantic.ImportResolver;
import cod.debug.DebugSystem;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Interpreter {

  private IOHandler ioHandler = new IOHandler();
  private ImportResolver importResolver = new ImportResolver();
  private TypeSystem typeSystem = new TypeSystem();
  private InterpreterVisitor visitor;

  public Interpreter() {
    this.visitor = new InterpreterVisitor(this, typeSystem, ioHandler);
  }

  public ImportResolver getImportResolver() {
    return importResolver;
  }

  boolean shouldReturnEarly(Map<String, Object> slotValues, Set<String> slotsInCurrentPath) {
    if (slotsInCurrentPath.isEmpty()) return false;
    for (String slotName : slotsInCurrentPath) {
      if (slotValues.get(slotName) == null) return false;
    }
    return true;
  }

  public Object evalReplStatement(
      StmtNode stmt,
      ObjectInstance obj,
      Map<String, Object> locals,
      Map<String, Object> slotValues) {
    Map<String, String> slotTypes = new HashMap<String, String>();
    ExecutionContext ctx = new ExecutionContext(obj, locals, slotValues, slotTypes);
    visitor.pushContext(ctx);
    try {
      return visitor.visit((ASTNode) stmt);
    } finally {
      visitor.popContext();
    }
  }

  public void run(ProgramNode program) {
    DebugSystem.startTimer("program_execution");
    DebugSystem.info("INTERPRETER", "Starting program execution");

    // Handle different program types
    if (program.programType == null) {
      // Fallback to original behavior (assume module)
      DebugSystem.warn("INTERPRETER", "No program type detected, assuming MODULE");
      runModule(program);
    } else {
      switch (program.programType) {
        case MODULE:
          runModule(program);
          break;
        case SCRIPT:
          runScript(program);
          break;
        case METHOD_SCRIPT:
          runMethodScript(program);
          break;
        default:
          throw new RuntimeException("Unknown program type: " + program.programType);
      }
    }

    DebugSystem.stopTimer("program_execution");
    DebugSystem.info("INTERPRETER", "Program execution completed");
    ioHandler.close();
  }

  /**
   * Run a MODULE program. Must have unit declaration and classes. Executes main() method in a
   * class.
   */
  private void runModule(ProgramNode program) {
    UnitNode unit = program.unit;
    initializeImportResolver(unit);

    // Find and execute main() method in a class
    boolean mainExecuted = false;
    for (TypeNode type : unit.types) {
      for (MethodNode method : type.methods) {
        boolean isMainMethod = "main".equals(method.name);
        boolean hasNoParameters = method.parameters.isEmpty();

        if (isMainMethod && hasNoParameters) {
          DebugSystem.methodEntry("main", Collections.<String, Object>emptyMap());
          ObjectInstance obj = new ObjectInstance(type);
          initializeFields(type, obj);
          if (type.constructor != null) evalConstructor(type.constructor, obj);
          Object result = evalMethod(method, obj, new HashMap<String, Object>());
          DebugSystem.methodExit("main", result);
          mainExecuted = true;
          break; // Only execute first main() found
        } else if (isMainMethod && !hasNoParameters) {
          DebugSystem.warn("INTERPRETER", "Ignoring main() with parameters in class: " + type.name);
        }
      }
      if (mainExecuted) break;
    }

    if (!mainExecuted) {
      throw new RuntimeException("Module must have a class with 'main()' method");
    }
  }

  /** Run a SCRIPT program. Direct statements executed top-down. No classes, no methods required. */
  private void runScript(ProgramNode program) {
    UnitNode unit = program.unit;
    initializeImportResolver(unit);

    // Create a dummy object instance for script execution
    ObjectInstance obj = new ObjectInstance(null);
    Map<String, Object> locals = new HashMap<String, Object>();

    DebugSystem.methodEntry("script", Collections.<String, Object>emptyMap());

    // Execute all statements in order (script has only synthetic __Script__ type)
    for (TypeNode type : unit.types) {
      // Look for synthetic script type
      if (type.name != null && type.name.equals("__Script__")) {
        if (type.statements != null) {
          for (StmtNode stmt : type.statements) {
            ExecutionContext ctx = new ExecutionContext(obj, locals, null, null);
            visitor.pushContext(ctx);
            try {
              Object result = visitor.visit((ASTNode) stmt);
              DebugSystem.debug("SCRIPT", "Executed statement: " + stmt.getClass().getSimpleName());
              if (result != null) {
                DebugSystem.debug("SCRIPT", "  Result: " + result);
              }
            } finally {
              visitor.popContext();
            }
          }
        }
        break;
      }
    }

    DebugSystem.methodExit("script", "completed");
  }

  /** Run a METHOD_SCRIPT program. Only methods, no direct code. Must have main() method. */
  private void runMethodScript(ProgramNode program) {
    UnitNode unit = program.unit;
    initializeImportResolver(unit);

    // Find main() method in synthetic __MethodScript__ type
    MethodNode mainMethod = null;
    TypeNode containerType = null;

    for (TypeNode type : unit.types) {
      // Look for synthetic method script type
      if (type.name != null && type.name.equals("__MethodScript__")) {
        containerType = type;
        if (type.methods != null) {
          for (MethodNode method : type.methods) {
            if ("main".equals(method.name)
                && (method.parameters == null || method.parameters.isEmpty())) {
              mainMethod = method;
              break;
            }
          }
        }
        break;
      }
    }

    // If not found in synthetic type, look in any type
    if (mainMethod == null) {
      for (TypeNode type : unit.types) {
        if (type.methods != null) {
          for (MethodNode method : type.methods) {
            if ("main".equals(method.name)
                && (method.parameters == null || method.parameters.isEmpty())) {
              mainMethod = method;
              containerType = type;
              break;
            }
          }
        }
        if (mainMethod != null) break;
      }
    }

    if (mainMethod == null) {
      throw new RuntimeException("Method script requires a 'main()' method");
    }

    DebugSystem.methodEntry("main", Collections.<String, Object>emptyMap());
    ObjectInstance obj = new ObjectInstance(containerType);
    initializeFields(containerType, obj);
    if (containerType.constructor != null) {
      evalConstructor(containerType.constructor, obj);
    }
    Object result = evalMethod(mainMethod, obj, new HashMap<String, Object>());
    DebugSystem.methodExit("main", result);
  }

  private void initializeFields(TypeNode type, ObjectInstance obj) {
    ExecutionContext ctx = new ExecutionContext(obj, new HashMap<String, Object>(), null, null);
    visitor.pushContext(ctx);
    try {
      if (type.fields != null) {
        for (FieldNode field : type.fields) {
          visitor.visit((ASTNode) field);
        }
      }
    } finally {
      visitor.popContext();
    }
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
    DebugSystem.methodEntry("Constructor", Collections.<String, Object>emptyMap());
    Map<String, Object> locals = new HashMap<String, Object>();
    for (ParamNode p : constructor.parameters) locals.put(p.name, 0);
    ExecutionContext ctx = new ExecutionContext(obj, locals, null, null);
    visitor.pushContext(ctx);
    for (StmtNode stmt : constructor.body) {
      Object val = visitor.visit((ASTNode) stmt);
      if (stmt instanceof FieldNode) obj.fields.put(((FieldNode) stmt).name, val);
    }
    visitor.popContext();
    DebugSystem.methodExit("Constructor", "completed");
  }

  private Object evalMethod(MethodNode method, ObjectInstance obj, Map<String, Object> locals) {
    DebugSystem.methodEntry(method.name, locals);
    // Use LinkedHashMap to preserve slot order
    Map<String, Object> slotValues = new LinkedHashMap<String, Object>();
    Map<String, String> slotTypes = new LinkedHashMap<String, String>();
    if (method.returnSlots != null) {
      for (SlotNode s : method.returnSlots) {
        slotValues.put(s.name, null);
        slotTypes.put(s.name, s.type);
      }
    }
    ExecutionContext ctx = new ExecutionContext(obj, locals, slotValues, slotTypes);
    visitor.pushContext(ctx);
    Object result = null;
    boolean hasSlots = method.returnSlots != null && !method.returnSlots.isEmpty();

    try {
      if (method.body != null) {
        for (StmtNode stmt : method.body) {
          result = visitor.visit((ASTNode) stmt);
          if (hasSlots && shouldReturnEarly(slotValues, ctx.slotsInCurrentPath)) break;
        }
      }
    } catch (EarlyExitException e) {
      // Early exit requested
    }
    visitor.popContext();
    DebugSystem.methodExit(method.name, slotValues);

    // Return slot values if method has return slots, otherwise return result
    return hasSlots ? slotValues : result;
  }

  public Object evalMethodCall(
      MethodCallNode call, ObjectInstance obj, Map<String, Object> locals) {
    MethodNode method = null;
    if (obj.type != null && obj.type.methods != null) {
      for (MethodNode m : obj.type.methods) {
        if (m.name.equals(call.name)) {
          method = m;
          break;
        }
      }
    }
    if (method == null) {
      String qName = call.qualifiedName != null ? call.qualifiedName : call.name;
      method = resolveImportedMethod(qName);
    }
    if (method == null) throw new RuntimeException("Method not found: " + call.name);
    if (method.isBuiltin) return handleBuiltinMethod(method, call);

    Map<String, Object> newLocals = new HashMap<String, Object>();
    Map<String, String> newLocalTypes = new HashMap<String, String>();

    int argCount = call.arguments != null ? call.arguments.size() : 0;
    int paramCount = method.parameters != null ? method.parameters.size() : 0;

    // Process each parameter
    for (int i = 0; i < paramCount; i++) {
      ParamNode param = method.parameters.get(i);
      Object argValue = null;

      // Check if argument is provided
      if (i < argCount) {
        ExprNode argExpr = call.arguments.get(i);

        // Check if argument is underscore placeholder
        if (argExpr instanceof ExprNode && "_".equals(((ExprNode) argExpr).name)) {
          // Use underscore placeholder
          if (param.hasDefaultValue) {
            // Evaluate default value expression
            argValue = visitor.visit((ASTNode) param.defaultValue);
          } else {
            throw new RuntimeException(
                "Parameter '"
                    + param.name
                    + "' has no default value and cannot be skipped with '_'");
          }
        } else {
          // Regular argument
          argValue = visitor.visit((ASTNode) argExpr);
        }
      } else {
        if (param.hasDefaultValue) {
          argValue = visitor.visit((ASTNode) param.defaultValue);

          // Validate type (even for defaults!)
          if (!typeSystem.validateType(param.type, argValue)) {
            throw new RuntimeException(
                "Default value for parameter '"
                    + param.name
                    + "' returns wrong type. Expected "
                    + param.type
                    + ", got: "
                    + typeSystem.getConcreteType(argValue));
          }
        }
      }

      String paramType = param.type;

      // Type validation (existing code)
      if (!typeSystem.validateType(paramType, argValue)) {
        if (paramType.equals(Keyword.TEXT.toString())) {
          argValue = typeSystem.convertType(argValue, paramType);
        } else {
          throw new RuntimeException(
              "Argument type mismatch for parameter " + param.name + ". Expected " + paramType);
        }
      }

      if (paramType.contains("|")) {
        String activeType = typeSystem.getConcreteType(typeSystem.unwrap(argValue));
        argValue = new TypedValue(argValue, activeType, paramType);
      }

      newLocals.put(param.name, argValue);
      newLocalTypes.put(param.name, paramType);
    }

    // Check for too many arguments
    if (argCount > paramCount) {
      throw new RuntimeException(
          "Too many arguments: expected " + paramCount + ", got " + argCount);
    }

    // Rest of the method remains the same...
    Map<String, Object> slotValues = new LinkedHashMap<String, Object>();
    Map<String, String> slotTypes = new LinkedHashMap<String, String>();
    if (method.returnSlots != null) {
      for (SlotNode s : method.returnSlots) {
        slotValues.put(s.name, null);
        slotTypes.put(s.name, s.type);
      }
    }

    ExecutionContext ctx = new ExecutionContext(obj, newLocals, slotValues, slotTypes);
    ctx.localTypes.putAll(newLocalTypes);

    visitor.pushContext(ctx);
    boolean calledMethodHasSlots = method.returnSlots != null && !method.returnSlots.isEmpty();

    try {
      if (method.body != null) {
        for (StmtNode stmt : method.body) {
          visitor.visit((ASTNode) stmt);
          if (calledMethodHasSlots && shouldReturnEarly(slotValues, ctx.slotsInCurrentPath)) break;
        }
      }
    } catch (EarlyExitException e) {
    }

    visitor.popContext();

    return calledMethodHasSlots ? slotValues : null;
  }

  private Object handleBuiltinMethod(MethodNode method, MethodCallNode call) {
    if ("printp".equals(method.name)) {
      StringBuilder result = new StringBuilder();
      if (call.arguments != null) {
        for (int i = 0; i < call.arguments.size(); i++) {
          Object value = visitor.visit((ASTNode) call.arguments.get(i));
          result.append(String.valueOf(value));
        }
      }
      ioHandler.output(result.toString());
      return null;
    }
    throw new RuntimeException("Unknown builtin method: " + method.name);
  }

  private MethodNode resolveImportedMethod(String qualifiedMethodName) {
    try {
      return importResolver.findMethod(qualifiedMethodName);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static class EarlyExitException extends RuntimeException {
    public EarlyExitException() {
      super("Early exit");
    }
  }
}
