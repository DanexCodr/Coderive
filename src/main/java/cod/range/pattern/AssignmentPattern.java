package cod.range.pattern;

import cod.ast.nodes.*;
import java.util.List;

public class AssignmentPattern {
    public final ExprNode array;  // The array being assigned to
    public final ExprNode expression;  // The right-hand side expression
    public final String indexVar;  // The loop variable used as index
    
    public AssignmentPattern(ExprNode array, ExprNode expression, String indexVar) {
        this.array = array;
        this.expression = expression;
        this.indexVar = indexVar;
    }
    
    public static AssignmentPattern extract(StmtNode stmt, String iterator) {
        if (!(stmt instanceof AssignmentNode)) {
            return null;
        }
        
        AssignmentNode assign = (AssignmentNode) stmt;
        
        // Must be array[index] assignment
        if (!(assign.left instanceof IndexAccessNode)) {
            return null;
        }
        
        IndexAccessNode indexAccess = (IndexAccessNode) assign.left;
        
        // Check if index is the iterator variable
        if (!(indexAccess.index instanceof ExprNode)) {
            return null;
        }
        
        ExprNode indexExpr = (ExprNode) indexAccess.index;
        
        // Check if index matches iterator name
        if (!iterator.equals(indexExpr.name)) {
            return null;
        }
        
        return new AssignmentPattern(
            indexAccess.array,
            assign.right,
            iterator
        );
    }
    
public static StatementSequencePattern extractSequence(List<StmtNode> statements, String iterator) {
    if (statements == null || statements.size() != 2) {
        return null;
    }
    
    // First statement: temp := expression OR temp = expression
    String tempVar;
    ExprNode tempExpr;
    
    if (statements.get(0) instanceof VarNode) {
        // Declaration with := (VarNode)
        VarNode varDecl = (VarNode) statements.get(0);
        tempVar = varDecl.name;
        tempExpr = varDecl.value;
    } else if (statements.get(0) instanceof AssignmentNode) {
        // Assignment with = (AssignmentNode)
        AssignmentNode assign = (AssignmentNode) statements.get(0);
        
        // Must assign to a simple variable (not array[index])
        if (!(assign.left instanceof ExprNode)) {
            return null;
        }
        ExprNode leftExpr = (ExprNode) assign.left;
        tempVar = leftExpr.name;
        tempExpr = assign.right;
        
        // Only optimize if this is a declaration (:=) or assignment to existing variable
        if (!assign.isDeclaration) {
            // This is assignment to existing variable - check if variable exists
            // We can't check here (no context), so be conservative
            return null;
        }
    } else {
        return null; // Not a variable declaration/assignment
    }
    
    // Skip if assigning to '_' or iterator variable
    if ("_".equals(tempVar) || iterator.equals(tempVar)) {
        return null;
    }
    
    // Check the second statement (array assignment)
    AssignmentPattern arrayAssign = extract(statements.get(1), iterator);
    if (arrayAssign == null) {
        return null;
    }
    
    // Additional validation - check that tempVar is used in final expression
    if (!usesVariable(arrayAssign.expression, tempVar)) {
        return null;
    }
    
    return new StatementSequencePattern(
        tempVar, 
        tempExpr,
        arrayAssign.array,
        arrayAssign.expression,
        iterator
    );
}
    
    // Helper: Check if expression uses a specific variable
    private static boolean usesVariable(ExprNode expr, String varName) {
        if (expr == null) return false;
        
        if (expr instanceof ExprNode && varName.equals(((ExprNode) expr).name)) {
            return true;
        }
        
        if (expr instanceof BinaryOpNode) {
            BinaryOpNode binOp = (BinaryOpNode) expr;
            return usesVariable(binOp.left, varName) || usesVariable(binOp.right, varName);
        }
        
        if (expr instanceof UnaryNode) {
            UnaryNode unary = (UnaryNode) expr;
            return usesVariable(unary.operand, varName);
        }
        
        if (expr instanceof MethodCallNode) {
            MethodCallNode call = (MethodCallNode) expr;
            if (call.arguments != null) {
                for (ExprNode arg : call.arguments) {
                    if (usesVariable(arg, varName)) return true;
                }
            }
            return false;
        }
        
        // Handle other node types that might contain expressions
        if (expr instanceof IndexAccessNode) {
            IndexAccessNode access = (IndexAccessNode) expr;
            return usesVariable(access.array, varName) || usesVariable(access.index, varName);
        }
        
        if (expr instanceof TypeCastNode) {
            TypeCastNode cast = (TypeCastNode) expr;
            return usesVariable(cast.expression, varName);
        }
        
        if (expr instanceof ArrayNode) {
            ArrayNode array = (ArrayNode) expr;
            if (array.elements != null) {
                for (ExprNode elem : array.elements) {
                    if (usesVariable(elem, varName)) return true;
                }
            }
            return false;
        }
        
        if (expr instanceof TupleNode) {
            TupleNode tuple = (TupleNode) expr;
            if (tuple.elements != null) {
                for (ExprNode elem : tuple.elements) {
                    if (usesVariable(elem, varName)) return true;
                }
            }
            return false;
        }
        
        return false;
    }
    
    // NEW: Pattern for 2-statement sequences
    public static class StatementSequencePattern {
        public final String tempVar;          // e.g., "squared"
        public final ExprNode tempExpr;       // e.g., i * i
        public final ExprNode targetArray;    // e.g., arr
        public final ExprNode finalExpr;      // e.g., squared + 10
        public final String indexVar;         // e.g., "i"
        
        public StatementSequencePattern(String tempVar, ExprNode tempExpr,
                                       ExprNode targetArray, ExprNode finalExpr,
                                       String indexVar) {
            this.tempVar = tempVar;
            this.tempExpr = tempExpr;
            this.targetArray = targetArray;
            this.finalExpr = finalExpr;
            this.indexVar = indexVar;
        }
        
        public boolean isOptimizable() {
            return tempVar != null && tempExpr != null && 
                   targetArray != null && finalExpr != null && 
                   indexVar != null;
        }
    }
}
