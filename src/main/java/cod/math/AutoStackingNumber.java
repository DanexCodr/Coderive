package cod.math;

import java.util.Arrays;

/**
 * Auto-stacking fixed-point number with 1-7 stacks of 64-bit words.
 * Each stack provides 64 bits: high word for integer part, low words for fraction.
 * Total precision: stacks × 64 bits (64 to 448 bits)
 * 
 * Java 7 compatible - no lambdas, no streams, no modern features.
 * 
 * Now with proper overflow checking! When a result exceeds 7 stacks,
 * an ArithmeticException is thrown rather than silent truncation.
 */
public class AutoStackingNumber implements Comparable<AutoStackingNumber> {
    
    // Maximum stacks (lucky number 7!)
    public static final int MAX_STACKS = 7;
    
    // Word size constants
    private static final int WORD_BITS = 64;
    private static final long WORD_MASK = 0xFFFFFFFFFFFFFFFFL;
    private static final long FRAC_MASK = (1L << 60) - 1;  // 60 bits of ones for fractional parts
    
    // Zero and One constants for each stack level
    private static final AutoStackingNumber[][] CONSTANTS = new AutoStackingNumber[MAX_STACKS + 1][3];
    
    // Instance fields
    private final int stacks;
    private final long[] words;  // words[0] = most significant, words[stacks-1] = least significant
    
    static {
        // Initialize constants lazily
        for (int s = 1; s <= MAX_STACKS; s++) {
            CONSTANTS[s] = new AutoStackingNumber[3];
        }
    }
    
    // ========== CONSTRUCTORS ==========
    
    public AutoStackingNumber(int stacks) {
        if (stacks < 1 || stacks > MAX_STACKS) {
            throw new IllegalArgumentException("Stacks must be 1-" + MAX_STACKS);
        }
        this.stacks = stacks;
        this.words = new long[stacks];
    }
    
    public AutoStackingNumber(long value) {
        this.stacks = 1;
        this.words = new long[1];
        this.words[0] = value;
    }
    
    public AutoStackingNumber(int stacks, long value) {
        if (stacks < 1 || stacks > MAX_STACKS) {
            throw new IllegalArgumentException("Stacks must be 1-" + MAX_STACKS);
        }
        this.stacks = stacks;
        this.words = new long[stacks];
        this.words[0] = value;
    }
    
    public AutoStackingNumber(long[] words) {
        if (words == null || words.length < 1 || words.length > MAX_STACKS) {
            throw new IllegalArgumentException("Words must be 1-" + MAX_STACKS + " elements");
        }
        this.stacks = words.length;
        this.words = Arrays.copyOf(words, words.length);
    }
    
    public AutoStackingNumber(AutoStackingNumber other) {
        this.stacks = other.stacks;
        this.words = Arrays.copyOf(other.words, other.words.length);
    }
    
    // ========== FACTORY METHODS ==========
    
    public static AutoStackingNumber zero(int stacks) {
        if (CONSTANTS[stacks][0] == null) {
            CONSTANTS[stacks][0] = new AutoStackingNumber(stacks);
        }
        return CONSTANTS[stacks][0];
    }
    
    public static AutoStackingNumber one(int stacks) {
        if (CONSTANTS[stacks][1] == null) {
            AutoStackingNumber one = new AutoStackingNumber(stacks);
            one.words[0] = 1L;
            CONSTANTS[stacks][1] = one;
        }
        return CONSTANTS[stacks][1];
    }
    
    public static AutoStackingNumber minusOne(int stacks) {
        if (CONSTANTS[stacks][2] == null) {
            AutoStackingNumber minusOne = new AutoStackingNumber(stacks);
            minusOne.words[0] = -1L;
            CONSTANTS[stacks][2] = minusOne;
        }
        return CONSTANTS[stacks][2];
    }
    
    public static AutoStackingNumber valueOf(String s) {
        if (s == null) {
            throw new NumberFormatException("Null string");
        }
        
        s = s.trim();
        
        // Handle empty string
        if (s.isEmpty()) {
            throw new NumberFormatException("Empty string");
        }
        
        // Handle sign
        boolean negative = false;
        if (s.startsWith("-")) {
            negative = true;
            s = s.substring(1);
        } else if (s.startsWith("+")) {
            s = s.substring(1);
        }
        
        // Handle empty after sign
        if (s.isEmpty()) {
            throw new NumberFormatException("Missing digits after sign");
        }
        
        // Check for decimal point
        int dotIndex = s.indexOf('.');
        if (dotIndex >= 0) {
            String intPart = s.substring(0, dotIndex);
            String fracPart = s.substring(dotIndex + 1);
            
            // Handle cases like ".5" or "5."
            if (intPart.isEmpty()) intPart = "0";
            if (fracPart.isEmpty()) fracPart = "0";
            
            // Remove leading zeros from integer part
            intPart = intPart.replaceFirst("^0+(?!$)", "");
            if (intPart.isEmpty()) intPart = "0";
            
            // Remove trailing zeros from fractional part
            fracPart = fracPart.replaceFirst("0+$", "");
            if (fracPart.isEmpty()) {
                // It was something like "5.0" - just return the integer part
                AutoStackingNumber result = valueOf(intPart);
                return negative ? result.negate() : result;
            }
            
            // Parse integer part using existing integer parsing
            AutoStackingNumber intNum;
            try {
                long longVal = Long.parseLong(intPart);
                intNum = new AutoStackingNumber(1, longVal);
            } catch (NumberFormatException e) {
                // Use your existing multi-word parsing logic
                intNum = parseMultiWordInteger(intPart);
            }
            
            // Parse fractional part: we need to create number = fracPart / 10^len
            // We'll do this by building the numerator and then dividing
            
            // First, create numerator from fractional digits
            AutoStackingNumber fracNum;
            try {
                long longFrac = Long.parseLong(fracPart);
                fracNum = new AutoStackingNumber(1, longFrac);
            } catch (NumberFormatException e) {
                // Fractional part too large for long - build it
                fracNum = parseMultiWordInteger(fracPart);
            }
            
            // Create denominator: 10^len
            int fracLen = fracPart.length();
            AutoStackingNumber denominator = new AutoStackingNumber(1, 10);
            for (int i = 1; i < fracLen; i++) {
                denominator = denominator.multiply(new AutoStackingNumber(1, 10));
            }
            
            // Divide numerator by denominator
            fracNum = fracNum.divide(denominator);
            
            // Add integer and fractional parts
            AutoStackingNumber result = intNum.add(fracNum);
            
            return negative ? result.negate() : result;
        }
        
        // No decimal point - use existing integer parsing logic
        // Try parsing as long first (fast path)
        try {
            long longValue = Long.parseLong(s);
            return new AutoStackingNumber(1, negative ? -longValue : longValue);
        } catch (NumberFormatException e) {
            // Not a long, continue with your existing multi-word parsing
        }
        
        // Use your existing multi-word parsing logic
        // Parse using your current method that builds the number word by word
        java.math.BigInteger bigInt;
        try {
            bigInt = new java.math.BigInteger(s);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Invalid number: " + (negative ? "-" : "") + s);
        }
        
        if (negative) {
            bigInt = bigInt.negate();
        }
        
        // Determine required stacks (your existing logic)
        int requiredStacks = 1;
        java.math.BigInteger twoTo64 = java.math.BigInteger.ONE.shiftLeft(64);
        java.math.BigInteger temp = bigInt.abs();
        while (temp.compareTo(twoTo64) >= 0) {
            requiredStacks++;
            temp = temp.shiftRight(64);
        }
        
        if (requiredStacks > MAX_STACKS) {
            throw new ArithmeticException(
                "Number exceeds maximum " + MAX_STACKS + 
                " stacks (" + (MAX_STACKS * 64) + " bits)"
            );
        }
        
        AutoStackingNumber result = new AutoStackingNumber(requiredStacks);
        temp = bigInt.abs();
        for (int i = 0; i < requiredStacks && temp.compareTo(java.math.BigInteger.ZERO) > 0; i++) {
            long wordValue = temp.and(java.math.BigInteger.valueOf(Long.MAX_VALUE)).longValue();
            result.words[requiredStacks - 1 - i] = wordValue;
            temp = temp.shiftRight(64);
        }
        
        if (negative) {
            result.words[0] = -result.words[0];
        }
        
        return result;
    }

    /**
     * Helper method to parse a multi-word integer string
     * (uses your existing logic but extracted for clarity)
     */
    private static AutoStackingNumber parseMultiWordInteger(String s) {
        java.math.BigInteger bigInt = new java.math.BigInteger(s);
        
        int requiredStacks = 1;
        java.math.BigInteger twoTo64 = java.math.BigInteger.ONE.shiftLeft(64);
        java.math.BigInteger temp = bigInt.abs();
        while (temp.compareTo(twoTo64) >= 0) {
            requiredStacks++;
            temp = temp.shiftRight(64);
        }
        
        if (requiredStacks > MAX_STACKS) {
            throw new ArithmeticException(
                "Number exceeds maximum " + MAX_STACKS + 
                " stacks (" + (MAX_STACKS * 64) + " bits)"
            );
        }
        
        AutoStackingNumber result = new AutoStackingNumber(requiredStacks);
        temp = bigInt.abs();
        for (int i = 0; i < requiredStacks && temp.compareTo(java.math.BigInteger.ZERO) > 0; i++) {
            long wordValue = temp.and(java.math.BigInteger.valueOf(Long.MAX_VALUE)).longValue();
            result.words[requiredStacks - 1 - i] = wordValue;
            temp = temp.shiftRight(64);
        }
        
        return result;
    }
    
    public static AutoStackingNumber fromLong(long value) {
        return new AutoStackingNumber(value);
    }
    
    /**
     * Double to AutoStackingNumber conversion without using valueOf()
     * This breaks the circular dependency with valueOf() and divide()
     */
    public static AutoStackingNumber fromDouble(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("Cannot convert NaN or Infinity");
        }
        
        boolean negative = value < 0;
        value = Math.abs(value);
        
        long intPart = (long) value;
        double fracPart = value - intPart;
        
        AutoStackingNumber result = new AutoStackingNumber(MAX_STACKS);
        result.words[0] = negative ? -intPart : intPart;
        
        double remaining = fracPart;
        for (int i = 1; i < MAX_STACKS && remaining > 1e-18; i++) {
            remaining *= 1L << 60;
            long word = (long) remaining;
            result.words[i] = word;
            remaining -= word;
            // DON'T divide here - remaining is already the fractional part for next word
        }
        
        return result;
    }

    // ========== OVERFLOW CHECKING ==========
    
    /**
     * Check if a carry would cause overflow beyond MAX_STACKS
     */
    private void checkOverflow(long carry, int requiredStacks) {
        if (carry != 0 && requiredStacks > MAX_STACKS) {
            throw new ArithmeticException(
                "Number exceeds maximum " + MAX_STACKS + 
                " stacks (" + (MAX_STACKS * 64) + " bits)"
            );
        }
    }
    
    /**
     * Check if this number is the minimum value (special case for negation)
     */
    private boolean isMinValue() {
        if (stacks < MAX_STACKS) return false;
        // Check if all words are zero except most significant which is Long.MIN_VALUE
        if (words[0] != Long.MIN_VALUE) return false;
        for (int i = 1; i < stacks; i++) {
            if (words[i] != 0) return false;
        }
        return true;
    }
    
    // ========== CORE ARITHMETIC ==========
    
    public AutoStackingNumber add(AutoStackingNumber other) {
        int maxStacks = Math.max(this.stacks, other.stacks);
        AutoStackingNumber result = new AutoStackingNumber(maxStacks);
        
        long carry = 0;
        for (int i = maxStacks - 1; i >= 0; i--) {
            long a = i < this.stacks ? this.words[i] : 0;
            long b = i < other.stacks ? other.words[i] : 0;
            
            long sum = a + b + carry;
            
            // Check for overflow to next word
            if (a >= 0 && b >= 0 && sum < 0) {
                carry = 1;
            } else if (a < 0 && b < 0 && sum >= 0) {
                carry = 1;
            } else {
                carry = 0;
            }
            
            result.words[i] = sum;
        }
        
        // Check for overflow beyond MAX_STACKS
        checkOverflow(carry, maxStacks + 1);
        
        if (carry != 0 && maxStacks < MAX_STACKS) {
            AutoStackingNumber promoted = new AutoStackingNumber(maxStacks + 1);
            System.arraycopy(result.words, 0, promoted.words, 1, maxStacks);
            promoted.words[0] = carry;
            return promoted;
        }
        
        return result;
    }
    
    public AutoStackingNumber subtract(AutoStackingNumber other) {
        return add(other.negate());
    }
    
    public AutoStackingNumber multiply(AutoStackingNumber other) {
        boolean resultNegative = (isNegative() ^ other.isNegative());
        AutoStackingNumber a = abs();
        AutoStackingNumber b = other.abs();
        
        // Simple case: single stack multiplication
        if (a.stacks == 1 && b.stacks == 1) {
            long product = a.words[0] * b.words[0];
            
            // Check if product overflowed 64 bits
            if (a.words[0] != 0 && product / a.words[0] != b.words[0]) {
                // Handle as multi-stack
                return multiplyMulti(a, b, resultNegative);
            }
            
            AutoStackingNumber result = new AutoStackingNumber(1, resultNegative ? -product : product);
            return result;
        }
        
        return multiplyMulti(a, b, resultNegative);
    }
    
    /**
     * Multi-stack multiplication with overflow checking
     */
    private AutoStackingNumber multiplyMulti(AutoStackingNumber a, AutoStackingNumber b, boolean resultNegative) {
        int resultStacks = Math.min(a.stacks + b.stacks, MAX_STACKS);
        AutoStackingNumber result = new AutoStackingNumber(resultStacks);
        
        // Multi-stack multiplication using 32-bit splitting to avoid overflow
        long[] temp = new long[resultStacks * 2 + 2]; // Extra space for overflow detection
        
        for (int i = 0; i < a.stacks; i++) {
            long aWord = a.words[i] & WORD_MASK;
            for (int j = 0; j < b.stacks; j++) {
                long bWord = b.words[j] & WORD_MASK;
                
                // Split into high/low 32-bit parts
                long aLow = aWord & 0xFFFFFFFFL;
                long aHigh = aWord >>> 32;
                long bLow = bWord & 0xFFFFFFFFL;
                long bHigh = bWord >>> 32;
                
                long lowLow = aLow * bLow;
                long lowHigh = aLow * bHigh;
                long highLow = aHigh * bLow;
                long highHigh = aHigh * bHigh;
                
                // Combine with carries
                int idx = i + j;
                temp[idx] += lowLow;
                temp[idx + 1] += (lowLow >>> 32) + lowHigh + highLow;
                temp[idx + 2] += (lowHigh >>> 32) + (highLow >>> 32) + highHigh;
                temp[idx + 3] += highHigh >>> 32;
            }
        }
        
        // Normalize carries and check for overflow
        long carry = 0;
        for (int i = 0; i < resultStacks; i++) {
            long val = temp[i] + carry;
            result.words[i] = val;
            carry = val >>> WORD_BITS;
            
            // Check if we're accumulating carry that would require more stacks
            if (i == resultStacks - 1 && carry != 0 && resultStacks == MAX_STACKS) {
                throw new ArithmeticException(
                    "Multiplication exceeds maximum " + MAX_STACKS + 
                    " stacks (" + (MAX_STACKS * 64) + " bits)"
                );
            }
        }
        
        // Check for overflow in higher temp words
        for (int i = resultStacks; i < temp.length; i++) {
            if (temp[i] != 0) {
                throw new ArithmeticException("Multiplication overflow");
            }
        }
        
        if (resultNegative) {
            result.words[0] = -result.words[0];
        }
        
        return result;
    }
    
    public AutoStackingNumber divide(AutoStackingNumber other) {
        if (other.isZero()) {
            throw new ArithmeticException("Division by zero");
        }
        
        // Simple case: single stack integer division
        if (this.stacks == 1 && other.stacks == 1) {
            long dividend = this.words[0];
            long divisor = other.words[0];
            
            if (divisor == 0) {
                throw new ArithmeticException("Division by zero");
            }
            
            // Check if division is exact (no remainder)
            if (dividend % divisor == 0) {
                long quotient = dividend / divisor;
                return new AutoStackingNumber(1, quotient);
            }
            
            // Not exact - use double but avoid circular dependency
            double result = (double) dividend / (double) divisor;
            return fromDouble(result);
        }
        
        // For multi-stack, use doubleValue()
        double dividend = this.doubleValue();
        double divisor = other.doubleValue();
        
        if (divisor == 0) {
            throw new ArithmeticException("Division by zero");
        }
        
        double quotient = dividend / divisor;
        return fromDouble(quotient);
    }
    
    public AutoStackingNumber remainder(AutoStackingNumber other) {
        if (other.isZero()) {
            throw new ArithmeticException("Division by zero");
        }
        
        // Simple case: single stack integer remainder
        if (this.stacks == 1 && other.stacks == 1) {
            long dividend = this.words[0];
            long divisor = other.words[0];
            
            if (divisor == 0) {
                throw new ArithmeticException("Division by zero");
            }
            
            long remainder = dividend % divisor;
            return new AutoStackingNumber(1, remainder);
        }
        
        // Use division algorithm for multi-stack
        AutoStackingNumber quotient = divide(other);
        AutoStackingNumber product = quotient.multiply(other);
        return this.subtract(product);
    }
    
    public AutoStackingNumber negate() {
        // Special case: can't negate MIN_VALUE
        if (isMinValue()) {
            throw new ArithmeticException("Cannot negate minimum value");
        }
        
        AutoStackingNumber result = new AutoStackingNumber(this.stacks);
        for (int i = 0; i < stacks; i++) {
            result.words[i] = -this.words[i];
        }
        return result;
    }
    
    public AutoStackingNumber abs() {
        if (!isNegative()) {
            return this;
        }
        return negate();
    }
    
    public AutoStackingNumber shiftLeft(int bits) {
        if (bits == 0) return this;
        if (bits < 0) return shiftRight(-bits);
        
        int wordShift = bits / WORD_BITS;
        int bitShift = bits % WORD_BITS;
        
        int newStacks = Math.min(stacks + wordShift + (bitShift > 0 ? 1 : 0), MAX_STACKS);
        
        // Check if shifting would lose data
        if (newStacks == MAX_STACKS && stacks + wordShift + (bitShift > 0 ? 1 : 0) > MAX_STACKS) {
            // Check if any bits would be shifted out of existence
            if (wordShift > 0) {
                for (int i = MAX_STACKS - wordShift; i < stacks; i++) {
                    if (words[i] != 0) {
                        throw new ArithmeticException("Shift left would lose data (exceeds " + MAX_STACKS + " stacks)");
                    }
                }
            }
            if (bitShift > 0 && stacks > 0) {
                long highBits = words[stacks - 1] >>> (WORD_BITS - bitShift);
                if (highBits != 0) {
                    throw new ArithmeticException("Shift left would lose data (exceeds " + MAX_STACKS + " stacks)");
                }
            }
        }
        
        AutoStackingNumber result = new AutoStackingNumber(newStacks);
        
        long carry = 0;
        for (int i = 0; i < stacks; i++) {
            int newIdx = i + wordShift;
            if (newIdx < newStacks) {
                long val = (this.words[i] & WORD_MASK) << bitShift;
                result.words[newIdx] = (val & WORD_MASK) | carry;
                carry = val >>> WORD_BITS;
            }
        }
        
        // Check if final carry would require more stacks
        if (carry != 0 && newStacks == MAX_STACKS) {
            throw new ArithmeticException("Shift left overflow (exceeds " + MAX_STACKS + " stacks)");
        }
        
        return result;
    }
    
    public AutoStackingNumber shiftRight(int bits) {
        if (bits == 0) return this;
        if (bits < 0) return shiftLeft(-bits);
        
        int wordShift = bits / WORD_BITS;
        int bitShift = bits % WORD_BITS;
        
        if (wordShift >= stacks) {
            return zero(1);
        }
        
        AutoStackingNumber result = new AutoStackingNumber(stacks - wordShift);
        
        long carry = 0;
        for (int i = stacks - 1; i >= wordShift; i--) {
            long val = (this.words[i] & WORD_MASK) >>> bitShift;
            result.words[i - wordShift] = val | (carry << (WORD_BITS - bitShift));
            carry = this.words[i] & ((1L << bitShift) - 1);
        }
        
        return result;
    }
    
    // ========== COMPARISON ==========
    
    @Override
    public int compareTo(AutoStackingNumber other) {
        // Compare as signed numbers
        long thisVal = this.words[0];
        long otherVal = other.words[0];
        
        if (thisVal < otherVal) return -1;
        if (thisVal > otherVal) return 1;
        
        // Compare remaining words if same sign and first word equal
        for (int i = 1; i < Math.max(this.stacks, other.stacks); i++) {
            long a = i < this.stacks ? (this.words[i] & WORD_MASK) : 0;
            long b = i < other.stacks ? (other.words[i] & WORD_MASK) : 0;
            
            if (a < b) return -1;
            if (a > b) return 1;
        }
        
        return 0;
    }
    
    public boolean isZero() {
        for (long word : words) {
            if (word != 0) return false;
        }
        return true;
    }
    
    public boolean isNegative() {
        return words[0] < 0;
    }
    
    public boolean isPositive() {
        return words[0] > 0;
    }
    
    // ========== CONVERSION METHODS ==========
    
    public long longValue() {
        if (stacks > 1) {
            for (int i = 1; i < stacks; i++) {
                if (words[i] != 0) {
                    throw new ArithmeticException("Number has fractional part");
                }
            }
        }
        return words[0];
    }
    
    public double doubleValue() {
        double result = 0;
        double scale = 1.0;
        
        boolean negative = isNegative();
        
        // Integer part (word 0)
        long intVal = words[0] & WORD_MASK;
        if (negative) {
            intVal = (-words[0]) & WORD_MASK;
        }
        result = intVal;
        
        // Fractional parts - each subsequent word represents 2^(-60) of the previous
        scale = 1.0 / (1L << 60);  // 2^-60
        for (int i = 1; i < stacks; i++) {
            long wordVal = words[i] & WORD_MASK;
            result += wordVal * scale;
            scale /= (1L << 60);  // Divide by 2^60 for each subsequent word
        }
        
        return negative ? -result : result;
    }
    
    @Override
    public String toString() {
        if (isZero()) {
            return "0";
        }
        
        StringBuilder sb = new StringBuilder();
        boolean negative = isNegative();
        
        if (negative) {
            sb.append('-');
        }
        
        // Integer part
        long intPart = negative ? -words[0] : words[0];
        sb.append(intPart);
        
        // Check for fractional part
        boolean hasFraction = false;
        for (int i = 1; i < stacks; i++) {
            if ((words[i] & FRAC_MASK) != 0) {
                hasFraction = true;
                break;
            }
        }
        
        if (hasFraction) {
            sb.append('.');
            
            // Work with unsigned 60‑bit fractional words
            long[] fracWords = new long[stacks - 1];
            for (int i = 0; i < stacks - 1; i++) {
                fracWords[i] = words[i + 1] & FRAC_MASK;
            }
            
            int maxDigits = 18;
            int digitCount = 0;
            
            while (digitCount < maxDigits) {
                long carry = 0;
                // Multiply each fractional word by 10 in base 2^60
                for (int i = fracWords.length - 1; i >= 0; i--) {
                    long word = fracWords[i];
                    long product = word * 10L + carry;
                    fracWords[i] = product & FRAC_MASK;      // new word = product mod 2^60
                    carry = product >>> 60;                  // carry = product / 2^60
                }
                sb.append(carry);   // next decimal digit
                digitCount++;
                
                // Check if all words became zero
                boolean allZero = true;
                for (int i = 0; i < fracWords.length; i++) {
                    if (fracWords[i] != 0) {
                        allZero = false;
                        break;
                    }
                }
                if (allZero) break;
            }
        }
        
        return sb.toString();
    }
    
    public String toPlainString() {
        return toString();
    }
    
    // ========== UTILITY METHODS ==========
    
    public int getStacks() {
        return stacks;
    }
    
    public long[] getWords() {
        return Arrays.copyOf(words, words.length);
    }
    
    public boolean fitsInStacks(int targetStacks) {
        if (targetStacks >= stacks) return true;
        for (int i = targetStacks; i < stacks; i++) {
            if (words[i] != 0) return false;
        }
        return true;
    }
    
    public AutoStackingNumber promote(int newStacks) {
        if (newStacks <= stacks) {
            return this;
        }
        if (newStacks > MAX_STACKS) {
            throw new IllegalArgumentException("Cannot promote beyond " + MAX_STACKS + " stacks");
        }
        
        AutoStackingNumber result = new AutoStackingNumber(newStacks);
        System.arraycopy(this.words, 0, result.words, 0, this.stacks);
        return result;
    }
    
    public AutoStackingNumber demote(int newStacks) {
        if (newStacks >= stacks) {
            return this;
        }
        if (newStacks < 1) {
            newStacks = 1;
        }
        
        // Check if demotion would lose data
        for (int i = newStacks; i < stacks; i++) {
            if (words[i] != 0) {
                throw new ArithmeticException("Cannot demote - would lose fractional data");
            }
        }
        
        AutoStackingNumber result = new AutoStackingNumber(newStacks);
        System.arraycopy(this.words, 0, result.words, 0, newStacks);
        return result;
    }
    
    public int getOptimalStacks() {
        for (int s = stacks; s > 1; s--) {
            if (words[s - 1] != 0) {
                return s;
            }
        }
        return 1;
    }
    
    public AutoStackingNumber pow(int exponent) {
        if (exponent == 0) return one(this.stacks);
        if (exponent < 0) throw new IllegalArgumentException("Negative exponent not supported");
        
        AutoStackingNumber result = one(this.stacks);
        AutoStackingNumber base = new AutoStackingNumber(this);
        
        for (int i = 0; i < exponent; i++) {
            result = result.multiply(base);
        }
        
        return result;
    }
    
    // ========== EQUALS AND HASHCODE ==========
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AutoStackingNumber other = (AutoStackingNumber) obj;
        if (this.stacks != other.stacks) return false;
        
        for (int i = 0; i < stacks; i++) {
            if (this.words[i] != other.words[i]) return false;
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        int hash = stacks;
        for (int i = 0; i < stacks; i++) {
            hash = 31 * hash + (int) (words[i] ^ (words[i] >>> 32));
        }
        return hash;
    }
              }
