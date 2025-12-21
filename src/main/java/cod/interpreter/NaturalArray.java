package cod.interpreter;

import cod.ast.nodes.*;
import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class NaturalArray {

    private final RangeNode baseRange;
    private final InterpreterVisitor visitor;
    private Map<Long, Object> cache;  
    private boolean isMutable = false;
    
    // Cached calculations
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
    private List<ConditionalFormula> conditionalFormulas = new ArrayList<>();
private List<MultiBranchFormula> multiBranchFormulas = new ArrayList<>();    
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
        
        getStart(); getEnd(); getStep();
        
        if (cachedStart instanceof String && cachedEnd instanceof String && 
            (baseRange.step == null || (cachedStep instanceof Long && ((Long)cachedStep).longValue() == 1L))) {
            
            this.isLexicographicalRange = true;
            this.startString = (String) cachedStart;
            this.endString = (String) cachedEnd;
            this.isLongOrIntegerRange = false;
            
            if (!isValidLexString(startString) || !isValidLexString(endString)) {
                throw new RuntimeException("Lexicographical range bounds must contain only letters (a-z, A-Z).");
            }
            if (hierarchicalSequenceToIndex(startString) > hierarchicalSequenceToIndex(endString)) {
                throw new RuntimeException("Lexicographical range start must come before end.");
            }
        }
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
        checkBounds(index);
        
        // 1. Single-element cache
        if (lastIndex != null && lastIndex == index) {
            return lastValue;
        }
        
        // 2. Manual overrides (user-set values)
        if (isMutable && cache != null && cache.containsKey(index)) {
            Object val = cache.get(index);
            lastIndex = index;
            lastValue = val;
            return val;
        }
        
        // 3. COMPUTED CACHE (for formula results)
        if (computedCache.containsKey(index)) {
            Object cached = computedCache.get(index);
            lastIndex = index;
            lastValue = cached;
            return cached;
        }
     
     // 4. Multibranch conditionals
    Object multiBranchResult = evaluateMultiBranchFormulas(index);
    if (multiBranchResult != null) {
        computedCache.put(index, multiBranchResult);
        lastIndex = index;
        lastValue = multiBranchResult;
        return multiBranchResult;
    }
        
        // 5. CONDITIONAL FORMULAS (NEW!)
        Object conditionalResult = evaluateConditionalFormulas(index);
        if (conditionalResult != null) {
            computedCache.put(index, conditionalResult);
            lastIndex = index;
            lastValue = conditionalResult;
            return conditionalResult;
        }
        
        // 6. Loop formulas (existing)
        Object loopResult = evaluateLoopFormula(index);
        if (loopResult != null) {
            computedCache.put(index, loopResult);
            lastIndex = index;
            lastValue = loopResult;
            return loopResult;
        }
        
        // 7. Base calculation
        Object result = calculateValue(index);
        lastIndex = index;
        lastValue = result;
        return result;
    }

public void addMultiBranchFormula(MultiBranchFormula formula) {
    multiBranchFormulas.add(formula);
    clearCache();
}

private Object evaluateMultiBranchFormulas(long index) {
    if (multiBranchFormulas.isEmpty()) {
        return null;
    }
    
    // Evaluate in reverse order (newest formulas override older ones)
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
        
        // Evaluate in reverse order (newest formulas override older ones)
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
    // Reverse iteration: newer formulas override older ones
    for (int i = loopFormulas.size() - 1; i >= 0; i--) {
        LoopFormula formula = loopFormulas.get(i);
        if (formula.contains(index)) {
            
            try {
                ExecutionContext currentCtx = visitor.getCurrentContext();
                
                // Merge current locals with loop index variable
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
                    
                    // Store in computedCache for future use
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
        
        if (sizeBD.compareTo(new BigDecimal(Long.MAX_VALUE)) > 0) throw new RuntimeException("Array size too large");
        
        cachedSize = sizeBD.longValue();
        return cachedSize;
    }
    
    public void set(long index, Object value) {
    checkBounds(index);
    
    // Clear single-element cache
    lastIndex = null;
    lastValue = null;
    
    // Clear computed cache for this index (formula result no longer valid)
    if (computedCache != null) {
        computedCache.remove(index);
    }
    
    // Ensure we're mutable
    if (!isMutable) {
        becomeMutable();
    }
    
    // Create cache if needed
    if (cache == null) {
        cache = new HashMap<Long, Object>();
    }
    
    // Store the manual override
    cache.put(index, value);
}
    
    private void checkBounds(long index) {
        if (index < 0 || index >= size()) throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
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

    private long getStartLong() {
        if (cachedStartLong == null) cachedStartLong = toLong(getStart());
        return cachedStartLong;
    }
    
    private long getStepLong() {
        if (cachedStepLong == null) cachedStepLong = toLong(getStep());
        return cachedStepLong;
    }
    
    private long getEndLong() { return toLong(getEnd()); }
    
    private BigDecimal getStartBD() {
        if (cachedStartBD == null) cachedStartBD = toBigDecimal(getStart());
        return cachedStartBD;
    }
    
    private BigDecimal getStepBD() {
        if (cachedStepBD == null) cachedStepBD = toBigDecimal(getStep());
        return cachedStepBD;
    }
    
    private BigDecimal getEndBD() { return toBigDecimal(getEnd()); }

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
    
    private void becomeMutable() {
        this.isMutable = true;
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
        } else {
            if (isLongOrIntegerRange) {
                long startVal = getStartLong();
                long endVal = getEndLong();
                cachedStep = (startVal <= endVal) ? 1L : -1L;
            } else {
                BigDecimal startVal = getStartBD();
                BigDecimal endVal = getEndBD();
                cachedStep = (startVal.compareTo(endVal) <= 0) ? BigDecimal.ONE : BigDecimal.ONE.negate();
            }
        }
        return cachedStep;
    }
    
    private void checkIntegerType(Object obj) {
        if (obj instanceof Integer || obj instanceof Long) {
            // OK, keep isLongOrIntegerRange = true (initially)
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
    
    private long toLong(Object obj) {
        if (obj instanceof BigDecimal) {
            if (isLongOrIntegerRange) return ((BigDecimal) obj).longValueExact();
            return ((BigDecimal) obj).longValue();
        }
        if (obj instanceof Integer) return ((Integer) obj).longValue();
        if (obj instanceof Long) return (Long) obj;
        throw new RuntimeException("Cannot convert to long: " + obj);
    }
    
    private BigDecimal toBigDecimal(Object obj) {
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        if (obj instanceof String) return BigDecimal.ZERO;
        return BigDecimal.valueOf(((Number)obj).doubleValue());
    }
    
    public boolean isMutable() { return isMutable; }
    public int getCacheSize() { return cache != null ? cache.size() : 0; }
    
    @Override
    public String toString() {
        Object start = getStart();
        Object end = getEnd();
        Object step = getStep();
        String formula = isLexicographicalRange ? 
            String.format("LexArray[\"%s\" to \"%s\"]", start, end) :
            String.format("NaturalArray[%s to %s", start, end);
            
        boolean isDefaultStep = false;
        if (!isLexicographicalRange) {
            if (isLongOrIntegerRange) {
                long s = getStepLong();
                isDefaultStep = (s == 1L && getStartLong() <= getEndLong()) || (s == -1L && getStartLong() > getEndLong());
            } else {
                BigDecimal s = getStepBD();
                boolean inc = getStartBD().compareTo(getEndBD()) <= 0;
                isDefaultStep = (s.compareTo(BigDecimal.ONE) == 0 && inc) || (s.compareTo(BigDecimal.ONE.negate()) == 0 && !inc);
            }
        }

        if (!isDefaultStep) formula += " step " + step;
        if (!isLexicographicalRange) formula += "]";
        
        StringBuilder sb = new StringBuilder(formula);
        sb.append(String.format(" (size: %d", size()));
        if (isMutable) sb.append(", mutable, cache: ").append(getCacheSize()).append("/").append(size());
        else sb.append(", immutable");
        sb.append(")");
        
        if (!loopFormulas.isEmpty()) {
            sb.append("\n  Loop formulas: ").append(loopFormulas.size());
        }
        return sb.toString();
    }
    
    public List<Object> toList() {
        List<Object> result = new ArrayList<Object>();
        for (long i = 0; i < size(); i++) result.add(get(i));
        return result;
    }
}