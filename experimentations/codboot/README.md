# CodBoot Experimentation (JS + Java 7)

This experiment follows `implementations/CodBoot-SelfHosting-Plan.md` and keeps all changes under `experimentations/`.

## Goals

- Shared `core.ce` for runtime behavior.
- Minimal host dependencies with staged support:
  - Level 1: `read-file`, `print`, `exit`
  - Level 2: arithmetic/comparison/string helpers
  - Level 3: optional environment helpers
- Two constrained hosts:
  - `CodBoot.js`
  - `CodBoot.java` (Java 7 compatible)
- Parity corpus and reproducible outputs.

## Layout

- `core/core.ce` — shared core entrypoint and experimental behavior.
- `js/CodBoot.js` — Node-based constrained host.
- `java/CodBoot.java` — Java 7 constrained host.
- `parity/` — corpus and expected outputs.
- `findings/` — experimentation logs and decisions.

## Run

Run the following commands from the repository root.

### JS host

```bash
node experimentations/codboot/js/CodBoot.js \
  experimentations/codboot/core/core.ce \
  experimentations/codboot/parity/programs/hello.cod

```

Bootstrap check:

```bash
node experimentations/codboot/js/CodBoot.js \
  experimentations/codboot/core/core.ce \
  experimentations/codboot/parity/programs/hello.cod \
  --bootstrap-self
```

Self-host-only check (strict mode; host fallback paths removed):

```bash
node experimentations/codboot/js/CodBoot.js \
  experimentations/codboot/core/core.ce \
  experimentations/codboot/parity/programs/hello.cod \
  --self-host-only
```

### Java 7 host

```bash
mkdir -p /tmp/codboot-java7
javac -source 7 -target 7 -d /tmp/codboot-java7 \
  experimentations/codboot/java/CodBoot.java
java -cp /tmp/codboot-java7 CodBoot \
  experimentations/codboot/core/core.ce \
  experimentations/codboot/parity/programs/hello.cod

```

Bootstrap check:

```bash
java -cp /tmp/codboot-java7 CodBoot \
  experimentations/codboot/core/core.ce \
  experimentations/codboot/parity/programs/hello.cod \
  --bootstrap-self
```

Self-host-only check (strict mode; host fallback paths removed):

```bash
java -cp /tmp/codboot-java7 CodBoot \
  experimentations/codboot/core/core.ce \
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

Run a full JS-vs-Java parity comparison across all parity `.cod` programs:

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

Capability tracking checklist:
- `experimentations/codboot/parity/capability-checklist.txt`

## Contract

Runtime behavior:
- Hosts run a built-in self-contained lexer/parser/evaluator implementation.
- Hosts do not depend on repository runtime Java/JS files for execution semantics.

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
- `core.ce` drives behavior and produces output as text.
- Hosts currently include pre-release language semantics for parity execution.
