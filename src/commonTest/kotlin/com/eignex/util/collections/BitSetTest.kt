package com.eignex.util.collections

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BitSetTest {
    @Test
    fun `common indexed updates preserve logical size and snapshots`() {
        for (bits in listOf<BitSet>(Bits(130), DynamicBits().apply { resize(130) })) {
            bits.set(0)
            bits.set(63)
            bits[64] = true
            bits.set(129)
            bits[64] = false

            assertEquals(130, bits.size)
            assertEquals(3, bits.cardinality())
            assertContentEquals(intArrayOf(0, 63, 129), bits.toIntArray())
            assertContentEquals(BooleanArray(130) { it == 0 || it == 63 || it == 129 }, bits.toBooleanArray())
        }
    }

    @Test
    fun `range updates preserve bits outside half open boundaries`() {
        val ranges = listOf(0 to 0, 0 to 64, 1 to 63, 63 to 65, 64 to 128, 65 to 130, 0 to 130, 130 to 130)
        for ((from, to) in ranges) {
            for (bits in listOf<BitSet>(Bits(130), DynamicBits().apply { resize(130) })) {
                bits.setRange(from, to)
                assertContentEquals(BooleanArray(130) { it in from until to }, bits.toBooleanArray())

                bits.setRange(0, bits.size)
                bits.clearRange(from, to)
                assertContentEquals(BooleanArray(130) { it !in from until to }, bits.toBooleanArray())
            }
        }
    }

    @Test
    fun `clearing flags preserves logical size and returns independent snapshots`() {
        for (bits in listOf<BitSet>(Bits(65), DynamicBits().apply { resize(65) })) {
            bits.setRange(0, bits.size)
            val snapshot = bits.toBooleanArray()

            bits.clear()

            assertEquals(65, bits.size)
            assertEquals(0, bits.cardinality())
            assertContentEquals(BooleanArray(65), bits.toBooleanArray())
            assertContentEquals(BooleanArray(65) { true }, snapshot)
        }
    }

    @Test
    fun `interface traversal visits only set indices in ascending order`() {
        for (bits in listOf<BitSet>(Bits(130), DynamicBits().apply { resize(130) })) {
            for (index in intArrayOf(129, 64, 63, 0)) bits.set(index)
            val visited = IntArrayList()

            bits.forEachSet(IntConsumer { visited.add(it) })

            assertContentEquals(intArrayOf(0, 63, 64, 129), visited.toIntArray())
        }
    }

    @Test
    fun `mixed representations copy all bits without sharing storage`() {
        for (target in listOf<BitSet>(Bits(130), DynamicBits().apply { resize(130) })) {
            for (source in listOf<BitSet>(Bits(130), DynamicBits().apply { resize(130) })) {
                target.setRange(0, target.size)
                source.set(64)

                target.copyFrom(source)
                source.clear()

                assertContentEquals(intArrayOf(64), target.toIntArray())
                assertEquals(130, target.size)
            }
        }
    }

    @Test
    fun `mixed representations support subset checks`() {
        for (bits in listOf<BitSet>(Bits(130), DynamicBits().apply { resize(130) })) {
            for (other in listOf<BitSet>(Bits(130), DynamicBits().apply { resize(130) })) {
                bits.setRange(0, 65)
                other.set(64)

                assertTrue(bits.containsAll(other))
                assertFalse(other.containsAll(bits))
                assertTrue(bits.containsAll(bits))
            }
        }
    }

    @Test
    fun `mixed representations combine bit values through the interface`() {
        val operations: List<Pair<(BitSet, BitSet) -> Unit, IntArray>> = listOf(
            { a: BitSet, b: BitSet -> a.andInPlace(b) } to intArrayOf(64),
            { a: BitSet, b: BitSet -> a.andNotInPlace(b) } to intArrayOf(0, 129),
            { a: BitSet, b: BitSet -> a.orInPlace(b) } to intArrayOf(0, 63, 64, 129),
        )
        for ((combine, expected) in operations) {
            for (bits in listOf<BitSet>(Bits(130), DynamicBits().apply { resize(130) })) {
                for (other in listOf<BitSet>(Bits(130), DynamicBits().apply { resize(130) })) {
                    bits.clear()
                    for (index in intArrayOf(0, 64, 129)) bits.set(index)
                    for (index in intArrayOf(63, 64)) other.set(index)

                    combine(bits, other)

                    assertContentEquals(expected, bits.toIntArray())
                    assertEquals(130, bits.size)
                }
            }
        }
    }

    @Test
    fun `combining bitsets rejects different logical sizes`() {
        for (bits in listOf<BitSet>(Bits(65), DynamicBits().apply { resize(65) })) {
            val other: BitSet = Bits(64)

            assertFailsWith<IllegalArgumentException> { bits.copyFrom(other) }
            assertFailsWith<IllegalArgumentException> { bits.containsAll(other) }
            assertFailsWith<IllegalArgumentException> { bits.andInPlace(other) }
            assertFailsWith<IllegalArgumentException> { bits.andNotInPlace(other) }
            assertFailsWith<IllegalArgumentException> { bits.orInPlace(other) }
        }
    }

    @Test
    fun `default methods work with a custom bitset implementation`() {
        val values = BooleanArray(65)
        val bits = object : BitSet {
            override val size: Int = values.size
            override fun get(i: Int): Boolean = values[i]
            override fun set(i: Int) {
                values[i] = true
            }
            override fun clear(i: Int) {
                values[i] = false
            }
        }

        bits.setRange(0, 65)
        bits.clearRange(1, 64)
        bits[0] = false

        assertEquals(1, bits.cardinality())
        assertContentEquals(intArrayOf(64), bits.toIntArray())
        assertContentEquals(BooleanArray(65) { it == 64 }, bits.toBooleanArray())
        bits.clear()
        assertContentEquals(BooleanArray(65), values)
    }
}
