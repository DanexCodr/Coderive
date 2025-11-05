package cod.ast.nodes;

public class VarNode extends StatementNode {
    public String name;
    public ExprNode value;
    public String explicitType; // <<< ADDED THIS FIELD

}