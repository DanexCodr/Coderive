package cod.ir;

import cod.ast.node.Type;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public final class IRWriter {
    static final int IR_STREAM_MAGIC = 0xAC0D1EB2;
    static final int IR_STREAM_VERSION = 1;

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

        BufferedOutputStream buffered = null;
        DataOutputStream headerOut = null;
        ObjectOutputStream objectOut = null;
        try {
            buffered = new BufferedOutputStream(new FileOutputStream(file));
            headerOut = new DataOutputStream(buffered);
            headerOut.writeInt(IR_STREAM_MAGIC);
            headerOut.writeInt(IR_STREAM_VERSION);
            headerOut.flush();

            objectOut = new ObjectOutputStream(buffered);
            objectOut.writeObject(type);
            objectOut.flush();
        } finally {
            if (objectOut != null) {
                try {
                    objectOut.close();
                } catch (IOException ignored) {}
            } else if (headerOut != null) {
                try {
                    headerOut.close();
                } catch (IOException ignored) {}
            } else if (buffered != null) {
                try {
                    buffered.close();
                } catch (IOException ignored) {}
            }
        }
    }
}
