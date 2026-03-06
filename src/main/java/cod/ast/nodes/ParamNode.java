package cod.ast.nodes;

import cod.ast.VisitorImpl;
import java.util.List;

public class ParamNode extends ASTNode {
  public String name;
  public String type;
  public ExprNode defaultValue;
  public boolean hasDefaultValue = false;
  public boolean typeInferred = false;
  public boolean isLambdaParameter = false;
  
  // For tuple destructuring like \((x, y))
  public boolean isTupleDestructuring = false;
  public List<String> tupleElements;

  @Override
  public final <T> T accept(VisitorImpl<T> visitor) {
    return visitor.visit(this);
  }
}