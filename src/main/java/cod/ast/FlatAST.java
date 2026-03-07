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
        Object legacyNode; // dual-write bridge
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
    public Object getLegacyNode(int n) { return n >= 0 && n < size ? nodes[n].legacyNode : null; }
    public void setLegacyNode(int n, Object obj) { if (n >= 0 && n < size) nodes[n].legacyNode = obj; }
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

    // === MUTATION HELPERS ===
    private static int[] appendInt(int[] arr, int val) {
        if (arr == null) arr = new int[0];
        int[] r = new int[arr.length + 1];
        System.arraycopy(arr, 0, r, 0, arr.length);
        r[arr.length] = val;
        return r;
    }
    private static String[] appendStr(String[] arr, String val) {
        if (arr == null) arr = new String[0];
        String[] r = new String[arr.length + 1];
        System.arraycopy(arr, 0, r, 0, arr.length);
        r[arr.length] = val;
        return r;
    }

    // PROGRAM
    public void programSetUnit(int n, int unitId) {
        nodes[n].child0 = unitId;
        if (nodes[n].legacyNode instanceof cod.ast.nodes.ProgramNode && unitId >= 0 && unitId < size && nodes[unitId].legacyNode instanceof cod.ast.nodes.UnitNode) {
            ((cod.ast.nodes.ProgramNode)nodes[n].legacyNode).unit = (cod.ast.nodes.UnitNode)nodes[unitId].legacyNode;
        }
    }
    public void programSetType(int n, String t) {
        nodes[n].str1 = t;
        if (nodes[n].legacyNode instanceof cod.ast.nodes.ProgramNode) {
            cod.ast.nodes.ProgramNode p = (cod.ast.nodes.ProgramNode)nodes[n].legacyNode;
            if ("MODULE".equals(t)) p.programType = cod.parser.MainParser.ProgramType.MODULE;
            else if ("SCRIPT".equals(t)) p.programType = cod.parser.MainParser.ProgramType.SCRIPT;
            else if ("METHOD_SCRIPT".equals(t)) p.programType = cod.parser.MainParser.ProgramType.METHOD_SCRIPT;
        }
    }
    public String programType(int n)                      { return nodes[n].str1; }
    public String programGetType(int n)                  { return nodes[n].str1; }

    // UNIT
    public void unitSetImports(int n, int importsId) {
        nodes[n].child0 = importsId;
        if (nodes[n].legacyNode instanceof cod.ast.nodes.UnitNode && importsId >= 0 && importsId < size && nodes[importsId].legacyNode instanceof cod.ast.nodes.UseNode) {
            ((cod.ast.nodes.UnitNode)nodes[n].legacyNode).imports = (cod.ast.nodes.UseNode)nodes[importsId].legacyNode;
        }
    }
    public void unitAddType(int n, int typeId) {
        nodes[n].children = appendInt(nodes[n].children, typeId);
        if (nodes[n].legacyNode instanceof cod.ast.nodes.UnitNode && typeId >= 0 && typeId < size && nodes[typeId].legacyNode instanceof cod.ast.nodes.TypeNode) {
            ((cod.ast.nodes.UnitNode)nodes[n].legacyNode).types.add((cod.ast.nodes.TypeNode)nodes[typeId].legacyNode);
        }
    }
    public void unitAddPolicy(int n, int policyId) {
        nodes[n].children2 = appendInt(nodes[n].children2, policyId);
        if (nodes[n].legacyNode instanceof cod.ast.nodes.UnitNode && policyId >= 0 && policyId < size && nodes[policyId].legacyNode instanceof cod.ast.nodes.PolicyNode) {
            ((cod.ast.nodes.UnitNode)nodes[n].legacyNode).policies.add((cod.ast.nodes.PolicyNode)nodes[policyId].legacyNode);
        }
    }
    public void unitSetMainClass(int n, String mc)       { nodes[n].str2 = mc; }
    public void unitSetResolvedImportsMap(int n, Map<String,Integer> m) { nodes[n].objVal = m; }

    // USE
    public void useAddImport(int n, String imp)          { nodes[n].strings = appendStr(nodes[n].strings, imp); }
    public void useSetImports(int n, String[] imps)      { nodes[n].strings = imps; }

    // TYPE
    public void typeAddField(int n, int fieldId) {
        nodes[n].children = appendInt(nodes[n].children, fieldId);
        if (n >= 0 && n < size && fieldId >= 0 && fieldId < size && nodes[n].legacyNode instanceof cod.ast.nodes.TypeNode && nodes[fieldId].legacyNode instanceof cod.ast.nodes.FieldNode)
            ((cod.ast.nodes.TypeNode)nodes[n].legacyNode).fields.add((cod.ast.nodes.FieldNode)nodes[fieldId].legacyNode);
    }
    public void typeAddMethod(int n, int methodId) {
        nodes[n].children2 = appendInt(nodes[n].children2, methodId);
        if (n >= 0 && n < size && methodId >= 0 && methodId < size && nodes[n].legacyNode instanceof cod.ast.nodes.TypeNode && nodes[methodId].legacyNode instanceof cod.ast.nodes.MethodNode)
            ((cod.ast.nodes.TypeNode)nodes[n].legacyNode).methods.add((cod.ast.nodes.MethodNode)nodes[methodId].legacyNode);
    }
    public void typeAddConstructor(int n, int ctorId) {
        Object[] arr = nodes[n].objVal instanceof Object[] ? (Object[]) nodes[n].objVal : new Object[]{new int[0], new int[0]};
        arr[0] = appendInt((int[]) arr[0], ctorId);
        nodes[n].objVal = arr;
        if (n >= 0 && n < size && ctorId >= 0 && ctorId < size && nodes[n].legacyNode instanceof cod.ast.nodes.TypeNode && nodes[ctorId].legacyNode instanceof cod.ast.nodes.ConstructorNode)
            ((cod.ast.nodes.TypeNode)nodes[n].legacyNode).constructors.add((cod.ast.nodes.ConstructorNode)nodes[ctorId].legacyNode);
    }
    public void typeAddStatement(int n, int stmtId) {
        Object[] arr = nodes[n].objVal instanceof Object[] ? (Object[]) nodes[n].objVal : new Object[]{new int[0], new int[0]};
        arr[1] = appendInt((int[]) arr[1], stmtId);
        nodes[n].objVal = arr;
        if (n >= 0 && n < size && stmtId >= 0 && stmtId < size
                && nodes[n].legacyNode instanceof cod.ast.nodes.TypeNode
                && nodes[stmtId].legacyNode instanceof cod.ast.nodes.StmtNode)
            ((cod.ast.nodes.TypeNode)nodes[n].legacyNode).statements.add(
                    (cod.ast.nodes.StmtNode)nodes[stmtId].legacyNode);
    }
    public void typeAddImplementedPolicy(int n, String p) { nodes[n].strings = appendStr(nodes[n].strings, p); }
    public void typeSetExtend(int n, String ext)         { nodes[n].str2 = ext; }
    public void typeSetVisibility(int n, Keyword v)      { nodes[n].kw = v; }

    // FIELD
    public void fieldSetVisibility(int n, Keyword v)     { nodes[n].kw = v; }
    public void fieldSetValue(int n, int valueId) {
        nodes[n].child0 = valueId;
        if (n >= 0 && n < size && valueId >= 0 && valueId < size
                && nodes[n].legacyNode instanceof cod.ast.nodes.FieldNode
                && nodes[valueId].legacyNode instanceof cod.ast.nodes.ExprNode)
            ((cod.ast.nodes.FieldNode)nodes[n].legacyNode).value =
                    (cod.ast.nodes.ExprNode)nodes[valueId].legacyNode;
    }

    // METHOD
    public void methodAddParam(int n, int paramId) {
        nodes[n].children = appendInt(nodes[n].children, paramId);
        if (n >= 0 && n < size && paramId >= 0 && paramId < size
                && nodes[n].legacyNode instanceof cod.ast.nodes.MethodNode
                && nodes[paramId].legacyNode instanceof cod.ast.nodes.ParamNode)
            ((cod.ast.nodes.MethodNode)nodes[n].legacyNode).parameters.add(
                    (cod.ast.nodes.ParamNode)nodes[paramId].legacyNode);
    }
    public void methodAddReturnSlot(int n, int slotId) {
        nodes[n].children2 = appendInt(nodes[n].children2, slotId);
        if (n >= 0 && n < size && slotId >= 0 && slotId < size
                && nodes[n].legacyNode instanceof cod.ast.nodes.MethodNode
                && nodes[slotId].legacyNode instanceof cod.ast.nodes.SlotNode)
            ((cod.ast.nodes.MethodNode)nodes[n].legacyNode).returnSlots.add(
                    (cod.ast.nodes.SlotNode)nodes[slotId].legacyNode);
    }
    public void methodAddBodyStmt(int n, int stmtId) {
        int[] body = nodes[n].objVal instanceof int[] ? (int[]) nodes[n].objVal : new int[0];
        nodes[n].objVal = appendInt(body, stmtId);
        if (n >= 0 && n < size && stmtId >= 0 && stmtId < size
                && nodes[n].legacyNode instanceof cod.ast.nodes.MethodNode
                && nodes[stmtId].legacyNode instanceof cod.ast.nodes.StmtNode)
            ((cod.ast.nodes.MethodNode)nodes[n].legacyNode).body.add(
                    (cod.ast.nodes.StmtNode)nodes[stmtId].legacyNode);
    }
    public void methodSetAssociatedClass(int n, String c){ nodes[n].str1 = c; }
    public void methodSetIsBuiltin(int n, boolean v)     { nodes[n].bool0 = v; }
    public boolean methodIsPolicyMethod(int n)            { return nodes[n].bool1; }
    public void methodSetIsPolicyMethod(int n, boolean v){ nodes[n].bool1 = v; }
    public void methodSetReturnSlots(int n, int[] ids) {
        nodes[n].children2 = ids;
        if (n >= 0 && n < size && nodes[n].legacyNode instanceof cod.ast.nodes.MethodNode) {
            cod.ast.nodes.MethodNode mn = (cod.ast.nodes.MethodNode) nodes[n].legacyNode;
            mn.returnSlots.clear();
            if (ids != null) {
                for (int slotId : ids) {
                    if (slotId >= 0 && slotId < size && nodes[slotId].legacyNode instanceof cod.ast.nodes.SlotNode)
                        mn.returnSlots.add((cod.ast.nodes.SlotNode) nodes[slotId].legacyNode);
                }
            }
        }
    }
    public void methodSetParams(int n, int[] ids)        { nodes[n].children = ids; }

    // CONSTRUCTOR
    public void constructorAddParam(int n, int paramId)  { nodes[n].children = appendInt(nodes[n].children, paramId); }
    public void constructorAddBodyStmt(int n, int stmtId) {
        nodes[n].children2 = appendInt(nodes[n].children2, stmtId);
        if (n >= 0 && n < size && stmtId >= 0 && stmtId < size && nodes[n].legacyNode instanceof cod.ast.nodes.ConstructorNode && nodes[stmtId].legacyNode instanceof cod.ast.nodes.StmtNode)
            ((cod.ast.nodes.ConstructorNode)nodes[n].legacyNode).body.add((cod.ast.nodes.StmtNode)nodes[stmtId].legacyNode);
    }
    public void constructorSetParams(int n, int[] ids)   { nodes[n].children = ids; }
    public void constructorSetBody(int n, int[] ids)     { nodes[n].children2 = ids; }

    // CONSTRUCTOR_CALL
    public void constructorCallSetArgs(int n, int[] argIds, String[] argNames) {
        nodes[n].children = argIds;
        nodes[n].strings = argNames;
    }
    public void constructorCallAddArg(int n, int argId, String argName) {
        nodes[n].children = appendInt(nodes[n].children, argId);
        nodes[n].strings = appendStr(nodes[n].strings, argName != null ? argName : "");
    }

    // POLICY
    public void policyAddMethod(int n, int methodId)     { nodes[n].children = appendInt(nodes[n].children, methodId); }
    public void policyAddComposed(int n, String comp)    { nodes[n].strings = appendStr(nodes[n].strings, comp); }
    public void policySetSourceUnit(int n, String su)    { nodes[n].str1 = su; }
    public void policySetComposed(int n, String[] arr)   { nodes[n].strings = arr; }

    // POLICY_METHOD
    public void policyMethodAddParam(int n, int paramId) { nodes[n].children = appendInt(nodes[n].children, paramId); }
    public void policyMethodAddReturnSlot(int n, int slotId) { nodes[n].children2 = appendInt(nodes[n].children2, slotId); }

    // BLOCK
    public void blockAddStmt(int n, int stmtId) {
        nodes[n].children = appendInt(nodes[n].children, stmtId);
        if (n >= 0 && n < size && stmtId >= 0 && stmtId < size
                && nodes[n].legacyNode instanceof cod.ast.nodes.BlockNode
                && nodes[stmtId].legacyNode instanceof cod.ast.nodes.StmtNode)
            ((cod.ast.nodes.BlockNode)nodes[n].legacyNode).statements.add(
                    (cod.ast.nodes.StmtNode)nodes[stmtId].legacyNode);
    }
    public void blockSetStmts(int n, int[] ids)          { nodes[n].children = ids; }

    // STMT_IF
    public void stmtIfSetCondition(int n, int condId) {
        nodes[n].child0 = condId;
        if (n >= 0 && n < size && condId >= 0 && condId < size
                && nodes[n].legacyNode instanceof cod.ast.nodes.StmtIfNode
                && nodes[condId].legacyNode instanceof cod.ast.nodes.ExprNode)
            ((cod.ast.nodes.StmtIfNode)nodes[n].legacyNode).condition =
                    (cod.ast.nodes.ExprNode)nodes[condId].legacyNode;
    }
    public void stmtIfSetThen(int n, int thenId) {
        nodes[n].child1 = thenId;
        if (n >= 0 && n < size && thenId >= 0 && thenId < size
                && nodes[n].legacyNode instanceof cod.ast.nodes.StmtIfNode
                && nodes[thenId].legacyNode instanceof cod.ast.nodes.BlockNode)
            ((cod.ast.nodes.StmtIfNode)nodes[n].legacyNode).thenBlock =
                    (cod.ast.nodes.BlockNode)nodes[thenId].legacyNode;
    }
    public void stmtIfSetElse(int n, int elseId) {
        nodes[n].child2 = elseId;
        if (n >= 0 && n < size && elseId >= 0 && elseId < size
                && nodes[n].legacyNode instanceof cod.ast.nodes.StmtIfNode
                && nodes[elseId].legacyNode instanceof cod.ast.nodes.BlockNode)
            ((cod.ast.nodes.StmtIfNode)nodes[n].legacyNode).elseBlock =
                    (cod.ast.nodes.BlockNode)nodes[elseId].legacyNode;
    }

    // FOR
    public void forSetBody(int n, int bodyId) {
        nodes[n].child2 = bodyId;
        if (n >= 0 && n < size && bodyId >= 0 && bodyId < size
                && nodes[n].legacyNode instanceof cod.ast.nodes.ForNode
                && nodes[bodyId].legacyNode instanceof cod.ast.nodes.BlockNode)
            ((cod.ast.nodes.ForNode)nodes[n].legacyNode).body =
                    (cod.ast.nodes.BlockNode)nodes[bodyId].legacyNode;
    }
    public void forSetRange(int n, int rangeId) {
        nodes[n].child0 = rangeId;
        if (n >= 0 && n < size && rangeId >= 0 && rangeId < size
                && nodes[n].legacyNode instanceof cod.ast.nodes.ForNode
                && nodes[rangeId].legacyNode instanceof cod.ast.nodes.RangeNode)
            ((cod.ast.nodes.ForNode)nodes[n].legacyNode).range =
                    (cod.ast.nodes.RangeNode)nodes[rangeId].legacyNode;
    }
    public void forSetArraySource(int n, int srcId) {
        nodes[n].child1 = srcId;
        if (n >= 0 && n < size && srcId >= 0 && srcId < size
                && nodes[n].legacyNode instanceof cod.ast.nodes.ForNode
                && nodes[srcId].legacyNode instanceof cod.ast.nodes.ExprNode)
            ((cod.ast.nodes.ForNode)nodes[n].legacyNode).arraySource =
                    (cod.ast.nodes.ExprNode)nodes[srcId].legacyNode;
    }

    // METHOD_CALL
    public void methodCallSetTarget(int n, int targetId) {
        nodes[n].child0 = targetId;
        if (n >= 0 && n < size && targetId >= 0 && targetId < size
                && nodes[n].legacyNode instanceof cod.ast.nodes.MethodCallNode
                && nodes[targetId].legacyNode instanceof cod.ast.nodes.ExprNode)
            ((cod.ast.nodes.MethodCallNode)nodes[n].legacyNode).target =
                    (cod.ast.nodes.ExprNode)nodes[targetId].legacyNode;
    }
    public void methodCallSetSlotNames(int n, String[] names) {
        nodes[n].strings = names;
        if (n >= 0 && n < size && nodes[n].legacyNode instanceof cod.ast.nodes.MethodCallNode) {
            cod.ast.nodes.MethodCallNode mc = (cod.ast.nodes.MethodCallNode)nodes[n].legacyNode;
            mc.slotNames.clear();
            if (names != null) for (String s : names) mc.slotNames.add(s);
        }
    }
    public void methodCallAddSlotName(int n, String name) {
        nodes[n].strings = appendStr(nodes[n].strings, name);
        if (n >= 0 && n < size && nodes[n].legacyNode instanceof cod.ast.nodes.MethodCallNode)
            ((cod.ast.nodes.MethodCallNode)nodes[n].legacyNode).slotNames.add(name != null ? name : "");
    }
    public void methodCallSetArgNames(int n, String[] names) { nodes[n].strings2 = names; }
    public void methodCallSetArgs(int n, int[] argIds)   { nodes[n].children = argIds; }
    public void methodCallAddArg(int n, int argId, String argName) {
        nodes[n].children = appendInt(nodes[n].children, argId);
        nodes[n].strings2 = appendStr(nodes[n].strings2, argName != null ? argName : "");
        if (n >= 0 && n < size && argId >= 0 && argId < size
                && nodes[n].legacyNode instanceof cod.ast.nodes.MethodCallNode
                && nodes[argId].legacyNode instanceof cod.ast.nodes.ExprNode) {
            cod.ast.nodes.MethodCallNode mc = (cod.ast.nodes.MethodCallNode)nodes[n].legacyNode;
            mc.arguments.add((cod.ast.nodes.ExprNode)nodes[argId].legacyNode);
            mc.argNames.add(argName != null ? argName : "");
        }
    }
    public void methodCallSetIsSuper(int n, boolean v) {
        nodes[n].bool0 = v;
        if (n >= 0 && n < size && nodes[n].legacyNode instanceof cod.ast.nodes.MethodCallNode)
            ((cod.ast.nodes.MethodCallNode)nodes[n].legacyNode).isSuperCall = v;
    }
    public void methodCallSetIsGlobal(int n, boolean v) {
        nodes[n].bool1 = v;
        if (n >= 0 && n < size && nodes[n].legacyNode instanceof cod.ast.nodes.MethodCallNode)
            ((cod.ast.nodes.MethodCallNode)nodes[n].legacyNode).isGlobal = v;
    }
    public void methodCallSetIsSingleSlot(int n, boolean v) {
        nodes[n].bool2 = v;
        if (n >= 0 && n < size && nodes[n].legacyNode instanceof cod.ast.nodes.MethodCallNode)
            ((cod.ast.nodes.MethodCallNode)nodes[n].legacyNode).isSingleSlotCall = v;
    }
    public void methodCallSetName(int n, String name) {
        nodes[n].str0 = name;
        if (n >= 0 && n < size && nodes[n].legacyNode instanceof cod.ast.nodes.MethodCallNode)
            ((cod.ast.nodes.MethodCallNode)nodes[n].legacyNode).name = name;
    }
    public void methodCallSetQualified(int n, String q) {
        nodes[n].str1 = q;
        if (n >= 0 && n < size && nodes[n].legacyNode instanceof cod.ast.nodes.MethodCallNode)
            ((cod.ast.nodes.MethodCallNode)nodes[n].legacyNode).qualifiedName = q;
    }

    // LAMBDA
    public void lambdaSetBody(int n, int bodyId)         { nodes[n].child0 = bodyId; }
    public void lambdaSetParams(int n, int[] paramIds)   { nodes[n].children = paramIds; }
    public void lambdaSetReturnSlots(int n, int[] ids)   { nodes[n].children2 = ids; }

    // VAR
    public void varSetExplicitType(int n, String t) {
        nodes[n].str1 = t;
        if (n >= 0 && n < size && nodes[n].legacyNode instanceof cod.ast.nodes.VarNode)
            ((cod.ast.nodes.VarNode)nodes[n].legacyNode).explicitType = t;
    }
    public void varSetValue(int n, int valueId) {
        nodes[n].child0 = valueId;
        if (n >= 0 && n < size && valueId >= 0 && valueId < size
                && nodes[n].legacyNode instanceof cod.ast.nodes.VarNode
                && nodes[valueId].legacyNode instanceof cod.ast.nodes.ExprNode)
            ((cod.ast.nodes.VarNode)nodes[n].legacyNode).value =
                    (cod.ast.nodes.ExprNode)nodes[valueId].legacyNode;
    }

    // PARAM
    public void paramSetTupleDestructuring(int n, boolean v, String[] elements) {
        nodes[n].str2 = v ? "true" : "false";
        nodes[n].strings = elements;
    }
    public void paramSetIsLambda(int n, boolean v)       { nodes[n].bool2 = v; }
    public void paramSetType(int n, String t)            { nodes[n].str1 = t; }

    // ARRAY
    public void arraySetElementType(int n, String t)     { nodes[n].str0 = t; }
    public void arraySetElements(int n, int[] ids)       { nodes[n].children = ids; }

    // RETURN_SLOT_ASSIGNMENT
    public void returnSlotSetMethodCall(int n, int mcId) { nodes[n].child0 = mcId; }
    public void returnSlotSetVarNames(int n, String[] names) { nodes[n].strings = names; }

    // EQUALITY_CHAIN
    public void equalityChainSetLeft(int n, int leftId)  { nodes[n].child0 = leftId; }
    public void equalityChainSetArgs(int n, int[] ids)   { nodes[n].children = ids; }

    // BOOLEAN_CHAIN
    public void booleanChainSetExprs(int n, int[] ids)   { nodes[n].children = ids; }

    // MULTIPLE_SLOT_ASSIGNMENT
    public void multiSlotSetAssignments(int n, int[] ids){ nodes[n].children = ids; }
}
