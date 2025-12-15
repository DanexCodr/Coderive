package cod.ast.nodes;

import cod.ast.ASTVisitor;

import java.util.*;

public class OutputNode extends StmtNode {
    public String varName;               // optional, for "output n = ..."
    public List<ExprNode> arguments = new ArrayList<>(); // normal output or multiple args
    
               @Override
        public final <T> T accept(ASTVisitor<T> visitor) {
           return visitor.visit(this);
        }
    
}