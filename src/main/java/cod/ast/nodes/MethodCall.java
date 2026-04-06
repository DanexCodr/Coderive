package cod.ast.nodes;

import cod.ast.VisitorImpl;
import java.util.List;
import java.util.ArrayList;

public class MethodCall extends Expr {
    public String name;
    public String qualifiedName;
    public List<Expr> arguments;
    public List<String> slotNames;
    public List<String> argNames;
    public boolean isSuperCall;
    public boolean isGlobal;
    public Expr target;
    
    // NEW: Flag to indicate this method call should return a single slot value directly
    public boolean isSingleSlotCall;

    public MethodCall() {
        this.arguments = new ArrayList<Expr>();
        this.slotNames = new ArrayList<String>();
        this.argNames = new ArrayList<String>();
        this.isSuperCall = false;
        this.isGlobal = false;
        this.isSingleSlotCall = false;
    }
    
    @Override
    public final <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}