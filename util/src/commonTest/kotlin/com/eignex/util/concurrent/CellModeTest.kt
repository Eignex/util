package com.eignex.util.concurrent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CellModeTest {
    @Test
    fun `only accumulation and observation are required by the counter contract`() {
        val counter: LongCounter = object : LongCounter {
            private var total = 0L
            override fun add(delta: Long) {
                total += delta
            }
            override fun load(): Long = total
        }
        counter.add(7L)
        assertEquals(7L, counter.load())
    }

    @Test
    fun `exact cells preserve replacement bits and return their own update result`() {
        for (mode in listOf(SerialCells, AtomicCells)) {
            val cell = mode.newDouble()
            for (value in listOf(
                1e20,
                -1e20,
                1.0,
                -0.0,
                Double.POSITIVE_INFINITY,
                Double.fromBits(0x7ff8000000000042L),
            )) {
                cell.store(value)
                assertEquals(value.toRawBits(), cell.load().toRawBits())
                assertEquals(value.toRawBits(), cell.addAndGet(0.0).toRawBits())
                assertEquals(value.toRawBits(), cell.addAndGet(-0.0).toRawBits())
            }
            cell.store(1.0)
            assertEquals(3.5, cell.addAndGet(2.5))
            val integer = mode.newLong(Long.MAX_VALUE)
            assertEquals(Long.MIN_VALUE, integer.addAndGet(1L))
            integer.store(3L)
            assertEquals(3L, integer.load())
        }
    }

    @Test
    fun `zero additions preserve negative zero in scalars and arrays`() {
        for (mode in listOf(SerialCells, AtomicCells)) {
            val scalar = mode.newDouble(-0.0)
            val array = mode.newDoubleArray(1) { -0.0 }

            scalar.add(0.0)
            array.add(0, 0.0)

            assertEquals((-0.0).toRawBits(), scalar.load().toRawBits())
            assertEquals((-0.0).toRawBits(), array.load(0).toRawBits())
        }
    }

    @Test
    fun `double compare and set distinguishes signed zeros`() {
        for (mode in listOf(SerialCells, AtomicCells)) {
            val cell = mode.newDouble(-0.0)

            assertFalse(cell.compareAndSet(0.0, 1.0))
            assertTrue(cell.compareAndSet(-0.0, 2.0))
            assertEquals(2.0, cell.load())
        }
    }

    @Test
    fun `double array updates leave neighboring slots intact`() {
        for (mode in listOf(SerialCells, AtomicCells)) {
            val cells = mode.newDoubleArray(3) { it.toDouble() }

            cells.add(1, 4.0)
            assertTrue(cells.compareAndSet(1, 5.0, 7.0))

            assertEquals(0.0, cells.load(0))
            assertEquals(7.0, cells.load(1))
            assertEquals(2.0, cells.load(2))
        }
    }

    @Test
    fun `adder counters are available on every target`() {
        val counter = AdderCounters.newLong(3L)

        counter.add(4L)

        assertEquals(7L, counter.load())
    }
}
