package cod.interpreter;

import cod.debug.DebugSystem;
import cod.syntax.Keyword;
import java.math.BigDecimal;
import java.util.Scanner;

public class IOHandler {
    private Scanner inputScanner = new Scanner(System.in);

    public Object readInput(String targetType) {
        DebugSystem.debug("INPUT", "Reading input as type: " + targetType);

        try {
            System.out.flush();

            // Convert to Keyword for safe comparison
            Keyword targetKeyword;
            try {
                targetKeyword = Keyword.valueOf(targetType.toUpperCase());
            } catch (IllegalArgumentException e) {
                targetKeyword = null; // Unknown type
            }

            if (targetKeyword == Keyword.INT) {
                // For int, read the entire line and parse it
                String intLine = inputScanner.nextLine().trim();
                if (intLine.isEmpty()) {
                    intLine = inputScanner.nextLine().trim(); // Try again if empty
                }
                return Integer.parseInt(intLine);

            } else if (targetKeyword == Keyword.FLOAT) {
                // For float, read the entire line and parse it
                String floatLine = inputScanner.nextLine().trim();
                if (floatLine.isEmpty()) {
                    floatLine = inputScanner.nextLine().trim();
                }
                return Float.parseFloat(floatLine);

            } else if (targetKeyword == Keyword.TEXT) {  // ← CHANGED FROM STRING TO TEXT
                // For text, just read the line
                String textValue = inputScanner.nextLine().trim();
                if (textValue.isEmpty()) {
                    textValue = inputScanner.nextLine().trim();
                }
                return textValue;

            } else if (targetKeyword == Keyword.BOOL) {
                // For bool, read the line and check content
                String boolLine = inputScanner.nextLine().trim().toLowerCase();
                if (boolLine.isEmpty()) {
                    boolLine = inputScanner.nextLine().trim().toLowerCase();
                }
                return boolLine.equals("true")
                        || boolLine.equals("1")
                        || boolLine.equals("yes");

            } else {
                // Unknown type, read as string
                String defaultLine = inputScanner.nextLine().trim();
                if (defaultLine.isEmpty()) {
                    defaultLine = inputScanner.nextLine().trim();
                }
                return defaultLine;
            }
        } catch (Exception e) {
            DebugSystem.error(
                    "INPUT", "Invalid input for type: " + targetType + " - " + e.getMessage());
            // Clear the invalid input
            if (inputScanner.hasNextLine()) {
                inputScanner.nextLine();
            }
            throw new RuntimeException("Invalid input for type: " + targetType);
        }
    }

    public void output(Object value) {
    // Format large numbers without scientific notation
    String output;
    
    // FIX: Handle BigDecimal directly
    if (value instanceof BigDecimal) {
        BigDecimal bd = (BigDecimal) value;
        bd = bd.stripTrailingZeros();  // Remove .000
        output = bd.toPlainString();    // No scientific notation
    } else if (value instanceof Double) {
        double d = (Double) value;
        // If it's a whole number and reasonably sized, show as integer
        if (d == Math.floor(d) && d <= 1e15 && d >= -1e15) {
            output = String.format("%.0f", d);
        } else {
            output = String.valueOf(value);
        }
    } else if (value instanceof Float) {
        float f = (Float) value;
        if (f == Math.floor(f) && f <= 1e15 && f >= -1e15) {
            output = String.format("%.0f", (double) f);
        } else {
            output = String.valueOf(value);
        }
    } else {
        output = String.valueOf(value);
    }
    
    System.out.print(output);
}

    public void close() {
        inputScanner.close();
    }
}
