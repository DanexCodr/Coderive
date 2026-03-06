package cod.semantic;

import cod.ast.nodes.*;
import cod.error.InternalError;
import cod.error.ProgramError;
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
    
    private Map<String, PolicyNode> importedPolicies = new HashMap<String, PolicyNode>();
    private Map<String, String> policyToUnitMap = new HashMap<String, String>();
    
    // Import name cache for O(1) lookups
    private Map<String, String> importNameCache = new HashMap<String, String>();
    
    // Type cache for O(1) type lookups
    private Map<String, TypeNode> typeCache = new HashMap<String, TypeNode>();
    
    // Filesystem result cache
    private Map<String, CachedFileResult> fileCache = new HashMap<String, CachedFileResult>();
    
    // File metadata cache (exists, isDirectory, lastModified)
    private Map<String, FileMetadata> fileMetadataCache = new HashMap<String, FileMetadata>();
    
    // Cache hit/miss counters for debugging
    private int fileCacheHits = 0;
    private int fileCacheMisses = 0;
    private int metadataCacheHits = 0;
    private int metadataCacheMisses = 0;
    
    // Cache entry with timestamp
    private static class CachedFileResult {
        final ProgramNode program;
        final long lastModified;
        
        CachedFileResult(ProgramNode program, long lastModified) {
            this.program = program;
            this.lastModified = lastModified;
        }
        
        boolean isValid(File file) {
            return file.exists() && file.lastModified() == lastModified;
        }
    }
    
    // File metadata cache
    private static class FileMetadata {
        final boolean exists;
        final boolean isDirectory;
        final long lastModified;
        
        FileMetadata(boolean exists, boolean isDirectory, long lastModified) {
            this.exists = exists;
            this.isDirectory = isDirectory;
            this.lastModified = lastModified;
        }
        
        boolean isValid(File file) {
            return file.exists() == exists && 
                   file.isDirectory() == isDirectory && 
                   file.lastModified() == lastModified;
        }
    }
    
    public ImportResolver() {
        importPaths.add("src/main");
        DebugSystem.debug("IMPORTS", "Initialized with strict src/main/ path");
    }
    
    // Get file metadata with caching
    private FileMetadata getFileMetadata(File file) {
        String path = file.getAbsolutePath();
        
        // Check cache
        if (fileMetadataCache.containsKey(path)) {
            FileMetadata cached = fileMetadataCache.get(path);
            if (cached.isValid(file)) {
                metadataCacheHits++;
                return cached;
            } else {
                // Stale cache entry
                fileMetadataCache.remove(path);
            }
        }
        
        metadataCacheMisses++;
        
        // Get fresh metadata
        boolean exists = file.exists();
        boolean isDirectory = exists && file.isDirectory();
        long lastModified = exists ? file.lastModified() : 0;
        
        FileMetadata metadata = new FileMetadata(exists, isDirectory, lastModified);
        fileMetadataCache.put(path, metadata);
        
        return metadata;
    }
    
    // Load file with caching
    private ProgramNode loadImportFromFileCached(String filePath) throws Exception {
        if (filePath == null || filePath.isEmpty()) {
            throw new InternalError("loadImportFromFileCached called with null/empty path");
        }
        
        File file = new File(filePath);
        
        // Check metadata first (fast path)
        FileMetadata metadata = getFileMetadata(file);
        if (!metadata.exists) {
            return null;
        }
        if (!metadata.isDirectory) {
            // Check file cache
            if (fileCache.containsKey(filePath)) {
                CachedFileResult cached = fileCache.get(filePath);
                if (cached.isValid(file)) {
                    fileCacheHits++;
                    DebugSystem.debug("IMPORTS_CACHE", "Cache hit for: " + filePath);
                    return cached.program;
                } else {
                    // Stale cache entry
                    fileCache.remove(filePath);
                    DebugSystem.debug("IMPORTS_CACHE", "Cache stale for: " + filePath);
                }
            }
            
            fileCacheMisses++;
            DebugSystem.debug("IMPORTS_CACHE", "Cache miss for: " + filePath);
            
            // Load fresh
            ProgramNode program = loadImportFromFile(filePath);
            if (program != null) {
                fileCache.put(filePath, new CachedFileResult(program, metadata.lastModified));
            }
            return program;
        }
        
        return null; // Is a directory, not a file
    }
    
    // Directory listing with caching
    private File[] listFilesCached(File directory) {

        // Check metadata
        FileMetadata metadata = getFileMetadata(directory);
        if (!metadata.exists || !metadata.isDirectory) {
            return null;
        }
        
        // For directories, we still need to list files (can't cache easily)
        // But we can use the metadata to avoid stat calls on non-existent dirs
        return directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".cod");
            }
        });
    }
    
    public void registerBroadcast(String packageName, String mainClassName) {
        if (packageName == null || packageName.isEmpty()) {
            throw new InternalError("registerBroadcast called with null/empty packageName");
        }
        if (mainClassName == null || mainClassName.isEmpty()) {
            throw new InternalError("registerBroadcast called with null/empty mainClassName");
        }
        
        if (packageBroadcasts.containsKey(packageName)) {
            String existing = packageBroadcasts.get(packageName);
            if (!existing.equals(mainClassName)) {
                throw new ProgramError(
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
    
    public String getBroadcast(String packageName) {
        return packageBroadcasts.get(packageName);
    }
    
    public void clearBroadcasts() {
        packageBroadcasts.clear();
    }
    
    public PolicyNode findPolicy(String qualifiedPolicyName) {
        if (qualifiedPolicyName == null || qualifiedPolicyName.isEmpty()) {
            throw new InternalError("findPolicy called with null/empty name");
        }
        
        DebugSystem.debug("POLICY", "findPolicy called for: " + qualifiedPolicyName);
        
        if (importedPolicies.containsKey(qualifiedPolicyName)) {
            DebugSystem.debug("POLICY", "Policy already loaded: " + qualifiedPolicyName);
            return importedPolicies.get(qualifiedPolicyName);
        }
        
        int lastDot = qualifiedPolicyName.lastIndexOf('.');
        String policyName;
        String importName;
        
        if (lastDot == -1) {
            policyName = qualifiedPolicyName;
            importName = qualifiedPolicyName;
        } else {
            policyName = qualifiedPolicyName.substring(lastDot + 1);
            importName = qualifiedPolicyName.substring(0, lastDot);
        }
        
        DebugSystem.debug("POLICY", "Import part: '" + importName + "', policy: '" + policyName + "'");
        
        String actualImportName = findMatchingImportCached(importName);
        
        if (!loadedPrograms.containsKey(actualImportName)) {
            try {
                DebugSystem.debug("POLICY", "Import not loaded, attempting to load: " + actualImportName);
                ProgramNode program = resolveImport(actualImportName);
                if (program == null) {
                    throw new ProgramError("Failed to load import: " + actualImportName);
                }
            } catch (ProgramError e) {
                throw e;
            } catch (Exception e) {
                throw new InternalError("Unexpected error loading import: " + actualImportName, e);
            }
        }
        
        ProgramNode program = loadedPrograms.get(actualImportName);
        if (program != null && program.unit != null && program.unit.policies != null) {
            for (PolicyNode policy : program.unit.policies) {
                if (policy.name.equals(policyName)) {
                    DebugSystem.debug("POLICY", "Found policy: " + policy.name);
                    importedPolicies.put(qualifiedPolicyName, policy);
                    policyToUnitMap.put(policyName, program.unit.name);
                    return policy;
                }
            }
        }
        
        throw new ProgramError(
            "Policy not found: '" + qualifiedPolicyName + "'\n" +
            "Available policies in import '" + actualImportName + "': " + 
            getPolicyNames(program)
        );
    }
    
    private String getPolicyNames(ProgramNode program) {
        if (program == null || program.unit == null || program.unit.policies == null) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < program.unit.policies.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(program.unit.policies.get(i).name);
        }
        return sb.toString();
    }
    
    public void registerPolicy(String qualifiedName, PolicyNode policy) {
        if (qualifiedName == null || qualifiedName.isEmpty()) {
            throw new InternalError("registerPolicy called with null/empty qualifiedName");
        }
        if (policy == null) {
            throw new InternalError("registerPolicy called with null policy");
        }
        
        importedPolicies.put(qualifiedName, policy);
        DebugSystem.debug("POLICY", "Registered policy: " + qualifiedName);
        
        if (!importedPolicies.containsKey(policy.name)) {
            importedPolicies.put(policy.name, policy);
        }
    }
    
    public String getPolicyUnit(String policyName) {
        return policyToUnitMap.get(policyName);
    }
    
    public Set<String> getRegisteredPolicies() {
        return importedPolicies.keySet();
    }

    public void addImportPath(String path) {
        if (path == null || path.isEmpty()) {
            throw new InternalError("addImportPath called with null/empty path");
        }
        importPaths.add(path);
        DebugSystem.debug("IMPORTS", "Added import path: " + path);
    }
    
    public void registerImport(String importName) {
        if (importName == null || importName.isEmpty()) {
            throw new InternalError("registerImport called with null/empty importName");
        }
        
        if (!registeredImports.contains(importName)) {
            registeredImports.add(importName);
            // Pre-cache the import name mapping
            cacheImportName(importName);
            DebugSystem.debug("IMPORTS", "Registered import (lazy): " + importName);
        }
    }
    
    // Cache import name mapping
    private void cacheImportName(String importName) {
        String[] parts = importName.split("\\.");
        if (parts.length > 0) {
            String lastPart = parts[parts.length - 1];
            importNameCache.put(lastPart, importName);
            
            // Also cache partial paths for nested lookups
            StringBuilder partial = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) partial.append(".");
                partial.append(parts[i]);
                importNameCache.put(partial.toString(), importName);
            }
        }
    }
    
    // O(1) import name lookup with cache
    private String findMatchingImportCached(String calledImport) {
        // Direct cache hit
        if (importNameCache.containsKey(calledImport)) {
            String cached = importNameCache.get(calledImport);
            DebugSystem.debug("IMPORTS", "Cache hit for import: " + calledImport + " -> " + cached);
            return cached;
        }
        
        // Check loaded programs (these should be in cache, but double-check)
        for (String loadedImport : loadedPrograms.keySet()) {
            if (loadedImport.endsWith("." + calledImport) || loadedImport.equals(calledImport)) {
                importNameCache.put(calledImport, loadedImport);
                return loadedImport;
            }
        }
        
        // Check registered imports (these should be in cache, but double-check)
        for (String registeredImport : registeredImports) {
            if (registeredImport.endsWith("." + calledImport) || registeredImport.equals(calledImport)) {
                importNameCache.put(calledImport, registeredImport);
                return registeredImport;
            }
        }
        
        return calledImport;
    }

    public ProgramNode resolveImport(String importName) throws Exception {
        if (importName == null || importName.isEmpty()) {
            throw new InternalError("resolveImport called with null/empty importName");
        }
        
        DebugSystem.debug("IMPORTS", "resolveImport called for: " + importName);
        
        if (loadedPrograms.containsKey(importName)) {
            DebugSystem.debug("IMPORTS", "Import already loaded: " + importName);
            return loadedPrograms.get(importName);
        }
        
        if (preloadedImports.containsKey(importName)) {
            DebugSystem.debug("IMPORTS", "Using preloaded import: " + importName);
            ProgramNode program = preloadedImports.get(importName);
            if (program == null) {
                throw new InternalError("Preloaded import entry is null for: " + importName);
            }
            
            loadedPrograms.put(importName, program);
            importedUnits.put(importName, program);
            
            // Cache this import name
            cacheImportName(importName);
            
            if (program.unit != null && program.unit.policies != null) {
                for (PolicyNode policy : program.unit.policies) {
                    String qualifiedName = program.unit.name + "." + policy.name;
                    registerPolicy(qualifiedName, policy);
                    policyToUnitMap.put(policy.name, program.unit.name);
                    DebugSystem.debug("IMPORTS", "Registered policy from preloaded import: " + qualifiedName);
                }
            }
            
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
        
        String dirPath = importName.replace('.', '/');
        List<String> attemptedPaths = new ArrayList<String>();
        ProgramNode program = null;
        
        for (String basePath : importPaths) {
            String fullDirPath;
            if (basePath.isEmpty()) {
                fullDirPath = dirPath;
            } else {
                fullDirPath = basePath + "/" + dirPath;
            }
            
            File directory = new File(fullDirPath);
            
            // Use cached metadata for directory check
            FileMetadata dirMetadata = getFileMetadata(directory);
            if (dirMetadata.exists && dirMetadata.isDirectory) {
                // Use cached directory listing
                File[] files = listFilesCached(directory);
                
                if (files != null && files.length > 0) {
                    if (files.length > 1) {
                        DebugSystem.warn("IMPORTS", 
                            "Multiple .cod files in directory " + fullDirPath + 
                            " - using first found: " + files[0].getName());
                    }
                    
                    for (File file : files) {
                        String filePath = file.getAbsolutePath();
                        attemptedPaths.add(filePath);
                        
                        DebugSystem.debug("IMPORTS", "Trying file in unit directory: " + filePath);
                        
                        try {
                            // Use cached file loading
                            program = loadImportFromFileCached(filePath);
                            if (program != null) {
                                validateUnitAgainstDirectory(importName, program, filePath);
                                
                                DebugSystem.debug("IMPORTS", "Successfully loaded import from: " + filePath);
                                loadedPrograms.put(importName, program);
                                importedUnits.put(importName, program);
                                
                                // Cache this import name
                                cacheImportName(importName);
                                
                                registerPoliciesAndBroadcast(program, importName);
                                
                                return program;
                            }
                        } catch (ProgramError e) {
                            throw e;
                        } catch (Exception e) {
                            DebugSystem.debug("IMPORTS", "Failed to load from " + filePath + ": " + e.getMessage());
                        }
                    }
                }
            }
            
            String filePath = fullDirPath + ".cod";
            attemptedPaths.add(filePath);
            
            DebugSystem.debug("IMPORTS", "Trying as file: " + filePath);
            
            try {
                // Use cached file loading
                program = loadImportFromFileCached(filePath);
                if (program != null) {
                    validateUnitAgainstDirectory(importName, program, filePath);
                    
                    DebugSystem.debug("IMPORTS", "Successfully loaded import from: " + filePath);
                    loadedPrograms.put(importName, program);
                    importedUnits.put(importName, program);
                    
                    // Cache this import name
                    cacheImportName(importName);
                    
                    registerPoliciesAndBroadcast(program, importName);
                    
                    return program;
                }
            } catch (ProgramError e) {
                throw e;
            } catch (Exception e) {
                DebugSystem.debug("IMPORTS", "Failed to load from " + filePath + ": " + e.getMessage());
            }
        }
        
        StringBuilder errorMsg = new StringBuilder();
        errorMsg.append("Import not found: ").append(importName).append("\n");
        errorMsg.append("Searched in:\n");
        for (String path : attemptedPaths) {
            errorMsg.append("  - ").append(path).append("\n");
        }
        errorMsg.append("\nExpected structure: src/main/").append(dirPath).append("/ (with .cod files)\n");
        errorMsg.append("Or file: src/main/").append(dirPath).append(".cod");
        
        throw new ProgramError(errorMsg.toString());
    }
    
    private void registerPoliciesAndBroadcast(ProgramNode program, String importName) {
        if (program == null || program.unit == null) {
            throw new InternalError("registerPoliciesAndBroadcast called with null program/unit");
        }
        
        if (program.unit.policies != null) {
            for (PolicyNode policy : program.unit.policies) {
                String qualifiedName = program.unit.name + "." + policy.name;
                registerPolicy(qualifiedName, policy);
                policyToUnitMap.put(policy.name, program.unit.name);
                DebugSystem.debug("IMPORTS", "Registered policy from import: " + qualifiedName);
            }
        }
        
        if (program.unit.mainClassName != null && !program.unit.mainClassName.isEmpty()) {
            String packageName = program.unit.name;
            String mainClassName = program.unit.mainClassName;
            
            DebugSystem.debug("BROADCAST", "Registering broadcast from import '" + 
                importName + "': package '" + packageName + "' (main: " + mainClassName + ")");
            
            registerBroadcast(packageName, mainClassName);
        }
    }

    private void validateUnitAgainstDirectory(String expectedUnit, ProgramNode program, String filePath) {
        if (program == null) {
            throw new InternalError("validateUnitAgainstDirectory called with null program");
        }
        if (program.unit == null || program.unit.name == null) {
            throw new ProgramError(
                "Program has no unit declaration in file: " + filePath + "\n" +
                "Add: unit " + expectedUnit + " at the top of the file."
            );
        }
        
        String declaredUnit = program.unit.name;
        
        if (!declaredUnit.equals(expectedUnit)) {
            String calculatedUnit = calculateUnitFromFilePath(filePath);
            
            if (!declaredUnit.equals(calculatedUnit)) {
                throw new ProgramError(
                    "Unit declaration mismatch:\n" +
                    "  File: " + filePath + "\n" +
                    "  Declared unit: " + declaredUnit + "\n" +
                    "  Expected unit: " + calculatedUnit + " (based on file location)\n" +
                    "  Import requested: " + expectedUnit + "\n" +
                    "\nUnit must match directory path relative to src/main/"
                );
            }
        }
    }
    
    private String calculateUnitFromFilePath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            throw new InternalError("calculateUnitFromFilePath called with null/empty path");
        }
        
        String normalized = filePath.replace('\\', '/');
        
        String srcMain = "src/main/";
        int srcMainIndex = normalized.indexOf(srcMain);
        
        if (srcMainIndex == -1) {
            for (String importPath : importPaths) {
                if (!importPath.isEmpty() && normalized.contains(importPath + "/")) {
                    int importPathIndex = normalized.indexOf(importPath + "/");
                    if (importPathIndex != -1) {
                        String relative = normalized.substring(importPathIndex + importPath.length() + 1);
                        return calculateUnitFromRelativePath(relative);
                    }
                }
            }
            throw new ProgramError("File not under any import path: " + filePath);
        }
        
        String relative = normalized.substring(srcMainIndex + srcMain.length());
        return calculateUnitFromRelativePath(relative);
    }
    
    private String calculateUnitFromRelativePath(String relativePath) {
        if (relativePath == null) {
            throw new InternalError("calculateUnitFromRelativePath called with null path");
        }
        
        if (relativePath.endsWith(".cod")) {
            relativePath = relativePath.substring(0, relativePath.length() - 4);
        }
        
        String[] parts = relativePath.split("/");
        List<String> unitParts = new ArrayList<String>();
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;
            
            boolean looksLikeFileName = part.length() > 0 && Character.isUpperCase(part.charAt(0));
            
            if (i == parts.length - 1 && looksLikeFileName) {
                continue;
            }
            
            unitParts.add(part);
        }
        
        if (unitParts.isEmpty()) {
            return "";
        }
        
        StringBuilder unitName = new StringBuilder();
        for (int i = 0; i < unitParts.size(); i++) {
            if (i > 0) unitName.append(".");
            unitName.append(unitParts.get(i));
        }
        
        return unitName.toString();
    }
    
    private ProgramNode loadImportFromFile(String filePath) throws Exception {
        if (filePath == null || filePath.isEmpty()) {
            throw new InternalError("loadImportFromFile called with null/empty path");
        }
        
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }
        if (!file.isFile()) {
            throw new ProgramError("Import path is not a file: " + filePath);
        }
        
        DebugSystem.debug("IMPORTS", "Loading import from file: " + filePath);
        
        BufferedReader reader = null;
        try {
            StringBuilder content = new StringBuilder();
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
            
            DebugSystem.debug("IMPORTS", "File content length: " + content.length() + " characters");
            
            MainLexer lexer = new MainLexer(content.toString());
            List<Token> tokens = lexer.tokenize();
            
            DebugSystem.debug("IMPORTS", "Generated " + tokens.size() + " tokens");
            
            MainParser parser = new MainParser(tokens);
            ProgramNode program = parser.parseProgram();
            
            if (program == null) {
                throw new InternalError("Parser returned null program for: " + filePath);
            }
            
            DebugSystem.debug("IMPORTS", "Successfully parsed import file: " + filePath);
            return program;

        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Failed to parse import file: " + filePath, e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    DebugSystem.debug("IMPORTS", "Failed to close reader: " + e.getMessage());
                }
            }
        }
    }

    // O(1) type lookup with cache
    public TypeNode findType(String qualifiedTypeName) {
        if (qualifiedTypeName == null || qualifiedTypeName.isEmpty()) {
            throw new InternalError("findType called with null/empty name");
        }
        
        DebugSystem.debug("IMPORTS", "findType called for: " + qualifiedTypeName);
        
        // Check cache first
        if (typeCache.containsKey(qualifiedTypeName)) {
            DebugSystem.debug("IMPORTS", "Type cache hit: " + qualifiedTypeName);
            return typeCache.get(qualifiedTypeName);
        }
        
        int lastDot = qualifiedTypeName.lastIndexOf('.');
        if (lastDot == -1) {
            DebugSystem.debug("IMPORTS", "Simple type name, searching all imports");
            TypeNode found = findTypeByName(qualifiedTypeName);
            if (found == null) {
                throw new ProgramError("Type not found: " + qualifiedTypeName);
            }
            // Cache the result
            typeCache.put(qualifiedTypeName, found);
            return found;
        }
        
        String typeName = qualifiedTypeName.substring(lastDot + 1);
        String importPart = qualifiedTypeName.substring(0, lastDot);
        
        DebugSystem.debug("IMPORTS", "Import part: '" + importPart + "', type: '" + typeName + "'");
        
        String actualImportName = findMatchingImportCached(importPart);
        
        DebugSystem.debug("IMPORTS", "Final import to resolve: '" + actualImportName + "', type: '" + typeName + "'");
        
        if (!loadedPrograms.containsKey(actualImportName)) {
            DebugSystem.debug("IMPORTS", "Import not loaded, trying to resolve: " + actualImportName);
            try {
                ProgramNode program = resolveImport(actualImportName);
                if (program == null) {
                    throw new ProgramError("Failed to resolve import: " + actualImportName);
                }
            } catch (ProgramError e) {
                throw e;
            } catch (Exception e) {
                throw new InternalError("Unexpected error resolving import: " + actualImportName, e);
            }
        }
        
        DebugSystem.debug("IMPORTS", "Searching for type '" + typeName + "' in loaded program: " + actualImportName);
        
        ProgramNode program = loadedPrograms.get(actualImportName);
        if (program != null && program.unit != null && program.unit.types != null) {
            for (TypeNode type : program.unit.types) {
                DebugSystem.debug("IMPORTS", "  Checking type: " + type.name);
                if (type.name.equals(typeName)) {
                    DebugSystem.debug("IMPORTS", "    *** FOUND TYPE: " + type.name + " ***");
                    // Cache the result
                    typeCache.put(qualifiedTypeName, type);
                    typeCache.put(typeName, type); // Also cache by simple name if unique
                    return type;
                }
            }
        }
        
        throw new ProgramError(
            "Type not found: '" + qualifiedTypeName + "'\n" +
            "Available types in import '" + actualImportName + "': " + 
            getTypeNames(program)
        );
    }
    
    private String getTypeNames(ProgramNode program) {
        if (program == null || program.unit == null || program.unit.types == null) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < program.unit.types.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(program.unit.types.get(i).name);
        }
        return sb.toString();
    }

    private TypeNode findTypeByName(String typeName) {
        // Check cache first
        if (typeCache.containsKey(typeName)) {
            return typeCache.get(typeName);
        }
        
        for (Map.Entry<String, ProgramNode> entry : loadedPrograms.entrySet()) {
            ProgramNode program = entry.getValue();
            if (program != null && program.unit != null && program.unit.types != null) {
                for (TypeNode type : program.unit.types) {
                    if (type.name.equals(typeName)) {
                        typeCache.put(typeName, type);
                        return type;
                    }
                }
            }
        }
        
        for (String importName : registeredImports) {
            if (importName.endsWith("." + typeName)) {
                try {
                    ProgramNode program = resolveImport(importName);
                    if (program != null && program.unit != null && program.unit.types != null) {
                        for (TypeNode type : program.unit.types) {
                            if (type.name.equals(typeName)) {
                                typeCache.put(typeName, type);
                                return type;
                            }
                        }
                    }
                } catch (Exception e) {
                    DebugSystem.debug("IMPORTS", "Failed to load " + importName + " while searching for " + typeName);
                }
            }
        }
        
        return null;
    }

    // Clear cache method for testing/refresh
    public void clearCache() {
        importNameCache.clear();
        typeCache.clear();
        fileCache.clear();
        fileMetadataCache.clear();
        fileCacheHits = 0;
        fileCacheMisses = 0;
        metadataCacheHits = 0;
        metadataCacheMisses = 0;
        DebugSystem.debug("IMPORTS_CACHE", "All caches cleared");
    }
    
    // Get cache statistics
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<String, Object>();
        stats.put("importNameCache", importNameCache.size());
        stats.put("typeCache", typeCache.size());
        stats.put("fileCache", fileCache.size());
        stats.put("fileMetadataCache", fileMetadataCache.size());
        stats.put("fileCacheHits", fileCacheHits);
        stats.put("fileCacheMisses", fileCacheMisses);
        stats.put("metadataCacheHits", metadataCacheHits);
        stats.put("metadataCacheMisses", metadataCacheMisses);
        
        double hitRate = (fileCacheHits + metadataCacheHits) / 
            (double)(fileCacheHits + fileCacheMisses + metadataCacheHits + metadataCacheMisses + 1) * 100;
        stats.put("cacheHitRate", String.format("%.1f%%", hitRate));
        
        return stats;
    }

    public MethodNode findMethod(String qualifiedMethodName) {
        if (qualifiedMethodName == null || qualifiedMethodName.isEmpty()) {
            throw new InternalError("findMethod called with null/empty name");
        }
        
        DebugSystem.debug("IMPORTS", "findMethod called for: " + qualifiedMethodName);
        
        int lastDot = qualifiedMethodName.lastIndexOf('.');
        if (lastDot == -1) {
            DebugSystem.debug("IMPORTS", "No dots in method name, not an imported method");
            return null;
        }
        
        String methodName = qualifiedMethodName.substring(lastDot + 1);
        String calledImport = qualifiedMethodName.substring(0, lastDot);
        
        DebugSystem.debug("IMPORTS", "Called import part: '" + calledImport + "', method: '" + methodName + "'");
        
        String actualImportName = findMatchingImportCached(calledImport);
        
        DebugSystem.debug("IMPORTS", "Final import to resolve: '" + actualImportName + "', method: '" + methodName + "'");
        
        if (!loadedPrograms.containsKey(actualImportName)) {
            DebugSystem.debug("IMPORTS", "Import not loaded, trying to resolve: " + actualImportName);
            try {
                ProgramNode program = resolveImport(actualImportName);
                if (program == null) {
                    throw new ProgramError("Failed to resolve import: " + actualImportName);
                }
            } catch (ProgramError e) {
                throw e;
            } catch (Exception e) {
                throw new InternalError("Unexpected error resolving import: " + actualImportName, e);
            }
        }
        
        DebugSystem.debug("IMPORTS", "Searching for method '" + methodName + "' in loaded program: " + actualImportName);
        
        ProgramNode program = loadedPrograms.get(actualImportName);
        if (program != null && program.unit != null && program.unit.types != null) {
            for (TypeNode type : program.unit.types) {
                DebugSystem.debug("IMPORTS", "  Searching in type: " + type.name);
                
                for (MethodNode method : type.methods) {
                    DebugSystem.debug("IMPORTS", "    Checking method: " + method.methodName);
                    if (method.methodName.equals(methodName)) {
                        DebugSystem.debug("IMPORTS", "    *** FOUND METHOD: " + method.methodName + " ***");
                        return method;
                    }
                }
            }
        }
        
        throw new ProgramError(
            "Method not found: '" + qualifiedMethodName + "'\n" +
            "Available methods in import '" + actualImportName + "': " + 
            getMethodNames(program)
        );
    }
    
    private String getMethodNames(ProgramNode program) {
        if (program == null || program.unit == null || program.unit.types == null) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (TypeNode type : program.unit.types) {
            for (MethodNode method : type.methods) {
                if (!first) sb.append(", ");
                sb.append(type.name).append(".").append(method.methodName);
                first = false;
            }
        }
        return sb.toString();
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
        DebugSystem.debug("IMPORTS", "Import name cache size: " + importNameCache.size());
        DebugSystem.debug("IMPORTS", "Type cache size: " + typeCache.size());
        DebugSystem.debug("IMPORTS", "File cache size: " + fileCache.size());
        DebugSystem.debug("IMPORTS", "File metadata cache size: " + fileMetadataCache.size());
        
        Map<String, Object> stats = getCacheStats();
        DebugSystem.debug("IMPORTS", "File cache hits: " + stats.get("fileCacheHits"));
        DebugSystem.debug("IMPORTS", "File cache misses: " + stats.get("fileCacheMisses"));
        DebugSystem.debug("IMPORTS", "Metadata cache hits: " + stats.get("metadataCacheHits"));
        DebugSystem.debug("IMPORTS", "Metadata cache misses: " + stats.get("metadataCacheMisses"));
        DebugSystem.debug("IMPORTS", "Cache hit rate: " + stats.get("cacheHitRate"));

        for (Map.Entry<String, ProgramNode> entry : importedUnits.entrySet()) {
            String unitName = entry.getKey();
            ProgramNode program = entry.getValue();
            DebugSystem.debug("IMPORTS", "Unit: " + unitName);
            if (program != null && program.unit != null) {
                if (program.unit.policies != null && !program.unit.policies.isEmpty()) {
                    DebugSystem.debug("IMPORTS", "  Policies:");
                    for (PolicyNode policy : program.unit.policies) {
                        DebugSystem.debug("IMPORTS", "    " + policy.name + 
                            (policy.composedPolicies != null && !policy.composedPolicies.isEmpty() ? 
                             " with " + policy.composedPolicies : ""));
                    }
                }
                if (program.unit.types != null) {
                    for (TypeNode type : program.unit.types) {
                        DebugSystem.debug("IMPORTS", "  Type: " + type.name);
                        for (MethodNode method : type.methods) {
                            DebugSystem.debug(
                                "IMPORTS",
                                "    Method: "
                                    + method.methodName
                                    + " (params: "
                                    + method.parameters.size()
                                    + ")");
                        }
                    }
                }
            }
        }
        DebugSystem.debug("IMPORTS", "=== END IMPORT STATUS ===");
    }

    public void preloadImport(String qualifiedName, ProgramNode program) {
        if (qualifiedName == null || qualifiedName.isEmpty()) {
            throw new InternalError("preloadImport called with null/empty qualifiedName");
        }
        if (program == null) {
            throw new InternalError("preloadImport called with null program");
        }
        
        importedUnits.put(qualifiedName, program);
        loadedPrograms.put(qualifiedName, program);
        preloadedImports.put(qualifiedName, program);
        
        // Cache this import name
        cacheImportName(qualifiedName);
        
        DebugSystem.debug("IMPORTS", "Pre-loaded import into resolver: " + qualifiedName);
        
        if (program.unit != null && program.unit.policies != null) {
            for (PolicyNode policy : program.unit.policies) {
                String qualifiedPolicyName = program.unit.name + "." + policy.name;
                registerPolicy(qualifiedPolicyName, policy);
                policyToUnitMap.put(policy.name, program.unit.name);
            }
        }
        
        // Pre-cache types from this import
        if (program.unit != null && program.unit.types != null) {
            for (TypeNode type : program.unit.types) {
                String qualifiedTypeName = program.unit.name + "." + type.name;
                typeCache.put(qualifiedTypeName, type);
                typeCache.put(type.name, type); // Also cache by simple name
            }
        }
    }

    public Map<String, ProgramNode> getLoadedPrograms() {
        return loadedPrograms;
    }

    public Set<String> getLoadedImports() {
        return importedUnits.keySet();
    }
    
    public Set<String> getRegisteredImports() {
        return new HashSet<String>(registeredImports);
    }
}