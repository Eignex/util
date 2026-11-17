#!/usr/bin/env bash
# Exercise the installed adapter; timings are wiring checks only. Requires jq.
set -euo pipefail
root=$(cd "$(dirname "$0")/.." && pwd)
java_bin=${JAVA_HOME:+$JAVA_HOME/bin/}java
output=
workloads=
while (($#)); do
  case "$1" in
    --java) java_bin=$2; shift 2 ;;
    --output) output=$2; shift 2 ;;
    --workloads) workloads=$2; shift 2 ;;
    *) echo "Unknown option: $1" >&2; exit 2 ;;
  esac
done
[[ -n "$output" ]] || { echo '--output is required' >&2; exit 2; }
mkdir -p "$(dirname "$output")"
mkdir "$output"
output=$(cd "$output" && pwd)
command=("$java_bin" -cp "$root/util-bench/build/install/util-bench/lib/*" com.eignex.util.bench.LabAdapterKt)
"${command[@]}" describe > "$output/descriptor.json"
jq -e '.id == "util-bench"' "$output/descriptor.json" >/dev/null
jq -n --arg workloads "$workloads" \
  '{parameters: ({smoke: "true", sizes: "65"} + if $workloads == "" then {} else {workloads: $workloads} end)}' \
  > "$output/request.json"
"${command[@]}" plan "$output/request.json" > "$output/plan.json"
count=$(jq -er '.cases | length | select(. > 0)' "$output/plan.json")
for ((index=0; index<count; index++)); do
  # SHA-256 of empty input: these workloads have no external inputs.
  jq --argjson index "$index" \
    '.cases[$index] | .attributes.labInputDigest = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"' \
    "$output/plan.json" > "$output/case-$index.json"
  capture="$output/capture-$index"
  "${command[@]}" run "$output/case-$index.json" "$capture"
  jq -e --slurpfile case "$output/case-$index.json" '
    .caseId == $case[0].id and .status == "success" and .details.smoke == true and
    ([.metrics[] | {key: .name, value: .value}] | from_entries |
      .mean_ns_per_element == .mean_ns_per_batch / 65)
  ' "$capture/result.json" >/dev/null
  while IFS= read -r artifact; do
    [[ -f "$capture/$artifact" ]]
  done < <(jq -r '.artifacts[].path' "$capture/result.json")
  echo "[$((index+1))/$count] $(jq -r '.id' "$output/case-$index.json"): passed"
done
