(function (global) {
  'use strict';
  const shared = global.CoderiveCod = global.CoderiveCod || {};
  const TT = shared.TT;
  const N = shared.N;
  const mkNode = shared.mkNode;
  const ParseError = shared.ParseError;

  class Parser {
    constructor(tokens) {
      this.tokens = tokens;
      this.pos = 0;
      this.bareInferredLambdaDisabledDepth = 0;
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
      if (this.bareInferredLambdaDisabledDepth === 0) {
        const inferredLambda = this.tryParseBareInferredLambda();
        if (inferredLambda) return inferredLambda;
      }

      // all[...] / any[...]
      if ((this.isKw('all') || this.isKw('any')) && this.isSymAt('LBRACKET', 1)) {
        return this.parseBooleanChain();
      }
      // if expr
      if (this.isKw('if')) return this.parseIfExpr();

      return this.parsePrecedence(10);
    }

    tryParseBareInferredLambda() {
      if (!this.isSym('DOLLAR')) return null;
      const saved = this.save();
      try {
        const expressionBody = this.parsePrecedence(10);
        if (!this.containsPlaceholderIdentifier(expressionBody)) {
          this.restore(saved);
          return null;
        }
        return mkNode(N.LAMBDA, { params: [], inferParams: true, returnSlots: null, body: null, expressionBody });
      } catch (e) {
        this.restore(saved);
        return null;
      }
    }

    containsPlaceholderIdentifier(node) {
      if (!node || typeof node !== 'object') return false;
      if (node._type === N.IDENT) {
        return typeof node.name === 'string' && node.name.startsWith('$') && node.name.length > 1;
      }
      if (node._type === N.LAMBDA) return false;
      for (const key of Object.keys(node)) {
        const val = node[key];
        if (Array.isArray(val)) {
          for (const item of val) {
            if (this.containsPlaceholderIdentifier(item)) return true;
          }
        } else if (val && typeof val === 'object') {
          if (this.containsPlaceholderIdentifier(val)) return true;
        }
      }
      return false;
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
          ['LPAREN', 'LBRACKET', 'BANG', 'PLUS', 'MINUS', 'LAMBDA', 'DOLLAR'].includes(t.symbol)) return true;
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
      let inferParams = false;
      if (!this.isSym('RPAREN')) {
        if (this.isSym('UNDERSCORE') && this.isSymAt('RPAREN', 1)) {
          this.consume();
          inferParams = true;
        } else {
          params.push(this.parseParam());
          while (this.consumeIfSym('COMMA')) params.push(this.parseParam());
        }
      }
      this.expectSym('RPAREN');

      if (!this.isSym('DOUBLE_COLON') && !this.isSym('TILDE_ARROW') && !this.isSym('LBRACE')) {
        const expressionBody = this.parseExprWithoutBareInferredLambda();
        return mkNode(N.LAMBDA, { params, inferParams, returnSlots: null, body: null, expressionBody });
      }

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

      return mkNode(N.LAMBDA, { params, inferParams, returnSlots, body, expressionBody: null });
    }

    parseExprWithoutBareInferredLambda() {
      this.bareInferredLambdaDisabledDepth++;
      try {
        return this.parseExpr();
      } finally {
        this.bareInferredLambdaDisabledDepth--;
      }
    }
  }

  // ============================================================
  
  shared.Parser = Parser;

})(typeof window !== 'undefined' ? window : (typeof global !== 'undefined' ? global : this));
