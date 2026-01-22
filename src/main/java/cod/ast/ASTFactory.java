package cod.ast;

import cod.ast.nodes.*;
import cod.syntax.Keyword;
import cod.lexer.Token;
import java.util.*;
import java.math.BigDecimal;

public class ASTFactory {
    // Helper method to create source span from token
    private static SourceSpan span(Token token) {
        return token != null ? new SourceSpan(token) : null;
    }

    // Merge source spans for composite nodes
    private static SourceSpan mergeSpans(SourceSpan... spans) {
        if (spans == null || spans.length == 0) return null;
        
        SourceSpan result = spans[0];
        for (int i = 1; i < spans.length; i++) {
            if (spans[i] != null) {
                result = SourceSpan.merge(result, spans[i]);
            }
        }
        return result;
    }
    
    public static ProgramNode createProgram() {
        return new ProgramNode();
    }

    public static UnitNode createUnit(String name, Token unitToken) {
        UnitNode unit = new UnitNode();
        unit.name = name;
        unit.imports = new UseNode();
        unit.types = new ArrayList<TypeNode>();
        unit.resolvedImports = new HashMap<String, ProgramNode>();
        unit.mainClassName = null;
        if (unitToken != null) {
            unit.setSourceSpan(span(unitToken));
        }
        return unit;
    }
    
    public static UnitNode createUnit(String name, String mainClassName, Token unitToken) {
        UnitNode unit = new UnitNode();
        unit.name = name;
        unit.imports = new UseNode();
        unit.types = new ArrayList<TypeNode>();
        unit.resolvedImports = new HashMap<String, ProgramNode>();
        unit.mainClassName = mainClassName;
        if (unitToken != null) {
            unit.setSourceSpan(span(unitToken));
        }
        return unit;
    }

    public static UseNode createUseNode(List<String> imports, Token useToken) {
        UseNode useNode = new UseNode();
        useNode.imports = imports;
        if (useToken != null) {
            useNode.setSourceSpan(span(useToken));
        }
        return useNode;
    }

    public static TypeNode createType(String name, Keyword visibility, String extendName, Token nameToken) {
        TypeNode type = new TypeNode();
        type.name = name;
        type.visibility = visibility;
        type.extendName = extendName;
        type.fields = new ArrayList<FieldNode>();
        type.methods = new ArrayList<MethodNode>();
        type.statements = new ArrayList<StmtNode>();
        type.constructors = new ArrayList<ConstructorNode>();
        type.implementedPolicies = new ArrayList<String>();
        if (nameToken != null) {
            type.setSourceSpan(span(nameToken));
        }
        return type;
    }
    
    public static PolicyNode createPolicy(String name, Keyword visibility, Token nameToken) {
        PolicyNode node = new PolicyNode();
        node.name = name;
        node.visibility = visibility;
        node.methods = new ArrayList<PolicyMethodNode>();
        node.composedPolicies = new ArrayList<String>();
        if (nameToken != null) {
            node.setSourceSpan(span(nameToken));
        }
        return node;
    }

    public static PolicyMethodNode createPolicyMethod(String name, Token nameToken) {
        PolicyMethodNode method = new PolicyMethodNode();
        method.methodName = name;
        method.parameters = new ArrayList<ParamNode>();
        method.returnSlots = new ArrayList<SlotNode>();
        if (nameToken != null) {
            method.setSourceSpan(span(nameToken));
        }
        return method;
    }

    public static FieldNode createField(String name, String type, ExprNode value, Token nameToken) {
        FieldNode field = new FieldNode();
        field.name = name;
        field.type = type;
        field.value = value;
        if (nameToken != null) {
            field.setSourceSpan(span(nameToken));
        }
        return field;
    }
    
    public static FieldNode createField(String name, String type, Token nameToken) {
        return createField(name, type, null, nameToken);
    }

    public static AssignmentNode createAssignment(ExprNode target, ExprNode value, boolean isDeclaration, Token assignToken) {
        AssignmentNode assignment = new AssignmentNode();
        assignment.left = target;
        assignment.right = value;
        assignment.isDeclaration = isDeclaration;
        assignment.setSourceSpan(mergeSpans(
            target != null ? target.getSourceSpan() : null,
            span(assignToken),
            value != null ? value.getSourceSpan() : null
        ));
        return assignment;
    }

    public static ConstructorCallNode createConstructorCall(String className, List<ExprNode> arguments, Token nameToken) {
        ConstructorCallNode call = new ConstructorCallNode();
        call.className = className;
        call.arguments = arguments != null ? arguments : new ArrayList<ExprNode>();
        call.argNames = new ArrayList<String>();
        if (arguments != null) {
            for (int i = 0; i < arguments.size(); i++) {
                call.argNames.add(null);
            }
        }
        if (nameToken != null) {
            call.setSourceSpan(span(nameToken));
        }
        return call;
    }

    public static ConstructorNode createConstructor(List<ParamNode> parameters, List<StmtNode> body, Token thisToken) {
        ConstructorNode cons = new ConstructorNode();
        cons.parameters = parameters != null ? parameters : new ArrayList<ParamNode>();
        cons.body = body != null ? body : new ArrayList<StmtNode>();
        if (thisToken != null) {
            cons.setSourceSpan(span(thisToken));
        }
        return cons;
    }

    public static MethodNode createMethod(
        String name, Keyword visibility, List<SlotNode> returnSlots, Token nameToken) {
        MethodNode node = new MethodNode();
        node.methodName = name;
        node.visibility = visibility;
        node.returnSlots = returnSlots != null ? returnSlots : new ArrayList<SlotNode>();
        node.parameters = new ArrayList<ParamNode>();
        node.body = new ArrayList<StmtNode>();
        node.isBuiltin = false;
        if (nameToken != null) {
            node.setSourceSpan(span(nameToken));
        }
        return node;
    }

    public static ParamNode createParam(
        String name, String type, ExprNode defaultValue, boolean typeInferred, Token nameToken) {
        ParamNode param = new ParamNode();
        param.name = name;
        param.type = type;
        if (defaultValue != null) {
            param.defaultValue = defaultValue;
            param.hasDefaultValue = true;
        }
        param.typeInferred = typeInferred;
        if (nameToken != null) {
            param.setSourceSpan(span(nameToken));
        }
        return param;
    }

    public static SlotNode createSlot(String type, String name, Token nameToken) {
        SlotNode slot = new SlotNode();
        slot.type = type;
        slot.name = name;
        if (nameToken != null) {
            slot.setSourceSpan(span(nameToken));
        }
        return slot;
    }

    public static ExprNode createIdentifier(String name, Token token) {
        ExprNode node = new ExprNode();
        node.name = name;
        if ("this".equals(name)) {
            node.isThis = true;
        }
        if ("super".equals(name)) {
            node.isSuper = true;
        }
        if (token != null) {
            node.setSourceSpan(span(token));
        }
        return node;
    }

    public static ExprNode createThisExpression(String className, Token thisToken) {
        ExprNode node = new ExprNode();
        node.name = "this";
        node.isThis = true;
        node.thisClassName = className;
        if (thisToken != null) {
            node.setSourceSpan(span(thisToken));
        }
        return node;
    }

    public static ExprNode createSuperExpression(Token superToken) {
        ExprNode node = new ExprNode();
        node.name = "super";
        node.isSuper = true;
        if (superToken != null) {
            node.setSourceSpan(span(superToken));
        }
        return node;
    }

    public static ExprNode createPropertyAccess(ExprNode base, String propertyName, Token dotToken) {
        ExprNode node = new ExprNode();
        if (base.isThis) {
            node.isThis = true;
            node.thisClassName = base.thisClassName;
            node.isPropertyAccess = true;
            node.propertyName = propertyName;
        } else if (base.isSuper) {
            node.isSuper = true;
            node.isPropertyAccess = true;
            node.propertyName = propertyName;
        } else {
            node.name = base.name + "." + propertyName;
            node.isPropertyAccess = true;
            node.propertyName = propertyName;
        }
        node.setSourceSpan(mergeSpans(
            base.getSourceSpan(),
            span(dotToken)
        ));
        return node;
    }

    public static ExprNode createIntLiteral(int value, Token token) {
        ExprNode node = new ExprNode();
        node.value = value;
        if (token != null) {
            node.setSourceSpan(span(token));
        }
        return node;
    }

    public static ExprNode createLongLiteral(long value, Token token) {
        ExprNode node = new ExprNode();
        node.value = value;
        if (token != null) {
            node.setSourceSpan(span(token));
        }
        return node;
    }

    public static ExprNode createFloatLiteral(BigDecimal value, Token token) {
        ExprNode node = new ExprNode();
        node.value = value;
        if (token != null) {
            node.setSourceSpan(span(token));
        }
        return node;
    }

    public static ExprNode createStringLiteral(String value, Token token) {
        ExprNode node = new ExprNode();
        node.value = value;
        if (token != null) {
            node.setSourceSpan(span(token));
        }
        return node;
    }

    public static ExprNode createBoolLiteral(boolean value, Token token) {
        ExprNode node = new ExprNode();
        node.value = value;
        if (token != null) {
            node.setSourceSpan(span(token));
        }
        return node;
    }

    public static ExprNode createNoneLiteral(Token token) {
        ExprNode node = new ExprNode();
        node.value = null;
        node.isNone = true;
        if (token != null) {
            node.setSourceSpan(span(token));
        }
        return node;
    }

    public static BinaryOpNode createBinaryOp(ExprNode left, String op, ExprNode right, Token opToken) {
        BinaryOpNode node = new BinaryOpNode();
        node.left = left;
        node.op = op;
        node.right = right;
        node.setSourceSpan(mergeSpans(
            left != null ? left.getSourceSpan() : null,
            span(opToken),
            right != null ? right.getSourceSpan() : null
        ));
        return node;
    }

    public static EqualityChainNode createEqualityChain(
        ExprNode left, String operator, boolean isAllChain, List<ExprNode> chainArguments,
        Token leftToken, Token opToken, Token chainToken) {
        EqualityChainNode chain = new EqualityChainNode();
        chain.left = left;
        chain.operator = operator;
        chain.isAllChain = isAllChain;
        chain.chainArguments = chainArguments != null ? chainArguments : new ArrayList<ExprNode>();
        
        // Build source span from all components
        List<SourceSpan> spans = new ArrayList<SourceSpan>();
        if (left != null) spans.add(left.getSourceSpan());
        if (leftToken != null) spans.add(span(leftToken));
        if (opToken != null) spans.add(span(opToken));
        if (chainToken != null) spans.add(span(chainToken));
        for (ExprNode arg : chainArguments) {
            if (arg != null) spans.add(arg.getSourceSpan());
        }
        
        chain.setSourceSpan(mergeSpans(spans.toArray(new SourceSpan[0])));
        return chain;
    }

    public static BooleanChainNode createBooleanChain(boolean isAll, List<ExprNode> expressions, Token keywordToken) {
        BooleanChainNode node = new BooleanChainNode();
        node.isAll = isAll;
        node.expressions = expressions != null ? expressions : new ArrayList<ExprNode>();
        
        // Build source span from keyword and all expressions
        List<SourceSpan> spans = new ArrayList<SourceSpan>();
        if (keywordToken != null) spans.add(span(keywordToken));
        for (ExprNode expr : node.expressions) {
            if (expr != null) spans.add(expr.getSourceSpan());
        }
        
        node.setSourceSpan(mergeSpans(spans.toArray(new SourceSpan[0])));
        return node;
    }

    public static UnaryNode createUnaryOp(String op, ExprNode operand, Token opToken) {
        UnaryNode node = new UnaryNode();
        node.op = op;
        node.operand = operand;
        node.setSourceSpan(mergeSpans(
            span(opToken),
            operand != null ? operand.getSourceSpan() : null
        ));
        return node;
    }

    public static TypeCastNode createTypeCast(String targetType, ExprNode expression, Token lparenToken) {
        TypeCastNode node = new TypeCastNode();
        node.targetType = targetType;
        node.expression = expression;
        node.setSourceSpan(mergeSpans(
            span(lparenToken),
            expression != null ? expression.getSourceSpan() : null
        ));
        return node;
    }

    public static SlotAssignmentNode createImplicitReturn(ExprNode returnExpr, Token returnToken) {
        SlotAssignmentNode returnStmt = new SlotAssignmentNode();
        returnStmt.slotName = "_";
        returnStmt.value = returnExpr;
        returnStmt.setSourceSpan(mergeSpans(
            returnToken != null ? span(returnToken) : null,
            returnExpr != null ? returnExpr.getSourceSpan() : null
        ));
        return returnStmt;
    }

    public static MethodCallNode createMethodCall(String name, String qualifiedName, Token nameToken) {
        MethodCallNode call = new MethodCallNode();
        call.name = name;
        call.qualifiedName = qualifiedName;
        call.arguments = new ArrayList<ExprNode>();
        call.slotNames = new ArrayList<String>();
        call.argNames = new ArrayList<String>();
        if (nameToken != null) {
            call.setSourceSpan(span(nameToken));
        }
        return call;
    }

    public static ArrayNode createArray(List<ExprNode> elements, Token lbracketToken) {
        ArrayNode array = new ArrayNode();
        array.elements = elements != null ? elements : new ArrayList<ExprNode>();
        
        // Build source span from bracket and all elements
        List<SourceSpan> spans = new ArrayList<SourceSpan>();
        if (lbracketToken != null) spans.add(span(lbracketToken));
        for (ExprNode elem : array.elements) {
            if (elem != null) spans.add(elem.getSourceSpan());
        }
        
        array.setSourceSpan(mergeSpans(spans.toArray(new SourceSpan[0])));
        return array;
    }

    public static TupleNode createTuple(List<ExprNode> elements, Token lparenToken) {
        TupleNode node = new TupleNode();
        node.elements = elements != null ? elements : new ArrayList<ExprNode>();
        
        // Build source span from paren and all elements
        List<SourceSpan> spans = new ArrayList<SourceSpan>();
        if (lparenToken != null) spans.add(span(lparenToken));
        for (ExprNode elem : node.elements) {
            if (elem != null) spans.add(elem.getSourceSpan());
        }
        
        node.setSourceSpan(mergeSpans(spans.toArray(new SourceSpan[0])));
        return node;
    }

    public static IndexAccessNode createIndexAccess(ExprNode array, ExprNode index, Token lbracketToken) {
        IndexAccessNode node = new IndexAccessNode();
        node.array = array;
        node.index = index;
        node.setSourceSpan(mergeSpans(
            array != null ? array.getSourceSpan() : null,
            span(lbracketToken),
            index != null ? index.getSourceSpan() : null
        ));
        return node;
    }

    public static RangeIndexNode createRangeIndex(ExprNode step, ExprNode start, ExprNode end, Token byToken, Token toToken) {
        RangeIndexNode node = new RangeIndexNode(step, start, end);
        
        List<SourceSpan> spans = new ArrayList<SourceSpan>();
        if (byToken != null) spans.add(span(byToken));
        if (start != null) spans.add(start.getSourceSpan());
        if (toToken != null) spans.add(span(toToken));
        if (end != null) spans.add(end.getSourceSpan());
        if (step != null) spans.add(step.getSourceSpan());
        
        node.setSourceSpan(mergeSpans(spans.toArray(new SourceSpan[0])));
        return node;
    }

    public static MultiRangeIndexNode createMultiRangeIndex(List<RangeIndexNode> ranges, Token firstLbracketToken) {
        MultiRangeIndexNode node = new MultiRangeIndexNode(ranges);
        
        List<SourceSpan> spans = new ArrayList<SourceSpan>();
        if (firstLbracketToken != null) spans.add(span(firstLbracketToken));
        for (RangeIndexNode range : ranges) {
            if (range != null) spans.add(range.getSourceSpan());
        }
        
        node.setSourceSpan(mergeSpans(spans.toArray(new SourceSpan[0])));
        return node;
    }

    public static ExprIfNode createIfExpression(
        ExprNode condition, ExprNode thenExpr, ExprNode elseExpr,
        Token ifToken, Token elseToken) {
        ExprIfNode node = new ExprIfNode();
        node.condition = condition;
        node.thenExpr = thenExpr;
        node.elseExpr = elseExpr;
        node.setSourceSpan(mergeSpans(
            span(ifToken),
            condition != null ? condition.getSourceSpan() : null,
            thenExpr != null ? thenExpr.getSourceSpan() : null,
            span(elseToken),
            elseExpr != null ? elseExpr.getSourceSpan() : null
        ));
        return node;
    }

    public static StmtIfNode createIfStatement(ExprNode condition, Token ifToken) {
        StmtIfNode stmtIfNode = new StmtIfNode();
        stmtIfNode.condition = condition;
        stmtIfNode.thenBlock = new BlockNode();
        stmtIfNode.elseBlock = new BlockNode();
        if (ifToken != null) {
            stmtIfNode.setSourceSpan(span(ifToken));
        }
        return stmtIfNode;
    }
    
    public static ForNode createFor(String iterator, RangeNode range, Token forToken, Token iteratorToken) {
        ForNode forNode = new ForNode();
        forNode.iterator = iterator;
        forNode.range = range;
        forNode.arraySource = null;
        forNode.body = new BlockNode();
        forNode.setSourceSpan(mergeSpans(
            span(forToken),
            span(iteratorToken),
            range != null ? range.getSourceSpan() : null
        ));
        return forNode;
    }

    public static ForNode createFor(String iterator, ExprNode arraySource, Token forToken, Token iteratorToken) {
        ForNode forNode = new ForNode();
        forNode.iterator = iterator;
        forNode.range = null;
        forNode.arraySource = arraySource;
        forNode.body = new BlockNode();
        forNode.setSourceSpan(mergeSpans(
            span(forToken),
            span(iteratorToken),
            arraySource != null ? arraySource.getSourceSpan() : null
        ));
        return forNode;
    }

    public static RangeNode createRange(ExprNode step, ExprNode start, ExprNode end, Token byToken, Token toToken) {
        RangeNode node = new RangeNode(step, start, end);
        
        List<SourceSpan> spans = new ArrayList<SourceSpan>();
        if (byToken != null) spans.add(span(byToken));
        if (start != null) spans.add(start.getSourceSpan());
        if (toToken != null) spans.add(span(toToken));
        if (end != null) spans.add(end.getSourceSpan());
        if (step != null) spans.add(step.getSourceSpan());
        
        node.setSourceSpan(mergeSpans(spans.toArray(new SourceSpan[0])));
        return node;
    }

    public static BlockNode createBlock(Token lbraceToken) {
        BlockNode block = new BlockNode();
        if (lbraceToken != null) {
            block.setSourceSpan(span(lbraceToken));
        }
        return block;
    }

    public static BlockNode createBlock(List<StmtNode> statements, Token lbraceToken) {
        BlockNode block = new BlockNode(statements);
        if (lbraceToken != null) {
            block.setSourceSpan(span(lbraceToken));
        }
        return block;
    }

    public static VarNode createVar(String name, ExprNode value, Token nameToken) {
        VarNode var = new VarNode();
        var.name = name;
        var.value = value;
        if (nameToken != null) {
            var.setSourceSpan(span(nameToken));
        }
        return var;
    }

    public static ExitNode createExit(Token exitToken) {
        ExitNode exit = new ExitNode();
        if (exitToken != null) {
            exit.setSourceSpan(span(exitToken));
        }
        return exit;
    }

    public static ArgumentListNode createArgumentList(List<ExprNode> arguments, Token lparenToken) {
        ArgumentListNode node = new ArgumentListNode();
        node.arguments = arguments != null ? arguments : new ArrayList<ExprNode>();
        
        List<SourceSpan> spans = new ArrayList<SourceSpan>();
        if (lparenToken != null) spans.add(span(lparenToken));
        for (ExprNode arg : node.arguments) {
            if (arg != null) spans.add(arg.getSourceSpan());
        }
        
        node.setSourceSpan(mergeSpans(spans.toArray(new SourceSpan[0])));
        return node;
    }

    public static SkipNode createSkipStatement(Token skipToken) {
        SkipNode skip = new SkipNode();
        if (skipToken != null) {
            skip.setSourceSpan(span(skipToken));
        }
        return skip;
    }
    
    public static BreakNode createBreakStatement(Token breakToken) {
        BreakNode brk = new BreakNode();
        if (breakToken != null) {
            brk.setSourceSpan(span(breakToken));
        }
        return brk;
    }

    public static ReturnSlotAssignmentNode createReturnSlotAssignment(
        List<String> variableNames, MethodCallNode methodCall, Token assignToken) {
        ReturnSlotAssignmentNode assignment = new ReturnSlotAssignmentNode();
        assignment.variableNames = variableNames;
        assignment.methodCall = methodCall;
        assignment.setSourceSpan(mergeSpans(
            span(assignToken),
            methodCall != null ? methodCall.getSourceSpan() : null
        ));
        return assignment;
    }

    public static SlotDeclarationNode createSlotDeclaration(List<String> slotNames, Token lbracketToken) {
        SlotDeclarationNode node = new SlotDeclarationNode();
        node.slotNames = slotNames;
        if (lbracketToken != null) {
            node.setSourceSpan(span(lbracketToken));
        }
        return node;
    }

    public static SlotAssignmentNode createSlotAssignment(String slotName, ExprNode value, Token colonToken) {
        SlotAssignmentNode node = new SlotAssignmentNode();
        node.slotName = slotName;
        node.value = value;
        node.setSourceSpan(mergeSpans(
            colonToken != null ? span(colonToken) : null,
            value != null ? value.getSourceSpan() : null
        ));
        return node;
    }

    public static MultipleSlotAssignmentNode createMultipleSlotAssignment(
        List<SlotAssignmentNode> assignments, Token tildeArrowToken) {
        MultipleSlotAssignmentNode node = new MultipleSlotAssignmentNode();
        node.assignments = assignments;
        
        List<SourceSpan> spans = new ArrayList<SourceSpan>();
        if (tildeArrowToken != null) spans.add(span(tildeArrowToken));
        for (SlotAssignmentNode assignment : assignments) {
            if (assignment != null) spans.add(assignment.getSourceSpan());
        }
        
        node.setSourceSpan(mergeSpans(spans.toArray(new SourceSpan[0])));
        return node;
    }
    
    public static String getConstructorSignature(ConstructorNode constructor) {
        if (constructor == null || constructor.parameters == null) {
            return "()";
        }
        
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < constructor.parameters.size(); i++) {
            if (i > 0) sb.append(", ");
            ParamNode p = constructor.parameters.get(i);
            sb.append(p.name).append(": ").append(p.type);
            if (p.hasDefaultValue) {
                sb.append(" = default");
            }
        }
        sb.append(")");
        return sb.toString();
    }
}