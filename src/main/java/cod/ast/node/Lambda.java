package cod.ast.node;

import cod.ast.VisitorImpl;
import java.util.List;
import java.util.ArrayList;

public class Lambda extends Expr {
    public List<Param> parameters;
    public List<Slot> returnSlots;
    public Stmt body;
    public Expr expressionBody;
    public boolean inferParameters;
    
    public Lambda() {
        this.parameters = new ArrayList<Param>();
        this.returnSlots = new ArrayList<Slot>();
        this.inferParameters = false;
    }
    
    public Lambda(List<Param> parameters, List<Slot> returnSlots, Stmt body) {
        this.parameters = parameters != null ? parameters : new ArrayList<Param>();
        this.returnSlots = returnSlots != null ? returnSlots : new ArrayList<Slot>();
        this.body = body;
        this.expressionBody = null;
        this.inferParameters = false;
    }
    
    @Override
    public <T> T accept(VisitorImpl<T> visitor) {
        return visitor.visit(this);
    }
}
