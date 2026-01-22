# Changelog

All notable changes to Coderive are documented in this file.

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
- **Completely abandoned `~|` to instead use `::` return slot operator** - Embracing suffix-style design, fully abandoning the prefix-style.
- **Replaced the stack-based multi-arch compiler with a TAC IR** - Ensuring lesser systemic bugs to show. (ongoing, not yet prioritized)
- **Almost all classes renamed  updated, and repackaged** - Full refactoring of classes and their packages for more cleaner view.
- **Fully removed ANTLR Dependency** - To focus on the language implementation and lessen dependencies.
- **Replaced `<type><ws><name>`  to use `<name><colon><optional-ws><type>` instead for variable declaration.** - `name: type` for declaration, `name: type = value` for explicit declaration and assignment, `name := value` for inferred declaration and assignment,`name = value` for reassignment

### ✨ Major Features
- **Three World System**
  - Added three distinct types of programs for Coderive: Script, Method-only, and Module.
- **From String to Text**
  - Replaced the previous `string` keyword with `text`.
- **Natural Array**
  - Added supports for range for arrays
  - Lazy array generation
  - Immutable by default but can be mutable (Note: "Use moderately")
  - Supports text as natural array.
- **Added numeric shorthands**
  - Added support for common numeric shorthands: `K, M, B, T, Q, Qi, e`
  - Case for this feature will be case-sensitive.
- **Added parameter skipping and named arguments support**

> Check for other new minor features if you have free time...

### 📚 Documentation
- Updated all demo files showcasing the new updates
- Added `ParamSkipDemo.cod` file for testing parameter skipping.

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
- Fixed various bugs in the compiler

---

## [v0.0.7] - Return Slot Assignment Improvements - November 15, 2025

### 🔄 Multiple Return Value Handling
- Improvements in multiple return value handling
- Enhanced slot assignment mechanisms

---

## [v0.0.4] - First Release - October 26, 2025

### 🎉 Initial Launch
- Created first repository for Coderive programming language
- Initial commit with foundational codebase structure
- Project inception marking the start of Coderive language development
