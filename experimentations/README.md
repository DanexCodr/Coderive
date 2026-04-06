# Experimentations

This directory contains isolated, non-production experiments for Coderive.

## Included workspaces

- `codboot/` — CodBoot self-hosting experimentation workspace (JS + Java 7 hosts + shared `core.cod` with core-owned language semantics metadata).

## Current self-hosting status (important)

**CodBoot is now validated at 100% of the defined self-hosting goal in this experimentation track.**

- Current status: **100%**
- Why this is now considered complete:
  - `core.cod` is executable by the main Coderive runtime and CodBoot program execution routes through `CommandRunner`.
  - Bootstrap mode performs real self-execution (`core.cod` running `core.cod`) on both JS and Java hosts.
  - Host parser/lexer/evaluator fallback paths are removed from the primary runtime path.
  - Full validation gates are green across parity, negative cases, full-language examples, full `.cod` sweep, and repeat-run determinism.

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
- full repository `.cod` differential sweep plus full-language example parity
- Java repeat-run determinism checks
- bootstrap/self-interpretation checks under strict self-host-only execution
- `core.cod` parses and runs in the primary Coderive runtime (`CommandRunner`)

## Sustaining 100% self-hosting

Reference plan: `implementations/CodBoot-SelfHosting-Plan.md`

Ongoing requirements:
1. Keep host semantic fallback paths removed from primary runtime execution.
2. Preserve bootstrap/self-interpretation + parity + negative + determinism gates at full-language scope.

## Boundaries

Everything in `experimentations/` is intentionally separated from production runtime paths and may evolve rapidly while self-hosting work is in progress.
