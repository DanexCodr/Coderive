package cod.ast.nodes;

import java.util.List;
import java.util.ArrayList;

import cod.ast.ASTVisitor;
import cod.syntax.Keyword;

public class TypeNode extends ASTNode {
    public String name;
    public Keyword visibility = Keyword.SHARE;
    public String extendName = null;
    public List<FieldNode> fields = new ArrayList<FieldNode>();
    public ConstructorNode constructor;
    public List<MethodNode> methods = new ArrayList<MethodNode>();
    public List<StmtNode> statements = new ArrayList<StmtNode>();
    public List<ConstructorNode> constructors = new ArrayList<ConstructorNode>();
    
               @Override
        public final <T> T accept(ASTVisitor<T> visitor) {
           return visitor.visit(this);
        }
    
  
}