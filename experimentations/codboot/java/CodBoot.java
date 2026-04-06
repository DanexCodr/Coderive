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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public final class CodBoot {
    // This constant is needed before core semantics are parsed; fallback prefix must match core message format.
    private static final String CORE_PARSE_EVAL_ERROR_PREFIX = "[core] parse/eval error: ";

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
                String line = inReader.readLine();
                return line == null ? "" : line;
            } catch (IOException ignored) {
                return "";
            }
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

    private static final class Token {
        private final String type;
        private final String value;
        private final int line;
        private final int column;

        private Token(String type, String value, int line, int column) {
            this.type = type;
            this.value = value;
            this.line = line;
            this.column = column;
        }
    }

    private static final class Lexer {
        private final String source;
        private int index;
        private int line;
        private int column;

        private Lexer(String source) {
            this.source = source;
            this.index = 0;
            this.line = 1;
            this.column = 1;
        }

        private char currentChar() {
            if (index >= source.length()) {
                return '\0';
            }
            return source.charAt(index);
        }

        private void advance() {
            char ch = currentChar();
            if (ch == '\n') {
                line += 1;
                column = 1;
            } else {
                column += 1;
            }
            index += 1;
        }

        private Token readString(int startLine, int startColumn) {
            StringBuilder result = new StringBuilder();
            advance();
            while (index < source.length()) {
                char ch = currentChar();
                if (ch == '"') {
                    advance();
                    return new Token("STRING", result.toString(), startLine, startColumn);
                }
                if (ch == '\\') {
                    advance();
                    char esc = currentChar();
                    if (esc == 'n') {
                        result.append('\n');
                    } else if (esc == 't') {
                        result.append('\t');
                    } else if (esc == '"') {
                        result.append('"');
                    } else if (esc == '\\') {
                        result.append('\\');
                    } else {
                        result.append(esc);
                    }
                    advance();
                } else {
                    result.append(ch);
                    advance();
                }
            }
            throw new RuntimeException("Unterminated string at line " + startLine + ", column " + startColumn);
        }

        private Token readWord(int startLine, int startColumn) {
            StringBuilder result = new StringBuilder();
            while (index < source.length()) {
                char ch = currentChar();
                if (ch == '\0' || ch == '\n' || ch == ' ' || ch == '\t' || ch == '\r' || ch == '(' || ch == ')' || ch == '#') {
                    break;
                }
                result.append(ch);
                advance();
            }
            return new Token("WORD", result.toString(), startLine, startColumn);
        }

        private List<Token> tokenize() {
            List<Token> tokens = new ArrayList<Token>();
            while (index < source.length()) {
                char ch = currentChar();
                if (ch == ' ' || ch == '\t' || ch == '\r') {
                    advance();
                    continue;
                }
                if (ch == '\n') {
                    tokens.add(new Token("NEWLINE", "\\n", line, column));
                    advance();
                    continue;
                }
                if (ch == '#') {
                    while (index < source.length() && currentChar() != '\n') {
                        advance();
                    }
                    continue;
                }
                if (ch == '/' && index + 1 < source.length() && source.charAt(index + 1) == '/') {
                    while (index < source.length() && currentChar() != '\n') {
                        advance();
                    }
                    continue;
                }
                if (ch == '(') {
                    tokens.add(new Token("LPAREN", "(", line, column));
                    advance();
                    continue;
                }
                if (ch == ')') {
                    tokens.add(new Token("RPAREN", ")", line, column));
                    advance();
                    continue;
                }
                if (ch == '"') {
                    tokens.add(readString(line, column));
                    continue;
                }
                tokens.add(readWord(line, column));
            }
            tokens.add(new Token("EOF", "", line, column));
            return tokens;
        }
    }

    private static final class Statement {
        private final String type;
        private final String text;
        private final String command;
        private final List<String> args;

        private Statement(String type, String text, String command, List<String> args) {
            this.type = type;
            this.text = text;
            this.command = command;
            this.args = args;
        }
    }

    private static final class Program {
        private final List<Statement> statements;

        private Program(List<Statement> statements) {
            this.statements = statements;
        }
    }

    private static final class Parser {
        private final List<Token> tokens;
        private final CoreSemantics semantics;
        private int index;

        private Parser(List<Token> tokens, CoreSemantics semantics) {
            this.tokens = tokens;
            this.semantics = semantics;
            this.index = 0;
        }

        private Token peek() {
            return tokens.get(index);
        }

        private Token advance() {
            Token token = peek();
            index += 1;
            return token;
        }

        private boolean match(String type, String value) {
            Token token = peek();
            if (!type.equals(token.type)) {
                return false;
            }
            if (value != null && !value.equals(token.value)) {
                return false;
            }
            advance();
            return true;
        }

        private Token expect(String type, String value) {
            Token token = peek();
            if (!match(type, value)) {
                throw new RuntimeException("Parse error at line " + token.line + ", column " + token.column + ": expected " + type + (value == null ? "" : " " + value));
            }
            return tokens.get(index - 1);
        }

        private void skipNewlines() {
            while (match("NEWLINE", null)) {
                // noop
            }
        }

        private Program parseProgram() {
            List<Statement> statements = new ArrayList<Statement>();
            skipNewlines();
            while (!"EOF".equals(peek().type)) {
                statements.add(parseStatement());
                skipNewlines();
            }
            return new Program(statements);
        }

        private Statement parseStatement() {
            Token token = expect("WORD", null);
            if (semantics.keywordOut.equals(token.value)) {
                String text = "";
                if (match("LPAREN", "(")) {
                    while (!"RPAREN".equals(peek().type) && !"NEWLINE".equals(peek().type) && !"EOF".equals(peek().type)) {
                        Token next = advance();
                        if ("STRING".equals(next.type) && text.length() == 0) {
                            text = next.value;
                        }
                    }
                    match("RPAREN", ")");
                } else {
                    while (!"NEWLINE".equals(peek().type) && !"EOF".equals(peek().type)) {
                        advance();
                    }
                }
                return new Statement("OutStatement", text, null, null);
            }
            if (semantics.keywordHost.equals(token.value)) {
                String command = expect("WORD", null).value;
                List<String> args = new ArrayList<String>();
                while (!"NEWLINE".equals(peek().type) && !"EOF".equals(peek().type)) {
                    Token next = peek();
                    if (!"WORD".equals(next.type) && !"STRING".equals(next.type)) {
                        throw new RuntimeException("Parse error at line " + next.line + ", column " + next.column + ": expected host argument");
                    }
                    args.add(advance().value);
                }
                return new Statement("HostStatement", null, command, args);
            }
            while (!"NEWLINE".equals(peek().type) && !"EOF".equals(peek().type)) {
                advance();
            }
            return new Statement("IgnoredStatement", null, null, null);
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

    private static final class CoreSemantics {
        private final String keywordOut;
        private final String keywordHost;
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
            throw new RuntimeException("[core] missing semantics key: " + key);
        }
        return unescapeJsonString(matcher.group(1));
    }

    private static CoreSemantics parseCoreSemantics(String coreSource) {
        String json = extractSemanticsJson(coreSource);
        if (json.length() == 0) {
            throw new RuntimeException("[core] missing semantics_json block");
        }
        return new CoreSemantics(
            requireJsonStringValue(json, "out"),
            requireJsonStringValue(json, "host"),
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

    private static String evaluateHost(String command, List<String> args, Host host, CoreSemantics semantics) {
        if (semantics.cmdAdd.equals(command)) {
            return formatNumber(host.add(asNumber(parseAtom(readToken(args, 0))), asNumber(parseAtom(readToken(args, 1)))));
        }
        if (semantics.cmdSubtract.equals(command)) {
            return formatNumber(host.subtract(asNumber(parseAtom(readToken(args, 0))), asNumber(parseAtom(readToken(args, 1)))));
        }
        if (semantics.cmdMultiply.equals(command)) {
            return formatNumber(host.multiply(asNumber(parseAtom(readToken(args, 0))), asNumber(parseAtom(readToken(args, 1)))));
        }
        if (semantics.cmdDivide.equals(command)) {
            try {
                return formatNumber(host.divide(asNumber(parseAtom(readToken(args, 0))), asNumber(parseAtom(readToken(args, 1)))));
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

    private static List<String> evaluateProgram(Program program, Host host, CoreSemantics semantics) {
        List<String> output = new ArrayList<String>();
        for (int i = 0; i < program.statements.size(); i++) {
            Statement stmt = program.statements.get(i);
            if ("OutStatement".equals(stmt.type)) {
                output.add(stmt.text);
            } else if ("HostStatement".equals(stmt.type)) {
                output.add(evaluateHost(stmt.command, stmt.args, host, semantics));
            }
        }
        return output;
    }

    private static RunResult runCore(String coreSource, String programPath, Host host, CoreSemantics semantics) throws IOException {
        if (!hasCoreEntrypoint(coreSource)) {
            List<String> invalid = new ArrayList<String>();
            invalid.add(semantics.invalidCoreFormat);
            return new RunResult(2, invalid);
        }

        String programSource = host.readFile(programPath);
        List<String> userLines;
        try {
            List<Token> tokens = new Lexer(programSource).tokenize();
            Program program = new Parser(tokens, semantics).parseProgram();
            userLines = evaluateProgram(program, host, semantics);
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

        RunResult result = runCore(coreSource, programPath, host, semantics);
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
