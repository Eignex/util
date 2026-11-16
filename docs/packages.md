# Package contracts

Util is one Kotlin Multiplatform library. Packages group APIs by responsibility;
source sets express where those APIs are available. Adding a target does not
justify adding unrelated helpers to a package.

## Ownership and dependencies

| Package | Responsibility | Availability |
| --- | --- | --- |
| `arrays` | Primitive-array algorithms and shared empty arrays | All targets |
| `hash` | Deterministic noncryptographic hashing and key mixing | All targets |
| `collections` | Primitive containers, lookup contracts and derived collection operations | All targets |
| `collections.storage` | Explicit storage inspection, borrowed arrays and packed-word operations | All targets |
| `collections.cooperative` | Budgeted, cancellable or yielding collection algorithms | All targets |
| `concurrent` | Synchronous locks, cells, additive counters and joined parallel execution | All targets; `parallelRun` requires JVM or Native |
| `io` | Forward-only text consumption and UTF-8 file adapters | Text sources on all targets; files on JVM and POSIX Native |
| `numbers` | Checked and saturating arithmetic, a mutable 128-bit accumulator, backend integers and exact fractions | Fixed-width arithmetic on all targets; `BigInt` and `BigFraction` on JVM and Native |
| `random` | Seeded streams, distribution samplers and Sobol sequences | All targets |
| `runtime` | Runtime reference-identity operations | JVM and Native |
| `runtime.diagnostics` | Explicit heap and compiler diagnostics with optional support | JVM and Native; unavailable Native measurements return null |
| `process` | Process configuration, standard streams and termination | JVM and Native |

`arrays` and `hash` are independent foundations. Collections depend on them,
never the reverse. `random` consumes hashing and cell contracts; foundational packages
do not depend on random generators. `collections.internals` owns table and packed-bit kernels;
its types and functions remain internal. Public packages outside the collections
family must not import collection internals.

`collections.storage` and `collections.cooperative` depend on the containers.
Core containers do not depend on these optional API layers. Internal accessors
and publication kernels let those layers reuse storage without wrapper objects,
copying arrays or reimplementing container invariants.

The other top-level packages have independent responsibilities. Avoid a generic
`platform`, `helpers` or `misc` package. Put platform implementations in the source
set of the API they implement, keeping the same semantic package.

## Arrays

Algorithms operate on primitive arrays or integer index ranges. Arguments and
results express whether memory is allocated, borrowed or mutated. Shared empty
arrays belong here; callers must not rely on their identity.

Argsort is stable: equal keys retain input-index order. The buffered overload
may return either supplied buffer. Always use its returned reference and consume
only `0 until n`; both buffers are scratch storage, with their suffixes preserved.

Binary search requires ascending input and a valid half-open range. It returns
the first matching index, or `-(insertion point) - 1`. Comparator and key callbacks
must remain consistent throughout an operation.

## Collections

Containers use primitive storage and avoid generic boxing in their primitive
traversal APIs. They are not thread-safe. Read-only interfaces restrict mutation
through that reference; they do not promise immutable underlying state.

- Lists preserve append and insertion order. `removeAtSwap` and `removeValueSwap`
  explicitly replace a removed element with the tail; they do not preserve order.
- Deques preserve sequence order, including `removeAt`, which shifts elements.
- Hash sets and maps have unspecified traversal order. Dense-id sets restrict
  membership to the id range documented by their constructor.
- Integer-keyed lookup interfaces expose missing values per call. A stored value
  may equal the default; use `containsKey` when presence matters. Immutable map
  builders retain the last value for duplicate keys and own their resulting storage.
- Object maps may store null. `get` cannot distinguish a missing key from a stored
  null; `containsKey` can, and `getValue` throws only for absence. `getOrPut` replaces
  null values, matching its documented contract.
- Fixed and dynamic bitsets have a logical size independent of cardinality and
  backing capacity. Fixed bit indices must lie within that size. Dynamic writes
  can extend it; clearing beyond it does not grow it. Bitwise operations require
  equal logical sizes. No cross-implementation equality contract is implied.
- Heaps, union-find and swap sets belong here as primitive data structures. Their
  dense-id ranges, priority rules and mutation costs are part of their own contracts.

Mutating a container during traversal is unsupported. Snapshot methods return
independent arrays. `clear` normally retains capacity; explicitly named `release`
operations relinquish owned storage. Constructor capacity is a reservation in
logical elements or entries, not a promise about physical array length.

Derived queries such as `valueAtMaxKey` and compound convenience operations such
as `removeKeysAbove` are extensions in `MapOperations.kt`. Core members own the
container's invariants and primitive mutations.

### Storage inspection

Import `collections.storage` deliberately when inspecting representation:

- `primitiveCapacityBytes` counts primitive backing payload capacity, including
  unused capacity, with one byte per Boolean occupancy slot. It excludes object
  headers, alignment, reference slots, referenced objects and shared empty storage.
  It is a portable accounting model, not an exact runtime heap measurement.
- `referenceCapacity` counts object-array slots. Runtime reference width and the
  ownership of referenced objects must be accounted for separately.
- `valueStorageIdentity` borrows the backing array as an identity token. Use
  reference comparison only; do not cast or mutate it. Growth invalidates a
  previously observed storage identity.
- `forEachSlot` and `forEachValueSlot` expose physical slots, including unused ones.
  Their order and capacity may change after mutation.
- `borrowBackingArray` explicitly lends mutable list storage. Only `0 until size`
  contains live elements. Growth or trimming can replace it; acquire it again
  after mutation. This API does not transfer ownership.
- `Bits` word access masks padding on writes. `PackedBits` instead operates on
  caller-owned `LongArray` words without a logical size or automatic padding mask.
- `hashTableCapacity` describes this library's current half-load table layout.
  Storage layout and accounting can change even when collection semantics do not.

These queries avoid allocating diagnostic snapshots. Ordinary container use does
not require a storage-inspection abstraction or extra per-instance state.

### Cooperative operations

`collections.cooperative` holds work/byte-limited lookup and insertion, funded map
updates, and `prepareIntSwapSet`. Exhaustion and cancellation preserve the published
container state where documented. Preparation may allocate temporary storage before
it can publish a result; retained and scratch limits have distinct meanings.

Callbacks must not mutate or re-enter the container. Work units describe these
algorithms, not wall-clock deadlines. Yielding construction emits null at progress
boundaries and exactly one completed set as its final value. It does not promise
that allocation itself is interruptible.

## Concurrency

`CounterMode` creates additive counters with only `add` and `load`. Concurrent
striped reads can be approximate; no operation combines an add with an exact
returned result. JVM adders normalize negative zero, and floating-point totals
depend on accumulation order. Integer addition wraps on overflow.

`CellMode` also supports exact replacement, update results and compare-and-set.
`AtomicCells` operations are linearizable on JVM and Native, independently per
array slot. `SerialCells` requires confinement or external synchronization.
JS and Wasm implementations operate within one execution agent and do not
synchronize separate workers. Reading several slots is not an atomic snapshot.

Double-cell compare-and-set uses raw bits, including NaN payloads and signed zero.
Zero additions preserve those bits. Reference cells compare identity. These
contracts do not make a compound sequence of separate operations atomic.

Locks are synchronous and non-reentrant. No-op locks require caller-owned or
otherwise synchronized state. `parallelRun` owns and joins its workers, returns
results in input order and waits for started tasks before reporting failure; it
is a host-only execution helper rather than a coroutine scheduler.

## Text input

A `CharSource` is a single-consumer, forward-only resource. `next` returns a
nonempty chunk or null; chunk boundaries carry no token, line or Unicode
code-point boundary guarantee. Every source is closeable. Closing is idempotent,
releases owned resources and makes later reads return null, including in-memory
sources.

The caller owns the source. Use `useSource` or `use` for both complete reads and
early exits. `readText`, `lineSequence` and `CharReader` borrow it; they do not
take ownership. Consume lazy lines inside the owning scope. A reader's peek
results are UTF-16 character codes, with -1 indicating end of input.

File adapters decode UTF-8. Compression is selected by the final suffix and may
require a system decompressor. JVM input streams; POSIX Native currently buffers
before decoding. A common interface does not imply identical memory use.

## Numbers

Checked Long helpers throw `CheckedLongOverflowException` rather than wrap;
the `OrNull` variants return null on overflow.
Saturating helpers clamp overflow to the signed range. Host integer division
helpers explicitly select floor or ceiling rounding; `BigFraction` represents
normalized exact rational values over the existing backend integers.
`Int128` is a mutable accumulator, not a general immutable numeric value. Its
overflow latch invalidates the result until reset; quotient helpers describe
rounding, range and null-result behavior individually.

`BigInt` remains a direct backend typealias for performance and interoperability.
It does not encapsulate backend members. Shared adapters normalize supported
operations where possible, but backend members take precedence over extensions.
In particular, use `remainderOf` for the shared truncating-remainder contract.
Do not add wrappers or imply that every exposed backend operation has portable
semantics. Backend-specific operations belong in target-specific consuming code.

## Random sampling

`CounterRandom` owns draw and child-stream counters. Its `CellMode` determines
confinement or synchronization; concurrent draws are assigned according to thread
scheduling, and reset requires exclusive access. Child-seed generation is separate
from draw position. These generators are not cryptographically secure.

Distribution samplers consume the caller's `Random` without adding shared mutable
stream state. `SobolSequence` owns its mutable position and scratch storage and is
single-threaded; its scrambling, dimension projection and finite period are part
of its documented sequence contract. Keep numerical tables and their license
resources with this package rather than mixing them into general hashing helpers.

## Hashing

Hashes are deterministic and noncryptographic. `hash64` has stable output across
platforms and library versions, with UTF-8 string encoding and fixed little-endian
byte assembly. Changing its output requires an explicit compatibility decision;
keep golden-vector coverage. It is not Kotlin's object `hashCode` contract.

Fibonacci mixers produce table hashes; they own no table capacity or probing
policy. Collections consume them from `hash`. `runtime.identityHash` instead
tracks a reference during its lifetime in one process and is not a persistent
content hash or a unique object id.

## Runtime and process

Runtime identity is independent of an object's overridden equality or hash code.
Diagnostics are explicitly opt-in: heap sampling can trigger garbage collection,
peak sampling starts a process-lifetime JVM daemon, and unavailable measurements
return null. These are diagnostic readings, not allocation accounting.

Process helpers own process-wide effects. Configuration lookup checks a JVM
property first, then an environment name uppercased with dots replaced by
underscores. Standard-error output, standard-output flushing and termination stay
in `process`; they do not belong in text-source adapters or runtime diagnostics.

## API migration

This cleanup intentionally changes public source and binary APIs. Update imports
and rebuild consumers together with the library; the old names are not retained
as a second public API surface.

| Previous API | Replacement |
| --- | --- |
| Primitive list `removeAt` / `removeValue` | `removeAtSwap` / `removeValueSwap` |
| Collection `allocatedBytes` | Import `collections.storage.primitiveCapacityBytes` |
| Public map storage properties / slot traversal | Import matching extensions from `collections.storage` |
| `IntArrayList.backingData` | Import and call `collections.storage.borrowBackingArray` |
| `Bits` word access and `wordCount` | Import matching extensions from `collections.storage` |
| Raw-array helpers on `Bits` | `collections.storage.PackedBits` |
| `collections.hashTableCapacity` | `collections.storage.hashTableCapacity` |
| `MutableIntObjectMap` derived helpers | Import `collections.removeKeysAbove` / `collections.valueAtMaxKey` |
| Budgeted Long-set and funded map members | Import matching extensions from `collections.cooperative` |
| `IntSwapSet.preparation` | `collections.cooperative.prepareIntSwapSet` |
| Counter `store` / `addAndGet` | Use a `CellMode` when exact replacement or update results are required |
| `platform.identityHash` | `runtime.identityHash` |
| Other `platform` diagnostics | `runtime.diagnostics` |
| Custom `CharSource` implementations | Implement idempotent `close` and stop reading after closure |

For an approximate add-then-read on a counter, call `add` followed by `load` and
accept that concurrent additions can be included. An exact cell update requires
`AtomicCells`, or `SerialCells` under the caller's synchronization.
