package cod.ast.node;

import cod.ast.VisitorImpl;
import java.util.List;

public class Param extends Base {
  public String name;
  public String type;
  public Expr defaultValue;
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