/**
 * coderive.js — Pure JavaScript port of the Coderive language runtime
 * Browser-based REPL implementation replacing the CheerpJ (Java-in-browser) approach.
 *
 * Public API:
 *   CodREPLRunner.eval(line)  → string output
 *   CodREPLRunner.reset()     → void
 */
(function (global) {
  'use strict';

  // ============================================================
  // SECTION 1: TOKENIZER / LEXER
  // ============================================================

  const TT = Object.freeze({
    KEYWORD: 'KEYWORD',
    INT_LIT: 'INT_LIT',
    FLOAT_LIT: 'FLOAT_LIT',
    TEXT_LIT: 'TEXT_LIT',
    BOOL_LIT: 'BOOL_LIT',
    ID: 'ID',
    SYMBOL: 'SYMBOL',
    EOF: 'EOF',
    INVALID: 'INVALID',
    INTERPOL: 'INTERPOL',
  });

  // All Coderive keywords (lowercase)
  const KEYWORDS = new Set([
    'share', 'local', 'unit', 'use', 'is', 'this', 'super',
    'if', 'else', 'elif', 'of', 'in',
    'for', 'break', 'skip', 'to', 'by',
    'int', 'text', 'float', 'bool', 'type',
    'policy', 'with', 'builtin',
    'all', 'any',
    'exit', 'none', 'true', 'false',
    'get', 'set', 'control', 'unsafe',
    'i8', 'i16', 'i32', 'i64', 'u8', 'u16', 'u32', 'u64', 'f32', 'f64',
  ]);

  // Symbol patterns — longest match first
  const SYMBOLS = [
    [':=', 'DOUBLE_COLON_ASSIGN'],
    ['::', 'DOUBLE_COLON'],
    ['==', 'EQ'],
    ['>=', 'GTE'],
    ['<=', 'LTE'],
    ['!=', 'NEQ'],
    ['+=', 'PLUS_ASSIGN'],
    ['-=', 'MINUS_ASSIGN'],
    ['*=', 'MUL_ASSIGN'],
    ['/=', 'DIV_ASSIGN'],
    ['~>', 'TILDE_ARROW'],
    ['..', 'RANGE_DOTDOT'],
    [':', 'COLON'],
    ['=', 'ASSIGN'],
    ['>', 'GT'],
    ['<', 'LT'],
    ['!', 'BANG'],
    ['+', 'PLUS'],
    ['-', 'MINUS'],
    ['*', 'MUL'],
    ['/', 'DIV'],
    ['|', 'PIPE'],
    ['&', 'AMPERSAND'],
    ['?', 'QUESTION'],
    ['%', 'MOD'],
    ['.', 'DOT'],
    ['#', 'RANGE_HASH'],
    [',', 'COMMA'],
    ['(', 'LPAREN'],
    [')', 'RPAREN'],
    ['{', 'LBRACE'],
    ['}', 'RBRACE'],
    ['[', 'LBRACKET'],
    [']', 'RBRACKET'],
    ['_', 'UNDERSCORE'],
    ['\\', 'LAMBDA'],
  ];

  function tokenize(input) {
    const tokens = [];
    let pos = 0;
    const len = input.length;

    function peek(offset) {
      const i = pos + (offset || 0);
      return i < len ? input[i] : '\0';
    }

    function consume() {
      return input[pos++];
    }

    function lexNumber() {
      let text = '';
      let isFloat = false;

      while (pos < len && /\d/.test(peek())) text += consume();

      // Decimal point — but not '..'
      if (peek() === '.' && peek(1) !== '.' && /\d/.test(peek(1))) {
        isFloat = true;
        text += consume();
        while (pos < len && /\d/.test(peek())) text += consume();
      }

      // Scientific notation
      if (peek() === 'e' || peek() === 'E') {
        isFloat = true;
        text += consume();
        if (peek() === '+' || peek() === '-') text += consume();
        while (pos < len && /\d/.test(peek())) text += consume();
      }

      // Numeric suffixes: K M B T Q Qi
      const rest = input.slice(pos);
      const sfx = rest.match(/^(Qi|[KMBTQ])/u);
      if (sfx) {
        text += sfx[0];
        pos += sfx[0].length;
        isFloat = true;
      }

      return { type: isFloat ? TT.FLOAT_LIT : TT.INT_LIT, text, symbol: null, keyword: null, childTokens: null };
    }

    function lexIdentifier() {
      let text = '';
      while (pos < len && /[\w]/.test(peek())) text += consume();

      if (text === 'true' || text === 'false') {
        return { type: TT.BOOL_LIT, text, symbol: null, keyword: text, childTokens: null };
      }
      if (KEYWORDS.has(text)) {
        return { type: TT.KEYWORD, text, symbol: null, keyword: text, childTokens: null };
      }
      return { type: TT.ID, text, symbol: null, keyword: null, childTokens: null };
    }

    function lexStringParts(endChar, isMultiline) {
      const parts = [];
      let current = '';

      while (pos < len) {
        const ch = peek();

        if (ch === endChar && !isMultiline) {
          pos++;
          break;
        }
        if (isMultiline && ch === '"' && peek(1) === '|') {
          pos += 2;
          break;
        }

        if (ch === '\\') {
          pos++;
          const esc = consume();
          switch (esc) {
            case 'n': current += '\n'; break;
            case 't': current += '\t'; break;
            case 'r': current += '\r'; break;
            case '\\': current += '\\'; break;
            case '"': current += '"'; break;
            case '{': current += '{'; break;
            default: current += '\\' + esc; break;
          }
          continue;
        }

        if (ch === '{') {
          if (current.length > 0) {
            parts.push({ type: TT.TEXT_LIT, text: current, symbol: null, keyword: null, childTokens: null });
            current = '';
          }
          pos++; // skip '{'
          let depth = 1;
          let expr = '';
          while (pos < len && depth > 0) {
            const ec = peek();
            if (ec === '{') depth++;
            else if (ec === '}') {
              depth--;
              if (depth === 0) { pos++; break; }
            }
            expr += consume();
          }
          const exprTokens = tokenize(expr.trim()).filter(t => t.type !== TT.EOF);
          parts.push({ type: TT.INTERPOL, text: expr, symbol: null, keyword: null, childTokens: exprTokens });
          continue;
        }

        current += consume();
      }

      if (current.length > 0) {
        parts.push({ type: TT.TEXT_LIT, text: current, symbol: null, keyword: null, childTokens: null });
      }

      return parts;
    }

    function lexString() {
      pos++; // skip opening '"'
      const parts = lexStringParts('"', false);

      if (parts.length === 0) return { type: TT.TEXT_LIT, text: '', symbol: null, keyword: null, childTokens: null };
      if (parts.length === 1 && parts[0].type === TT.TEXT_LIT) return parts[0];
      return { type: TT.INTERPOL, text: '', symbol: null, keyword: null, childTokens: parts };
    }

    function lexMultilineString() {
      pos += 2; // skip |"
      // Skip whitespace/rest of first line
      while (pos < len && peek() !== '\n') pos++;
      if (pos < len) pos++; // skip \n

      const parts = lexStringParts(null, true);

      if (parts.length === 0) return { type: TT.TEXT_LIT, text: '', symbol: null, keyword: null, childTokens: null };
      if (parts.length === 1 && parts[0].type === TT.TEXT_LIT) return parts[0];
      return { type: TT.INTERPOL, text: '', symbol: null, keyword: null, childTokens: parts };
    }

    while (pos < len) {
      const c = peek();

      // Whitespace
      if (/\s/.test(c)) { pos++; continue; }

      // Line comment
      if (c === '/' && peek(1) === '/') {
        while (pos < len && peek() !== '\n') pos++;
        continue;
      }

      // Block comment
      if (c === '/' && peek(1) === '*') {
        pos += 2;
        while (pos < len - 1 && !(peek() === '*' && peek(1) === '/')) pos++;
        if (pos < len) pos += 2;
        continue;
      }

      // Multiline string |"..."| 
      if (c === '|' && peek(1) === '"') {
        tokens.push(lexMultilineString());
        continue;
      }

      // String
      if (c === '"') {
        tokens.push(lexString());
        continue;
      }

      // Number
      if (/\d/.test(c)) {
        tokens.push(lexNumber());
        continue;
      }

      // Identifier / keyword / boolean
      if (/[a-zA-Z_]/.test(c)) {
        tokens.push(lexIdentifier());
        continue;
      }

      // Symbols (longest match first)
      let matched = false;
      for (const [pat, sym] of SYMBOLS) {
        if (input.startsWith(pat, pos)) {
          pos += pat.length;
          tokens.push({ type: TT.SYMBOL, text: pat, symbol: sym, keyword: null, childTokens: null });
          matched = true;
          break;
        }
      }
      if (matched) continue;

      // Invalid character
      tokens.push({ type: TT.INVALID, text: consume(), symbol: null, keyword: null, childTokens: null });
    }

    tokens.push({ type: TT.EOF, text: '', symbol: null, keyword: null, childTokens: null });
    return tokens;
  }

  // ============================================================
  // SECTION 2: AST NODE TYPES
  // ============================================================

  const N = Object.freeze({
    PROGRAM: 'ProgramNode',
    UNIT: 'UnitNode',
    TYPE: 'TypeNode',
    METHOD: 'MethodNode',
    PARAM: 'ParamNode',
    FIELD: 'FieldNode',
    BLOCK: 'BlockNode',
    STMT_IF: 'StmtIfNode',
    EXPR_IF: 'ExprIfNode',
    FOR: 'ForNode',
    RANGE: 'RangeNode',
    RANGE_INDEX: 'RangeIndexNode',
    BREAK: 'BreakNode',
    SKIP: 'SkipNode',
    EXIT: 'ExitNode',
    VAR: 'VarNode',
    ASSIGN: 'AssignmentNode',
    SLOT_ASSIGN: 'SlotAssignmentNode',
    MULTI_SLOT: 'MultipleSlotAssignmentNode',
    RET_SLOT: 'ReturnSlotAssignmentNode',
    SLOT: 'SlotNode',
    IDENT: 'IdentifierNode',
    INT_LIT: 'IntLiteralNode',
    FLOAT_LIT: 'FloatLiteralNode',
    TEXT_LIT: 'TextLiteralNode',
    BOOL_LIT: 'BoolLiteralNode',
    NONE_LIT: 'NoneLiteralNode',
    BINARY_OP: 'BinaryOpNode',
    UNARY: 'UnaryNode',
    METHOD_CALL: 'MethodCallNode',
    ARRAY: 'ArrayNode',
    INDEX: 'IndexAccessNode',
    PROP: 'PropertyAccessNode',
    TYPE_CAST: 'TypeCastNode',
    EQUALITY_CHAIN: 'EqualityChainNode',
    BOOL_CHAIN: 'BooleanChainNode',
    CONSTRUCTOR_CALL: 'ConstructorCallNode',
    THIS: 'ThisNode',
    SUPER: 'SuperNode',
    LAMBDA: 'LambdaNode',
    TUPLE: 'TupleNode',
  });

  function mkNode(type, props) {
    return Object.assign({ _type: type }, props);
  }

  // Expression node types (result is returned in REPL)
  const EXPR_TYPES = new Set([
    N.INT_LIT, N.FLOAT_LIT, N.TEXT_LIT, N.BOOL_LIT, N.NONE_LIT,
    N.IDENT, N.BINARY_OP, N.UNARY, N.METHOD_CALL, N.INDEX,
    N.PROP, N.ARRAY, N.TYPE_CAST, N.EQUALITY_CHAIN, N.BOOL_CHAIN,
    N.EXPR_IF, N.CONSTRUCTOR_CALL, N.THIS, N.SUPER, N.LAMBDA,
    N.TUPLE, N.RANGE,
  ]);

  function isExprNode(node) {
    return node && EXPR_TYPES.has(node._type);
  }

  // ============================================================
  // SECTION 3: PARSER
  // ============================================================

  class ParseError extends Error {
    constructor(msg) { super(msg); this.name = 'ParseError'; }
  }

  class Parser {
    constructor(tokens) {
      this.tokens = tokens;
      this.pos = 0;
    }

    now() {
      return this.tokens[this.pos] || { type: TT.EOF, text: '', symbol: null, keyword: null };
    }

    next(offset) {
      const o = (offset === undefined ? 1 : offset);
      return this.tokens[this.pos + o] || { type: TT.EOF, text: '', symbol: null, keyword: null };
    }

    consume() {
      const t = this.now();
      if (t.type !== TT.EOF) this.pos++;
      return t;
    }

    atEOF() { return this.now().type === TT.EOF; }

    isKw(kw) { const t = this.now(); return t.type === TT.KEYWORD && t.keyword === kw; }
    isSym(sym) { const t = this.now(); return t.type === TT.SYMBOL && t.symbol === sym; }
    isId() { return this.now().type === TT.ID; }

    isKwAt(kw, offset) {
      const t = this.next(offset);
      return t.type === TT.KEYWORD && t.keyword === kw;
    }

    isSymAt(sym, offset) {
      const t = this.next(offset);
      return t.type === TT.SYMBOL && t.symbol === sym;
    }

    expectType(type) {
      const t = this.now();
      if (t.type !== type) throw new ParseError(`Expected ${type}, got ${t.type} ('${t.text}')`);
      return this.consume();
    }

    expectKw(kw) {
      const t = this.now();
      if (t.type !== TT.KEYWORD || t.keyword !== kw) {
        throw new ParseError(`Expected keyword '${kw}', got '${t.text}'`);
      }
      return this.consume();
    }

    expectSym(sym) {
      const t = this.now();
      if (t.type !== TT.SYMBOL || t.symbol !== sym) {
        throw new ParseError(`Expected '${sym}', got '${t.text}'`);
      }
      return this.consume();
    }

    consumeIfKw(kw) {
      if (this.isKw(kw)) { this.consume(); return true; }
      return false;
    }

    consumeIfSym(sym) {
      if (this.isSym(sym)) { this.consume(); return true; }
      return false;
    }

    save() { return this.pos; }
    restore(p) { this.pos = p; }

    // Try a parse function — restore position on failure, return null
    tryParse(fn) {
      const saved = this.save();
      try { return fn(); }
      catch (e) { this.restore(saved); return null; }
    }

    // ── Entry point ──────────────────────────────────────────

    parseProgram() {
      const statements = [];
      while (!this.atEOF()) {
        const t = this.now();
        if (t.type === TT.INVALID) {
          throw new ParseError(`Invalid token: '${t.text}'`);
        }
        statements.push(this.parseStmt());
      }
      return mkNode(N.PROGRAM, { statements });
    }

    parseSingleLine() {
      if (this.atEOF()) return null;
      if (this.now().type === TT.INVALID) {
        throw new ParseError(`Invalid token: '${this.now().text}'`);
      }
      const stmt = this.parseStmt();
      const trailing = this.now();
      if (trailing && trailing.type !== TT.EOF) {
        if (trailing.type === TT.INVALID) {
          throw new ParseError(`Invalid token: '${trailing.text}'`);
        }
        throw new ParseError(`Unexpected token: '${trailing.text}' (${trailing.type})`);
      }
      return stmt;
    }

    // ── Statements ───────────────────────────────────────────

    parseStmt() {
      const t = this.now();

      if (t.type === TT.KEYWORD) {
        if (t.keyword === 'if')    return this.parseIfStmt();
        if (t.keyword === 'for')   return this.parseForStmt();
        if (t.keyword === 'exit')  { this.consume(); return mkNode(N.EXIT, {}); }
        if (t.keyword === 'break') { this.consume(); return mkNode(N.BREAK, {}); }
        if (t.keyword === 'skip')  { this.consume(); return mkNode(N.SKIP, {}); }
        if (t.keyword === 'share' || t.keyword === 'local' || t.keyword === 'builtin') {
          return this.parseClassOrMethodDecl();
        }
        if (t.keyword === 'policy') {
          return this.parsePolicyDecl();
        }
        if (t.keyword === 'unit') {
          this.consume(); // skip 'unit'
          while (!this.atEOF() && !this.isKw('share') && !this.isKw('local')) this.consume();
          return mkNode(N.BLOCK, { statements: [] });
        }
      }

      if (t.type === TT.SYMBOL && t.symbol === 'TILDE_ARROW') {
        return this.parseSlotAssignment();
      }

      if (t.type === TT.ID) {
        const t2 = this.next(1);

        // Index assignment: arr[idx] = expr
        if (t2.type === TT.SYMBOL && t2.symbol === 'LBRACKET') {
          const r = this.tryParse(() => this.parseIndexAssignment());
          if (r) return r;
        }

        // Variable declaration: name: type [= expr]  or  name := expr
        if ((t2.type === TT.SYMBOL && t2.symbol === 'COLON') ||
            (t2.type === TT.SYMBOL && t2.symbol === 'DOUBLE_COLON_ASSIGN')) {
          return this.parseVarDecl();
        }

        // Simple assignment or return-slot assignment: name = ...
        if (t2.type === TT.SYMBOL && t2.symbol === 'ASSIGN') {
          const r = this.tryParse(() => this.parseReturnSlotAssignment());
          if (r) return r;
          return this.parseSimpleAssignment();
        }

        // Multi-name return-slot: a, b = [s]:method()
        if (t2.type === TT.SYMBOL && t2.symbol === 'COMMA') {
          const r = this.tryParse(() => this.parseReturnSlotAssignment());
          if (r) return r;
        }

        // Compound assignment: +=, -=, *=, /=
        if (t2.type === TT.SYMBOL &&
            ['PLUS_ASSIGN', 'MINUS_ASSIGN', 'MUL_ASSIGN', 'DIV_ASSIGN'].includes(t2.symbol)) {
          return this.parseCompoundAssignment();
        }
      }

      return this.parseExprStmt();
    }

    parseCompoundAssignment() {
      const nameToken = this.expectType(TT.ID);
      const name = nameToken.text;
      const opToken = this.consume(); // +=, -=, etc.
      const baseOp = { 'PLUS_ASSIGN': '+', 'MINUS_ASSIGN': '-', 'MUL_ASSIGN': '*', 'DIV_ASSIGN': '/' }[opToken.symbol];
      const rhs = this.parseExpr();
      const target = mkNode(N.IDENT, { name });
      const combined = mkNode(N.BINARY_OP, { left: target, op: baseOp, right: rhs });
      return mkNode(N.ASSIGN, { left: mkNode(N.IDENT, { name }), right: combined });
    }

    parseClassOrMethodDecl() {
      const vis = this.consume().keyword; // share | local | builtin

      const t = this.now();

      // Method: share methodName(params) :: slots { body }
      if (t.type === TT.ID) {
        const name = this.consume().text;

        if (this.isSym('LPAREN')) {
          return this.parseMethodDecl(vis, name, null);
        }

        // Class: share ClassName { ... }
        if (this.isSym('LBRACE')) {
          return this.parseClassBody(vis, name, null);
        }

        // Class with inheritance: share ClassName is Parent { ... }
        if (this.isKw('is')) {
          this.consume();
          const parent = this.expectType(TT.ID).text;
          return this.parseClassBody(vis, name, parent);
        }

        // Class with policy: share ClassName with PolicyName { ... }
        if (this.isKw('with')) {
          this.consume();
          // consume policy list
          while (!this.isSym('LBRACE') && !this.atEOF()) this.consume();
          return this.parseClassBody(vis, name, null);
        }
      }

      // Fallback: skip until we find a stable point
      return mkNode(N.BLOCK, { statements: [] });
    }

    parsePolicyDecl() {
      this.consume(); // 'policy'
      // Skip entire policy declaration
      let depth = 0;
      while (!this.atEOF()) {
        if (this.isSym('LBRACE')) { depth++; this.consume(); }
        else if (this.isSym('RBRACE')) { depth--; this.consume(); if (depth <= 0) break; }
        else this.consume();
      }
      return mkNode(N.BLOCK, { statements: [] });
    }

    parseMethodDecl(vis, name, className) {
      this.expectSym('LPAREN');
      const params = [];
      if (!this.isSym('RPAREN')) {
        params.push(this.parseParam());
        while (this.consumeIfSym('COMMA')) params.push(this.parseParam());
      }
      this.expectSym('RPAREN');

      let returnSlots = null;
      if (this.isSym('DOUBLE_COLON')) {
        this.consume();
        returnSlots = this.parseSlotContract();
      }

      const body = this.parseBlock();
      return mkNode(N.METHOD, { name, visibility: vis, params, returnSlots, body, className });
    }

    parseParam() {
      const name = this.expectType(TT.ID).text;
      let type = null;
      if (this.consumeIfSym('COLON')) type = this.parseTypeRef();
      let defaultValue = null;
      if (this.consumeIfSym('ASSIGN')) defaultValue = this.parseExpr();
      return mkNode(N.PARAM, { name, type, defaultValue });
    }

    parseSlotContract() {
      const slots = [];
      do {
        const name = this.expectType(TT.ID).text;
        this.expectSym('COLON');
        const type = this.parseTypeRef();
        slots.push(mkNode(N.SLOT, { name, type }));
      } while (this.consumeIfSym('COMMA'));
      return slots;
    }

    parseClassBody(vis, name, parent) {
      this.expectSym('LBRACE');
      const fields = [];
      const methods = [];

      while (!this.isSym('RBRACE') && !this.atEOF()) {
        const t = this.now();

        if (t.type === TT.KEYWORD &&
            (t.keyword === 'share' || t.keyword === 'local' || t.keyword === 'builtin')) {
          const mVis = this.consume().keyword;
          const mName = this.consume().text;
          if (this.isSym('LPAREN')) {
            methods.push(this.parseMethodDecl(mVis, mName, name));
          } else if (this.isSym('COLON')) {
            this.consume();
            const fType = this.parseTypeRef();
            let fVal = null;
            if (this.consumeIfSym('ASSIGN')) fVal = this.parseExpr();
            fields.push(mkNode(N.FIELD, { name: mName, visibility: mVis, type: fType, value: fVal }));
          }
          continue;
        }

        if (t.type === TT.ID) {
          const fName = this.consume().text;
          let fType = null;
          let fVal = null;
          if (this.consumeIfSym('COLON')) fType = this.parseTypeRef();
          if (this.consumeIfSym('ASSIGN')) fVal = this.parseExpr();
          fields.push(mkNode(N.FIELD, { name: fName, type: fType, value: fVal }));
          continue;
        }

        // Skip unknown tokens inside class
        this.consume();
      }

      this.consumeIfSym('RBRACE');
      return mkNode(N.TYPE, { name, visibility: vis, parent, fields, methods });
    }

    parseTypeRef() {
      const t = this.now();

      if (this.consumeIfSym('LBRACKET')) {
        if (this.consumeIfSym('RBRACKET')) return '[]';
        const inner = this.parseTypeRef();
        this.expectSym('RBRACKET');
        return `[${inner}]`;
      }

      const primitives = new Set(['int', 'text', 'float', 'bool', 'type', 'none',
        'i8', 'i16', 'i32', 'i64', 'u8', 'u16', 'u32', 'u64', 'f32', 'f64']);

      if (t.type === TT.KEYWORD && primitives.has(t.keyword)) {
        let typeName = this.consume().text;
        if (this.consumeIfSym('QUESTION')) return typeName + '|none';
        while (this.consumeIfSym('PIPE')) typeName += '|' + this.parseTypeRef();
        return typeName;
      }

      if (t.type === TT.ID) {
        let typeName = this.consume().text;
        if (this.consumeIfSym('QUESTION')) return typeName + '|none';
        while (this.consumeIfSym('PIPE')) typeName += '|' + this.parseTypeRef();
        return typeName;
      }

      throw new ParseError(`Expected type name, got '${t.text}'`);
    }

    parseBlock() {
      this.expectSym('LBRACE');
      const stmts = [];
      while (!this.isSym('RBRACE') && !this.atEOF()) {
        stmts.push(this.parseStmt());
      }
      this.expectSym('RBRACE');
      return mkNode(N.BLOCK, { statements: stmts });
    }

    parseBlockOrSingle() {
      if (this.isSym('LBRACE')) return this.parseBlock();
      const stmt = this.parseStmt();
      return mkNode(N.BLOCK, { statements: [stmt] });
    }

    parseIfStmt() {
      this.expectKw('if');
      const cond = this.parseExpr();
      const thenBlock = this.parseBlockOrSingle();
      const elseBlock = mkNode(N.BLOCK, { statements: [] });

      if (this.isKw('elif')) {
        elseBlock.statements.push(this.parseElifChain());
      } else if (this.isKw('else')) {
        this.consume();
        if (this.isKw('if')) {
          elseBlock.statements.push(this.parseIfStmt());
        } else {
          const eb = this.parseBlockOrSingle();
          elseBlock.statements.push(...eb.statements);
        }
      }

      return mkNode(N.STMT_IF, { condition: cond, thenBlock, elseBlock });
    }

    parseElifChain() {
      this.expectKw('elif');
      const cond = this.parseExpr();
      const thenBlock = this.parseBlockOrSingle();
      const elseBlock = mkNode(N.BLOCK, { statements: [] });

      if (this.isKw('elif')) {
        elseBlock.statements.push(this.parseElifChain());
      } else if (this.isKw('else')) {
        this.consume();
        if (this.isKw('if')) {
          elseBlock.statements.push(this.parseIfStmt());
        } else {
          const eb = this.parseBlockOrSingle();
          elseBlock.statements.push(...eb.statements);
        }
      }

      return mkNode(N.STMT_IF, { condition: cond, thenBlock, elseBlock });
    }

    parseForStmt() {
      this.expectKw('for');
      const iterator = this.expectType(TT.ID).text;

      // Accept both 'of' (Java source) and 'in' (task examples)
      if (this.isKw('of') || this.isKw('in')) {
        this.consume();
      } else {
        throw new ParseError("Expected 'of' or 'in' after loop iterator");
      }

      const source = this.parseExpr();

      let range = null;
      let arraySource = null;

      if (this.isSym('RANGE_DOTDOT') || this.isKw('to')) {
        this.consume(); // consume '..' or 'to'
        const end = this.parseExpr();
        let step = null;
        if (this.isKw('by') || this.isSym('RANGE_HASH')) {
          this.consume();
          step = this.parseExpr();
        }
        range = mkNode(N.RANGE, { start: source, end, step });
      } else {
        arraySource = source;
      }

      const body = this.parseBlockOrSingle();
      return mkNode(N.FOR, { iterator, range, arraySource, body });
    }

    parseVarDecl() {
      const varName = this.expectType(TT.ID).text;
      let explicitType = null;
      let value = null;
      let isConst = false;

      if (this.consumeIfSym('DOUBLE_COLON_ASSIGN')) {
        // name := expr
        isConst = /^[A-Z][A-Z0-9_]*$/.test(varName);
        value = this.parseExpr();
      } else if (this.consumeIfSym('COLON')) {
        // name: type  or  name: type = expr
        explicitType = this.parseTypeRef();
        if (this.consumeIfSym('ASSIGN')) value = this.parseExpr();
      } else {
        throw new ParseError(`Unexpected token in variable declaration: '${this.now().text}'`);
      }

      return mkNode(N.VAR, { name: varName, explicitType, value, isConst });
    }

    parseSimpleAssignment() {
      const name = this.expectType(TT.ID).text;
      this.expectSym('ASSIGN');
      const value = this.parseExpr();
      return mkNode(N.ASSIGN, { left: mkNode(N.IDENT, { name }), right: value });
    }

    parseIndexAssignment() {
      const arrName = this.expectType(TT.ID).text;
      this.expectSym('LBRACKET');
      const idx = this.parseExpr();
      this.expectSym('RBRACKET');
      this.expectSym('ASSIGN');
      const value = this.parseExpr();
      const arrExpr = mkNode(N.IDENT, { name: arrName });
      const target = mkNode(N.INDEX, { array: arrExpr, index: idx });
      return mkNode(N.ASSIGN, { left: target, right: value });
    }

    parseReturnSlotAssignment() {
      // Parses:  name = [s]:method()  or  n1, n2 = [s1, s2]:method()
      const names = [this.expectType(TT.ID).text];
      while (this.consumeIfSym('COMMA')) names.push(this.expectType(TT.ID).text);
      this.expectSym('ASSIGN');
      if (!this.isSym('LBRACKET')) throw new ParseError('Expected [slots]:method()');
      const slotNames = this.parseReturnSlots();
      this.expectSym('COLON');
      const call = this.parseCallExpr();
      call.slotNames = slotNames;
      return mkNode(N.RET_SLOT, { variableNames: names, methodCall: call });
    }

    parseReturnSlots() {
      this.expectSym('LBRACKET');
      const slots = [];
      do {
        if (this.now().type === TT.ID) slots.push(this.consume().text);
        else if (this.now().type === TT.INT_LIT) slots.push(this.consume().text);
        else throw new ParseError('Expected slot name or index');
      } while (this.consumeIfSym('COMMA'));
      this.expectSym('RBRACKET');
      return slots;
    }

    parseSlotAssignment() {
      this.consume(); // ~>
      const assignments = [this.parseSingleSlot()];
      while (this.consumeIfSym('COMMA')) assignments.push(this.parseSingleSlot());

      if (assignments.length === 1) return assignments[0];
      return mkNode(N.MULTI_SLOT, { assignments });
    }

    parseSingleSlot() {
      // Named: name: expr  or  positional: expr
      const saved = this.save();
      if (this.now().type === TT.ID && this.isSymAt('COLON', 1)) {
        const slotName = this.consume().text;
        this.consume(); // ':'
        const value = this.parseExpr();
        return mkNode(N.SLOT_ASSIGN, { slotName, value });
      }
      this.restore(saved);
      const value = this.parseExpr();
      return mkNode(N.SLOT_ASSIGN, { slotName: null, value });
    }

    parseExprStmt() {
      return this.parseExpr();
    }

    // ── Expressions ──────────────────────────────────────────

    parseExpr() {
      // all[...] / any[...]
      if ((this.isKw('all') || this.isKw('any')) && this.isSymAt('LBRACKET', 1)) {
        return this.parseBooleanChain();
      }
      // if expr
      if (this.isKw('if')) return this.parseIfExpr();

      return this.parsePrecedence(10);
    }

    parseIfExpr() {
      this.expectKw('if');
      const cond = this.parseExpr();
      let thenExpr;
      if (this.isSym('LBRACE')) {
        this.consume(); thenExpr = this.parseExpr(); this.expectSym('RBRACE');
      } else {
        thenExpr = this.parseExpr();
      }
      this.expectKw('else');
      let elseExpr;
      if (this.isSym('LBRACE')) {
        this.consume(); elseExpr = this.parseExpr(); this.expectSym('RBRACE');
      } else {
        elseExpr = this.parseExpr();
      }
      return mkNode(N.EXPR_IF, { condition: cond, thenExpr, elseExpr });
    }

    parseBooleanChain() {
      const isAll = this.now().keyword === 'all';
      this.consume(); // all | any
      this.expectSym('LBRACKET');
      const exprs = [];
      if (!this.isSym('RBRACKET')) {
        exprs.push(this.parseExpr());
        while (this.consumeIfSym('COMMA')) exprs.push(this.parseExpr());
      }
      this.expectSym('RBRACKET');
      return mkNode(N.BOOL_CHAIN, { isAll, expressions: exprs });
    }

    getPrecedence(t) {
      if (!t) return 0;
      if (t.type === TT.KEYWORD && t.keyword === 'is') return 40;
      if (t.type !== TT.SYMBOL) return 0;
      switch (t.symbol) {
        case 'EQ':  case 'NEQ': return 50;
        case 'LT':  case 'GT': case 'LTE': case 'GTE': return 60;
        case 'PLUS': case 'MINUS': return 70;
        case 'MUL':  case 'DIV': case 'MOD': return 80;
        default: return 0;
      }
    }

    isCmpOp(t) {
      if (!t || t.type !== TT.SYMBOL) return false;
      return ['EQ', 'NEQ', 'LT', 'GT', 'LTE', 'GTE'].includes(t.symbol);
    }

    isChainFollows() {
      // Does next(1) start an all/any chain?
      const t = this.next(1);
      return t.type === TT.KEYWORD && (t.keyword === 'all' || t.keyword === 'any');
    }

    parsePrecedence(minPrec) {
      let left = this.parsePrefix();

      while (true) {
        const op = this.now();
        const prec = this.getPrecedence(op);

        if (prec < minPrec) break;

        // Equality chain: left <cmp> all[...] / any[...]
        if (this.isCmpOp(op) && this.isChainFollows()) {
          return this.parseEqualityChain(left);
        }

        if (prec > 0) {
          const opToken = this.consume();
          const opText = opToken.type === TT.KEYWORD ? opToken.keyword : opToken.text;
          const right = this.parsePrecedence(prec + 1);
          left = mkNode(N.BINARY_OP, { left, op: opText, right });
          continue;
        }

        break;
      }

      return left;
    }

    parseEqualityChain(left) {
      const opToken = this.consume();
      const op = opToken.text;
      const chainToken = this.consume(); // all | any
      const isAll = chainToken.keyword === 'all';

      const chainArgs = [];
      if (this.isSym('LBRACKET')) {
        this.consume();
        if (!this.isSym('RBRACKET')) {
          chainArgs.push(this.parseExpr());
          while (this.consumeIfSym('COMMA')) chainArgs.push(this.parseExpr());
        }
        this.expectSym('RBRACKET');
      } else if (this.now().type === TT.ID) {
        chainArgs.push(mkNode(N.IDENT, { name: this.consume().text }));
      } else {
        throw new ParseError("Expected '[' or identifier after all/any in equality chain");
      }

      return mkNode(N.EQUALITY_CHAIN, {
        left, operator: op, isAllChain: isAll, chainArguments: chainArgs,
      });
    }

    parsePrefix() {
      const t = this.now();
      if (t.type === TT.SYMBOL &&
          (t.symbol === 'BANG' || t.symbol === 'MINUS' || t.symbol === 'PLUS')) {
        const opToken = this.consume();
        const operand = this.parsePrecedence(90);
        return mkNode(N.UNARY, { op: opToken.text, operand });
      }
      return this.parsePostfix();
    }

    parsePostfix() {
      let expr = this.parsePrimary();

      while (true) {
        // Index: expr[...]
        if (this.isSym('LBRACKET')) {
          const saved = this.save();
          try {
            this.consume(); // [
            if (this.isRangeStart()) {
              const ri = this.parseRangeIndex();
              this.expectSym('RBRACKET');
              expr = mkNode(N.INDEX, { array: expr, index: ri });
            } else {
              const idx = this.parseExpr();
              this.expectSym('RBRACKET');
              expr = mkNode(N.INDEX, { array: expr, index: idx });
            }
            continue;
          } catch (e) {
            this.restore(saved);
            break;
          }
        }

        // Property access or method call: expr.name  /  expr.method(args)
        if (this.isSym('DOT')) {
          this.consume(); // .
          const propName = this.consume().text;
          if (this.isSym('LPAREN')) {
            this.consume(); // (
            const args = [];
            if (!this.isSym('RPAREN')) {
              args.push(this.parseExpr());
              while (this.consumeIfSym('COMMA')) args.push(this.parseExpr());
            }
            this.expectSym('RPAREN');
            const call = mkNode(N.METHOD_CALL, {
              name: propName, qualifiedName: propName, arguments: args,
              argNames: args.map(() => null), slotNames: [],
              isGlobal: false, target: expr, isSuperCall: false,
            });
            expr = call;
          } else {
            expr = mkNode(N.PROP, {
              left: expr, right: mkNode(N.IDENT, { name: propName }),
            });
          }
          continue;
        }

        break;
      }

      return expr;
    }

    isRangeStart() {
      const saved = this.save();
      try {
        if (!this.isExprStart()) return false;
        this.parseExpr();
        return this.isSym('RANGE_DOTDOT') || this.isKw('to');
      } catch (e) {
        return false;
      } finally {
        this.restore(saved);
      }
    }

    parseRangeIndex() {
      const start = this.parseExpr();
      if (!this.consumeIfSym('RANGE_DOTDOT') && !this.consumeIfKw('to')) {
        throw new ParseError('Expected range operator in index');
      }
      const end = this.parseExpr();
      let step = null;
      if (this.isKw('by') || this.isSym('RANGE_HASH')) {
        this.consume(); step = this.parseExpr();
      }
      return mkNode(N.RANGE_INDEX, { start, end, step });
    }

    consumeIfKw(kw) {
      if (this.isKw(kw)) { this.consume(); return true; }
      return false;
    }

    isExprStart() {
      const t = this.now();
      if (!t) return false;
      if (t.type === TT.INT_LIT || t.type === TT.FLOAT_LIT ||
          t.type === TT.TEXT_LIT || t.type === TT.BOOL_LIT ||
          t.type === TT.INTERPOL || t.type === TT.ID) return true;
      if (t.type === TT.SYMBOL &&
          ['LPAREN', 'LBRACKET', 'BANG', 'PLUS', 'MINUS', 'LAMBDA'].includes(t.symbol)) return true;
      if (t.type === TT.KEYWORD &&
          ['none', 'true', 'false', 'this', 'super', 'if', 'all', 'any',
           'int', 'text', 'float', 'bool'].includes(t.keyword)) return true;
      return false;
    }

    parsePrimary() {
      const t = this.now();
      if (!t || t.type === TT.EOF) throw new ParseError('Unexpected end of input in expression');

      // Lambda: \(params) ~> expr  or  \(params) { body }
      if (t.type === TT.SYMBOL && t.symbol === 'LAMBDA') {
        return this.parseLambda();
      }

      // super
      if (t.type === TT.KEYWORD && t.keyword === 'super') {
        this.consume();
        return mkNode(N.SUPER, {});
      }

      // this
      if (t.type === TT.KEYWORD && t.keyword === 'this') {
        this.consume();
        return mkNode(N.THIS, { className: null });
      }

      // Integer literal
      if (t.type === TT.INT_LIT) {
        this.consume();
        return mkNode(N.INT_LIT, { value: this.parseNumericSuffix(t.text, false) });
      }

      // Float literal
      if (t.type === TT.FLOAT_LIT) {
        this.consume();
        return mkNode(N.FLOAT_LIT, { value: this.parseNumericSuffix(t.text, true) });
      }

      // Text literal
      if (t.type === TT.TEXT_LIT) {
        this.consume();
        return mkNode(N.TEXT_LIT, { value: t.text });
      }

      // Interpolated string
      if (t.type === TT.INTERPOL) {
        this.consume();
        return this.buildInterpol(t);
      }

      // Bool literal
      if (t.type === TT.BOOL_LIT) {
        this.consume();
        return mkNode(N.BOOL_LIT, { value: t.text === 'true' });
      }

      // none
      if (t.type === TT.KEYWORD && t.keyword === 'none') {
        this.consume();
        return mkNode(N.NONE_LIT, {});
      }

      // true / false as keywords
      if (t.type === TT.KEYWORD && t.keyword === 'true') { this.consume(); return mkNode(N.BOOL_LIT, { value: true }); }
      if (t.type === TT.KEYWORD && t.keyword === 'false') { this.consume(); return mkNode(N.BOOL_LIT, { value: false }); }

      // Type cast used as function: int(x), text(x), float(x), bool(x)
      if (t.type === TT.KEYWORD && ['int', 'text', 'float', 'bool'].includes(t.keyword)) {
        if (this.isSymAt('LPAREN', 1)) {
          const typeName = this.consume().text;
          this.consume(); // (
          const arg = this.parseExpr();
          this.expectSym('RPAREN');
          return mkNode(N.TYPE_CAST, { targetType: typeName, expression: arg });
        }
      }

      // Parenthesized expression  or  type cast: (type) expr
      if (t.type === TT.SYMBOL && t.symbol === 'LPAREN') {
        return this.parseParenOrCast();
      }

      // Array literal
      if (t.type === TT.SYMBOL && t.symbol === 'LBRACKET') {
        // Check for [slot]:method() syntax
        if (this.isSlotCallExpression()) {
          return this.parseSlotCallExpression();
        }
        return this.parseArrayLiteral();
      }

      // all/any — either as method call or boolean chain
      if (t.type === TT.KEYWORD && (t.keyword === 'all' || t.keyword === 'any')) {
        if (this.isSymAt('LPAREN', 1)) {
          return this.parseCallByName(t.keyword);
        }
        return this.parseBooleanChain();
      }

      // Identifier, method call, constructor call
      if (t.type === TT.ID) {
        return this.parseIdentOrCall();
      }

      throw new ParseError(`Unexpected token: '${t.text}' (${t.type})`);
    }

    parseNumericSuffix(text, isFloat) {
      if (text.endsWith('Qi')) return parseFloat(text) * 1e18;
      if (text.endsWith('K')) return parseFloat(text) * 1e3;
      if (text.endsWith('M')) return parseFloat(text) * 1e6;
      if (text.endsWith('B')) return parseFloat(text) * 1e9;
      if (text.endsWith('T')) return parseFloat(text) * 1e12;
      if (text.endsWith('Q')) return parseFloat(text) * 1e15;
      return isFloat ? parseFloat(text) : parseInt(text, 10);
    }

    buildInterpol(token) {
      // token.childTokens: array of TEXT_LIT and INTERPOL sub-tokens
      const parts = token.childTokens;
      if (!parts || parts.length === 0) return mkNode(N.TEXT_LIT, { value: '' });

      let result = null;
      for (const part of parts) {
        let node;
        if (part.type === TT.TEXT_LIT) {
          node = mkNode(N.TEXT_LIT, { value: part.text });
        } else {
          // INTERPOL: parse its childTokens as an expression
          node = this.parseInnerTokens(part.childTokens);
        }
        result = result === null ? node : mkNode(N.BINARY_OP, { left: result, op: '+', right: node });
      }

      return result || mkNode(N.TEXT_LIT, { value: '' });
    }

    parseInnerTokens(innerTokens) {
      if (!innerTokens || innerTokens.length === 0) return mkNode(N.TEXT_LIT, { value: '' });
      const savedTokens = this.tokens;
      const savedPos = this.pos;
      this.tokens = [...innerTokens, { type: TT.EOF, text: '', symbol: null, keyword: null, childTokens: null }];
      this.pos = 0;
      try {
        return this.parseExpr();
      } finally {
        this.tokens = savedTokens;
        this.pos = savedPos;
      }
    }

    parseParenOrCast() {
      // Try type cast first: (type) expr
      const saved = this.save();
      try {
        this.consume(); // (
        const t = this.now();
        const primitives = new Set(['int', 'text', 'float', 'bool', 'type']);
        if (t.type === TT.KEYWORD && primitives.has(t.keyword)) {
          const typeName = this.consume().text;
          if (this.isSym('RPAREN')) {
            this.consume(); // )
            if (this.isExprStart()) {
              const expr = this.parsePrecedence(90);
              return mkNode(N.TYPE_CAST, { targetType: typeName, expression: expr });
            }
          }
        }
        throw new ParseError('not a cast');
      } catch (e) {
        this.restore(saved);
      }

      // Grouped expression
      this.consume(); // (
      const expr = this.parseExpr();
      this.expectSym('RPAREN');
      return expr;
    }

    isSlotCallExpression() {
      // [s1, s2]:method(args) — detect by scanning for ]: pattern
      const saved = this.save();
      try {
        if (!this.isSym('LBRACKET')) return false;
        let depth = 1;
        let p = this.pos + 1;
        while (p < this.tokens.length && depth > 0) {
          const t = this.tokens[p];
          if (t.type === TT.SYMBOL && t.symbol === 'LBRACKET') depth++;
          else if (t.type === TT.SYMBOL && t.symbol === 'RBRACKET') depth--;
          p++;
        }
        // After RBRACKET, expect COLON
        if (p < this.tokens.length) {
          const after = this.tokens[p];
          return after.type === TT.SYMBOL && after.symbol === 'COLON';
        }
        return false;
      } finally {
        this.restore(saved);
      }
    }

    parseSlotCallExpression() {
      const slotNames = this.parseReturnSlots();
      this.expectSym('COLON');
      const call = this.parseCallExpr();
      call.slotNames = slotNames;
      return call;
    }

    parseArrayLiteral() {
      this.expectSym('LBRACKET');
      const elements = [];

      if (!this.isSym('RBRACKET')) {
        if (this.isRangeStart()) {
          elements.push(this.parseRangeExpr());
        } else {
          elements.push(this.parseExpr());
        }

        while (this.consumeIfSym('COMMA')) {
          if (this.isRangeStart()) {
            elements.push(this.parseRangeExpr());
          } else {
            elements.push(this.parseExpr());
          }
        }
      }

      this.expectSym('RBRACKET');
      return mkNode(N.ARRAY, { elements });
    }

    parseRangeExpr() {
      const start = this.parseExpr();
      if (!this.consumeIfSym('RANGE_DOTDOT') && !this.consumeIfKw('to')) {
        throw new ParseError('Expected range operator');
      }
      const end = this.parseExpr();
      let step = null;
      if (this.isKw('by') || this.isSym('RANGE_HASH')) {
        this.consume(); step = this.parseExpr();
      }
      return mkNode(N.RANGE, { start, end, step });
    }

    parseIdentOrCall() {
      const idToken = this.consume();
      const name = idToken.text;

      // Constructor call: UpperCase(args)
      if (/^[A-Z]/.test(name) && this.isSym('LPAREN')) {
        return this.parseConstructorCallArgs(name);
      }

      // Direct method call: name(args)
      if (this.isSym('LPAREN')) {
        return this.buildCall(name, name, null);
      }

      // Just identifier
      return mkNode(N.IDENT, { name });
    }

    parseCallByName(name) {
      this.consume(); // consume keyword
      return this.buildCall(name, name, null);
    }

    parseCallExpr() {
      const t = this.now();
      if (t.type !== TT.ID &&
          !(t.type === TT.KEYWORD && ['all', 'any'].includes(t.keyword))) {
        throw new ParseError('Expected method name or object.method');
      }
      const firstName = this.consume().text;

      // Handle obj.method() syntax: p.sum()
      if (this.isSym('DOT')) {
        this.consume(); // .
        const methodName = this.consume().text;
        const target = mkNode(N.IDENT, { name: firstName });
        return this.buildCall(methodName, firstName + '.' + methodName, target);
      }

      return this.buildCall(firstName, firstName, null);
    }

    buildCall(name, qualifiedName, target) {
      this.consume(); // (
      const args = [];
      const argNames = [];

      if (!this.isSym('RPAREN')) {
        if (this.isNamedArg()) {
          this.parseNamedArgs(args, argNames);
        } else {
          args.push(this.parseExpr()); argNames.push(null);
          while (this.consumeIfSym('COMMA')) {
            args.push(this.parseExpr()); argNames.push(null);
          }
        }
      }

      this.expectSym('RPAREN');
      return mkNode(N.METHOD_CALL, {
        name, qualifiedName, arguments: args, argNames,
        slotNames: [], isGlobal: true,
        target: target || null, isSuperCall: false,
      });
    }

    parseConstructorCallArgs(name) {
      this.consume(); // (
      const args = [];
      const argNames = [];
      if (!this.isSym('RPAREN')) {
        if (this.isNamedArg()) {
          this.parseNamedArgs(args, argNames);
        } else {
          args.push(this.parseExpr()); argNames.push(null);
          while (this.consumeIfSym('COMMA')) {
            args.push(this.parseExpr()); argNames.push(null);
          }
        }
      }
      this.expectSym('RPAREN');
      return mkNode(N.CONSTRUCTOR_CALL, { className: name, arguments: args, argNames });
    }

    isNamedArg() {
      const t1 = this.now();
      const t2 = this.next(1);
      return t1.type === TT.ID && t2.type === TT.SYMBOL && t2.symbol === 'COLON';
    }

    parseNamedArgs(args, argNames) {
      do {
        const name = this.expectType(TT.ID).text;
        this.expectSym('COLON');
        args.push(this.parseExpr());
        argNames.push(name);
      } while (!this.isSym('RPAREN') && this.consumeIfSym('COMMA'));
    }

    parseLambda() {
      this.consume(); // \
      this.expectSym('LPAREN');
      const params = [];
      if (!this.isSym('RPAREN')) {
        params.push(this.parseParam());
        while (this.consumeIfSym('COMMA')) params.push(this.parseParam());
      }
      this.expectSym('RPAREN');

      let returnSlots = null;
      if (this.isSym('DOUBLE_COLON')) {
        this.consume();
        returnSlots = this.parseSlotContract();
      }

      let body;
      if (this.isSym('LBRACE')) {
        body = this.parseBlock();
      } else if (this.isSym('TILDE_ARROW')) {
        this.consume();
        const assigns = [this.parseSingleSlot()];
        while (this.consumeIfSym('COMMA')) assigns.push(this.parseSingleSlot());
        // Build the body: single slot or multi-slot wrapper
        const stmts = assigns.length === 1
          ? assigns
          : [mkNode(N.MULTI_SLOT, { assignments: assigns })];
        body = mkNode(N.BLOCK, { statements: stmts });
      } else {
        throw new ParseError("Expected '{' or '~>' in lambda body");
      }

      return mkNode(N.LAMBDA, { params, returnSlots, body });
    }
  }

  // ============================================================
  // SECTION 4: INTERPRETER
  // ============================================================

  class CodError extends Error {
    constructor(msg) { super(msg); this.name = 'CodError'; }
  }

  class BreakSignal {}
  class SkipSignal {}
  class ExitSignal {}

  class Scope {
    constructor(parent) {
      this.vars = Object.create(null);
      this.parent = parent || null;
      this.slotValues = null;
      this.slotTypes = null;
    }

    get(name) {
      let s = this;
      while (s) {
        if (name in s.vars) return s.vars[name];
        s = s.parent;
      }
      return undefined;
    }

    has(name) {
      let s = this;
      while (s) {
        if (name in s.vars) return true;
        s = s.parent;
      }
      return false;
    }

    // Set in innermost (current) scope
    set(name, value) {
      this.vars[name] = value;
    }

    // Assign: update existing binding, or create in current scope
    assign(name, value) {
      let s = this;
      while (s) {
        if (name in s.vars) { s.vars[name] = value; return; }
        s = s.parent;
      }
      this.vars[name] = value;
    }

    // Set a slot value (walks up to find the slot scope)
    setSlot(name, value) {
      let s = this;
      while (s) {
        if (s.slotValues && name in s.slotValues) {
          s.slotValues[name] = value;
          return true;
        }
        s = s.parent;
      }
      return false;
    }

    getSlotValues() {
      let s = this;
      while (s) {
        if (s.slotValues) return s.slotValues;
        s = s.parent;
      }
      return null;
    }

    getSlotNames() {
      const sv = this.getSlotValues();
      return sv ? Object.keys(sv) : [];
    }

    hasSlot(name) {
      let s = this;
      while (s) {
        if (s.slotValues && name in s.slotValues) return true;
        s = s.parent;
      }
      return false;
    }
  }

  class ObjectInstance {
    constructor(typeDef) {
      this.type = typeDef || null;
      this.fields = Object.create(null);
    }
  }

  class CodInterpreter {
    constructor() {
      this.output = '';
      this.types = Object.create(null);   // name → TypeNode
      this.methods = Object.create(null); // name → MethodNode
    }

    resetOutput() { this.output = ''; }
    getOutput() { return this.output; }

    evalRepl(ast, globals) {
      if (!ast) return undefined;

      // Build a scope from the persistent globals map
      const scope = new Scope(null);
      for (const k of Object.keys(globals)) scope.vars[k] = globals[k];

      let result;
      try {
        result = this.eval(ast, scope);
      } catch (e) {
        if (e instanceof ExitSignal) {
          // `exit` terminates the current evaluation gracefully
          result = undefined;
        } else {
          throw e;
        }
      }

      // Write back all top-level vars to the persistent globals map
      for (const k of Object.keys(scope.vars)) globals[k] = scope.vars[k];

      return result;
    }

    eval(node, scope) {
      if (!node) return undefined;

      switch (node._type) {
        // ── Literals ──
        case N.INT_LIT:   return node.value;
        case N.FLOAT_LIT: return node.value;
        case N.TEXT_LIT:  return node.value;
        case N.BOOL_LIT:  return node.value;
        case N.NONE_LIT:  return null;

        // ── Expressions ──
        case N.IDENT:          return this.evalIdent(node, scope);
        case N.BINARY_OP:      return this.evalBinaryOp(node, scope);
        case N.UNARY:          return this.evalUnary(node, scope);
        case N.METHOD_CALL:    return this.evalMethodCall(node, scope);
        case N.CONSTRUCTOR_CALL: return this.evalConstructorCall(node, scope);
        case N.INDEX:          return this.evalIndex(node, scope);
        case N.PROP:           return this.evalProp(node, scope);
        case N.ARRAY:          return this.evalArray(node, scope);
        case N.TYPE_CAST:      return this.evalTypeCast(node, scope);
        case N.EQUALITY_CHAIN: return this.evalEqualityChain(node, scope);
        case N.BOOL_CHAIN:     return this.evalBoolChain(node, scope);
        case N.EXPR_IF:        return this.evalExprIf(node, scope);
        case N.THIS:           return this.evalThis(scope);
        case N.SUPER:          return this.evalSuper(scope);
        case N.LAMBDA:         return node; // lambda is a value
        case N.TUPLE:          return node.elements.map(e => this.eval(e, scope));
        case N.RANGE:          return this.evalRange(node, scope);
        case N.RANGE_INDEX:    return this.evalRangeIndex(node, scope);

        // ── Statements ──
        case N.VAR:       return this.evalVar(node, scope);
        case N.ASSIGN:    return this.evalAssign(node, scope);
        case N.STMT_IF:   return this.evalStmtIf(node, scope);
        case N.FOR:       return this.evalFor(node, scope);
        case N.BREAK:     throw new BreakSignal();
        case N.SKIP:      throw new SkipSignal();
        case N.EXIT:      throw new ExitSignal();
        case N.BLOCK:     return this.evalBlock(node, scope);
        case N.PROGRAM:   return this.evalBlock(node, scope);

        // ── Slot assignments ──
        case N.SLOT_ASSIGN:  return this.evalSlotAssign(node, scope);
        case N.MULTI_SLOT:   return this.evalMultiSlot(node, scope);
        case N.RET_SLOT:     return this.evalRetSlot(node, scope);

        // ── Declarations ──
        case N.TYPE:    return this.evalTypeDecl(node, scope);
        case N.METHOD:  return this.evalMethodDecl(node, scope);
        case N.FIELD:   return this.evalField(node, scope);

        default:
          throw new CodError(`Unknown AST node type: ${node._type}`);
      }
    }

    // ── Identifiers ──────────────────────────────────────────

    evalIdent(node, scope) {
      const val = scope.get(node.name);
      if (val !== undefined) return val;

      // Check current object's fields (for use inside class methods)
      const thisObj = scope.get('__this__');
      if (thisObj instanceof ObjectInstance && node.name in thisObj.fields) {
        return thisObj.fields[node.name];
      }

      if (this.types[node.name]) return this.types[node.name];
      throw new CodError(`Undefined variable: '${node.name}'`);
    }

    // ── Binary operations ────────────────────────────────────

    evalBinaryOp(node, scope) {
      if (node.op === 'is') {
        const left = this.eval(node.left, scope);
        const right = this.eval(node.right, scope);
        return this.checkIs(left, right);
      }

      const left = this.eval(node.left, scope);
      const right = this.eval(node.right, scope);
      return this.applyBinOp(node.op, left, right);
    }

    applyBinOp(op, left, right) {
      switch (op) {
        case '+': case '+=':
          if (typeof left === 'string' || typeof right === 'string') {
            return this.stringify(left) + this.stringify(right);
          }
          return this.toNum(left) + this.toNum(right);

        case '-': case '-=': return this.toNum(left) - this.toNum(right);
        case '*': case '*=': return this.toNum(left) * this.toNum(right);
        case '/': case '/=': {
          const r = this.toNum(right);
          if (r === 0) throw new CodError('Division by zero');
          return this.toNum(left) / r;
        }
        case '%': return this.toNum(left) % this.toNum(right);

        case '==': return this.areEqual(left, right);
        case '!=': return !this.areEqual(left, right);
        case '<':  return this.compare(left, right) < 0;
        case '>':  return this.compare(left, right) > 0;
        case '<=': return this.compare(left, right) <= 0;
        case '>=': return this.compare(left, right) >= 0;

        default: throw new CodError(`Unknown operator: '${op}'`);
      }
    }

    // ── Unary operations ─────────────────────────────────────

    evalUnary(node, scope) {
      const val = this.eval(node.operand, scope);
      switch (node.op) {
        case '-': return -this.toNum(val);
        case '+': return +this.toNum(val);
        case '!': return !this.isTruthy(val);
        default: throw new CodError(`Unknown unary operator: '${node.op}'`);
      }
    }

    // ── Method calls ─────────────────────────────────────────

    evalMethodCall(node, scope) {
      const args = node.arguments.map(a => this.eval(a, scope));

      // Object method call: obj.method(args)
      if (node.target) {
        const obj = this.eval(node.target, scope);
        return this.callOnValue(obj, node.name, args, scope, node.slotNames);
      }

      // Built-in global functions
      const bi = this.callBuiltin(node.name, args, scope);
      if (bi !== undefined) return bi;

      // User-defined method in current object
      const thisObj = scope.get('__this__');
      if (thisObj instanceof ObjectInstance) {
        const m = this.findMethod(thisObj, node.name);
        if (m) return this.callUserMethod(m, args, scope, thisObj, node.slotNames);
      }

      // Globally registered methods
      if (this.methods[node.name]) {
        return this.callUserMethod(this.methods[node.name], args, scope, null, node.slotNames);
      }

      // Qualified name lookup: obj.method()
      if (node.qualifiedName && node.qualifiedName.includes('.')) {
        const parts = node.qualifiedName.split('.');
        const objName = parts[0];
        const methodName = parts[parts.length - 1];
        const obj = scope.get(objName);
        if (obj !== undefined) {
          return this.callOnValue(obj, methodName, args, scope, node.slotNames);
        }
      }

      throw new CodError(`Undefined function: '${node.name}'`);
    }

    callBuiltin(name, args, scope) {
      switch (name) {
        case 'out': {
          if (args.length === 0) {
            this.output += '\n';
          } else if (args.length === 1) {
            this.output += this.stringify(args[0]) + '\n';
          } else {
            for (const a of args) this.output += this.stringify(a) + '\n';
          }
          return null;
        }

        case 'outs': {
          if (args.length === 1) {
            this.output += this.stringify(args[0]);
          } else if (args.length > 1) {
            this.output += args.map(a => this.stringify(a)).join(' ');
          }
          return null;
        }

        case 'in':
          throw new CodError('in() is not supported in the web REPL. Use variables instead.');

        case 'timer':
          return Date.now();

        case 'text': {
          if (args.length < 1) throw new CodError('text() requires 1 argument');
          return this.stringify(args[0]);
        }

        case 'int': {
          if (args.length < 1) throw new CodError('int() requires 1 argument');
          const v = args[0];
          if (typeof v === 'number') return Math.trunc(v);
          if (typeof v === 'string') {
            const n = parseInt(v, 10);
            if (isNaN(n)) throw new CodError(`Cannot convert "${v}" to int`);
            return n;
          }
          if (typeof v === 'boolean') return v ? 1 : 0;
          if (v === null) return 0;
          throw new CodError(`Cannot convert ${typeof v} to int`);
        }

        case 'float': {
          if (args.length < 1) throw new CodError('float() requires 1 argument');
          const v = args[0];
          if (typeof v === 'number') return v;
          if (typeof v === 'string') {
            const n = parseFloat(v);
            if (isNaN(n)) throw new CodError(`Cannot convert "${v}" to float`);
            return n;
          }
          if (typeof v === 'boolean') return v ? 1.0 : 0.0;
          if (v === null) return 0.0;
          throw new CodError(`Cannot convert ${typeof v} to float`);
        }

        case 'bool': {
          if (args.length < 1) throw new CodError('bool() requires 1 argument');
          return this.isTruthy(args[0]);
        }

        case 'sqrt':  return args.length === 1 ? Math.sqrt(this.toNum(args[0])) : undefined;
        case 'abs':   return args.length === 1 ? Math.abs(this.toNum(args[0])) : undefined;
        case 'floor': return args.length === 1 ? Math.floor(this.toNum(args[0])) : undefined;
        case 'ceil':  return args.length === 1 ? Math.ceil(this.toNum(args[0])) : undefined;
        case 'round': return args.length === 1 ? Math.round(this.toNum(args[0])) : undefined;
        case 'min':   return Math.min(...args.map(a => this.toNum(a)));
        case 'max':   return Math.max(...args.map(a => this.toNum(a)));
        case 'pow':   return args.length === 2 ? Math.pow(this.toNum(args[0]), this.toNum(args[1])) : undefined;
        case 'log':   return args.length === 1 ? Math.log(this.toNum(args[0])) : undefined;
        case 'log10': return args.length === 1 ? Math.log10(this.toNum(args[0])) : undefined;

        default:
          return undefined; // Not a built-in
      }
    }

    callOnValue(obj, methodName, args, scope, slotNames) {
      // Array methods
      if (Array.isArray(obj)) {
        switch (methodName) {
          case 'size':     return obj.length;
          case 'push': case 'add': obj.push(args[0]); return null;
          case 'pop':      return obj.pop();
          case 'get':      return obj[this.toInt(args[0])];
          case 'set':      obj[this.toInt(args[0])] = args[1]; return null;
          case 'contains': case 'has': return obj.includes(args[0]);
          case 'indexOf':  return obj.indexOf(args[0]);
          case 'slice':    return obj.slice(this.toInt(args[0]), args[1] !== undefined ? this.toInt(args[1]) : undefined);
          case 'join':     return obj.map(v => this.stringify(v)).join(args[0] !== undefined ? args[0] : ', ');
          case 'first':    return obj[0];
          case 'last':     return obj[obj.length - 1];
          case 'reverse':  return [...obj].reverse();
          case 'sort':     return [...obj].sort((a, b) => this.compare(a, b));
          case 'map': {
            const fn = args[0];
            return obj.map((v, i) => this.callLambda(fn, [v, i], scope));
          }
          case 'filter': {
            const fn = args[0];
            return obj.filter((v, i) => this.isTruthy(this.callLambda(fn, [v, i], scope)));
          }
          case 'reduce': {
            const fn = args[0];
            const init = args[1];
            return obj.reduce((acc, v) => this.callLambda(fn, [acc, v], scope), init);
          }
          case 'forEach': {
            const fn = args[0];
            obj.forEach((v, i) => this.callLambda(fn, [v, i], scope));
            return null;
          }
          default: throw new CodError(`No method '${methodName}' on array`);
        }
      }

      // String methods
      if (typeof obj === 'string') {
        switch (methodName) {
          case 'size': case 'length': return obj.length;
          case 'upper': case 'toUpper': return obj.toUpperCase();
          case 'lower': case 'toLower': return obj.toLowerCase();
          case 'trim': case 'trimmed': return obj.trim();
          case 'contains': return obj.includes(String(args[0]));
          case 'startsWith': return obj.startsWith(String(args[0]));
          case 'endsWith': return obj.endsWith(String(args[0]));
          case 'replace': return obj.replace(String(args[0]), String(args[1]));
          case 'split': return obj.split(args[0] !== undefined ? String(args[0]) : '');
          case 'charAt': case 'get': return obj[this.toInt(args[0])] || '';
          case 'indexOf': return obj.indexOf(String(args[0]));
          case 'slice': case 'substring': return obj.slice(this.toInt(args[0]), args[1] !== undefined ? this.toInt(args[1]) : undefined);
          case 'repeat': return obj.repeat(this.toInt(args[0]));
          default: throw new CodError(`No method '${methodName}' on string`);
        }
      }

      // Number methods
      if (typeof obj === 'number') {
        switch (methodName) {
          case 'abs':   return Math.abs(obj);
          case 'floor': return Math.floor(obj);
          case 'ceil':  return Math.ceil(obj);
          case 'round': return Math.round(obj);
          case 'sqrt':  return Math.sqrt(obj);
          case 'pow':   return Math.pow(obj, this.toNum(args[0]));
          case 'max':   return Math.max(obj, this.toNum(args[0]));
          case 'min':   return Math.min(obj, this.toNum(args[0]));
          case 'toText': case 'toString': return String(obj);
          default: throw new CodError(`No method '${methodName}' on number`);
        }
      }

      // ObjectInstance method call
      if (obj instanceof ObjectInstance) {
        const m = this.findMethod(obj, methodName);
        if (m) return this.callUserMethod(m, args, scope, obj, slotNames);
        if (methodName in obj.fields) return obj.fields[methodName];
        throw new CodError(`Method '${methodName}' not found on ${obj.type ? obj.type.name : 'object'}`);
      }

      throw new CodError(`Cannot call '${methodName}' on ${typeof obj}`);
    }

    findMethod(obj, name) {
      if (!(obj instanceof ObjectInstance) || !obj.type) return null;
      let t = obj.type;
      while (t) {
        if (t.methods) {
          const m = t.methods.find(m => m.name === name);
          if (m) return m;
        }
        t = t.parent ? this.types[t.parent] : null;
      }
      return null;
    }

    callUserMethod(method, args, callerScope, thisObj, slotNames) {
      const mScope = new Scope(null); // Methods get a fresh scope

      if (thisObj) mScope.vars['__this__'] = thisObj;

      // Bind parameters
      const params = method.params || [];
      for (let i = 0; i < params.length; i++) {
        const p = params[i];
        let val;
        if (i < args.length) {
          val = args[i];
        } else if (p.defaultValue) {
          val = this.eval(p.defaultValue, callerScope);
        } else {
          val = null;
        }
        mScope.vars[p.name] = val;
      }

      // Set up return slots
      const returnSlots = method.returnSlots;
      if (returnSlots && returnSlots.length > 0) {
        mScope.slotValues = Object.create(null);
        mScope.slotTypes = Object.create(null);
        for (const sl of returnSlots) {
          mScope.slotValues[sl.name] = null;
          if (sl.type) mScope.slotTypes[sl.name] = sl.type;
        }
      }

      try {
        this.evalBlock(method.body, mScope);
      } catch (e) {
        if (e instanceof ExitSignal) { /* normal method exit */ }
        else throw e;
      }

      if (returnSlots && returnSlots.length > 0) {
        const sv = mScope.slotValues;
        // If single slot and caller wants just the value
        if (returnSlots.length === 1 && slotNames && slotNames.length === 1) {
          return sv[slotNames[0]] !== undefined ? sv[slotNames[0]] : sv[returnSlots[0].name];
        }
        return sv;
      }

      return null;
    }

    callLambda(fn, args, scope) {
      if (typeof fn === 'function') return fn(...args);
      if (fn && fn._type === N.LAMBDA) {
        return this.callUserMethod(
          { params: fn.params, returnSlots: fn.returnSlots, body: fn.body },
          args, scope, null, []
        );
      }
      throw new CodError('Expected a lambda/function');
    }

    // ── Constructor calls ────────────────────────────────────

    evalConstructorCall(node, scope) {
      const typeDef = this.types[node.className];
      if (!typeDef) throw new CodError(`Unknown type: '${node.className}'`);

      const obj = new ObjectInstance(typeDef);

      // Initialize fields
      if (typeDef.fields) {
        for (const f of typeDef.fields) {
          obj.fields[f.name] = f.value ? this.eval(f.value, scope) : null;
        }
      }

      // Parent fields too
      if (typeDef.parent && this.types[typeDef.parent]) {
        const parent = this.types[typeDef.parent];
        if (parent.fields) {
          for (const f of parent.fields) {
            if (!(f.name in obj.fields)) {
              obj.fields[f.name] = f.value ? this.eval(f.value, scope) : null;
            }
          }
        }
      }

      // Constructor method (same name as class or 'init')
      const ctor = typeDef.methods &&
        typeDef.methods.find(m => m.name === node.className || m.name === 'init');
      if (ctor) {
        const args = node.arguments.map(a => this.eval(a, scope));
        this.callUserMethod(ctor, args, scope, obj, []);
      }

      return obj;
    }

    // ── Index access ─────────────────────────────────────────

    evalIndex(node, scope) {
      const arr = this.eval(node.array, scope);
      const idxNode = node.index;

      if (idxNode._type === N.RANGE_INDEX) {
        const ri = this.eval(idxNode, scope);
        return this.applyRangeIdx(arr, ri);
      }

      const idx = this.eval(idxNode, scope);

      if (Array.isArray(arr)) {
        const i = this.toInt(idx);
        if (i < 0 || i >= arr.length) {
          throw new CodError(`Index ${i} out of bounds (size ${arr.length})`);
        }
        return arr[i];
      }

      if (typeof arr === 'string') {
        const i = this.toInt(idx);
        if (i < 0 || i >= arr.length) {
          throw new CodError(`Index ${i} out of bounds (length ${arr.length})`);
        }
        return arr[i];
      }

      throw new CodError(`Cannot index into ${typeof arr}`);
    }

    evalRangeIndex(node, scope) {
      return {
        _rangeIdx: true,
        start: this.eval(node.start, scope),
        end: this.eval(node.end, scope),
        step: node.step ? this.eval(node.step, scope) : null,
      };
    }

    applyRangeIdx(arr, ri) {
      const start = this.toInt(ri.start);
      const end = this.toInt(ri.end);
      const step = ri.step !== null ? this.toInt(ri.step) : (start <= end ? 1 : -1);
      if (step === 0) throw new CodError('Range step cannot be zero');

      if (Array.isArray(arr)) {
        const result = [];
        if (step > 0) {
          for (let i = start; i <= end && i < arr.length; i += step) result.push(arr[i]);
        } else {
          for (let i = start; i >= end && i >= 0; i += step) result.push(arr[i]);
        }
        return result;
      }
      throw new CodError('Range index only applies to arrays');
    }

    // ── Property access ──────────────────────────────────────

    evalProp(node, scope) {
      const obj = this.eval(node.left, scope);
      const name = node.right.name;

      if (Array.isArray(obj)) {
        switch (name) {
          case 'size': case 'length': return obj.length;
          case 'first': return obj[0];
          case 'last': return obj[obj.length - 1];
          case 'isEmpty': return obj.length === 0;
          case 'reversed': return [...obj].reverse();
        }
        throw new CodError(`No property '${name}' on array`);
      }

      if (typeof obj === 'string') {
        switch (name) {
          case 'size': case 'length': return obj.length;
          case 'upper': return obj.toUpperCase();
          case 'lower': return obj.toLowerCase();
          case 'trimmed': case 'trim': return obj.trim();
          case 'isEmpty': return obj.length === 0;
        }
        throw new CodError(`No property '${name}' on string`);
      }

      if (typeof obj === 'number') {
        switch (name) {
          case 'abs': return Math.abs(obj);
          case 'floor': return Math.floor(obj);
          case 'ceil': return Math.ceil(obj);
          case 'round': return Math.round(obj);
          case 'isInt': return Number.isInteger(obj);
        }
        throw new CodError(`No property '${name}' on number`);
      }

      if (obj instanceof ObjectInstance) {
        if (name in obj.fields) return obj.fields[name];
        // Try zero-arg method as property getter
        const m = this.findMethod(obj, name);
        if (m && (!m.params || m.params.length === 0)) {
          return this.callUserMethod(m, [], scope, obj, []);
        }
        throw new CodError(`Property '${name}' not found on ${obj.type ? obj.type.name : 'object'}`);
      }

      if (obj === null) throw new CodError(`Cannot access property '${name}' on none`);

      throw new CodError(`Cannot access property '${name}' on ${typeof obj}`);
    }

    // ── Arrays ───────────────────────────────────────────────

    evalArray(node, scope) {
      // Single range: [1 to 5]
      if (node.elements.length === 1 && node.elements[0]._type === N.RANGE) {
        return this.expandRange(node.elements[0], scope);
      }

      return node.elements.map(e => {
        if (e._type === N.RANGE) return this.expandRange(e, scope);
        return this.eval(e, scope);
      });
    }

    evalRange(node, scope) {
      return {
        _range: true,
        start: this.eval(node.start, scope),
        end: this.eval(node.end, scope),
        step: node.step ? this.eval(node.step, scope) : null,
      };
    }

    expandRange(rangeNode, scope) {
      const s = this.toNum(this.eval(rangeNode.start, scope));
      const e = this.toNum(this.eval(rangeNode.end, scope));
      const st = rangeNode.step
        ? this.toNum(this.eval(rangeNode.step, scope))
        : (s <= e ? 1 : -1);

      if (st === 0) throw new CodError('Range step cannot be zero');

      const result = [];
      if (st > 0) { for (let i = s; i <= e; i += st) result.push(i); }
      else        { for (let i = s; i >= e; i += st) result.push(i); }
      return result;
    }

    // ── Type casts ───────────────────────────────────────────

    evalTypeCast(node, scope) {
      const val = this.eval(node.expression, scope);
      switch (node.targetType) {
        case 'text':  return this.stringify(val);
        case 'int':   return Math.trunc(this.toNum(val));
        case 'float': return this.toNum(val);
        case 'bool':  return this.isTruthy(val);
        default:      return val;
      }
    }

    // ── Equality chain: x == any[1,2,3] ─────────────────────

    evalEqualityChain(node, scope) {
      const left = this.eval(node.left, scope);
      const op = node.operator;
      const isAll = node.isAllChain;

      for (const arg of node.chainArguments) {
        const rightVal = this.eval(arg, scope);
        const items = Array.isArray(rightVal) ? rightVal : [rightVal];

        for (const item of items) {
          let result;
          switch (op) {
            case '==': result = this.areEqual(left, item); break;
            case '!=': result = !this.areEqual(left, item); break;
            case '>':  result = this.compare(left, item) > 0; break;
            case '<':  result = this.compare(left, item) < 0; break;
            case '>=': result = this.compare(left, item) >= 0; break;
            case '<=': result = this.compare(left, item) <= 0; break;
            default: throw new CodError(`Unknown chain operator: '${op}'`);
          }
          if (isAll && !result) return false;
          if (!isAll && result) return true;
        }
      }

      return isAll;
    }

    // ── Boolean chain: all[a, b, c] ──────────────────────────

    evalBoolChain(node, scope) {
      for (const e of node.expressions) {
        const v = this.isTruthy(this.eval(e, scope));
        if (node.isAll && !v) return false;
        if (!node.isAll && v) return true;
      }
      return node.isAll;
    }

    // ── If expression ────────────────────────────────────────

    evalExprIf(node, scope) {
      return this.isTruthy(this.eval(node.condition, scope))
        ? this.eval(node.thenExpr, scope)
        : this.eval(node.elseExpr, scope);
    }

    // ── this / super ─────────────────────────────────────────

    evalThis(scope) {
      const v = scope.get('__this__');
      if (v === undefined) throw new CodError("Cannot use 'this' outside of an object");
      return v;
    }

    evalSuper(scope) {
      const v = scope.get('__this__');
      if (v === undefined) throw new CodError("Cannot use 'super' outside of an object");
      return v;
    }

    // ── Variable declaration ─────────────────────────────────

    evalVar(node, scope) {
      const val = node.value ? this.eval(node.value, scope) : null;
      scope.set(node.name, val);
      return undefined;
    }

    // ── Assignment ───────────────────────────────────────────

    evalAssign(node, scope) {
      const val = this.eval(node.right, scope);

      switch (node.left._type) {
        case N.IDENT: {
          const name = node.left.name;
          // Inside a class method, assignment to a field name sets the field
          const thisObj = scope.get('__this__');
          if (thisObj instanceof ObjectInstance && name in thisObj.fields) {
            thisObj.fields[name] = val;
          } else {
            scope.assign(name, val);
          }
          break;
        }
        case N.INDEX: {
          const arr = this.eval(node.left.array, scope);
          const idx = this.toInt(this.eval(node.left.index, scope));
          if (!Array.isArray(arr)) throw new CodError('Cannot index into non-array for assignment');
          if (idx < 0 || idx >= arr.length) throw new CodError(`Index ${idx} out of bounds for assignment`);
          arr[idx] = val;
          break;
        }
        case N.PROP: {
          const obj = this.eval(node.left.left, scope);
          if (obj instanceof ObjectInstance) {
            obj.fields[node.left.right.name] = val;
          } else {
            throw new CodError("Cannot assign property on non-object");
          }
          break;
        }
        default:
          throw new CodError(`Invalid assignment target: ${node.left._type}`);
      }

      return undefined;
    }

    // ── If statement ─────────────────────────────────────────

    evalStmtIf(node, scope) {
      const cond = this.isTruthy(this.eval(node.condition, scope));
      const childScope = new Scope(scope);
      if (cond) {
        this.evalBlock(node.thenBlock, childScope);
      } else {
        this.evalBlock(node.elseBlock, childScope);
      }
      return undefined;
    }

    // ── For loop ─────────────────────────────────────────────

    evalFor(node, scope) {
      if (node.range) {
        this.evalRangeLoop(node, scope);
      } else if (node.arraySource) {
        this.evalArrayLoop(node, scope);
      }
      return undefined;
    }

    evalRangeLoop(node, scope) {
      const startVal = this.toNum(this.eval(node.range.start, scope));
      const endVal   = this.toNum(this.eval(node.range.end, scope));
      let step;
      if (node.range.step) {
        step = this.toNum(this.eval(node.range.step, scope));
      } else {
        step = startVal <= endVal ? 1 : -1;
      }
      if (step === 0) throw new CodError('Loop step cannot be zero');

      const iter = node.iterator;
      const loopScope = new Scope(scope);

      if (step > 0) {
        for (let i = startVal; i <= endVal; i += step) {
          loopScope.vars[iter] = i;
          try { this.evalBlock(node.body, loopScope); }
          catch (e) {
            if (e instanceof BreakSignal) break;
            if (e instanceof SkipSignal) continue;
            throw e;
          }
        }
      } else {
        for (let i = startVal; i >= endVal; i += step) {
          loopScope.vars[iter] = i;
          try { this.evalBlock(node.body, loopScope); }
          catch (e) {
            if (e instanceof BreakSignal) break;
            if (e instanceof SkipSignal) continue;
            throw e;
          }
        }
      }
    }

    evalArrayLoop(node, scope) {
      const arr = this.eval(node.arraySource, scope);
      if (!Array.isArray(arr)) {
        throw new CodError(`Cannot iterate over ${typeof arr} (expected array)`);
      }

      const iter = node.iterator;
      const loopScope = new Scope(scope);

      for (const item of arr) {
        loopScope.vars[iter] = item;
        try { this.evalBlock(node.body, loopScope); }
        catch (e) {
          if (e instanceof BreakSignal) break;
          if (e instanceof SkipSignal) continue;
          throw e;
        }
      }
    }

    // ── Block ────────────────────────────────────────────────

    evalBlock(node, scope) {
      if (!node || !node.statements) return undefined;
      let last;
      for (const stmt of node.statements) last = this.eval(stmt, scope);
      return last;
    }

    // ── Slot assignments ─────────────────────────────────────

    evalSlotAssign(node, scope) {
      const val = this.eval(node.value, scope);

      if (node.slotName) {
        if (!scope.setSlot(node.slotName, val)) {
          scope.assign(node.slotName, val);
        }
      } else {
        // Positional: assign to first available slot
        const names = scope.getSlotNames();
        if (names.length > 0) {
          scope.setSlot(names[0], val);
        }
      }

      return val;
    }

    evalMultiSlot(node, scope) {
      const names = scope.getSlotNames();
      let idx = 0;

      for (const assign of node.assignments) {
        const val = this.eval(assign.value, scope);
        if (assign.slotName) {
          scope.setSlot(assign.slotName, val);
        } else if (idx < names.length) {
          scope.setSlot(names[idx++], val);
        }
      }

      return undefined;
    }

    evalRetSlot(node, scope) {
      // [s1, s2]:method(args) with assignment to variables
      const call = node.methodCall;
      const result = this.evalMethodCall(call, scope);

      if (result && typeof result === 'object' && !Array.isArray(result) &&
          !(result instanceof ObjectInstance)) {
        // Result is a slot map
        for (let i = 0; i < node.variableNames.length; i++) {
          const slotName = call.slotNames[i];
          const varName = node.variableNames[i];
          const val = result[slotName] !== undefined ? result[slotName] : null;
          scope.assign(varName, val);
        }
      } else if (node.variableNames.length === 1) {
        scope.assign(node.variableNames[0], result);
      }

      return undefined;
    }

    // ── Declarations ─────────────────────────────────────────

    evalTypeDecl(node, scope) {
      this.types[node.name] = node;
      return undefined;
    }

    evalMethodDecl(node, scope) {
      this.methods[node.name] = node;
      return undefined;
    }

    evalField(node, scope) {
      const thisObj = scope.get('__this__');
      const val = node.value ? this.eval(node.value, scope) : null;
      if (thisObj instanceof ObjectInstance) thisObj.fields[node.name] = val;
      return val;
    }

    // ── Type system helpers ───────────────────────────────────

    checkIs(val, typeVal) {
      if (typeof typeVal === 'string') return this.validateType(typeVal, val);
      if (typeVal && typeVal._type === N.TYPE) {
        return val instanceof ObjectInstance && val.type === typeVal;
      }
      return false;
    }

    validateType(typeName, value) {
      if (!typeName) return true;
      const base = typeName.split('|')[0];
      switch (base) {
        case 'int':   return typeof value === 'number' && Number.isInteger(value);
        case 'float': return typeof value === 'number';
        case 'text':  return typeof value === 'string';
        case 'bool':  return typeof value === 'boolean';
        case 'none':  return value === null || value === undefined;
        case '[]':    return Array.isArray(value);
        default:
          if (base.startsWith('[')) return Array.isArray(value);
          if (value instanceof ObjectInstance) return value.type && value.type.name === base;
          return true;
      }
    }

    isTruthy(val) {
      if (val === null || val === undefined) return false;
      if (typeof val === 'boolean') return val;
      if (typeof val === 'number') return val !== 0;
      if (typeof val === 'string') return val.length > 0 && val !== 'false';
      if (Array.isArray(val)) return val.length > 0;
      return true;
    }

    areEqual(a, b) {
      if (a === null && b === null) return true;
      if (a === null || b === null) return false;
      return a === b;
    }

    compare(a, b) {
      if (typeof a === 'number' && typeof b === 'number') return a - b;
      if (typeof a === 'string' && typeof b === 'string') return a < b ? -1 : a > b ? 1 : 0;
      throw new CodError(`Cannot compare ${typeof a} and ${typeof b}`);
    }

    toNum(val) {
      if (typeof val === 'number') return val;
      if (typeof val === 'boolean') return val ? 1 : 0;
      if (typeof val === 'string') {
        const n = Number(val);
        if (isNaN(n)) throw new CodError(`Cannot convert "${val}" to number`);
        return n;
      }
      if (val === null || val === undefined) return 0;
      throw new CodError(`Cannot convert ${typeof val} to number`);
    }

    toInt(val) { return Math.trunc(this.toNum(val)); }

    // Convert any value to its display string (used by out(), string concat)
    stringify(val) {
      if (val === null || val === undefined) return 'none';
      if (typeof val === 'boolean') return val ? 'true' : 'false';
      if (typeof val === 'number') {
        if (Number.isInteger(val) && Math.abs(val) < 1e15) return String(val);
        return String(val);
      }
      if (typeof val === 'string') return val;
      if (Array.isArray(val)) return '[' + val.map(v => this.stringify(v)).join(', ') + ']';
      if (val instanceof ObjectInstance) {
        const typeName = val.type ? val.type.name : 'Object';
        const fields = Object.keys(val.fields);
        if (fields.length === 0) return typeName + '{}';
        const fStr = fields.map(k => `${k}: ${this.stringify(val.fields[k])}`).join(', ');
        return `${typeName}{${fStr}}`;
      }
      return String(val);
    }

    formatValue(val) {
      return this.stringify(val);
    }
  }

  // ============================================================
  // SECTION 5: REPL RUNNER
  // ============================================================

  const _interp = new CodInterpreter();
  const _globals = Object.create(null);

  const HELP_TEXT = [
    'Coderive REPL — JavaScript port',
    '',
    '  Variables:     x = 10                  (assign)',
    '                 y: int = 5              (typed)',
    '                 NAME := "Alice"         (constant)',
    '  Output:        out("Hello")            (with newline)',
    '                 outs("no newline")      (without newline)',
    '                 out("x =", x)           (multiple args)',
    '  Arithmetic:    5 + 3 * 2, 10 / 4, 7 % 3',
    '  Strings:       "hello " + name, "Hi {name}!"',
    '  Booleans:      true, false, !, ==, !=',
    '  Comparisons:   x > 5, x == any[1,2,3], all[a, b]',
    '  If:            if x > 5 { out("big") } else { out("small") }',
    '  For range:     for i of 1 to 5 { out(i) }',
    '  For array:     for x of arr { out(x) }',
    '  Arrays:        arr = [1, 2, 3]  arr[0]  arr.size',
    '  Range arrays:  narr = [1 to 10]',
    '  Casts:         text(42)  int("5")  float("3.14")',
    '  Slots:         ~> result: 42',
    '  Classes:       share MyClass { ... }',
    '  Methods:       share greet(name: text) :: msg: text { ~> "Hi {name}" }',
    '  Commands:      ;help  ;reset  ;exit',
  ].join('\n');

  const CodREPLRunner = {
    /**
     * Evaluate a single line of Coderive code.
     * @param {string} line
     * @returns {string} — captured output plus expression value (if applicable)
     */
    eval(line) {
      if (!line || !line.trim()) return '';

      line = line.trim();

      // Special REPL commands
      if (line === ';reset') { this.reset(); return 'State reset.'; }
      if (line === ';help')  return HELP_TEXT;
      if (line === ';exit' || line === ';quit') return '';

      try {
        const tokens = tokenize(line);
        const realTokens = tokens.filter(t => t.type !== TT.EOF && t.type !== TT.INVALID);
        if (realTokens.length === 0) return '';

        const parser = new Parser(tokens);
        const ast = parser.parseSingleLine();
        if (!ast) return '';

        _interp.resetOutput();

        const result = _interp.evalRepl(ast, _globals);

        const output = _interp.getOutput();

        // Echo expression value in REPL (but not statements)
        if (isExprNode(ast) && result !== undefined) {
          // null means 'none' only for literal none; method calls returning null are silent
          if (result === null && ast._type !== N.NONE_LIT) return output;
          return output + _interp.formatValue(result);
        }

        return output;
      } catch (e) {
        if (e instanceof ParseError) return 'Parse error: ' + e.message;
        if (e instanceof CodError)  return 'Error: ' + e.message;
        return 'Error: ' + (e && e.message ? e.message : String(e));
      }
    },

    /**
     * Compile and run a complete multi-line Coderive program.
     * @param {string} source
     * @param {{ reset?: boolean }} options
     * @returns {string} Program output, or empty string when source is empty/whitespace-only.
     */
    compileAndRun(source, options) {
      const opts = options || {};
      if (!source || !source.trim()) return '';

      if (opts.reset) this.reset();

      try {
        const tokens = tokenize(source);
        const parser = new Parser(tokens);
        const ast = parser.parseProgram();

        _interp.resetOutput();
        _interp.evalRepl(ast, _globals);
        if (_interp.methods && _interp.methods.main) {
          _interp.callUserMethod(_interp.methods.main, [], null, null, []);
        }
        return _interp.getOutput();
      } catch (e) {
        if (e instanceof ParseError) return 'Parse error: ' + e.message;
        if (e instanceof CodError)  return 'Error: ' + e.message;
        return 'Error: ' + (e && e.message ? e.message : String(e));
      }
    },

    /**
     * Reset REPL state — clears all variables, types, and methods.
     */
    reset() {
      for (const k of Object.keys(_globals)) delete _globals[k];
      _interp.types   = Object.create(null);
      _interp.methods = Object.create(null);
      _interp.output  = '';
    },
  };

  // Export
  if (typeof window !== 'undefined') {
    window.CodREPLRunner = CodREPLRunner;
    window.CoderiveLanguage = { tokenize };
  } else if (typeof module !== 'undefined' && module.exports) {
    module.exports = { CodREPLRunner, tokenize, Parser, CodInterpreter };
  }

})(typeof window !== 'undefined' ? window : (typeof global !== 'undefined' ? global : this));
