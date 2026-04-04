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

# Runtime modes:
# --runtime-mode=auto (default): full runtime parser/evaluator path
# --runtime-mode=legacy: parity protocol fallback path
# --runtime-mode=native: require runtime availability, fail otherwise
```

Bootstrap check:

```bash
node experimentations/codboot/js/CodBoot.js \
  experimentations/codboot/core/core.ce \
  experimentations/codboot/parity/programs/hello.cod \
  --bootstrap-self
```

### Java 7 host

```bash
mkdir -p /tmp/codboot-java7
javac -source 7 -target 7 -d /tmp/codboot-java7 \
  experimentations/codboot/java/CodBoot.java
java -cp /tmp/codboot-java7 CodBoot \
  experimentations/codboot/core/core.ce \
  experimentations/codboot/parity/programs/hello.cod

# Runtime modes:
# --runtime-mode=auto (default): full runtime parser/evaluator path
# --runtime-mode=legacy: parity protocol fallback path
# --runtime-mode=native: require runtime availability, fail otherwise
```

Bootstrap check:

```bash
java -cp /tmp/codboot-java7 CodBoot \
  experimentations/codboot/core/core.ce \
  experimentations/codboot/parity/programs/hello.cod \
  --bootstrap-self
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

## Contract

Runtime behavior:
- Default execution is runtime-complete (`--runtime-mode=auto`).
- Legacy protocol mode is explicit (`--runtime-mode=legacy`) for parity fixtures only.
- JS host attempts Java CommandRunner bridge first, then JS runtime fallback when available.


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
- Hosts do not implement language semantics beyond transport/bootstrap.
