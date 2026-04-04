(function (global) {
  'use strict';
  const shared = global.CoderiveCod = global.CoderiveCod || {};
  const tokenize = shared.tokenize;
  const Parser = shared.Parser;
  const CodInterpreter = shared.CodInterpreter;
  const ParseError = shared.ParseError;
  const CodError = shared.CodError;
  const TT = shared.TT;
  const N = shared.N;
  const isExprNode = shared.isExprNode;

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

  if (typeof window !== 'undefined') {
    window.CodREPLRunner = CodREPLRunner;
    window.CoderiveLanguage = { tokenize };
  } else if (typeof module !== 'undefined' && module.exports) {
    module.exports = { CodREPLRunner, tokenize, Parser, CodInterpreter };
  }

})(typeof window !== 'undefined' ? window : (typeof global !== 'undefined' ? global : this));
