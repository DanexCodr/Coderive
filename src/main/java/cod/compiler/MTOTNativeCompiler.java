package cod.compiler;

import static cod.compiler.MTOTRegistry.*;
import static cod.compiler.MTOTRegistry.AArch64Registers.*;
import static cod.compiler.MTOTRegistry.x86_64Registers.*;
import cod.compiler.TACInstruction.Opcode;
import cod.debug.DebugSystem;
import java.util.*;

public class MTOTNativeCompiler {

  public final CPUProfile cpuProfile;
  public final String thisRegister;
  private final Set<String> abiCallerSavedSet;
  public final List<String> argumentRegisters;

  private final String scratchReg1;
  private final String scratchReg2;

  public final RegisterManager registerManager;
  private final RegisterManager.RegisterSpiller spiller;

  private List<String> assemblyCode = new ArrayList<String>();
  private final List<String> dataSection = new ArrayList<String>();

  private int dataLabelCounter = 0;
  private String currentMethodName = "";
  
  public MTOTNativeCompiler(CPUProfile cpuProfile) {
    this.cpuProfile = cpuProfile;
    this.argumentRegisters = cpuProfile.registerFile.argumentRegisters;

    if (cpuProfile.architecture.equals("aarch64")) {
      this.thisRegister = x19;
      this.scratchReg1 = x9;
      this.scratchReg2 = x10;
      this.abiCallerSavedSet =
          new HashSet<String>(
              Arrays.asList(
                  x0, x1, x2, x3, x4, x5, x6, x7, x9, x10, x11, x12, x13, x14, x15, x16, x17));
    } else {
      this.thisRegister = r15;
      this.scratchReg1 = r10;
      this.scratchReg2 = r11;
      this.abiCallerSavedSet =
          new HashSet<String>(Arrays.asList(rax, rcx, rdx, rsi, rdi, r8, r9, r10, r11));
    }

    this.registerManager = new RegisterManager(this);
    this.spiller = this.registerManager.getSpiller();
  }

  public String compileMethodFromTAC(String methodName, List<TACInstruction> tac) {
    DebugSystem.info("MTOT", "Compiling method: " + methodName + " (" + tac.size() + " TAC instr)");

    assemblyCode.clear();
    dataSection.clear();
    registerManager.reset();
    dataLabelCounter = 0;
    this.currentMethodName = methodName;

    // FIX: Pass the ABI caller-saved registers to the RegisterManager's graph 
    // BEFORE running allocation.
    this.registerManager.setCallerSavedRegisters(this.abiCallerSavedSet);

    registerManager.runAllocation(tac);
    compileBody(tac);
    return constructFinalAssembly(methodName);
  }

  private void compileBody(List<TACInstruction> tac) {
    Map<String, String> labelMap = new HashMap<String, String>();
    for (TACInstruction instr : tac) {
      if (instr.opcode == Opcode.LABEL) {
        labelMap.put((String) instr.operand1, "L_" + currentMethodName + "_" + instr.operand1);
      }
    }

    for (int i = 0; i < tac.size(); i++) {
      TACInstruction instr = tac.get(i);

      try {
        switch (instr.opcode) {
          case LABEL:
            assemblyCode.add(labelMap.get((String) instr.operand1) + ":");
            break;

          case ASSIGN:
            String src = getPhysReg(instr.operand1, scratchReg1, true);
            String dest = getPhysReg(instr.result, scratchReg2, false);
            if (!src.equals(dest)) emitMove(dest, src);
            commitSpill(instr.result, dest);
            break;

          case LOAD_IMM:
            String immDest = getPhysReg(instr.result, scratchReg1, false);
            int val = 0;
            if (instr.operand1 instanceof Integer) val = (Integer) instr.operand1;
            else if (instr.operand1 instanceof Boolean) val = ((Boolean) instr.operand1) ? 1 : 0;

            String asm =
                cpuProfile
                    .getPattern("load_immediate_int")
                    .assemblyTemplate
                    .get(0)
                    .replace("{dest}", immDest)
                    .replace("{value}", String.valueOf(val));
            assemblyCode.add("    " + asm);
            commitSpill(instr.result, immDest);
            break;

          case LOAD_ADDR:
            String addrDest = getPhysReg(instr.result, scratchReg1, false);
            String dataLabel = generateDataLabel("str");
            dataSection.add(
                cpuProfile
                    .syntax
                    .stringDirective
                    .replace("{label}", dataLabel)
                    .replace("{value}", escapeString((String) instr.operand1)));
            for (String t : cpuProfile.getPattern("load_address").assemblyTemplate) {
              assemblyCode.add(
                  "    " + t.replace("{dest}", addrDest).replace("{label}", dataLabel));
            }
            commitSpill(instr.result, addrDest);
            break;

          case ADD:
          case SUB:
          case MUL:
          case DIV:
          case MOD:
          case CMP_EQ:
          case CMP_NE:
          case CMP_LT:
          case CMP_LE:
          case CMP_GT:
          case CMP_GE:
            compileBinaryOp(instr);
            break;

          case INT_TO_STRING:
          case CONCAT:
            compileStringOp(instr);
            break;

          case NEG:
            String negSrc = getPhysReg(instr.operand1, scratchReg1, true);
            String negDest = getPhysReg(instr.result, scratchReg2, false);
            for (String t : cpuProfile.getPattern("neg_int").assemblyTemplate) {
              assemblyCode.add("    " + t.replace("{dest}", negDest).replace("{src}", negSrc));
            }
            commitSpill(instr.result, negDest);
            break;

          case GOTO:
            assemblyCode.add(
                "    "
                    + cpuProfile
                        .getPattern("jmp")
                        .assemblyTemplate
                        .get(0)
                        .replace("{label}", labelMap.get((String) instr.operand1)));
            break;

          case IF_GOTO:
            String ifCond =
                getPhysReg(
                    instr.result, scratchReg1, true); // Sometimes cond is result in TAC struct
            String ifLabel = labelMap.get((String) instr.operand2);
            if (cpuProfile.architecture.equals("aarch64")) {
              assemblyCode.add("    cmp " + ifCond + ", #0");
              assemblyCode.add("    b.eq " + ifLabel);
            } else {
              assemblyCode.add("    test " + ifCond + ", " + ifCond);
              assemblyCode.add("    jz " + ifLabel);
            }
            break;

          case RET:
            if (instr.operand1 != null) {
              String retSrc = getPhysReg(instr.operand1, scratchReg1, true);
              String abiRet = cpuProfile.architecture.equals("x86_64") ? rax : x0;
              if (!retSrc.equals(abiRet)) emitMove(abiRet, retSrc);
            }
            break;

          case CALL:
          case CALL_SLOTS:
            compileCall(instr, i, tac);
            break;

          case PRINT:
            compileRuntimeCall("runtime_print", new String[] {(String) instr.operand1}, false);
            break;

          case READ_INPUT:
            compileReadInput(instr.result, (String) instr.operand1);
            break;

          case ARRAY_NEW:
            compileRuntimeCall("array_new", new String[] {(String) instr.operand1}, true);
            if (instr.result != null) {
              String retReg = cpuProfile.architecture.equals("x86_64") ? rax : x0;
              String d = getPhysReg(instr.result, scratchReg1, false);
              emitMove(d, retReg);
              commitSpill(instr.result, d);
            }
            break;

          case LOAD_ARRAY:
            compileRuntimeCall(
                "array_load",
                new String[] {(String) instr.operand1, (String) instr.operand2},
                true);
            String laRet = cpuProfile.architecture.equals("x86_64") ? rax : x0;
            String laDest = getPhysReg(instr.result, scratchReg1, false);
            emitMove(laDest, laRet);
            commitSpill(instr.result, laDest);
            break;

          case STORE_ARRAY:
            compileRuntimeCall(
                "array_store",
                new String[] {(String) instr.operand1, (String) instr.operand2, instr.result},
                false);
            break;

          case PARAM:
            break;
          default:
            DebugSystem.warn("MTOT", "Ignored TAC: " + instr.opcode);
        }
      } catch (Exception e) {
        DebugSystem.error("MTOT", "Error compiling " + instr + ": " + e.getMessage());
      }
    }
  }

  private void compileBinaryOp(TACInstruction instr) {
    String pName;
    switch (instr.opcode) {
      case ADD:
        pName = "add_int";
        break;
      case SUB:
        pName = "sub_int";
        break;
      case MUL:
        pName = "mul_int";
        break;
      case DIV:
        pName = "div_int";
        break;
      case MOD:
        pName = "mod_int";
        break;
      case CMP_EQ:
        pName = "cmp_eq_int";
        break;
      case CMP_NE:
        pName = "cmp_ne_int";
        break;
      case CMP_LT:
        pName = "cmp_lt_int";
        break;
      case CMP_LE:
        pName = "cmp_le_int";
        break;
      case CMP_GT:
        pName = "cmp_gt_int";
        break;
      case CMP_GE:
        pName = "cmp_ge_int";
        break;
      default:
        return;
    }

    InstructionPattern p = cpuProfile.getPattern(pName);
    String r1 = getPhysReg(instr.operand1, scratchReg1, true);
    String r2 = getPhysReg(instr.operand2, scratchReg2, true);
    String rDest = getPhysReg(instr.result, scratchReg1, false);

    for (String t : p.assemblyTemplate) {
      assemblyCode.add(
          "    " + t.replace("{dest}", rDest).replace("{src1}", r1).replace("{src2}", r2));
    }
    commitSpill(instr.result, rDest);
  }

  private void compileStringOp(TACInstruction instr) {
    switch (instr.opcode) {
        case INT_TO_STRING: {
            String src = getPhysReg(instr.operand1, scratchReg1, true);
            // Pass integer in argument register 0
            String argReg0 = argumentRegisters.get(0);
            if (!src.equals(argReg0)) {
                emitMove(argReg0, src);
            }
            // Call runtime function to convert int to string
            assemblyCode.add("    " + cpuProfile.getPattern("call").assemblyTemplate.get(0)
                .replace("{name}", "int_to_string"));
            // Get return value
            String retReg = cpuProfile.architecture.equals("x86_64") ? rax : x0;
            String dest = getPhysReg(instr.result, scratchReg1, false);
            if (!dest.equals(retReg)) {
                emitMove(dest, retReg);
            }
            commitSpill(instr.result, dest);
            break;
        }
            
        case CONCAT: {
            String str1 = getPhysReg(instr.operand1, scratchReg1, true);
            String str2 = getPhysReg(instr.operand2, scratchReg2, true);
            // Pass strings in argument registers 0 and 1
            String argReg0 = argumentRegisters.get(0);
            String argReg1 = argumentRegisters.get(1);
            if (!str1.equals(argReg0)) {
                emitMove(argReg0, str1);
            }
            if (!str2.equals(argReg1)) {
                emitMove(argReg1, str2);
            }
            // Call runtime function to concatenate strings
            assemblyCode.add("    " + cpuProfile.getPattern("call").assemblyTemplate.get(0)
                .replace("{name}", "string_concat"));
            // Get return value
            String retReg = cpuProfile.architecture.equals("x86_64") ? rax : x0;
            String dest = getPhysReg(instr.result, scratchReg1, false);
            if (!dest.equals(retReg)) {
                emitMove(dest, retReg);
            }
            commitSpill(instr.result, dest);
            break;
        }
        
        default:
    throw new IllegalArgumentException(
        "compileStringOp called with non-string opcode: " + instr.opcode);
    }
}

  private void compileCall(TACInstruction instr, int index, List<TACInstruction> tac) {
    String methodName;
    int numArgs;

    // Check if this is CALL or CALL_SLOTS
    if (instr.opcode == Opcode.CALL_SLOTS) {
      // For CALL_SLOTS, operand1 is method name, operand2 is arg count
      methodName = (String) instr.operand1;
      numArgs = (Integer) instr.operand2;
    } else {
      // Regular CALL
      methodName = (String) instr.operand1;
      numArgs = (Integer) instr.operand2;
    }

    // Collect arguments from preceding PARAM instructions
    Stack<String> args = new Stack<String>();
    for (int k = index - 1; k >= 0 && args.size() < numArgs; k--) {
      if (tac.get(k).opcode == Opcode.PARAM) {
        args.push((String) tac.get(k).operand1);
      }
    }

    // Pass arguments in registers
    List<String> abiRegs = argumentRegisters;
    int abiIdx = 0;
    while (!args.isEmpty()) {
      String argTemp = args.pop();
      if (abiIdx < abiRegs.size()) {
        String srcReg = getPhysReg(argTemp, scratchReg1, true);
        if (!srcReg.equals(abiRegs.get(abiIdx))) {
          emitMove(abiRegs.get(abiIdx), srcReg);
        }
        abiIdx++;
      }
    }

    // Pass 'this' pointer
    emitMove(abiRegs.get(0), thisRegister);

    // Emit call instruction
    assemblyCode.add(
        "    "
            + cpuProfile.getPattern("call").assemblyTemplate.get(0).replace("{name}", methodName));

    // Handle return value
    if (instr.result != null) {
      String retReg = cpuProfile.architecture.equals("x86_64") ? rax : x0;
      String dest = getPhysReg(instr.result, scratchReg1, false);
      emitMove(dest, retReg);
      commitSpill(instr.result, dest);
    }
  }

  private void compileRuntimeCall(String name, String[] argTemps, boolean hasRet) {
    List<String> abiRegs = argumentRegisters;
    for (int i = 0; i < argTemps.length; i++) {
      if (argTemps[i] == null) continue;
      String src = getPhysReg(argTemps[i], scratchReg1, true);
      emitMove(abiRegs.get(i), src);
    }
    assemblyCode.add(
        "    " + cpuProfile.getPattern("call").assemblyTemplate.get(0).replace("{name}", name));
  }

  private void compileReadInput(String resultTemp, String typeStr) {
    String dataLabel = generateDataLabel("type");
    dataSection.add(
        cpuProfile
            .syntax
            .stringDirective
            .replace("{label}", dataLabel)
            .replace("{value}", escapeString(typeStr)));
    String arg0 = argumentRegisters.get(0);
    for (String t : cpuProfile.getPattern("load_address").assemblyTemplate) {
      assemblyCode.add("    " + t.replace("{dest}", arg0).replace("{label}", dataLabel));
    }
    assemblyCode.add(
        "    "
            + cpuProfile
                .getPattern("call")
                .assemblyTemplate
                .get(0)
                .replace("{name}", "runtime_read_input"));
    String retReg = cpuProfile.architecture.equals("x86_64") ? rax : x0;
    String dest = getPhysReg(resultTemp, scratchReg1, false);
    emitMove(dest, retReg);
    commitSpill(resultTemp, dest);
  }

  private String getPhysReg(Object operand, String scratchReg, boolean loadIfSpilled) {
    if (!(operand instanceof String)) return scratchReg;
    String temp = (String) operand;
    String reg = registerManager.getRegister(temp);
    if (reg != null) return reg;

    Integer offset = spiller.getSpillOffset(temp);
    if (offset == null) {
      spiller.forceSpill(temp);
      offset = spiller.getSpillOffset(temp);
    }

    if (loadIfSpilled) {
      String asm =
          cpuProfile
              .getPattern("load_from_stack")
              .assemblyTemplate
              .get(0)
              .replace("{dest_reg}", scratchReg)
              .replace("{offset}", String.valueOf(Math.abs(offset)));
      assemblyCode.add("    " + asm);
    }
    return scratchReg;
  }

  private void commitSpill(String temp, String srcReg) {
    if (registerManager.getRegister(temp) != null) return;
    Integer offset = spiller.getSpillOffset(temp);
    String asm =
        cpuProfile
            .getPattern("store_to_stack")
            .assemblyTemplate
            .get(0)
            .replace("{src_reg}", srcReg)
            .replace("{offset}", String.valueOf(Math.abs(offset)));
    assemblyCode.add("    " + asm);
  }

  private void emitMove(String dest, String src) {
    if (dest.equals(src)) return;
    assemblyCode.add(
        "    "
            + cpuProfile
                .getPattern("move_reg")
                .assemblyTemplate
                .get(0)
                .replace("{dest}", dest)
                .replace("{src}", src));
  }

  private String constructFinalAssembly(String methodName) {
    List<String> finalAsm = new ArrayList<String>();
    if (!dataSection.isEmpty()) {
      finalAsm.add(cpuProfile.syntax.dataSection);
      finalAsm.addAll(dataSection);
    }
    finalAsm.add(cpuProfile.syntax.textSection);
    if (cpuProfile.architecture.equals("x86_64")) {
      finalAsm.add("    global " + methodName);
      finalAsm.add(
          "    extern runtime_print, int_to_string, string_concat, array_new, array_load, array_store, runtime_read_input");
    } else {
      finalAsm.add("    .global " + methodName);
      finalAsm.add("    .extern int_to_string");
      finalAsm.add("    .extern string_concat");
      finalAsm.add("    .extern runtime_print");
      finalAsm.add("    .extern runtime_read_input");
      finalAsm.add("    .extern array_new");
      finalAsm.add("    .extern array_load");
      finalAsm.add("    .extern array_store");
    }
    finalAsm.add(methodName + ":");
    for (String t : cpuProfile.getPattern("prologue").assemblyTemplate) finalAsm.add("    " + t);

    int stackSize = spiller.getTotalSpillSize();
    if (stackSize > 0) {
      finalAsm.add(
          "    "
              + cpuProfile
                  .getPattern("alloc_stack_frame")
                  .assemblyTemplate
                  .get(0)
                  .replace("{size}", String.valueOf(stackSize)));
    }
    finalAsm.addAll(assemblyCode);
    for (String t : cpuProfile.getPattern("epilogue").assemblyTemplate) finalAsm.add("    " + t);

    StringBuilder sb = new StringBuilder();
    for (String s : finalAsm) sb.append(s).append("\n");
    return sb.toString();
  }

  private String generateDataLabel(String prefix) {
    return prefix + "_" + currentMethodName + "_" + (dataLabelCounter++);
  }

  private String escapeString(String str) {
    return str.replace("\"", "\\\"").replace("\n", "\\n");
  }
}
