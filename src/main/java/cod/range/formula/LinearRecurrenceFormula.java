package cod.range.formula;

import cod.math.AutoStackingNumber;

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
    private final AutoStackingNumber[] precomputedValues;
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
        this.precomputedValues = buildPrecomputedValues();
    }

    public boolean contains(long index) {
        return index >= start && index <= end;
    }

    public Object evaluate(long index) {
        if (precomputedValues != null) {
            if (index < start || index > end) {
                return null;
            }
            long offset = index - start;
            if (offset < 0 || offset >= precomputedValues.length) {
                return null;
            }
            return precomputedValues[(int) offset];
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

        long lastSeedIndex = recurrenceStart - 1L;
        long steps = index - lastSeedIndex;

        int dim = hasConstantTerm ? order + 1 : order;
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

        AutoStackingNumber[] state = new AutoStackingNumber[dim];
        for (int j = 0; j < order; j++) {
            state[j] = seedValues[order - 1 - j];
        }
        if (hasConstantTerm) {
            state[dim - 1] = ONE;
        }

        AutoStackingNumber[][] power = matrixPow(transition, steps);
        AutoStackingNumber[] result = multiply(power, state);
        return result[0];
    }

    private AutoStackingNumber[] buildPrecomputedValues() {
        if (order <= 0 || seedValues == null || seedValues.length != order) {
            return null;
        }

        long total = end - start + 1L;
        if (total <= 0L || total > Integer.MAX_VALUE) {
            return null;
        }

        int size = (int) total;
        AutoStackingNumber[] table = new AutoStackingNumber[size];
        java.util.HashMap<Long, AutoStackingNumber> values = new java.util.HashMap<Long, AutoStackingNumber>();

        for (int i = 0; i < seedValues.length; i++) {
            values.put(seedStartIndex + i, seedValues[i]);
        }

        long computeStart = Math.min(start, seedStartIndex);
        for (long idx = computeStart; idx <= end; idx++) {
            AutoStackingNumber value = values.get(idx);
            if (value == null && !values.containsKey(idx)) {
                if (idx >= recurrenceStart) {
                    AutoStackingNumber sum = hasConstantTerm ? constantTerm : ZERO;
                    for (int lag = 1; lag <= order; lag++) {
                        AutoStackingNumber prev = values.get(idx - lag);
                        if (prev == null && !values.containsKey(idx - lag)) {
                            sum = null;
                            break;
                        }
                        AutoStackingNumber coeff = coefficientsByLag[lag - 1];
                        AutoStackingNumber safeCoeff = coeff != null ? coeff : ZERO;
                        if (prev == null) {
                            sum = null;
                            break;
                        }
                        sum = sum.add(safeCoeff.multiply(prev));
                    }
                    value = sum;
                } else {
                    value = null;
                }
                values.put(idx, value);
            }

            if (idx >= start) {
                table[(int) (idx - start)] = values.get(idx);
            }
        }

        return table;
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
