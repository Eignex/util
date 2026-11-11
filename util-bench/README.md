# util-bench

JVM benchmarks for util's primitive collections, integrated with the
[Lab Bench SDK](https://github.com/Eignex/eignex-lab/tree/main/lab-bench).
The bench module depends on the library; the library has no benchmark dependencies.
Commands below run from the repository root. Gradle needs Java 21 or newer for KBuild;
compilation, benchmark execution and JMH forks use Java 17.

## Build and smoke check

```sh
./gradlew :util-bench:check :util-bench:lintDocs :util-bench:installDist
python3 util-bench/smoke.py --java /absolute/jdk17/bin/java --output /tmp/util-smoke
```

The smoke helper exercises `describe`, `plan` and `run` through the installed distribution,
running the complete catalog at size 65 with one fork and one 10ms warmup/measurement.
The output directory must be new. It retains every frozen case, raw JMH JSON, log, controls
and SDK result. Python is only needed for this local helper; lab execution uses the JVM adapter.
Smoke timings establish integration and correctness, and are too short for performance conclusions.
Use `--workloads int-int-map.lookup-hit,bits.range` for a smaller integration check.

The SDK is pinned to `0.1.0` from public GitHub release assets. Development builds can use
`-PlabBenchRepository=file:///absolute/adapter-repository -PlabBenchVersion=VERSION`,
with the repository staged by eignex-lab's `:lab-bench:stageAdapterRepository` task.

## Workloads

The 77 workload names are `<collection>.<operation>`. The planner's case parameters contain
the exact names; `plan` with an empty request selects the whole catalog.

| Collection | Operations |
| --- | --- |
| `int-int-map`, `int-long-map`, `int-double-map`, `int-object-map`, `long-int-map`, `long-object-map` | `lookup-hit`, `lookup-miss`, `insert`, `grow`, `remove-restore` |
| `int-set`, `long-set` | `contains-hit`, `contains-miss`, `insert`, `grow`, `remove-restore` |
| `int-int-dense`, `int-int-sparse`, `int-long-dense`, `int-long-sparse` | `lookup-hit`, `lookup-miss`, `build` |
| `int-list`, `long-list` | `read`, `append`, `grow`, `remove-restore` |
| `int-deque` | `cycle`, `grow` |
| `int-swap-set` | `contains`, `remove-restore` |
| `bits` | `get`, `cardinality`, `range`, `traverse` |
| `dynamic-bits` | `get`, `cardinality`, `range`, `traverse`, `grow` |
| `indexed-heap` | `update`, `extract-restore` |
| `disjoint-set` | `union-find`, `find-compressed` |

Inputs use a seeded `SplittableRandom`. Hash keys are unique, include zero and signed extrema
when the size permits, and miss keys are disjoint. Dense immutable maps contain even keys and
probe odd holes; sparse storage is explicitly selected. Primitive-key object maps reuse boxed
integer payloads allocated before measurement. Lists contain ordered integer or wide long values;
bits set every third position. Heap and dense-id accesses use a seeded shuffled permutation.

JMH creates and reference-checks each fixture before timing, checks it again after the trial,
and consumes each batch's result. Checks validate complete stored entries and collection sizes,
bit values, heap extraction order and union-find connectivity. Repeated checks catch destructive
workloads whose fixtures fail after the first invocation.

Each batch performs the named work over `size` elements:

- `insert`/`append` include clearing reused, preallocated storage and filling it again.
- `grow` and immutable `build` include allocation; their returned collection escapes to JMH.
- Map/set removal includes immediate reinsertion of the original payload. List removal swaps
  the tail into a shuffled index and includes restoring the original list order.
- Deque `cycle` removes the front and appends it, wrapping the ring while preserving the sequence
  after a complete batch. Heap `update` raises and restores every priority; `extract-restore`
  drains the heap and restores all ids at their cached keys.
- Bit `range` clears storage, sets the full range, clears `[1, size)`, and reads cardinality.
  `cardinality` and `traverse` scan the bitset once. `dynamic-bits.grow` appends all logical bits.
- Disjoint `union-find` allocates, unions a shuffled chain, and compresses it; `find-compressed`
  reads an already compressed structure.

There is one workload dispatch and, for scalar results, one boxed checksum per batch. Setup,
random generation and reference checks are outside measurement. The primary JMH score is ns/batch;
the adapter exports `mean_ns_per_batch`, `mean_ns_per_element` and `median_ns_per_element`.
Per-element values divide by logical size, including for whole-bitset scans. Composite operations
include all named work, so a remove/restore element includes both removal and restoration.
Compare matching workload identities and controls; these metrics are not comparable across operations.
Raw JMH JSON retains every fork/iteration and the uncertainty estimates.

## Local process contract

```sh
BENCH=util-bench/build/install/util-bench/bin/util-bench
JAVA_HOME=/absolute/jdk17 "$BENCH" describe
printf '%s\n' '{"parameters":{"workloads":"int-int-map.lookup-hit","sizes":"1024","seeds":"42"}}' > /tmp/util-request.json
JAVA_HOME=/absolute/jdk17 "$BENCH" plan /tmp/util-request.json > /tmp/util-plan.json
python3 -c 'import json; print(json.dumps(json.load(open("/tmp/util-plan.json"))["cases"][0]))' > /tmp/util-case.json
JAVA_HOME=/absolute/jdk17 "$BENCH" run /tmp/util-case.json /tmp/util-capture
```

Supported request parameters are strict: unknown names, empty selections, duplicate values,
unknown workloads and out-of-range controls fail planning. Each selection must resolve.

| Parameter | Default | Accepted values |
| --- | --- | --- |
| `workloads` | All 77 | Comma-separated exact workload names |
| `sizes` | `1024` | Comma-separated unique integers in `1..65536` |
| `seeds` | `42` | Comma-separated unique signed Long values |
| `warmups`, `samples` | `5`, `5` | `1..30` |
| `iteration-ms` | `300` | `10..5000` |
| `forks` | `2` | `1..5` |
| `smoke` | `false` | `true` fixes one fork, one warmup/sample and 10ms iterations |

Cases freeze all defaults, workload, size, seed and resources. The adapter uses one JMH thread,
Serial GC and a fixed 512MiB heap, clears inherited JVM instrumentation for the child process,
and retains the runtime and controls. Case resources reserve one CPU and 1GiB memory, allowing
for the adapter, JMH coordinator and fork. The timeout includes startup and checking headroom.
Failed captures retain logs and partial files and exit unsuccessfully.

## Lab runs

Use the maintained authenticated HTTP client at `https://lab.eignex.com`:

```sh
lab run util-bench/lab/recipes/smoke.json
# Edit the baseline/candidate refs in a copy of comparison.json before submission.
lab run /absolute/path/comparison.json
```

Both revisions must contain the adapter and support the same frozen workloads. The comparison
recipe samples sizes 65, 1024 and 65536, with three paired repetitions. Reduce `workloads` to
focus an experiment. The recipes do not register schedules or submit jobs automatically.

Each lab row runs all revision arms for one workload, size, seed and repetition on one compatible
machine in one worker attempt. Keep automatic placement: preparation and other rows may use other
machines. Worker loss replaces the entire unfinished row; retain failed-attempt and preparation costs
separately. Inspect variation across forks and complete paired repetitions before drawing conclusions.
Lab SSH, host discovery and server administration belong to the eignex-lab operations owner.

The earlier standalone JMH harness and retained captures remain under
[`benchmarks/collections`](../benchmarks/collections/README.md). Its protocol and scores differ;
start a fresh baseline for util-bench.
