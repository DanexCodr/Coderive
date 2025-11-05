package cod.ast.nodes;

import java.util.List;
import java.util.ArrayList;

public class ConstructorNode extends ASTNode {
    public List<ParamNode> parameters = new ArrayList<ParamNode>();
    public List<StatementNode> body = new ArrayList<StatementNode>();
}
