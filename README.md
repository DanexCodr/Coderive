# Coderive

A mobile-first general programming language written primarily in Java 7. The runtime is built in c. It is a dual parser (antlr + manual "recursive backtracking"), dual compilation language (bytecode + native code generation).

## Vision
To have the very first filipino-made mobile-first production-ready self-hosting (native) programming language.

## Mission  
To have a programming language as a general use and solve any issues with the currently existing ones.

## Core Values
- Clean code structure
- Fast compilation and runtime
- Mobile-first development

## Development Environment
The developer (DanexCodr) is constrained by only having a phone:

1. **Java NIDE** - Fast Java 7 compiler
2. **Quickedit** - Fast editor  
3. **Termux** - Comprehensive linux environment
4. **AI Assistants** - Deepseek and Gemini for faster code debugging

## Technical Architecture
- Custom AOT compiler with JIT techniques
- "Future-cost — predict next use register spilling" register allocation
- Dual compilation pipeline (bytecode + native)
- Multi-target code generation (ARM64/x86_64)

## Language Example
```coderive
unit demo.program get {
    cdrv.Math
}

share interactiveDemo {
    ~| formula, operation
    local calculate(int a, int b, string op) {
        if op == "+" {
            ~ formula a + b
            ~ operation "addition"
        } else if op == "-" {
            ~ formula a - b
            ~ operation "subtraction"
        }
        // ... more code
    }
    
    share main() {
        output "=== CODERIVE INTERACTIVE DEMO ==="
        demonstrateArrays()
        getUserInfo()
        interactiveCalculator()
        numberSeries()
        edgeCaseLoops()
    }
}
```

## Compilation Output

```
[20:26:28.018] [INFO] RUNNER: Starting MTOT compilation pipeline
[20:26:28.021] [INFO] RUNNER: Input file: interactiveDemo.cdrv
[20:26:28.056] [INFO] RUNNER: AST built successfully
[20:26:28.068] [INFO] BYTECODE: Compilation complete: 1076 instructions across 9 methods
[20:26:28.071] [INFO] MTOT: Detected CPU: aarch64
[20:26:28.227] [INFO] MTOT: Full compilation pipeline complete.
```

## Generated Assembly Sample

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

## Runtime Performance

The system demonstrates working register allocation and proper loop execution across both interpreter and native compilation targets, with the native code showing expected output for complex loop patterns and edge cases.

## Notable Features

· Multi-return slots with slit declarations on top of each function declarations.

· Expressive and smart for-loops.

· Implicit class and method declaration.

· Cleaner importing

## Current Status

· ✅ Working interpreter with full language features

· ✅ Native code generation for ARM64/x86_64

· ✅ Advanced register allocation with spill optimization

· ✅ Complex loop patterns and control flow

· 🔧 String handling improvements in progress

· 🔧 Enhanced type system in development

---

>>>  Built with passion and persistence on and for mobile devices — proving that innovation knows no hardware boundaries.
