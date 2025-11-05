// DebugSystem.java
package cod.debug;

import java.util.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DebugSystem {
    public enum Level {
        OFF(0),
        ERROR(1),
        WARN(2),
        INFO(3),
        DEBUG(4),
        TRACE(5);

        private final int level;

        Level(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }

    private static Level currentLevel = Level.INFO;
    private static Set<String> enabledCategories = new HashSet<>();
    private static boolean showTimestamp = true;
    private static boolean showThread = false;
    private static Map<String, Long> timers = new HashMap<>();

    static {
        // Enable common categories by default
        enabledCategories.add("AST");
        enabledCategories.add("METHODS");
        enabledCategories.add("SLOTS");
        enabledCategories.add("INTERPRETER");
    }

    // Configuration methods
    public static void setLevel(Level level) {
        currentLevel = level;
    }

    public static void enableCategory(String category) {
        enabledCategories.add(category);
    }

    public static void disableCategory(String category) {
        enabledCategories.remove(category);
    }

    public static void enableAllCategories() {
        enabledCategories.addAll(
                Arrays.asList(
                        "AST",
                        "METHODS",
                        "SLOTS",
                        "FIELDS",
                        "EXPRESSIONS",
                        "INTERPRETER",
                        "MEMORY"));
    }

    public static void setShowTimestamp(boolean show) {
        showTimestamp = show;
    }

    public static void setShowThread(boolean show) {
        showThread = show;
    }

    // Logging methods
    public static void error(String category, String message) {
        log(Level.ERROR, category, message);
    }

    public static void warn(String category, String message) {
        log(Level.WARN, category, message);
    }

    public static void info(String category, String message) {
        log(Level.INFO, category, message);
    }

    public static void debug(String category, String message) {
        log(Level.DEBUG, category, message);
    }

    public static void trace(String category, String message) {
        log(Level.TRACE, category, message);
    }

    // Specialized logging methods
    public static void methodEntry(String methodName, Map<String, Object> params) {
        if (shouldLog(Level.DEBUG, "METHODS")) {
            debug("METHODS", "→ " + methodName + "(" + params + ")");
        }
    }

    public static void methodExit(String methodName, Object result) {
        if (shouldLog(Level.DEBUG, "METHODS")) {
            debug("METHODS", "← " + methodName + " = " + result);
        }
    }

    public static void slotUpdate(String slotName, Object oldValue, Object newValue) {
        if (shouldLog(Level.DEBUG, "SLOTS")) {
            debug("SLOTS", "Slot " + slotName + ": " + oldValue + " → " + newValue);
        }
    }

    public static void fieldUpdate(String fieldName, Object value) {
        if (shouldLog(Level.DEBUG, "FIELDS")) {
            debug("FIELDS", "Field " + fieldName + " = " + value);
        }
    }

    // Performance timing
    public static void startTimer(String name) {
        if (shouldLog(Level.DEBUG, "PERF")) {
            timers.put(name, System.currentTimeMillis());
        }
    }

    public static void stopTimer(String name) {
        if (shouldLog(Level.DEBUG, "PERF")) {
            Long start = timers.remove(name);
            if (start != null) {
                long duration = System.currentTimeMillis() - start;
                debug("PERF", name + " took " + duration + "ms");
            }
        }
    }

    // AST building helpers
    public static void astBuilding(String nodeType, String details) {
        if (shouldLog(Level.TRACE, "AST")) {
            trace("AST", "Building " + nodeType + ": " + details);
        }
    }

    public static void astComplete(String nodeType, String summary) {
        if (shouldLog(Level.DEBUG, "AST")) {
            debug("AST", "Built " + nodeType + " → " + summary);
        }
    }

    // Private implementation
    private static void log(Level level, String category, String message) {
        if (shouldLog(level, category)) {
            StringBuilder logLine = new StringBuilder();

            // Timestamp
            if (showTimestamp) {
                logLine.append("[")
                        .append(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")))
                        .append("] ");
            }

            // Level
            logLine.append("[").append(level).append("] ");

            // Thread (optional)
            if (showThread) {
                logLine.append("[").append(Thread.currentThread().getName()).append("] ");
            }

            // Category and message
            logLine.append(category).append(": ").append(message);

            System.out.println(logLine.toString());
        }
    }

    private static boolean shouldLog(Level level, String category) {
        return level.getLevel() <= currentLevel.getLevel() && enabledCategories.contains(category);
    }
}
