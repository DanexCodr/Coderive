package cod.util;

import cod.ast.nodes.*;
import java.io.*;
import java.util.*;

public class BytecodeManager {
    private static final String BIN_DIR = "bin";
    private static final String BYTECODE_EXT = ".codb";
    private static final int MAGIC = 0xAC0D1EB1; // "A CODIE B I" - A Coderive Bytecode Interpretation
    private static final int VERSION = 1;
    
    private final String projectRoot;
    private final Map<String, Map<String, TypeNode>> cache;
    
    public BytecodeManager(String projectRoot) {
        this.projectRoot = projectRoot;
        this.cache = new HashMap<String, Map<String, TypeNode>>();
    }
    
    public TypeNode load(String unit, String className) {
    // Check memory cache
    if (cache.containsKey(unit)) {
        Map<String, TypeNode> unitCache = cache.get(unit);
        if (unitCache != null) {
            TypeNode cached = unitCache.get(className);
            if (cached != null) {
                System.err.println("[BYTECODE] Cache hit for: " + className);
                return cached;
            }
        }
    }
    
    File file = getBytecodeFile(unit, className);
    if (!file.exists()) {
        System.err.println("[BYTECODE] File not found: " + file.getAbsolutePath());
        return null;
    }
    
    System.err.println("[BYTECODE] Loading from: " + file.getAbsolutePath());
    
    ObjectInputStream in = null;
    try {
        in = new ObjectInputStream(new FileInputStream(file));
        int magic = in.readInt();
        if (magic != MAGIC) {
            System.err.println("[BYTECODE] Invalid magic number: expected 0xAC0D1EB1, got 0x" + Integer.toHexString(magic));
            return null;
        }
        int version = in.readInt();
        if (version != VERSION) {
            System.err.println("[BYTECODE] Version mismatch: expected " + VERSION + ", got " + version);
            return null;
        }
        TypeNode node = (TypeNode) in.readObject();
        
        System.err.println("[BYTECODE] Loaded: " + className);
        System.err.println("[BYTECODE]   Methods: " + (node.methods != null ? node.methods.size() : 0));
        if (node.methods != null && !node.methods.isEmpty()) {
            MethodNode first = node.methods.get(0);
            System.err.println("[BYTECODE]   First method: " + first.methodName);
            System.err.println("[BYTECODE]   Body size: " + (first.body != null ? first.body.size() : 0));
        } else {
            System.err.println("[BYTECODE]   WARNING: No methods found in loaded node!");
        }
        
        Map<String, TypeNode> unitCache = cache.get(unit);
        if (unitCache == null) {
            unitCache = new HashMap<String, TypeNode>();
            cache.put(unit, unitCache);
        }
        unitCache.put(className, node);
        return node;
    } catch (Exception e) {
        System.err.println("[BYTECODE] Failed to load " + className + ": " + e.getMessage());
        e.printStackTrace();
        return null;
    } finally {
        if (in != null) {
            try { in.close(); } catch (IOException e) {}
        }
    }
}
    
    public void save(String unit, TypeNode node) {
    if (node == null) return;
    
    // ========== SERIALIZATION TEST ==========
    // Test if the node is fully serializable before saving
    try {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.ObjectOutputStream testOut = new java.io.ObjectOutputStream(baos);
        testOut.writeObject(node);
        testOut.close();
        System.err.println("[BYTECODE] Serialization test PASSED for: " + node.name);
        System.err.println("[BYTECODE]   Methods: " + (node.methods != null ? node.methods.size() : 0));
        if (node.methods != null && !node.methods.isEmpty()) {
            MethodNode first = node.methods.get(0);
            System.err.println("[BYTECODE]   First method: " + first.methodName);
            System.err.println("[BYTECODE]   Body size: " + (first.body != null ? first.body.size() : 0));
        }
    } catch (Exception e) {
        System.err.println("[BYTECODE] Serialization test FAILED for: " + node.name);
        System.err.println("[BYTECODE]   Error: " + e.getMessage());
        e.printStackTrace();
        return; // Don't save if not serializable
    }
    // ========== END SERIALIZATION TEST ==========
    
    File file = getBytecodeFile(unit, node.name);
    File parent = file.getParentFile();
    if (parent != null && !parent.exists()) {
        parent.mkdirs();
    }
    
    ObjectOutputStream out = null;
    try {
        out = new ObjectOutputStream(new FileOutputStream(file));
        out.writeInt(MAGIC);        // "A CODIE B I" - A Coderive Bytecode Interpretation
        out.writeInt(VERSION);
        out.writeObject(node);
        out.flush();
        
        System.err.println("[BYTECODE] Successfully saved: " + node.name + " to " + file.getAbsolutePath());
        
        Map<String, TypeNode> unitCache = cache.get(unit);
        if (unitCache == null) {
            unitCache = new HashMap<String, TypeNode>();
            cache.put(unit, unitCache);
        }
        unitCache.put(node.name, node);
    } catch (IOException e) {
        System.err.println("[BYTECODE] Failed to save: " + node.name + " - " + e.getMessage());
        e.printStackTrace();
    } finally {
        if (out != null) {
            try { out.close(); } catch (IOException e) {}
        }
    }
}
    
    public boolean isStale(String unit, String className, long sourceTimestamp) {
        File file = getBytecodeFile(unit, className);
        if (!file.exists()) return true;
        return file.lastModified() < sourceTimestamp;
    }
    
    public void invalidate(String unit, String className) {
        Map<String, TypeNode> unitCache = cache.get(unit);
        if (unitCache != null) {
            unitCache.remove(className);
        }
        File file = getBytecodeFile(unit, className);
        if (file.exists()) file.delete();
    }
    
    public void invalidateUnit(String unit) {
        cache.remove(unit);
        File dir = new File(projectRoot + "/src/" + BIN_DIR + "/" + unit);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
            dir.delete();
        }
    }
    
    private File getBytecodeFile(String unit, String className) {
        String path = projectRoot + "/src/" + BIN_DIR + "/" + unit + "/" + className + BYTECODE_EXT;
        return new File(path);
    }
    
    public void clearCache() {
        cache.clear();
    }
    
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<String, Object>();
        int total = 0;
        for (Map<String, TypeNode> unitCache : cache.values()) {
            total += unitCache.size();
        }
        stats.put("units", cache.size());
        stats.put("classes", total);
        return stats;
    }
}