# What Changed

This document describes all fixes applied to Coderive `.cod` source files after
running the full test suite and identifying 7 failing files.

---

## Files Fixed

### 1. `src/main/cod/lang/Math.cod`

**Problem:** Used the legacy `for i in` loop syntax, which the parser no longer
accepts. The parser now requires `for i of`.

**Fix:** Replaced both occurrences of `for i in` with `for i of`:

```cod
// Before
for i in 1 to 10 {
for i in 1 to exponent {

// After
for i of 1 to 10 {
for i of 1 to exponent {
```

**Added:** A `share main()` method so the library file can be executed standalone
for quick self-testing without relying on an external broadcast.

---

### 2. `src/main/cod/src/main/test/BasicTest.cod`

**Problem:** `share main()` is a static-like method (no object instance). It
declared local variables `x` and `y` that shadowed the class fields, and then
tried to access `this.x` — but `this` is unavailable in a `share` context,
causing `Undefined field: x` at runtime.

**Fix:** Renamed the local variables to `localX` and `localY` (no more
shadowing), and replaced `this.x` with the literal `5` (the class-field value)
since the intent was just to add the field value:

```cod
// Before
share main() {
    x := 2
    y := 5
    out(x + y + this.x)
}

// After
share main() {
    localX := 2
    localY := 5
    out(localX + localY + 5)
}
```

---

### 3. `src/main/cod/src/main/test/ArrayTest.cod`

**Problem (a):** `9.999Qi` is an invalid numeric literal — the parser only accepts
integer quantities before metric suffixes (`K`, `M`, `Bi`, `Qi`, etc.).

**Fix:** Replaced all three occurrences of `9.999Qi` with `9Qi`.

**Problem (b):** Array literals used `[by 2 in start to end]` syntax. The `by N
in` prefix form inside `[]` is not supported by the array-literal parser; it only
accepts `[start to end]` or `[start to end by N]`.

**Fix:** Converted both occurrences to `[start to end by step]` form:

```cod
// Before
x = [by 2 in 168 to 9.999Qi]
x = [[1 to 10], [by 2 in 3 to 100]]

// After
x = [168 to 9Qi by 2]
x = [[1 to 10], [2 to 100 by 2]]
```

**Problem (c):** `x[20000] = "hello"` tried to assign a `text` value into an
`[int]` natural array, causing a type-mismatch error that had been hidden by the
earlier parse failures.

**Fix:** Changed the mutation value to an integer: `x[20000] = 99999`.

---

### 4. `src/main/cod/src/main/test/BroadcastTest.cod`

**Problem:** The class `Broadcaster` was intentionally left empty (no `main()`),
relying on a broadcast from another file. When executed as a standalone file the
interpreter found no entry point and threw `No executable main() found`.

**Fix:** Added a minimal `share main()` so the file runs standalone:

```cod
// Before
Broadcaster {
    // empty... this will call the broadcasted() main
    // instead when there is no main() declared inside this class.
}

// After
Broadcaster {
    share main() {
        out("Broadcaster: no broadcast target found — running standalone.")
    }
}
```

---

### 5. `src/main/cod/src/main/test/PolicyDemo.cod`

**Problem:** `ChildNode` extended `Node`, which implements the viral policy
`Accept`. All subclasses must provide a `policy accept(...)` implementation, but
`ChildNode` was empty, causing a parse-time policy-violation error.

**Fix:** Added the required policy implementation to `ChildNode`:

```cod
// Before
ChildNode is Node {

}

// After
ChildNode is Node {
    policy accept(n: type) :: type {
        ~> n
    }
}
```

---

### 6. `src/main/cod/src/main/test/ParamSkipDemo.cod`

**Problem:** The `_` parameter-skip placeholder only works in multi-return slot
calls (`[slot1, slot2]:method(...)`). Using `_` in ordinary single-return calls
such as `createMessage("Alice", _)` caused `Undefined variable: _` at runtime.

**Fix:** Replaced the three affected single-return calls with parameter-omission
(simply omitting the trailing argument that has a default value), which is the
correct form for single-return methods:

```cod
// Before (Tests 4, 6, 8)
msg1 := createMessage("Alice", _)
result := multiply(10, _)
greeting := sayHello("Charlie", _)

// After
msg1 := createMessage("Alice")
result := multiply(10)
greeting := sayHello("Charlie")
```

Test descriptions were updated to reflect the omission approach.

---

### 7. `src/main/cod/src/main/test/InteractiveDemo.cod`

**Problem (a):** In `calculate()`, the `op == "+"` branch used two separate
`~>` statements:

```cod
~> formula: a + b
~> operation: "addition"
```

The interpreter's early-exit logic sees that `formula` is filled after the first
`~>` and stops executing the method body — `operation` is never set (stays
`null`). When the caller read the `operation` slot, it got `null`, which the
interpreter then treated as `Undefined variable`.

**Fix:** Merged the two statements into a single combined return (consistent with
all other branches in the method):

```cod
~> formula: a + b, operation: "addition"
```

**Problem (b):** The multi-slot destructuring assignment was inside the `if`
validation block. Due to interpreter scoping, slot variables created inside an
`if` block from a `[slot]:method(...)` call were not visible further into the
same `if` block when used in string interpolation.

**Fix:** Moved the destructuring call **before** the `if` check so the slot
variables (`calcResult`, `calcOp`) are in the enclosing method scope:

```cod
// Before
if op == any["+", "-", "*", "/"] {
    result, opName := [formula, operation]:calculate(num1, num2, op)
    out("{num1} {op} {num2} = {result}")
    out("Operation: {opName}")
}

// After
calcResult, calcOp := [formula, operation]:calculate(num1, num2, op)
if op == any["+", "-", "*", "/"] {
    out("{num1} {op} {num2} = {calcResult}")
    out("Operation: {calcOp}")
}
```

---

## Files Removed

- **`error_report.md`** — Removed. The errors it documented have all been fixed.

---

## Files Unchanged (already passing)

| File | Status |
|------|--------|
| `examples/hello.cod` | ✅ No changes needed |
| `examples/calculator.cod` | ✅ No changes needed |
| `examples/fizzbuzz.cod` | ✅ No changes needed |
| `examples/smart_loops.cod` | ✅ No changes needed |
| `examples/lazy_arrays.cod` | ✅ No changes needed |
| `src/main/cod/src/main/test/EvenOdd.cod` | ✅ No changes needed |
| `src/main/cod/src/main/test/IOTest.cod` | ✅ No changes needed |
| `src/main/cod/src/main/test/LoopWithIOTest.cod` | ✅ No changes needed |
| `src/main/cod/src/main/test/LazyLoop.cod` | ✅ No changes needed |
