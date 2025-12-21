package cod.interpreter;

import cod.ast.ASTFactory;
import cod.ast.BaseASTVisitor;
import cod.ast.nodes.*;
import cod.semantic.ImportResolver;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import cod.syntax.Keyword;
import static cod.syntax.Keyword.*;
import cod.semantic.ConstructorResolver;

public class InterpreterVisitor extends BaseASTVisitor<Object> {

  private final Interpreter interpreter;
  public final TypeSystem typeSystem;
  private final IOHandler ioHandler;
  private final Stack<ExecutionContext> contextStack = new Stack<ExecutionContext>();

  private static final int DECIMAL_SCALE = 20;

  public InterpreterVisitor(Interpreter interpreter, TypeSystem typeSystem, IOHandler ioHandler) {
    this.interpreter = interpreter;
    this.typeSystem = typeSystem;
    this.ioHandler = ioHandler;
  }

  public void pushContext(ExecutionContext context) {
    contextStack.push(context);
  }

  public void popContext() {
    contextStack.pop();
  }

  public ExecutionContext getCurrentContext() {
    return contextStack.peek();
  }

  @Override
  public Object visit(ExprNode node) {

    ExecutionContext ctx = getCurrentContext();

    if (node.name != null) {

      if (ctx.objectInstance != null && ctx.objectInstance.type != null) {
        Object fieldValue =
            interpreter
                .getConstructorResolver()
                .getFieldFromHierarchy(ctx.objectInstance.type, node.name, ctx);
        if (fieldValue != null) {
          return fieldValue;
        }
      }

      if (ctx.locals.containsKey(node.name)) {
        Object val = ctx.locals.get(node.name);
        return val;
      }

      if (ctx.slotValues != null && ctx.slotValues.containsKey(node.name)) {
        Object val = ctx.slotValues.get(node.name);
        return val;
      }
      throw new RuntimeException("Undefined Variable: " + node.name);
    }

    if (node.value != null) {

      if (node.value instanceof String) {
        String s = (String) node.value;
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
          String result = s.substring(1, s.length() - 1);
          return result;
        }
        return s;
      }

      if (node.value instanceof BigDecimal) {
        BigDecimal result = ((BigDecimal) node.value).stripTrailingZeros();
        return result;
      }
      return node.value;
    }
    return null;
  }

  @Override
  public Object visit(ConstructorCallNode node) {
    return interpreter.getConstructorResolver().resolveAndCreate(node, getCurrentContext());
  }

  @Override
  public Object visit(ConstructorNode node) {
    return null;
  }

  @Override
  public Object visit(BinaryOpNode node) {
    Object left = dispatch(node.left);
    Object right = dispatch(node.right);
    Object result = null;

    switch (node.op) {
      case "+":
      case "+=":
        if (left instanceof String || right instanceof String) {
          Object unwrappedLeft = typeSystem.unwrap(left);
          Object unwrappedRight = typeSystem.unwrap(right);

          if (unwrappedLeft instanceof BigDecimal) {
            BigDecimal bdLeft = ((BigDecimal) unwrappedLeft).stripTrailingZeros();
            unwrappedLeft = bdLeft.toPlainString();
          }
          if (unwrappedRight instanceof BigDecimal) {
            BigDecimal bdRight = ((BigDecimal) unwrappedRight).stripTrailingZeros();
            unwrappedRight = bdRight.toPlainString();
          }

          result = String.valueOf(unwrappedLeft) + String.valueOf(unwrappedRight);
        } else {
          result = typeSystem.addNumbers(left, right);
        }
        break;

      case "*":
      case "*=":
        result = typeSystem.multiplyNumbers(left, right);
        break;

      case "-":
      case "-=":
        result = typeSystem.subtractNumbers(left, right);
        break;

      case "/":
      case "/=":
        result = typeSystem.divideNumbers(left, right);
        break;

      case "%":
        result = typeSystem.modulusNumbers(left, right);
        break;

      case ">":
        result = typeSystem.compare(left, right) > 0;
        break;

      case "<":
        result = typeSystem.compare(left, right) < 0;
        break;

      case ">=":
        result = typeSystem.compare(left, right) >= 0;
        break;

      case "<=":
        result = typeSystem.compare(left, right) <= 0;
        break;

      case "=":
        result = right;
        break;

      case "==":
        result = typeSystem.areEqual(left, right);
        break;

      case "!=":
        result = !typeSystem.areEqual(left, right);
        break;

      default:
        throw new RuntimeException("Unknown operator: " + node.op);
    }
    return result;
  }

  @Override
  public Object visit(BooleanChainNode node) {
    boolean isAll = node.isAll;

    for (ExprNode expr : node.expressions) {
      Object result = dispatch(expr);
      result = typeSystem.unwrap(result);
      boolean isTruthy = isTruthy(result);

      if (isAll) {
        if (!isTruthy) return false;
      } else {
        if (isTruthy) return true;
      }
    }

    return isAll;
  }

  @Override
  public Object visit(EqualityChainNode node) {
    Object leftValue = dispatch(node.left);
    leftValue = typeSystem.unwrap(leftValue);

    for (ExprNode arg : node.chainArguments) {
      Object rightValue = dispatch(arg);
      rightValue = typeSystem.unwrap(rightValue);

      boolean comparisonResult;
      switch (node.operator) {
        case "==":
          comparisonResult = typeSystem.unwrap(leftValue).equals(typeSystem.unwrap(rightValue));
          break;
        case "!=":
          comparisonResult = !typeSystem.unwrap(leftValue).equals(typeSystem.unwrap(rightValue));
          break;
        case ">":
          comparisonResult = typeSystem.compare(leftValue, rightValue) > 0;
          break;
        case "<":
          comparisonResult = typeSystem.compare(leftValue, rightValue) < 0;
          break;
        case ">=":
          comparisonResult = typeSystem.compare(leftValue, rightValue) >= 0;
          break;
        case "<=":
          comparisonResult = typeSystem.compare(leftValue, rightValue) <= 0;
          break;
        default:
          throw new RuntimeException(
              "Unknown comparison operator in equality chain: " + node.operator);
      }

      if (node.isAllChain) {
        if (!comparisonResult) {
          return false;
        }
      } else {
        if (comparisonResult) {
          return true;
        }
      }
    }

    return node.isAllChain;
  }

  @Override
  public Object visit(UnaryNode node) {
    Object operand = dispatch(node.operand);

    switch (node.op) {
      case "-":
        return typeSystem.negateNumber(operand);
      case "+":
        return operand;
      case "!":
        return !isTruthy(operand);
      default:
        throw new RuntimeException("Unknown unary operator: " + node.op);
    }
  }

  @Override
  public Object visit(TypeCastNode node) {
    Object val = dispatch(node.expression);
    if (!typeSystem.validateType(node.targetType, val)) {
      return typeSystem.convertType(val, node.targetType);
    }
    return val;
  }

  @Override
  public Object visit(ArrayNode node) {
    if (node.elements.size() == 1) {
      ExprNode onlyElement = node.elements.get(0);
      if (onlyElement instanceof RangeNode) {
        RangeNode range = (RangeNode) onlyElement;
        return new NaturalArray(range, this);
      }
    }

    List<Object> result = new ArrayList<Object>();
    for (ExprNode element : node.elements) {
      if (element instanceof RangeNode) {
        result.add(new NaturalArray((RangeNode) element, this));
      } else {
        Object evaluated = dispatch(element);

        if (element instanceof ArrayNode && evaluated instanceof NaturalArray) {
          result.add(evaluated);
        } else {
          result.add(evaluated);
        }
      }
    }
    return result;
  }

  @Override
  public Object visit(TupleNode node) {
    List<Object> tuple = new ArrayList<Object>();
    for (ExprNode elem : node.elements) tuple.add(dispatch(elem));
    return Collections.unmodifiableList(tuple);
  }

  @Override
  public Object visit(IndexAccessNode node) {
    Object arrayObj = dispatch(node.array);
    Object indexObj = dispatch(node.index);

    arrayObj = typeSystem.unwrap(arrayObj);
    indexObj = typeSystem.unwrap(indexObj);

    if (arrayObj instanceof NaturalArray) {
      NaturalArray natural = (NaturalArray) arrayObj;
      long index = toLongIndex(indexObj);
      if (index < 0 || index >= natural.size()) {
        throw new RuntimeException(
            "Index out of bounds: " + index + " for NaturalArray of size " + natural.size());
      }
      return natural.get(index);
    }

    if (arrayObj instanceof List) {
      List<Object> list = (List<Object>) arrayObj;
      if (indexObj instanceof BigDecimal) {
        int index = ((BigDecimal) indexObj).intValue();
        if (index < 0 || index >= list.size()) {
          throw new RuntimeException(
              "Index out of bounds: " + index + " for array of size " + list.size());
        }
        return list.get(index);
      } else {
        int index = ((Number) indexObj).intValue();
        if (index < 0 || index >= list.size()) {
          throw new RuntimeException(
              "Index out of bounds: " + index + " for array of size " + list.size());
        }
        return list.get(index);
      }
    }

    throw new RuntimeException(
        "Invalid array access: expected NaturalArray or List, got "
            + (arrayObj != null ? arrayObj.getClass().getSimpleName() : "null"));
  }

  private long toLong(Object obj) {
    if (obj instanceof Integer) return ((Integer) obj).longValue();
    if (obj instanceof Long) return (Long) obj;
    if (obj instanceof BigDecimal) return ((BigDecimal) obj).longValue();
    throw new RuntimeException("Cannot convert to long: " + obj);
  }

  private long toLongIndex(Object indexObj) {
    if (indexObj == null) {
      throw new RuntimeException("Array index cannot be null");
    }

    if (indexObj instanceof Integer) {
      return ((Integer) indexObj).longValue();
    }

    if (indexObj instanceof Long) {
      return (Long) indexObj;
    }

    if (indexObj instanceof Double || indexObj instanceof Float) {
      double d = ((Number) indexObj).doubleValue();
      if (d != Math.floor(d)) {
        throw new RuntimeException("Array index must be integer, got: Double (" + d + ")");
      }
      return (long) d;
    }

    if (indexObj instanceof BigDecimal) {
      BigDecimal bd = (BigDecimal) indexObj;
      try {
        return bd.longValueExact();
      } catch (ArithmeticException e) {
        throw new RuntimeException(
            "Array index must be an exact integer, got: BigDecimal (" + bd + ")");
      }
    }

    throw new RuntimeException(
        "Array index must be integer, got: " + indexObj.getClass().getSimpleName());
  }

  private int toIntIndex(Object indexObj) {
    if (indexObj == null) {
      throw new RuntimeException("Array index cannot be null");
    }
    if (indexObj instanceof Integer) return (Integer) indexObj;
    if (indexObj instanceof Long) return ((Long) indexObj).intValue();

    if (indexObj instanceof BigDecimal) {
      return ((BigDecimal) indexObj).intValue();
    }

    throw new RuntimeException("Array index must be integer");
  }

  @Override
  public Object visit(AssignmentNode node) {
    ExecutionContext ctx = getCurrentContext();
    Object newValue = dispatch(node.right);

    if (node.left instanceof IndexAccessNode) {
      IndexAccessNode indexAccess = (IndexAccessNode) node.left;
      Object arrayObj = dispatch(indexAccess.array);
      arrayObj = typeSystem.unwrap(arrayObj);
      Object indexObj = dispatch(indexAccess.index);
      indexObj = typeSystem.unwrap(indexObj);

      if (arrayObj instanceof NaturalArray) {
        NaturalArray natural = (NaturalArray) arrayObj;
        long index = toLongIndex(indexObj);
        natural.set(index, newValue);
        return newValue;
      }

      if (arrayObj instanceof List) {
        int intIndex = toIntIndex(indexObj);
        List<Object> list = (List<Object>) arrayObj;
        list.set(intIndex, newValue);
        return newValue;
      }
      throw new RuntimeException("Invalid assignment target");
    } else if (node.left.name != null) {
      String varName = node.left.name;
      if ("_".equals(varName)) {
        throw new RuntimeException("Cannot assign to '_'");
      }

      if (ctx.locals.containsKey(varName)) {
        if (ctx.localTypes.containsKey(varName)) {
          String type = ctx.localTypes.get(varName);
          if (!typeSystem.validateType(type, newValue)) {
            throw new RuntimeException("Type mismatch in assignment for " + varName);
          }
          if (type.contains("|")) {
            String activeType = typeSystem.getConcreteType(typeSystem.unwrap(newValue));
            newValue = new TypedValue(newValue, activeType, type);
          }
        }
        ctx.locals.put(varName, newValue);
        return newValue;
      }

      if (ctx.objectInstance != null && ctx.objectInstance.type != null) {
        Object fieldValue =
            interpreter
                .getConstructorResolver()
                .getFieldFromHierarchy(ctx.objectInstance.type, varName, ctx);
        if (fieldValue != null) {
          ctx.objectInstance.fields.put(varName, newValue);
          return newValue;
        }
      }

      throw new RuntimeException("Cannot assign to undefined variable: " + varName);
    }
    throw new RuntimeException("Invalid assignment target");
  }

  @Override
  public Object visit(SlotDeclarationNode node) {
    return null;
  }

  @Override
  public Object visit(MethodCallNode node) {
    if (interpreter.getGlobalRegistry().isGlobal(node.name)) {
      return interpreter.getGlobalRegistry().executeGlobal(node.name, node.arguments, this);
    }

    if (node.chainType != null && node.chainArguments != null) {
      return evaluateConditionalChain(node);
    }

    ExecutionContext ctx = getCurrentContext();
    MethodNode method = null;

    if (ctx.currentClass != null) {
      method =
          interpreter
              .getConstructorResolver()
              .findMethodInHierarchy(ctx.currentClass, node.name, ctx);
    }

    if (method == null && ctx.objectInstance != null && ctx.objectInstance.type != null) {
      method =
          interpreter
              .getConstructorResolver()
              .findMethodInHierarchy(ctx.objectInstance.type, node.name, ctx);
    }

    if (method == null) {
      String qName = node.qualifiedName;
      if (qName != null && qName.contains(".")) {
        String[] parts = qName.split("\\.");
        if (parts.length == 2) {
          String receiver = parts[0];
          String methodName = parts[1];
          if (ctx.locals.containsKey(receiver)) {
            Object receiverObj = ctx.locals.get(receiver);
            if (receiverObj instanceof ObjectInstance) {
              ObjectInstance objInst = (ObjectInstance) receiverObj;
              if (objInst.type != null) {
                qName = objInst.type.name + "." + methodName;
              }
            }
          }
        }
      }
      if (qName == null) qName = node.name;
      method = interpreter.getImportResolver().findMethod(qName);
    }

    if (method == null) throw new RuntimeException("Method not found: " + node.name);

    if (method.isBuiltin) return interpreter.handleBuiltinMethod(method, node);

    Object result = interpreter.evalMethodCall(node, ctx.objectInstance, ctx.locals, method);

    if (node.slotNames != null && !node.slotNames.isEmpty()) {
      if (!(result instanceof Map)) throw new RuntimeException("Method did not return slots.");

      Map<String, Object> map = (Map<String, Object>) result;
      String requestedSlot = node.slotNames.get(0);

      if (!map.containsKey(requestedSlot) && method != null && method.returnSlots != null) {
        try {
          int index = Integer.parseInt(requestedSlot);
          if (index >= 0 && index < method.returnSlots.size()) {
            requestedSlot = method.returnSlots.get(index).name;
          }
        } catch (NumberFormatException e) {
        }
      }

      if (map.containsKey(requestedSlot)) return map.get(requestedSlot);
      else throw new RuntimeException("Undefined method slot: " + requestedSlot);
    }

    return result;
  }

  @Override
  public Object visit(MultipleSlotAssignmentNode node) {
    ExecutionContext ctx = getCurrentContext();
    List<String> declaredSlots = new ArrayList<String>(ctx.slotTypes.keySet());
    Object lastValue = null;
    int slotIndex = 0;

    for (SlotAssignmentNode assign : node.assignments) {
      Object value = dispatch(assign.value);
      String target;
      if (assign.slotName != null && !assign.slotName.isEmpty() && !"_".equals(assign.slotName)) {
        target = assign.slotName;
      } else {
        if (slotIndex < declaredSlots.size()) target = declaredSlots.get(slotIndex);
        else throw new RuntimeException("Too many positional slot assignments.");
      }

      if (ctx.slotValues.containsKey(target)) {
        String declaredType = ctx.slotTypes.get(target);
        validateSlotType(ctx, target, value);
        if (declaredType.contains("|")) {
          String activeType = typeSystem.getConcreteType(typeSystem.unwrap(value));
          value = new TypedValue(value, activeType, declaredType);
        }
        ctx.slotValues.put(target, value);
        ctx.slotsInCurrentPath.add(target);
      } else {
        throw new RuntimeException("Assignment to '" + target + "' failed: Slot is not declared.");
      }
      lastValue = value;
      slotIndex++;
    }
    return lastValue;
  }

  @Override
  public Object visit(SlotAssignmentNode node) {
    ExecutionContext ctx = getCurrentContext();
    Object value = dispatch(node.value);
    String varName = node.slotName;

    String slotTarget;
    if (varName != null && !varName.isEmpty() && !"_".equals(varName)) {
      slotTarget = varName;
    } else {
      if (ctx.slotValues != null && !ctx.slotValues.isEmpty()) {
        slotTarget = ctx.slotTypes.keySet().iterator().next();
      } else {
        throw new RuntimeException("Assignment failed: Method has no declared return slots.");
      }
    }

    if (ctx.slotValues != null && ctx.slotValues.containsKey(slotTarget)) {
      String declaredType = ctx.slotTypes.get(slotTarget);
      validateSlotType(ctx, slotTarget, value);
      if (declaredType.contains("|")) {
        String activeType = typeSystem.getConcreteType(typeSystem.unwrap(value));
        value = new TypedValue(value, activeType, declaredType);
      }
      ctx.slotValues.put(slotTarget, value);
      ctx.slotsInCurrentPath.add(slotTarget);
    } else {
      throw new RuntimeException("Assignment to slot '" + slotTarget + "' failed.");
    }
    return value;
  }

  @Override
  public Object visit(FieldNode node) {
    ExecutionContext ctx = getCurrentContext();
    Object val = node.value != null ? dispatch(node.value) : null;
    if (ctx.objectInstance.type != null) {
      Object existingField =
          interpreter
              .getConstructorResolver()
              .getFieldFromHierarchy(ctx.objectInstance.type, node.name, ctx);
      if (existingField != null) throw new RuntimeException("Cannot redeclare field: " + node.name);
    }
    ctx.objectInstance.fields.put(node.name, val);
    return val;
  }

  @Override
  public Object visit(VarNode node) {
    Object val = node.value != null ? dispatch(node.value) : null;
    if (node.explicitType != null) {
      String declaredType = node.explicitType;
      getCurrentContext().localTypes.put(node.name, declaredType);
      if (!typeSystem.validateType(declaredType, val)) {
        throw new RuntimeException("Type mismatch for " + node.name + ". Expected " + declaredType);
      }
      if (declaredType.contains("|")) {
        String activeType = typeSystem.getConcreteType(typeSystem.unwrap(val));
        val = new TypedValue(val, activeType, declaredType);
      }
    }
    getCurrentContext().locals.put(node.name, val);
    return val;
  }

  @Override
  public Object visit(ExitNode node) {
    throw new Interpreter.EarlyExitException();
  }

  @Override
  public Object visit(StmtIfNode node) {
    Object testObj = dispatch(node.condition);
    boolean test = isTruthy(typeSystem.unwrap(testObj));
    List<StmtNode> branch = test ? node.thenBlock.statements : node.elseBlock.statements;

    ExecutionContext ctx = getCurrentContext();
    Set<String> prevSlots = new HashSet<String>(ctx.slotsInCurrentPath);

    for (StmtNode s : branch) {
      dispatch(s);
      if (!ctx.slotsInCurrentPath.isEmpty()
          && interpreter.shouldReturnEarly(ctx.slotValues, ctx.slotsInCurrentPath)) break;
    }
    ctx.slotsInCurrentPath = prevSlots;
    return null;
  }

  @Override
  public Object visit(ExprIfNode node) {
    Object condValue = dispatch(node.condition);
    if (isTruthy(typeSystem.unwrap(condValue))) return dispatch(node.thenExpr);
    else return dispatch(node.elseExpr);
  }

  @Override
  public Object visit(ForNode node) {
    // FIRST: Try to detect conditional pattern
    if (node.body.statements.size() == 1) {
      StmtNode stmt = node.body.statements.get(0);

      if (stmt instanceof StmtIfNode) {
        StmtIfNode ifStmt = (StmtIfNode) stmt;

        ConditionalPattern pattern = extractConditionalPattern(ifStmt, node.iterator);

        if (pattern != null && pattern.isOptimizable()) {
          // Get the array object
          Object arrayObj = dispatch(pattern.arrayExpression);

          if (arrayObj instanceof NaturalArray) {
            NaturalArray arr = (NaturalArray) arrayObj;

            // Determine loop bounds
            long start = 0, end = 0;
            boolean boundsFound = false;

            if (node.range != null) {
              Object startObj = dispatch(node.range.start);
              Object endObj = dispatch(node.range.end);

              start = toLong(startObj);
              end = toLong(endObj);
              boundsFound = true;
            } else if (node.arraySource != null) {
              Object sourceObj = dispatch(node.arraySource);

              if (sourceObj instanceof NaturalArray) {
                NaturalArray sourceArr = (NaturalArray) sourceObj;

                if (sourceArr.size() > 0) {
                  try {
                    Object first = sourceArr.get(0);
                    Object last = sourceArr.get(sourceArr.size() - 1);

                    start = toLong(first);
                    end = toLong(last);
                    boundsFound = true;
                  } catch (Exception e) {
                    // Fall through
                  }
                }
              }
            }

            if (boundsFound) {
              long min = Math.min(start, end);
              long max = Math.max(start, end);

              // NEW: Check for elif branches
              if (pattern.hasElif()) {
                // Create multi-branch formula
                MultiBranchFormula formula =
                    new MultiBranchFormula(
                        min,
                        max,
                        node.iterator,
                        pattern.condition,
                        pattern.thenExpr,
                        pattern.elifConditions,
                        pattern.elifExpressions,
                        pattern.elseExpr);

                arr.addMultiBranchFormula(formula);
                return arr;
              } else {
                // Regular conditional formula (if-else or if-only)
                ConditionalFormula formula =
                    new ConditionalFormula(
                        min,
                        max,
                        node.iterator,
                        pattern.condition,
                        pattern.thenExpr,
                        pattern.elseExpr);

                arr.addConditionalFormula(formula);
                return arr;
              }
            }
          }
        }
      }
      // NEW: Try simple assignment pattern
      else if (stmt instanceof AssignmentNode) {
        AssignmentNode assign = (AssignmentNode) stmt;
        Object result = tryOptimizeSimpleAssignment(node, assign);
        if (result != null) {
          return result; // Optimization succeeded
        }
      }
    }
    // If no pattern matched or not optimizable, fall back to normal execution
    return executeForLoopNormally(node);
  }

  private Object tryOptimizeSimpleAssignment(ForNode forNode, AssignmentNode assign) {
    String iterator = forNode.iterator;

    // Extract assignment pattern
    AssignmentPattern pattern = AssignmentPattern.extract(assign, iterator);
    if (pattern == null) {
      return null; // Not optimizable
    }

    // Get the array object
    Object arrayObj = dispatch(pattern.array);
    if (!(arrayObj instanceof NaturalArray)) {
      return null; // Not a NaturalArray
    }

    NaturalArray arr = (NaturalArray) arrayObj;

    // Determine loop bounds
    long start = 0, end = 0;
    boolean boundsFound = false;

    if (forNode.range != null) {
      // for i in [start to end]
      Object startObj = dispatch(forNode.range.start);
      Object endObj = dispatch(forNode.range.end);

      start = toLong(startObj);
      end = toLong(endObj);
      boundsFound = true;
    } else if (forNode.arraySource != null) {
      // for i in arraySource
      Object sourceObj = dispatch(forNode.arraySource);
      if (sourceObj instanceof NaturalArray) {
        NaturalArray sourceArr = (NaturalArray) sourceObj;
        if (sourceArr.size() > 0) {
          start = 0;
          end = sourceArr.size() - 1;
          boundsFound = true;
        }
      } else if (sourceObj instanceof List) {
        List<?> list = (List<?>) sourceObj;
        if (!list.isEmpty()) {
          start = 0;
          end = list.size() - 1;
          boundsFound = true;
        }
      }
    }

    if (!boundsFound) {
      return null; // Can't determine bounds
    }

    long min = Math.min(start, end);
    long max = Math.max(start, end);

    // Create loop formula
    LoopFormula formula = new LoopFormula(min, max, pattern.expression, iterator);
    arr.addLoopFormula(formula);

    // Optimization applied! No actual iteration needed.
    return arr;
  }

  private Object executeForLoopNormally(ForNode node) {
    ExecutionContext ctx = getCurrentContext();
    String iter = node.iterator;

    if (node.range != null) {
      return executeRangeLoop(ctx, node, iter);
    } else if (node.arraySource != null) {
      Object arrayObj = dispatch(node.arraySource);
      arrayObj = typeSystem.unwrap(arrayObj);
      return executeArrayLoop(ctx, node, iter, arrayObj);
    }
    throw new RuntimeException("Invalid for loop");
  }

  private Object executeArrayLoop(
      ExecutionContext ctx, ForNode node, String iter, Object arrayObj) {
    if (arrayObj instanceof NaturalArray) {
      NaturalArray natural = (NaturalArray) arrayObj;
      long size = natural.size();
      for (long i = 0; i < size; i++) {
        Object currentValue = natural.get(i);
        ctx.locals.put(iter, currentValue);
        executeLoopBody(ctx, node);
      }
    } else if (arrayObj instanceof List) {
      List<Object> list = (List<Object>) arrayObj;
      for (Object currentValue : list) {
        ctx.locals.put(iter, currentValue);
        executeLoopBody(ctx, node);
      }
    } else {
      throw new RuntimeException("Cannot iterate over: " + arrayObj.getClass().getSimpleName());
    }
    return null;
  }

  private Object executeRangeLoop(ExecutionContext ctx, ForNode node, String iter) {
    Object startObj = dispatch(node.range.start);
    Object endObj = dispatch(node.range.end);
    startObj = typeSystem.unwrap(startObj);
    endObj = typeSystem.unwrap(endObj);

    if (node.range.step != null && node.range.step instanceof BinaryOpNode) {
      BinaryOpNode binOp = (BinaryOpNode) node.range.step;
      if (binOp.left instanceof ExprNode
          && ((ExprNode) binOp.left).name.equals(iter)
          && (binOp.op.equals("*") || binOp.op.equals("/"))) {
        Object rightObj = dispatch(binOp.right);
        rightObj = typeSystem.unwrap(rightObj);
        BigDecimal factorBD = typeSystem.toBigDecimal(rightObj);
        validateFactor(factorBD, binOp.op);
        return executeMultiplicativeLoop(ctx, node, startObj, endObj, factorBD, binOp.op);
      }
    }

    BigDecimal stepBD;
    if (node.range.step != null) {
      Object stepObj = dispatch(node.range.step);
      stepBD = typeSystem.toBigDecimal(typeSystem.unwrap(stepObj));
    } else {
      BigDecimal startBD = typeSystem.toBigDecimal(startObj);
      BigDecimal endBD = typeSystem.toBigDecimal(endObj);
      stepBD = (startBD.compareTo(endBD) > 0) ? BigDecimal.ONE.negate() : BigDecimal.ONE;
    }

    if (stepBD.compareTo(BigDecimal.ZERO) == 0)
      throw new RuntimeException("Loop step cannot be zero.");

    return executeAdditiveLoop(ctx, node, startObj, endObj, stepBD);
  }

  private Object executeAdditiveLoop(
      ExecutionContext ctx, ForNode node, Object startObj, Object endObj, BigDecimal stepBD) {
    String iter = node.iterator;
    BigDecimal startBD = typeSystem.toBigDecimal(startObj);
    BigDecimal endBD = typeSystem.toBigDecimal(endObj);
    BigDecimal current = startBD;
    boolean increasing = stepBD.compareTo(BigDecimal.ZERO) > 0;

    while (shouldContinueAdditive(current, endBD, stepBD, increasing)) {
      executeIteration(ctx, node, current, startObj);
      current = current.add(stepBD);
    }
    return null;
  }

  private Object executeMultiplicativeLoop(
      ExecutionContext ctx,
      ForNode node,
      Object startObj,
      Object endObj,
      BigDecimal factorBD,
      String operation) {
    BigDecimal startBD = typeSystem.toBigDecimal(startObj);
    BigDecimal endBD = typeSystem.toBigDecimal(endObj);
    BigDecimal current = startBD;

    while (shouldContinueMultiplicative(current, startBD, endBD, factorBD, operation)) {
      executeIteration(ctx, node, current, startObj);
      if (operation.equals("*")) current = current.multiply(factorBD);
      else current = current.divide(factorBD, DECIMAL_SCALE, RoundingMode.HALF_UP);
    }
    return null;
  }

  private void executeIteration(
      ExecutionContext ctx, ForNode node, BigDecimal current, Object startObj) {
    String iter = node.iterator;
    Object currentValue = convertToAppropriateType(current, startObj);
    ctx.locals.put(iter, currentValue);
    if (!ctx.localTypes.containsKey(iter)) {
      String inferredType =
          (currentValue instanceof Integer || currentValue instanceof Long)
              ? INT.toString()
              : FLOAT.toString();
      ctx.localTypes.put(iter, inferredType);
    }
    executeLoopBody(ctx, node);
  }

  private void executeLoopBody(ExecutionContext ctx, ForNode node) {
    for (StmtNode s : node.body.statements) {
      try {
        dispatch(s);
      } catch (Interpreter.SkipIterationException e) {
        break;
      } catch (Interpreter.BreakLoopException e) {
        throw e;
      }

      if (!ctx.slotsInCurrentPath.isEmpty()
          && interpreter.shouldReturnEarly(ctx.slotValues, ctx.slotsInCurrentPath)) return;
    }
  }

  private boolean shouldContinueAdditive(
      BigDecimal current, BigDecimal end, BigDecimal step, boolean increasing) {
    return increasing ? current.compareTo(end) <= 0 : current.compareTo(end) >= 0;
  }

  private void validateFactor(BigDecimal factor, String operation) {
    if (factor.compareTo(BigDecimal.ZERO) <= 0)
      throw new RuntimeException("Factor must be positive");
  }

  private boolean shouldContinueMultiplicative(
      BigDecimal current, BigDecimal start, BigDecimal end, BigDecimal factor, String operation) {
    int startEndComparison = start.compareTo(end);
    if (operation.equals("*")) {
      return factor.compareTo(BigDecimal.ONE) > 0
          ? (startEndComparison < 0 ? current.compareTo(end) <= 0 : current.compareTo(end) >= 0)
          : (startEndComparison > 0 ? current.compareTo(end) >= 0 : current.compareTo(end) <= 0);
    } else {
      return factor.compareTo(BigDecimal.ONE) > 0
          ? (startEndComparison > 0 ? current.compareTo(end) >= 0 : current.compareTo(end) <= 0)
          : (startEndComparison < 0 ? current.compareTo(end) <= 0 : current.compareTo(end) >= 0);
    }
  }

  private Object convertToAppropriateType(BigDecimal value, Object original) {
    if ((original instanceof Integer || original instanceof Long) && value.scale() == 0) {
      try {
        return value.intValueExact();
      } catch (ArithmeticException e) {
        return value.longValue();
      }
    }
    return value;
  }

  @Override
  public Object visit(SkipNode node) {
    throw new Interpreter.SkipIterationException();
  }

  @Override
  public Object visit(BreakNode node) {
    throw new Interpreter.BreakLoopException();
  }

  @Override
  public Object visit(ReturnSlotAssignmentNode node) {
    ExecutionContext ctx = getCurrentContext();
    Object res = interpreter.evalMethodCall(node.methodCall, ctx.objectInstance, ctx.locals, null);

    if (res instanceof Map) {
      Map<String, Object> map = (Map<String, Object>) res;
      MethodNode method = null;
      if (ctx.objectInstance.type != null) {
        method =
            interpreter
                .getConstructorResolver()
                .findMethodInHierarchy(ctx.objectInstance.type, node.methodCall.name, ctx);
      }

      for (int i = 0; i < node.variableNames.size(); i++) {
        String slot = node.methodCall.slotNames.get(i);
        String requestedSlot = slot;
        if (!map.containsKey(requestedSlot) && method != null && method.returnSlots != null) {
          try {
            int index = Integer.parseInt(requestedSlot);
            if (index >= 0 && index < method.returnSlots.size()) {
              requestedSlot = method.returnSlots.get(index).name;
            }
          } catch (NumberFormatException e) {
          }
        }

        if (map.containsKey(requestedSlot)) {
          ctx.locals.put(node.variableNames.get(i), map.get(requestedSlot));
        } else {
          throw new RuntimeException("Missing slot: " + slot);
        }
      }
    } else throw new RuntimeException("Method did not return slot values");
    return res;
  }

  private Object evaluateConditionalChain(MethodCallNode call) {
    boolean result = ALL.toString().equals(call.chainType);
    for (ExprNode arg : call.chainArguments) {
      MethodCallNode currentCall = ASTFactory.createMethodCall(call.name, call.qualifiedName);
      currentCall.arguments = new ArrayList<ExprNode>();
      if (arg instanceof ArgumentListNode)
        currentCall.arguments.addAll(((ArgumentListNode) arg).arguments);
      else currentCall.arguments.add(arg);

      boolean negated = (arg instanceof UnaryNode && "!".equals(((UnaryNode) arg).op));
      Object methodResultObj = dispatch(currentCall);
      boolean methodResult = isTruthy(typeSystem.unwrap(methodResultObj));
      boolean finalResult = negated ? !methodResult : methodResult;

      if (ALL.toString().equals(call.chainType)) {
        if (!finalResult) return false;
      } else {
        if (finalResult) return true;
      }
    }
    return result;
  }

  private ConditionalPattern extractConditionalPattern(StmtIfNode ifStmt, String iterator) {
    return ConditionalPattern.extract(ifStmt, iterator);
  }

  private boolean isTruthy(Object value) {
    if (value == null) return false;
    if (value instanceof Boolean) return (Boolean) value;
    if (value instanceof Number) {
      if (value instanceof BigDecimal) return ((BigDecimal) value).compareTo(BigDecimal.ZERO) != 0;
      return ((Number) value).doubleValue() != 0.0;
    }
    if (value instanceof String)
      return !((String) value).isEmpty() && !((String) value).equalsIgnoreCase("false");
    if (value instanceof List) return !((List) value).isEmpty();
    if (value instanceof NaturalArray) return ((NaturalArray) value).size() > 0;
    return true;
  }

  private void validateSlotType(ExecutionContext ctx, String slotName, Object value) {
    if (ctx.slotTypes == null || !ctx.slotTypes.containsKey(slotName) || value == null) return;
    String type = ctx.slotTypes.get(slotName);
    if (!typeSystem.validateType(type, value)) {
      throw new RuntimeException("Type mismatch: " + slotName + " expected " + type);
    }
  }
}
