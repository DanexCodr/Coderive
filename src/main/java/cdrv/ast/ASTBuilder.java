package cod.ast;

import static cdrv.Constants.*;
import cod.ast.nodes.*;
import cod.debug.DebugSystem;
import java.util.*;

public class ASTBuilder {
    private ImportResolver importResolver = new ImportResolver();

    public ProgramNode build(CoderiveParser.ProgramContext ctx) {
        ProgramNode program = ASTFactory.createProgram();

        // Check if unit declaration exists
        if (ctx.unitDeclaration() != null) {
            program.unit = buildUnit(ctx.unitDeclaration());
        } else {
            // Create a default unit if none exists
            program.unit = ASTFactory.createUnit("default");
        }

        // Resolve imports
        resolveImports(program.unit);

        for (CoderiveParser.TypeDeclarationContext tctx : ctx.typeDeclaration()) {
            program.unit.types.add(buildType(tctx));
        }

        return program;
    }

    // Add this method to handle return slot assignment
    public StatementNode buildReturnSlotAssignment(CoderiveParser.ReturnSlotAssignmentContext ctx) {
        // Extract variable names from assignableList using idList
        List<String> variableNames = new ArrayList<String>();
        CoderiveParser.IdListContext idList = ctx.assignableList().idList();
        variableNames.add(idList.ID(0).getText());
        if (idList.ID().size() > 1) variableNames.add(idList.ID(1).getText());
        if (idList.ID().size() > 2) variableNames.add(idList.ID(2).getText());

        // Extract the slot method call
        MethodCallNode methodCall = (MethodCallNode) buildSlotMethodCall(ctx.slotMethodCall());

        ReturnSlotAssignmentNode assignment = ASTFactory.createReturnSlotAssignment(variableNames, methodCall);

        DebugSystem.debug(
                "AST",
                "Return slot assignment: "
                        + assignment.variableNames
                        + " = "
                        + assignment.methodCall.slotNames);
        return assignment;
    }

    private void resolveImports(UnitNode unit) {
        if (unit.imports == null || unit.imports.imports.isEmpty()) return;

        DebugSystem.debug("IMPORTS", "Resolving imports for unit: " + unit.name);

        for (String importName : unit.imports.imports) {
            try {
                ProgramNode importedProgram = importResolver.resolveImport(importName);
                // Store resolved imports for later use by interpreter
                unit.resolvedImports.put(importName, importedProgram);
                DebugSystem.debug("IMPORTS", "Successfully resolved import: " + importName);
            } catch (Exception e) {
                DebugSystem.error(
                        "IMPORTS",
                        "Failed to resolve import: " + importName + " - " + e.getMessage());
            }
        }
    }

    public UnitNode buildUnit(CoderiveParser.UnitDeclarationContext ctx) {
        UnitNode unit = ASTFactory.createUnit(ctx.qualifiedName().getText());

        // Create GetNode for imports
        if (ctx.qualifiedNameList() != null) {
            List<String> imports = new ArrayList<String>();
            for (CoderiveParser.QualifiedNameContext q : ctx.qualifiedNameList().qualifiedName()) {
                imports.add(q.getText());
            }
            unit.imports = ASTFactory.createGetNode(imports);
        }

        return unit;
    }

    public TypeNode buildType(CoderiveParser.TypeDeclarationContext ctx) {
        String visibility = share;
        if (ctx.modifiers() != null) {
            visibility = ctx.modifiers().getText();
        }
        
        String extendName = null;
        if (ctx.EXTEND() != null && ctx.qualifiedName() != null) {
            extendName = ctx.qualifiedName().getText();
        }
        
        TypeNode type = ASTFactory.createType(ctx.ID().getText(), visibility, extendName);

        for (CoderiveParser.TypeBodyContext tb : ctx.typeBody()) {
            ASTNode n = buildTypeBody(tb);
            if (n instanceof FieldNode) type.fields.add((FieldNode) n);
            else if (n instanceof ConstructorNode) type.constructor = (ConstructorNode) n;
            else if (n instanceof MethodNode) type.methods.add((MethodNode) n);
            else if (n instanceof StatementNode) type.statements.add((StatementNode) n);
        }

        return type;
    }

    public FieldNode buildField(CoderiveParser.FieldDeclarationContext ctx) {
        FieldNode field = ASTFactory.createField(ctx.ID().getText(), ctx.type().getText());
        if (ctx.expr() != null) {
            field.value = buildExpression(ctx.expr());
        }
        return field;
    }

    public ConstructorNode buildConstructor(CoderiveParser.ConstructorContext ctx) {
        ConstructorNode cons = ASTFactory.createConstructor();
        
        if (ctx.parameterList() != null) {
            for (CoderiveParser.ParameterContext p : ctx.parameterList().parameter()) {
                cons.parameters.add(buildParameter(p));
            }
        }
        for (CoderiveParser.StatementContext s : ctx.statement()) {
            cons.body.add(buildStatement(s));
        }
        return cons;
    }

    public MethodNode buildMethod(CoderiveParser.MethodDeclarationContext ctx) {
        String visibility = share;
        if (ctx.modifiers() != null) {
            visibility = ctx.modifiers().getText();
        }
        
        List<String> returnSlots = null;
        if (ctx.slotList() != null) {
            returnSlots = new ArrayList<String>();
            CoderiveParser.IdListContext idList = ctx.slotList().idList();
            returnSlots.add(idList.ID(0).getText());
            if (idList.ID().size() > 1) returnSlots.add(idList.ID(1).getText());
            if (idList.ID().size() > 2) returnSlots.add(idList.ID(2).getText());
        }
        
        MethodNode method = ASTFactory.createMethod(ctx.ID().getText(), visibility, returnSlots);

        if (ctx.parameterList() != null) {
            for (CoderiveParser.ParameterContext p : ctx.parameterList().parameter()) {
                method.parameters.add(buildParameter(p));
            }
        }

        for (CoderiveParser.StatementContext s : ctx.statement()) {
            method.body.add(buildStatement(s));
        }

        return method;
    }

    public ExprNode buildUnaryExpr(CoderiveParser.UnaryExprContext ctx) {
        String op = ctx.getChild(0).getText(); // Get the unary operator (+ or -)
        ExprNode operand = buildExpression(ctx.expr()); // The expression being operated on
        return ASTFactory.createUnaryOp(op, operand);
    }

    public ParamNode buildParameter(CoderiveParser.ParameterContext ctx) {
        return ASTFactory.createParam(ctx.ID().getText(), ctx.type().getText());
    }

    public StatementNode buildInputAssignment(CoderiveParser.InputAssignmentContext ctx) {
        String targetType = ctx.inputStatement().typeInput().type().getText();
        String variableName = ctx.assignable().ID().getText();
        return ASTFactory.createInput(targetType, variableName);
    }

    public StatementNode buildOutputSlotCall(CoderiveParser.OutputSlotCallContext ctx) {
    OutputNode output = ASTFactory.createOutput();
    
    // Check if there's slot casting
    if (ctx.slotCast() != null) {
        // Extract slot names from slotCast
        List<String> slotNames = new ArrayList<String>();
        CoderiveParser.IdListContext idList = ctx.slotCast().idList();
        slotNames.add(idList.ID(0).getText());
        if (idList.ID().size() > 1) slotNames.add(idList.ID(1).getText());
        if (idList.ID().size() > 2) slotNames.add(idList.ID(2).getText());
        
        // Build method call with slot names
        MethodCallNode methodCall = (MethodCallNode) buildMethodCall(ctx.methodCall());
        methodCall.slotNames = slotNames;
        output.arguments.add(methodCall);
        
        DebugSystem.debug("AST", "Output with slot casting: " + slotNames + ":" + methodCall.name);
    } else {
        // Regular method call without slot casting
        MethodCallNode methodCall = (MethodCallNode) buildMethodCall(ctx.methodCall());
        output.arguments.add(methodCall);
    }
    
    return output;
}

    public StatementNode buildOutputNamedAssignment(CoderiveParser.OutputNamedAssignmentContext ctx) {
        OutputNode output = ASTFactory.createOutput(ctx.ID().getText());
        // For named assignment, we only add the expression
        output.arguments.add(buildExpression(ctx.expr()));
        return output;
    }

    public StatementNode buildOutputExpression(CoderiveParser.OutputExpressionContext ctx) {
        OutputNode output = ASTFactory.createOutput();
        // For simple expressions, we only add the expression itself
        output.arguments.add(buildExpression(ctx.expr()));
        return output;
    }

    // Add this method to handle the new slotMethodCallStatement
    public StatementNode buildSlotMethodCallStatement(CoderiveParser.SlotMethodCallStatementContext ctx) {
        MethodCallNode call = (MethodCallNode) buildSlotMethodCall(ctx.slotMethodCall());
        return call; // Return as MethodCallNode with slotNames populated
    }

    // Update the existing methodCall to handle slot casting
    public StatementNode buildMethodCall(CoderiveParser.MethodCallContext ctx) {
        // Build the full qualified method name
        StringBuilder fullName = new StringBuilder();
        
        if (ctx.qualifiedName() != null) {
            fullName.append(ctx.qualifiedName().getText()).append(".");
        }
        fullName.append(ctx.ID().getText());
        
        MethodCallNode call = ASTFactory.createMethodCall(ctx.ID().getText(), fullName.toString());
        
        if (ctx.argumentList() != null) {
            for (CoderiveParser.ExprContext e : ctx.argumentList().expr()) {
                call.arguments.add(buildExpression(e));
            }
        }
        return call;
    }

    // Add buildSlotMethodCall method
    public StatementNode buildSlotMethodCall(CoderiveParser.SlotMethodCallContext ctx) {
        MethodCallNode call = (MethodCallNode) buildMethodCall(ctx.methodCall());

        // Extract slot names from slotCast using idList
        call.slotNames = new ArrayList<String>();
        CoderiveParser.IdListContext idList = ctx.slotCast().idList();
        call.slotNames.add(idList.ID(0).getText());
        if (idList.ID().size() > 1) call.slotNames.add(idList.ID(1).getText());
        if (idList.ID().size() > 2) call.slotNames.add(idList.ID(2).getText());

        DebugSystem.debug("AST", "Slot method call with slots: " + call.slotNames);
        return call;
    }

// FIXED: Use AssignmentNode instead of FieldNode for assignments
public StatementNode buildAssignment(CoderiveParser.AssignmentContext ctx) {
    // Regular assignment only - input assignments are handled separately
    ExprNode value = buildExpression(ctx.expr());
    ExprNode target;

    if (ctx.assignable().ID() != null) {
        // Simple variable assignment: x = value
        target = ASTFactory.createIdentifier(ctx.assignable().ID().getText());
    } else {
        // Array assignment: arr[0] = value
        target = buildIndexAccess(ctx.assignable().indexAccess());
    }
    
    // Create AssignmentNode instead of FieldNode
    return ASTFactory.createAssignment(target, value);
}

    public ExprNode buildTypeCast(CoderiveParser.TypeCastContext ctx) {
        String targetType = ctx.type().getText();
        ExprNode expression = buildExpression(ctx.expr());
        return ASTFactory.createTypeCast(targetType, expression);
    }

    public ExprNode buildComparisonExpr(CoderiveParser.ComparisonExprContext ctx) {
        ExprNode left = buildExpression(ctx.expr(0));
        ExprNode right = buildExpression(ctx.expr(1));
        String op = ctx.getChild(1).getText();
        return ASTFactory.createBinaryOp(left, op, right);
    }

    public StatementNode buildForStatement(CoderiveParser.ForStatementContext ctx) {
    String iterator = ctx.ID().getText();
    
    ExprNode step;
    if (ctx.forStepExpr() != null) {
        ExprNode stepExpr = buildForStepExpr(ctx.forStepExpr());
        
        // If it's an operator step (*2, /2, etc.), create the binary operation
        if (stepExpr instanceof UnaryNode) {
            UnaryNode unary = (UnaryNode) stepExpr;
            ExprNode iteratorRef = ASTFactory.createIdentifier(iterator);
            
            switch (unary.op) {
                case "*":
                case "/":
                    step = ASTFactory.createBinaryOp(iteratorRef, unary.op, unary.operand);
                    break;
                case "+":
                    step = unary.operand; // +1 becomes just 1
                    break;
                case "-":
                    step = unary; // -1 stays as negative literal
                    break;
                default:
                    step = ASTFactory.createIntLiteral(1);
            }
        } else {
            step = stepExpr; // Regular step expression
        }
    } else {
        step = ASTFactory.createIntLiteral(1); // Default step
    }
    
    ExprNode start = buildExpression(ctx.expr(0));
    ExprNode end = buildExpression(ctx.expr(1));
    
    RangeNode range = ASTFactory.createRange(step, start, end);
    ForNode forNode = ASTFactory.createFor(iterator, range);

    for (CoderiveParser.StatementContext s : ctx.statement()) {
        forNode.body.statements.add(buildStatement(s));
    }
    return forNode;
}

public ExprNode buildForStepExpr(CoderiveParser.ForStepExprContext ctx) {
    if (ctx instanceof CoderiveParser.OperatorStepContext) {
        return buildOperatorStep((CoderiveParser.OperatorStepContext) ctx);
    } else {
        return buildExpression(((CoderiveParser.RegularStepContext) ctx).expr());
    }
}

public ExprNode buildOperatorStep(CoderiveParser.OperatorStepContext ctx) {
    String operator = ctx.getChild(0).getText(); // *, /, +, or -
    ExprNode operand = buildExpression(ctx.expr());
    
    // For the for-loop context, we need to create operations relative to the iterator
    // This gets handled in buildForStatement with the iterator context
    return ASTFactory.createUnaryOp(operator, operand);
}

    public StatementNode buildIfStatement(CoderiveParser.IfStatementContext ctx) {
        DebugSystem.debug("AST", "Building if-elif-else chain...");

        // 1. Build the root IF node (using expr(0) and thenBlock(0))
        ExprNode ifCondition = buildExpression(ctx.expr(0));
        IfNode rootIfNode = ASTFactory.createIf(ifCondition);
        
        CoderiveParser.ThenBlockContext ifThenBlock = ctx.thenBlock(0);
        for (CoderiveParser.StatementContext stmt : ifThenBlock.statement()) {
            rootIfNode.thenBlock.statements.add(buildStatement(stmt));
        }
        DebugSystem.debug("AST", "Built root if-block with " + ifThenBlock.statement().size() + " statements");

        // 2. Keep track of the 'current' node to attach the next 'else' part to.
        IfNode currentNode = rootIfNode;

        // 3. Iterate over all ELIF blocks
        int elifCount = ctx.ELIF().size();
        for (int i = 0; i < elifCount; i++) {
            // ELIF condition is expr(i + 1)
            // ELIF block is thenBlock(i + 1)
            ExprNode elifCondition = buildExpression(ctx.expr(i + 1));
            CoderiveParser.ThenBlockContext elifThenBlock = ctx.thenBlock(i + 1);
            
            // Create a new IfNode for the elif
            IfNode elifNode = ASTFactory.createIf(elifCondition);
            
            // Build the elif's then-block
            for (CoderiveParser.StatementContext stmt : elifThenBlock.statement()) {
                elifNode.thenBlock.statements.add(buildStatement(stmt));
            }
            DebugSystem.debug("AST", "Built elif-block #" + (i+1) + " with " + elifThenBlock.statement().size() + " statements");

            // Add this new elifNode as the single statement inside the
            // 'else' block of the *previous* node.
            currentNode.elseBlock.statements.add(elifNode);
            
            // The new elifNode becomes the one we attach the *next* 'else' to.
            currentNode = elifNode;
        }

        // 4. Check for a final ELSE clause (which could be 'else if' or 'else')
        if (ctx.ELSE() != null) {
            if (ctx.ifStatement() != null) {
                // This is an 'else if'
                DebugSystem.debug("AST", "Else contains another if (else-if)");
                StatementNode elseIfNode = buildIfStatement(ctx.ifStatement());
                // Attach the entire 'else if' chain to the last node's elseBlock
                currentNode.elseBlock.statements.add(elseIfNode);
            } else if (ctx.elseBlock() != null) {
                // This is a final 'else'
                DebugSystem.debug("AST", "Else contains regular else block");
                for (CoderiveParser.StatementContext stmt : ctx.elseBlock().statement()) {
                    currentNode.elseBlock.statements.add(buildStatement(stmt));
                }
                DebugSystem.debug(
                        "AST",
                        "Else-block has " + ctx.elseBlock().statement().size() + " statements");
            }
        }

        // Return the head of the entire nested structure
        return rootIfNode;
    }

    public BlockNode buildThenBlock(CoderiveParser.ThenBlockContext ctx) {
        BlockNode block = ASTFactory.createBlock();
        for (CoderiveParser.StatementContext stmt : ctx.statement()) {
            block.statements.add(buildStatement(stmt));
        }
        return block;
    }

    public BlockNode buildElseBlock(CoderiveParser.ElseBlockContext ctx) {
        BlockNode block = ASTFactory.createBlock();
        for (CoderiveParser.StatementContext stmt : ctx.statement()) {
            block.statements.add(buildStatement(stmt));
        }
        return block;
    }

    public StatementNode buildVariableDeclaration(CoderiveParser.VariableDeclarationContext ctx) {
        if (ctx.VAR() != null) {
            VarNode var = ASTFactory.createVar(ctx.ID().getText(), null);
            if (ctx.expr() != null) var.value = buildExpression(ctx.expr());
            return var;
        } else {
            FieldNode f = ASTFactory.createField(ctx.ID().getText(), ctx.type().getText());
            if (ctx.expr() != null) f.value = buildExpression(ctx.expr());
            return f;
        }
    }

    public ExprNode buildIndexAccess(CoderiveParser.IndexAccessContext ctx) {
        // For assignment targets: arr[0] or arr[0][1]
        IndexAccessNode current = ASTFactory.createIndexAccess(null, null);

        // Start with the array identifier
        current.array = ASTFactory.createIdentifier(ctx.ID().getText());

        // Handle multiple index expressions
        for (int i = 0; i < ctx.expr().size(); i++) {
            if (i > 0) {
                // Nested index access (multi-dimensional)
                IndexAccessNode next = ASTFactory.createIndexAccess(current, null);
                next.index = buildExpression(ctx.expr(i));
                current = next;
            } else {
                // First index
                current.index = buildExpression(ctx.expr(i));
            }
        }

        return current;
    }

    public ExprNode buildIndexAccessExpr(CoderiveParser.IndexAccessExprContext ctx) {
        // Handle multiple index accesses like arr[0][1]
        ExprNode current = buildAtom(ctx.atom());

        // ctx.expr() returns the list of index expressions inside the brackets
        for (int i = 0; i < ctx.expr().size(); i++) {
            IndexAccessNode indexAccess = ASTFactory.createIndexAccess(current, null);
            indexAccess.index = buildExpression(ctx.expr(i));
            current = indexAccess;
        }

        return current;
    }

    public ExprNode buildArrayLiteralExpr(CoderiveParser.ArrayLiteralExprContext ctx) {
        List<ExprNode> elements = new ArrayList<ExprNode>();
        
        if (ctx.arrayLiteral().exprList() != null) {
            for (CoderiveParser.ExprContext exprCtx : ctx.arrayLiteral().exprList().expr()) {
                ExprNode element = buildExpression(exprCtx);
                elements.add(element);
            }
        }

        return ASTFactory.createArray(elements);
    }

    // NEW: Handle type with arrays
    public ExprNode buildType(CoderiveParser.TypeContext ctx) {
        // Build type string including array brackets
        StringBuilder typeName = new StringBuilder();
        typeName.append(ctx.simpleType().getText());

        // Add array brackets for each dimension
        for (int i = 0; i < ctx.LBRACKET().size(); i++) {
            typeName.append("[]");
        }

        return ASTFactory.createIdentifier(typeName.toString());
    }

    public StatementNode buildExpressionStatement(CoderiveParser.ExpressionStatementContext ctx) {
        return (StatementNode) buildExpression(ctx.expr());
    }

    public ExprNode buildPrimaryExpr(CoderiveParser.PrimaryExprContext ctx) {
        return buildPrimary(ctx.primary());
    }

    public ExprNode buildAdditiveExpr(CoderiveParser.AdditiveExprContext ctx) {
        ExprNode left = buildExpression(ctx.expr(0));
        ExprNode right = buildExpression(ctx.expr(1));
        String op = ctx.getChild(1).getText();
        return ASTFactory.createBinaryOp(left, op, right);
    }

    public ExprNode buildMultiplicativeExpr(CoderiveParser.MultiplicativeExprContext ctx) {
        ExprNode left = buildExpression(ctx.expr(0));
        ExprNode right = buildExpression(ctx.expr(1));
        String op = ctx.getChild(1).getText();
        return ASTFactory.createBinaryOp(left, op, right);
    }

    public ExprNode buildMethodCallExpr(CoderiveParser.MethodCallExprContext ctx) {
        return (ExprNode) buildMethodCall(ctx.methodCall());
    }

    public ExprNode buildIdentifierExpr(CoderiveParser.IdentifierExprContext ctx) {
        return ASTFactory.createIdentifier(ctx.ID().getText());
    }

    public ExprNode buildIntLiteralExpr(CoderiveParser.IntLiteralExprContext ctx) {
        int value = Integer.parseInt(ctx.INT_LIT().getText());
        return ASTFactory.createIntLiteral(value);
    }

    public ExprNode buildFloatLiteralExpr(CoderiveParser.FloatLiteralExprContext ctx) {
        float value = Float.parseFloat(ctx.FLOAT_LIT().getText());
        return ASTFactory.createFloatLiteral(value);
    }

    public ExprNode buildStringLiteralExpr(CoderiveParser.StringLiteralExprContext ctx) {
        String value = ctx.STRING_LIT().getText();
        return ASTFactory.createStringLiteral(value);
    }

    public ExprNode buildBoolLiteralExpr(CoderiveParser.BoolLiteralExprContext ctx) {
        boolean value = Boolean.parseBoolean(ctx.BOOL_LIT().getText());
        return ASTFactory.createBoolLiteral(value);
    }

    public ExprNode buildParenthesizedExpr(CoderiveParser.ParenthesizedExprContext ctx) {
        return buildExpression(ctx.expr());
    }

    // Helper methods for dispatching
    private ASTNode buildTypeBody(CoderiveParser.TypeBodyContext ctx) {
        if (ctx.fieldDeclaration() != null) return buildField(ctx.fieldDeclaration());
        if (ctx.constructor() != null) return buildConstructor(ctx.constructor());
        if (ctx.methodDeclaration() != null) return buildMethod(ctx.methodDeclaration());
        if (ctx.statement() != null) return buildStatement(ctx.statement());
        return null;
    }

    private StatementNode buildStatement(CoderiveParser.StatementContext ctx) {
        if (ctx.variableDeclaration() != null) return buildVariableDeclaration(ctx.variableDeclaration());
        if (ctx.assignment() != null) return buildAssignment(ctx.assignment());
        if (ctx.returnSlotAssignment() != null) return buildReturnSlotAssignment(ctx.returnSlotAssignment());
        if (ctx.inputAssignment() != null) return buildInputAssignment(ctx.inputAssignment());
        if (ctx.methodCallStatement() != null) return buildMethodCall(ctx.methodCallStatement().methodCall());
        if (ctx.outputStatement() != null) return buildOutputStatement(ctx.outputStatement());
        if (ctx.ifStatement() != null) return buildIfStatement(ctx.ifStatement());
        if (ctx.forStatement() != null) return buildForStatement(ctx.forStatement());
        if (ctx.expressionStatement() != null) return buildExpressionStatement(ctx.expressionStatement());
        if (ctx.slotMethodCallStatement() != null) return buildSlotMethodCallStatement(ctx.slotMethodCallStatement());
        return null;
    }

    private StatementNode buildOutputStatement(CoderiveParser.OutputStatementContext ctx) {
        CoderiveParser.OutputTargetContext target = ctx.outputTarget();
        if (target instanceof CoderiveParser.OutputSlotCallContext) 
            return buildOutputSlotCall((CoderiveParser.OutputSlotCallContext) target);
        if (target instanceof CoderiveParser.OutputNamedAssignmentContext) 
            return buildOutputNamedAssignment((CoderiveParser.OutputNamedAssignmentContext) target);
        if (target instanceof CoderiveParser.OutputExpressionContext) 
            return buildOutputExpression((CoderiveParser.OutputExpressionContext) target);
        return null;
    }

    private ExprNode buildExpression(CoderiveParser.ExprContext ctx) {
        if (ctx instanceof CoderiveParser.UnaryExprContext) return buildUnaryExpr((CoderiveParser.UnaryExprContext) ctx);
        if (ctx instanceof CoderiveParser.MultiplicativeExprContext) return buildMultiplicativeExpr((CoderiveParser.MultiplicativeExprContext) ctx);
        if (ctx instanceof CoderiveParser.AdditiveExprContext) return buildAdditiveExpr((CoderiveParser.AdditiveExprContext) ctx);
        if (ctx instanceof CoderiveParser.ComparisonExprContext) return buildComparisonExpr((CoderiveParser.ComparisonExprContext) ctx);
        if (ctx instanceof CoderiveParser.PrimaryExprContext) return buildPrimaryExpr((CoderiveParser.PrimaryExprContext) ctx);
        if (ctx instanceof CoderiveParser.MethodCallExprContext) return buildMethodCallExpr((CoderiveParser.MethodCallExprContext) ctx);
        if (ctx instanceof CoderiveParser.TypeCastExprContext) return buildTypeCast((CoderiveParser.TypeCastContext) ((CoderiveParser.TypeCastExprContext) ctx).typeCast());
        return null;
    }

    private ExprNode buildPrimary(CoderiveParser.PrimaryContext ctx) {
        if (ctx instanceof CoderiveParser.IndexAccessExprContext) return buildIndexAccessExpr((CoderiveParser.IndexAccessExprContext) ctx);
        // For other primary types, we need to handle them via the atom
        return null;
    }

    private ExprNode buildAtom(CoderiveParser.AtomContext ctx) {
        if (ctx instanceof CoderiveParser.IdentifierExprContext) return buildIdentifierExpr((CoderiveParser.IdentifierExprContext) ctx);
        if (ctx instanceof CoderiveParser.IntLiteralExprContext) return buildIntLiteralExpr((CoderiveParser.IntLiteralExprContext) ctx);
        if (ctx instanceof CoderiveParser.FloatLiteralExprContext) return buildFloatLiteralExpr((CoderiveParser.FloatLiteralExprContext) ctx);
        if (ctx instanceof CoderiveParser.StringLiteralExprContext) return buildStringLiteralExpr((CoderiveParser.StringLiteralExprContext) ctx);
        if (ctx instanceof CoderiveParser.BoolLiteralExprContext) return buildBoolLiteralExpr((CoderiveParser.BoolLiteralExprContext) ctx);
        if (ctx instanceof CoderiveParser.ParenthesizedExprContext) return buildParenthesizedExpr((CoderiveParser.ParenthesizedExprContext) ctx);
        if (ctx instanceof CoderiveParser.ArrayLiteralExprContext) return buildArrayLiteralExpr((CoderiveParser.ArrayLiteralExprContext) ctx);
        return null;
    }
}
