package cdrv.ast.nodes;

import cdrv.ast.CoderiveParser;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.tree.ParseTree;

public class GetNode extends ASTNode {
    public List<String> imports = new ArrayList<>();
}