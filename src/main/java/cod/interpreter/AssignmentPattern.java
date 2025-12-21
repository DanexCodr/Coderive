package cod.interpreter;

import cod.ast.nodes.*;

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
}