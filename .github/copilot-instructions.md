# Copilot Repository Instructions

## IR / Bytecode Versioning Rule (Pre-version Phase)

- Keep `cod.ir.IRCodec.VERSION` at `1` during pre-version development.
- Do **not** bump IR/bytecode format versions for internal format changes while we are not maintaining legacy compatibility yet.
- Only bump IR/bytecode version when maintainers explicitly decide to introduce compatibility/migration handling.
