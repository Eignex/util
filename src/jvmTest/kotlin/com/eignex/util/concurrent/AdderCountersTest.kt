package com.eignex.util.concurrent

import kotlin.test.Test
import kotlin.test.assertEquals
private const val DELTA = 1e-9

class AdderCountersTest {

    @Test
    fun `DoubleAdder round trips load and store`() {
        val d = AdderCounters.newDouble(0.0)
        d.store(2.5)
        assertEquals(2.5, d.load(), DELTA)
    }

    @Test
    fun `DoubleAdder add accumulates`() {
        val d = AdderCounters.newDouble(0.0)
        d.add(1.5)
        d.add(2.5)
        assertEquals(4.0, d.load(), DELTA)
    }

    @Test
    fun `DoubleAdder addAndGet returns updated value`() {
        val d = AdderCounters.newDouble(1.0)
        assertEquals(3.5, d.addAndGet(2.5), DELTA)
        assertEquals(3.5, d.load(), DELTA)
    }

    @Test
    fun `DoubleAdder store resets then assigns`() {
        val d = AdderCounters.newDouble(100.0)
        d.store(7.0)
        assertEquals(7.0, d.load(), DELTA)
    }

    @Test
    fun `LongAdder round trips and accumulates`() {
        val l = AdderCounters.newLong(0L)
        l.add(3L)
        l.add(7L)
        assertEquals(10L, l.load())
        assertEquals(15L, l.addAndGet(5L))
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

    @Test
    fun `DoubleAdder store recovers from an infinite sum`() {
        val d = AdderCounters.newDouble(Double.POSITIVE_INFINITY)
        d.store(0.0)
        assertEquals(0.0, d.load())
    }

    @Test
    fun `DoubleAdder store assigns an infinite value`() {
        val d = AdderCounters.newDouble(Double.NEGATIVE_INFINITY)
        d.store(Double.POSITIVE_INFINITY)
        assertEquals(Double.POSITIVE_INFINITY, d.load())
    }
}
