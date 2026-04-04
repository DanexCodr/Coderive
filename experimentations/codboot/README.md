# CodBoot Experimentation (JS + Java 7)

This experiment follows `implementations/CodBoot-SelfHosting-Plan.md` and keeps all changes under `experimentations/`.

## Goals

- Shared `core.ce` for runtime behavior.
- Minimal host dependencies (`read-file`, `print`, `exit`) by default.
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

### JS host

```bash
node /home/runner/work/Coderive/Coderive/experimentations/codboot/js/CodBoot.js \
  /home/runner/work/Coderive/Coderive/experimentations/codboot/core/core.ce \
  /home/runner/work/Coderive/Coderive/experimentations/codboot/parity/programs/hello.cod
```

### Java 7 host

```bash
mkdir -p /tmp/codboot-java7
javac -source 7 -target 7 -d /tmp/codboot-java7 \
  /home/runner/work/Coderive/Coderive/experimentations/codboot/java/CodBoot.java
java -cp /tmp/codboot-java7 CodBoot \
  /home/runner/work/Coderive/Coderive/experimentations/codboot/core/core.ce \
  /home/runner/work/Coderive/Coderive/experimentations/codboot/parity/programs/hello.cod
```

## Contract

- Host exposes only:
  - `read-file(path)`
  - `print(text)`
  - `exit(code)`
- `core.ce` drives behavior and produces output as text.
- Hosts do not implement language semantics beyond transport/bootstrap.
