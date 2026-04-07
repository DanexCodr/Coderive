package cod.ir;

import cod.ast.node.Type;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;

public final class IRReader {
    public Type read(File file) throws IOException {
        if (file == null) {
            throw new IOException("IR source file is null");
        }
        if (!file.exists() || !file.isFile()) {
            throw new IOException("IR source file not found: " + file.getAbsolutePath());
        }

        DataInputStream in = null;
        try {
            in = new DataInputStream(new BufferedInputStream(new java.io.FileInputStream(file)));
            int magic = in.readInt();
            int version = in.readInt();

            // New direct object-stream IR path (Load -> Execute)
            if (magic == IRWriter.STREAM_MAGIC) {
                if (version != IRWriter.STREAM_VERSION) {
                    throw new IOException("Unsupported IR stream version: " + version);
                }
                ObjectInputStream objectIn = null;
                try {
                    objectIn = new ObjectInputStream(in);
                    Object value = objectIn.readObject();
                    if (!(value instanceof Type)) {
                        throw new IOException("IR root is not a Type: " + (value == null ? "null" : value.getClass().getName()));
                    }
                    return (Type) value;
                } catch (ClassNotFoundException e) {
                    throw new IOException("Failed to load IR object graph", e);
                } finally {
                    if (objectIn != null) {
                        try {
                            objectIn.close();
                        } catch (IOException ignored) {}
                    }
                }
            }

            // Legacy IR path for backward compatibility
            if (magic == IRCodec.MAGIC && version == IRCodec.VERSION) {
                Object value = IRCodec.readValue(in, 0);
                if (!(value instanceof Type)) {
                    throw new IOException("IR root is not a Type: " + (value == null ? "null" : value.getClass().getName()));
                }
                return (Type) value;
            }

            throw new IOException("Invalid IR header");
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {}
            }
        }
    }
}
