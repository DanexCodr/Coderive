package cod.ptac;

public final class CodPTACOptions {
    public enum Mode {
        INTERPRETER,
        COMPILE_ONLY,
        COMPILE_EXECUTE
    }

    private final Mode mode;
    private final boolean fallbackEnabled;

    private CodPTACOptions(Mode mode, boolean fallbackEnabled) {
        this.mode = mode;
        this.fallbackEnabled = fallbackEnabled;
    }

    public static CodPTACOptions current() {
        String rawMode = firstNonEmpty(
            System.getProperty("cod.ptac.mode"),
            System.getenv("COD_PTAC_MODE")
        );
        String rawFallback = firstNonEmpty(
            System.getProperty("cod.ptac.fallback"),
            System.getenv("COD_PTAC_FALLBACK")
        );

        Mode mode = parseMode(rawMode);
        boolean fallback = rawFallback == null || !"false".equalsIgnoreCase(rawFallback.trim());
        return new CodPTACOptions(mode, fallback);
    }

    public static CodPTACOptions compileExecuteWithFallback(boolean fallback) {
        return new CodPTACOptions(Mode.COMPILE_EXECUTE, fallback);
    }

    public Mode getMode() {
        return mode;
    }

    public boolean isCompileEnabled() {
        return mode == Mode.COMPILE_ONLY || mode == Mode.COMPILE_EXECUTE;
    }

    public boolean isCompileExecuteEnabled() {
        return mode == Mode.COMPILE_EXECUTE;
    }

    public boolean isFallbackEnabled() {
        return fallbackEnabled;
    }

    private static Mode parseMode(String raw) {
        if (raw == null) return Mode.INTERPRETER;
        String normalized = raw.trim().toLowerCase();
        if ("interpreter".equals(normalized)) return Mode.INTERPRETER;
        if ("compile-only".equals(normalized)) return Mode.COMPILE_ONLY;
        if ("compile_execute".equals(normalized) || "compile-execute".equals(normalized)) {
            return Mode.COMPILE_EXECUTE;
        }
        return Mode.INTERPRETER;
    }

    private static String firstNonEmpty(String a, String b) {
        if (a != null && !a.trim().isEmpty()) return a;
        if (b != null && !b.trim().isEmpty()) return b;
        return null;
    }
}
