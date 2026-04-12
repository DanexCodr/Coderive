# Issue Report: NaturalArray Lazy Registry + PTAC Coverage

## Context
Task requested:
1. Move array literal registry usage from eager materialization to NaturalArray-backed behavior and validate size-independent complexity.
2. Clean unused imports/methods.
3. Improve compiler/PTAC execution completeness to reduce interpreter fallback.
4. Submit an issue report (file in repo root when direct issue submission is unavailable).

## What was changed

### 1) Literal registry array operations now use NaturalArray lazily
- File: `src/main/java/cod/interpreter/registry/LiteralRegistry.java`
- `map` and `filter` on `NaturalArray` no longer force `toList()` eager materialization.
- Added lazy `AbstractList` views for NaturalArray:
  - `LazyNaturalArrayMapView`
  - `LazyNaturalArrayFilterView`
- `reduce` on `NaturalArray` now streams by index (`size()` + `get(i)`) instead of first materializing a full list.
- Kept list behavior unchanged for non-NaturalArray inputs.

### 2) PTAC executor native coverage increased
- File: `src/main/java/cod/ptac/Executor.java`
- Added instruction pointer execution model with label indexing:
  - Native `NOP`, `BRANCH`, `BRANCH_IF`
- Added native lazy array op handling:
  - `LAZY_GET`, `LAZY_SET` (list-backed), `LAZY_SIZE`, `LAZY_COMMIT`
- Added native pattern execution:
  - `MAP`, `FILTER`, `REDUCE`, `FILTER_MAP`
- Kept sensitive recursive opcodes (`SELF`, `TAIL_CALL`) on fallback path to preserve parity correctness.
- Added safe fallback for non-numeric range bounds in native range execution.

### 3) Validation runner added for complexity comparison
- New file: `src/main/java/cod/runner/ArrayLiteralRegistryComplexityRunner.java`
- Benchmarks prior eager strategy vs current NaturalArray path across sizes:
  - `1000`, `10000`, `100000`
- Validates that map/filter setup growth is smaller than eager growth.

### 4) Cleanup
- Removed unused import in new runner.
- Removed unused parameter from executor instruction dispatch.

## Validation performed

Commands run:
- `./gradlew --no-daemon sourceJar`
- `javac -d /tmp/coderive-compile $(find src/main/java -name '*.java')`
- `java -cp /tmp/coderive-compile cod.runner.ArrayLiteralRegistryComplexityRunner`
- `java -cp /tmp/coderive-compile cod.runner.CodPTACParityRunner /home/runner/work/Coderive/Coderive/src/main/cod/demo/src/main/test`

Results:
- Build + Java compile: **pass**
- Complexity runner: **pass**
  - Sample output indicated map/filter setup remained effectively size-independent versus eager growth.
- PTAC parity suite: **47 passed, 0 failed**

## Findings / remaining issues

1. `reduce` semantic result is still computed eagerly (returns final scalar), so total time cannot be strictly O(1) for general reductions.
   - Improvement made: removed eager full-list materialization for NaturalArray reduce (space reduced to streaming behavior).
2. PTAC still falls back for advanced opcodes not yet natively implemented:
   - `SCAN`, `ZIP`, `WHERE`, `FILTER_MAP_REDUCE`, `LAZY_SLICE`,
   - slot/closure/tail-recursion/formula/storage opcodes.
3. Non-numeric range bounds are intentionally routed to fallback to avoid incorrect native execution.

## Suggested follow-up issues

1. Add native executor support for `SCAN`, `ZIP`, `WHERE`, and `FILTER_MAP_REDUCE`.
2. Implement native slot and closure stack model (`SLOT_*`, `CLOSURE`, `ANCESTOR`) for larger fallback reduction.
3. Design typed lazy reduction objects (or algebraic reductions) if strict O(1) reduce setup + deferred evaluation is required by language semantics.

