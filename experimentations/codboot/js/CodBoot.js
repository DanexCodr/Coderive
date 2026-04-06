'use strict';

const fs = require('fs');
const path = require('path');
const childProcess = require('child_process');
// This constant is needed before core semantics are parsed; keep in sync with semantics_json messages.parseEvalErrorPrefix in core.cod.
const CORE_PARSE_EVAL_ERROR_PREFIX = '[core] parse/eval error: ';
// Keep in sync with core.cod semantics_json missing-semantics error contract.
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

function isCommentLine(line) {
  return line.length > 1 && line.charAt(0) === '/' && line.charAt(1) === '/';
}

function validateBridgePath(filePath, label) {
  const value = String(filePath || '');
  if (value.length === 0 || value.indexOf('\0') >= 0 || /[\r\n]/.test(value)) {
    throw new Error('invalid ' + label + ' path');
  }
  if (!fs.existsSync(value)) {
    throw new Error(label + ' path not found: ' + value);
  }
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
    consumeRemainingInput: function() {
      loadInput();
      if (inputLines.length === 0) {
        return '';
      }
      const remaining = inputLines.join('\n');
      inputLines = [];
      return remaining;
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

function deriveRepoRootFromCorePath(corePath) {
  return path.resolve(path.dirname(corePath), '..', '..', '..');
}

function findJarFromDir(startDir) {
  let dir = path.resolve(startDir);
  for (let i = 0; i < 10; i += 1) {
    const candidate = path.join(dir, 'docs', 'assets', 'Coderive.jar');
    if (fs.existsSync(candidate)) {
      return candidate;
    }
    const parent = path.dirname(dir);
    if (parent === dir) {
      break;
    }
    dir = parent;
  }
  return '';
}

function resolveCoderiveJarPath(corePath) {
  if (process.env.CODERIVE_JAR && fs.existsSync(process.env.CODERIVE_JAR)) {
    return process.env.CODERIVE_JAR;
  }
  const fromCwd = findJarFromDir(process.cwd());
  if (fromCwd) {
    return fromCwd;
  }
  const fromCore = findJarFromDir(path.dirname(corePath));
  if (fromCore) {
    return fromCore;
  }
  return path.join(deriveRepoRootFromCorePath(corePath), 'docs', 'assets', 'Coderive.jar');
}

function runViaCommandRunner(corePath, programPath, hostInput) {
  const jarPath = resolveCoderiveJarPath(corePath);
  validateBridgePath(jarPath, 'jar');
  validateBridgePath(programPath, 'program');
  const args = ['-cp', jarPath, 'cod.runner.CommandRunner', programPath, '--quiet'];
  const result = childProcess.spawnSync('java', args, {
    encoding: 'utf8',
    cwd: process.cwd(),
    shell: false,
    input: hostInput || ''
  });
  const stdout = (result.stdout || '').replace(/\r\n/g, '\n').replace(/\n+$/, '');
  const stderr = (result.stderr || '').replace(/\r\n/g, '\n').replace(/\n+$/, '');
  const lines = stdout.length === 0 ? [] : stdout.split('\n');
  return {
    exitCode: typeof result.status === 'number' ? result.status : 1,
    lines: lines,
    stderr: stderr
  };
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
    if (line.length === 0 || line.charAt(0) === '#' || isCommentLine(line)) {
      continue;
    }
    return line === 'entrypoint := "CodBootCore::v0"';
  }
  return false;
}

function runCore(coreSource, corePath, programPath, host, semantics) {
  if (!hasCoreEntrypoint(coreSource)) {
    return { exitCode: 2, lines: [semantics.messages.invalidCoreFormat] };
  }
  try {
    const hostInput = host.consumeRemainingInput();
    const runnerResult = runViaCommandRunner(corePath, programPath, hostInput);
    if (runnerResult.exitCode !== 0) {
      const err = runnerResult.stderr.length > 0 ? runnerResult.stderr : 'CommandRunner failed';
      return { exitCode: 2, lines: [semantics.messages.parseEvalErrorPrefix + err] };
    }
    const userLines = runnerResult.lines;
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
  } catch (err) {
    return { exitCode: 2, lines: [semantics.messages.parseEvalErrorPrefix + err.message] };
  }
}

function runBootstrapSelf(corePath, semantics) {
  try {
    const runnerResult = runViaCommandRunner(corePath, corePath, '');
    if (runnerResult.exitCode !== 0) {
      const err = runnerResult.stderr.length > 0 ? runnerResult.stderr : 'CommandRunner failed';
      return { exitCode: 2, lines: [semantics.messages.parseEvalErrorPrefix + err] };
    }
    return { exitCode: 0, lines: [semantics.messages.bootstrapSelfCheckPassed] };
  } catch (err) {
    return { exitCode: 2, lines: [semantics.messages.parseEvalErrorPrefix + err.message] };
  }
}

function isParseEvalError(result, semantics) {
  return result.exitCode !== 0 &&
    result.lines.length > 0 &&
    result.lines[0].indexOf(semantics.messages.parseEvalErrorPrefix) === 0;
}

function main(argv, host) {
  if (argv.length < 4) {
    host.print('Usage: node CodBoot.js <core.cod-path> <program.cod-path> [--bootstrap-self]');
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
    const bootstrapResult = runBootstrapSelf(corePath, semantics);
    for (let i = 0; i < bootstrapResult.lines.length; i += 1) {
      host.print(bootstrapResult.lines[i]);
    }
    return bootstrapResult.exitCode;
  }

  let result = runCore(coreSource, corePath, programPath, host, semantics);
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
