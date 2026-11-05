<p align="center">
  <a href="https://eignex.com/">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/Eignex/.github/refs/heads/main/profile/banner-white.svg">
      <source media="(prefers-color-scheme: light)" srcset="https://raw.githubusercontent.com/Eignex/.github/refs/heads/main/profile/banner.svg">
      <img alt="Eignex" src="https://raw.githubusercontent.com/Eignex/.github/refs/heads/main/profile/banner.svg" style="max-width: 100%; width: 22em;">
    </picture>
  </a>
</p>

# Util

[![Build](https://github.com/Eignex/util/actions/workflows/build.yml/badge.svg)](https://github.com/Eignex/util/actions/workflows/build.yml)

Shared Kotlin Multiplatform primitives for Eignex libraries and applications.

Primitive collections, array algorithms and concurrency storage without a dependency
on a solver, statistics library or BLAS engine. The API is under development.

## Utilities

- Primitive collections: lists, deques, sets, maps and packed bitsets.
- Array algorithms: stable argsort and primitive binary search.
- Concurrency primitives: synchronous locks, atomic cells and additive counters.
- Numeric primitives: checked Long arithmetic, a signed 128-bit accumulator and arbitrary-precision integers.
- Hashing: deterministic SplitMix64 mixing and 64-bit byte/string hashes.
- Text input: chunked character sources, buffered character reading and compressed file input.
- Host concurrency: ordered parallel execution on JVM threads and Native workers.
- Process support: properties/environment, standard output flushing, error output and process exit.
- Runtime diagnostics: reference identity hashing, JVM heap samples and cumulative JIT compilation time.

## Usage

Add the snapshot repository and dependency to your Kotlin Multiplatform build:

```kotlin
repositories {
    mavenCentral()
    maven("https://central.sonatype.com/repository/maven-snapshots/")
}

kotlin.sourceSets.commonMain.dependencies {
    implementation("com.eignex:util:0.0.1-SNAPSHOT")
}
```

Accumulate values with primitive keys:

```kotlin
import com.eignex.util.collections.MutableIntDoubleMap

val totals = MutableIntDoubleMap()
totals.addTo(42, 1.5)
totals.addTo(42, 2.0)
check(totals.getOrDefault(42, 0.0) == 3.5)
```

Create an atomic counter or protect coupled state:

```kotlin
import com.eignex.util.concurrent.AtomicCells
import com.eignex.util.concurrent.Mutex
import com.eignex.util.concurrent.guarded

val count = AtomicCells.newLong()
count.add(1L)

val lock = Mutex()
val values = mutableListOf<Int>()
lock.guarded { values.add(42) }
```

`SerialCells` requires single-threaded ownership or external synchronization.
`AtomicCells` supports compare-and-set and per-cell atomic updates. `AdderCounters`
uses striped JVM adders with best-effort totals and atomic cells on other targets.
JS and Wasm operations run within a single execution agent; these primitives do
not synchronize separate workers. Mutexes are synchronous and do not support re-entry.

The collection interfaces expose primitive elements: `IntCollection`/`LongCollection`
provide reading and snapshots, indexed variants provide positional access, and
`MutableIntSet`/`MutableLongSet` provide membership changes. Maps expose `IntKeyed` or
`LongKeyed` for key traversal and snapshots. `IntIntLookup` and `IntLongLookup` work
with both mutable hash maps and immutable dense/sparse maps. Read-only interfaces
do not make a mutable backing collection immutable.

Concrete collections retain inline `forEach` calls. Through an interface, pass an
`IntConsumer` or `LongConsumer` for primitive traversal. Sorted snapshot helpers
accept the interfaces, including deques, swap sets and immutable map keys.

Collections are mutable and require external synchronization when shared. List
removal uses swap removal, which does not preserve element order.

Arbitrary-precision integers, parallel execution, process support and runtime diagnostics
are available on JVM and Native. Heap and JIT diagnostics return `null` on Native.
Text file input is available on JVM and POSIX Native targets (Linux and Apple).
These APIs can be called from `commonMain` when all consumer targets support them.

Read a UTF-8 file with automatic decompression based on its suffix:

```kotlin
import com.eignex.util.io.openTextFile
import com.eignex.util.io.readText
import com.eignex.util.io.useSource

val text = openTextFile("input.txt.gz").useSource { it.readText() }
```

Supported compression suffixes are `.gz`, `.xz`, `.bz2` and `.zst`. JVM gzip input
uses the JDK; other formats require their corresponding command-line decompressor
on `PATH`. POSIX Native uses an external decompressor for each format and buffers
the complete decoded input. `useSource` closes the source even when reading stops early.

`BigInt` uses `java.math.BigInteger` on JVM and IonSpin bignum on Native. Use
`remainderOf` for a remainder with the dividend's sign on both platforms.
