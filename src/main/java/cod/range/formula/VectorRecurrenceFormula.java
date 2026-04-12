package cod.range.formula;

import cod.math.AutoStackingNumber;

import java.util.Arrays;

public class VectorRecurrenceFormula {
    public final long start;
    public final long end;
    public final long recurrenceStart;
    public final long seedStartIndex;
    public final int dimension;
    public final int order;
    public final AutoStackingNumber[][] coefficients;
    public final AutoStackingNumber[] constant;
    public final AutoStackingNumber[][] seedValues;
    private final boolean hasConstantTerm;
    private transient long rollingIndex = Long.MIN_VALUE;
    private transient AutoStackingNumber[] rollingState = null;

    private static final AutoStackingNumber ZERO = AutoStackingNumber.fromLong(0L);
    private static final AutoStackingNumber ONE = AutoStackingNumber.fromLong(1L);

    public VectorRecurrenceFormula(
        long start,
        long end,
        long recurrenceStart,
        long seedStartIndex,
        int dimension,
        int order,
        AutoStackingNumber[][] coefficients,
        AutoStackingNumber[] constant,
        AutoStackingNumber[][] seedValues
    ) {
        this.start = start;
        this.end = end;
        this.recurrenceStart = recurrenceStart;
        this.seedStartIndex = seedStartIndex;
        this.dimension = dimension;
        this.order = order;
        this.coefficients = coefficients;
        this.constant = constant != null ? constant : zerosVector(dimension);
        this.seedValues = seedValues;
        this.hasConstantTerm = hasNonZeroConstant(this.constant);
        resetRollingState();
    }

    public boolean contains(long index) {
        return index >= start && index <= end;
    }

    public Object evaluate(long index, int sequenceIndex) {
        if (sequenceIndex < 0 || sequenceIndex >= dimension) {
            return null;
        }
        if (order <= 0 || dimension <= 0 || coefficients == null || seedValues == null) {
            return null;
        }
        if (seedValues.length != dimension) {
            return null;
        }
        for (int i = 0; i < dimension; i++) {
            if (seedValues[i] == null || seedValues[i].length != order) {
                return null;
            }
        }

        if (index < recurrenceStart) {
            Integer seedOffset = validateOffsetInIntRange(index - seedStartIndex);
            if (seedOffset == null) {
                return null;
            }
            if (seedOffset.intValue() >= order) {
                return null;
            }
            return seedValues[sequenceIndex][seedOffset.intValue()];
        }

        synchronized (this) {
            if (rollingState != null && index == rollingIndex) {
                return rollingState[sequenceIndex];
            }
            if (rollingState != null && index == rollingIndex + 1L) {
                advanceRollingState();
                rollingIndex = index;
                return rollingState[sequenceIndex];
            }
        }

        long lastSeedIndex = recurrenceStart - 1L;
        long steps = index - lastSeedIndex;
        int baseDim = dimension * order;
        int matrixDim = hasConstantTerm ? baseDim + 1 : baseDim;

        AutoStackingNumber[][] transition = buildTransition(baseDim, matrixDim);
        AutoStackingNumber[] state = buildBaseState(baseDim, matrixDim);
        AutoStackingNumber[][] power = matrixPow(transition, steps);
        AutoStackingNumber[] result = multiply(power, state);

        synchronized (this) {
            rollingState = Arrays.copyOf(result, baseDim);
            rollingIndex = index;
            return rollingState[sequenceIndex];
        }
    }

    private AutoStackingNumber[][] buildTransition(int baseDim, int matrixDim) {
        AutoStackingNumber[][] transition = new AutoStackingNumber[matrixDim][matrixDim];
        for (int i = 0; i < matrixDim; i++) {
            for (int j = 0; j < matrixDim; j++) {
                transition[i][j] = ZERO;
            }
        }

        for (int row = 0; row < dimension; row++) {
            AutoStackingNumber[] coeffRow = coefficients[row];
            if (coeffRow == null || coeffRow.length != baseDim) {
                continue;
            }
            for (int col = 0; col < baseDim; col++) {
                AutoStackingNumber c = coeffRow[col];
                transition[row][col] = c != null ? c : ZERO;
            }
        }

        for (int block = 1; block < order; block++) {
            for (int seq = 0; seq < dimension; seq++) {
                int row = (block * dimension) + seq;
                int col = ((block - 1) * dimension) + seq;
                transition[row][col] = ONE;
            }
        }

        if (hasConstantTerm) {
            int constCol = matrixDim - 1;
            for (int row = 0; row < dimension; row++) {
                AutoStackingNumber c = constant[row];
                transition[row][constCol] = c != null ? c : ZERO;
            }
            transition[constCol][constCol] = ONE;
        }
        return transition;
    }

    private AutoStackingNumber[] buildBaseState(int baseDim, int matrixDim) {
        AutoStackingNumber[] state = new AutoStackingNumber[matrixDim];
        for (int i = 0; i < matrixDim; i++) {
            state[i] = ZERO;
        }
        for (int block = 0; block < order; block++) {
            long sourceIndex = (recurrenceStart - 1L) - block;
            Integer seedOffset = validateOffsetInIntRange(sourceIndex - seedStartIndex);
            if (seedOffset == null) {
                return null;
            }
            for (int seq = 0; seq < dimension; seq++) {
                state[(block * dimension) + seq] = seedValues[seq][seedOffset.intValue()];
            }
        }
        if (hasConstantTerm) {
            state[matrixDim - 1] = ONE;
        }
        return state;
    }

    private void advanceRollingState() {
        int baseDim = dimension * order;
        AutoStackingNumber[] nextState = new AutoStackingNumber[baseDim];

        for (int row = 0; row < dimension; row++) {
            AutoStackingNumber sum = constant[row] != null ? constant[row] : ZERO;
            AutoStackingNumber[] coeffRow = coefficients[row];
            for (int col = 0; col < baseDim; col++) {
                AutoStackingNumber c = coeffRow[col];
                if (c != null && !c.isZero()) {
                    sum = sum.add(c.multiply(rollingState[col]));
                }
            }
            nextState[row] = sum;
        }

        for (int block = 1; block < order; block++) {
            for (int seq = 0; seq < dimension; seq++) {
                nextState[(block * dimension) + seq] = rollingState[((block - 1) * dimension) + seq];
            }
        }

        rollingState = nextState;
    }

    private AutoStackingNumber[][] matrixPow(AutoStackingNumber[][] base, long exp) {
        int dim = base.length;
        AutoStackingNumber[][] result = identity(dim);
        AutoStackingNumber[][] current = base;
        long e = exp;
        while (e > 0) {
            if ((e & 1L) == 1L) {
                result = multiply(result, current);
            }
            e >>= 1;
            if (e > 0) {
                current = multiply(current, current);
            }
        }
        return result;
    }

    private AutoStackingNumber[][] identity(int dim) {
        AutoStackingNumber[][] id = new AutoStackingNumber[dim][dim];
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                id[i][j] = (i == j) ? ONE : ZERO;
            }
        }
        return id;
    }

    private AutoStackingNumber[][] multiply(AutoStackingNumber[][] a, AutoStackingNumber[][] b) {
        int n = a.length;
        AutoStackingNumber[][] out = new AutoStackingNumber[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                AutoStackingNumber sum = ZERO;
                for (int k = 0; k < n; k++) {
                    sum = sum.add(a[i][k].multiply(b[k][j]));
                }
                out[i][j] = sum;
            }
        }
        return out;
    }

    private AutoStackingNumber[] multiply(AutoStackingNumber[][] a, AutoStackingNumber[] v) {
        int n = a.length;
        AutoStackingNumber[] out = new AutoStackingNumber[n];
        for (int i = 0; i < n; i++) {
            AutoStackingNumber sum = ZERO;
            for (int k = 0; k < n; k++) {
                sum = sum.add(a[i][k].multiply(v[k]));
            }
            out[i] = sum;
        }
        return out;
    }

    private static boolean hasNonZeroConstant(AutoStackingNumber[] constantVector) {
        if (constantVector == null) return false;
        for (AutoStackingNumber n : constantVector) {
            if (n != null && !n.isZero()) return true;
        }
        return false;
    }

    private static AutoStackingNumber[] zerosVector(int length) {
        AutoStackingNumber[] out = new AutoStackingNumber[length];
        for (int i = 0; i < length; i++) {
            out[i] = ZERO;
        }
        return out;
    }

    private Integer validateOffsetInIntRange(long value) {
        if (value < 0L || value > Integer.MAX_VALUE) {
            return null;
        }
        return Integer.valueOf((int) value);
    }

    private void resetRollingState() {
        rollingIndex = Long.MIN_VALUE;
        rollingState = null;
    }
}
