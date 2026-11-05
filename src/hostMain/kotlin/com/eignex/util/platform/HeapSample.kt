package com.eignex.util.platform

/** A diagnostic heap reading; collecting a sample may trigger garbage collection. */
class HeapSample(
    /** Live heap bytes after collection. */
    val retainedBytes: Long,
    /** Heap bytes committed by the runtime. */
    val committedBytes: Long,
    /** Highest sampled heap usage since sampling started, or null when it has not started. */
    val peakBytes: Long?,
)
