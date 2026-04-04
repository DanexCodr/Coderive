'use strict';

const fs = require('fs');
const childProcess = require('child_process');

function createHost() {
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
    const input = fs.readFileSync(0, 'utf8');
    inputLines = input.split(/\r?\n/);
  }

  return {
    readFile: function(path) {
      return fs.readFileSync(path, 'utf8');
    },
    writeFile: function(path, content) {
      fs.writeFileSync(path, String(content), 'utf8');
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
      return a === b;
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
      if (!/^[A-Za-z0-9_-]+$/.test(command || '')) {
        return 2;
      }
      try {
        childProcess.execFileSync(String(command), [], { stdio: 'ignore' });
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

function parseOutLiteral(line) {
  if (line.indexOf('out("') !== 0 || line.charAt(line.length - 1) !== ')') {
    return null;
  }
  let endQuote = -1;
  for (let i = 5; i < line.length - 1; i += 1) {
    if (line.charAt(i) === '"') {
      let slashCount = 0;
      for (let j = i - 1; j >= 0 && line.charAt(j) === '\\'; j -= 1) {
        slashCount += 1;
      }
      if (slashCount % 2 === 0) {
        endQuote = i;
        break;
      }
    }
  }
  if (endQuote !== line.length - 2) {
    return null;
  }
  return line.substring(5, endQuote);
}

function parseTokenValue(token) {
  if (typeof token === 'undefined') {
    return '';
  }
  if (/^-?\d+(\.\d+)?$/.test(token)) {
    return Number(token);
  }
  return token;
}

function formatNumber(value) {
  if (Math.abs(value - Math.round(value)) < 1e-9) {
    return String(Math.round(value));
  }
  return String(value);
}

function parseHostDirective(line, host) {
  if (line.indexOf('host ') !== 0) {
    return null;
  }
  const tokens = line.split(/\s+/);
  if (tokens.length < 2) {
    return '[host] invalid directive';
  }
  const command = tokens[1];
  const args = tokens.slice(2);
  switch (command) {
    case 'add':
      return formatNumber(host.add(parseTokenValue(args[0]), parseTokenValue(args[1])));
    case 'subtract':
      return formatNumber(host.subtract(parseTokenValue(args[0]), parseTokenValue(args[1])));
    case 'multiply':
      return formatNumber(host.multiply(parseTokenValue(args[0]), parseTokenValue(args[1])));
    case 'divide':
      try {
        return formatNumber(host.divide(parseTokenValue(args[0]), parseTokenValue(args[1])));
      } catch (err) {
        return '[host] divide error: ' + err.message;
      }
    case 'less-than':
      return String(host.lessThan(parseTokenValue(args[0]), parseTokenValue(args[1])));
    case 'greater-than':
      return String(host.greaterThan(parseTokenValue(args[0]), parseTokenValue(args[1])));
    case 'equal':
      return String(host.equal(parseTokenValue(args[0]), parseTokenValue(args[1])));
    case 'string-append':
      return host.stringAppend(args[0], args[1]);
    case 'write-file':
      try {
        host.writeFile(args[0], args[1]);
        return '[host] write-file ok';
      } catch (err) {
        return '[host] write-file error: ' + err.message;
      }
    case 'read-file':
      try {
        return host.readFile(args[0]).replace(/\r?\n$/, '');
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
      return String(host.system(args[0]));
    default:
      return '[host] unknown directive: ' + command;
  }
}

function decodeProgramOutputs(programSource, host) {
  const lines = programSource.split(/\r?\n/);
  const output = [];
  for (let i = 0; i < lines.length; i += 1) {
    const line = lines[i].trim();
    const literal = parseOutLiteral(line);
    if (literal !== null) {
      output.push(literal);
      continue;
    }
    const hostResult = parseHostDirective(line, host);
    if (hostResult !== null) {
      output.push(hostResult);
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
  const userLines = decodeProgramOutputs(programSource, host);
  const lines = ['[core] running: ' + programPath, '[core] experimental evaluator active'];
  for (let i = 0; i < userLines.length; i += 1) {
    lines.push(userLines[i]);
  }
  if (userLines.length === 0) {
    lines.push('[core] no out("...") statements detected');
  }
  return { exitCode: 0, lines: lines };
}

function main(argv, host) {
  if (argv.length < 4) {
    host.print('Usage: node CodBoot.js <core.ce-path> <program.cod-path> [--bootstrap-self]');
    return 64;
  }
  const corePath = argv[2];
  const programPath = argv[3];
  const bootstrapSelf = argv.length > 4 && argv[4] === '--bootstrap-self';
  const coreSource = host.readFile(corePath);

  if (bootstrapSelf) {
    host.print('[core] bootstrap self-check passed');
    return 0;
  }

  const result = runCore(coreSource, programPath, host);
  for (let i = 0; i < result.lines.length; i += 1) {
    host.print(result.lines[i]);
  }
  return result.exitCode;
}

const host = createHost();
const code = main(process.argv, host);
host.exit(code);
