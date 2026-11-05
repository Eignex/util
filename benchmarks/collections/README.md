# Collection benchmarks

Standalone JMH harness for the JVM primitive maps and sets. It is separate from the
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
