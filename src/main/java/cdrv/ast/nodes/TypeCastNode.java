package cdrv.ast.nodes;

import cdrv.ast.ASTBuilder;
import cdrv.ast.CoderiveParser;
import org.antlr.v4.runtime.tree.ParseTree;

public class TypeCastNode extends ExprNode {
    public String targetType;    // The target type to cast to
    public ExprNode expression;  // The expression being cast
    
    @Override
    public String toString() {
        return "TypeCastNode{type=" + targetType + ", expr=" + expression + "}";
    }
}