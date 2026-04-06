package cod.ir;

import cod.ast.nodes.Type;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;

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
            IRCodec.readHeader(in);
            Object value = IRCodec.readValue(in, 0);
            if (!(value instanceof Type)) {
                throw new IOException("IR root is not a Type: " + (value == null ? "null" : value.getClass().getName()));
            }
            return (Type) value;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {}
            }
        }
    }
}
