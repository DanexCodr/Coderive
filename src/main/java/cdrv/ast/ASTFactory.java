package cdrv.ast;

import cdrv.ast.nodes.*;
import java.util.*;

public class ASTFactory {
    
    // Program creation
    public static ProgramNode createProgram() {
        return new ProgramNode();
    }
    
    public static UnitNode createUnit(String name) {
        UnitNode unit = new UnitNode();
        unit.name = name;
        unit.imports = new GetNode();
        unit.types = new ArrayList<TypeNode>();
        unit.resolvedImports = new HashMap<String, ProgramNode>();
        return unit;
    }
    
    public static GetNode createGetNode(List<String> imports) {
        GetNode getNode = new GetNode();
        getNode.imports = imports;
        return getNode;
    }
    
    // Type creation
    public static TypeNode createType(String name, String visibility, String extendName) {
        TypeNode type = new TypeNode();
        type.name = name;
        type.visibility = visibility;
        type.extendName = extendName;
        type.fields = new ArrayList<FieldNode>();
        type.methods = new ArrayList<MethodNode>();
        type.statements = new ArrayList<StatementNode>();
        return type;
    }
    
    // Field creation
    public static FieldNode createField(String name, String type, ExprNode value) {
        FieldNode field = new FieldNode();
        field.name = name;
        field.type = type;
        field.value = value;
        return field;
    }
    
    public static FieldNode createField(String name, String type) {
        return createField(name, type, null);
    }

    /**
     * Renamed method to resolve ambiguity.
     * This is called by the parser for field declarations like 'public string name'.
     */
    public static FieldNode createFieldWithVisibility(String name, String type, String visibility) {
        FieldNode field = new FieldNode();
        field.name = name;
        field.type = type;
        field.visibility = visibility;
        return field;
    }
    
public static AssignmentNode createAssignment(ExprNode target, ExprNode value) {
    AssignmentNode assignment = new AssignmentNode();
    assignment.left = target;
    assignment.right = value;
    return assignment;
}

    // Constructor creation
    public static ConstructorNode createConstructor() {
        ConstructorNode cons = new ConstructorNode();
        cons.parameters = new ArrayList<ParamNode>();
        cons.body = new ArrayList<StatementNode>();
        return cons;
    }
    
    // Method creation
    public static MethodNode createMethod(String name, String visibility, List<String> returnSlots) {
        MethodNode method = new MethodNode();
        method.name = name;
        method.visibility = visibility;
        method.returnSlots = new ArrayList<SlotNode>();
        method.parameters = new ArrayList<ParamNode>();
        method.body = new ArrayList<StatementNode>();
        
        if (returnSlots != null) {
            for (String slotName : returnSlots) {
                method.returnSlots.add(createSlot(slotName));
            }
        }
        
        return method;
    }
    
    // Parameter creation
    public static ParamNode createParam(String name, String type) {
        ParamNode param = new ParamNode();
        param.name = name;
        param.type = type;
        return param;
    }
    
    // Slot creation
    public static SlotNode createSlot(String name) {
        SlotNode slot = new SlotNode();
        slot.name = name;
        return slot;
    }
    
    // Expression creation
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
    
    public static ExprNode createFloatLiteral(float value) {
        ExprNode node = new ExprNode();
        node.value = value;
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
    
    public static BinaryOpNode createBinaryOp(ExprNode left, String op, ExprNode right) {
        BinaryOpNode node = new BinaryOpNode();
        node.left = left;
        node.op = op;
        node.right = right;
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
    
    // Method call creation
    public static MethodCallNode createMethodCall(String name, String qualifiedName) {
        MethodCallNode call = new MethodCallNode();
        call.name = name;
        call.qualifiedName = qualifiedName;
        call.arguments = new ArrayList<ExprNode>();
        call.slotNames = new ArrayList<String>();
        return call;
    }
    
    // Array creation
    public static ArrayNode createArray(List<ExprNode> elements) {
        ArrayNode array = new ArrayNode();
        array.elements = elements != null ? elements : new ArrayList<ExprNode>();
        return array;
    }
    
    public static ArrayNode createArray() {
        return createArray(null);
    }
    
    // Index access creation
    public static IndexAccessNode createIndexAccess(ExprNode array, ExprNode index) {
        IndexAccessNode node = new IndexAccessNode();
        node.array = array;
        node.index = index;
        return node;
    }
    
    // Control structures
    public static IfNode createIf(ExprNode condition) {
        IfNode ifNode = new IfNode();
        ifNode.condition = condition;
        ifNode.thenBlock = new BlockNode();
        ifNode.elseBlock = new BlockNode();
        return ifNode;
    }
    
    public static ForNode createFor(String iterator, RangeNode range) {
        ForNode forNode = new ForNode();
        forNode.iterator = iterator;
        forNode.range = range;
        forNode.body = new BlockNode();
        return forNode;
    }
    
    public static RangeNode createRange(ExprNode step, ExprNode start, ExprNode end) {
    RangeNode range = new RangeNode();
    range.step = step;
    range.start = start;
    range.end = end;
    return range;
}

    
    // Block creation
    public static BlockNode createBlock() {
        return new BlockNode();
    }
    
    public static BlockNode createBlock(List<StatementNode> statements) {
        return new BlockNode(statements);
    }
    
    // Variable and input/output
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
    
    // Return slot assignment
    public static ReturnSlotAssignmentNode createReturnSlotAssignment(List<String> variableNames, MethodCallNode methodCall) {
        ReturnSlotAssignmentNode assignment = new ReturnSlotAssignmentNode();
        assignment.variableNames = variableNames;
        assignment.methodCall = methodCall;
        return assignment;
    }
    
    // --- NEW SLOT NODES ---

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
}