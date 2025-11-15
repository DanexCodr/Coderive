package cod.ast.nodes;

import java.util.List;

public class MultipleSlotAssignmentNode extends StatementNode {
    public List<SlotAssignmentNode> assignments;
}