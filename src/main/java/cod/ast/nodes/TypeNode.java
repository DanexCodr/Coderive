package cod.ast.nodes;

import java.util.List;
import java.util.ArrayList;

import cod.ast.VisitorImpl;
import cod.lexer.Token;
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
  public List<String> implementedPolicies = new ArrayList<String>();
  public Token extendToken;
  public Token parentToken;
  public java.util.Map<String, Token> policyTokens;
  // Cache for viral policy checking (optional optimization)
  public List<String> cachedAncestorPolicies = null;
  public boolean viralPoliciesValidated = false;

  @Override
  public final <T> T accept(VisitorImpl<T> visitor) {
    return visitor.visit(this);
  }
}
