package cod.ast.nodes;

import cod.ast.CoderiveParser;
import org.antlr.v4.runtime.tree.ParseTree;

public class InputNode extends StatementNode {
    public String targetType;    // The type being read (int, string, float, bool)
    public String variableName;  // The variable to assign the input to
    
    @Override
    public String toString() {
        return "InputNode{type=" + targetType + ", var=" + variableName + "}";
    }
}