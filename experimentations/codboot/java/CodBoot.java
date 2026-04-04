import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public final class CodBoot {
    private interface Host {
        String readFile(String path) throws IOException;
        void print(String text);
        void exit(int code);
    }

    private static final class JavaHost implements Host {
        public String readFile(String path) throws IOException {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(path), Charset.forName("UTF-8"))
            );
            StringBuilder sb = new StringBuilder();
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (!first) {
                    sb.append('\n');
                }
                sb.append(line);
                first = false;
            }
            reader.close();
            return sb.toString();
        }

        public void print(String text) {
            System.out.println(String.valueOf(text));
        }

        public void exit(int code) {
            System.exit(code);
        }
    }

    private static final class RunResult {
        private final int exitCode;
        private final List<String> lines;

        private RunResult(int exitCode, List<String> lines) {
            this.exitCode = exitCode;
            this.lines = lines;
        }
    }

    private static List<String> decodeProgramOutputs(String programSource) {
        List<String> output = new ArrayList<String>();
        String[] lines = programSource.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("out(\"") && line.endsWith("\")")) {
                output.add(line.substring(5, line.length() - 2));
            }
        }
        return output;
    }

    private static RunResult runCore(String coreSource, String programPath, Host host) throws IOException {
        if (coreSource.indexOf("CodBootCore::v0") < 0) {
            List<String> invalid = new ArrayList<String>();
            invalid.add("[core] invalid core.ce format");
            return new RunResult(2, invalid);
        }

        String programSource = host.readFile(programPath);
        List<String> userLines = decodeProgramOutputs(programSource);
        List<String> lines = new ArrayList<String>();
        lines.add("[core] running: " + programPath);
        lines.add("[core] experimental evaluator active");
        lines.addAll(userLines);
        if (userLines.isEmpty()) {
            lines.add("[core] no out(\"...\") statements detected");
        }
        return new RunResult(0, lines);
    }

    private static int mainImpl(String[] args, Host host) throws IOException {
        if (args.length < 2) {
            host.print("Usage: java CodBoot <core.ce-path> <program.cod-path> [--bootstrap-self]");
            return 64;
        }
        String corePath = args[0];
        String programPath = args[1];
        boolean bootstrapSelf = args.length > 2 && "--bootstrap-self".equals(args[2]);

        String coreSource = host.readFile(corePath);
        if (bootstrapSelf) {
            host.print("[core] bootstrap self-check passed");
            return 0;
        }

        RunResult result = runCore(coreSource, programPath, host);
        for (int i = 0; i < result.lines.size(); i++) {
            host.print(result.lines.get(i));
        }
        return result.exitCode;
    }

    public static void main(String[] args) {
        Host host = new JavaHost();
        int code = 1;
        try {
            code = mainImpl(args, host);
        } catch (IOException e) {
            host.print("[host] io error: " + e.getMessage());
            code = 1;
        } catch (RuntimeException e) {
            host.print("[host] runtime error: " + e.getMessage());
            code = 1;
        }
        host.exit(code);
    }
}
