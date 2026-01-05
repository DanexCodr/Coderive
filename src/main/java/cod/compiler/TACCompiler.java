package cod.compiler;

import cod.ast.ASTVisitor;
import cod.ast.nodes.*;
import cod.compiler.TACInstruction.Opcode;
import cod.debug.DebugSystem;
import java.util.*;

public class TACCompiler extends ASTVisitor<String> {

    private TACProgram tacProgram;
    private List<TACInstruction> currentCode;
    
    private Map<String, String> variableToTempMap = new HashMap<String, String>();
    private int labelCounter = 0;
    private int nextTempId = 0;
    private String currentMethodName = "";

    public TACProgram compile(ProgramNode program) {
        DebugSystem.info("TAC", "Starting Visitor-based TAC compilation");
        this.tacProgram = new TACProgram();
        visit(program); 
        return tacProgram;
    }

    private void emit(TACInstruction instr) {
        currentCode.add(instr);
    }

    private String newTemp() {
        return "$t" + (nextTempId++);
    }

    private String getNewLabel(String prefix) {
        return "L_" + currentMethodName + "_" + prefix + "_" + (labelCounter++);
    }

    private String allocateVariable(String name) {
        String tacName = "$v_" + name;
        variableToTempMap.put(name, tacName);
        return tacName;
    }

    private String getOrAllocateVariable(String name) {
        if (!variableToTempMap.containsKey(name)) {
            return allocateVariable(name);
        }
        return variableToTempMap.get(name);
    }

    // --- Structural Visitors ---

    @Override
    public String visit(ProgramNode node) {
        if (node.unit != null) visit(node.unit);
        return null;
    }

    @Override
    public String visit(UnitNode node) {
        for (TypeNode type : node.types) visit(type);
        return null;
    }

    @Override
    public String visit(TypeNode node) {
        for (MethodNode method : node.methods) visit(method);
        return null;
    }

    @Override
    public String visit(MethodNode node) {
        currentCode = new ArrayList<TACInstruction>();
        variableToTempMap.clear();
        labelCounter = 0;
        nextTempId = 0;
        currentMethodName = node.methodName;

        // Allocate variables for parameters
        for (ParamNode param : node.parameters) allocateVariable(param.name);
        
        // Allocate variables for return slots
        for (SlotNode slot : node.returnSlots) allocateVariable(slot.name);

        // Compile node body
        for (StmtNode stmt : node.body) visit(stmt);

        // Add implicit return if missing
        if (currentCode.isEmpty() || currentCode.get(currentCode.size() - 1).opcode != Opcode.RET) {
            if (!node.returnSlots.isEmpty()) {
                String firstSlot = variableToTempMap.get(node.returnSlots.get(0).name);
                emit(new TACInstruction(Opcode.RET, firstSlot));
            } else {
                emit(new TACInstruction(Opcode.RET));
            }
        }

        tacProgram.addMethod(node.methodName, new ArrayList<TACInstruction>(currentCode));
        return null;
    }

    // --- Statement Visitors ---

    @Override
    public String visit(VarNode node) {
        String targetTemp = getOrAllocateVariable(node.name);
        if (node.value != null) {
            String valTemp = dispatch(node.value);
            if (valTemp != null) {
                emit(new TACInstruction(Opcode.ASSIGN, targetTemp, valTemp));
            }
        }
        return null;
    }

    @Override
    public String visit(AssignmentNode node) {
        if (node.left instanceof IndexAccessNode) {
            IndexAccessNode access = (IndexAccessNode) node.left;
            String arrTemp = dispatch(access.array);
            String idxTemp = dispatch(access.index);
            String valTemp = dispatch(node.right);
            emit(new TACInstruction(Opcode.STORE_ARRAY, valTemp, arrTemp, idxTemp));
        } 
        else if (node.left instanceof ExprNode) {
            String name = ((ExprNode) node.left).name;
            String valTemp = dispatch(node.right);
            String targetTemp = getOrAllocateVariable(name);
            emit(new TACInstruction(Opcode.ASSIGN, targetTemp, valTemp));
        }
        return null;
    }

    @Override
    public String visit(SlotAssignmentNode node) {
        String valTemp = dispatch(node.value);
        String targetName = node.slotName;
        String targetTemp = getOrAllocateVariable(targetName);
        emit(new TACInstruction(Opcode.ASSIGN, targetTemp, valTemp));
        return null;
    }

    @Override
    public String visit(MultipleSlotAssignmentNode node) {
        for (SlotAssignmentNode sub : node.assignments) visit(sub);
        return null;
    }

    @Override
    public String visit(ReturnSlotAssignmentNode node) {
        MethodCallNode call = node.methodCall;
        
        // Step 1: Push all arguments
        for (ExprNode arg : call.arguments) {
            String t = dispatch(arg);
            emit(new TACInstruction(Opcode.PARAM, t));
        }
        
        // Step 2: Call node - returns primary result (could be array/map)
        String primaryRet = newTemp();
        
        // Use CALL_SLOTS to indicate this returns multiple values
        // Operand1: node name
        // Operand2: argument count
        // No need for slot count in operand since we handle it in runtime
        emit(new TACInstruction(Opcode.CALL_SLOTS, primaryRet, call.qualifiedName, call.arguments.size()));
        
        // Step 3: Extract each slot to destination variables
        for (int i = 0; i < node.variableNames.size(); i++) {
            String targetVar = node.variableNames.get(i);
            String targetTemp = getOrAllocateVariable(targetVar);
            
            if (i == 0) {
                // First variable gets the primary return
                emit(new TACInstruction(Opcode.ASSIGN, targetTemp, primaryRet));
            } else {
                // Additional variables from array (if node returns array)
                // For now, assume node returns array with slots at indices
                String idxTemp = newTemp();
                emit(new TACInstruction(Opcode.LOAD_IMM, idxTemp, i));
                emit(new TACInstruction(Opcode.LOAD_ARRAY, targetTemp, primaryRet, idxTemp));
            }
        }
        return null;
    }

    @Override
    public String visit(StmtIfNode node) {
        String elseLabel = getNewLabel("else");
        String endLabel = getNewLabel("endif");

        String condTemp = dispatch(node.condition);
        
        // Compare condition with 0 (false)
        String zeroTemp = newTemp();
        emit(new TACInstruction(Opcode.LOAD_IMM, zeroTemp, 0));
        emit(new TACInstruction(Opcode.IF_GOTO, condTemp, zeroTemp, elseLabel)); 

        // Then block
        visitBlock(node.thenBlock);

        boolean hasElse = node.elseBlock != null && !node.elseBlock.statements.isEmpty();
        if (hasElse) {
            emit(new TACInstruction(Opcode.GOTO, endLabel));
        }

        // Else block
        emit(new TACInstruction(Opcode.LABEL, elseLabel));
        if (hasElse) {
            visitBlock(node.elseBlock);
        }
        
        emit(new TACInstruction(Opcode.LABEL, endLabel));
        return null;
    }

    private void visitBlock(BlockNode block) {
        if (block == null) return;
        for (StmtNode s : block.statements) visit(s);
    }

    @Override
    public String visit(ForNode node) {
        String iterTemp = getOrAllocateVariable(node.iterator);
        
        // Evaluate start expression
        String startTemp = dispatch(node.range.start);
        if (startTemp == null) {
            startTemp = newTemp();
            emit(new TACInstruction(Opcode.LOAD_IMM, startTemp, 0));
        }
        
        // Evaluate end expression  
        String endTemp = dispatch(node.range.end);
        if (endTemp == null) {
            endTemp = newTemp();
            emit(new TACInstruction(Opcode.LOAD_IMM, endTemp, 0));
        }

        // Evaluate step expression (or default to 1)
        String stepTemp = newTemp();
        if (node.range.step != null) {
            String stepVal = dispatch(node.range.step);
            if (stepVal != null) {
                stepTemp = stepVal;
            } else {
                emit(new TACInstruction(Opcode.LOAD_IMM, stepTemp, 1));
            }
        } else {
            emit(new TACInstruction(Opcode.LOAD_IMM, stepTemp, 1));
        }
        
        emit(new TACInstruction(Opcode.ASSIGN, iterTemp, startTemp));

        String startLabel = getNewLabel("loop_start");
        String endLabel = getNewLabel("loop_end");
        String checkNegativeLabel = getNewLabel("check_neg");
        String checkDoneLabel = getNewLabel("check_done");

        emit(new TACInstruction(Opcode.LABEL, startLabel));
        
        // --- BEGIN Dynamic Loop Condition Check ---
        
        // 1. Determine direction: Check if step is positive (>= 0)
        String zeroTemp = newTemp();
        emit(new TACInstruction(Opcode.LOAD_IMM, zeroTemp, 0));
        
        String isPositive = newTemp();
        emit(new TACInstruction(Opcode.CMP_GE, isPositive, stepTemp, zeroTemp));

        // If step is NOT positive (i.e., step < 0), jump to check_neg
        emit(new TACInstruction(Opcode.IF_GOTO, isPositive, zeroTemp, checkNegativeLabel));
        
        // Fallthrough: Positive Step (Check: iter > end -> Break)
        String condPos = newTemp();
        emit(new TACInstruction(Opcode.CMP_GT, condPos, iterTemp, endTemp));
        String oneTemp = newTemp();
        emit(new TACInstruction(Opcode.LOAD_IMM, oneTemp, 1));
        emit(new TACInstruction(Opcode.IF_GOTO, condPos, oneTemp, endLabel));
        emit(new TACInstruction(Opcode.GOTO, checkDoneLabel)); // Skip negative check

        // Negative Step Check (Check: iter < end -> Break)
        emit(new TACInstruction(Opcode.LABEL, checkNegativeLabel));
        String condNeg = newTemp();
        emit(new TACInstruction(Opcode.CMP_LT, condNeg, iterTemp, endTemp));
        emit(new TACInstruction(Opcode.LOAD_IMM, oneTemp, 1)); // Reuse oneTemp
        emit(new TACInstruction(Opcode.IF_GOTO, condNeg, oneTemp, endLabel));

        emit(new TACInstruction(Opcode.LABEL, checkDoneLabel));
        
        // --- END Dynamic Loop Condition Check ---

        // Loop body
        visitBlock(node.body);

        // Increment iterator
        String nextIter = newTemp();
        // The step variable (stepTemp) holds the value, positive or negative
        emit(new TACInstruction(Opcode.ADD, nextIter, iterTemp, stepTemp));
        emit(new TACInstruction(Opcode.ASSIGN, iterTemp, nextIter));

        emit(new TACInstruction(Opcode.GOTO, startLabel));
        emit(new TACInstruction(Opcode.LABEL, endLabel));
        return null;
    }

    @Override
    public String visit(BinaryOpNode node) {
        System.err.println("DEBUG BinaryOpNode: " + node.op);
        
        // ALWAYS get values for both operands
        String leftTemp = dispatch(node.left);
        if (leftTemp == null) {
            leftTemp = newTemp();
            System.err.println("  WARNING: left operand returned null, using default");
            emit(new TACInstruction(Opcode.LOAD_IMM, leftTemp, 0));
        }
        
        String rightTemp = dispatch(node.right);
        if (rightTemp == null) {
            rightTemp = newTemp();
            System.err.println("  WARNING: right operand returned null, using default");
            emit(new TACInstruction(Opcode.LOAD_IMM, rightTemp, 0));
        }
        
        String resultTemp = newTemp();
        
        // Handle string concatenation
        if ("+".equals(node.op)) {
            boolean leftIsString = isStringNode(node.left);
            boolean rightIsString = isStringNode(node.right);
            
            if (leftIsString || rightIsString) {
                // String concatenation
                if (!leftIsString) {
                    String leftStr = newTemp();
                    emit(new TACInstruction(Opcode.INT_TO_STRING, leftStr, leftTemp));
                    leftTemp = leftStr;
                }
                if (!rightIsString) {
                    String rightStr = newTemp();
                    emit(new TACInstruction(Opcode.INT_TO_STRING, rightStr, rightTemp));
                    rightTemp = rightStr;
                }
                emit(new TACInstruction(Opcode.CONCAT, resultTemp, leftTemp, rightTemp));
                return resultTemp;
            }
        }
        
        // Handle other binary operations
        Opcode opcode = mapOp(node.op);
        emit(new TACInstruction(opcode, resultTemp, leftTemp, rightTemp));
        return resultTemp;
    }

    private boolean isStringNode(ExprNode node) {
        if (node == null) return false;
        if (node.value instanceof String) return true;
        if (node instanceof BinaryOpNode && "+".equals(((BinaryOpNode)node).op)) {
            return isStringNode(((BinaryOpNode)node).left) || isStringNode(((BinaryOpNode)node).right);
        }
        return false;
    }

    @Override
    public String visit(UnaryNode node) {
        System.err.println("DEBUG UnaryNode: op=" + node.op);
        
        // Get operand value
        String operandTemp = dispatch(node.operand);
        if (operandTemp == null) {
            operandTemp = newTemp();
            System.err.println("  WARNING: operand returned null, using default");
            
            // Try to extract value from ExprNode
            if (node.operand instanceof ExprNode) {
                ExprNode expr = (ExprNode) node.operand;
                if (expr.value instanceof Integer) {
                    int val = (Integer) expr.value;
                    emit(new TACInstruction(Opcode.LOAD_IMM, operandTemp, val));
                } else if (expr.value instanceof Float) {
                    float val = (Float) expr.value;
                    emit(new TACInstruction(Opcode.LOAD_IMM, operandTemp, val));
                } else {
                    emit(new TACInstruction(Opcode.LOAD_IMM, operandTemp, 0));
                }
            } else {
                emit(new TACInstruction(Opcode.LOAD_IMM, operandTemp, 0));
            }
        }
        
        String resultTemp = newTemp();
        
        if ("-".equals(node.op)) {
            // Check if we can load immediate negative value
            if (node.operand instanceof ExprNode) {
                ExprNode expr = (ExprNode) node.operand;
                if (expr.value instanceof Integer) {
                    int val = (Integer) expr.value;
                    System.err.println("  Loading immediate negative: -" + val);
                    emit(new TACInstruction(Opcode.LOAD_IMM, resultTemp, -val));
                    return resultTemp;
                } else if (expr.value instanceof Float) {
                    float val = (Float) expr.value;
                    System.err.println("  Loading immediate negative: -" + val);
                    emit(new TACInstruction(Opcode.LOAD_IMM, resultTemp, -val));
                    return resultTemp;
                }
            }
            // General case: use NEG instruction
            System.err.println("  Using NEG instruction");
            emit(new TACInstruction(Opcode.NEG, resultTemp, operandTemp));
        } else if ("!".equals(node.op)) {
            // Logical NOT
            String zeroTemp = newTemp();
            emit(new TACInstruction(Opcode.LOAD_IMM, zeroTemp, 0));
            emit(new TACInstruction(Opcode.CMP_EQ, resultTemp, operandTemp, zeroTemp));
        } else {
            // Unary plus or other
            emit(new TACInstruction(Opcode.ASSIGN, resultTemp, operandTemp));
        }
        
        return resultTemp;
    }

    @Override
    public String visit(MethodCallNode node) {
        // Push arguments
        for (ExprNode arg : node.arguments) {
            String t = dispatch(arg);
            emit(new TACInstruction(Opcode.PARAM, t));
        }
        
        String result = newTemp();
        
        // Check if it's a slot call
        if (!node.slotNames.isEmpty()) {
            // For slot calls, we need to handle multiple returns
            emit(new TACInstruction(Opcode.CALL_SLOTS, result, node.qualifiedName, node.arguments.size()));
            
            // Extract slots if needed (for standalone slot calls)
            for (int i = 0; i < node.slotNames.size(); i++) {
                String slotName = node.slotNames.get(i);
                String slotTemp = getOrAllocateVariable(slotName);
                
                if (i == 0) {
                    // First slot is primary return
                    emit(new TACInstruction(Opcode.ASSIGN, slotTemp, result));
                } else {
                    // Additional slots from array
                    String idxTemp = newTemp();
                    emit(new TACInstruction(Opcode.LOAD_IMM, idxTemp, i));
                    emit(new TACInstruction(Opcode.LOAD_ARRAY, slotTemp, result, idxTemp));
                }
            }
        } else {
            // Regular node call
            emit(new TACInstruction(Opcode.CALL, result, node.qualifiedName, node.arguments.size()));
        }
        
        return result;
    }

    private String visitStatementExpression(ExprNode expr) {
        if (expr instanceof MethodCallNode) {
            MethodCallNode call = (MethodCallNode) expr;
            // Visit but don't use the result
            visit(call);
            return null;
        } else {
            // For other expressions, just evaluate
            return visit(expr);
        }
    }

    @Override
    public String visit(ExprNode node) {
        System.err.println("DEBUG ExprNode: name=" + node.name + ", value=" + node.value);
        
        if (node.name != null) {
            // Variable reference
            String varName = getOrAllocateVariable(node.name);
            System.err.println("  -> Variable: " + node.name + " -> " + varName);
            return varName;
        }
        
        if (node.value != null) {
            // Literal value
            String temp = newTemp();
            if (node.value instanceof String) {
                String str = (String) node.value;
                // Remove quotes if present
                if (str.startsWith("\"") && str.endsWith("\"")) {
                    str = str.substring(1, str.length() - 1);
                }
                System.err.println("  -> String literal: '" + str + "' -> " + temp);
                emit(new TACInstruction(Opcode.LOAD_ADDR, temp, str));
            } else {
                System.err.println("  -> Literal: " + node.value + " -> " + temp);
                emit(new TACInstruction(Opcode.LOAD_IMM, temp, node.value));
            }
            return temp;
        }
        
        if (node.isNull) {
            // null literal
            String temp = newTemp();
            System.err.println("  -> null -> " + temp);
            emit(new TACInstruction(Opcode.LOAD_IMM, temp, 0));
            return temp;
        }
        
        // Empty expression - create default
        String temp = newTemp();
        System.err.println("  WARNING: Empty ExprNode, using default 0 -> " + temp);
        emit(new TACInstruction(Opcode.LOAD_IMM, temp, 0));
        return temp;
    }

    @Override
    public String visit(IndexAccessNode node) {
        String arr = dispatch(node.array);
        String idx = dispatch(node.index);
        String res = newTemp();
        emit(new TACInstruction(Opcode.LOAD_ARRAY, res, arr, idx));
        return res;
    }

    @Override
    public String visit(ArrayNode node) {
        String sizeTemp = newTemp();
        emit(new TACInstruction(Opcode.LOAD_IMM, sizeTemp, node.elements.size()));
        String arrTemp = newTemp();
        emit(new TACInstruction(Opcode.ARRAY_NEW, arrTemp, sizeTemp));
        
        // Store each element
        for (int i = 0; i < node.elements.size(); i++) {
            String val = dispatch(node.elements.get(i));
            String idx = newTemp();
            emit(new TACInstruction(Opcode.LOAD_IMM, idx, i));
            emit(new TACInstruction(Opcode.STORE_ARRAY, val, arrTemp, idx));
        }
        return arrTemp;
    }

    @Override
    public String visit(TypeCastNode node) {
        // For TAC, we'll just evaluate the expression
        // Type checking happens at compile time or runtime
        return visit(node.expression);
    }

    @Override
    public String visit(EqualityChainNode node) {
        // Compile as series of comparisons
        String left = dispatch(node.left);
        String result = newTemp();
        String oneTemp = newTemp();
        String zeroTemp = newTemp();
        
        emit(new TACInstruction(Opcode.LOAD_IMM, oneTemp, 1));
        emit(new TACInstruction(Opcode.LOAD_IMM, zeroTemp, 0));
        
        if (node.isAllChain) {
            // Initialize result to true (1)
            emit(new TACInstruction(Opcode.ASSIGN, result, oneTemp));
            
            for (ExprNode arg : node.chainArguments) {
                String right = dispatch(arg);
                String cmpTemp = newTemp();
                emit(new TACInstruction(mapOp(node.operator), cmpTemp, left, right));
                
                // result = result AND cmpTemp
                String andTemp = newTemp();
                emit(new TACInstruction(Opcode.CMP_NE, andTemp, cmpTemp, zeroTemp));
                emit(new TACInstruction(Opcode.ASSIGN, result, andTemp));
            }
        } else {
            // ANY chain: Initialize result to false (0)
            emit(new TACInstruction(Opcode.ASSIGN, result, zeroTemp));
            
            for (ExprNode arg : node.chainArguments) {
                String right = dispatch(arg);
                String cmpTemp = newTemp();
                emit(new TACInstruction(mapOp(node.operator), cmpTemp, left, right));
                
                // result = result OR cmpTemp
                String orTemp = newTemp();
                emit(new TACInstruction(Opcode.CMP_NE, orTemp, cmpTemp, zeroTemp));
                emit(new TACInstruction(Opcode.ASSIGN, result, orTemp));
            }
        }
        
        return result;
    }

    @Override
    public String visit(BooleanChainNode node) {
        // Compile ALL/ANY expressions
        String result = newTemp();
        String oneTemp = newTemp();
        String zeroTemp = newTemp();
        
        emit(new TACInstruction(Opcode.LOAD_IMM, oneTemp, 1));
        emit(new TACInstruction(Opcode.LOAD_IMM, zeroTemp, 0));
        
        if (node.isAll) {
            // ALL: Start with true, AND with each expression
            emit(new TACInstruction(Opcode.ASSIGN, result, oneTemp));
            
            for (ExprNode expr : node.expressions) {
                String exprVal = dispatch(expr);
                // Convert to boolean (0 or 1)
                String boolTemp = newTemp();
                emit(new TACInstruction(Opcode.CMP_NE, boolTemp, exprVal, zeroTemp));
                
                // result = result AND boolTemp
                String andTemp = newTemp();
                emit(new TACInstruction(Opcode.CMP_NE, andTemp, result, zeroTemp));
                emit(new TACInstruction(Opcode.CMP_NE, andTemp, andTemp, zeroTemp));
                emit(new TACInstruction(Opcode.CMP_EQ, andTemp, andTemp, oneTemp));
                emit(new TACInstruction(Opcode.ASSIGN, result, andTemp));
            }
        } else {
            // ANY: Start with false, OR with each expression
            emit(new TACInstruction(Opcode.ASSIGN, result, zeroTemp));
            
            for (ExprNode expr : node.expressions) {
                String exprVal = dispatch(expr);
                // Convert to boolean (0 or 1)
                String boolTemp = newTemp();
                emit(new TACInstruction(Opcode.CMP_NE, boolTemp, exprVal, zeroTemp));
                
                // result = result OR boolTemp
                String orTemp = newTemp();
                emit(new TACInstruction(Opcode.CMP_NE, orTemp, result, zeroTemp));
                emit(new TACInstruction(Opcode.CMP_NE, orTemp, orTemp, zeroTemp));
                emit(new TACInstruction(Opcode.CMP_EQ, orTemp, orTemp, oneTemp));
                emit(new TACInstruction(Opcode.ASSIGN, result, orTemp));
            }
        }
        
        return result;
    }

    private Opcode mapOp(String op) {
        switch (op) {
            case "+": return Opcode.ADD;
            case "-": return Opcode.SUB;
            case "*": return Opcode.MUL;
            case "/": return Opcode.DIV;
            case "%": return Opcode.MOD;
            case "==": return Opcode.CMP_EQ;
            case "!=": return Opcode.CMP_NE;
            case "<": return Opcode.CMP_LT;
            case "<=": return Opcode.CMP_LE;
            case ">": return Opcode.CMP_GT;
            case ">=": return Opcode.CMP_GE;
            default: return Opcode.ADD;
        }
    }
}