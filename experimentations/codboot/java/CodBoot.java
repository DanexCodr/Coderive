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
        private int index;

        private Parser(List<Token> tokens) {
            this.tokens = tokens;
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
            if ("out".equals(token.value)) {
                expect("LPAREN", "(");
                String text = expect("STRING", null).value;
                expect("RPAREN", ")");
                return new Statement("OutStatement", text, null, null);
            }
            if ("host".equals(token.value)) {
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
            throw new RuntimeException("Parse error at line " + token.line + ", column " + token.column + ": unknown statement " + token.value);
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

    private static String evaluateHost(String command, List<String> args, Host host) {
        if ("add".equals(command)) {
            return formatNumber(host.add(asNumber(parseAtom(readToken(args, 0))), asNumber(parseAtom(readToken(args, 1)))));
        }
        if ("subtract".equals(command)) {
            return formatNumber(host.subtract(asNumber(parseAtom(readToken(args, 0))), asNumber(parseAtom(readToken(args, 1)))));
        }
        if ("multiply".equals(command)) {
            return formatNumber(host.multiply(asNumber(parseAtom(readToken(args, 0))), asNumber(parseAtom(readToken(args, 1)))));
        }
        if ("divide".equals(command)) {
            try {
                return formatNumber(host.divide(asNumber(parseAtom(readToken(args, 0))), asNumber(parseAtom(readToken(args, 1)))));
            } catch (RuntimeException e) {
                return "[host] divide error: " + e.getMessage();
            }
        }
        if ("less-than".equals(command)) {
            return String.valueOf(host.lessThan(asNumber(parseAtom(readToken(args, 0))), asNumber(parseAtom(readToken(args, 1)))));
        }
        if ("greater-than".equals(command)) {
            return String.valueOf(host.greaterThan(asNumber(parseAtom(readToken(args, 0))), asNumber(parseAtom(readToken(args, 1)))));
        }
        if ("equal".equals(command)) {
            return String.valueOf(host.equal(String.valueOf(parseAtom(readToken(args, 0))), String.valueOf(parseAtom(readToken(args, 1)))));
        }
        if ("string-append".equals(command)) {
            return host.stringAppend(readToken(args, 0), readToken(args, 1));
        }
        if ("write-file".equals(command)) {
            try {
                host.writeFile(readToken(args, 0), readToken(args, 1));
                return "[host] write-file ok";
            } catch (IOException e) {
                return "[host] write-file error: " + e.getMessage();
            }
        }
        if ("read-file".equals(command)) {
            try {
                return host.readFile(readToken(args, 0)).replaceFirst("\\r?\\n$", "");
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
            return String.valueOf(host.system(readToken(args, 0)));
        }
        return "[host] unknown directive: " + command;
    }

    private static List<String> evaluateProgram(Program program, Host host) {
        List<String> output = new ArrayList<String>();
        for (int i = 0; i < program.statements.size(); i++) {
            Statement stmt = program.statements.get(i);
            if ("OutStatement".equals(stmt.type)) {
                output.add(stmt.text);
            } else if ("HostStatement".equals(stmt.type)) {
                output.add(evaluateHost(stmt.command, stmt.args, host));
            }
        }
        return output;
    }

    private static RunResult runCore(String coreSource, String programPath, Host host) throws IOException {
        if (!hasCoreEntrypoint(coreSource)) {
            List<String> invalid = new ArrayList<String>();
            invalid.add("[core] invalid core.ce format");
            return new RunResult(2, invalid);
        }

        String programSource = host.readFile(programPath);
        List<String> userLines;
        try {
            List<Token> tokens = new Lexer(programSource).tokenize();
            Program program = new Parser(tokens).parseProgram();
            userLines = evaluateProgram(program, host);
        } catch (RuntimeException e) {
            List<String> parseError = new ArrayList<String>();
            parseError.add("[core] parse/eval error: " + e.getMessage());
            return new RunResult(2, parseError);
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
            host.print("Usage: java CodBoot <core.ce-path> <program.cod-path> [--bootstrap-self]");
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
