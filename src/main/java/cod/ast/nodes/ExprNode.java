package cod.ast.nodes;

import cod.ast.VisitorImpl;

public class ExprNode extends StmtNode {
    public String name; // For identifiers or method names
    public Object value; // For literals
    public String op; // Operator (+, -, *, /, slot_cast, etc.)
    public ExprNode left; // Left operand
    public ExprNode right; // Right operand
    public boolean isNone = false;
    
    public boolean isThis = false;          // true if this expression is "this"
    public boolean isSuper = false;        // ture if this expression is "super"
    public String thisClassName = null;     // optional class name for "ClassName.this"
    
    // Fields for property access
    public boolean isPropertyAccess = false; // true if this is a property access like "this.field"
    public String propertyName = null;       // the property name being accessed
    

    public ExprNode() {}
    
            @Override
        public <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}