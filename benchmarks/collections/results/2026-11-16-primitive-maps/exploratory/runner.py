"""Run adjacent, alternating baseline/candidate JMH cases on one machine."""

import argparse
import json
import math
import os
from pathlib import Path
import subprocess
import time


parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--baseline", type=Path, required=True)
parser.add_argument("--candidate", type=Path, required=True)
parser.add_argument("--classpath", type=Path, required=True)
parser.add_argument("--java", required=True)
parser.add_argument("--output", type=Path, required=True)
parser.add_argument("--repetitions", type=int, default=2)
parser.add_argument("--suite", choices=["collections", "deque-disjoint", "primitive-maps"], default="collections")
parser.add_argument("--sizes", help="Comma-separated sizes for deque-disjoint or primitive-maps (default: 1024,65536)")
args = parser.parse_args()
try:
    sizes = [int(size) for size in (args.sizes or "1024,65536").split(",")]
except ValueError:
    parser.error("--sizes must be comma-separated integers")
if not sizes or len(set(sizes)) != len(sizes) or any(size < 1 or size > 65536 for size in sizes):
    parser.error("--sizes must be unique integers in 1..65536")
if args.sizes is not None and args.suite == "collections":
    parser.error("--sizes is only supported for focused suites")
if args.repetitions < 1:
    parser.error("--repetitions must be positive")

jars = {"baseline": args.baseline.resolve(strict=True), "candidate": args.candidate.resolve(strict=True)}
classpath = [Path(entry).resolve() for entry in args.classpath.read_text().strip().split(os.pathsep)]
if not any(entry in jars.values() for entry in classpath):
    parser.error("classpath must contain the supplied baseline or candidate JAR")
classpath = [entry for entry in classpath if entry not in jars.values()]
args.output.mkdir(parents=True, exist_ok=True)
collection_cases = [
    "intGrowth", "intInsert", "intLookup", "intRemoveRestore", "intSetContains",
    "longGrowth", "longInsert", "longLookup", "longRemoveRestore", "longSetContains",
    "objectRemoveRestore",
]
cases = [(case, f"CollectionsBench.{case}$", []) for case in collection_cases]
extra_controls = []
if args.suite == "deque-disjoint":
    cases = []
    for size in sizes:
        for group_size in (1, 32, 65536):
            cases.append((f"groups-{size}-{group_size}", "DequeDisjointBench.groups$",
                          ["-p", f"size={size}", "-p", f"groupSize={group_size}"]))
        for position in ("front", "middle", "back"):
            cases.append((f"remove-{size}-{position}", "DequeDisjointBench.removeAt$",
                          ["-p", f"size={size}", "-p", f"position={position}"]))
        cases.append((f"grow-{size}", "DequeDisjointBench.grow$", ["-p", f"size={size}"]))
    extra_controls = ["-jvmArgs", "-Xms512m -Xmx512m -XX:+UseSerialGC", "-prof", "gc"]
if args.suite == "primitive-maps":
    workloads = [f"{kind}.build" for kind in ("int-int-dense", "int-long-dense", "int-int-sparse", "int-long-sparse")]
    workloads += [f"{kind}.remove-restore" for kind in (
        "int-int-map", "int-long-map", "int-double-map", "int-object-map",
        "long-int-map", "long-object-map", "int-set", "long-set")]
    cases = [(f"{workload}-{size}", "com.eignex.util.bench.CollectionsBenchmark.batch$",
              ["-p", f"workload={workload}", "-p", f"size={size}", "-p", "seed=42"])
             for size in sizes for workload in workloads]
    extra_controls = ["-jvmArgs", "-Xms512m -Xmx512m -XX:+UseSerialGC", "-prof", "gc"]
order = []
for repetition in range(args.repetitions):
    results = {"baseline": [], "candidate": []}
    for index, (case, benchmark, parameters) in enumerate(cases):
        arms = ["baseline", "candidate"] if (index + repetition) % 2 == 0 else ["candidate", "baseline"]
        for arm in arms:
            result = args.output / f"case-{repetition + 1}-{case}-{arm}.json"
            fork_classpath = os.pathsep.join(str(entry) for entry in [*classpath, jars[arm]])
            command = [
                args.java, "-cp", fork_classpath, "org.openjdk.jmh.Main", benchmark,
                "-f", "1", "-wi", "3", "-w", "300ms", "-i", "5", "-r", "300ms",
                "-rf", "json", "-rff", str(result),
                *parameters, *extra_controls,
            ]
            started = time.time()
            with (args.output / "jmh.log").open("a") as log:
                subprocess.run(command, stdout=log, stderr=subprocess.STDOUT, check=True)
            rows = json.loads(result.read_text())
            if len(rows) != 1 or not math.isfinite(rows[0]["primaryMetric"]["score"]):
                raise RuntimeError(f"Missing benchmark result: {case}, {arm}")
            results[arm].extend(rows)
            order.append({
                "repetition": repetition + 1, "case": case, "arm": arm,
                "startedUtc": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime(started)),
            })
        scores = {arm: round(results[arm][-1]["primaryMetric"]["score"], 3) for arm in results}
        print(repetition + 1, case, scores, flush=True)
    for arm, rows in results.items():
        (args.output / f"{arm}-{repetition + 1}.json").write_text(json.dumps(rows, indent=2) + "\n")
(args.output / "order.json").write_text(json.dumps(order, indent=2) + "\n")
