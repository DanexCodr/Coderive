# CodBoot Experimentation Log

## Objective

Create a full CodBoot experiment aligned with `implementations/` guidance while keeping dependencies minimal in JS and Java 7, and keeping all work isolated in `experimentations/`.

## What was built

- Shared experimental core:
  - `experimentations/codboot/core/core.ce`
- Minimal hosts:
  - `experimentations/codboot/js/CodBoot.js`
  - `experimentations/codboot/java/CodBoot.java`
- Parity corpus:
  - `experimentations/codboot/parity/programs/*.cod`
  - `experimentations/codboot/parity/expected/*.out`

## Host dependency boundary

Implemented Level 1 shape only:

- `read-file`
- `print`
- `exit`

No arithmetic, random, time, subprocess, or network host APIs were introduced.

## Experiment behavior

- Host loads `core.ce` from disk.
- Host validates core signature (`CodBootCore::v0` marker).
- Host reads `.cod` program file.
- Host decodes simple `out("...")` statements for parity demonstration.
- Host prints normalized output and exits with deterministic code.
- Optional bootstrap flag confirms self-bootstrap path (`--bootstrap-self`).

## Notes and limitations

- This is an isolated experimentation prototype, not yet wired to production runtime paths.
- `core.ce` currently defines protocol/contracts and experimental entrypoint shape; it is not yet a complete parser/evaluator implementation.
- JS and Java hosts are intentionally constrained and symmetric for boundary testing.

## Next experiments suggested

1. Move protocol parsing/dispatch into executable `core.ce` semantics.
2. Expand parity corpus to include negative/error behavior and structured diagnostics.
3. Add differential runner script in `experimentations/` to auto-compare JS/Java outputs.
4. Replace simple `out("...")` extraction with core-driven parse/eval once core execution path is available.
