package cod.compiler;

import java.util.*;

import static cod.compiler.MTOTRegistry.AArch64Registers.*;
import static cod.compiler.MTOTRegistry.x86_64Registers.*;

public class MTOTRegistry {

    private static final Map<String, CPUProfile> profiles = new HashMap<String, CPUProfile>();

    // ======================================================================
    // --- REGISTER DEFINITIONS ---
    // ======================================================================

    /** Contains all register definitions for the AArch64 architecture. */
    public static class AArch64Registers {
        // Callee-saved (must be preserved by callee)
        public static final String x19 = "x19",
                x20 = "x20",
                x21 = "x21",
                x22 = "x22",
                x23 = "x23",
                x24 = "x24",
                x25 = "x25",
                x26 = "x26",
                x27 = "x27",
                x28 = "x28",
                x29 = "x29",
                x30 = "x30";

        // Caller-saved (can be clobbered by callee)
        public static final String x9 = "x9",
                x10 = "x10",
                x11 = "x11",
                x12 = "x12",
                x13 = "x13",
                x14 = "x14",
                x15 = "x15";

        // Argument/Return registers (Caller-saved)
        public static final String x0 = "x0",
                x1 = "x1",
                x2 = "x2",
                x3 = "x3",
                x4 = "x4",
                x5 = "x5",
                x6 = "x6",
                x7 = "x7";

        // Special Purpose
        public static final String sp = "sp",
                fp = "x29", // Frame Pointer
                lr = "x30", // Link Register
                zr = "xzr"; // Zero Register

        // Scratch / Intra-procedural (Caller-saved)
        public static final String x16 = "x16", // (IP0)
                x17 = "x17"; // (IP1)

        // Vector Registers (simplified names)
        public static final String v0 = "v0",
                v1 = "v1",
                v2 = "v2",
                v3 = "v3",
                v4 = "v4",
                v5 = "v5",
                v6 = "v6",
                v7 = "v7";
    }

    /** Contains all register definitions for the x86_64 (System V AMD64) architecture. */
    public static class x86_64Registers {
        // Callee-saved (must be preserved by callee)
        public static final String rbx = "rbx",
                rbp = "rbp", // Frame Pointer
                r12 = "r12",
                r13 = "r13",
                r14 = "r14",
                r15 = "r15";

        // Caller-saved (can be clobbered by callee)
        public static final String rax = "rax", // Return value
                rcx = "rcx", // Arg 4
                rdx = "rdx", // Arg 3 / Return value 2
                rsi = "rsi", // Arg 2
                rdi = "rdi", // Arg 1
                r8 = "r8", // Arg 5
                r9 = "r9", // Arg 6
                r10 = "r10",
                r11 = "r11";

        // Special Purpose
        public static final String rsp = "rsp"; // Stack Pointer

        // Vector Registers (ymm for AVx)
        public static final String ymm0 = "ymm0",
                ymm1 = "ymm1",
                ymm2 = "ymm2",
                ymm3 = "ymm3",
                ymm4 = "ymm4",
                ymm5 = "ymm5",
                ymm6 = "ymm6",
                ymm7 = "ymm7";
    }

    // ======================================================================
    // --- INSTRUCTION PATTERN CONSTANTS ---
    // ======================================================================

    // Instruction patterns as final Strings - shared by both architectures
    private static final String prologue = "prologue",
            epilogue = "epilogue",
            move_reg = "move_reg",
            load_immediate_int = "load_immediate_int",
            load_address = "load_address",
            add_int = "add_int",
            sub_int = "sub_int",
            mul_int = "mul_int",
            div_int = "div_int",
            mod_int = "mod_int",
            neg_int = "neg_int",
            cmp_eq_int = "cmp_eq_int",
            cmp_ne_int = "cmp_ne_int",
            cmp_lt_int = "cmp_lt_int",
            cmp_le_int = "cmp_le_int",
            cmp_gt_int = "cmp_gt_int",
            cmp_ge_int = "cmp_ge_int",
            pop = "pop",
            jmp = "jmp",
            jmp_if_false = "jmp_if_false",
            jmp_if_true = "jmp_if_true",
            call = "call",
            store_to_stack = "store_to_stack",
            load_from_stack = "load_from_stack",
            load_field_offset = "load_field_offset",
            store_field_offset = "store_field_offset",
            alloc_stack_frame = "alloc_stack_frame",
            dealloc_stack_frame = "dealloc_stack_frame",
            save_callee_reg_pair = "save_callee_reg_pair",
            save_callee_reg_single = "save_callee_reg_single",
            restore_callee_reg_pair = "restore_callee_reg_pair",
            restore_callee_reg_single = "restore_callee_reg_single";

    // ======================================================================
    // --- INSTRUCTION TEMPLATE STRINGS ---
    // ======================================================================

    // x86_64 instruction templates
    public static final String x86_prologue_1 = "push " + rbp,
            x86_prologue_2 = "mov " + rbp + ", " + rsp,
            x86_epilogue_1 = "mov " + rsp + ", " + rbp,
            x86_epilogue_2 = "pop " + rbp,
            x86_epilogue_3 = "ret",
            x86_move_reg = "mov {dest}, {src}",
            x86_load_immediate_int = "mov {dest}, {value}",
            x86_load_address = "mov {dest}, {label}",
            x86_add_int_1 = "mov {dest}, {src1}",
            x86_add_int_2 = "add {dest}, {src2}",
            x86_sub_int_1 = "mov {dest}, {src1}",
            x86_sub_int_2 = "sub {dest}, {src2}",
            x86_mul_int_1 = "mov " + rax + ", {src1}",
            x86_mul_int_2 = "imul {src2}",
            x86_mul_int_3 = "mov {dest}, " + rax,
            x86_div_int_1 = "mov " + rax + ", {src1}",
            x86_div_int_2 = "cqo",
            x86_div_int_3 = "idiv {src2}",
            x86_div_int_4 = "mov {dest}, " + rax,
            x86_mod_int_1 = "mov " + rax + ", {src1}",
            x86_mod_int_2 = "cqo",
            x86_mod_int_3 = "idiv {src2}",
            x86_mod_int_4 = "mov {dest}, " + rdx,
            x86_neg_int_1 = "mov {dest}, {src}",
            x86_neg_int_2 = "neg {dest}",
            x86_cmp_eq_int_1 = "cmp {src1}, {src2}",
            x86_cmp_eq_int_2 = "sete al",
            x86_cmp_eq_int_3 = "movzx {dest}, al",
            x86_cmp_ne_int_1 = "cmp {src1}, {src2}",
            x86_cmp_ne_int_2 = "setne al",
            x86_cmp_ne_int_3 = "movzx {dest}, al",
            x86_cmp_lt_int_1 = "cmp {src1}, {src2}",
            x86_cmp_lt_int_2 = "setl al",
            x86_cmp_lt_int_3 = "movzx {dest}, al",
            x86_cmp_le_int_1 = "cmp {src1}, {src2}",
            x86_cmp_le_int_2 = "setle al",
            x86_cmp_le_int_3 = "movzx {dest}, al",
            x86_cmp_gt_int_1 = "cmp {src1}, {src2}",
            x86_cmp_gt_int_2 = "setg al",
            x86_cmp_gt_int_3 = "movzx {dest}, al",
            x86_cmp_ge_int_1 = "cmp {src1}, {src2}",
            x86_cmp_ge_int_2 = "setge al",
            x86_cmp_ge_int_3 = "movzx {dest}, al",
            x86_jmp = "jmp {label}",
            x86_jmp_if_false_1 = "test {condition}, {condition}",
            x86_jmp_if_false_2 = "jz {label}",
            x86_jmp_if_true_1 = "test {condition}, {condition}",
            x86_jmp_if_true_2 = "jnz {label}",
            x86_call = "call {name}",
            x86_store_to_stack = "mov [" + rbp + " - {offset}], {src_reg}", // Note: x86 offsets from RBP are negative
            x86_load_from_stack = "mov {dest_reg}, [" + rbp + " - {offset}]",
            x86_load_field_offset = "mov {dest_reg}, [{base_reg} + {offset}]",
            x86_store_field_offset = "mov [{base_reg} + {offset}], {src_reg}",
            x86_alloc_stack_frame = "sub " + rsp + ", {size}",
            x86_dealloc_stack_frame = "mov " + rsp + ", " + rbp,
            x86_save_callee_reg_single = "push {reg}",
            x86_restore_callee_reg_single = "pop {reg}";

    // AArch64 instruction templates
    public static final String
            arm_prologue_1 = "stp " + x29 + ", " + x30 + ", [" + sp + ", #-16]!",
            arm_prologue_2 = "mov " + x29 + ", " + sp,
            arm_epilogue_1 = "mov " + sp + ", " + x29,
            arm_epilogue_2 = "ldp " + x29 + ", " + x30 + ", [" + sp + "], #16",
            arm_epilogue_3 = "ret",
            arm_move_reg = "mov {dest}, {src}",
            arm_load_immediate_int = "mov {dest}, #{value}",
            arm_load_address_1 = "adrp {dest}, {label}",
            arm_load_address_2 = "add {dest}, {dest}, :lo12:{label}",
            arm_add_int = "add {dest}, {src1}, {src2}",
            arm_sub_int = "sub {dest}, {src1}, {src2}",
            arm_mul_int = "mul {dest}, {src1}, {src2}",
            arm_div_int = "sdiv {dest}, {src1}, {src2}",
            arm_mod_int_1 = "sdiv " + x16 + ", {src1}, {src2}",
            arm_mod_int_2 = "mul " + x16 + ", " + x16 + ", {src2}",
            arm_mod_int_3 = "sub {dest}, {src1}, " + x16,
            arm_neg_int = "neg {dest}, {src}",
            arm_cmp_eq_int_1 = "cmp {src1}, {src2}",
            arm_cmp_eq_int_2 = "cset {dest}, eq",
            arm_cmp_ne_int_1 = "cmp {src1}, {src2}",
            arm_cmp_ne_int_2 = "cset {dest}, ne",
            arm_cmp_lt_int_1 = "cmp {src1}, {src2}",
            arm_cmp_lt_int_2 = "cset {dest}, lt",
            arm_cmp_le_int_1 = "cmp {src1}, {src2}",
            arm_cmp_le_int_2 = "cset {dest}, le",
            arm_cmp_gt_int_1 = "cmp {src1}, {src2}",
            arm_cmp_gt_int_2 = "cset {dest}, gt",
            arm_cmp_ge_int_1 = "cmp {src1}, {src2}",
            arm_cmp_ge_int_2 = "cset {dest}, ge",
            arm_jmp = "b {label}",
            arm_jmp_if_false_1 = "cmp {condition}, #0",
            arm_jmp_if_false_2 = "b.eq {label}",
            arm_jmp_if_true_1 = "cmp {condition}, #0",
            arm_jmp_if_true_2 = "b.ne {label}",
            arm_call = "bl {name}",
            arm_store_to_stack = "str {src_reg}, [" + x29 + ", #{offset}]", // Note: ARM offsets from FP are negative
            arm_load_from_stack = "ldr {dest_reg}, [" + x29 + ", #{offset}]",
            arm_load_field_offset = "ldr {dest_reg}, [{base_reg}, #{offset}]",
            arm_store_field_offset = "str {src_reg}, [{base_reg}, #{offset}]",
            arm_alloc_stack_frame = "sub " + sp + ", " + sp + ", #{size}",
            arm_dealloc_stack_frame = "mov " + sp + ", " + x29,
            arm_save_callee_reg_pair = "stp {reg1}, {reg2}, [" + fp + ", #{offset}]",
            arm_save_callee_reg_single = "str {reg1}, [" + fp + ", #{offset}]",
            arm_restore_callee_reg_pair = "ldp {reg1}, {reg2}, [" + fp + ", #{offset}]",
            arm_restore_callee_reg_single = "ldr {reg1}, [" + fp + ", #{offset}]";

    // ======================================================================
    // --- INNER PROFILE CLASSES ---
    // ======================================================================

    /** Defines the assembly syntax for a specific toolchain (e.g., GAS, NASM). */
    public static class SyntaxProfile {
        public final String commentMarker;
        public final String textSection;
        public final String dataSection;
        public final String globalDirective; // e.g., "global {name}"
        public final String stringDirective; // e.g., "{label}: db \"{value}\", 0"
        public final String floatDirective; // e.g., "{label}: .float {value}"

        public SyntaxProfile(
                String commentMarker,
                String textSection,
                String dataSection,
                String globalDirective,
                String stringDirective,
                String floatDirective) {
            this.commentMarker = commentMarker;
            this.textSection = textSection;
            this.dataSection = dataSection;
            this.globalDirective = globalDirective;
            this.stringDirective = stringDirective;
            this.floatDirective = floatDirective;
        }
    }

    /** Defines a specific instruction pattern for a CPU. */
    public static class InstructionPattern {
        public final String blockType;
        public final List<String> assemblyTemplate;
        public final List<String> requiredRegisters;

        public InstructionPattern(
                String blockType, List<String> assemblyTemplate, List<String> requiredRegisters) {
            this.blockType = blockType;
            this.assemblyTemplate = assemblyTemplate;
            this.requiredRegisters = requiredRegisters;
        }
    }

    /** Defines the vector capabilities of a CPU. */
    public static class VectorCapabilities {
        public final int vectorSize;
        public final boolean hasAVx;
        public final boolean hasNEON;
        public final boolean hasRVV;

        public VectorCapabilities(int vectorSize, boolean hasAVx, boolean hasNEON, boolean hasRVV) {
            this.vectorSize = vectorSize;
            this.hasAVx = hasAVx;
            this.hasNEON = hasNEON;
            this.hasRVV = hasRVV;
        }
    }

    /** Defines the register layout for a CPU. */
    public static class RegisterFile {
        public final List<String> generalPurpose; // Available for allocation
        public final List<String> vectorRegisters;
        public final List<String> argumentRegisters; // Used for args/return
        public final String stackPointer;
        public final String framePointer;
        public final int registerCount;

        /** Constructor for AArch64 RegisterFile. */
        public RegisterFile(AArch64Registers regs) {
            this.generalPurpose =
                    Collections.unmodifiableList(
                            new ArrayList<String>(
                                    Arrays.asList(
                                            x19, x20, x21, x22, x23, x24, x25, x26, x27, x28, x9,
                                            x10, x11, x12, x13, x14, x15)));
            this.vectorRegisters =
                    Collections.unmodifiableList(
                            new ArrayList<String>(Arrays.asList(v0, v1, v2, v3, v4, v5, v6, v7)));
            this.argumentRegisters =
                    Collections.unmodifiableList(
                            new ArrayList<String>(Arrays.asList(x0, x1, x2, x3, x4, x5, x6, x7)));
            this.stackPointer = sp;
            this.framePointer = fp;
            this.registerCount = this.generalPurpose.size();
        }

        /** Constructor for x86_64 RegisterFile. */
        public RegisterFile() {
            // General purpose allocatable = Callee-saved + Caller-saved (non-arg)
            this.generalPurpose =
                    Collections.unmodifiableList(
                            new ArrayList<String>(
                                    Arrays.asList(
                                            // Callee-saved
                                            rbx,
                                            r12,
                                            r13,
                                            r14,
                                            r15,
                                            // Caller-saved (non-arg, non-return)
                                            r10,
                                            r11)));
            this.vectorRegisters =
                    Collections.unmodifiableList(
                            new ArrayList<String>(
                                    Arrays.asList(ymm0, ymm1, ymm2, ymm3, ymm4, ymm5, ymm6, ymm7)));
            // Argument registers (in System V ABI order)
            // --- MODIFIED: x86 arg regs are also used for return (rax, rdx) ---
            this.argumentRegisters =
                    Collections.unmodifiableList(
                            new ArrayList<String>(Arrays.asList(
                                rdi, // arg 0
                                rsi, // arg 1
                                rdx, // arg 2 (also return 1)
                                rcx, // arg 3
                                r8,  // arg 4
                                r9,  // arg 5
                                rax  // return 0 (not technically an arg reg, but convenient here)
                                )));
            this.stackPointer = rsp;
            this.framePointer = rbp;
            this.registerCount = this.generalPurpose.size();
        }
    }

    /** Main class holding all profile information for a specific CPU architecture. */
    public static class CPUProfile {
        public final String architecture;
        public final Map<String, InstructionPattern> patterns;
        // --- MODIFICATION: Removed 'static' ---
        public final RegisterFile registerFile;
        // --- END MODIFICATION ---
        public final VectorCapabilities vector;
        public final SyntaxProfile syntax;

        public CPUProfile(
                String architecture,
                Map<String, InstructionPattern> patterns,
                RegisterFile registerFile, // <--- ADDED to constructor
                VectorCapabilities vector,
                SyntaxProfile syntax) {
            this.architecture = architecture;
            this.patterns = patterns;
            this.registerFile = registerFile; // <--- ADDED assignment
            this.vector = vector;
            this.syntax = syntax;
        }

        public InstructionPattern getPattern(String blockType) {
            return patterns.get(blockType);
        }
    }

    // ======================================================================
    // --- REGISTRY INITIALIZATION ---
    // ======================================================================

    static {
        List<String> emptyList = Collections.emptyList();

        // ======================================================================
        // x86_64 Profile (NASM syntax)
        // ======================================================================

        SyntaxProfile x86Syntax =
                new SyntaxProfile(
                        ";", // commentMarker
                        "section .text", // textSection
                        "section .data", // dataSection
                        "global {name}", // globalDirective
                        "{label}: db \"{value}\", 0", // stringDirective
                        "{label}: dd __float32__({value})" // floatDirective (NASM syntax for 32-bit
                                                           // float)
                        );

        Map<String, InstructionPattern> x86Patterns = new HashMap<String, InstructionPattern>();
        x86Patterns.put(
                prologue,
                new InstructionPattern(
                        prologue, Arrays.asList(x86_prologue_1, x86_prologue_2), emptyList));
        x86Patterns.put(
                epilogue,
                new InstructionPattern(
                        epilogue,
                        Arrays.asList(x86_epilogue_1, x86_epilogue_2, x86_epilogue_3),
                        emptyList));
        x86Patterns.put(
                move_reg, new InstructionPattern(move_reg, Arrays.asList(x86_move_reg), emptyList));
        x86Patterns.put(
                load_immediate_int,
                new InstructionPattern(
                        load_immediate_int, Arrays.asList(x86_load_immediate_int), emptyList));
        x86Patterns.put(
                load_address,
                new InstructionPattern(load_address, Arrays.asList(x86_load_address), emptyList));
        x86Patterns.put(
                add_int,
                new InstructionPattern(
                        add_int, Arrays.asList(x86_add_int_1, x86_add_int_2), emptyList));
        x86Patterns.put(
                sub_int,
                new InstructionPattern(
                        sub_int, Arrays.asList(x86_sub_int_1, x86_sub_int_2), emptyList));
        x86Patterns.put(
                mul_int,
                new InstructionPattern(
                        mul_int,
                        Arrays.asList(x86_mul_int_1, x86_mul_int_2, x86_mul_int_3),
                        Arrays.asList(rax, rdx) // Clobbers rax, rdx
                        ));
        x86Patterns.put(
                div_int,
                new InstructionPattern(
                        div_int,
                        Arrays.asList(x86_div_int_1, x86_div_int_2, x86_div_int_3, x86_div_int_4),
                        Arrays.asList(rax, rdx) // Clobbers rax, rdx
                        ));
        x86Patterns.put(
                mod_int,
                new InstructionPattern(
                        mod_int,
                        Arrays.asList(x86_mod_int_1, x86_mod_int_2, x86_mod_int_3, x86_mod_int_4),
                        Arrays.asList(rax, rdx) // Clobbers rax, rdx
                        ));
        x86Patterns.put(
                neg_int,
                new InstructionPattern(
                        neg_int, Arrays.asList(x86_neg_int_1, x86_neg_int_2), emptyList));
        x86Patterns.put(
                cmp_eq_int,
                new InstructionPattern(
                        cmp_eq_int,
                        Arrays.asList(x86_cmp_eq_int_1, x86_cmp_eq_int_2, x86_cmp_eq_int_3),
                        Arrays.asList("al") // Uses 'al' register
                        ));
        x86Patterns.put(
                cmp_ne_int,
                new InstructionPattern(
                        cmp_ne_int,
                        Arrays.asList(x86_cmp_ne_int_1, x86_cmp_ne_int_2, x86_cmp_ne_int_3),
                        Arrays.asList("al")));
        x86Patterns.put(
                cmp_lt_int,
                new InstructionPattern(
                        cmp_lt_int,
                        Arrays.asList(x86_cmp_lt_int_1, x86_cmp_lt_int_2, x86_cmp_lt_int_3),
                        Arrays.asList("al")));
        x86Patterns.put(
                cmp_le_int,
                new InstructionPattern(
                        cmp_le_int,
                        Arrays.asList(x86_cmp_le_int_1, x86_cmp_le_int_2, x86_cmp_le_int_3),
                        Arrays.asList("al")));
        x86Patterns.put(
                cmp_gt_int,
                new InstructionPattern(
                        cmp_gt_int,
                        Arrays.asList(x86_cmp_gt_int_1, x86_cmp_gt_int_2, x86_cmp_gt_int_3),
                        Arrays.asList("al")));
        x86Patterns.put(
                cmp_ge_int,
                new InstructionPattern(
                        cmp_ge_int,
                        Arrays.asList(x86_cmp_ge_int_1, x86_cmp_ge_int_2, x86_cmp_ge_int_3),
                        Arrays.asList("al")));
        x86Patterns.put(
                pop, new InstructionPattern(pop, Collections.<String>emptyList(), emptyList));
        x86Patterns.put(jmp, new InstructionPattern(jmp, Arrays.asList(x86_jmp), emptyList));
        x86Patterns.put(
                jmp_if_false,
                new InstructionPattern(
                        jmp_if_false,
                        Arrays.asList(x86_jmp_if_false_1, x86_jmp_if_false_2),
                        emptyList));
        x86Patterns.put(
                jmp_if_true,
                new InstructionPattern(
                        jmp_if_true,
                        Arrays.asList(x86_jmp_if_true_1, x86_jmp_if_true_2),
                        emptyList));
        x86Patterns.put(call, new InstructionPattern(call, Arrays.asList(x86_call), emptyList));
        x86Patterns.put(
                store_to_stack,
                new InstructionPattern(
                        store_to_stack, Arrays.asList(x86_store_to_stack), emptyList));
        x86Patterns.put(
                load_from_stack,
                new InstructionPattern(
                        load_from_stack, Arrays.asList(x86_load_from_stack), emptyList));
        x86Patterns.put(
                load_field_offset,
                new InstructionPattern(
                        load_field_offset, Arrays.asList(x86_load_field_offset), emptyList));
        x86Patterns.put(
                store_field_offset,
                new InstructionPattern(
                        store_field_offset, Arrays.asList(x86_store_field_offset), emptyList));
        
        x86Patterns.put(alloc_stack_frame, new InstructionPattern(alloc_stack_frame, Arrays.asList(x86_alloc_stack_frame), emptyList));
        x86Patterns.put(dealloc_stack_frame, new InstructionPattern(dealloc_stack_frame, Arrays.asList(x86_dealloc_stack_frame), emptyList));
        x86Patterns.put(save_callee_reg_single, new InstructionPattern(save_callee_reg_single, Arrays.asList(x86_save_callee_reg_single), emptyList));
        x86Patterns.put(restore_callee_reg_single, new InstructionPattern(restore_callee_reg_single, Arrays.asList(x86_restore_callee_reg_single), emptyList));
        x86Patterns.put(save_callee_reg_pair, new InstructionPattern(save_callee_reg_pair, Collections.<String>emptyList(), emptyList));
        x86Patterns.put(restore_callee_reg_pair, new InstructionPattern(restore_callee_reg_pair, Collections.<String>emptyList(), emptyList));

        RegisterFile x86Registers = new RegisterFile();
        VectorCapabilities x86Vector = new VectorCapabilities(256, true, false, false);
        
        // --- MODIFIED: Pass RegisterFile to constructor ---
        profiles.put(
                "x86_64",
                new CPUProfile("x86_64", x86Patterns, x86Registers, x86Vector, x86Syntax));
        // --- END MODIFIED ---

        // ======================================================================
        // AArch64 Profile (ARM64)
        // ======================================================================

        SyntaxProfile armSyntax =
                new SyntaxProfile(
                        "//", // commentMarker
                        "    .text", // textSection
                        "    .data", // dataSection
                        "    .global {name}", // globalDirective
                        "{label}: .asciz \"{value}\"", // stringDirective
                        "{label}: .float {value}" // floatDirective
                        );

        Map<String, InstructionPattern> armPatterns = new HashMap<String, InstructionPattern>();
        armPatterns.put(
                prologue,
                new InstructionPattern(
                        prologue, Arrays.asList(arm_prologue_1, arm_prologue_2), emptyList));
        armPatterns.put(
                epilogue,
                new InstructionPattern(
                        epilogue,
                        Arrays.asList(arm_epilogue_1, arm_epilogue_2, arm_epilogue_3),
                        emptyList));
        armPatterns.put(
                move_reg, new InstructionPattern(move_reg, Arrays.asList(arm_move_reg), emptyList));
        armPatterns.put(
                load_immediate_int,
                new InstructionPattern(
                        load_immediate_int, Arrays.asList(arm_load_immediate_int), emptyList));
        armPatterns.put(
                load_address,
                new InstructionPattern(
                        load_address,
                        Arrays.asList(arm_load_address_1, arm_load_address_2),
                        emptyList));
        armPatterns.put(
                add_int, new InstructionPattern(add_int, Arrays.asList(arm_add_int), emptyList));
        armPatterns.put(
                sub_int, new InstructionPattern(sub_int, Arrays.asList(arm_sub_int), emptyList));
        armPatterns.put(
                mul_int, new InstructionPattern(mul_int, Arrays.asList(arm_mul_int), emptyList));
        armPatterns.put(
                div_int, new InstructionPattern(div_int, Arrays.asList(arm_div_int), emptyList));
        armPatterns.put(
                mod_int,
                new InstructionPattern(
                        mod_int,
                        Arrays.asList(arm_mod_int_1, arm_mod_int_2, arm_mod_int_3),
                        Arrays.asList(x16) // Clobbers x16
                        ));
        armPatterns.put(
                neg_int, new InstructionPattern(neg_int, Arrays.asList(arm_neg_int), emptyList));
        armPatterns.put(
                cmp_eq_int,
                new InstructionPattern(
                        cmp_eq_int, Arrays.asList(arm_cmp_eq_int_1, arm_cmp_eq_int_2), emptyList));
        armPatterns.put(
                cmp_ne_int,
                new InstructionPattern(
                        cmp_ne_int, Arrays.asList(arm_cmp_ne_int_1, arm_cmp_ne_int_2), emptyList));
        armPatterns.put(
                cmp_lt_int,
                new InstructionPattern(
                        cmp_lt_int, Arrays.asList(arm_cmp_lt_int_1, arm_cmp_lt_int_2), emptyList));
        armPatterns.put(
                cmp_le_int,
                new InstructionPattern(
                        cmp_le_int, Arrays.asList(arm_cmp_le_int_1, arm_cmp_le_int_2), emptyList));
        armPatterns.put(
                cmp_gt_int,
                new InstructionPattern(
                        cmp_gt_int, Arrays.asList(arm_cmp_gt_int_1, arm_cmp_gt_int_2), emptyList));
        armPatterns.put(
                cmp_ge_int,
                new InstructionPattern(
                        cmp_ge_int, Arrays.asList(arm_cmp_ge_int_1, arm_cmp_ge_int_2), emptyList));
        armPatterns.put(
                pop, new InstructionPattern(pop, Collections.<String>emptyList(), emptyList));
        armPatterns.put(jmp, new InstructionPattern(jmp, Arrays.asList(arm_jmp), emptyList));
        armPatterns.put(
                jmp_if_false,
                new InstructionPattern(
                        jmp_if_false,
                        Arrays.asList(arm_jmp_if_false_1, arm_jmp_if_false_2),
                        emptyList));
        armPatterns.put(
                jmp_if_true,
                new InstructionPattern(
                        jmp_if_true,
                        Arrays.asList(arm_jmp_if_true_1, arm_jmp_if_true_2),
                        emptyList));
        armPatterns.put(call, new InstructionPattern(call, Arrays.asList(arm_call), emptyList));
        armPatterns.put(
                store_to_stack,
                new InstructionPattern(
                        store_to_stack, Arrays.asList(arm_store_to_stack), emptyList));
        armPatterns.put(
                load_from_stack,
                new InstructionPattern(
                        load_from_stack, Arrays.asList(arm_load_from_stack), emptyList));
        armPatterns.put(
                load_field_offset,
                new InstructionPattern(
                        load_field_offset, Arrays.asList(arm_load_field_offset), emptyList));
        armPatterns.put(
                store_field_offset,
                new InstructionPattern(
                        store_field_offset, Arrays.asList(arm_store_field_offset), emptyList));
        
        armPatterns.put(alloc_stack_frame, new InstructionPattern(alloc_stack_frame, Arrays.asList(arm_alloc_stack_frame), emptyList));
        armPatterns.put(dealloc_stack_frame, new InstructionPattern(dealloc_stack_frame, Arrays.asList(arm_dealloc_stack_frame), emptyList));
        armPatterns.put(save_callee_reg_pair, new InstructionPattern(save_callee_reg_pair, Arrays.asList(arm_save_callee_reg_pair), emptyList));
        armPatterns.put(save_callee_reg_single, new InstructionPattern(save_callee_reg_single, Arrays.asList(arm_save_callee_reg_single), emptyList));
        armPatterns.put(restore_callee_reg_pair, new InstructionPattern(restore_callee_reg_pair, Arrays.asList(arm_restore_callee_reg_pair), emptyList));
        armPatterns.put(restore_callee_reg_single, new InstructionPattern(restore_callee_reg_single, Arrays.asList(arm_restore_callee_reg_single), emptyList));

        RegisterFile armRegisters = new RegisterFile(new AArch64Registers());
        VectorCapabilities armVector = new VectorCapabilities(128, false, true, false);
        
        // --- MODIFIED: Pass RegisterFile to constructor ---
        profiles.put(
                "aarch64",
                new CPUProfile("aarch64", armPatterns, armRegisters, armVector, armSyntax));
        // --- END MODIFIED ---
    }

    // ======================================================================
    // --- PUBLIC API ---
    // ======================================================================

    public static CPUProfile detectCPU() {
        String arch = System.getProperty("os.arch").toLowerCase();
        if (arch.contains("x86_64") || arch.contains("amd64")) {
            return profiles.get("x86_64");
        } else if (arch.contains("aarch64")) {
            return profiles.get("aarch64");
        }
        System.err.println(
                "Warning: Unsupported architecture '" + arch + "'. Defaulting to aarch64.");
        return profiles.get("aarch64");
    }

    public static CPUProfile getProfile(String architecture) {
        CPUProfile profile = profiles.get(architecture);
        if (profile == null) {
            throw new IllegalArgumentException("Unsupported architecture: " + architecture);
        }
        return profile;
    }
}