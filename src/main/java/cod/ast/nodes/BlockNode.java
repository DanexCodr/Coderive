package cod.ast.nodes;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.tree.ParseTree;

public class BlockNode extends StatementNode {
    public List<StatementNode> statements = new ArrayList<>();

    public BlockNode() {}

    public BlockNode(List<StatementNode> statements) {
        this.statements = statements;
    }
}
