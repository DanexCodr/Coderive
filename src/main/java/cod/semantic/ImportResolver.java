package cod.semantic;

import cod.ast.nodes.*;

import cod.lexer.*;
import cod.parser.MainParser;

import cod.debug.DebugSystem;
import java.util.*;
import java.io.*;

public class ImportResolver {
    private Map<String, ProgramNode> importedUnits = new HashMap<String, ProgramNode>();
    private Map<String, ProgramNode> loadedPrograms = new HashMap<String, ProgramNode>();
    private Map<String, ProgramNode> preloadedImports = new HashMap<String, ProgramNode>();
    private Set<String> registeredImports = new HashSet<String>();
    private List<String> importPaths = new ArrayList<String>();
    private Map<String, String> packageBroadcasts = new HashMap<String, String>();
    
    // Policy registry for cross-file policy resolution
    private Map<String, PolicyNode> importedPolicies = new HashMap<String, PolicyNode>();
    private Map<String, String> policyToUnitMap = new HashMap<String, String>(); // Maps policy name -> unit name
    
    // Register broadcast declarations
    public void registerBroadcast(String packageName, String mainClassName) {
        if (packageBroadcasts.containsKey(packageName)) {
            // Check for conflicts - multiple broadcasts in same package
            String existing = packageBroadcasts.get(packageName);
            if (!existing.equals(mainClassName)) {
                throw new RuntimeException(
                    "Broadcast conflict in package '" + packageName + "':\n" +
                    "  Already declared: (main: " + existing + ")\n" +
                    "  New declaration: (main: " + mainClassName + ")\n" +
                    "Only one broadcast per package allowed."
                );
            }
        } else {
            packageBroadcasts.put(packageName, mainClassName);
            DebugSystem.debug("BROADCAST", "Registered broadcast for package '" + 
                packageName + "': (main: " + mainClassName + ")");
        }
    }
    
    // Get broadcast for package
    public String getBroadcast(String packageName) {
        return packageBroadcasts.get(packageName);
    }
    
    // Clear broadcasts (for testing)
    public void clearBroadcasts() {
        packageBroadcasts.clear();
    }
    
    // Find a policy by qualified name
    public PolicyNode findPolicy(String qualifiedPolicyName) {
        DebugSystem.debug("POLICY_RESOLUTION", "findPolicy called for: " + qualifiedPolicyName);
        
        // Check if already loaded
        if (importedPolicies.containsKey(qualifiedPolicyName)) {
            DebugSystem.debug("POLICY_RESOLUTION", "Policy already loaded: " + qualifiedPolicyName);
            return importedPolicies.get(qualifiedPolicyName);
        }
        
        // Extract package and policy name
        int lastDot = qualifiedPolicyName.lastIndexOf('.');
        String policyName;
        String importName;
        
        if (lastDot == -1) {
            // Simple name like "Serializable"
            policyName = qualifiedPolicyName;
            importName = qualifiedPolicyName;
        } else {
            // Qualified name like "io.Serializable"
            policyName = qualifiedPolicyName.substring(lastDot + 1);
            importName = qualifiedPolicyName.substring(0, lastDot);
        }
        
        DebugSystem.debug("POLICY_RESOLUTION", "Import part: '" + importName + "', policy: '" + policyName + "'");
        
        // Try to resolve the import if not already loaded
        if (!loadedPrograms.containsKey(importName)) {
            try {
                DebugSystem.debug("POLICY_RESOLUTION", "Import not loaded, attempting to load: " + importName);
                resolveImport(importName);
            } catch (Exception e) {
                DebugSystem.error("POLICY_RESOLUTION", "Failed to load import " + importName + ": " + e.getMessage());
                return null;
            }
        }
        
        // Search in loaded program for the policy
        ProgramNode program = loadedPrograms.get(importName);
        if (program != null && program.unit != null && program.unit.policies != null) {
            for (PolicyNode policy : program.unit.policies) {
                if (policy.name.equals(policyName)) {
                    DebugSystem.debug("POLICY_RESOLUTION", "Found policy: " + policy.name);
                    // Cache it with full qualified name
                    importedPolicies.put(qualifiedPolicyName, policy);
                    policyToUnitMap.put(policyName, program.unit.name);
                    return policy;
                }
            }
        }
        
        // Also check if policy is in any loaded program with simple name
        // (for backward compatibility)
        for (Map.Entry<String, ProgramNode> entry : loadedPrograms.entrySet()) {
            ProgramNode prog = entry.getValue();
            if (prog != null && prog.unit != null && prog.unit.policies != null) {
                for (PolicyNode policy : prog.unit.policies) {
                    if (policy.name.equals(policyName)) {
                        DebugSystem.debug("POLICY_RESOLUTION", "Found policy by simple name: " + policyName);
                        // Cache with the qualified name from the unit
                        String qualified = prog.unit.name + "." + policyName;
                        importedPolicies.put(qualified, policy);
                        importedPolicies.put(policyName, policy); // Also cache simple name
                        policyToUnitMap.put(policyName, prog.unit.name);
                        return policy;
                    }
                }
            }
        }
        
        DebugSystem.error("POLICY_RESOLUTION", "Policy not found: " + qualifiedPolicyName);
        DebugSystem.debug("POLICY_RESOLUTION", "Loaded policies: " + importedPolicies.keySet());
        return null;
    }
    
    // Register a policy from a loaded program
    public void registerPolicy(String qualifiedName, PolicyNode policy) {
        importedPolicies.put(qualifiedName, policy);
        DebugSystem.debug("POLICY_RESOLUTION", "Registered policy: " + qualifiedName);
        
        // Also register with simple name for easier lookup
        if (!importedPolicies.containsKey(policy.name)) {
            importedPolicies.put(policy.name, policy);
        }
    }
    
    // Get the unit name for a policy
    public String getPolicyUnit(String policyName) {
        return policyToUnitMap.get(policyName);
    }
    
    // Get all registered policies
    public Set<String> getRegisteredPolicies() {
        return importedPolicies.keySet();
    }

    public ImportResolver() {
        // Initialize with strict src/main/ path only
        importPaths.add("src/main");
        DebugSystem.debug("IMPORTS", "Initialized with strict src/main/ path");
    }

    public void addImportPath(String path) {
        importPaths.add(path);
        DebugSystem.debug("IMPORTS", "Added import path: " + path);
    }
    
    public void registerImport(String importName) {
        // Just store the import name for later resolution
        if (!registeredImports.contains(importName)) {
            registeredImports.add(importName);
            DebugSystem.debug("IMPORTS", "Registered import (lazy): " + importName);
        }
    }

    public ProgramNode resolveImport(String importName) throws Exception {
        DebugSystem.debug("IMPORTS", "resolveImport called for: " + importName);
        
        // Check if already loaded
        if (loadedPrograms.containsKey(importName)) {
            DebugSystem.debug("IMPORTS", "Import already loaded: " + importName);
            return loadedPrograms.get(importName);
        }
        
        // Check if preloaded during AST building
        if (preloadedImports.containsKey(importName)) {
            DebugSystem.debug("IMPORTS", "Using preloaded import: " + importName);
            ProgramNode program = preloadedImports.get(importName);
            loadedPrograms.put(importName, program);
            importedUnits.put(importName, program); // Also add to importedUnits for compatibility
            
            // Register policies from the preloaded import
            if (program.unit != null && program.unit.policies != null) {
                for (PolicyNode policy : program.unit.policies) {
                    // Register with unit.policyName format
                    String qualifiedName = program.unit.name + "." + policy.name;
                    registerPolicy(qualifiedName, policy);
                    policyToUnitMap.put(policy.name, program.unit.name);
                    DebugSystem.debug("IMPORTS", "Registered policy from preloaded import: " + qualifiedName);
                }
            }
            
            // Register any broadcast from the preloaded import
            if (program.unit != null && program.unit.mainClassName != null && 
                !program.unit.mainClassName.isEmpty()) {
                String packageName = program.unit.name;
                String mainClassName = program.unit.mainClassName;
                
                DebugSystem.debug("BROADCAST", "Registering broadcast from preloaded import '" + 
                    importName + "': package '" + packageName + "' (main: " + mainClassName + ")");
                
                registerBroadcast(packageName, mainClassName);
            }
            
            return program;
        }
        
        // STRICT RULE: importName is the UNIT (directory path)
        // Try different file naming patterns in that directory
        
        List<String> attemptedPaths = new ArrayList<String>();
        ProgramNode program = null;
        
        // Convert unit name to directory path
        String dirPath = importName.replace('.', '/');
        
        // Try each import path
        for (String basePath : importPaths) {
            // Build full directory path
            String fullDirPath;
            if (basePath.isEmpty()) {
                fullDirPath = dirPath;
            } else {
                fullDirPath = basePath + "/" + dirPath;
            }
            
            File directory = new File(fullDirPath);
            
            if (directory.exists() && directory.isDirectory()) {
                // Look for .cod files in this directory
                File[] files = directory.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".cod");
                    }
                });
                
                if (files != null && files.length > 0) {
                    // Try each .cod file in the directory
                    for (File file : files) {
                        String filePath = file.getAbsolutePath();
                        attemptedPaths.add(filePath);
                        
                        DebugSystem.debug("IMPORTS", "Trying file in unit directory: " + filePath);
                        
                        try {
                            program = loadImportFromFile(filePath);
                            if (program != null) {
                                // VALIDATE: Check if unit declaration matches directory
                                validateUnitAgainstDirectory(importName, program, filePath);
                                
                                DebugSystem.debug("IMPORTS", "Successfully loaded import from: " + filePath);
                                loadedPrograms.put(importName, program);
                                importedUnits.put(importName, program);
                                
                                // Register policies from the imported program
                                if (program.unit != null && program.unit.policies != null) {
                                    for (PolicyNode policy : program.unit.policies) {
                                        // Register with unit.policyName format
                                        String qualifiedName = program.unit.name + "." + policy.name;
                                        registerPolicy(qualifiedName, policy);
                                        policyToUnitMap.put(policy.name, program.unit.name);
                                        DebugSystem.debug("IMPORTS", "Registered policy from import: " + qualifiedName);
                                    }
                                }
                                
                                // Register any broadcast from the imported program
                                if (program.unit != null && program.unit.mainClassName != null && 
                                    !program.unit.mainClassName.isEmpty()) {
                                    String packageName = program.unit.name;
                                    String mainClassName = program.unit.mainClassName;
                                    
                                    DebugSystem.debug("BROADCAST", "Registering broadcast from import '" + 
                                        importName + "': package '" + packageName + "' (main: " + mainClassName + ")");
                                    
                                    registerBroadcast(packageName, mainClassName);
                                }
                                
                                return program;
                            }
                        } catch (Exception e) {
                            DebugSystem.debug("IMPORTS", "Failed to load from " + filePath + ": " + e.getMessage());
                            // Continue to next file
                        }
                    }
                }
            }
            
            // Also try if it's a file directly (for leaf units like "cod.io" as a file)
            String filePath = fullDirPath + ".cod";
            attemptedPaths.add(filePath);
            
            DebugSystem.debug("IMPORTS", "Trying as file: " + filePath);
            
            try {
                program = loadImportFromFile(filePath);
                if (program != null) {
                    // VALIDATE: Check if unit declaration matches
                    validateUnitAgainstDirectory(importName, program, filePath);
                    
                    DebugSystem.debug("IMPORTS", "Successfully loaded import from: " + filePath);
                    loadedPrograms.put(importName, program);
                    importedUnits.put(importName, program);
                    
                    // Register policies and broadcasts
                    if (program.unit != null && program.unit.policies != null) {
                        for (PolicyNode policy : program.unit.policies) {
                            String qualifiedName = program.unit.name + "." + policy.name;
                            registerPolicy(qualifiedName, policy);
                            policyToUnitMap.put(policy.name, program.unit.name);
                        }
                    }
                    
                    if (program.unit != null && program.unit.mainClassName != null && 
                        !program.unit.mainClassName.isEmpty()) {
                        registerBroadcast(program.unit.name, program.unit.mainClassName);
                    }
                    
                    return program;
                }
            } catch (Exception e) {
                DebugSystem.debug("IMPORTS", "Failed to load from " + filePath + ": " + e.getMessage());
            }
        }
        
        // If we get here, no file was found
        String error = "Import not found: " + importName + 
                      "\nNo .cod files found for unit.\n" +
                      "Expected directory: src/main/" + dirPath + "/ (with .cod files)\n" +
                      "Or file: src/main/" + dirPath + ".cod\n" +
                      "Attempted paths: " + attemptedPaths;
        DebugSystem.error("IMPORTS", error);
        throw new RuntimeException(error);
    }

    // VALIDATE: Check if unit declaration matches directory
    private void validateUnitAgainstDirectory(String expectedUnit, ProgramNode program, String filePath) {
        if (program.unit == null || program.unit.name == null) {
            throw new RuntimeException("Program has no unit declaration in file: " + filePath);
        }
        
        String declaredUnit = program.unit.name;
        
        if (!declaredUnit.equals(expectedUnit)) {
            // Calculate what unit should be based on file location
            String calculatedUnit = calculateUnitFromFilePath(filePath);
            
            if (!declaredUnit.equals(calculatedUnit)) {
                throw new RuntimeException(
                    "Unit declaration mismatch:\n" +
                    "  File: " + filePath + "\n" +
                    "  Declared unit: " + declaredUnit + "\n" +
                    "  Expected unit: " + calculatedUnit + " (based on file location)\n" +
                    "  Import requested: " + expectedUnit + "\n" +
                    "\nUnit should match directory path relative to src/main/"
                );
            }
        }
    }
    
    // Calculate unit name from file path
    private String calculateUnitFromFilePath(String filePath) {
        // Convert: src/main/cod/io/Serializable.cod -> cod.io
        // Convert: src/main/cod/io.cod -> cod.io
        // Convert: src/main/cod.cod -> cod
        
        String normalized = filePath.replace('\\', '/');
        
        // Find src/main/ in the path
        String srcMain = "src/main/";
        int srcMainIndex = normalized.indexOf(srcMain);
        
        if (srcMainIndex == -1) {
            // Try to find any of our import paths
            for (String importPath : importPaths) {
                if (!importPath.isEmpty() && normalized.contains(importPath + "/")) {
                    int importPathIndex = normalized.indexOf(importPath + "/");
                    if (importPathIndex != -1) {
                        String relative = normalized.substring(importPathIndex + importPath.length() + 1);
                        return calculateUnitFromRelativePath(relative);
                    }
                }
            }
            throw new RuntimeException("File not under any import path: " + filePath);
        }
        
        String relative = normalized.substring(srcMainIndex + srcMain.length());
        return calculateUnitFromRelativePath(relative);
    }
    
    private String calculateUnitFromRelativePath(String relativePath) {
        // Remove .cod extension
        if (relativePath.endsWith(".cod")) {
            relativePath = relativePath.substring(0, relativePath.length() - 4);
        }
        
        // Split into components
        String[] parts = relativePath.split("/");
        List<String> unitParts = new ArrayList<String>();
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;
            
            // Check if this looks like a file name (PascalCase for classes/policies)
            boolean looksLikeFileName = part.length() > 0 && Character.isUpperCase(part.charAt(0));
            
            if (i == parts.length - 1 && looksLikeFileName) {
                // Last component and looks like a class name, skip it
                continue;
            }
            
            unitParts.add(part);
        }
        
        // Join with dots
        StringBuilder unitName = new StringBuilder();
        for (int i = 0; i < unitParts.size(); i++) {
            if (i > 0) unitName.append(".");
            unitName.append(unitParts.get(i));
        }
        
        return unitName.toString();
    }
    
    // Load import from file
    private ProgramNode loadImportFromFile(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        
        DebugSystem.debug("IMPORTS", "Loading import from file: " + filePath);
        
        BufferedReader reader = null;
        try {
            // Read the file content
            StringBuilder content = new StringBuilder();
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
            
            DebugSystem.debug("IMPORTS", "File content length: " + content.length() + " characters");
            
            // Use the SAME MANUAL parser that we use for the main file
            MainLexer lexer = new MainLexer(content.toString());
            List<Token> tokens = lexer.tokenize();
            
            DebugSystem.debug("IMPORTS", "Generated " + tokens.size() + " tokens");
            
            MainParser parser = new MainParser(tokens);
            ProgramNode program = parser.parseProgram();
            
            DebugSystem.debug("IMPORTS", "Successfully parsed import file: " + filePath);
            return program;

        } catch (Exception e) {
            DebugSystem.error("IMPORTS", "Failed to parse import file: " + filePath + " - " + e.getMessage());
            throw new RuntimeException("Failed to parse import file: " + filePath + " - " + e.getMessage(), e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    public TypeNode findType(String qualifiedTypeName) {
        DebugSystem.debug("IMPORTS", "findType called for: " + qualifiedTypeName);
        
        // Extract type name and potential import name
        int lastDot = qualifiedTypeName.lastIndexOf('.');
        if (lastDot == -1) {
            // Simple name like "Sys" - need to search all imports
            DebugSystem.debug("IMPORTS", "Simple type name, searching all imports");
            return findTypeByName(qualifiedTypeName);
        }
        
        String typeName = qualifiedTypeName.substring(lastDot + 1);
        String importPart = qualifiedTypeName.substring(0, lastDot);
        
        DebugSystem.debug("IMPORTS", "Import part: '" + importPart + "', type: '" + typeName + "'");
        
        // First check if we already have a loaded import that matches
        String actualImportName = null;
        
        // Check loaded imports first
        for (String loadedImport : loadedPrograms.keySet()) {
            DebugSystem.debug("IMPORTS", "Checking loaded import: " + loadedImport);
            
            if (loadedImport.endsWith("." + importPart) || loadedImport.equals(importPart)) {
                actualImportName = loadedImport;
                DebugSystem.debug("IMPORTS", "Found matching loaded import: " + actualImportName);
                break;
            }
        }
        
        // If not found in loaded imports, check registered imports
        if (actualImportName == null) {
            for (String registeredImport : registeredImports) {
                DebugSystem.debug("IMPORTS", "Checking registered import: " + registeredImport);
                
                if (registeredImport.endsWith("." + importPart) || registeredImport.equals(importPart)) {
                    actualImportName = registeredImport;
                    DebugSystem.debug("IMPORTS", "Found matching registered import: " + actualImportName);
                    break;
                }
            }
        }
        
        // If still not found, use the import part as-is
        if (actualImportName == null) {
            actualImportName = importPart;
            DebugSystem.debug("IMPORTS", "No import matched, using: " + actualImportName);
        }
        
        DebugSystem.debug("IMPORTS", "Final import to resolve: '" + actualImportName + "', type: '" + typeName + "'");
        
        // Try to resolve the import if not already loaded
        if (!loadedPrograms.containsKey(actualImportName)) {
            DebugSystem.debug("IMPORTS", "Import not loaded, trying to resolve: " + actualImportName);
            try {
                ProgramNode program = resolveImport(actualImportName);
                if (program != null) {
                    loadedPrograms.put(actualImportName, program);
                    importedUnits.put(actualImportName, program);
                    registeredImports.remove(actualImportName);
                    DebugSystem.debug("IMPORTS", "Successfully loaded import: " + actualImportName);
                }
            } catch (Exception e) {
                DebugSystem.error("IMPORTS", "Failed to load import " + actualImportName + ": " + e.getMessage());
                return null;
            }
        }
        
        // Search through loaded programs for the type
        DebugSystem.debug("IMPORTS", "Searching for type '" + typeName + "' in loaded program: " + actualImportName);
        
        ProgramNode program = loadedPrograms.get(actualImportName);
        if (program != null && program.unit != null && program.unit.types != null) {
            for (TypeNode type : program.unit.types) {
                DebugSystem.debug("IMPORTS", "  Checking type: " + type.name);
                if (type.name.equals(typeName)) {
                    DebugSystem.debug("IMPORTS", "    *** FOUND TYPE: " + type.name + " ***");
                    return type;
                }
            }
        }
        
        // Type not found
        DebugSystem.error("IMPORTS", "*** TYPE NOT FOUND: " + qualifiedTypeName + " ***");
        DebugSystem.debug("IMPORTS", "Loaded imports: " + loadedPrograms.keySet());
        DebugSystem.debug("IMPORTS", "Registered imports: " + registeredImports);
        
        return null;
    }

    private TypeNode findTypeByName(String typeName) {
        // Search all loaded programs for this type name
        for (Map.Entry<String, ProgramNode> entry : loadedPrograms.entrySet()) {
            ProgramNode program = entry.getValue();
            if (program != null && program.unit != null && program.unit.types != null) {
                for (TypeNode type : program.unit.types) {
                    if (type.name.equals(typeName)) {
                        return type;
                    }
                }
            }
        }
        
        // Try loading from registered imports that might contain this type
        for (String importName : registeredImports) {
            if (importName.endsWith("." + typeName)) {
                try {
                    ProgramNode program = resolveImport(importName);
                    if (program != null && program.unit != null && program.unit.types != null) {
                        for (TypeNode type : program.unit.types) {
                            if (type.name.equals(typeName)) {
                                return type;
                            }
                        }
                    }
                } catch (Exception e) {
                    continue;
                }
            }
        }
        
        return null;
    }

    public MethodNode findMethod(String qualifiedMethodName) {
        DebugSystem.debug("IMPORTS", "findMethod called for: " + qualifiedMethodName);
        
        // Extract node name and potential import name
        int lastDot = qualifiedMethodName.lastIndexOf('.');
        if (lastDot == -1) {
            DebugSystem.debug("IMPORTS", "No dots in node name, not an imported node");
            return null; // Not an imported node
        }
        
        String methodName = qualifiedMethodName.substring(lastDot + 1);
        String calledImport = qualifiedMethodName.substring(0, lastDot); // This is "Sys" from "Sys.outln"
        
        DebugSystem.debug("IMPORTS", "Called import part: '" + calledImport + "', node: '" + methodName + "'");
        
        // FIX: First check if we already have a loaded import that matches
        String actualImportName = null;
        
        // Check loaded imports first
        for (String loadedImport : loadedPrograms.keySet()) {
            DebugSystem.debug("IMPORTS", "Checking loaded import: " + loadedImport);
            
            // Check if loaded import ends with the called import
            // e.g., "cod.Sys" ends with ".Sys" and calledImport is "Sys"
            if (loadedImport.endsWith("." + calledImport) || loadedImport.equals(calledImport)) {
                actualImportName = loadedImport;
                DebugSystem.debug("IMPORTS", "Found matching loaded import: " + actualImportName);
                break;
            }
        }
        
        // If not found in loaded imports, check registered imports
        if (actualImportName == null) {
            for (String registeredImport : registeredImports) {
                DebugSystem.debug("IMPORTS", "Checking registered import: " + registeredImport);
                
                // Check if registered import ends with the called import
                if (registeredImport.endsWith("." + calledImport) || registeredImport.equals(calledImport)) {
                    actualImportName = registeredImport;
                    DebugSystem.debug("IMPORTS", "Found matching registered import: " + actualImportName);
                    break;
                }
            }
        }
        
        // If still not found, use the called import as-is
        if (actualImportName == null) {
            actualImportName = calledImport;
            DebugSystem.debug("IMPORTS", "No import matched, using: " + actualImportName);
        }
        
        DebugSystem.debug("IMPORTS", "Final import to resolve: '" + actualImportName + "', node: '" + methodName + "'");
        
        // Try to resolve the import if not already loaded
        if (!loadedPrograms.containsKey(actualImportName)) {
            DebugSystem.debug("IMPORTS", "Import not loaded, trying to resolve: " + actualImportName);
            try {
                ProgramNode program = resolveImport(actualImportName);
                if (program != null) {
                    loadedPrograms.put(actualImportName, program);
                    importedUnits.put(actualImportName, program);
                    registeredImports.remove(actualImportName);
                    DebugSystem.debug("IMPORTS", "Successfully loaded import: " + actualImportName);
                }
            } catch (Exception e) {
                DebugSystem.error("IMPORTS", "Failed to load import " + actualImportName + ": " + e.getMessage());
                return null;
            }
        }
        
        // Search through loaded programs for the node
        DebugSystem.debug("IMPORTS", "Searching for node '" + methodName + "' in loaded program: " + actualImportName);
        
        ProgramNode program = loadedPrograms.get(actualImportName);
        if (program != null && program.unit != null && program.unit.types != null) {
            for (TypeNode type : program.unit.types) {
                DebugSystem.debug("IMPORTS", "  Searching in type: " + type.name);
                
                for (MethodNode node : type.methods) {
                    DebugSystem.debug("IMPORTS", "    Checking node: " + node.methodName);
                    if (node.methodName.equals(methodName)) {
                        DebugSystem.debug("IMPORTS", "    *** FOUND METHOD: " + node.methodName + " ***");
                        return node;
                    }
                }
            }
        }
        
        // Method not found
        DebugSystem.error("IMPORTS", "*** METHOD NOT FOUND: " + qualifiedMethodName + " ***");
        DebugSystem.debug("IMPORTS", "Loaded imports: " + loadedPrograms.keySet());
        DebugSystem.debug("IMPORTS", "Registered imports: " + registeredImports);
        
        return null;
    }

    public void debugFileSystem(String importName) {
        DebugSystem.debug("FILE_SYSTEM", "=== FILE SYSTEM DEBUG for: " + importName + " ===");
        
        String dirPath = importName.replace('.', '/');
        DebugSystem.debug("FILE_SYSTEM", "Looking for unit directory: " + dirPath);
        DebugSystem.debug("FILE_SYSTEM", "Import paths: " + importPaths);
        
        for (String basePath : importPaths) {
            String fullDirPath;
            if (basePath.isEmpty()) {
                fullDirPath = dirPath;
            } else {
                fullDirPath = basePath + "/" + dirPath;
            }
            
            File dir = new File(fullDirPath);
            DebugSystem.debug("FILE_SYSTEM", "Directory: " + dir.getAbsolutePath() + 
                             " [exists: " + dir.exists() + ", isDirectory: " + dir.isDirectory() + "]");
            
            if (dir.exists() && dir.isDirectory()) {
                DebugSystem.debug("FILE_SYSTEM", "Directory contents:");
                String[] files = dir.list();
                if (files != null) {
                    for (String f : files) {
                        DebugSystem.debug("FILE_SYSTEM", "  - " + f + 
                                         " [dir: " + new File(dir, f).isDirectory() + "]");
                    }
                }
            }
            
            // Also check for file directly
            File file = new File(fullDirPath + ".cod");
            DebugSystem.debug("FILE_SYSTEM", "File: " + file.getAbsolutePath() + 
                             " [exists: " + file.exists() + ", isFile: " + file.isFile() + "]");
        }
        DebugSystem.debug("FILE_SYSTEM", "=== END FILE SYSTEM DEBUG ===");
    }

    public void debugImportStatus() {
        DebugSystem.debug("IMPORTS", "=== IMPORT RESOLVER STATUS ===");
        DebugSystem.debug("IMPORTS", "Import paths: " + importPaths);
        DebugSystem.debug("IMPORTS", "Loaded imports: " + importedUnits.keySet());
        DebugSystem.debug("IMPORTS", "Registered imports (lazy): " + registeredImports);
        DebugSystem.debug("IMPORTS", "Registered policies: " + getRegisteredPolicies());

        for (Map.Entry<String, ProgramNode> entry : importedUnits.entrySet()) {
            String unitName = entry.getKey();
            ProgramNode program = entry.getValue();
            DebugSystem.debug("IMPORTS", "Unit: " + unitName);
            if (program != null && program.unit != null) {
                // Show policies
                if (program.unit.policies != null && !program.unit.policies.isEmpty()) {
                    DebugSystem.debug("IMPORTS", "  Policies:");
                    for (PolicyNode policy : program.unit.policies) {
                        // UPDATED: Only show composition, not inheritance
                        DebugSystem.debug("IMPORTS", "    " + policy.name + 
                            (policy.composedPolicies != null && !policy.composedPolicies.isEmpty() ? 
                             " with " + policy.composedPolicies : ""));
                    }
                }
                // Show types
                if (program.unit.types != null) {
                    for (TypeNode type : program.unit.types) {
                        DebugSystem.debug("IMPORTS", "  Type: " + type.name);
                        for (MethodNode node : type.methods) {
                            DebugSystem.debug(
                                "IMPORTS",
                                "    Method: "
                                    + node.methodName
                                    + " (params: "
                                    + node.parameters.size()
                                    + ")");
                        }
                    }
                }
            }
        }
        DebugSystem.debug("IMPORTS", "=== END IMPORT STATUS ===");
    }

    public void preloadImport(String qualifiedName, ProgramNode program) {
        importedUnits.put(qualifiedName, program);
        loadedPrograms.put(qualifiedName, program);
        preloadedImports.put(qualifiedName, program);
        DebugSystem.debug("IMPORTS", "Pre-loaded import into resolver: " + qualifiedName);
        
        // Register policies from preloaded import
        if (program.unit != null && program.unit.policies != null) {
            for (PolicyNode policy : program.unit.policies) {
                String qualifiedPolicyName = program.unit.name + "." + policy.name;
                registerPolicy(qualifiedPolicyName, policy);
                policyToUnitMap.put(policy.name, program.unit.name);
            }
        }
    }

    public Set<String> getLoadedImports() {
        return importedUnits.keySet();
    }
    
    public Set<String> getRegisteredImports() {
        return new HashSet<String>(registeredImports);
    }
}