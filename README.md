<!-- markdownlint-disable first-line-h1 -->
<!-- markdownlint-disable html -->
<!-- markdownlint-disable no-duplicate-header -->

<div align="center">
  <img src="https://raw.githubusercontent.com/DanexCodr/Coderive/main/assets/1762666234889.jpg" alt="Coderive Logo" width="60%" />
</div>
<hr>
<div align="center" style="line-height: 1;">
  <a href="https://github.com/DanexCodr/Coderive"><img alt="Repository"    src="https://img.shields.io/badge/Project-Coderive-536af5?color=536af5&logoColor=white"/></a>
  <a href="https://github.com/DanexCodr/Coderive/discussions"><img alt="Discussions"
    src="https://img.shields.io/badge/💬%20Discussions-Community-ffc107?color=ffc107&logoColor=white"/></a>
  <a href="https://github.com/DanexCodr/Coderive/issues"><img alt="Issues"
    src="https://img.shields.io/badge/🐛%20Issues-Report%20Bugs-brightgreen?color=brightgreen&logoColor=white"/></a>
  <br>
  <a href="https://github.com/DanexCodr/Coderive"><img alt="GitHub Stars"
    src="https://img.shields.io/github/stars/DanexCodr/Coderive.svg?color=7289da&logo=github&logoColor=white"/></a>
  <a href="https://github.com/DanexCodr/Coderive/blob/main/LICENSE"><img alt="License"
    src="https://img.shields.io/badge/License-MIT-f5de53?&color=f5de53"/></a>
  <br>

  <b>Mobile-First Programming Language</b>
</div>

## Table of Contents

1. [Introduction](#1-introduction)
2. [Technical Architecture](#2-technical-architecture) 
3. [Language Features](#3-language-features)
4. [Performance Validation](#4-performance-validation)
5. [Getting Started](#5-getting-started)
6. [Current Status](#6-current-status)
7. [License](#7-license)
8. [Contact](#8-contact)

## 1. Introduction

We present **Coderive v0.2.3**, a mobile-first general programming language designed for **safe, fast, and clear** coding. 
Coderive features a dual parser system (ANTLR + manual recursive backtracking) and dual compilation pipeline (bytecode + native code generation).
Built entirely on mobile devices, Coderive proves that serious compiler development can happen outside traditional environments.

<p align="center">
  <img width="90%" src="https://raw.githubusercontent.com/DanexCodr/Coderive/main/assets/quantifier_estimation.jpg">
</p>

## 2. Technical Architecture

---

**Compiler Pipeline: Efficient Code Generation**

- **Dual Compilation:** Simultaneous bytecode and native code generation
- **Multi-Target Support:** ARM64 and x86_64 code generation from single codebase
- **Advanced Register Allocation:** Hybrid "future-cost" predictive register spilling
- **Mobile-First Design:** Built and tested primarily on Android devices

---

**Development Environment: Constraint-Driven Innovation**

The language was developed under the constraint of mobile-only development:
- **Java NIDE:** Fast Java 7 compiler for Android
- **Quickedit:** High-performance mobile code editor  
- **Termux:** Comprehensive Linux environment
- **AI Assistants:** DeepSeek and Gemini for accelerated debugging

---

## 3. Language Features

### Core Innovations

**Quantifier-First Logic Design**
Coderive replaces traditional boolean operators with expressive quantifiers:

| Traditional | Coderive |
|-------------|----------|
| `A && B && C` | `all[A, B, C]` |
| `A \|\| B \|\| C` | `any[A, B, C]` |
| `A && (B \|\| C)` | `all[A, any[B, C]]` |

**Multi-Return Slot System**
```python
share Calculator {

    local calculate(int a, int b) :: result: int, operation: text  /* Return slot declarations */
    {
        ~> a + b, "addition"  # Slot assignments
    }
}
```

**Smart For-Loops**

```python
for i by *2 in 1 to 10 {  # Complex step patterns
    # Loop body
}
```

**Language Example**

```python
unit sample.program

get {
    lang.Math
}

share InteractiveDemo {

    ~| int formula, string operation
    local calculate(int a, int b, string op) {
        if all[a >= 0, b >= 0] {
            if op == any["+", "-", "*"] {
                ~> a + b, "valid operation"
            }
        }
        ~> 0, "invalid"
    }
}
```

## 4. Performance Validation

The system demonstrates efficient execution across both interpreter and native compilation targets. 
Internal performance profiling shows that Coderive's quantifier operations approach the efficiency of
highly optimized data structures in established languages.

<div align="center">

|Feature|Status|Target|Details|
|-------|------|------|-------|
|Interpreter|✅ Working|JVM Bytecode|Full language support|
|Native Compilation|✅ Working|ARM64/x86_64|Advanced register allocation|
|Quantifier Performance|✅ Validated|All targets|Efficient short-circuiting|

</div>

## 5. Getting Started

**System Requirements**

· Java 7 or later

· Linux environment (Termux recommended for mobile)

**Quick Start**

```bash
# Run interpreter
java -jar coderive.jar program.cod

# Compile to native
java -jar coderive.jar --native program.cod
```

**Compilation Output**

```
[20:26:28.018] [INFO] RUNNER: Starting MTOT compilation pipeline
[20:26:28.056] [INFO] RUNNER: AST built successfully  
[20:26:28.068] [INFO] BYTECODE: Compilation complete: 1076 instructions across 9 methods
[20:26:28.227] [INFO] MTOT: Full compilation pipeline complete.
```

**Generated Assembly Sample**

A snippet of the resulting ARM64 assembly code from the compilation pipeline:

```assembly
    .text
    .global add
add:
    stp x29, x30, [sp, #-16]!
    mov x29, sp
    sub sp, sp, #48
    // Saving callee-saved registers
    stp x19, x20, [x29, #-16]
    // ... ARM64 assembly code
    mov x0, x23
    ret
```

## 6. Current Status

<div align="center">

|Component|Status|Notes|
|---------|------|-----|
|Interpreter|✅ Complete|Full language features|
|Native Code Generation|✅ Complete|ARM64/x86_64 support|
|Register Allocation|✅ Complete|Predictive spilling|
|String Handling|🔧 In Progress|Enhanced implementation|
|Type System|🔧 In Progress|Extended features|

</div>

## 7. License

This project is licensed under the [MIT License](/LICENSE).

## 8. Contact

Have questions or want to contribute? 

Join our community discussions:

· [GitHub Discussions](https://github.com/DanexCodr/Coderive/discussions) - Ask questions and share ideas

· [GitHub Issues](https://github.com/DanexCodr/Coderive/issues) - Report bugs and problems

· Developer's Email: danisonnunez001@gmail.com

---

<div align="center">
  <em>Built with passion on mobile devices — proving innovation knows no hardware boundaries.</em>
</div>
