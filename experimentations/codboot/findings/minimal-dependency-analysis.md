# Minimal Dependency Analysis (JS + Java 7)

## Requirement interpreted

“Try having a minimal dependency in js and java 7” was implemented as strict Level 1 host APIs from `implementations/CodBoot-SelfHosting-Plan.md`.

## APIs used by JS host

- Node `fs.readFileSync` for `read-file`.
- `process.stdout.write` for `print`.
- `process.exit` for `exit`.

## APIs used by Java 7 host

- `FileInputStream` + `InputStreamReader` + `BufferedReader` for `read-file`.
- `System.out.println` for `print`.
- `System.exit` for `exit`.

## Deliberately excluded

- No host-provided arithmetic operators.
- No host-level parser/evaluator dependency on external libraries.
- No dynamic class loading, reflection, network, subprocess, or random/time APIs.
- No build-system coupling required for the experiment (manual `javac -source 7 -target 7` works).

## Why this boundary helps

- Keeps portability and parity predictable.
- Makes host contracts small and easier to freeze.
- Pushes language evolution pressure toward shared core representation (`core.ce`) instead of host-specific logic.
