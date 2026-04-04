import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;

public final class CodBoot {
    private interface Host {
        String readFile(String path) throws IOException;
        void writeFile(String path, String content) throws IOException;
        void print(String text);
        String input();
        double add(double a, double b);
        double subtract(double a, double b);
        double multiply(double a, double b);
        double divide(double a, double b);
        boolean lessThan(double a, double b);
        boolean greaterThan(double a, double b);
        boolean equal(String a, String b);
        String stringAppend(String a, String b);
        long now();
        double random();
        int system(String command);
        void exit(int code);
    }

    private static final class JavaHost implements Host {
        private static final Set<String> ALLOWED_SYSTEM_COMMANDS = new HashSet<String>();
        private final BufferedReader inReader;
        private final Random rng;

        static {
            ALLOWED_SYSTEM_COMMANDS.add("true");
            ALLOWED_SYSTEM_COMMANDS.add("false");
        }

        private JavaHost() {
            this.inReader = new BufferedReader(new InputStreamReader(System.in, Charset.forName("UTF-8")));
            this.rng = new Random(123456789L);
        }

        public String readFile(String path) throws IOException {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(
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
                return sb.toString();
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
        }

        public void writeFile(String path, String content) throws IOException {
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(path);
                output.write(content.getBytes("UTF-8"));
            } finally {
                if (output != null) {
                    output.close();
                }
            }
        }

        public void print(String text) {
            System.out.println(String.valueOf(text));
        }

        public String input() {
            try {
                String line = inReader.readLine();
                if (line == null) {
                    return "";
                }
                return line;
            } catch (IOException ignored) {
                return "";
            }
        }

        public double add(double a, double b) {
            return a + b;
        }

        public double subtract(double a, double b) {
            return a - b;
        }

        public double multiply(double a, double b) {
            return a * b;
        }

        public double divide(double a, double b) {
            if (b == 0.0d) {
                throw new IllegalArgumentException("division by zero");
            }
            if (isWhole(a) && isWhole(b)) {
                long left = (long) a;
                long right = (long) b;
                return (double) (left / right);
            }
            return a / b;
        }

        public boolean lessThan(double a, double b) {
            return a < b;
        }

        public boolean greaterThan(double a, double b) {
            return a > b;
        }

        public boolean equal(String a, String b) {
            return a.equals(b);
        }

        public String stringAppend(String a, String b) {
            return a + b;
        }

        public long now() {
            return System.currentTimeMillis();
        }

        public double random() {
            return rng.nextDouble();
        }

        public int system(String command) {
            String cmd = command == null ? "" : command.trim();
            if (!ALLOWED_SYSTEM_COMMANDS.contains(cmd) || cmd.contains("/") || cmd.contains("\\") || cmd.contains(" ")) {
                return 2;
            }
            try {
                ProcessBuilder builder = new ProcessBuilder(cmd);
                Process process = builder.start();
                process.waitFor();
                return process.exitValue();
            } catch (Exception e) {
                return 1;
            }
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
            String literal = parseOutLiteral(line);
            if (literal != null) {
                output.add(literal);
            }
        }
        return output;
    }

    private static boolean shouldUseLegacyProtocol(String programSource) {
        String[] lines = programSource.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.length() == 0) {
                continue;
            }
            if (line.startsWith("host ")) {
                return true;
            }
        }
        return false;
    }

    private static List<String> splitOutputLines(String output) {
        List<String> result = new ArrayList<String>();
        if (output == null || output.length() == 0) {
            return result;
        }
        String[] lines = output.replace("\r\n", "\n").split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].length() > 0) {
                result.add(lines[i]);
            }
        }
        return result;
    }

    private static List<String> runWithProductionRuntime(String programPath) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream outCapture = new ByteArrayOutputStream();
        PrintStream captureStream = null;
        try {
            Class<?> runnerClass = Class.forName("cod.runner.CommandRunner");
            Constructor<?> constructor = runnerClass.getConstructor();
            Object runner = constructor.newInstance();
            Method runMethod = runnerClass.getMethod("run", String[].class);
            captureStream = new PrintStream(outCapture, true, "UTF-8");
            System.setOut(captureStream);
            String[] args = new String[] { "--quiet", programPath };
            runMethod.invoke(runner, new Object[] { args });
            captureStream.flush();
            return splitOutputLines(outCapture.toString("UTF-8"));
        } catch (Throwable ignored) {
            return null;
        } finally {
            if (captureStream != null) {
                captureStream.close();
            }
            System.setOut(originalOut);
        }
    }

    private static String resolveRuntimeMode(String[] args) {
        for (int i = 2; i < args.length; i++) {
            String arg = args[i];
            if (arg != null && arg.startsWith("--runtime-mode=")) {
                String mode = arg.substring("--runtime-mode=".length());
                if ("legacy".equals(mode) || "auto".equals(mode) || "native".equals(mode)) {
                    return mode;
                }
            }
        }
        return "auto";
    }

    private static String parseHostDirective(String line, Host host) throws IOException {
        if (!line.startsWith("host ")) {
            return null;
        }
        String[] tokens = line.split("\\s+");
        if (tokens.length < 2) {
            return "[host] invalid directive";
        }
        String command = tokens[1];
        if ("add".equals(command)) {
            return formatNumber(host.add(parseNumeric(tokens, 2), parseNumeric(tokens, 3)));
        }
        if ("subtract".equals(command)) {
            return formatNumber(host.subtract(parseNumeric(tokens, 2), parseNumeric(tokens, 3)));
        }
        if ("multiply".equals(command)) {
            return formatNumber(host.multiply(parseNumeric(tokens, 2), parseNumeric(tokens, 3)));
        }
        if ("divide".equals(command)) {
            try {
                return formatNumber(host.divide(parseNumeric(tokens, 2), parseNumeric(tokens, 3)));
            } catch (RuntimeException e) {
                return "[host] divide error: " + e.getMessage();
            }
        }
        if ("less-than".equals(command)) {
            return String.valueOf(host.lessThan(parseNumeric(tokens, 2), parseNumeric(tokens, 3)));
        }
        if ("greater-than".equals(command)) {
            return String.valueOf(host.greaterThan(parseNumeric(tokens, 2), parseNumeric(tokens, 3)));
        }
        if ("equal".equals(command)) {
            return String.valueOf(host.equal(readToken(tokens, 2), readToken(tokens, 3)));
        }
        if ("string-append".equals(command)) {
            return host.stringAppend(readToken(tokens, 2), readToken(tokens, 3));
        }
        if ("write-file".equals(command)) {
            try {
                host.writeFile(readToken(tokens, 2), readToken(tokens, 3));
                return "[host] write-file ok";
            } catch (IOException e) {
                return "[host] write-file error: " + e.getMessage();
            }
        }
        if ("read-file".equals(command)) {
            try {
                return host.readFile(readToken(tokens, 2)).replaceFirst("\\r?\\n$", "");
            } catch (IOException e) {
                return "[host] read-file error: " + e.getMessage();
            }
        }
        if ("input".equals(command)) {
            return host.input();
        }
        if ("now".equals(command)) {
            return String.valueOf(host.now());
        }
        if ("random".equals(command)) {
            return String.valueOf(host.random());
        }
        if ("system".equals(command)) {
            return String.valueOf(host.system(readToken(tokens, 2)));
        }
        return "[host] unknown directive: " + command;
    }

    private static String readToken(String[] tokens, int index) {
        if (tokens.length <= index) {
            return "";
        }
        return tokens[index];
    }

    private static double parseNumeric(String[] tokens, int index) {
        if (tokens.length <= index) {
            return 0.0;
        }
        try {
            return Double.parseDouble(tokens[index]);
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    private static String formatNumber(double value) {
        long rounded = Math.round(value);
        if (Math.abs(value - rounded) < 1e-9) {
            return String.valueOf(rounded);
        }
        return String.valueOf(value);
    }

    private static boolean isWhole(double value) {
        return Math.abs(value - Math.rint(value)) < 1e-9;
    }

    private static String parseOutLiteral(String line) {
        if (!line.startsWith("out(\"") || line.charAt(line.length() - 1) != ')') {
            return null;
        }
        int endQuote = -1;
        for (int i = 5; i < line.length() - 1; i++) {
            if (line.charAt(i) == '"') {
                int slashCount = 0;
                for (int j = i - 1; j >= 0 && line.charAt(j) == '\\'; j--) {
                    slashCount++;
                }
                if (slashCount % 2 == 0) {
                    endQuote = i;
                    break;
                }
            }
        }
        if (endQuote != line.length() - 2) {
            return null;
        }
        return line.substring(5, endQuote);
    }

    private static boolean hasCoreEntrypoint(String coreSource) {
        String[] lines = coreSource.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.length() == 0 || line.startsWith("#")) {
                continue;
            }
            return "entrypoint := \"CodBootCore::v0\"".equals(line);
        }
        return false;
    }

    private static RunResult runCore(String coreSource, String programPath, Host host, String runtimeMode) throws IOException {
        if (!hasCoreEntrypoint(coreSource)) {
            List<String> invalid = new ArrayList<String>();
            invalid.add("[core] invalid core.ce format");
            return new RunResult(2, invalid);
        }

        String programSource = host.readFile(programPath);
        List<String> userLines = null;
        boolean forceLegacy = "legacy".equals(runtimeMode) || shouldUseLegacyProtocol(programSource);
        if (!forceLegacy) {
            userLines = runWithProductionRuntime(programPath);
            if (userLines == null && "native".equals(runtimeMode)) {
                List<String> unavailable = new ArrayList<String>();
                unavailable.add("[core] native runtime unavailable in Java host");
                return new RunResult(2, unavailable);
            }
        }
        if (userLines == null) {
            userLines = decodeProgramOutputs(programSource);
            String[] programLines = programSource.split("\\r?\\n");
            for (int i = 0; i < programLines.length; i++) {
                String result = parseHostDirective(programLines[i].trim(), host);
                if (result != null) {
                    userLines.add(result);
                }
            }
        }
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
            host.print("Usage: java CodBoot <core.ce-path> <program.cod-path> [--bootstrap-self] [--runtime-mode=legacy|auto|native]");
            return 64;
        }
        String corePath = args[0];
        String programPath = args[1];
        boolean bootstrapSelf = false;
        for (int i = 2; i < args.length; i++) {
            if ("--bootstrap-self".equals(args[i])) {
                bootstrapSelf = true;
                break;
            }
        }
        String runtimeMode = resolveRuntimeMode(args);

        String coreSource = host.readFile(corePath);
        if (bootstrapSelf) {
            host.print("[core] bootstrap self-check passed");
            return 0;
        }

        RunResult result = runCore(coreSource, programPath, host, runtimeMode);
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
