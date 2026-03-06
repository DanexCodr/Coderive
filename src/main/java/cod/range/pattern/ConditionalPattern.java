package cod.range.pattern;

import cod.ast.nodes.*;
import java.util.*;

public class ConditionalPattern {
    public final ExprNode array;
    public final String indexVar;
    public final List<Branch> branches;
    public final List<StmtNode> elseStatements;
    
    public static class Branch {
        public final ExprNode condition;
        public final List<StmtNode> statements;
        
        public Branch(ExprNode condition, List<StmtNode> statements) {
            this.condition = condition;
            this.statements = statements;
        }
    }
    
    public ConditionalPattern(ExprNode array, String indexVar,
                             List<Branch> branches,
                             List<StmtNode> elseStatements) {
        this.array = array;
        this.indexVar = indexVar;
        this.branches = branches != null ? branches : new ArrayList<Branch>();
        this.elseStatements = elseStatements != null ? elseStatements : new ArrayList<StmtNode>();
    }
    
    public boolean isOptimizable() {
        return array != null && indexVar != null && 
               branches != null && !branches.isEmpty();
    }
    
    public static ConditionalPattern extract(StmtIfNode ifStmt, String iterator) {
        return extractRecursive(ifStmt, iterator, null, new ArrayList<Branch>());
    }
    
    private static ConditionalPattern extractRecursive(
            StmtIfNode ifStmt, String iterator, 
            ExprNode targetArray, List<Branch> accumulatedBranches) {
        
        // Extract then branch statements
        List<StmtNode> thenStatements = ifStmt.thenBlock.statements;
        
        // Validate all statements in then branch
        ExprNode branchArray = validateBranchStatements(thenStatements, iterator, targetArray);
        if (branchArray == null) return null;
        
        // Create branch
        Branch currentBranch = new Branch(ifStmt.condition, thenStatements);
        accumulatedBranches.add(currentBranch);
        
        // Handle else/elif
        if (ifStmt.elseBlock != null && !ifStmt.elseBlock.statements.isEmpty()) {
            StmtNode firstElseStmt = ifStmt.elseBlock.statements.get(0);
            
            if (firstElseStmt instanceof StmtIfNode) {
                // Recursively handle elif
                return extractRecursive(
                    (StmtIfNode) firstElseStmt, iterator, 
                    branchArray, accumulatedBranches
                );
            } else {
                // Final else branch
                List<StmtNode> elseStatements = ifStmt.elseBlock.statements;
                
                // Validate else statements all target same array
                for (StmtNode stmt : elseStatements) {
                    ExprNode stmtArray = validateStatementTarget(stmt, iterator);
                    if (stmtArray != null && !isSameArray(stmtArray, branchArray)) {
                        return null;
                    }
                }
                
                return new ConditionalPattern(
                    branchArray, iterator, 
                    accumulatedBranches, elseStatements
                );
            }
        }
        
        // No else clause
        return new ConditionalPattern(
            branchArray, iterator, 
            accumulatedBranches, new ArrayList<StmtNode>()
        );
    }
    
    private static ExprNode validateBranchStatements(
            List<StmtNode> statements, String iterator, ExprNode expectedArray) {
        
        ExprNode branchArray = null;
        
        for (StmtNode stmt : statements) {
            ExprNode stmtArray = validateStatementTarget(stmt, iterator);
            
            // Variable declarations don't have array targets - skip them
            if (stmtArray == null) continue;
            
            if (branchArray == null) {
                branchArray = stmtArray;
            } else if (!isSameArray(stmtArray, branchArray)) {
                return null; // Mixed array targets in same branch
            }
            
            if (expectedArray != null && !isSameArray(stmtArray, expectedArray)) {
                return null; // Different array than previous branches
            }
        }
        
        return branchArray;
    }
    
    private static ExprNode validateStatementTarget(StmtNode stmt, String iterator) {
        if (stmt instanceof AssignmentNode) {
            AssignmentNode assign = (AssignmentNode) stmt;
            if (assign.left instanceof IndexAccessNode) {
                IndexAccessNode indexAccess = (IndexAccessNode) assign.left;
                if (isIndexIterator(indexAccess.index, iterator)) {
                    return indexAccess.array;
                }
            }
        }
        // VarNode (variable declarations) are allowed but don't target array
        return null;
    }
    
    private static boolean isIndexIterator(ExprNode index, String iterator) {
        return index instanceof IdentifierNode && 
               iterator.equals(((IdentifierNode) index).name);
    }
    
    private static boolean isSameArray(ExprNode a, ExprNode b) {
        if (a == null || b == null) return false;
        if (a instanceof IdentifierNode && b instanceof IdentifierNode) {
            return ((IdentifierNode) a).name.equals(((IdentifierNode) b).name);
        }
        return a.toString().equals(b.toString());
    }
}