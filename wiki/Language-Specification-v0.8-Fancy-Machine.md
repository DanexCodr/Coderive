# Coderive Language Specification

This page is the wiki-ready **literate specification** for the Coderive language.
Each section explains both **what a feature is** and **how it is used**.

## 1) Lexical Categories

### What It Is
The lexical layer defines how source text is tokenized before parsing:
- Keywords
- Identifiers
- Symbols/operators
- Literals (`int`, `float`, `text`, `bool`, `none`)
- Comments (line and block)
- Interpolation tokens

### How It Is Used
Write programs using these categories as the parser-facing surface:
- Keywords express control/type/runtime constructs.
- Symbols and delimiters shape expressions, calls, slots, and blocks.
- Literals and identifiers provide values and names in expressions.

## 2) Keywords

### What It Is
Reserved words with language-defined meaning:

`share`, `local`, `unit`, `use`, `is`, `this`, `super`, `if`, `else`, `elif`, `for`, `of`, `to`, `by`, `break`, `skip`, `exit`, `int`, `text`, `float`, `bool`, `type`, `policy`, `with`, `builtin`, `all`, `any`, `none`, `true`, `false`, `get`, `set`, `control`, `unsafe`, `i8`, `i16`, `i32`, `i64`, `u8`, `u16`, `u32`, `u64`, `f32`, `f64`.

### How It Is Used
Use keywords according to their domain:
- Scope/module declarations (`share`, `local`, `unit`, `use`)
- Flow control (`if`, `elif`, `else`, `for`, `break`, `skip`, `exit`)
- Type/runtime system (`type`, `policy`, `with`, `unsafe`, primitives)
- Boolean composition (`all`, `any`)

## 3) Symbols and Operators

### What It Is
Syntax operators and delimiters used to build declarations, expressions, and calls.

### How It Is Used

#### Assignment and returns
- `=`, `:=`, `::`, `::=`, `~>`

#### Arithmetic/comparison
- `+`, `-`, `*`, `/`, `%`
- `==`, `!=`, `>`, `<`, `>=`, `<=`

#### Range and stepping
- `..`, `#`, `to`, `by`

#### Grouping/access
- `(` `)` `[` `]` `{` `}` `.` `,` `:`

#### Other tokens
- `!`, `?`, `|`, `&`, `$`, `_`

#### Lambda/self-call syntax
- `\` (lambda declaration)
- `<~(...)` (self-call in method or lambda)
- `<~N(...)` (lambda ancestry call by numeric level)
- `<~CONST(...)` (lambda ancestry call by ALL_CAPS constant level)

## 4) Declarations and Type Rules

### What It Is
Rules that govern variable/constant creation and type expression.

### How It Is Used
- Typed declaration: `name: type` or `name: type = value`
- Inferred declaration: `name := value`
- Union types: `int|float`
- ALL_CAPS identifiers are constants:
  - must be initialized at declaration
  - are immutable after declaration
  - cannot be used as loop iterators

## 5) Functions, Slots, and Calls

### What It Is
Coderive functions use named return slots, and lambdas are first-class callable values.

### How It Is Used
- Define return contracts with `::`.
- Assign return values with `~>`.
- Extract named slots with `[slotA, slotB]:fn(...)`.
- Pass/store lambdas as values.
- Auto-currying behavior:
  - under-applied lambdas return closures
  - extra arguments can continue into returned lambdas

## 6) Self-Call and Lambda Ancestry (`<~`)

### What It Is
A recursion and ancestry-call operator family for methods and lambdas.

### Syntax
- `<~(...)` — self-call
- `<~0(...)` — current lambda (explicit)
- `<~1(...)` — parent lambda
- `<~2(...)` — grandparent lambda
- `<~CONST(...)` — lambda level from ALL_CAPS constant

### How It Is Used
- `<~(...)` is valid in methods and lambdas.
- `<~N(...)` and `<~CONST(...)` are lambda-only ancestry calls.
- Bare `<~` is invalid; parentheses are required.
- In lambda nesting, levels are:
  - `0` current lambda
  - `1` direct parent
  - `2` grandparent
  - etc.

### Example
```java
// Method self recursion
local factorial(n: int) :: result: int {
    if n < 2 { ~> result: 1 }
    ~> result: n * <~(n - 1)
}

// Lambda ancestry calls
result := 0
result = [result]:\(a: int) :: result: int {
    if a <= 0 { ~> result: 0 }

    b := a
    inner := 0
    inner = [result]:\(b: int) :: result: int {
        if b <= 0 { ~> result: 0 }
        same := <~0(b - 1)
        parent := <~1(b - 1)
        ~> result: b + same + parent
    }

    ~> result: inner
}
```

## 7) Control Flow

### What It Is
Branching and iteration constructs for deterministic flow control.

### How It Is Used
- Conditional branching: `if / elif / else`
- Looping: `for <id> of <source>`
  - range loops: `for i of 1 to 10 by 2`
  - formula shorthand: `for i of 1..32#*2`
- Loop/program control: `break`, `skip`, `exit`

## 8) Classes, Inheritance, and Policy

### What It Is
Object-oriented and contract features:
- inheritance (`is`)
- instance references (`this`, `super`)
- policy contracts (`policy`, `with`)

### How It Is Used
- Extend types with `is`.
- Access current and parent behavior with `this`/`super`.
- Declare required behavior with policies.
- Policy requirements propagate to descendants (viral policy behavior).

## 9) Arrays and Ranges

### What It Is
Sequence features with lazy/natural range support.

### How It Is Used
- Use ranges for large-scale sequences without full materialization.
- Use index/range slicing for subsequences.
- Use additive and formula-like step behavior with `by` and `#`.

## 10) Runtime Behavior Notes

### What It Is
Runtime semantics that affect correctness and performance.

### How It Is Used
- Self-call syntax requires parentheses (`<~(...)`).
- Self-call levels accept integer literals and ALL_CAPS constants.
- Tail-recursive self-calls in return-slot positions use optimized execution paths.
- Boolean composition is quantifier-first with `all[...]` and `any[...]`.

## 11) Scope

This specification is intended as a full-language reference for Coderive, not only a release-delta summary.
