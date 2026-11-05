package com.eignex.util.platform

/**
 * Start process-lifetime peak-heap sampling on a daemon JVM thread. Repeated calls are a no-op.
 * Native targets have no comparable accounting and leave sampling disabled.
 */
expect fun startHeapPeakSampler()

/**
 * Collect and sample the JVM heap, or return null when comparable accounting is unavailable.
 * This can trigger garbage collection and is intended for explicit diagnostics.
 * Peak readings include uncollected objects and depend on the configured heap ceiling.
 */
expect fun sampleHeap(): HeapSample?

/** Cumulative JIT compiler time in milliseconds, or null when unsupported. */
expect fun compilationTimeMillis(): Long?
