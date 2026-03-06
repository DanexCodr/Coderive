<!-- markdownlint-disable first-line-h1 -->
<!-- markdownlint-disable html -->
<!-- markdownlint-disable no-duplicate-header -->

<div align="center">
  <img src="https://raw.githubusercontent.com/DanexCodr/Coderive/main/docs/assets/coderive-logo.jpg" alt="Coderive Logo" width="55%" />
  <h3>Safe. Fast. Clear.</h3>
  <p><em>A modern general-purpose programming language built for expressive, safe code.</em></p>
</div>

<div align="center">

[![Version](https://img.shields.io/badge/version-0.8.0-536af5?style=flat-square&logo=github)](https://github.com/DanexCodr/Coderive/releases)
[![Java](https://img.shields.io/badge/requires-Java%207%2B-ed8b00?style=flat-square&logo=openjdk&logoColor=white)](https://adoptium.net/)
[![License](https://img.shields.io/badge/license-MIT-f5de53?style=flat-square)](LICENSE)
[![Stars](https://img.shields.io/github/stars/DanexCodr/Coderive?style=flat-square&color=7289da&logo=github)](https://github.com/DanexCodr/Coderive/stargazers)
[![Discussions](https://img.shields.io/badge/discussions-join-ffc107?style=flat-square&logo=github)](https://github.com/DanexCodr/Coderive/discussions)

</div>

---

## Table of Contents

- [What is Coderive?](#what-is-coderive)
- [Why Coderive?](#why-coderive)
- [Getting Started](#getting-started)
- [Language Features](#language-features)
- [Examples](#examples)
- [Web Playground](#web-playground)
- [License](#license)
- [Contact](#contact)

---

## What is Coderive?

**Coderive** is a modern, interpreted programming language designed around three principles:

| Principle | What it means |
|-----------|--------------|
| **Safe** | Quantifier-first logic eliminates operator-precedence bugs; typed slots make multi-return values explicit |
| **Fast** | O(1) lazy arrays and formula-optimized loops handle data at any scale without materialising elements |
| **Clear** | Human-readable syntax — `all[...]`, `any[...]`, `~>`, `::` — reads like intent, not symbols |

Coderive features a fully hand-written, modular lexer-parser pipeline (no ANTLR), a recursive-descent parser with backtracking, and an O(1) range system that can represent a quintillion-element array in a few microseconds.

> **Built on mobile.** Coderive was developed entirely on a phone — Java NIDE, Quickedit, Termux, and AI assistants (DeepSeek, Gemini). Serious language design knows no hardware limits.

---

## Why Coderive?

### ① Quantifier-First Logic

No more `&&` / `||` precedence headaches. Logic reads exactly like its intent.

```python
# Traditional — what does this evaluate first?
if a && b || c && d { ... }

# Coderive — crystal clear
if any[all[a, b], all[c, d]] { ... }
```

Use `all[]` for AND, `any[]` for OR, nest them freely:

```python
if all[score >= 60, attempts <= maxAttempts] {
    out("Passed!")
}

if role == any["admin", "moderator", "owner"] {
    out("Access granted for {role}")
}
```

---

### ② Multi-Return Slot System

Functions declare named return slots with `::` and assign them with `~>`. Call sites destructure them cleanly.

```python
local divide(a: int, b: int)
:: quotient: float, remainder: int, status: text {
    if b == 0 {
        ~> 0.0, 0, "division by zero"
    }
    ~> a / b, a % b, "ok"
}

# Destructure only what you need
q, r, s := [quotient, remainder, status]:divide(17, 5)
out("{17} ÷ 5 = {q}  remainder {r}  ({s})")
```

---

### ③ Smart For-Loops

Every step pattern you'll ever need, built into the language:

```python
for i of 1 to 10          { ... }  # count up
for i of 10 to 1          { ... }  # auto-reverse
for i of 1 to 100 by 2    { ... }  # additive step
for i of 1 to 64  by *2   { ... }  # multiplicative (powers of 2)
for i of 64 to 1  by /2   { ... }  # divisive (halving)
for i of 1..32#*2          { ... }  # inline formula shorthand
```

---

### ④ O(1) Lazy Arrays

Create a quintillion-element array instantly — no memory, no iteration:

```python
huge : [text] = [0 to 1Qi]   // 1 Quintillion elements, created in < 1 ms

// Loop body detected as a conditional formula — still O(1)
for i of huge {
    if i % 2 == 0 { huge[i] = "even" }
    else          { huge[i] = "odd"  }
}

out(huge[24000])  // → "even"
out(huge[24001])  // → "odd"
```

Range indexing, negative indices, and lexicographic ranges all work out of the box:

```python
slice    := nums[-5 to -1]           // last 5 elements
alphabet := ["a" to "z"]            // 26-element letter range
letters  := ["aa" to "zz"]          // 676-element 2-letter range
```

---

## Getting Started

### Requirements

- **Java 7 or later** ([Download Adoptium JDK](https://adoptium.net/))
- Linux, macOS, or Windows (WSL/Git Bash for the installer)

---

### Option 1 — One-command Install (recommended)

Clone the repo and run the installer. It will verify Java, install the runtime, and register the `coderive` command:

```bash
git clone https://github.com/DanexCodr/Coderive.git
cd Coderive
chmod +x install.sh && ./install.sh
```

After install, add `~/.local/bin` to your PATH if prompted:

```bash
echo 'export PATH="$HOME/.local/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc
```

Then run your first program:

```bash
coderive examples/hello.cod
```

---

### Option 2 — Direct jar execution

If you prefer not to use the installer, run programs directly with Java:

```bash
java -cp docs/assets/Coderive.jar cod.runner.CommandRunner examples/hello.cod
```

You can alias this for convenience:

```bash
alias coderive='java -cp /path/to/Coderive.jar cod.runner.CommandRunner'
```

---

### Your first program

Create `hello.cod`:

```python
name := in(text, "What's your name? ")
out("Hello, {name}! 👋")
out("Welcome to Coderive.")
```

Run it:

```bash
coderive hello.cod
```

```
What's your name? Ada
Hello, Ada! 👋
Welcome to Coderive.
```

---

## Language Features

### Variables & Types

```python
# Type-inferred declaration
x := 42
name := "Coderive"
ratio := 3.14

# Explicit typed declaration
count: int = 0
label: text = "hello"

# Union types
value: int|float = 9.5
```

### Input & Output

```python
out("Hello!")                           # print with newline
out("A", "B", "C")                      # each arg on its own line
outs("Loading") outs(".") outs(".")     # print without newline

name    := in(text, "Enter name: ")     # prompt + read text
age     := in(int,  "Enter age: ")      # prompt + read int
height  := in(float)                    # read float (no prompt)
```

### Control Flow

```python
if all[age >= 18, hasId] {
    out("Welcome!")
} else if age >= 16 {
    out("Almost there.")
} else {
    out("Sorry, too young.")
}
```

### Functions & Slots

```python
# Single-expression shorthand
local square(n: int) ~> n * n

# Multi-return slots
local stats(a: int, b: int, c: int)
:: total: int, average: float {
    sum := a + b + c
    ~> sum, sum / 3.0
}

# Call and destructure
t, avg := [total, average]:stats(10, 20, 30)
out("Total: {t}, Average: {avg}")
```

### Default Parameters & Skipping

```python
local greet(name: text, greeting: text = "Hello") :: msg: text {
    ~> msg: "{greeting}, {name}!"
}

result := greet("Alice", _)   # skip optional param — uses default
```

### Classes & Policies

```python
policy Printable {
    print() :: output: text
}

share Document with Printable {
    title: text = "Untitled"

    policy print() :: output: text {
        ~> output: "Document: {this.title}"
    }

    share main() {
        doc := Document()
        msg := print():doc
        out(msg)
    }
}
```

### Multi-line Strings

```python
report := |"
    Name:  Alice
    Score: 98
    Grade: A+
    "|
out(report)
```

### Numeric Shorthands

| Suffix | Value |
|--------|-------|
| `1K`   | 1,000 |
| `1M`   | 1,000,000 |
| `1B`   | 1,000,000,000 |
| `1T`   | 1,000,000,000,000 |
| `1Q`   | 1,000,000,000,000,000 |
| `1Qi`  | 1,000,000,000,000,000,000 |
| `1e6`  | 1,000,000 (scientific) |

### String Built-in Methods

Call methods directly on any `text` value using the dot operator:

```python
greeting := "  Hello, Coderive!  "
clean    := greeting.trim()           # remove whitespace

out(clean.upper())                    # → "HELLO, CODERIVE!"
out(clean.lower())                    # → "hello, coderive!"
out(clean.length)                     # → 16

out(clean.contains("Coderive"))       # → true
out(clean.startsWith("Hello"))        # → true
out(clean.endsWith("!"))              # → true

out(clean.replace("Hello", "Hi"))     # → "Hi, Coderive!"
out(clean.reversed())                 # → "!eviredoC ,olleH"

# Chaining
out("  coderive  ".trim().upper())    # → "CODERIVE"
```

| Method | Description |
|--------|-------------|
| `.length` | Number of characters |
| `.upper()` | Convert to uppercase |
| `.lower()` | Convert to lowercase |
| `.trim()` | Remove leading/trailing whitespace |
| `.contains(sub)` | `true` if `sub` is found |
| `.startsWith(prefix)` | `true` if text starts with `prefix` |
| `.endsWith(suffix)` | `true` if text ends with `suffix` |
| `.replace(old, new)` | Replace every occurrence of `old` with `new` |
| `.reversed()` | Reverse the characters |

### Number Built-in Methods

Built-in math operations available directly on numeric values:

```python
n := -42
out(n.abs())           # → 42

base := 2
out(base.pow(10))      # → 1024

side := 144
out(side.sqrt())       # → 12

# Combining methods
a := -3
b := -4
out((a.abs().pow(2) + b.abs().pow(2)).sqrt())   # → 5
```

| Method | Description |
|--------|-------------|
| `.abs()` | Absolute value |
| `.pow(n)` | Raise to power `n` |
| `.sqrt()` | Square root |

---

## Examples

The [`examples/`](examples/) directory has ready-to-run programs:

| File | What it shows |
|------|--------------|
| [`hello.cod`](examples/hello.cod) | String interpolation, input |
| [`fizzbuzz.cod`](examples/fizzbuzz.cod) | `all[]` logic, `for…of` loops |
| [`calculator.cod`](examples/calculator.cod) | Multi-return slots, `any[]` validation |
| [`smart_loops.cod`](examples/smart_loops.cod) | Additive / multiplicative / divisive steps, inline formula |
| [`lazy_arrays.cod`](examples/lazy_arrays.cod) | O(1) NaturalArray, formula loops, range indexing, lexicographic ranges |
| [`string_methods.cod`](examples/string_methods.cod) | String built-in methods (v0.8.0) |
| [`number_methods.cod`](examples/number_methods.cod) | Number built-in methods (v0.8.0) |

Run any example:

```bash
coderive examples/fizzbuzz.cod
coderive examples/calculator.cod
coderive examples/smart_loops.cod
coderive examples/lazy_arrays.cod
coderive examples/string_methods.cod
coderive examples/number_methods.cod
```

---

## Web Playground

Try Coderive directly in your browser — no install required:

**[→ Launch the Playground](https://danexcodr.github.io/Coderive)**

The playground gives you a full interactive editor with instant output, making it the fastest way to experiment with the language.

---

## License

This project is licensed under the [MIT License](LICENSE).

---

## Contact

Have questions, ideas, or want to contribute?

- 💬 [GitHub Discussions](https://github.com/DanexCodr/Coderive/discussions) — ask questions, share ideas
- 🐛 [GitHub Issues](https://github.com/DanexCodr/Coderive/issues) — report bugs
- 📧 danisonnunez001@gmail.com

---

<div align="center">
  <em>Built with passion on mobile — proving that innovation knows no hardware boundaries.</em>
</div>
