package cod.ir;

import cod.ast.node.Type;
import cod.ptac.CodPTACArtifact;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public final class IRReader {
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
            return IRArtifactCodec.readArtifact(in);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {}
            }
        }
    }
}
