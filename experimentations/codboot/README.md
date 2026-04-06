# CodBoot Experimentation (JS + Java 7)

This experiment follows `implementations/CodBoot-SelfHosting-Plan.md` and keeps all changes under `experimentations/`.

## Goals

- Shared `core.cod` as source-of-truth for runtime semantics and diagnostics.
- Minimal host dependencies with staged support:
  - Level 1: `read-file`, `print`, `exit`
  - Level 2: arithmetic/comparison/string helpers
  - Level 3: optional environment helpers
- Two constrained hosts:
  - `CodBoot.js`
  - `CodBoot.java` (Java 7 compatible)
- Parity corpus and reproducible outputs.

## Layout

- `core/core.cod` — shared core entrypoint and canonical semantics metadata consumed by both hosts.
- `js/CodBoot.js` — Node-based constrained host.
- `java/CodBoot.java` — Java 7 constrained host.
- `parity/` — corpus and expected outputs.
- `findings/` — experimentation logs and decisions.

## Run

Run the following commands from the repository root.

### JS host

```bash
node experimentations/codboot/js/CodBoot.js \
  experimentations/codboot/core/core.cod \
  experimentations/codboot/parity/programs/hello.cod

```

Bootstrap check:

```bash
node experimentations/codboot/js/CodBoot.js \
  experimentations/codboot/core/core.cod \
  experimentations/codboot/parity/programs/hello.cod \
  --bootstrap-self
```

Self-host-only check (strict mode; host fallback paths removed):

```bash
node experimentations/codboot/js/CodBoot.js \
  experimentations/codboot/core/core.cod \
  experimentations/codboot/parity/programs/hello.cod \
  --self-host-only
```

### Java 7 host

```bash
JAVA_OUT="$(mktemp -d)"
javac -source 7 -target 7 -d "$JAVA_OUT" \
  experimentations/codboot/java/CodBoot.java
java -cp "$JAVA_OUT" CodBoot \
  experimentations/codboot/core/core.cod \
  experimentations/codboot/parity/programs/hello.cod

```

Bootstrap check:

```bash
java -cp "$JAVA_OUT" CodBoot \
  experimentations/codboot/core/core.cod \
  experimentations/codboot/parity/programs/hello.cod \
  --bootstrap-self
```

Self-host-only check (strict mode; host fallback paths removed):

```bash
java -cp "$JAVA_OUT" CodBoot \
  experimentations/codboot/core/core.cod \
  experimentations/codboot/parity/programs/hello.cod \
  --self-host-only
```

## Parity corpus

- Programs:
  - `parity/programs/hello.cod`
  - `parity/programs/empty.cod`
  - `parity/programs/level2.cod`
  - `parity/programs/level3.cod`
  - `parity/programs/level2_edge.cod`
  - `parity/programs/level3_edge.cod`
- Expected output templates:
  - `parity/expected/hello.out`
  - `parity/expected/empty.out`
  - `parity/expected/level2.out`
  - `parity/expected/level3.out`
  - `parity/expected/level2_edge.out`
  - `parity/expected/level3_edge.out`

`<PROGRAM_PATH>` in expected files is replaced at runtime with the absolute executed program path.
`<INPUT_LINE>` in `level3.out` is replaced with the provided stdin line.

## Findings

- `findings/experimentation-log.md`
- `findings/minimal-dependency-analysis.md`

## Differential parity check

Run a full JS-vs-Java parity comparison across all parity `.cod` programs (strict self-host-only mode + bootstrap self-check):

```bash
experimentations/codboot/parity/compare_hosts.sh
```

## Full functionality validation (100% checklist-oriented)

Run comprehensive validation beyond the baseline parity corpus:

```bash
experimentations/codboot/parity/full_validation.sh
```

This validates:
- strict self-host-only parity for existing parity corpus
- negative/error-path parity (`parity/negative/*.cod`)
- generated mixed-behavior coverage
- differential sweep across all repository `.cod` files
- Java repeat-run determinism/consistency checks
- bootstrap self-check parity checks

Capability tracking checklist:
- `experimentations/codboot/parity/capability-checklist.txt`

## Contract

Runtime behavior:
- Hosts execute only constrained boundary operations (I/O/process/platform APIs plus staged primitives).
- Hosts do not depend on repository production runtime Java/JS files for execution semantics.
- Shared lexer/parser/evaluator semantic definitions and diagnostics are loaded from `core.cod` in both hosts.
- Full-language `.cod` execution runs through `cod.runner.CommandRunner` bridge in both JS and Java hosts (no legacy host-side `out(...)`/`host ...` fallback execution path).

- Host exposes staged dependencies:
  - Level 1:
    - `read-file(path)`
    - `print(text)`
    - `exit(code)`
  - Level 2:
    - `add`, `subtract`, `multiply`, `divide`
    - `less-than`, `greater-than`, `equal`
    - `string-append`
  - Level 3:
    - `write-file(path, text)`
    - `input()`
    - `now()`
    - `random()`
    - `system(command)`
- `core.cod` drives behavior and produces output as text.
- `core.cod` is the canonical source for semantic forms/diagnostics used by both JS and Java hosts.
- Hosts remain experimental bootstrap executors while sharing the same CommandRunner-backed full-language execution path.
