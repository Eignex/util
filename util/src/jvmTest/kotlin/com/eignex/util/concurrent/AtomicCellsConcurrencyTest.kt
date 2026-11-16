package com.eignex.util.concurrent

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AtomicCellsConcurrencyTest {
    @Test
    fun `atomic additions preserve every concurrent contribution`() {
        val scalar = AtomicCells.newDouble()
        val array = AtomicCells.newLongArray(1)

        runConcurrently(4, 1_000) { _, _ ->
            scalar.add(1.0)
            array.add(0, 1L)
        }

        assertEquals(4_000.0, scalar.load())
        assertEquals(4_000L, array.load(0))
    }

    @Test
    fun `guarded releases the lock when the block throws`() {
        val mutex = Mutex()
        val executor = Executors.newSingleThreadExecutor()
        try {
            assertFailsWith<IllegalArgumentException> {
                mutex.guarded { throw IllegalArgumentException("rejected") }
            }

            val result = executor.submit<Int> { mutex.guarded { 42 } }.get(250, TimeUnit.MILLISECONDS)

            assertEquals(42, result)
        } finally {
            executor.shutdownNow()
        }
    }
}
