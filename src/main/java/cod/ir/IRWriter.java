package cod.ir;

import cod.ast.node.Type;
import cod.ptac.CodPTACArtifact;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

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

        ObjectOutputStream objectOut = null;
        try {
            objectOut = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            objectOut.writeObject(artifact);
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
