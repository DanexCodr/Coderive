package cdrv.ast.nodes;

import cdrv.ast.CoderiveParser;
import java.util.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

public class OutputNode extends StatementNode {
    public String varName;               // optional, for "output n = ..."
    public List<ExprNode> arguments = new ArrayList<>(); // normal output or multiple args
    
}