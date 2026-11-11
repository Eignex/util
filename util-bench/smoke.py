#!/usr/bin/env python3
"""Exercise the installed Lab Bench process contract; timings are wiring checks only."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess


def main():
    root = Path(__file__).resolve().parent.parent
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--java", default=(str(Path(os.environ["JAVA_HOME"]) / "bin/java")
                                          if "JAVA_HOME" in os.environ else shutil.which("java")))
    parser.add_argument("--output", type=Path, required=True, help="New directory for retained captures")
    parser.add_argument("--workloads", help="Comma-separated exact workload names; default: entire catalog")
    args = parser.parse_args()
    args.output = args.output.resolve()
    args.output.mkdir(parents=True, exist_ok=False)
    command = [args.java, "-cp", str(root / "util-bench/build/install/util-bench/lib/*"),
               "com.eignex.util.bench.LabAdapterKt"]
    descriptor = subprocess.check_output(command + ["describe"], cwd=root, text=True)
    assert json.loads(descriptor)["id"] == "util-bench"
    (args.output / "descriptor.json").write_text(descriptor)
    parameters = {"smoke": "true", "sizes": "65"}
    if args.workloads:
        parameters["workloads"] = args.workloads
    request = args.output / "request.json"
    request.write_text(json.dumps({"parameters": parameters}))
    plan = subprocess.check_output(command + ["plan", str(request)], cwd=root, text=True)
    (args.output / "plan.json").write_text(plan)
    cases = json.loads(plan)["cases"]
    assert cases
    for index, case in enumerate(cases):
        # Mirror the lab's input pinning; these workloads have no external inputs.
        case["attributes"]["labInputDigest"] = hashlib.sha256(b"").hexdigest()
        case_file = args.output / f"case-{index}.json"
        case_file.write_text(json.dumps(case))
        output = args.output / f"capture-{index}"
        subprocess.run(command + ["run", str(case_file), str(output)], cwd=root, check=True)
        result = json.loads((output / "result.json").read_text())
        assert result["caseId"] == case["id"] and result["status"] == "success"
        assert result["details"]["smoke"] is True
        metrics = {metric["name"]: metric["value"] for metric in result["metrics"]}
        assert metrics["mean_ns_per_element"] == metrics["mean_ns_per_batch"] / 65
        assert all((output / artifact["path"]).is_file() for artifact in result["artifacts"])
        print(f"[{index + 1}/{len(cases)}] {case['id']}: passed", flush=True)


if __name__ == "__main__":
    main()
