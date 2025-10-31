// Generated from CoderiveParser.g4 by ANTLR 4.13.2

package cdrv.ast;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class CoderiveParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		SHIP=1, LOCAL=2, UNIT=3, GET=4, EXTEND=5, THIS=6, VAR=7, OUTPUT=8, INPUT=9, 
		IF=10, ELSE=11, ELIF=12, FOR=13, BY=14, IN=15, TO=16, INT=17, STRING=18, 
		FLOAT=19, BOOL=20, INT_LIT=21, FLOAT_LIT=22, STRING_LIT=23, BOOL_LIT=24, 
		ID=25, ASSIGN=26, PLUS=27, MINUS=28, MUL=29, DIV=30, MOD=31, COLON=32, 
		GT=33, LT=34, GTE=35, LTE=36, EQ=37, NEQ=38, DOT=39, COMMA=40, LPAREN=41, 
		RPAREN=42, LBRACE=43, RBRACE=44, LBRACKET=45, RBRACKET=46, LINE_COMMENT=47, 
		BLOCK_COMMENT=48, WS=49;
	public static final int
		RULE_program = 0, RULE_unitDeclaration = 1, RULE_qualifiedNameList = 2, 
		RULE_qualifiedName = 3, RULE_typeDeclaration = 4, RULE_modifiers = 5, 
		RULE_typeBody = 6, RULE_fieldDeclaration = 7, RULE_constructor = 8, RULE_methodDeclaration = 9, 
		RULE_idList = 10, RULE_slotList = 11, RULE_parameterList = 12, RULE_parameter = 13, 
		RULE_type = 14, RULE_simpleType = 15, RULE_statement = 16, RULE_variableDeclaration = 17, 
		RULE_assignment = 18, RULE_expressionStatement = 19, RULE_assignable = 20, 
		RULE_returnSlotAssignment = 21, RULE_assignableList = 22, RULE_slotMethodCallStatement = 23, 
		RULE_slotMethodCall = 24, RULE_slotCast = 25, RULE_inputAssignment = 26, 
		RULE_inputStatement = 27, RULE_typeInput = 28, RULE_outputStatement = 29, 
		RULE_outputTarget = 30, RULE_methodCallStatement = 31, RULE_methodCall = 32, 
		RULE_argumentList = 33, RULE_ifStatement = 34, RULE_thenBlock = 35, RULE_elseBlock = 36, 
		RULE_forStatement = 37, RULE_forStepExpr = 38, RULE_expr = 39, RULE_primary = 40, 
		RULE_atom = 41, RULE_arrayLiteral = 42, RULE_exprList = 43, RULE_indexAccess = 44, 
		RULE_typeCast = 45;
	private static String[] makeRuleNames() {
		return new String[] {
			"program", "unitDeclaration", "qualifiedNameList", "qualifiedName", "typeDeclaration", 
			"modifiers", "typeBody", "fieldDeclaration", "constructor", "methodDeclaration", 
			"idList", "slotList", "parameterList", "parameter", "type", "simpleType", 
			"statement", "variableDeclaration", "assignment", "expressionStatement", 
			"assignable", "returnSlotAssignment", "assignableList", "slotMethodCallStatement", 
			"slotMethodCall", "slotCast", "inputAssignment", "inputStatement", "typeInput", 
			"outputStatement", "outputTarget", "methodCallStatement", "methodCall", 
			"argumentList", "ifStatement", "thenBlock", "elseBlock", "forStatement", 
			"forStepExpr", "expr", "primary", "atom", "arrayLiteral", "exprList", 
			"indexAccess", "typeCast"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'ship'", "'local'", "'unit'", "'get'", "'extend'", "'this'", "'var'", 
			"'output'", "'input'", "'if'", "'else'", "'elif'", "'for'", "'by'", "'in'", 
			"'to'", "'int'", "'string'", "'float'", "'bool'", null, null, null, null, 
			null, "'='", "'+'", "'-'", "'*'", "'/'", "'%'", "':'", "'>'", "'<'", 
			"'>='", "'<='", "'=='", "'!='", "'.'", "','", "'('", "')'", "'{'", "'}'", 
			"'['", "']'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "SHIP", "LOCAL", "UNIT", "GET", "EXTEND", "THIS", "VAR", "OUTPUT", 
			"INPUT", "IF", "ELSE", "ELIF", "FOR", "BY", "IN", "TO", "INT", "STRING", 
			"FLOAT", "BOOL", "INT_LIT", "FLOAT_LIT", "STRING_LIT", "BOOL_LIT", "ID", 
			"ASSIGN", "PLUS", "MINUS", "MUL", "DIV", "MOD", "COLON", "GT", "LT", 
			"GTE", "LTE", "EQ", "NEQ", "DOT", "COMMA", "LPAREN", "RPAREN", "LBRACE", 
			"RBRACE", "LBRACKET", "RBRACKET", "LINE_COMMENT", "BLOCK_COMMENT", "WS"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "CoderiveParser.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public CoderiveParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ProgramContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(CoderiveParser.EOF, 0); }
		public UnitDeclarationContext unitDeclaration() {
			return getRuleContext(UnitDeclarationContext.class,0);
		}
		public List<TypeDeclarationContext> typeDeclaration() {
			return getRuleContexts(TypeDeclarationContext.class);
		}
		public TypeDeclarationContext typeDeclaration(int i) {
			return getRuleContext(TypeDeclarationContext.class,i);
		}
		public ProgramContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_program; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterProgram(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitProgram(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitProgram(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ProgramContext program() throws RecognitionException {
		ProgramContext _localctx = new ProgramContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_program);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(93);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==UNIT) {
				{
				setState(92);
				unitDeclaration();
				}
			}

			setState(98);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 33554438L) != 0)) {
				{
				{
				setState(95);
				typeDeclaration();
				}
				}
				setState(100);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(101);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class UnitDeclarationContext extends ParserRuleContext {
		public TerminalNode UNIT() { return getToken(CoderiveParser.UNIT, 0); }
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public TerminalNode GET() { return getToken(CoderiveParser.GET, 0); }
		public TerminalNode LBRACE() { return getToken(CoderiveParser.LBRACE, 0); }
		public QualifiedNameListContext qualifiedNameList() {
			return getRuleContext(QualifiedNameListContext.class,0);
		}
		public TerminalNode RBRACE() { return getToken(CoderiveParser.RBRACE, 0); }
		public UnitDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unitDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterUnitDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitUnitDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitUnitDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UnitDeclarationContext unitDeclaration() throws RecognitionException {
		UnitDeclarationContext _localctx = new UnitDeclarationContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_unitDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(103);
			match(UNIT);
			setState(104);
			qualifiedName();
			setState(110);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==GET) {
				{
				setState(105);
				match(GET);
				setState(106);
				match(LBRACE);
				setState(107);
				qualifiedNameList();
				setState(108);
				match(RBRACE);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class QualifiedNameListContext extends ParserRuleContext {
		public List<QualifiedNameContext> qualifiedName() {
			return getRuleContexts(QualifiedNameContext.class);
		}
		public QualifiedNameContext qualifiedName(int i) {
			return getRuleContext(QualifiedNameContext.class,i);
		}
		public QualifiedNameListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_qualifiedNameList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterQualifiedNameList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitQualifiedNameList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitQualifiedNameList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QualifiedNameListContext qualifiedNameList() throws RecognitionException {
		QualifiedNameListContext _localctx = new QualifiedNameListContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_qualifiedNameList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(113); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(112);
				qualifiedName();
				}
				}
				setState(115); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==ID );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class QualifiedNameContext extends ParserRuleContext {
		public List<TerminalNode> ID() { return getTokens(CoderiveParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(CoderiveParser.ID, i);
		}
		public List<TerminalNode> DOT() { return getTokens(CoderiveParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(CoderiveParser.DOT, i);
		}
		public QualifiedNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_qualifiedName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterQualifiedName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitQualifiedName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitQualifiedName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QualifiedNameContext qualifiedName() throws RecognitionException {
		QualifiedNameContext _localctx = new QualifiedNameContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_qualifiedName);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(117);
			match(ID);
			setState(122);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(118);
					match(DOT);
					setState(119);
					match(ID);
					}
					} 
				}
				setState(124);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeDeclarationContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(CoderiveParser.ID, 0); }
		public TerminalNode LBRACE() { return getToken(CoderiveParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(CoderiveParser.RBRACE, 0); }
		public ModifiersContext modifiers() {
			return getRuleContext(ModifiersContext.class,0);
		}
		public TerminalNode EXTEND() { return getToken(CoderiveParser.EXTEND, 0); }
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public List<TypeBodyContext> typeBody() {
			return getRuleContexts(TypeBodyContext.class);
		}
		public TypeBodyContext typeBody(int i) {
			return getRuleContext(TypeBodyContext.class,i);
		}
		public TypeDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterTypeDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitTypeDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitTypeDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeDeclarationContext typeDeclaration() throws RecognitionException {
		TypeDeclarationContext _localctx = new TypeDeclarationContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_typeDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(126);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SHIP || _la==LOCAL) {
				{
				setState(125);
				modifiers();
				}
			}

			setState(128);
			match(ID);
			setState(131);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==EXTEND) {
				{
				setState(129);
				match(EXTEND);
				setState(130);
				qualifiedName();
				}
			}

			setState(133);
			match(LBRACE);
			setState(137);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 37383864984966L) != 0)) {
				{
				{
				setState(134);
				typeBody();
				}
				}
				setState(139);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(140);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ModifiersContext extends ParserRuleContext {
		public TerminalNode SHIP() { return getToken(CoderiveParser.SHIP, 0); }
		public TerminalNode LOCAL() { return getToken(CoderiveParser.LOCAL, 0); }
		public ModifiersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_modifiers; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterModifiers(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitModifiers(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitModifiers(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ModifiersContext modifiers() throws RecognitionException {
		ModifiersContext _localctx = new ModifiersContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_modifiers);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(142);
			_la = _input.LA(1);
			if ( !(_la==SHIP || _la==LOCAL) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeBodyContext extends ParserRuleContext {
		public FieldDeclarationContext fieldDeclaration() {
			return getRuleContext(FieldDeclarationContext.class,0);
		}
		public ConstructorContext constructor() {
			return getRuleContext(ConstructorContext.class,0);
		}
		public MethodDeclarationContext methodDeclaration() {
			return getRuleContext(MethodDeclarationContext.class,0);
		}
		public StatementContext statement() {
			return getRuleContext(StatementContext.class,0);
		}
		public TypeBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterTypeBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitTypeBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitTypeBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeBodyContext typeBody() throws RecognitionException {
		TypeBodyContext _localctx = new TypeBodyContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_typeBody);
		try {
			setState(148);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(144);
				fieldDeclaration();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(145);
				constructor();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(146);
				methodDeclaration();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(147);
				statement();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FieldDeclarationContext extends ParserRuleContext {
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TerminalNode ID() { return getToken(CoderiveParser.ID, 0); }
		public TerminalNode ASSIGN() { return getToken(CoderiveParser.ASSIGN, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public FieldDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fieldDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterFieldDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitFieldDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitFieldDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FieldDeclarationContext fieldDeclaration() throws RecognitionException {
		FieldDeclarationContext _localctx = new FieldDeclarationContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_fieldDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(150);
			type();
			setState(151);
			match(ID);
			setState(154);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ASSIGN) {
				{
				setState(152);
				match(ASSIGN);
				setState(153);
				expr(0);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ConstructorContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(CoderiveParser.ID, 0); }
		public TerminalNode LPAREN() { return getToken(CoderiveParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(CoderiveParser.RPAREN, 0); }
		public TerminalNode LBRACE() { return getToken(CoderiveParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(CoderiveParser.RBRACE, 0); }
		public ParameterListContext parameterList() {
			return getRuleContext(ParameterListContext.class,0);
		}
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public ConstructorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constructor; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterConstructor(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitConstructor(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitConstructor(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstructorContext constructor() throws RecognitionException {
		ConstructorContext _localctx = new ConstructorContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_constructor);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(156);
			match(ID);
			setState(157);
			match(LPAREN);
			setState(159);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 35520512L) != 0)) {
				{
				setState(158);
				parameterList();
				}
			}

			setState(161);
			match(RPAREN);
			setState(162);
			match(LBRACE);
			setState(166);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 37383864984960L) != 0)) {
				{
				{
				setState(163);
				statement();
				}
				}
				setState(168);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(169);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MethodDeclarationContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(CoderiveParser.ID, 0); }
		public TerminalNode LPAREN() { return getToken(CoderiveParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(CoderiveParser.RPAREN, 0); }
		public TerminalNode LBRACE() { return getToken(CoderiveParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(CoderiveParser.RBRACE, 0); }
		public SlotListContext slotList() {
			return getRuleContext(SlotListContext.class,0);
		}
		public ParameterListContext parameterList() {
			return getRuleContext(ParameterListContext.class,0);
		}
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public ModifiersContext modifiers() {
			return getRuleContext(ModifiersContext.class,0);
		}
		public MethodDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_methodDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterMethodDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitMethodDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitMethodDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MethodDeclarationContext methodDeclaration() throws RecognitionException {
		MethodDeclarationContext _localctx = new MethodDeclarationContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_methodDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(181);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
			case 1:
				{
				setState(171);
				slotList();
				setState(173);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==SHIP || _la==LOCAL) {
					{
					setState(172);
					modifiers();
					}
				}

				}
				break;
			case 2:
				{
				setState(176);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==SHIP || _la==LOCAL) {
					{
					setState(175);
					modifiers();
					}
				}

				setState(179);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LBRACKET) {
					{
					setState(178);
					slotList();
					}
				}

				}
				break;
			}
			setState(183);
			match(ID);
			setState(184);
			match(LPAREN);
			setState(186);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 35520512L) != 0)) {
				{
				setState(185);
				parameterList();
				}
			}

			setState(188);
			match(RPAREN);
			setState(189);
			match(LBRACE);
			setState(193);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 37383864984960L) != 0)) {
				{
				{
				setState(190);
				statement();
				}
				}
				setState(195);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(196);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class IdListContext extends ParserRuleContext {
		public List<TerminalNode> ID() { return getTokens(CoderiveParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(CoderiveParser.ID, i);
		}
		public List<TerminalNode> COMMA() { return getTokens(CoderiveParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CoderiveParser.COMMA, i);
		}
		public IdListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_idList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterIdList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitIdList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitIdList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdListContext idList() throws RecognitionException {
		IdListContext _localctx = new IdListContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_idList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(198);
			match(ID);
			setState(201);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,18,_ctx) ) {
			case 1:
				{
				setState(199);
				match(COMMA);
				setState(200);
				match(ID);
				}
				break;
			}
			setState(205);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(203);
				match(COMMA);
				setState(204);
				match(ID);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SlotListContext extends ParserRuleContext {
		public TerminalNode LBRACKET() { return getToken(CoderiveParser.LBRACKET, 0); }
		public IdListContext idList() {
			return getRuleContext(IdListContext.class,0);
		}
		public TerminalNode RBRACKET() { return getToken(CoderiveParser.RBRACKET, 0); }
		public SlotListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_slotList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterSlotList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitSlotList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitSlotList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SlotListContext slotList() throws RecognitionException {
		SlotListContext _localctx = new SlotListContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_slotList);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(207);
			match(LBRACKET);
			setState(208);
			idList();
			setState(209);
			match(RBRACKET);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ParameterListContext extends ParserRuleContext {
		public List<ParameterContext> parameter() {
			return getRuleContexts(ParameterContext.class);
		}
		public ParameterContext parameter(int i) {
			return getRuleContext(ParameterContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(CoderiveParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CoderiveParser.COMMA, i);
		}
		public ParameterListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parameterList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterParameterList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitParameterList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitParameterList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ParameterListContext parameterList() throws RecognitionException {
		ParameterListContext _localctx = new ParameterListContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_parameterList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(211);
			parameter();
			setState(216);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(212);
				match(COMMA);
				setState(213);
				parameter();
				}
				}
				setState(218);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ParameterContext extends ParserRuleContext {
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TerminalNode ID() { return getToken(CoderiveParser.ID, 0); }
		public ParameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parameter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterParameter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitParameter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitParameter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ParameterContext parameter() throws RecognitionException {
		ParameterContext _localctx = new ParameterContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_parameter);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(219);
			type();
			setState(220);
			match(ID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeContext extends ParserRuleContext {
		public SimpleTypeContext simpleType() {
			return getRuleContext(SimpleTypeContext.class,0);
		}
		public List<TerminalNode> LBRACKET() { return getTokens(CoderiveParser.LBRACKET); }
		public TerminalNode LBRACKET(int i) {
			return getToken(CoderiveParser.LBRACKET, i);
		}
		public List<TerminalNode> RBRACKET() { return getTokens(CoderiveParser.RBRACKET); }
		public TerminalNode RBRACKET(int i) {
			return getToken(CoderiveParser.RBRACKET, i);
		}
		public TypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeContext type() throws RecognitionException {
		TypeContext _localctx = new TypeContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_type);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(222);
			simpleType();
			setState(227);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LBRACKET) {
				{
				{
				setState(223);
				match(LBRACKET);
				setState(224);
				match(RBRACKET);
				}
				}
				setState(229);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SimpleTypeContext extends ParserRuleContext {
		public TerminalNode INT() { return getToken(CoderiveParser.INT, 0); }
		public TerminalNode STRING() { return getToken(CoderiveParser.STRING, 0); }
		public TerminalNode FLOAT() { return getToken(CoderiveParser.FLOAT, 0); }
		public TerminalNode BOOL() { return getToken(CoderiveParser.BOOL, 0); }
		public TerminalNode ID() { return getToken(CoderiveParser.ID, 0); }
		public SimpleTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_simpleType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterSimpleType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitSimpleType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitSimpleType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SimpleTypeContext simpleType() throws RecognitionException {
		SimpleTypeContext _localctx = new SimpleTypeContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_simpleType);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(230);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 35520512L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StatementContext extends ParserRuleContext {
		public VariableDeclarationContext variableDeclaration() {
			return getRuleContext(VariableDeclarationContext.class,0);
		}
		public AssignmentContext assignment() {
			return getRuleContext(AssignmentContext.class,0);
		}
		public ReturnSlotAssignmentContext returnSlotAssignment() {
			return getRuleContext(ReturnSlotAssignmentContext.class,0);
		}
		public InputAssignmentContext inputAssignment() {
			return getRuleContext(InputAssignmentContext.class,0);
		}
		public MethodCallStatementContext methodCallStatement() {
			return getRuleContext(MethodCallStatementContext.class,0);
		}
		public OutputStatementContext outputStatement() {
			return getRuleContext(OutputStatementContext.class,0);
		}
		public IfStatementContext ifStatement() {
			return getRuleContext(IfStatementContext.class,0);
		}
		public ForStatementContext forStatement() {
			return getRuleContext(ForStatementContext.class,0);
		}
		public ExpressionStatementContext expressionStatement() {
			return getRuleContext(ExpressionStatementContext.class,0);
		}
		public SlotMethodCallStatementContext slotMethodCallStatement() {
			return getRuleContext(SlotMethodCallStatementContext.class,0);
		}
		public StatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StatementContext statement() throws RecognitionException {
		StatementContext _localctx = new StatementContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_statement);
		try {
			setState(242);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,22,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(232);
				variableDeclaration();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(233);
				assignment();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(234);
				returnSlotAssignment();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(235);
				inputAssignment();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(236);
				methodCallStatement();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(237);
				outputStatement();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(238);
				ifStatement();
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(239);
				forStatement();
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(240);
				expressionStatement();
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(241);
				slotMethodCallStatement();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class VariableDeclarationContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(CoderiveParser.ID, 0); }
		public TerminalNode VAR() { return getToken(CoderiveParser.VAR, 0); }
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TerminalNode ASSIGN() { return getToken(CoderiveParser.ASSIGN, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public VariableDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterVariableDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitVariableDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitVariableDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableDeclarationContext variableDeclaration() throws RecognitionException {
		VariableDeclarationContext _localctx = new VariableDeclarationContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_variableDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(246);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case VAR:
				{
				setState(244);
				match(VAR);
				}
				break;
			case INT:
			case STRING:
			case FLOAT:
			case BOOL:
			case ID:
				{
				setState(245);
				type();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(248);
			match(ID);
			setState(251);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ASSIGN) {
				{
				setState(249);
				match(ASSIGN);
				setState(250);
				expr(0);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AssignmentContext extends ParserRuleContext {
		public AssignableContext assignable() {
			return getRuleContext(AssignableContext.class,0);
		}
		public TerminalNode ASSIGN() { return getToken(CoderiveParser.ASSIGN, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public AssignmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assignment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterAssignment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitAssignment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitAssignment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AssignmentContext assignment() throws RecognitionException {
		AssignmentContext _localctx = new AssignmentContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_assignment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(253);
			assignable();
			setState(254);
			match(ASSIGN);
			setState(255);
			expr(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExpressionStatementContext extends ParserRuleContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public ExpressionStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expressionStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterExpressionStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitExpressionStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitExpressionStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionStatementContext expressionStatement() throws RecognitionException {
		ExpressionStatementContext _localctx = new ExpressionStatementContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_expressionStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(257);
			expr(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AssignableContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(CoderiveParser.ID, 0); }
		public IndexAccessContext indexAccess() {
			return getRuleContext(IndexAccessContext.class,0);
		}
		public AssignableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assignable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterAssignable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitAssignable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitAssignable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AssignableContext assignable() throws RecognitionException {
		AssignableContext _localctx = new AssignableContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_assignable);
		try {
			setState(261);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,25,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(259);
				match(ID);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(260);
				indexAccess();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ReturnSlotAssignmentContext extends ParserRuleContext {
		public AssignableListContext assignableList() {
			return getRuleContext(AssignableListContext.class,0);
		}
		public TerminalNode ASSIGN() { return getToken(CoderiveParser.ASSIGN, 0); }
		public SlotMethodCallContext slotMethodCall() {
			return getRuleContext(SlotMethodCallContext.class,0);
		}
		public ReturnSlotAssignmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_returnSlotAssignment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterReturnSlotAssignment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitReturnSlotAssignment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitReturnSlotAssignment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ReturnSlotAssignmentContext returnSlotAssignment() throws RecognitionException {
		ReturnSlotAssignmentContext _localctx = new ReturnSlotAssignmentContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_returnSlotAssignment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(263);
			assignableList();
			setState(264);
			match(ASSIGN);
			setState(265);
			slotMethodCall();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AssignableListContext extends ParserRuleContext {
		public IdListContext idList() {
			return getRuleContext(IdListContext.class,0);
		}
		public AssignableListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assignableList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterAssignableList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitAssignableList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitAssignableList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AssignableListContext assignableList() throws RecognitionException {
		AssignableListContext _localctx = new AssignableListContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_assignableList);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(267);
			idList();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SlotMethodCallStatementContext extends ParserRuleContext {
		public SlotMethodCallContext slotMethodCall() {
			return getRuleContext(SlotMethodCallContext.class,0);
		}
		public SlotMethodCallStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_slotMethodCallStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterSlotMethodCallStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitSlotMethodCallStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitSlotMethodCallStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SlotMethodCallStatementContext slotMethodCallStatement() throws RecognitionException {
		SlotMethodCallStatementContext _localctx = new SlotMethodCallStatementContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_slotMethodCallStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(269);
			slotMethodCall();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SlotMethodCallContext extends ParserRuleContext {
		public SlotCastContext slotCast() {
			return getRuleContext(SlotCastContext.class,0);
		}
		public MethodCallContext methodCall() {
			return getRuleContext(MethodCallContext.class,0);
		}
		public SlotMethodCallContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_slotMethodCall; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterSlotMethodCall(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitSlotMethodCall(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitSlotMethodCall(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SlotMethodCallContext slotMethodCall() throws RecognitionException {
		SlotMethodCallContext _localctx = new SlotMethodCallContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_slotMethodCall);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(271);
			slotCast();
			setState(272);
			methodCall();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SlotCastContext extends ParserRuleContext {
		public TerminalNode LBRACKET() { return getToken(CoderiveParser.LBRACKET, 0); }
		public IdListContext idList() {
			return getRuleContext(IdListContext.class,0);
		}
		public TerminalNode RBRACKET() { return getToken(CoderiveParser.RBRACKET, 0); }
		public TerminalNode COLON() { return getToken(CoderiveParser.COLON, 0); }
		public SlotCastContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_slotCast; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterSlotCast(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitSlotCast(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitSlotCast(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SlotCastContext slotCast() throws RecognitionException {
		SlotCastContext _localctx = new SlotCastContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_slotCast);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(274);
			match(LBRACKET);
			setState(275);
			idList();
			setState(276);
			match(RBRACKET);
			setState(277);
			match(COLON);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class InputAssignmentContext extends ParserRuleContext {
		public AssignableContext assignable() {
			return getRuleContext(AssignableContext.class,0);
		}
		public TerminalNode ASSIGN() { return getToken(CoderiveParser.ASSIGN, 0); }
		public InputStatementContext inputStatement() {
			return getRuleContext(InputStatementContext.class,0);
		}
		public InputAssignmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inputAssignment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterInputAssignment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitInputAssignment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitInputAssignment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InputAssignmentContext inputAssignment() throws RecognitionException {
		InputAssignmentContext _localctx = new InputAssignmentContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_inputAssignment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(279);
			assignable();
			setState(280);
			match(ASSIGN);
			setState(281);
			inputStatement();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class InputStatementContext extends ParserRuleContext {
		public TypeInputContext typeInput() {
			return getRuleContext(TypeInputContext.class,0);
		}
		public TerminalNode INPUT() { return getToken(CoderiveParser.INPUT, 0); }
		public InputStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inputStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterInputStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitInputStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitInputStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InputStatementContext inputStatement() throws RecognitionException {
		InputStatementContext _localctx = new InputStatementContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_inputStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(283);
			typeInput();
			setState(284);
			match(INPUT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeInputContext extends ParserRuleContext {
		public TerminalNode LPAREN() { return getToken(CoderiveParser.LPAREN, 0); }
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(CoderiveParser.RPAREN, 0); }
		public TypeInputContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeInput; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterTypeInput(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitTypeInput(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitTypeInput(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeInputContext typeInput() throws RecognitionException {
		TypeInputContext _localctx = new TypeInputContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_typeInput);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(286);
			match(LPAREN);
			setState(287);
			type();
			setState(288);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class OutputStatementContext extends ParserRuleContext {
		public TerminalNode OUTPUT() { return getToken(CoderiveParser.OUTPUT, 0); }
		public OutputTargetContext outputTarget() {
			return getRuleContext(OutputTargetContext.class,0);
		}
		public OutputStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_outputStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterOutputStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitOutputStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitOutputStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OutputStatementContext outputStatement() throws RecognitionException {
		OutputStatementContext _localctx = new OutputStatementContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_outputStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(290);
			match(OUTPUT);
			setState(291);
			outputTarget();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class OutputTargetContext extends ParserRuleContext {
		public OutputTargetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_outputTarget; }
	 
		public OutputTargetContext() { }
		public void copyFrom(OutputTargetContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class OutputSlotCallContext extends OutputTargetContext {
		public MethodCallContext methodCall() {
			return getRuleContext(MethodCallContext.class,0);
		}
		public SlotCastContext slotCast() {
			return getRuleContext(SlotCastContext.class,0);
		}
		public OutputSlotCallContext(OutputTargetContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterOutputSlotCall(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitOutputSlotCall(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitOutputSlotCall(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class OutputNamedAssignmentContext extends OutputTargetContext {
		public TerminalNode ID() { return getToken(CoderiveParser.ID, 0); }
		public TerminalNode ASSIGN() { return getToken(CoderiveParser.ASSIGN, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public OutputNamedAssignmentContext(OutputTargetContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterOutputNamedAssignment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitOutputNamedAssignment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitOutputNamedAssignment(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class OutputExpressionContext extends OutputTargetContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public OutputExpressionContext(OutputTargetContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterOutputExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitOutputExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitOutputExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OutputTargetContext outputTarget() throws RecognitionException {
		OutputTargetContext _localctx = new OutputTargetContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_outputTarget);
		int _la;
		try {
			setState(301);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,27,_ctx) ) {
			case 1:
				_localctx = new OutputSlotCallContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(294);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LBRACKET) {
					{
					setState(293);
					slotCast();
					}
				}

				setState(296);
				methodCall();
				}
				break;
			case 2:
				_localctx = new OutputNamedAssignmentContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(297);
				match(ID);
				setState(298);
				match(ASSIGN);
				setState(299);
				expr(0);
				}
				break;
			case 3:
				_localctx = new OutputExpressionContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(300);
				expr(0);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MethodCallStatementContext extends ParserRuleContext {
		public MethodCallContext methodCall() {
			return getRuleContext(MethodCallContext.class,0);
		}
		public MethodCallStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_methodCallStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterMethodCallStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitMethodCallStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitMethodCallStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MethodCallStatementContext methodCallStatement() throws RecognitionException {
		MethodCallStatementContext _localctx = new MethodCallStatementContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_methodCallStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(303);
			methodCall();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MethodCallContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(CoderiveParser.ID, 0); }
		public TerminalNode LPAREN() { return getToken(CoderiveParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(CoderiveParser.RPAREN, 0); }
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public TerminalNode DOT() { return getToken(CoderiveParser.DOT, 0); }
		public ArgumentListContext argumentList() {
			return getRuleContext(ArgumentListContext.class,0);
		}
		public MethodCallContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_methodCall; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterMethodCall(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitMethodCall(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitMethodCall(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MethodCallContext methodCall() throws RecognitionException {
		MethodCallContext _localctx = new MethodCallContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_methodCall);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(308);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,28,_ctx) ) {
			case 1:
				{
				setState(305);
				qualifiedName();
				setState(306);
				match(DOT);
				}
				break;
			}
			setState(310);
			match(ID);
			setState(311);
			match(LPAREN);
			setState(313);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 37383863009280L) != 0)) {
				{
				setState(312);
				argumentList();
				}
			}

			setState(315);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ArgumentListContext extends ParserRuleContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(CoderiveParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CoderiveParser.COMMA, i);
		}
		public ArgumentListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_argumentList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterArgumentList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitArgumentList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitArgumentList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgumentListContext argumentList() throws RecognitionException {
		ArgumentListContext _localctx = new ArgumentListContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_argumentList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(317);
			expr(0);
			setState(322);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(318);
				match(COMMA);
				setState(319);
				expr(0);
				}
				}
				setState(324);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class IfStatementContext extends ParserRuleContext {
		public TerminalNode IF() { return getToken(CoderiveParser.IF, 0); }
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public List<ThenBlockContext> thenBlock() {
			return getRuleContexts(ThenBlockContext.class);
		}
		public ThenBlockContext thenBlock(int i) {
			return getRuleContext(ThenBlockContext.class,i);
		}
		public List<TerminalNode> ELIF() { return getTokens(CoderiveParser.ELIF); }
		public TerminalNode ELIF(int i) {
			return getToken(CoderiveParser.ELIF, i);
		}
		public TerminalNode ELSE() { return getToken(CoderiveParser.ELSE, 0); }
		public IfStatementContext ifStatement() {
			return getRuleContext(IfStatementContext.class,0);
		}
		public ElseBlockContext elseBlock() {
			return getRuleContext(ElseBlockContext.class,0);
		}
		public IfStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ifStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterIfStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitIfStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitIfStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IfStatementContext ifStatement() throws RecognitionException {
		IfStatementContext _localctx = new IfStatementContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_ifStatement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(325);
			match(IF);
			setState(326);
			expr(0);
			setState(327);
			thenBlock();
			setState(334);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ELIF) {
				{
				{
				setState(328);
				match(ELIF);
				setState(329);
				expr(0);
				setState(330);
				thenBlock();
				}
				}
				setState(336);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(342);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ELSE) {
				{
				setState(337);
				match(ELSE);
				setState(340);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case IF:
					{
					setState(338);
					ifStatement();
					}
					break;
				case LBRACE:
					{
					setState(339);
					elseBlock();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ThenBlockContext extends ParserRuleContext {
		public TerminalNode LBRACE() { return getToken(CoderiveParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(CoderiveParser.RBRACE, 0); }
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public ThenBlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thenBlock; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterThenBlock(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitThenBlock(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitThenBlock(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ThenBlockContext thenBlock() throws RecognitionException {
		ThenBlockContext _localctx = new ThenBlockContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_thenBlock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(344);
			match(LBRACE);
			setState(348);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 37383864984960L) != 0)) {
				{
				{
				setState(345);
				statement();
				}
				}
				setState(350);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(351);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ElseBlockContext extends ParserRuleContext {
		public TerminalNode LBRACE() { return getToken(CoderiveParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(CoderiveParser.RBRACE, 0); }
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public ElseBlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_elseBlock; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterElseBlock(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitElseBlock(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitElseBlock(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ElseBlockContext elseBlock() throws RecognitionException {
		ElseBlockContext _localctx = new ElseBlockContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_elseBlock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(353);
			match(LBRACE);
			setState(357);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 37383864984960L) != 0)) {
				{
				{
				setState(354);
				statement();
				}
				}
				setState(359);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(360);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ForStatementContext extends ParserRuleContext {
		public TerminalNode FOR() { return getToken(CoderiveParser.FOR, 0); }
		public TerminalNode ID() { return getToken(CoderiveParser.ID, 0); }
		public TerminalNode IN() { return getToken(CoderiveParser.IN, 0); }
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode TO() { return getToken(CoderiveParser.TO, 0); }
		public TerminalNode LBRACE() { return getToken(CoderiveParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(CoderiveParser.RBRACE, 0); }
		public TerminalNode BY() { return getToken(CoderiveParser.BY, 0); }
		public ForStepExprContext forStepExpr() {
			return getRuleContext(ForStepExprContext.class,0);
		}
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public ForStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_forStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterForStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitForStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitForStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ForStatementContext forStatement() throws RecognitionException {
		ForStatementContext _localctx = new ForStatementContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_forStatement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(362);
			match(FOR);
			setState(363);
			match(ID);
			setState(366);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==BY) {
				{
				setState(364);
				match(BY);
				setState(365);
				forStepExpr();
				}
			}

			setState(368);
			match(IN);
			setState(369);
			expr(0);
			setState(370);
			match(TO);
			setState(371);
			expr(0);
			setState(372);
			match(LBRACE);
			setState(376);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 37383864984960L) != 0)) {
				{
				{
				setState(373);
				statement();
				}
				}
				setState(378);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(379);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ForStepExprContext extends ParserRuleContext {
		public ForStepExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_forStepExpr; }
	 
		public ForStepExprContext() { }
		public void copyFrom(ForStepExprContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class RegularStepContext extends ForStepExprContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public RegularStepContext(ForStepExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterRegularStep(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitRegularStep(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitRegularStep(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class OperatorStepContext extends ForStepExprContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode MUL() { return getToken(CoderiveParser.MUL, 0); }
		public TerminalNode DIV() { return getToken(CoderiveParser.DIV, 0); }
		public TerminalNode PLUS() { return getToken(CoderiveParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(CoderiveParser.MINUS, 0); }
		public OperatorStepContext(ForStepExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterOperatorStep(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitOperatorStep(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitOperatorStep(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ForStepExprContext forStepExpr() throws RecognitionException {
		ForStepExprContext _localctx = new ForStepExprContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_forStepExpr);
		int _la;
		try {
			setState(384);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,38,_ctx) ) {
			case 1:
				_localctx = new OperatorStepContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(381);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 2013265920L) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(382);
				expr(0);
				}
				break;
			case 2:
				_localctx = new RegularStepContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(383);
				expr(0);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExprContext extends ParserRuleContext {
		public ExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expr; }
	 
		public ExprContext() { }
		public void copyFrom(ExprContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class MethodCallExprContext extends ExprContext {
		public MethodCallContext methodCall() {
			return getRuleContext(MethodCallContext.class,0);
		}
		public MethodCallExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterMethodCallExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitMethodCallExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitMethodCallExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class UnaryExprContext extends ExprContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode PLUS() { return getToken(CoderiveParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(CoderiveParser.MINUS, 0); }
		public UnaryExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterUnaryExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitUnaryExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitUnaryExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PrimaryExprContext extends ExprContext {
		public PrimaryContext primary() {
			return getRuleContext(PrimaryContext.class,0);
		}
		public PrimaryExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterPrimaryExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitPrimaryExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitPrimaryExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ComparisonExprContext extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode GT() { return getToken(CoderiveParser.GT, 0); }
		public TerminalNode LT() { return getToken(CoderiveParser.LT, 0); }
		public TerminalNode GTE() { return getToken(CoderiveParser.GTE, 0); }
		public TerminalNode LTE() { return getToken(CoderiveParser.LTE, 0); }
		public TerminalNode EQ() { return getToken(CoderiveParser.EQ, 0); }
		public TerminalNode NEQ() { return getToken(CoderiveParser.NEQ, 0); }
		public ComparisonExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterComparisonExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitComparisonExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitComparisonExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class TypeCastExprContext extends ExprContext {
		public TypeCastContext typeCast() {
			return getRuleContext(TypeCastContext.class,0);
		}
		public TypeCastExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterTypeCastExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitTypeCastExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitTypeCastExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class AdditiveExprContext extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode PLUS() { return getToken(CoderiveParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(CoderiveParser.MINUS, 0); }
		public AdditiveExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterAdditiveExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitAdditiveExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitAdditiveExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class MultiplicativeExprContext extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode MUL() { return getToken(CoderiveParser.MUL, 0); }
		public TerminalNode DIV() { return getToken(CoderiveParser.DIV, 0); }
		public TerminalNode MOD() { return getToken(CoderiveParser.MOD, 0); }
		public MultiplicativeExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterMultiplicativeExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitMultiplicativeExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitMultiplicativeExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExprContext expr() throws RecognitionException {
		return expr(0);
	}

	private ExprContext expr(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ExprContext _localctx = new ExprContext(_ctx, _parentState);
		ExprContext _prevctx = _localctx;
		int _startState = 78;
		enterRecursionRule(_localctx, 78, RULE_expr, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(392);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,39,_ctx) ) {
			case 1:
				{
				_localctx = new UnaryExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(387);
				_la = _input.LA(1);
				if ( !(_la==PLUS || _la==MINUS) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(388);
				expr(7);
				}
				break;
			case 2:
				{
				_localctx = new PrimaryExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(389);
				primary();
				}
				break;
			case 3:
				{
				_localctx = new MethodCallExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(390);
				methodCall();
				}
				break;
			case 4:
				{
				_localctx = new TypeCastExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(391);
				typeCast();
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(405);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,41,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(403);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,40,_ctx) ) {
					case 1:
						{
						_localctx = new MultiplicativeExprContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(394);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(395);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 3758096384L) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(396);
						expr(7);
						}
						break;
					case 2:
						{
						_localctx = new AdditiveExprContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(397);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(398);
						_la = _input.LA(1);
						if ( !(_la==PLUS || _la==MINUS) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(399);
						expr(6);
						}
						break;
					case 3:
						{
						_localctx = new ComparisonExprContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(400);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(401);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 541165879296L) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(402);
						expr(5);
						}
						break;
					}
					} 
				}
				setState(407);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,41,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PrimaryContext extends ParserRuleContext {
		public PrimaryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_primary; }
	 
		public PrimaryContext() { }
		public void copyFrom(PrimaryContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class IndexAccessExprContext extends PrimaryContext {
		public AtomContext atom() {
			return getRuleContext(AtomContext.class,0);
		}
		public List<TerminalNode> LBRACKET() { return getTokens(CoderiveParser.LBRACKET); }
		public TerminalNode LBRACKET(int i) {
			return getToken(CoderiveParser.LBRACKET, i);
		}
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public List<TerminalNode> RBRACKET() { return getTokens(CoderiveParser.RBRACKET); }
		public TerminalNode RBRACKET(int i) {
			return getToken(CoderiveParser.RBRACKET, i);
		}
		public IndexAccessExprContext(PrimaryContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterIndexAccessExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitIndexAccessExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitIndexAccessExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PrimaryContext primary() throws RecognitionException {
		PrimaryContext _localctx = new PrimaryContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_primary);
		try {
			int _alt;
			_localctx = new IndexAccessExprContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(408);
			atom();
			setState(415);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,42,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(409);
					match(LBRACKET);
					setState(410);
					expr(0);
					setState(411);
					match(RBRACKET);
					}
					} 
				}
				setState(417);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,42,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AtomContext extends ParserRuleContext {
		public AtomContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_atom; }
	 
		public AtomContext() { }
		public void copyFrom(AtomContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class FloatLiteralExprContext extends AtomContext {
		public TerminalNode FLOAT_LIT() { return getToken(CoderiveParser.FLOAT_LIT, 0); }
		public FloatLiteralExprContext(AtomContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterFloatLiteralExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitFloatLiteralExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitFloatLiteralExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class StringLiteralExprContext extends AtomContext {
		public TerminalNode STRING_LIT() { return getToken(CoderiveParser.STRING_LIT, 0); }
		public StringLiteralExprContext(AtomContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterStringLiteralExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitStringLiteralExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitStringLiteralExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class IntLiteralExprContext extends AtomContext {
		public TerminalNode INT_LIT() { return getToken(CoderiveParser.INT_LIT, 0); }
		public IntLiteralExprContext(AtomContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterIntLiteralExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitIntLiteralExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitIntLiteralExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ParenthesizedExprContext extends AtomContext {
		public TerminalNode LPAREN() { return getToken(CoderiveParser.LPAREN, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(CoderiveParser.RPAREN, 0); }
		public ParenthesizedExprContext(AtomContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterParenthesizedExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitParenthesizedExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitParenthesizedExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ArrayLiteralExprContext extends AtomContext {
		public ArrayLiteralContext arrayLiteral() {
			return getRuleContext(ArrayLiteralContext.class,0);
		}
		public ArrayLiteralExprContext(AtomContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterArrayLiteralExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitArrayLiteralExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitArrayLiteralExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class BoolLiteralExprContext extends AtomContext {
		public TerminalNode BOOL_LIT() { return getToken(CoderiveParser.BOOL_LIT, 0); }
		public BoolLiteralExprContext(AtomContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterBoolLiteralExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitBoolLiteralExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitBoolLiteralExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class IdentifierExprContext extends AtomContext {
		public TerminalNode ID() { return getToken(CoderiveParser.ID, 0); }
		public IdentifierExprContext(AtomContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterIdentifierExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitIdentifierExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitIdentifierExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AtomContext atom() throws RecognitionException {
		AtomContext _localctx = new AtomContext(_ctx, getState());
		enterRule(_localctx, 82, RULE_atom);
		try {
			setState(428);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				_localctx = new IdentifierExprContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(418);
				match(ID);
				}
				break;
			case INT_LIT:
				_localctx = new IntLiteralExprContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(419);
				match(INT_LIT);
				}
				break;
			case FLOAT_LIT:
				_localctx = new FloatLiteralExprContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(420);
				match(FLOAT_LIT);
				}
				break;
			case STRING_LIT:
				_localctx = new StringLiteralExprContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(421);
				match(STRING_LIT);
				}
				break;
			case BOOL_LIT:
				_localctx = new BoolLiteralExprContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(422);
				match(BOOL_LIT);
				}
				break;
			case LPAREN:
				_localctx = new ParenthesizedExprContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(423);
				match(LPAREN);
				setState(424);
				expr(0);
				setState(425);
				match(RPAREN);
				}
				break;
			case LBRACKET:
				_localctx = new ArrayLiteralExprContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(427);
				arrayLiteral();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ArrayLiteralContext extends ParserRuleContext {
		public TerminalNode LBRACKET() { return getToken(CoderiveParser.LBRACKET, 0); }
		public TerminalNode RBRACKET() { return getToken(CoderiveParser.RBRACKET, 0); }
		public ExprListContext exprList() {
			return getRuleContext(ExprListContext.class,0);
		}
		public ArrayLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arrayLiteral; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterArrayLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitArrayLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitArrayLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArrayLiteralContext arrayLiteral() throws RecognitionException {
		ArrayLiteralContext _localctx = new ArrayLiteralContext(_ctx, getState());
		enterRule(_localctx, 84, RULE_arrayLiteral);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(430);
			match(LBRACKET);
			setState(432);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 37383863009280L) != 0)) {
				{
				setState(431);
				exprList();
				}
			}

			setState(434);
			match(RBRACKET);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExprListContext extends ParserRuleContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(CoderiveParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CoderiveParser.COMMA, i);
		}
		public ExprListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_exprList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterExprList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitExprList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitExprList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExprListContext exprList() throws RecognitionException {
		ExprListContext _localctx = new ExprListContext(_ctx, getState());
		enterRule(_localctx, 86, RULE_exprList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(436);
			expr(0);
			setState(441);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(437);
				match(COMMA);
				setState(438);
				expr(0);
				}
				}
				setState(443);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class IndexAccessContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(CoderiveParser.ID, 0); }
		public List<TerminalNode> LBRACKET() { return getTokens(CoderiveParser.LBRACKET); }
		public TerminalNode LBRACKET(int i) {
			return getToken(CoderiveParser.LBRACKET, i);
		}
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public List<TerminalNode> RBRACKET() { return getTokens(CoderiveParser.RBRACKET); }
		public TerminalNode RBRACKET(int i) {
			return getToken(CoderiveParser.RBRACKET, i);
		}
		public IndexAccessContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_indexAccess; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterIndexAccess(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitIndexAccess(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitIndexAccess(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IndexAccessContext indexAccess() throws RecognitionException {
		IndexAccessContext _localctx = new IndexAccessContext(_ctx, getState());
		enterRule(_localctx, 88, RULE_indexAccess);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(444);
			match(ID);
			setState(449); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(445);
				match(LBRACKET);
				setState(446);
				expr(0);
				setState(447);
				match(RBRACKET);
				}
				}
				setState(451); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==LBRACKET );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeCastContext extends ParserRuleContext {
		public TerminalNode LPAREN() { return getToken(CoderiveParser.LPAREN, 0); }
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(CoderiveParser.RPAREN, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TypeCastContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeCast; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).enterTypeCast(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CoderiveParserListener ) ((CoderiveParserListener)listener).exitTypeCast(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CoderiveParserVisitor ) return ((CoderiveParserVisitor<? extends T>)visitor).visitTypeCast(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeCastContext typeCast() throws RecognitionException {
		TypeCastContext _localctx = new TypeCastContext(_ctx, getState());
		enterRule(_localctx, 90, RULE_typeCast);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(453);
			match(LPAREN);
			setState(454);
			type();
			setState(455);
			match(RPAREN);
			setState(456);
			expr(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 39:
			return expr_sempred((ExprContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean expr_sempred(ExprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 6);
		case 1:
			return precpred(_ctx, 5);
		case 2:
			return precpred(_ctx, 4);
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u00011\u01cb\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002"+
		"\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007\u000f"+
		"\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007\u0012"+
		"\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007\u0015"+
		"\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002\u0018\u0007\u0018"+
		"\u0002\u0019\u0007\u0019\u0002\u001a\u0007\u001a\u0002\u001b\u0007\u001b"+
		"\u0002\u001c\u0007\u001c\u0002\u001d\u0007\u001d\u0002\u001e\u0007\u001e"+
		"\u0002\u001f\u0007\u001f\u0002 \u0007 \u0002!\u0007!\u0002\"\u0007\"\u0002"+
		"#\u0007#\u0002$\u0007$\u0002%\u0007%\u0002&\u0007&\u0002\'\u0007\'\u0002"+
		"(\u0007(\u0002)\u0007)\u0002*\u0007*\u0002+\u0007+\u0002,\u0007,\u0002"+
		"-\u0007-\u0001\u0000\u0003\u0000^\b\u0000\u0001\u0000\u0005\u0000a\b\u0000"+
		"\n\u0000\f\u0000d\t\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0003\u0001"+
		"o\b\u0001\u0001\u0002\u0004\u0002r\b\u0002\u000b\u0002\f\u0002s\u0001"+
		"\u0003\u0001\u0003\u0001\u0003\u0005\u0003y\b\u0003\n\u0003\f\u0003|\t"+
		"\u0003\u0001\u0004\u0003\u0004\u007f\b\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0003\u0004\u0084\b\u0004\u0001\u0004\u0001\u0004\u0005\u0004\u0088"+
		"\b\u0004\n\u0004\f\u0004\u008b\t\u0004\u0001\u0004\u0001\u0004\u0001\u0005"+
		"\u0001\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0003\u0006"+
		"\u0095\b\u0006\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0003\u0007"+
		"\u009b\b\u0007\u0001\b\u0001\b\u0001\b\u0003\b\u00a0\b\b\u0001\b\u0001"+
		"\b\u0001\b\u0005\b\u00a5\b\b\n\b\f\b\u00a8\t\b\u0001\b\u0001\b\u0001\t"+
		"\u0001\t\u0003\t\u00ae\b\t\u0001\t\u0003\t\u00b1\b\t\u0001\t\u0003\t\u00b4"+
		"\b\t\u0003\t\u00b6\b\t\u0001\t\u0001\t\u0001\t\u0003\t\u00bb\b\t\u0001"+
		"\t\u0001\t\u0001\t\u0005\t\u00c0\b\t\n\t\f\t\u00c3\t\t\u0001\t\u0001\t"+
		"\u0001\n\u0001\n\u0001\n\u0003\n\u00ca\b\n\u0001\n\u0001\n\u0003\n\u00ce"+
		"\b\n\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\f\u0001\f\u0001"+
		"\f\u0005\f\u00d7\b\f\n\f\f\f\u00da\t\f\u0001\r\u0001\r\u0001\r\u0001\u000e"+
		"\u0001\u000e\u0001\u000e\u0005\u000e\u00e2\b\u000e\n\u000e\f\u000e\u00e5"+
		"\t\u000e\u0001\u000f\u0001\u000f\u0001\u0010\u0001\u0010\u0001\u0010\u0001"+
		"\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001"+
		"\u0010\u0003\u0010\u00f3\b\u0010\u0001\u0011\u0001\u0011\u0003\u0011\u00f7"+
		"\b\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0003\u0011\u00fc\b\u0011"+
		"\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0013\u0001\u0013"+
		"\u0001\u0014\u0001\u0014\u0003\u0014\u0106\b\u0014\u0001\u0015\u0001\u0015"+
		"\u0001\u0015\u0001\u0015\u0001\u0016\u0001\u0016\u0001\u0017\u0001\u0017"+
		"\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0019\u0001\u0019\u0001\u0019"+
		"\u0001\u0019\u0001\u0019\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a"+
		"\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001c\u0001\u001c\u0001\u001c"+
		"\u0001\u001c\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001e\u0003\u001e"+
		"\u0127\b\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e"+
		"\u0003\u001e\u012e\b\u001e\u0001\u001f\u0001\u001f\u0001 \u0001 \u0001"+
		" \u0003 \u0135\b \u0001 \u0001 \u0001 \u0003 \u013a\b \u0001 \u0001 \u0001"+
		"!\u0001!\u0001!\u0005!\u0141\b!\n!\f!\u0144\t!\u0001\"\u0001\"\u0001\""+
		"\u0001\"\u0001\"\u0001\"\u0001\"\u0005\"\u014d\b\"\n\"\f\"\u0150\t\"\u0001"+
		"\"\u0001\"\u0001\"\u0003\"\u0155\b\"\u0003\"\u0157\b\"\u0001#\u0001#\u0005"+
		"#\u015b\b#\n#\f#\u015e\t#\u0001#\u0001#\u0001$\u0001$\u0005$\u0164\b$"+
		"\n$\f$\u0167\t$\u0001$\u0001$\u0001%\u0001%\u0001%\u0001%\u0003%\u016f"+
		"\b%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0005%\u0177\b%\n%\f%\u017a"+
		"\t%\u0001%\u0001%\u0001&\u0001&\u0001&\u0003&\u0181\b&\u0001\'\u0001\'"+
		"\u0001\'\u0001\'\u0001\'\u0001\'\u0003\'\u0189\b\'\u0001\'\u0001\'\u0001"+
		"\'\u0001\'\u0001\'\u0001\'\u0001\'\u0001\'\u0001\'\u0005\'\u0194\b\'\n"+
		"\'\f\'\u0197\t\'\u0001(\u0001(\u0001(\u0001(\u0001(\u0005(\u019e\b(\n"+
		"(\f(\u01a1\t(\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)"+
		"\u0001)\u0001)\u0003)\u01ad\b)\u0001*\u0001*\u0003*\u01b1\b*\u0001*\u0001"+
		"*\u0001+\u0001+\u0001+\u0005+\u01b8\b+\n+\f+\u01bb\t+\u0001,\u0001,\u0001"+
		",\u0001,\u0001,\u0004,\u01c2\b,\u000b,\f,\u01c3\u0001-\u0001-\u0001-\u0001"+
		"-\u0001-\u0001-\u0000\u0001N.\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010"+
		"\u0012\u0014\u0016\u0018\u001a\u001c\u001e \"$&(*,.02468:<>@BDFHJLNPR"+
		"TVXZ\u0000\u0006\u0001\u0000\u0001\u0002\u0002\u0000\u0011\u0014\u0019"+
		"\u0019\u0001\u0000\u001b\u001e\u0001\u0000\u001b\u001c\u0001\u0000\u001d"+
		"\u001f\u0001\u0000!&\u01de\u0000]\u0001\u0000\u0000\u0000\u0002g\u0001"+
		"\u0000\u0000\u0000\u0004q\u0001\u0000\u0000\u0000\u0006u\u0001\u0000\u0000"+
		"\u0000\b~\u0001\u0000\u0000\u0000\n\u008e\u0001\u0000\u0000\u0000\f\u0094"+
		"\u0001\u0000\u0000\u0000\u000e\u0096\u0001\u0000\u0000\u0000\u0010\u009c"+
		"\u0001\u0000\u0000\u0000\u0012\u00b5\u0001\u0000\u0000\u0000\u0014\u00c6"+
		"\u0001\u0000\u0000\u0000\u0016\u00cf\u0001\u0000\u0000\u0000\u0018\u00d3"+
		"\u0001\u0000\u0000\u0000\u001a\u00db\u0001\u0000\u0000\u0000\u001c\u00de"+
		"\u0001\u0000\u0000\u0000\u001e\u00e6\u0001\u0000\u0000\u0000 \u00f2\u0001"+
		"\u0000\u0000\u0000\"\u00f6\u0001\u0000\u0000\u0000$\u00fd\u0001\u0000"+
		"\u0000\u0000&\u0101\u0001\u0000\u0000\u0000(\u0105\u0001\u0000\u0000\u0000"+
		"*\u0107\u0001\u0000\u0000\u0000,\u010b\u0001\u0000\u0000\u0000.\u010d"+
		"\u0001\u0000\u0000\u00000\u010f\u0001\u0000\u0000\u00002\u0112\u0001\u0000"+
		"\u0000\u00004\u0117\u0001\u0000\u0000\u00006\u011b\u0001\u0000\u0000\u0000"+
		"8\u011e\u0001\u0000\u0000\u0000:\u0122\u0001\u0000\u0000\u0000<\u012d"+
		"\u0001\u0000\u0000\u0000>\u012f\u0001\u0000\u0000\u0000@\u0134\u0001\u0000"+
		"\u0000\u0000B\u013d\u0001\u0000\u0000\u0000D\u0145\u0001\u0000\u0000\u0000"+
		"F\u0158\u0001\u0000\u0000\u0000H\u0161\u0001\u0000\u0000\u0000J\u016a"+
		"\u0001\u0000\u0000\u0000L\u0180\u0001\u0000\u0000\u0000N\u0188\u0001\u0000"+
		"\u0000\u0000P\u0198\u0001\u0000\u0000\u0000R\u01ac\u0001\u0000\u0000\u0000"+
		"T\u01ae\u0001\u0000\u0000\u0000V\u01b4\u0001\u0000\u0000\u0000X\u01bc"+
		"\u0001\u0000\u0000\u0000Z\u01c5\u0001\u0000\u0000\u0000\\^\u0003\u0002"+
		"\u0001\u0000]\\\u0001\u0000\u0000\u0000]^\u0001\u0000\u0000\u0000^b\u0001"+
		"\u0000\u0000\u0000_a\u0003\b\u0004\u0000`_\u0001\u0000\u0000\u0000ad\u0001"+
		"\u0000\u0000\u0000b`\u0001\u0000\u0000\u0000bc\u0001\u0000\u0000\u0000"+
		"ce\u0001\u0000\u0000\u0000db\u0001\u0000\u0000\u0000ef\u0005\u0000\u0000"+
		"\u0001f\u0001\u0001\u0000\u0000\u0000gh\u0005\u0003\u0000\u0000hn\u0003"+
		"\u0006\u0003\u0000ij\u0005\u0004\u0000\u0000jk\u0005+\u0000\u0000kl\u0003"+
		"\u0004\u0002\u0000lm\u0005,\u0000\u0000mo\u0001\u0000\u0000\u0000ni\u0001"+
		"\u0000\u0000\u0000no\u0001\u0000\u0000\u0000o\u0003\u0001\u0000\u0000"+
		"\u0000pr\u0003\u0006\u0003\u0000qp\u0001\u0000\u0000\u0000rs\u0001\u0000"+
		"\u0000\u0000sq\u0001\u0000\u0000\u0000st\u0001\u0000\u0000\u0000t\u0005"+
		"\u0001\u0000\u0000\u0000uz\u0005\u0019\u0000\u0000vw\u0005\'\u0000\u0000"+
		"wy\u0005\u0019\u0000\u0000xv\u0001\u0000\u0000\u0000y|\u0001\u0000\u0000"+
		"\u0000zx\u0001\u0000\u0000\u0000z{\u0001\u0000\u0000\u0000{\u0007\u0001"+
		"\u0000\u0000\u0000|z\u0001\u0000\u0000\u0000}\u007f\u0003\n\u0005\u0000"+
		"~}\u0001\u0000\u0000\u0000~\u007f\u0001\u0000\u0000\u0000\u007f\u0080"+
		"\u0001\u0000\u0000\u0000\u0080\u0083\u0005\u0019\u0000\u0000\u0081\u0082"+
		"\u0005\u0005\u0000\u0000\u0082\u0084\u0003\u0006\u0003\u0000\u0083\u0081"+
		"\u0001\u0000\u0000\u0000\u0083\u0084\u0001\u0000\u0000\u0000\u0084\u0085"+
		"\u0001\u0000\u0000\u0000\u0085\u0089\u0005+\u0000\u0000\u0086\u0088\u0003"+
		"\f\u0006\u0000\u0087\u0086\u0001\u0000\u0000\u0000\u0088\u008b\u0001\u0000"+
		"\u0000\u0000\u0089\u0087\u0001\u0000\u0000\u0000\u0089\u008a\u0001\u0000"+
		"\u0000\u0000\u008a\u008c\u0001\u0000\u0000\u0000\u008b\u0089\u0001\u0000"+
		"\u0000\u0000\u008c\u008d\u0005,\u0000\u0000\u008d\t\u0001\u0000\u0000"+
		"\u0000\u008e\u008f\u0007\u0000\u0000\u0000\u008f\u000b\u0001\u0000\u0000"+
		"\u0000\u0090\u0095\u0003\u000e\u0007\u0000\u0091\u0095\u0003\u0010\b\u0000"+
		"\u0092\u0095\u0003\u0012\t\u0000\u0093\u0095\u0003 \u0010\u0000\u0094"+
		"\u0090\u0001\u0000\u0000\u0000\u0094\u0091\u0001\u0000\u0000\u0000\u0094"+
		"\u0092\u0001\u0000\u0000\u0000\u0094\u0093\u0001\u0000\u0000\u0000\u0095"+
		"\r\u0001\u0000\u0000\u0000\u0096\u0097\u0003\u001c\u000e\u0000\u0097\u009a"+
		"\u0005\u0019\u0000\u0000\u0098\u0099\u0005\u001a\u0000\u0000\u0099\u009b"+
		"\u0003N\'\u0000\u009a\u0098\u0001\u0000\u0000\u0000\u009a\u009b\u0001"+
		"\u0000\u0000\u0000\u009b\u000f\u0001\u0000\u0000\u0000\u009c\u009d\u0005"+
		"\u0019\u0000\u0000\u009d\u009f\u0005)\u0000\u0000\u009e\u00a0\u0003\u0018"+
		"\f\u0000\u009f\u009e\u0001\u0000\u0000\u0000\u009f\u00a0\u0001\u0000\u0000"+
		"\u0000\u00a0\u00a1\u0001\u0000\u0000\u0000\u00a1\u00a2\u0005*\u0000\u0000"+
		"\u00a2\u00a6\u0005+\u0000\u0000\u00a3\u00a5\u0003 \u0010\u0000\u00a4\u00a3"+
		"\u0001\u0000\u0000\u0000\u00a5\u00a8\u0001\u0000\u0000\u0000\u00a6\u00a4"+
		"\u0001\u0000\u0000\u0000\u00a6\u00a7\u0001\u0000\u0000\u0000\u00a7\u00a9"+
		"\u0001\u0000\u0000\u0000\u00a8\u00a6\u0001\u0000\u0000\u0000\u00a9\u00aa"+
		"\u0005,\u0000\u0000\u00aa\u0011\u0001\u0000\u0000\u0000\u00ab\u00ad\u0003"+
		"\u0016\u000b\u0000\u00ac\u00ae\u0003\n\u0005\u0000\u00ad\u00ac\u0001\u0000"+
		"\u0000\u0000\u00ad\u00ae\u0001\u0000\u0000\u0000\u00ae\u00b6\u0001\u0000"+
		"\u0000\u0000\u00af\u00b1\u0003\n\u0005\u0000\u00b0\u00af\u0001\u0000\u0000"+
		"\u0000\u00b0\u00b1\u0001\u0000\u0000\u0000\u00b1\u00b3\u0001\u0000\u0000"+
		"\u0000\u00b2\u00b4\u0003\u0016\u000b\u0000\u00b3\u00b2\u0001\u0000\u0000"+
		"\u0000\u00b3\u00b4\u0001\u0000\u0000\u0000\u00b4\u00b6\u0001\u0000\u0000"+
		"\u0000\u00b5\u00ab\u0001\u0000\u0000\u0000\u00b5\u00b0\u0001\u0000\u0000"+
		"\u0000\u00b6\u00b7\u0001\u0000\u0000\u0000\u00b7\u00b8\u0005\u0019\u0000"+
		"\u0000\u00b8\u00ba\u0005)\u0000\u0000\u00b9\u00bb\u0003\u0018\f\u0000"+
		"\u00ba\u00b9\u0001\u0000\u0000\u0000\u00ba\u00bb\u0001\u0000\u0000\u0000"+
		"\u00bb\u00bc\u0001\u0000\u0000\u0000\u00bc\u00bd\u0005*\u0000\u0000\u00bd"+
		"\u00c1\u0005+\u0000\u0000\u00be\u00c0\u0003 \u0010\u0000\u00bf\u00be\u0001"+
		"\u0000\u0000\u0000\u00c0\u00c3\u0001\u0000\u0000\u0000\u00c1\u00bf\u0001"+
		"\u0000\u0000\u0000\u00c1\u00c2\u0001\u0000\u0000\u0000\u00c2\u00c4\u0001"+
		"\u0000\u0000\u0000\u00c3\u00c1\u0001\u0000\u0000\u0000\u00c4\u00c5\u0005"+
		",\u0000\u0000\u00c5\u0013\u0001\u0000\u0000\u0000\u00c6\u00c9\u0005\u0019"+
		"\u0000\u0000\u00c7\u00c8\u0005(\u0000\u0000\u00c8\u00ca\u0005\u0019\u0000"+
		"\u0000\u00c9\u00c7\u0001\u0000\u0000\u0000\u00c9\u00ca\u0001\u0000\u0000"+
		"\u0000\u00ca\u00cd\u0001\u0000\u0000\u0000\u00cb\u00cc\u0005(\u0000\u0000"+
		"\u00cc\u00ce\u0005\u0019\u0000\u0000\u00cd\u00cb\u0001\u0000\u0000\u0000"+
		"\u00cd\u00ce\u0001\u0000\u0000\u0000\u00ce\u0015\u0001\u0000\u0000\u0000"+
		"\u00cf\u00d0\u0005-\u0000\u0000\u00d0\u00d1\u0003\u0014\n\u0000\u00d1"+
		"\u00d2\u0005.\u0000\u0000\u00d2\u0017\u0001\u0000\u0000\u0000\u00d3\u00d8"+
		"\u0003\u001a\r\u0000\u00d4\u00d5\u0005(\u0000\u0000\u00d5\u00d7\u0003"+
		"\u001a\r\u0000\u00d6\u00d4\u0001\u0000\u0000\u0000\u00d7\u00da\u0001\u0000"+
		"\u0000\u0000\u00d8\u00d6\u0001\u0000\u0000\u0000\u00d8\u00d9\u0001\u0000"+
		"\u0000\u0000\u00d9\u0019\u0001\u0000\u0000\u0000\u00da\u00d8\u0001\u0000"+
		"\u0000\u0000\u00db\u00dc\u0003\u001c\u000e\u0000\u00dc\u00dd\u0005\u0019"+
		"\u0000\u0000\u00dd\u001b\u0001\u0000\u0000\u0000\u00de\u00e3\u0003\u001e"+
		"\u000f\u0000\u00df\u00e0\u0005-\u0000\u0000\u00e0\u00e2\u0005.\u0000\u0000"+
		"\u00e1\u00df\u0001\u0000\u0000\u0000\u00e2\u00e5\u0001\u0000\u0000\u0000"+
		"\u00e3\u00e1\u0001\u0000\u0000\u0000\u00e3\u00e4\u0001\u0000\u0000\u0000"+
		"\u00e4\u001d\u0001\u0000\u0000\u0000\u00e5\u00e3\u0001\u0000\u0000\u0000"+
		"\u00e6\u00e7\u0007\u0001\u0000\u0000\u00e7\u001f\u0001\u0000\u0000\u0000"+
		"\u00e8\u00f3\u0003\"\u0011\u0000\u00e9\u00f3\u0003$\u0012\u0000\u00ea"+
		"\u00f3\u0003*\u0015\u0000\u00eb\u00f3\u00034\u001a\u0000\u00ec\u00f3\u0003"+
		">\u001f\u0000\u00ed\u00f3\u0003:\u001d\u0000\u00ee\u00f3\u0003D\"\u0000"+
		"\u00ef\u00f3\u0003J%\u0000\u00f0\u00f3\u0003&\u0013\u0000\u00f1\u00f3"+
		"\u0003.\u0017\u0000\u00f2\u00e8\u0001\u0000\u0000\u0000\u00f2\u00e9\u0001"+
		"\u0000\u0000\u0000\u00f2\u00ea\u0001\u0000\u0000\u0000\u00f2\u00eb\u0001"+
		"\u0000\u0000\u0000\u00f2\u00ec\u0001\u0000\u0000\u0000\u00f2\u00ed\u0001"+
		"\u0000\u0000\u0000\u00f2\u00ee\u0001\u0000\u0000\u0000\u00f2\u00ef\u0001"+
		"\u0000\u0000\u0000\u00f2\u00f0\u0001\u0000\u0000\u0000\u00f2\u00f1\u0001"+
		"\u0000\u0000\u0000\u00f3!\u0001\u0000\u0000\u0000\u00f4\u00f7\u0005\u0007"+
		"\u0000\u0000\u00f5\u00f7\u0003\u001c\u000e\u0000\u00f6\u00f4\u0001\u0000"+
		"\u0000\u0000\u00f6\u00f5\u0001\u0000\u0000\u0000\u00f7\u00f8\u0001\u0000"+
		"\u0000\u0000\u00f8\u00fb\u0005\u0019\u0000\u0000\u00f9\u00fa\u0005\u001a"+
		"\u0000\u0000\u00fa\u00fc\u0003N\'\u0000\u00fb\u00f9\u0001\u0000\u0000"+
		"\u0000\u00fb\u00fc\u0001\u0000\u0000\u0000\u00fc#\u0001\u0000\u0000\u0000"+
		"\u00fd\u00fe\u0003(\u0014\u0000\u00fe\u00ff\u0005\u001a\u0000\u0000\u00ff"+
		"\u0100\u0003N\'\u0000\u0100%\u0001\u0000\u0000\u0000\u0101\u0102\u0003"+
		"N\'\u0000\u0102\'\u0001\u0000\u0000\u0000\u0103\u0106\u0005\u0019\u0000"+
		"\u0000\u0104\u0106\u0003X,\u0000\u0105\u0103\u0001\u0000\u0000\u0000\u0105"+
		"\u0104\u0001\u0000\u0000\u0000\u0106)\u0001\u0000\u0000\u0000\u0107\u0108"+
		"\u0003,\u0016\u0000\u0108\u0109\u0005\u001a\u0000\u0000\u0109\u010a\u0003"+
		"0\u0018\u0000\u010a+\u0001\u0000\u0000\u0000\u010b\u010c\u0003\u0014\n"+
		"\u0000\u010c-\u0001\u0000\u0000\u0000\u010d\u010e\u00030\u0018\u0000\u010e"+
		"/\u0001\u0000\u0000\u0000\u010f\u0110\u00032\u0019\u0000\u0110\u0111\u0003"+
		"@ \u0000\u01111\u0001\u0000\u0000\u0000\u0112\u0113\u0005-\u0000\u0000"+
		"\u0113\u0114\u0003\u0014\n\u0000\u0114\u0115\u0005.\u0000\u0000\u0115"+
		"\u0116\u0005 \u0000\u0000\u01163\u0001\u0000\u0000\u0000\u0117\u0118\u0003"+
		"(\u0014\u0000\u0118\u0119\u0005\u001a\u0000\u0000\u0119\u011a\u00036\u001b"+
		"\u0000\u011a5\u0001\u0000\u0000\u0000\u011b\u011c\u00038\u001c\u0000\u011c"+
		"\u011d\u0005\t\u0000\u0000\u011d7\u0001\u0000\u0000\u0000\u011e\u011f"+
		"\u0005)\u0000\u0000\u011f\u0120\u0003\u001c\u000e\u0000\u0120\u0121\u0005"+
		"*\u0000\u0000\u01219\u0001\u0000\u0000\u0000\u0122\u0123\u0005\b\u0000"+
		"\u0000\u0123\u0124\u0003<\u001e\u0000\u0124;\u0001\u0000\u0000\u0000\u0125"+
		"\u0127\u00032\u0019\u0000\u0126\u0125\u0001\u0000\u0000\u0000\u0126\u0127"+
		"\u0001\u0000\u0000\u0000\u0127\u0128\u0001\u0000\u0000\u0000\u0128\u012e"+
		"\u0003@ \u0000\u0129\u012a\u0005\u0019\u0000\u0000\u012a\u012b\u0005\u001a"+
		"\u0000\u0000\u012b\u012e\u0003N\'\u0000\u012c\u012e\u0003N\'\u0000\u012d"+
		"\u0126\u0001\u0000\u0000\u0000\u012d\u0129\u0001\u0000\u0000\u0000\u012d"+
		"\u012c\u0001\u0000\u0000\u0000\u012e=\u0001\u0000\u0000\u0000\u012f\u0130"+
		"\u0003@ \u0000\u0130?\u0001\u0000\u0000\u0000\u0131\u0132\u0003\u0006"+
		"\u0003\u0000\u0132\u0133\u0005\'\u0000\u0000\u0133\u0135\u0001\u0000\u0000"+
		"\u0000\u0134\u0131\u0001\u0000\u0000\u0000\u0134\u0135\u0001\u0000\u0000"+
		"\u0000\u0135\u0136\u0001\u0000\u0000\u0000\u0136\u0137\u0005\u0019\u0000"+
		"\u0000\u0137\u0139\u0005)\u0000\u0000\u0138\u013a\u0003B!\u0000\u0139"+
		"\u0138\u0001\u0000\u0000\u0000\u0139\u013a\u0001\u0000\u0000\u0000\u013a"+
		"\u013b\u0001\u0000\u0000\u0000\u013b\u013c\u0005*\u0000\u0000\u013cA\u0001"+
		"\u0000\u0000\u0000\u013d\u0142\u0003N\'\u0000\u013e\u013f\u0005(\u0000"+
		"\u0000\u013f\u0141\u0003N\'\u0000\u0140\u013e\u0001\u0000\u0000\u0000"+
		"\u0141\u0144\u0001\u0000\u0000\u0000\u0142\u0140\u0001\u0000\u0000\u0000"+
		"\u0142\u0143\u0001\u0000\u0000\u0000\u0143C\u0001\u0000\u0000\u0000\u0144"+
		"\u0142\u0001\u0000\u0000\u0000\u0145\u0146\u0005\n\u0000\u0000\u0146\u0147"+
		"\u0003N\'\u0000\u0147\u014e\u0003F#\u0000\u0148\u0149\u0005\f\u0000\u0000"+
		"\u0149\u014a\u0003N\'\u0000\u014a\u014b\u0003F#\u0000\u014b\u014d\u0001"+
		"\u0000\u0000\u0000\u014c\u0148\u0001\u0000\u0000\u0000\u014d\u0150\u0001"+
		"\u0000\u0000\u0000\u014e\u014c\u0001\u0000\u0000\u0000\u014e\u014f\u0001"+
		"\u0000\u0000\u0000\u014f\u0156\u0001\u0000\u0000\u0000\u0150\u014e\u0001"+
		"\u0000\u0000\u0000\u0151\u0154\u0005\u000b\u0000\u0000\u0152\u0155\u0003"+
		"D\"\u0000\u0153\u0155\u0003H$\u0000\u0154\u0152\u0001\u0000\u0000\u0000"+
		"\u0154\u0153\u0001\u0000\u0000\u0000\u0155\u0157\u0001\u0000\u0000\u0000"+
		"\u0156\u0151\u0001\u0000\u0000\u0000\u0156\u0157\u0001\u0000\u0000\u0000"+
		"\u0157E\u0001\u0000\u0000\u0000\u0158\u015c\u0005+\u0000\u0000\u0159\u015b"+
		"\u0003 \u0010\u0000\u015a\u0159\u0001\u0000\u0000\u0000\u015b\u015e\u0001"+
		"\u0000\u0000\u0000\u015c\u015a\u0001\u0000\u0000\u0000\u015c\u015d\u0001"+
		"\u0000\u0000\u0000\u015d\u015f\u0001\u0000\u0000\u0000\u015e\u015c\u0001"+
		"\u0000\u0000\u0000\u015f\u0160\u0005,\u0000\u0000\u0160G\u0001\u0000\u0000"+
		"\u0000\u0161\u0165\u0005+\u0000\u0000\u0162\u0164\u0003 \u0010\u0000\u0163"+
		"\u0162\u0001\u0000\u0000\u0000\u0164\u0167\u0001\u0000\u0000\u0000\u0165"+
		"\u0163\u0001\u0000\u0000\u0000\u0165\u0166\u0001\u0000\u0000\u0000\u0166"+
		"\u0168\u0001\u0000\u0000\u0000\u0167\u0165\u0001\u0000\u0000\u0000\u0168"+
		"\u0169\u0005,\u0000\u0000\u0169I\u0001\u0000\u0000\u0000\u016a\u016b\u0005"+
		"\r\u0000\u0000\u016b\u016e\u0005\u0019\u0000\u0000\u016c\u016d\u0005\u000e"+
		"\u0000\u0000\u016d\u016f\u0003L&\u0000\u016e\u016c\u0001\u0000\u0000\u0000"+
		"\u016e\u016f\u0001\u0000\u0000\u0000\u016f\u0170\u0001\u0000\u0000\u0000"+
		"\u0170\u0171\u0005\u000f\u0000\u0000\u0171\u0172\u0003N\'\u0000\u0172"+
		"\u0173\u0005\u0010\u0000\u0000\u0173\u0174\u0003N\'\u0000\u0174\u0178"+
		"\u0005+\u0000\u0000\u0175\u0177\u0003 \u0010\u0000\u0176\u0175\u0001\u0000"+
		"\u0000\u0000\u0177\u017a\u0001\u0000\u0000\u0000\u0178\u0176\u0001\u0000"+
		"\u0000\u0000\u0178\u0179\u0001\u0000\u0000\u0000\u0179\u017b\u0001\u0000"+
		"\u0000\u0000\u017a\u0178\u0001\u0000\u0000\u0000\u017b\u017c\u0005,\u0000"+
		"\u0000\u017cK\u0001\u0000\u0000\u0000\u017d\u017e\u0007\u0002\u0000\u0000"+
		"\u017e\u0181\u0003N\'\u0000\u017f\u0181\u0003N\'\u0000\u0180\u017d\u0001"+
		"\u0000\u0000\u0000\u0180\u017f\u0001\u0000\u0000\u0000\u0181M\u0001\u0000"+
		"\u0000\u0000\u0182\u0183\u0006\'\uffff\uffff\u0000\u0183\u0184\u0007\u0003"+
		"\u0000\u0000\u0184\u0189\u0003N\'\u0007\u0185\u0189\u0003P(\u0000\u0186"+
		"\u0189\u0003@ \u0000\u0187\u0189\u0003Z-\u0000\u0188\u0182\u0001\u0000"+
		"\u0000\u0000\u0188\u0185\u0001\u0000\u0000\u0000\u0188\u0186\u0001\u0000"+
		"\u0000\u0000\u0188\u0187\u0001\u0000\u0000\u0000\u0189\u0195\u0001\u0000"+
		"\u0000\u0000\u018a\u018b\n\u0006\u0000\u0000\u018b\u018c\u0007\u0004\u0000"+
		"\u0000\u018c\u0194\u0003N\'\u0007\u018d\u018e\n\u0005\u0000\u0000\u018e"+
		"\u018f\u0007\u0003\u0000\u0000\u018f\u0194\u0003N\'\u0006\u0190\u0191"+
		"\n\u0004\u0000\u0000\u0191\u0192\u0007\u0005\u0000\u0000\u0192\u0194\u0003"+
		"N\'\u0005\u0193\u018a\u0001\u0000\u0000\u0000\u0193\u018d\u0001\u0000"+
		"\u0000\u0000\u0193\u0190\u0001\u0000\u0000\u0000\u0194\u0197\u0001\u0000"+
		"\u0000\u0000\u0195\u0193\u0001\u0000\u0000\u0000\u0195\u0196\u0001\u0000"+
		"\u0000\u0000\u0196O\u0001\u0000\u0000\u0000\u0197\u0195\u0001\u0000\u0000"+
		"\u0000\u0198\u019f\u0003R)\u0000\u0199\u019a\u0005-\u0000\u0000\u019a"+
		"\u019b\u0003N\'\u0000\u019b\u019c\u0005.\u0000\u0000\u019c\u019e\u0001"+
		"\u0000\u0000\u0000\u019d\u0199\u0001\u0000\u0000\u0000\u019e\u01a1\u0001"+
		"\u0000\u0000\u0000\u019f\u019d\u0001\u0000\u0000\u0000\u019f\u01a0\u0001"+
		"\u0000\u0000\u0000\u01a0Q\u0001\u0000\u0000\u0000\u01a1\u019f\u0001\u0000"+
		"\u0000\u0000\u01a2\u01ad\u0005\u0019\u0000\u0000\u01a3\u01ad\u0005\u0015"+
		"\u0000\u0000\u01a4\u01ad\u0005\u0016\u0000\u0000\u01a5\u01ad\u0005\u0017"+
		"\u0000\u0000\u01a6\u01ad\u0005\u0018\u0000\u0000\u01a7\u01a8\u0005)\u0000"+
		"\u0000\u01a8\u01a9\u0003N\'\u0000\u01a9\u01aa\u0005*\u0000\u0000\u01aa"+
		"\u01ad\u0001\u0000\u0000\u0000\u01ab\u01ad\u0003T*\u0000\u01ac\u01a2\u0001"+
		"\u0000\u0000\u0000\u01ac\u01a3\u0001\u0000\u0000\u0000\u01ac\u01a4\u0001"+
		"\u0000\u0000\u0000\u01ac\u01a5\u0001\u0000\u0000\u0000\u01ac\u01a6\u0001"+
		"\u0000\u0000\u0000\u01ac\u01a7\u0001\u0000\u0000\u0000\u01ac\u01ab\u0001"+
		"\u0000\u0000\u0000\u01adS\u0001\u0000\u0000\u0000\u01ae\u01b0\u0005-\u0000"+
		"\u0000\u01af\u01b1\u0003V+\u0000\u01b0\u01af\u0001\u0000\u0000\u0000\u01b0"+
		"\u01b1\u0001\u0000\u0000\u0000\u01b1\u01b2\u0001\u0000\u0000\u0000\u01b2"+
		"\u01b3\u0005.\u0000\u0000\u01b3U\u0001\u0000\u0000\u0000\u01b4\u01b9\u0003"+
		"N\'\u0000\u01b5\u01b6\u0005(\u0000\u0000\u01b6\u01b8\u0003N\'\u0000\u01b7"+
		"\u01b5\u0001\u0000\u0000\u0000\u01b8\u01bb\u0001\u0000\u0000\u0000\u01b9"+
		"\u01b7\u0001\u0000\u0000\u0000\u01b9\u01ba\u0001\u0000\u0000\u0000\u01ba"+
		"W\u0001\u0000\u0000\u0000\u01bb\u01b9\u0001\u0000\u0000\u0000\u01bc\u01c1"+
		"\u0005\u0019\u0000\u0000\u01bd\u01be\u0005-\u0000\u0000\u01be\u01bf\u0003"+
		"N\'\u0000\u01bf\u01c0\u0005.\u0000\u0000\u01c0\u01c2\u0001\u0000\u0000"+
		"\u0000\u01c1\u01bd\u0001\u0000\u0000\u0000\u01c2\u01c3\u0001\u0000\u0000"+
		"\u0000\u01c3\u01c1\u0001\u0000\u0000\u0000\u01c3\u01c4\u0001\u0000\u0000"+
		"\u0000\u01c4Y\u0001\u0000\u0000\u0000\u01c5\u01c6\u0005)\u0000\u0000\u01c6"+
		"\u01c7\u0003\u001c\u000e\u0000\u01c7\u01c8\u0005*\u0000\u0000\u01c8\u01c9"+
		"\u0003N\'\u0000\u01c9[\u0001\u0000\u0000\u0000/]bnsz~\u0083\u0089\u0094"+
		"\u009a\u009f\u00a6\u00ad\u00b0\u00b3\u00b5\u00ba\u00c1\u00c9\u00cd\u00d8"+
		"\u00e3\u00f2\u00f6\u00fb\u0105\u0126\u012d\u0134\u0139\u0142\u014e\u0154"+
		"\u0156\u015c\u0165\u016e\u0178\u0180\u0188\u0193\u0195\u019f\u01ac\u01b0"+
		"\u01b9\u01c3";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}