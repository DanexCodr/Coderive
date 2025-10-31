// Generated from CoderiveParser.g4 by ANTLR 4.13.2

package cdrv.ast;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link CoderiveParser}.
 */
public interface CoderiveParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#program}.
	 * @param ctx the parse tree
	 */
	void enterProgram(CoderiveParser.ProgramContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#program}.
	 * @param ctx the parse tree
	 */
	void exitProgram(CoderiveParser.ProgramContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#unitDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterUnitDeclaration(CoderiveParser.UnitDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#unitDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitUnitDeclaration(CoderiveParser.UnitDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#qualifiedNameList}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedNameList(CoderiveParser.QualifiedNameListContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#qualifiedNameList}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedNameList(CoderiveParser.QualifiedNameListContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedName(CoderiveParser.QualifiedNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedName(CoderiveParser.QualifiedNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#typeDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterTypeDeclaration(CoderiveParser.TypeDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#typeDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitTypeDeclaration(CoderiveParser.TypeDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#modifiers}.
	 * @param ctx the parse tree
	 */
	void enterModifiers(CoderiveParser.ModifiersContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#modifiers}.
	 * @param ctx the parse tree
	 */
	void exitModifiers(CoderiveParser.ModifiersContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#typeBody}.
	 * @param ctx the parse tree
	 */
	void enterTypeBody(CoderiveParser.TypeBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#typeBody}.
	 * @param ctx the parse tree
	 */
	void exitTypeBody(CoderiveParser.TypeBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#fieldDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterFieldDeclaration(CoderiveParser.FieldDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#fieldDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitFieldDeclaration(CoderiveParser.FieldDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#constructor}.
	 * @param ctx the parse tree
	 */
	void enterConstructor(CoderiveParser.ConstructorContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#constructor}.
	 * @param ctx the parse tree
	 */
	void exitConstructor(CoderiveParser.ConstructorContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#methodDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterMethodDeclaration(CoderiveParser.MethodDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#methodDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitMethodDeclaration(CoderiveParser.MethodDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#idList}.
	 * @param ctx the parse tree
	 */
	void enterIdList(CoderiveParser.IdListContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#idList}.
	 * @param ctx the parse tree
	 */
	void exitIdList(CoderiveParser.IdListContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#slotList}.
	 * @param ctx the parse tree
	 */
	void enterSlotList(CoderiveParser.SlotListContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#slotList}.
	 * @param ctx the parse tree
	 */
	void exitSlotList(CoderiveParser.SlotListContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#parameterList}.
	 * @param ctx the parse tree
	 */
	void enterParameterList(CoderiveParser.ParameterListContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#parameterList}.
	 * @param ctx the parse tree
	 */
	void exitParameterList(CoderiveParser.ParameterListContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#parameter}.
	 * @param ctx the parse tree
	 */
	void enterParameter(CoderiveParser.ParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#parameter}.
	 * @param ctx the parse tree
	 */
	void exitParameter(CoderiveParser.ParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(CoderiveParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(CoderiveParser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#simpleType}.
	 * @param ctx the parse tree
	 */
	void enterSimpleType(CoderiveParser.SimpleTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#simpleType}.
	 * @param ctx the parse tree
	 */
	void exitSimpleType(CoderiveParser.SimpleTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(CoderiveParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(CoderiveParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#variableDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterVariableDeclaration(CoderiveParser.VariableDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#variableDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitVariableDeclaration(CoderiveParser.VariableDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#assignment}.
	 * @param ctx the parse tree
	 */
	void enterAssignment(CoderiveParser.AssignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#assignment}.
	 * @param ctx the parse tree
	 */
	void exitAssignment(CoderiveParser.AssignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#expressionStatement}.
	 * @param ctx the parse tree
	 */
	void enterExpressionStatement(CoderiveParser.ExpressionStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#expressionStatement}.
	 * @param ctx the parse tree
	 */
	void exitExpressionStatement(CoderiveParser.ExpressionStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#assignable}.
	 * @param ctx the parse tree
	 */
	void enterAssignable(CoderiveParser.AssignableContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#assignable}.
	 * @param ctx the parse tree
	 */
	void exitAssignable(CoderiveParser.AssignableContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#returnSlotAssignment}.
	 * @param ctx the parse tree
	 */
	void enterReturnSlotAssignment(CoderiveParser.ReturnSlotAssignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#returnSlotAssignment}.
	 * @param ctx the parse tree
	 */
	void exitReturnSlotAssignment(CoderiveParser.ReturnSlotAssignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#assignableList}.
	 * @param ctx the parse tree
	 */
	void enterAssignableList(CoderiveParser.AssignableListContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#assignableList}.
	 * @param ctx the parse tree
	 */
	void exitAssignableList(CoderiveParser.AssignableListContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#slotMethodCallStatement}.
	 * @param ctx the parse tree
	 */
	void enterSlotMethodCallStatement(CoderiveParser.SlotMethodCallStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#slotMethodCallStatement}.
	 * @param ctx the parse tree
	 */
	void exitSlotMethodCallStatement(CoderiveParser.SlotMethodCallStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#slotMethodCall}.
	 * @param ctx the parse tree
	 */
	void enterSlotMethodCall(CoderiveParser.SlotMethodCallContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#slotMethodCall}.
	 * @param ctx the parse tree
	 */
	void exitSlotMethodCall(CoderiveParser.SlotMethodCallContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#slotCast}.
	 * @param ctx the parse tree
	 */
	void enterSlotCast(CoderiveParser.SlotCastContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#slotCast}.
	 * @param ctx the parse tree
	 */
	void exitSlotCast(CoderiveParser.SlotCastContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#inputAssignment}.
	 * @param ctx the parse tree
	 */
	void enterInputAssignment(CoderiveParser.InputAssignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#inputAssignment}.
	 * @param ctx the parse tree
	 */
	void exitInputAssignment(CoderiveParser.InputAssignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#inputStatement}.
	 * @param ctx the parse tree
	 */
	void enterInputStatement(CoderiveParser.InputStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#inputStatement}.
	 * @param ctx the parse tree
	 */
	void exitInputStatement(CoderiveParser.InputStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#typeInput}.
	 * @param ctx the parse tree
	 */
	void enterTypeInput(CoderiveParser.TypeInputContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#typeInput}.
	 * @param ctx the parse tree
	 */
	void exitTypeInput(CoderiveParser.TypeInputContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#outputStatement}.
	 * @param ctx the parse tree
	 */
	void enterOutputStatement(CoderiveParser.OutputStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#outputStatement}.
	 * @param ctx the parse tree
	 */
	void exitOutputStatement(CoderiveParser.OutputStatementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code outputSlotCall}
	 * labeled alternative in {@link CoderiveParser#outputTarget}.
	 * @param ctx the parse tree
	 */
	void enterOutputSlotCall(CoderiveParser.OutputSlotCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code outputSlotCall}
	 * labeled alternative in {@link CoderiveParser#outputTarget}.
	 * @param ctx the parse tree
	 */
	void exitOutputSlotCall(CoderiveParser.OutputSlotCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code outputNamedAssignment}
	 * labeled alternative in {@link CoderiveParser#outputTarget}.
	 * @param ctx the parse tree
	 */
	void enterOutputNamedAssignment(CoderiveParser.OutputNamedAssignmentContext ctx);
	/**
	 * Exit a parse tree produced by the {@code outputNamedAssignment}
	 * labeled alternative in {@link CoderiveParser#outputTarget}.
	 * @param ctx the parse tree
	 */
	void exitOutputNamedAssignment(CoderiveParser.OutputNamedAssignmentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code outputExpression}
	 * labeled alternative in {@link CoderiveParser#outputTarget}.
	 * @param ctx the parse tree
	 */
	void enterOutputExpression(CoderiveParser.OutputExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code outputExpression}
	 * labeled alternative in {@link CoderiveParser#outputTarget}.
	 * @param ctx the parse tree
	 */
	void exitOutputExpression(CoderiveParser.OutputExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#methodCallStatement}.
	 * @param ctx the parse tree
	 */
	void enterMethodCallStatement(CoderiveParser.MethodCallStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#methodCallStatement}.
	 * @param ctx the parse tree
	 */
	void exitMethodCallStatement(CoderiveParser.MethodCallStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#methodCall}.
	 * @param ctx the parse tree
	 */
	void enterMethodCall(CoderiveParser.MethodCallContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#methodCall}.
	 * @param ctx the parse tree
	 */
	void exitMethodCall(CoderiveParser.MethodCallContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#argumentList}.
	 * @param ctx the parse tree
	 */
	void enterArgumentList(CoderiveParser.ArgumentListContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#argumentList}.
	 * @param ctx the parse tree
	 */
	void exitArgumentList(CoderiveParser.ArgumentListContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#ifStatement}.
	 * @param ctx the parse tree
	 */
	void enterIfStatement(CoderiveParser.IfStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#ifStatement}.
	 * @param ctx the parse tree
	 */
	void exitIfStatement(CoderiveParser.IfStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#thenBlock}.
	 * @param ctx the parse tree
	 */
	void enterThenBlock(CoderiveParser.ThenBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#thenBlock}.
	 * @param ctx the parse tree
	 */
	void exitThenBlock(CoderiveParser.ThenBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#elseBlock}.
	 * @param ctx the parse tree
	 */
	void enterElseBlock(CoderiveParser.ElseBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#elseBlock}.
	 * @param ctx the parse tree
	 */
	void exitElseBlock(CoderiveParser.ElseBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#forStatement}.
	 * @param ctx the parse tree
	 */
	void enterForStatement(CoderiveParser.ForStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#forStatement}.
	 * @param ctx the parse tree
	 */
	void exitForStatement(CoderiveParser.ForStatementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code operatorStep}
	 * labeled alternative in {@link CoderiveParser#forStepExpr}.
	 * @param ctx the parse tree
	 */
	void enterOperatorStep(CoderiveParser.OperatorStepContext ctx);
	/**
	 * Exit a parse tree produced by the {@code operatorStep}
	 * labeled alternative in {@link CoderiveParser#forStepExpr}.
	 * @param ctx the parse tree
	 */
	void exitOperatorStep(CoderiveParser.OperatorStepContext ctx);
	/**
	 * Enter a parse tree produced by the {@code regularStep}
	 * labeled alternative in {@link CoderiveParser#forStepExpr}.
	 * @param ctx the parse tree
	 */
	void enterRegularStep(CoderiveParser.RegularStepContext ctx);
	/**
	 * Exit a parse tree produced by the {@code regularStep}
	 * labeled alternative in {@link CoderiveParser#forStepExpr}.
	 * @param ctx the parse tree
	 */
	void exitRegularStep(CoderiveParser.RegularStepContext ctx);
	/**
	 * Enter a parse tree produced by the {@code methodCallExpr}
	 * labeled alternative in {@link CoderiveParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterMethodCallExpr(CoderiveParser.MethodCallExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code methodCallExpr}
	 * labeled alternative in {@link CoderiveParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitMethodCallExpr(CoderiveParser.MethodCallExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unaryExpr}
	 * labeled alternative in {@link CoderiveParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterUnaryExpr(CoderiveParser.UnaryExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unaryExpr}
	 * labeled alternative in {@link CoderiveParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitUnaryExpr(CoderiveParser.UnaryExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code primaryExpr}
	 * labeled alternative in {@link CoderiveParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterPrimaryExpr(CoderiveParser.PrimaryExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code primaryExpr}
	 * labeled alternative in {@link CoderiveParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitPrimaryExpr(CoderiveParser.PrimaryExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code comparisonExpr}
	 * labeled alternative in {@link CoderiveParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterComparisonExpr(CoderiveParser.ComparisonExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code comparisonExpr}
	 * labeled alternative in {@link CoderiveParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitComparisonExpr(CoderiveParser.ComparisonExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code typeCastExpr}
	 * labeled alternative in {@link CoderiveParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterTypeCastExpr(CoderiveParser.TypeCastExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code typeCastExpr}
	 * labeled alternative in {@link CoderiveParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitTypeCastExpr(CoderiveParser.TypeCastExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code additiveExpr}
	 * labeled alternative in {@link CoderiveParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterAdditiveExpr(CoderiveParser.AdditiveExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code additiveExpr}
	 * labeled alternative in {@link CoderiveParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitAdditiveExpr(CoderiveParser.AdditiveExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code multiplicativeExpr}
	 * labeled alternative in {@link CoderiveParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterMultiplicativeExpr(CoderiveParser.MultiplicativeExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code multiplicativeExpr}
	 * labeled alternative in {@link CoderiveParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitMultiplicativeExpr(CoderiveParser.MultiplicativeExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code indexAccessExpr}
	 * labeled alternative in {@link CoderiveParser#primary}.
	 * @param ctx the parse tree
	 */
	void enterIndexAccessExpr(CoderiveParser.IndexAccessExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code indexAccessExpr}
	 * labeled alternative in {@link CoderiveParser#primary}.
	 * @param ctx the parse tree
	 */
	void exitIndexAccessExpr(CoderiveParser.IndexAccessExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code identifierExpr}
	 * labeled alternative in {@link CoderiveParser#atom}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierExpr(CoderiveParser.IdentifierExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code identifierExpr}
	 * labeled alternative in {@link CoderiveParser#atom}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierExpr(CoderiveParser.IdentifierExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code intLiteralExpr}
	 * labeled alternative in {@link CoderiveParser#atom}.
	 * @param ctx the parse tree
	 */
	void enterIntLiteralExpr(CoderiveParser.IntLiteralExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code intLiteralExpr}
	 * labeled alternative in {@link CoderiveParser#atom}.
	 * @param ctx the parse tree
	 */
	void exitIntLiteralExpr(CoderiveParser.IntLiteralExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code floatLiteralExpr}
	 * labeled alternative in {@link CoderiveParser#atom}.
	 * @param ctx the parse tree
	 */
	void enterFloatLiteralExpr(CoderiveParser.FloatLiteralExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code floatLiteralExpr}
	 * labeled alternative in {@link CoderiveParser#atom}.
	 * @param ctx the parse tree
	 */
	void exitFloatLiteralExpr(CoderiveParser.FloatLiteralExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code stringLiteralExpr}
	 * labeled alternative in {@link CoderiveParser#atom}.
	 * @param ctx the parse tree
	 */
	void enterStringLiteralExpr(CoderiveParser.StringLiteralExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code stringLiteralExpr}
	 * labeled alternative in {@link CoderiveParser#atom}.
	 * @param ctx the parse tree
	 */
	void exitStringLiteralExpr(CoderiveParser.StringLiteralExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code boolLiteralExpr}
	 * labeled alternative in {@link CoderiveParser#atom}.
	 * @param ctx the parse tree
	 */
	void enterBoolLiteralExpr(CoderiveParser.BoolLiteralExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code boolLiteralExpr}
	 * labeled alternative in {@link CoderiveParser#atom}.
	 * @param ctx the parse tree
	 */
	void exitBoolLiteralExpr(CoderiveParser.BoolLiteralExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parenthesizedExpr}
	 * labeled alternative in {@link CoderiveParser#atom}.
	 * @param ctx the parse tree
	 */
	void enterParenthesizedExpr(CoderiveParser.ParenthesizedExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parenthesizedExpr}
	 * labeled alternative in {@link CoderiveParser#atom}.
	 * @param ctx the parse tree
	 */
	void exitParenthesizedExpr(CoderiveParser.ParenthesizedExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arrayLiteralExpr}
	 * labeled alternative in {@link CoderiveParser#atom}.
	 * @param ctx the parse tree
	 */
	void enterArrayLiteralExpr(CoderiveParser.ArrayLiteralExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arrayLiteralExpr}
	 * labeled alternative in {@link CoderiveParser#atom}.
	 * @param ctx the parse tree
	 */
	void exitArrayLiteralExpr(CoderiveParser.ArrayLiteralExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#arrayLiteral}.
	 * @param ctx the parse tree
	 */
	void enterArrayLiteral(CoderiveParser.ArrayLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#arrayLiteral}.
	 * @param ctx the parse tree
	 */
	void exitArrayLiteral(CoderiveParser.ArrayLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#exprList}.
	 * @param ctx the parse tree
	 */
	void enterExprList(CoderiveParser.ExprListContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#exprList}.
	 * @param ctx the parse tree
	 */
	void exitExprList(CoderiveParser.ExprListContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#indexAccess}.
	 * @param ctx the parse tree
	 */
	void enterIndexAccess(CoderiveParser.IndexAccessContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#indexAccess}.
	 * @param ctx the parse tree
	 */
	void exitIndexAccess(CoderiveParser.IndexAccessContext ctx);
	/**
	 * Enter a parse tree produced by {@link CoderiveParser#typeCast}.
	 * @param ctx the parse tree
	 */
	void enterTypeCast(CoderiveParser.TypeCastContext ctx);
	/**
	 * Exit a parse tree produced by {@link CoderiveParser#typeCast}.
	 * @param ctx the parse tree
	 */
	void exitTypeCast(CoderiveParser.TypeCastContext ctx);
}