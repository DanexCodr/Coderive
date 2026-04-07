package cod.ir;

import cod.ast.node.Type;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public final class IRWriter {
    public void write(File file, Type type) throws IOException {
        if (file == null) {
            throw new IOException("IR target file is null");
        }
        if (type == null) {
            throw new IOException("IR type is null");
        }

        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create IR directory: " + parent.getAbsolutePath());
        }

        ObjectOutputStream objectOut = null;
        try {
            objectOut = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            objectOut.writeObject(type);
            objectOut.flush();
        } finally {
            if (objectOut != null) {
                try {
                    objectOut.close();
                } catch (IOException ignored) {}
            }
        }
    }
}
