package com.eignex.util.collections

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class DynamicBitsTest {
    @Test
    fun `initial capacity does not create logical bits`() {
        for (capacity in intArrayOf(0, 1, 63, 64, 65, 130)) {
            val bits = DynamicBits(capacity)

            assertEquals(0, bits.size)
            assertEquals(0, bits.cardinality())
            assertContentEquals(booleanArrayOf(), bits.toBooleanArray())
            assertContentEquals(intArrayOf(), bits.toIntArray())
        }
    }

    @Test
    fun `setting a distant bit grows with false gaps and preserves existing flags`() {
        val bits = DynamicBits()
        bits.set(0)

        bits.set(129)

        assertEquals(130, bits.size)
        assertContentEquals(BooleanArray(130) { it == 0 || it == 129 }, bits.toBooleanArray())
    }

    @Test
    fun `assigning false extends logical size`() {
        val bits = DynamicBits()
        bits.set(63)

        bits[129] = false

        assertEquals(130, bits.size)
        assertContentEquals(BooleanArray(130) { it == 63 }, bits.toBooleanArray())
    }

    @Test
    fun `appending retains trailing false values across word boundaries`() {
        val bits = DynamicBits()

        repeat(130) { bits.add(it == 0 || it == 63 || it == 64) }

        assertEquals(130, bits.size)
        assertContentEquals(BooleanArray(130) { it == 0 || it == 63 || it == 64 }, bits.toBooleanArray())
    }

    @Test
    fun `reserving bits returns the first appended index and preserves existing flags`() {
        val bits = DynamicBits()
        assertEquals(0, bits.reserve(65))
        bits.set(64)

        val start = bits.reserve(65)

        assertEquals(65, start)
        assertEquals(130, bits.size)
        assertContentEquals(BooleanArray(130) { it == 64 }, bits.toBooleanArray())
        assertEquals(130, bits.reserve(0))
        assertEquals(130, bits.size)
    }

    @Test
    fun `discarded suffix bits remain false when regrown`() {
        for (size in intArrayOf(0, 1, 63, 64, 65, 127, 128, 129)) {
            val bits = DynamicBits()
            bits.setRange(0, 200)

            bits.resize(size)
            bits.resize(200)

            assertContentEquals(BooleanArray(200) { it < size }, bits.toBooleanArray())
        }
    }

    @Test
    fun `reset discards flags before storage is reused`() {
        val bits = DynamicBits()
        bits.setRange(0, 130)

        bits.reset()
        assertEquals(0, bits.size)
        assertEquals(0, bits.cardinality())
        bits.reserve(200)

        assertContentEquals(BooleanArray(200), bits.toBooleanArray())
    }

    @Test
    fun `clearing beyond the logical range does not grow`() {
        val bits = DynamicBits()
        bits.set(64)

        bits.clear(129)
        bits.clearRange(100, 200)
        bits.clearRange(0, 64)

        assertEquals(65, bits.size)
        assertContentEquals(intArrayOf(64), bits.toIntArray())
    }

    @Test
    fun `range setting grows but an empty range does not extend size`() {
        val bits = DynamicBits()

        bits.setRange(63, 129)
        bits.setRange(200, 200)

        assertEquals(129, bits.size)
        assertContentEquals(BooleanArray(129) { it >= 63 }, bits.toBooleanArray())
    }

    @Test
    fun `copy preserves false suffixes and owns independent storage`() {
        val original = DynamicBits(256)
        original.reserve(130)
        original.set(64)

        val copy = original.copy()
        original.reset()
        copy.set(0)

        assertEquals(0, original.size)
        assertEquals(130, copy.size)
        assertContentEquals(BooleanArray(130) { it == 0 || it == 64 }, copy.toBooleanArray())
    }

    @Test
    fun `reads reject indices outside the logical range even within capacity`() {
        val bits = DynamicBits(200)
        bits.reserve(65)

        assertFailsWith<IndexOutOfBoundsException> { bits[-1] }
        assertFailsWith<IndexOutOfBoundsException> { bits[65] }
        assertFailsWith<IndexOutOfBoundsException> { bits[129] }
        assertFalse(bits[64])
    }

    @Test
    fun `invalid growth requests leave existing values intact`() {
        val bits = DynamicBits().apply { set(64) }

        assertFailsWith<IllegalArgumentException> { DynamicBits(-1) }
        assertFailsWith<IllegalArgumentException> { bits.resize(-1) }
        assertFailsWith<IllegalArgumentException> { bits.reserve(-1) }
        assertFailsWith<IllegalArgumentException> { bits.reserve(Int.MAX_VALUE) }
        assertFailsWith<IllegalArgumentException> { bits.set(-1) }
        assertFailsWith<IllegalArgumentException> { bits.set(Int.MAX_VALUE) }
        assertFailsWith<IllegalArgumentException> { bits[-1] = false }
        assertFailsWith<IllegalArgumentException> { bits[Int.MAX_VALUE] = false }
        assertFailsWith<IllegalArgumentException> { bits.clear(-1) }
        assertFailsWith<IllegalArgumentException> { bits.setRange(-1, 65) }
        assertFailsWith<IllegalArgumentException> { bits.setRange(65, 64) }
        assertFailsWith<IllegalArgumentException> { bits.clearRange(-1, 65) }
        assertFailsWith<IllegalArgumentException> { bits.clearRange(65, 64) }
        assertContentEquals(intArrayOf(64), bits.toIntArray())
        assertEquals(65, bits.size)
    }

    @Test
    fun `inline traversal supports a nonlocal return`() {
        val bits = DynamicBits().apply {
            set(129)
            set(63)
        }
        fun first(): Int {
            bits.forEachSet { return it }
            return -1
        }

        assertEquals(63, first())
    }

    @Test
    fun `mixed updates agree with a boolean sequence through growth and reuse`() {
        val random = Random(42)
        val bits = DynamicBits()
        val expected = mutableListOf<Boolean>()

        repeat(400) {
            when (random.nextInt(8)) {
                0 -> {
                    val value = random.nextBoolean()
                    bits.add(value)
                    expected.add(value)
                }

                1 -> {
                    val count = random.nextInt(5)
                    assertEquals(expected.size, bits.reserve(count))
                    repeat(count) { expected.add(false) }
                }

                2 -> {
                    val index = random.nextInt(260)
                    val value = random.nextBoolean()
                    bits[index] = value
                    while (expected.size <= index) expected.add(false)
                    expected[index] = value
                }

                3 -> {
                    val index = random.nextInt(260)
                    bits.clear(index)
                    if (index < expected.size) expected[index] = false
                }

                4 -> {
                    val size = random.nextInt(260)
                    bits.resize(size)
                    while (expected.size > size) expected.removeAt(expected.lastIndex)
                    while (expected.size < size) expected.add(false)
                }

                5, 6 -> {
                    val from = random.nextInt(260)
                    val to = random.nextInt(from, 261)
                    if (random.nextBoolean()) {
                        bits.setRange(from, to)
                        if (from < to) while (expected.size < to) expected.add(false)
                        for (i in from until to) expected[i] = true
                    } else {
                        bits.clearRange(from, to)
                        for (i in from until minOf(to, expected.size)) expected[i] = false
                    }
                }

                else -> {
                    bits.reset()
                    expected.clear()
                }
            }

            assertEquals(expected.size, bits.size)
            assertContentEquals(expected.toBooleanArray(), bits.toBooleanArray())
            assertContentEquals(expected.indices.filter { expected[it] }.toIntArray(), bits.toIntArray())
            assertEquals(expected.count { it }, bits.cardinality())
        }
    }
}
