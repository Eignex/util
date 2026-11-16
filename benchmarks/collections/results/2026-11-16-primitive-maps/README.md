# Primitive-map construction performance

The final candidate changes only `IntIntMap` and `IntLongMap` construction:

- Start from the input length and subtract duplicates while writing entries,
  replacing a separate scan of the entire occupancy array to calculate `size`.
  Sparse probing already detects duplicates, so unique sparse insertions need no
  additional counting work.
- Allocate dense payload arrays without filling absent slots with the sentinel.
  Lookups already check occupancy before reading a slot. Stored sentinel values,
  missing-key defaults, last-write-wins duplicates and key iteration retain their
  existing behavior.

The measured baseline is clean `main` at `3fc5e2698c74a889ab98a5bac1ddf9d761e396cf`.
These captures and local validation predate rebasing the PR onto newer `main`;
the measured builder sources are unchanged by that rebase.
Artifact hashes, source hashes, changed JAR entries and runtime controls are in
`environment.json`. Only the two map implementations and their nested classes
differ in the final JARs. No deletion edits remain.

## Protocol

Java 17, JMH 1.37, one thread, Serial GC and a fixed 512 MiB heap. Each of eight
workload/size cases runs adjacent baseline and candidate JVM forks, with three
300 ms warmups and five 300 ms measurements. Repeat twice and reverse each pair's
arm order. This produces 16 complete pairs and 32 successful captures.

The standalone harness compiles the existing `util-bench` Java fixtures directly;
it adds no workloads to the maintained catalog or frozen status sweep. Those
fixtures check complete entries and map sizes before and after timing. Inputs use
seed 42, unique keys, nonzero absent sentinel `-1`, and escaping constructed maps.
Dense inputs use every other key, leaving holes. Sparse storage is forced.
Scores are nanoseconds per complete build, including allocation.

This was a shared local host. CPU contention decreased before the final captures,
but remained uncontrolled. Each repetition is retained separately. These are JVM
microbenchmarks, not Native speed or downstream application measurements. Default
sentinel `0` and duplicate-heavy inputs were correctness-tested but not timed.

## Results

Speedup is baseline time divided by candidate time; above 1 is faster and below 1
is slower. No overall average is used to hide differences between cases.

| Build workload | Entries | Speedup, repetition 1 | Speedup, repetition 2 |
| --- | ---: | ---: | ---: |
| int-int-dense.build | 1,024 | 1.76× | 2.34× |
| int-long-dense.build | 1,024 | 1.59× | 1.68× |
| int-int-sparse.build | 1,024 | 1.42× | 1.53× |
| int-long-sparse.build | 1,024 | 1.22× | 0.62× |
| int-int-dense.build | 65,536 | 1.93× | 1.96× |
| int-long-dense.build | 65,536 | 1.56× | 1.42× |
| int-int-sparse.build | 65,536 | 0.97× | 1.85× |
| int-long-sparse.build | 65,536 | 1.05× | 1.05× |

Dense construction improved in all measured pairs, by 1.42–2.34×. Sparse Int-to-Int
construction at 1,024 entries also improved in both repetitions. Other sparse
results are mixed or small, so these captures do not establish a general sparse
construction speedup. In particular, the second 1,024-entry sparse Int-to-Long
candidate capture slowed across iterations and has a wide uncertainty estimate;
that result is retained rather than excluded. Allocated storage sizes are unchanged:
this optimization removes initialization and scanning work.

The four arm/repetition JSON files contain every sample and uncertainty estimate.
`summary.json` contains all matching pairs, `order.json` the execution order, and
`jmh.log` every final capture. The [exploratory captures](exploratory/README.md)
retain the separate rejected deletion experiment and its original construction
prototype; they are not evidence for the final artifact and are not pooled here.

## Validation and reproduction

`./gradlew jvmTest linuxX64Test jvmJar lintDocs -PciVersion=0.0.0-ci --offline`
passed: 347 JVM tests and 326 Linux Native tests, zero failures. New tests compare
both representations and payload widths against reference maps with duplicate-heavy
inputs, holes, sentinel payloads, custom missing defaults, empty maps and extreme
keys. All 32 final JMH captures passed their fixture checks.

Use the `primitive-maps` command in [the harness README](../../README.md) with the
four build workloads. The benchmark helper also supports the eight existing mutable
map/set remove-restore fixtures for future comparisons. Its default remains the
original standalone collection suite.
