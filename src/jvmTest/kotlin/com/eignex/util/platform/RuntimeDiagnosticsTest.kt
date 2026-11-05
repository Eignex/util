package com.eignex.util.platform

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RuntimeDiagnosticsTest {
    @Test
    fun `a heap sample reports live and committed bytes`() {
        val sample = assertNotNull(sampleHeap())

        assertTrue(sample.retainedBytes > 0L)
        assertTrue(sample.committedBytes >= sample.retainedBytes)
    }

    @Test
    fun `starting the sampler repeatedly retains a peak reading`() {
        startHeapPeakSampler()
        startHeapPeakSampler()

        val sample = assertNotNull(sampleHeap())

        assertTrue(assertNotNull(sample.peakBytes) >= sample.retainedBytes)
    }

    @Test
    fun `compilation counters are nonnegative when supported`() {
        compilationTimeMillis()?.let { assertTrue(it >= 0L) }
    }
}
