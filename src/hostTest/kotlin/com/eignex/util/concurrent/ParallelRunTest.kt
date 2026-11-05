@file:Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction", "FunctionNaming")

package com.eignex.util.concurrent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class ParallelRunTest {
    @Test
    fun `empty input returns no results`() {
        assertEquals(emptyList(), parallelRun<Int>(emptyList()))
    }

    @Test
    fun `results preserve task order and nullable values`() {
        val result = parallelRun(listOf({ "first" }, { null }, { "last" }))

        assertEquals(listOf("first", null, "last"), result)
    }

    @Test
    fun `multiple tasks can be active together`() {
        val arrived = AtomicCells.newLong()
        val started = TimeSource.Monotonic.markNow()

        val result = parallelRun(
            List(2) {
                {
                    arrived.add(1L)
                    while (arrived.load() != 2L) {
                        check(started.elapsedNow() < 5.seconds) { "The tasks did not overlap" }
                    }
                    true
                }
            },
        )

        assertTrue(result.all { it })
    }

    @Test
    fun `a task failure is rethrown after other tasks finish`() {
        val finished = AtomicCells.newLong()

        val failure = assertFailsWith<IllegalArgumentException> {
            parallelRun(
                listOf(
                    { throw IllegalArgumentException("first") },
                    { finished.add(1L) },
                    { throw IllegalStateException("later") },
                ),
            )
        }

        assertEquals("first", failure.message)
        assertEquals(1L, finished.load())
    }

    @Test
    fun `a single task returns its result`() {
        assertEquals(listOf(42), parallelRun(listOf({ 42 })))
    }
}
