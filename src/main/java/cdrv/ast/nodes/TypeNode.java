package cdrv.ast.nodes;

import cdrv.ast.CoderiveParser;
import java.util.List;
import java.util.ArrayList;

import static cdrv.Constants.*;

public class TypeNode extends ASTNode {
    public String name;
    public String visibility = share;
    public String extendName = null;
    public List<FieldNode> fields = new ArrayList<FieldNode>();
    public ConstructorNode constructor;
    public List<MethodNode> methods = new ArrayList<MethodNode>();
    public List<StatementNode> statements = new ArrayList<StatementNode>();
    
}