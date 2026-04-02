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
        List<ConditionalPattern> patterns = extractAll(ifStmt, iterator);
        if (patterns.isEmpty()) {
            return null;
        }
        return patterns.get(0);
    }
    
    /**
     * Extract one conditional pattern per target array from an if/elif/else chain.
     */
    public static List<ConditionalPattern> extractAll(StmtIfNode ifStmt, String iterator) {
        if (ifStmt == null || iterator == null) {
            return new ArrayList<ConditionalPattern>();
        }
        
        ChainExtraction chain = flattenChain(ifStmt);
        if (chain == null || chain.branches.isEmpty()) {
            return new ArrayList<ConditionalPattern>();
        }
        
        List<ExprNode> targetArrays = collectTargetArrays(chain, iterator);
        List<ConditionalPattern> results = new ArrayList<ConditionalPattern>();
        
        for (ExprNode targetArray : targetArrays) {
            List<Branch> filteredBranches = new ArrayList<Branch>();
            boolean hasAnyAssignment = false;
            
            for (Branch branch : chain.branches) {
                List<StmtNode> filtered = filterStatementsForArray(branch.statements, iterator, targetArray);
                if (!filtered.isEmpty()) {
                    filteredBranches.add(new Branch(branch.condition, filtered));
                    if (containsArrayAssignment(filtered, iterator, targetArray)) {
                        hasAnyAssignment = true;
                    }
                }
            }
            
            List<StmtNode> filteredElse = filterStatementsForArray(chain.elseStatements, iterator, targetArray);
            if (containsArrayAssignment(filteredElse, iterator, targetArray)) {
                hasAnyAssignment = true;
            }
            
            if (hasAnyAssignment && !filteredBranches.isEmpty()) {
                results.add(new ConditionalPattern(targetArray, iterator, filteredBranches, filteredElse));
            }
        }
        
        return results;
    }
    
    private static class ChainExtraction {
        final List<Branch> branches;
        final List<StmtNode> elseStatements;
        
        ChainExtraction(List<Branch> branches, List<StmtNode> elseStatements) {
            this.branches = branches;
            this.elseStatements = elseStatements;
        }
    }
    
    private static ChainExtraction flattenChain(StmtIfNode ifStmt) {
        List<Branch> branches = new ArrayList<Branch>();
        List<StmtNode> elseStatements = new ArrayList<StmtNode>();
        StmtIfNode current = ifStmt;
        
        while (current != null) {
            List<StmtNode> thenStatements = current.thenBlock != null && current.thenBlock.statements != null
                ? current.thenBlock.statements
                : new ArrayList<StmtNode>();
            branches.add(new Branch(current.condition, thenStatements));
            
            if (current.elseBlock == null || current.elseBlock.statements == null ||
                current.elseBlock.statements.isEmpty()) {
                break;
            }
            
            StmtNode firstElse = current.elseBlock.statements.get(0);
            if (firstElse instanceof StmtIfNode) {
                current = (StmtIfNode) firstElse;
            } else {
                elseStatements = current.elseBlock.statements;
                break;
            }
        }
        
        return new ChainExtraction(branches, elseStatements);
    }
    
    private static List<ExprNode> collectTargetArrays(ChainExtraction chain, String iterator) {
        List<ExprNode> targets = new ArrayList<ExprNode>();
        
        for (Branch branch : chain.branches) {
            addTargetsFromStatements(targets, branch.statements, iterator);
        }
        addTargetsFromStatements(targets, chain.elseStatements, iterator);
        
        return targets;
    }
    
    private static void addTargetsFromStatements(List<ExprNode> targets, List<StmtNode> statements, String iterator) {
        if (statements == null) {
            return;
        }
        
        for (StmtNode stmt : statements) {
            ExprNode stmtArray = validateStatementTarget(stmt, iterator);
            if (stmtArray == null) {
                continue;
            }
            
            boolean exists = false;
            for (ExprNode existing : targets) {
                if (isSameArray(existing, stmtArray)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                targets.add(stmtArray);
            }
        }
    }
    
    private static List<StmtNode> filterStatementsForArray(List<StmtNode> statements, String iterator, ExprNode targetArray) {
        List<StmtNode> filtered = new ArrayList<StmtNode>();
        if (statements == null) {
            return filtered;
        }
        
        for (StmtNode stmt : statements) {
            ExprNode stmtArray = validateStatementTarget(stmt, iterator);
            if (stmtArray == null) {
                if (isVariableDeclaration(stmt)) {
                    filtered.add(stmt);
                }
                continue;
            }
            
            if (isSameArray(stmtArray, targetArray)) {
                filtered.add(stmt);
            }
        }
        
        return filtered;
    }
    
    private static boolean isVariableDeclaration(StmtNode stmt) {
        if (stmt instanceof VarNode) {
            return true;
        }
        
        if (stmt instanceof AssignmentNode) {
            return ((AssignmentNode) stmt).isDeclaration;
        }
        
        return false;
    }
    
    private static boolean containsArrayAssignment(List<StmtNode> statements, String iterator, ExprNode targetArray) {
        if (statements == null) {
            return false;
        }
        
        for (StmtNode stmt : statements) {
            ExprNode stmtArray = validateStatementTarget(stmt, iterator);
            if (stmtArray != null && isSameArray(stmtArray, targetArray)) {
                return true;
            }
        }
        
        return false;
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
