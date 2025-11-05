package cod.ast;

import cod.ast.nodes.*;

public class ASTPrinter {

    public static void print(ASTNode node, int indent) {
        if (node == null) return;
        String pad = new String(new char[indent]).replace("\0", "  ");

        if (node instanceof ProgramNode) {
            System.out.println(pad + "Program");
            print(((ProgramNode) node).unit, indent + 1);

        } else if (node instanceof UnitNode) {
            UnitNode u = (UnitNode) node;
            System.out.println(pad + "Unit: " + u.name);
            // Print GetNode if it exists
            if (u.imports != null) {
                print(u.imports, indent + 1);
            }
            for (TypeNode t : u.types) print(t, indent + 1);
            
        } else if (node instanceof GetNode) {
            GetNode get = (GetNode) node;
            if (get.imports.isEmpty()) {
                System.out.println(pad + "Get imports: []");
            } else {
                System.out.println(pad + "Get imports: " + get.imports);
            }
            
        } else if (node instanceof TypeNode) {
            TypeNode t = (TypeNode) node;
            System.out.println(
                    pad
                            + "Class: "
                            + t.name
                            + " Extends: "
                            + t.extendName
                            + " Visibility: "
                            + t.visibility);
            for (FieldNode f : t.fields) print(f, indent + 1);
            if (t.constructor != null) print(t.constructor, indent + 1);
            for (MethodNode m : t.methods) print(m, indent + 1);
            for (StatementNode s : t.statements) print(s, indent + 1);

        } else if (node instanceof FieldNode) {
            FieldNode f = (FieldNode) node;
            System.out.println(pad + "Field: " + f.type + " " + f.name + " Visibility: " + f.visibility);
            if (f.value != null) {
                System.out.print(pad + "  Value: ");
                print(f.value, indent + 2);
            }
            // REMOVED: Array assignment handling from FieldNode since it's now in AssignmentNode

        } else if (node instanceof AssignmentNode) {
            // NEW: Handle AssignmentNode
            AssignmentNode assignment = (AssignmentNode) node;
            System.out.println(pad + "Assignment:");
            System.out.print(pad + "  Target: ");
            print(assignment.left, indent + 2);
            System.out.print(pad + "  Value: ");
            print(assignment.right, indent + 2);
            
        } else if (node instanceof ConstructorNode) {
            ConstructorNode c = (ConstructorNode) node;
            System.out.println(pad + "Constructor Params: " + c.parameters.size());
            for (ParamNode p : c.parameters) print(p, indent + 1);
            for (StatementNode s : c.body) print(s, indent + 1);

        } else if (node instanceof MethodNode) {
            MethodNode m = (MethodNode) node;
            System.out.print(pad + "Method: " + m.name + " Slots: ");
            for (SlotNode s : m.returnSlots) System.out.print(s.name + " ");
            System.out.println(" Visibility: " + m.visibility);
            for (ParamNode p : m.parameters) print(p, indent + 1);
            for (StatementNode s : m.body) print(s, indent + 1);

        } else if (node instanceof MethodCallNode) {
            MethodCallNode mc = (MethodCallNode) node;
            System.out.print(
                    pad
                            + "Identifier/Call: "
                            + (mc.qualifiedName != null ? mc.qualifiedName : mc.name));
            // FIX: Handle multiple slot names
            if (mc.slotNames != null && !mc.slotNames.isEmpty()) {
                System.out.print(" (slot_cast: ");
                for (int i = 0; i < mc.slotNames.size(); i++) {
                    if (i > 0) System.out.print(", ");
                    System.out.print(mc.slotNames.get(i));
                }
                System.out.print(")");
            }
            System.out.println();
            for (ExprNode arg : mc.arguments) print(arg, indent + 1);

        } else if (node instanceof ArrayNode) {
            ArrayNode arr = (ArrayNode) node;
            System.out.println(pad + "ArrayLiteral with " + arr.elements.size() + " elements:");
            for (int i = 0; i < arr.elements.size(); i++) {
                System.out.print(pad + "  [" + i + "]: ");
                print(arr.elements.get(i), indent + 2);
            }

        } else if (node instanceof IndexAccessNode) {
            IndexAccessNode idx = (IndexAccessNode) node;
            System.out.println(pad + "IndexAccess");
            System.out.print(pad + "  Array: ");
            print(idx.array, indent + 2);
            System.out.print(pad + "  Index: ");
            print(idx.index, indent + 2);

        } else if (node instanceof UnaryNode) {
            UnaryNode unary = (UnaryNode) node;
            System.out.println(pad + "Unary: " + unary.op);
            print(unary.operand, indent + 1);

        } else if (node instanceof BinaryOpNode) {
            BinaryOpNode b = (BinaryOpNode) node;
            System.out.println(pad + "BinaryOp: " + b.op);
            if (b.left != null) print(b.left, indent + 1);
            if (b.right != null) print(b.right, indent + 1);

        } else if (node instanceof ExprNode) {
            ExprNode e = (ExprNode) node;
            if (e.value != null) {
                System.out.println(pad + "Value: " + e.value);
            } else if (e.name != null) {
                System.out.println(pad + "Identifier: " + e.name);
            } else if (e.left != null && e.right != null && e.op != null) {
                // This might be a BinaryOpNode that got cast to ExprNode
                System.out.println(pad + "BinaryOp: " + e.op);
                print(e.left, indent + 1);
                print(e.right, indent + 1);
            } else if (e instanceof IndexAccessNode) {
                // Handle IndexAccessNode that got cast to ExprNode
                IndexAccessNode idx = (IndexAccessNode) e;
                System.out.println(pad + "IndexAccess");
                System.out.print(pad + "  Array: ");
                print(idx.array, indent + 2);
                System.out.print(pad + "  Index: ");
                print(idx.index, indent + 2);
            } else {
                System.out.println(
                        pad + "Expr (unresolved - name: " + e.name + ", value: " + e.value + ")");
            }

        } else if (node instanceof ParamNode) {
            ParamNode p = (ParamNode) node;
            System.out.println(pad + "Param: " + p.type + " " + p.name);

        } else if (node instanceof SlotNode) {
            SlotNode s = (SlotNode) node;
            System.out.println(pad + "Slot: " + s.name);

        } else if (node instanceof BlockNode) {
            BlockNode block = (BlockNode) node;
            System.out.println(pad + "Block:");
            for (StatementNode s : block.statements) print(s, indent + 1);

        } else if (node instanceof OutputNode) {
            OutputNode p = (OutputNode) node;
            System.out.print(pad + "Output");
            if (p.varName != null) System.out.print(" Var: " + p.varName);
            System.out.println();

            if (p.arguments.isEmpty()) {
                System.out.println(pad + "  (no arguments)");
            } else {
                for (int i = 0; i < p.arguments.size(); i++) {
                    System.out.print(pad + "  Argument " + i + ": ");
                    print(p.arguments.get(i), indent + 2);
                }
            }

        } else if (node instanceof InputNode) {
            InputNode input = (InputNode) node;
            System.out.println(
                    pad + "Input: " + input.variableName + " = (" + input.targetType + ") input");

        } else if (node instanceof VarNode) {
            VarNode v = (VarNode) node;
            System.out.print(pad + "Var: " + v.name);
            if (v.value != null) {
                System.out.println(" = ");
                print(v.value, indent + 1);
            } else {
                System.out.println();
            }

        } else if (node instanceof IfNode) {
            IfNode ifn = (IfNode) node;
            System.out.println(pad + "If");
            System.out.print(pad + "  Condition: ");
            print(ifn.condition, indent + 2);
            System.out.println(pad + "  Then:");
            print(ifn.thenBlock, indent + 2);
            if (!ifn.elseBlock.statements.isEmpty()) {
                System.out.println(pad + "  Else:");
                print(ifn.elseBlock, indent + 2);
            }

        } else if (node instanceof RangeNode) {
    RangeNode r = (RangeNode) node;
    System.out.print(pad + "Range: step ");
    print(r.step, 0);
    System.out.print(" in ");
    print(r.start, 0);
    System.out.print(" to ");
    print(r.end, 0);
    System.out.println();

        } else if (node instanceof ForNode) {
            ForNode f = (ForNode) node;
            System.out.println(pad + "For iterator: " + f.iterator);
            System.out.print(pad + "  Range: ");
            print(f.range, 0);
            System.out.println();
            System.out.println(pad + "  Body:");
            print(f.body, indent + 2);

        } else if (node instanceof ReturnSlotAssignmentNode) {
            ReturnSlotAssignmentNode rsa = (ReturnSlotAssignmentNode) node;
            System.out.println(pad + "ReturnSlotAssignment:");
            System.out.println(pad + "  Variables: " + rsa.variableNames);
            System.out.print(pad + "  MethodCall: ");
            print(rsa.methodCall, indent + 2);

        } else {
            System.out.println(pad + node.getClass().getSimpleName());
        }
    }

    public static void print(ASTNode node) {
        print(node, 0);
    }
}