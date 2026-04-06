package cod.ir;

import cod.ast.VisitorImpl;
import cod.ast.nodes.*;

import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class SerializationVisitor implements VisitorImpl<Void> {
    private final DataOutput out;
    private final int depth;

    SerializationVisitor(DataOutput out, int depth) {
        this.out = out;
        this.depth = depth;
    }

    Void write(Base node) {
        return node.accept(this);
    }

    NodeBuilder writeNode(String nodeName) {
        return new NodeBuilder(nodeName);
    }

    final class NodeBuilder {
        private final String nodeName;
        private final List<String> fieldNames = new ArrayList<String>();
        private final List<Object> values = new ArrayList<Object>();

        NodeBuilder(String nodeName) {
            this.nodeName = nodeName;
        }

        NodeBuilder field(String name, Object value) {
            fieldNames.add(name);
            values.add(value);
            return this;
        }

        Void done() {
            try {
                IRCodec.writeNodeFields(out, nodeName, depth, fieldNames.toArray(new String[0]), values.toArray(new Object[0]));
                return null;
            } catch (IOException e) {
                throw new SerializationException(e);
            }
        }
    }

    static final class SerializationException extends RuntimeException {
        final IOException io;

        SerializationException(IOException io) {
            this.io = io;
        }
    }

    @Override
    public Void visit(Program n) {
        return writeNode("Program")
                .field("unit", n.unit)
                .field("programType", n.programType)
                .done();
    }

    @Override
    public Void visit(Unit n) {
        return writeNode("Unit")
                .field("name", n.name)
                .field("imports", n.imports)
                .field("policies", n.policies)
                .field("types", n.types)
                .field("mainClassName", n.mainClassName)
                .field("resolvedImports", n.resolvedImports)
                .done();
    }

    @Override
    public Void visit(Use n) {
        return writeNode("Use")
                .field("imports", n.imports)
                .done();
    }

    @Override
    public Void visit(Type n) {
        return writeNode("Type")
                .field("name", n.name)
                .field("visibility", n.visibility)
                .field("extendName", n.extendName)
                .field("fields", n.fields)
                .field("constructor", n.constructor)
                .field("methods", n.methods)
                .field("statements", n.statements)
                .field("constructors", n.constructors)
                .field("implementedPolicies", n.implementedPolicies)
                .field("cachedAncestorPolicies", n.cachedAncestorPolicies)
                .field("viralPoliciesValidated", Boolean.valueOf(n.viralPoliciesValidated))
                .done();
    }

    @Override
    public Void visit(Field n) {
        return writeNode("Field")
                .field("name", n.name)
                .field("type", n.type)
                .field("visibility", n.visibility)
                .field("value", n.value)
                .done();
    }

    @Override
    public Void visit(Method n) {
        return writeNode("Method")
                .field("methodName", n.methodName)
                .field("associatedClass", n.associatedClass)
                .field("visibility", n.visibility)
                .field("returnSlots", n.returnSlots)
                .field("parameters", n.parameters)
                .field("body", n.body)
                .field("isBuiltin", Boolean.valueOf(n.isBuiltin))
                .field("isPolicyMethod", Boolean.valueOf(n.isPolicyMethod))
                .done();
    }

    @Override
    public Void visit(Param n) {
        return writeNode("Param")
                .field("name", n.name)
                .field("type", n.type)
                .field("defaultValue", n.defaultValue)
                .field("hasDefaultValue", Boolean.valueOf(n.hasDefaultValue))
                .field("typeInferred", Boolean.valueOf(n.typeInferred))
                .field("isLambdaParameter", Boolean.valueOf(n.isLambdaParameter))
                .field("isTupleDestructuring", Boolean.valueOf(n.isTupleDestructuring))
                .field("tupleElements", n.tupleElements)
                .done();
    }

    @Override
    public Void visit(Constructor n) {
        return writeNode("Constructor")
                .field("parameters", n.parameters)
                .field("body", n.body)
                .done();
    }

    @Override
    public Void visit(ConstructorCall n) {
        return writeNode("ConstructorCall")
                .field("className", n.className)
                .field("arguments", n.arguments)
                .field("argNames", n.argNames)
                .done();
    }

    @Override
    public Void visit(Policy n) {
        return writeNode("Policy")
                .field("name", n.name)
                .field("visibility", n.visibility)
                .field("methods", n.methods)
                .field("sourceUnit", n.sourceUnit)
                .field("composedPolicies", n.composedPolicies)
                .done();
    }

    @Override
    public Void visit(PolicyMethod n) {
        return writeNode("PolicyMethod")
                .field("methodName", n.methodName)
                .field("parameters", n.parameters)
                .field("returnSlots", n.returnSlots)
                .done();
    }

    @Override
    public Void visit(Block n) {
        return writeNode("Block")
                .field("statements", n.statements)
                .done();
    }

    @Override
    public Void visit(Assignment n) {
        return writeNode("Assignment")
                .field("left", n.left)
                .field("right", n.right)
                .field("isDeclaration", Boolean.valueOf(n.isDeclaration))
                .done();
    }

    @Override
    public Void visit(Var n) {
        return writeNode("Var")
                .field("name", n.name)
                .field("value", n.value)
                .field("explicitType", n.explicitType)
                .done();
    }

    @Override
    public Void visit(StmtIf n) {
        return writeNode("StmtIf")
                .field("condition", n.condition)
                .field("thenBlock", n.thenBlock)
                .field("elseBlock", n.elseBlock)
                .done();
    }

    @Override
    public Void visit(ExprIf n) {
        return writeNode("ExprIf")
                .field("condition", n.condition)
                .field("thenExpr", n.thenExpr)
                .field("elseExpr", n.elseExpr)
                .done();
    }

    @Override
    public Void visit(For n) {
        return writeNode("For")
                .field("iterator", n.iterator)
                .field("range", n.range)
                .field("arraySource", n.arraySource)
                .field("body", n.body)
                .done();
    }

    @Override
    public Void visit(Skip n) {
        return writeNode("Skip").done();
    }

    @Override
    public Void visit(Break n) {
        return writeNode("Break").done();
    }

    @Override
    public Void visit(Range n) {
        return writeNode("Range")
                .field("step", n.step)
                .field("start", n.start)
                .field("end", n.end)
                .done();
    }

    @Override
    public Void visit(Exit n) {
        return writeNode("Exit").done();
    }

    @Override
    public Void visit(Tuple n) {
        return writeNode("Tuple")
                .field("elements", n.elements)
                .done();
    }

    @Override
    public Void visit(ReturnSlotAssignment n) {
        return writeNode("ReturnSlotAssignment")
                .field("variableNames", n.variableNames)
                .field("methodCall", n.methodCall)
                .field("lambda", n.lambda)
                .done();
    }

    @Override
    public Void visit(SlotDeclaration n) {
        return writeNode("SlotDeclaration")
                .field("slotNames", n.slotNames)
                .done();
    }

    @Override
    public Void visit(SlotAssignment n) {
        return writeNode("SlotAssignment")
                .field("slotName", n.slotName)
                .field("value", n.value)
                .done();
    }

    @Override
    public Void visit(MultipleSlotAssignment n) {
        return writeNode("MultipleSlotAssignment")
                .field("assignments", n.assignments)
                .done();
    }

    @Override
    public Void visit(BinaryOp n) {
        return writeNode("BinaryOp")
                .field("left", n.left)
                .field("op", n.op)
                .field("right", n.right)
                .done();
    }

    @Override
    public Void visit(Unary n) {
        return writeNode("Unary")
                .field("op", n.op)
                .field("operand", n.operand)
                .done();
    }

    @Override
    public Void visit(TypeCast n) {
        return writeNode("TypeCast")
                .field("targetType", n.targetType)
                .field("expression", n.expression)
                .done();
    }

    @Override
    public Void visit(MethodCall n) {
        return writeNode("MethodCall")
                .field("name", n.name)
                .field("qualifiedName", n.qualifiedName)
                .field("arguments", n.arguments)
                .field("slotNames", n.slotNames)
                .field("argNames", n.argNames)
                .field("isSuperCall", Boolean.valueOf(n.isSuperCall))
                .field("isGlobal", Boolean.valueOf(n.isGlobal))
                .field("target", n.target)
                .field("isSingleSlotCall", Boolean.valueOf(n.isSingleSlotCall))
                .done();
    }

    @Override
    public Void visit(cod.ast.nodes.Array n) {
        return writeNode("Array")
                .field("elements", n.elements)
                .field("elementType", n.elementType)
                .done();
    }

    @Override
    public Void visit(IndexAccess n) {
        return writeNode("IndexAccess")
                .field("array", n.array)
                .field("index", n.index)
                .done();
    }

    @Override
    public Void visit(RangeIndex n) {
        return writeNode("RangeIndex")
                .field("step", n.step)
                .field("start", n.start)
                .field("end", n.end)
                .done();
    }

    @Override
    public Void visit(MultiRangeIndex n) {
        return writeNode("MultiRangeIndex")
                .field("ranges", n.ranges)
                .done();
    }

    @Override
    public Void visit(EqualityChain n) {
        return writeNode("EqualityChain")
                .field("left", n.left)
                .field("operator", n.operator)
                .field("isAllChain", Boolean.valueOf(n.isAllChain))
                .field("chainArguments", n.chainArguments)
                .done();
    }

    @Override
    public Void visit(BooleanChain n) {
        return writeNode("BooleanChain")
                .field("isAll", Boolean.valueOf(n.isAll))
                .field("expressions", n.expressions)
                .done();
    }

    @Override
    public Void visit(Slot n) {
        return writeNode("Slot")
                .field("name", n.name)
                .field("type", n.type)
                .done();
    }

    @Override
    public Void visit(Lambda n) {
        return writeNode("Lambda")
                .field("parameters", n.parameters)
                .field("returnSlots", n.returnSlots)
                .field("body", n.body)
                .field("expressionBody", n.expressionBody)
                .field("inferParameters", Boolean.valueOf(n.inferParameters))
                .done();
    }

    @Override
    public Void visit(Identifier n) {
        return writeNode("Identifier")
                .field("name", n.name)
                .done();
    }

    @Override
    public Void visit(IntLiteral n) {
        return writeNode("IntLiteral")
                .field("value", n.value)
                .done();
    }

    @Override
    public Void visit(FloatLiteral n) {
        return writeNode("FloatLiteral")
                .field("value", n.value)
                .done();
    }

    @Override
    public Void visit(TextLiteral n) {
        return writeNode("TextLiteral")
                .field("value", n.value)
                .field("isInterpolated", Boolean.valueOf(n.isInterpolated))
                .done();
    }

    @Override
    public Void visit(BoolLiteral n) {
        return writeNode("BoolLiteral")
                .field("value", Boolean.valueOf(n.value))
                .done();
    }

    @Override
    public Void visit(NoneLiteral n) {
        return writeNode("NoneLiteral").done();
    }

    @Override
    public Void visit(This n) {
        return writeNode("This")
                .field("className", n.className)
                .done();
    }

    @Override
    public Void visit(Super n) {
        return writeNode("Super").done();
    }

    @Override
    public Void visit(ChainedComparison n) {
        return writeNode("ChainedComparison")
                .field("expressions", n.expressions)
                .field("operators", n.operators)
                .done();
    }

    @Override
    public Void visit(PropertyAccess n) {
        return writeNode("PropertyAccess")
                .field("left", n.left)
                .field("right", n.right)
                .done();
    }

    @Override
    public List<Void> visitList(List<? extends Base> nodes) {
        List<Void> results = new ArrayList<Void>();
        for (Base n : nodes) {
            results.add(visit(n));
        }
        return results;
    }

    @Override
    public void visitAll(List<? extends Base> nodes) {
        for (Base n : nodes) {
            visit(n);
        }
    }

    @Override
    public Void visit(Expr n) {
        if (n instanceof ArgumentList) {
            ArgumentList args = (ArgumentList) n;
            return writeNode("ArgumentList")
                    .field("arguments", args.arguments)
                    .done();
        }
        throw new SerializationException(new IOException("Unsupported IR expr node type: " + n.getClass().getName()));
    }

    @Override
    public Void visit(Base n) {
        throw new SerializationException(new IOException("Unsupported IR node type: " + n.getClass().getName()));
    }
}
