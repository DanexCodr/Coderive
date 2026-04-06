package cod.ir;

import cod.ast.VisitorImpl;
import cod.ast.nodes.*;
import cod.math.AutoStackingNumber;
import cod.parser.MainParser.ProgramType;
import cod.syntax.Keyword;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class IRCodec {
    static final int MAGIC = 0xAC0D1EB1;
    static final int VERSION = 1;

    private static final int MAX_DEPTH = 512;
    private static final int MAX_STRING_BYTES = 4 * 1024 * 1024;
    private static final int MAX_COLLECTION_SIZE = 1_000_000;
    private static final int MAX_FIELDS = 1_000;

    private static final byte TAG_NULL = 0;
    private static final byte TAG_STRING = 1;
    private static final byte TAG_BOOL = 2;
    private static final byte TAG_INT = 3;
    private static final byte TAG_LONG = 4;
    private static final byte TAG_DOUBLE = 5;
    private static final byte TAG_ENUM = 6;
    private static final byte TAG_LIST = 7;
    private static final byte TAG_MAP = 8;
    private static final byte TAG_NODE = 9;
    private static final byte TAG_AUTO_STACKING = 10;

    private static final String NODE_PACKAGE_PREFIX = "cod.ast.nodes.";

    private IRCodec() {}

    static void writeHeader(DataOutput out) throws IOException {
        out.writeInt(MAGIC);
        out.writeInt(VERSION);
    }

    static void readHeader(DataInput in) throws IOException {
        int magic = in.readInt();
        if (magic != MAGIC) {
            throw new IOException("Invalid IR magic number: expected 0x" + Integer.toHexString(MAGIC) +
                    ", got 0x" + Integer.toHexString(magic));
        }

        int version = in.readInt();
        if (version != VERSION) {
            throw new IOException("Unsupported IR version: expected " + VERSION + ", got " + version);
        }
    }

    static void writeValue(DataOutput out, Object value, int depth) throws IOException {
        ensureDepth(depth);

        if (value == null) {
            out.writeByte(TAG_NULL);
            return;
        }

        if (value instanceof String) {
            out.writeByte(TAG_STRING);
            writeString(out, (String) value);
            return;
        }

        if (value instanceof Boolean) {
            out.writeByte(TAG_BOOL);
            out.writeBoolean(((Boolean) value).booleanValue());
            return;
        }

        if (value instanceof Integer) {
            out.writeByte(TAG_INT);
            out.writeInt(((Integer) value).intValue());
            return;
        }

        if (value instanceof Long) {
            out.writeByte(TAG_LONG);
            out.writeLong(((Long) value).longValue());
            return;
        }

        if (value instanceof Float) {
            out.writeByte(TAG_DOUBLE);
            out.writeDouble(((Float) value).doubleValue());
            return;
        }

        if (value instanceof Double) {
            out.writeByte(TAG_DOUBLE);
            out.writeDouble(((Double) value).doubleValue());
            return;
        }

        if (value instanceof Enum) {
            out.writeByte(TAG_ENUM);
            Enum<?> e = (Enum<?>) value;
            writeString(out, e.getDeclaringClass().getName());
            writeString(out, e.name());
            return;
        }

        if (value instanceof AutoStackingNumber) {
            out.writeByte(TAG_AUTO_STACKING);
            writeString(out, value.toString());
            return;
        }

        if (value instanceof List) {
            out.writeByte(TAG_LIST);
            List<?> list = (List<?>) value;
            ensureCollectionSize(list.size(), "list");
            out.writeInt(list.size());
            for (Object item : list) {
                writeValue(out, item, depth + 1);
            }
            return;
        }

        if (value instanceof Map) {
            out.writeByte(TAG_MAP);
            Map<?, ?> map = (Map<?, ?>) value;
            ensureCollectionSize(map.size(), "map");
            out.writeInt(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object key = entry.getKey();
                if (!(key instanceof String)) {
                    throw new IOException("Only String map keys are supported in IR, got: " +
                            (key == null ? "null" : key.getClass().getName()));
                }
                writeString(out, (String) key);
                writeValue(out, entry.getValue(), depth + 1);
            }
            return;
        }

        if (value instanceof Base) {
            writeNode(out, (Base) value, depth + 1);
            return;
        }

        throw new IOException("Unsupported IR value type: " + value.getClass().getName());
    }

    static Object readValue(DataInput in, int depth) throws IOException {
        ensureDepth(depth);
        byte tag = in.readByte();

        switch (tag) {
            case TAG_NULL:
                return null;
            case TAG_STRING:
                return readString(in);
            case TAG_BOOL:
                return Boolean.valueOf(in.readBoolean());
            case TAG_INT:
                return Integer.valueOf(in.readInt());
            case TAG_LONG:
                return Long.valueOf(in.readLong());
            case TAG_DOUBLE:
                return Double.valueOf(in.readDouble());
            case TAG_ENUM:
                return readEnum(in);
            case TAG_LIST:
                return readList(in, depth + 1);
            case TAG_MAP:
                return readMap(in, depth + 1);
            case TAG_NODE:
                return readNode(in, depth + 1);
            case TAG_AUTO_STACKING:
                return AutoStackingNumber.valueOf(readString(in));
            default:
                throw new IOException("Unknown IR tag: " + tag);
        }
    }

    private static void writeNode(DataOutput out, Base node, int depth) throws IOException {
        out.writeByte(TAG_NODE);

        if (node instanceof ValueExpr) {
            writeNodeFields(out, "ValueExpr", depth, new String[]{"value"}, new Object[]{((ValueExpr) node).getValue()});
            return;
        }

        try {
            node.accept(new NodeWriteVisitor(out, depth));
        } catch (NodeWriteException e) {
            throw e.io;
        }
    }

    private static Base readNode(DataInput in, int depth) throws IOException {
        String className = readString(in);
        if (!className.startsWith(NODE_PACKAGE_PREFIX)) {
            throw new IOException("Invalid IR node class: " + className);
        }

        int fieldCount = in.readInt();
        if (fieldCount < 0 || fieldCount > MAX_FIELDS) {
            throw new IOException("Invalid IR field count: " + fieldCount + " for " + className);
        }

        Map<String, Object> values = new LinkedHashMap<String, Object>();
        for (int i = 0; i < fieldCount; i++) {
            String fieldName = readString(in);
            Object value = readValue(in, depth + 1);
            values.put(fieldName, value);
        }

        String simpleClassName = className.substring(NODE_PACKAGE_PREFIX.length());
        Base node = instantiateNode(simpleClassName, values);
        applyNodeFields(node, values);
        return node;
    }

    private static Base instantiateNode(String nodeName, Map<String, Object> values) throws IOException {
        if ("Identifier".equals(nodeName)) {
            return new Identifier((String) values.get("name"));
        }
        if ("TextLiteral".equals(nodeName)) {
            String value = (String) values.get("value");
            boolean interpolated = asBoolean(values.get("isInterpolated"));
            return new TextLiteral(value, interpolated);
        }
        if ("BoolLiteral".equals(nodeName)) {
            return new BoolLiteral(asBoolean(values.get("value")));
        }
        if ("IntLiteral".equals(nodeName)) {
            return new IntLiteral((AutoStackingNumber) values.get("value"));
        }
        if ("FloatLiteral".equals(nodeName)) {
            return new FloatLiteral((AutoStackingNumber) values.get("value"));
        }
        if ("RangeIndex".equals(nodeName)) {
            return new RangeIndex((Expr) values.get("step"), (Expr) values.get("start"), (Expr) values.get("end"));
        }
        if ("MultiRangeIndex".equals(nodeName)) {
            return new MultiRangeIndex(IRCodec.<RangeIndex>castList(values.get("ranges")));
        }
        if ("ChainedComparison".equals(nodeName)) {
            return new ChainedComparison(
                    IRCodec.<Expr>castList(values.get("expressions")),
                    IRCodec.<String>castList(values.get("operators"))
            );
        }
        if ("ValueExpr".equals(nodeName)) {
            return new ValueExpr(values.get("value"));
        }
        if ("This".equals(nodeName)) {
            String className = (String) values.get("className");
            return className == null ? new This() : new This(className);
        }

        if ("Program".equals(nodeName)) return new Program();
        if ("Unit".equals(nodeName)) return new Unit();
        if ("Use".equals(nodeName)) return new Use();
        if ("Type".equals(nodeName)) return new Type();
        if ("Field".equals(nodeName)) return new Field();
        if ("Method".equals(nodeName)) return new Method();
        if ("Param".equals(nodeName)) return new Param();
        if ("Constructor".equals(nodeName)) return new Constructor();
        if ("ConstructorCall".equals(nodeName)) return new ConstructorCall();
        if ("Policy".equals(nodeName)) return new Policy();
        if ("PolicyMethod".equals(nodeName)) return new PolicyMethod();
        if ("Block".equals(nodeName)) return new Block();
        if ("Assignment".equals(nodeName)) return new Assignment();
        if ("Var".equals(nodeName)) return new Var();
        if ("StmtIf".equals(nodeName)) return new StmtIf();
        if ("ExprIf".equals(nodeName)) return new ExprIf();
        if ("For".equals(nodeName)) return new For();
        if ("Skip".equals(nodeName)) return new Skip();
        if ("Break".equals(nodeName)) return new Break();
        if ("Range".equals(nodeName)) return new Range();
        if ("Exit".equals(nodeName)) return new Exit();
        if ("Tuple".equals(nodeName)) return new Tuple();
        if ("ReturnSlotAssignment".equals(nodeName)) return new ReturnSlotAssignment();
        if ("SlotDeclaration".equals(nodeName)) return new SlotDeclaration();
        if ("SlotAssignment".equals(nodeName)) return new SlotAssignment();
        if ("MultipleSlotAssignment".equals(nodeName)) return new MultipleSlotAssignment();
        if ("BinaryOp".equals(nodeName)) return new BinaryOp();
        if ("Unary".equals(nodeName)) return new Unary();
        if ("TypeCast".equals(nodeName)) return new TypeCast();
        if ("MethodCall".equals(nodeName)) return new MethodCall();
        if ("Array".equals(nodeName)) return new Array();
        if ("IndexAccess".equals(nodeName)) return new IndexAccess();
        if ("EqualityChain".equals(nodeName)) return new EqualityChain();
        if ("BooleanChain".equals(nodeName)) return new BooleanChain();
        if ("Slot".equals(nodeName)) return new Slot();
        if ("Lambda".equals(nodeName)) return new Lambda();
        if ("NoneLiteral".equals(nodeName)) return new NoneLiteral();
        if ("Super".equals(nodeName)) return new Super();
        if ("PropertyAccess".equals(nodeName)) return new PropertyAccess();
        if ("ArgumentList".equals(nodeName)) return new ArgumentList();

        throw new IOException("Unknown IR node class: " + NODE_PACKAGE_PREFIX + nodeName);
    }

    private static void applyNodeFields(Base node, Map<String, Object> values) {
        if (node instanceof Program) {
            Program n = (Program) node;
            if (values.containsKey("unit")) n.unit = (Unit) values.get("unit");
            if (values.containsKey("programType")) n.programType = (ProgramType) values.get("programType");
            return;
        }

        if (node instanceof Unit) {
            Unit n = (Unit) node;
            if (values.containsKey("name")) n.name = (String) values.get("name");
            if (values.containsKey("imports")) n.imports = (Use) values.get("imports");
            if (values.containsKey("policies")) n.policies = castList(values.get("policies"));
            if (values.containsKey("types")) n.types = castList(values.get("types"));
            if (values.containsKey("mainClassName")) n.mainClassName = (String) values.get("mainClassName");
            if (values.containsKey("resolvedImports")) n.resolvedImports = castMap(values.get("resolvedImports"));
            return;
        }

        if (node instanceof Use) {
            Use n = (Use) node;
            if (values.containsKey("imports")) n.imports = castList(values.get("imports"));
            return;
        }

        if (node instanceof Type) {
            Type n = (Type) node;
            if (values.containsKey("name")) n.name = (String) values.get("name");
            if (values.containsKey("visibility")) n.visibility = (Keyword) values.get("visibility");
            if (values.containsKey("extendName")) n.extendName = (String) values.get("extendName");
            if (values.containsKey("fields")) n.fields = castList(values.get("fields"));
            if (values.containsKey("constructor")) n.constructor = (Constructor) values.get("constructor");
            if (values.containsKey("methods")) n.methods = castList(values.get("methods"));
            if (values.containsKey("statements")) n.statements = castList(values.get("statements"));
            if (values.containsKey("constructors")) n.constructors = castList(values.get("constructors"));
            if (values.containsKey("implementedPolicies")) n.implementedPolicies = castList(values.get("implementedPolicies"));
            if (values.containsKey("cachedAncestorPolicies")) n.cachedAncestorPolicies = castList(values.get("cachedAncestorPolicies"));
            if (values.containsKey("viralPoliciesValidated")) n.viralPoliciesValidated = asBoolean(values.get("viralPoliciesValidated"));
            return;
        }

        if (node instanceof Field) {
            Field n = (Field) node;
            if (values.containsKey("name")) n.name = (String) values.get("name");
            if (values.containsKey("type")) n.type = (String) values.get("type");
            if (values.containsKey("visibility")) n.visibility = (Keyword) values.get("visibility");
            if (values.containsKey("value")) n.value = (Expr) values.get("value");
            return;
        }

        if (node instanceof Method) {
            Method n = (Method) node;
            if (values.containsKey("methodName")) n.methodName = (String) values.get("methodName");
            if (values.containsKey("associatedClass")) n.associatedClass = (String) values.get("associatedClass");
            if (values.containsKey("visibility")) n.visibility = (Keyword) values.get("visibility");
            if (values.containsKey("returnSlots")) n.returnSlots = castList(values.get("returnSlots"));
            if (values.containsKey("parameters")) n.parameters = castList(values.get("parameters"));
            if (values.containsKey("body")) n.body = castList(values.get("body"));
            if (values.containsKey("isBuiltin")) n.isBuiltin = asBoolean(values.get("isBuiltin"));
            if (values.containsKey("isPolicyMethod")) n.isPolicyMethod = asBoolean(values.get("isPolicyMethod"));
            return;
        }

        if (node instanceof Param) {
            Param n = (Param) node;
            if (values.containsKey("name")) n.name = (String) values.get("name");
            if (values.containsKey("type")) n.type = (String) values.get("type");
            if (values.containsKey("defaultValue")) n.defaultValue = (Expr) values.get("defaultValue");
            if (values.containsKey("hasDefaultValue")) n.hasDefaultValue = asBoolean(values.get("hasDefaultValue"));
            if (values.containsKey("typeInferred")) n.typeInferred = asBoolean(values.get("typeInferred"));
            if (values.containsKey("isLambdaParameter")) n.isLambdaParameter = asBoolean(values.get("isLambdaParameter"));
            if (values.containsKey("isTupleDestructuring")) n.isTupleDestructuring = asBoolean(values.get("isTupleDestructuring"));
            if (values.containsKey("tupleElements")) n.tupleElements = castList(values.get("tupleElements"));
            return;
        }

        if (node instanceof Constructor) {
            Constructor n = (Constructor) node;
            if (values.containsKey("parameters")) n.parameters = castList(values.get("parameters"));
            if (values.containsKey("body")) n.body = castList(values.get("body"));
            return;
        }

        if (node instanceof ConstructorCall) {
            ConstructorCall n = (ConstructorCall) node;
            if (values.containsKey("className")) n.className = (String) values.get("className");
            if (values.containsKey("arguments")) n.arguments = castList(values.get("arguments"));
            if (values.containsKey("argNames")) n.argNames = castList(values.get("argNames"));
            return;
        }

        if (node instanceof Policy) {
            Policy n = (Policy) node;
            if (values.containsKey("name")) n.name = (String) values.get("name");
            if (values.containsKey("visibility")) n.visibility = (Keyword) values.get("visibility");
            if (values.containsKey("methods")) n.methods = castList(values.get("methods"));
            if (values.containsKey("sourceUnit")) n.sourceUnit = (String) values.get("sourceUnit");
            if (values.containsKey("composedPolicies")) n.composedPolicies = castList(values.get("composedPolicies"));
            return;
        }

        if (node instanceof PolicyMethod) {
            PolicyMethod n = (PolicyMethod) node;
            if (values.containsKey("methodName")) n.methodName = (String) values.get("methodName");
            if (values.containsKey("parameters")) n.parameters = castList(values.get("parameters"));
            if (values.containsKey("returnSlots")) n.returnSlots = castList(values.get("returnSlots"));
            return;
        }

        if (node instanceof Block) {
            Block n = (Block) node;
            if (values.containsKey("statements")) n.statements = castList(values.get("statements"));
            return;
        }

        if (node instanceof Assignment) {
            Assignment n = (Assignment) node;
            if (values.containsKey("left")) n.left = (Expr) values.get("left");
            if (values.containsKey("right")) n.right = (Expr) values.get("right");
            if (values.containsKey("isDeclaration")) n.isDeclaration = asBoolean(values.get("isDeclaration"));
            return;
        }

        if (node instanceof Var) {
            Var n = (Var) node;
            if (values.containsKey("name")) n.name = (String) values.get("name");
            if (values.containsKey("value")) n.value = (Expr) values.get("value");
            if (values.containsKey("explicitType")) n.explicitType = (String) values.get("explicitType");
            return;
        }

        if (node instanceof StmtIf) {
            StmtIf n = (StmtIf) node;
            if (values.containsKey("condition")) n.condition = (Expr) values.get("condition");
            if (values.containsKey("thenBlock")) n.thenBlock = (Block) values.get("thenBlock");
            if (values.containsKey("elseBlock")) n.elseBlock = (Block) values.get("elseBlock");
            return;
        }

        if (node instanceof ExprIf) {
            ExprIf n = (ExprIf) node;
            if (values.containsKey("condition")) n.condition = (Expr) values.get("condition");
            if (values.containsKey("thenExpr")) n.thenExpr = (Expr) values.get("thenExpr");
            if (values.containsKey("elseExpr")) n.elseExpr = (Expr) values.get("elseExpr");
            return;
        }

        if (node instanceof For) {
            For n = (For) node;
            if (values.containsKey("iterator")) n.iterator = (String) values.get("iterator");
            if (values.containsKey("range")) n.range = (Range) values.get("range");
            if (values.containsKey("arraySource")) n.arraySource = (Expr) values.get("arraySource");
            if (values.containsKey("body")) n.body = (Block) values.get("body");
            return;
        }

        if (node instanceof Range) {
            Range n = (Range) node;
            if (values.containsKey("step")) n.step = (Expr) values.get("step");
            if (values.containsKey("start")) n.start = (Expr) values.get("start");
            if (values.containsKey("end")) n.end = (Expr) values.get("end");
            return;
        }

        if (node instanceof Tuple) {
            Tuple n = (Tuple) node;
            if (values.containsKey("elements")) n.elements = castList(values.get("elements"));
            return;
        }

        if (node instanceof ReturnSlotAssignment) {
            ReturnSlotAssignment n = (ReturnSlotAssignment) node;
            if (values.containsKey("variableNames")) n.variableNames = castList(values.get("variableNames"));
            if (values.containsKey("methodCall")) n.methodCall = (MethodCall) values.get("methodCall");
            if (values.containsKey("lambda")) n.lambda = (Lambda) values.get("lambda");
            return;
        }

        if (node instanceof SlotDeclaration) {
            SlotDeclaration n = (SlotDeclaration) node;
            if (values.containsKey("slotNames")) n.slotNames = castList(values.get("slotNames"));
            return;
        }

        if (node instanceof SlotAssignment) {
            SlotAssignment n = (SlotAssignment) node;
            if (values.containsKey("slotName")) n.slotName = (String) values.get("slotName");
            if (values.containsKey("value")) n.value = (Expr) values.get("value");
            return;
        }

        if (node instanceof MultipleSlotAssignment) {
            MultipleSlotAssignment n = (MultipleSlotAssignment) node;
            if (values.containsKey("assignments")) n.assignments = castList(values.get("assignments"));
            return;
        }

        if (node instanceof BinaryOp) {
            BinaryOp n = (BinaryOp) node;
            if (values.containsKey("left")) n.left = (Expr) values.get("left");
            if (values.containsKey("op")) n.op = (String) values.get("op");
            if (values.containsKey("right")) n.right = (Expr) values.get("right");
            return;
        }

        if (node instanceof Unary) {
            Unary n = (Unary) node;
            if (values.containsKey("op")) n.op = (String) values.get("op");
            if (values.containsKey("operand")) n.operand = (Expr) values.get("operand");
            return;
        }

        if (node instanceof TypeCast) {
            TypeCast n = (TypeCast) node;
            if (values.containsKey("targetType")) n.targetType = (String) values.get("targetType");
            if (values.containsKey("expression")) n.expression = (Expr) values.get("expression");
            return;
        }

        if (node instanceof MethodCall) {
            MethodCall n = (MethodCall) node;
            if (values.containsKey("name")) n.name = (String) values.get("name");
            if (values.containsKey("qualifiedName")) n.qualifiedName = (String) values.get("qualifiedName");
            if (values.containsKey("arguments")) n.arguments = castList(values.get("arguments"));
            if (values.containsKey("slotNames")) n.slotNames = castList(values.get("slotNames"));
            if (values.containsKey("argNames")) n.argNames = castList(values.get("argNames"));
            if (values.containsKey("isSuperCall")) n.isSuperCall = asBoolean(values.get("isSuperCall"));
            if (values.containsKey("isGlobal")) n.isGlobal = asBoolean(values.get("isGlobal"));
            if (values.containsKey("target")) n.target = (Expr) values.get("target");
            if (values.containsKey("isSingleSlotCall")) n.isSingleSlotCall = asBoolean(values.get("isSingleSlotCall"));
            return;
        }

        if (node instanceof Array) {
            Array n = (Array) node;
            if (values.containsKey("elements")) n.elements = castList(values.get("elements"));
            if (values.containsKey("elementType")) n.elementType = (String) values.get("elementType");
            return;
        }

        if (node instanceof IndexAccess) {
            IndexAccess n = (IndexAccess) node;
            if (values.containsKey("array")) n.array = (Expr) values.get("array");
            if (values.containsKey("index")) n.index = (Expr) values.get("index");
            return;
        }

        if (node instanceof RangeIndex) {
            RangeIndex n = (RangeIndex) node;
            if (values.containsKey("step")) n.step = (Expr) values.get("step");
            if (values.containsKey("start")) n.start = (Expr) values.get("start");
            if (values.containsKey("end")) n.end = (Expr) values.get("end");
            return;
        }

        if (node instanceof MultiRangeIndex) {
            MultiRangeIndex n = (MultiRangeIndex) node;
            if (values.containsKey("ranges")) n.ranges = castList(values.get("ranges"));
            return;
        }

        if (node instanceof EqualityChain) {
            EqualityChain n = (EqualityChain) node;
            if (values.containsKey("left")) n.left = (Expr) values.get("left");
            if (values.containsKey("operator")) n.operator = (String) values.get("operator");
            if (values.containsKey("isAllChain")) n.isAllChain = asBoolean(values.get("isAllChain"));
            if (values.containsKey("chainArguments")) n.chainArguments = castList(values.get("chainArguments"));
            return;
        }

        if (node instanceof BooleanChain) {
            BooleanChain n = (BooleanChain) node;
            if (values.containsKey("isAll")) n.isAll = asBoolean(values.get("isAll"));
            if (values.containsKey("expressions")) n.expressions = castList(values.get("expressions"));
            return;
        }

        if (node instanceof Slot) {
            Slot n = (Slot) node;
            if (values.containsKey("name")) n.name = (String) values.get("name");
            if (values.containsKey("type")) n.type = (String) values.get("type");
            return;
        }

        if (node instanceof Lambda) {
            Lambda n = (Lambda) node;
            if (values.containsKey("parameters")) n.parameters = castList(values.get("parameters"));
            if (values.containsKey("returnSlots")) n.returnSlots = castList(values.get("returnSlots"));
            if (values.containsKey("body")) n.body = (Stmt) values.get("body");
            if (values.containsKey("expressionBody")) n.expressionBody = (Expr) values.get("expressionBody");
            if (values.containsKey("inferParameters")) n.inferParameters = asBoolean(values.get("inferParameters"));
            return;
        }

        if (node instanceof PropertyAccess) {
            PropertyAccess n = (PropertyAccess) node;
            if (values.containsKey("left")) n.left = (Expr) values.get("left");
            if (values.containsKey("right")) n.right = (Expr) values.get("right");
            return;
        }

        if (node instanceof ArgumentList) {
            ArgumentList n = (ArgumentList) node;
            if (values.containsKey("arguments")) n.arguments = castList(values.get("arguments"));
        }
    }

    private static Enum<?> readEnum(DataInput in) throws IOException {
        String enumClassName = readString(in);
        String enumName = readString(in);

        if (Keyword.class.getName().equals(enumClassName)) {
            try {
                return Keyword.valueOf(enumName);
            } catch (IllegalArgumentException e) {
                throw new IOException("Unknown enum constant '" + enumName + "' for " + enumClassName, e);
            }
        }

        if (ProgramType.class.getName().equals(enumClassName)) {
            try {
                return ProgramType.valueOf(enumName);
            } catch (IllegalArgumentException e) {
                throw new IOException("Unknown enum constant '" + enumName + "' for " + enumClassName, e);
            }
        }

        throw new IOException("Unknown enum class in IR: " + enumClassName);
    }

    private static List<Object> readList(DataInput in, int depth) throws IOException {
        int size = in.readInt();
        ensureCollectionSize(size, "list");
        List<Object> list = new ArrayList<Object>(size);
        for (int i = 0; i < size; i++) {
            list.add(readValue(in, depth + 1));
        }
        return list;
    }

    private static Map<String, Object> readMap(DataInput in, int depth) throws IOException {
        int size = in.readInt();
        ensureCollectionSize(size, "map");
        Map<String, Object> map = new LinkedHashMap<String, Object>(size);
        for (int i = 0; i < size; i++) {
            String key = readString(in);
            Object value = readValue(in, depth + 1);
            map.put(key, value);
        }
        return map;
    }

    private static void writeString(DataOutput out, String value) throws IOException {
        if (value == null) {
            out.writeInt(-1);
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_STRING_BYTES) {
            throw new IOException("String exceeds IR limit: " + bytes.length + " bytes");
        }
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInput in) throws IOException {
        int len = in.readInt();
        if (len == -1) {
            return null;
        }
        if (len < 0 || len > MAX_STRING_BYTES) {
            throw new IOException("Invalid string length in IR: " + len);
        }
        byte[] bytes = new byte[len];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void ensureDepth(int depth) throws IOException {
        if (depth > MAX_DEPTH) {
            throw new IOException("IR structure exceeds maximum depth of " + MAX_DEPTH);
        }
    }

    private static void ensureCollectionSize(int size, String kind) throws IOException {
        if (size < 0 || size > MAX_COLLECTION_SIZE) {
            throw new IOException("Invalid " + kind + " size in IR: " + size);
        }
    }

    private static void writeNodeFields(DataOutput out, String nodeName, int depth, String[] fieldNames, Object[] values)
            throws IOException {
        if (fieldNames.length != values.length) {
            throw new IOException("IR node field name/value mismatch for " + nodeName);
        }
        ensureCollectionSize(fieldNames.length, "fields");
        writeString(out, NODE_PACKAGE_PREFIX + nodeName);
        out.writeInt(fieldNames.length);
        for (int i = 0; i < fieldNames.length; i++) {
            writeString(out, fieldNames[i]);
            writeValue(out, values[i], depth + 1);
        }
    }

    private static boolean asBoolean(Object value) {
        return value != null && ((Boolean) value).booleanValue();
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> castList(Object value) {
        return (List<T>) value;
    }

    @SuppressWarnings("unchecked")
    private static <T> Map<String, T> castMap(Object value) {
        return (Map<String, T>) value;
    }

    private static final class NodeWriteException extends RuntimeException {
        final IOException io;

        NodeWriteException(IOException io) {
            this.io = io;
        }
    }

    private static final class NodeWriteVisitor implements VisitorImpl<Void> {
        private final DataOutput out;
        private final int depth;

        NodeWriteVisitor(DataOutput out, int depth) {
            this.out = out;
            this.depth = depth;
        }

        private Void writeNode(String nodeName, String[] fieldNames, Object[] values) {
            try {
                writeNodeFields(out, nodeName, depth, fieldNames, values);
                return null;
            } catch (IOException e) {
                throw new NodeWriteException(e);
            }
        }

        @Override
        public Void visit(Program n) {
            return writeNode("Program", new String[]{"unit", "programType"}, new Object[]{n.unit, n.programType});
        }

        @Override
        public Void visit(Unit n) {
            return writeNode("Unit", new String[]{"name", "imports", "policies", "types", "mainClassName", "resolvedImports"},
                    new Object[]{n.name, n.imports, n.policies, n.types, n.mainClassName, n.resolvedImports});
        }

        @Override
        public Void visit(Use n) {
            return writeNode("Use", new String[]{"imports"}, new Object[]{n.imports});
        }

        @Override
        public Void visit(Type n) {
            return writeNode("Type", new String[]{
                    "name", "visibility", "extendName", "fields", "constructor", "methods", "statements",
                    "constructors", "implementedPolicies", "cachedAncestorPolicies", "viralPoliciesValidated"
            }, new Object[]{
                    n.name, n.visibility, n.extendName, n.fields, n.constructor, n.methods, n.statements,
                    n.constructors, n.implementedPolicies, n.cachedAncestorPolicies, Boolean.valueOf(n.viralPoliciesValidated)
            });
        }

        @Override
        public Void visit(Field n) {
            return writeNode("Field", new String[]{"name", "type", "visibility", "value"},
                    new Object[]{n.name, n.type, n.visibility, n.value});
        }

        @Override
        public Void visit(Method n) {
            return writeNode("Method", new String[]{
                    "methodName", "associatedClass", "visibility", "returnSlots", "parameters", "body", "isBuiltin", "isPolicyMethod"
            }, new Object[]{
                    n.methodName, n.associatedClass, n.visibility, n.returnSlots, n.parameters, n.body,
                    Boolean.valueOf(n.isBuiltin), Boolean.valueOf(n.isPolicyMethod)
            });
        }

        @Override
        public Void visit(Param n) {
            return writeNode("Param", new String[]{
                    "name", "type", "defaultValue", "hasDefaultValue", "typeInferred", "isLambdaParameter", "isTupleDestructuring", "tupleElements"
            }, new Object[]{
                    n.name, n.type, n.defaultValue, Boolean.valueOf(n.hasDefaultValue), Boolean.valueOf(n.typeInferred),
                    Boolean.valueOf(n.isLambdaParameter), Boolean.valueOf(n.isTupleDestructuring), n.tupleElements
            });
        }

        @Override
        public Void visit(Constructor n) {
            return writeNode("Constructor", new String[]{"parameters", "body"}, new Object[]{n.parameters, n.body});
        }

        @Override
        public Void visit(ConstructorCall n) {
            return writeNode("ConstructorCall", new String[]{"className", "arguments", "argNames"},
                    new Object[]{n.className, n.arguments, n.argNames});
        }

        @Override
        public Void visit(Policy n) {
            return writeNode("Policy", new String[]{"name", "visibility", "methods", "sourceUnit", "composedPolicies"},
                    new Object[]{n.name, n.visibility, n.methods, n.sourceUnit, n.composedPolicies});
        }

        @Override
        public Void visit(PolicyMethod n) {
            return writeNode("PolicyMethod", new String[]{"methodName", "parameters", "returnSlots"},
                    new Object[]{n.methodName, n.parameters, n.returnSlots});
        }

        @Override
        public Void visit(Block n) {
            return writeNode("Block", new String[]{"statements"}, new Object[]{n.statements});
        }

        @Override
        public Void visit(Assignment n) {
            return writeNode("Assignment", new String[]{"left", "right", "isDeclaration"},
                    new Object[]{n.left, n.right, Boolean.valueOf(n.isDeclaration)});
        }

        @Override
        public Void visit(Var n) {
            return writeNode("Var", new String[]{"name", "value", "explicitType"},
                    new Object[]{n.name, n.value, n.explicitType});
        }

        @Override
        public Void visit(StmtIf n) {
            return writeNode("StmtIf", new String[]{"condition", "thenBlock", "elseBlock"},
                    new Object[]{n.condition, n.thenBlock, n.elseBlock});
        }

        @Override
        public Void visit(ExprIf n) {
            return writeNode("ExprIf", new String[]{"condition", "thenExpr", "elseExpr"},
                    new Object[]{n.condition, n.thenExpr, n.elseExpr});
        }

        @Override
        public Void visit(For n) {
            return writeNode("For", new String[]{"iterator", "range", "arraySource", "body"},
                    new Object[]{n.iterator, n.range, n.arraySource, n.body});
        }

        @Override
        public Void visit(Skip n) {
            return writeNode("Skip", new String[0], new Object[0]);
        }

        @Override
        public Void visit(Break n) {
            return writeNode("Break", new String[0], new Object[0]);
        }

        @Override
        public Void visit(Range n) {
            return writeNode("Range", new String[]{"step", "start", "end"}, new Object[]{n.step, n.start, n.end});
        }

        @Override
        public Void visit(Exit n) {
            return writeNode("Exit", new String[0], new Object[0]);
        }

        @Override
        public Void visit(Tuple n) {
            return writeNode("Tuple", new String[]{"elements"}, new Object[]{n.elements});
        }

        @Override
        public Void visit(ReturnSlotAssignment n) {
            return writeNode("ReturnSlotAssignment", new String[]{"variableNames", "methodCall", "lambda"},
                    new Object[]{n.variableNames, n.methodCall, n.lambda});
        }

        @Override
        public Void visit(SlotDeclaration n) {
            return writeNode("SlotDeclaration", new String[]{"slotNames"}, new Object[]{n.slotNames});
        }

        @Override
        public Void visit(SlotAssignment n) {
            return writeNode("SlotAssignment", new String[]{"slotName", "value"},
                    new Object[]{n.slotName, n.value});
        }

        @Override
        public Void visit(MultipleSlotAssignment n) {
            return writeNode("MultipleSlotAssignment", new String[]{"assignments"}, new Object[]{n.assignments});
        }

        @Override
        public Void visit(BinaryOp n) {
            return writeNode("BinaryOp", new String[]{"left", "op", "right"}, new Object[]{n.left, n.op, n.right});
        }

        @Override
        public Void visit(Unary n) {
            return writeNode("Unary", new String[]{"op", "operand"}, new Object[]{n.op, n.operand});
        }

        @Override
        public Void visit(TypeCast n) {
            return writeNode("TypeCast", new String[]{"targetType", "expression"}, new Object[]{n.targetType, n.expression});
        }

        @Override
        public Void visit(MethodCall n) {
            return writeNode("MethodCall", new String[]{
                    "name", "qualifiedName", "arguments", "slotNames", "argNames", "isSuperCall", "isGlobal", "target", "isSingleSlotCall"
            }, new Object[]{
                    n.name, n.qualifiedName, n.arguments, n.slotNames, n.argNames,
                    Boolean.valueOf(n.isSuperCall), Boolean.valueOf(n.isGlobal), n.target, Boolean.valueOf(n.isSingleSlotCall)
            });
        }

        @Override
        public Void visit(Array n) {
            return writeNode("Array", new String[]{"elements", "elementType"}, new Object[]{n.elements, n.elementType});
        }

        @Override
        public Void visit(IndexAccess n) {
            return writeNode("IndexAccess", new String[]{"array", "index"}, new Object[]{n.array, n.index});
        }

        @Override
        public Void visit(RangeIndex n) {
            return writeNode("RangeIndex", new String[]{"step", "start", "end"}, new Object[]{n.step, n.start, n.end});
        }

        @Override
        public Void visit(MultiRangeIndex n) {
            return writeNode("MultiRangeIndex", new String[]{"ranges"}, new Object[]{n.ranges});
        }

        @Override
        public Void visit(EqualityChain n) {
            return writeNode("EqualityChain", new String[]{"left", "operator", "isAllChain", "chainArguments"},
                    new Object[]{n.left, n.operator, Boolean.valueOf(n.isAllChain), n.chainArguments});
        }

        @Override
        public Void visit(BooleanChain n) {
            return writeNode("BooleanChain", new String[]{"isAll", "expressions"},
                    new Object[]{Boolean.valueOf(n.isAll), n.expressions});
        }

        @Override
        public Void visit(Slot n) {
            return writeNode("Slot", new String[]{"name", "type"}, new Object[]{n.name, n.type});
        }

        @Override
        public Void visit(Lambda n) {
            return writeNode("Lambda", new String[]{"parameters", "returnSlots", "body", "expressionBody", "inferParameters"},
                    new Object[]{n.parameters, n.returnSlots, n.body, n.expressionBody, Boolean.valueOf(n.inferParameters)});
        }

        @Override
        public Void visit(Identifier n) {
            return writeNode("Identifier", new String[]{"name"}, new Object[]{n.name});
        }

        @Override
        public Void visit(IntLiteral n) {
            return writeNode("IntLiteral", new String[]{"value"}, new Object[]{n.value});
        }

        @Override
        public Void visit(FloatLiteral n) {
            return writeNode("FloatLiteral", new String[]{"value"}, new Object[]{n.value});
        }

        @Override
        public Void visit(TextLiteral n) {
            return writeNode("TextLiteral", new String[]{"value", "isInterpolated"},
                    new Object[]{n.value, Boolean.valueOf(n.isInterpolated)});
        }

        @Override
        public Void visit(BoolLiteral n) {
            return writeNode("BoolLiteral", new String[]{"value"}, new Object[]{Boolean.valueOf(n.value)});
        }

        @Override
        public Void visit(NoneLiteral n) {
            return writeNode("NoneLiteral", new String[0], new Object[0]);
        }

        @Override
        public Void visit(This n) {
            return writeNode("This", new String[]{"className"}, new Object[]{n.className});
        }

        @Override
        public Void visit(Super n) {
            return writeNode("Super", new String[0], new Object[0]);
        }

        @Override
        public Void visit(ChainedComparison n) {
            return writeNode("ChainedComparison", new String[]{"expressions", "operators"},
                    new Object[]{n.expressions, n.operators});
        }

        @Override
        public Void visit(PropertyAccess n) {
            return writeNode("PropertyAccess", new String[]{"left", "right"}, new Object[]{n.left, n.right});
        }

        public List<Void> visitList(List<? extends Base> nodes) {
            List<Void> results = new ArrayList<Void>();
            for (Base n : nodes) {
                results.add(visit(n));
            }
            return results;
        }

        public void visitAll(List<? extends Base> nodes) {
            for (Base n : nodes) {
                visit(n);
            }
        }

        @Override
        public Void visit(Expr n) {
            if (n instanceof ArgumentList) {
                ArgumentList args = (ArgumentList) n;
                return writeNode("ArgumentList", new String[]{"arguments"}, new Object[]{args.arguments});
            }
            throw new NodeWriteException(new IOException("Unsupported IR expr node type: " + n.getClass().getName()));
        }

        @Override
        public Void visit(Base n) {
            throw new NodeWriteException(new IOException("Unsupported IR node type: " + n.getClass().getName()));
        }
    }
}
