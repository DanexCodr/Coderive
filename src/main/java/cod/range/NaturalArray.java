package cod.range;

import cod.ast.nodes.*;
import cod.interpreter.*;
import cod.interpreter.context.ExecutionContext;
import cod.range.formula.*;

import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class NaturalArray {

    private final RangeNode baseRange;
    private final InterpreterVisitor visitor;
    private Map<Long, Object> cache;  
    private boolean isMutable = false;
    
    // Cached values
    private Object cachedStart = null;    
    private Object cachedEnd = null;      
    private Object cachedStep = null;     
    private Long cachedSize = null;       
    
    private boolean isLongOrIntegerRange = false;
    private boolean isLexicographicalRange = false;
    private String startString = null;
    private String endString = null;
    
    private Long cachedStartLong = null;
    private Long cachedStepLong = null;
    private BigDecimal cachedStartBD = null;
    private BigDecimal cachedStepBD = null;
    
    // Single-element cache
    private transient Long lastIndex = null;
    private transient Object lastValue = null;
    
    private static final long[] POWERS_26 = new long[11];
    private static final long[] POWERS_2 = new long[11];
    private static final long[] TOTAL_UP_TO_LENGTH = new long[11];
    
    private List<LoopFormula> loopFormulas = new ArrayList<LoopFormula>();
    private List<ConditionalFormula> conditionalFormulas = new ArrayList<ConditionalFormula>();
    private List<MultiBranchFormula> multiBranchFormulas = new ArrayList<MultiBranchFormula>();    
    private Map<Long, Object> computedCache = new HashMap<Long, Object>();
    
    static {
        POWERS_26[0] = 1;
        POWERS_2[0] = 1;
        for (int i = 1; i <= 10; i++) {
            POWERS_26[i] = POWERS_26[i-1] * 26;
            POWERS_2[i] = POWERS_2[i-1] * 2;
        }
        TOTAL_UP_TO_LENGTH[0] = 0;
        for (int len = 1; len <= 10; len++) {
            TOTAL_UP_TO_LENGTH[len] = TOTAL_UP_TO_LENGTH[len-1] + POWERS_2[len] * POWERS_26[len];
        }
    }

    public NaturalArray(RangeNode range, InterpreterVisitor visitor) {
    this.baseRange = range;
    this.visitor = visitor;
    this.cache = null;
    this.isMutable = false;
    
    // FIRST: Check if start/end are strings
    Object rawStart = visitor.dispatch(baseRange.start);
    Object rawEnd = visitor.dispatch(baseRange.end);
    
    if (rawStart instanceof String && rawEnd instanceof String) {
        // Handle as lexicographical range
        this.isLexicographicalRange = true;
        this.startString = (String) rawStart;
        this.endString = (String) rawEnd;
        this.isLongOrIntegerRange = false;
        
        // Validate and set cached values
        if (!isValidLexString(startString) || !isValidLexString(endString)) {
            throw new RuntimeException("Lexicographical range bounds must contain only letters (a-z, A-Z).");
        }
        if (hierarchicalSequenceToIndex(startString) > hierarchicalSequenceToIndex(endString)) {
            throw new RuntimeException("Lexicographical range start must come before end.");
        }
        
        // Set cached values to avoid recomputation
        this.cachedStart = rawStart;
        this.cachedEnd = rawEnd;
        this.cachedStep = (baseRange.step == null) ? 1L : visitor.dispatch(baseRange.step);
    } else {
        // Handle as numeric range
        this.isLongOrIntegerRange = true;
        getStart(); getEnd(); getStep();
    }
}
    
    // Lazy range view class
    private class LazyRangeView extends AbstractList<Object> {
        private final Object originalStart;
        private final Object originalEnd;
        private final long step;
        
        private long start;
        private long end;
        private boolean converted = false;
        private Long cachedSize = null;
        
        public LazyRangeView(RangeSpec range) {
            this.originalStart = range.start;
            this.originalEnd = range.end;
            this.step = calculateStep(range);
            
        }
        
        private void ensureConverted() {
            if (converted) return;
            
                start = toLongIndex(originalStart);
                if (start < 0) {
                    long arraySize = NaturalArray.this.size();
                    start = arraySize + start;
                }
            
            

                end = toLongIndex(originalEnd);
                if (end < 0) {
                    long arraySize = NaturalArray.this.size();
                    end = arraySize + end;
                }
            
            
            try {
                long arraySize = NaturalArray.this.size();
                if (start < 0 || start >= arraySize) {
                    throw new RuntimeException("Start index out of bounds: " + start);
                }
                if (end < 0 || end >= arraySize) {
                    throw new RuntimeException("End index out of bounds: " + end);
                }
            } catch (RuntimeException e) {
                // Individual element access will fail if out of bounds
            }
            
            converted = true;
        }
        
        @Override
        public Object get(int index) {
            if (index < 0) {
                throw new IndexOutOfBoundsException("Negative index: " + index);
            }
            
            ensureConverted();
            
            long actualIndex;
            if (step > 0) {
                actualIndex = start + index * step;
                if (actualIndex > end) {
                    throw new IndexOutOfBoundsException("Index " + index + " exceeds range view");
                }
            } else {
                actualIndex = start + index * step;
                if (actualIndex < end) {
                    throw new IndexOutOfBoundsException("Index " + index + " exceeds range view");
                }
            }
            
            return NaturalArray.this.get(actualIndex);
        }
        
        @Override
        public int size() {
            if (cachedSize != null) return cachedSize.intValue();
            
            ensureConverted();
            
            if (step == 0) {
                cachedSize = 0L;
                return 0;
            }
            
            long diff;
            if (step > 0) {
                diff = end - start;
            } else {
                diff = start - end;
            }
            
            long absStep = Math.abs(step);
            long size = diff / absStep + 1;
            
            if (size > Integer.MAX_VALUE) {
                throw new RuntimeException("Range view size too large: " + size);
            }
            
            cachedSize = size;
            return (int) size;
        }
        
        @Override
        public Iterator<Object> iterator() {
            return new LazyRangeIterator();
        }
        
        private class LazyRangeIterator implements Iterator<Object> {
            private int currentIndex = 0;
            
            @Override
            public boolean hasNext() {
                return currentIndex < size();
            }
            
            @Override
            public Object next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return get(currentIndex++);
            }
            
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }
    }
    
    // Lazy multi-range view class
    private class LazyMultiRangeView extends AbstractList<Object> {
        private final List<RangeSpec> ranges;
        private List<LazyRangeView> rangeViews;
        private int totalSize = -1;
        
        public LazyMultiRangeView(MultiRangeSpec multiRange) {
            this.ranges = multiRange.ranges;
            this.rangeViews = new ArrayList<LazyRangeView>();
            for (RangeSpec range : ranges) {
                rangeViews.add(new LazyRangeView(range));
            }
        }
        
        @Override
        public Object get(int index) {
            int currentIndex = index;
            for (LazyRangeView view : rangeViews) {
                int size = view.size();
                if (currentIndex < size) {
                    return view.get(currentIndex);
                }
                currentIndex -= size;
            }
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
        }
        
        @Override
        public int size() {
            if (totalSize >= 0) return totalSize;
            
            totalSize = 0;
            for (LazyRangeView view : rangeViews) {
                totalSize += view.size();
            }
            return totalSize;
        }
        
        @Override
        public Iterator<Object> iterator() {
            return new LazyMultiRangeIterator();
        }
        
        private class LazyMultiRangeIterator implements Iterator<Object> {
            private int currentRangeIndex = 0;
            private Iterator<Object> currentIterator = null;
            
            public LazyMultiRangeIterator() {
                if (!rangeViews.isEmpty()) {
                    currentIterator = rangeViews.get(0).iterator();
                }
            }
            
            @Override
            public boolean hasNext() {
                if (currentIterator == null) return false;
                
                if (currentIterator.hasNext()) {
                    return true;
                }
                
                currentRangeIndex++;
                if (currentRangeIndex < rangeViews.size()) {
                    currentIterator = rangeViews.get(currentRangeIndex).iterator();
                    return currentIterator.hasNext();
                }
                
                return false;
            }
            
            @Override
            public Object next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return currentIterator.next();
            }
            
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }
    }
    
    private BigDecimal toBigDecimalSafe(Object obj) {
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        if (obj instanceof String) {
            try {
                return new BigDecimal((String) obj);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Cannot convert to BigDecimal: " + obj);
            }
        }
        if (obj instanceof Number) {
            return new BigDecimal(obj.toString());
        }
        throw new RuntimeException("Cannot convert to BigDecimal: " + obj);
    }
    
    private long toLongSafe(Object obj) {
        if (obj instanceof BigDecimal) {
            try {
                return ((BigDecimal) obj).longValueExact();
            } catch (ArithmeticException e) {
                throw new RuntimeException("Cannot convert to exact long: " + obj);
            }
        }
        if (obj instanceof Integer) return ((Integer) obj).longValue();
        if (obj instanceof Long) return (Long) obj;
        if (obj instanceof String) {
            try {
                return Long.parseLong((String) obj);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Cannot convert to long: " + obj);
            }
        }
        throw new RuntimeException("Cannot convert to long: " + obj);
    }

    public void addLoopFormula(LoopFormula formula) {
        loopFormulas.add(formula);
        lastIndex = null;
        lastValue = null;
    }

    public void clearCache() {
        if (computedCache != null) {
            computedCache.clear();
        }
        lastIndex = null;
        lastValue = null;
    }
    
    public Object get(long index) {
        if (index < 0) {
            long size = size();
            index = size + index;
        }
        
        checkBounds(index);
        
        if (lastIndex != null && lastIndex == index) {
            return lastValue;
        }
        
        if (isMutable && cache != null && cache.containsKey(index)) {
            Object val = cache.get(index);
            lastIndex = index;
            lastValue = val;
            return val;
        }
        
        // FIX: Reinforced caching check and initialization
        if (computedCache != null && computedCache.containsKey(index)) {
            Object cached = computedCache.get(index);
            lastIndex = index;
            lastValue = cached;
            return cached;
        }
     
        Object multiBranchResult = evaluateMultiBranchFormulas(index);
        if (multiBranchResult != null) {
            if (computedCache == null) computedCache = new HashMap<Long, Object>();
            computedCache.put(index, multiBranchResult);
            lastIndex = index;
            lastValue = multiBranchResult;
            return multiBranchResult;
        }
        
        Object conditionalResult = evaluateConditionalFormulas(index);
        if (conditionalResult != null) {
            if (computedCache == null) computedCache = new HashMap<Long, Object>();
            computedCache.put(index, conditionalResult);
            lastIndex = index;
            lastValue = conditionalResult;
            return conditionalResult;
        }
        
        Object loopResult = evaluateLoopFormula(index);
        if (loopResult != null) {
            if (computedCache == null) computedCache = new HashMap<Long, Object>();
            computedCache.put(index, loopResult);
            lastIndex = index;
            lastValue = loopResult;
            return loopResult;
        }
        
        Object result = calculateValue(index);
        lastIndex = index;
        lastValue = result;
        return result;
    }
    
    private Object calculateValue(long index) {
        if (isLexicographicalRange) return calculateLexValue(index);
        
        if (isLongOrIntegerRange) {
            long startVal = getStartLong();
            long stepVal = getStepLong();
            long result = startVal + index * stepVal;
            
            if (cachedStart instanceof Integer && result >= Integer.MIN_VALUE && result <= Integer.MAX_VALUE) {
                return (int) result;
            }
            return result;
        }
        
        BigDecimal startBD = getStartBD();
        BigDecimal stepBD = getStepBD();
        BigDecimal indexBD = BigDecimal.valueOf(index);
        return startBD.add(indexBD.multiply(stepBD));
    }

    public long size() {
        if (cachedSize != null) return cachedSize;
        
        if (isLexicographicalRange) {
            cachedSize = calculateLexSize();
            return cachedSize;
        }

        if (isLongOrIntegerRange) {
            long startVal = getStartLong();
            long endVal = getEndLong();
            long stepVal = getStepLong();
            
            if (stepVal == 0) { cachedSize = 0L; return 0; }
            if ((stepVal > 0 && startVal > endVal) || (stepVal < 0 && startVal < endVal)) {
                 cachedSize = 0L; return 0;
            }
            
            long diff = Math.abs(endVal - startVal);
            cachedSize = diff / Math.abs(stepVal) + 1;
            return cachedSize;
        }
        
        BigDecimal startBD = getStartBD();
        BigDecimal endBD = getEndBD();
        BigDecimal stepBD = getStepBD();
        
        if (stepBD.compareTo(BigDecimal.ZERO) == 0) { cachedSize = 0L; return 0; }

        boolean increasing = stepBD.compareTo(BigDecimal.ZERO) > 0;
        if ((increasing && startBD.compareTo(endBD) > 0) || (!increasing && startBD.compareTo(endBD) < 0)) {
            cachedSize = 0L; return 0;
        }
        
        BigDecimal diff = endBD.subtract(startBD);
        BigDecimal absStep = stepBD.abs();
        BigDecimal sizeBD = diff.abs().divide(absStep, 0, RoundingMode.DOWN).add(BigDecimal.ONE);
        
        if (sizeBD.compareTo(new BigDecimal(Long.MAX_VALUE)) > 0) {
            throw new RuntimeException("Array size too large");
        }
        
        cachedSize = sizeBD.longValue();
        return cachedSize;
    }
    
    public void set(long index, Object value) {
        if (index < 0) {
            long size = size();
            index = size + index;
        }
        
        checkBounds(index);
        
        lastIndex = null;
        lastValue = null;
        
        if (computedCache != null) {
            computedCache.remove(index);
        }
        
        if (!isMutable) {
            becomeMutable();
        }
        
        if (cache == null) {
            cache = new HashMap<Long, Object>();
        }
        
        cache.put(index, value);
    }
    
    private void checkBounds(long index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Negative index: " + index);
        }
        
        if (index >= size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
        }
    }
    
    private Object getStart() {
        if (cachedStart != null) return cachedStart;
        Object startObj = visitor.dispatch(baseRange.start);       
            checkIntegerType(startObj);
            cachedStart = startObj;
        return cachedStart;
    }
    
    private Object getEnd() {
        if (cachedEnd != null) return cachedEnd;
        Object endObj = visitor.dispatch(baseRange.end);
        
            if (isLongOrIntegerRange) checkIntegerType(endObj);
            cachedEnd = endObj;
        return cachedEnd;
    }
    
    private Object getStep() {
        if (cachedStep != null) return cachedStep;
        if (baseRange.step != null) {
            Object stepObj = visitor.dispatch(baseRange.step);
            
            if (isLongOrIntegerRange) checkIntegerType(stepObj);
            cachedStep = stepObj;
        } else if (isLongOrIntegerRange) {
                long startVal = getStartLong();
                long endVal = getEndLong();
                cachedStep = (startVal <= endVal) ? 1L : -1L;
            } else {
                BigDecimal startVal = getStartBD();
                BigDecimal endVal = getEndBD();
                cachedStep = (startVal.compareTo(endVal) <= 0) ? BigDecimal.ONE : BigDecimal.ONE.negate();
            }
   
        return cachedStep;
    }
    
    private long getStartLong() {
        if (cachedStartLong == null) {
            cachedStartLong = toLongSafe(getStart());
        }
        return cachedStartLong;
    }
    
    private long getStepLong() {
        if (cachedStepLong == null) {
            cachedStepLong = toLongSafe(getStep());
        }
        return cachedStepLong;
    }
    
    private BigDecimal getStartBD() {
        if (cachedStartBD == null) {
            cachedStartBD = toBigDecimalSafe(getStart());
        }
        return cachedStartBD;
    }
    
    private BigDecimal getStepBD() {
        if (cachedStepBD == null) {
            cachedStepBD = toBigDecimalSafe(getStep());
        }
        return cachedStepBD;
    }
    
    private void checkIntegerType(Object obj) {
        if (obj instanceof Integer || obj instanceof Long) {
            // OK
        } else if (obj instanceof BigDecimal) {
            try {
                ((BigDecimal)obj).longValueExact();
            } catch (ArithmeticException e) {
                isLongOrIntegerRange = false;
            }
        } else {
            isLongOrIntegerRange = false;
        }
    }
    
    private void becomeMutable() {
        this.isMutable = true;
    }
    
    private boolean isValidLexString(String s) {
        for (char c : s.toCharArray()) {
            if (!(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z')) return false;
        }
        return true;
    }
    
    private long hierarchicalSequenceToIndex(String s) {
        int n = s.length();
        if (n > 10) throw new RuntimeException("String too long");
        long index = TOTAL_UP_TO_LENGTH[n - 1];
        long patternMask = 0;
        long contentIndex = 0;
        
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            boolean isUpper = c < 'a';
            if (isUpper) {
                patternMask |= (1L << (n - 1 - i));
                c += 32;
            }
            int digit = c - 'a';
            contentIndex = contentIndex * 26 + digit;
        }
        return index + (patternMask * POWERS_26[n]) + contentIndex;
    }
    
    private String hierarchicalIndexToSequence(long globalIndex) {
        int n = 1;
        while (n <= 10 && globalIndex >= TOTAL_UP_TO_LENGTH[n]) n++;
        if (n > 10) throw new RuntimeException("Index too large");
        
        long indexInLengthGroup = globalIndex - TOTAL_UP_TO_LENGTH[n - 1];
        long stringsPerPattern = POWERS_26[n];
        long patternIndex = indexInLengthGroup / stringsPerPattern;
        long contentIndex = indexInLengthGroup % stringsPerPattern;
        
        char[] chars = new char[n];
        long temp = contentIndex;
        
        for (int i = n - 1; i >= 0; i--) {
            int digit = (int)(temp % 26);
            chars[i] = (char)('a' + digit); 
            temp /= 26;
        }
        
        for (int i = 0; i < n; i++) {
            if (((patternIndex >> (n - 1 - i)) & 1) == 1) chars[i] &= ~32;
        }
        return new String(chars);
    }
    
    private long calculateLexSize() {
        return hierarchicalSequenceToIndex(endString) - hierarchicalSequenceToIndex(startString) + 1;
    }
    
    private String calculateLexValue(long index) {
        return hierarchicalIndexToSequence(hierarchicalSequenceToIndex(startString) + index);
    }
    
    private long toLongIndex(Object obj) {
        
        if (obj instanceof BigDecimal) {
            try {
                return ((BigDecimal) obj).longValueExact();
            } catch (ArithmeticException e) {
                throw new RuntimeException("Array index must be integer: " + obj);
            }
        }
        if (obj instanceof Integer) return ((Integer) obj).longValue();
        if (obj instanceof Long) return (Long) obj;
        if (obj instanceof String) {
            try {
                return Long.parseLong((String) obj);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Array index must be integer: " + obj);
            }
        }
        throw new RuntimeException("Cannot convert to long index: " + obj);
    }
    
    private long calculateStep(RangeSpec range) {
        if (range.step != null) {
            return toLongIndex(range.step);
        } else {
            
            long start = toLongIndex(range.start);
            long end = toLongIndex(range.end);
            if (start == end) return 1L;
            return (start < end) ? 1L : -1L;
        }
    }
    
    public List<Object> getRange(RangeSpec range) {
        return new LazyRangeView(range);
    }
    
    public List<Object> getMultiRange(MultiRangeSpec multiRange) {
        return new LazyMultiRangeView(multiRange);
    }
    
    public void setRange(RangeSpec range, Object value) {
        LazyRangeView view = new LazyRangeView(range);
        view.ensureConverted();  
        
        long start = view.start;
        long end = view.end;
        long step = view.step;

        long arraySize = size();
        if (start < 0 || start >= arraySize) {
            throw new RuntimeException("Start index out of bounds: " + start);
        }
        if (end < 0 || end >= arraySize) {
            throw new RuntimeException("End index out of bounds: " + end);
        }
        
        if (step > 0) {
            for (long i = start; i <= end && i < arraySize; i += step) {
                set(i, value);
            }
        } else if (step < 0) {
            for (long i = start; i >= end && i >= 0; i += step) {
                set(i, value);
            }
        } else {
            throw new RuntimeException("Step cannot be zero");
        }
    }
    
    public void setMultiRange(MultiRangeSpec multiRange, Object value) {
        for (RangeSpec range : multiRange.ranges) {
            setRange(range, value);
        }
    }
    
    public void addMultiBranchFormula(MultiBranchFormula formula) {
        multiBranchFormulas.add(formula);
        clearCache();
    }
    
    private Object evaluateMultiBranchFormulas(long index) {
        if (multiBranchFormulas.isEmpty()) {
            return null;
        }
        
        for (int i = multiBranchFormulas.size() - 1; i >= 0; i--) {
            MultiBranchFormula formula = multiBranchFormulas.get(i);
            if (formula.contains(index)) {
                Object result = formula.evaluate(index, visitor);
                if (result != null) {
                    return result;
                }
            }
        }
        
        return null;
    }
    
    public void addConditionalFormula(ConditionalFormula formula) {
        conditionalFormulas.add(formula);
        clearCache();
    }
    
    private Object evaluateConditionalFormulas(long index) {
        if (conditionalFormulas.isEmpty()) {
            return null;
        }
        
        for (int i = conditionalFormulas.size() - 1; i >= 0; i--) {
            ConditionalFormula formula = conditionalFormulas.get(i);
            if (formula.contains(index)) {
                Object result = formula.evaluate(index, visitor);
                if (result != null) {
                    return result;
                }
            }
        }
        
        return null;
    }
    
    private Object evaluateLoopFormula(long index) {    
        if (loopFormulas.isEmpty()) {
            return null;
        }
        
        for (int i = loopFormulas.size() - 1; i >= 0; i--) {
            LoopFormula formula = loopFormulas.get(i);
            if (formula.contains(index)) {
                try {
                    ExecutionContext currentCtx = visitor.getCurrentContext();
                    
                    Map<String, Object> mergedLocals = new HashMap<String, Object>(currentCtx.locals);
                    mergedLocals.put(formula.indexVar, index);
                    
                    ExecutionContext tempCtx = new ExecutionContext(
                        currentCtx.objectInstance,
                        mergedLocals,
                        currentCtx.slotValues,
                        currentCtx.slotTypes,
                        currentCtx.currentClass
                    );
                    
                    visitor.pushContext(tempCtx);
                    try {
                        Object result = formula.formula.accept(visitor);
                        
                        if (result != null) {
                            if (computedCache == null) {
                                computedCache = new HashMap<Long, Object>();
                            }
                            computedCache.put(index, result);
                        }
                        return result;
                    } finally {
                        visitor.popContext();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }
        return null;
    }
    
    private long getEndLong() {
        return toLongSafe(getEnd());
    }

    private BigDecimal getEndBD() {
        return toBigDecimalSafe(getEnd());
    }
    
    @Override
    public String toString() {
        Object start = getStart();
        Object end = getEnd();
        Object step = getStep();
        
        String startStr = formatForDisplay(start);
        String endStr = formatForDisplay(end);
        String stepStr = formatForDisplay(step);
        
        String formula = isLexicographicalRange ? 
            String.format("LexArray[\"%s\" to \"%s\"]", startStr, endStr) :
            String.format("NaturalArray[%s to %s", startStr, endStr);
            
        boolean isDefaultStep = false;
        if (!isLexicographicalRange) {
            if (isLongOrIntegerRange) {
                long s = getStepLong();
                long startLong = 0;
                try { startLong = getStartLong(); } catch (Exception e) {}
                long endLong = 0;
                try { endLong = toLongSafe(end); } catch (Exception e) {}
                isDefaultStep = (s == 1L && startLong <= endLong) || (s == -1L && startLong > endLong);
            } else {
                try {
                    BigDecimal s = getStepBD();
                    BigDecimal startBD = getStartBD();
                    BigDecimal endBD = toBigDecimalSafe(end);
                    boolean inc = startBD.compareTo(endBD) <= 0;
                    isDefaultStep = (s.compareTo(BigDecimal.ONE) == 0 && inc) || 
                                   (s.compareTo(BigDecimal.ONE.negate()) == 0 && !inc);
                } catch (Exception e) {
                    isDefaultStep = false;
                }
            }
        }

        if (!isDefaultStep) formula += " step " + stepStr;
        if (!isLexicographicalRange) formula += "]";
        
        StringBuilder sb = new StringBuilder(formula);
        
        try {
            long size = size();
            sb.append(String.format(" (size: %d", size));
        } catch (RuntimeException e) {
        }
        
        if (isMutable) sb.append(", mutable, cache: ").append(getCacheSize());
        else sb.append(", immutable");
        sb.append(")");
        
        if (!loopFormulas.isEmpty()) {
            sb.append("\n  Loop formulas: ").append(loopFormulas.size());
        }
        return sb.toString();
    }
    
    private String formatForDisplay(Object obj) {
        
        if (obj instanceof BigDecimal) {
            return ((BigDecimal) obj).stripTrailingZeros().toPlainString();
        }
        return String.valueOf(obj);
    }
    
    public List<Object> toList() {
           
        List<Object> result = new ArrayList<Object>();
        for (long i = 0; i < size(); i++) result.add(get(i));
        return result;
    }
    
    public boolean isMutable() { 
        return isMutable; 
    }
    
    public int getCacheSize() { 
        return cache != null ? cache.size() : 0; 
    }
}