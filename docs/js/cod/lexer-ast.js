(function (global) {
  'use strict';
  const shared = global.CoderiveCod = global.CoderiveCod || {};

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
    ['$', 'DOLLAR'],
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
  
  shared.TT = TT;
  shared.KEYWORDS = KEYWORDS;
  shared.SYMBOLS = SYMBOLS;
  shared.tokenize = tokenize;
  shared.N = N;
  shared.mkNode = mkNode;
  shared.isExprNode = isExprNode;

  class ParseError extends Error {
    constructor(msg) { super(msg); this.name = 'ParseError'; }
  }
  shared.ParseError = ParseError;

})(typeof window !== 'undefined' ? window : (typeof global !== 'undefined' ? global : this));
