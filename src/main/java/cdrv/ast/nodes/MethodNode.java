package cdrv.ast.nodes;

import java.util.List;
import java.util.ArrayList;

public class MethodNode extends ASTNode {
    public String name;
    public String visibility = "public";
    public List<SlotNode> returnSlots = new ArrayList<SlotNode>();
    public List<ParamNode> parameters = new ArrayList<ParamNode>();
    public List<StatementNode> body = new ArrayList<StatementNode>();
}