# Coderive Language Specification (v0.8.0 — Fancy Machine)

This page is the wiki-ready language specification for Coderive v0.8.0.

## 1) Lexical Categories

- Keywords
- Identifiers
- Symbols
- Literals (`int`, `float`, `text`, `bool`, `none`)
- Comments (line and block)
- Interpolation tokens

## 2) Keywords

`share`, `local`, `unit`, `use`, `is`, `this`, `super`, `if`, `else`, `elif`, `for`, `of`, `to`, `by`, `break`, `skip`, `exit`, `int`, `text`, `float`, `bool`, `type`, `policy`, `with`, `builtin`, `all`, `any`, `none`, `true`, `false`, `get`, `set`, `control`, `unsafe`, `i8`, `i16`, `i32`, `i64`, `u8`, `u16`, `u32`, `u64`, `f32`, `f64`.

## 3) Symbols and Operators

### Assignment and returns
- `=`, `:=`, `::`, `::=`, `~>`

### Arithmetic/comparison
- `+`, `-`, `*`, `/`, `%`
- `==`, `!=`, `>`, `<`, `>=`, `<=`

### Range and stepping
- `..`, `#`, `to`, `by`

### Grouping/access
- `(` `)` `[` `]` `{` `}` `.` `,` `:`

### Other tokens
- `!`, `?`, `|`, `&`, `$`, `_`

### Lambda/self-call syntax
- `\` (lambda)
- `<~(...)`
- `<~N(...)`
- `<~CONST(...)` where `CONST` is an ALL_CAPS identifier resolved as an integer level at runtime.

## 4) Declarations and Type Rules

- Typed declaration: `name: type` or `name: type = value`
- Inferred declaration: `name := value`
- ALL_CAPS identifiers are constants and must be initialized.
- Loop iterators cannot be ALL_CAPS.
- Union types are supported (`int|float`).

## 5) Functions, Slots, and Calls

- Slot contracts use `::`.
- Return-slot assignment uses `~>`.
- Named slot extraction syntax: `[slotA, slotB]:fn(...)`.
- Lambdas are first-class.
- Under-applied lambdas can return closures (auto-currying behavior).

## 6) Control Flow

- `if / elif / else`
- `for <id> of <source>`
  - Range loops: `for i of 1 to 10 by 2`
  - Formula shorthand: `for i of 1..32#*2`
- `break`, `skip`, `exit`

## 7) Classes, Inheritance, Policy

- Inheritance via `is`
- Instance references: `this`, `super`
- Policies via `policy` and `with`
- Policy requirements propagate to descendants (viral policy behavior)

## 8) Arrays and Ranges

- Natural/lazy ranges are supported for large-scale sequences.
- Index slicing/range indexing is supported.
- Range steps support additive and formula-like progression.

## 9) Runtime Behavior Notes

- Self-call syntax requires parentheses; bare `<~` is invalid.
- Self-call levels support integer literals and ALL_CAPS constants.
- Tail-recursive self-calls in return-slot positions use optimized execution paths.
- Quantifier-first boolean composition uses `all[...]` and `any[...]`.

## 10) Version

This specification targets **Coderive v0.8.0 (Fancy Machine)**.
