package cod.interpreter;

import cod.debug.DebugSystem;
import java.util.Scanner;

public class IOHandler {
    private Scanner inputScanner = new Scanner(System.in);

    public Object readInput(String targetType) {
        DebugSystem.debug("INPUT", "Reading input as type: " + targetType);

        try {
            System.out.print(">> ");
            System.out.flush();

            switch (targetType) {
                case "int":
                    // For int, read the entire line and parse it
                    String intLine = inputScanner.nextLine().trim();
                    if (intLine.isEmpty()) {
                        intLine = inputScanner.nextLine().trim(); // Try again if empty
                    }
                    return Integer.parseInt(intLine);

                case "float":
                    // For float, read the entire line and parse it
                    String floatLine = inputScanner.nextLine().trim();
                    if (floatLine.isEmpty()) {
                        floatLine = inputScanner.nextLine().trim();
                    }
                    return Float.parseFloat(floatLine);

                case "string":
                    // For string, just read the line
                    String stringValue = inputScanner.nextLine().trim();
                    if (stringValue.isEmpty()) {
                        stringValue = inputScanner.nextLine().trim();
                    }
                    return stringValue;

                case "bool":
                    // For bool, read the line and check content
                    String boolLine = inputScanner.nextLine().trim().toLowerCase();
                    if (boolLine.isEmpty()) {
                        boolLine = inputScanner.nextLine().trim().toLowerCase();
                    }
                    return boolLine.equals("true")
                            || boolLine.equals("1")
                            || boolLine.equals("yes");

                default:
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
        System.out.print(String.valueOf(value)); // CHANGED: println to print
    }

    public void close() {
        inputScanner.close();
    }
}