# Coderive

A mobile first general programming language written primarily in java 7. The runtime is built in c. It is a dual parser (antlr + manual "recursive backtracking"), dual compilation language (bytecode + native code generation).

The compiler is a custom AOT with some JIT techniques implemented. It has a "future-cost — predict next use register spilling" type of register allocation.

Here is the sample run of the compiler:

'''

[20:26:28.018] [INFO] RUNNER: Starting MTOT compilation pipeline
[20:26:28.019] [INFO] RUNNER: Parser mode: MANUAL
[20:26:28.019] [INFO] RUNNER: Compilation mode: NATIVE_ONLY
[20:26:28.020] [INFO] RUNNER: Input file: /storage/emulated/0/JavaNIDE/Programming-Language/Coderive/executables/interactiveDemo.cdrv
[20:26:28.021] [INFO] RUNNER: Output file: /storage/emulated/0/program.s
[20:26:28.021] [INFO] RUNNER: Print AST: false
[20:26:28.022] [INFO] RUNNER: Enable linting: true
[20:26:28.056] [INFO] RUNNER: AST built successfully
[20:26:28.062] [INFO] BYTECODE: Starting MTOT bytecode compilation
[20:26:28.064] [WARN] BYTECODE: Slot return value mechanism not fully implemented in bytecode for method: calculate
[20:26:28.065] [WARN] BYTECODE: Slot return value mechanism not fully implemented in bytecode for method: add
[20:26:28.065] [WARN] BYTECODE: Slot return value mechanism not fully implemented in bytecode for method: haha
---- Linter found 2 issue(s) ----
Warning [interactiveDemo.add]: Method 'add' is 'local' and is never called.
Warning [interactiveDemo.haha]: Method 'haha' is 'local' and is never called.
----------------------------------
[20:26:28.068] [INFO] BYTECODE: Compilation complete: 1076 instructions across 9 methods
[20:26:28.071] [INFO] MTOT: Detected CPU: aarch64
[20:26:28.219] [INFO] RUNNER: Writing native assembly to /storage/emulated/0/program.s
[20:26:28.227] [INFO] MTOT: Full compilation pipeline complete.


'''

Here is a part of the generated assembly code:

'''

    .text
    .global add
add:
stp x29, x30, [sp, #-16]!
    mov x29, sp
    sub sp, sp, #48
    // Saving callee-saved registers: [x19, x20, x21, x22, x23, x24]
    stp x19, x20, [x29, #-16]
    stp x21, x22, [x29, #-32]
    stp x23, x24, [x29, #-48]
    mov x19, x0 // Copy 'this' pointer
    mov x20, #0 // WARNING: Used uninitialized local slot 0
    mov x21, x20
    mov x22, #0 // WARNING: Used uninitialized local slot 1
    mov x23, x22
    add x24, x21, x23
    mov x23, x24
    mov x24, #0
    mov x0, x23
    // Restoring callee-saved registers: [x19, x20, x21, x22, x23, x24]
    ldp x23, x24, [x29, #-64]
    ldp x21, x22, [x29, #-48]
    ldp x19, x20, [x29, #-32]
    mov sp, x29
    ldp x29, x30, [sp], #16
    ret
    .text
    .global haha
haha:
stp x29, x30, [sp, #-16]!
    mov x29, sp
    sub sp, sp, #16
    // Saving callee-saved registers: [x19, x20]
    stp x19, x20, [x29, #-16]
    mov x19, x0 // Copy 'this' pointer
    mov x20, #0
    mov x0, x20
    // Restoring callee-saved registers: [x19, x20]
    ldp x19, x20, [x29, #-32]
    mov sp, x29
    ldp x29, x30, [sp], #16
    ret
    .data
str_edgeCaseLoops_0: .asciz "=== Edge Case Loops ==="
str_edgeCaseLoops_1: .asciz "Single element:"
str_edgeCaseLoops_2: .asciz "Single: "
str_edgeCaseLoops_3: .asciz "Zero range up:"
str_edgeCaseLoops_4: .asciz "Zero up: "
str_edgeCaseLoops_5: .asciz "Zero range down:"
str_edgeCaseLoops_6: .asciz "Zero down: "
str_edgeCaseLoops_7: .asciz "Negative ranges:"
str_edgeCaseLoops_8: .asciz "Negative up: "
str_edgeCaseLoops_9: .asciz "Negative down: "
str_edgeCaseLoops_10: .asciz "Large steps:"
str_edgeCaseLoops_11: .asciz "Large step: "
    .text
    .global edgeCaseLoops
edgeCaseLoops:
stp x29, x30, [sp, #-16]!
    mov x29, sp
    sub sp, sp, #96
    // Saving callee-saved registers: [x19, x20, x21, x22, x23, x24, x25, x26, x27, x28]
    stp x19, x20, [x29, #-16]
    stp x21, x22, [x29, #-32]
    stp x23, x24, [x29, #-48]
    stp x25, x26, [x29, #-64]
    stp x27, x28, [x29, #-80]
    mov x19, x0 // Copy 'this' pointer
    adrp x20, str_edgeCaseLoops_0
    add x20, x20, :lo12:str_edgeCaseLoops_0
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    adrp x20, str_edgeCaseLoops_1
    add x20, x20, :lo12:str_edgeCaseLoops_1
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    mov x20, #5
    mov x21, x20
    mov x20, #5
    mov x22, x20
    b L_edgeCaseLoops_1
L_edgeCaseLoops_0:
    adrp x20, str_edgeCaseLoops_2
    add x20, x20, :lo12:str_edgeCaseLoops_2
    mov x23, x21
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x23
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    mov x24, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x20
    mov x1, x24
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x23, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x23
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x23, x21
    mov x24, #1
    add x20, x23, x24
    mov x21, x20
L_edgeCaseLoops_1:
    mov x20, x21
    mov x24, x22
    cmp x20, x24
    cset x23, le
    cmp x23, #0
    b.ne L_edgeCaseLoops_0
L_edgeCaseLoops_2:
    adrp x23, str_edgeCaseLoops_3
    add x23, x23, :lo12:str_edgeCaseLoops_3
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x23
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x23, #0
    mov x21, x23
    mov x23, #0
    mov x24, x23
    b L_edgeCaseLoops_4
L_edgeCaseLoops_3:
    adrp x23, str_edgeCaseLoops_4
    add x23, x23, :lo12:str_edgeCaseLoops_4
    mov x20, x21
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x25, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x23
    mov x1, x25
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x20, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x20, x21
    mov x25, #1
    add x23, x20, x25
    mov x21, x23
L_edgeCaseLoops_4:
    mov x23, x21
    mov x25, x24
    cmp x23, x25
    cset x20, le
    cmp x20, #0
    b.ne L_edgeCaseLoops_3
L_edgeCaseLoops_5:
    adrp x20, str_edgeCaseLoops_5
    add x20, x20, :lo12:str_edgeCaseLoops_5
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x20, #0
    mov x21, x20
    mov x20, #0
    mov x25, x20
    b L_edgeCaseLoops_7
L_edgeCaseLoops_6:
    adrp x20, str_edgeCaseLoops_6
    add x20, x20, :lo12:str_edgeCaseLoops_6
    mov x23, x21
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x23
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x26, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x20
    mov x1, x26
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x23, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x23
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x23, x21
    mov x26, #1
    add x20, x23, x26
    mov x21, x20
L_edgeCaseLoops_7:
    mov x20, x21
    mov x26, x25
    cmp x20, x26
    cset x23, le
    cmp x23, #0
    b.ne L_edgeCaseLoops_6
L_edgeCaseLoops_8:
    adrp x23, str_edgeCaseLoops_7
    add x23, x23, :lo12:str_edgeCaseLoops_7
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x23
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x23, #5
    neg x26, x23
    mov x21, x26
    mov x26, #1
    neg x23, x26
    mov x26, x23
    b L_edgeCaseLoops_10
L_edgeCaseLoops_9:
    adrp x23, str_edgeCaseLoops_8
    add x23, x23, :lo12:str_edgeCaseLoops_8
    mov x20, x21
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x27, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x23
    mov x1, x27
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x20, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x20, x21
    mov x27, #1
    add x23, x20, x27
    mov x21, x23
L_edgeCaseLoops_10:
    mov x23, x21
    mov x27, x26
    cmp x23, x27
    cset x20, le
    cmp x20, #0
    b.ne L_edgeCaseLoops_9
L_edgeCaseLoops_11:
    mov x20, #1
    neg x27, x20
    mov x21, x27
    mov x27, #5
    neg x20, x27
    mov x27, x20
    b L_edgeCaseLoops_13
L_edgeCaseLoops_12:
    adrp x20, str_edgeCaseLoops_9
    add x20, x20, :lo12:str_edgeCaseLoops_9
    mov x23, x21
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x23
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x28, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x20
    mov x1, x28
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x23, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x23
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x23, x21
    mov x28, #-1
    add x20, x23, x28
    mov x21, x20
L_edgeCaseLoops_13:
    mov x20, x21
    mov x28, x27
    cmp x20, x28
    cset x23, ge
    cmp x23, #0
    b.ne L_edgeCaseLoops_12
L_edgeCaseLoops_14:
    adrp x23, str_edgeCaseLoops_10
    add x23, x23, :lo12:str_edgeCaseLoops_10
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x23
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x23, #0
    mov x21, x23
    mov x23, #50
    mov x28, x23
    b L_edgeCaseLoops_16
L_edgeCaseLoops_15:
    adrp x23, str_edgeCaseLoops_11
    add x23, x23, :lo12:str_edgeCaseLoops_11
    mov x20, x21
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x9, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7, x9]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    mov x0, x23
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x1, x9
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7, x9]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x20, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x20, x21
    mov x9, #10
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    add x23, x20, x9
    mov x21, x23
L_edgeCaseLoops_16:
    mov x23, x21
    mov x9, x28
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    cmp x23, x9
    cset x20, le
    cmp x20, #0
    b.ne L_edgeCaseLoops_15
L_edgeCaseLoops_17:
    mov x20, #0
    mov x0, x20
    // Restoring callee-saved registers: [x19, x20, x21, x22, x23, x24, x25, x26, x27, x28]
    ldp x27, x28, [x29, #-96]
    ldp x25, x26, [x29, #-80]
    ldp x23, x24, [x29, #-64]
    ldp x21, x22, [x29, #-48]
    ldp x19, x20, [x29, #-32]
    mov sp, x29
    ldp x29, x30, [sp], #16
    ret

    '''

    

