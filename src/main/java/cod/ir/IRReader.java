package cod.ir;

import cod.ast.node.Type;

import java.io.File;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

public final class IRReader {
    public Type read(File file) throws IOException {
        if (file == null) {
            throw new IOException("IR source file is null");
        }
        if (!file.exists() || !file.isFile()) {
            throw new IOException("IR source file not found: " + file.getAbsolutePath());
        }

        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
            Object value = in.readObject();
            if (!(value instanceof Type)) {
                throw new IOException("IR root is not a Type: " + (value == null ? "null" : value.getClass().getName()));
            }
            return (Type) value;
        } catch (ClassNotFoundException e) {
            throw new IOException("Failed to load IR object graph", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {}
            }
        }
    }
}
