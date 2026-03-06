package cod.interpreter.handler;

import cod.debug.DebugSystem;
import cod.math.AutoStackingNumber;
import java.math.BigDecimal;
import java.util.Scanner;

public class IOHandler {
    private Scanner inputScanner = null;
    private boolean closed = false;
    
    public IOHandler() {
        DebugSystem.debug("IO", "Direct mobile I/O handler initialized (no buffering)");
    }
    
    // Direct output - no buffering
    public void outs(Object value) {
        if (closed) return;
        
        try {
            System.out.print(formatValue(value));
            System.out.flush();
        } catch (Exception e) {
            // Silently ignore
        }
    }
    
    // Direct output with newline
    public void out(Object value) {
        if (closed) return;
        
        try {
            System.out.println(formatValue(value));
            System.out.flush();
        } catch (Exception e) {
            // Silently ignore
        }
    }
    
    // Read input
    public Object readInput(String targetType) {
        if (closed) throw new RuntimeException("I/O handler closed");
        
        try {
            System.out.flush();
            
            if (inputScanner == null) {
                inputScanner = new Scanner(System.in);
            }
            
            String inputLine = "";
            if (inputScanner.hasNextLine()) {
                inputLine = inputScanner.nextLine().trim();
            }
            
            DebugSystem.debug("INPUT", "Read: '" + inputLine + "' for type: " + targetType);
            
            if ("int".equalsIgnoreCase(targetType)) {
                try {
                    return Integer.parseInt(inputLine);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid integer: " + inputLine);
                }
            } 
            else if ("float".equalsIgnoreCase(targetType)) {
                try {
                    return Double.parseDouble(inputLine);
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
            else {
                return inputLine;
            }
            
        } catch (Exception e) {
            DebugSystem.error("INPUT", "Failed: " + e.getMessage());
            throw new RuntimeException("Input error: " + e.getMessage());
        }
    }
    
    // Clean formatting - relies on AutoStackingNumber's toString()
    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        
        // Let AutoStackingNumber handle its own formatting
        if (value instanceof AutoStackingNumber) {
            return value.toString();
        }
        
        // Simple handling for all other types
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
    }
  }
