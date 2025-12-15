package cod.ast;

import cod.ast.nodes.*;

import cod.syntax.Keyword;

import java.util.*;
import java.math.BigDecimal;

public class ASTFactory {
    
    public static ProgramNode createProgram() {
        return new ProgramNode();
    }
    
    public static UnitNode createUnit(String name) {
        UnitNode unit = new UnitNode();
        unit.name = name;
        unit.imports = new UseNode();
        unit.types = new ArrayList<TypeNode>();
        unit.resolvedImports = new HashMap<String, ProgramNode>();
        return unit;
    }
    
    public static UseNode createUseNode(List<String> imports) {
        UseNode useNode = new UseNode();
        useNode.imports = imports;
        return useNode;
    }
    
    public static TypeNode createType(String name, Keyword visibility, String extendName) {
        TypeNode type = new TypeNode();
        type.name = name;
        type.visibility = visibility;
        type.extendName = extendName;
        type.fields = new ArrayList<FieldNode>();
        type.methods = new ArrayList<MethodNode>();
        type.statements = new ArrayList<StmtNode>();
        return type;
    }
    
    public static FieldNode createField(String name, String type, ExprNode value) {
        FieldNode field = new FieldNode();
        field.name = name;
        field.type = type;
        field.value = value;
        return field;
    }

    public static FieldNode createField(String name, String type) {
        FieldNode field = new FieldNode();
        field.name = name;
        field.type = type;
        return field;
    }
    
    public static AssignmentNode createAssignment(ExprNode target, ExprNode value) {
        AssignmentNode assignment = new AssignmentNode();
        assignment.left = target;
        assignment.right = value;
        return assignment;
    }

    public static ConstructorNode createConstructor() {
        ConstructorNode cons = new ConstructorNode();
        cons.parameters = new ArrayList<ParamNode>();
        cons.body = new ArrayList<StmtNode>();
        return cons;
    }
    
    public static MethodNode createMethod(String name, Keyword visibility, List<SlotNode> returnSlots) {
        MethodNode method = new MethodNode();
        method.name = name;
        method.visibility = visibility;
        method.returnSlots = returnSlots != null ? returnSlots : new ArrayList<SlotNode>();
        method.parameters = new ArrayList<ParamNode>();
        method.body = new ArrayList<StmtNode>();
        return method;
    }
    
public static ParamNode createParam(String name, String type, ExprNode defaultValue, boolean typeInferred) {
    ParamNode param = new ParamNode();
    param.name = name;
    param.type = type;
    if (defaultValue != null) {
        param.defaultValue = defaultValue;
        param.hasDefaultValue = true;
    }
    param.typeInferred = typeInferred;
    return param;
}
    
    public static SlotNode createSlot(String type, String name) {
        SlotNode slot = new SlotNode();
        slot.type = type;
        slot.name = name;
        return slot;
    }
    
    public static ExprNode createIdentifier(String name) {
        ExprNode node = new ExprNode();
        node.name = name;
        return node;
    }
    
    public static ExprNode createIntLiteral(int value) {
        ExprNode node = new ExprNode();
        node.value = value;
        return node;
    }
    
    public static ExprNode createLongLiteral(long value) {
    ExprNode node = new ExprNode();
    node.value = value;
    return node;
}
    
    public static ExprNode createFloatLiteral(BigDecimal value) {
    ExprNode node = new ExprNode();
    node.value = value; // Stores the precise BigDecimal object
    return node;
}
    
    public static ExprNode createStringLiteral(String value) {
        ExprNode node = new ExprNode();
        node.value = value;
        return node;
    }
    
    public static ExprNode createBoolLiteral(boolean value) {
        ExprNode node = new ExprNode();
        node.value = value;
        return node;
    }
    
    public static ExprNode createNullLiteral() {
        ExprNode node = new ExprNode();
        node.value = null;
        node.isNull = true;
        return node;
    }
    
    public static BinaryOpNode createBinaryOp(ExprNode left, String op, ExprNode right) {
        BinaryOpNode node = new BinaryOpNode();
        node.left = left;
        node.op = op;
        node.right = right;
        return node;
    }
    
    public static EqualityChainNode createEqualityChain(ExprNode left, String operator, boolean isAllChain, List<ExprNode> chainArguments) {
        EqualityChainNode chain = new EqualityChainNode();
        chain.left = left;
        chain.operator = operator;
        chain.isAllChain = isAllChain;
        chain.chainArguments = chainArguments != null ? chainArguments : new ArrayList<ExprNode>();
        return chain;
    }
    
    public static BooleanChainNode createBooleanChain(boolean isAll, List<ExprNode> expressions) {
        BooleanChainNode node = new BooleanChainNode();
        node.isAll = isAll;
        node.expressions = expressions != null ? expressions : new ArrayList<ExprNode>();
        return node;
    }
    
    public static UnaryNode createUnaryOp(String op, ExprNode operand) {
        UnaryNode node = new UnaryNode();
        node.op = op;
        node.operand = operand;
        return node;
    }
    
    public static TypeCastNode createTypeCast(String targetType, ExprNode expression) {
        TypeCastNode node = new TypeCastNode();
        node.targetType = targetType;
        node.expression = expression;
        return node;
    }
    
    public static SlotAssignmentNode createImplicitReturn(ExprNode returnExpr) {
        SlotAssignmentNode returnStmt = new SlotAssignmentNode();
        returnStmt.slotName = "_";
        returnStmt.value = returnExpr;
        return returnStmt;
    }
    
    public static MethodCallNode createMethodCall(String name, String qualifiedName) {
        MethodCallNode call = new MethodCallNode();
        call.name = name;
        call.qualifiedName = qualifiedName;
        call.arguments = new ArrayList<ExprNode>();
        call.slotNames = new ArrayList<String>();
        call.chainType = null;
        call.chainArguments = null;
        return call;
    }
    
    public static ArrayNode createArray(List<ExprNode> elements) {
        ArrayNode array = new ArrayNode();
        array.elements = elements != null ? elements : new ArrayList<ExprNode>();
        return array;
    }
    
    public static ArrayNode createArray() {
        return createArray(null);
    }
    
    public static TupleNode createTuple(List<ExprNode> elements) {
        TupleNode node = new TupleNode();
        node.elements = elements != null ? elements : new ArrayList<ExprNode>();
        return node;
    }
    
    public static IndexAccessNode createIndexAccess(ExprNode array, ExprNode index) {
        IndexAccessNode node = new IndexAccessNode();
        node.array = array;
        node.index = index;
        return node;
    }
    
    public static ExprIfNode createIfExpression(ExprNode condition, ExprNode thenExpr, ExprNode elseExpr) {
    ExprIfNode node = new ExprIfNode();
    node.condition = condition;
    node.thenExpr = thenExpr;
    node.elseExpr = elseExpr;
    return node;
}
    
    public static StmtIfNode createIfStatement(ExprNode condition) {
        StmtIfNode stmtIfNode = new StmtIfNode();
        stmtIfNode.condition = condition;
        stmtIfNode.thenBlock = new BlockNode();
        stmtIfNode.elseBlock = new BlockNode();
        return stmtIfNode;
    }
    
    public static ForNode createFor(String iterator, RangeNode range) {
        ForNode forNode = new ForNode();
        forNode.iterator = iterator;
        forNode.range = range;
        forNode.body = new BlockNode();
        return forNode;
    }
    
public static RangeNode createRange(ExprNode step, ExprNode start, ExprNode end) {
    return new RangeNode(step, start, end);
}
    
    public static BlockNode createBlock() {
        return new BlockNode();
    }
    
    public static BlockNode createBlock(List<StmtNode> statements) {
        return new BlockNode(statements);
    }
    
    public static VarNode createVar(String name, ExprNode value) {
        VarNode var = new VarNode();
        var.name = name;
        var.value = value;
        return var;
    }
    
    public static InputNode createInput(String targetType, String variableName) {
        InputNode input = new InputNode();
        input.targetType = targetType;
        input.variableName = variableName;
        return input;
    }
    
    public static OutputNode createOutput(String varName) {
        OutputNode output = new OutputNode();
        output.varName = varName;
        output.arguments = new ArrayList<ExprNode>();
        return output;
    }
    
    public static OutputNode createOutput() {
        return createOutput(null);
    }
    
    public static ExitNode createExit() {
      return new ExitNode();
    }
    
    public static ArgumentListNode createArgumentList(List<ExprNode> arguments) {
        ArgumentListNode node = new ArgumentListNode();
        node.arguments = arguments != null ? arguments : new ArrayList<ExprNode>();
        return node;
    }
    
    public static ReturnSlotAssignmentNode createReturnSlotAssignment(List<String> variableNames, MethodCallNode methodCall) {
        ReturnSlotAssignmentNode assignment = new ReturnSlotAssignmentNode();
        assignment.variableNames = variableNames;
        assignment.methodCall = methodCall;
        return assignment;
    }
    
    public static SlotDeclarationNode createSlotDeclaration(List<String> slotNames) {
        SlotDeclarationNode node = new SlotDeclarationNode();
        node.slotNames = slotNames;
        return node;
    }

    public static SlotAssignmentNode createSlotAssignment(String slotName, ExprNode value) {
        SlotAssignmentNode node = new SlotAssignmentNode();
        node.slotName = slotName;
        node.value = value;
        return node;
    }
    
    public static MultipleSlotAssignmentNode createMultipleSlotAssignment(List<SlotAssignmentNode> assignments) {
        MultipleSlotAssignmentNode node = new MultipleSlotAssignmentNode();
        node.assignments = assignments;
        return node;
    }
}