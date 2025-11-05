package cod.compiler;

import cod.ast.nodes.*;
import cod.debug.DebugSystem;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BytecodeCompiler {
    private List<BytecodeInstruction> code = new ArrayList<BytecodeInstruction>();
    private Map<String, Integer> variableSlots = new HashMap<String, Integer>();
    private int nextSlot = 0;
    private int labelCounter = 0;

    // Pattern to parse combined step operators like "*+2", "/-3", "*2", "+1", "-1" etc.
    // Group 1: First operator (*, /, +, -) - Optional
    // Group 2: First number (digits, potentially signed like "+2" or "-3") - Optional (defaults based on op1)
    private static final Pattern STEP_OP_PATTERN = Pattern.compile("^([*/+-])?([+-]?\\d+)?$");


    // --- Helper Method ---
    /**
     * Checks if an expression node represents a constant integer value
     * (either a direct literal or a negated literal) and returns it.
     * Returns null if it's not a constant integer.
     */
    private Integer getConstantIntValue(ExprNode expr) {
        if (expr == null) {
            return null;
        }
        if (expr.value instanceof Integer) {
            return (Integer) expr.value;
        }
        // Check for negated integer literal (e.g., -5)
        if (expr instanceof UnaryNode) {
            UnaryNode unary = (UnaryNode) expr;
            if ("-".equals(unary.op) && unary.operand != null && unary.operand.value instanceof Integer) {
                return -(Integer) unary.operand.value;
            }
        }
        return null; // Not a constant integer we can easily evaluate
    }

    // --- Helper to check if a step expression is multiplicative/divisive ---
     private boolean isMultDivStep(ExprNode expr) {
        if (expr == null) return false;

        if (expr.name != null && expr.value == null) {
             String op = expr.name.trim();
             // FIX: Just check the prefix. This correctly identifies "*2" AND "*num"
             // as multiplicative, while "2" and "num" are not.
             if (op.startsWith("*") || op.startsWith("/")) return true;
        } else if (expr instanceof BinaryOpNode) {
            String op = ((BinaryOpNode)expr).op;
            // Check if the root operation is mult/div OR if it involves multiplicative operator strings
            if ("*".equals(op) || "/".equals(op) || "%".equals(op)) return true;
             // Recursively check if either side implies multiplication/division relative to 'i'
             // This handles cases like `by *2 + 1` (root is '+', but involves '*')
             if (isMultDivStep(((BinaryOpNode)expr).left)) return true;
             if (isMultDivStep(((BinaryOpNode)expr).right)) return true;
        }
        // Assignment steps like 'i *= 2' are handled separately in compileForLoop
        return false;
    }


    public BytecodeProgram compile(ProgramNode program) {
        DebugSystem.info("BYTECODE", "Starting MTOT bytecode compilation");
        BytecodeProgram result = new BytecodeProgram();
        if (program.unit != null) {
            compileUnit(program.unit, result);
        }
        int totalInstructions = 0;
        for (List<BytecodeInstruction> methodCode : result.getMethods().values()) {
            totalInstructions += methodCode.size();
        }
        DebugSystem.info("BYTECODE", "Compilation complete: " + totalInstructions +
            " instructions across " + result.getMethods().size() + " methods");
        return result;
    }

    private void compileUnit(UnitNode unit, BytecodeProgram program) {
        for (TypeNode type : unit.types) {
            compileType(type, program);
        }
    }

    private void compileType(TypeNode type, BytecodeProgram program) {
        for (MethodNode method : type.methods) {
            compileMethod(method, program);
        }
    }

    private void compileMethod(MethodNode method, BytecodeProgram program) {
        DebugSystem.debug("BYTECODE", "=== COMPILING METHOD: " + method.name + " ===");
        code.clear();
        variableSlots.clear();
        nextSlot = 0;
        labelCounter = 0;

        for (ParamNode param : method.parameters) {
            allocateVariableSlot(param.name);
             DebugSystem.debug("BYTECODE_VARS", "Allocated slot " + (nextSlot - 1) + " for param: " + param.name);
        }
        for (SlotNode slot : method.returnSlots) {
            allocateVariableSlot(slot.name);
             DebugSystem.debug("BYTECODE_VARS", "Allocated slot " + (nextSlot - 1) + " for return slot: " + slot.name);
        }
        for (StatementNode stmt : method.body) {
            compileStatement(stmt);
        }
        if (method.returnSlots.isEmpty()) {
            code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.PUSH_NULL));
        } else {
             code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.PUSH_NULL)); // Placeholder
             DebugSystem.warn("BYTECODE", "Slot return value mechanism not fully implemented in bytecode for method: " + method.name);
        }
        code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.RET));
        program.addMethod(method.name, new ArrayList<BytecodeInstruction>(code));
         DebugSystem.debug("BYTECODE", "=== FINISHED METHOD: " + method.name + " (" + code.size() + " instructions) ===");
    }

     private void compileStatement(StatementNode stmt) {
        if (stmt instanceof VarNode) { compileVariableDeclaration((VarNode) stmt); }
        else if (stmt instanceof AssignmentNode) { compileAssignment((AssignmentNode) stmt); }
        else if (stmt instanceof SlotAssignmentNode) { compileSlotAssignment((SlotAssignmentNode) stmt); }
        else if (stmt instanceof OutputNode) { compileOutput((OutputNode) stmt); }
        else if (stmt instanceof IfNode) { compileIfStatement((IfNode) stmt); }
        else if (stmt instanceof ForNode) { compileForLoop((ForNode) stmt); }
        else if (stmt instanceof MethodCallNode) {
            compileMethodCall((MethodCallNode) stmt);
             // Basic pop logic (needs refinement based on AST parent info)
             if (!isExpressionUsed(stmt, (MethodCallNode)stmt)) { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.POP)); }
        } else if (stmt instanceof ReturnSlotAssignmentNode) { compileReturnSlotAssignment((ReturnSlotAssignmentNode) stmt); }
        else if (stmt instanceof InputNode) { compileInput((InputNode) stmt); }
        else if (stmt instanceof FieldNode) { /* Ignore */ }
        else if (stmt instanceof ExprNode) {
             compileExpression((ExprNode) stmt);
              if (!isExpressionUsed(stmt, null)) { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.POP)); }
        } else { DebugSystem.warn("BYTECODE", "Unhandled statement type: " + stmt.getClass().getSimpleName()); }
    }

    // Crude check, needs AST parent info to be accurate
    private boolean isExpressionUsed(StatementNode stmt, MethodCallNode call) {
       // If it's a method call that specifies slots, assume it's used
       if (call != null && call.slotNames != null && !call.slotNames.isEmpty()) {
           return true;
       }
       // HACK: Don't pop 'main'
       if (call != null && "main".equals(call.name)) {
           return true;
       }
       
       return false; // Defaulting to pop if usage isn't obvious
    }


    private void compileSlotAssignment(SlotAssignmentNode assign) {
        compileExpression(assign.value);
        Integer slotIndex = variableSlots.get(assign.slotName);
        if (slotIndex != null) { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.STORE_SLOT, slotIndex)); }
        else { DebugSystem.error("BYTECODE", "Slot '" + assign.slotName + "' not found..."); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.POP)); }
    }

    private void compileAssignment(AssignmentNode assign) {
        if (assign.left instanceof IndexAccessNode) { /* Array store */ IndexAccessNode access = (IndexAccessNode) assign.left; compileExpression(access.array); compileExpression(access.index); compileExpression(assign.right); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.ARRAY_STORE)); }
        else if (assign.left.name != null) { /* Variable store */ String targetName = assign.left.name; compileExpression(assign.right); int slot = getOrAllocateVariableSlot(targetName); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.STORE_LOCAL, slot)); }
        else { DebugSystem.warn("BYTECODE", "Unhandled assignment target..."); compileExpression(assign.right); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.POP)); }
    }

    private void compileVariableDeclaration(VarNode var) {
        int slot = getOrAllocateVariableSlot(var.name);
        if (var.value != null) { compileExpression(var.value); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.STORE_LOCAL, slot)); }
        else { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.PUSH_NULL)); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.STORE_LOCAL, slot)); }
    }

    private void compileOutput(OutputNode output) {
        if (output.arguments.isEmpty()) { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.PUSH_STRING, "")); }
        else { compileExpression(output.arguments.get(0)); }
        code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.PRINT));
    }

    private void compileIfStatement(IfNode ifNode) {
        String elseLabel = generateLabel("else"); String endLabel = generateLabel("endif"); compileExpression(ifNode.condition); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.JMP_IF_FALSE, elseLabel)); for (StatementNode stmt : ifNode.thenBlock.statements) { compileStatement(stmt); } boolean hasElse = !ifNode.elseBlock.statements.isEmpty(); if (hasElse) { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.JMP, endLabel)); } placeLabel(elseLabel); if (hasElse) { for (StatementNode stmt : ifNode.elseBlock.statements) { compileStatement(stmt); } } placeLabel(endLabel);
    }

    // --- METHOD MODIFIED (FINAL LOOP FIX) ---
    private void compileForLoop(ForNode forNode) {
        int iteratorSlot = getOrAllocateVariableSlot(forNode.iterator);
        int endSlot = allocateVariableSlot(forNode.iterator + "_end");

        compileExpression(forNode.range.start); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.STORE_LOCAL, iteratorSlot));
        compileExpression(forNode.range.end); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.STORE_LOCAL, endSlot));

        ExprNode stepExpr = forNode.range.step;

        // CASE 1: Smart Default
        if (stepExpr == null) {
            // (Omitted for brevity, logic is correct)
             Integer startVal = getConstantIntValue(forNode.range.start); Integer endVal = getConstantIntValue(forNode.range.end); if (startVal != null && endVal != null) { boolean countUp = (startVal <= endVal); String loopBody = generateLabel("loop_body_" + (countUp ? "up" : "down")); String loopCheck = generateLabel("loop_check_" + (countUp ? "up" : "down")); String loopEnd = generateLabel("loop_end"); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.JMP, loopCheck)); placeLabel(loopBody); for (StatementNode stmt : forNode.body.statements) { compileStatement(stmt); } code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.LOAD_LOCAL, iteratorSlot)); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.PUSH_INT, countUp ? 1 : -1)); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.ADD_INT)); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.STORE_LOCAL, iteratorSlot)); placeLabel(loopCheck); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.LOAD_LOCAL, iteratorSlot)); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.LOAD_LOCAL, endSlot)); code.add(new BytecodeInstruction(countUp ? BytecodeInstruction.Opcode.CMP_LE_INT : BytecodeInstruction.Opcode.CMP_GE_INT)); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.JMP_IF_TRUE, loopBody)); placeLabel(loopEnd); } else { /* Runtime check needed */ String loopBodyUp = generateLabel("loop_body_up"); String loopCheckUp = generateLabel("loop_check_up"); String loopBodyDown = generateLabel("loop_body_down"); String loopCheckDown = generateLabel("loop_check_down"); String loopEnd = generateLabel("loop_end"); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.LOAD_LOCAL, iteratorSlot)); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.LOAD_LOCAL, endSlot)); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.CMP_LE_INT)); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.JMP_IF_TRUE, loopCheckUp)); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.JMP, loopCheckDown)); placeLabel(loopBodyDown); for (StatementNode stmt : forNode.body.statements) { compileStatement(stmt); } code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.LOAD_LOCAL, iteratorSlot)); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.PUSH_INT, -1)); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.ADD_INT)); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.STORE_LOCAL, iteratorSlot)); placeLabel(loopCheckDown); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.LOAD_LOCAL, iteratorSlot)); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.LOAD_LOCAL, endSlot)); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.CMP_GE_INT)); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.JMP_IF_TRUE, loopBodyDown)); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.JMP, loopEnd)); placeLabel(loopBodyUp); for (StatementNode stmt : forNode.body.statements) { compileStatement(stmt); } code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.LOAD_LOCAL, iteratorSlot)); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.PUSH_INT, 1)); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.ADD_INT)); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.STORE_LOCAL, iteratorSlot)); placeLabel(loopCheckUp); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.LOAD_LOCAL, iteratorSlot)); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.LOAD_LOCAL, endSlot)); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.CMP_LE_INT)); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.JMP_IF_TRUE, loopBodyUp)); placeLabel(loopEnd); }
        // CASE 2: Explicit Step
        } else {
            String loopStart = generateLabel("loop_start"); String loopContinueCheck = generateLabel("loop_cont"); String loopEnd = generateLabel("loop_end");

            // --- Static analysis for direction ---
            boolean isLikelyCountdown = false;
            // (Omitted for brevity, logic is correct for basic cases)
             Integer stepConst = getConstantIntValue(stepExpr); if (stepConst != null && stepConst < 0) { isLikelyCountdown = true; } else if (stepExpr instanceof UnaryNode && "-".equals(((UnaryNode)stepExpr).op)) { isLikelyCountdown = true; } else if (stepExpr instanceof BinaryOpNode) { String op = ((BinaryOpNode)stepExpr).op; if (op.equals("-=") || op.equals("/=")) { isLikelyCountdown = true; } else if (op.equals("=")) { ExprNode rhs = ((BinaryOpNode)stepExpr).right; if (rhs instanceof BinaryOpNode) { String rhsOp = ((BinaryOpNode)rhs).op; ExprNode rhsLeft = ((BinaryOpNode)rhs).left; if (rhsLeft != null && rhsLeft.name != null && rhsLeft.name.equals(forNode.iterator)) { if (rhsOp.equals("-") || rhsOp.equals("/")) { isLikelyCountdown = true; } } } else { Integer constRhs = getConstantIntValue(rhs); if(constRhs != null && constRhs < 0) isLikelyCountdown = true; } } else { if (op.equals("-") || op.equals("/")) isLikelyCountdown = true; Integer constRight = getConstantIntValue(((BinaryOpNode)stepExpr).right); if(constRight != null && constRight < 0 && (op.equals("+") || op.equals("*"))) isLikelyCountdown = true; } } else if (stepExpr.value instanceof String || stepExpr.name != null) { String op = (stepExpr.value != null) ? (String)stepExpr.value : stepExpr.name; if (op != null) { op = op.trim(); if (op.startsWith("-") || op.startsWith("/")) { isLikelyCountdown = true; } else if (op.startsWith("*") || op.startsWith("/")) { try { String numStr = op.substring(1); int value = Integer.parseInt(numStr); if (value < 0) isLikelyCountdown = true; } catch (Exception e) {} } else { try { if(Integer.parseInt(op) < 0) isLikelyCountdown = true; } catch(Exception e){} } } }
            DebugSystem.debug("BYTECODE", "For-loop '" + forNode.iterator + "' (explicit step): Compile-time direction assumption -> " + (isLikelyCountdown ? "DOWN" : "UP"));


            code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.JMP, loopContinueCheck));
            placeLabel(loopStart);
            for (StatementNode stmt : forNode.body.statements) { compileStatement(stmt); }

            // --- MODIFIED STEP LOGIC (FINAL FIX) ---

            boolean isAssignment = (stepExpr instanceof BinaryOpNode &&
                                    (((BinaryOpNode)stepExpr).op.endsWith("=") || "=".equals(((BinaryOpNode)stepExpr).op)));

            if (isAssignment) {
                BinaryOpNode binOp = (BinaryOpNode) stepExpr;
                if ("=".equals(binOp.op)) {
                    // "by i = i * 2 + 1" -> RHS is the new value
                    compileStepRHS(binOp.right, iteratorSlot);
                } else {
                    // "by i += 1", "by i *= 2" -> Load i, compile RHS, apply op
                    code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.LOAD_LOCAL, iteratorSlot));
                    compileStepRHS(binOp.right, iteratorSlot); // Compile RHS

                    String op = binOp.op;
                    if ("+=".equals(op)) { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.ADD_INT)); }
                    else if ("-=".equals(op)) { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.SUB_INT)); }
                    else if ("*=".equals(op)) { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.MUL_INT)); }
                    else if ("/=".equals(op)) { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.DIV_INT)); }
                    else { DebugSystem.warn("BYTECODE_STEP", "Unhandled compound assignment in step: " + op); }
                }
            }
            // --- It's a "SIMPLE" step (by 2, by steps, by *2, by *2 + 1, by *+2) ---
            else {
                boolean isMultDiv = isMultDivStep(stepExpr);

                // Multiplicative/Divisive step ("*2", "/2", "*2 + 1", "*+2")
                // The result of this expression *replaces* i.
                if (isMultDiv) {
                    compileStepRHS(stepExpr, iteratorSlot);
                }
                // Additive/Subtractive step ("2", "steps", "+1", "-1")
                // This expression is *added* to i.
                else {
                    code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.LOAD_LOCAL, iteratorSlot)); // Load 'i'
                    compileStepRHS(stepExpr, iteratorSlot); // Compile the step value/expr
                    code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.ADD_INT)); // i + (result of expr)
                }
            }

            // --- END MODIFIED STEP LOGIC ---
            code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.STORE_LOCAL, iteratorSlot));

            // Condition Check (same)
            placeLabel(loopContinueCheck);
            code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.LOAD_LOCAL, iteratorSlot)); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.LOAD_LOCAL, endSlot)); code.add(new BytecodeInstruction(isLikelyCountdown ? BytecodeInstruction.Opcode.CMP_GE_INT : BytecodeInstruction.Opcode.CMP_LE_INT)); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.JMP_IF_TRUE, loopStart));
            placeLabel(loopEnd);
        }
    }
// Fix the step calculation logic
private void compileStepRHS(ExprNode expr, int iteratorSlot) {
    if (expr instanceof BinaryOpNode) {
        BinaryOpNode binOp = (BinaryOpNode) expr;
        
        // Handle assignment operations in steps (e.g., "by i = i + 1")
        // Note: This logic seems to be from your existing code for "by i = ..."
        if (binOp.op.equals("=") || binOp.op.endsWith("=")) {
            compileStepRHS(binOp.right, iteratorSlot);
            return;
        }
        
        // Standard binary operation (e.g., "by i * 2 + 1")
        compileStepRHS(binOp.left, iteratorSlot);
        compileStepRHS(binOp.right, iteratorSlot);
        
        String op = binOp.op;
        if ("+".equals(op)) { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.ADD_INT)); }
        else if ("-".equals(op)) { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.SUB_INT)); }
        else if ("*".equals(op)) { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.MUL_INT)); }
        else if ("/".equals(op)) { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.DIV_INT)); }
        else if ("%".equals(op)) { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.MOD_INT)); }
        return;
    }
    
    if (expr instanceof UnaryNode) {
        compileStepRHS(((UnaryNode)expr).operand, iteratorSlot);
        if ("-".equals(((UnaryNode)expr).op)) { 
            code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.NEG_INT)); 
        }
        return;
    }
    
    // --- NEW LOGIC FOR HANDLING STEP NAMES ---
    
    if (expr.name != null) {
        String name = expr.name.trim();

        // CASE 1: It's an operator string (e.g., "*2", "/-1", "*num", "/steps")
        if (name.startsWith("*") || name.startsWith("/")) {
            char op = name.charAt(0);
            String operandName = name.substring(1).trim();
            
            if (operandName.isEmpty()) {
                DebugSystem.error("BYTECODE_STEP", "Multiplicative step operator '" + op + "' has no operand. Defaulting to 1.");
                code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.LOAD_LOCAL, iteratorSlot)); 
                code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.PUSH_INT, 1)); 
            } else {
                // Load i first for multiplicative steps
                code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.LOAD_LOCAL, iteratorSlot));

                // Now parse the operand (which could be a literal OR a variable)
                Integer operandSlot = variableSlots.get(operandName);
                if (operandSlot != null) {
                    // It's a variable (e.g., "*num")
                    code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.LOAD_LOCAL, operandSlot));
                } else {
                    // Try to parse as a literal (e.g., "*2")
                    try {
                        int value = Integer.parseInt(operandName);
                        code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.PUSH_INT, value));
                    } catch (NumberFormatException e2) {
                        DebugSystem.error("BYTECODE_STEP", "Step operand '" + operandName + "' is not a known variable or an integer literal. Defaulting to 1.");
                        code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.PUSH_INT, 1)); 
                    }
                }
            }

            // Add the operation
            if (op == '*') {
                code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.MUL_INT));
            } else {
                code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.DIV_INT));
            }
            return;
        }

        // CASE 2: It's a simple variable (e.g., "i", "num", "steps")
        Integer slot = variableSlots.get(name);
        if (slot != null) {
            code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.LOAD_LOCAL, slot));
            return;
        }

        // CASE 3: It's a simple literal number (e.g., "2", "-1")
        try {
            int value = Integer.parseInt(name);
            code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.PUSH_INT, value));
            return;
        } catch (NumberFormatException e) {
            // Not a simple number, and not an operator string we handled
        }

        // CASE 4: Fallback
        DebugSystem.warn("BYTECODE_STEP", "Unhandled step expression name: " + name + ". Defaulting to 1.");
        code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.PUSH_INT, 1));
        return;
    }
    // --- END NEW LOGIC ---

    
    if (expr.value != null) {
        compileExpression(expr);
        return;
    }
    
    // Fallback: push 1 as default step
    DebugSystem.warn("BYTECODE_STEP", "Unhandled step expression, defaulting to 1");
    code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.PUSH_INT, 1));
}


    private void compileMethodCall(MethodCallNode call) {
        for (ExprNode arg : call.arguments) { compileExpression(arg); }
        code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.CALL, call.qualifiedName));
    }

    private void compileReturnSlotAssignment(ReturnSlotAssignmentNode assignment) {
         for (ExprNode arg : assignment.methodCall.arguments) { compileExpression(arg); }
         int numSlotsToReceive = assignment.variableNames.size(); String methodName = assignment.methodCall.qualifiedName; Object[] callOperand = new Object[] { methodName, numSlotsToReceive }; code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.CALL_SLOTS, callOperand));
         for (int i = numSlotsToReceive - 1; i >= 0; i--) { String varName = assignment.variableNames.get(i); int slot = getOrAllocateVariableSlot(varName); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.STORE_LOCAL, slot)); }
         DebugSystem.debug("BYTECODE", "Compiled ReturnSlotAssignment for " + numSlotsToReceive + " slots from " + methodName);
    }

    private void compileInput(InputNode input) {
         code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.READ_INPUT, input.targetType)); int slot = getOrAllocateVariableSlot(input.variableName); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.STORE_LOCAL, slot));
         DebugSystem.debug("BYTECODE", "Compiled InputNode for " + input.variableName + " (type: "+input.targetType+")");
    }

    private void compileExpression(ExprNode expr) {
        if (expr == null) { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.PUSH_NULL)); return; }
        if (expr instanceof UnaryNode) { compileUnary((UnaryNode) expr); }
        else if (expr instanceof BinaryOpNode) { compileBinaryOperation((BinaryOpNode) expr); }
        else if (expr instanceof ArrayNode) { compileArrayLiteral((ArrayNode) expr); }
        else if (expr instanceof IndexAccessNode) { compileArrayAccess((IndexAccessNode) expr); }
        else if (expr instanceof MethodCallNode) { compileMethodCall((MethodCallNode) expr); }
        else if (expr instanceof TypeCastNode) { compileTypeCast((TypeCastNode) expr); }
        else if (expr.name != null) {
            // Generic expression compiler ONLY understands variables.
            Integer slot = variableSlots.get(expr.name);
            if (slot != null) {
                code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.LOAD_LOCAL, slot));
            } else {
                DebugSystem.warn("BYTECODE", "Loading unknown name potentially as field: " + expr.name);
                code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.LOAD_FIELD, expr.name));
            }
        }
        else if (expr.value != null) { if (expr.value instanceof Integer) { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.PUSH_INT, expr.value)); } else if (expr.value instanceof Float) { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.PUSH_FLOAT, expr.value)); } else if (expr.value instanceof String) { String str = (String) expr.value; if (str.length() >= 2 && str.startsWith("\"") && str.endsWith("\"")) { str = str.substring(1, str.length() - 1); } code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.PUSH_STRING, str)); } else if (expr.value instanceof Boolean) { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.PUSH_BOOL, expr.value)); } else { DebugSystem.warn("BYTECODE", "Unhandled literal type: " + expr.value.getClass().getName()); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.PUSH_NULL)); } }
        else { DebugSystem.warn("BYTECODE", "Unhandled expression type: " + expr.getClass().getSimpleName()); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.PUSH_NULL)); }
    }

    private void compileUnary(UnaryNode unary) {
         compileExpression(unary.operand);
         switch (unary.op) {
             case "-": code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.NEG_INT)); break;
             case "+": break; // Unary plus is a no-op
             default: DebugSystem.warn("BYTECODE", "Unhandled unary operator: " + unary.op); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.POP));
         }
    }

    private void compileTypeCast(TypeCastNode cast) {
         compileExpression(cast.expression); DebugSystem.warn("BYTECODE", "CAST instruction bytecode generation not implemented.");
    }

    private void compileArrayLiteral(ArrayNode literal) {
        int size = literal.elements.size();
        code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.PUSH_INT, size));
        code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.ARRAY_NEW));
        for (int i = 0; i < size; i++) {
            code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.DUP));
            code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.PUSH_INT, i));
            compileExpression(literal.elements.get(i));
            code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.ARRAY_STORE));
        }
    }


    private void compileArrayAccess(IndexAccessNode access) {
        compileExpression(access.array); compileExpression(access.index); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.ARRAY_LOAD));
    }

    private void compileBinaryOperation(BinaryOpNode binOp) {
        String op = binOp.op;

        // --- '+' is special for strings ---
        if ("+".equals(op)) {
            compileExpression(binOp.left);
            boolean leftIsString = expressionMightBeString(binOp.left);

            compileExpression(binOp.right);
            boolean rightIsString = expressionMightBeString(binOp.right);

            if (leftIsString && rightIsString) {
                code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.CONCAT_STRING));
            } else if (leftIsString && !rightIsString) {
                // Convert right (int/float/bool) to string
                code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.INT_TO_STRING)); // Needs type check for non-int
                code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.CONCAT_STRING));
            } else if (!leftIsString && rightIsString) {
                // Convert left (int/float/bool) to string
                code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.SWAP));
                code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.INT_TO_STRING)); // Needs type check for non-int
                code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.CONCAT_STRING));
            } else {
                // Both numeric
                code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.ADD_INT)); // Needs float support
            }
            return;
        }

        // --- All other operators are numeric (for now) ---
        compileExpression(binOp.left);
        compileExpression(binOp.right);

        if ("-".equals(op)) { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.SUB_INT)); }
        else if ("*".equals(op)) { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.MUL_INT)); }
        else if ("/".equals(op)) { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.DIV_INT)); }
        else if ("%".equals(op)) { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.MOD_INT)); }
        else if ("==".equals(op)) { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.CMP_EQ_INT)); }
        else if ("!=".equals(op)) { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.CMP_NE_INT)); }
        else if ("<".equals(op)) { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.CMP_LT_INT)); }
        else if ("<=".equals(op)) { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.CMP_LE_INT)); }
        else if (">".equals(op)) { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.CMP_GT_INT)); }
        else if (">=".equals(op)) { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.CMP_GE_INT)); }
        else { DebugSystem.warn("BYTECODE", "Unhandled binary operator: " + op); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.POP)); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.POP)); code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.PUSH_NULL)); }
    }


    private boolean expressionMightBeString(ExprNode expr) {
        // Basic check, needs type inference for accuracy
        if (expr == null) return false;
        if (expr.value instanceof String) return true;
        if (expr instanceof BinaryOpNode && "+".equals(((BinaryOpNode)expr).op)) return true; // '+' often means concat
        if (expr instanceof MethodCallNode) return true; // Assume methods might return strings
        if (expr instanceof TypeCastNode && "string".equals(((TypeCastNode)expr).targetType)) return true;
        // Check if variable is known to be string (requires symbol table)
        return false;
    }


    private int allocateVariableSlot(String name) {
        if (variableSlots.containsKey(name)) { DebugSystem.debug("BYTECODE_VARS", "Variable '" + name + "' is being re-allocated (shadowing or temp slot)."); }
        int slot = nextSlot++; variableSlots.put(name, slot); return slot;
    }

    private int getExistingVariableSlot(String name) { Integer slot = variableSlots.get(name); if (slot == null) { throw new IllegalStateException("Attempted to get slot for unallocated variable/param: " + name); } return slot; }
    private int getOrAllocateVariableSlot(String name) { if (!variableSlots.containsKey(name)) { return allocateVariableSlot(name); } return variableSlots.get(name); }
    private String generateLabel(String prefix) { return prefix + "_" + (labelCounter++); }
    private void placeLabel(String label) { code.add(new BytecodeInstruction(BytecodeInstruction.Opcode.LABEL, label)); }

} // End of BytecodeCompiler class