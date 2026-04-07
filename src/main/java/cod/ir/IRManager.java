package cod.ir;

import cod.ast.node.Type;
import cod.ptac.CodPTACArtifact;
import cod.ptac.CodPTACCompiler;
import cod.ptac.CodPTACUnit;

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
    private final Map<String, Map<String, CodPTACArtifact>> artifactCache;
    private final CodPTACCompiler compiler;

    public IRManager(String projectRoot) {
        this.projectRoot = projectRoot;
        this.writer = new IRWriter();
        this.reader = new IRReader();
        this.cache = new HashMap<String, Map<String, Type>>();
        this.artifactCache = new HashMap<String, Map<String, CodPTACArtifact>>();
        this.compiler = new CodPTACCompiler();
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
            CodPTACArtifact artifact = reader.readArtifact(file);
            if (artifact != null) {
                putArtifactCache(unit, className, artifact);
                Type type = artifact.typeSnapshot;
                if (type != null) {
                    putCache(unit, className, type);
                }
                return type;
            }
            return null;
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
            CodPTACArtifact artifact = compiler.compile(unit, type);
            writer.writeArtifact(file, artifact);
            putCache(unit, type.name, type);
            putArtifactCache(unit, type.name, artifact);
        } catch (IOException ignored) {}
    }

    public CodPTACArtifact loadArtifact(String unit, String className) {
        if (unit == null || className == null) {
            return null;
        }

        Map<String, CodPTACArtifact> unitCache = artifactCache.get(unit);
        if (unitCache != null && unitCache.containsKey(className)) {
            return unitCache.get(className);
        }

        File file = getIRFile(unit, className);
        if (!file.exists()) {
            return null;
        }

        try {
            CodPTACArtifact artifact = reader.readArtifact(file);
            if (artifact != null) {
                putArtifactCache(unit, className, artifact);
                if (artifact.typeSnapshot != null) {
                    putCache(unit, className, artifact.typeSnapshot);
                }
            }
            return artifact;
        } catch (IOException e) {
            return null;
        }
    }

    public CodPTACUnit loadCodPTACUnit(String unit, String className) {
        CodPTACArtifact artifact = loadArtifact(unit, className);
        return artifact != null ? artifact.unit : null;
    }

    public void saveArtifact(String unit, CodPTACArtifact artifact) {
        if (artifact == null || unit == null || artifact.className == null) return;
        File file = getIRFile(unit, artifact.className);
        try {
            writer.writeArtifact(file, artifact);
            putArtifactCache(unit, artifact.className, artifact);
            if (artifact.typeSnapshot != null) {
                putCache(unit, artifact.className, artifact.typeSnapshot);
            }
        } catch (IOException ignored) {}
    }

    public void clearCache() {
        cache.clear();
        artifactCache.clear();
    }

    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<String, Object>();
        int total = 0;
        for (Map<String, Type> unitCache : cache.values()) {
            total += unitCache.size();
        }
        stats.put("units", cache.size());
        stats.put("classes", total);
        int artifacts = 0;
        for (Map<String, CodPTACArtifact> unitArtifacts : artifactCache.values()) {
            artifacts += unitArtifacts.size();
        }
        stats.put("artifacts", artifacts);
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

    private void putArtifactCache(String unit, String className, CodPTACArtifact artifact) {
        Map<String, CodPTACArtifact> unitCache = artifactCache.get(unit);
        if (unitCache == null) {
            unitCache = new HashMap<String, CodPTACArtifact>();
            artifactCache.put(unit, unitCache);
        }
        unitCache.put(className, artifact);
    }

    private File getIRFile(String unit, String className) {
        String path = projectRoot + "/src/" + BIN_DIR + "/" + unit + "/" + className + IR_EXT;
        return new File(path);
    }
}
