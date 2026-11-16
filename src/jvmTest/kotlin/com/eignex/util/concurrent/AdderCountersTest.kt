package com.eignex.util.concurrent

import kotlin.test.Test
import kotlin.test.assertEquals
private const val DELTA = 1e-9

class AdderCountersTest {

    @Test
    fun `DoubleAdder add accumulates`() {
        val d = AdderCounters.newDouble(0.0)
        d.add(1.5)
        d.add(2.5)
        assertEquals(4.0, d.load(), DELTA)
    }

    @Test
    fun `LongAdder round trips and accumulates`() {
        val l = AdderCounters.newLong(0L)
        l.add(3L)
        l.add(7L)
        assertEquals(10L, l.load())
        l.add(5L)
        assertEquals(15L, l.load())
    }

    @Test
    fun `concurrent DoubleAdder writes sum exactly`() {
        val d = AdderCounters.newDouble(0.0)
        val threads = 8
        val iters = 10_000
        runConcurrently(threads, iters) { _, _ ->
            d.add(1.0)
        }
        assertEquals((threads * iters).toDouble(), d.load(), DELTA)
    }

    @Test
    fun `concurrent LongAdder writes sum exactly`() {
        val l = AdderCounters.newLong(0L)
        val threads = 8
        val iters = 10_000
        runConcurrently(threads, iters) { _, _ ->
            l.add(1L)
        }
        assertEquals((threads * iters).toLong(), l.load())
    }
}
