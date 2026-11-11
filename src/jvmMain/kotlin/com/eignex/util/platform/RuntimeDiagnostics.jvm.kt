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

@Suppress("ExplicitGarbageCollectionCall")
actual fun sampleHeap(): HeapSample? {
    // Include usage since the sampler's last poll before collecting.
    val peak = if (heapSamplerRunning.get()) {
        heapPeak.updateAndGet { maxOf(it, ManagementFactory.getMemoryMXBean().heapMemoryUsage.used) }
    } else {
        null
    }
    System.gc()
    val runtime = Runtime.getRuntime()
    val retained = runtime.totalMemory() - runtime.freeMemory()
    return HeapSample(retained, runtime.totalMemory(), peak)
}

actual fun compilationTimeMillis(): Long? =
    ManagementFactory.getCompilationMXBean()?.takeIf { it.isCompilationTimeMonitoringSupported }?.totalCompilationTime
