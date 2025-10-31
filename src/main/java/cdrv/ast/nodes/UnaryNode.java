package cdrv.ast.nodes;

import org.antlr.v4.runtime.tree.ParseTree;

public class UnaryNode extends ExprNode {
    public String op;  // "+" or "-"
    public ExprNode operand;
    
    public UnaryNode() {}
    
    public UnaryNode(String op, ExprNode operand) {
        this.op = op;
        this.operand = operand;
    }
}