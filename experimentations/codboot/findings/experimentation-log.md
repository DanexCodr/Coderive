# CodBoot Experimentation Log

## Objective

Create a full CodBoot experiment aligned with `implementations/` guidance while keeping dependencies minimal in JS and Java 7, and keeping all work isolated in `experimentations/`.

## What was built

- Shared experimental core:
  - `experimentations/codboot/core/core.cod`
- Minimal hosts:
  - `experimentations/codboot/js/CodBoot.js`
  - `experimentations/codboot/java/CodBoot.java`
- Parity corpus:
  - `experimentations/codboot/parity/programs/*.cod`
  - `experimentations/codboot/parity/expected/*.out`
  - `experimentations/codboot/parity/compare_hosts.sh`

## Host dependency boundary

Implemented Level 1-3 host dependency shape:

- Level 1:
  - `read-file`
  - `print`
  - `exit`
- Level 2:
  - `add`, `subtract`, `multiply`, `divide`
  - `less-than`, `greater-than`, `equal`
  - `string-append`
- Level 3:
  - `write-file`
  - `input`
  - `now`
  - `random`
  - `system`

## Experiment behavior

- Host loads `core.cod` from disk.
- Host validates core signature (`CodBootCore::v0` marker).
- Host reads `.cod` program file.
- Host decodes simple `out("...")` statements for parity demonstration.
- Host supports `host <operation> ...` parity directives for Level 2/3 validation.
- Differential script compares JS and Java host output for every parity program.
- Host prints normalized output and exits with deterministic code.
- Optional bootstrap flag confirms self-bootstrap path (`--bootstrap-self`).

## Notes and limitations

- This is an isolated experimentation prototype, not yet wired to production runtime paths.
- `core.cod` currently defines protocol/contracts and experimental entrypoint shape; it is not yet a complete parser/evaluator implementation.
- JS and Java hosts now run a built-in self-contained lexer/parser/evaluator path with no repository runtime Java/JS dependency.

## Next experiments suggested

1. Move protocol parsing/dispatch into executable `core.cod` semantics.
2. Expand parity corpus to include additional protocol/error-path diagnostics once `core.cod` owns more semantics.
3. Keep differential runner as required gate for every parity corpus change.
4. Replace simple `out("...")` extraction with core-driven parse/eval once core execution path is available.
