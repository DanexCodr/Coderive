# CodBoot Self-Hosting Plan for Coderive

## Goal

Make Coderive self-hosting by moving language implementation logic into a shared `core.ce` runtime, while reducing JS/Java hosts to stable boot loaders (`CodBoot.js`, `CodBoot.java`) with minimal host dependencies.

## Scope and constraints

- Use a single shared `core.ce` as the source of truth for language behavior.
- Keep host responsibilities limited to platform I/O and process boundaries.
- Ensure identical behavior across JS and Java hosts for the same `core.ce`.
- Treat JS/Java language logic still required for bootstrapping as transitional only.

## Dependency model (authoritative)

### Level 1 (absolute minimum host dependencies)

- `read-file`
- `print`
- `exit`

### Level 2 (recommended for speed)

- `add`
- `subtract`
- `multiply`
- `divide`
- `less-than`
- `greater-than`
- `equal`
- `string-append`

### Level 3 (optional host dependencies)

- `write-file`
- `input`
- `now`
- `random`
- `system`

## Architecture boundaries

### Must remain in host

- File system access
- Console output
- Process lifecycle/termination
- Network operations

### Must move into Coderive core (`core.ce`)

- Tokenizer/lexer
- Parser
- AST representation and transforms
- Evaluator/interpreter
- Environment and variable binding
- Function representation and invocation
- List operations (`car`, `cdr`, `cons`)
- Conditionals (`if`)
- Function definition (`define`, `lambda`)
- Recursion handling
- Error model and propagation
- Garbage collection strategy (if required)

## Target deliverables

- `CodBoot.js` constrained bootstrap host (target ~60 lines, excluding formatting/comments).
- `CodBoot.java` constrained bootstrap host (target ~80 lines, excluding formatting/comments).
- `core.ce` with full language pipeline (initial target <500 lines, then evolve as needed).
- Parity and bootstrap validation suite runnable on both hosts.

## Phased implementation plan

### Phase 0: Baseline inventory and contracts

- Inventory current JS and Java implementations: lexer, parser, evaluator, runtime primitives, and builtins.
- Define a shared host interface contract for Level 1-3 dependencies.
- Document canonical data exchange format between host and `core.ce` (values, errors, source locations).
- Define deterministic behavior rules (numeric ops, string rules, error text normalization).

**Exit criteria**
- Complete inventory table mapping each existing feature to host/core ownership.
- Signed-off host contract document used by both JS and Java boot hosts.

### Phase 1: Minimal CodBoot hosts

- Implement `CodBoot.js` and `CodBoot.java` wrappers exposing only approved host dependency levels.
- Ensure both hosts can load `core.ce` from disk and invoke a canonical entrypoint.
- Add strict guardrails so host-only APIs are not leaked into core accidentally.
- Add stable startup/exit conventions (success/failure exit codes and top-level error printing).

**Exit criteria**
- Both hosts run `core.ce` entrypoint with only Level 1 dependencies enabled.
- Behavior is consistent across JS and Java for startup and fatal error handling.

### Phase 2: Core pipeline migration into `core.ce`

- Port lexer/tokenizer into `core.ce` and route execution through core first.
- Port parser and AST construction into `core.ce`.
- Port evaluator and environment model into `core.ce`.
- Port core forms and runtime semantics (`if`, `define`, `lambda`, recursion, lists).
- Unify error handling semantics in `core.ce` and keep host errors as transport-only.

**Exit criteria**
- Parsing and evaluation are performed by `core.ce`, not by JS/Java language logic.
- Same `.cod` file produces equivalent output and errors on both hosts.

### Phase 3: Bootstrap and self-interpretation

- Enable `core.ce` to load and run external `.cod` programs.
- Add bootstrap flow where `core.ce` can load and run itself.
- Validate recursive bootstrap stability across repeated runs.
- Introduce optional Level 2 dependencies to improve performance without changing semantics.

**Exit criteria**
- JS host loads and runs `core.ce`.
- Java host loads and runs the exact same `core.ce`.
- `core.ce` can parse/evaluate any supported `.cod` input and can run itself.

### Phase 4: Decommission host language logic

- Remove or isolate legacy JS/Java parser/lexer/evaluator paths from production runtime.
- Keep legacy implementations only if needed for temporary fallback behind explicit flags.
- Lock host APIs and mark CodBoot hosts as feature-frozen.
- Move all new language work to `core.ce` exclusively.

**Exit criteria**
- Removing JS/Java parser/lexer/evaluator does not break runtime operation.
- New language features are implemented only in `core.ce`.

### Phase 5: Hardening, parity, and freeze

- Build a parity suite with functional, error-path, and edge-case coverage.
- Add cross-host differential tests comparing outputs, errors, and exit codes.
- Add bootstrap regression tests in CI-like flows.
- Performance profile with/without Level 2 dependencies and document trade-offs.
- Freeze CodBoot hosts when interface and behavior are stable.

**Exit criteria**
- Same `core.ce` behaves identically on JS and Java backends.
- CodBoot JS/Java require no changes for new language features.

## Acceptance gates (mapped to your success criteria)

1. JS CodBoot can load and run `core.ce`.
2. Java CodBoot can load and run the same `core.ce`.
3. `core.ce` can parse any supported `.cod` program.
4. `core.ce` can evaluate any supported `.cod` program.
5. `core.ce` can load and run itself.
6. Deleting JS/Java parser and lexer does not break runtime behavior.

## “Not self-hosting yet” anti-criteria

Do **not** declare success if any of the following remain true:

- Parser still executes in JS or Java in normal runtime.
- Lexer still executes in JS or Java in normal runtime.
- Evaluator still executes in JS or Java in normal runtime.
- Language semantics are maintained in duplicate (host + `core.ce`) rather than core-first.

## Current execution reality (2026-04)

- `core.ce` is now executable by the primary Coderive runtime (no longer metadata-only), which removes one historical blocker in Phase 3.
- CodBoot JS/Java now execute full-language `.cod` programs through `CommandRunner` runtime bridging and keep only boundary adaptation plus legacy-compat handling for codboot parity directives.
- Remaining Option-1 completion work is focused on hardening/cleanup (removing dormant host parser/evaluator code paths entirely while preserving parity gates and host boundary constraints).

## Testing and validation strategy

- **Cross-host parity tests:** same input set, compare output/error/exit code.
- **Golden corpus:** canonical `.cod` programs for syntax, semantics, recursion, and lists.
- **Bootstrap tests:** run `core.ce` on itself and verify deterministic behavior.
- **Negative tests:** malformed syntax, runtime type errors, missing symbols, stack boundary cases.
- **Compatibility gates:** ensure host contracts remain unchanged when `core.ce` evolves.

## Risk register and mitigations

- **Semantic drift between hosts:** mitigate with differential parity tests and canonical error normalization.
- **Bootstrap circularity issues:** mitigate with staged bootstrap and deterministic entrypoints.
- **Performance regressions after migration:** mitigate by enabling Level 2 arithmetic/string ops in host.
- **Hidden host coupling:** mitigate with strict API boundary checks and host capability whitelists.
- **Large first-core complexity:** mitigate by migrating in slices (lexer → parser → evaluator) with gates.

## Ownership and operating model

- CodBoot hosts are platform adapters; core team owns language evolution in `core.ce`.
- Any new language feature proposal must include `core.ce` implementation and cross-host parity tests.
- Host changes are allowed only for boundary capabilities (I/O/process/network), not semantics.

## Definition of done (final)

- CodBoot JS and Java are frozen except boundary maintenance.
- All language development is in `core.ce`.
- One shared `core.ce` runs identically on both hosts.
- Legacy host parsers/lexers/evaluators are removed from primary runtime path.
