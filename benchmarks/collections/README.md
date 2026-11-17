# Collection benchmarks

Standalone JMH harness, separate from library publication. Build and save each revision's
JAR with `./gradlew :util:jvmJar -PciVersion=0.0.0-ci`, then run on Java 17:

```sh
./gradlew -p benchmarks/collections benchmark \
  -PutilJar=/absolute/path/candidate.jar -Presult=/tmp/candidate.json \
  --no-configuration-cache
```

For focused workloads, build the classpath and invoke JMH directly:

```sh
./gradlew -p benchmarks/collections writeBenchmarkClasspath \
  -PutilJar=/absolute/path/candidate.jar --no-configuration-cache
java -cp "$(cat benchmarks/collections/build/benchmark-classpath.txt)" org.openjdk.jmh.Main \
  'com.eignex.util.bench.CollectionsBenchmark.batch$' \
  -p workload=int-list.read,long-list.read -p size=1024 -p seed=42 \
  -f 1 -wi 3 -w 300ms -i 5 -r 300ms -prof gc \
  -jvmArgs '-Xms512m -Xmx512m -XX:+UseSerialGC' -rf json -rff /tmp/lists.json
```

Use JMH `-l` to list the standalone collection, deque/disjoint-set, hashmap and long-hash
benchmarks. Shared fixture workloads are described in [util-bench](../../util-bench/README.md).
Scores use each benchmark's declared units; shared fixtures report ns/batch.

Compare adjacent baseline/candidate runs with identical controls, then reverse their order.
Keep raw results and artifact hashes outside the repository, or use the paired lab recipe
in util-bench. Normal tiering is primary. For C1 or non-tiered C2 diagnostics, pass
`-XX:TieredStopAtLevel=1` or `-XX:-TieredCompilation` through JMH's `-jvmArgsAppend`.
Capture `-XX:+UnlockDiagnosticVMOptions -XX:+LogCompilation` separately from timing runs.
