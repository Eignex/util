# Collection benchmarks

Standalone JMH harness for the JVM primitive collections. It is separate from the
library build and publication.

Build the revisions to compare with `./gradlew :jvmJar -PciVersion=0.0.0-ci`, saving
each `build/libs/util-jvm-0.0.0-ci.jar` outside its checkout. The retained baseline
is commit `0160741ae181d9327579ce1a8a49896b7d2fb5c7`.

Run a complete comparison on Java 17:

```sh
./gradlew -p benchmarks/collections writeBenchmarkClasspath \
  -PutilJar=/absolute/path/baseline.jar --no-configuration-cache

python3 benchmarks/collections/run-paired.py \
  --baseline /absolute/path/baseline.jar \
  --candidate /absolute/path/candidate.jar \
  --classpath benchmarks/collections/build/benchmark-classpath.txt \
  --java /absolute/path/to/java17/bin/java \
  --output /tmp/util-collections-comparison
```

The runner executes both arms consecutively for each workload, alternates which arm
runs first, and repeats the complete comparison twice. Each arm uses one JVM fork,
three 300 ms warmup iterations and five 300 ms measurement iterations. Raw results,
execution order and the JMH log are written to the output directory. Python 3 is
only required for this comparison runner.

For a single artifact, the Gradle `benchmark` task uses two forks, three 500 ms
warmup iterations and five 500 ms measurement iterations:

```sh
./gradlew -p benchmarks/collections benchmark \
  -PutilJar=/absolute/path/candidate.jar -Presult=/tmp/candidate.json \
  --no-configuration-cache
```

Each workload processes 1,024 keys generated with seed 42. Lookups hit existing
entries; insertion includes clearing the preallocated map, growth includes creating
a map, and removal includes restoring each removed entry. Scores are nanoseconds
per key processed. This is a cache-resident workload on one JVM and host.

The retained comparison is in `results/2026-11-05/`. Its environment file records
the artifact hashes and protocol. CPU contention varied during measurement; the raw
samples and agreement between repetitions are needed to assess the results.

## Deque and disjoint-set comparison

The focused suite measures grouping, indexed removal and wrapped growth at sizes
1,024 and 65,536. Build and save both library JARs as above, then compile the harness
against either supplied JAR and run:

```sh
python3 benchmarks/collections/run-paired.py --suite deque-disjoint \
  --baseline /absolute/path/baseline.jar \
  --candidate /absolute/path/candidate.jar \
  --classpath benchmarks/collections/build/benchmark-classpath.txt \
  --java /absolute/path/to/java17/bin/java \
  --output /tmp/util-deque-disjoint-comparison
```

This suite uses the same alternating paired protocol with a fixed 512 MiB heap,
Serial GC and JMH's GC profiler. Scores are per invocation. Grouping returns a
complete partition snapshot from compressed paths. Removal includes appending
the removed value. Growth includes allocating, filling and wrapping a deque,
then growing its full buffer once. Fixture checks run outside measurement.

The [2026-11-16 report](results/2026-11-16-deque-disjoint/README.md) includes
per-repetition results, allocation measurements, caveats and raw captures. The
default suite and its earlier retained comparison remain unchanged.

## Primitive-map comparisons

The `primitive-maps` suite reuses the maintained Java fixtures from `util-bench`
without its SDK or adapter. It supports four immutable builds and eight mutable
map/set remove-restore workloads, at sizes 1,024 and 65,536 with seed 42 by default.
Build both library JARs as above, then compile this harness against either supplied
JAR. Select the immutable builders with:

```sh
python3 benchmarks/collections/run-paired.py --suite primitive-maps \
  --workloads int-int-dense.build,int-long-dense.build,int-int-sparse.build,int-long-sparse.build \
  --baseline /absolute/path/baseline.jar \
  --candidate /absolute/path/candidate.jar \
  --classpath benchmarks/collections/build/benchmark-classpath.txt \
  --java /absolute/path/to/java17/bin/java \
  --output /tmp/util-primitive-map-comparison
```

Omit `--workloads` to run all 12 cases, or use `--sizes 65,1024,65536` to change the
sizes. Both focused suites support `--sizes`; deque sizes must be powers of two so
the growth fixture fills its ring. Names and sizes must be unique and valid.

The paired controls match the deque suite: fixed 512 MiB heap, Serial GC, GC
profiling, two reversed-order repetitions, one fork per arm, three 300 ms warmups
and five 300 ms measurements. Scores are per complete batch, not per element.
Build batches allocate and return a whole map; removal batches remove and restore
every fixture key. Complete reference checks run outside timing.

The [2026-11-16 primitive-map report](results/2026-11-16-primitive-maps/README.md)
retains final build measurements, uncertainty, raw samples, and the separate
exploratory comparison that led to reverting the deletion candidate.
