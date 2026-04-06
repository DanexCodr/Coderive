import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public final class CodBoot {
    // This constant is needed before core semantics are parsed; keep in sync with semantics_json messages.parseEvalErrorPrefix in core.ce.
    private static final String CORE_PARSE_EVAL_ERROR_PREFIX = "[core] parse/eval error: ";
    private static final String CORE_MISSING_SEMANTICS_KEY_PREFIX = "[core] missing semantics key: ";
    // Keep in sync with core.ce semantics_json missing-semantics error contract.
    private static final String CORE_MISSING_SEMANTICS_JSON_MESSAGE = "[core] missing semantics_json block";
    // Matches JSON string literals and captures escaped content between quotes.
    // Unlike JSON_NUMBER_VALUE_REGEX (a string template expanded at runtime per key),
    // this Pattern is compiled once and reused directly for key-agnostic string item extraction.
    private static final Pattern JSON_STRING_ITEM_PATTERN = Pattern.compile("\"((?:\\\\.|[^\\\\\"])*)\"");
    // Matches JSON numeric values used by semantics payload: optional sign, integer part, optional decimal part, optional exponent.
    // Capture group 1 returns the number text only: -? (sign), \d+ (integer), (?:\.\d+)? (fraction), (?:[eE][+-]?\d+)? (exponent).
    // Note: this intentionally does not support non-JSON forms like leading-dot `.5` or trailing-dot `0.`.
    // Kept as a template string because the JSON key is dynamic and inserted via String.format.
    private static final String JSON_NUMBER_VALUE_REGEX = "\"%s\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)";

    private interface Host {
        String readFile(String path) throws IOException;
        void writeFile(String path, String content) throws IOException;
        void print(String text);
        String input();
        String consumeRemainingInput();
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
        private final List<String> inputBuffer;

        static {
            ALLOWED_SYSTEM_COMMANDS.add("true");
            ALLOWED_SYSTEM_COMMANDS.add("false");
        }

        private JavaHost() {
            this.inReader = new BufferedReader(new InputStreamReader(System.in, Charset.forName("UTF-8")));
            this.rng = new Random(123456789L);
            this.inputBuffer = new ArrayList<String>();
        }

        public String readFile(String path) throws IOException {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), Charset.forName("UTF-8")));
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
                if (!inputBuffer.isEmpty()) {
                    return inputBuffer.remove(0);
                }
                String line = inReader.readLine();
                return line == null ? "" : line;
            } catch (IOException ignored) {
                return "";
            }
        }

        public String consumeRemainingInput() {
            try {
                String line;
                while ((line = inReader.readLine()) != null) {
                    inputBuffer.add(line);
                }
            } catch (IOException ignored) {
            }
            if (inputBuffer.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < inputBuffer.size(); i++) {
                if (i > 0) {
                    sb.append('\n');
                }
                sb.append(inputBuffer.get(i));
            }
            inputBuffer.clear();
            return sb.toString();
        }

        public double add(double a, double b) { return a + b; }
        public double subtract(double a, double b) { return a - b; }
        public double multiply(double a, double b) { return a * b; }

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

        public boolean lessThan(double a, double b) { return a < b; }
        public boolean greaterThan(double a, double b) { return a > b; }
        public boolean equal(String a, String b) { return a.equals(b); }
        public String stringAppend(String a, String b) { return a + b; }
        public long now() { return System.currentTimeMillis(); }
        public double random() { return rng.nextDouble(); }

        public int system(String command) {
            String cmd = command == null ? "" : command.trim();
            // Defense-in-depth: explicitly block path separators even with strict allowlist + metachar filtering.
            if (!ALLOWED_SYSTEM_COMMANDS.contains(cmd) || containsPathSeparator(cmd) || containsUnsafeShellChar(cmd)) {
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

        private boolean containsUnsafeShellChar(String cmd) {
            for (int i = 0; i < cmd.length(); i++) {
                char ch = cmd.charAt(i);
                if (Character.isWhitespace(ch) || ch == ';' || ch == '|' || ch == '&' || ch == '$' || ch == '`') {
                    return true;
                }
            }
            return false;
        }

        private boolean containsPathSeparator(String cmd) {
            return cmd.indexOf('/') >= 0 || cmd.indexOf('\\') >= 0;
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

    private static final class RunnerResult {
        private final int exitCode;
        private final List<String> lines;
        private final String stderr;

        private RunnerResult(int exitCode, List<String> lines, String stderr) {
            this.exitCode = exitCode;
            this.lines = lines;
            this.stderr = stderr;
        }
    }

    private static final class CoreSemantics {
        private final String keywordOut;
        private final String keywordHost;
        private final boolean lexerAllowParentheses;
        private final boolean lexerHashCommentsEnabled;
        private final boolean lexerDoubleSlashCommentsEnabled;
        private final double evaluatorWholeNumberTolerance;
        private final String evaluatorWholeNumberMode;
        private final String invalidCoreFormat;
        private final String runningPrefix;
        private final String experimentalEvaluatorActive;
        private final String bootstrapSelfCheckPassed;
        private final String parseEvalErrorPrefix;
        private final String noOutStatementsDetected;
        private final String selfHostOnlyNoFallback;
        private final String unknownDirectivePrefix;
        private final String divideErrorPrefix;
        private final String writeFileOk;
        private final String writeFileErrorPrefix;
        private final String readFileErrorPrefix;
        private final String cmdAdd;
        private final String cmdSubtract;
        private final String cmdMultiply;
        private final String cmdDivide;
        private final String cmdLessThan;
        private final String cmdGreaterThan;
        private final String cmdEqual;
        private final String cmdStringAppend;
        private final String cmdWriteFile;
        private final String cmdReadFile;
        private final String cmdInput;
        private final String cmdNow;
        private final String cmdRandom;
        private final String cmdSystem;

        private CoreSemantics(
            String keywordOut,
            String keywordHost,
            boolean lexerAllowParentheses,
            boolean lexerHashCommentsEnabled,
            boolean lexerDoubleSlashCommentsEnabled,
            double evaluatorWholeNumberTolerance,
            String evaluatorWholeNumberMode,
            String invalidCoreFormat,
            String runningPrefix,
            String experimentalEvaluatorActive,
            String bootstrapSelfCheckPassed,
            String parseEvalErrorPrefix,
            String noOutStatementsDetected,
            String selfHostOnlyNoFallback,
            String unknownDirectivePrefix,
            String divideErrorPrefix,
            String writeFileOk,
            String writeFileErrorPrefix,
            String readFileErrorPrefix,
            String cmdAdd,
            String cmdSubtract,
            String cmdMultiply,
            String cmdDivide,
            String cmdLessThan,
            String cmdGreaterThan,
            String cmdEqual,
            String cmdStringAppend,
            String cmdWriteFile,
            String cmdReadFile,
            String cmdInput,
            String cmdNow,
            String cmdRandom,
            String cmdSystem
        ) {
            this.keywordOut = keywordOut;
            this.keywordHost = keywordHost;
            this.lexerAllowParentheses = lexerAllowParentheses;
            this.lexerHashCommentsEnabled = lexerHashCommentsEnabled;
            this.lexerDoubleSlashCommentsEnabled = lexerDoubleSlashCommentsEnabled;
            this.evaluatorWholeNumberTolerance = evaluatorWholeNumberTolerance;
            this.evaluatorWholeNumberMode = evaluatorWholeNumberMode;
            this.invalidCoreFormat = invalidCoreFormat;
            this.runningPrefix = runningPrefix;
            this.experimentalEvaluatorActive = experimentalEvaluatorActive;
            this.bootstrapSelfCheckPassed = bootstrapSelfCheckPassed;
            this.parseEvalErrorPrefix = parseEvalErrorPrefix;
            this.noOutStatementsDetected = noOutStatementsDetected;
            this.selfHostOnlyNoFallback = selfHostOnlyNoFallback;
            this.unknownDirectivePrefix = unknownDirectivePrefix;
            this.divideErrorPrefix = divideErrorPrefix;
            this.writeFileOk = writeFileOk;
            this.writeFileErrorPrefix = writeFileErrorPrefix;
            this.readFileErrorPrefix = readFileErrorPrefix;
            this.cmdAdd = cmdAdd;
            this.cmdSubtract = cmdSubtract;
            this.cmdMultiply = cmdMultiply;
            this.cmdDivide = cmdDivide;
            this.cmdLessThan = cmdLessThan;
            this.cmdGreaterThan = cmdGreaterThan;
            this.cmdEqual = cmdEqual;
            this.cmdStringAppend = cmdStringAppend;
            this.cmdWriteFile = cmdWriteFile;
            this.cmdReadFile = cmdReadFile;
            this.cmdInput = cmdInput;
            this.cmdNow = cmdNow;
            this.cmdRandom = cmdRandom;
            this.cmdSystem = cmdSystem;
        }
    }

    private static boolean hasCoreEntrypoint(String coreSource) {
        String[] lines = coreSource.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.length() == 0 || line.startsWith("#") || line.startsWith("//")) {
                continue;
            }
            return "entrypoint := \"CodBootCore::v0\"".equals(line);
        }
        return false;
    }

    private static String extractSemanticsJson(String coreSource) {
        Pattern triplePattern = Pattern.compile("semantics_json\\s*:=\\s*\"\"\"\\s*([\\s\\S]*?)\\s*\"\"\"");
        Matcher tripleMatcher = triplePattern.matcher(coreSource);
        if (tripleMatcher.find()) {
            return tripleMatcher.group(1);
        }

        Pattern commentPattern = Pattern.compile("//\\s*semantics_json_begin\\s*\\r?\\n([\\s\\S]*?)//\\s*semantics_json_end");
        Matcher commentMatcher = commentPattern.matcher(coreSource);
        if (commentMatcher.find()) {
            String[] lines = commentMatcher.group(1).split("\\r?\\n");
            StringBuilder json = new StringBuilder();
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                Matcher lineMatcher = Pattern.compile("^\\s*//\\s?(.*)$").matcher(line);
                if (lineMatcher.find()) {
                    if (json.length() > 0) {
                        json.append('\n');
                    }
                    json.append(lineMatcher.group(1));
                }
            }
            String value = json.toString().trim();
            if (value.length() > 0) {
                return value;
            }
        }

        Pattern singlePattern = Pattern.compile("semantics_json\\s*:=\\s*\"((?:\\\\.|[^\\\\\"])*)\"");
        Matcher singleMatcher = singlePattern.matcher(coreSource);
        if (!singleMatcher.find()) {
            return "";
        }
        return unescapeJsonString(singleMatcher.group(1));
    }

    private static String unescapeJsonString(String value) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\\' && i + 1 < value.length()) {
                char esc = value.charAt(i + 1);
                if (esc == 'n') {
                    out.append('\n');
                } else if (esc == 't') {
                    out.append('\t');
                } else if (esc == 'r') {
                    out.append('\r');
                } else if (esc == '"') {
                    out.append('"');
                } else if (esc == '\\') {
                    out.append('\\');
                } else if (esc == '/') {
                    out.append('/');
                } else if (esc == 'b') {
                    out.append('\b');
                } else if (esc == 'f') {
                    out.append('\f');
                } else {
                    out.append(esc);
                }
                i += 1;
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }

    private static String requireJsonStringValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\\\\\"])*)\"");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            throw new RuntimeException(CORE_MISSING_SEMANTICS_KEY_PREFIX + key);
        }
        return unescapeJsonString(matcher.group(1));
    }

    private static boolean requireJsonBooleanValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(true|false)");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            throw new RuntimeException(CORE_MISSING_SEMANTICS_KEY_PREFIX + key);
        }
        return "true".equals(matcher.group(1));
    }

    private static double requireJsonNumberValue(String json, String key) {
        Pattern pattern = Pattern.compile(String.format(JSON_NUMBER_VALUE_REGEX, Pattern.quote(key)));
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            throw new RuntimeException(CORE_MISSING_SEMANTICS_KEY_PREFIX + key);
        }
        return Double.parseDouble(matcher.group(1));
    }

    private static boolean jsonArrayContainsString(String json, String arrayKey, String value) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(arrayKey) + "\"\\s*:\\s*\\[([\\s\\S]*?)\\]");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            throw new RuntimeException(CORE_MISSING_SEMANTICS_KEY_PREFIX + arrayKey);
        }
        String body = matcher.group(1);
        Matcher itemMatcher = JSON_STRING_ITEM_PATTERN.matcher(body);
        while (itemMatcher.find()) {
            String item = unescapeJsonString(itemMatcher.group(1));
            if (value.equals(item)) {
                return true;
            }
        }
        return false;
    }

    private static CoreSemantics parseCoreSemantics(String coreSource) {
        String json = extractSemanticsJson(coreSource);
        if (json.length() == 0) {
            throw new RuntimeException(CORE_MISSING_SEMANTICS_JSON_MESSAGE);
        }
        return new CoreSemantics(
            requireJsonStringValue(json, "out"),
            requireJsonStringValue(json, "host"),
            requireJsonBooleanValue(json, "allowParentheses"),
            jsonArrayContainsString(json, "lineComments", "#"),
            jsonArrayContainsString(json, "lineComments", "//"),
            requireJsonNumberValue(json, "wholeNumberTolerance"),
            requireJsonStringValue(json, "wholeNumberMode"),
            requireJsonStringValue(json, "invalidCoreFormat"),
            requireJsonStringValue(json, "runningPrefix"),
            requireJsonStringValue(json, "experimentalEvaluatorActive"),
            requireJsonStringValue(json, "bootstrapSelfCheckPassed"),
            requireJsonStringValue(json, "parseEvalErrorPrefix"),
            requireJsonStringValue(json, "noOutStatementsDetected"),
            requireJsonStringValue(json, "selfHostOnlyNoFallback"),
            requireJsonStringValue(json, "unknownDirectivePrefix"),
            requireJsonStringValue(json, "divideErrorPrefix"),
            requireJsonStringValue(json, "writeFileOk"),
            requireJsonStringValue(json, "writeFileErrorPrefix"),
            requireJsonStringValue(json, "readFileErrorPrefix"),
            requireJsonStringValue(json, "add"),
            requireJsonStringValue(json, "subtract"),
            requireJsonStringValue(json, "multiply"),
            requireJsonStringValue(json, "divide"),
            requireJsonStringValue(json, "lessThan"),
            requireJsonStringValue(json, "greaterThan"),
            requireJsonStringValue(json, "equal"),
            requireJsonStringValue(json, "stringAppend"),
            requireJsonStringValue(json, "writeFile"),
            requireJsonStringValue(json, "readFile"),
            requireJsonStringValue(json, "input"),
            requireJsonStringValue(json, "now"),
            requireJsonStringValue(json, "random"),
            requireJsonStringValue(json, "system")
        );
    }

    private static Object parseAtom(String token) {
        if (token == null) {
            return "";
        }
        if (token.matches("^-?\\d+(\\.\\d+)?$")) {
            return Double.valueOf(token);
        }
        return token;
    }

    private static String readToken(List<String> tokens, int index) {
        if (index < 0 || index >= tokens.size()) {
            return "";
        }
        return tokens.get(index);
    }

    private static double asNumber(Object value) {
        if (value instanceof Double) {
            return ((Double) value).doubleValue();
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0.0d;
        }
    }

    private static long truncateTowardZero(double value) {
        return (long) (value >= 0 ? Math.floor(value) : Math.ceil(value));
    }

    private static String formatNumber(double value, CoreSemantics semantics) {
        String mode = semantics.evaluatorWholeNumberMode;
        if (!"round".equals(mode) && !"trunc".equals(mode)) {
            throw new RuntimeException("invalid wholeNumberMode: " + mode + ". Expected \"round\" or \"trunc\"");
        }
        long whole = "trunc".equals(mode) ? truncateTowardZero(value) : Math.round(value);
        if (Math.abs(value - whole) < semantics.evaluatorWholeNumberTolerance) {
            return String.valueOf(whole);
        }
        return String.valueOf(value);
    }

    private static boolean isWhole(double value) {
        return Math.abs(value - Math.rint(value)) < 1e-9;
    }

    private static String evaluateHost(String command, List<String> args, Host host, CoreSemantics semantics) {
        if (semantics.cmdAdd.equals(command)) {
            return formatNumber(host.add(asNumber(parseAtom(readToken(args, 0))), asNumber(parseAtom(readToken(args, 1)))), semantics);
        }
        if (semantics.cmdSubtract.equals(command)) {
            return formatNumber(host.subtract(asNumber(parseAtom(readToken(args, 0))), asNumber(parseAtom(readToken(args, 1)))), semantics);
        }
        if (semantics.cmdMultiply.equals(command)) {
            return formatNumber(host.multiply(asNumber(parseAtom(readToken(args, 0))), asNumber(parseAtom(readToken(args, 1)))), semantics);
        }
        if (semantics.cmdDivide.equals(command)) {
            try {
                return formatNumber(host.divide(asNumber(parseAtom(readToken(args, 0))), asNumber(parseAtom(readToken(args, 1)))), semantics);
            } catch (RuntimeException e) {
                return semantics.divideErrorPrefix + e.getMessage();
            }
        }
        if (semantics.cmdLessThan.equals(command)) {
            return String.valueOf(host.lessThan(asNumber(parseAtom(readToken(args, 0))), asNumber(parseAtom(readToken(args, 1)))));
        }
        if (semantics.cmdGreaterThan.equals(command)) {
            return String.valueOf(host.greaterThan(asNumber(parseAtom(readToken(args, 0))), asNumber(parseAtom(readToken(args, 1)))));
        }
        if (semantics.cmdEqual.equals(command)) {
            return String.valueOf(host.equal(String.valueOf(parseAtom(readToken(args, 0))), String.valueOf(parseAtom(readToken(args, 1)))));
        }
        if (semantics.cmdStringAppend.equals(command)) {
            return host.stringAppend(readToken(args, 0), readToken(args, 1));
        }
        if (semantics.cmdWriteFile.equals(command)) {
            try {
                host.writeFile(readToken(args, 0), readToken(args, 1));
                return semantics.writeFileOk;
            } catch (IOException e) {
                return semantics.writeFileErrorPrefix + e.getMessage();
            }
        }
        if (semantics.cmdReadFile.equals(command)) {
            try {
                return host.readFile(readToken(args, 0)).replaceFirst("\\r?\\n$", "");
            } catch (IOException e) {
                return semantics.readFileErrorPrefix + e.getMessage();
            }
        }
        if (semantics.cmdInput.equals(command)) {
            return host.input();
        }
        if (semantics.cmdNow.equals(command)) {
            return String.valueOf(host.now());
        }
        if (semantics.cmdRandom.equals(command)) {
            return String.valueOf(host.random());
        }
        if (semantics.cmdSystem.equals(command)) {
            return String.valueOf(host.system(readToken(args, 0)));
        }
        return semantics.unknownDirectivePrefix + command;
    }


    private static RunResult runCore(String coreSource, String corePath, String programPath, Host host, CoreSemantics semantics) throws IOException {
        if (!hasCoreEntrypoint(coreSource)) {
            List<String> invalid = new ArrayList<String>();
            invalid.add(semantics.invalidCoreFormat);
            return new RunResult(2, invalid);
        }

        String programSource = host.readFile(programPath);
        List<String> userLines;
        try {
            if (isLegacyCodBootProgram(programSource, semantics)) {
                userLines = runLegacyCodBoot(programSource, host, semantics);
            } else {
                String hostInput = host.consumeRemainingInput();
                RunnerResult runner = runViaCommandRunner(programPath, hostInput, corePath);
                if (runner.exitCode != 0) {
                    List<String> parseError = new ArrayList<String>();
                    parseError.add(semantics.parseEvalErrorPrefix + (runner.stderr.length() > 0 ? runner.stderr : "CommandRunner failed"));
                    return new RunResult(2, parseError);
                }
                userLines = runner.lines;
            }
        } catch (RuntimeException e) {
            List<String> parseError = new ArrayList<String>();
            parseError.add(semantics.parseEvalErrorPrefix + e.getMessage());
            return new RunResult(2, parseError);
        }

        List<String> lines = new ArrayList<String>();
        lines.add(semantics.runningPrefix + programPath);
        lines.add(semantics.experimentalEvaluatorActive);
        lines.addAll(userLines);
        if (userLines.isEmpty()) {
            lines.add(semantics.noOutStatementsDetected);
        }
        return new RunResult(0, lines);
    }

    private static String deriveRepoRootFromCorePath(String corePath) {
        File dir = new File(corePath).getParentFile();
        if (dir == null) {
            return new File(".").getAbsolutePath();
        }
        File root = dir;
        for (int i = 0; i < 3 && root != null; i++) {
            root = root.getParentFile();
        }
        return root == null ? new File(".").getAbsolutePath() : root.getAbsolutePath();
    }

    private static String resolveCoderiveJarPath(String corePath) {
        String envJar = System.getenv("CODERIVE_JAR");
        if (envJar != null && envJar.length() > 0 && new File(envJar).exists()) {
            return envJar;
        }
        String fromCwd = findJarFromDir(new File(".").getAbsoluteFile());
        if (fromCwd.length() > 0) {
            return fromCwd;
        }
        File coreDir = new File(corePath).getParentFile();
        String fromCore = coreDir == null ? "" : findJarFromDir(coreDir);
        if (fromCore.length() > 0) {
            return fromCore;
        }
        return new File(deriveRepoRootFromCorePath(corePath), "docs" + File.separator + "assets" + File.separator + "Coderive.jar").getPath();
    }

    private static String findJarFromDir(File startDir) {
        File dir = startDir;
        for (int i = 0; i < 10 && dir != null; i++) {
            File candidate = new File(dir, "docs" + File.separator + "assets" + File.separator + "Coderive.jar");
            if (candidate.exists()) {
                return candidate.getPath();
            }
            dir = dir.getParentFile();
        }
        return "";
    }

    private static RunnerResult runViaCommandRunner(String programPath, String hostInput, String corePath) throws IOException {
        List<String> command = new ArrayList<String>();
        String jarPath = resolveCoderiveJarPath(corePath);
        validateBridgePath(jarPath, "jar");
        validateBridgePath(programPath, "program");
        command.add("java");
        command.add("-cp");
        command.add(jarPath);
        command.add("cod.runner.CommandRunner");
        command.add(programPath);
        command.add("--quiet");
        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();
        if (hostInput != null && hostInput.length() > 0) {
            OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), Charset.forName("UTF-8"));
            writer.write(hostInput);
            if (!hostInput.endsWith("\n")) {
                writer.write('\n');
            }
            writer.flush();
            writer.close();
        } else {
            process.getOutputStream().close();
        }
        String stdout = trimTrailingNewlines(readStream(process.getInputStream()));
        String stderr = trimTrailingNewlines(readStream(process.getErrorStream()));
        int code;
        try {
            code = process.waitFor();
        } catch (InterruptedException e) {
            throw new IOException("Interrupted while waiting for CommandRunner", e);
        }
        List<String> lines = new ArrayList<String>();
        if (stdout.length() > 0) {
            String normalized = stdout.replace("\r\n", "\n");
            if (normalized.endsWith("\n")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            if (normalized.length() > 0) {
                String[] split = normalized.split("\n", -1);
                for (int i = 0; i < split.length; i++) {
                    lines.add(split[i]);
                }
            }
        }
        return new RunnerResult(code, lines, stderr);
    }

    private static String readStream(java.io.InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int n;
        while ((n = in.read(data)) != -1) {
            buffer.write(data, 0, n);
        }
        return new String(buffer.toByteArray(), "UTF-8");
    }

    private static String trimTrailingNewlines(String text) {
        int end = text.length();
        while (end > 0) {
            char ch = text.charAt(end - 1);
            if (ch == '\n' || ch == '\r') {
                end--;
            } else {
                break;
            }
        }
        return text.substring(0, end);
    }

    private static void validateBridgePath(String filePath, String label) {
        String value = filePath == null ? "" : filePath;
        if (value.length() == 0 || value.indexOf('\0') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new RuntimeException("invalid " + label + " path");
        }
        if (!new File(value).exists()) {
            throw new RuntimeException(label + " path not found: " + value);
        }
    }

    private static boolean isLegacyCodBootProgram(String source, CoreSemantics semantics) {
        String[] lines = source.split("\\r?\\n");
        String outPrefix = semantics.keywordOut + "(";
        String hostPrefix = semantics.keywordHost + " ";
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.length() == 0 || line.startsWith("#") || line.startsWith("//")) {
                continue;
            }
            if (line.startsWith(outPrefix) || line.startsWith(hostPrefix)) {
                continue;
            }
            return false;
        }
        return true;
    }

    private static String parseLegacyStringLiteral(String text) {
        if (text == null || text.length() < 2 || text.charAt(0) != '"' || text.charAt(text.length() - 1) != '"') {
            throw new RuntimeException("Invalid out statement: " + text);
        }
        StringBuilder out = new StringBuilder();
        for (int i = 1; i < text.length() - 1; i++) {
            char ch = text.charAt(i);
            if (ch == '\\') {
                if (i + 1 >= text.length() - 1) {
                    throw new RuntimeException("Unterminated string literal in legacy CodBoot statement");
                }
                char esc = text.charAt(i + 1);
                if (esc == 'n') {
                    out.append('\n');
                } else if (esc == 't') {
                    out.append('\t');
                } else if (esc == '"' || esc == '\\') {
                    out.append(esc);
                } else {
                    out.append(esc);
                }
                i += 1;
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }

    private static List<String> splitLegacyHostArgs(String text) {
        List<String> args = new ArrayList<String>();
        int i = 0;
        while (i < text.length()) {
            while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
                i++;
            }
            if (i >= text.length()) {
                break;
            }
            if (text.charAt(i) == '"') {
                StringBuilder token = new StringBuilder();
                token.append('"');
                i++;
                boolean closed = false;
                while (i < text.length()) {
                    char ch = text.charAt(i);
                    token.append(ch);
                    if (ch == '\\') {
                        i++;
                        if (i < text.length()) {
                            token.append(text.charAt(i));
                        }
                    } else if (ch == '"') {
                        closed = true;
                        i++;
                        break;
                    }
                    i++;
                }
                if (!closed) {
                    throw new RuntimeException("Unterminated string literal in host statement");
                }
                args.add(parseLegacyStringLiteral(token.toString()));
                continue;
            }
            int start = i;
            while (i < text.length() && !Character.isWhitespace(text.charAt(i))) {
                i++;
            }
            args.add(text.substring(start, i));
        }
        return args;
    }

    private static List<String> runLegacyCodBoot(String source, Host host, CoreSemantics semantics) {
        List<String> out = new ArrayList<String>();
        String[] lines = source.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.length() == 0 || line.startsWith("#") || line.startsWith("//")) {
                continue;
            }
            if (line.startsWith(semantics.keywordOut + "(")) {
                if (!line.endsWith(")")) {
                    throw new RuntimeException("Invalid out statement: " + line);
                }
                String payload = line.substring(semantics.keywordOut.length() + 1, line.length() - 1).trim();
                out.add(parseLegacyStringLiteral(payload));
                continue;
            }
            if (line.startsWith(semantics.keywordHost + " ")) {
                String body = line.substring((semantics.keywordHost + " ").length());
                int splitAt = -1;
                for (int idx = 0; idx < body.length(); idx++) {
                    if (Character.isWhitespace(body.charAt(idx))) {
                        splitAt = idx;
                        break;
                    }
                }
                String command;
                List<String> args;
                if (splitAt < 0) {
                    command = body;
                    args = new ArrayList<String>();
                } else {
                    command = body.substring(0, splitAt);
                    args = splitLegacyHostArgs(body.substring(splitAt + 1));
                }
                out.add(evaluateHost(command, args, host, semantics));
                continue;
            }
        }
        return out;
    }

    private static boolean isParseEvalError(RunResult result, CoreSemantics semantics) {
        return result.exitCode != 0
            && !result.lines.isEmpty()
            && result.lines.get(0).startsWith(semantics.parseEvalErrorPrefix);
    }

    private static int mainImpl(String[] args, Host host) throws IOException {
        if (args.length < 2) {
            host.print("Usage: java CodBoot <core.ce-path> <program.cod-path> [--bootstrap-self]");
            return 64;
        }

        String corePath = args[0];
        String programPath = args[1];
        boolean bootstrapSelf = false;
        boolean selfHostOnly = false;
        for (int i = 2; i < args.length; i++) {
            if ("--bootstrap-self".equals(args[i])) {
                bootstrapSelf = true;
            }
            if ("--self-host-only".equals(args[i])) {
                selfHostOnly = true;
            }
        }

        String coreSource = host.readFile(corePath);
        CoreSemantics semantics;
        try {
            semantics = parseCoreSemantics(coreSource);
        } catch (RuntimeException e) {
            host.print(CORE_PARSE_EVAL_ERROR_PREFIX + e.getMessage());
            return 2;
        }
        if (bootstrapSelf) {
            host.print(semantics.bootstrapSelfCheckPassed);
            return 0;
        }

        RunResult result = runCore(coreSource, corePath, programPath, host, semantics);
        if (selfHostOnly && isParseEvalError(result, semantics)) {
            result.lines.add(semantics.selfHostOnlyNoFallback);
        }
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
