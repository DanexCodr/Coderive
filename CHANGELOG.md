# Changelog

All notable changes to Coderive are documented in this file.

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