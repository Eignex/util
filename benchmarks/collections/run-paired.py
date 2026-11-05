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
args = parser.parse_args()
if args.repetitions < 1:
    parser.error("--repetitions must be positive")

jars = {"baseline": args.baseline.resolve(strict=True), "candidate": args.candidate.resolve(strict=True)}
classpath = [Path(entry).resolve() for entry in args.classpath.read_text().strip().split(os.pathsep)]
if not any(entry in jars.values() for entry in classpath):
    parser.error("classpath must contain the supplied baseline or candidate JAR")
classpath = [entry for entry in classpath if entry not in jars.values()]
args.output.mkdir(parents=True, exist_ok=True)
cases = [
    "intGrowth", "intInsert", "intLookup", "intRemoveRestore", "intSetContains",
    "longGrowth", "longInsert", "longLookup", "longRemoveRestore", "longSetContains",
    "objectRemoveRestore",
]
order = []
for repetition in range(args.repetitions):
    results = {"baseline": [], "candidate": []}
    for index, case in enumerate(cases):
        arms = ["baseline", "candidate"] if (index + repetition) % 2 == 0 else ["candidate", "baseline"]
        for arm in arms:
            result = args.output / f"case-{repetition + 1}-{case}-{arm}.json"
            fork_classpath = os.pathsep.join(str(entry) for entry in [*classpath, jars[arm]])
            command = [
                args.java, "-cp", fork_classpath, "org.openjdk.jmh.Main", f"CollectionsBench.{case}$",
                "-f", "1", "-wi", "3", "-w", "300ms", "-i", "5", "-r", "300ms",
                "-rf", "json", "-rff", str(result),
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
