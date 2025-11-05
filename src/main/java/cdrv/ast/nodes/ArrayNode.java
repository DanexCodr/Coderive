package cod.ast.nodes;

import cod.ast.CoderiveParser;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.tree.ParseTree;

public class ArrayNode extends ExprNode {
    public List<ExprNode> elements = new ArrayList<>();
    public String elementType; // int[], string[], etc.
}
