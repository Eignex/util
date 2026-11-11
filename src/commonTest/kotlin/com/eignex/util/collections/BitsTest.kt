package com.eignex.util.collections

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BitsTest {
    @Test
    fun `packed word writes clear final padding`() {
        for (size in intArrayOf(1, 63, 64, 65, 130)) {
            val bits = Bits(size)

            for (word in 0 until bits.wordCount) bits.setWord(word, -1L)

            assertEquals(size, bits.cardinality())
            assertContentEquals(IntArray(size) { it }, bits.toIntArray())
            assertEquals(if (size % 64 == 0) -1L else -1L ushr (64 - size % 64), bits.getWord(bits.wordCount - 1))
        }
    }

    @Test
    fun `copying a word range preserves words outside it`() {
        val source = Bits.full(130)
        val target = Bits(130)

        target.copyWordsFrom(source, 1, 2)

        assertContentEquals(IntArray(64) { it + 64 }, target.toIntArray())
        source.clear()
        assertEquals(64, target.cardinality())
    }

    @Test
    fun `set and get should round-trip across word boundaries`() {
        val b = Bits(130)
        b.set(0)
        b.set(63)
        b.set(64)
        b.set(129)
        assertTrue(b.get(0))
        assertTrue(b.get(63))
        assertTrue(b.get(64))
        assertTrue(b.get(129))
        assertFalse(b.get(1))
        assertFalse(b.get(128))
        assertEquals(4, b.cardinality())
    }

    @Test
    fun `or and andNot should combine bit sets`() {
        val a = Bits.of(10, intArrayOf(0, 2, 4, 6, 8))
        val b = Bits.of(10, intArrayOf(2, 3, 4))
        val u = a.copy().also { it.orInPlace(b) }
        assertEquals(listOf(0, 2, 3, 4, 6, 8), u.toIntArray().toList())
        val d = a.copy().also { it.andNotInPlace(b) }
        assertEquals(listOf(0, 6, 8), d.toIntArray().toList())
    }

    @Test
    fun `containsAll should follow subset semantics`() {
        val universe = Bits.full(10)
        val sub = Bits.of(10, intArrayOf(2, 5, 7))
        assertTrue(universe.containsAll(sub))
        assertFalse(sub.containsAll(universe))
        assertTrue(sub.containsAll(sub))
    }

    @Test
    fun `forEachSet should iterate in ascending order`() {
        val b = Bits.of(200, intArrayOf(199, 0, 63, 64, 100))
        val seen = mutableListOf<Int>()
        b.forEachSet { seen.add(it) }
        assertEquals(listOf(0, 63, 64, 100, 199), seen)
    }

    @Test
    fun `companion set has and clear should round-trip across words`() {
        val bits = LongArray(3)
        Bits.set(bits, 0)
        Bits.set(bits, 63)
        Bits.set(bits, 64)
        Bits.set(bits, 129)
        assertTrue(Bits.has(bits, 0))
        assertTrue(Bits.has(bits, 63))
        assertTrue(Bits.has(bits, 64))
        assertTrue(Bits.has(bits, 129))
        assertFalse(Bits.has(bits, 1))
        assertFalse(Bits.has(bits, 128))
        Bits.clear(bits, 64)
        assertFalse(Bits.has(bits, 64))
        assertTrue(Bits.has(bits, 63))
    }

    @Test
    fun `fillRange should set a half open range across word boundaries`() {
        val bits = LongArray(3)
        Bits.fillRange(bits, 61, 67)
        for (i in 0..60) assertFalse(Bits.has(bits, i), "bit $i")
        for (i in 61..66) assertTrue(Bits.has(bits, i), "bit $i")
        for (i in 67..140) assertFalse(Bits.has(bits, i), "bit $i")
    }

    @Test
    fun `clearBelow should clear only bits below the exclusive boundary`() {
        val bits = LongArray(3)
        Bits.fillRange(bits, 0, 140)
        Bits.clearBelow(bits, 65)
        for (i in 0..64) assertFalse(Bits.has(bits, i), "bit $i")
        for (i in 65..139) assertTrue(Bits.has(bits, i), "bit $i")
    }

    @Test
    fun `clearAbove should clear only bits above the inclusive boundary`() {
        val bits = LongArray(3)
        Bits.fillRange(bits, 0, 140)
        Bits.clearAbove(bits, 65)
        for (i in 0..65) assertTrue(Bits.has(bits, i), "bit $i")
        for (i in 66..139) assertFalse(Bits.has(bits, i), "bit $i")
    }

    @Test
    fun `clearAbove should clear all bits for every negative boundary`() {
        for (boundary in intArrayOf(Int.MIN_VALUE, -65, -64, -2, -1)) {
            val bits = longArrayOf(1L, Long.MIN_VALUE, 0x55L, -1L)

            Bits.clearAbove(bits, boundary)

            assertContentEquals(LongArray(bits.size), bits, "boundary $boundary")
            Bits.clearAbove(LongArray(0), boundary)
        }
    }

    @Test
    fun `clearAbove should preserve all bits when the boundary is Int MAX_VALUE`() {
        val bits = longArrayOf(1L, Long.MIN_VALUE, 0x55L, -1L)
        val expected = bits.copyOf()

        Bits.clearAbove(bits, Int.MAX_VALUE)

        assertContentEquals(expected, bits)
    }

    @Test
    fun `firstSet and lastSet should return a sentinel when empty and the extremes otherwise`() {
        val empty = LongArray(2)
        assertEquals(-1, Bits.firstSet(empty))
        assertEquals(-1, Bits.lastSet(empty))

        val bits = LongArray(3)
        Bits.set(bits, 5)
        Bits.set(bits, 73)
        Bits.set(bits, 130)
        assertEquals(5, Bits.firstSet(bits))
        assertEquals(130, Bits.lastSet(bits))
    }
}
