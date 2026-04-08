package cod.ptac;

import cod.error.ProgramError;
import cod.interpreter.Interpreter;
import cod.ast.node.Program;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CodPTACExecutor {
    private final CodPTACOptions options;
    private static final Object FALLBACK_EXECUTED = new Object();

    private static final class CodPTACRange {
        final BigInteger start;
        final BigInteger end;
        final BigInteger step;

        CodPTACRange(BigInteger start, BigInteger end, BigInteger step) {
            this.start = start;
            this.end = end;
            this.step = step;
        }
    }

    public CodPTACExecutor(CodPTACOptions options) {
        this.options = options != null ? options : CodPTACOptions.current();
    }

    public Object execute(CodPTACArtifact artifact, Interpreter fallbackInterpreter) {
        if (artifact == null) {
            throw new ProgramError("Cannot execute null CodP-TAC artifact");
        }

        if (artifact.unit == null || artifact.unit.functions == null || artifact.unit.functions.isEmpty()) {
            return fallback(artifact, fallbackInterpreter, "No executable CodP-TAC unit in artifact");
        }

        CodPTACFunction entry = findEntry(artifact.unit);
        if (entry == null) {
            return fallback(artifact, fallbackInterpreter, "No entry function found in CodP-TAC unit");
        }
        Object result = executeFunction(artifact.unit, entry, new ArrayList<Object>(), fallbackInterpreter, artifact);
        return result == FALLBACK_EXECUTED ? null : result;
    }

    private Object executeFunction(
        CodPTACUnit unit,
        CodPTACFunction function,
        List<Object> args,
        Interpreter fallbackInterpreter,
        CodPTACArtifact artifact
    ) {
        Map<String, Object> registers = new HashMap<String, Object>();
        if (function.parameters != null) {
            for (int i = 0; i < function.parameters.size(); i++) {
                Object arg = i < args.size() ? args.get(i) : null;
                registers.put(function.parameters.get(i), arg);
            }
        }

        if (function.instructions == null) return null;

        for (CodPTACInstruction inst : function.instructions) {
            if (inst == null) continue;
            Object result = runInstruction(unit, inst, registers, fallbackInterpreter, artifact);
            if (result == FALLBACK_EXECUTED) {
                return FALLBACK_EXECUTED;
            }
            if (inst.opcode == CodPTACOpcode.RETURN) {
                return result;
            }
        }
        return null;
    }

    private Object runInstruction(
        CodPTACUnit unit,
        CodPTACInstruction inst,
        Map<String, Object> registers,
        Interpreter fallbackInterpreter,
        CodPTACArtifact artifact
    ) {
        if (inst.opcode == CodPTACOpcode.ASSIGN) {
            Object value = operandValue(inst.operands, 0, registers);
            registers.put(inst.dest, value);
            return value;
        }

        if (isMath(inst.opcode)) {
            Object left = operandValue(inst.operands, 0, registers);
            Object right = operandValue(inst.operands, 1, registers);
            Object out = evaluateMath(inst.opcode, left, right);
            if (inst.dest != null) registers.put(inst.dest, out);
            return out;
        }

        if (isCompare(inst.opcode)) {
            Object left = operandValue(inst.operands, 0, registers);
            Object right = operandValue(inst.operands, 1, registers);
            Boolean out = evaluateCompare(inst.opcode, left, right);
            if (inst.dest != null) registers.put(inst.dest, out);
            return out;
        }

        if (inst.opcode == CodPTACOpcode.RANGE
            || inst.opcode == CodPTACOpcode.RANGE_Q
            || inst.opcode == CodPTACOpcode.RANGE_S
            || inst.opcode == CodPTACOpcode.RANGE_L
            || inst.opcode == CodPTACOpcode.RANGE_LS) {
            Object start = operandValue(inst.operands, 0, registers);
            Object end = operandValue(inst.operands, 1, registers);
            Object stepVal = inst.operands != null && inst.operands.size() > 2
                ? operandValue(inst.operands, 2, registers)
                : 1;
            CodPTACRange range = new CodPTACRange(toBigInt(start), toBigInt(end), toBigInt(stepVal));
            if (inst.dest != null) registers.put(inst.dest, range);
            return range;
        }

        if (inst.opcode == CodPTACOpcode.TAKE) {
            CodPTACRange source = asRange(operandValue(inst.operands, 0, registers));
            BigInteger n = toBigInt(operandValue(inst.operands, 1, registers));
            List<BigInteger> out = take(source, n);
            if (inst.dest != null) registers.put(inst.dest, out);
            return out;
        }

        if (inst.opcode == CodPTACOpcode.FILTER
            || inst.opcode == CodPTACOpcode.MAP
            || inst.opcode == CodPTACOpcode.FILTER_MAP
            || inst.opcode == CodPTACOpcode.REDUCE
            || inst.opcode == CodPTACOpcode.SCAN
            || inst.opcode == CodPTACOpcode.ZIP
            || inst.opcode == CodPTACOpcode.WHERE
            || inst.opcode == CodPTACOpcode.FILTER_MAP_REDUCE
            || inst.opcode == CodPTACOpcode.LAZY_GET
            || inst.opcode == CodPTACOpcode.LAZY_SET
            || inst.opcode == CodPTACOpcode.LAZY_COMMIT
            || inst.opcode == CodPTACOpcode.LAZY_SIZE
            || inst.opcode == CodPTACOpcode.LAZY_SLICE
            || inst.opcode == CodPTACOpcode.SLOT_GET
            || inst.opcode == CodPTACOpcode.SLOT_SET
            || inst.opcode == CodPTACOpcode.SLOT_RET
            || inst.opcode == CodPTACOpcode.SLOT_UNPACK
            || inst.opcode == CodPTACOpcode.SLOT_DIV
            || inst.opcode == CodPTACOpcode.ANCESTOR
            || inst.opcode == CodPTACOpcode.SELF
            || inst.opcode == CodPTACOpcode.TAIL_CALL
            || inst.opcode == CodPTACOpcode.CLOSURE
            || inst.opcode == CodPTACOpcode.FORMULA_SEQ
            || inst.opcode == CodPTACOpcode.FORMULA_COND
            || inst.opcode == CodPTACOpcode.FORMULA_RECUR
            || inst.opcode == CodPTACOpcode.FORMULA_FUSE
            || inst.opcode == CodPTACOpcode.STORE
            || inst.opcode == CodPTACOpcode.LOAD
            || inst.opcode == CodPTACOpcode.BRANCH
            || inst.opcode == CodPTACOpcode.BRANCH_IF) {
            return fallback(artifact, fallbackInterpreter, "Opcode not yet natively executed: " + inst.opcode);
        }

        if (inst.opcode == CodPTACOpcode.CALL) {
            String functionName = String.valueOf(operandValue(inst.operands, 0, registers));
            CodPTACFunction target = findFunction(unit, functionName);
            if (target == null) {
                return fallback(artifact, fallbackInterpreter, "Unknown function: " + functionName);
            }
            List<Object> args = new ArrayList<Object>();
            for (int i = 1; i < inst.operands.size(); i++) {
                args.add(operandValue(inst.operands, i, registers));
            }
            Object result = executeFunction(unit, target, args, fallbackInterpreter, artifact);
            if (result == FALLBACK_EXECUTED) {
                return FALLBACK_EXECUTED;
            }
            if (inst.dest != null) registers.put(inst.dest, result);
            return result;
        }

        if (inst.opcode == CodPTACOpcode.RETURN) {
            return operandValue(inst.operands, 0, registers);
        }

        return null;
    }

    private Object fallback(CodPTACArtifact artifact, Interpreter fallbackInterpreter, String reason) {
        if (!options.isFallbackEnabled()) {
            throw new ProgramError("CodP-TAC execution failed without fallback: " + reason);
        }
        if (fallbackInterpreter != null) {
            Program currentProgram = fallbackInterpreter.getCurrentProgram();
            if (currentProgram != null) {
                fallbackInterpreter.run(currentProgram);
                return FALLBACK_EXECUTED;
            }
            if (artifact != null && artifact.typeSnapshot != null) {
                fallbackInterpreter.runType(artifact.typeSnapshot);
                return FALLBACK_EXECUTED;
            }
        }
        throw new ProgramError("CodP-TAC fallback unavailable: " + reason);
    }

    private CodPTACFunction findEntry(CodPTACUnit unit) {
        if (unit.entryFunction != null) {
            CodPTACFunction explicit = findFunction(unit, unit.entryFunction);
            if (explicit != null) return explicit;
        }
        return findFunction(unit, "main");
    }

    private CodPTACFunction findFunction(CodPTACUnit unit, String name) {
        if (unit == null || unit.functions == null || name == null) return null;
        for (CodPTACFunction fn : unit.functions) {
            if (fn != null && name.equals(fn.name)) return fn;
        }
        return null;
    }

    private Object operandValue(List<CodPTACOperand> operands, int index, Map<String, Object> registers) {
        if (operands == null || index >= operands.size()) return null;
        CodPTACOperand operand = operands.get(index);
        if (operand == null) return null;
        if (operand.kind == CodPTACOperandKind.REGISTER && operand.value instanceof String) {
            return registers.get(operand.value);
        }
        return operand.value;
    }

    private boolean isMath(CodPTACOpcode opcode) {
        return opcode == CodPTACOpcode.ADD
            || opcode == CodPTACOpcode.SUB
            || opcode == CodPTACOpcode.MUL
            || opcode == CodPTACOpcode.DIV
            || opcode == CodPTACOpcode.MOD;
    }

    private boolean isCompare(CodPTACOpcode opcode) {
        return opcode == CodPTACOpcode.EQ
            || opcode == CodPTACOpcode.NE
            || opcode == CodPTACOpcode.GT
            || opcode == CodPTACOpcode.LT
            || opcode == CodPTACOpcode.GTE
            || opcode == CodPTACOpcode.LTE;
    }

    private Object evaluateMath(CodPTACOpcode opcode, Object a, Object b) {
        BigInteger left = toBigInt(a);
        BigInteger right = toBigInt(b);
        if (opcode == CodPTACOpcode.ADD) return left.add(right);
        if (opcode == CodPTACOpcode.SUB) return left.subtract(right);
        if (opcode == CodPTACOpcode.MUL) return left.multiply(right);
        if (opcode == CodPTACOpcode.DIV) {
            if (right.equals(BigInteger.ZERO)) {
                throw new ProgramError("CodP-TAC division by zero");
            }
            return left.divide(right);
        }
        if (opcode == CodPTACOpcode.MOD) {
            if (right.equals(BigInteger.ZERO)) {
                throw new ProgramError("CodP-TAC modulo by zero");
            }
            return left.mod(right.abs());
        }
        return BigInteger.ZERO;
    }

    private Boolean evaluateCompare(CodPTACOpcode opcode, Object a, Object b) {
        BigInteger left = toBigInt(a);
        BigInteger right = toBigInt(b);
        int cmp = left.compareTo(right);
        if (opcode == CodPTACOpcode.EQ) return cmp == 0;
        if (opcode == CodPTACOpcode.NE) return cmp != 0;
        if (opcode == CodPTACOpcode.GT) return cmp > 0;
        if (opcode == CodPTACOpcode.LT) return cmp < 0;
        if (opcode == CodPTACOpcode.GTE) return cmp >= 0;
        if (opcode == CodPTACOpcode.LTE) return cmp <= 0;
        return false;
    }

    private CodPTACRange asRange(Object value) {
        if (value instanceof CodPTACRange) return (CodPTACRange) value;
        return new CodPTACRange(BigInteger.ZERO, BigInteger.ZERO, BigInteger.ONE);
    }

    private List<BigInteger> take(CodPTACRange range, BigInteger n) {
        List<BigInteger> out = new ArrayList<BigInteger>();
        if (range == null || n == null || n.compareTo(BigInteger.ZERO) <= 0) return out;

        BigInteger current = range.start;
        BigInteger remaining = n;
        while (remaining.compareTo(BigInteger.ZERO) > 0) {
            if (range.step.compareTo(BigInteger.ZERO) >= 0 && current.compareTo(range.end) > 0) break;
            if (range.step.compareTo(BigInteger.ZERO) < 0 && current.compareTo(range.end) < 0) break;
            out.add(current);
            current = current.add(range.step);
            remaining = remaining.subtract(BigInteger.ONE);
        }
        return out;
    }

    private BigInteger toBigInt(Object value) {
        if (value == null) return BigInteger.ZERO;
        if (value instanceof BigInteger) return (BigInteger) value;
        if (value instanceof Number) return BigInteger.valueOf(((Number) value).longValue());
        try {
            return new BigInteger(String.valueOf(value));
        } catch (Exception ignored) {
            return BigInteger.ZERO;
        }
    }
}
