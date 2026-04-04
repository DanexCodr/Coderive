(function (global) {
  'use strict';
  const shared = global.CoderiveCod = global.CoderiveCod || {};
  const N = shared.N;
  const mkNode = shared.mkNode;

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
        const resolved = this.resolveLambdaForCall(fn);
        if (resolved.expressionBody) {
          const lambdaScope = new Scope(scope || null);
          const values = Array.isArray(args) ? args : [];
          if (fn.inferParams && resolved.aliasOnly) {
            this.bindInferredLambdaAliases(lambdaScope, values);
          }
          for (let i = 0; i < resolved.params.length; i++) {
            const p = resolved.params[i];
            if (!p || !p.name) continue;
            lambdaScope.vars[p.name] = i < values.length ? values[i] : null;
          }
          return this.eval(resolved.expressionBody, lambdaScope);
        }
        return this.callUserMethod(
          { params: resolved.params, returnSlots: fn.returnSlots, body: fn.body },
          args, scope, null, []
        );
      }
      throw new CodError('Expected a lambda/function');
    }

    bindInferredLambdaAliases(lambdaScope, values) {
      if (!values || values.length === 0) return;
      const first = values[0];
      this.bindIfAbsent(lambdaScope, '$item', first);
      this.bindIfAbsent(lambdaScope, '$left', first);
      this.bindIfAbsent(lambdaScope, '$acc', first);
      this.bindIfAbsent(lambdaScope, '$value', first);
      if (values.length > 1) {
        const second = values[1];
        this.bindIfAbsent(lambdaScope, '$index', second);
        this.bindIfAbsent(lambdaScope, '$right', second);
        this.bindIfAbsent(lambdaScope, '$next', second);
      }
      if (values.length > 2) {
        const third = values[2];
        this.bindIfAbsent(lambdaScope, '$index', third);
        this.bindIfAbsent(lambdaScope, '$position', third);
      }
    }

    bindIfAbsent(lambdaScope, name, value) {
      if (!(name in lambdaScope.vars)) {
        lambdaScope.vars[name] = value;
      }
    }

    resolveLambdaForCall(fn) {
      const params = Array.isArray(fn.params) ? fn.params.slice() : [];
      if (params.length > 0 || !fn.inferParams) {
        return { params, expressionBody: fn.expressionBody || null };
      }
      const names = this.collectPlaceholderNames(fn.expressionBody || fn.body);
      if (names.length === 0) {
        return {
          params: [],
          expressionBody: fn.expressionBody || null,
          aliasOnly: true,
        };
      }
      return {
        params: names.map(name => ({ _type: N.PARAM, name, type: null, defaultValue: null })),
        expressionBody: fn.expressionBody || null,
        aliasOnly: false,
      };
    }

    collectPlaceholderNames(node, out) {
      const names = out || [];
      if (!node || typeof node !== 'object') return names;
      if (node._type === N.IDENT && typeof node.name === 'string' && node.name.startsWith('$') && node.name.length > 1) {
        if (!names.includes(node.name)) names.push(node.name);
        return names;
      }
      if (node._type === N.LAMBDA) return names;
      for (const key of Object.keys(node)) {
        const val = node[key];
        if (Array.isArray(val)) {
          for (const item of val) this.collectPlaceholderNames(item, names);
        } else if (val && typeof val === 'object') {
          this.collectPlaceholderNames(val, names);
        }
      }
      return names;
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
  
  shared.CodError = CodError;
  shared.CodInterpreter = CodInterpreter;
  shared.ObjectInstance = ObjectInstance;

})(typeof window !== 'undefined' ? window : (typeof global !== 'undefined' ? global : this));
