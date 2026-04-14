package cod.range.formula;

import cod.math.AutoStackingNumber;
import java.util.Arrays;

public class LinearRecurrenceFormula {
    public final long start;
    public final long end;
    public final long recurrenceStart;
    public final int order;
    public final AutoStackingNumber[] coefficientsByLag;
    public final AutoStackingNumber constantTerm;
    public final AutoStackingNumber[] seedValues;
    public final long seedStartIndex;
    private final boolean hasConstantTerm;
    private final LinearRecurrenceFormula newerFormula;
    private final LinearRecurrenceFormula olderFormula;
    private transient long rollingIndex = Long.MIN_VALUE;
    private transient AutoStackingNumber[] rollingState = null;
    private static final AutoStackingNumber ZERO = AutoStackingNumber.fromLong(0L);
    private static final AutoStackingNumber ONE = AutoStackingNumber.fromLong(1L);

    public LinearRecurrenceFormula(
        long start,
        long end,
        long recurrenceStart,
        AutoStackingNumber[] coefficientsByLag,
        AutoStackingNumber constantTerm,
        AutoStackingNumber[] seedValues,
        long seedStartIndex
    ) {
        this.start = start;
        this.end = end;
        this.recurrenceStart = recurrenceStart;
        this.coefficientsByLag = coefficientsByLag;
        this.constantTerm = constantTerm != null ? constantTerm : ZERO;
        this.seedValues = seedValues;
        this.seedStartIndex = seedStartIndex;
        this.order = coefficientsByLag != null ? coefficientsByLag.length : 0;
        this.hasConstantTerm = !this.constantTerm.isZero();
        this.newerFormula = null;
        this.olderFormula = null;
        resetRollingState();
    }

    private LinearRecurrenceFormula(long start, long end,
                                    LinearRecurrenceFormula newerFormula,
                                    LinearRecurrenceFormula olderFormula) {
        this.start = start;
        this.end = end;
        this.recurrenceStart = 0L;
        this.order = 0;
        this.coefficientsByLag = null;
        this.constantTerm = ZERO;
        this.seedValues = null;
        this.seedStartIndex = 0L;
        this.hasConstantTerm = false;
        this.newerFormula = newerFormula;
        this.olderFormula = olderFormula;
        resetRollingState();
    }

    public static LinearRecurrenceFormula compose(LinearRecurrenceFormula newerFormula, LinearRecurrenceFormula olderFormula) {
        if (newerFormula == null) return olderFormula;
        if (olderFormula == null) return newerFormula;
        long mergedStart = Math.min(newerFormula.start, olderFormula.start);
        long mergedEnd = Math.max(newerFormula.end, olderFormula.end);
        return new LinearRecurrenceFormula(mergedStart, mergedEnd, newerFormula, olderFormula);
    }

    public boolean contains(long index) {
        if (isComposite()) {
            return (newerFormula != null && newerFormula.contains(index))
                || (olderFormula != null && olderFormula.contains(index));
        }
        return index >= start && index <= end;
    }

    public Object evaluate(long index) {
        if (isComposite()) {
            if (newerFormula != null && newerFormula.contains(index)) {
                return newerFormula.evaluate(index);
            }
            if (olderFormula != null && olderFormula.contains(index)) {
                return olderFormula.evaluate(index);
            }
            return null;
        }

        if (order <= 0 || seedValues == null || seedValues.length != order) {
            return null;
        }

        if (index < recurrenceStart) {
            int offset = (int) (index - seedStartIndex);
            if (offset >= 0 && offset < seedValues.length) {
                return seedValues[offset];
            }
            return null;
        }

        synchronized (this) {
            if (rollingState != null && index == rollingIndex) {
                return rollingState[0];
            }
            if (rollingState != null && index == rollingIndex + 1L) {
                advanceRollingState();
                rollingIndex = index;
                return rollingState[0];
            }
        }

        long lastSeedIndex = recurrenceStart - 1L;
        long steps = index - lastSeedIndex;

        int dim = hasConstantTerm ? order + 1 : order;
        AutoStackingNumber[][] transition = buildTransition(dim);
        AutoStackingNumber[] state = buildBaseState(dim);

        AutoStackingNumber[] result = applyMatrixPowerToVector(transition, steps, state);
        synchronized (this) {
            rollingState = copyState(result);
            rollingIndex = index;
        }
        return result[0];
    }

    private boolean isComposite() {
        return newerFormula != null || olderFormula != null;
    }

    private AutoStackingNumber[][] buildTransition(int dim) {
        AutoStackingNumber[][] transition = new AutoStackingNumber[dim][dim];
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                transition[i][j] = ZERO;
            }
        }

        for (int lag = 1; lag <= order; lag++) {
            AutoStackingNumber coeff = coefficientsByLag[lag - 1];
            transition[0][lag - 1] = coeff != null ? coeff : ZERO;
        }
        for (int i = 1; i < order; i++) {
            transition[i][i - 1] = ONE;
        }
        if (hasConstantTerm) {
            int last = dim - 1;
            transition[0][last] = constantTerm;
            transition[last][last] = ONE;
        }

        return transition;
    }

    private AutoStackingNumber[] buildBaseState(int dim) {
        AutoStackingNumber[] state = new AutoStackingNumber[dim];
        for (int j = 0; j < order; j++) {
            state[j] = seedValues[order - 1 - j];
        }
        if (hasConstantTerm) {
            state[dim - 1] = ONE;
        }
        return state;
    }

    private AutoStackingNumber[] copyState(AutoStackingNumber[] state) {
        return Arrays.copyOf(state, state.length);
    }

    private void advanceRollingState() {
        AutoStackingNumber next = hasConstantTerm ? constantTerm : ZERO;
        for (int lag = 1; lag <= order; lag++) {
            AutoStackingNumber coeff = coefficientsByLag[lag - 1];
            if (coeff != null && !coeff.isZero()) {
                next = next.add(coeff.multiply(rollingState[lag - 1]));
            }
        }

        AutoStackingNumber[] nextState = new AutoStackingNumber[rollingState.length];
        nextState[0] = next;
        for (int i = 1; i < order; i++) {
            nextState[i] = rollingState[i - 1];
        }

        if (hasConstantTerm) {
            nextState[nextState.length - 1] = ONE;
        }
        rollingState = nextState;
    }

    private void resetRollingState() {
        rollingIndex = Long.MIN_VALUE;
        rollingState = null;
    }

    private AutoStackingNumber[] applyMatrixPowerToVector(AutoStackingNumber[][] base, long exp, AutoStackingNumber[] vector) {
        AutoStackingNumber[] result = Arrays.copyOf(vector, vector.length);
        if (exp <= 0) {
            return result;
        }

        AutoStackingNumber[][] current = base;
        long e = exp;
        while (e > 0) {
            if ((e & 1L) == 1L) {
                result = multiply(current, result);
            }
            e >>= 1;
            if (e > 0) {
                current = multiply(current, current);
            }
        }
        return result;
    }

    private AutoStackingNumber[][] multiply(AutoStackingNumber[][] a, AutoStackingNumber[][] b) {
        int n = a.length;
        AutoStackingNumber[][] out = new AutoStackingNumber[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                AutoStackingNumber sum = ZERO;
                for (int k = 0; k < n; k++) {
                    AutoStackingNumber prod = a[i][k].multiply(b[k][j]);
                    sum = sum.add(prod);
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
                AutoStackingNumber prod = a[i][k].multiply(v[k]);
                sum = sum.add(prod);
            }
            out[i] = sum;
        }
        return out;
    }
}
