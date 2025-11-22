## Changelog

**v0.1.3** [Conditional Chain Revolution Begins] - November 23, 2025

**· GROUNDBREAKING FEATURE:** Added conditional chain syntax with any: and all:

· **Method call style:** user.hasPermission(any: "read", "write", "execute")

· **Equality style:** status == any:("active", "pending", "verified")

· **Inner negation support:** user.checkStatus(all: "active", !"banned")

· Short-circuit evaluation for optimal performance

**· PARSER ENHANCEMENTS:**

· Added BANG token for logical negation (!)
· Extended comparison expression parsing for equality chains
· Support for nested conditional chains
· Parentheses handling (optional for methods, required for equality)

**· AST EXTENSIONS:**

· Added EqualityChainNode for equality-style conditional chains
· Enhanced MethodCallNode with chain type and arguments
· Updated AST factory with chain creation methods

**· INTERPRETER SUPPORT:**

· Conditional chain evaluation in ExpressionEvaluator
· Truthiness detection for various value types
· Recursive chain expansion with proper short-circuiting

**· LEXER UPDATES:**

· Added ALL and ANY token types
· Enhanced symbol recognition for new syntax
· Improved token type organization

**v0.1.0** [Added More Features & Bug Fixes] - November 19, 2025

· Separated Error Handling from the ManualParser.

· Encapsulated token types into an inner class in Manual Lexer.

· Added 'builtin' keyword.

· Introducing first version of Sys.

· Added 'final' as implicit based on naming and enforced naming patterns.

· **PascalCase** for class naming.
· **CamelCase/SnakeCase** for method/field/variable namings.
· **AllCaps/AllUpperCase** for final method/field/variable namings.

· Fixed some bugs in the compiler.

**v0.0.7** [Added Improvements in Return Slot Assignment] - November 15, 2025

**v0.0.4** [First Release] - October 26, 2025

**🎉 Initial Launch**

· Created first repository for Coderive programming language.

· Initial commit with foundational codebase structure.

· Project inception marking the start of Coderive language development.
