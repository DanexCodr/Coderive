package cod.semantic;

import cod.ast.nodes.*;

import cod.lexer.MainLexer;
import cod.parser.MainParser;

import cod.debug.DebugSystem;
import java.util.*;
import java.io.*;

public class ImportResolver {
    private Map<String, ProgramNode> importedUnits = new HashMap<>();
    private Map<String, ProgramNode> loadedPrograms = new HashMap<>();
    private Map<String, ProgramNode> preloadedImports = new HashMap<>();
    private Set<String> registeredImports = new HashSet<>();
    private List<String> importPaths = new ArrayList<>();

    public ImportResolver() {
        // Initialize with default import paths
        importPaths.add(".");
        importPaths.add("./lib");
        importPaths.add("./imports");
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
        
        // Debug file system first
        debugFileSystem(importName);
        
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
            return program;
        }
        
        List<String> attemptedPaths = new ArrayList<>();
        ProgramNode program = null;
        
        // Try different file paths and extensions
        String[] basePaths = {
            "",  // current directory
            "cod/",
            "src/cod/",
            "../cod/",
            "/storage/emulated/0/JavaNIDE/Programming-Language/Coderive/executables/"
        };
        
        String[] extensions = {
            ".cod",
            ".txt",
            ""
        };
        
        // Convert import name to file path (e.g., "cod.Sys" -> "cod/Sys")
        String filePath = importName.replace('.', '/');
        
        for (String basePath : basePaths) {
            for (String extension : extensions) {
                String fullPath = basePath + filePath + extension;
                attemptedPaths.add(fullPath);
                
                try {
                    DebugSystem.debug("IMPORTS", "Trying path: " + fullPath);
                    program = loadImportFromFile(fullPath);
                    if (program != null) {
                        DebugSystem.debug("IMPORTS", "Successfully loaded import from: " + fullPath);
                        loadedPrograms.put(importName, program);
                        importedUnits.put(importName, program); // Also add to importedUnits for compatibility
                        return program;
                    }
                } catch (Exception e) {
                    DebugSystem.debug("IMPORTS", "Failed to load from " + fullPath + ": " + e.getMessage());
                    // Continue to next path
                }
            }
        }
        
        // UPDATED: Better error message but still throw
        if (program == null) {
            String error = "Import not found: " + importName + 
                          " (searched: " + attemptedPaths + ")";
            DebugSystem.error("IMPORTS", error);
            throw new RuntimeException(error);
        }
        
        return program;
    }

    private ProgramNode loadImportFromFile(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        
        DebugSystem.debug("IMPORTS", "Loading import from file: " + filePath);
        
        try {
            // Read the file content
            StringBuilder content = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
            
            DebugSystem.debug("IMPORTS", "File content length: " + content.length() + " characters");
            
            // Use the SAME MANUAL parser that we use for the main file
            MainLexer lexer = new MainLexer(content.toString());
            List<MainLexer.Token> tokens = lexer.tokenize();
            
            DebugSystem.debug("IMPORTS", "Generated " + tokens.size() + " tokens");
            
            MainParser parser = new MainParser(tokens);
            ProgramNode program = parser.parseProgram();
            
            DebugSystem.debug("IMPORTS", "Successfully parsed import file using manual parser: " + filePath);
            return program;

        } catch (Exception e) {
            DebugSystem.error("IMPORTS", "Failed to parse import file: " + filePath + " - " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to parse import file: " + filePath + " - " + e.getMessage(), e);
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
        
        String filePath = importName.replace('.', '/') + ".cod";
        DebugSystem.debug("FILE_SYSTEM", "Looking for: " + filePath);
        DebugSystem.debug("FILE_SYSTEM", "Import paths: " + importPaths);
        
        for (String basePath : importPaths) {
            File file = new File(basePath, filePath);
            DebugSystem.debug("FILE_SYSTEM", "Path: " + file.getAbsolutePath() + 
                             " [exists: " + file.exists() + ", isFile: " + file.isFile() + "]");
            
            // Also check the directory
            File dir = file.getParentFile();
            if (dir != null && dir.exists()) {
                DebugSystem.debug("FILE_SYSTEM", "Directory contents of " + dir.getAbsolutePath() + ":");
                String[] files = dir.list();
                if (files != null) {
                    for (String f : files) {
                        DebugSystem.debug("FILE_SYSTEM", "  - " + f);
                    }
                }
            }
        }
        DebugSystem.debug("FILE_SYSTEM", "=== END FILE SYSTEM DEBUG ===");
    }

    public void debugImportStatus() {
        DebugSystem.debug("IMPORTS", "=== IMPORT RESOLVER STATUS ===");
        DebugSystem.debug("IMPORTS", "Import paths: " + importPaths);
        DebugSystem.debug("IMPORTS", "Loaded imports: " + importedUnits.keySet());
        DebugSystem.debug("IMPORTS", "Registered imports (lazy): " + registeredImports);

        for (Map.Entry<String, ProgramNode> entry : importedUnits.entrySet()) {
            String unitName = entry.getKey();
            ProgramNode program = entry.getValue();
            DebugSystem.debug("IMPORTS", "Unit: " + unitName);
            if (program != null && program.unit != null && program.unit.types != null) {
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
        DebugSystem.debug("IMPORTS", "=== END IMPORT STATUS ===");
    }

    public void preloadImport(String qualifiedName, ProgramNode program) {
        importedUnits.put(qualifiedName, program);
        loadedPrograms.put(qualifiedName, program);
        preloadedImports.put(qualifiedName, program);
        DebugSystem.debug("IMPORTS", "Pre-loaded import into resolver: " + qualifiedName);
    }

    public Set<String> getLoadedImports() {
        return importedUnits.keySet();
    }
    
    public Set<String> getRegisteredImports() {
        return new HashSet<>(registeredImports);
    }
}