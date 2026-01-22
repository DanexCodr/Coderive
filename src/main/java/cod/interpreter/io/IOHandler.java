package cod.interpreter.io;

import cod.debug.DebugSystem;
import java.math.BigDecimal;
import java.util.Scanner;

public class IOHandler {
    private Scanner inputScanner = null;
    private boolean closed = false;
    
    public IOHandler() {
        DebugSystem.debug("IO", "Direct mobile I/O handler initialized (no buffering)");
    }
    
    // Direct output - no buffering
    public void output(Object value) {
        if (closed) return;
        
        try {
            System.out.print(formatValue(value));
            System.out.flush(); // Immediate flush
        } catch (Exception e) {
            // Silently ignore - mobile I/O can be flaky
        }
    }
    
    // Direct output with newline
    public void outputln(Object value) {
        if (closed) return;
        
        try {
            System.out.println(formatValue(value));
            System.out.flush(); // Immediate flush
        } catch (Exception e) {
            // Silently ignore
        }
    }
    
    // Read input - simple and direct
    public Object readInput(String targetType) {
        if (closed) throw new RuntimeException("I/O handler closed");
        
        try {
            // Ensure output is flushed before reading
            System.out.flush();
            
            // Create scanner if needed
            if (inputScanner == null) {
                inputScanner = new Scanner(System.in);
            }
            
            // Read line
            String inputLine = "";
            if (inputScanner.hasNextLine()) {
                inputLine = inputScanner.nextLine().trim();
            }
            
            DebugSystem.debug("INPUT", "Read: '" + inputLine + "' for type: " + targetType);
            
            // Convert based on target type
            if ("int".equalsIgnoreCase(targetType)) {
                try {
                    return Integer.parseInt(inputLine);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid integer: " + inputLine);
                }
            } 
            else if ("float".equalsIgnoreCase(targetType)) {
                try {
                    return Float.parseFloat(inputLine);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid float: " + inputLine);
                }
            }
            else if ("bool".equalsIgnoreCase(targetType)) {
                String lower = inputLine.toLowerCase();
                if (lower.equals("true") || lower.equals("1") || 
                    lower.equals("yes") || lower.equals("y")) {
                    return true;
                } else if (lower.equals("false") || lower.equals("0") || 
                          lower.equals("no") || lower.equals("n")) {
                    return false;
                } else {
                    throw new RuntimeException("Invalid boolean: " + inputLine);
                }
            }
            else { // text or any other type
                return inputLine;
            }
            
        } catch (Exception e) {
            DebugSystem.error("INPUT", "Failed: " + e.getMessage());
            throw new RuntimeException("Input error: " + e.getMessage());
        }
    }
    
    // Format value
    private String formatValue(Object value) {
        if (value instanceof BigDecimal) {
            BigDecimal bd = (BigDecimal) value;
            bd = bd.stripTrailingZeros();
            return bd.toPlainString();
        } else if (value instanceof Double) {
            double d = (Double) value;
            if (d == Math.floor(d) && Math.abs(d) <= 1e15) {
                return String.format("%.0f", d);
            }
        } else if (value instanceof Float) {
            float f = (Float) value;
            if (f == Math.floor(f) && Math.abs(f) <= 1e15) {
                return String.format("%.0f", (double) f);
            }
        }
        return String.valueOf(value);
    }
    
    // Close resources
    public void close() {
        closed = true;
        try {
            if (inputScanner != null) {
                inputScanner.close();
                inputScanner = null;
            }
        } catch (Exception e) {
            // Ignore
        }
        DebugSystem.debug("IO", "I/O handler closed");
    }
    
    // Reset for reuse
    public void reset() {
        closed = false;
        // Don't close scanner, just mark as not closed
    }
}