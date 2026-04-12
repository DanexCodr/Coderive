package cod.range.formula;

import cod.ast.ASTFactory;
import cod.ast.node.*;
import cod.error.ProgramError;
import cod.interpreter.Evaluator;
import cod.interpreter.context.ExecutionContext;
import cod.interpreter.handler.TypeHandler;
import java.util.*;

public class ConditionalFormula {
    public final long start;
    public final long end;
    public final String indexVar;

    private final Expr unifiedExpression;
    private final boolean usesUnifiedExpression;

    private final List<Expr> conditions;
    private final List<List<Stmt>> branchStatements;
    private final List<Stmt> elseStatements;

    private final ConditionalFormula newerFormula;
    private final ConditionalFormula olderFormula;

    public ConditionalFormula(long start, long end, String indexVar,
                             List<Expr> conditions,
                             List<List<Stmt>> branchStatements,
                             List<Stmt> elseStatements) {
        this.start = start;
        this.end = end;
        this.indexVar = indexVar;
        this.conditions = conditions != null ? conditions : new ArrayList<Expr>();
        this.branchStatements = branchStatements != null ? branchStatements : new ArrayList<List<Stmt>>();
        this.elseStatements = elseStatements != null ? elseStatements : new ArrayList<Stmt>();
        this.newerFormula = null;
        this.olderFormula = null;

        Expr built = tryBuildUnifiedExpression();
        this.unifiedExpression = built;
        this.usesUnifiedExpression = built != null;
    }

    private ConditionalFormula(long start, long end, String indexVar,
                               ConditionalFormula newerFormula,
                               ConditionalFormula olderFormula) {
        this.start = start;
        this.end = end;
        this.indexVar = indexVar;
        this.newerFormula = newerFormula;
        this.olderFormula = olderFormula;

        this.conditions = new ArrayList<Expr>();
        this.branchStatements = new ArrayList<List<Stmt>>();
        this.elseStatements = new ArrayList<Stmt>();
        this.unifiedExpression = null;
        this.usesUnifiedExpression = false;
    }

    public static ConditionalFormula compose(ConditionalFormula newerFormula, ConditionalFormula olderFormula) {
        if (newerFormula == null) return olderFormula;
        if (olderFormula == null) return newerFormula;
        long mergedStart = Math.min(newerFormula.start, olderFormula.start);
        long mergedEnd = Math.max(newerFormula.end, olderFormula.end);
        String mergedIndexVar = newerFormula.indexVar != null ? newerFormula.indexVar : olderFormula.indexVar;
        return new ConditionalFormula(mergedStart, mergedEnd, mergedIndexVar, newerFormula, olderFormula);
    }

    public boolean contains(long index) {
        if (isComposite()) {
            return (newerFormula != null && newerFormula.contains(index))
                || (olderFormula != null && olderFormula.contains(index));
        }
        return index >= start && index <= end;
    }

    public Object evaluate(long index, Evaluator evaluator, ExecutionContext context) {
        if (isComposite()) {
            return evaluateComposite(index, evaluator, context);
        }

        ExecutionContext evalCtx = context.copyWithVariable(indexVar, index, null);
        if (usesUnifiedExpression) {
            try {
                return evaluator.evaluate(unifiedExpression, evalCtx);
            } catch (ProgramError e) {
                return evaluateLegacy(evalCtx, evaluator);
            } catch (Exception e) {
                return evaluateLegacy(evalCtx, evaluator);
            }
        }
        return evaluateLegacy(evalCtx, evaluator);
    }

    private boolean isComposite() {
        return newerFormula != null || olderFormula != null;
    }

    private Object evaluateComposite(long index, Evaluator evaluator, ExecutionContext context) {
        if (newerFormula != null && newerFormula.contains(index)) {
            return newerFormula.evaluate(index, evaluator, context);
        }
        if (olderFormula != null && olderFormula.contains(index)) {
            return olderFormula.evaluate(index, evaluator, context);
        }
        return null;
    }

    private Object evaluateLegacy(ExecutionContext evalCtx, Evaluator evaluator) {
        TypeHandler typeSystem = new TypeHandler();
        for (int i = 0; i < conditions.size(); i++) {
            Object condResult = evaluator.evaluate(conditions.get(i), evalCtx);
            if (typeSystem.isTruthy(typeSystem.unwrap(condResult))) {
                return executeStatementSequence(branchStatements.get(i), evaluator, evalCtx);
            }
        }
        return executeStatementSequence(elseStatements, evaluator, evalCtx);
    }

    private Expr tryBuildUnifiedExpression() {
        if (conditions.isEmpty() || branchStatements.isEmpty() || conditions.size() != branchStatements.size()) {
            return null;
        }
        if (indexVar == null || indexVar.isEmpty()) {
            return null;
        }

        List<Expr> indicatorExpressions = new ArrayList<Expr>(conditions.size());
        List<Expr> branchExpressions = new ArrayList<Expr>(conditions.size());

        for (int i = 0; i < conditions.size(); i++) {
            Expr condition = conditions.get(i);
            if (condition == null || !isPureExpression(condition)) {
                return null;
            }
            Expr indicator = buildNumericIndicator(condition);
            if (indicator == null) {
                return null;
            }
            indicatorExpressions.add(indicator);

            Expr branchExpr = extractPureBranchExpression(branchStatements.get(i));
            if (branchExpr == null || !isPureExpression(branchExpr)) {
                return null;
            }
            branchExpressions.add(branchExpr);
        }

        Expr elseExpr = extractPureBranchExpression(elseStatements);
        if (elseExpr == null || !isPureExpression(elseExpr)) {
            return null;
        }

        Expr unified = cloneExpr(elseExpr);
        for (int i = indicatorExpressions.size() - 1; i >= 0; i--) {
            Expr indicator = cloneExpr(indicatorExpressions.get(i));
            Expr branchExpr = cloneExpr(branchExpressions.get(i));
            Expr complementIndicator = simplifyExpr(ASTFactory.createBinaryOp(one(), "-", indicator, null));
            Expr leftTerm = simplifyExpr(ASTFactory.createBinaryOp(indicator, "*", branchExpr, null));
            Expr rightTerm = simplifyExpr(ASTFactory.createBinaryOp(complementIndicator, "*", unified, null));
            unified = simplifyExpr(ASTFactory.createBinaryOp(leftTerm, "+", rightTerm, null));
        }
        return simplifyExpr(unified);
    }

    private Expr extractPureBranchExpression(List<Stmt> statements) {
        if (statements == null) return null;
        Map<String, Expr> tempExpressions = new HashMap<String, Expr>();
        Expr finalExpression = null;

        for (Stmt stmt : statements) {
            if (stmt instanceof Var) {
                Var var = (Var) stmt;
                if (var.name == null || var.value == null || !isPureExpression(var.value)) {
                    return null;
                }
                Expr resolved = substituteIdentifiers(cloneExpr(var.value), tempExpressions);
                if (!isPureExpression(resolved)) {
                    return null;
                }
                tempExpressions.put(var.name, resolved);
                continue;
            }

            if (stmt instanceof Assignment) {
                Assignment assignment = (Assignment) stmt;
                if (assignment.left instanceof Identifier && assignment.isDeclaration) {
                    Identifier id = (Identifier) assignment.left;
                    if (id.name == null || assignment.right == null || !isPureExpression(assignment.right)) {
                        return null;
                    }
                    Expr resolved = substituteIdentifiers(cloneExpr(assignment.right), tempExpressions);
                    if (!isPureExpression(resolved)) {
                        return null;
                    }
                    tempExpressions.put(id.name, resolved);
                    continue;
                }

                if (assignment.left instanceof IndexAccess) {
                    IndexAccess indexAccess = (IndexAccess) assignment.left;
                    if (!(indexAccess.index instanceof Identifier)) {
                        return null;
                    }
                    Identifier idx = (Identifier) indexAccess.index;
                    if (idx.name == null || !idx.name.equals(indexVar) || assignment.right == null) {
                        return null;
                    }
                    Expr resolved = substituteIdentifiers(cloneExpr(assignment.right), tempExpressions);
                    if (!isPureExpression(resolved)) {
                        return null;
                    }
                    finalExpression = resolved;
                    continue;
                }
            }

            return null;
        }

        return finalExpression;
    }

    private Expr substituteIdentifiers(Expr expr, Map<String, Expr> replacements) {
        if (expr == null) return null;

        if (expr instanceof Identifier) {
            Identifier id = (Identifier) expr;
            Expr replacement = replacements.get(id.name);
            return replacement != null ? cloneExpr(replacement) : expr;
        }

        if (expr instanceof BinaryOp) {
            BinaryOp op = (BinaryOp) expr;
            return ASTFactory.createBinaryOp(
                substituteIdentifiers(op.left, replacements),
                op.op,
                substituteIdentifiers(op.right, replacements),
                null
            );
        }

        if (expr instanceof Unary) {
            Unary unary = (Unary) expr;
            return ASTFactory.createUnaryOp(unary.op, substituteIdentifiers(unary.operand, replacements), null);
        }

        if (expr instanceof TypeCast) {
            TypeCast cast = (TypeCast) expr;
            return ASTFactory.createTypeCast(cast.targetType, substituteIdentifiers(cast.expression, replacements), null);
        }

        if (expr instanceof ExprIf) {
            ExprIf exprIf = (ExprIf) expr;
            return new ExprIf(
                substituteIdentifiers(exprIf.condition, replacements),
                substituteIdentifiers(exprIf.thenExpr, replacements),
                substituteIdentifiers(exprIf.elseExpr, replacements)
            );
        }

        if (expr instanceof EqualityChain) {
            EqualityChain chain = (EqualityChain) expr;
            List<Expr> args = new ArrayList<Expr>();
            if (chain.chainArguments != null) {
                for (Expr arg : chain.chainArguments) {
                    args.add(substituteIdentifiers(arg, replacements));
                }
            }
            return ASTFactory.createEqualityChain(
                substituteIdentifiers(chain.left, replacements),
                chain.operator,
                chain.isAllChain,
                args,
                null,
                null,
                null
            );
        }

        if (expr instanceof ChainedComparison) {
            ChainedComparison cmp = (ChainedComparison) expr;
            List<Expr> copiedExpressions = new ArrayList<Expr>();
            if (cmp.expressions != null) {
                for (Expr item : cmp.expressions) {
                    copiedExpressions.add(substituteIdentifiers(item, replacements));
                }
            }
            List<String> copiedOperators = cmp.operators != null ? new ArrayList<String>(cmp.operators) : new ArrayList<String>();
            return new ChainedComparison(copiedExpressions, copiedOperators);
        }

        if (expr instanceof BooleanChain) {
            BooleanChain chain = (BooleanChain) expr;
            List<Expr> expressions = new ArrayList<Expr>();
            if (chain.expressions != null) {
                for (Expr item : chain.expressions) {
                    expressions.add(substituteIdentifiers(item, replacements));
                }
            }
            return ASTFactory.createBooleanChain(chain.isAll, expressions, null);
        }

        return expr;
    }

    private Expr buildNumericIndicator(Expr condition) {
        return simplifyExpr(convertBooleanToNumeric(cloneExpr(condition)));
    }

    private Expr convertBooleanToNumeric(Expr condition) {
        if (condition == null) return null;

        Boolean constant = evaluateConstantBoolean(condition);
        if (constant != null) {
            return constant.booleanValue() ? one() : zero();
        }

        if (condition instanceof BoolLiteral) {
            return ((BoolLiteral) condition).value ? one() : zero();
        }

        if (condition instanceof Unary) {
            Unary unary = (Unary) condition;
            if ("!".equals(unary.op)) {
                Expr inner = convertBooleanToNumeric(unary.operand);
                if (inner == null) return null;
                return ASTFactory.createBinaryOp(one(), "-", inner, null);
            }
        }

        if (condition instanceof BinaryOp) {
            BinaryOp binary = (BinaryOp) condition;
            if ("&&".equals(binary.op) || "and".equals(binary.op)) {
                Expr left = convertBooleanToNumeric(binary.left);
                Expr right = convertBooleanToNumeric(binary.right);
                if (left == null || right == null) return null;
                return ASTFactory.createBinaryOp(left, "*", right, null);
            }
            if ("||".equals(binary.op) || "or".equals(binary.op)) {
                Expr left = convertBooleanToNumeric(binary.left);
                Expr right = convertBooleanToNumeric(binary.right);
                if (left == null || right == null) return null;
                Expr leftCloneA = cloneExpr(left);
                Expr rightCloneA = cloneExpr(right);
                Expr leftCloneB = cloneExpr(left);
                Expr rightCloneB = cloneExpr(right);
                Expr sum = ASTFactory.createBinaryOp(leftCloneA, "+", rightCloneA, null);
                Expr prod = ASTFactory.createBinaryOp(leftCloneB, "*", rightCloneB, null);
                return ASTFactory.createBinaryOp(sum, "-", prod, null);
            }
        }

        return new ExprIf(condition, one(), zero());
    }

    private boolean isPureExpression(Expr expr) {
        if (expr == null) return false;

        if (expr instanceof Identifier
            || expr instanceof IntLiteral
            || expr instanceof FloatLiteral
            || expr instanceof BoolLiteral
            || expr instanceof TextLiteral
            || expr instanceof NoneLiteral
            || expr instanceof ValueExpr) {
            return true;
        }

        if (expr instanceof BinaryOp) {
            BinaryOp op = (BinaryOp) expr;
            return isPureExpression(op.left) && isPureExpression(op.right);
        }

        if (expr instanceof Unary) {
            Unary unary = (Unary) expr;
            return isPureExpression(unary.operand);
        }

        if (expr instanceof TypeCast) {
            TypeCast cast = (TypeCast) expr;
            return isPureExpression(cast.expression);
        }

        if (expr instanceof ExprIf) {
            ExprIf exprIf = (ExprIf) expr;
            return isPureExpression(exprIf.condition)
                && isPureExpression(exprIf.thenExpr)
                && isPureExpression(exprIf.elseExpr);
        }

        if (expr instanceof EqualityChain) {
            EqualityChain chain = (EqualityChain) expr;
            if (!isPureExpression(chain.left)) return false;
            if (chain.chainArguments != null) {
                for (Expr arg : chain.chainArguments) {
                    if (!isPureExpression(arg)) return false;
                }
            }
            return true;
        }

        if (expr instanceof ChainedComparison) {
            ChainedComparison chain = (ChainedComparison) expr;
            if (chain.expressions != null) {
                for (Expr item : chain.expressions) {
                    if (!isPureExpression(item)) return false;
                }
            }
            return true;
        }

        if (expr instanceof BooleanChain) {
            BooleanChain chain = (BooleanChain) expr;
            if (chain.expressions != null) {
                for (Expr item : chain.expressions) {
                    if (!isPureExpression(item)) return false;
                }
            }
            return true;
        }

        return false;
    }

    private Expr simplifyExpr(Expr expr) {
        if (!(expr instanceof BinaryOp)) return expr;

        BinaryOp op = (BinaryOp) expr;
        Expr left = simplifyExpr(op.left);
        Expr right = simplifyExpr(op.right);

        Expr folded = foldNumericConstants(op.op, left, right);
        if (folded != null) {
            return folded;
        }

        if ("*".equals(op.op)) {
            if (isZero(left) || isZero(right)) return zero();
            if (isOne(left)) return right;
            if (isOne(right)) return left;
        } else if ("+".equals(op.op)) {
            if (isZero(left)) return right;
            if (isZero(right)) return left;
            if (structurallyEqual(left, right)) {
                return ASTFactory.createBinaryOp(ASTFactory.createIntLiteral(2, null), "*", left, null);
            }
            Expr factored = tryFactorCommonTerm(left, right);
            if (factored != null) {
                return simplifyExpr(factored);
            }
            Expr branchCollapse = tryCollapseEquivalentBranchMix(left, right);
            if (branchCollapse != null) {
                return branchCollapse;
            }
        } else if ("-".equals(op.op)) {
            if (isZero(right)) return left;
            if (sameLiteral(left, right) || structurallyEqual(left, right)) return zero();
        }

        return ASTFactory.createBinaryOp(left, op.op, right, null);
    }

    private Expr foldNumericConstants(String op, Expr left, Expr right) {
        if (!(left instanceof IntLiteral) || !(right instanceof IntLiteral)) {
            return null;
        }
        long leftValue = ((IntLiteral) left).value.longValue();
        long rightValue = ((IntLiteral) right).value.longValue();
        if ("+".equals(op)) return ASTFactory.createLongLiteral(leftValue + rightValue, null);
        if ("-".equals(op)) return ASTFactory.createLongLiteral(leftValue - rightValue, null);
        if ("*".equals(op)) return ASTFactory.createLongLiteral(leftValue * rightValue, null);
        if ("/".equals(op)) {
            if (rightValue == 0L) return null;
            if (leftValue % rightValue != 0L) return null;
            return ASTFactory.createLongLiteral(leftValue / rightValue, null);
        }
        return null;
    }

    private Expr tryFactorCommonTerm(Expr left, Expr right) {
        if (!(left instanceof BinaryOp) || !(right instanceof BinaryOp)) {
            return null;
        }
        BinaryOp leftBin = (BinaryOp) left;
        BinaryOp rightBin = (BinaryOp) right;
        if (!"*".equals(leftBin.op) || !"*".equals(rightBin.op)) {
            return null;
        }

        if (structurallyEqual(leftBin.left, rightBin.left)) {
            Expr sum = simplifyExpr(ASTFactory.createBinaryOp(leftBin.right, "+", rightBin.right, null));
            return ASTFactory.createBinaryOp(leftBin.left, "*", sum, null);
        }
        if (structurallyEqual(leftBin.left, rightBin.right)) {
            Expr sum = simplifyExpr(ASTFactory.createBinaryOp(leftBin.right, "+", rightBin.left, null));
            return ASTFactory.createBinaryOp(leftBin.left, "*", sum, null);
        }
        if (structurallyEqual(leftBin.right, rightBin.left)) {
            Expr sum = simplifyExpr(ASTFactory.createBinaryOp(leftBin.left, "+", rightBin.right, null));
            return ASTFactory.createBinaryOp(leftBin.right, "*", sum, null);
        }
        if (structurallyEqual(leftBin.right, rightBin.right)) {
            Expr sum = simplifyExpr(ASTFactory.createBinaryOp(leftBin.left, "+", rightBin.left, null));
            return ASTFactory.createBinaryOp(leftBin.right, "*", sum, null);
        }
        return null;
    }

    private Expr tryCollapseEquivalentBranchMix(Expr left, Expr right) {
        Expr[] leftParts = splitProduct(left);
        Expr[] rightParts = splitProduct(right);
        if (leftParts == null || rightParts == null) {
            return null;
        }

        Expr leftCoefficient = leftParts[0];
        Expr leftValue = leftParts[1];
        Expr rightCoefficient = rightParts[0];
        Expr rightValue = rightParts[1];

        if (!structurallyEqual(leftValue, rightValue)) {
            return null;
        }
        if (isComplementIndicator(leftCoefficient, rightCoefficient)) {
            return leftValue;
        }
        return null;
    }

    private Expr[] splitProduct(Expr expr) {
        if (!(expr instanceof BinaryOp)) return null;
        BinaryOp op = (BinaryOp) expr;
        if (!"*".equals(op.op)) return null;
        return new Expr[] { op.left, op.right };
    }

    private boolean isComplementIndicator(Expr a, Expr b) {
        if (!(b instanceof BinaryOp)) return false;
        BinaryOp bOp = (BinaryOp) b;
        return "-".equals(bOp.op) && isOne(bOp.left) && structurallyEqual(a, bOp.right);
    }

    private boolean structurallyEqual(Expr a, Expr b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (!a.getClass().equals(b.getClass())) return false;

        if (a instanceof Identifier) {
            String leftName = ((Identifier) a).name;
            String rightName = ((Identifier) b).name;
            return leftName != null ? leftName.equals(rightName) : rightName == null;
        }
        if (a instanceof IntLiteral) {
            return ((IntLiteral) a).value.equals(((IntLiteral) b).value);
        }
        if (a instanceof FloatLiteral) {
            return ((FloatLiteral) a).value.equals(((FloatLiteral) b).value);
        }
        if (a instanceof BoolLiteral) {
            return ((BoolLiteral) a).value == ((BoolLiteral) b).value;
        }
        if (a instanceof TextLiteral) {
            return Objects.equals(((TextLiteral) a).value, ((TextLiteral) b).value);
        }
        if (a instanceof NoneLiteral) {
            return true;
        }
        if (a instanceof BinaryOp) {
            BinaryOp x = (BinaryOp) a;
            BinaryOp y = (BinaryOp) b;
            return Objects.equals(x.op, y.op)
                && structurallyEqual(x.left, y.left)
                && structurallyEqual(x.right, y.right);
        }
        if (a instanceof Unary) {
            Unary x = (Unary) a;
            Unary y = (Unary) b;
            return Objects.equals(x.op, y.op) && structurallyEqual(x.operand, y.operand);
        }
        if (a instanceof ExprIf) {
            ExprIf x = (ExprIf) a;
            ExprIf y = (ExprIf) b;
            return structurallyEqual(x.condition, y.condition)
                && structurallyEqual(x.thenExpr, y.thenExpr)
                && structurallyEqual(x.elseExpr, y.elseExpr);
        }
        return a.toString().equals(b.toString());
    }

    private Boolean evaluateConstantBoolean(Expr expr) {
        if (expr == null) return null;

        if (expr instanceof BoolLiteral) {
            return Boolean.valueOf(((BoolLiteral) expr).value);
        }
        if (expr instanceof Unary) {
            Unary unary = (Unary) expr;
            if ("!".equals(unary.op)) {
                Boolean inner = evaluateConstantBoolean(unary.operand);
                return inner != null ? Boolean.valueOf(!inner.booleanValue()) : null;
            }
        }
        if (expr instanceof BinaryOp) {
            BinaryOp op = (BinaryOp) expr;
            if ("&&".equals(op.op) || "and".equals(op.op)) {
                Boolean left = evaluateConstantBoolean(op.left);
                Boolean right = evaluateConstantBoolean(op.right);
                if (left != null && right != null) {
                    return Boolean.valueOf(left.booleanValue() && right.booleanValue());
                }
                return null;
            }
            if ("||".equals(op.op) || "or".equals(op.op)) {
                Boolean left = evaluateConstantBoolean(op.left);
                Boolean right = evaluateConstantBoolean(op.right);
                if (left != null && right != null) {
                    return Boolean.valueOf(left.booleanValue() || right.booleanValue());
                }
                return null;
            }

            Object leftConst = constantValue(op.left);
            Object rightConst = constantValue(op.right);
            if (leftConst == null || rightConst == null) {
                return null;
            }

            if ("==".equals(op.op)) return Boolean.valueOf(leftConst.equals(rightConst));
            if ("!=".equals(op.op)) return Boolean.valueOf(!leftConst.equals(rightConst));

            Integer cmp = compareConstants(leftConst, rightConst);
            if (cmp == null) return null;
            if (">".equals(op.op)) return Boolean.valueOf(cmp.intValue() > 0);
            if ("<".equals(op.op)) return Boolean.valueOf(cmp.intValue() < 0);
            if (">=".equals(op.op)) return Boolean.valueOf(cmp.intValue() >= 0);
            if ("<=".equals(op.op)) return Boolean.valueOf(cmp.intValue() <= 0);
        }
        return null;
    }

    private Object constantValue(Expr expr) {
        if (expr instanceof IntLiteral) {
            return ((IntLiteral) expr).value;
        }
        if (expr instanceof FloatLiteral) {
            return ((FloatLiteral) expr).value;
        }
        if (expr instanceof BoolLiteral) {
            return Boolean.valueOf(((BoolLiteral) expr).value);
        }
        if (expr instanceof TextLiteral) {
            return ((TextLiteral) expr).value;
        }
        return null;
    }

    private Integer compareConstants(Object left, Object right) {
        if (left instanceof cod.math.AutoStackingNumber && right instanceof cod.math.AutoStackingNumber) {
            return Integer.valueOf(((cod.math.AutoStackingNumber) left).compareTo((cod.math.AutoStackingNumber) right));
        }
        if (left instanceof Boolean && right instanceof Boolean) {
            boolean leftBool = ((Boolean) left).booleanValue();
            boolean rightBool = ((Boolean) right).booleanValue();
            return Integer.valueOf(leftBool == rightBool ? 0 : (leftBool ? 1 : -1));
        }
        if (left instanceof String && right instanceof String) {
            return Integer.valueOf(((String) left).compareTo((String) right));
        }
        return null;
    }

    private boolean isZero(Expr expr) {
        if (!(expr instanceof IntLiteral)) return false;
        return ((IntLiteral) expr).value.isZero();
    }

    private boolean isOne(Expr expr) {
        if (!(expr instanceof IntLiteral)) return false;
        return ((IntLiteral) expr).value.longValue() == 1L;
    }

    private boolean sameLiteral(Expr a, Expr b) {
        if (a instanceof IntLiteral && b instanceof IntLiteral) {
            return ((IntLiteral) a).value.equals(((IntLiteral) b).value);
        }
        if (a instanceof BoolLiteral && b instanceof BoolLiteral) {
            return ((BoolLiteral) a).value == ((BoolLiteral) b).value;
        }
        return false;
    }

    private Expr cloneExpr(Expr expr) {
        if (expr == null) return null;
        if (expr instanceof Identifier) return ASTFactory.createIdentifier(((Identifier) expr).name, null);
        if (expr instanceof IntLiteral) {
            long value = ((IntLiteral) expr).value.longValue();
            if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
                return ASTFactory.createIntLiteral((int) value, null);
            }
            return ASTFactory.createLongLiteral(value, null);
        }
        if (expr instanceof FloatLiteral) return ASTFactory.createFloatLiteral(((FloatLiteral) expr).value, null);
        if (expr instanceof BoolLiteral) return ASTFactory.createBoolLiteral(((BoolLiteral) expr).value, null);
        if (expr instanceof TextLiteral) return ASTFactory.createTextLiteral(((TextLiteral) expr).value, null);
        if (expr instanceof NoneLiteral) return ASTFactory.createNoneLiteral(null);
        if (expr instanceof ValueExpr) return new ValueExpr(((ValueExpr) expr).getValue());
        if (expr instanceof BinaryOp) {
            BinaryOp op = (BinaryOp) expr;
            return ASTFactory.createBinaryOp(cloneExpr(op.left), op.op, cloneExpr(op.right), null);
        }
        if (expr instanceof Unary) {
            Unary unary = (Unary) expr;
            return ASTFactory.createUnaryOp(unary.op, cloneExpr(unary.operand), null);
        }
        if (expr instanceof TypeCast) {
            TypeCast cast = (TypeCast) expr;
            return ASTFactory.createTypeCast(cast.targetType, cloneExpr(cast.expression), null);
        }
        if (expr instanceof ExprIf) {
            ExprIf exprIf = (ExprIf) expr;
            return new ExprIf(cloneExpr(exprIf.condition), cloneExpr(exprIf.thenExpr), cloneExpr(exprIf.elseExpr));
        }
        if (expr instanceof EqualityChain) {
            EqualityChain chain = (EqualityChain) expr;
            List<Expr> args = new ArrayList<Expr>();
            if (chain.chainArguments != null) {
                for (Expr arg : chain.chainArguments) {
                    args.add(cloneExpr(arg));
                }
            }
            return ASTFactory.createEqualityChain(cloneExpr(chain.left), chain.operator, chain.isAllChain, args, null, null, null);
        }
        if (expr instanceof ChainedComparison) {
            ChainedComparison source = (ChainedComparison) expr;
            List<Expr> copiedExpressions = new ArrayList<Expr>();
            if (source.expressions != null) {
                for (Expr item : source.expressions) {
                    copiedExpressions.add(cloneExpr(item));
                }
            }
            List<String> copiedOperators = source.operators != null ? new ArrayList<String>(source.operators) : new ArrayList<String>();
            return new ChainedComparison(copiedExpressions, copiedOperators);
        }
        if (expr instanceof BooleanChain) {
            BooleanChain source = (BooleanChain) expr;
            List<Expr> items = new ArrayList<Expr>();
            if (source.expressions != null) {
                for (Expr item : source.expressions) {
                    items.add(cloneExpr(item));
                }
            }
            return ASTFactory.createBooleanChain(source.isAll, items, null);
        }
        return expr;
    }

    private Expr zero() {
        return ASTFactory.createIntLiteral(0, null);
    }

    private Expr one() {
        return ASTFactory.createIntLiteral(1, null);
    }

    private Object executeStatementSequence(List<Stmt> statements, Evaluator evaluator, ExecutionContext ctx) {
        Object lastResult = null;
        ctx.pushScope();
        try {
            for (Stmt stmt : statements) {
                lastResult = evaluator.evaluate(stmt, ctx);
            }
        } finally {
            ctx.popScope();
        }
        return lastResult;
    }
}
