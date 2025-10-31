// Generated from CoderiveParser.g4 by ANTLR 4.13.2

package cdrv.ast;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link CoderiveParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface CoderiveParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#program}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgram(CoderiveParser.ProgramContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#unitDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnitDeclaration(CoderiveParser.UnitDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#qualifiedNameList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedNameList(CoderiveParser.QualifiedNameListContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#qualifiedName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedName(CoderiveParser.QualifiedNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#typeDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeDeclaration(CoderiveParser.TypeDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#modifiers}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModifiers(CoderiveParser.ModifiersContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#typeBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeBody(CoderiveParser.TypeBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#fieldDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFieldDeclaration(CoderiveParser.FieldDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#constructor}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructor(CoderiveParser.ConstructorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#methodDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodDeclaration(CoderiveParser.MethodDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#idList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdList(CoderiveParser.IdListContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#slotList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSlotList(CoderiveParser.SlotListContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#parameterList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameterList(CoderiveParser.ParameterListContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#parameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter(CoderiveParser.ParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType(CoderiveParser.TypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#simpleType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleType(CoderiveParser.SimpleTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(CoderiveParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#variableDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDeclaration(CoderiveParser.VariableDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#assignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment(CoderiveParser.AssignmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#expressionStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionStatement(CoderiveParser.ExpressionStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#assignable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignable(CoderiveParser.AssignableContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#returnSlotAssignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnSlotAssignment(CoderiveParser.ReturnSlotAssignmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#assignableList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignableList(CoderiveParser.AssignableListContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#slotMethodCallStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSlotMethodCallStatement(CoderiveParser.SlotMethodCallStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#slotMethodCall}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSlotMethodCall(CoderiveParser.SlotMethodCallContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#slotCast}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSlotCast(CoderiveParser.SlotCastContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#inputAssignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInputAssignment(CoderiveParser.InputAssignmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#inputStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInputStatement(CoderiveParser.InputStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#typeInput}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeInput(CoderiveParser.TypeInputContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#outputStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOutputStatement(CoderiveParser.OutputStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code outputSlotCall}
	 * labeled alternative in {@link CoderiveParser#outputTarget}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOutputSlotCall(CoderiveParser.OutputSlotCallContext ctx);
	/**
	 * Visit a parse tree produced by the {@code outputNamedAssignment}
	 * labeled alternative in {@link CoderiveParser#outputTarget}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOutputNamedAssignment(CoderiveParser.OutputNamedAssignmentContext ctx);
	/**
	 * Visit a parse tree produced by the {@code outputExpression}
	 * labeled alternative in {@link CoderiveParser#outputTarget}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOutputExpression(CoderiveParser.OutputExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#methodCallStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodCallStatement(CoderiveParser.MethodCallStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#methodCall}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodCall(CoderiveParser.MethodCallContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#argumentList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgumentList(CoderiveParser.ArgumentListContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#ifStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfStatement(CoderiveParser.IfStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#thenBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitThenBlock(CoderiveParser.ThenBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#elseBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElseBlock(CoderiveParser.ElseBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#forStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForStatement(CoderiveParser.ForStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code operatorStep}
	 * labeled alternative in {@link CoderiveParser#forStepExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperatorStep(CoderiveParser.OperatorStepContext ctx);
	/**
	 * Visit a parse tree produced by the {@code regularStep}
	 * labeled alternative in {@link CoderiveParser#forStepExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRegularStep(CoderiveParser.RegularStepContext ctx);
	/**
	 * Visit a parse tree produced by the {@code methodCallExpr}
	 * labeled alternative in {@link CoderiveParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodCallExpr(CoderiveParser.MethodCallExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code unaryExpr}
	 * labeled alternative in {@link CoderiveParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryExpr(CoderiveParser.UnaryExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code primaryExpr}
	 * labeled alternative in {@link CoderiveParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimaryExpr(CoderiveParser.PrimaryExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code comparisonExpr}
	 * labeled alternative in {@link CoderiveParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparisonExpr(CoderiveParser.ComparisonExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code typeCastExpr}
	 * labeled alternative in {@link CoderiveParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeCastExpr(CoderiveParser.TypeCastExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code additiveExpr}
	 * labeled alternative in {@link CoderiveParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdditiveExpr(CoderiveParser.AdditiveExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code multiplicativeExpr}
	 * labeled alternative in {@link CoderiveParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplicativeExpr(CoderiveParser.MultiplicativeExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code indexAccessExpr}
	 * labeled alternative in {@link CoderiveParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndexAccessExpr(CoderiveParser.IndexAccessExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code identifierExpr}
	 * labeled alternative in {@link CoderiveParser#atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifierExpr(CoderiveParser.IdentifierExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code intLiteralExpr}
	 * labeled alternative in {@link CoderiveParser#atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntLiteralExpr(CoderiveParser.IntLiteralExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code floatLiteralExpr}
	 * labeled alternative in {@link CoderiveParser#atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFloatLiteralExpr(CoderiveParser.FloatLiteralExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code stringLiteralExpr}
	 * labeled alternative in {@link CoderiveParser#atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringLiteralExpr(CoderiveParser.StringLiteralExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code boolLiteralExpr}
	 * labeled alternative in {@link CoderiveParser#atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBoolLiteralExpr(CoderiveParser.BoolLiteralExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code parenthesizedExpr}
	 * labeled alternative in {@link CoderiveParser#atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenthesizedExpr(CoderiveParser.ParenthesizedExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code arrayLiteralExpr}
	 * labeled alternative in {@link CoderiveParser#atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayLiteralExpr(CoderiveParser.ArrayLiteralExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#arrayLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayLiteral(CoderiveParser.ArrayLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#exprList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprList(CoderiveParser.ExprListContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#indexAccess}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndexAccess(CoderiveParser.IndexAccessContext ctx);
	/**
	 * Visit a parse tree produced by {@link CoderiveParser#typeCast}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeCast(CoderiveParser.TypeCastContext ctx);
}