package cod.interpreter;

import cod.ast.nodes.*;
import cod.ast.ASTFactory;
import cod.debug.DebugSystem;
import cod.interpreter.context.*;
import cod.interpreter.exception.*;
import cod.interpreter.io.IOHandler;
import cod.interpreter.registry.*;
import cod.interpreter.type.*;
import cod.semantic.ImportResolver;
import cod.semantic.ConstructorResolver;
import cod.syntax.Keyword;
import static cod.syntax.Keyword.*;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
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
  private String currentFilePath;  // ADD THIS FIELD
  
public Interpreter() {
    this.visitor = new InterpreterVisitor(this, typeSystem);
    this.constructorResolver = new ConstructorResolver(typeSystem, visitor, this);
    this.currentProgram = null;
    builtinRegistry = new BuiltinRegistry(ioHandler);
    globalRegistry = new GlobalRegistry(ioHandler, builtinRegistry);
}

  // Add a method to set the file path
  public void setFilePath(String filePath) {
      this.currentFilePath = filePath;
      DebugSystem.debug("INTERPRETER", "Set file path: " + filePath);
  }
  
  // Add a getter for the file path
  public String getCurrentFilePath() {
      return currentFilePath;
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
    DebugSystem.info("INTERPRETER", "Starting program execution from: " + 
        (currentFilePath != null ? currentFilePath : "unknown location"));
    
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
    
    // NEW: Scan for broadcasts in the same package
    if (unit.mainClassName == null || unit.mainClassName.isEmpty()) {
        scanPackageForBroadcasts(unit.name);
    }
    
    // NEW: Check for imported broadcasts if no local broadcast
    String mainClassNameToUse = null;
    boolean useImportedBroadcast = false;
    
    if (unit.mainClassName != null && !unit.mainClassName.isEmpty()) {
        // Local broadcast takes priority
        mainClassNameToUse = unit.mainClassName;
        DebugSystem.info("INTERPRETER", "Using local broadcast: (main: " + mainClassNameToUse + ")");
    } else {
        // Check imported broadcasts for this package
        String importedBroadcast = importResolver.getBroadcast(unit.name);
        if (importedBroadcast != null) {
            mainClassNameToUse = importedBroadcast;
            useImportedBroadcast = true;
            DebugSystem.info("INTERPRETER", "Using imported broadcast for package '" + 
                unit.name + "': (main: " + mainClassNameToUse + ")");
        }
    }
    
    boolean mainExecuted = false;
    
    // Strategy 1: Look for local main() in any class in this file
    TypeNode localMainClass = null;
    MethodNode localMainMethod = null;
    
    for (TypeNode type : unit.types) {
        for (MethodNode method : type.methods) {
            if ("main".equals(method.methodName) && 
                method.parameters.isEmpty()) {
                localMainClass = type;
                localMainMethod = method;
                break;
            }
        }
        if (localMainClass != null) break;
    }
    
    // Rule: Local main() takes priority
    if (localMainClass != null && localMainMethod != null) {
        DebugSystem.methodEntry("main (local)", Collections.<String, Object>emptyMap());
        
        // FIX: Create an empty context before creating the object
        ExecutionContext emptyContext = new ExecutionContext(null, new HashMap<String, Object>(), null, null);
        visitor.pushContext(emptyContext);
        
        try {
            // UPDATED: Pass null as the token parameter to createConstructorCall
            ObjectInstance obj = constructorResolver.resolveAndCreate(
                ASTFactory.createConstructorCall(localMainClass.name, new ArrayList<ExprNode>(), null), 
                emptyContext
            );
            
            Object result = evalMethod(localMainMethod, obj, new HashMap<String, Object>());
            DebugSystem.methodExit("main", result);
            mainExecuted = true;
        } finally {
            visitor.popContext();
        }
    }
    // Strategy 2: Use broadcasted main class (local or imported)
    else if (!mainExecuted && mainClassNameToUse != null) {
        DebugSystem.info("INTERPRETER", "Running " + 
            (useImportedBroadcast ? "imported" : "local") + 
            " broadcasted main class: " + mainClassNameToUse);
        
        // Find the broadcasted main class
        TypeNode broadcastedClass = null;
        MethodNode broadcastedMainMethod = null;
        
        if (useImportedBroadcast) {
            // For imported broadcast, we need to find the class in imports
            broadcastedClass = importResolver.findType(mainClassNameToUse);
            if (broadcastedClass != null) {
                // Find main method in the imported class
                for (MethodNode method : broadcastedClass.methods) {
                    if ("main".equals(method.methodName) && 
                        method.parameters.isEmpty()) {
                        broadcastedMainMethod = method;
                        break;
                    }
                }
            }
        } else {
            // Local broadcast (existing code)
            for (TypeNode type : unit.types) {
                if (type.name.equals(mainClassNameToUse)) {
                    broadcastedClass = type;
                    for (MethodNode method : type.methods) {
                        if ("main".equals(method.methodName) && 
                            method.parameters.isEmpty()) {
                            broadcastedMainMethod = method;
                            break;
                        }
                    }
                    break;
                }
            }
        }
        
        if (broadcastedClass != null && broadcastedMainMethod != null) {
            DebugSystem.methodEntry("main (broadcast)", Collections.<String, Object>emptyMap());
            
            // FIX: Create an empty context before creating the object
            ExecutionContext emptyContext = new ExecutionContext(null, new HashMap<String, Object>(), null, null);
            visitor.pushContext(emptyContext);
            
            try {
                // UPDATED: Pass null as the token parameter to createConstructorCall
                ObjectInstance obj = constructorResolver.resolveAndCreate(
                    ASTFactory.createConstructorCall(broadcastedClass.name, new ArrayList<ExprNode>(), null), 
                    emptyContext
                );
                
                Object result = evalMethod(broadcastedMainMethod, obj, new HashMap<String, Object>());
                DebugSystem.methodExit("main", result);
                mainExecuted = true;
            } finally {
                visitor.popContext();
            }
        } else {
            String source = useImportedBroadcast ? "imported broadcast" : "local broadcast";
            throw new RuntimeException(
                source + " main class '" + mainClassNameToUse + 
                "' not found or has no main() method"
            );
        }
    }
    // Strategy 3: Fallback - any class with main() in the package
    else {
        // Legacy behavior: find any main() in package
        for (TypeNode type : unit.types) {
            for (MethodNode method : type.methods) {
                if ("main".equals(method.methodName) && method.parameters.isEmpty()) {
                    DebugSystem.methodEntry("main (legacy)", Collections.<String, Object>emptyMap());
                    
                    // FIX: Create an empty context before creating the object
                    ExecutionContext emptyContext = new ExecutionContext(null, new HashMap<String, Object>(), null, null);
                    visitor.pushContext(emptyContext);
                    
                    try {
                        // UPDATED: Pass null as the token parameter to createConstructorCall
                        ObjectInstance obj = constructorResolver.resolveAndCreate(
                            ASTFactory.createConstructorCall(type.name, new ArrayList<ExprNode>(), null), 
                            emptyContext
                        );
                        
                        Object result = evalMethod(method, obj, new HashMap<String, Object>());
                        DebugSystem.methodExit("main", result);
                        mainExecuted = true;
                    } finally {
                        visitor.popContext();
                    }
                    break;
                }
            }
            if (mainExecuted) break;
        }
    }
    
    if (!mainExecuted) {
        String broadcastInfo = "";
        String importedBroadcast = importResolver.getBroadcast(unit.name);
        if (importedBroadcast != null) {
            broadcastInfo = "\nFound imported broadcast: unit " + unit.name + 
                           " (main: " + importedBroadcast + ")\n" +
                           "But class '" + importedBroadcast + "' not found in imports.";
        }
        
        throw new RuntimeException(
            "No executable main() found in package '" + unit.name + "'\n" +
            "Add one of:\n" +
            "1. A class with main() method in this file\n" +
            "2. A broadcast declaration in this file: unit " + unit.name + 
               " (main: YourClass)\n" +
               "   (YourClass must be in same file and have share main())\n" +
            broadcastInfo
        );
    }
}

private void scanPackageForBroadcasts(String packageName) {
    DebugSystem.debug("BROADCAST", "=== STARTING BROADCAST SCAN ===");
    DebugSystem.debug("BROADCAST", "Scanning package '" + packageName + "' for broadcasts");
    
    // Get the directory of the current file
    String currentFilePath = getCurrentFilePath();
    if (currentFilePath == null) {
        DebugSystem.debug("BROADCAST", "No file path available for scanning");
        DebugSystem.debug("BROADCAST", "=== BROADCAST SCAN COMPLETE (NO FILE PATH) ===");
        return;
    }
    
    DebugSystem.debug("BROADCAST", "Current file path: " + currentFilePath);
    
    File currentFile = new File(currentFilePath);
    File packageDir = currentFile.getParentFile();
    if (packageDir == null || !packageDir.exists()) {
        DebugSystem.debug("BROADCAST", "Package directory not found: " + packageDir);
        DebugSystem.debug("BROADCAST", "=== BROADCAST SCAN COMPLETE (NO DIRECTORY) ===");
        return;
    }
    
    DebugSystem.debug("BROADCAST", "Scanning directory: " + packageDir.getAbsolutePath());
    
    // List all .cod files in the directory
    File[] files = packageDir.listFiles(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".cod");
        }
    });
    
    if (files == null) {
        DebugSystem.debug("BROADCAST", "No files found in directory");
        DebugSystem.debug("BROADCAST", "=== BROADCAST SCAN COMPLETE (NO FILES) ===");
        return;
    }
    
    DebugSystem.debug("BROADCAST", "Found " + files.length + " .cod files in directory");
    
    for (File file : files) {
        DebugSystem.debug("BROADCAST", "Processing file: " + file.getName());
        
        // Skip the current file
        if (file.getAbsolutePath().equals(currentFilePath)) {
            DebugSystem.debug("BROADCAST", "Skipping current file: " + file.getName());
            continue;
        }
        
        BufferedReader reader = null;
        try {
            DebugSystem.debug("BROADCAST", "Checking file for package '" + packageName + "': " + file.getName());
            
            // Read file content
            StringBuilder content = new StringBuilder();
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            
            String fileContent = content.toString();
            
            // Quick check: does this file declare the same package?
            if (fileContent.contains("unit " + packageName)) {
                DebugSystem.debug("BROADCAST", "File " + file.getName() + " declares unit " + packageName);
                
                // Check if it has a broadcast
                Pattern broadcastPattern = Pattern.compile(
                    "unit\\s+" + Pattern.quote(packageName) + "\\s*\\(\\s*main\\s*:\\s*([A-Za-z][A-Za-z0-9_]*)\\s*\\)"
                );
                Matcher matcher = broadcastPattern.matcher(fileContent);
                
                if (matcher.find()) {
                    String broadcastClass = matcher.group(1);
                    DebugSystem.info("BROADCAST", "Found broadcast in " + file.getName() + ": (main: " + broadcastClass + ")");
                    
                    // Register the broadcast
                    importResolver.registerBroadcast(packageName, broadcastClass);
                    
                    // Also load the file to make the class available
                    try {
                        String importName = file.getName().replace(".cod", "");
                        ProgramNode importedProgram = importResolver.resolveImport(importName);
                        if (importedProgram != null) {
                            DebugSystem.debug("BROADCAST", "Successfully loaded broadcast file: " + file.getName());
                        }
                    } catch (Exception e) {
                        DebugSystem.warn("BROADCAST", "Could not fully load broadcast file " + 
                            file.getName() + ": " + e.getMessage());
                    }
                } else {
                    DebugSystem.debug("BROADCAST", "File " + file.getName() + " has no broadcast declaration");
                }
            } else {
                DebugSystem.debug("BROADCAST", "File " + file.getName() + " does not declare unit " + packageName);
            }
        } catch (Exception e) {
            DebugSystem.debug("BROADCAST", "Error scanning file " + file.getName() + ": " + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Ignore close error
                }
            }
        }
    }
    
    // Check if broadcast was registered
    String registeredBroadcast = importResolver.getBroadcast(packageName);
    if (registeredBroadcast != null) {
        DebugSystem.info("BROADCAST", "Broadcast registered for package '" + packageName + "': (main: " + registeredBroadcast + ")");
    } else {
        DebugSystem.debug("BROADCAST", "No broadcast found for package '" + packageName + "'");
    }
    
    DebugSystem.debug("BROADCAST", "=== BROADCAST SCAN COMPLETE ===");
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
              DebugSystem.debug("INTERPRETER", "Executed script statement: " + stmt.getClass().getSimpleName());
              if (result != null) {
                DebugSystem.debug("INTERPRETER", "  Result: " + result);
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
    // FIX: Create a context for field initialization
    ExecutionContext fieldCtx = new ExecutionContext(obj, new HashMap<String, Object>(), null, null);
    visitor.pushContext(fieldCtx);
    
    try {
        for (FieldNode field : type.fields) {
            if (field.value != null) {
                Object defaultValue = visitor.dispatch(field.value);
                obj.fields.put(field.name, defaultValue);
            } else {
                String fieldType = field.type;
                if (fieldType.contains(INT.toString())) {
                    obj.fields.put(field.name, 0);
                } else if (fieldType.contains(FLOAT.toString())) {
                    obj.fields.put(field.name, 0.0);
                } else if (fieldType.contains(TEXT.toString())) {
                    obj.fields.put(field.name, "");
                } else if (fieldType.contains(BOOL.toString())) {
                    obj.fields.put(field.name, false);
                } else {
                    obj.fields.put(field.name, null);
                }
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

  public Object evalMethod(MethodNode node, ObjectInstance obj, Map<String, Object> locals) {
    DebugSystem.methodEntry(node.methodName, locals);
    Map<String, Object> slotValues = new LinkedHashMap<String, Object>();
    Map<String, String> slotTypes = new LinkedHashMap<String, String>();
    if (node.returnSlots != null) {
        for (SlotNode s : node.returnSlots) {
            slotValues.put(s.name, null);
            slotTypes.put(s.name, s.type);
        }
    }
    
    // FIXED: Ensure object instance is always set and current class is tracked
    ExecutionContext ctx = new ExecutionContext(obj, locals, slotValues, slotTypes);
    
    // Always set object instance (even if null)
    ctx.objectInstance = obj;
    
    // Set current class if we have object instance
    if (obj != null && obj.type != null) {
        TypeNode currentClass = findTypeByName(obj.type.name);
        if (currentClass != null) {
            ctx.currentClass = currentClass;
        }
    }
    
    // Also set current class from method association
    if (node.associatedClass != null && ctx.currentClass == null) {
        TypeNode associatedClass = findTypeByName(node.associatedClass);
        if (associatedClass != null) {
            ctx.currentClass = associatedClass;
        }
    }
    
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
                // FIXED: Pass the object instance to the evaluation context
                ExecutionContext argCtx = new ExecutionContext(obj, locals, null, null);
                visitor.pushContext(argCtx);
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
                // FIXED: Pass the object instance to the evaluation context
                ExecutionContext argCtx = new ExecutionContext(obj, locals, null, null);
                visitor.pushContext(argCtx);
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
            argValue = new TypeValue(argValue, activeType, paramType);
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

    // FIXED: Ensure object instance is always properly set
    ExecutionContext ctx = new ExecutionContext(obj, newLocals, slotValues, slotTypes);
    ctx.localTypes.putAll(newLocalTypes);
    
    // Always set object instance
    ctx.objectInstance = obj;
    
    // Set current class from method association
    if (method.associatedClass != null) {
        TypeNode classType = findTypeByName(method.associatedClass);
        if (classType != null) {
            ctx.currentClass = classType;
        }
    }
    
    // Also set current class from object type
    if (obj != null && obj.type != null && ctx.currentClass == null) {
        TypeNode classType = findTypeByName(obj.type.name);
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
}