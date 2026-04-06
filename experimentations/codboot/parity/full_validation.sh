#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
CORE_PATH="$ROOT_DIR/experimentations/codboot/core/core.ce"
PARITY_DIR="$ROOT_DIR/experimentations/codboot/parity"
PROGRAM_DIR="$PARITY_DIR/programs"
EXPECTED_DIR="$PARITY_DIR/expected"
EXAMPLES_DIR="$ROOT_DIR/examples"
NEGATIVE_DIR="$PARITY_DIR/negative"
JS_HOST="$ROOT_DIR/experimentations/codboot/js/CodBoot.js"
JAVA_HOST="$ROOT_DIR/experimentations/codboot/java/CodBoot.java"
JAVA_OUT="${CODBOOT_JAVA_OUT_DIR:-$(mktemp -d)}"
TMP_DIR="${CODBOOT_TMP_DIR:-$(mktemp -d)}"
GENERATED_DIR="$TMP_DIR/generated"
GENERATED_IO_PATH="$TMP_DIR/codboot-generated.txt"

mkdir -p "$JAVA_OUT" "$TMP_DIR" "$GENERATED_DIR"
javac -Xlint:-options -source 7 -target 7 -d "$JAVA_OUT" "$JAVA_HOST"

normalize_expected() {
  local expected_file="$1"
  local program_path="$2"
  local input_line="$3"
  sed -e "s|<PROGRAM_PATH>|$program_path|g" \
      -e "s|<INPUT_LINE>|$input_line|g" \
      "$expected_file"
}

run_js() {
  local program_path="$1"
  local input_line="$2"
  local out_file="$3"
  local run_dir="$4"
  if [[ -n "$input_line" ]]; then
    (cd "$run_dir" && printf '%s\n' "$input_line" | node "$JS_HOST" "$CORE_PATH" "$program_path" --self-host-only >"$out_file")
  else
    (cd "$run_dir" && node "$JS_HOST" "$CORE_PATH" "$program_path" --self-host-only >"$out_file" </dev/null)
  fi
}

run_java() {
  local program_path="$1"
  local input_line="$2"
  local out_file="$3"
  local run_dir="$4"
  if [[ -n "$input_line" ]]; then
    (cd "$run_dir" && printf '%s\n' "$input_line" | java -cp "$JAVA_OUT" CodBoot "$CORE_PATH" "$program_path" --self-host-only >"$out_file")
  else
    (cd "$run_dir" && java -cp "$JAVA_OUT" CodBoot "$CORE_PATH" "$program_path" --self-host-only >"$out_file" </dev/null)
  fi
}

canonicalize_output_for_diff() {
  local in_file="$1"
  local out_file="$2"
  local lazy_creation_suffix=' ms  (O(1) — no elements generated yet)'
  local lazy_pattern_suffix=' ms  (formula, not 1Qi iterations)'
  local decimal_number='[-]?[0-9]+([.][0-9]+)?'
  sed -E \
    -e "s/(Done in )${decimal_number}(${lazy_creation_suffix//\//\\/})/\\1<TIME_MS>\\2/" \
    -e "s/(Pattern recorded in )${decimal_number}(${lazy_pattern_suffix//\//\\/})/\\1<TIME_MS>\\2/" \
    -e "s/${decimal_number} ms/<TIME_MS> ms/g" \
    "$in_file" \
    | awk '
        { lines[NR] = $0 }
        END {
          n = NR
          while (n > 0 && lines[n] == "") n--
          for (i = 1; i <= n; i++) print lines[i]
        }
      ' >"$out_file"
}

run_parity_case() {
  local label="$1"
  local program_path="$2"
  local input_line="$3"
  local expected_file="${4:-}"
  local require_nonzero="${5:-false}"

  local safe_label
  safe_label="$(printf '%s' "$label" | sed 's/[^A-Za-z0-9._-]/_/g')"
  local js_out="$TMP_DIR/$safe_label.js.out"
  local java_out="$TMP_DIR/$safe_label.java.out"
  local js_cmp="$TMP_DIR/$safe_label.js.cmp.out"
  local java_cmp="$TMP_DIR/$safe_label.java.cmp.out"
  local expected_out="$TMP_DIR/$safe_label.expected.out"
  local run_dir="$TMP_DIR/run-$safe_label"
  mkdir -p "$run_dir"

  set +e
  run_js "$program_path" "$input_line" "$js_out" "$run_dir"
  local js_code=$?
  run_java "$program_path" "$input_line" "$java_out" "$run_dir"
  local java_code=$?
  set -e

  if [[ "$js_code" -ne "$java_code" ]]; then
    echo "Exit code mismatch for $label (js=$js_code java=$java_code)" >&2
    return 1
  fi

  canonicalize_output_for_diff "$js_out" "$js_cmp"
  canonicalize_output_for_diff "$java_out" "$java_cmp"

  if ! diff -u "$js_cmp" "$java_cmp"; then
    echo "Output mismatch for $label" >&2
    return 1
  fi

  if [[ "$require_nonzero" == "true" && "$js_code" -eq 0 ]]; then
    echo "Expected non-zero exit for $label but got 0" >&2
    return 1
  fi

  if [[ -n "$expected_file" ]]; then
    normalize_expected "$expected_file" "$program_path" "$input_line" >"$expected_out"
    if ! diff -u "$expected_out" "$js_cmp"; then
      echo "Expected output mismatch for $label" >&2
      return 1
    fi
  fi
}

run_full_language_example_case() {
  local label="$1"
  local program_path="$2"
  local input_line="$3"
  local safe_label
  safe_label="$(printf '%s' "$label" | sed 's/[^A-Za-z0-9._-]/_/g')"
  local js_out="$TMP_DIR/$safe_label.js.out"
  local java_out="$TMP_DIR/$safe_label.java.out"
  local js_cmp="$TMP_DIR/$safe_label.js.cmp.out"
  local java_cmp="$TMP_DIR/$safe_label.java.cmp.out"
  local run_dir="$TMP_DIR/run-$safe_label"
  mkdir -p "$run_dir"
  set +e
  run_js "$program_path" "$input_line" "$js_out" "$run_dir"
  local js_code=$?
  run_java "$program_path" "$input_line" "$java_out" "$run_dir"
  local java_code=$?
  set -e
  if [[ "$js_code" -ne "$java_code" ]]; then
    echo "Exit code mismatch for $label (js=$js_code java=$java_code)" >&2
    return 1
  fi
  canonicalize_output_for_diff "$js_out" "$js_cmp"
  canonicalize_output_for_diff "$java_out" "$java_cmp"
  if ! diff -u "$js_cmp" "$java_cmp"; then
    echo "Output mismatch for $label" >&2
    return 1
  fi
}

echo "[1/7] Baseline parity corpus (strict self-host-only)"
for program_path in "$PROGRAM_DIR"/*.cod; do
  name="$(basename "$program_path" .cod)"
  expected_file="$EXPECTED_DIR/$name.out"
  input_line=""
  case "$(basename "$program_path")" in
    level3.cod)
      input_line="level3-test-input"
      ;;
  esac
  run_parity_case "parity-$name" "$program_path" "$input_line" "$expected_file"
done

echo "[2/7] Negative corpus (error parity)"
for program_path in "$NEGATIVE_DIR"/*.cod; do
  name="$(basename "$program_path" .cod)"
  run_parity_case "negative-$name" "$program_path" "" "" "true"
done

echo "[3/7] Generated corpus (expanded behavior coverage)"
cat > "$GENERATED_DIR/generated_simple.cod" <<'EOF'
out("Generated simple start")
out(41 + 1)
out("cod" + "boot")
EOF
cat > "$GENERATED_DIR/generated_mixed.cod" <<'EOF'
out("Generated mixed start")
out(-3 * 7)
out(5 / 2)
out("left" == "right")
out(100 > 99)
out("unknown-op")
EOF
cat > "$GENERATED_DIR/generated_io.cod" <<EOF
out("Generated io start")
out("[host] write-file ok")
out("generated-ok")
out(in(text, ""))
out(0)
EOF
for program_path in "$GENERATED_DIR"/*.cod; do
  name="$(basename "$program_path" .cod)"
  input_line=""
  if [[ "$name" == "generated_io" ]]; then
    input_line="generated-input"
  fi
  run_parity_case "generated-$name" "$program_path" "$input_line"
done

echo "[4/7] Full repository .cod differential sweep"
find "$ROOT_DIR" -path "$ROOT_DIR/.git" -prune -o -name '*.cod' -print | sort >"$TMP_DIR/all-cod-files.txt"
while IFS= read -r program_path; do
  run_parity_case "full-sweep-$(basename "$program_path")" "$program_path" ""
done <"$TMP_DIR/all-cod-files.txt"

run_full_language_example_case "example-fizzbuzz" "$EXAMPLES_DIR/fizzbuzz.cod" ""
run_full_language_example_case "example-smart_loops" "$EXAMPLES_DIR/smart_loops.cod" ""
run_full_language_example_case "example-lazy_arrays" "$EXAMPLES_DIR/lazy_arrays.cod" ""
run_full_language_example_case "example-hello" "$EXAMPLES_DIR/hello.cod" "level3-test-input"

echo "[5/7] Java-only repeat-run consistency checks"
deterministic_programs=(
  "$PROGRAM_DIR/hello.cod"
  "$PROGRAM_DIR/empty.cod"
  "$PROGRAM_DIR/level2.cod"
  "$PROGRAM_DIR/level2_edge.cod"
  "$PROGRAM_DIR/level3.cod"
  "$PROGRAM_DIR/level3_edge.cod"
  "$GENERATED_DIR/generated_simple.cod"
  "$GENERATED_DIR/generated_mixed.cod"
  "$GENERATED_DIR/generated_io.cod"
)
for program_path in "${deterministic_programs[@]}"; do
  name="$(basename "$program_path" .cod)"
  input_line=""
  if [[ "$name" == "level3" ]]; then
    input_line="level3-test-input"
  fi
  if [[ "$name" == "generated_io" ]]; then
    input_line="generated-input"
  fi
  baseline_out="$TMP_DIR/java-repeat-$name.1.out"
  repeat_run_dir="$TMP_DIR/java-repeat-$name"
  mkdir -p "$repeat_run_dir"
  set +e
  run_java "$program_path" "$input_line" "$baseline_out" "$repeat_run_dir"
  baseline_code=$?
  set -e
  for i in 2 3; do
    current_out="$TMP_DIR/java-repeat-$name.$i.out"
    set +e
    run_java "$program_path" "$input_line" "$current_out" "$repeat_run_dir"
    current_code=$?
    set -e
    if [[ "$current_code" -ne "$baseline_code" ]]; then
      echo "Java repeat-run exit mismatch for $name ($baseline_code vs $current_code)" >&2
      exit 1
    fi
    if ! diff -u "$baseline_out" "$current_out"; then
      echo "Java repeat-run output mismatch for $name" >&2
      exit 1
    fi
  done
done

echo "[6/7] Bootstrap self-checks"
"$PARITY_DIR/compare_hosts.sh" --bootstrap-only

echo "[7/7] Baseline script compatibility"
"$PARITY_DIR/compare_hosts.sh"

echo "CodBoot full validation passed:"
echo "- Capability checklist: $PARITY_DIR/capability-checklist.txt"
echo "- Positive parity, negative parity, generated coverage, full .cod sweep, full-language examples, and Java repeat-run checks all green."
