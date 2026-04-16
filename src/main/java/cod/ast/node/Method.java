package cod.ast.node;

import java.util.List;
import java.util.ArrayList;

import cod.ast.VisitorImpl;
import cod.lexer.TokenType.Keyword;

public class Method extends Base {
    public String methodName;
    public String associatedClass;
    public Keyword visibility = Keyword.SHARE;
    public List<Slot> returnSlots = new ArrayList<Slot>();
    public List<Param> parameters = new ArrayList<Param>();
    public List<Stmt> body = new ArrayList<Stmt>();
    public boolean isBuiltin = false;
    public boolean isPolicyMethod = false;
    public boolean isUnsafe = false;
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}
