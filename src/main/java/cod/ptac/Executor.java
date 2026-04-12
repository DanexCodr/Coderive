package cod.ptac;

import cod.error.ProgramError;
import cod.interpreter.Interpreter;
import cod.ast.node.Program;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
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

    private static final class ExecutionResult {
        final Object value;
        final Integer nextPc;
        final boolean returned;
        final boolean fallback;

        ExecutionResult(Object value, Integer nextPc, boolean returned, boolean fallback) {
            this.value = value;
            this.nextPc = nextPc;
            this.returned = returned;
            this.fallback = fallback;
        }

        static ExecutionResult normal(Object value) {
            return new ExecutionResult(value, null, false, false);
        }

        static ExecutionResult jump(int nextPc) {
            return new ExecutionResult(null, Integer.valueOf(nextPc), false, false);
        }

        static ExecutionResult returned(Object value) {
            return new ExecutionResult(value, null, true, false);
        }

        static ExecutionResult fallback() {
            return new ExecutionResult(null, null, false, true);
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
        Map<String, Integer> labels = indexLabels(function);
        int pc = 0;
        while (pc < function.instructions.size()) {
            Instruction inst = function.instructions.get(pc);
            if (inst == null) {
                pc++;
                continue;
            }
            ExecutionResult result = runInstruction(
                unit, inst, registers, fallbackInterpreter, artifact, labels, pc
            );
            if (result.fallback) {
                return FALLBACK_SENTINEL;
            }
            if (result.returned) {
                return result.value;
            }
            if (result.nextPc != null) {
                pc = result.nextPc.intValue();
            } else {
                pc++;
            }
        }
        return null;
    }

    private ExecutionResult runInstruction(
        Unit unit,
        Instruction inst,
        Map<String, Object> registers,
        Interpreter fallbackInterpreter,
        Artifact artifact,
        Map<String, Integer> labels,
        int currentPc
    ) {
        if (inst.opcode == Opcode.ASSIGN) {
            Object value = operandValue(inst.operands, 0, registers);
            registers.put(inst.dest, value);
            return ExecutionResult.normal(value);
        }

        if (isMath(inst.opcode)) {
            Object left = operandValue(inst.operands, 0, registers);
            Object right = operandValue(inst.operands, 1, registers);
            Object out = evaluateMath(inst.opcode, left, right);
            if (inst.dest != null) registers.put(inst.dest, out);
            return ExecutionResult.normal(out);
        }

        if (isCompare(inst.opcode)) {
            Object left = operandValue(inst.operands, 0, registers);
            Object right = operandValue(inst.operands, 1, registers);
            Boolean out = evaluateCompare(inst.opcode, left, right);
            if (inst.dest != null) registers.put(inst.dest, out);
            return ExecutionResult.normal(out);
        }

        if (inst.opcode == Opcode.RANGE
            || inst.opcode == Opcode.RANGE_Q
            || inst.opcode == Opcode.RANGE_S
            || inst.opcode == Opcode.RANGE_L
            || inst.opcode == Opcode.RANGE_LS) {
            Object start = operandValue(inst.operands, 0, registers);
            Object end = operandValue(inst.operands, 1, registers);
            if (!isNumericLike(start) || !isNumericLike(end)) {
                Object fallback = fallback(
                    artifact,
                    fallbackInterpreter,
                    "Non-numeric range bounds are not yet natively executed"
                );
                if (fallback == FALLBACK_SENTINEL) return ExecutionResult.fallback();
            }
            Object stepVal = inst.operands != null && inst.operands.size() > 2
                ? operandValue(inst.operands, 2, registers)
                : 1;
            Range range = new Range(toBigInt(start), toBigInt(end), toBigInt(stepVal));
            if (inst.dest != null) registers.put(inst.dest, range);
            return ExecutionResult.normal(range);
        }

        if (inst.opcode == Opcode.TAKE) {
            Range source = asRange(operandValue(inst.operands, 0, registers));
            BigInteger n = toBigInt(operandValue(inst.operands, 1, registers));
            List<BigInteger> out = take(source, n);
            if (inst.dest != null) registers.put(inst.dest, out);
            return ExecutionResult.normal(out);
        }

        if (inst.opcode == Opcode.NOP) {
            return ExecutionResult.normal(null);
        }

        if (inst.opcode == Opcode.BRANCH) {
            String label = asLabel(operandValue(inst.operands, 0, registers));
            Integer jumpTarget = label != null ? labels.get(label) : null;
            if (jumpTarget == null) {
                Object fallback = fallback(
                    artifact,
                    fallbackInterpreter,
                    "Unknown branch label: " + label
                );
                if (fallback == FALLBACK_SENTINEL) return ExecutionResult.fallback();
            }
            return ExecutionResult.jump(jumpTarget.intValue());
        }

        if (inst.opcode == Opcode.BRANCH_IF) {
            Object condition = operandValue(inst.operands, 0, registers);
            if (isTruthy(condition)) {
                String label = asLabel(operandValue(inst.operands, 1, registers));
                Integer jumpTarget = label != null ? labels.get(label) : null;
                if (jumpTarget == null) {
                    Object fallback = fallback(
                        artifact,
                        fallbackInterpreter,
                        "Unknown branch-if label: " + label
                    );
                    if (fallback == FALLBACK_SENTINEL) return ExecutionResult.fallback();
                }
                return ExecutionResult.jump(jumpTarget.intValue());
            }
            return ExecutionResult.normal(null);
        }

        if (inst.opcode == Opcode.LAZY_GET) {
            Object source = operandValue(inst.operands, 0, registers);
            BigInteger index = toBigInt(operandValue(inst.operands, 1, registers));
            Object out = lazyGet(source, index);
            if (inst.dest != null) registers.put(inst.dest, out);
            return ExecutionResult.normal(out);
        }

        if (inst.opcode == Opcode.LAZY_SET) {
            Object source = operandValue(inst.operands, 0, registers);
            BigInteger index = toBigInt(operandValue(inst.operands, 1, registers));
            Object value = operandValue(inst.operands, 2, registers);
            Object out = lazySet(source, index, value);
            if (out == FALLBACK_SENTINEL) {
                return ExecutionResult.fallback();
            }
            return ExecutionResult.normal(out);
        }

        if (inst.opcode == Opcode.LAZY_SIZE) {
            Object source = operandValue(inst.operands, 0, registers);
            Object out = lazySize(source);
            if (inst.dest != null) registers.put(inst.dest, out);
            return ExecutionResult.normal(out);
        }

        if (inst.opcode == Opcode.LAZY_COMMIT) {
            return ExecutionResult.normal(null);
        }

        if (inst.opcode == Opcode.MAP) {
            Object mapped = runMap(unit, inst, registers, fallbackInterpreter, artifact);
            if (mapped == FALLBACK_SENTINEL) {
                return ExecutionResult.fallback();
            }
            if (inst.dest != null) registers.put(inst.dest, mapped);
            return ExecutionResult.normal(mapped);
        }

        if (inst.opcode == Opcode.FILTER) {
            Object filtered = runFilter(unit, inst, registers, fallbackInterpreter, artifact);
            if (filtered == FALLBACK_SENTINEL) {
                return ExecutionResult.fallback();
            }
            if (inst.dest != null) registers.put(inst.dest, filtered);
            return ExecutionResult.normal(filtered);
        }

        if (inst.opcode == Opcode.REDUCE) {
            Object reduced = runReduce(unit, inst, registers, fallbackInterpreter, artifact);
            if (reduced == FALLBACK_SENTINEL) {
                return ExecutionResult.fallback();
            }
            if (inst.dest != null) registers.put(inst.dest, reduced);
            return ExecutionResult.normal(reduced);
        }

        if (inst.opcode == Opcode.FILTER_MAP) {
            Object filteredMapped = runFilterMap(unit, inst, registers, fallbackInterpreter, artifact);
            if (filteredMapped == FALLBACK_SENTINEL) {
                return ExecutionResult.fallback();
            }
            if (inst.dest != null) registers.put(inst.dest, filteredMapped);
            return ExecutionResult.normal(filteredMapped);
        }

        if (inst.opcode == Opcode.FILTER
            || inst.opcode == Opcode.SCAN
            || inst.opcode == Opcode.ZIP
            || inst.opcode == Opcode.WHERE
            || inst.opcode == Opcode.FILTER_MAP_REDUCE
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
            || inst.opcode == Opcode.LOAD) {
            Object fallback = fallback(artifact, fallbackInterpreter, "Opcode not yet natively executed: " + inst.opcode);
            if (fallback == FALLBACK_SENTINEL) return ExecutionResult.fallback();
        }

        if (inst.opcode == Opcode.CALL) {
            String functionName = String.valueOf(operandValue(inst.operands, 0, registers));
            Function target = findFunction(unit, functionName);
            if (target == null) {
                Object fallback = fallback(artifact, fallbackInterpreter, "Unknown function: " + functionName);
                if (fallback == FALLBACK_SENTINEL) return ExecutionResult.fallback();
            }
            List<Object> args = new ArrayList<Object>();
            for (int i = 1; i < inst.operands.size(); i++) {
                args.add(operandValue(inst.operands, i, registers));
            }
            Object result = executeFunction(unit, target, args, fallbackInterpreter, artifact);
            if (result == FALLBACK_SENTINEL) {
                return ExecutionResult.fallback();
            }
            if (inst.dest != null) registers.put(inst.dest, result);
            return ExecutionResult.normal(result);
        }

        if (inst.opcode == Opcode.RETURN) {
            return ExecutionResult.returned(operandValue(inst.operands, 0, registers));
        }

        return ExecutionResult.normal(null);
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

    private Map<String, Integer> indexLabels(Function function) {
        if (function == null || function.instructions == null || function.instructions.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Integer> labels = new HashMap<String, Integer>();
        for (int i = 0; i < function.instructions.size(); i++) {
            Instruction inst = function.instructions.get(i);
            if (inst == null) continue;
            if (inst.opcode == Opcode.NOP && inst.dest != null) {
                labels.put(inst.dest, Integer.valueOf(i));
            }
        }
        return labels;
    }

    private String asLabel(Object value) {
        return value == null ? null : String.valueOf(value);
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

    private List<Object> asSequence(Object value) {
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            return list;
        }
        if (value instanceof Range) {
            return materializeRange((Range) value);
        }
        return null;
    }

    private List<Object> materializeRange(Range range) {
        List<Object> out = new ArrayList<Object>();
        if (range == null || range.step == null || range.step.equals(BigInteger.ZERO)) return out;
        BigInteger current = range.start;
        BigInteger step = range.step;
        while (true) {
            if (step.compareTo(BigInteger.ZERO) >= 0) {
                if (current.compareTo(range.end) > 0) break;
            } else {
                if (current.compareTo(range.end) < 0) break;
            }
            out.add(current);
            current = current.add(step);
        }
        return out;
    }

    private Object runMap(
        Unit unit,
        Instruction inst,
        Map<String, Object> registers,
        Interpreter fallbackInterpreter,
        Artifact artifact
    ) {
        List<Object> source = asSequence(operandValue(inst.operands, 0, registers));
        if (source == null) {
            return fallback(artifact, fallbackInterpreter, "MAP source is not a sequence");
        }
        String mapperName = String.valueOf(operandValue(inst.operands, 1, registers));
        Function mapper = findFunction(unit, mapperName);
        if (mapper == null) {
            return fallback(artifact, fallbackInterpreter, "MAP function not found: " + mapperName);
        }
        List<Object> out = new ArrayList<Object>(source.size());
        for (int i = 0; i < source.size(); i++) {
            Object element = source.get(i);
            List<Object> args = new ArrayList<Object>();
            args.add(element);
            Object mapped = executeFunction(unit, mapper, args, fallbackInterpreter, artifact);
            if (mapped == FALLBACK_SENTINEL) {
                return FALLBACK_SENTINEL;
            }
            out.add(mapped);
        }
        return out;
    }

    private Object runFilter(
        Unit unit,
        Instruction inst,
        Map<String, Object> registers,
        Interpreter fallbackInterpreter,
        Artifact artifact
    ) {
        List<Object> source = asSequence(operandValue(inst.operands, 0, registers));
        if (source == null) {
            return fallback(artifact, fallbackInterpreter, "FILTER source is not a sequence");
        }
        String predicateName = String.valueOf(operandValue(inst.operands, 1, registers));
        Function predicate = findFunction(unit, predicateName);
        if (predicate == null) {
            return fallback(artifact, fallbackInterpreter, "FILTER function not found: " + predicateName);
        }
        List<Object> out = new ArrayList<Object>();
        for (int i = 0; i < source.size(); i++) {
            Object element = source.get(i);
            List<Object> args = new ArrayList<Object>();
            args.add(element);
            Object keep = executeFunction(unit, predicate, args, fallbackInterpreter, artifact);
            if (keep == FALLBACK_SENTINEL) {
                return FALLBACK_SENTINEL;
            }
            if (isTruthy(keep)) {
                out.add(element);
            }
        }
        return out;
    }

    private Object runReduce(
        Unit unit,
        Instruction inst,
        Map<String, Object> registers,
        Interpreter fallbackInterpreter,
        Artifact artifact
    ) {
        List<Object> source = asSequence(operandValue(inst.operands, 0, registers));
        if (source == null) {
            return fallback(artifact, fallbackInterpreter, "REDUCE source is not a sequence");
        }
        if (source.isEmpty()) {
            return null;
        }
        String reducerName = String.valueOf(operandValue(inst.operands, 1, registers));
        Function reducer = findFunction(unit, reducerName);
        if (reducer == null) {
            return fallback(artifact, fallbackInterpreter, "REDUCE function not found: " + reducerName);
        }
        Object accumulator = source.get(0);
        for (int i = 1; i < source.size(); i++) {
            List<Object> args = new ArrayList<Object>();
            args.add(accumulator);
            args.add(source.get(i));
            Object reduced = executeFunction(unit, reducer, args, fallbackInterpreter, artifact);
            if (reduced == FALLBACK_SENTINEL) {
                return FALLBACK_SENTINEL;
            }
            accumulator = reduced;
        }
        return accumulator;
    }

    private Object runFilterMap(
        Unit unit,
        Instruction inst,
        Map<String, Object> registers,
        Interpreter fallbackInterpreter,
        Artifact artifact
    ) {
        List<Object> source = asSequence(operandValue(inst.operands, 0, registers));
        if (source == null) {
            return fallback(artifact, fallbackInterpreter, "FILTER_MAP source is not a sequence");
        }
        String predicateName = String.valueOf(operandValue(inst.operands, 1, registers));
        String mapperName = String.valueOf(operandValue(inst.operands, 2, registers));
        Function predicate = findFunction(unit, predicateName);
        Function mapper = findFunction(unit, mapperName);
        if (predicate == null || mapper == null) {
            return fallback(
                artifact,
                fallbackInterpreter,
                "FILTER_MAP function not found: predicate=" + predicateName + ", mapper=" + mapperName
            );
        }
        List<Object> out = new ArrayList<Object>();
        for (int i = 0; i < source.size(); i++) {
            Object element = source.get(i);
            List<Object> predicateArgs = new ArrayList<Object>();
            predicateArgs.add(element);
            Object keep = executeFunction(unit, predicate, predicateArgs, fallbackInterpreter, artifact);
            if (keep == FALLBACK_SENTINEL) {
                return FALLBACK_SENTINEL;
            }
            if (isTruthy(keep)) {
                List<Object> mapperArgs = new ArrayList<Object>();
                mapperArgs.add(element);
                Object mapped = executeFunction(unit, mapper, mapperArgs, fallbackInterpreter, artifact);
                if (mapped == FALLBACK_SENTINEL) {
                    return FALLBACK_SENTINEL;
                }
                out.add(mapped);
            }
        }
        return out;
    }

    private Object lazyGet(Object source, BigInteger index) {
        if (source instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) source;
            int idx = normalizeListIndex(list.size(), index);
            return list.get(idx);
        }
        if (source instanceof Range) {
            Range range = (Range) source;
            BigInteger size = rangeSize(range);
            BigInteger normalized = normalizeRangeIndex(size, index);
            return range.start.add(range.step.multiply(normalized));
        }
        return null;
    }

    private Object lazySet(Object source, BigInteger index, Object value) {
        if (source instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) source;
            int idx = normalizeListIndex(list.size(), index);
            list.set(idx, value);
            return value;
        }
        return FALLBACK_SENTINEL;
    }

    private Object lazySize(Object source) {
        if (source instanceof List) {
            return Integer.valueOf(((List<?>) source).size());
        }
        if (source instanceof Range) {
            BigInteger size = rangeSize((Range) source);
            if (size.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0) {
                return Integer.valueOf(size.intValue());
            }
            return size;
        }
        return Integer.valueOf(0);
    }

    private int normalizeListIndex(int size, BigInteger index) {
        int idx = index.intValue();
        if (idx < 0) {
            idx = size + idx;
        }
        if (idx < 0 || idx >= size) {
            throw new ProgramError("Index: " + idx + ", Size: " + size);
        }
        return idx;
    }

    private BigInteger normalizeRangeIndex(BigInteger size, BigInteger index) {
        BigInteger idx = index;
        if (idx.compareTo(BigInteger.ZERO) < 0) {
            idx = size.add(idx);
        }
        if (idx.compareTo(BigInteger.ZERO) < 0 || idx.compareTo(size) >= 0) {
            throw new ProgramError("Index: " + idx + ", Size: " + size);
        }
        return idx;
    }

    private BigInteger rangeSize(Range range) {
        if (range == null || range.step == null || range.step.equals(BigInteger.ZERO)) {
            return BigInteger.ZERO;
        }
        boolean increasing = range.step.compareTo(BigInteger.ZERO) > 0;
        if (increasing && range.start.compareTo(range.end) > 0) return BigInteger.ZERO;
        if (!increasing && range.start.compareTo(range.end) < 0) return BigInteger.ZERO;

        BigInteger distance = increasing
            ? range.end.subtract(range.start)
            : range.start.subtract(range.end);
        BigInteger stride = range.step.abs();
        return distance.divide(stride).add(BigInteger.ONE);
    }

    private boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return ((Boolean) value).booleanValue();
        if (value instanceof Number) return ((Number) value).doubleValue() != 0.0d;
        if (value instanceof String) {
            String s = (String) value;
            return s.length() > 0 && !"false".equalsIgnoreCase(s);
        }
        if (value instanceof List) return !((List<?>) value).isEmpty();
        return true;
    }

    private boolean isNumericLike(Object value) {
        if (value == null) return false;
        if (value instanceof Number || value instanceof BigInteger) return true;
        try {
            new BigInteger(String.valueOf(value));
            return true;
        } catch (Exception ignored) {
            return false;
        }
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
