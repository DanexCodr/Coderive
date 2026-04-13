package cod.ir;

import cod.ast.node.Type;
import cod.ptac.Artifact;
import cod.ptac.Compiler;
import cod.ptac.Unit;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.CRC32;

public class IRManager {
    private static final String BIN_DIR = "bin";
    private static final String IR_EXT = ".codb";
    private static final String CONTAINER_EXT = ".codc";
    private static final String PROJECT_CONTAINER_NAME = "project";
    private static final String PROJECT_INDEX_FILE_NAME = "HOOK.toml";
    private static final int BUFFER_SIZE = 8192;
    private static final Map<String, Object> CONTAINER_LOCKS = new ConcurrentHashMap<String, Object>();

    private final String projectRoot;
    private final IRWriter writer;
    private final IRReader reader;
    private final Map<String, Map<String, Type>> cache;
    private final Map<String, Map<String, Artifact>> artifactCache;
    private final Compiler compiler;

    public IRManager(String projectRoot) {
        this.projectRoot = projectRoot;
        this.writer = new IRWriter();
        this.reader = new IRReader();
        this.cache = new HashMap<String, Map<String, Type>>();
        this.artifactCache = new HashMap<String, Map<String, Artifact>>();
        this.compiler = new Compiler();
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

        try {
            Artifact artifact = readArtifactFromContainer(unit, className);
            if (artifact == null) {
                // Standalone .codb files are a permanent supported format.
                // .codc containers are additive grouping, not a replacement.
                File file = getIRFile(unit, className);
                if (!file.exists()) {
                    return null;
                }
                artifact = reader.readArtifact(file);
            }
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
        try {
            Artifact artifact = compiler.compile(unit, type);
            writeArtifactToContainer(unit, artifact.className, artifact);
            putCache(unit, type.name, type);
            putArtifactCache(unit, type.name, artifact);
        } catch (IOException ignored) {}
    }

    public Artifact loadArtifact(String unit, String className) {
        if (unit == null || className == null) {
            return null;
        }

        Map<String, Artifact> unitCache = artifactCache.get(unit);
        if (unitCache != null && unitCache.containsKey(className)) {
            return unitCache.get(className);
        }

        try {
            Artifact artifact = readArtifactFromContainer(unit, className);
            if (artifact == null) {
                // Standalone .codb files are a permanent supported format.
                // .codc containers are additive grouping, not a replacement.
                File file = getIRFile(unit, className);
                if (!file.exists()) {
                    return null;
                }
                artifact = reader.readArtifact(file);
            }
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

    public Unit loadCodPTACUnit(String unit, String className) {
        Artifact artifact = loadArtifact(unit, className);
        return artifact != null ? artifact.unit : null;
    }

    public void saveArtifact(String unit, Artifact artifact) {
        if (artifact == null || unit == null || artifact.className == null) return;
        try {
            writeArtifactToContainer(unit, artifact.className, artifact);
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

    public String loadIndex(String unit) {
        if (unit == null || unit.isEmpty()) return null;
        String entryName = getProjectIndexEntryName();
        try {
            byte[] data = readContainerEntry(unit, entryName);
            if (data == null) return null;
            return new String(data, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    public void saveIndex(String unit, String indexContent) throws IOException {
        if (unit == null || unit.isEmpty() || indexContent == null) return;
        String entryName = getProjectIndexEntryName();
        writeContainerEntry(unit, entryName, indexContent.getBytes(StandardCharsets.UTF_8));
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
        for (Map<String, Artifact> unitArtifacts : artifactCache.values()) {
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

    private void putArtifactCache(String unit, String className, Artifact artifact) {
        Map<String, Artifact> unitCache = artifactCache.get(unit);
        if (unitCache == null) {
            unitCache = new HashMap<String, Artifact>();
            artifactCache.put(unit, unitCache);
        }
        unitCache.put(className, artifact);
    }

    private File getIRFile(String unit, String className) {
        String path = projectRoot + "/src/" + BIN_DIR + "/" + unit + "/" + className + IR_EXT;
        return new File(path);
    }

    private File getContainerFile(String unit) {
        String path = projectRoot + "/src/" + BIN_DIR + "/" + PROJECT_CONTAINER_NAME + CONTAINER_EXT;
        return new File(path);
    }

    private String getContainerEntryName(String unit, String className) {
        return unit + "/" + className + IR_EXT;
    }

    private String getProjectIndexEntryName() {
        return PROJECT_INDEX_FILE_NAME;
    }

    private Artifact readArtifactFromContainer(String unit, String className) throws IOException {
        byte[] data = readContainerEntry(unit, getContainerEntryName(unit, className));
        if (data == null) return null;
        return readArtifactFromBytes(data);
    }

    private void writeArtifactToContainer(String unit, String className, Artifact artifact) throws IOException {
        if (unit == null || className == null || artifact == null) return;
        writeContainerEntry(unit, getContainerEntryName(unit, className), writeArtifactToBytes(artifact));
    }

    private byte[] readContainerEntry(String unit, String entryName) throws IOException {
        if (unit == null || entryName == null) return null;
        File container = getContainerFile(unit);
        if (!container.exists() || !container.isFile()) {
            return null;
        }

        ZipInputStream in = null;
        try {
            in = new ZipInputStream(new BufferedInputStream(new FileInputStream(container)));
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if (!entry.isDirectory() && entryName.equals(entry.getName())) {
                    return readAllBytes(in);
                }
            }
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {}
            }
        }
    }

    private void writeContainerEntry(String unit, String entryName, byte[] entryData) throws IOException {
        if (unit == null || entryName == null || entryData == null) return;

        File container = getContainerFile(unit);
        File parent = container.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create IR container directory: " + parent.getAbsolutePath());
        }

        Object containerLock = getContainerLock(container);
        synchronized (containerLock) {
            Map<String, byte[]> entries = readContainerEntries(container);
            entries.put(entryName, entryData);

            File temp = new File(container.getAbsolutePath() + ".tmp");
            ZipOutputStream out = null;
            boolean moved = false;
            try {
                out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(temp)));
                out.setLevel(0);
                for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                    byte[] value = e.getValue();
                    CRC32 crc = new CRC32();
                    crc.update(value);
                    ZipEntry zipEntry = new ZipEntry(e.getKey());
                    // .codc is intentionally an uncompressed zip container (level 0, STORED entries).
                    zipEntry.setMethod(ZipEntry.STORED);
                    zipEntry.setSize(value.length);
                    zipEntry.setCompressedSize(value.length);
                    zipEntry.setCrc(crc.getValue());
                    out.putNextEntry(zipEntry);
                    out.write(value);
                    out.closeEntry();
                }
                out.finish();
                Files.move(temp.toPath(), container.toPath(), StandardCopyOption.REPLACE_EXISTING);
                moved = true;
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException ignored) {}
                }
                if (!moved && temp.exists()) {
                    try {
                        Files.delete(temp.toPath());
                    } catch (IOException ignored) {}
                }
            }
        }
    }

    private Object getContainerLock(File container) {
        String key = container.getAbsolutePath();
        Object lock = CONTAINER_LOCKS.get(key);
        if (lock != null) return lock;
        Object created = new Object();
        Object existing = CONTAINER_LOCKS.putIfAbsent(key, created);
        return existing != null ? existing : created;
    }

    private Map<String, byte[]> readContainerEntries(File container) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        if (container == null || !container.exists() || !container.isFile()) {
            return entries;
        }

        ZipInputStream in = null;
        try {
            in = new ZipInputStream(new BufferedInputStream(new FileInputStream(container)));
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                entries.put(entry.getName(), readAllBytes(in));
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {}
            }
        }
        return entries;
    }

    private byte[] writeArtifactToBytes(Artifact artifact) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = null;
        try {
            out = new DataOutputStream(baos);
            IRArtifactCodec.writeArtifact(out, artifact);
            out.flush();
            return baos.toByteArray();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {}
            }
        }
    }

    private Artifact readArtifactFromBytes(byte[] data) throws IOException {
        if (data == null) return null;
        DataInputStream in = null;
        try {
            in = new DataInputStream(new ByteArrayInputStream(data));
            return IRArtifactCodec.readArtifact(in);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {}
            }
        }
    }

    private byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = in.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
}
