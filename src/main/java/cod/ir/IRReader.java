package cod.ir;

import cod.ast.node.Type;
import cod.ptac.CodPTACArtifact;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public final class IRReader {
    private static final int JAVA_SERIAL_STREAM_MAGIC = 0xACED0005;

    public Type read(File file) throws IOException {
        CodPTACArtifact artifact = readArtifact(file);
        if (artifact == null) {
            return null;
        }
        return artifact.typeSnapshot;
    }

    public CodPTACArtifact readArtifact(File file) throws IOException {
        if (file == null) {
            throw new IOException("IR source file is null");
        }
        if (!file.exists() || !file.isFile()) {
            throw new IOException("IR source file not found: " + file.getAbsolutePath());
        }

        DataInputStream in = null;
        try {
            in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
            int magic = in.readInt();
            if (magic == IRCodec.MAGIC) {
                return IRArtifactCodec.readArtifact(in);
            }
            if (magic == JAVA_SERIAL_STREAM_MAGIC) {
                return readLegacyObjectStream(file);
            }
            throw new IOException("Unknown IR format magic: 0x" + Integer.toHexString(magic));
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {}
            }
        }
    }

    private CodPTACArtifact readLegacyObjectStream(File file) throws IOException {
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
            Object value = in.readObject();
            if (value instanceof CodPTACArtifact) {
                return (CodPTACArtifact) value;
            }
            if (value instanceof Type) {
                CodPTACArtifact legacy = new CodPTACArtifact();
                legacy.className = ((Type) value).name;
                legacy.typeSnapshot = (Type) value;
                return legacy;
            }
            throw new IOException("IR root is not a CodPTACArtifact/Type: "
                + (value == null ? "null" : value.getClass().getName()));
        } catch (ClassNotFoundException e) {
            throw new IOException("Failed to load legacy IR object graph", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {}
            }
        }
    }
}
