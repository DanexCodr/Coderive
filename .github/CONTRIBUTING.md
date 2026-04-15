# Contributing to Coderive

First off, thank you for considering contributing to Coderive! It's people like you that make Coderive a great language.

## How Can I Contribute?

### Reporting Bugs

Before creating a bug report, please check the existing issues to avoid duplicates.

**When creating a bug report, include:**
- Coderive version (`coderive --version` or from VERSION file)
- Java version (`java -version`)
- Operating system (Android, Linux, macOS, Windows)
- Minimal code example that reproduces the issue
- Expected behavior vs actual behavior

**Use the bug report template** when submitting issues.

### Suggesting Enhancements

Enhancement suggestions are welcome! Please include:
- A clear use case (not just "add feature X")
- How it fits with Coderive's principles (Safe. Fast. Clear.)
- Example syntax if applicable

**Use the feature request template** when submitting suggestions.

### Pull Requests

1. Fork the repository
2. Create a feature branch:
   `git checkout -b feature/amazing-feature`
3. Make your changes
4. Run tests to ensure nothing breaks:
   `./gradlew test`
5. Commit with a clear message:
   `git commit -m "Add amazing feature that does X"`
6. Push to your fork:
   `git push origin feature/amazing-feature`
7. Open a Pull Request against `main`

### First Time Contributors

Looking for something to work on? Check issues labeled `good-first-issue` or `help-wanted`. Feel free to ask questions on Discord or in the issue comments.

## Development Setup

### Requirements

- Java 7 or later (Java 8+ recommended for development)
- Gradle (wrapper included, no manual install needed)

### Building

Build everything:
`./gradlew build`

Create runnable JAR:
`./gradlew fatJar`

### Running Tests

Run all tests:
`./gradlew test`

Run parity validation suite:
`java -cp build/libs/Coderive.jar cod.runner.CodPTACParityRunner`

### Project Structure

    src/main/java/cod/
    ├── lexer/         # Targeted lexers (zero-copy, perfect hash)
    ├── parser/        # Pratt parser with backtracking
    ├── ast/           # Abstract syntax tree nodes
    ├── semantic/      # Type checking, policies, validation
    ├── interpreter/   # Runtime interpreter
    ├── ir/            # CodP-TAC IR container
    ├── ptac/          # Pattern TAC optimizer
    └── runner/        # CLI runners (Command, REPL, Test)

## Code Style

- 2 spaces indentation (no tabs)
- ALL_CAPS for constants
- PascalCase for classes
- camelCase for methods and variables
- Javadoc for public APIs

## Testing Guidelines

- Add tests for new features
- Ensure all 53+ parity tests pass
- Invalid syntax should produce clear error messages

## Documentation

- Update README.md for user-facing changes
- Update website docs for language changes
- Add examples for new features

## Community

- Join [Discord](https://discord.gg/5Ne3wrXCX) for questions and discussion
- Be respectful and follow the Code of Conduct
- Help others when you can

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
