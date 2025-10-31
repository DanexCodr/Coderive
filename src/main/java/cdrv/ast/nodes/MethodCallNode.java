package cdrv.ast.nodes;

import java.util.*;

public class MethodCallNode extends ExprNode {
    public List<ExprNode> arguments = new ArrayList<>();
    public List<String> slotNames = new ArrayList<>();
    public String qualifiedName;
    
    // Remove all ANTLR imports and references
}