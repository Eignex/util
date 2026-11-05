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
- Numeric primitives: checked Long arithmetic and a signed 128-bit accumulator.
- Hashing: deterministic SplitMix64 mixing and 64-bit byte/string hashes.
- Platform support: reference identity hashing on JVM and Native.

## Usage

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

Collections are mutable and require external synchronization when shared. List
removal uses swap removal, which does not preserve element order.
