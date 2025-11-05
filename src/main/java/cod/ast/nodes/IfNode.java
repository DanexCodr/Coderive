package cod.ast.nodes;

import java.util.*;

public class IfNode extends StatementNode {
    public ExprNode condition;
    public BlockNode thenBlock = new BlockNode();
    public BlockNode elseBlock = new BlockNode();

    public IfNode() {}
    
    public IfNode(ExprNode condition) {
        this.condition = condition;
    }
    
    // Remove ANTLR imports and references
}