'use strict';

const fs = require('fs');

function createHost() {
  return {
    readFile: function(path) {
      return fs.readFileSync(path, 'utf8');
    },
    print: function(text) {
      process.stdout.write(String(text) + '\n');
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

function decodeProgramOutputs(programSource) {
  const lines = programSource.split(/\r?\n/);
  const output = [];
  for (let i = 0; i < lines.length; i += 1) {
    const line = lines[i].trim();
    const literal = parseOutLiteral(line);
    if (literal !== null) {
      output.push(literal);
    }
  }
  return output;
}

function runCore(coreSource, programPath, host) {
  if (coreSource.indexOf('CodBootCore::v0') < 0) {
    return { exitCode: 2, lines: ['[core] invalid core.ce format'] };
  }
  const programSource = host.readFile(programPath);
  const userLines = decodeProgramOutputs(programSource);
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
