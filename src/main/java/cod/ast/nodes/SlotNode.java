package cod.ast.nodes;

public class SlotNode extends ASTNode {
    public String name;
    public String type;
    
    // Helper to check if this slot was declared with a name or auto-generated (0, 1, etc)
    public boolean isNamed() {
        if (name == null || name.isEmpty()) return false;
        // If name starts with digit, it's auto-generated (since IDs can't start with digit)
        return !Character.isDigit(name.charAt(0));
    }
}