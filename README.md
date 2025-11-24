# ✨ Coderive v0.2.3: Mobile-First Programming Language

<div align="center">

  <img src="https://raw.githubusercontent.com/DanexCodr/Coderive/main/assets/1762666234889.jpg" alt="Coderive Logo" width="200">

</div>

| Feature | Language Base | Runtime Base | Development | Project Status | Popularity |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **Status** | ![Java](https://img.shields.io/badge/Java-7-yellow) | ![Runtime](https://img.shields.io/badge/Runtime%20In-C-yellow) | ![Built On](https://img.shields.io/badge/Built%20On-Phone-purple) | ![Status](https://img.shields.io/badge/Status-Active-brightgreen) | [![GitHub Stars](https://img.shields.io/github/stars/DanexCodr/Coderive.svg)](https://github.com/DanexCodr/Coderive/stargazers) |
| **Details** | Written primarily in **Java 7** | Execution engine in C | **Mobile-first** architecture | Actively developed | Check out our stars! |

A mobile-first general programming language designed to be a go-to for **safe, fast, and clear** vibe coding. It features a **dual parser** (ANTLR + manual "recursive backtracking") and a **dual compilation** pipeline (bytecode + native code generation).

###### 🗓️ **Development period:** 1 month old

---

## 🎯 Vision & Values

### **Vision** 🇵🇭
To have the very first **Filipino-made**, mobile-first, production-ready, self-hosting (native) programming language.

### **Core Values**
* **Clean code structure** 🧼
* **Fast compilation and runtime** ⚡
* **Mobile-first development** 📱

---

## 🏗️ Technical Architecture

### **Compiler Pipeline**
Coderive uses a custom AOT compiler with JIT techniques and a multi-target code generation system.

* **Dual Compilation:** Bytecode and Native code generation.
* **Code Generation:** Multi-target support for **ARM64** / **x86_64**.
* **Register Allocation:** Hybrid "Future-cost — predict next use register spilling".

### **Development Environment** (Built Under Constraint) 🛠️
The developer (DanexCodr) is constrained by only using a phone, relying on:
1.  **Java NIDE:** Fast Java 7 compiler.
2.  **Quickedit:** Fast editor.
3.  **Termux:** Comprehensive Linux environment.
4.  **AI Assistants:** Deepseek and Gemini for faster code debugging.

---

## 🧪 Language and Execution Proof

### **Language Example: InteractiveDemo**
The following snippet demonstrates **multi-return slots** (`~|`) and expressive local control flow:

```python
unit sample.program

get {
    cod.Math
}

share InteractiveDemo {
    ~| formula, operation # Multi-return slots
    local calculate(int a, int b, string op) {
        if op == "+" {
            ~> a + b, "addition" # Return slot assignment
        } else if op == "-" {
            ~> a - b, "subtraction"
        }
        // ... more code
    }
    
    share main() {
        # ... main logic ...
    }
}
```

Show the complete of the Coderive file here: [The "InteractiveDemo" file](./src/main/cod/InteractiveDemo.cod/)

### **Compilation Output Snapshot**
The MTOT (Mobile Target Output Tool) pipeline successfully builds the AST and compiles the code:

```java
[20:26:28.018] [INFO] RUNNER: Starting MTOT compilation pipeline
[20:26:28.056] [INFO] RUNNER: AST built successfully
[20:26:28.068] [INFO] BYTECODE: Compilation complete: 1076 instructions across 9 methods
[20:26:28.071] [INFO] MTOT: Detected CPU: aarch64
[20:26:28.227] [INFO] MTOT: Full compilation pipeline complete.
```

### **Generated Assembly Sample**
A snippet of the resulting ARM64 assembly code:

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

---

## 📈 Quantifier Performance Validation

The system demonstrates working register allocation and proper execution across both interpreter and native compilation targets.

The chart below validates the **fast compilation and runtime** core value by demonstrating Coderive's design goal for fast logic during a simple membership check (the equivalent of `if element == any[...]`). The data shown is based on **internal, relative performance profiling** designed to validate the efficiency of the Coderive runtime implementation against common language equivalents.

<div align="center">
  <img src="https://raw.githubusercontent.com/DanexCodr/Coderive/main/assets/quantifier_estimation.jpg" alt="Estimated Runtime for Membership Check with Coderive any[]" width="600">
</div>

* **Result:** Coderive's runtime for `any[]` is designed to approach the performance characteristics of highly optimized hash-based lookups (Python set, Java HashSet).
* **Conclusion:** This validates the **Fast compilation and runtime** core value, proving that syntactic clarity does not come at the cost of execution speed.

---

## 🧠 Logic Revolution: Quantifier-First Design

Coderive replaces traditional boolean operators (`&&`, `||`) with expressive quantifiers, prioritizing **readability** and **clarity**.

| Traditional (Verbose) | Coderive (Declarative) |
| :--- | :--- |
| `if (name != "" && age >= 0 && age <= 120)` | `if all[name != "", age >= 0, age <= 120]` |
| `if (isAdmin || (isOwner && isActive))` | `if any[isAdmin, all[isOwner, isActive]]` |

### **Key Benefits**
* **More expressive:** Code says what it means.
* **Fewer bugs:** Eliminates operator precedence confusion.
* **Automatic Short-Circuiting:** Built directly into the language logic.

### **Quick Conversion Guide**

| Traditional | Coderive Equivalent |
| :--- | :--- |
| `A && B && C` | `all[A, B, C]` |
| `A || B || C` | `any[A, B, C]` |
| `A && (B || C)` | `all[A, any[B, C]]` |

---

## ⚙️ Notable Features

### **Language Innovations**
* **Multi-Return Slots:** Slot declarations (`~|`) atop function definitions.
* **Smart For-Loops:** Expressive step patterns like `by *2`, `by i+=1`, or `by *+2`.
* **Reduced Boilerplate:** Implicit class and method declaration.
* **Modular Imports:** Cleaner importing system with unit support.

### **Technical Breakthroughs**
* **Mobile-First Compiler:** Designed and built entirely on Android devices.
* **Predictive Register Allocation:** Hybrid "future-cost" register spilling.
* **Robust Parsing:** Dual-parser system (ANTLR + recursive backtracking).
* **Native Multi-Targeting:** Compiles for ARM64/x86_64 from a single Java codebase.

---

## 🏁 Getting Started

```bash
# Run interpreter
java -jar coderive.jar program.cod

# Compile to native
java -jar coderive.jar --native program.cod
```

---

## 💬 Community & Status

### **Current Status**
* ✅ Working interpreter with full language features
* ✅ Native code generation for ARM64/x86_64
* ✅ Advanced register allocation with spill optimization
* 🔧 String handling improvements in progress
* 🔧 Enhanced type system in development

### **Join the Discussion!**
[GitHub Discussions](https://github.com/DanexCodr/Coderive/discussions) - Ask questions, suggest features, share projects
[Issues](https://github.com/DanexCodr/Coderive/issues) - Report bugs and problems

---

> Built with passion and persistence on and for mobile devices — proving that innovation knows no hardware boundaries. Happy coding to derive your visions! 😊
