package cod.ir;

import cod.ast.node.Type;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class IRManager {
    private static final String BIN_DIR = "bin";
    private static final String IR_EXT = ".codb";

    private final String projectRoot;
    private final IRWriter writer;
    private final IRReader reader;
    private final Map<String, Map<String, Type>> cache;

    public IRManager(String projectRoot) {
        this.projectRoot = projectRoot;
        this.writer = new IRWriter();
        this.reader = new IRReader();
        this.cache = new HashMap<String, Map<String, Type>>();
    }

    public Type load(String unit, String className) {
        if (unit == null || className == null) {
            return null;
        }

        Map<String, Type> unitCache = cache.get(unit);
        if (unitCache != null) {
            Type cached = unitCache.get(className);
            if (cached != null) {
                return cached;
            }
        }

        File file = getIRFile(unit, className);
        if (!file.exists()) {
            return null;
        }

        try {
            Type type = reader.read(file);
            putCache(unit, className, type);
            return type;
        } catch (IOException e) {
            return null;
        }
    }

    public void save(String unit, Type type) {
        if (type == null || unit == null || type.name == null) {
            return;
        }
        File file = getIRFile(unit, type.name);
        try {
            writer.write(file, type);
            putCache(unit, type.name, type);
        } catch (IOException ignored) {}
    }

    public void clearCache() {
        cache.clear();
    }

    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<String, Object>();
        int total = 0;
        for (Map<String, Type> unitCache : cache.values()) {
            total += unitCache.size();
        }
        stats.put("units", cache.size());
        stats.put("classes", total);
        return stats;
    }

    private void putCache(String unit, String className, Type type) {
        Map<String, Type> unitCache = cache.get(unit);
        if (unitCache == null) {
            unitCache = new HashMap<String, Type>();
            cache.put(unit, unitCache);
        }
        unitCache.put(className, type);
    }

    private File getIRFile(String unit, String className) {
        String path = projectRoot + "/src/" + BIN_DIR + "/" + unit + "/" + className + IR_EXT;
        return new File(path);
    }
}
