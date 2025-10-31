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
    .data
str_numberSeries_0: .asciz "=== Number Series Generator ==="
str_numberSeries_1: .asciz "Enter start number:"
input_type_numberSeries_2: .asciz "int"
str_numberSeries_3: .asciz "Enter end number:"
input_type_numberSeries_4: .asciz "int"
str_numberSeries_5: .asciz "Number series from "
str_numberSeries_6: .asciz " to "
str_numberSeries_7: .asciz ":"
str_numberSeries_8: .asciz ""
str_numberSeries_9: .asciz "=== DEFAULT STEPS (No 'by' clause) ==="
str_numberSeries_10: .asciz "Counting up naturally:"
str_numberSeries_11: .asciz "Default step: "
str_numberSeries_12: .asciz "Counting down naturally:"
str_numberSeries_13: .asciz "User range - smart default:"
str_numberSeries_14: .asciz "Smart default: "
str_numberSeries_15: .asciz "=== BASIC STEPS ==="
str_numberSeries_16: .asciz "Enter step size:"
input_type_numberSeries_17: .asciz "int"
str_numberSeries_18: .asciz "Step by "
str_numberSeries_19: .asciz ": "
str_numberSeries_20: .asciz "Manual i=i+1: "
str_numberSeries_21: .asciz "Compound i+=1: "
str_numberSeries_22: .asciz "Step by +1: "
str_numberSeries_23: .asciz "Countdown by -1: "
str_numberSeries_24: .asciz "Countdown manual: "
str_numberSeries_25: .asciz "Countdown compound: "
str_numberSeries_26: .asciz "=== MULTIPLICATIVE STEPS ==="
str_numberSeries_27: .asciz "Doubling: "
str_numberSeries_28: .asciz "Doubling manual: "
str_numberSeries_29: .asciz "Doubling compound: "
str_numberSeries_30: .asciz "Step by *+2: "
str_numberSeries_31: .asciz "=== DIVISION STEPS ==="
str_numberSeries_32: .asciz "Halving: "
str_numberSeries_33: .asciz "Halving manual: "
str_numberSeries_34: .asciz "Halving compound: "
str_numberSeries_35: .asciz "Step by /+2: "
str_numberSeries_36: .asciz "=== PRACTICAL PATTERNS ==="
str_numberSeries_37: .asciz "Powers of 2:"
str_numberSeries_38: .asciz "2^"
str_numberSeries_39: .asciz " = "
str_numberSeries_40: .asciz "Power compound: "
str_numberSeries_41: .asciz "Countdown sequences:"
str_numberSeries_42: .asciz "Natural countdown: "
str_numberSeries_43: .asciz "Explicit countdown: "
str_numberSeries_44: .asciz "Growing sequences:"
str_numberSeries_45: .asciz "Growing: "
str_numberSeries_46: .asciz "Growing manual: "
str_numberSeries_47: .asciz "Mixed operations:"
str_numberSeries_48: .asciz "Mixed default: "
str_numberSeries_49: .asciz "Mixed +2: "
str_numberSeries_50: .asciz "Mixed -2: "
    .text
    .global numberSeries
numberSeries:
stp x29, x30, [sp, #-16]!
    mov x29, sp
    sub sp, sp, #208
    // Saving callee-saved registers: [x19, x20, x21, x22, x23, x24, x25, x26, x27, x28]
    stp x19, x20, [x29, #-16]
    stp x21, x22, [x29, #-32]
    stp x23, x24, [x29, #-48]
    stp x25, x26, [x29, #-64]
    stp x27, x28, [x29, #-80]
    mov x19, x0 // Copy 'this' pointer
    adrp x20, str_numberSeries_0
    add x20, x20, :lo12:str_numberSeries_0
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    adrp x20, str_numberSeries_1
    add x20, x20, :lo12:str_numberSeries_1
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    adrp x0, input_type_numberSeries_2
    add x0, x0, :lo12:input_type_numberSeries_2
    bl runtime_read_input // Call runtime helper (expects type* in x0)
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    mov x20, x0 // Get input result
    mov x21, x20
    adrp x20, str_numberSeries_3
    add x20, x20, :lo12:str_numberSeries_3
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    adrp x0, input_type_numberSeries_4
    add x0, x0, :lo12:input_type_numberSeries_4
    bl runtime_read_input // Call runtime helper (expects type* in x0)
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    mov x20, x0 // Get input result
    mov x22, x20
    adrp x20, str_numberSeries_5
    add x20, x20, :lo12:str_numberSeries_5
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
    adrp x24, str_numberSeries_6
    add x24, x24, :lo12:str_numberSeries_6
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x23
    mov x1, x24
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x20, x0 // Get result
    mov x24, x22
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x24
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x23, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x20
    mov x1, x23
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x24, x0 // Get result
    adrp x23, str_numberSeries_7
    add x23, x23, :lo12:str_numberSeries_7
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x24
    mov x1, x23
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x20, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    adrp x20, str_numberSeries_8
    add x20, x20, :lo12:str_numberSeries_8
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    adrp x20, str_numberSeries_9
    add x20, x20, :lo12:str_numberSeries_9
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    adrp x20, str_numberSeries_10
    add x20, x20, :lo12:str_numberSeries_10
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x20, #1
    mov x23, x20
    mov x20, #5
    mov x24, x20
    b L_numberSeries_1
L_numberSeries_0:
    adrp x20, str_numberSeries_11
    add x20, x20, :lo12:str_numberSeries_11
    mov x25, x23
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x25
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x26, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x20
    mov x1, x26
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x25, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x25
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x25, x23
    mov x26, #1
    add x20, x25, x26
    mov x23, x20
L_numberSeries_1:
    mov x20, x23
    mov x26, x24
    cmp x20, x26
    cset x25, le
    cmp x25, #0
    b.ne L_numberSeries_0
L_numberSeries_2:
    adrp x25, str_numberSeries_12
    add x25, x25, :lo12:str_numberSeries_12
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x25
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x25, #5
    mov x23, x25
    mov x25, #1
    mov x26, x25
    b L_numberSeries_4
L_numberSeries_3:
    adrp x25, str_numberSeries_11
    add x25, x25, :lo12:str_numberSeries_11
    mov x20, x23
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x27, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x25
    mov x1, x27
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x20, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x20, x23
    mov x27, #-1
    add x25, x20, x27
    mov x23, x25
L_numberSeries_4:
    mov x25, x23
    mov x27, x26
    cmp x25, x27
    cset x20, ge
    cmp x20, #0
    b.ne L_numberSeries_3
L_numberSeries_5:
    adrp x20, str_numberSeries_13
    add x20, x20, :lo12:str_numberSeries_13
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x20, x21
    mov x23, x20
    mov x20, x22
    mov x27, x20
    mov x20, x23
    mov x25, x27
    cmp x20, x25
    cset x28, le
    cmp x28, #0
    b.ne L_numberSeries_9
    b L_numberSeries_7
L_numberSeries_6:
    adrp x28, str_numberSeries_14
    add x28, x28, :lo12:str_numberSeries_14
    mov x25, x23
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x25
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x20, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x28
    mov x1, x20
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x25, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x25
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x25, x23
    mov x20, #-1
    add x28, x25, x20
    mov x23, x28
L_numberSeries_7:
    mov x28, x23
    mov x20, x27
    cmp x28, x20
    cset x25, ge
    cmp x25, #0
    b.ne L_numberSeries_6
    b L_numberSeries_10
L_numberSeries_8:
    adrp x25, str_numberSeries_14
    add x25, x25, :lo12:str_numberSeries_14
    mov x20, x23
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x28, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x25
    mov x1, x28
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x20, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x20, x23
    mov x28, #1
    add x25, x20, x28
    mov x23, x25
L_numberSeries_9:
    mov x25, x23
    mov x28, x27
    cmp x25, x28
    cset x20, le
    cmp x20, #0
    b.ne L_numberSeries_8
L_numberSeries_10:
    adrp x20, str_numberSeries_8
    add x20, x20, :lo12:str_numberSeries_8
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    adrp x20, str_numberSeries_15
    add x20, x20, :lo12:str_numberSeries_15
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    adrp x20, str_numberSeries_16
    add x20, x20, :lo12:str_numberSeries_16
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    adrp x0, input_type_numberSeries_17
    add x0, x0, :lo12:input_type_numberSeries_17
    bl runtime_read_input // Call runtime helper (expects type* in x0)
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x20, x0 // Get input result
    mov x28, x20
    mov x20, x21
    mov x23, x20
    mov x20, x22
    mov x25, x20
    b L_numberSeries_12
L_numberSeries_11:
    adrp x20, str_numberSeries_18
    add x20, x20, :lo12:str_numberSeries_18
    mov x9, x28
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7, x9]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x0, x9
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7, x9]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x10, x0 // Get result
    // Spilling caller-saved registers before call: [x10, x2, x3, x4, x5, x6, x7]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    mov x0, x20
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x1, x10
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x10, x2, x3, x4, x5, x6, x7]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x9, x0 // Get result
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    adrp x10, str_numberSeries_19
    add x10, x10, :lo12:str_numberSeries_19
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x0, x9
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x1, x10
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x10, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x20, x0 // Get result
    mov x10, x23
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x2, x3, x4, x5, x6, x7]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x10, x2, x3, x4, x5, x6, x7]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x9, x0 // Get result
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7, x9]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    mov x0, x20
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x1, x9
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7, x9]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x10, x0 // Get result
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x2, x3, x4, x5, x6, x7]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x10, x2, x3, x4, x5, x6, x7]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x10, x23
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    mov x9, x28
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    add x20, x10, x9
    mov x23, x20
L_numberSeries_12:
    mov x20, x23
    mov x9, x25
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    cmp x20, x9
    cset x10, le
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    cmp x10, #0
    b.ne L_numberSeries_11
L_numberSeries_13:
    mov x10, x21
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x23, x10
    mov x10, x22
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x9, x10
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    b L_numberSeries_15
L_numberSeries_14:
    adrp x10, str_numberSeries_20
    add x10, x10, :lo12:str_numberSeries_20
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    mov x20, x23
    // Spilling caller-saved registers before call: [x10, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    mov x0, x20
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x10, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x11, x0 // Get result
    // Spilling caller-saved registers before call: [x10, x11, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    mov x1, x11
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x20, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7, x9]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7, x9]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x20, x23
    mov x11, #1
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    add x10, x20, x11
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x23, x10
L_numberSeries_15:
    mov x10, x23
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x11, x9
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    cmp x10, x11
    cset x20, le
    cmp x20, #0
    b.ne L_numberSeries_14
L_numberSeries_16:
    mov x20, x21
    mov x23, x20
    mov x20, x22
    mov x11, x20
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    b L_numberSeries_18
L_numberSeries_17:
    adrp x20, str_numberSeries_21
    add x20, x20, :lo12:str_numberSeries_21
    mov x10, x23
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x11, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x12, x0 // Get result
    // Spilling caller-saved registers before call: [x11, x12, x2, x3, x4, x5, x6, x7, x9]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    mov x0, x20
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    mov x1, x12
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x11, x12, x2, x3, x4, x5, x6, x7, x9]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x10, x0 // Get result
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x11, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x10, x23
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    mov x12, #1
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    add x20, x10, x12
    mov x23, x20
L_numberSeries_18:
    mov x20, x23
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    mov x12, x11
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    cmp x20, x12
    cset x10, le
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    cmp x10, #0
    b.ne L_numberSeries_17
L_numberSeries_19:
    mov x10, x21
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x23, x10
    mov x10, x22
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x12, x10
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    b L_numberSeries_21
L_numberSeries_20:
    adrp x10, str_numberSeries_22
    add x10, x10, :lo12:str_numberSeries_22
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    mov x20, x23
    // Spilling caller-saved registers before call: [x10, x11, x12, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    mov x0, x20
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x13, x0 // Get result
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    mov x1, x13
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x20, x0 // Get result
    // Spilling caller-saved registers before call: [x11, x12, x2, x3, x4, x5, x6, x7, x9]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x11, x12, x2, x3, x4, x5, x6, x7, x9]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x20, x23
    mov x13, #1
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    add x10, x20, x13
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x23, x10
L_numberSeries_21:
    mov x10, x23
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    mov x13, x12
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    cmp x10, x13
    cset x20, le
    cmp x20, #0
    b.ne L_numberSeries_20
L_numberSeries_22:
    mov x20, #10
    mov x23, x20
    mov x20, #1
    mov x13, x20
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    b L_numberSeries_24
L_numberSeries_23:
    adrp x20, str_numberSeries_23
    add x20, x20, :lo12:str_numberSeries_23
    mov x10, x23
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x14, x0 // Get result
    // Spilling caller-saved registers before call: [x11, x12, x13, x14, x2, x3, x4, x5, x6, x7, x9]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    mov x0, x20
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x1, x14
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x11, x12, x13, x14, x2, x3, x4, x5, x6, x7, x9]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x10, x0 // Get result
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x10, x23
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    mov x14, #1
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    neg x20, x14
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    add x14, x10, x20
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x23, x14
L_numberSeries_24:
    mov x14, x23
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    mov x20, x13
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    cmp x14, x20
    cset x10, ge
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    cmp x10, #0
    b.ne L_numberSeries_23
L_numberSeries_25:
    mov x10, #10
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x23, x10
    mov x10, #1
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x20, x10
    b L_numberSeries_27
L_numberSeries_26:
    adrp x10, str_numberSeries_24
    add x10, x10, :lo12:str_numberSeries_24
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    mov x14, x23
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x0, x14
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x15, x0 // Get result
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    mov x1, x15
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x14, x0 // Get result
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spilling caller-saved registers before call: [x11, x12, x13, x14, x2, x3, x4, x5, x6, x7, x9]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x0, x14
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x11, x12, x13, x14, x2, x3, x4, x5, x6, x7, x9]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x14, x23
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    mov x15, #1
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    sub x10, x14, x15
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x23, x10
L_numberSeries_27:
    mov x10, x23
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    mov x15, x20
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    cmp x10, x15
    cset x14, ge
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    cmp x14, #0
    b.ne L_numberSeries_26
L_numberSeries_28:
    mov x14, #10
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x23, x14
    mov x14, #1
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x15, x14
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    b L_numberSeries_30
L_numberSeries_29:
    adrp x14, str_numberSeries_25
    add x14, x14, :lo12:str_numberSeries_25
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    mov x10, x23
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    // Spill x24 to [fp-64]
    str x24, [x29, #-64]
    mov x24, x0 // Get result
    // Spill x24 to [fp-64]
    str x24, [x29, #-64]
    // Spilling caller-saved registers before call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x0, x14
    // Fill x24 from [fp-64]
    ldr x24, [x29, #-64]
    mov x1, x24
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x10, x0 // Get result
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x10, x23
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    mov x24, #1
    // Spill x24 to [fp-64]
    str x24, [x29, #-64]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x24 from [fp-64]
    ldr x24, [x29, #-64]
    sub x14, x10, x24
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x23, x14
L_numberSeries_30:
    mov x14, x23
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    mov x24, x15
    // Spill x24 to [fp-64]
    str x24, [x29, #-64]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x24 from [fp-64]
    ldr x24, [x29, #-64]
    cmp x14, x24
    cset x10, ge
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    cmp x10, #0
    b.ne L_numberSeries_29
L_numberSeries_31:
    adrp x10, str_numberSeries_8
    add x10, x10, :lo12:str_numberSeries_8
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    adrp x10, str_numberSeries_26
    add x10, x10, :lo12:str_numberSeries_26
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x10, #1
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x23, x10
    mov x10, #32
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x24, x10
    // Spill x24 to [fp-64]
    str x24, [x29, #-64]
    b L_numberSeries_33
L_numberSeries_32:
    adrp x10, str_numberSeries_27
    add x10, x10, :lo12:str_numberSeries_27
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    mov x14, x23
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x0, x14
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    // Spill x26 to [fp-72]
    str x26, [x29, #-72]
    mov x26, x0 // Get result
    // Spill x26 to [fp-72]
    str x26, [x29, #-72]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    // Fill x26 from [fp-72]
    ldr x26, [x29, #-72]
    mov x1, x26
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x14, x0 // Get result
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spilling caller-saved registers before call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x0, x14
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x14, x23
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    mov x26, #2
    // Spill x26 to [fp-72]
    str x26, [x29, #-72]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x26 from [fp-72]
    ldr x26, [x29, #-72]
    mul x10, x14, x26
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x23, x10
L_numberSeries_33:
    mov x10, x23
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x24 from [fp-64]
    ldr x24, [x29, #-64]
    mov x26, x24
    // Spill x26 to [fp-72]
    str x26, [x29, #-72]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x26 from [fp-72]
    ldr x26, [x29, #-72]
    cmp x10, x26
    cset x14, le
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    cmp x14, #0
    b.ne L_numberSeries_32
L_numberSeries_34:
    mov x14, #1
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x23, x14
    mov x14, #32
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x26, x14
    // Spill x26 to [fp-72]
    str x26, [x29, #-72]
    b L_numberSeries_36
L_numberSeries_35:
    adrp x14, str_numberSeries_28
    add x14, x14, :lo12:str_numberSeries_28
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    mov x10, x23
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    // Spill x27 to [fp-80]
    str x27, [x29, #-80]
    mov x27, x0 // Get result
    // Spill x27 to [fp-80]
    str x27, [x29, #-80]
    // Spilling caller-saved registers before call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x0, x14
    // Fill x27 from [fp-80]
    ldr x27, [x29, #-80]
    mov x1, x27
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x10, x0 // Get result
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x10, x23
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    mov x27, #2
    // Spill x27 to [fp-80]
    str x27, [x29, #-80]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x27 from [fp-80]
    ldr x27, [x29, #-80]
    mul x14, x10, x27
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x23, x14
L_numberSeries_36:
    mov x14, x23
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Fill x26 from [fp-72]
    ldr x26, [x29, #-72]
    mov x27, x26
    // Spill x27 to [fp-80]
    str x27, [x29, #-80]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x27 from [fp-80]
    ldr x27, [x29, #-80]
    cmp x14, x27
    cset x10, le
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    cmp x10, #0
    b.ne L_numberSeries_35
L_numberSeries_37:
    mov x10, #1
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x23, x10
    mov x10, #32
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x27, x10
    // Spill x27 to [fp-80]
    str x27, [x29, #-80]
    b L_numberSeries_39
L_numberSeries_38:
    adrp x10, str_numberSeries_29
    add x10, x10, :lo12:str_numberSeries_29
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    mov x14, x23
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x0, x14
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    // Spill x28 to [fp-88]
    str x28, [x29, #-88]
    mov x28, x0 // Get result
    // Spill x28 to [fp-88]
    str x28, [x29, #-88]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    // Fill x28 from [fp-88]
    ldr x28, [x29, #-88]
    mov x1, x28
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x14, x0 // Get result
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spilling caller-saved registers before call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x0, x14
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x14, x23
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    mov x28, #2
    // Spill x28 to [fp-88]
    str x28, [x29, #-88]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x28 from [fp-88]
    ldr x28, [x29, #-88]
    mul x10, x14, x28
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x23, x10
L_numberSeries_39:
    mov x10, x23
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x27 from [fp-80]
    ldr x27, [x29, #-80]
    mov x28, x27
    // Spill x28 to [fp-88]
    str x28, [x29, #-88]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x28 from [fp-88]
    ldr x28, [x29, #-88]
    cmp x10, x28
    cset x14, le
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    cmp x14, #0
    b.ne L_numberSeries_38
L_numberSeries_40:
    mov x14, x21
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x23, x14
    mov x14, x22
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x28, x14
    // Spill x28 to [fp-88]
    str x28, [x29, #-88]
    b L_numberSeries_42
L_numberSeries_41:
    adrp x14, str_numberSeries_30
    add x14, x14, :lo12:str_numberSeries_30
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    mov x10, x23
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    // Spill x25 to [fp-96]
    str x25, [x29, #-96]
    mov x25, x0 // Get result
    // Spill x25 to [fp-96]
    str x25, [x29, #-96]
    // Spilling caller-saved registers before call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x0, x14
    // Fill x25 from [fp-96]
    ldr x25, [x29, #-96]
    mov x1, x25
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x10, x0 // Get result
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x10, x23
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    mov x25, #2
    // Spill x25 to [fp-96]
    str x25, [x29, #-96]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x25 from [fp-96]
    ldr x25, [x29, #-96]
    mul x14, x10, x25
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x23, x14
L_numberSeries_42:
    mov x14, x23
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Fill x28 from [fp-88]
    ldr x28, [x29, #-88]
    mov x25, x28
    // Spill x25 to [fp-96]
    str x25, [x29, #-96]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x25 from [fp-96]
    ldr x25, [x29, #-96]
    cmp x14, x25
    cset x10, le
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    cmp x10, #0
    b.ne L_numberSeries_41
L_numberSeries_43:
    adrp x10, str_numberSeries_8
    add x10, x10, :lo12:str_numberSeries_8
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    adrp x10, str_numberSeries_31
    add x10, x10, :lo12:str_numberSeries_31
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x10, #64
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x23, x10
    mov x10, #2
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x25, x10
    // Spill x25 to [fp-96]
    str x25, [x29, #-96]
    b L_numberSeries_45
L_numberSeries_44:
    adrp x10, str_numberSeries_32
    add x10, x10, :lo12:str_numberSeries_32
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    mov x14, x23
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x0, x14
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    // Spill x20 to [fp-104]
    str x20, [x29, #-104]
    mov x20, x0 // Get result
    // Spill x20 to [fp-104]
    str x20, [x29, #-104]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    // Fill x20 from [fp-104]
    ldr x20, [x29, #-104]
    mov x1, x20
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x14, x0 // Get result
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spilling caller-saved registers before call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x0, x14
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x14, x23
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    mov x20, #2
    // Spill x20 to [fp-104]
    str x20, [x29, #-104]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x20 from [fp-104]
    ldr x20, [x29, #-104]
    sdiv x10, x14, x20
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x23, x10
L_numberSeries_45:
    mov x10, x23
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x25 from [fp-96]
    ldr x25, [x29, #-96]
    mov x20, x25
    // Spill x20 to [fp-104]
    str x20, [x29, #-104]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x20 from [fp-104]
    ldr x20, [x29, #-104]
    cmp x10, x20
    cset x14, ge
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    cmp x14, #0
    b.ne L_numberSeries_44
L_numberSeries_46:
    mov x14, #64
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x23, x14
    mov x14, #2
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x20, x14
    // Spill x20 to [fp-104]
    str x20, [x29, #-104]
    b L_numberSeries_48
L_numberSeries_47:
    adrp x14, str_numberSeries_33
    add x14, x14, :lo12:str_numberSeries_33
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    mov x10, x23
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    // Spill x24 to [fp-64]
    str x24, [x29, #-64]
    mov x24, x0 // Get result
    // Spill x24 to [fp-64]
    str x24, [x29, #-64]
    // Spilling caller-saved registers before call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x0, x14
    // Fill x24 from [fp-64]
    ldr x24, [x29, #-64]
    mov x1, x24
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x10, x0 // Get result
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x10, x23
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    mov x24, #2
    // Spill x24 to [fp-64]
    str x24, [x29, #-64]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x24 from [fp-64]
    ldr x24, [x29, #-64]
    sdiv x14, x10, x24
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x23, x14
L_numberSeries_48:
    mov x14, x23
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Fill x20 from [fp-104]
    ldr x20, [x29, #-104]
    mov x24, x20
    // Spill x24 to [fp-64]
    str x24, [x29, #-64]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x24 from [fp-64]
    ldr x24, [x29, #-64]
    cmp x14, x24
    cset x10, ge
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    cmp x10, #0
    b.ne L_numberSeries_47
L_numberSeries_49:
    mov x10, #64
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x23, x10
    mov x10, #2
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x24, x10
    // Spill x24 to [fp-64]
    str x24, [x29, #-64]
    b L_numberSeries_51
L_numberSeries_50:
    adrp x10, str_numberSeries_34
    add x10, x10, :lo12:str_numberSeries_34
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    mov x14, x23
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x0, x14
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    // Spill x26 to [fp-72]
    str x26, [x29, #-72]
    mov x26, x0 // Get result
    // Spill x26 to [fp-72]
    str x26, [x29, #-72]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    // Fill x26 from [fp-72]
    ldr x26, [x29, #-72]
    mov x1, x26
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x14, x0 // Get result
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spilling caller-saved registers before call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x0, x14
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x14, x23
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    mov x26, #2
    // Spill x26 to [fp-72]
    str x26, [x29, #-72]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x26 from [fp-72]
    ldr x26, [x29, #-72]
    sdiv x10, x14, x26
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x23, x10
L_numberSeries_51:
    mov x10, x23
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x24 from [fp-64]
    ldr x24, [x29, #-64]
    mov x26, x24
    // Spill x26 to [fp-72]
    str x26, [x29, #-72]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x26 from [fp-72]
    ldr x26, [x29, #-72]
    cmp x10, x26
    cset x14, ge
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    cmp x14, #0
    b.ne L_numberSeries_50
L_numberSeries_52:
    mov x14, #32
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x23, x14
    mov x14, #1
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x26, x14
    // Spill x26 to [fp-72]
    str x26, [x29, #-72]
    b L_numberSeries_54
L_numberSeries_53:
    adrp x14, str_numberSeries_35
    add x14, x14, :lo12:str_numberSeries_35
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    mov x10, x23
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    // Spill x27 to [fp-80]
    str x27, [x29, #-80]
    mov x27, x0 // Get result
    // Spill x27 to [fp-80]
    str x27, [x29, #-80]
    // Spilling caller-saved registers before call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x0, x14
    // Fill x27 from [fp-80]
    ldr x27, [x29, #-80]
    mov x1, x27
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x10, x0 // Get result
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x10, x23
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    mov x27, #2
    // Spill x27 to [fp-80]
    str x27, [x29, #-80]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x27 from [fp-80]
    ldr x27, [x29, #-80]
    sdiv x14, x10, x27
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x23, x14
L_numberSeries_54:
    mov x14, x23
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Fill x26 from [fp-72]
    ldr x26, [x29, #-72]
    mov x27, x26
    // Spill x27 to [fp-80]
    str x27, [x29, #-80]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x27 from [fp-80]
    ldr x27, [x29, #-80]
    cmp x14, x27
    cset x10, ge
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    cmp x10, #0
    b.ne L_numberSeries_53
L_numberSeries_55:
    adrp x10, str_numberSeries_8
    add x10, x10, :lo12:str_numberSeries_8
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    adrp x10, str_numberSeries_36
    add x10, x10, :lo12:str_numberSeries_36
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    adrp x10, str_numberSeries_37
    add x10, x10, :lo12:str_numberSeries_37
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x10, #1
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x27, x10
    // Spill x27 to [fp-80]
    str x27, [x29, #-80]
    mov x10, #64
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x14, x10
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    b L_numberSeries_57
L_numberSeries_56:
    adrp x10, str_numberSeries_38
    add x10, x10, :lo12:str_numberSeries_38
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x27 from [fp-80]
    ldr x27, [x29, #-80]
    // Spill x21 to [fp-112]
    str x21, [x29, #-112]
    mov x21, x27
    // Spill x21 to [fp-112]
    str x21, [x29, #-112]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x21 from [fp-112]
    ldr x21, [x29, #-112]
    mov x0, x21
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    // Spill x22 to [fp-120]
    str x22, [x29, #-120]
    mov x22, x0 // Get result
    // Spill x22 to [fp-120]
    str x22, [x29, #-120]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    // Fill x22 from [fp-120]
    ldr x22, [x29, #-120]
    mov x1, x22
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x21, x0 // Get result
    // Spill x21 to [fp-112]
    str x21, [x29, #-112]
    adrp x22, str_numberSeries_39
    add x22, x22, :lo12:str_numberSeries_39
    // Spill x22 to [fp-120]
    str x22, [x29, #-120]
    // Spilling caller-saved registers before call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x21 from [fp-112]
    ldr x21, [x29, #-112]
    mov x0, x21
    // Fill x22 from [fp-120]
    ldr x22, [x29, #-120]
    mov x1, x22
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x10, x0 // Get result
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x27 from [fp-80]
    ldr x27, [x29, #-80]
    mov x22, x27
    // Spill x22 to [fp-120]
    str x22, [x29, #-120]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x22 from [fp-120]
    ldr x22, [x29, #-120]
    mov x0, x22
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x21, x0 // Get result
    // Spill x21 to [fp-112]
    str x21, [x29, #-112]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    // Fill x21 from [fp-112]
    ldr x21, [x29, #-112]
    mov x1, x21
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x22, x0 // Get result
    // Spill x22 to [fp-120]
    str x22, [x29, #-120]
    // Spilling caller-saved registers before call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x22 from [fp-120]
    ldr x22, [x29, #-120]
    mov x0, x22
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    // Fill x27 from [fp-80]
    ldr x27, [x29, #-80]
    mov x22, x27
    // Spill x22 to [fp-120]
    str x22, [x29, #-120]
    mov x21, #2
    // Spill x21 to [fp-112]
    str x21, [x29, #-112]
    // Fill x22 from [fp-120]
    ldr x22, [x29, #-120]
    // Fill x21 from [fp-112]
    ldr x21, [x29, #-112]
    mul x10, x22, x21
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x27 from [fp-80]
    ldr x27, [x29, #-80]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x27, x10
    // Spill x27 to [fp-80]
    str x27, [x29, #-80]
L_numberSeries_57:
    // Fill x27 from [fp-80]
    ldr x27, [x29, #-80]
    mov x10, x27
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    mov x21, x14
    // Spill x21 to [fp-112]
    str x21, [x29, #-112]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x21 from [fp-112]
    ldr x21, [x29, #-112]
    cmp x10, x21
    cset x22, le
    // Spill x22 to [fp-120]
    str x22, [x29, #-120]
    // Fill x22 from [fp-120]
    ldr x22, [x29, #-120]
    cmp x22, #0
    b.ne L_numberSeries_56
L_numberSeries_58:
    mov x22, #1
    // Spill x22 to [fp-120]
    str x22, [x29, #-120]
    // Fill x27 from [fp-80]
    ldr x27, [x29, #-80]
    // Fill x22 from [fp-120]
    ldr x22, [x29, #-120]
    mov x27, x22
    // Spill x27 to [fp-80]
    str x27, [x29, #-80]
    mov x22, #64
    // Spill x22 to [fp-120]
    str x22, [x29, #-120]
    // Fill x22 from [fp-120]
    ldr x22, [x29, #-120]
    mov x21, x22
    // Spill x21 to [fp-112]
    str x21, [x29, #-112]
    b L_numberSeries_60
L_numberSeries_59:
    adrp x22, str_numberSeries_40
    add x22, x22, :lo12:str_numberSeries_40
    // Spill x22 to [fp-120]
    str x22, [x29, #-120]
    // Fill x27 from [fp-80]
    ldr x27, [x29, #-80]
    mov x10, x27
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    // Spill x28 to [fp-88]
    str x28, [x29, #-88]
    mov x28, x0 // Get result
    // Spill x28 to [fp-88]
    str x28, [x29, #-88]
    // Spilling caller-saved registers before call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x22 from [fp-120]
    ldr x22, [x29, #-120]
    mov x0, x22
    // Fill x28 from [fp-88]
    ldr x28, [x29, #-88]
    mov x1, x28
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x10, x0 // Get result
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    // Fill x27 from [fp-80]
    ldr x27, [x29, #-80]
    mov x10, x27
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    mov x28, #2
    // Spill x28 to [fp-88]
    str x28, [x29, #-88]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x28 from [fp-88]
    ldr x28, [x29, #-88]
    mul x22, x10, x28
    // Spill x22 to [fp-120]
    str x22, [x29, #-120]
    // Fill x27 from [fp-80]
    ldr x27, [x29, #-80]
    // Fill x22 from [fp-120]
    ldr x22, [x29, #-120]
    mov x27, x22
    // Spill x27 to [fp-80]
    str x27, [x29, #-80]
L_numberSeries_60:
    // Fill x27 from [fp-80]
    ldr x27, [x29, #-80]
    mov x22, x27
    // Spill x22 to [fp-120]
    str x22, [x29, #-120]
    // Fill x21 from [fp-112]
    ldr x21, [x29, #-112]
    mov x28, x21
    // Spill x28 to [fp-88]
    str x28, [x29, #-88]
    // Fill x22 from [fp-120]
    ldr x22, [x29, #-120]
    // Fill x28 from [fp-88]
    ldr x28, [x29, #-88]
    cmp x22, x28
    cset x10, le
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    cmp x10, #0
    b.ne L_numberSeries_59
L_numberSeries_61:
    adrp x10, str_numberSeries_41
    add x10, x10, :lo12:str_numberSeries_41
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x10, #10
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x28, x10
    // Spill x28 to [fp-88]
    str x28, [x29, #-88]
    mov x10, #1
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x22, x10
    // Spill x22 to [fp-120]
    str x22, [x29, #-120]
    b L_numberSeries_63
L_numberSeries_62:
    adrp x10, str_numberSeries_42
    add x10, x10, :lo12:str_numberSeries_42
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x28 from [fp-88]
    ldr x28, [x29, #-88]
    // Spill x25 to [fp-96]
    str x25, [x29, #-96]
    mov x25, x28
    // Spill x25 to [fp-96]
    str x25, [x29, #-96]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x25 from [fp-96]
    ldr x25, [x29, #-96]
    mov x0, x25
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    // Spill x20 to [fp-104]
    str x20, [x29, #-104]
    mov x20, x0 // Get result
    // Spill x20 to [fp-104]
    str x20, [x29, #-104]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    // Fill x20 from [fp-104]
    ldr x20, [x29, #-104]
    mov x1, x20
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x25, x0 // Get result
    // Spill x25 to [fp-96]
    str x25, [x29, #-96]
    // Spilling caller-saved registers before call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x25 from [fp-96]
    ldr x25, [x29, #-96]
    mov x0, x25
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    // Fill x28 from [fp-88]
    ldr x28, [x29, #-88]
    mov x25, x28
    // Spill x25 to [fp-96]
    str x25, [x29, #-96]
    mov x20, #-1
    // Spill x20 to [fp-104]
    str x20, [x29, #-104]
    // Fill x25 from [fp-96]
    ldr x25, [x29, #-96]
    // Fill x20 from [fp-104]
    ldr x20, [x29, #-104]
    add x10, x25, x20
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x28 from [fp-88]
    ldr x28, [x29, #-88]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x28, x10
    // Spill x28 to [fp-88]
    str x28, [x29, #-88]
L_numberSeries_63:
    // Fill x28 from [fp-88]
    ldr x28, [x29, #-88]
    mov x10, x28
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x22 from [fp-120]
    ldr x22, [x29, #-120]
    mov x20, x22
    // Spill x20 to [fp-104]
    str x20, [x29, #-104]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x20 from [fp-104]
    ldr x20, [x29, #-104]
    cmp x10, x20
    cset x25, ge
    // Spill x25 to [fp-96]
    str x25, [x29, #-96]
    // Fill x25 from [fp-96]
    ldr x25, [x29, #-96]
    cmp x25, #0
    b.ne L_numberSeries_62
L_numberSeries_64:
    mov x25, #10
    // Spill x25 to [fp-96]
    str x25, [x29, #-96]
    // Fill x28 from [fp-88]
    ldr x28, [x29, #-88]
    // Fill x25 from [fp-96]
    ldr x25, [x29, #-96]
    mov x28, x25
    // Spill x28 to [fp-88]
    str x28, [x29, #-88]
    mov x25, #1
    // Spill x25 to [fp-96]
    str x25, [x29, #-96]
    // Fill x25 from [fp-96]
    ldr x25, [x29, #-96]
    mov x20, x25
    // Spill x20 to [fp-104]
    str x20, [x29, #-104]
    b L_numberSeries_66
L_numberSeries_65:
    adrp x25, str_numberSeries_43
    add x25, x25, :lo12:str_numberSeries_43
    // Spill x25 to [fp-96]
    str x25, [x29, #-96]
    // Fill x28 from [fp-88]
    ldr x28, [x29, #-88]
    mov x10, x28
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    // Spill x24 to [fp-64]
    str x24, [x29, #-64]
    mov x24, x0 // Get result
    // Spill x24 to [fp-64]
    str x24, [x29, #-64]
    // Spilling caller-saved registers before call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x25 from [fp-96]
    ldr x25, [x29, #-96]
    mov x0, x25
    // Fill x24 from [fp-64]
    ldr x24, [x29, #-64]
    mov x1, x24
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x10, x0 // Get result
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    // Fill x28 from [fp-88]
    ldr x28, [x29, #-88]
    mov x10, x28
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    mov x24, #1
    // Spill x24 to [fp-64]
    str x24, [x29, #-64]
    // Fill x24 from [fp-64]
    ldr x24, [x29, #-64]
    neg x25, x24
    // Spill x25 to [fp-96]
    str x25, [x29, #-96]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x25 from [fp-96]
    ldr x25, [x29, #-96]
    add x24, x10, x25
    // Spill x24 to [fp-64]
    str x24, [x29, #-64]
    // Fill x28 from [fp-88]
    ldr x28, [x29, #-88]
    // Fill x24 from [fp-64]
    ldr x24, [x29, #-64]
    mov x28, x24
    // Spill x28 to [fp-88]
    str x28, [x29, #-88]
L_numberSeries_66:
    // Fill x28 from [fp-88]
    ldr x28, [x29, #-88]
    mov x24, x28
    // Spill x24 to [fp-64]
    str x24, [x29, #-64]
    // Fill x20 from [fp-104]
    ldr x20, [x29, #-104]
    mov x25, x20
    // Spill x25 to [fp-96]
    str x25, [x29, #-96]
    // Fill x24 from [fp-64]
    ldr x24, [x29, #-64]
    // Fill x25 from [fp-96]
    ldr x25, [x29, #-96]
    cmp x24, x25
    cset x10, ge
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    cmp x10, #0
    b.ne L_numberSeries_65
L_numberSeries_67:
    adrp x10, str_numberSeries_44
    add x10, x10, :lo12:str_numberSeries_44
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x10, #1
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x25, x10
    // Spill x25 to [fp-96]
    str x25, [x29, #-96]
    mov x10, #50
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x24, x10
    // Spill x24 to [fp-64]
    str x24, [x29, #-64]
    b L_numberSeries_69
L_numberSeries_68:
    adrp x10, str_numberSeries_45
    add x10, x10, :lo12:str_numberSeries_45
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x25 from [fp-96]
    ldr x25, [x29, #-96]
    // Spill x23 to [fp-128]
    str x23, [x29, #-128]
    mov x23, x25
    // Spill x23 to [fp-128]
    str x23, [x29, #-128]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x23 from [fp-128]
    ldr x23, [x29, #-128]
    mov x0, x23
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    // Spill x26 to [fp-72]
    str x26, [x29, #-72]
    mov x26, x0 // Get result
    // Spill x26 to [fp-72]
    str x26, [x29, #-72]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    // Fill x26 from [fp-72]
    ldr x26, [x29, #-72]
    mov x1, x26
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x23, x0 // Get result
    // Spill x23 to [fp-128]
    str x23, [x29, #-128]
    // Spilling caller-saved registers before call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x23 from [fp-128]
    ldr x23, [x29, #-128]
    mov x0, x23
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    // Fill x25 from [fp-96]
    ldr x25, [x29, #-96]
    mov x23, x25
    // Spill x23 to [fp-128]
    str x23, [x29, #-128]
    mov x26, #2
    // Spill x26 to [fp-72]
    str x26, [x29, #-72]
    mov x10, #1
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x27 to [fp-80]
    str x27, [x29, #-80]
    // Fill x26 from [fp-72]
    ldr x26, [x29, #-72]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    add x27, x26, x10
    // Spill x27 to [fp-80]
    str x27, [x29, #-80]
    // Fill x23 from [fp-128]
    ldr x23, [x29, #-128]
    // Fill x27 from [fp-80]
    ldr x27, [x29, #-80]
    mul x10, x23, x27
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x25 from [fp-96]
    ldr x25, [x29, #-96]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x25, x10
    // Spill x25 to [fp-96]
    str x25, [x29, #-96]
L_numberSeries_69:
    // Fill x25 from [fp-96]
    ldr x25, [x29, #-96]
    mov x10, x25
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x24 from [fp-64]
    ldr x24, [x29, #-64]
    mov x27, x24
    // Spill x27 to [fp-80]
    str x27, [x29, #-80]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x27 from [fp-80]
    ldr x27, [x29, #-80]
    cmp x10, x27
    cset x23, le
    // Spill x23 to [fp-128]
    str x23, [x29, #-128]
    // Fill x23 from [fp-128]
    ldr x23, [x29, #-128]
    cmp x23, #0
    b.ne L_numberSeries_68
L_numberSeries_70:
    mov x23, #1
    // Spill x23 to [fp-128]
    str x23, [x29, #-128]
    // Fill x25 from [fp-96]
    ldr x25, [x29, #-96]
    // Fill x23 from [fp-128]
    ldr x23, [x29, #-128]
    mov x25, x23
    // Spill x25 to [fp-96]
    str x25, [x29, #-96]
    mov x23, #50
    // Spill x23 to [fp-128]
    str x23, [x29, #-128]
    // Fill x23 from [fp-128]
    ldr x23, [x29, #-128]
    mov x27, x23
    // Spill x27 to [fp-80]
    str x27, [x29, #-80]
    b L_numberSeries_72
L_numberSeries_71:
    adrp x23, str_numberSeries_46
    add x23, x23, :lo12:str_numberSeries_46
    // Spill x23 to [fp-128]
    str x23, [x29, #-128]
    // Fill x25 from [fp-96]
    ldr x25, [x29, #-96]
    mov x10, x25
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x26, x0 // Get result
    // Spill x26 to [fp-72]
    str x26, [x29, #-72]
    // Spilling caller-saved registers before call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x23 from [fp-128]
    ldr x23, [x29, #-128]
    mov x0, x23
    // Fill x26 from [fp-72]
    ldr x26, [x29, #-72]
    mov x1, x26
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x10, x0 // Get result
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x0, x10
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    // Fill x25 from [fp-96]
    ldr x25, [x29, #-96]
    mov x10, x25
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    mov x26, #2
    // Spill x26 to [fp-72]
    str x26, [x29, #-72]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x26 from [fp-72]
    ldr x26, [x29, #-72]
    mul x23, x10, x26
    // Spill x23 to [fp-128]
    str x23, [x29, #-128]
    mov x26, #1
    // Spill x26 to [fp-72]
    str x26, [x29, #-72]
    // Fill x23 from [fp-128]
    ldr x23, [x29, #-128]
    // Fill x26 from [fp-72]
    ldr x26, [x29, #-72]
    add x10, x23, x26
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x25 from [fp-96]
    ldr x25, [x29, #-96]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x25, x10
    // Spill x25 to [fp-96]
    str x25, [x29, #-96]
L_numberSeries_72:
    // Fill x25 from [fp-96]
    ldr x25, [x29, #-96]
    mov x10, x25
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Fill x27 from [fp-80]
    ldr x27, [x29, #-80]
    mov x26, x27
    // Spill x26 to [fp-72]
    str x26, [x29, #-72]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x26 from [fp-72]
    ldr x26, [x29, #-72]
    cmp x10, x26
    cset x23, le
    // Spill x23 to [fp-128]
    str x23, [x29, #-128]
    // Fill x23 from [fp-128]
    ldr x23, [x29, #-128]
    cmp x23, #0
    b.ne L_numberSeries_71
L_numberSeries_73:
    adrp x23, str_numberSeries_47
    add x23, x23, :lo12:str_numberSeries_47
    // Spill x23 to [fp-128]
    str x23, [x29, #-128]
    // Spilling caller-saved registers before call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x23 from [fp-128]
    ldr x23, [x29, #-128]
    mov x0, x23
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x23, #1
    // Spill x23 to [fp-128]
    str x23, [x29, #-128]
    // Fill x23 from [fp-128]
    ldr x23, [x29, #-128]
    mov x26, x23
    // Spill x26 to [fp-72]
    str x26, [x29, #-72]
    mov x23, #20
    // Spill x23 to [fp-128]
    str x23, [x29, #-128]
    // Fill x23 from [fp-128]
    ldr x23, [x29, #-128]
    mov x10, x23
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    b L_numberSeries_75
L_numberSeries_74:
    adrp x23, str_numberSeries_48
    add x23, x23, :lo12:str_numberSeries_48
    // Spill x23 to [fp-128]
    str x23, [x29, #-128]
    // Fill x26 from [fp-72]
    ldr x26, [x29, #-72]
    // Spill x21 to [fp-112]
    str x21, [x29, #-112]
    mov x21, x26
    // Spill x21 to [fp-112]
    str x21, [x29, #-112]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x21 from [fp-112]
    ldr x21, [x29, #-112]
    mov x0, x21
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    // Spill x22 to [fp-120]
    str x22, [x29, #-120]
    mov x22, x0 // Get result
    // Spill x22 to [fp-120]
    str x22, [x29, #-120]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x23 from [fp-128]
    ldr x23, [x29, #-128]
    mov x0, x23
    // Fill x22 from [fp-120]
    ldr x22, [x29, #-120]
    mov x1, x22
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x21, x0 // Get result
    // Spill x21 to [fp-112]
    str x21, [x29, #-112]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x21 from [fp-112]
    ldr x21, [x29, #-112]
    mov x0, x21
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    // Fill x26 from [fp-72]
    ldr x26, [x29, #-72]
    mov x21, x26
    // Spill x21 to [fp-112]
    str x21, [x29, #-112]
    mov x22, #1
    // Spill x22 to [fp-120]
    str x22, [x29, #-120]
    // Fill x21 from [fp-112]
    ldr x21, [x29, #-112]
    // Fill x22 from [fp-120]
    ldr x22, [x29, #-120]
    add x23, x21, x22
    // Spill x23 to [fp-128]
    str x23, [x29, #-128]
    // Fill x26 from [fp-72]
    ldr x26, [x29, #-72]
    // Fill x23 from [fp-128]
    ldr x23, [x29, #-128]
    mov x26, x23
    // Spill x26 to [fp-72]
    str x26, [x29, #-72]
L_numberSeries_75:
    // Fill x26 from [fp-72]
    ldr x26, [x29, #-72]
    mov x23, x26
    // Spill x23 to [fp-128]
    str x23, [x29, #-128]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    mov x22, x10
    // Spill x22 to [fp-120]
    str x22, [x29, #-120]
    // Fill x23 from [fp-128]
    ldr x23, [x29, #-128]
    // Fill x22 from [fp-120]
    ldr x22, [x29, #-120]
    cmp x23, x22
    cset x21, le
    // Spill x21 to [fp-112]
    str x21, [x29, #-112]
    // Fill x21 from [fp-112]
    ldr x21, [x29, #-112]
    cmp x21, #0
    b.ne L_numberSeries_74
L_numberSeries_76:
    mov x21, #1
    // Spill x21 to [fp-112]
    str x21, [x29, #-112]
    // Fill x26 from [fp-72]
    ldr x26, [x29, #-72]
    // Fill x21 from [fp-112]
    ldr x21, [x29, #-112]
    mov x26, x21
    // Spill x26 to [fp-72]
    str x26, [x29, #-72]
    mov x21, #20
    // Spill x21 to [fp-112]
    str x21, [x29, #-112]
    // Fill x21 from [fp-112]
    ldr x21, [x29, #-112]
    mov x22, x21
    // Spill x22 to [fp-120]
    str x22, [x29, #-120]
    b L_numberSeries_78
L_numberSeries_77:
    adrp x21, str_numberSeries_49
    add x21, x21, :lo12:str_numberSeries_49
    // Spill x21 to [fp-112]
    str x21, [x29, #-112]
    // Fill x26 from [fp-72]
    ldr x26, [x29, #-72]
    mov x23, x26
    // Spill x23 to [fp-128]
    str x23, [x29, #-128]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x23 from [fp-128]
    ldr x23, [x29, #-128]
    mov x0, x23
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    // Spill x28 to [fp-88]
    str x28, [x29, #-88]
    mov x28, x0 // Get result
    // Spill x28 to [fp-88]
    str x28, [x29, #-88]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x21 from [fp-112]
    ldr x21, [x29, #-112]
    mov x0, x21
    // Fill x28 from [fp-88]
    ldr x28, [x29, #-88]
    mov x1, x28
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x23, x0 // Get result
    // Spill x23 to [fp-128]
    str x23, [x29, #-128]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x23 from [fp-128]
    ldr x23, [x29, #-128]
    mov x0, x23
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    // Fill x26 from [fp-72]
    ldr x26, [x29, #-72]
    mov x23, x26
    // Spill x23 to [fp-128]
    str x23, [x29, #-128]
    mov x28, #2
    // Spill x28 to [fp-88]
    str x28, [x29, #-88]
    // Fill x23 from [fp-128]
    ldr x23, [x29, #-128]
    // Fill x28 from [fp-88]
    ldr x28, [x29, #-88]
    add x21, x23, x28
    // Spill x21 to [fp-112]
    str x21, [x29, #-112]
    // Fill x26 from [fp-72]
    ldr x26, [x29, #-72]
    // Fill x21 from [fp-112]
    ldr x21, [x29, #-112]
    mov x26, x21
    // Spill x26 to [fp-72]
    str x26, [x29, #-72]
L_numberSeries_78:
    // Fill x26 from [fp-72]
    ldr x26, [x29, #-72]
    mov x21, x26
    // Spill x21 to [fp-112]
    str x21, [x29, #-112]
    // Fill x22 from [fp-120]
    ldr x22, [x29, #-120]
    mov x28, x22
    // Spill x28 to [fp-88]
    str x28, [x29, #-88]
    // Fill x21 from [fp-112]
    ldr x21, [x29, #-112]
    // Fill x28 from [fp-88]
    ldr x28, [x29, #-88]
    cmp x21, x28
    cset x23, le
    // Spill x23 to [fp-128]
    str x23, [x29, #-128]
    // Fill x23 from [fp-128]
    ldr x23, [x29, #-128]
    cmp x23, #0
    b.ne L_numberSeries_77
L_numberSeries_79:
    mov x23, #20
    // Spill x23 to [fp-128]
    str x23, [x29, #-128]
    // Fill x26 from [fp-72]
    ldr x26, [x29, #-72]
    // Fill x23 from [fp-128]
    ldr x23, [x29, #-128]
    mov x26, x23
    // Spill x26 to [fp-72]
    str x26, [x29, #-72]
    mov x23, #2
    // Spill x23 to [fp-128]
    str x23, [x29, #-128]
    // Fill x23 from [fp-128]
    ldr x23, [x29, #-128]
    mov x28, x23
    // Spill x28 to [fp-88]
    str x28, [x29, #-88]
    b L_numberSeries_81
L_numberSeries_80:
    adrp x23, str_numberSeries_50
    add x23, x23, :lo12:str_numberSeries_50
    // Spill x23 to [fp-128]
    str x23, [x29, #-128]
    // Fill x26 from [fp-72]
    ldr x26, [x29, #-72]
    mov x21, x26
    // Spill x21 to [fp-112]
    str x21, [x29, #-112]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x21 from [fp-112]
    ldr x21, [x29, #-112]
    mov x0, x21
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    // Spill x20 to [fp-104]
    str x20, [x29, #-104]
    mov x20, x0 // Get result
    // Spill x20 to [fp-104]
    str x20, [x29, #-104]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x23 from [fp-128]
    ldr x23, [x29, #-128]
    mov x0, x23
    // Fill x20 from [fp-104]
    ldr x20, [x29, #-104]
    mov x1, x20
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    mov x21, x0 // Get result
    // Spill x21 to [fp-112]
    str x21, [x29, #-112]
    // Spilling caller-saved registers before call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Spill x10 to [fp-16]
    str x10, [x29, #-16]
    // Spill x11 to [fp-24]
    str x11, [x29, #-24]
    // Spill x12 to [fp-32]
    str x12, [x29, #-32]
    // Spill x13 to [fp-40]
    str x13, [x29, #-40]
    // Spill x14 to [fp-48]
    str x14, [x29, #-48]
    // Spill x15 to [fp-56]
    str x15, [x29, #-56]
    // Spill x9 to [fp-8]
    str x9, [x29, #-8]
    // Fill x21 from [fp-112]
    ldr x21, [x29, #-112]
    mov x0, x21
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x10, x11, x12, x13, x14, x15, x2, x3, x4, x5, x6, x7, x9]
    // Fill x10 from [fp-16]
    ldr x10, [x29, #-16]
    // Fill x11 from [fp-24]
    ldr x11, [x29, #-24]
    // Fill x12 from [fp-32]
    ldr x12, [x29, #-32]
    // Fill x13 from [fp-40]
    ldr x13, [x29, #-40]
    // Fill x14 from [fp-48]
    ldr x14, [x29, #-48]
    // Fill x15 from [fp-56]
    ldr x15, [x29, #-56]
    // Fill x9 from [fp-8]
    ldr x9, [x29, #-8]
    // Fill x26 from [fp-72]
    ldr x26, [x29, #-72]
    mov x21, x26
    // Spill x21 to [fp-112]
    str x21, [x29, #-112]
    mov x20, #2
    // Spill x20 to [fp-104]
    str x20, [x29, #-104]
    // Fill x20 from [fp-104]
    ldr x20, [x29, #-104]
    neg x23, x20
    // Spill x23 to [fp-128]
    str x23, [x29, #-128]
    // Fill x21 from [fp-112]
    ldr x21, [x29, #-112]
    // Fill x23 from [fp-128]
    ldr x23, [x29, #-128]
    add x20, x21, x23
    // Spill x20 to [fp-104]
    str x20, [x29, #-104]
    // Fill x26 from [fp-72]
    ldr x26, [x29, #-72]
    // Fill x20 from [fp-104]
    ldr x20, [x29, #-104]
    mov x26, x20
    // Spill x26 to [fp-72]
    str x26, [x29, #-72]
L_numberSeries_81:
    // Fill x26 from [fp-72]
    ldr x26, [x29, #-72]
    mov x20, x26
    // Spill x20 to [fp-104]
    str x20, [x29, #-104]
    // Fill x28 from [fp-88]
    ldr x28, [x29, #-88]
    mov x23, x28
    // Spill x23 to [fp-128]
    str x23, [x29, #-128]
    // Fill x20 from [fp-104]
    ldr x20, [x29, #-104]
    // Fill x23 from [fp-128]
    ldr x23, [x29, #-128]
    cmp x20, x23
    cset x21, ge
    // Spill x21 to [fp-112]
    str x21, [x29, #-112]
    // Fill x21 from [fp-112]
    ldr x21, [x29, #-112]
    cmp x21, #0
    b.ne L_numberSeries_80
L_numberSeries_82:
    mov x21, #0
    // Spill x21 to [fp-112]
    str x21, [x29, #-112]
    mov x0, x21
    // Restoring callee-saved registers: [x19, x20, x21, x22, x23, x24, x25, x26, x27, x28]
    ldp x27, x28, [x29, #-96]
    ldp x25, x26, [x29, #-80]
    ldp x23, x24, [x29, #-64]
    ldp x21, x22, [x29, #-48]
    ldp x19, x20, [x29, #-32]
    mov sp, x29
    ldp x29, x30, [sp], #16
    ret
    .data
str_interactiveCalculator_0: .asciz "=== Interactive Calculator ==="
str_interactiveCalculator_1: .asciz "Enter first number:"
input_type_interactiveCalculator_2: .asciz "int"
str_interactiveCalculator_3: .asciz "Enter second number:"
input_type_interactiveCalculator_4: .asciz "int"
str_interactiveCalculator_5: .asciz "Enter operation (+, -, *, /):"
input_type_interactiveCalculator_6: .asciz "string"
str_interactiveCalculator_7: .asciz ""
str_interactiveCalculator_8: .asciz "Calculation Result:"
str_interactiveCalculator_9: .asciz " "
str_interactiveCalculator_10: .asciz " = "
str_interactiveCalculator_11: .asciz "Operation: "
    .text
    .global interactiveCalculator
interactiveCalculator:
stp x29, x30, [sp, #-16]!
    mov x29, sp
    sub sp, sp, #80
    // Saving callee-saved registers: [x19, x20, x21, x22, x23, x24, x25, x26, x27]
    stp x19, x20, [x29, #-16]
    stp x21, x22, [x29, #-32]
    stp x23, x24, [x29, #-48]
    stp x25, x26, [x29, #-64]
    str x27, [x29, #-80]
    mov x19, x0 // Copy 'this' pointer
    adrp x20, str_interactiveCalculator_0
    add x20, x20, :lo12:str_interactiveCalculator_0
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    adrp x20, str_interactiveCalculator_1
    add x20, x20, :lo12:str_interactiveCalculator_1
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    adrp x0, input_type_interactiveCalculator_2
    add x0, x0, :lo12:input_type_interactiveCalculator_2
    bl runtime_read_input // Call runtime helper (expects type* in x0)
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    mov x20, x0 // Get input result
    mov x21, x20
    adrp x20, str_interactiveCalculator_3
    add x20, x20, :lo12:str_interactiveCalculator_3
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    adrp x0, input_type_interactiveCalculator_4
    add x0, x0, :lo12:input_type_interactiveCalculator_4
    bl runtime_read_input // Call runtime helper (expects type* in x0)
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    mov x20, x0 // Get input result
    mov x22, x20
    adrp x20, str_interactiveCalculator_5
    add x20, x20, :lo12:str_interactiveCalculator_5
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    adrp x0, input_type_interactiveCalculator_6
    add x0, x0, :lo12:input_type_interactiveCalculator_6
    bl runtime_read_input // Call runtime helper (expects type* in x0)
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    mov x20, x0 // Get input result
    mov x23, x20
    mov x20, x21
    mov x24, x22
    mov x25, x23
    // Spilling caller-saved registers before call: [x4, x5, x6, x7]
    mov x1, x20 // Setup arg 1
    mov x2, x24 // Setup arg 2
    mov x3, x25 // Setup arg 3
    mov x0, x19 // Setup 'this' pointer
    bl calculate
    // Filling caller-saved registers after call: [x4, x5, x6, x7]
    mov x25, x0 // Get result slot 0
    mov x24, x1 // Get result slot 1
    mov x20, x24
    mov x24, x25
    adrp x25, str_interactiveCalculator_7
    add x25, x25, :lo12:str_interactiveCalculator_7
    // Spilling caller-saved registers before call: [x4, x5, x6, x7]
    mov x0, x25
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x4, x5, x6, x7]
    adrp x25, str_interactiveCalculator_8
    add x25, x25, :lo12:str_interactiveCalculator_8
    // Spilling caller-saved registers before call: [x4, x5, x6, x7]
    mov x0, x25
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x4, x5, x6, x7]
    mov x25, x21
    adrp x26, str_interactiveCalculator_9
    add x26, x26, :lo12:str_interactiveCalculator_9
    // Spilling caller-saved registers before call: [x4, x5, x6, x7]
    mov x0, x25
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x4, x5, x6, x7]
    mov x27, x0 // Get result
    // Spilling caller-saved registers before call: [x4, x5, x6, x7]
    mov x0, x26
    mov x1, x27
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x4, x5, x6, x7]
    mov x25, x0 // Get result
    mov x27, x23
    // Spilling caller-saved registers before call: [x4, x5, x6, x7]
    mov x0, x27
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x4, x5, x6, x7]
    mov x26, x0 // Get result
    // Spilling caller-saved registers before call: [x4, x5, x6, x7]
    mov x0, x25
    mov x1, x26
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x4, x5, x6, x7]
    mov x27, x0 // Get result
    adrp x26, str_interactiveCalculator_9
    add x26, x26, :lo12:str_interactiveCalculator_9
    // Spilling caller-saved registers before call: [x4, x5, x6, x7]
    mov x0, x27
    mov x1, x26
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x4, x5, x6, x7]
    mov x25, x0 // Get result
    mov x26, x22
    // Spilling caller-saved registers before call: [x4, x5, x6, x7]
    mov x0, x26
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x4, x5, x6, x7]
    mov x27, x0 // Get result
    // Spilling caller-saved registers before call: [x4, x5, x6, x7]
    mov x0, x25
    mov x1, x27
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x4, x5, x6, x7]
    mov x26, x0 // Get result
    adrp x27, str_interactiveCalculator_10
    add x27, x27, :lo12:str_interactiveCalculator_10
    // Spilling caller-saved registers before call: [x4, x5, x6, x7]
    mov x0, x26
    mov x1, x27
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x4, x5, x6, x7]
    mov x25, x0 // Get result
    mov x27, x24
    // Spilling caller-saved registers before call: [x4, x5, x6, x7]
    mov x0, x27
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x4, x5, x6, x7]
    mov x26, x0 // Get result
    // Spilling caller-saved registers before call: [x4, x5, x6, x7]
    mov x0, x25
    mov x1, x26
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x4, x5, x6, x7]
    mov x27, x0 // Get result
    // Spilling caller-saved registers before call: [x4, x5, x6, x7]
    mov x0, x27
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x4, x5, x6, x7]
    adrp x27, str_interactiveCalculator_11
    add x27, x27, :lo12:str_interactiveCalculator_11
    mov x26, x20
    // Spilling caller-saved registers before call: [x4, x5, x6, x7]
    mov x0, x26
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x4, x5, x6, x7]
    mov x25, x0 // Get result
    // Spilling caller-saved registers before call: [x4, x5, x6, x7]
    mov x0, x27
    mov x1, x25
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x4, x5, x6, x7]
    mov x26, x0 // Get result
    // Spilling caller-saved registers before call: [x4, x5, x6, x7]
    mov x0, x26
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x4, x5, x6, x7]
    mov x26, #0
    mov x0, x26
    // Restoring callee-saved registers: [x19, x20, x21, x22, x23, x24, x25, x26, x27]
    ldr x27, [x29, #-72]
    ldp x25, x26, [x29, #-56]
    ldp x23, x24, [x29, #-40]
    ldp x21, x22, [x29, #-24]
    ldp x19, x20, [x29, #-8]
    mov sp, x29
    ldp x29, x30, [sp], #16
    ret
    .data
str_demonstrateArrays_0: .asciz "=== Array Demo ==="
str_demonstrateArrays_1: .asciz "Array: "
str_demonstrateArrays_2: .asciz "First element: "
str_demonstrateArrays_3: .asciz "Last element: "
str_demonstrateArrays_4: .asciz "After modification: "
    .text
    .global demonstrateArrays
demonstrateArrays:
stp x29, x30, [sp, #-16]!
    mov x29, sp
    sub sp, sp, #48
    // Saving callee-saved registers: [x19, x20, x21, x22, x23, x24]
    stp x19, x20, [x29, #-16]
    stp x21, x22, [x29, #-32]
    stp x23, x24, [x29, #-48]
    mov x19, x0 // Copy 'this' pointer
    adrp x20, str_demonstrateArrays_0
    add x20, x20, :lo12:str_demonstrateArrays_0
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    mov x20, #5
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl array_new // Call runtime helper
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    mov x21, x0 // Get result
    mov x20, x21
    mov x22, #0
    mov x23, #10
    // Spilling caller-saved registers before call: [x3, x4, x5, x6, x7]
    mov x0, x20
    mov x1, x22
    mov x2, x23
    bl array_store // Call runtime helper
    // Filling caller-saved registers after call: [x3, x4, x5, x6, x7]
    mov x23, x21
    mov x22, #1
    mov x20, #20
    // Spilling caller-saved registers before call: [x3, x4, x5, x6, x7]
    mov x0, x23
    mov x1, x22
    mov x2, x20
    bl array_store // Call runtime helper
    // Filling caller-saved registers after call: [x3, x4, x5, x6, x7]
    mov x20, x21
    mov x22, #2
    mov x23, #30
    // Spilling caller-saved registers before call: [x3, x4, x5, x6, x7]
    mov x0, x20
    mov x1, x22
    mov x2, x23
    bl array_store // Call runtime helper
    // Filling caller-saved registers after call: [x3, x4, x5, x6, x7]
    mov x23, x21
    mov x22, #3
    mov x20, #40
    // Spilling caller-saved registers before call: [x3, x4, x5, x6, x7]
    mov x0, x23
    mov x1, x22
    mov x2, x20
    bl array_store // Call runtime helper
    // Filling caller-saved registers after call: [x3, x4, x5, x6, x7]
    mov x20, x21
    mov x22, #4
    mov x23, #50
    // Spilling caller-saved registers before call: [x3, x4, x5, x6, x7]
    mov x0, x20
    mov x1, x22
    mov x2, x23
    bl array_store // Call runtime helper
    // Filling caller-saved registers after call: [x3, x4, x5, x6, x7]
    mov x23, x21
    adrp x21, str_demonstrateArrays_1
    add x21, x21, :lo12:str_demonstrateArrays_1
    mov x22, x23
    // Spilling caller-saved registers before call: [x3, x4, x5, x6, x7]
    mov x0, x22
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x3, x4, x5, x6, x7]
    mov x20, x0 // Get result
    // Spilling caller-saved registers before call: [x3, x4, x5, x6, x7]
    mov x0, x21
    mov x1, x20
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x3, x4, x5, x6, x7]
    mov x22, x0 // Get result
    // Spilling caller-saved registers before call: [x3, x4, x5, x6, x7]
    mov x0, x22
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x3, x4, x5, x6, x7]
    adrp x22, str_demonstrateArrays_2
    add x22, x22, :lo12:str_demonstrateArrays_2
    mov x20, x23
    mov x21, #0
    // Spilling caller-saved registers before call: [x3, x4, x5, x6, x7]
    mov x0, x20
    mov x1, x21
    bl array_load // Call runtime helper
    // Filling caller-saved registers after call: [x3, x4, x5, x6, x7]
    mov x24, x0 // Get result
    // Spilling caller-saved registers before call: [x3, x4, x5, x6, x7]
    mov x0, x24
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x3, x4, x5, x6, x7]
    mov x21, x0 // Get result
    // Spilling caller-saved registers before call: [x3, x4, x5, x6, x7]
    mov x0, x22
    mov x1, x21
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x3, x4, x5, x6, x7]
    mov x24, x0 // Get result
    // Spilling caller-saved registers before call: [x3, x4, x5, x6, x7]
    mov x0, x24
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x3, x4, x5, x6, x7]
    adrp x24, str_demonstrateArrays_3
    add x24, x24, :lo12:str_demonstrateArrays_3
    mov x21, x23
    mov x22, #4
    // Spilling caller-saved registers before call: [x3, x4, x5, x6, x7]
    mov x0, x21
    mov x1, x22
    bl array_load // Call runtime helper
    // Filling caller-saved registers after call: [x3, x4, x5, x6, x7]
    mov x20, x0 // Get result
    // Spilling caller-saved registers before call: [x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x3, x4, x5, x6, x7]
    mov x22, x0 // Get result
    // Spilling caller-saved registers before call: [x3, x4, x5, x6, x7]
    mov x0, x24
    mov x1, x22
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x3, x4, x5, x6, x7]
    mov x20, x0 // Get result
    // Spilling caller-saved registers before call: [x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x3, x4, x5, x6, x7]
    mov x20, x23
    mov x22, #2
    mov x24, #99
    // Spilling caller-saved registers before call: [x3, x4, x5, x6, x7]
    mov x0, x20
    mov x1, x22
    mov x2, x24
    bl array_store // Call runtime helper
    // Filling caller-saved registers after call: [x3, x4, x5, x6, x7]
    adrp x24, str_demonstrateArrays_4
    add x24, x24, :lo12:str_demonstrateArrays_4
    mov x22, x23
    // Spilling caller-saved registers before call: [x3, x4, x5, x6, x7]
    mov x0, x22
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x3, x4, x5, x6, x7]
    mov x20, x0 // Get result
    // Spilling caller-saved registers before call: [x3, x4, x5, x6, x7]
    mov x0, x24
    mov x1, x20
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x3, x4, x5, x6, x7]
    mov x22, x0 // Get result
    // Spilling caller-saved registers before call: [x3, x4, x5, x6, x7]
    mov x0, x22
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x3, x4, x5, x6, x7]
    mov x22, #0
    mov x0, x22
    // Restoring callee-saved registers: [x19, x20, x21, x22, x23, x24]
    ldp x23, x24, [x29, #-64]
    ldp x21, x22, [x29, #-48]
    ldp x19, x20, [x29, #-32]
    mov sp, x29
    ldp x29, x30, [sp], #16
    ret
    .data
str_main_0: .asciz "=== CODERIVE INTERACTIVE DEMO ==="
str_main_1: .asciz ""
str_main_2: .asciz "=== DEMO COMPLETE ==="
str_main_3: .asciz "Thank you for using Coderive!"
    .text
    .global main
main:
stp x29, x30, [sp, #-16]!
    mov x29, sp
    sub sp, sp, #16
    // Saving callee-saved registers: [x19, x20]
    stp x19, x20, [x29, #-16]
    mov x19, x0 // Copy 'this' pointer
    adrp x20, str_main_0
    add x20, x20, :lo12:str_main_0
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    adrp x20, str_main_1
    add x20, x20, :lo12:str_main_1
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x19 // Setup 'this' pointer
    bl demonstrateArrays
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    mov x20, x0 // Get result
    adrp x20, str_main_1
    add x20, x20, :lo12:str_main_1
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x19 // Setup 'this' pointer
    bl getUserInfo
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    mov x20, x0 // Get result
    adrp x20, str_main_1
    add x20, x20, :lo12:str_main_1
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x19 // Setup 'this' pointer
    bl interactiveCalculator
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    mov x20, x0 // Get result
    adrp x20, str_main_1
    add x20, x20, :lo12:str_main_1
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x19 // Setup 'this' pointer
    bl numberSeries
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    mov x20, x0 // Get result
    adrp x20, str_main_1
    add x20, x20, :lo12:str_main_1
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x19 // Setup 'this' pointer
    bl edgeCaseLoops
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    mov x20, x0 // Get result
    adrp x20, str_main_1
    add x20, x20, :lo12:str_main_1
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    adrp x20, str_main_2
    add x20, x20, :lo12:str_main_2
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    adrp x20, str_main_3
    add x20, x20, :lo12:str_main_3
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    mov x20, #0
    mov x0, x20
    // Restoring callee-saved registers: [x19, x20]
    ldp x19, x20, [x29, #-32]
    mov sp, x29
    ldp x29, x30, [sp], #16
    ret
    .data
str_calculate_0: .asciz "+"
str_calculate_1: .asciz "addition"
str_calculate_2: .asciz "-"
str_calculate_3: .asciz "subtraction"
str_calculate_4: .asciz "*"
str_calculate_5: .asciz "multiplication"
str_calculate_6: .asciz "/"
str_calculate_7: .asciz "division"
str_calculate_8: .asciz "unknown"
    .text
    .global calculate
calculate:
stp x29, x30, [sp, #-16]!
    mov x29, sp
    sub sp, sp, #80
    // Saving callee-saved registers: [x19, x20, x21, x22, x23, x24, x25, x26, x27]
    stp x19, x20, [x29, #-16]
    stp x21, x22, [x29, #-32]
    stp x23, x24, [x29, #-48]
    stp x25, x26, [x29, #-64]
    str x27, [x29, #-80]
    mov x19, x0 // Copy 'this' pointer
    mov x20, #0 // WARNING: Used uninitialized local slot 2
    mov x21, x20
    adrp x22, str_calculate_0
    add x22, x22, :lo12:str_calculate_0
    cmp x21, x22
    cset x23, eq
    cmp x23, #0
    b.eq L_calculate_0
    mov x23, #0 // WARNING: Used uninitialized local slot 0
    mov x22, x23
    mov x21, #0 // WARNING: Used uninitialized local slot 1
    mov x24, x21
    add x25, x22, x24
    mov x24, x25
    adrp x25, str_calculate_1
    add x25, x25, :lo12:str_calculate_1
    mov x22, x25
    b L_calculate_7
L_calculate_0:
    mov x25, x20
    adrp x26, str_calculate_2
    add x26, x26, :lo12:str_calculate_2
    cmp x25, x26
    cset x27, eq
    cmp x27, #0
    b.eq L_calculate_1
    mov x27, x23
    mov x26, x21
    sub x25, x27, x26
    mov x24, x25
    adrp x25, str_calculate_3
    add x25, x25, :lo12:str_calculate_3
    mov x22, x25
    b L_calculate_6
L_calculate_1:
    mov x25, x20
    adrp x26, str_calculate_4
    add x26, x26, :lo12:str_calculate_4
    cmp x25, x26
    cset x27, eq
    cmp x27, #0
    b.eq L_calculate_2
    mov x27, x23
    mov x26, x21
    mul x25, x27, x26
    mov x24, x25
    adrp x25, str_calculate_5
    add x25, x25, :lo12:str_calculate_5
    mov x22, x25
    b L_calculate_5
L_calculate_2:
    mov x25, x20
    adrp x26, str_calculate_6
    add x26, x26, :lo12:str_calculate_6
    cmp x25, x26
    cset x27, eq
    cmp x27, #0
    b.eq L_calculate_3
    mov x27, x23
    mov x26, x21
    sdiv x25, x27, x26
    mov x24, x25
    adrp x25, str_calculate_7
    add x25, x25, :lo12:str_calculate_7
    mov x22, x25
    b L_calculate_4
L_calculate_3:
    mov x25, #0
    mov x24, x25
    adrp x25, str_calculate_8
    add x25, x25, :lo12:str_calculate_8
    mov x22, x25
L_calculate_4:
L_calculate_5:
L_calculate_6:
L_calculate_7:
    mov x25, #0
    mov x0, x24
    mov x1, x22
    // Restoring callee-saved registers: [x19, x20, x21, x22, x23, x24, x25, x26, x27]
    ldr x27, [x29, #-72]
    ldp x25, x26, [x29, #-56]
    ldp x23, x24, [x29, #-40]
    ldp x21, x22, [x29, #-24]
    ldp x19, x20, [x29, #-8]
    mov sp, x29
    ldp x29, x30, [sp], #16
    ret
    .data
str_getUserInfo_0: .asciz "=== User Registration ==="
str_getUserInfo_1: .asciz "Enter your name:"
input_type_getUserInfo_2: .asciz "string"
str_getUserInfo_3: .asciz "Enter your age:"
input_type_getUserInfo_4: .asciz "int"
str_getUserInfo_5: .asciz "Enter your height (meters):"
input_type_getUserInfo_6: .asciz "float"
str_getUserInfo_7: .asciz "Are you a student? (true/false)"
input_type_getUserInfo_8: .asciz "bool"
str_getUserInfo_9: .asciz ""
str_getUserInfo_10: .asciz "Registration Complete!"
str_getUserInfo_11: .asciz "Name: "
str_getUserInfo_12: .asciz "Age: "
str_getUserInfo_13: .asciz "Height: "
str_getUserInfo_14: .asciz "m"
str_getUserInfo_15: .asciz "Student: "
str_getUserInfo_16: .asciz "Welcome student "
str_getUserInfo_17: .asciz "!"
str_getUserInfo_18: .asciz "Welcome "
    .text
    .global getUserInfo
getUserInfo:
stp x29, x30, [sp, #-16]!
    mov x29, sp
    sub sp, sp, #64
    // Saving callee-saved registers: [x19, x20, x21, x22, x23, x24, x25, x26]
    stp x19, x20, [x29, #-16]
    stp x21, x22, [x29, #-32]
    stp x23, x24, [x29, #-48]
    stp x25, x26, [x29, #-64]
    mov x19, x0 // Copy 'this' pointer
    adrp x20, str_getUserInfo_0
    add x20, x20, :lo12:str_getUserInfo_0
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    adrp x20, str_getUserInfo_1
    add x20, x20, :lo12:str_getUserInfo_1
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    adrp x0, input_type_getUserInfo_2
    add x0, x0, :lo12:input_type_getUserInfo_2
    bl runtime_read_input // Call runtime helper (expects type* in x0)
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    mov x20, x0 // Get input result
    mov x21, x20
    adrp x20, str_getUserInfo_3
    add x20, x20, :lo12:str_getUserInfo_3
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    adrp x0, input_type_getUserInfo_4
    add x0, x0, :lo12:input_type_getUserInfo_4
    bl runtime_read_input // Call runtime helper (expects type* in x0)
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    mov x20, x0 // Get input result
    mov x22, x20
    adrp x20, str_getUserInfo_5
    add x20, x20, :lo12:str_getUserInfo_5
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    adrp x0, input_type_getUserInfo_6
    add x0, x0, :lo12:input_type_getUserInfo_6
    bl runtime_read_input // Call runtime helper (expects type* in x0)
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    mov x20, x0 // Get input result
    mov x23, x20
    adrp x20, str_getUserInfo_7
    add x20, x20, :lo12:str_getUserInfo_7
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    adrp x0, input_type_getUserInfo_8
    add x0, x0, :lo12:input_type_getUserInfo_8
    bl runtime_read_input // Call runtime helper (expects type* in x0)
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    mov x20, x0 // Get input result
    mov x24, x20
    adrp x20, str_getUserInfo_9
    add x20, x20, :lo12:str_getUserInfo_9
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    adrp x20, str_getUserInfo_10
    add x20, x20, :lo12:str_getUserInfo_10
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    adrp x20, str_getUserInfo_11
    add x20, x20, :lo12:str_getUserInfo_11
    mov x25, x21
    // Spilling caller-saved registers before call: [x1, x2, x3, x4, x5, x6, x7]
    mov x0, x25
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x1, x2, x3, x4, x5, x6, x7]
    mov x26, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x20
    mov x1, x26
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x25, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x25
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    adrp x25, str_getUserInfo_12
    add x25, x25, :lo12:str_getUserInfo_12
    mov x26, x22
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x26
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x20, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x25
    mov x1, x20
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x26, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x26
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    adrp x26, str_getUserInfo_13
    add x26, x26, :lo12:str_getUserInfo_13
    mov x20, x23
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x25, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x26
    mov x1, x25
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x20, x0 // Get result
    adrp x25, str_getUserInfo_14
    add x25, x25, :lo12:str_getUserInfo_14
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x20
    mov x1, x25
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x26, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x26
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    adrp x26, str_getUserInfo_15
    add x26, x26, :lo12:str_getUserInfo_15
    mov x25, x24
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x25
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x20, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x26
    mov x1, x20
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x25, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x25
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x25, x24
    cmp x25, #0
    b.eq L_getUserInfo_0
    adrp x25, str_getUserInfo_16
    add x25, x25, :lo12:str_getUserInfo_16
    mov x20, x21
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x20
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x26, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x25
    mov x1, x26
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x20, x0 // Get result
    adrp x26, str_getUserInfo_17
    add x26, x26, :lo12:str_getUserInfo_17
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x20
    mov x1, x26
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x25, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x25
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    b L_getUserInfo_1
L_getUserInfo_0:
    adrp x25, str_getUserInfo_18
    add x25, x25, :lo12:str_getUserInfo_18
    mov x26, x21
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x26
    bl runtime_int_to_string // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x20, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x25
    mov x1, x20
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x26, x0 // Get result
    adrp x20, str_getUserInfo_17
    add x20, x20, :lo12:str_getUserInfo_17
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x26
    mov x1, x20
    bl string_concat // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
    mov x25, x0 // Get result
    // Spilling caller-saved registers before call: [x2, x3, x4, x5, x6, x7]
    mov x0, x25
    bl runtime_print // Call runtime helper
    // Filling caller-saved registers after call: [x2, x3, x4, x5, x6, x7]
L_getUserInfo_1:
    mov x25, #0
    mov x0, x25
    // Restoring callee-saved registers: [x19, x20, x21, x22, x23, x24, x25, x26]
    ldp x25, x26, [x29, #-80]
    ldp x23, x24, [x29, #-64]
    ldp x21, x22, [x29, #-48]
    ldp x19, x20, [x29, #-32]
    mov sp, x29
    ldp x29, x30, [sp], #16
    ret
