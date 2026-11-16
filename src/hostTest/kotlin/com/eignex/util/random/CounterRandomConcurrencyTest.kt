@file:Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction", "FunctionNaming")

package com.eignex.util.random

import com.eignex.util.concurrent.AtomicCells
import com.eignex.util.concurrent.parallelRun
import kotlin.test.Test
import kotlin.test.assertContentEquals

class CounterRandomConcurrencyTest {
    @Test
    fun `concurrent draws preserve every serial draw`() {
        val random = CounterRandom(17L, AtomicCells)
        val serial = CounterRandom(17L)

        val draws = parallelRun(List(4) { { IntArray(256) { random.nextInt() } } })

        val actual = draws.flatMap { it.toList() }.sorted()
        val expected = List(1024) { serial.nextInt() }.sorted()
        assertContentEquals(expected, actual)
    }

    @Test
    fun `concurrent child seeds preserve every serial seed`() {
        val random = CounterRandom(17L, AtomicCells)
        val serial = CounterRandom(17L)

        val seeds = parallelRun(List(4) { { LongArray(256) { random.childSeed() } } })

        val actual = seeds.flatMap { it.toList() }.sorted()
        val expected = List(1024) { serial.childSeed() }.sorted()
        assertContentEquals(expected, actual)
    }
}
