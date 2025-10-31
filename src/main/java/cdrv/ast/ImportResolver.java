package cdrv.ast;

import cdrv.ast.nodes.*;
import cdrv.debug.DebugSystem;
import java.util.*;
import java.io.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

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

    private String extractImportName(String qualifiedMethodName) {
        int lastDot = qualifiedMethodName.lastIndexOf('.');
        if (lastDot > 0) {
            return qualifiedMethodName.substring(0, lastDot);
        }
        return qualifiedMethodName;
    }

    private String extractMethodName(String qualifiedMethodName) {
        int lastDot = qualifiedMethodName.lastIndexOf('.');
        if (lastDot > 0) {
            return qualifiedMethodName.substring(lastDot + 1);
        }
        return qualifiedMethodName; // No dot, so it's just the method name
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
            "cdrv/",
            "src/cdrv/",
            "../cdrv/"
        };
        
        String[] extensions = {
            ".cdrv",
            ".txt",
            ""
        };
        
        // Convert import name to file path (e.g., "cdrv.Math" -> "cdrv/Math")
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
                    DebugSystem.trace("IMPORTS", "Failed to load from " + fullPath + ": " + e.getMessage());
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
            InputStream is = new FileInputStream(file);
            ANTLRInputStream input = new ANTLRInputStream(is);
            CoderiveLexer lexer = new CoderiveLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            CoderiveParser parser = new CoderiveParser(tokens);

            // Add error listener for better error reporting
            parser.removeErrorListeners();
            parser.addErrorListener(
                    new BaseErrorListener() {
                        @Override
                        public void syntaxError(
                                Recognizer<?, ?> recognizer,
                                Object offendingSymbol,
                                int line,
                                int charPositionInLine,
                                String msg,
                                RecognitionException e) {
                            DebugSystem.error(
                                    "PARSER",
                                    "Syntax error in import at line "
                                            + line
                                            + ":"
                                            + charPositionInLine
                                            + " - "
                                            + msg);
                            throw new RuntimeException(
                                    "Syntax error in import at line "
                                            + line
                                            + ":"
                                            + charPositionInLine
                                            + " - "
                                            + msg);
                        }
                    });

            CoderiveParser.ProgramContext programContext = parser.program();
            ASTBuilder builder = new ASTBuilder();
            ProgramNode program = builder.build(programContext);

            DebugSystem.debug("IMPORTS", "Successfully parsed import file: " + filePath);
            return program;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to parse import file: " + filePath + " - " + e.getMessage(), e);
        }
    }

    private String findImportFile(String qualifiedName) {
        String fileName = qualifiedName.replace('.', '/') + ".cdrv";
        DebugSystem.debug("IMPORTS", "Looking for file: " + fileName);
        DebugSystem.debug("IMPORTS", "Search paths: " + importPaths);

        for (String basePath : importPaths) {
            File file = new File(basePath, fileName);
            DebugSystem.debug(
                    "IMPORTS",
                    "Checking path: "
                            + file.getAbsolutePath()
                            + " [exists: "
                            + file.exists()
                            + "]");
            if (file.exists() && file.isFile()) {
                DebugSystem.debug("IMPORTS", "Found file at: " + file.getAbsolutePath());
                return file.getAbsolutePath();
            }

            // Also try without subdirectory for top-level files
            file = new File(basePath, qualifiedName + ".cdrv");
            DebugSystem.debug(
                    "IMPORTS",
                    "Checking alternative path: "
                            + file.getAbsolutePath()
                            + " [exists: "
                            + file.exists()
                            + "]");
            if (file.exists() && file.isFile()) {
                DebugSystem.debug("IMPORTS", "Found file at: " + file.getAbsolutePath());
                return file.getAbsolutePath();
            }
        }

        DebugSystem.warn("IMPORTS", "Import file not found for: " + qualifiedName);
        return null;
    }

    // In ImportResolver.java - Update the findMethod method

public MethodNode findMethod(String qualifiedMethodName) {
    DebugSystem.debug("IMPORTS", "findMethod called for: " + qualifiedMethodName);
    
    // Handle the case where qualifiedMethodName is exactly "cdrv.Math.sqrt"
    String importName = extractImportName(qualifiedMethodName);
    String methodName = extractMethodName(qualifiedMethodName);
    
    DebugSystem.debug("IMPORTS", "Extracted import: '" + importName + "', method: '" + methodName + "'");
    
    // FIRST: Try to resolve the import if it's registered but not loaded
    if (registeredImports.contains(importName) && !loadedPrograms.containsKey(importName)) {
        DebugSystem.debug("IMPORTS", "Attempting lazy resolution for: " + importName);
        try {
            ProgramNode program = resolveImport(importName);
            if (program != null) {
                loadedPrograms.put(importName, program);
                importedUnits.put(importName, program);
                registeredImports.remove(importName);
                DebugSystem.debug("IMPORTS", "Successfully loaded import: " + importName);
            }
        } catch (Exception e) {
            DebugSystem.error("IMPORTS", "Failed to load import " + importName + ": " + e.getMessage());
            // Don't remove from registered imports so we can try again with different paths
        }
    }
    
    // SECOND: If the import is still not loaded, try direct file loading
    if (!loadedPrograms.containsKey(importName)) {
        DebugSystem.debug("IMPORTS", "Import not loaded, trying direct file resolution: " + importName);
        try {
            // Convert import name to file path
            String filePath = importName.replace('.', '/') + ".cdrv";
            DebugSystem.debug("IMPORTS", "Looking for file: " + filePath);
            
            // Try all import paths
            for (String basePath : importPaths) {
                File file = new File(basePath, filePath);
                DebugSystem.debug("IMPORTS", "Checking: " + file.getAbsolutePath());
                
                if (file.exists() && file.isFile()) {
                    DebugSystem.debug("IMPORTS", "Found file, attempting to load: " + file.getAbsolutePath());
                    try {
                        ProgramNode program = loadImportFromFile(file.getAbsolutePath());
                        if (program != null) {
                            loadedPrograms.put(importName, program);
                            importedUnits.put(importName, program);
                            DebugSystem.debug("IMPORTS", "Successfully loaded from file: " + importName);
                            break;
                        }
                    } catch (Exception e) {
                        DebugSystem.error("IMPORTS", "Error loading file: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            DebugSystem.error("IMPORTS", "Error in direct file resolution: " + e.getMessage());
        }
    }
    
    // THIRD: Search through all loaded programs for the method
    DebugSystem.debug("IMPORTS", "Searching for method '" + methodName + "' in " + loadedPrograms.size() + " loaded programs");
    
    for (Map.Entry<String, ProgramNode> entry : loadedPrograms.entrySet()) {
        String loadedImportName = entry.getKey();
        ProgramNode program = entry.getValue();
        
        DebugSystem.debug("IMPORTS", "Checking program: " + loadedImportName);
        
        // Check if this program contains our import (exact match or hierarchical)
        boolean importMatches = loadedImportName.equals(importName) || 
                               importName.startsWith(loadedImportName + ".") ||
                               loadedImportName.startsWith(importName + ".");
        
        if (importMatches) {
            DebugSystem.debug("IMPORTS", "Import matches! Searching for method: " + methodName);
            
            if (program.unit != null && program.unit.types != null) {
                for (TypeNode type : program.unit.types) {
                    DebugSystem.debug("IMPORTS", "  Searching in type: " + type.name);
                    
                    for (MethodNode method : type.methods) {
                        DebugSystem.debug("IMPORTS", "    Checking method: " + method.name);
                        if (method.name.equals(methodName)) {
                            DebugSystem.debug("IMPORTS", "    *** FOUND METHOD: " + method.name + " ***");
                            return method;
                        }
                    }
                }
            }
        }
    }
    
    // FINAL: Method not found - provide detailed diagnostics
    DebugSystem.error("IMPORTS", "*** METHOD NOT FOUND: " + qualifiedMethodName + " ***");
    debugImportStatus();
    
    return null;
}

// Add to ImportResolver.java
public void debugFileSystem(String importName) {
    DebugSystem.debug("FILE_SYSTEM", "=== FILE SYSTEM DEBUG for: " + importName + " ===");
    
    String filePath = importName.replace('.', '/') + ".cdrv";
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
                    for (MethodNode method : type.methods) {
                        DebugSystem.debug(
                                "IMPORTS",
                                "    Method: "
                                        + method.name
                                        + " (params: "
                                        + method.parameters.size()
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