package cod.ast.nodes;

import java.util.List;
import java.util.ArrayList;

import cod.ast.VisitorImpl;
import cod.syntax.Keyword;

public class MethodNode extends ASTNode {
    public String methodName;
    public String associatedClass;
    public Keyword visibility = Keyword.SHARE;
    public List<SlotNode> returnSlots = new ArrayList<SlotNode>();
    public List<ParamNode> parameters = new ArrayList<ParamNode>();
    public List<StmtNode> body = new ArrayList<StmtNode>();
    public boolean isBuiltin = false;
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}