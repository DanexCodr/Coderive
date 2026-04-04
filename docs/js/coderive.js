// js/coderive.js - compatibility shim; runtime moved to js/cod/*
(function(global){
  if (!global.CodREPLRunner || !global.CoderiveLanguage) {
    console.warn('Coderive runtime modules not loaded. Ensure js/cod/lexer-ast.js, parser.js, interpreter.js, repl.js are loaded in order.');
  }
})(typeof window !== 'undefined' ? window : (typeof global !== 'undefined' ? global : this));
