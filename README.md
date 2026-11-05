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

The repository contains the build scaffold and a `com.eignex.util` package stub.
Public utilities have not been extracted yet.

## Intended scope

- Primitive collections: lists, deques, sets, maps and packed bitsets.
- Array algorithms: sorting, searching and reusable temporary buffers.
- Concurrency primitives: locks, atomic cells and additive counters.
- Platform support: host identity, CPU information and native library loading.
- Numeric primitives: checked arithmetic and wider integer helpers.

Initial candidates come from [Klause](https://github.com/Eignex/klause),
[Koblas](https://github.com/Eignex/koblas) and
[Kumulant](https://github.com/Eignex/kumulant). Solver policies, BLAS engine
selection and statistical concurrency guarantees belong in their consuming libraries.

## Targets

The default build configures JVM and Linux x64. `-Ptargets.full` also enables
JavaScript, Wasm JS, Wasm WASI, Linux ARM64, macOS ARM64, Windows x64 and iOS
(x64, ARM64 and ARM64 simulator). Target execution depends on the host platform.

The library uses Java 17. Gradle runs on Java 21 or later and provisions the
compilation toolchain when needed.

## Development

Run the default checks and documentation lint:

```sh
./gradlew check lintDocs
```

Validate the full target configuration on the current host:

```sh
./gradlew check lintDocs -Ptargets.full
```

The build follows [Kbuild](https://github.com/Eignex/kbuild) conventions, using
the setup from [KPermute](https://github.com/Eignex/kpermute). CI checks JVM
and Linux x64 by default; release validation also covers macOS, iOS simulator
and Windows. Successful builds on `main` trigger snapshot publication, and
version tags trigger release validation and publication.
