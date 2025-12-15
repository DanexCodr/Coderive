package cod.compiler;

import java.util.*;

public class TACInstruction {
    public enum Opcode {
        // Arithmetic
        ADD, SUB, MUL, DIV, MOD,
        // Logic / Comparison
        CMP_EQ, CMP_NE, CMP_LT, CMP_LE, CMP_GT, CMP_GE,
        NEG, INT_TO_STRING,
        CONCAT,
        // Data Movement
        ASSIGN, LOAD_IMM, LOAD_ADDR,
        // Control Flow
        GOTO, IF_GOTO, LABEL,
        RET,
        // Object / Memory
        LOAD_FIELD, STORE_FIELD,
        LOAD_ARRAY, STORE_ARRAY,
        ARRAY_NEW,
        // Function Calls
        CALL, CALL_SLOTS, PARAM,
        // Runtime / IO
        PRINT, READ_INPUT
    }

    public final Opcode opcode;
    public final String result;
    public final Object operand1;
    public final Object operand2;

    public TACInstruction(Opcode opcode, String result, Object operand1, Object operand2) {
        this.opcode = opcode;
        this.result = result;
        this.operand1 = operand1;
        this.operand2 = operand2;
    }

    public TACInstruction(Opcode opcode, String result, Object operand1) {
        this(opcode, result, operand1, null);
    }

    public TACInstruction(Opcode opcode, Object operand1) {
        this(opcode, null, operand1, null);
    }

    public TACInstruction(Opcode opcode) {
        this(opcode, null, null, null);
    }

    @Override
    public String toString() {
        String s = String.format("%-12s", opcode);
        if (result != null) s += " " + result + " =";
        if (operand1 != null) s += " " + operand1;
        if (operand2 != null) s += ", " + operand2;
        return s;
    }

    // --- Analysis Helpers ---

    public String getDef() {
        if (result == null) return null;
        switch (opcode) {
            case ADD: case SUB: case MUL: case DIV: case MOD:
            case CMP_EQ: case CMP_NE: case CMP_LT: case CMP_LE: case CMP_GT: case CMP_GE:
            case NEG: case INT_TO_STRING: case CONCAT:
            case ASSIGN: case LOAD_IMM: case LOAD_ADDR:
            case LOAD_FIELD: case LOAD_ARRAY:
            case CALL: case CALL_SLOTS:
            case READ_INPUT: case ARRAY_NEW:
                return result;
            default:
                return null;
        }
    }

    public List<String> getUses() {
        List<String> uses = new ArrayList<String>();
        addUseIfVariable(uses, operand1);
        addUseIfVariable(uses, operand2);
        
        // STORE operations read the 'result' field (it holds the value to store)
        if (opcode == Opcode.STORE_ARRAY || opcode == Opcode.STORE_FIELD) {
             if (result != null && isVariable(result)) {
                 uses.add(result);
             }
        }
        
        // IF_GOTO uses its operand1 (condition)
        if (opcode == Opcode.IF_GOTO) {
             // IF_GOTO cond, zero, label -> cond is used
             addUseIfVariable(uses, result); // Sometimes cond is in result pos in constructor
        }

        return uses;
    }

    private void addUseIfVariable(List<String> uses, Object operand) {
        if (operand instanceof String) {
            String str = (String) operand;
            if (isVariable(str)) {
                uses.add(str);
            }
        }
    }

    private boolean isVariable(String s) {
        if (s == null) return false;
        // Exclude string literals and labels
        if (s.startsWith("\"")) return false; 
        if (s.startsWith("L_") || s.startsWith("loop_") || s.startsWith("else_") || s.startsWith("endif_")) return false; 
        // Variables start with $ or letters
        return true;
    }
}