# Object-map lookup and empty-clear performance

The final production change touches only `MutableIntObjectMap` and
`MutableLongObjectMap`:

- Required lookup (`getValue`) returns directly from one probe. A stored null
  previously triggered a second `containsKey` probe; missing keys also walked the
  same chain twice before throwing. Exception type and message are unchanged.
- Clearing an already-empty map returns before scanning its occupancy and reference
  arrays. Nonempty clearing still releases references and retains backing storage.

No numeric-map clear guards, long-key mixer changes or public hash edits remain.
Those broader candidates were measured and rejected; their results are retained
under [exploratory](exploratory/README.md). In particular, the existing mixer has
an extreme collision pattern for `i << 48`, but the attempted fix regressed an
otherwise favorable `i << 32` control by about 6×. That remains an opportunity for
a future solution with a better tradeoff.

## Final comparison

The baseline is clean main at `e8c0d83110533464e707dc51802e4e4e9fbd065a`.
Measurements and local validation predate rebasing the PR onto `1513d00`;
the two measured object-map sources and benchmark fixtures are unchanged by that
rebase. GitHub CI validates the integrated revision.
Only the two object-map class files differ between baseline and final JARs; their
`javap -public` output is identical. Artifact and source hashes are recorded in
`environment.json` and were checked again after timing. Exploratory artifacts
are different JARs and are not pooled with the final results.

Java 17, JMH 1.37, one thread, Serial GC and a fixed 512 MiB heap. Each of 14
cases runs adjacent baseline and candidate JVM forks, with three 300 ms warmups
and five 300 ms measurements. Repeat twice, reversing each pair's arm order:
28 complete pairs and 56 successful captures.

Inputs use seed 42 and unique int or long keys, including zero and the extrema.
Payloads are all null or all distinct non-null objects. Lookup checks every
returned payload by identity outside timing, validates missing-key exceptions,
and returns a null-count checksum. Mixed null/non-null payloads are covered by
correctness tests, but not timed here. Scores are nanoseconds per whole lookup
batch, not per key.

The empty-clear fixture reserves space for the requested number of entries but
starts empty. The nonempty control puts one entry and clears it on every
invocation. Both return the map to JMH and verify emptiness outside timing.
These clear scores include dispatch and, for the control, insertion; they are not
isolated method-call latencies. Fully populated clearing and growth are not timed.

This was a shared local host with substantial variable contention. Results are
kept per repetition. JMH uncertainty estimates cover within-fork samples, not
between-arm changes in host load. These are JVM microbenchmarks; no Native timing
or downstream application speedup is claimed.

Speedup is baseline time divided by candidate time. Above 1 is faster; below 1
is slower. No overall average is used to conceal differences between cases.

| Workload | Parameters | Speedup, repetition 1 | Speedup, repetition 2 |
| --- | --- | ---: | ---: |
| requiredLookup | kind=int, payload=null, size=1024 | 1.39× | 1.13× |
| requiredLookup | kind=int, payload=value, size=1024 | 1.00× | 1.01× |
| requiredLookup | kind=long, payload=null, size=1024 | 1.66× | 1.00× |
| requiredLookup | kind=long, payload=value, size=1024 | 0.91× | 1.03× |
| clear | kind=int-object, refill=false, size=1024 | 80.14× | 86.99× |
| clear | kind=long-object, refill=false, size=1024 | 76.84× | 70.96× |
| clear | kind=int-object, refill=true, size=1024 | 1.02× | 0.90× |
| requiredLookup | kind=int, payload=null, size=65536 | 1.19× | 1.35× |
| requiredLookup | kind=int, payload=value, size=65536 | 0.95× | 1.06× |
| requiredLookup | kind=long, payload=null, size=65536 | 1.86× | 1.35× |
| requiredLookup | kind=long, payload=value, size=65536 | 1.00× | 0.75× |
| clear | kind=int-object, refill=false, size=65536 | 11240.32× | 7822.71× |
| clear | kind=long-object, refill=false, size=65536 | 8440.69× | 10821.94× |
| clear | kind=int-object, refill=true, size=65536 | 0.98× | 0.98× |

Null-payload lookup speedups range from 1.00× to 1.86×;
one small long-key pair is effectively unchanged. Value-only lookup controls are
mixed (0.75–1.06×), including a slower large long-key pair.
These results support removing redundant null-path probes, not a blanket lookup
throughput claim. Empty object-map clears improve 71–11,240×
in these reserved-table fixtures. The put-one-then-clear controls are mixed
(0.90–1.02×), so no general nonempty-clear improvement is claimed.

The four arm/repetition JSON files contain all samples and uncertainty estimates.
`summary.json` contains matching pairs, `order.json` execution order, and `jmh.log`
every final capture. Both rejected cohorts retain the same evidence, exact
prototype patches, and captured runners in their own directories.

## Validation and reproduction

`./gradlew jvmTest linuxX64Test jvmJar lintDocs -PciVersion=0.0.0-ci --offline`
passed for the final sources: 371 JVM tests and 350 Linux Native tests, zero
failures, errors or skips. Tests cover nullable payloads in wraparound collision
chains, missing-key exceptions, extreme keys, growth, deletion, reference release,
repeated clears and reuse without replacing backing arrays. Public JVM signatures
are unchanged in both modified maps. All 56 final JMH captures passed fixture checks.

Build both revisions and compile the standalone harness as described in
[the harness README](../../README.md). Run `--suite hashmaps` with `--cases`
set to the comma-separated `cases` array in `environment.json`; it selects the
14 object-map cases above. Omitting `--cases` also runs numeric-map controls.
The `long-hash` suite and saved exploratory patches reproduce the rejected mixer
experiment. The maintained `util-bench` workload catalog is unchanged.
