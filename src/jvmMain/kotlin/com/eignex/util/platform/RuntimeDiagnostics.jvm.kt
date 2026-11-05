@file:Suppress("UndocumentedPublicFunction")

package com.eignex.util.platform

import java.lang.management.ManagementFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

private const val HEAP_SAMPLE_INTERVAL_MS = 10L

private val heapSamplerRunning = AtomicBoolean(false)
private val heapPeak = AtomicLong(0)

actual fun startHeapPeakSampler() {
    if (!heapSamplerRunning.compareAndSet(false, true)) return
    val memory = ManagementFactory.getMemoryMXBean()
    Thread {
        try {
            while (true) {
                heapPeak.updateAndGet { maxOf(it, memory.heapMemoryUsage.used) }
                Thread.sleep(HEAP_SAMPLE_INTERVAL_MS)
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }.apply {
        isDaemon = true
        name = "util-heap-peak"
        priority = Thread.MIN_PRIORITY
        start()
    }
}

// The collection is the point: without it `retained` reports whatever the allocator happened to be
// holding rather than live data. This operation is for explicit diagnostics, not a computation hot path.
@Suppress("ExplicitGarbageCollectionCall")
actual fun sampleHeap(): HeapSample? {
    // Read the peak before collecting, and fold in a reading of the moment, so the window between the
    // sampler's last poll and this call is covered.
    val peak = if (heapSamplerRunning.get()) {
        heapPeak.updateAndGet { maxOf(it, ManagementFactory.getMemoryMXBean().heapMemoryUsage.used) }
    } else {
        null
    }
    System.gc()
    val runtime = Runtime.getRuntime()
    val retained = runtime.totalMemory() - runtime.freeMemory()
    // Committed is not a peak: the JVM grows the heap under pressure and rarely gives it back, so it
    // cannot establish when high-water demand arose. Pool peak marks cannot be summed because pools
    // peak at different times.
    return HeapSample(retained, runtime.totalMemory(), peak)
}

/** Cumulative compiler time, or null when the runtime does not support monitoring it. */
actual fun compilationTimeMillis(): Long? =
    ManagementFactory.getCompilationMXBean()?.takeIf { it.isCompilationTimeMonitoringSupported }?.totalCompilationTime
