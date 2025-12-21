package cod.interpreter;

import cod.syntax.Keyword;
import static cod.syntax.Keyword.*;
import cod.ast.nodes.*;
import cod.ast.ASTFactory;
import cod.semantic.ImportResolver;
import cod.semantic.ConstructorResolver;
import cod.debug.DebugSystem;

import java.util.*;

public class Interpreter {

  private IOHandler ioHandler = new IOHandler();
  private ImportResolver importResolver = new ImportResolver();
  private TypeSystem typeSystem = new TypeSystem();
  private InterpreterVisitor visitor;
  private ConstructorResolver constructorResolver;
  private ProgramNode currentProgram;
  private BuiltinRegistry builtinRegistry;
  private GlobalRegistry globalRegistry;

  public Interpreter() {
    this.visitor = new InterpreterVisitor(this, typeSystem, ioHandler);
    this.constructorResolver = new ConstructorResolver(typeSystem, visitor, this);
    this.currentProgram = null;
    builtinRegistry = new BuiltinRegistry(ioHandler, typeSystem);
    globalRegistry = new GlobalRegistry(ioHandler, typeSystem, builtinRegistry);
  }

  public ImportResolver getImportResolver() {
    return importResolver;
  }

  public ConstructorResolver getConstructorResolver() {
    return constructorResolver;
  }

  public GlobalRegistry getGlobalRegistry() {
    return globalRegistry;
  }

  boolean shouldReturnEarly(Map<String, Object> slotValues, Set<String> slotsInCurrentPath) {
    if (slotsInCurrentPath.isEmpty()) return false;
    for (String slotName : slotsInCurrentPath) {
      if (slotValues.get(slotName) == null) return false;
    }
    return true;
  }
  
  public ProgramNode getCurrentProgram() {
    return currentProgram;
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
    
    this.currentProgram = program;

    if (program.programType == null) {
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

  private void runModule(ProgramNode program) {
    UnitNode unit = program.unit;
    initializeImportResolver(unit);

    boolean mainExecuted = false;
    for (TypeNode type : unit.types) {
        for (MethodNode node : type.methods) {
            boolean isMainMethod = "main".equals(node.methodName);
            boolean hasNoParameters = node.parameters.isEmpty();

            if (isMainMethod && hasNoParameters) {
                DebugSystem.methodEntry("main", Collections.<String, Object>emptyMap());
                
                ObjectInstance obj = constructorResolver.resolveAndCreate(
                    ASTFactory.createConstructorCall(type.name, new ArrayList<ExprNode>()), 
                    new ExecutionContext(null, new HashMap<String, Object>(), null, null)
                );
                
                Object result = evalMethod(node, obj, new HashMap<String, Object>());
                DebugSystem.methodExit("main", result);
                mainExecuted = true;
                break;
            } else if (isMainMethod && !hasNoParameters) {
                DebugSystem.warn("INTERPRETER", "Ignoring main() with parameters in class: " + type.name);
            }
        }
        if (mainExecuted) break;
    }

    if (!mainExecuted) {
        throw new RuntimeException("Module must have a class with 'main()' node");
    }
}

  private void runScript(ProgramNode program) {
    UnitNode unit = program.unit;
    initializeImportResolver(unit);

    ObjectInstance obj = new ObjectInstance(null);
    Map<String, Object> locals = new HashMap<String, Object>();

    DebugSystem.methodEntry("script", Collections.<String, Object>emptyMap());

    for (TypeNode type : unit.types) {
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

  private void runMethodScript(ProgramNode program) {
    UnitNode unit = program.unit;
    initializeImportResolver(unit);

    MethodNode mainMethod = null;
    TypeNode containerType = null;

    for (TypeNode type : unit.types) {
      if (type.name != null && type.name.equals("__MethodScript__")) {
        containerType = type;
        if (type.methods != null) {
          for (MethodNode node : type.methods) {
            if ("main".equals(node.methodName)
                && (node.parameters == null || node.parameters.isEmpty())) {
              mainMethod = node;
              break;
            }
          }
        }
        break;
      }
    }

    if (mainMethod == null) {
      for (TypeNode type : unit.types) {
        if (type.methods != null) {
          for (MethodNode node : type.methods) {
            if ("main".equals(node.methodName)
                && (node.parameters == null || node.parameters.isEmpty())) {
              mainMethod = node;
              containerType = type;
              break;
            }
          }
        }
        if (mainMethod != null) break;
      }
    }

    if (mainMethod == null) {
      throw new RuntimeException("Method script requires a 'main()' node");
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

  private Object evalMethod(MethodNode node, ObjectInstance obj, Map<String, Object> locals) {
    DebugSystem.methodEntry(node.methodName, locals);
    Map<String, Object> slotValues = new LinkedHashMap<String, Object>();
    Map<String, String> slotTypes = new LinkedHashMap<String, String>();
    if (node.returnSlots != null) {
        for (SlotNode s : node.returnSlots) {
            slotValues.put(s.name, null);
            slotTypes.put(s.name, s.type);
        }
    }
    
    ExecutionContext ctx = new ExecutionContext(obj, locals, slotValues, slotTypes);
    visitor.pushContext(ctx);
    Object result = null;
    boolean hasSlots = node.returnSlots != null && !node.returnSlots.isEmpty();

    try {
        if (node.body != null) {
            for (StmtNode stmt : node.body) {
                result = visitor.visit((ASTNode) stmt);
                if (hasSlots && shouldReturnEarly(slotValues, ctx.slotsInCurrentPath)) {
                    break;
                }
            }
        }
    } catch (EarlyExitException e) {
    }
    
    visitor.popContext();
    DebugSystem.methodExit(node.methodName, slotValues);

    return hasSlots ? slotValues : result;
}

  public Object evalMethodCall(
    MethodCallNode call, ObjectInstance obj, Map<String, Object> locals, MethodNode methodParam) {
    
    if (obj == null && globalRegistry.isGlobal(call.name)) {
        return globalRegistry.executeGlobal(call.name, call.arguments, visitor);
    }
    
    MethodNode method = methodParam;
    
    if (method == null) {
        if (obj != null && obj.type != null) {
            method = constructorResolver.findMethodInHierarchy(obj.type, call.name, 
                new ExecutionContext(obj, locals, null, null));
        }
        
        if (method == null) {
            String qName = call.qualifiedName != null ? call.qualifiedName : call.name;
            method = resolveImportedMethod(qName);
        }
    }
    
    if (method == null) {
        if (globalRegistry.isGlobal(call.name)) {
            return globalRegistry.executeGlobal(call.name, call.arguments, visitor);
        }
        throw new RuntimeException("Method not found: " + call.name);
    }
    
    if (method.isBuiltin) {
        return handleBuiltinMethod(method, call);
    }

    Map<String, Object> newLocals = new HashMap<String, Object>();
    Map<String, String> newLocalTypes = new HashMap<String, String>();

    int argCount = call.arguments != null ? call.arguments.size() : 0;
    int paramCount = method.parameters != null ? method.parameters.size() : 0;

    for (int i = 0; i < paramCount; i++) {
        ParamNode param = method.parameters.get(i);
        Object argValue = null;

        if (i < argCount) {
            ExprNode argExpr = call.arguments.get(i);

            if (argExpr instanceof ExprNode && "_".equals(((ExprNode) argExpr).name)) {
                if (param.hasDefaultValue) {
                    argValue = visitor.visit((ASTNode) param.defaultValue);
                } else {
                    throw new RuntimeException(
                        "Parameter '" + param.name + "' has no default value and cannot be skipped with '_'");
                }
            } else {
                ExecutionContext savedCtx = visitor.getCurrentContext();
                visitor.pushContext(new ExecutionContext(obj, locals, null, null));
                try {
                    argValue = visitor.visit((ASTNode) argExpr);
                } finally {
                    visitor.popContext();
                    if (savedCtx != null) {
                        visitor.pushContext(savedCtx);
                    }
                }
            }
        } else {
            if (param.hasDefaultValue) {
                ExecutionContext savedCtx = visitor.getCurrentContext();
                visitor.pushContext(new ExecutionContext(obj, locals, null, null));
                try {
                    argValue = visitor.visit((ASTNode) param.defaultValue);
                } finally {
                    visitor.popContext();
                    if (savedCtx != null) {
                        visitor.pushContext(savedCtx);
                    }
                }

                if (!typeSystem.validateType(param.type, argValue)) {
                    throw new RuntimeException(
                        "Default value for parameter '" + param.name + 
                        "' returns wrong type. Expected " + param.type + 
                        ", got: " + typeSystem.getConcreteType(argValue));
                }
            } else {
                throw new RuntimeException(
                    "Missing argument for parameter '" + param.name + 
                    "'. Expected " + paramCount + " arguments, got " + argCount);
            }
        }

        String paramType = param.type;

        if (!typeSystem.validateType(paramType, argValue)) {
            if (paramType.equals(Keyword.TEXT.toString())) {
                argValue = typeSystem.convertType(argValue, paramType);
            } else {
                throw new RuntimeException(
                    "Argument type mismatch for parameter " + param.name + 
                    ". Expected " + paramType + ", got: " + typeSystem.getConcreteType(argValue));
            }
        }

        if (paramType.contains("|")) {
            String activeType = typeSystem.getConcreteType(typeSystem.unwrap(argValue));
            argValue = new TypedValue(argValue, activeType, paramType);
        }

        newLocals.put(param.name, argValue);
        newLocalTypes.put(param.name, paramType);
    }

    if (argCount > paramCount) {
        throw new RuntimeException(
            "Too many arguments: expected " + paramCount + ", got " + argCount);
    }

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
    
    if (method.associatedClass != null) {
        TypeNode classType = findTypeByName(method.associatedClass);
        if (classType != null) {
            ctx.currentClass = classType;
        }
    }

    visitor.pushContext(ctx);
    boolean calledMethodHasSlots = method.returnSlots != null && !method.returnSlots.isEmpty();

    try {
        if (method.body != null) {
            for (StmtNode stmt : method.body) {
                visitor.visit((ASTNode) stmt);
                
                if (calledMethodHasSlots && shouldReturnEarly(slotValues, ctx.slotsInCurrentPath)) {
                    break;
                }
            }
        }
    } catch (EarlyExitException e) {
    }

    visitor.popContext();

    return calledMethodHasSlots ? slotValues : null;
}

public Object handleBuiltinMethod(MethodNode node, MethodCallNode call) {
    return builtinRegistry.executeBuiltin(node.methodName, call, visitor);
  }

  private MethodNode resolveImportedMethod(String qualifiedMethodName) {
    try {
        MethodNode node = importResolver.findMethod(qualifiedMethodName);
        
        if (node != null) {
            return node;
        }
        
        if (qualifiedMethodName.contains(".")) {
            String[] parts = qualifiedMethodName.split("\\.");
            if (parts.length >= 2) {
                String className = parts[0];
                String methodName = parts[1];
                
                TypeNode type = importResolver.findType(className);
                if (type != null) {
                    return constructorResolver.findMethodInHierarchy(type, methodName, 
                        new ExecutionContext(null, new HashMap<String, Object>(), null, null));
                }
            }
        }
        
        return null;
    } catch (Exception e) {
        e.printStackTrace();
        return null;
    }
}

private TypeNode findTypeByName(String className) {
    TypeNode type = importResolver.findType(className);
    if (type != null) {
        return type;
    }
    
    ProgramNode currentProgram = getCurrentProgram();
    if (currentProgram != null && currentProgram.unit != null && currentProgram.unit.types != null) {
        for (TypeNode t : currentProgram.unit.types) {
            if (t.name.equals(className)) {
                return t;
            }
        }
    }
    
    return null;
}

  public static class EarlyExitException extends RuntimeException {
    public EarlyExitException() {
      super("Early exit");
    }
  }
  
  public static class SkipIterationException extends RuntimeException {
    public SkipIterationException() {
      super("Skip iteration");
    }
  }
  
  public static class BreakLoopException extends RuntimeException {
    public BreakLoopException() {
        super("Break loop");
    }
  }
}