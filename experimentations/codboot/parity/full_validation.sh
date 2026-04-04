#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
CORE_PATH="$ROOT_DIR/experimentations/codboot/core/core.ce"
PARITY_DIR="$ROOT_DIR/experimentations/codboot/parity"
PROGRAM_DIR="$PARITY_DIR/programs"
EXPECTED_DIR="$PARITY_DIR/expected"
NEGATIVE_DIR="$PARITY_DIR/negative"
JS_HOST="$ROOT_DIR/experimentations/codboot/js/CodBoot.js"
JAVA_HOST="$ROOT_DIR/experimentations/codboot/java/CodBoot.java"
JAVA_OUT="/tmp/codboot-java7-full"
TMP_DIR="/tmp/codboot-full-validation"
GENERATED_DIR="$TMP_DIR/generated"

mkdir -p "$JAVA_OUT" "$TMP_DIR" "$GENERATED_DIR"
javac -source 7 -target 7 -d "$JAVA_OUT" "$JAVA_HOST"

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
  set +e
  if [[ -n "$input_line" ]]; then
    printf '%s\n' "$input_line" | node "$JS_HOST" "$CORE_PATH" "$program_path" --self-host-only >"$out_file"
  else
    node "$JS_HOST" "$CORE_PATH" "$program_path" --self-host-only >"$out_file"
  fi
  local code=$?
  set -e
  return "$code"
}

run_java() {
  local program_path="$1"
  local input_line="$2"
  local out_file="$3"
  set +e
  if [[ -n "$input_line" ]]; then
    printf '%s\n' "$input_line" | java -cp "$JAVA_OUT" CodBoot "$CORE_PATH" "$program_path" --self-host-only >"$out_file"
  else
    java -cp "$JAVA_OUT" CodBoot "$CORE_PATH" "$program_path" --self-host-only >"$out_file"
  fi
  local code=$?
  set -e
  return "$code"
}

run_parity_case() {
  local label="$1"
  local program_path="$2"
  local input_line="$3"
  local expected_file="${4:-}"
  local require_nonzero="${5:-false}"

  local safe_label
  safe_label="$(echo "$label" | tr '/ ' '__')"
  local js_out="$TMP_DIR/$safe_label.js.out"
  local java_out="$TMP_DIR/$safe_label.java.out"
  local expected_out="$TMP_DIR/$safe_label.expected.out"

  run_js "$program_path" "$input_line" "$js_out"
  local js_code=$?
  run_java "$program_path" "$input_line" "$java_out"
  local java_code=$?

  if [[ "$js_code" -ne "$java_code" ]]; then
    echo "Exit code mismatch for $label (js=$js_code java=$java_code)" >&2
    return 1
  fi

  if ! diff -u "$js_out" "$java_out"; then
    echo "Output mismatch for $label" >&2
    return 1
  fi

  if [[ "$require_nonzero" == "true" && "$js_code" -eq 0 ]]; then
    echo "Expected non-zero exit for $label but got 0" >&2
    return 1
  fi

  if [[ -n "$expected_file" ]]; then
    normalize_expected "$expected_file" "$program_path" "$input_line" >"$expected_out"
    if ! diff -u "$expected_out" "$js_out"; then
      echo "Expected output mismatch for $label" >&2
      return 1
    fi
  fi
}

echo "[1/6] Baseline parity corpus (strict self-host-only)"
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

echo "[2/6] Negative corpus (error parity)"
for program_path in "$NEGATIVE_DIR"/*.cod; do
  name="$(basename "$program_path" .cod)"
  run_parity_case "negative-$name" "$program_path" "" "" "true"
done

echo "[3/6] Generated corpus (expanded behavior coverage)"
cat > "$GENERATED_DIR/generated_simple.cod" <<'EOF'
out("Generated simple start")
host add 41 1
host string-append cod boot
EOF
cat > "$GENERATED_DIR/generated_mixed.cod" <<'EOF'
out("Generated mixed start")
host multiply -3 7
host divide 5 0
host equal left right
host greater-than 100 99
host unknown-op
EOF
cat > "$GENERATED_DIR/generated_io.cod" <<'EOF'
out("Generated io start")
host write-file /tmp/codboot-generated.txt generated-ok
host read-file /tmp/codboot-generated.txt
host input
host system true
EOF
for program_path in "$GENERATED_DIR"/*.cod; do
  name="$(basename "$program_path" .cod)"
  input_line=""
  if [[ "$name" == "generated_io" ]]; then
    input_line="generated-input"
  fi
  run_parity_case "generated-$name" "$program_path" "$input_line"
done

echo "[4/6] Full repository .cod differential sweep"
find "$ROOT_DIR" -path "$ROOT_DIR/.git" -prune -o -name '*.cod' -print | sort >"$TMP_DIR/all-cod-files.txt"
while IFS= read -r program_path; do
  run_parity_case "full-sweep-$(basename "$program_path")" "$program_path" ""
done <"$TMP_DIR/all-cod-files.txt"

echo "[5/6] Java-only repeat-run consistency checks"
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
  run_java "$program_path" "$input_line" "$baseline_out"
  baseline_code=$?
  for i in 2 3; do
    current_out="$TMP_DIR/java-repeat-$name.$i.out"
    run_java "$program_path" "$input_line" "$current_out"
    current_code=$?
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

echo "[6/6] Baseline script compatibility"
"$PARITY_DIR/compare_hosts.sh"

echo "CodBoot full validation passed:"
echo "- Capability checklist: $PARITY_DIR/capability-checklist.txt"
echo "- Positive parity, negative parity, generated coverage, full .cod sweep, and Java repeat-run checks all green."
