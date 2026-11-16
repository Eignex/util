package com.eignex.util.random

import com.eignex.util.concurrent.AtomicCells
import com.eignex.util.concurrent.SerialCells
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CounterRandomTest {
    @Test
    fun `seeded draws retain their reference sequence`() {
        val random = CounterRandom(17L)

        assertContentEquals(
            intArrayOf(-900074886, 1367665405, -1318073829, -737360537, -183547061, 846590545, 270817494, 68149051),
            IntArray(8) { random.nextInt() },
        )
    }

    @Test
    fun `storage modes produce the same seeded draw sequence`() {
        val serial = CounterRandom(17L)
        val atomic = CounterRandom(17L, AtomicCells)

        assertContentEquals(IntArray(32) { serial.nextInt() }, IntArray(32) { atomic.nextInt() })
    }

    @Test
    fun `reset replays draws`() {
        for (mode in listOf(SerialCells, AtomicCells)) {
            val random = CounterRandom(41L, mode)
            val expected = IntArray(32) { random.nextInt() }

            random.reset()

            assertContentEquals(expected, IntArray(32) { random.nextInt() })
        }
    }

    @Test
    fun `draws and resets do not restart child seeds`() {
        for (mode in listOf(SerialCells, AtomicCells)) {
            val random = CounterRandom(17L, mode)
            val reference = CounterRandom(17L, mode)
            assertEquals(reference.childSeed(), random.childSeed())

            repeat(32) { random.nextInt() }
            random.reset()

            assertEquals(reference.childSeed(), random.childSeed())
        }
    }

    @Test
    fun `child seeds depend on parent seed and ordinal`() {
        assertNotEquals(deriveChildSeed(17L, 1L), deriveChildSeed(17L, 2L))
        assertNotEquals(deriveChildSeed(17L, 1L), deriveChildSeed(18L, 1L))
    }

    @Test
    fun `child seeds retain their reference sequence`() {
        val random = CounterRandom(17L)

        assertContentEquals(
            longArrayOf(4703473720624924466L, 7659275002808998026L, -8042665077451066992L),
            LongArray(3) { random.childSeed() },
        )
    }

    @Test
    fun `zero bit draws return zero`() {
        assertEquals(0, CounterRandom(17L).nextBits(0))
    }
}
