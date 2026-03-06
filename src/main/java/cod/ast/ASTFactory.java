package cod.ast;

import cod.syntax.Keyword;
import cod.lexer.Token;
import cod.math.AutoStackingNumber;
import java.util.Arrays;
import java.util.List;

public class ASTFactory {

    private FlatAST ast;

    public ASTFactory() {
        this.ast = new FlatAST();
    }

    public FlatAST getAST() {
        return ast;
    }

    private SourceSpan span(Token token) {
        return token != null ? new SourceSpan(token) : null;
    }

    private static int[] toIntArray(List<Integer> list) {
        if (list == null) return new int[0];
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    private int createEmptyUseNode() {
        return createUseNode(null, null);
    }

    // === PROGRAM ===

    public int createProgram() {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.PROGRAM;
        data.span = null;
        return ast.add(data);
    }

    // === UNIT ===

    public int createUnit(String name, Token unitToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.UNIT;
        data.str0 = name;
        data.span = span(unitToken);
        data.child0 = createEmptyUseNode();
        data.children = new int[0];
        data.children2 = new int[0];
        return ast.add(data);
    }

    public int createUnit(String name, String mainClassName, Token unitToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.UNIT;
        data.str0 = name;
        data.str2 = mainClassName;
        data.span = span(unitToken);
        data.child0 = createEmptyUseNode();
        data.children = new int[0];
        data.children2 = new int[0];
        return ast.add(data);
    }

    // === USE ===

    public int createUseNode(List<String> imports, Token useToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.USE;
        data.span = span(useToken);
        if (imports != null) {
            data.strings = imports.toArray(new String[0]);
        } else {
            data.strings = new String[0];
        }
        return ast.add(data);
    }

    // === TYPE ===

    public int createType(String name, Keyword visibility, String extendName, Token nameToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.TYPE;
        data.str0 = name;
        data.kw = visibility;
        data.str2 = extendName;
        data.span = span(nameToken);
        data.children = new int[0];
        data.children2 = new int[0];
        data.strings = new String[0];
        data.objVal = new Object[]{new int[0], new int[0]};
        return ast.add(data);
    }

    // === POLICY ===

    public int createPolicy(String name, Keyword visibility, Token nameToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.POLICY;
        data.str0 = name;
        data.kw = visibility;
        data.span = span(nameToken);
        data.children = new int[0];
        data.strings = new String[0];
        return ast.add(data);
    }

    // === POLICY_METHOD ===

    public int createPolicyMethod(String name, Token nameToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.POLICY_METHOD;
        data.str0 = name;
        data.span = span(nameToken);
        data.children = new int[0];
        data.children2 = new int[0];
        return ast.add(data);
    }

    // === FIELD ===

    public int createField(String name, String type, int valueId, Token nameToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.FIELD;
        data.str0 = name;
        data.str1 = type;
        data.child0 = valueId;
        data.span = span(nameToken);
        return ast.add(data);
    }

    public int createField(String name, String type, Token nameToken) {
        return createField(name, type, FlatAST.NULL, nameToken);
    }

    // === ASSIGNMENT ===

    public int createAsmt(int targetId, int valueId, boolean isDeclaration, Token assignToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.ASSIGNMENT;
        data.child0 = targetId;
        data.child1 = valueId;
        data.bool0 = isDeclaration;
        data.span = span(assignToken);
        return ast.add(data);
    }

    // === CONSTRUCTOR_CALL ===

    public int createConstructorCall(String className, List<Integer> argIds, Token nameToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.CONSTRUCTOR_CALL;
        data.str0 = className;
        data.span = span(nameToken);
        int[] arr = toIntArray(argIds);
        data.children = arr;
        data.strings = new String[arr.length];
        Arrays.fill(data.strings, "");
        return ast.add(data);
    }

    // === CONSTRUCTOR ===

    public int createConstructor(List<Integer> paramIds, List<Integer> bodyIds, Token thisToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.CONSTRUCTOR;
        data.span = span(thisToken);
        data.children = toIntArray(paramIds);
        data.children2 = toIntArray(bodyIds);
        return ast.add(data);
    }

    // === METHOD ===

    public int createMethod(String name, Keyword visibility, List<Integer> returnSlotIds, Token nameToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.METHOD;
        data.str0 = name;
        data.kw = visibility;
        data.span = span(nameToken);
        data.children = new int[0];
        data.children2 = toIntArray(returnSlotIds);
        data.objVal = new int[0];
        return ast.add(data);
    }

    // === PARAM ===

    public int createParam(String name, String type, int defaultValueId, boolean typeInferred, Token nameToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.PARAM;
        data.str0 = name;
        data.str1 = type;
        data.child0 = defaultValueId;
        data.bool0 = (defaultValueId != FlatAST.NULL);
        data.bool1 = typeInferred;
        data.span = span(nameToken);
        return ast.add(data);
    }

    // === SLOT ===

    public int createSlot(String type, String name, Token nameToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.SLOT;
        data.str0 = name;
        data.str1 = type;
        data.span = span(nameToken);
        return ast.add(data);
    }

    // === PROPERTY_ACCESS ===

    public int createPropertyAccess(int leftId, int rightId, Token dotToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.PROPERTY_ACCESS;
        data.child0 = leftId;
        data.child1 = rightId;
        data.span = span(dotToken);
        return ast.add(data);
    }

    // === IDENTIFIER ===

    public int createIdentifier(String name, Token token) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.IDENTIFIER;
        data.str0 = name;
        data.span = span(token);
        return ast.add(data);
    }

    // === INT_LITERAL ===

    public int createIntLiteral(int value, Token token) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.INT_LITERAL;
        data.longVal = value;
        data.span = span(token);
        return ast.add(data);
    }

    public int createLongLiteral(long value, Token token) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.INT_LITERAL;
        data.longVal = value;
        data.span = span(token);
        return ast.add(data);
    }

    // === FLOAT_LITERAL ===

    public int createFloatLiteral(AutoStackingNumber value, Token token) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.FLOAT_LITERAL;
        data.objVal = value;
        data.span = span(token);
        return ast.add(data);
    }

    // === TEXT_LITERAL ===

    public int createTextLiteral(String value, Token token) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.TEXT_LITERAL;
        data.str0 = value;
        data.bool0 = false;
        data.span = span(token);
        return ast.add(data);
    }

    public int createTextLiteralInterpolated(String value, Token token) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.TEXT_LITERAL;
        data.str0 = value;
        data.bool0 = true;
        data.span = span(token);
        return ast.add(data);
    }

    // === BOOL_LITERAL ===

    public int createBoolLiteral(boolean value, Token token) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.BOOL_LITERAL;
        data.bool0 = value;
        data.span = span(token);
        return ast.add(data);
    }

    // === NONE_LITERAL ===

    public int createNoneLiteral(Token token) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.NONE_LITERAL;
        data.span = span(token);
        return ast.add(data);
    }

    // === THIS ===

    public int createThisExpr(String className, Token thisToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.THIS;
        data.str0 = className;
        data.span = span(thisToken);
        return ast.add(data);
    }

    // === SUPER ===

    public int createSuperExpr(Token superToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.SUPER;
        data.span = span(superToken);
        return ast.add(data);
    }

    // === BINARY_OP ===

    public int createBinaryOp(int leftId, String op, int rightId, Token opToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.BINARY_OP;
        data.str0 = op;
        data.child0 = leftId;
        data.child1 = rightId;
        data.span = span(opToken);
        return ast.add(data);
    }

    // === EQUALITY_CHAIN ===

    public int createEqualityChain(int leftId, String operator, boolean isAllChain,
            List<Integer> chainArgIds, Token leftToken, Token opToken, Token chainToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.EQUALITY_CHAIN;
        data.str0 = operator;
        data.bool0 = isAllChain;
        data.child0 = leftId;
        data.children = toIntArray(chainArgIds);
        data.span = span(opToken != null ? opToken : leftToken);
        return ast.add(data);
    }

    // === BOOLEAN_CHAIN ===

    public int createBooleanChain(boolean isAll, List<Integer> exprIds, Token keywordToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.BOOLEAN_CHAIN;
        data.bool0 = isAll;
        data.children = toIntArray(exprIds);
        data.span = span(keywordToken);
        return ast.add(data);
    }

    // === UNARY ===

    public int createUnaryOp(String op, int operandId, Token opToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.UNARY;
        data.str0 = op;
        data.child0 = operandId;
        data.span = span(opToken);
        return ast.add(data);
    }

    // === TYPE_CAST ===

    public int createTypeCast(String targetType, int expressionId, Token lparenToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.TYPE_CAST;
        data.str0 = targetType;
        data.child0 = expressionId;
        data.span = span(lparenToken);
        return ast.add(data);
    }

    // === SLOT_ASSIGNMENT (implicit return) ===

    public int createImplicitReturn(int returnExprId, Token returnToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.SLOT_ASSIGNMENT;
        data.str0 = "_";
        data.child0 = returnExprId;
        data.span = span(returnToken);
        return ast.add(data);
    }

    // === METHOD_CALL ===

    public int createMethodCall(String name, String qualifiedName, Token nameToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.METHOD_CALL;
        data.str0 = name;
        data.str1 = qualifiedName;
        data.child0 = FlatAST.NULL;
        data.bool0 = false;
        data.bool1 = false;
        data.bool2 = false;
        data.children = new int[0];
        data.strings = new String[0];
        data.strings2 = new String[0];
        data.span = span(nameToken);
        return ast.add(data);
    }

    // === ARRAY ===

    public int createArray(List<Integer> elementIds, Token lbracketToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.ARRAY;
        data.children = toIntArray(elementIds);
        data.span = span(lbracketToken);
        return ast.add(data);
    }

    // === TUPLE ===

    public int createTuple(List<Integer> elementIds, Token lparenToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.TUPLE;
        data.children = toIntArray(elementIds);
        data.span = span(lparenToken);
        return ast.add(data);
    }

    // === INDEX_ACCESS ===

    public int createIndexAccess(int arrayId, int indexId, Token lbracketToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.INDEX_ACCESS;
        data.child0 = arrayId;
        data.child1 = indexId;
        data.span = span(lbracketToken);
        return ast.add(data);
    }

    // === RANGE_INDEX ===

    public int createRangeIndex(int stepId, int startId, int endId, Token byToken, Token toToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.RANGE_INDEX;
        data.child0 = stepId;
        data.child1 = startId;
        data.child2 = endId;
        data.span = span(byToken != null ? byToken : toToken);
        return ast.add(data);
    }

    // === MULTI_RANGE_INDEX ===

    public int createMultiRangeIndex(List<Integer> rangeIds, Token firstLbracketToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.MULTI_RANGE_INDEX;
        data.children = toIntArray(rangeIds);
        data.span = span(firstLbracketToken);
        return ast.add(data);
    }

    // === EXPR_IF ===

    public int createIfExpr(int conditionId, int thenId, int elseId, Token ifToken, Token elseToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.EXPR_IF;
        data.child0 = conditionId;
        data.child1 = thenId;
        data.child2 = elseId;
        data.span = span(ifToken);
        return ast.add(data);
    }

    // === STMT_IF ===

    public int createIfStmt(int conditionId, Token ifToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.STMT_IF;
        data.child0 = conditionId;
        data.child1 = createBlock(ifToken);
        data.child2 = createBlock(null);
        data.span = span(ifToken);
        return ast.add(data);
    }

    // === FOR (range-based) ===

    public int createFor(String iterator, int rangeId, Token forToken, Token iteratorToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.FOR;
        data.str0 = iterator;
        data.child0 = rangeId;
        data.child1 = FlatAST.NULL;
        data.child2 = createBlock(forToken);
        data.span = span(forToken);
        return ast.add(data);
    }

    // === FOR (array-based) ===

    public int createForArray(String iterator, int arraySourceId, Token forToken, Token iteratorToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.FOR;
        data.str0 = iterator;
        data.child0 = FlatAST.NULL;
        data.child1 = arraySourceId;
        data.child2 = createBlock(forToken);
        data.span = span(forToken);
        return ast.add(data);
    }

    // === RANGE ===

    public int createRange(int stepId, int startId, int endId, Token byToken, Token toToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.RANGE;
        data.child0 = stepId;
        data.child1 = startId;
        data.child2 = endId;
        data.span = span(byToken != null ? byToken : toToken);
        return ast.add(data);
    }

    // === BLOCK ===

    public int createBlock(Token lbraceToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.BLOCK;
        data.children = new int[0];
        data.span = span(lbraceToken);
        return ast.add(data);
    }

    public int createBlock(List<Integer> stmtIds, Token lbraceToken, Token rbraceToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.BLOCK;
        data.children = toIntArray(stmtIds);
        if (lbraceToken != null && rbraceToken != null) {
            data.span = new SourceSpan(lbraceToken, rbraceToken);
        } else {
            data.span = span(lbraceToken);
        }
        return ast.add(data);
    }

    // === VAR ===

    public int createVar(String name, int valueId, Token nameToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.VAR;
        data.str0 = name;
        data.child0 = valueId;
        data.span = span(nameToken);
        return ast.add(data);
    }

    // === EXIT ===

    public int createExit(Token exitToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.EXIT;
        data.span = span(exitToken);
        return ast.add(data);
    }

    // === ARGUMENT_LIST (temporary holder, stored as TUPLE) ===

    public int createArgumentList(List<Integer> argIds, Token lparenToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.TUPLE;
        data.children = toIntArray(argIds);
        data.span = span(lparenToken);
        return ast.add(data);
    }

    // === SKIP ===

    public int createSkipStmt(Token skipToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.SKIP;
        data.span = span(skipToken);
        return ast.add(data);
    }

    // === BREAK ===

    public int createBreakStmt(Token breakToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.BREAK;
        data.span = span(breakToken);
        return ast.add(data);
    }

    // === RETURN_SLOT_ASSIGNMENT ===

    public int createReturnSlotAsmt(List<String> variableNames, int methodCallId, Token assignToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.RETURN_SLOT_ASSIGNMENT;
        data.child0 = methodCallId;
        data.strings = variableNames != null ? variableNames.toArray(new String[0]) : new String[0];
        data.span = span(assignToken);
        return ast.add(data);
    }

    // === SLOT_DECLARATION ===

    public int createSlotDeclaration(List<String> slotNames, Token lbracketToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.SLOT_DECLARATION;
        data.strings = slotNames != null ? slotNames.toArray(new String[0]) : new String[0];
        data.span = span(lbracketToken);
        return ast.add(data);
    }

    // === SLOT_ASSIGNMENT ===

    public int createSlotAsmt(String slotName, int valueId, Token colonToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.SLOT_ASSIGNMENT;
        data.str0 = slotName;
        data.child0 = valueId;
        data.span = span(colonToken);
        return ast.add(data);
    }

    // === MULTIPLE_SLOT_ASSIGNMENT ===

    public int createMultipleSlotAsmt(List<Integer> assignmentIds, Token tildeArrowToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.MULTIPLE_SLOT_ASSIGNMENT;
        data.children = toIntArray(assignmentIds);
        data.span = span(tildeArrowToken);
        return ast.add(data);
    }

    // === LAMBDA ===

    public int createLambda(List<Integer> paramIds, List<Integer> returnSlotIds, int bodyId, Token lambdaToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.LAMBDA;
        data.child0 = bodyId;
        data.children = toIntArray(paramIds);
        data.children2 = toIntArray(returnSlotIds);
        data.span = span(lambdaToken);
        return ast.add(data);
    }

    public int createLambdaEmpty(List<Integer> paramIds, Token lambdaToken) {
        FlatAST.NodeData data = new FlatAST.NodeData();
        data.kind = NodeKind.LAMBDA;
        data.child0 = FlatAST.NULL;
        data.children = toIntArray(paramIds);
        data.children2 = new int[0];
        data.span = span(lambdaToken);
        return ast.add(data);
    }

    // === CONSTRUCTOR SIGNATURE ===

    public String getConstructorSignature(int constructorId) {
        int[] paramIds = ast.constructorParams(constructorId);
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < paramIds.length; i++) {
            if (i > 0) sb.append(", ");
            int p = paramIds[i];
            sb.append(ast.paramName(p)).append(": ").append(ast.paramType(p));
            if (ast.paramHasDefault(p)) {
                sb.append(" = default");
            }
        }
        sb.append(")");
        return sb.toString();
    }
}