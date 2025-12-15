package cod.compiler;

import java.util.*;
import static cod.compiler.MTOTRegistry.AArch64Registers.*;
import static cod.compiler.MTOTRegistry.x86_64Registers.*;

public class MTOTRegistry {
    private static final Map<String, CPUProfile> profiles = new HashMap<String, CPUProfile>();

    // --- Added method for manual profile retrieval ---
    public static CPUProfile getProfile(String architecture) {
        return profiles.get(architecture);
    }
    // -------------------------------------------------

    public static class AArch64Registers {
        public static final String x19="x19", x20="x20", x21="x21", x22="x22", x23="x23", x24="x24", x25="x25", x26="x26", x27="x27", x28="x28", x29="x29", x30="x30";
        public static final String x9="x9", x10="x10", x11="x11", x12="x12", x13="x13", x14="x14", x15="x15", x16="x16", x17="x17";
        public static final String x0="x0", x1="x1", x2="x2", x3="x3", x4="x4", x5="x5", x6="x6", x7="x7";
        public static final String sp="sp", fp="x29", lr="x30", zr="xzr";
    }

    public static class x86_64Registers {
        public static final String rbx="rbx", rbp="rbp", r12="r12", r13="r13", r14="r14", r15="r15";
        public static final String rax="rax", rcx="rcx", rdx="rdx", rsi="rsi", rdi="rdi", r8="r8", r9="r9", r10="r10", r11="r11";
        public static final String rsp="rsp";
    }

    public static class SyntaxProfile {
        public final String commentMarker, textSection, dataSection, stringDirective;
        public SyntaxProfile(String c, String t, String d, String s) { commentMarker=c; textSection=t; dataSection=d; stringDirective=s; }
    }

    public static class InstructionPattern {
        public final List<String> assemblyTemplate;
        public InstructionPattern(List<String> a) { assemblyTemplate=a; }
    }

    public static class RegisterFile {
        public final List<String> generalPurpose, argumentRegisters;
        public RegisterFile(List<String> gp, List<String> args) { generalPurpose=gp; argumentRegisters=args; }
    }

    public static class CPUProfile {
        public final String architecture;
        public final Map<String, InstructionPattern> patterns;
        public final RegisterFile registerFile;
        public final SyntaxProfile syntax;
        public CPUProfile(String a, Map<String, InstructionPattern> p, RegisterFile r, SyntaxProfile s) { architecture=a; patterns=p; registerFile=r; syntax=s; }
        public InstructionPattern getPattern(String t) { return patterns.get(t); }
    }

    static {
        
        // --- x86_64 ---
        Map<String, InstructionPattern> x86 = new HashMap<String, InstructionPattern>();
        x86.put("prologue", new InstructionPattern(Arrays.asList("push " + rbp, "mov " + rbp + ", " + rsp)));
        x86.put("epilogue", new InstructionPattern(Arrays.asList("mov " + rsp + ", " + rbp, "pop " + rbp, "ret")));
        x86.put("move_reg", new InstructionPattern(Arrays.asList("mov {dest}, {src}")));
        x86.put("load_immediate_int", new InstructionPattern(Arrays.asList("mov {dest}, {value}")));
        x86.put("load_address", new InstructionPattern(Arrays.asList("lea {dest}, [rel {label}]"))); 
        x86.put("add_int", new InstructionPattern(Arrays.asList("add {dest}, {src2}"))); 
        x86.put("sub_int", new InstructionPattern(Arrays.asList("sub {dest}, {src2}")));
        x86.put("mul_int", new InstructionPattern(Arrays.asList("imul {dest}, {src2}")));
        x86.put("div_int", new InstructionPattern(Arrays.asList("mov rax, {dest}", "cqo", "idiv {src2}", "mov {dest}, rax")));
        x86.put("mod_int", new InstructionPattern(Arrays.asList("mov rax, {dest}", "cqo", "idiv {src2}", "mov {dest}, rdx")));
        x86.put("neg_int", new InstructionPattern(Arrays.asList("neg {dest}")));
        x86.put("cmp_eq_int", new InstructionPattern(Arrays.asList("cmp {src1}, {src2}", "sete al", "movzx {dest}, al")));
        x86.put("cmp_ne_int", new InstructionPattern(Arrays.asList("cmp {src1}, {src2}", "setne al", "movzx {dest}, al")));
        x86.put("cmp_lt_int", new InstructionPattern(Arrays.asList("cmp {src1}, {src2}", "setl al", "movzx {dest}, al")));
        x86.put("cmp_le_int", new InstructionPattern(Arrays.asList("cmp {src1}, {src2}", "setle al", "movzx {dest}, al")));
        x86.put("cmp_gt_int", new InstructionPattern(Arrays.asList("cmp {src1}, {src2}", "setg al", "movzx {dest}, al")));
        x86.put("cmp_ge_int", new InstructionPattern(Arrays.asList("cmp {src1}, {src2}", "setge al", "movzx {dest}, al")));
        x86.put("jmp", new InstructionPattern(Arrays.asList("jmp {label}")));
        x86.put("call", new InstructionPattern(Arrays.asList("call {name}")));
        x86.put("store_to_stack", new InstructionPattern(Arrays.asList("mov [" + rbp + " - {offset}], {src_reg}")));
        x86.put("load_from_stack", new InstructionPattern(Arrays.asList("mov {dest_reg}, [" + rbp + " - {offset}]")));
        x86.put("alloc_stack_frame", new InstructionPattern(Arrays.asList("sub " + rsp + ", {size}")));

        profiles.put("x86_64", new CPUProfile("x86_64", x86, 
            new RegisterFile(Arrays.asList(rbx, r12, r13, r14, r15), Arrays.asList(rdi, rsi, rdx, rcx, r8, r9)), 
            new SyntaxProfile(";", "section .text", "section .data", "{label}: db \"{value}\", 0")));

        // --- AArch64 ---
        Map<String, InstructionPattern> arm = new HashMap<String, InstructionPattern>();
        arm.put("prologue", new InstructionPattern(Arrays.asList("stp " + fp + ", " + lr + ", [" + sp + ", #-16]!", "mov " + fp + ", " + sp)));
        arm.put("epilogue", new InstructionPattern(Arrays.asList("mov " + sp + ", " + fp, "ldp " + fp + ", " + lr + ", [" + sp + "], #16", "ret")));
        arm.put("move_reg", new InstructionPattern(Arrays.asList("mov {dest}, {src}")));
        arm.put("load_immediate_int", new InstructionPattern(Arrays.asList("mov {dest}, #{value}")));
        arm.put("load_address", new InstructionPattern(Arrays.asList("adrp {dest}, {label}", "add {dest}, {dest}, :lo12:{label}")));
        arm.put("add_int", new InstructionPattern(Arrays.asList("add {dest}, {src1}, {src2}")));
        arm.put("sub_int", new InstructionPattern(Arrays.asList("sub {dest}, {src1}, {src2}")));
        arm.put("mul_int", new InstructionPattern(Arrays.asList("mul {dest}, {src1}, {src2}")));
        arm.put("div_int", new InstructionPattern(Arrays.asList("sdiv {dest}, {src1}, {src2}")));
        arm.put("cmp_eq_int", new InstructionPattern(Arrays.asList("cmp {src1}, {src2}", "cset {dest}, eq")));
        arm.put("cmp_ne_int", new InstructionPattern(Arrays.asList("cmp {src1}, {src2}", "cset {dest}, ne")));
        arm.put("cmp_lt_int", new InstructionPattern(Arrays.asList("cmp {src1}, {src2}", "cset {dest}, lt")));
        arm.put("cmp_le_int", new InstructionPattern(Arrays.asList("cmp {src1}, {src2}", "cset {dest}, le")));
        arm.put("cmp_gt_int", new InstructionPattern(Arrays.asList("cmp {src1}, {src2}", "cset {dest}, gt")));
        arm.put("cmp_ge_int", new InstructionPattern(Arrays.asList("cmp {src1}, {src2}", "cset {dest}, ge")));
        arm.put("jmp", new InstructionPattern(Arrays.asList("b {label}")));
        arm.put("call", new InstructionPattern(Arrays.asList("bl {name}")));
        
        // FIX: Use negative offset relative to FP for Store (spilled var)
        arm.put("store_to_stack", new InstructionPattern(Arrays.asList("str {src_reg}, [" + fp + ", #-{offset}]")));
        
        // FIX: Use negative offset relative to FP for Load (spilled var)
        arm.put("load_from_stack", new InstructionPattern(Arrays.asList("ldr {dest_reg}, [" + fp + ", #-{offset}]")));
        
        arm.put("alloc_stack_frame", new InstructionPattern(Arrays.asList("sub " + sp + ", " + sp + ", #{size}")));

        profiles.put("aarch64", new CPUProfile("aarch64", arm, 
            new RegisterFile(Arrays.asList(x19, x20, x21, x22, x23, x24, x25, x26, x27, x28), Arrays.asList(x0, x1, x2, x3, x4, x5, x6, x7)), 
            new SyntaxProfile("//", ".text", ".data", "{label}: .asciz \"{value}\"")));
    }

    public static CPUProfile detectCPU() {
        String arch = System.getProperty("os.arch").toLowerCase();
        if (arch.contains("aarch64") || arch.contains("arm64")) return profiles.get("aarch64");
        return profiles.get("x86_64");
    }
}