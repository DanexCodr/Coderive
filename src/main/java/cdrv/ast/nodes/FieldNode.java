package cdrv.ast.nodes;

public class FieldNode extends StatementNode {
    public String name;
    public String type;
    public String visibility; // [FIX] Added visibility property
    public ExprNode value;
    
    // The 'left' property for assignments has been removed, 
    // as the new AssignmentNode is a much cleaner way to handle that.
}
