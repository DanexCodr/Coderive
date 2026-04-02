package cod.semantic;

import cod.ast.ASTFactory;
import cod.ast.nodes.*;
import cod.error.InternalError;
import cod.error.ProgramError;
import cod.lexer.*;
import cod.parser.MainParser;
import cod.debug.DebugSystem;
import cod.util.Index;
import cod.util.BytecodeManager;

import java.util.*;
import java.io.*;

public class ImportResolver {
    private Map<String, ProgramNode> importedUnits = new HashMap<String, ProgramNode>();
    private Map<String, ProgramNode> loadedPrograms = new HashMap<String, ProgramNode>();
    private Map<String, ProgramNode> preloadedImports = new HashMap<String, ProgramNode>();
    private Set<String> registeredImports = new HashSet<String>();
    private List<String> methodImportSpecs = new ArrayList<String>();
    private Set<String> wildcardEverythingUnits = new HashSet<String>();
    private Set<String> wildcardClassUnits = new HashSet<String>();
    private Map<String, String> explicitFieldImports = new HashMap<String, String>();
    private List<String> importPaths = new ArrayList<String>();
    private Map<String, String> packageBroadcasts = new HashMap<String, String>();
    
    private Map<String, PolicyNode> importedPolicies = new HashMap<String, PolicyNode>();
    private Map<String, String> policyToUnitMap = new HashMap<String, String>();
    
    // Import name cache for O(1) lookups
    private Map<String, String> importNameCache = new HashMap<String, String>();
    
    // Type cache for O(1) type lookups
    private Map<String, TypeNode> typeCache = new HashMap<String, TypeNode>();
    
    // Index cache for O(1) class lookups
    private Map<String, Index> indexCache = new HashMap<String, Index>();
    
    // Bytecode manager for .codb files
    private BytecodeManager bytecodeManager;
    
    // Cache for loaded TypeNodes (bytecode or parsed)
    private Map<String, TypeNode> loadedTypes = new HashMap<String, TypeNode>();
    
    // Filesystem result cache
    private Map<String, CachedFileResult> fileCache = new HashMap<String, CachedFileResult>();
    
    // File metadata cache (exists, isDirectory, lastModified)
    private Map<String, FileMetadata> fileMetadataCache = new HashMap<String, FileMetadata>();
    
    // Cache hit/miss counters for debugging
    private int fileCacheHits = 0;
    private int fileCacheMisses = 0;
    private int metadataCacheHits = 0;
    private int metadataCacheMisses = 0;
    private int indexCacheHits = 0;
    private int indexCacheMisses = 0;
    private int bytecodeCacheHits = 0;
    private int bytecodeCacheMisses = 0;
    
    // Current file location for relative import resolution
    private String srcMainRoot;
    private String currentFileDirectory;
    private String projectRoot;
    
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
        DebugSystem.debug("IMPORTS", "Initialized with import paths: " + importPaths);
    }
    
    /**
     * Set the current file directory and automatically find the src/main/ root
     */
    public void setCurrentFileDirectory(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            DebugSystem.debug("IMPORTS", "setCurrentFileDirectory called with null/empty path");
            return;
        }
        
        DebugSystem.debug("IMPORTS", "=== setCurrentFileDirectory CALLED ===");
        DebugSystem.debug("IMPORTS", "filePath: " + filePath);
        
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        
        if (parentDir == null || !parentDir.exists()) {
            DebugSystem.debug("IMPORTS", "Parent directory does not exist for: " + filePath);
            return;
        }
        
        this.currentFileDirectory = parentDir.getAbsolutePath();
        DebugSystem.debug("IMPORTS", "Current file directory set to: " + currentFileDirectory);
        
        // Navigate up to find the src/main/ directory structure
        File searchDir = parentDir;
        this.srcMainRoot = null;
        
        while (searchDir != null) {
            // Check if this directory IS src/main/
            if (searchDir.getName().equals("main") && 
                searchDir.getParentFile() != null && 
                searchDir.getParentFile().getName().equals("src")) {
                
                this.srcMainRoot = searchDir.getAbsolutePath();
                DebugSystem.debug("IMPORTS", "Found src/main/ root (directory itself): " + srcMainRoot);
                break;
            }
            
            // Check if src/main/ is a subdirectory
            File srcMain = new File(searchDir, "src/main");
            if (srcMain.exists() && srcMain.isDirectory()) {
                this.srcMainRoot = srcMain.getAbsolutePath();
                DebugSystem.debug("IMPORTS", "Found src/main/ at: " + srcMainRoot);
                break;
            }
            
            // Move up one level
            searchDir = searchDir.getParentFile();
        }
        
        // If we found src/main/, add it to import paths and set project root
        if (srcMainRoot != null) {
            // Add the src/main/ root to import paths if not present
            if (!importPaths.contains(srcMainRoot)) {
                importPaths.add(0, srcMainRoot);
                DebugSystem.debug("IMPORTS", "Added srcMainRoot to import paths: " + srcMainRoot);
            }
            
            // Set the project root for Index class (for src/idx/ location)
            Index.setProjectRoot(srcMainRoot);
            DebugSystem.debug("IMPORTS", "Set Index project root from: " + srcMainRoot);
            
            // Calculate and store project root for bytecode manager
            this.projectRoot = Index.getProjectRoot();
            if (this.projectRoot != null) {
                this.bytecodeManager = new BytecodeManager(this.projectRoot);
                DebugSystem.debug("IMPORTS", "Initialized BytecodeManager with root: " + this.projectRoot);
            }
            
        } else {
            DebugSystem.debug("IMPORTS", "Could not find src/main/ structure, imports will be relative to: " + currentFileDirectory);
        }
        
        DebugSystem.debug("IMPORTS", "Final configuration - srcMainRoot: " + srcMainRoot + 
                         ", currentFileDirectory: " + currentFileDirectory +
                         ", importPaths: " + importPaths);
    }
    
    public String getCurrentFileDirectory() {
        return currentFileDirectory;
    }
    
    public String getSrcMainRoot() {
        return srcMainRoot;
    }
    
    public String getProjectRoot() {
        return projectRoot;
    }
    
    /**
     * Get absolute path for a unit
     */
    private String getUnitPath(String unitName) {
        if (srcMainRoot != null) {
            return srcMainRoot + "/" + unitName;
        }
        return "src/main/" + unitName;
    }
    
    /**
     * Get or create index for a unit (cached)
     */
    private Index getIndex(String unitName) {
        if (unitName == null || unitName.isEmpty()) {
            return null;
        }
        
        // Check memory cache
        if (indexCache.containsKey(unitName)) {
            Index cached = indexCache.get(unitName);
            String unitPath = getUnitPath(unitName);
            if (!cached.isStale(unitPath)) {
                indexCacheHits++;
                DebugSystem.debug("IMPORTS_CACHE", "Index cache hit for unit: " + unitName);
                return cached;
            } else {
                indexCache.remove(unitName);
                DebugSystem.debug("IMPORTS_CACHE", "Index cache stale for unit: " + unitName);
            }
        }
        
        indexCacheMisses++;
        
        // Try to load from disk
        Index index = Index.load(unitName);
        
        if (index != null) {
            String unitPath = getUnitPath(unitName);
            if (!index.isStale(unitPath)) {
                indexCache.put(unitName, index);
                DebugSystem.debug("IMPORTS_CACHE", "Loaded index from disk for unit: " + unitName + 
                                 " (" + index.size() + " classes)");
                return index;
            } else {
                DebugSystem.debug("IMPORTS_CACHE", "Index file stale for unit: " + unitName);
            }
        }
        
        // Generate new index - this may throw IllegalStateException on duplicates
        try {
            index = generateIndex(unitName);
            if (index != null && !index.isEmpty()) {
                index.save();
                indexCache.put(unitName, index);
                DebugSystem.debug("IMPORTS_CACHE", "Generated new index for unit: " + unitName + 
                                 " (" + index.size() + " classes)");
            }
            return index;
        } catch (IllegalStateException e) {
            // Convert to ProgramError for user-friendly message
            throw new ProgramError(e.getMessage());
        }
    }
    
    /**
     * Generate index by scanning unit directory
     */
    private Index generateIndex(String unitName) {
        String unitPath = getUnitPath(unitName);
        if (unitPath == null) {
            return null;
        }
        
        Index index = new Index(unitName);
        if (index.refresh(unitPath)) {
            return index;
        }
        
        return null;
    }
    
    private FileMetadata getFileMetadata(File file) {
        String path = file.getAbsolutePath();
        
        if (fileMetadataCache.containsKey(path)) {
            FileMetadata cached = fileMetadataCache.get(path);
            if (cached.isValid(file)) {
                metadataCacheHits++;
                return cached;
            } else {
                fileMetadataCache.remove(path);
            }
        }
        
        metadataCacheMisses++;
        
        boolean exists = file.exists();
        boolean isDirectory = exists && file.isDirectory();
        long lastModified = exists ? file.lastModified() : 0;
        
        FileMetadata metadata = new FileMetadata(exists, isDirectory, lastModified);
        fileMetadataCache.put(path, metadata);
        
        return metadata;
    }
    
    private ProgramNode loadImportFromFileCached(String filePath) throws Exception {
        if (filePath == null || filePath.isEmpty()) {
            throw new InternalError("loadImportFromFileCached called with null/empty path");
        }
        
        File file = new File(filePath);
        
        FileMetadata metadata = getFileMetadata(file);
        if (!metadata.exists) {
            return null;
        }
        if (!metadata.isDirectory) {
            if (fileCache.containsKey(filePath)) {
                CachedFileResult cached = fileCache.get(filePath);
                if (cached.isValid(file)) {
                    fileCacheHits++;
                    DebugSystem.debug("IMPORTS_CACHE", "File cache hit: " + filePath);
                    return cached.program;
                } else {
                    fileCache.remove(filePath);
                    DebugSystem.debug("IMPORTS_CACHE", "File cache stale: " + filePath);
                }
            }
            
            fileCacheMisses++;
            DebugSystem.debug("IMPORTS_CACHE", "File cache miss: " + filePath);
            
            ProgramNode program = loadImportFromFile(filePath);
            if (program != null) {
                fileCache.put(filePath, new CachedFileResult(program, metadata.lastModified));
            }
            return program;
        }
        
        return null;
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
                ProgramNode program = resolveImportAsProgram(actualImportName);
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

        if (importName.contains("(")) {
            if (!methodImportSpecs.contains(importName)) {
                methodImportSpecs.add(importName);
            }
            DebugSystem.debug("IMPORTS", "Registered method import spec: " + importName);
            return;
        }

        if (importName.endsWith(".**")) {
            String unitName = importName.substring(0, importName.length() - 3);
            wildcardEverythingUnits.add(unitName);
            DebugSystem.debug("IMPORTS", "Registered everything wildcard import: " + importName);
            return;
        }

        if (importName.endsWith(".*")) {
            String unitName = importName.substring(0, importName.length() - 2);
            wildcardClassUnits.add(unitName);
            DebugSystem.debug("IMPORTS", "Registered class wildcard import: " + importName);
            return;
        }

        int lastDot = importName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < importName.length() - 1) {
            String alias = importName.substring(lastDot + 1);
            explicitFieldImports.put(alias, importName);
        }

        if (!registeredImports.contains(importName)) {
            registeredImports.add(importName);
            cacheImportName(importName);
            DebugSystem.debug("IMPORTS", "Registered import (lazy): " + importName);
        }
    }
    
    private void cacheImportName(String importName) {
        String[] parts = importName.split("\\.");
        if (parts.length > 0) {
            String lastPart = parts[parts.length - 1];
            importNameCache.put(lastPart, importName);
            
            StringBuilder partial = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) partial.append(".");
                partial.append(parts[i]);
                importNameCache.put(partial.toString(), importName);
            }
        }
    }
    
    private String findMatchingImportCached(String calledImport) {
        if (importNameCache.containsKey(calledImport)) {
            String cached = importNameCache.get(calledImport);
            DebugSystem.debug("IMPORTS", "Cache hit for import: " + calledImport + " -> " + cached);
            return cached;
        }
        
        for (String loadedImport : loadedPrograms.keySet()) {
            if (loadedImport.endsWith("." + calledImport) || loadedImport.equals(calledImport)) {
                importNameCache.put(calledImport, loadedImport);
                return loadedImport;
            }
        }
        
        for (String registeredImport : registeredImports) {
            if (registeredImport.endsWith("." + calledImport) || registeredImport.equals(calledImport)) {
                importNameCache.put(calledImport, registeredImport);
                return registeredImport;
            }
        }
        
        return calledImport;
    }

    /**
     * Resolve import and return TypeNode directly
     */
    public TypeNode resolveImport(String importName) throws Exception {
        if (importName == null || importName.isEmpty()) {
            throw new InternalError("resolveImport called with null/empty importName");
        }
        
        DebugSystem.debug("IMPORTS", "=== RESOLVING IMPORT: " + importName + " ===");
        
        // Check cache first
        if (loadedTypes.containsKey(importName)) {
            DebugSystem.debug("IMPORTS", "Type already loaded: " + importName);
            return loadedTypes.get(importName);
        }
        
        // Parse import name: unit.Class
        String[] parts = importName.split("\\.");
        if (parts.length != 2) {
            throw new ProgramError(
                "Invalid import format: '" + importName + "'\n" +
                "Expected format: unit.Class (e.g., sample.Imported)"
            );
        }
        
        String unitName = parts[0];
        String className = parts[1];
        
        DebugSystem.debug("IMPORTS", "Unit: " + unitName + ", Class: " + className);
        
        // ========== TRY BYTECODE FIRST (FAST PATH) ==========
        if (bytecodeManager != null) {
            TypeNode cachedType = bytecodeManager.load(unitName, className);
            if (cachedType != null) {
                bytecodeCacheHits++;
                DebugSystem.debug("BYTECODE", "Loaded " + className + " from .codb (cache hit)");
                loadedTypes.put(importName, cachedType);
                return cachedType;
            } else {
                bytecodeCacheMisses++;
                DebugSystem.debug("BYTECODE", "Bytecode not found for " + className + " (cache miss)");
            }
        }
        // ========== END BYTECODE CHECK ==========
        
        // Try to get index (fast path for source)
        Index index = getIndex(unitName);
        if (index != null) {
            String fileName = index.getFile(className);
            if (fileName != null) {
                String filePath = getUnitPath(unitName) + "/" + fileName;
                DebugSystem.debug("IMPORTS", "Found class '" + className + "' in '" + fileName + "' via index");
                
                ProgramNode program = loadImportFromFileCached(filePath);
                if (program != null) {
                    // Extract the TypeNode from the program
                    for (TypeNode type : program.unit.types) {
                        if (type.name.equals(className)) {
                            // Save bytecode for next time
                            if (bytecodeManager != null) {
                                bytecodeManager.save(unitName, type);
                                DebugSystem.debug("BYTECODE", "Saved " + className + " to .codb");
                            }
                            loadedTypes.put(importName, type);
                            return type;
                        }
                    }
                }
            }
            
            // Class not found in index
            throw new ProgramError(
                "Class '" + className + "' not found in unit '" + unitName + "'\n" +
                "Available classes: " + index.getClassNames()
            );
        }
        
        // Fallback to directory scanning (slow path)
        DebugSystem.debug("IMPORTS", "No index found, scanning directory for unit: " + unitName);
        return resolveImportByScan(importName, unitName, className);
    }
    
    /**
     * Legacy method for ProgramNode resolution (for policies, etc.)
     */
    public ProgramNode resolveImportAsProgram(String importName) throws Exception {
        if (importName == null || importName.isEmpty()) {
            throw new InternalError("resolveImportAsProgram called with null/empty importName");
        }
        
        // Check if already loaded
        if (loadedPrograms.containsKey(importName)) {
            return loadedPrograms.get(importName);
        }
        
        // Check preloaded imports
        if (preloadedImports.containsKey(importName)) {
            ProgramNode program = preloadedImports.get(importName);
            if (program != null) {
                loadedPrograms.put(importName, program);
                importedUnits.put(importName, program);
                cacheImportName(importName);
                registerPoliciesAndBroadcast(program, importName);
                return program;
            }
        }
        
        // Resolve as TypeNode first, then wrap
        TypeNode type = resolveImport(importName);
        if (type != null) {
            ProgramNode program = ASTFactory.createProgram();
            program.unit = ASTFactory.createUnit("default", null);
            program.unit.types.add(type);
            loadedPrograms.put(importName, program);
            return program;
        }
        
        return null;
    }
    
    /**
     * Fallback: resolve import by scanning directory (slow path)
     */
    private TypeNode resolveImportByScan(String importName, String unitName, String className) throws Exception {
        String dirPath = unitName.replace('.', '/');
        DebugSystem.debug("IMPORTS", "Scanning for: " + dirPath);
        
        List<String> pathsToTry = new ArrayList<String>();
        
        if (srcMainRoot != null) {
            pathsToTry.add(srcMainRoot + "/" + dirPath);
            pathsToTry.add(srcMainRoot + "/" + dirPath + ".cod");
        }
        
        if (currentFileDirectory != null && 
            (srcMainRoot == null || !currentFileDirectory.equals(srcMainRoot))) {
            pathsToTry.add(currentFileDirectory + "/" + dirPath);
            pathsToTry.add(currentFileDirectory + "/" + dirPath + ".cod");
        }
        
        for (String basePath : importPaths) {
            if (basePath == null || basePath.isEmpty()) continue;
            if (srcMainRoot != null && basePath.equals(srcMainRoot)) continue;
            
            pathsToTry.add(basePath + "/" + dirPath);
            pathsToTry.add(basePath + "/" + dirPath + ".cod");
        }
        
        pathsToTry.add(dirPath);
        pathsToTry.add(dirPath + ".cod");
        
        List<String> attemptedPaths = new ArrayList<String>();
        
        for (String fullPath : pathsToTry) {
            if (fullPath == null) continue;
            
            File file = new File(fullPath);
            String absolutePath = file.getAbsolutePath();
            attemptedPaths.add(absolutePath);
            
            DebugSystem.debug("IMPORTS", "Checking: " + absolutePath);
            
            if (file.exists() && file.isFile()) {
                DebugSystem.debug("IMPORTS", "FOUND import at: " + absolutePath);
                try {
                    ProgramNode program = loadImportFromFileCached(absolutePath);
                    if (program != null) {
                        if (program.unit != null && program.unit.name != null) {
                            if (!program.unit.name.equals(importName)) {
                                DebugSystem.warn("IMPORTS", 
                                    "Imported file unit name '" + program.unit.name + 
                                    "' does not match import name '" + importName + "'");
                            }
                        }
                        
                        // Extract the TypeNode
                        for (TypeNode type : program.unit.types) {
                            if (type.name.equals(className)) {
                                // Generate index for future use
                                Index index = generateIndex(unitName);
                                if (index != null && !index.isEmpty()) {
                                    index.save();
                                    indexCache.put(unitName, index);
                                }
                                
                                // Save bytecode
                                if (bytecodeManager != null) {
                                    bytecodeManager.save(unitName, type);
                                    DebugSystem.debug("BYTECODE", "Saved " + className + " to .codb");
                                }
                                
                                loadedTypes.put(importName, type);
                                return type;
                            }
                        }
                    }
                } catch (Exception e) {
                    DebugSystem.debug("IMPORTS", "Failed to load from " + absolutePath + ": " + e.getMessage());
                }
            }
        }
        
        StringBuilder errorMsg = new StringBuilder();
        errorMsg.append("Import not found: ").append(importName).append("\n");
        errorMsg.append("Searched in:\n");
        
        Set<String> uniquePaths = new LinkedHashSet<String>(attemptedPaths);
        for (String path : uniquePaths) {
            errorMsg.append("  - ").append(path).append("\n");
        }
        
        errorMsg.append("\nExpected structure: ").append(dirPath).append("/ (with .cod files)\n");
        errorMsg.append("Or file: ").append(dirPath).append(".cod\n");
        
        if (srcMainRoot != null) {
            errorMsg.append("\nDetected src/main/ root: ").append(srcMainRoot).append("\n");
        }
        if (currentFileDirectory != null) {
            errorMsg.append("Current file directory: ").append(currentFileDirectory).append("\n");
        }
        errorMsg.append("Import paths: ").append(importPaths);
        
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

    public TypeNode findType(String qualifiedTypeName) {
        if (qualifiedTypeName == null || qualifiedTypeName.isEmpty()) {
            throw new InternalError("findType called with null/empty name");
        }
        
        DebugSystem.debug("IMPORTS", "findType called for: " + qualifiedTypeName);
        
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
            typeCache.put(qualifiedTypeName, found);
            return found;
        }
        
        String typeName = qualifiedTypeName.substring(lastDot + 1);
        String importPart = qualifiedTypeName.substring(0, lastDot);
        
        DebugSystem.debug("IMPORTS", "Import part: '" + importPart + "', type: '" + typeName + "'");
        
        String actualImportName = findMatchingImportCached(importPart);
        
        DebugSystem.debug("IMPORTS", "Final import to resolve: '" + actualImportName + "', type: '" + typeName + "'");
        
        if (!loadedTypes.containsKey(actualImportName)) {
            DebugSystem.debug("IMPORTS", "Import not loaded, trying to resolve: " + actualImportName);
            try {
                TypeNode type = resolveImport(actualImportName);
                if (type == null) {
                    throw new ProgramError("Failed to resolve import: " + actualImportName);
                }
                loadedTypes.put(actualImportName, type);
            } catch (ProgramError e) {
                throw e;
            } catch (Exception e) {
                throw new InternalError("Unexpected error resolving import: " + actualImportName, e);
            }
        }
        
        return loadedTypes.get(actualImportName);
    }

    private TypeNode findTypeByName(String typeName) {
        if (typeCache.containsKey(typeName)) {
            return typeCache.get(typeName);
        }
        
        for (Map.Entry<String, TypeNode> entry : loadedTypes.entrySet()) {
            TypeNode type = entry.getValue();
            if (type != null && type.name.equals(typeName)) {
                typeCache.put(typeName, type);
                return type;
            }
        }
        
        for (String importName : registeredImports) {
            if (importName.endsWith("." + typeName)) {
                try {
                    TypeNode type = resolveImport(importName);
                    if (type != null) {
                        typeCache.put(typeName, type);
                        return type;
                    }
                } catch (Exception e) {
                    DebugSystem.debug("IMPORTS", "Failed to load " + importName + " while searching for " + typeName);
                }
            }
        }

        for (String unitName : wildcardClassUnits) {
            try {
                TypeNode type = resolveImport(unitName + "." + typeName);
                if (type != null) {
                    typeCache.put(typeName, type);
                    return type;
                }
            } catch (Exception e) {
                // Continue searching
            }
        }
        
        for (String unitName : wildcardEverythingUnits) {
            try {
                TypeNode type = resolveImport(unitName + "." + typeName);
                if (type != null) {
                    typeCache.put(typeName, type);
                    return type;
                }
            } catch (Exception e) {
                // Continue searching
            }
        }
        
        return null;
    }

    public void clearCache() {
        importNameCache.clear();
        typeCache.clear();
        indexCache.clear();
        fileCache.clear();
        fileMetadataCache.clear();
        loadedTypes.clear();
        explicitFieldImports.clear();
        methodImportSpecs.clear();
        wildcardEverythingUnits.clear();
        wildcardClassUnits.clear();
        if (bytecodeManager != null) {
            bytecodeManager.clearCache();
        }
        fileCacheHits = 0;
        fileCacheMisses = 0;
        metadataCacheHits = 0;
        metadataCacheMisses = 0;
        indexCacheHits = 0;
        indexCacheMisses = 0;
        bytecodeCacheHits = 0;
        bytecodeCacheMisses = 0;
        DebugSystem.debug("IMPORTS_CACHE", "All caches cleared");
    }
    
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<String, Object>();
        stats.put("importNameCache", importNameCache.size());
        stats.put("typeCache", typeCache.size());
        stats.put("indexCache", indexCache.size());
        stats.put("fileCache", fileCache.size());
        stats.put("fileMetadataCache", fileMetadataCache.size());
        stats.put("loadedTypes", loadedTypes.size());
        stats.put("fileCacheHits", fileCacheHits);
        stats.put("fileCacheMisses", fileCacheMisses);
        stats.put("metadataCacheHits", metadataCacheHits);
        stats.put("metadataCacheMisses", metadataCacheMisses);
        stats.put("indexCacheHits", indexCacheHits);
        stats.put("indexCacheMisses", indexCacheMisses);
        stats.put("bytecodeCacheHits", bytecodeCacheHits);
        stats.put("bytecodeCacheMisses", bytecodeCacheMisses);
        if (bytecodeManager != null) {
            stats.put("bytecodeStats", bytecodeManager.getCacheStats());
        }
        
        double hitRate = (fileCacheHits + metadataCacheHits + indexCacheHits + bytecodeCacheHits) / 
            (double)(fileCacheHits + fileCacheMisses + metadataCacheHits + metadataCacheMisses + 
                     indexCacheHits + indexCacheMisses + bytecodeCacheHits + bytecodeCacheMisses + 1) * 100;
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
            MethodNode methodBySpec = findMethodFromSpecs(qualifiedMethodName);
            if (methodBySpec != null) {
                return methodBySpec;
            }
            return null;
        }
        
        String methodName = qualifiedMethodName.substring(lastDot + 1);
        String calledImport = qualifiedMethodName.substring(0, lastDot);
        
        DebugSystem.debug("IMPORTS", "Called import part: '" + calledImport + "', method: '" + methodName + "'");
        
        String actualImportName = findMatchingImportCached(calledImport);
        
        DebugSystem.debug("IMPORTS", "Final import to resolve: '" + actualImportName + "', method: '" + methodName + "'");
        
        TypeNode type = null;
        try {
            type = findType(actualImportName);
            if (type != null) {
                DebugSystem.debug("IMPORTS", "Searching for method '" + methodName + "' in type: " + type.name);
                for (MethodNode method : type.methods) {
                    DebugSystem.debug("IMPORTS", "    Checking method: " + method.methodName);
                    if (method.methodName.equals(methodName)) {
                        DebugSystem.debug("IMPORTS", "    *** FOUND METHOD: " + method.methodName + " ***");
                        return method;
                    }
                }
            }
        } catch (ProgramError ignoreTypeError) {
            MethodNode moduleMethod = findMethodInStaticModule(actualImportName, methodName);
            if (moduleMethod != null) {
                return moduleMethod;
            }
        }
        
        MethodNode moduleMethod = findMethodInStaticModule(actualImportName, methodName);
        if (moduleMethod != null) {
            return moduleMethod;
        }
        
        throw new ProgramError(
            "Method not found: '" + qualifiedMethodName + "'\n" +
            "Available methods in import '" + actualImportName + "': " + 
            getMethodNames(type)
        );
    }
    
    private String getMethodNames(TypeNode type) {
        if (type == null || type.methods == null || type.methods.isEmpty()) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < type.methods.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(type.methods.get(i).methodName);
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
        DebugSystem.debug("IMPORTS", "Loaded types: " + loadedTypes.keySet());
        DebugSystem.debug("IMPORTS", "Registered imports (lazy): " + registeredImports);
        DebugSystem.debug("IMPORTS", "Registered policies: " + getRegisteredPolicies());
        DebugSystem.debug("IMPORTS", "Import name cache size: " + importNameCache.size());
        DebugSystem.debug("IMPORTS", "Type cache size: " + typeCache.size());
        DebugSystem.debug("IMPORTS", "Index cache size: " + indexCache.size());
        DebugSystem.debug("IMPORTS", "File cache size: " + fileCache.size());
        DebugSystem.debug("IMPORTS", "File metadata cache size: " + fileMetadataCache.size());
        if (bytecodeManager != null) {
            DebugSystem.debug("IMPORTS", "Bytecode stats: " + bytecodeManager.getCacheStats());
        }
        
        Map<String, Object> stats = getCacheStats();
        DebugSystem.debug("IMPORTS", "File cache hits: " + stats.get("fileCacheHits"));
        DebugSystem.debug("IMPORTS", "File cache misses: " + stats.get("fileCacheMisses"));
        DebugSystem.debug("IMPORTS", "Metadata cache hits: " + stats.get("metadataCacheHits"));
        DebugSystem.debug("IMPORTS", "Metadata cache misses: " + stats.get("metadataCacheMisses"));
        DebugSystem.debug("IMPORTS", "Index cache hits: " + stats.get("indexCacheHits"));
        DebugSystem.debug("IMPORTS", "Index cache misses: " + stats.get("indexCacheMisses"));
        DebugSystem.debug("IMPORTS", "Bytecode cache hits: " + stats.get("bytecodeCacheHits"));
        DebugSystem.debug("IMPORTS", "Bytecode cache misses: " + stats.get("bytecodeCacheMisses"));
        DebugSystem.debug("IMPORTS", "Cache hit rate: " + stats.get("cacheHitRate"));
        DebugSystem.debug("IMPORTS", "Loaded types: " + stats.get("loadedTypes"));

        for (Map.Entry<String, TypeNode> entry : loadedTypes.entrySet()) {
            String typeName = entry.getKey();
            TypeNode type = entry.getValue();
            DebugSystem.debug("IMPORTS", "Type: " + typeName);
            if (type != null && type.methods != null) {
                DebugSystem.debug("IMPORTS", "  Methods: " + type.methods.size());
                for (MethodNode method : type.methods) {
                    DebugSystem.debug("IMPORTS", "    Method: " + method.methodName);
                }
            }
        }
        
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
        
        cacheImportName(qualifiedName);
        
        DebugSystem.debug("IMPORTS", "Pre-loaded import into resolver: " + qualifiedName);
        
        if (program.unit != null && program.unit.policies != null) {
            for (PolicyNode policy : program.unit.policies) {
                String qualifiedPolicyName = program.unit.name + "." + policy.name;
                registerPolicy(qualifiedPolicyName, policy);
                policyToUnitMap.put(policy.name, program.unit.name);
            }
        }
        
        if (program.unit != null && program.unit.types != null) {
            for (TypeNode type : program.unit.types) {
                String qualifiedTypeName = program.unit.name + "." + type.name;
                typeCache.put(qualifiedTypeName, type);
                typeCache.put(type.name, type);
                loadedTypes.put(qualifiedTypeName, type);
                
                // Pre-save bytecode if available
                if (bytecodeManager != null) {
                    bytecodeManager.save(program.unit.name, type);
                }
            }
        }
        
        // Preload index for this unit
        if (program.unit != null && program.unit.name != null) {
            String unitName = program.unit.name;
            Index index = new Index(unitName);
            for (TypeNode type : program.unit.types) {
                String fileName = type.name + ".cod";
                index.add(type.name, fileName);
            }
            if (!index.isEmpty()) {
                index.save();
                indexCache.put(unitName, index);
            }
        }
    }

    public Map<String, ProgramNode> getLoadedPrograms() {
        return loadedPrograms;
    }

    public Map<String, TypeNode> getLoadedTypes() {
        return loadedTypes;
    }

    public Set<String> getLoadedImports() {
        return importedUnits.keySet();
    }
    
    public Set<String> getRegisteredImports() {
        return new HashSet<String>(registeredImports);
    }

    public FieldNode findField(String qualifiedFieldName) {
        if (qualifiedFieldName == null || qualifiedFieldName.isEmpty()) {
            return null;
        }
        
        String fullName = qualifiedFieldName;
        if (explicitFieldImports.containsKey(qualifiedFieldName)) {
            fullName = explicitFieldImports.get(qualifiedFieldName);
        }
        
        int lastDot = fullName.lastIndexOf('.');
        if (lastDot <= 0 || lastDot >= fullName.length() - 1) {
            return null;
        }
        
        String unitName = fullName.substring(0, lastDot);
        String fieldName = fullName.substring(lastDot + 1);
        
        TypeNode staticType = loadStaticModuleType(unitName);
        if (staticType != null && staticType.fields != null) {
            for (FieldNode field : staticType.fields) {
                if (fieldName.equals(field.name)) {
                    return field;
                }
            }
        }
        
        for (String wildcardUnit : wildcardEverythingUnits) {
            TypeNode wildcardType = loadStaticModuleType(wildcardUnit);
            if (wildcardType != null && wildcardType.fields != null) {
                for (FieldNode field : wildcardType.fields) {
                    if (qualifiedFieldName.equals(field.name) || fieldName.equals(field.name)) {
                        return field;
                    }
                }
            }
        }
        
        return null;
    }

    private MethodNode findMethodFromSpecs(String methodName) {
        for (String spec : methodImportSpecs) {
            ParsedMethodImport parsed = parseMethodImport(spec);
            if (parsed != null && parsed.methodName.equals(methodName)) {
                MethodNode method = findMethodInStaticModule(parsed.unitName, parsed.methodName);
                if (method != null) {
                    return method;
                }
            }
        }
        
        for (String unitName : wildcardEverythingUnits) {
            MethodNode method = findMethodInStaticModule(unitName, methodName);
            if (method != null) {
                return method;
            }
        }
        
        return null;
    }

    private MethodNode findMethodInStaticModule(String unitName, String methodName) {
        TypeNode staticType = loadStaticModuleType(unitName);
        if (staticType == null || staticType.methods == null) {
            return null;
        }
        
        for (MethodNode method : staticType.methods) {
            if (methodName.equals(method.methodName)) {
                return method;
            }
        }
        
        return null;
    }

    private TypeNode loadStaticModuleType(String unitName) {
        if (unitName == null || unitName.isEmpty()) {
            return null;
        }
        
        ProgramNode program = loadedPrograms.get(unitName);
        if (program == null) {
            program = loadStaticModuleProgram(unitName);
            if (program != null) {
                loadedPrograms.put(unitName, program);
            }
        }
        
        if (program == null || program.unit == null || program.unit.types == null) {
            return null;
        }
        
        for (TypeNode type : program.unit.types) {
            if ("__StaticModule__".equals(type.name)) {
                return type;
            }
        }
        
        return null;
    }

    private ProgramNode loadStaticModuleProgram(String unitName) {
        String dirPath = unitName.replace('.', '/');
        List<String> pathsToTry = new ArrayList<String>();
        
        if (srcMainRoot != null) {
            pathsToTry.add(srcMainRoot + "/" + dirPath + ".cod");
        }
        
        if (currentFileDirectory != null && 
            (srcMainRoot == null || !currentFileDirectory.equals(srcMainRoot))) {
            pathsToTry.add(currentFileDirectory + "/" + dirPath + ".cod");
        }
        
        for (String basePath : importPaths) {
            if (basePath == null || basePath.isEmpty()) continue;
            if (srcMainRoot != null && basePath.equals(srcMainRoot)) continue;
            pathsToTry.add(basePath + "/" + dirPath + ".cod");
        }
        
        pathsToTry.add(dirPath + ".cod");
        
        for (String path : pathsToTry) {
            try {
                ProgramNode program = loadImportFromFileCached(path);
                if (program != null) {
                    return program;
                }
            } catch (Exception e) {
                // Continue trying
            }
        }
        
        return null;
    }

    private ParsedMethodImport parseMethodImport(String spec) {
        int open = spec.indexOf('(');
        int close = spec.lastIndexOf(')');
        if (open <= 0 || close <= open) {
            return null;
        }
        
        String head = spec.substring(0, open);
        int lastDot = head.lastIndexOf('.');
        if (lastDot <= 0 || lastDot >= head.length() - 1) {
            return null;
        }
        
        ParsedMethodImport parsed = new ParsedMethodImport();
        parsed.unitName = head.substring(0, lastDot);
        parsed.methodName = head.substring(lastDot + 1);
        parsed.signature = spec.substring(open + 1, close);
        return parsed;
    }

    private static class ParsedMethodImport {
        String unitName;
        String methodName;
        String signature;
    }
}
