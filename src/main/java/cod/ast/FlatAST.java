package cod.ast;

import cod.syntax.Keyword;
import cod.math.AutoStackingNumber;
import java.util.Arrays;
import java.util.Map;

public class FlatAST {

    public static final int NULL = -1;

    private NodeData[] nodes;
    private int size;

    static final class NodeData {
        NodeKind kind;
        SourceSpan span;
        String str0, str1, str2;
        int child0 = -1, child1 = -1, child2 = -1, child3 = -1;
        boolean bool0, bool1, bool2;
        cod.syntax.Keyword kw;
        long longVal;
        Object objVal;
        int[] children;
        int[] children2;
        String[] strings;
        String[] strings2;
    }

    public FlatAST() {
        this.nodes = new NodeData[64];
        this.size = 0;
    }

    public int add(NodeData data) {
        if (size >= nodes.length) {
            nodes = Arrays.copyOf(nodes, nodes.length * 2);
        }
        int id = size++;
        nodes[id] = data;
        return id;
    }

    public int size() { return size; }

    // Kind and span accessors
    public NodeKind kind(int n) { return nodes[n].kind; }
    public SourceSpan span(int n) { return nodes[n].span; }

    // === PROGRAM ===
    public int programUnit(int n) { return nodes[n].child0; }

    // === UNIT ===
    public String unitName(int n) { return nodes[n].str0; }
    public String unitMainClass(int n) { return nodes[n].str2; }
    public int unitImports(int n) { return nodes[n].child0; }
    public int[] unitTypes(int n) { return nodes[n].children != null ? nodes[n].children : new int[0]; }
    public int[] unitPolicies(int n) { return nodes[n].children2 != null ? nodes[n].children2 : new int[0]; }
    @SuppressWarnings("unchecked")
    public Map<String, Integer> unitResolvedImports(int n) {
        return nodes[n].objVal instanceof Map ? (Map<String, Integer>) nodes[n].objVal : null;
    }
    public void unitSetResolvedImports(int n, Map<String, Integer> m) { nodes[n].objVal = m; }

    // === USE ===
    public String[] useImports(int n) { return nodes[n].strings != null ? nodes[n].strings : new String[0]; }

    // === TYPE ===
    public String typeName(int n) { return nodes[n].str0; }
    public String typeExtend(int n) { return nodes[n].str2; }
    public Keyword typeVisibility(int n) { return nodes[n].kw; }
    public int[] typeFields(int n) { return nodes[n].children != null ? nodes[n].children : new int[0]; }
    public int[] typeMethods(int n) { return nodes[n].children2 != null ? nodes[n].children2 : new int[0]; }
    public String[] typeImplementedPolicies(int n) { return nodes[n].strings != null ? nodes[n].strings : new String[0]; }
    public int[] typeConstructors(int n) {
        if (nodes[n].objVal instanceof Object[]) {
            Object[] arr = (Object[]) nodes[n].objVal;
            return arr[0] instanceof int[] ? (int[]) arr[0] : new int[0];
        }
        return new int[0];
    }
    public int[] typeStatements(int n) {
        if (nodes[n].objVal instanceof Object[]) {
            Object[] arr = (Object[]) nodes[n].objVal;
            return arr[1] instanceof int[] ? (int[]) arr[1] : new int[0];
        }
        return new int[0];
    }
    public void typeSetConstructorsAndStatements(int n, int[] constructorIds, int[] statementIds) {
        nodes[n].objVal = new Object[]{constructorIds, statementIds};
    }

    // === FIELD ===
    public String fieldName(int n) { return nodes[n].str0; }
    public String fieldType(int n) { return nodes[n].str1; }
    public Keyword fieldVisibility(int n) { return nodes[n].kw; }
    public int fieldValue(int n) { return nodes[n].child0; }

    // === METHOD ===
    public String methodName(int n) { return nodes[n].str0; }
    public String methodAssociatedClass(int n) { return nodes[n].str1; }
    public Keyword methodVisibility(int n) { return nodes[n].kw; }
    public boolean methodIsBuiltin(int n) { return nodes[n].bool0; }
    public int[] methodParams(int n) { return nodes[n].children != null ? nodes[n].children : new int[0]; }
    public int[] methodReturnSlots(int n) { return nodes[n].children2 != null ? nodes[n].children2 : new int[0]; }
    public int[] methodBody(int n) { return nodes[n].objVal instanceof int[] ? (int[]) nodes[n].objVal : new int[0]; }
    public void methodSetBody(int n, int[] bodyIds) { nodes[n].objVal = bodyIds; }

    // === PARAM ===
    public String paramName(int n) { return nodes[n].str0; }
    public String paramType(int n) { return nodes[n].str1; }
    public int paramDefaultValue(int n) { return nodes[n].child0; }
    public boolean paramHasDefault(int n) { return nodes[n].bool0; }
    public boolean paramTypeInferred(int n) { return nodes[n].bool1; }
    public boolean paramIsLambda(int n) { return nodes[n].bool2; }
    public boolean paramIsTupleDestructuring(int n) { return "true".equals(nodes[n].str2); }
    public String[] paramTupleElements(int n) { return nodes[n].strings != null ? nodes[n].strings : new String[0]; }

    // === CONSTRUCTOR ===
    public int[] constructorParams(int n) { return nodes[n].children != null ? nodes[n].children : new int[0]; }
    public int[] constructorBody(int n) { return nodes[n].children2 != null ? nodes[n].children2 : new int[0]; }

    // === CONSTRUCTOR_CALL ===
    public String constructorCallClass(int n) { return nodes[n].str0; }
    public int[] constructorCallArgs(int n) { return nodes[n].children != null ? nodes[n].children : new int[0]; }
    public String[] constructorCallArgNames(int n) { return nodes[n].strings != null ? nodes[n].strings : new String[0]; }

    // === POLICY ===
    public String policyName(int n) { return nodes[n].str0; }
    public String policySourceUnit(int n) { return nodes[n].str1; }
    public Keyword policyVisibility(int n) { return nodes[n].kw; }
    public int[] policyMethods(int n) { return nodes[n].children != null ? nodes[n].children : new int[0]; }
    public String[] policyComposed(int n) { return nodes[n].strings != null ? nodes[n].strings : new String[0]; }

    // === POLICY_METHOD ===
    public String policyMethodName(int n) { return nodes[n].str0; }
    public int[] policyMethodParams(int n) { return nodes[n].children != null ? nodes[n].children : new int[0]; }
    public int[] policyMethodReturnSlots(int n) { return nodes[n].children2 != null ? nodes[n].children2 : new int[0]; }

    // === BLOCK ===
    public int[] blockStmts(int n) { return nodes[n].children != null ? nodes[n].children : new int[0]; }

    // === ASSIGNMENT ===
    public int assignLeft(int n) { return nodes[n].child0; }
    public int assignRight(int n) { return nodes[n].child1; }
    public boolean assignIsDeclaration(int n) { return nodes[n].bool0; }

    // === VAR ===
    public String varName(int n) { return nodes[n].str0; }
    public String varExplicitType(int n) { return nodes[n].str1; }
    public int varValue(int n) { return nodes[n].child0; }

    // === STMT_IF ===
    public int stmtIfCondition(int n) { return nodes[n].child0; }
    public int stmtIfThen(int n) { return nodes[n].child1; }
    public int stmtIfElse(int n) { return nodes[n].child2; }

    // === EXPR_IF ===
    public int exprIfCondition(int n) { return nodes[n].child0; }
    public int exprIfThen(int n) { return nodes[n].child1; }
    public int exprIfElse(int n) { return nodes[n].child2; }

    // === FOR ===
    public String forIterator(int n) { return nodes[n].str0; }
    public int forRange(int n) { return nodes[n].child0; }
    public int forArraySource(int n) { return nodes[n].child1; }
    public int forBody(int n) { return nodes[n].child2; }

    // === RANGE ===
    public int rangeStep(int n) { return nodes[n].child0; }
    public int rangeStart(int n) { return nodes[n].child1; }
    public int rangeEnd(int n) { return nodes[n].child2; }

    // === TUPLE ===
    public int[] tupleElements(int n) { return nodes[n].children != null ? nodes[n].children : new int[0]; }

    // === RETURN_SLOT_ASSIGNMENT ===
    public int returnSlotMethodCall(int n) { return nodes[n].child0; }
    public String[] returnSlotVarNames(int n) { return nodes[n].strings != null ? nodes[n].strings : new String[0]; }

    // === SLOT_DECLARATION ===
    public String[] slotDeclNames(int n) { return nodes[n].strings != null ? nodes[n].strings : new String[0]; }

    // === SLOT_ASSIGNMENT ===
    public String slotAsmtName(int n) { return nodes[n].str0; }
    public int slotAsmtValue(int n) { return nodes[n].child0; }

    // === MULTIPLE_SLOT_ASSIGNMENT ===
    public int[] multiSlotAssignments(int n) { return nodes[n].children != null ? nodes[n].children : new int[0]; }

    // === BINARY_OP ===
    public String binaryOp(int n) { return nodes[n].str0; }
    public int binaryLeft(int n) { return nodes[n].child0; }
    public int binaryRight(int n) { return nodes[n].child1; }

    // === UNARY ===
    public String unaryOp(int n) { return nodes[n].str0; }
    public int unaryOperand(int n) { return nodes[n].child0; }

    // === TYPE_CAST ===
    public String typeCastTarget(int n) { return nodes[n].str0; }
    public int typeCastExpr(int n) { return nodes[n].child0; }

    // === METHOD_CALL ===
    public String methodCallName(int n) { return nodes[n].str0; }
    public String methodCallQualified(int n) { return nodes[n].str1; }
    public int methodCallTarget(int n) { return nodes[n].child0; }
    public boolean methodCallIsSuper(int n) { return nodes[n].bool0; }
    public boolean methodCallIsGlobal(int n) { return nodes[n].bool1; }
    public boolean methodCallIsSingleSlot(int n) { return nodes[n].bool2; }
    public int[] methodCallArgs(int n) { return nodes[n].children != null ? nodes[n].children : new int[0]; }
    public String[] methodCallSlotNames(int n) { return nodes[n].strings != null ? nodes[n].strings : new String[0]; }
    public String[] methodCallArgNames(int n) { return nodes[n].strings2 != null ? nodes[n].strings2 : new String[0]; }

    // === ARRAY ===
    public String arrayElementType(int n) { return nodes[n].str0; }
    public int[] arrayElements(int n) { return nodes[n].children != null ? nodes[n].children : new int[0]; }

    // === INDEX_ACCESS ===
    public int indexAccessArray(int n) { return nodes[n].child0; }
    public int indexAccessIndex(int n) { return nodes[n].child1; }

    // === RANGE_INDEX ===
    public int rangeIndexStep(int n) { return nodes[n].child0; }
    public int rangeIndexStart(int n) { return nodes[n].child1; }
    public int rangeIndexEnd(int n) { return nodes[n].child2; }

    // === MULTI_RANGE_INDEX ===
    public int[] multiRangeRanges(int n) { return nodes[n].children != null ? nodes[n].children : new int[0]; }

    // === EQUALITY_CHAIN ===
    public String equalityChainOp(int n) { return nodes[n].str0; }
    public boolean equalityChainIsAll(int n) { return nodes[n].bool0; }
    public int equalityChainLeft(int n) { return nodes[n].child0; }
    public int[] equalityChainArgs(int n) { return nodes[n].children != null ? nodes[n].children : new int[0]; }

    // === BOOLEAN_CHAIN ===
    public boolean booleanChainIsAll(int n) { return nodes[n].bool0; }
    public int[] booleanChainExprs(int n) { return nodes[n].children != null ? nodes[n].children : new int[0]; }

    // === SLOT ===
    public String slotName(int n) { return nodes[n].str0; }
    public String slotType(int n) { return nodes[n].str1; }

    // === LAMBDA ===
    public int lambdaBody(int n) { return nodes[n].child0; }
    public int[] lambdaParams(int n) { return nodes[n].children != null ? nodes[n].children : new int[0]; }
    public int[] lambdaReturnSlots(int n) { return nodes[n].children2 != null ? nodes[n].children2 : new int[0]; }

    // === PROPERTY_ACCESS ===
    public int propertyLeft(int n) { return nodes[n].child0; }
    public int propertyRight(int n) { return nodes[n].child1; }

    // === IDENTIFIER ===
    public String identName(int n) { return nodes[n].str0; }

    // === INT_LITERAL ===
    public long intLiteralValue(int n) { return nodes[n].longVal; }

    // === FLOAT_LITERAL ===
    public AutoStackingNumber floatLiteralValue(int n) { return (AutoStackingNumber) nodes[n].objVal; }

    // === TEXT_LITERAL ===
    public String textLiteralValue(int n) { return nodes[n].str0; }
    public boolean textLiteralIsInterpolated(int n) { return nodes[n].bool0; }

    // === BOOL_LITERAL ===
    public boolean boolLiteralValue(int n) { return nodes[n].bool0; }

    // === THIS ===
    public String thisClassName(int n) { return nodes[n].str0; }

    // Direct node data access (for building)
    public NodeData nodeData(int n) { return nodes[n]; }
}
