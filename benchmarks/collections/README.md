# Collection benchmarks

Standalone JMH harness for the JVM primitive collections. It is separate from the
library build and publication.

Build the revisions to compare with `./gradlew :util:jvmJar -PciVersion=0.0.0-ci`, saving
each `util/build/libs/util-jvm-0.0.0-ci.jar` outside its checkout.

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

Keep measurement output outside the repository. Record artifact hashes and runtime
controls alongside raw samples, and compare complete paired repetitions to assess
variation from host contention.

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
sizes. All focused suites support `--sizes`; deque sizes must be powers of two so
the growth fixture fills its ring. Names and sizes must be unique and valid.

The paired controls match the deque suite: fixed 512 MiB heap, Serial GC, GC
profiling, two reversed-order repetitions, one fork per arm, three 300 ms warmups
and five 300 ms measurements. Scores are per complete batch, not per element.
Build batches allocate and return a whole map; removal batches remove and restore
every fixture key. Complete reference checks run outside timing.

## Hashmap comparisons

The `hashmaps` suite measures nullable required lookups, repeated clears of empty
reserved maps, and put-one-entry-then-clear controls. It covers all six mutable
map types at reserved sizes 1,024 and 65,536. Run it with the same JAR, classpath,
Java and output arguments shown above, replacing the suite with `--suite hashmaps`.
Lookup scores are per complete batch; clear scores are per clear invocation,
including dispatch and, for the nonempty control, insertion.

The `long-hash` suite measures `MutableLongIntMap` construction, successful lookup,
and remove-restore batches with random, sequential, upper-32-bit and upper-48-bit
keys. Its default sizes are 1,024 and 8,192. Sizes cannot exceed 65,536, so every
upper-48-bit key remains unique. Build batches include allocation; lookup and
removal use preallocated tables. Setup and teardown check all entries and sizes;
lookup and removal checksums are checked outside timing.

```sh
python3 benchmarks/collections/run-paired.py --suite long-hash \
  --baseline /absolute/path/baseline.jar \
  --candidate /absolute/path/candidate.jar \
  --classpath benchmarks/collections/build/benchmark-classpath.txt \
  --java /absolute/path/to/java17/bin/java \
  --output /tmp/util-long-hash-comparison
```

Both suites use the same alternating paired protocol, fixed 512 MiB heap, Serial
GC and GC profiling as the other focused suites. Select generated case names with `--cases`, for
example `--cases required-int-null-1024,empty-clear-int-object-1024`.
