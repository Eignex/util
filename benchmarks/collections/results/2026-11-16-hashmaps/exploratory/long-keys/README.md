# Rejected long-key mixer experiment

The original table mixer multiplies a Long and folds the upper 32 bits into the
lower 32 bits. For keys `i << 48`, both the low product bits and the low bits of
that fold are zero. At the measured table capacities, all these keys therefore
share one starting bucket. Building and looking up the whole map take quadratic
probe work as the chain grows.

This prototype xor-folded the upper half of the input before multiplication too.
It spread all tested strides, including shifts 40, 48 and 56. At 8,192 entries,
`upper48` construction improved 297–399×, lookup 870–1,272×, and remove-restore
2,013–2,129×. However, `upper32` previously had collision-free homes; its
8,192-entry remove-restore control became about 6× slower in both repetitions.
Random and sequential controls were also mixed or slower. The mixer was rejected
rather than shipping that broad tradeoff based only on a favorable edge case.

The public `fibonacciHash(Long)` formula was held unchanged during the experiment.
To reconstruct this candidate, use `git apply --unidiff-zero` to apply `../lookup-clear/numeric-clear.patch` and
`long-mixer.patch` to the final sources. The second patch also includes its
additional correctness and distribution tests. Validation passed 374 JVM and
353 Linux Native tests, plus `lintDocs`; all eight changed JVM class surfaces were
identical. There are 48 complete pairs and 96 successful captures.

Scores are nanoseconds per whole 1,024- or 8,192-key batch. Build includes allocation;
lookup hits all entries; remove-restore deletes and immediately reinserts every key.
Inputs are unique, use seed 42 for the random control, and preserve insertion order
for every shape. Fixtures check every entry and size before and after timing,
including checksums for lookup and remove-restore.

Speedup is baseline time / candidate time. Values below 1 are slower.

| Workload | Parameters | Speedup, repetition 1 | Speedup, repetition 2 |
| --- | --- | ---: | ---: |
| build | shape=random, size=1024 | 1.05× | 0.55× |
| lookup | shape=random, size=1024 | 0.98× | 0.36× |
| removeRestore | shape=random, size=1024 | 0.62× | 1.20× |
| build | shape=sequential, size=1024 | 0.98× | 0.84× |
| lookup | shape=sequential, size=1024 | 0.74× | 0.94× |
| removeRestore | shape=sequential, size=1024 | 0.94× | 0.95× |
| build | shape=upper32, size=1024 | 0.66× | 0.62× |
| lookup | shape=upper32, size=1024 | 1.03× | 0.81× |
| removeRestore | shape=upper32, size=1024 | 0.49× | 0.57× |
| build | shape=upper48, size=1024 | 91.41× | 97.37× |
| lookup | shape=upper48, size=1024 | 170.47× | 311.13× |
| removeRestore | shape=upper48, size=1024 | 519.48× | 489.73× |
| build | shape=random, size=8192 | 0.86× | 1.26× |
| lookup | shape=random, size=8192 | 1.02× | 0.79× |
| removeRestore | shape=random, size=8192 | 1.10× | 0.63× |
| build | shape=sequential, size=8192 | 1.01× | 1.12× |
| lookup | shape=sequential, size=8192 | 0.72× | 0.58× |
| removeRestore | shape=sequential, size=8192 | 0.96× | 0.86× |
| build | shape=upper32, size=8192 | 1.17× | 0.90× |
| lookup | shape=upper32, size=8192 | 0.42× | 0.32× |
| removeRestore | shape=upper32, size=8192 | 0.16× | 0.17× |
| build | shape=upper48, size=8192 | 297.24× | 399.19× |
| lookup | shape=upper48, size=8192 | 1272.36× | 869.79× |
| removeRestore | shape=upper48, size=8192 | 2129.39× | 2013.48× |

The four arm/repetition JSON files retain every measurement sample and uncertainty
estimate. `summary.json` contains each pair, `order.json` the arm order, and
`jmh.log` the complete JMH output. Source and artifact hashes are in
`environment.json`; the captured runner is `run-paired.py`. These results are kept
separate from final-candidate results.
