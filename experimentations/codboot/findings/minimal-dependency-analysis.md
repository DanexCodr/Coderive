# Minimal Dependency Analysis (JS + Java 7)

## Requirement interpreted

The staged dependency model from `implementations/CodBoot-SelfHosting-Plan.md` is implemented across Level 1, Level 2, and Level 3 in both JS and Java 7 hosts.

## APIs used by JS host

- Node `fs.readFileSync` for `read-file`.
- Node `fs.writeFileSync` for `write-file`.
- `process.stdout.write` for `print`.
- stdin read for `input`.
- `Date.now` for `now`.
- deterministic LCG for `random`.
- `child_process.execFileSync` for `system`.
- `process.exit` for `exit`.

## APIs used by Java 7 host

- `FileInputStream` + `InputStreamReader` + `BufferedReader` for `read-file`.
- `FileOutputStream` for `write-file`.
- `System.out.println` for `print`.
- `BufferedReader(System.in)` for `input`.
- `System.currentTimeMillis` for `now`.
- `java.util.Random` seeded deterministically for `random`.
- `ProcessBuilder` for `system`.
- `System.exit` for `exit`.

## Deliberately excluded

- No host-level parser/evaluator dependency on external libraries.
- No network APIs.
- No build-system coupling required for the experiment (manual `javac -source 7 -target 7` works).

## Runtime-mode note

- In `--runtime-mode=legacy`, both hosts stay in the minimal constrained-path model.
- In Java `--runtime-mode=auto|native`, CodBoot may use reflection to invoke `cod.runner.CommandRunner` when available on classpath.
- This reflective path is an intentional migration bridge toward runtime-native execution and is not part of the minimal legacy contract.

## Why this boundary helps

- Keeps portability and parity predictable.
- Makes host contracts small and easier to freeze.
- Pushes language evolution pressure toward shared core representation (`core.cod`) instead of host-specific logic.
