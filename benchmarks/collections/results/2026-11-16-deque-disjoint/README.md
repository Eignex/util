# Deque and disjoint-set performance comparison

The candidate replaces boxed disjoint-set grouping with two passes over primitive
arrays, shifts the shorter side during indexed deque removal, and uses two bulk
copies to grow a wrapped deque. Public signatures and ordering stay the same.

The baseline is clean tracked source at `93a62baf78510ade006a83b6c7c03b593cca35f6`.
The measured candidate starts at the same revision with only these library edits.
These captures and the validation below predate the PR rebase onto current `main`.
Pre-existing changes in the original checkout were excluded from both arms. Artifact and harness
hashes, runtime, controls and changed JAR entries are in `environment.json`.

## Protocol

Java 17, JMH 1.37, one thread, Serial GC and a fixed 512 MiB heap. For each of 14
cases, run adjacent baseline/candidate arms in separate JVMs with three 300 ms
warmups and five 300 ms measurements. Repeat twice, reversing each pair's order.
The retained four arm/repetition JSON files contain all raw iteration data and
uncertainty estimates; `order.json` records execution order and `jmh.log` records
all 56 successful captures. `summary.json` retains all 28 matching pairs.

Group snapshots start with compressed paths and use singleton groups, groups of
32, or one group. Group order and every member are checked outside measurement.
Indexed removal starts with a wrapped full deque, removes a fixed logical
position and appends the removed value. Size and complete membership are checked
after the trial. Growth includes allocation, filling, wrapping and one growth
from an initially full deque; the returned deque escapes. Its order is checked
before timing. Scores are nanoseconds per invocation, not per element.

## Results

Speedup is baseline time divided by candidate time: values above 1 are faster,
values below 1 are slower. These local measurements used a shared host with varying
CPU contention; the two repetitions are shown separately rather than pooled.

| Groups: elements / members per group | Speedup, repetition 1 | Speedup, repetition 2 | Allocated bytes reduced, both repetitions |
| --- | ---: | ---: | ---: |
| 1,024 / 1 | 3.81× | 4.42× | 75.4–75.4% |
| 1,024 / 32 | 2.27× | 1.56× | 85.3–85.3% |
| 1,024 / 1,024 | 2.54× | 1.65× | 83.5–83.5% |
| 65,536 / 1 | 4.66× | 4.76× | 75.5–75.6% |
| 65,536 / 32 | 2.51× | 2.74× | 85.9–85.9% |
| 65,536 / 65,536 | 1.99× | 1.55× | 85.4–85.4% |

Grouping is consistently faster and removes per-member boxing, temporary member
lists and the root hash table. Exact-sized output arrays reduce allocation by
75–86% in the tested shapes.

| Deque case | Elements | Speedup, repetition 1 | Speedup, repetition 2 |
| --- | ---: | ---: | ---: |
| remove front | 1,024 | 1.04× | 0.76× |
| remove middle | 1,024 | 1.02× | 0.84× |
| remove back | 1,024 | 159.17× | 200.63× |
| grow (complete lifecycle) | 1,024 | 1.20× | 1.05× |
| remove front | 65,536 | 1.04× | 0.89× |
| remove middle | 65,536 | 1.19× | 1.05× |
| remove back | 65,536 | 3728.25× | 14685.65× |
| grow (complete lifecycle) | 65,536 | 0.94× | 1.08× |

Tail removal now shifts zero elements, changing it from O(size) to O(1). In general,
indexed removal shifts `min(index, size - index - 1)` elements. Front and middle
timings include regressions in some pairs, so these captures do not establish a
speedup for those positions. Deque removal itself allocates no objects; tiny
profiler allocation readings are background overhead and should not be interpreted
as collection allocation.

Bulk copying simplifies growth and avoids the per-element ring mask, but the
complete-lifecycle growth results range from 0.94× to 1.20×. This run does not
establish a consistent growth speedup. These results do not measure Native speed
or downstream application throughput.

## Validation

`./gradlew jvmTest linuxX64Test jvmJar lintDocs -PciVersion=0.0.0-ci --offline`
passed in the isolated worktree: 319 JVM tests and 294 Linux Native tests, zero
failures. New tests exhaust ring positions and removal indices at small sizes,
wrapped growth at both ends, invalid removal, empty/singleton partitions,
non-minimal roots, repeated grouping and independent result arrays. The standalone
JMH harness compiled and all fixture checks passed in both arms.

Run the comparison from the benchmark checkout using the `--suite deque-disjoint`
command in [the harness README](../../README.md). Save a clean baseline JAR before
applying the candidate library changes, and compile the harness against that JAR.
