package cod.ir;

import cod.ast.node.Type;
import cod.ptac.CodPTACArtifact;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class IRWriter {
    public void write(File file, Type type) throws IOException {
        CodPTACArtifact artifact = new CodPTACArtifact();
        artifact.className = type != null ? type.name : null;
        artifact.typeSnapshot = type;
        writeArtifact(file, artifact);
    }

    public void writeArtifact(File file, CodPTACArtifact artifact) throws IOException {
        if (file == null) {
            throw new IOException("IR target file is null");
        }
        if (artifact == null) {
            throw new IOException("IR artifact is null");
        }

        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create IR directory: " + parent.getAbsolutePath());
        }

        DataOutputStream out = null;
        try {
            out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            IRArtifactCodec.writeArtifact(out, artifact);
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
