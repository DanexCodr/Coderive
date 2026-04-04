#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
CORE_PATH="$ROOT_DIR/experimentations/codboot/core/core.ce"
PROGRAM_DIR="$ROOT_DIR/experimentations/codboot/parity/programs"
EXPECTED_DIR="$ROOT_DIR/experimentations/codboot/parity/expected"
JS_HOST="$ROOT_DIR/experimentations/codboot/js/CodBoot.js"
JAVA_HOST="$ROOT_DIR/experimentations/codboot/java/CodBoot.java"
JAVA_OUT="/tmp/codboot-java7"
TMP_DIR="/tmp/codboot-parity-diff"

mkdir -p "$JAVA_OUT" "$TMP_DIR"
javac -source 7 -target 7 -d "$JAVA_OUT" "$JAVA_HOST"

normalize_expected() {
  local expected_file="$1"
  local program_path="$2"
  local input_line="$3"
  sed -e "s|<PROGRAM_PATH>|$program_path|g" \
      -e "s|<INPUT_LINE>|$input_line|g" \
      "$expected_file"
}

run_one() {
  local program_path="$1"
  local expected_file="$2"
  local input_line="$3"
  local name
  name="$(basename "$program_path" .cod)"
  local js_out="$TMP_DIR/$name.js.out"
  local java_out="$TMP_DIR/$name.java.out"
  local expected_out="$TMP_DIR/$name.expected.out"

  if [[ -n "$input_line" ]]; then
    printf '%s\n' "$input_line" | node "$JS_HOST" "$CORE_PATH" "$program_path" --runtime-mode=legacy >"$js_out"
    printf '%s\n' "$input_line" | java -cp "$JAVA_OUT" CodBoot "$CORE_PATH" "$program_path" --runtime-mode=legacy >"$java_out"
  else
    node "$JS_HOST" "$CORE_PATH" "$program_path" --runtime-mode=legacy >"$js_out"
    java -cp "$JAVA_OUT" CodBoot "$CORE_PATH" "$program_path" --runtime-mode=legacy >"$java_out"
  fi

  if ! diff -u "$js_out" "$java_out"; then
    echo "Host parity mismatch for $program_path" >&2
    return 1
  fi

  if [[ -f "$expected_file" ]]; then
    normalize_expected "$expected_file" "$program_path" "$input_line" >"$expected_out"
    if ! diff -u "$expected_out" "$js_out"; then
      echo "Expected output mismatch for $program_path" >&2
      return 1
    fi
  fi
}

for program_path in "$PROGRAM_DIR"/*.cod; do
  expected_file="$EXPECTED_DIR/$(basename "${program_path%.cod}").out"
  input_line=""
  case "$(basename "$program_path")" in
    level3.cod)
      input_line="diff-input"
      ;;
  esac
  run_one "$program_path" "$expected_file" "$input_line"
done

echo "CodBoot JS/Java parity comparison passed."
