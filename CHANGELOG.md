# Changelog

All notable changes to Coderive are documented in this file.

## [v0.9.0] - Platform Snapshot - April 13, 2026

### 🔀 Merge Coverage for This Snapshot
- **Linear recurrence refactor/composition and v0.8.4 version alignment** — recurrence formula refactor/composition updates and version alignment work.
- **Rolling/vector recurrence execution improvements** — rolling/vector linear recurrence execution upgrades and related parity-path updates.
- **`.codc` container write/read hardening** — custom zip-container behavior and container stability improvements.
- **Shared index consolidation in project container root** — import index consolidation into project container root entry (`HOOK.toml`).
- **Unit-path structure fixes and deterministic lowering reset** — `.codb`/`.codc` path structure fixes plus deterministic PTAC lowering counter reset behavior.
- **`CommandRunner` full compile mode and summary polish** — `-f`/`--full` support and compile summary output improvements.
- **Borrow-check enforcement for unsafe indexed mutation** — borrow safety checks extended into unsafe indexed mutation paths.
- **Active-borrow tracking performance upgrades** — zero-cost/O(1) active-borrow tracking improvements.
- **Unsafe operation boundary model implementation** — `unsafe` class/method modifier support with `safe(...)` wrapping for calling unsafe operations from safe contexts.

### 🚨 Breaking / Behavioral Changes
- **Unsafe declaration ordering enforced** — Unsafe declarations require explicit visibility before `unsafe` (`share unsafe ...` or `local unsafe ...`).  
  Migration example: `unsafe share MyType { ... }` → `share unsafe MyType { ... }`.
- **Unsafe operation boundary model enforced** — `unsafe` is supported as a class/method modifier, and unsafe constructors/methods must be wrapped with `safe(...)` when called from safe contexts; `safe(...)` is not allowed inside unsafe contexts.
- **Borrow checks tightened in unsafe mutation paths** — Unsafe indexed mutation now participates in borrow-safety enforcement.

### ✨ Major Features
- **Unsafe pointer model added** — Unsafe contexts support pointer-oriented syntax/runtime behavior (`*T`, `T[n]`, `&`, `*`, and pointer arithmetic with bounds checks).
- **Full-project compile mode in CLI** — `CommandRunner` supports `-f` / `--full` to compile all `.cod` files under `src/main`.
- **O(1) borrow checking via active-borrow tracking** — Borrow-state checking moved to O(1)-style active tracking for lower runtime overhead.

### 🔧 Runtime / PTAC / IR Improvements
- **Deterministic lowering register naming** — PTAC lowering counters reset per `lower()` call to keep generated naming stable per artifact.
- **IR path normalization for dotted units** — Dotted unit names are normalized into slash paths for both `.codc` entries and standalone `.codb` fallback lookup.
- **Project-level index container** — Shared index data is stored in `src/bin/project.codc` at root entry `HOOK.toml`.
- **Container format and write hardening** — `.codc` container behavior was tightened with stronger write/cleanup semantics and stable zip-container handling.
- **Standalone `.codb` support retained** — Standalone `.codb` artifacts remain a permanent supported format alongside `.codc` grouping.

### ⚡ Lazy Array & Formula Execution
- **Conditional formula composition in `NaturalArray`** — Conditional formulas compose into a single merged formula path.
- **Linear recurrence composition and rolling/vector execution upgrades** — Recurrence paths were refactored for better composition, lower memory pressure, and improved recurrence execution behavior.
- **Lazy map/filter chaining composition** — LiteralRegistry composes chained lazy map/filter views to reduce wrapper layering.

## [v0.8.4] - Formula Lock-In - April 12, 2026

### ✨ Major Updates
- **LinearRecurrenceFormula moved to constant-space evaluation** — Removed range-sized recurrence precomputation tables so recurrence evaluation no longer allocates memory proportional to covered range size.
- **Linear recurrence composition in NaturalArray** — `addLinearRecurrenceFormula(...)` now composes newly added formulas with existing recurrence formulas (newer-over-older), mirroring the optimized conditional formula path.
- **Recurrence lookup cache cleanup** — Removed per-index recurrence-formula index caching to avoid linear growth of recurrence metadata with random index access.

## [v0.8.0] - Fancy Machine - April 07, 2026

### ✨ Major Updates
- **Version bump to v0.8.0** — Updated repository and documentation versioning for the Fancy Machine release.
- **Comprehensive language specification** — Added detailed documentation coverage for keywords, symbols/operators, syntax forms, and runtime behavior.
- **Documentation page upgrades** — Added a specification-focused section and integrated client-side documentation search.
- **Custom spec branding asset** — Added a custom spec logo generated via Python for the Documentation page.
- **Wiki/discussion publication prep** — Added wiki-ready specification markdown and discussion announcement draft files for direct publishing.

## [v0.7.4] - Be Structured - April 01, 2026

### ✨ Major Updates Since v0.7.0
- **Browser runtime migration** — Replaced the CheerpJ-based playground path with a pure JavaScript Coderive runtime for faster startup and no Java boot delay in the web playground.
- **Playground loading UX improvements** — Added a clearer multi-step loading flow and earlier runtime preloading to improve perceived responsiveness.
- **Repository automation updates** — Added workflow support for source jar refresh and branch-cleanup automation updates.

### 🔧 Stability & Runtime Fixes
- **Text literal handling fixes** — Corrected text literal extraction paths and cleaned related parser debug noise.
- **Decimal output formatting improvements** — Improved numeric formatting consistency for decimal output.
- **Param skip and test/runtime polish** — Follow-up fixes for parameter skip behavior and associated runtime/testing paths.

### 🧪 Validation Notes
- Ran existing Gradle test task (`./gradlew test`) successfully.
- Ran unit `.cod` files under `src/main/cod/src/main/test/` with `CommandRunner`; current repo still has pre-existing issues in some interactive/import/broadcast test files (for example: `Broadcast.cod`, `Import.cod`, `Interactive.cod`, `Parity.cod`) that are not introduced by this release-note update.

## [v0.7.0] - Be Structured - March 06, 2026

### 🚨 Breaking Changes
- **`for...in` replaced with `for...of`** — Loop iteration keyword changed from `in` to `of` for clarity and consistency (`for i of 1 to 5`). All existing loop code must be updated.
- **Loop step clause reordered** — The `by` clause now follows the range specification: `for i of start to end by steps` (previously `for i by steps in start to end`).
- **`in()` prompt argument added** — Input method now accepts an optional type and prompt string directly: `in(text, "Enter name: ")` replaces the previous `outs("Enter name: ")` + `in()` pattern.
- **Token class redesigned** — Multiple overloaded constructors replaced with static factory methods; a `keyword` field is now embedded directly on the `Token` object instead of being derived at parse time.
- **`cod.compiler` package removed** — The entire native compiler pipeline (TAC, MTOT) has been deleted. Coderive is now an interpreter-only language runtime.

### ✨ Major Features
- **Output-Aware Loop Optimization** — New `OutputAwarePattern` detects loops containing I/O statements and applies dedicated optimizations, significantly improving performance for mixed-output loops. Introduced `OutputAwarePattern` and `SequencePattern` as the primary range execution strategies.
- **LiteralRegistry** — Introduced an extension system that enables property access and method calls directly on literal values (ranges, numbers, strings, arrays). Provides a clean, pluggable API for built-in literal behaviors such as `.size`, `.contains()`, and more.
- **PolicyResolver with Caching** — Dedicated resolver for enforcing Policy type virality. Uses per-policy caches for O(1) method-requirement lookups, composition chains, and per-class validation results, replacing ad-hoc policy checks scattered across the interpreter.
- **Web Documentation & Playground Site** — Added a complete `docs/` single-page application with a home page, documentation browser, and an in-browser interactive Coderive playground.
- **SlotParser** — Extracted return-slot (`::`) parsing from `MainParser` into a dedicated `SlotParser` class for improved separation of concerns and maintainability.
- **Inline Range Formula Shorthand** — New concise syntax for range formulas: `1..32#*2` (equivalent to a multiplicative range from 1 to 32 doubling each step) alongside the existing `for i of 1 to 32 by *2` form.

### 🔧 Engine & Architecture Improvements
- **Lexer Modularization** — Decomposed the monolithic `MainLexer` into six focused sub-lexers: `CommentLexer`, `IdentifierLexer`, `NumberLexer`, `StringLexer`, `SymbolLexer`, and `WhitespaceLexer`, each responsible for a single token category.
- **Interpreter Handler Unification** — All handler classes (`AssignmentHandler`, `ExpressionHandler`, `TypeHandler`, `IOHandler`) consolidated under the `cod.interpreter.handler` package. `TypeSystem` and `TypeValue` merged into `TypeHandler`, eliminating a separate `cod.interpreter.type` package.
- **Keyword Lookup Optimization** — Added `Keyword.fromString()` backed by a pre-built `HashMap` for O(1) keyword resolution, replacing linear enum scans.
- **Slot Access Optimization in `ExecutionContext`** — Return slot storage now uses parallel arrays (`slotNamesList`, `slotValuesList`, `slotTypesList`) with an index map for O(1) slot reads and writes.
- **New Utility Classes** — `ObjectChecker` for reusable null/type validation and `TokenSkipper` for efficient token-stream navigation, reducing boilerplate throughout the parser and interpreter.
- **`InternalError`** — New dedicated error class for interpreter-internal assertion failures, distinct from user-facing `ProgramError`.

### 📦 Range System Refactoring
- **`SequenceFormula`** replaces both `MultiBranchFormula` and `LoopFormula` as the primary formula type for sequential range evaluation, reducing formula class count and improving composability.
- **`OutputAwarePattern` and `SequencePattern`** added as new execution-aware range patterns. `AssignmentPattern` removed.
- **`NaturalArray` overhauled** — Significant internal rewrite for correctness, better interaction with the new pattern/formula architecture, and improved lazy evaluation behaviour.

### 🗑️ Removed Components
- **Compiler Pipeline** — The entire `cod.compiler` package deleted: `TACCompiler`, `MTOTNativeCompiler`, `GraphColoringAllocator`, `LivenessAnalyzer`, `RegisterManager`, `BasicBlock`, `TACInstruction`, `TACProgram`, and `MTOTRegistry`.
- **C Runtime** — `src/main/c/runtime.c` removed; native compilation is no longer a supported target.
- **Legacy Runner Classes** — `CompilerRunner`, `CompilerTestRunner`, and `InterpreterRunner` fully removed after their replacements (`TestRunner`, `CommandRunner`) were stabilized in v0.6.0.
- **`cod.parser.program` Package** — `ProgramType` and `ProgramTypeScanner` removed; their functionality has been absorbed into other components.
- **`TokenValidator`** — Dedicated semantic token-validation class removed after its responsibilities were redistributed.

### 🌐 Documentation & Demo Updates
- **Coderive Docs Website** — New `docs/` directory containing a full SPA: router, home page, documentation viewer, interactive playground, and shared CSS/JS modules.
- **`PlaygroundServer.java` Updated** — Improved request handling and API compatibility for the hosted playground.
- **Demo Files Updated** — `InteractiveDemo.cod` and `LazyLoop.cod` revised to reflect new `for...of` loop syntax and updated `in()` prompt API.
- **New Test File** — Added `LoopWithIOTest.cod` to exercise and validate the output-aware loop optimization path.

## [v0.6.0] - Powered Up - January 22, 2026

### 🚨 Breaking Changes
- **Replaced out/in methods** - `outln()` → `out()` (auto-newline per arg), `out()` → `outs()` (auto-spacing between args).
- **Removed ambiguous method chain syntax** - Eliminated `method(any[ args...])` and `method(all[args...])` syntax as chaining methods, keeping only `any[]`/`all[]` array forms for clarity and consistency.
- **Removed Builtin I/O methods** - `out()`, `outln()`, and `in()` removed from BuiltinRegistry, now only accessible globally via GlobalRegistry.
- **Project structure standardization** - Moved to `src/main/` project structure for cleaner organization and standardized imports.

### ✨ Major Features
- **`this` and `super` Keywords** - Fully integrated as object pointers for class instance referencing and parent class access.
- **Policy Type with Virality** - Introduced Policy type where implementation forces all children (even far descendants) to implement the same Policy.
- **Enhanced I/O Methods** - 
  - `out()`: Supports consecutive multi-argument output with auto-newline after each argument
  - `outs()`: Successive output with automatic spacing between arguments
- **SourceSpan Integration** - New SourceSpan class for improved line/column handling in error messages and incremental parsing.

### 🔧 Engine & Parser Improvements
- **Parser Refactoring** - Replaced PositionHolder with proper ParserContext, added ParserState and ParserResult for better maintainability and performance.
- **Expect Pattern** - Introduced `expect()` pattern for consistent token consumption, fixing "forgot to consume" issues.
- **InterpreterVisitor Modularization** - Refactored into separate helper classes for cleaner architecture.
- **Program Type Scanning** - Separated ProgramTypeScanner from MainParser for dedicated program type analysis.
- **Multiline String Fix** - Fixed baseline handling to use aligned opening/closing delimiters and auto-strip leading whitespace.

### 📦 Architecture & Organization
- **Package Restructuring** - 
  - ParseResult, ParserContext, ParserState → `cod.parser.context`
  - ProgramType and ProgramTypeScanner → `cod.parser.program`
- **Class Renaming** - 
  - `InterpreterRunner` → `TestRunner`
  - `CompilerRunner` → `CompilerTestRunner`
- **Token Validation** - Introduced TokenValidator for dedicated enum validation.
- **Demo File Reorganization** - Moved demo files to follow `src/main/` structure.
- **Fixed Main Broadcasting** - Fully repaired broken `main()` broadcasting feature.

### 🗑️ Cleanup & Removal
- **Removed Unused Components** - Eliminated unused `Sys.cod` file.
- **Standardized Project Layout** - Adopted conventional `src/main/` directory structure.

## [v0.5.0] - Heavy Clean Changes - January 5, 2026

### 🚨 Breaking Changes
- **Heavy package restructuring** - Multiple classes moved to new packages for better organization.
- **Renamed core classes** - `ASTVisitor` → `VisitorImpl`, `BaseASTVisitor` → `ASTVisitor`, `TypedValue` → `TypeValue`, `CoderiveREPL` → `REPLRunner`.
- **Removed optimization components** - Eliminated `ConstantFolder`, `Deadcode eliminator`, and associated helper methods.

### ✨ Major Features
- **Enhanced Formula Loops** - Added support for combinations of patterns at the top level.
- **Multi-line String Support** - Introduced `|"` and `"|` delimiter pair for multi-line strings with baseline-aware content alignment (different from traditional `"""` triple quotes).
- **Package Private Classes** - Added support for package-private class visibility.
- **Package Broadcasted main()** - Added main method broadcasting at package level with call restrictions.
- **Native Range Indexing** - Added native range index feature for arrays.
- **Builtin Timer Method** - Added `timer()` method accessible both as builtin and globally.

### 🔧 Engine & Parser Improvements
- **Enhanced MainLexer** - Made more modular and maintainable.
- **Project Restructuring** - Heavy reformatting and repackaging of entire codebase:
  - Moved `ExecutionContext` and `ObjectInstance` to `interpreter.context`
  - Moved `BreakLoopException`, `EarlyExitException`, `SkipIterationException` to `interpreter.exception`
  - Moved `IOHandler` to `interpreter.io`
  - Moved `BuiltinRegistry` and `GlobalRegistry` to `interpreter.registry`
  - Moved `TypeSystem` and `TypeValue` to `interpreter.type`
  - Moved `NaturalArray` to dedicated `range` package
  - Moved formula classes to `range.formula`
  - Moved pattern classes to `range.pattern`
- **Performance Improvements** - Fixed subtle bugs to enhance overall performance.

### 📦 Architecture & Organization
- **Cleaner Codebase Structure** - Logical separation of concerns with dedicated packages for exceptions, IO, registries, types, and range handling.
- **Improved Maintainability** - Modular lexer and better organized class hierarchy.
- **Dedicated Range Package** - Centralized range and pattern handling functionality.

## [v0.4.0] - Infinite Possibilities - December 21, 2025

### 🚨 Breaking Changes

- **Removed `input` and `output` keywords** - Replaced with method calls `Sys.in(...)` and `Sys.out(...)`.

### ✨ Major Features

- **Class Inheritance** - Added class inheritance with the `is` keyword.
- **Class Constructors** - Added class constructors using `this` as a special method name.
- **Class Calling** - Added support for direct class calling.
- **Flexible Control Flows** - Enhanced control flow structures to be more flexible while maintaining safety.
- **Enhanced Loop Safety** - Added checks to prevent loop steps of 0 and ensure loops do not reverse from their stated start and end direction.
- **External Array Iteration** - Added support for iterating over external arrays within loops.
- **Builtin Method Syntax** - Removed the requirement for `{}` bodies for builtin methods.
- **Dedicated Registry Classes** - Added separate Registry classes for Builtin and Global methods.
- **Global I/O Methods** - Added `in()` and `out()` as both Builtin and Global methods for accessibility.

### 🔧 Engine & Parser Improvements

- **Fixed BinaryOpNode Bug** - Resolved a deeply hidden bug in the BinaryOpNode.
- **Optimized Array Iteration** - Added formula-based loops for O(1) iteration over natural arrays.
- **Proper Code Instantiation** - Fixed issues related to code instantiation.

---

## [v0.3.0] - The Design Leap - December 15, 2025

### 🚨 Breaking Changes
- **`::` Return Slot Operator** — Replaced the previous `~|` prefix-style return syntax with the `::` suffix-style return slot operator for a more consistent, readable design.
- **TAC IR Compiler** — Replaced the stack-based multi-architecture compiler with a Three-Address Code (TAC) intermediate representation, reducing systemic code generation bugs.
- **Full Class & Package Refactoring** — Almost all classes renamed, updated, and repackaged for a cleaner and more navigable codebase structure.
- **Removed ANTLR Dependency** — Parser and lexer now fully hand-written to reduce external dependencies and improve implementation control.
- **New Variable Declaration Syntax** — Replaced `<type> <name>` with `<name>: <type>` style declarations:
  - `name: type` — typed declaration
  - `name: type = value` — explicit declaration with assignment
  - `name := value` — type-inferred declaration with assignment
  - `name = value` — plain reassignment

### ✨ Major Features
- **Three World System** — Established three distinct program types for Coderive: Script (top-level imperative code), Method-only (function library), and Module (reusable component).
- **`text` Type** — Replaced the previous `string` keyword with `text` as the built-in string type.
- **Natural Arrays** — Introduced lazy range-based arrays:
  - Range support (`[0 to N]`) with lazy generation
  - Immutable by default; opt-in mutability available
  - `text` values supported as iterable natural arrays
- **Numeric Shorthands** — Added common suffixes for large numbers: `K` (thousand), `M` (million), `B` (billion), `T` (trillion), `Q` (quadrillion), `Qi` (quintillion), `e` (scientific notation). Case-sensitive.
- **Parameter Skipping & Named Arguments** — Added support for skipping positional parameters and passing arguments by name for improved call-site readability.
- **`share` Export Keyword** — Introduced `share` as the visibility modifier for publicly accessible declarations, replacing the earlier `ship` keyword.

### 📚 Documentation
- Updated all demo files to reflect the new variable declaration syntax, quantifier syntax, and export keyword.
- Added `ParamSkipDemo.cod` demonstrating named argument and parameter-skipping behaviour.

---

## [v0.2.3] - The Great Logic Revolution - November 23, 2025

### 🚨 Breaking Changes
- **Completely abandoned `&&` and `||` operators** - Embracing quantifier-first design

### ✨ Major Features
- **Quantifier-First Logic System**
  - Replaced traditional boolean operators with expressive quantifiers
  - `any[]` and `all[]` syntax for clear, intentional logic
  - Natural language syntax that lowers learning curve
  - Eliminates common operator precedence mistakes

### 🔧 Syntax Evolution

```kotlin
// Clean, consistent bracket-based syntax
if any[isReady, hasBackup, forceMode] 
if all[scores >= 60, !isFailed, attempts < 3]
user == any[admin, moderator, owner]
```

### 🛠 Parser Refinements
- Updated conditional chain parsing to use `LBRACKET`/`RBRACKET` consistently
- Enhanced error messages for mixed logical styles
- Improved support for array-based logical operations

### 📚 Documentation
- Updated `InteractiveDemo.cod` showcasing clarity of `any[]`/`all[]` syntax

---

## [v0.2.0] - Conditional Chain Revolution Begins - November 23, 2025

### 🌟 Groundbreaking Features
- **Conditional Chain Syntax**
  - Method call style: `user.hasPermission(any: "read", "write", "execute")`
  - Equality style: `status == any:("active", "pending", "verified")`
  - Inner negation support: `user.checkStatus(all: "active", !"banned")`
  - Short-circuit evaluation for optimal performance

### 🏗 Parser Enhancements
- Added `BANG` token for logical negation (`!`)
- Extended comparison expression parsing for equality chains
- Support for nested conditional chains
- Parentheses handling (optional for methods, required for equality)

### 🌳 AST Extensions
- Added `EqualityChainNode` for equality-style conditional chains
- Enhanced `MethodCallNode` with chain type and arguments
- Updated AST factory with chain creation methods

### ⚡ Interpreter Support
- Conditional chain evaluation in `ExpressionEvaluator`
- Truthiness detection for various value types
- Recursive chain expansion with proper short-circuiting

### 🔤 Lexer Updates
- Added `ALL` and `ANY` token types
- Enhanced symbol recognition for new syntax
- Improved token type organization

---

## [v0.1.0] - More Features & Bug Fixes - November 19, 2025

### 🎯 Code Quality
- Separated Error Handling from the `ManualParser`
- Encapsulated token types into inner class in Manual Lexer

### 🔑 New Keywords
- Added `builtin` keyword
- Introduced first version of `Sys`

### 📝 Naming Conventions
- **PascalCase** for class naming
- **camelCase**/**snake_case** for method/field/variable naming  
- **ALL_CAPS** for final method/field/variable naming
- `final` as implicit based on naming patterns

### 🐛 Bug Fixes
- Fixed multiple parsing and evaluation bugs in the early-stage compiler and interpreter.
- Resolved token handling edge cases in the manual lexer.

---

## [v0.0.7] - Return Slot Assignment Improvements - November 15, 2025

### 🔄 Multiple Return Value Handling
- Improved multiple return value handling via the `::` return slot operator.
- Enhanced slot assignment mechanisms for more reliable multi-value returns.
- Fixed edge cases in slot resolution when methods return more than one named value.

---

## [v0.0.4] - First Release - October 26, 2025

### 🎉 Initial Launch
- Created the first public repository for the Coderive programming language.
- Initial commit establishing the foundational codebase structure, including the manual lexer, basic parser, and early interpreter skeleton.
- Project inception marking the start of Coderive language development.
