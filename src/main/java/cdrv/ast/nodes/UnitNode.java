package cdrv.ast.nodes;

import cdrv.ast.CoderiveParser;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.antlr.v4.runtime.tree.ParseTree;

public class UnitNode extends ASTNode {
    public String name;
    public GetNode imports; // CHANGED: from List<String> to GetNode
    public List<TypeNode> types = new ArrayList<TypeNode>();
    
    // Add this field for resolved imports
    public Map<String, ProgramNode> resolvedImports = new HashMap<String, ProgramNode>();
    
}