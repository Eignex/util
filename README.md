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

- Primitive collections: lists, deques, sets, maps, and fixed-size or growable packed bitsets.
- Array algorithms: stable argsort and primitive binary search.
- Concurrency primitives: synchronous locks, atomic cells and additive counters.
- Numeric primitives: checked Long arithmetic, a signed 128-bit accumulator and arbitrary-precision integers.
- Hashing: deterministic SplitMix64 mixing and 64-bit byte/string hashes.
- Text input: chunked character sources, buffered character reading and compressed file input.
- Host concurrency: ordered parallel execution on JVM threads and Native workers.
- Process support: properties/environment, standard output flushing, error output and process exit.
- Runtime diagnostics: reference identity hashing, JVM heap samples and cumulative JIT compilation time.
