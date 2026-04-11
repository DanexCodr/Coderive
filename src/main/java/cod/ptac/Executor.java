package cod.ptac;

import cod.error.ProgramError;
import cod.interpreter.Interpreter;
import cod.ast.node.Program;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Executor {
    private final Options options;
    private static final Object FALLBACK_SENTINEL = new Object();

    private static final class Range {
        final BigInteger start;
        final BigInteger end;
        final BigInteger step;

        Range(BigInteger start, BigInteger end, BigInteger step) {
            this.start = start;
            this.end = end;
            this.step = step;
        }
    }

    public Executor(Options options) {
        this.options = options != null ? options : Options.current();
    }

    public Object execute(Artifact artifact, Interpreter fallbackInterpreter) {
        if (artifact == null) {
            throw new ProgramError("Cannot execute null CodP-TAC artifact");
        }

        if (artifact.unit == null || artifact.unit.functions == null || artifact.unit.functions.isEmpty()) {
            return fallback(artifact, fallbackInterpreter, "No executable CodP-TAC unit in artifact");
        }

        Function entry = findEntry(artifact.unit);
        if (entry == null) {
            return fallback(artifact, fallbackInterpreter, "No entry function found in CodP-TAC unit");
        }
        Object result = executeFunction(artifact.unit, entry, new ArrayList<Object>(), fallbackInterpreter, artifact);
        return result == FALLBACK_SENTINEL ? null : result;
    }

    private Object executeFunction(
        Unit unit,
        Function function,
        List<Object> args,
        Interpreter fallbackInterpreter,
        Artifact artifact
    ) {
        Map<String, Object> registers = new HashMap<String, Object>();
        if (function.parameters != null) {
            for (int i = 0; i < function.parameters.size(); i++) {
                Object arg = i < args.size() ? args.get(i) : null;
                registers.put(function.parameters.get(i), arg);
            }
        }

        if (function.instructions == null) return null;

        for (Instruction inst : function.instructions) {
            if (inst == null) continue;
            Object result = runInstruction(unit, inst, registers, fallbackInterpreter, artifact);
            if (result == FALLBACK_SENTINEL) {
                return FALLBACK_SENTINEL;
            }
            if (inst.opcode == Opcode.RETURN) {
                return result;
            }
        }
        return null;
    }

    private Object runInstruction(
        Unit unit,
        Instruction inst,
        Map<String, Object> registers,
        Interpreter fallbackInterpreter,
        Artifact artifact
    ) {
        if (inst.opcode == Opcode.ASSIGN) {
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

        if (inst.opcode == Opcode.RANGE
            || inst.opcode == Opcode.RANGE_Q
            || inst.opcode == Opcode.RANGE_S
            || inst.opcode == Opcode.RANGE_L
            || inst.opcode == Opcode.RANGE_LS) {
            Object start = operandValue(inst.operands, 0, registers);
            Object end = operandValue(inst.operands, 1, registers);
            Object stepVal = inst.operands != null && inst.operands.size() > 2
                ? operandValue(inst.operands, 2, registers)
                : 1;
            Range range = new Range(toBigInt(start), toBigInt(end), toBigInt(stepVal));
            if (inst.dest != null) registers.put(inst.dest, range);
            return range;
        }

        if (inst.opcode == Opcode.TAKE) {
            Range source = asRange(operandValue(inst.operands, 0, registers));
            BigInteger n = toBigInt(operandValue(inst.operands, 1, registers));
            List<BigInteger> out = take(source, n);
            if (inst.dest != null) registers.put(inst.dest, out);
            return out;
        }

        if (inst.opcode == Opcode.FILTER
            || inst.opcode == Opcode.MAP
            || inst.opcode == Opcode.FILTER_MAP
            || inst.opcode == Opcode.REDUCE
            || inst.opcode == Opcode.SCAN
            || inst.opcode == Opcode.ZIP
            || inst.opcode == Opcode.WHERE
            || inst.opcode == Opcode.FILTER_MAP_REDUCE
            || inst.opcode == Opcode.LAZY_GET
            || inst.opcode == Opcode.LAZY_SET
            || inst.opcode == Opcode.LAZY_COMMIT
            || inst.opcode == Opcode.LAZY_SIZE
            || inst.opcode == Opcode.LAZY_SLICE
            || inst.opcode == Opcode.SLOT_GET
            || inst.opcode == Opcode.SLOT_SET
            || inst.opcode == Opcode.SLOT_RET
            || inst.opcode == Opcode.SLOT_UNPACK
            || inst.opcode == Opcode.SLOT_DIV
            || inst.opcode == Opcode.ANCESTOR
            || inst.opcode == Opcode.SELF
            || inst.opcode == Opcode.TAIL_CALL
            || inst.opcode == Opcode.CLOSURE
            || inst.opcode == Opcode.FORMULA_SEQ
            || inst.opcode == Opcode.FORMULA_COND
            || inst.opcode == Opcode.FORMULA_RECUR
            || inst.opcode == Opcode.FORMULA_FUSE
            || inst.opcode == Opcode.STORE
            || inst.opcode == Opcode.LOAD
            || inst.opcode == Opcode.BRANCH
            || inst.opcode == Opcode.BRANCH_IF) {
            return fallback(artifact, fallbackInterpreter, "Opcode not yet natively executed: " + inst.opcode);
        }

        if (inst.opcode == Opcode.CALL) {
            String functionName = String.valueOf(operandValue(inst.operands, 0, registers));
            Function target = findFunction(unit, functionName);
            if (target == null) {
                return fallback(artifact, fallbackInterpreter, "Unknown function: " + functionName);
            }
            List<Object> args = new ArrayList<Object>();
            for (int i = 1; i < inst.operands.size(); i++) {
                args.add(operandValue(inst.operands, i, registers));
            }
            Object result = executeFunction(unit, target, args, fallbackInterpreter, artifact);
            if (result == FALLBACK_SENTINEL) {
                return FALLBACK_SENTINEL;
            }
            if (inst.dest != null) registers.put(inst.dest, result);
            return result;
        }

        if (inst.opcode == Opcode.RETURN) {
            return operandValue(inst.operands, 0, registers);
        }

        return null;
    }

    private Object fallback(Artifact artifact, Interpreter fallbackInterpreter, String reason) {
        if (!options.isFallbackEnabled()) {
            throw new ProgramError("CodP-TAC execution failed without fallback: " + reason);
        }
        if (fallbackInterpreter != null) {
            Program currentProgram = fallbackInterpreter.getCurrentProgram();
            if (currentProgram != null) {
                fallbackInterpreter.run(currentProgram);
                return FALLBACK_SENTINEL;
            }
            if (artifact != null && artifact.typeSnapshot != null) {
                fallbackInterpreter.runType(artifact.typeSnapshot);
                return FALLBACK_SENTINEL;
            }
        }
        throw new ProgramError("CodP-TAC fallback unavailable: " + reason);
    }

    private Function findEntry(Unit unit) {
        if (unit.entryFunction != null) {
            Function explicit = findFunction(unit, unit.entryFunction);
            if (explicit != null) return explicit;
        }
        return findFunction(unit, "main");
    }

    private Function findFunction(Unit unit, String name) {
        if (unit == null || unit.functions == null || name == null) return null;
        for (Function fn : unit.functions) {
            if (fn != null && name.equals(fn.name)) return fn;
        }
        return null;
    }

    private Object operandValue(List<Operand> operands, int index, Map<String, Object> registers) {
        if (operands == null || index >= operands.size()) return null;
        Operand operand = operands.get(index);
        if (operand == null) return null;
        if (operand.kind == OperandKind.REGISTER && operand.value instanceof String) {
            return registers.get(operand.value);
        }
        return operand.value;
    }

    private boolean isMath(Opcode opcode) {
        return opcode == Opcode.ADD
            || opcode == Opcode.SUB
            || opcode == Opcode.MUL
            || opcode == Opcode.DIV
            || opcode == Opcode.MOD;
    }

    private boolean isCompare(Opcode opcode) {
        return opcode == Opcode.EQ
            || opcode == Opcode.NE
            || opcode == Opcode.GT
            || opcode == Opcode.LT
            || opcode == Opcode.GTE
            || opcode == Opcode.LTE;
    }

    private Object evaluateMath(Opcode opcode, Object a, Object b) {
        BigInteger left = toBigInt(a);
        BigInteger right = toBigInt(b);
        if (opcode == Opcode.ADD) return left.add(right);
        if (opcode == Opcode.SUB) return left.subtract(right);
        if (opcode == Opcode.MUL) return left.multiply(right);
        if (opcode == Opcode.DIV) {
            if (right.equals(BigInteger.ZERO)) {
                throw new ProgramError("CodP-TAC division by zero");
            }
            return left.divide(right);
        }
        if (opcode == Opcode.MOD) {
            if (right.equals(BigInteger.ZERO)) {
                throw new ProgramError("CodP-TAC modulo by zero");
            }
            return left.mod(right.abs());
        }
        return BigInteger.ZERO;
    }

    private Boolean evaluateCompare(Opcode opcode, Object a, Object b) {
        BigInteger left = toBigInt(a);
        BigInteger right = toBigInt(b);
        int cmp = left.compareTo(right);
        if (opcode == Opcode.EQ) return cmp == 0;
        if (opcode == Opcode.NE) return cmp != 0;
        if (opcode == Opcode.GT) return cmp > 0;
        if (opcode == Opcode.LT) return cmp < 0;
        if (opcode == Opcode.GTE) return cmp >= 0;
        if (opcode == Opcode.LTE) return cmp <= 0;
        return false;
    }

    private Range asRange(Object value) {
        if (value instanceof Range) return (Range) value;
        return new Range(BigInteger.ZERO, BigInteger.ZERO, BigInteger.ONE);
    }

    private List<BigInteger> take(Range range, BigInteger n) {
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
