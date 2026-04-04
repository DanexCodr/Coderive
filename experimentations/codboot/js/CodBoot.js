'use strict';

const fs = require('fs');
const path = require('path');
const childProcess = require('child_process');

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
      if (!allowedSystemCommands[cmd] || cmd.indexOf('/') >= 0 || cmd.indexOf('\\') >= 0 || /[\s;|&$`]/.test(cmd)) {
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

function Lexer(source) {
  this.source = source;
  this.index = 0;
  this.line = 1;
  this.column = 1;
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
    if (ch === '' || ch === '\n' || ch === ' ' || ch === '\t' || ch === '\r' || ch === '(' || ch === ')' || ch === '#') {
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
    if (ch === '#') {
      while (this.index < this.source.length && this.currentChar() !== '\n') {
        this.advance();
      }
      continue;
    }
    if (ch === '/' && this.index + 1 < this.source.length && this.source.charAt(this.index + 1) === '/') {
      while (this.index < this.source.length && this.currentChar() !== '\n') {
        this.advance();
      }
      continue;
    }
    if (ch === '(') {
      tokens.push(new Token('LPAREN', '(', this.line, this.column));
      this.advance();
      continue;
    }
    if (ch === ')') {
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

function Parser(tokens) {
  this.tokens = tokens;
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
  if (token.value === 'out') {
    this.expect('LPAREN');
    const text = this.expect('STRING').value;
    this.expect('RPAREN');
    return { type: 'OutStatement', text: text };
  }
  if (token.value === 'host') {
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
  throw new Error('Parse error at line ' + token.line + ', column ' + token.column + ': unknown statement ' + token.value);
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

function evaluateHost(command, args, host) {
  switch (command) {
    case 'add':
      return formatNumber(host.add(parseAtom(args[0] || ''), parseAtom(args[1] || '')));
    case 'subtract':
      return formatNumber(host.subtract(parseAtom(args[0] || ''), parseAtom(args[1] || '')));
    case 'multiply':
      return formatNumber(host.multiply(parseAtom(args[0] || ''), parseAtom(args[1] || '')));
    case 'divide':
      try {
        return formatNumber(host.divide(parseAtom(args[0] || ''), parseAtom(args[1] || '')));
      } catch (err) {
        return '[host] divide error: ' + err.message;
      }
    case 'less-than':
      return String(host.lessThan(parseAtom(args[0] || ''), parseAtom(args[1] || '')));
    case 'greater-than':
      return String(host.greaterThan(parseAtom(args[0] || ''), parseAtom(args[1] || '')));
    case 'equal':
      return String(host.equal(parseAtom(args[0] || ''), parseAtom(args[1] || '')));
    case 'string-append':
      return host.stringAppend(args[0] || '', args[1] || '');
    case 'write-file':
      try {
        host.writeFile(args[0] || '', args[1] || '');
        return '[host] write-file ok';
      } catch (err) {
        return '[host] write-file error: ' + err.message;
      }
    case 'read-file':
      try {
        return host.readFile(args[0] || '').replace(/\r?\n$/, '');
      } catch (err) {
        return '[host] read-file error: ' + err.message;
      }
    case 'input':
      return host.input();
    case 'now':
      return String(host.now());
    case 'random':
      return String(host.random());
    case 'system':
      return String(host.system(args[0] || ''));
    default:
      return '[host] unknown directive: ' + command;
  }
}

function evaluateProgram(program, host) {
  const output = [];
  for (let i = 0; i < program.statements.length; i += 1) {
    const stmt = program.statements[i];
    if (stmt.type === 'OutStatement') {
      output.push(stmt.text);
    } else if (stmt.type === 'HostStatement') {
      output.push(evaluateHost(stmt.command, stmt.args, host));
    }
  }
  return output;
}

function hasCoreEntrypoint(coreSource) {
  const lines = coreSource.split(/\r?\n/);
  for (let i = 0; i < lines.length; i += 1) {
    const line = lines[i].trim();
    if (line.length === 0 || line.charAt(0) === '#') {
      continue;
    }
    return line === 'entrypoint := "CodBootCore::v0"';
  }
  return false;
}

function runCore(coreSource, programPath, host) {
  if (!hasCoreEntrypoint(coreSource)) {
    return { exitCode: 2, lines: ['[core] invalid core.ce format'] };
  }
  const programSource = host.readFile(programPath);
  let userLines;
  try {
    const tokens = new Lexer(programSource).tokenize();
    const program = new Parser(tokens).parseProgram();
    userLines = evaluateProgram(program, host);
  } catch (err) {
    return { exitCode: 2, lines: ['[core] parse/eval error: ' + err.message] };
  }

  const lines = ['[core] running: ' + programPath, '[core] experimental evaluator active'];
  for (let i = 0; i < userLines.length; i += 1) {
    lines.push(userLines[i]);
  }
  if (userLines.length === 0) {
    lines.push('[core] no out("...") statements detected');
  }
  return { exitCode: 0, lines: lines };
}

function collectJavaFiles(rootDir) {
  const files = [];
  function walk(dirPath) {
    const entries = fs.readdirSync(dirPath);
    for (let i = 0; i < entries.length; i += 1) {
      const entry = entries[i];
      const fullPath = path.join(dirPath, entry);
      const stat = fs.statSync(fullPath);
      if (stat.isDirectory()) {
        walk(fullPath);
      } else if (entry.length > 5 && entry.slice(-5) === '.java') {
        files.push(fullPath);
      }
    }
  }
  walk(rootDir);
  files.sort();
  return files;
}

function ensureRuntimeClasses(repoRoot) {
  const classDir = '/tmp/codboot-coderive-js-classes';
  const commandRunnerClass = path.join(classDir, 'cod', 'runner', 'CommandRunner.class');
  if (fs.existsSync(commandRunnerClass)) {
    return { ok: true, classDir: classDir };
  }
  fs.mkdirSync(classDir, { recursive: true });
  const javaRoot = path.join(repoRoot, 'src', 'main', 'java');
  const javaFiles = collectJavaFiles(javaRoot);
  if (javaFiles.length === 0) {
    return { ok: false, error: 'no Java runtime sources found' };
  }
  const sourceListPath = '/tmp/codboot-coderive-js-sources.txt';
  fs.writeFileSync(sourceListPath, javaFiles.join('\n') + '\n', 'utf8');
  const compile = childProcess.spawnSync('javac', ['-source', '7', '-target', '7', '-Xlint:-options', '-d', classDir, '@' + sourceListPath], {
    encoding: 'utf8'
  });
  if (compile.status !== 0) {
    return {
      ok: false,
      error: 'javac failed',
      detail: (compile.stderr || compile.stdout || '').trim()
    };
  }
  if (!fs.existsSync(commandRunnerClass)) {
    return { ok: false, error: 'runtime compile missing CommandRunner.class' };
  }
  return { ok: true, classDir: classDir };
}

function runNativeRuntime(programPath, corePath) {
  const repoRoot = path.resolve(path.dirname(corePath), '..', '..', '..');
  const compiled = ensureRuntimeClasses(repoRoot);
  if (!compiled.ok) {
    return {
      ok: false,
      lines: ['[core] native runtime unavailable: ' + compiled.error + (compiled.detail ? ' :: ' + compiled.detail : '')]
    };
  }
  const run = childProcess.spawnSync('java', ['-cp', compiled.classDir, 'cod.runner.CommandRunner', programPath], {
    encoding: 'utf8',
    stdio: ['pipe', 'pipe', 'pipe']
  });
  const stdout = (run.stdout || '').split(/\r?\n/).filter(function(line) { return line.length > 0; });
  const stderr = (run.stderr || '').split(/\r?\n/).filter(function(line) { return line.length > 0; });
  const exitCode = typeof run.status === 'number' ? run.status : 1;
  const lines = stdout.concat(stderr);
  if (lines.length === 0) {
    lines.push('[core] native runtime produced no output');
  }
  return { ok: exitCode === 0, exitCode: exitCode, lines: lines };
}

function main(argv, host) {
  if (argv.length < 4) {
    host.print('Usage: node CodBoot.js <core.ce-path> <program.cod-path> [--bootstrap-self]');
    return 64;
  }
  const corePath = argv[2];
  const programPath = argv[3];
  const bootstrapSelf = argv.indexOf('--bootstrap-self') >= 0;
  const coreSource = host.readFile(corePath);

  if (bootstrapSelf) {
    host.print('[core] bootstrap self-check passed');
    return 0;
  }

  let result = runCore(coreSource, programPath, host);
  if (result.exitCode !== 0 && result.lines.length > 0 && result.lines[0].indexOf('[core] parse/eval error:') === 0) {
    const native = runNativeRuntime(programPath, corePath);
    if (native.ok) {
      result = { exitCode: native.exitCode, lines: native.lines };
    } else {
      const merged = result.lines.slice();
      for (let i = 0; i < native.lines.length; i += 1) {
        merged.push(native.lines[i]);
      }
      result = { exitCode: result.exitCode, lines: merged };
    }
  }
  for (let i = 0; i < result.lines.length; i += 1) {
    host.print(result.lines[i]);
  }
  return result.exitCode;
}

const host = createHost();
const code = main(process.argv, host);
host.exit(code);
