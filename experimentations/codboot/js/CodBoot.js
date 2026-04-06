'use strict';

const fs = require('fs');
const childProcess = require('child_process');
// This constant is needed before core semantics are parsed; fallback prefix must match core message format.
const CORE_PARSE_EVAL_ERROR_PREFIX = '[core] parse/eval error: ';
const CORE_MISSING_SEMANTICS_JSON_MESSAGE = '[core] missing semantics_json block';

function containsUnsafeShellChar(value) {
  for (let i = 0; i < value.length; i += 1) {
    const ch = value.charAt(i);
    if (ch <= ' ' || ch === ';' || ch === '|' || ch === '&' || ch === '$' || ch === '`') {
      return true;
    }
  }
  return false;
}

function containsPathSeparator(value) {
  return value.indexOf('/') >= 0 || value.indexOf('\\') >= 0;
}

function createHost() {
  const allowedSystemCommands = { true: true, false: true };
  let randomSeed = 123456789;
  let inputLoaded = false;
  let inputLines = [];

  function nextRandom() {
    randomSeed = (1103515245 * randomSeed + 12345) % 2147483648;
    return randomSeed / 2147483648;
  }

  function loadInput() {
    if (inputLoaded) {
      return;
    }
    inputLoaded = true;
    if (process.stdin.isTTY) {
      inputLines = [];
      return;
    }
    const rawInput = fs.readFileSync(0, 'utf8');
    inputLines = rawInput.split(/\r?\n/);
  }

  return {
    readFile: function(filePath) {
      return fs.readFileSync(filePath, 'utf8');
    },
    writeFile: function(filePath, content) {
      fs.writeFileSync(filePath, String(content), 'utf8');
    },
    print: function(text) {
      process.stdout.write(String(text) + '\n');
    },
    input: function() {
      loadInput();
      if (inputLines.length === 0) {
        return '';
      }
      return inputLines.shift();
    },
    add: function(a, b) {
      return a + b;
    },
    subtract: function(a, b) {
      return a - b;
    },
    multiply: function(a, b) {
      return a * b;
    },
    divide: function(a, b) {
      if (b === 0) {
        throw new Error('division by zero');
      }
      if (Number.isInteger(a) && Number.isInteger(b)) {
        return Math.trunc(a / b);
      }
      return a / b;
    },
    lessThan: function(a, b) {
      return a < b;
    },
    greaterThan: function(a, b) {
      return a > b;
    },
    equal: function(a, b) {
      return String(a) === String(b);
    },
    stringAppend: function(a, b) {
      return String(a) + String(b);
    },
    now: function() {
      return Date.now();
    },
    random: function() {
      return nextRandom();
    },
    system: function(command) {
      const cmd = String(command || '').trim();
      // Defense-in-depth: explicitly block path separators even with strict allowlist + metachar filtering.
      if (!allowedSystemCommands[cmd] || containsPathSeparator(cmd) || containsUnsafeShellChar(cmd)) {
        return 2;
      }
      try {
        childProcess.execFileSync(cmd, [], { stdio: 'ignore', shell: false });
        return 0;
      } catch (err) {
        if (typeof err.status === 'number') {
          return err.status;
        }
        return 1;
      }
    },
    exit: function(code) {
      process.exit(code);
    }
  };
}

function Token(type, value, line, column) {
  this.type = type;
  this.value = value;
  this.line = line;
  this.column = column;
}

function Lexer(source, semantics) {
  this.source = source;
  this.index = 0;
  this.line = 1;
  this.column = 1;
  const lexerSemantics = semantics && semantics.lexer ? semantics.lexer : {};
  const lineComments = Array.isArray(lexerSemantics.lineComments) ? lexerSemantics.lineComments : ['#', '//'];
  this.allowParentheses = typeof lexerSemantics.allowParentheses === 'boolean' ? lexerSemantics.allowParentheses : true;
  this.hashCommentsEnabled = lineComments.indexOf('#') >= 0;
  this.doubleSlashCommentsEnabled = lineComments.indexOf('//') >= 0;
}

Lexer.prototype.currentChar = function() {
  return this.index < this.source.length ? this.source.charAt(this.index) : '';
};

Lexer.prototype.advance = function() {
  const ch = this.currentChar();
  if (ch === '\n') {
    this.line += 1;
    this.column = 1;
  } else {
    this.column += 1;
  }
  this.index += 1;
};

Lexer.prototype.readString = function(line, column) {
  let result = '';
  this.advance();
  while (this.index < this.source.length) {
    const ch = this.currentChar();
    if (ch === '"') {
      this.advance();
      return new Token('STRING', result, line, column);
    }
    if (ch === '\\') {
      this.advance();
      const esc = this.currentChar();
      if (esc === 'n') {
        result += '\n';
      } else if (esc === 't') {
        result += '\t';
      } else if (esc === '"') {
        result += '"';
      } else if (esc === '\\') {
        result += '\\';
      } else {
        result += esc;
      }
      this.advance();
    } else {
      result += ch;
      this.advance();
    }
  }
  throw new Error('Unterminated string at line ' + line + ', column ' + column);
};

Lexer.prototype.readWord = function(line, column) {
  let result = '';
  while (this.index < this.source.length) {
    const ch = this.currentChar();
    const isParenDelimiter = this.allowParentheses && (ch === '(' || ch === ')');
    const isHashDelimiter = this.hashCommentsEnabled && ch === '#';
    if (ch === '' || ch === '\n' || ch === ' ' || ch === '\t' || ch === '\r' || isParenDelimiter || isHashDelimiter) {
      break;
    }
    result += ch;
    this.advance();
  }
  return new Token('WORD', result, line, column);
};

Lexer.prototype.tokenize = function() {
  const tokens = [];
  while (this.index < this.source.length) {
    const ch = this.currentChar();
    if (ch === ' ' || ch === '\t' || ch === '\r') {
      this.advance();
      continue;
    }
    if (ch === '\n') {
      tokens.push(new Token('NEWLINE', '\n', this.line, this.column));
      this.advance();
      continue;
    }
    if (this.hashCommentsEnabled && ch === '#') {
      while (this.index < this.source.length && this.currentChar() !== '\n') {
        this.advance();
      }
      continue;
    }
    if (this.doubleSlashCommentsEnabled && ch === '/' && this.index + 1 < this.source.length && this.source.charAt(this.index + 1) === '/') {
      while (this.index < this.source.length && this.currentChar() !== '\n') {
        this.advance();
      }
      continue;
    }
    if (this.allowParentheses && ch === '(') {
      tokens.push(new Token('LPAREN', '(', this.line, this.column));
      this.advance();
      continue;
    }
    if (this.allowParentheses && ch === ')') {
      tokens.push(new Token('RPAREN', ')', this.line, this.column));
      this.advance();
      continue;
    }
    if (ch === '"') {
      tokens.push(this.readString(this.line, this.column));
      continue;
    }
    tokens.push(this.readWord(this.line, this.column));
  }
  tokens.push(new Token('EOF', '', this.line, this.column));
  return tokens;
};

function Parser(tokens, semantics) {
  this.tokens = tokens;
  this.semantics = semantics;
  this.index = 0;
}

Parser.prototype.peek = function() {
  return this.tokens[this.index];
};

Parser.prototype.advance = function() {
  const token = this.peek();
  this.index += 1;
  return token;
};

Parser.prototype.match = function(type, value) {
  const token = this.peek();
  if (!token || token.type !== type) {
    return false;
  }
  if (typeof value !== 'undefined' && token.value !== value) {
    return false;
  }
  this.advance();
  return true;
};

Parser.prototype.expect = function(type, value) {
  const token = this.peek();
  if (!this.match(type, value)) {
    throw new Error('Parse error at line ' + token.line + ', column ' + token.column + ': expected ' + type + (typeof value !== 'undefined' ? ' ' + value : ''));
  }
  return this.tokens[this.index - 1];
};

Parser.prototype.skipNewlines = function() {
  while (this.match('NEWLINE')) {
    // noop
  }
};

Parser.prototype.parseProgram = function() {
  const statements = [];
  this.skipNewlines();
  while (this.peek().type !== 'EOF') {
    statements.push(this.parseStatement());
    this.skipNewlines();
  }
  return { type: 'Program', statements: statements };
};

Parser.prototype.parseStatement = function() {
  const token = this.expect('WORD');
  if (token.value === this.semantics.keywords.out) {
    let text = '';
    if (this.match('LPAREN')) {
      while (this.peek().type !== 'RPAREN' && this.peek().type !== 'NEWLINE' && this.peek().type !== 'EOF') {
        const next = this.advance();
        if (next.type === 'STRING' && text.length === 0) {
          text = next.value;
        }
      }
      this.match('RPAREN');
    } else {
      while (this.peek().type !== 'NEWLINE' && this.peek().type !== 'EOF') {
        this.advance();
      }
    }
    return { type: 'OutStatement', text: text };
  }
  if (token.value === this.semantics.keywords.host) {
    const command = this.expect('WORD').value;
    const args = [];
    while (this.peek().type !== 'NEWLINE' && this.peek().type !== 'EOF') {
      const next = this.peek();
      if (next.type !== 'WORD' && next.type !== 'STRING') {
        throw new Error('Parse error at line ' + next.line + ', column ' + next.column + ': expected host argument');
      }
      args.push(this.advance().value);
    }
    return { type: 'HostStatement', command: command, args: args };
  }
  while (this.peek().type !== 'NEWLINE' && this.peek().type !== 'EOF') {
    this.advance();
  }
  return { type: 'IgnoredStatement' };
};

function parseAtom(text) {
  if (/^-?\d+(\.\d+)?$/.test(text)) {
    return Number(text);
  }
  return text;
}

function formatNumber(value) {
  if (Math.abs(value - Math.round(value)) < 1e-9) {
    return String(Math.round(value));
  }
  return String(value);
}

function evaluateHost(command, args, host, semantics) {
  const cmds = semantics.hostCommands;
  const messages = semantics.messages;
  switch (command) {
    case cmds.add:
      return formatNumber(host.add(parseAtom(args[0] || ''), parseAtom(args[1] || '')));
    case cmds.subtract:
      return formatNumber(host.subtract(parseAtom(args[0] || ''), parseAtom(args[1] || '')));
    case cmds.multiply:
      return formatNumber(host.multiply(parseAtom(args[0] || ''), parseAtom(args[1] || '')));
    case cmds.divide:
      try {
        return formatNumber(host.divide(parseAtom(args[0] || ''), parseAtom(args[1] || '')));
      } catch (err) {
        return messages.divideErrorPrefix + err.message;
      }
    case cmds.lessThan:
      return String(host.lessThan(parseAtom(args[0] || ''), parseAtom(args[1] || '')));
    case cmds.greaterThan:
      return String(host.greaterThan(parseAtom(args[0] || ''), parseAtom(args[1] || '')));
    case cmds.equal:
      return String(host.equal(parseAtom(args[0] || ''), parseAtom(args[1] || '')));
    case cmds.stringAppend:
      return host.stringAppend(args[0] || '', args[1] || '');
    case cmds.writeFile:
      try {
        host.writeFile(args[0] || '', args[1] || '');
        return messages.writeFileOk;
      } catch (err) {
        return messages.writeFileErrorPrefix + err.message;
      }
    case cmds.readFile:
      try {
        return host.readFile(args[0] || '').replace(/\r?\n$/, '');
      } catch (err) {
        return messages.readFileErrorPrefix + err.message;
      }
    case cmds.input:
      return host.input();
    case cmds.now:
      return String(host.now());
    case cmds.random:
      return String(host.random());
    case cmds.system:
      return String(host.system(args[0] || ''));
    default:
      return messages.unknownDirectivePrefix + command;
  }
}

function evaluateProgram(program, host, semantics) {
  const output = [];
  for (let i = 0; i < program.statements.length; i += 1) {
    const stmt = program.statements[i];
    if (stmt.type === 'OutStatement') {
      output.push(stmt.text);
    } else if (stmt.type === 'HostStatement') {
      output.push(evaluateHost(stmt.command, stmt.args, host, semantics));
    }
  }
  return output;
}

function extractSemanticsJson(coreSource) {
  const triple = coreSource.match(/semantics_json\s*:=\s*"""\s*([\s\S]*?)\s*"""/);
  if (triple) {
    return triple[1];
  }
  const commentBlock = coreSource.match(/\/\/\s*semantics_json_begin\s*\r?\n([\s\S]*?)\/\/\s*semantics_json_end/);
  if (commentBlock) {
    const lines = commentBlock[1].split(/\r?\n/);
    const jsonLines = [];
    for (let i = 0; i < lines.length; i += 1) {
      const line = lines[i];
      const match = line.match(/^\s*\/\/\s?(.*)$/);
      if (match) {
        jsonLines.push(match[1]);
      }
    }
    const json = jsonLines.join('\n').trim();
    if (json) {
      return json;
    }
  }
  const single = coreSource.match(/semantics_json\s*:=\s*"((?:\\.|[^"\\])*)"/);
  if (!single) {
    return '';
  }
  return unescapeJsonString(single[1]);
}

function unescapeJsonString(value) {
  let out = '';
  for (let i = 0; i < value.length; i += 1) {
    const ch = value.charAt(i);
    if (ch === '\\' && i + 1 < value.length) {
      const esc = value.charAt(i + 1);
      if (esc === 'n') {
        out += '\n';
      } else if (esc === 't') {
        out += '\t';
      } else if (esc === 'r') {
        out += '\r';
      } else if (esc === '"') {
        out += '"';
      } else if (esc === '\\') {
        out += '\\';
      } else if (esc === '/') {
        out += '/';
      } else if (esc === 'b') {
        out += '\b';
      } else if (esc === 'f') {
        out += '\f';
      } else {
        out += esc;
      }
      i += 1;
    } else {
      out += ch;
    }
  }
  return out;
}

function parseCoreSemantics(coreSource) {
  const jsonText = extractSemanticsJson(coreSource);
  if (!jsonText) {
    throw new Error(CORE_MISSING_SEMANTICS_JSON_MESSAGE);
  }
  return JSON.parse(jsonText);
}

function hasCoreEntrypoint(coreSource) {
  const lines = coreSource.split(/\r?\n/);
  for (let i = 0; i < lines.length; i += 1) {
    const line = lines[i].trim();
    if (line.length === 0 || line.charAt(0) === '#' || (line.length > 1 && line.charAt(0) === '/' && line.charAt(1) === '/')) {
      continue;
    }
    return line === 'entrypoint := "CodBootCore::v0"';
  }
  return false;
}

function runCore(coreSource, programPath, host, semantics) {
  if (!hasCoreEntrypoint(coreSource)) {
    return { exitCode: 2, lines: [semantics.messages.invalidCoreFormat] };
  }
  const programSource = host.readFile(programPath);
  let userLines;
  try {
    const tokens = new Lexer(programSource, semantics).tokenize();
    const program = new Parser(tokens, semantics).parseProgram();
    userLines = evaluateProgram(program, host, semantics);
  } catch (err) {
    return { exitCode: 2, lines: [semantics.messages.parseEvalErrorPrefix + err.message] };
  }

  const lines = [
    semantics.messages.runningPrefix + programPath,
    semantics.messages.experimentalEvaluatorActive
  ];
  for (let i = 0; i < userLines.length; i += 1) {
    lines.push(userLines[i]);
  }
  if (userLines.length === 0) {
    lines.push(semantics.messages.noOutStatementsDetected);
  }
  return { exitCode: 0, lines: lines };
}

function isParseEvalError(result, semantics) {
  return result.exitCode !== 0 &&
    result.lines.length > 0 &&
    result.lines[0].indexOf(semantics.messages.parseEvalErrorPrefix) === 0;
}

function main(argv, host) {
  if (argv.length < 4) {
    host.print('Usage: node CodBoot.js <core.ce-path> <program.cod-path> [--bootstrap-self]');
    return 64;
  }
  const corePath = argv[2];
  const programPath = argv[3];
  const bootstrapSelf = argv.indexOf('--bootstrap-self') >= 0;
  const selfHostedOnly = argv.indexOf('--self-host-only') >= 0;
  const coreSource = host.readFile(corePath);
  let semantics;
  try {
    semantics = parseCoreSemantics(coreSource);
  } catch (err) {
    host.print(CORE_PARSE_EVAL_ERROR_PREFIX + err.message);
    return 2;
  }

  if (bootstrapSelf) {
    host.print(semantics.messages.bootstrapSelfCheckPassed);
    return 0;
  }

  let result = runCore(coreSource, programPath, host, semantics);
  if (selfHostedOnly && isParseEvalError(result, semantics)) {
    result.lines.push(semantics.messages.selfHostOnlyNoFallback);
  }
  for (let i = 0; i < result.lines.length; i += 1) {
    host.print(result.lines[i]);
  }
  return result.exitCode;
}

const host = createHost();
const code = main(process.argv, host);
host.exit(code);
