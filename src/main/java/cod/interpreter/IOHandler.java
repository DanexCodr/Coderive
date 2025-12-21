package cod.interpreter;

import cod.debug.DebugSystem;
import java.math.BigDecimal;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class IOHandler {
    // Mobile-friendly settings (smaller buffers)
    private static final int BUFFER_SIZE = 4096;    // 4KB for mobile
    private static final int FLUSH_LINES = 10;      // Smaller batches
    
    // Simple thread-safe buffer
    private final StringBuilder buffer = new StringBuilder(BUFFER_SIZE);
    private final Object bufferLock = new Object();
    private int currentLineCount = 0;
    private long lastFlushTime = System.currentTimeMillis();
    
    // Input
    private Scanner inputScanner = new Scanner(System.in);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    
    // No async threads - use sync with smart batching
    public IOHandler() {
        DebugSystem.debug("IO", "Mobile-safe I/O handler initialized");
    }
    
    // Optimized sync output with batching
    public void output(Object value) {
        if (closed.get()) return;
        
        String output = formatValue(value);
        
        synchronized (bufferLock) {
            buffer.append(output);
            
            // Flush on buffer full or timeout
            boolean shouldFlush = buffer.length() >= BUFFER_SIZE ||
                                 (System.currentTimeMillis() - lastFlushTime) > 50; // 50ms max
            
            if (shouldFlush) {
                flushBuffer(false);
            }
        }
    }
    
    // Newline-aware output
    public void outputln(Object value) {
        if (closed.get()) return;
        
        String output = formatValue(value) + "\n";
        currentLineCount++;
        
        synchronized (bufferLock) {
            buffer.append(output);
            
            // Flush on newline if we have enough lines
            boolean shouldFlush = currentLineCount >= FLUSH_LINES ||
                                 buffer.length() >= BUFFER_SIZE;
            
            if (shouldFlush) {
                flushBuffer(true);
            }
        }
    }
    
    // Safe buffer flushing
    private void flushBuffer(boolean force) {
        synchronized (bufferLock) {
            if (buffer.length() == 0) return;
            
            try {
                // Single system call for batch
                System.out.print(buffer.toString());
                if (force) {
                    System.out.flush();
                }
                
                // Reset
                buffer.setLength(0);
                currentLineCount = 0;
                lastFlushTime = System.currentTimeMillis();
                
            } catch (Exception e) {
                // Swallow I/O errors in mobile environment
                DebugSystem.warn("IO", "Flush failed: " + e.getMessage());
                buffer.setLength(0); // Clear buffer anyway
            }
        }
    }
    
    // Read input (unchanged, but ensure flush before reading)
    public Object readInput(String targetType) {
        if (closed.get()) throw new RuntimeException("I/O handler closed");
        
        // Ensure all output is visible before prompting
        synchronized (bufferLock) {
            if (buffer.length() > 0) {
                flushBuffer(true);
            }
        }
        
        try {
            System.out.flush(); // Extra safety
            
            String inputLine = inputScanner.nextLine().trim();
            if (inputLine.isEmpty()) {
                inputLine = inputScanner.nextLine().trim();
            }
            
            DebugSystem.debug("INPUT", "Raw input: '" + inputLine + "'");
            
            // Type conversion (same as before)
            if ("int".equalsIgnoreCase(targetType)) {
                try {
                    return Integer.parseInt(inputLine);
                } catch (NumberFormatException e) {
                    try {
                        double d = Double.parseDouble(inputLine);
                        return (int) Math.round(d);
                    } catch (NumberFormatException e2) {
                        throw new RuntimeException("Invalid integer input: " + inputLine);
                    }
                }
            } 
            else if ("float".equalsIgnoreCase(targetType)) {
                try {
                    return Float.parseFloat(inputLine);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid float input: " + inputLine);
                }
            }
            else if ("text".equalsIgnoreCase(targetType)) {
                return inputLine;
            }
            else if ("bool".equalsIgnoreCase(targetType)) {
                String lower = inputLine.toLowerCase();
                if (lower.equals("true") || lower.equals("1") || lower.equals("yes") || lower.equals("y")) {
                    return true;
                } else if (lower.equals("false") || lower.equals("0") || lower.equals("no") || lower.equals("n")) {
                    return false;
                } else {
                    throw new RuntimeException("Invalid boolean input: " + inputLine);
                }
            }
            else {
                return inputLine;
            }
            
        } catch (Exception e) {
            DebugSystem.error("INPUT", "Read failed: " + e.getMessage());
            throw new RuntimeException("Input error: " + e.getMessage());
        }
    }
    
    // Format value (same as before)
    private String formatValue(Object value) {
        if (value instanceof BigDecimal) {
            BigDecimal bd = (BigDecimal) value;
            bd = bd.stripTrailingZeros();
            return bd.toPlainString();
        } else if (value instanceof Double) {
            double d = (Double) value;
            if (d == Math.floor(d) && d <= 1e15 && d >= -1e15) {
                return String.format("%.0f", d);
            } else {
                return String.valueOf(value);
            }
        } else if (value instanceof Float) {
            float f = (Float) value;
            if (f == Math.floor(f) && f <= 1e15 && f >= -1e15) {
                return String.format("%.0f", (double) f);
            } else {
                return String.valueOf(value);
            }
        } else {
            return String.valueOf(value);
        }
    }
    
    // Public flush for explicit control
    public void flush() {
        if (closed.get()) return;
        
        synchronized (bufferLock) {
            flushBuffer(true);
        }
    }
    
    // Safe close - NO THREADS to clean up
    public void close() {
        if (closed.getAndSet(true)) return;
        
        try {
            // Final flush
            synchronized (bufferLock) {
                flushBuffer(true);
            }
            
            // Close scanner
            if (inputScanner != null) {
                inputScanner.close();
                inputScanner = null;
            }
            
        } catch (Exception e) {
            // Ignore errors during close
        }
        
        DebugSystem.debug("IO", "I/O handler closed safely");
    }
    
    // Destructor pattern for mobile
    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }
}