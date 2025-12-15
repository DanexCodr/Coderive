package cod.interpreter;

import cod.ast.nodes.*;
import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class NaturalArray {

    // Core formula
    private final RangeNode baseRange;
    private final InterpreterVisitor visitor;
    
    // Mutation support
    private Map<Long, Object> cache;  
    private boolean isMutable = false;
    
    // Cached calculations - PRESERVE ORIGINAL TYPES
    private Object cachedStart = null;    
    private Object cachedEnd = null;      
    private Object cachedStep = null;     
    private Long cachedSize = null;       
    
    // Type tracking (Determines if we use fast long arithmetic or precise BigDecimal)
    private boolean isLongOrIntegerRange = false;
    
    // === HIERARCHICAL LEXICOGRAPHICAL RANGE FIELDS ===
    private boolean isLexicographicalRange = false;
    private String startString = null;
    private String endString = null;
    
    // === PERFORMANCE OPTIMIZATIONS ===
    private Long cachedStartLong = null;
    private Long cachedStepLong = null;
    private BigDecimal cachedStartBD = null;
    private BigDecimal cachedStepBD = null;
    
    // Single-element cache for sequential access
    private transient Long lastIndex = null;
    private transient Object lastValue = null;
    
    // Precomputed powers of 26 for performance (up to length 10)
    private static final long[] POWERS_26 = new long[11];
    private static final long[] POWERS_2 = new long[11];
    private static final long[] TOTAL_UP_TO_LENGTH = new long[11];
    
    static {
        // Precompute for performance
        POWERS_26[0] = 1;
        POWERS_2[0] = 1;
        for (int i = 1; i <= 10; i++) {
            POWERS_26[i] = POWERS_26[i-1] * 26;
            POWERS_2[i] = POWERS_2[i-1] * 2;
        }
        
        // Total sequences up to each length: sum_{i=1}^{len} (2^i × 26^i)
        TOTAL_UP_TO_LENGTH[0] = 0;
        for (int len = 1; len <= 10; len++) {
            TOTAL_UP_TO_LENGTH[len] = TOTAL_UP_TO_LENGTH[len-1] + 
                                     POWERS_2[len] * POWERS_26[len];
        }
    }

    public NaturalArray(RangeNode range, InterpreterVisitor visitor) {
        this.baseRange = range;
        this.visitor = visitor;
        this.cache = null;
        this.isMutable = false;
        
        // Eagerly check type on construction
        getStart(); 
        getEnd();
        getStep();
        
        // Check if this is a lexicographical range
        // A range is lexicographical if start and end are strings AND step is 1 or implicit (null)
        if (cachedStart instanceof String && cachedEnd instanceof String && 
            (baseRange.step == null || (cachedStep instanceof Long && ((Long)cachedStep).longValue() == 1L))) {
            
            this.isLexicographicalRange = true;
            this.startString = (String) cachedStart;
            this.endString = (String) cachedEnd;
            this.isLongOrIntegerRange = false; // Must be false for string range
            
            // Validate the alphabet. We allow only a-z and A-Z.
            if (!isValidLexString(startString) || !isValidLexString(endString)) {
                throw new RuntimeException("Lexicographical range bounds must contain only letters (a-z, A-Z).");
            }
            
            // For hierarchical ordering, start must be <= end
            long startIdx = hierarchicalSequenceToIndex(startString);
            long endIdx = hierarchicalSequenceToIndex(endString);
            if (startIdx > endIdx) {
                throw new RuntimeException("Lexicographical range start must come before end in hierarchical ordering.");
            }
        }
    }
    
    // --- IMMUTABLE OPERATIONS ---
    
    public Object get(long index) {
        checkBounds(index);
        
        // 1. Single-element cache (extremely fast for sequential access)
        if (lastIndex != null && lastIndex == index) {
            return lastValue;
        }
        
        // 2. FAST PATH: If not mutable, calculate directly (no HashMap checks!)
        if (!isMutable) {
            Object result = calculateValue(index);
            lastIndex = index;
            lastValue = result;
            return result;
        }
        
        // 3. Only check cache if we're actually mutable
        if (cache != null) {
            Object val = cache.get(index);  // Single HashMap lookup
            if (val != null) {
                lastIndex = index;
                lastValue = val;
                return val;
            }
        }
        
        // 4. Calculate and cache
        Object result = calculateValue(index);
        lastIndex = index;
        lastValue = result;
        return result;
    }
    
    public long size() {
        if (cachedSize != null) return cachedSize;
        
        if (isLexicographicalRange) {
            // HIERARCHICAL Lexicographical Size
            long size = calculateLexSize();
            cachedSize = size;
            return size;
        }

        if (isLongOrIntegerRange) {
            // Use fast long arithmetic with cached values
            long startVal = getStartLong();
            long endVal = getEndLong();
            long stepVal = getStepLong();
            
            if (stepVal == 0) { cachedSize = 0L; return 0; }
            if ((stepVal > 0 && startVal > endVal) || (stepVal < 0 && startVal < endVal)) {
                 cachedSize = 0L; return 0;
            }
            
            // Fix: Add 1 correctly for the range
            long diff = Math.abs(endVal - startVal);
            long count = diff / Math.abs(stepVal) + 1;
            cachedSize = count;
            return count;
        }
        
        // Use precise BigDecimal arithmetic
        BigDecimal startBD = getStartBD();
        BigDecimal endBD = getEndBD();
        BigDecimal stepBD = getStepBD();
        
        if (stepBD.compareTo(BigDecimal.ZERO) == 0) {
            cachedSize = 0L;
            return 0;
        }

        // Check if range is valid
        boolean increasing = stepBD.compareTo(BigDecimal.ZERO) > 0;
        if ((increasing && startBD.compareTo(endBD) > 0) || (!increasing && startBD.compareTo(endBD) < 0)) {
            cachedSize = 0L;
            return 0;
        }
        
        // Calculate size: abs((end - start) / step) + 1
        BigDecimal diff = endBD.subtract(startBD);
        BigDecimal absStep = stepBD.abs();
        
        BigDecimal sizeBD = diff.abs().divide(absStep, 0, RoundingMode.DOWN).add(BigDecimal.ONE);
        
        // Check if it fits in long
        if (sizeBD.compareTo(new BigDecimal(Long.MAX_VALUE)) > 0) {
            throw new RuntimeException("Array size too large: exceeds long maximum.");
        }
        
        long size = sizeBD.longValue();
        cachedSize = size;
        return size;
    }
    
    // --- MUTABLE OPERATIONS ---
    
    public void set(long index, Object value) {
        checkBounds(index);
        
        // Clear single-element cache (it's now invalid)
        lastIndex = null;
        lastValue = null;
        
        if (!isMutable) {
            becomeMutable();
        }
        
        if (cache == null) {
            cache = new HashMap<Long, Object>();
        }
        
        cache.put(index, value);
    }
    
    // --- PRIVATE HELPERS ---
    
    private void checkBounds(long index) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
        }
    }
    
    private Object calculateValue(long index) {
        if (isLexicographicalRange) {
            // HIERARCHICAL Lexicographical Value Calculation
            return calculateLexValue(index);
        }

        if (isLongOrIntegerRange) {
            // Use CACHED long arithmetic for integer/long ranges (fast path)
            long startVal = getStartLong();
            long stepVal = getStepLong();
            long result = startVal + index * stepVal;
            
            if (cachedStart instanceof Integer && result >= Integer.MIN_VALUE && result <= Integer.MAX_VALUE) {
                return (int) result;
            }
            return result;
        }
        
        // Use precise BigDecimal arithmetic for floating point ranges
        BigDecimal startBD = getStartBD();
        BigDecimal stepBD = getStepBD();
        BigDecimal indexBD = BigDecimal.valueOf(index);
        
        // Calculation: start + index * step
        BigDecimal resultBD = startBD.add(indexBD.multiply(stepBD));
        
        // Return the BigDecimal object
        return resultBD;
    }

    // === PERFORMANCE OPTIMIZATION METHODS ===
    
    private long getStartLong() {
        if (cachedStartLong == null) {
            cachedStartLong = toLong(getStart());
        }
        return cachedStartLong;
    }
    
    private long getStepLong() {
        if (cachedStepLong == null) {
            cachedStepLong = toLong(getStep());
        }
        return cachedStepLong;
    }
    
    private long getEndLong() {
        // Not cached by default, but used in size()
        return toLong(getEnd());
    }
    
    private BigDecimal getStartBD() {
        if (cachedStartBD == null) {
            cachedStartBD = toBigDecimal(getStart());
        }
        return cachedStartBD;
    }
    
    private BigDecimal getStepBD() {
        if (cachedStepBD == null) {
            cachedStepBD = toBigDecimal(getStep());
        }
        return cachedStepBD;
    }
    
    private BigDecimal getEndBD() {
        // Not cached by default, but used in size()
        return toBigDecimal(getEnd());
    }

    // === HIERARCHICAL LEXICOGRAPHICAL LOGIC (OPTIMIZED & MERGED) ===

    private boolean isValidLexString(String s) {
        for (char c : s.toCharArray()) {
            if (!(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z')) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * HIERARCHICAL: Converts string to its global index. (OPTIMIZED SINGLE-PASS)
     */
    private long hierarchicalSequenceToIndex(String s) {
        int n = s.length();
        if (n > 10) {
            throw new RuntimeException("String too long for lexicographical range (max 10 characters)");
        }
        
        // 1. Add all sequences of shorter lengths
        long index = TOTAL_UP_TO_LENGTH[n - 1];
        
        // 2. SINGLE PASS: Compute pattern AND content simultaneously
        long patternMask = 0;
        long contentIndex = 0;
        
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            
            // Fast uppercase check (ASCII: 'A'-'Z' < 'a')
            boolean isUpper = c < 'a';
            
            if (isUpper) {
                // Set pattern bit (MSB is position 0)
                patternMask |= (1L << (n - 1 - i));
                
                // Fast lowercase conversion for content calculation
                c += 32;
            }
            
            // Fast char to 0-25 digit (ASCII math: 'a'-'a'=0)
            int digit = c - 'a';
            
            // Base 26 content calculation (Horner's method)
            contentIndex = contentIndex * 26 + digit;
        }
        
        // 3. Position: index + patternIndex * 26^n + contentIndex
        return index + (patternMask * POWERS_26[n]) + contentIndex;
    }
    
    /**
     * HIERARCHICAL: Converts global index back to string. (OPTIMIZED SINGLE-PASS)
     */
    private String hierarchicalIndexToSequence(long globalIndex) {
        // 1. Find length n
        int n = 1;
        while (n <= 10 && globalIndex >= TOTAL_UP_TO_LENGTH[n]) {
            n++;
        }
        
        if (n > 10) {
            throw new RuntimeException("Index too large for lexicographical range");
        }
        
        // 2. Index within length group
        long indexInLengthGroup = globalIndex - TOTAL_UP_TO_LENGTH[n - 1];
        
        // 3. Split into pattern and content
        long stringsPerPattern = POWERS_26[n];
        long patternIndex = indexInLengthGroup / stringsPerPattern;
        long contentIndex = indexInLengthGroup % stringsPerPattern;
        
        // 4. Convert contentIndex to base-26 digits and build array (Right-to-Left)
        char[] chars = new char[n];
        long temp = contentIndex;
        
        for (int i = n - 1; i >= 0; i--) {
            int digit = (int)(temp % 26);
            chars[i] = (char)('a' + digit); // Base lowercase character
            temp /= 26;
        }
        
        // 5. Apply case pattern using fast bit checks
        for (int i = 0; i < n; i++) {
            // Check uppercase bit: MSB = position 0 (first character)
            if (((patternIndex >> (n - 1 - i)) & 1) == 1) {
                // Fast uppercase: clear bit 5 (32)
                chars[i] &= ~32;
            }
        }
        
        return new String(chars);
    }
    
    /**
     * Calculate lexicographical size using HIERARCHICAL ordering.
     */
    private long calculateLexSize() {
        long startIndex = hierarchicalSequenceToIndex(startString);
        long endIndex = hierarchicalSequenceToIndex(endString);
        
        return endIndex - startIndex + 1;
    }
    
    /**
     * Calculate lexicographical value using HIERARCHICAL ordering.
     */
    private String calculateLexValue(long index) {
        long startIndex = hierarchicalSequenceToIndex(startString);
        long targetIndex = startIndex + index;
        return hierarchicalIndexToSequence(targetIndex);
    }
    
    private void becomeMutable() {
        this.isMutable = true;
    }
    
    // FIXED: Now detects integer BigDecimal ranges for fast path
    private Object getStart() {
        if (cachedStart != null) return cachedStart;
        Object startObj = visitor.dispatch(baseRange.start);
        
        // Check if it's an integer (Integer, Long, or integer BigDecimal)
        if (startObj instanceof Integer || startObj instanceof Long) {
            isLongOrIntegerRange = true;
        } else if (startObj instanceof BigDecimal) {
            BigDecimal bd = (BigDecimal) startObj;
            // Check if it's an exact integer
            try {
                bd.longValueExact();  // Will throw if not integer
                isLongOrIntegerRange = true;  // Integer BigDecimal!
            } catch (ArithmeticException e) {
                isLongOrIntegerRange = false;  // Fractional BigDecimal
            }
        } else {
            isLongOrIntegerRange = false;
        }
        
        cachedStart = startObj;
        return cachedStart;
    }
    
    // FIXED: Now handles BigDecimal in type detection
    private Object getEnd() {
        if (cachedEnd != null) return cachedEnd;
        Object endObj = visitor.dispatch(baseRange.end);
        
        // Update isLongOrIntegerRange based on end type
        if (isLongOrIntegerRange) {  // Only update if still true
            if (!(endObj instanceof Integer || endObj instanceof Long)) {
                if (endObj instanceof BigDecimal) {
                    BigDecimal bd = (BigDecimal) endObj;
                    try {
                        bd.longValueExact();  // Check if integer
                        // Keep isLongOrIntegerRange = true
                    } catch (ArithmeticException e) {
                        isLongOrIntegerRange = false;  // Fractional
                    }
                } else {
                    isLongOrIntegerRange = false;
                }
            }
        }
        
        cachedEnd = endObj;
        return cachedEnd;
    }
    
    // FIXED: Now handles BigDecimal in type detection
    private Object getStep() {
        if (cachedStep != null) return cachedStep;
        
        if (baseRange.step != null) {
            Object stepObj = visitor.dispatch(baseRange.step);
            
            // Update isLongOrIntegerRange based on step type
            if (isLongOrIntegerRange) {  // Only update if still true
                if (!(stepObj instanceof Integer || stepObj instanceof Long)) {
                    if (stepObj instanceof BigDecimal) {
                        BigDecimal bd = (BigDecimal) stepObj;
                        try {
                            bd.longValueExact();  // Check if integer
                            // Keep isLongOrIntegerRange = true
                        } catch (ArithmeticException e) {
                            isLongOrIntegerRange = false;  // Fractional
                        }
                    } else {
                        isLongOrIntegerRange = false;
                    }
                }
            }
            
            cachedStep = stepObj;
        } else {
            // Default step calculation (always 1 or -1) is always integer/long
            
            if (isLongOrIntegerRange) {
                long startVal = getStartLong();
                long endVal = getEndLong();
                cachedStep = (startVal <= endVal) ? 1L : -1L;
            } else {
                // If not integer range, but no step is given, still default to 1.0 or -1.0
                BigDecimal startVal = getStartBD();
                BigDecimal endVal = getEndBD();
                cachedStep = (startVal.compareTo(endVal) <= 0) ? BigDecimal.ONE : BigDecimal.ONE.negate();
            }
        }
        
        return cachedStep;
    }
    
    // FIXED: Better handling of BigDecimal to long conversion
    private long toLong(Object obj) {
        if (obj instanceof BigDecimal) {
            BigDecimal bd = (BigDecimal) obj;
            if (isLongOrIntegerRange) {
                // Only require exact for integer ranges
                try {
                    return bd.longValueExact();
                } catch (ArithmeticException e) {
                    throw new RuntimeException("Cannot convert non-integer BigDecimal to long: " + bd);
                }
            } else {
                // For fractional ranges, truncate (but this shouldn't be called in that case)
                return bd.longValue();
            }
        }
        if (obj instanceof Integer) return ((Integer) obj).longValue();
        if (obj instanceof Long) return (Long) obj;
        throw new RuntimeException("Cannot convert to long: " + obj);
    }
    
    private BigDecimal toBigDecimal(Object obj) {
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        if (obj instanceof String) {
            // Check if we're in a lexicographical range context
            if (cachedStart instanceof String || cachedEnd instanceof String) {
                // For lexicographical ranges, return a default value
                // This prevents the error when calculating size or other numeric operations
                return BigDecimal.ZERO;
            }
            throw new RuntimeException("Cannot convert string to BigDecimal for numeric calculation.");
        }
        return BigDecimal.valueOf(((Number)obj).doubleValue());
    }
    
    // --- UTILITIES ---
    
    public boolean isMutable() {
        return isMutable;
    }
    
    public int getCacheSize() {
        return cache != null ? cache.size() : 0;
    }
    
    public double getCacheRatio() {
        long size = size();
        if (size == 0) return 0.0;
        return cache != null ? (double) cache.size() / size : 0.0;
    }
    
    @Override
    public String toString() {
        // Always show as formula, never materialize
        Object start = getStart();
        Object end = getEnd();
        Object step = getStep();
        
        String formula;
        
        if (isLexicographicalRange) {
             formula = String.format("LexArray[\"%s\" to \"%s\"]", start, end);
        } else {
            formula = String.format("NaturalArray[%s to %s", start, end);
        }
        
        boolean isDefaultStep = false;
        if (!isLexicographicalRange) {
             if (isLongOrIntegerRange) {
                long s = getStepLong();
                long st = getStartLong();
                long en = getEndLong();
                isDefaultStep = (s == 1L && st <= en) || (s == -1L && st > en);
            } else {
                BigDecimal s = getStepBD();
                BigDecimal st = getStartBD();
                BigDecimal en = getEndBD();
                boolean increasing = st.compareTo(en) <= 0;
                isDefaultStep = (s.compareTo(BigDecimal.ONE) == 0 && increasing) || (s.compareTo(BigDecimal.ONE.negate()) == 0 && !increasing);
            }
        }

        if (!isDefaultStep) {
            formula += " step " + step;
        }
        
        if (!isLexicographicalRange) {
            formula += "]";
        }

        // Add state info
        formula += String.format(" (size: %d", size());
        if (isMutable) {
            formula += ", mutable, cache: " + getCacheSize() + "/" + size();
        } else {
            formula += ", immutable";
        }
        formula += ")";
        
        // For VERY small arrays, optionally show values
        if (size() <= 5) {
            StringBuilder sb = new StringBuilder("[");
            for (long i = 0; i < size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(get(i));
            }
            sb.append("]");
            return sb.toString();
        }
        
        return formula;
    }
    
    // Convert to traditional List (eager materialization)
    public List<Object> toList() {
        List<Object> result = new ArrayList<Object>();
        for (long i = 0; i < size(); i++) {
            result.add(get(i));
        }
        return result;
    }
}