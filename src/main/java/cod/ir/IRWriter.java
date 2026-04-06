package cod.ir;

import cod.ast.node.Type;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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

        DataOutputStream out = null;
        try {
            out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            IRCodec.writeHeader(out);
            IRCodec.writeValue(out, type, 0);
            out.flush();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {}
            }
        }
    }
}
