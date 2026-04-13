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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.CRC32;

public class IRManager {
    private static final String BIN_DIR = "bin";
    private static final String IR_EXT = ".codb";
    private static final String CONTAINER_EXT = ".codc";

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
        String rootUnit = getRootUnit(unit);
        String path = projectRoot + "/src/" + BIN_DIR + "/" + rootUnit + CONTAINER_EXT;
        return new File(path);
    }

    private String getRootUnit(String unit) {
        if (unit == null || unit.isEmpty()) return "default";
        int dot = unit.indexOf('.');
        if (dot <= 0) return unit;
        return unit.substring(0, dot);
    }

    private String getContainerEntryName(String unit, String className) {
        return unit + "/" + className + IR_EXT;
    }

    private Artifact readArtifactFromContainer(String unit, String className) throws IOException {
        File container = getContainerFile(unit);
        if (!container.exists() || !container.isFile()) {
            return null;
        }

        String targetEntry = getContainerEntryName(unit, className);
        ZipInputStream in = null;
        try {
            in = new ZipInputStream(new BufferedInputStream(new FileInputStream(container)));
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if (!entry.isDirectory() && targetEntry.equals(entry.getName())) {
                    byte[] data = readAllBytes(in);
                    return readArtifactFromBytes(data);
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

    private void writeArtifactToContainer(String unit, String className, Artifact artifact) throws IOException {
        if (unit == null || className == null || artifact == null) return;

        File container = getContainerFile(unit);
        File parent = container.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create IR container directory: " + parent.getAbsolutePath());
        }

        Map<String, byte[]> entries = readContainerEntries(container);
        entries.put(getContainerEntryName(unit, className), writeArtifactToBytes(artifact));

        File temp = new File(container.getAbsolutePath() + ".tmp");
        ZipOutputStream out = null;
        try {
            out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(temp)));
            out.setLevel(0);
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                byte[] value = e.getValue();
                CRC32 crc = new CRC32();
                crc.update(value);
                ZipEntry zipEntry = new ZipEntry(e.getKey());
                zipEntry.setMethod(ZipEntry.STORED);
                zipEntry.setSize(value.length);
                zipEntry.setCompressedSize(value.length);
                zipEntry.setCrc(crc.getValue());
                out.putNextEntry(zipEntry);
                out.write(value);
                out.closeEntry();
            }
            out.finish();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {}
            }
        }

        Files.move(temp.toPath(), container.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private Map<String, byte[]> readContainerEntries(File container) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
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
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) >= 0) {
            if (read > 0) {
                out.write(buffer, 0, read);
            }
        }
        return out.toByteArray();
    }
}
