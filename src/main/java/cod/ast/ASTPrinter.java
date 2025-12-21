package cod.ast;

import static cod.syntax.Keyword.*;
import cod.ast.nodes.*;

public class ASTPrinter extends BaseASTVisitor<Void> {
    private int indent = 0;
    
    private String getIndent() {
        return new String(new char[indent]).replace("\0", "|   ");
    }
    
    private void print(String message) {
        System.out.print(getIndent() + message);
    }
    
    private void println(String message) {
        print(getIndent() + message + "\n");
    }
    
    @Override
    public Void visit(ProgramNode node) {
        println("\nPROGRAM");
        println("|   ");
        if (node.unit != null) visit(node.unit);
        return null;
    }
    
    @Override
    public Void visit(UnitNode node) {
        println("|   UNIT: " + node.name);
        
        if (node.imports != null) visit(node.imports);
        visitAll(node.types);
        println("|   ");
        return null;
    }
    
    @Override
    public Void visit(UseNode node) {
            println("USE imports: " + (node.imports.isEmpty() ? "[]" : node.imports));
            println("|   ");
        return null;
    }
    
    @Override
    public Void visit(TypeNode node) {
        println("CLASS: " + node.name + " extends: " + node.extendName + " visibility: " + node.visibility + "\n|   |   ");
        indent++;
        visitAll(node.fields);
        if (node.constructor != null) visit(node.constructor);
        visitAll(node.methods);
        visitAll(node.statements);
        indent--;
        return null;
    }
    
    @Override
    public Void visit(FieldNode node) {
        println("FIELD: " + node.type + " " + node.name + " visibility: " + node.visibility);
        if (node.value != null) {
            println("|   value:");
            indent += 2;
            visit(node.value);
            indent -= 2;
        }
        return null;
    }
    
    @Override
    public Void visit(MethodNode node) {
        print("|   METHOD: " + node.methodName + " slots: ");
        for (SlotNode s : node.returnSlots) System.out.print(s.name + " ");
        System.out.println(" visibility: " + node.visibility);
        visitAll(node.parameters);
        visitAll(node.body);
        return null;
    }
    
    @Override
    public Void visit(ParamNode node) {
        println("|   PARAM: " + node.type + " " + node.name);
        return null;
    }
    
    @Override
    public Void visit(ConstructorNode node) {
        println("CONSTRUCTOR PARAMS: " + node.parameters.size());
        indent++;
        visitAll(node.parameters);
        visitAll(node.body);
        indent--;
        return null;
    }
    
    @Override
    public Void visit(BlockNode node) {
        println("BLOCK:");
        indent++;
        visitAll(node.statements);
        indent--;
        return null;
    }
    
    @Override
    public Void visit(AssignmentNode node) {
        println("ASSIGNMENT:");
        println("|   target:");
        indent += 2;
        if (node.left != null) visit(node.left);
        indent -= 2;
        println("|   value:");
        indent += 2;
        if (node.right != null) visit(node.right);
        indent -= 2;
        return null;
    }
    
    @Override
    public Void visit(VarNode node) {
        print("VAR: " + node.name);
        if (node.explicitType != null) {
            System.out.print(" (type: " + node.explicitType + ")");
        }
        if (node.value != null) {
            System.out.println(" =");
            indent++;
            visit(node.value);
            indent--;
        } else {
            System.out.println();
        }
        return null;
    }
    
    @Override
    public Void visit(StmtIfNode node) {
        println("|   |   IF condition:");
        indent ++;
        if (node.condition != null) visit(node.condition);
        indent --;
        println("|   |   |   THEN execute:");
        indent += 2;
        if (node.thenBlock != null) visit(node.thenBlock);
        indent -= 2;
        if (node.elseBlock != null && !node.elseBlock.statements.isEmpty()) {
            println("|   |   ELSE:");
            indent += 2;
            visit(node.elseBlock);
            indent -= 2;
        }
        return null;
    }
    
    @Override
    public Void visit(ForNode node) {
        println("FOR iterator: " + node.iterator);
        indent += 2;
        if (node.range != null) visit(node.range);
        indent -= 2;
        println("|   BODY:");
        indent += 2;
        if (node.body != null) visit(node.body);
        indent -= 2;
        return null;
    }
    
    @Override
    public Void visit(RangeNode node) {
        println("|   step:");
        indent += 2;
        if (node.step != null) visit(node.step);
        indent -= 2;
        println("RANGE:");
        println("|   start:");
        indent += 2;
        if (node.start != null) visit(node.start);
        indent -= 2;
        println("|   end:");
        indent += 2;
        if (node.end != null) visit(node.end);
        indent -= 2;
        return null;
    }
    
    @Override
    public Void visit(ExitNode node) {
        println("EXIT");
        return null;
    }
    
    @Override
    public Void visit(ReturnSlotAssignmentNode node) {
        println("RETURN SLOT ASSIGNMENT:");
        println("|   VARIABLES: " + node.variableNames);
        println("|   METHOD CALL:");
        indent += 2;
        if (node.methodCall != null) visit(node.methodCall);
        indent -= 2;
        return null;
    }
    
    @Override
    public Void visit(SlotDeclarationNode node) {
        println("SLOT DECLARATION: " + node.slotNames);
        return null;
    }
    
    @Override
    public Void visit(SlotAssignmentNode node) {
        println("SLOT ASSIGNMENT:");
        println("|   slot: " + (node.slotName != null ? node.slotName : "(implicit)"));
        println("|   value:");
        indent += 2;
        if (node.value != null) visit(node.value);
        indent -= 2;
        return null;
    }
    
    @Override
    public Void visit(MultipleSlotAssignmentNode node) {
        println("MULTIPLE SLOT ASSIGNMENT:");
        indent++;
        for (int i = 0; i < node.assignments.size(); i++) {
            SlotAssignmentNode assign = node.assignments.get(i);
            println("ASSIGNMENT " + i + ":");
            println("|   slot: " + (assign.slotName != null ? assign.slotName : "(positional)"));
            println("|   value:");
            indent += 2;
            if (assign.value != null) visit(assign.value);
            indent -= 2;
        }
        indent--;
        return null;
    }
    
    @Override
    public Void visit(ExprNode node) {
        // Check if this ExprNode is actually a more specific type (like in original printer)
        if (node instanceof IndexAccessNode) {
            // Handle IndexAccessNode that got cast to ExprNode
            IndexAccessNode idx = (IndexAccessNode) node;
            println("INDEX ACCESS");
            println("|   array:");
            indent += 2;
            visit(idx.array);
            indent -= 2;
            println("|   index:");
            indent += 2;
            visit(idx.index);
            indent -= 2;
        } else if (node instanceof ArrayNode) {
            // Handle ArrayNode that got cast to ExprNode
            ArrayNode arr = (ArrayNode) node;
            println("ARRAY literal with " + arr.elements.size() + " elements:");
            indent++;
            for (int i = 0; i < arr.elements.size(); i++) {
                println("[" + i + "]:");
                indent++;
                visit(arr.elements.get(i));
                indent--;
            }
            indent--;
        } else if (node instanceof BooleanChainNode) {
            // Handle BooleanChainNode that got cast to ExprNode
            BooleanChainNode chain = (BooleanChainNode) node;
            println("BOOLEAN chain: " + (chain.isAll ? ALL : ANY));
            indent++;
            for (ExprNode expr : chain.expressions) {
                visit(expr);
            }
            indent--;
        } else if (node instanceof EqualityChainNode) {
            // Handle EqualityChainNode that got cast to ExprNode
            EqualityChainNode chain = (EqualityChainNode) node;
            println("EQUALITY chain: " + (chain.isAllChain ? ALL : ANY) + " " + chain.operator);
            println("|   LEFT:");
            indent += 2;
            visit(chain.left);
            indent -= 2;
            println("|   CHAIN arguments:");
            indent += 2;
            for (ExprNode arg : chain.chainArguments) {
                visit(arg);
            }
            indent -= 2;
        } else if (node instanceof UnaryNode) {
            // Handle UnaryNode that got cast to ExprNode
            UnaryNode unary = (UnaryNode) node;
            println("UNARY: " + unary.op);
            indent++;
            visit(unary.operand);
            indent--;
        } else if (node.value != null) {
            println("value: " + node.value);
        } else if (node.name != null) {
            println("IDENTIFIER: " + node.name);
        } else if (node.left != null && node.right != null && node.op != null) {
            // This might be a BinaryOpNode that got cast to ExprNode
            println("|   BINARY operation: " + node.op);
            println("|   left:");
            indent += 2;
            visit(node.left);
            indent -= 2;
            println("|   right:");
            indent += 2;
            visit(node.right);
            indent -= 2;
        } else {
            println("EXPR (unresolved - name: " + node.name + ", value: " + node.value + ")");
        }
        return null;
    }
    
    @Override
    public Void visit(BinaryOpNode node) {
        println("|   BINARY operation: " + node.op);
        println("|   left:");
        indent += 2;
        if (node.left != null) visit(node.left);
        indent -= 2;
        println("|   right:");
        indent += 2;
        if (node.right != null) visit(node.right);
        indent -= 2;
        return null;
    }
    
    @Override
    public Void visit(UnaryNode node) {
        println("UNARY: " + node.op);
        indent++;
        if (node.operand != null) visit(node.operand);
        indent--;
        return null;
    }
    
    @Override
    public Void visit(TypeCastNode node) {
        println("TYPECAST: " + node.targetType);
        println("|   expression:");
        indent += 2;
        if (node.expression != null) visit(node.expression);
        indent -= 2;
        return null;
    }
    
    @Override
    public Void visit(MethodCallNode node) {
        print("IDENTIFIER/CALL: " + (node.qualifiedName != null ? node.qualifiedName : node.name));
        if (node.slotNames != null && !node.slotNames.isEmpty()) {
            System.out.print(" (slot_cast: ");
            for (int i = 0; i < node.slotNames.size(); i++) {
                if (i > 0) System.out.print(", ");
                System.out.print(node.slotNames.get(i));
            }
            System.out.print(")");
        }
        if (node.chainType != null) {
            System.out.print(" (chain: " + node.chainType + ")");
        }
        System.out.println();
        
        if (node.chainArguments != null && !node.chainArguments.isEmpty()) {
            println("|   CHAIN arguments:");
            indent += 2;
            for (ExprNode arg : node.chainArguments) {
                visit(arg);
            }
            indent -= 2;
        }
        
        if (node.arguments != null && !node.arguments.isEmpty()) {
            println("|   ARGUMENTS:");
            indent += 2;
            for (ExprNode arg : node.arguments) {
                visit(arg);
            }
            indent -= 2;
        } else {
            println("|   (no arguments)");
        }
        return null;
    }
    
    @Override
    public Void visit(ArrayNode node) {
        println("ARRAY literal with " + node.elements.size() + " elements:");
        indent++;
        for (int i = 0; i < node.elements.size(); i++) {
            println("[" + i + "]:");
            indent++;
            visit(node.elements.get(i));
            indent--;
        }
        indent--;
        return null;
    }
    
    @Override
    public Void visit(IndexAccessNode node) {
        println("INDEX access");
        println("|   ARRAY:");
        indent += 2;
        if (node.array != null) visit(node.array);
        indent -= 2;
        println("|   INDEX:");
        indent += 2;
        if (node.index != null) visit(node.index);
        indent -= 2;
        return null;
    }
    
    @Override
    public Void visit(EqualityChainNode node) {
        println("EQUALITY chain: " + (node.isAllChain ? ALL : ANY) + " " + node.operator);
        println("|   LEFT:");
        indent += 2;
        if (node.left != null) visit(node.left);
        indent -= 2;
        println("|   CHAIN arguments:");
        indent += 2;
        for (ExprNode arg : node.chainArguments) {
            visit(arg);
        }
        indent -= 2;
        return null;
    }
    
    @Override
    public Void visit(BooleanChainNode node) {
        println("BOOLEAN chain: " + (node.isAll ? ALL : ANY));
        indent++;
        for (ExprNode expr : node.expressions) {
            visit(expr);
        }
        indent--;
        return null;
    }
    
    @Override
    public Void visit(SlotNode node) {
        println("SLOT: " + node.name + " (type: " + node.type + ")");
        return null;
    }
    
    public static void print(ASTNode node) {
        ASTPrinter printer = new ASTPrinter();
        printer.visit(node);
    }
}