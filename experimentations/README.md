# Experimentations

This directory contains isolated, non-production experiments for Coderive.

## Included workspaces

- `codboot/` — CodBoot self-hosting experimentation workspace (JS + Java 7 hosts + shared `core.ce` protocol artifacts).

## Current self-hosting status (important)

**CodBoot is not fully self-hosting yet.**

- Current status: **~65% of final self-hosting goal**
- Why not 100% yet:
  - `core.ce` is still a protocol-level experimental core and does **not** yet implement the full language pipeline (full lexer/parser/evaluator semantics) that production Java Coderive supports.
  - JS/Java hosts still carry substantial language/runtime logic; they are not yet reduced to minimal boundary adapters for all normal execution paths.
  - The anti-criteria in `implementations/CodBoot-SelfHosting-Plan.md` still apply (language semantics remain host-heavy instead of fully core-owned).

In short: parity and stability checks are strong for the current experimental scope, but that scope is still narrower than full production runtime completeness.

## How to validate current experiment quality

Run from repository root:

```bash
./gradlew --no-daemon sourceJar
experimentations/codboot/parity/compare_hosts.sh
experimentations/codboot/parity/full_validation.sh
```

What this confirms today:
- JS↔Java parity on known parity corpus
- negative/error-path parity
- generated mixed-behavior checks
- full repository `.cod` differential sweep
- Java repeat-run determinism checks

## What remains to reach 100% self-hosting

Reference plan: `implementations/CodBoot-SelfHosting-Plan.md`

Primary remaining milestones:
1. Move full language implementation (lexer/parser/evaluator + semantic forms) into shared `core.ce`.
2. Keep host responsibilities to boundary-only concerns (I/O/process/platform APIs).
3. Ensure identical behavior across JS and Java using the same core-owned semantics.
4. Remove/disable host semantic fallback paths from primary runtime execution.
5. Prove bootstrap/self-interpretation and run complete parity + negative + determinism gates at full language scope.

## Boundaries

Everything in `experimentations/` is intentionally separated from production runtime paths and may evolve rapidly while self-hosting work is in progress.
