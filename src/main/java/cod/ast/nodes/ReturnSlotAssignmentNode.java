package cod.ast.nodes;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.tree.ParseTree;

public class ReturnSlotAssignmentNode extends StatementNode {
    public List<String> variableNames = new ArrayList<>();
    public MethodCallNode methodCall;
}
