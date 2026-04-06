# Experimentations

This directory contains isolated, non-production experiments for Coderive.

## Included workspaces

- `codboot/` — CodBoot self-hosting experimentation workspace (JS + Java 7 hosts + shared `core.ce` with core-owned language semantics metadata).

## Current self-hosting status (important)

**CodBoot remains experimental and not yet fully production-equivalent self-hosting.**

- Current status: **~80% of final self-hosting goal**
- Why not 100% yet:
  - `core.ce` now owns shared semantics definitions (forms, host command mapping, diagnostics) and is executable by the main Coderive runtime, and CodBoot full-language execution now routes through the `CommandRunner` bridge.
  - Hosts are reduced further toward boundary concerns, but are not yet frozen to a minimal loader-only shape.
  - Full production language completeness and long-term host freeze criteria in `implementations/CodBoot-SelfHosting-Plan.md` are not yet fully satisfied.

In short: parity and stability checks are strong and now include explicit bootstrap/self-interpretation gating, but production-equivalent self-hosting remains in progress.

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
- `core.ce` parses and runs in the primary Coderive runtime (`CommandRunner`)

## What remains to reach 100% self-hosting

Reference plan: `implementations/CodBoot-SelfHosting-Plan.md`

Primary remaining milestones:
1. Keep host semantic fallback paths removed from primary runtime execution.
2. Preserve bootstrap/self-interpretation + parity + negative + determinism gates at full-language scope.

## Boundaries

Everything in `experimentations/` is intentionally separated from production runtime paths and may evolve rapidly while self-hosting work is in progress.
