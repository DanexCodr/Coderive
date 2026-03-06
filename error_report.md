# Coderive `.cod` Error Report

**Generated:** 2026-03-06  
**Runner:** `java -cp docs/assets/Coderive.jar cod.runner.CommandRunner`  
**Total files scanned:** 16  
**Files with errors:** 7  
**Files passing:** 9

---

## Summary

| File | Status | Error Type |
|------|--------|------------|
| `examples/hello.cod` | ✅ PASS | — |
| `examples/calculator.cod` | ✅ PASS | — |
| `examples/fizzbuzz.cod` | ✅ PASS | — |
| `examples/smart_loops.cod` | ✅ PASS | — |
| `examples/lazy_arrays.cod` | ✅ PASS | — |
| `src/main/cod/src/main/test/EvenOdd.cod` | ✅ PASS | — |
| `src/main/cod/src/main/test/IOTest.cod` | ✅ PASS | — |
| `src/main/cod/src/main/test/LoopWithIOTest.cod` | ✅ PASS | — |
| `src/main/cod/src/main/test/LazyLoop.cod` | ✅ PASS | — |
| `src/main/cod/src/main/test/BasicTest.cod` | ❌ ERROR | RuntimeError |
| `src/main/cod/src/main/test/ArrayTest.cod` | ❌ ERROR | ParseError |
| `src/main/cod/src/main/test/BroadcastTest.cod` | ❌ ERROR | RuntimeError |
| `src/main/cod/src/main/test/PolicyDemo.cod` | ❌ ERROR | ParseError |
| `src/main/cod/src/main/test/ParamSkipDemo.cod` | ❌ ERROR | RuntimeError |
| `src/main/cod/lang/Math.cod` | ❌ ERROR | ParseError |
| `src/main/cod/src/main/test/InteractiveDemo.cod` | ❌ ERROR | RuntimeError |

---

## Error Details

---

### 1. `src/main/cod/src/main/test/BasicTest.cod`

**Type:** `cod.error.ProgramError` (Runtime Error)  
**Message:** `Undefined field: x`

**Description:**  
The class `Test1` declares an instance field `x: int = 5`, then inside `main()` it reassigns `x := 2` (creating a local variable that shadows the field) and tries to access `this.x`. The interpreter cannot resolve the `this.x` field reference, throwing "Undefined field: x".

**Relevant code (BasicTest.cod):**
```
Test1 {
  x: int = 5
  y: int = 7
  share main() {
    x := 2
    y := 5
    out(x + y + this.x)   ← this.x cannot be resolved
  }
}
```

**Stack trace:**
```
cod.error.ProgramError: Undefined field: x
    at cod.interpreter.InterpreterVisitor.handleThisPropertyAccess(InterpreterVisitor.java:880)
    at cod.interpreter.InterpreterVisitor.visit(InterpreterVisitor.java:787)
    at cod.ast.nodes.PropertyAccessNode.accept(PropertyAccessNode.java:21)
    ...
    at cod.runner.CommandRunner.main(CommandRunner.java:111)
```

---

### 2. `src/main/cod/src/main/test/ArrayTest.cod`

**Type:** `cod.error.ParseError` (Parse Error)  
**Message:** `Invalid numeric literal: 9.999Qi at 39:30-36`

**Description:**  
The file uses the numeric literal `9.999Qi` (9.999 Quintillion). The parser does not support a decimal fractional part before the `Qi` suffix — it only accepts integer suffixes like `1Qi`, `100K`, `1M`, etc. The literal `9.999Qi` is therefore invalid.

**Relevant code (ArrayTest.cod, line 39):**
```
out("Big number: " + 9.999Qi)   ← 9.999Qi is not a valid numeric literal
```

**Stack trace:**
```
cod.error.ParseError: Invalid numeric literal: 9.999Qi at 39:30-36
    at cod.parser.BaseParser.error(BaseParser.java:222)
    at cod.parser.ExpressionParser$4.parse(ExpressionParser.java:621)
    ...
    at cod.runner.CommandRunner.main(CommandRunner.java:111)
```

---

### 3. `src/main/cod/src/main/test/BroadcastTest.cod`

**Type:** `cod.error.ProgramError` (Runtime Error)  
**Message:** `No executable main() found in package 'test'`

**Description:**  
`BroadcastTest.cod` intentionally declares an empty class `Broadcaster` with no `main()` method, relying on a broadcast from another file. When run as a standalone file, no `main()` entry point is found. This is by design for multi-file use, but fails as a standalone execution.

**Relevant code (BroadcastTest.cod):**
```
unit test

Broadcaster {
  // empty... this will call the broadcasted() main
  // instead when there is no main() declared inside this class.
}
```

**Stack trace:**
```
cod.error.ProgramError: No executable main() found in package 'test'
...
This file has no main() method and no local broadcast.
    at cod.interpreter.Interpreter.runModule(Interpreter.java:623)
    ...
    at cod.runner.CommandRunner.main(CommandRunner.java:111)
```

---

### 4. `src/main/cod/src/main/test/PolicyDemo.cod`

**Type:** `cod.error.ParseError` (Parse Error)  
**Message:** `Class 'ChildNode' inherits from 'Node'. The ancestor Node (implements policy 'Accept') requires policy method 'accept'. Add: policy accept(...) { ... } inside the class at 13:14-17`

**Description:**  
`ChildNode` extends `Node`, which implements the `Accept` policy. The policy declares an `accept` method signature that viral policies require all subclasses to also implement. `ChildNode` is empty and does not provide the `accept` implementation, causing a parse-time policy violation.

**Relevant code (PolicyDemo.cod):**
```
policy Accept {
    accept(n: type) :: type
}

Node with Accept {
    policy accept(n: type) :: type {
        // Implementation here
    }
}

ChildNode is Node {
    // ← missing: policy accept(...) { ... }
}
```

**Stack trace:**
```
cod.error.ParseError: Class 'ChildNode' inherits from 'Node'
The ancestor Node (implements policy 'Accept') requires policy method 'accept'
Add: policy accept(...) { ... } inside the class at 13:14-17
    at cod.parser.BaseParser.error(BaseParser.java:222)
    at cod.parser.DeclarationParser.validateClassViralPolicies(DeclarationParser.java:498)
    ...
    at cod.runner.CommandRunner.main(CommandRunner.java:111)
```

---

### 5. `src/main/cod/src/main/test/ParamSkipDemo.cod`

**Type:** `cod.error.ProgramError` (Runtime Error)  
**Message:** `Undefined variable: _`

**Description:**  
The `_` wildcard (parameter-skip placeholder) is used in method calls like `createMessage("Alice", _)` to skip optional parameters. The interpreter resolves `_` in some call patterns (multi-return slot calls such as `[sum, product]:calculate(...)`) but fails when `_` appears in a regular single-return call (e.g., `createMessage("Alice", _)`), treating it as an undefined variable.

**Relevant code (ParamSkipDemo.cod, line 67):**
```
msg1 := createMessage("Alice", _)   ← _ not resolved in single-return call
```

**Stack trace:**
```
cod.error.ProgramError: Undefined variable: _
    at cod.interpreter.InterpreterVisitor.visit(InterpreterVisitor.java:658)
    at cod.ast.nodes.IdentifierNode.accept(IdentifierNode.java:14)
    ...
    at cod.runner.CommandRunner.main(CommandRunner.java:111)
```

---

### 6. `src/main/cod/lang/Math.cod`

**Type:** `cod.error.ParseError` (Parse Error)  
**Message:** `Expected keyword 'of', got id ('in') at line 13:19`

**Description:**  
The `Math.cod` library uses the old `for i in 1 to 10` loop syntax. The Coderive language has since changed the loop keyword from `in` to `of` (i.e. `for i of 1 to 10`). The parser now requires `of` and rejects the legacy `in` keyword.

**Relevant code (Math.cod, line 13):**
```
for i in 1 to 10 {   ← should be: for i of 1 to 10 {
    guess = (guess + x / guess) / 2
}
```

**Stack trace:**
```
cod.error.ParseError: Expected keyword 'of', got id ('in') at line 13:19
    at cod.parser.context.ParserContext.expect(ParserContext.java:84)
    at cod.parser.BaseParser.expect(BaseParser.java:66)
    at cod.parser.StatementParser.parseForStmt(StatementParser.java:461)
    ...
    at cod.runner.CommandRunner.main(CommandRunner.java:111)
```

---

### 7. `src/main/cod/src/main/test/InteractiveDemo.cod`

**Type:** `cod.error.ProgramError` (Runtime Error)  
**Message:** `Undefined variable: operation`

**Description:**  
The `interactiveCalculator()` method performs a multi-return slot call: `result, operation := [formula, operation]:calculate(num1, num2, op)`. However, the slot name `operation` conflicts with an already-declared local variable `operation` from an earlier destructuring, causing the interpreter to fail to resolve it in the enclosing scope when it's used in the `out("Operation: {operation}")` interpolation call.

**Relevant code (InteractiveDemo.cod, lines 111–113):**
```
result, operation := [formula, operation]:calculate(num1, num2, op)
...
out("Operation: {operation}")   ← 'operation' is undefined in this scope
```

**Stack trace:**
```
cod.error.ProgramError: Undefined variable: operation
    at cod.interpreter.InterpreterVisitor.visit(InterpreterVisitor.java:658)
    at cod.ast.nodes.IdentifierNode.accept(IdentifierNode.java:14)
    ...
    at cod.interpreter.Interpreter.runMethodScript(Interpreter.java:831)
    at cod.runner.CommandRunner.main(CommandRunner.java:111)
```

---

## How to Fix

| File | Fix |
|------|-----|
| `BasicTest.cod` | Ensure the instance field `x` is stored on the object so `this.x` can resolve it, or rename the local variable to avoid shadowing. |
| `ArrayTest.cod` | Replace `9.999Qi` with a valid literal such as `9999Ti` (Trillion) or `9999999999999999` (plain integer). |
| `BroadcastTest.cod` | Add a `share main()` to `Broadcaster`, or run alongside the file that broadcasts a main. |
| `PolicyDemo.cod` | Add `policy accept(n: type) :: type { ... }` inside `ChildNode`. |
| `ParamSkipDemo.cod` | Use `_` only in multi-return slot calls (`[slot]:method(...)`), or update the interpreter to support `_` in all call sites. |
| `Math.cod` | Replace all `for ... in ...` with `for ... of ...` to match the current syntax. |
| `InteractiveDemo.cod` | Rename the destructured slot variable `operation` to avoid conflicts, e.g. `result, opName := [formula, operation]:calculate(...)`. |
