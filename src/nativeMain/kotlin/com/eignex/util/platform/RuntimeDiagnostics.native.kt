package com.eignex.util.platform

/** Native runtimes have no comparable heap accounting; sampling is unavailable. */
actual fun startHeapPeakSampler() = Unit

/** Native runtimes have no comparable heap accounting. */
actual fun sampleHeap(): HeapSample? = null

/** Native runtimes have no JIT compilation-time counter. */
actual fun compilationTimeMillis(): Long? = null
