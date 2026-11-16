# Exploratory nullable lookups and six-map empty-clear guards

This candidate changed required lookup in both object-valued maps and added an
empty-clear guard to all six mutable maps. It did not change the long-key mixer.
The four numeric-map guards were subsequently removed: the 1,024-entry numeric
put-one-then-clear control regressed in both repetitions (0.69× and 0.73× speedups).
Object-map controls were mixed. Repeated empty clears improved in all captured
pairs, but this does not establish a general nonempty-clear improvement.

To reconstruct this candidate, use `git apply --unidiff-zero` to apply `numeric-clear.patch` to the final production
sources. Its six changed class files and exact source hashes are recorded in
`environment.json`. Validation passed 371 JVM and 350 Linux Native tests, plus
`lintDocs`. There are 48 complete pairs and 96 successful captures.

Speedup is baseline time / candidate time. Values below 1 are slower.

| Workload | Parameters | Speedup, repetition 1 | Speedup, repetition 2 |
| --- | --- | ---: | ---: |
| requiredLookup | kind=int, payload=null, size=1024 | 1.99× | 1.55× |
| requiredLookup | kind=int, payload=value, size=1024 | 1.24× | 0.98× |
| requiredLookup | kind=long, payload=null, size=1024 | 1.76× | 1.42× |
| requiredLookup | kind=long, payload=value, size=1024 | 0.89× | 0.90× |
| clear | kind=int-int, refill=false, size=1024 | 10.20× | 6.85× |
| clear | kind=int-long, refill=false, size=1024 | 8.45× | 7.96× |
| clear | kind=int-double, refill=false, size=1024 | 8.67× | 9.83× |
| clear | kind=long-int, refill=false, size=1024 | 7.42× | 10.96× |
| clear | kind=int-object, refill=false, size=1024 | 95.06× | 100.59× |
| clear | kind=long-object, refill=false, size=1024 | 104.33× | 105.18× |
| clear | kind=int-int, refill=true, size=1024 | 0.69× | 0.73× |
| clear | kind=int-object, refill=true, size=1024 | 0.89× | 1.04× |
| requiredLookup | kind=int, payload=null, size=65536 | 1.03× | 1.24× |
| requiredLookup | kind=int, payload=value, size=65536 | 1.21× | 1.01× |
| requiredLookup | kind=long, payload=null, size=65536 | 0.85× | 1.33× |
| requiredLookup | kind=long, payload=value, size=65536 | 0.73× | 1.02× |
| clear | kind=int-int, refill=false, size=65536 | 672.61× | 545.68× |
| clear | kind=int-long, refill=false, size=65536 | 590.74× | 579.78× |
| clear | kind=int-double, refill=false, size=65536 | 974.97× | 503.61× |
| clear | kind=long-int, refill=false, size=65536 | 604.20× | 850.29× |
| clear | kind=int-object, refill=false, size=65536 | 11836.01× | 9497.51× |
| clear | kind=long-object, refill=false, size=65536 | 12757.99× | 11761.83× |
| clear | kind=int-int, refill=true, size=65536 | 1.43× | 1.13× |
| clear | kind=int-object, refill=true, size=65536 | 0.79× | 0.98× |

The four arm/repetition JSON files retain every measurement sample and uncertainty
estimate. `summary.json` contains each pair, `order.json` the arm order, and
`jmh.log` the complete JMH output. Source and artifact hashes are in
`environment.json`; the captured runner is `run-paired.py`. These results are kept
separate from final-candidate results.
