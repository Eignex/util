package com.eignex.util.collections

import com.eignex.util.hash.fibonacciHash
import kotlin.test.Test
import kotlin.test.assertEquals

class PrimitiveMapBuildTest {
    @Test
    fun `sparse builds retain duplicate updates through a wrapped collision chain`() {
        // Six input entries select 16 slots; these keys all start in the final slot.
        val colliding = (0..512).filter { fibonacciHash(it) and 15 == 15 }.take(4)
        val keys = IntArray(6) { colliding[it % 3] }
        val values = intArrayOf(1, 2, 3, Int.MIN_VALUE, 0, Int.MAX_VALUE)
        val wide = longArrayOf(1, 2, 3, Long.MIN_VALUE, 0, Long.MAX_VALUE)
        val ints = IntIntMap.build(keys, values, absent = 0, denseThreshold = 0)
        val longs = IntLongMap.build(keys, wide, absent = 0L, denseThreshold = 0)

        assertEquals(3, ints.size)
        assertEquals(3, longs.size)
        for (i in colliding.indices) {
            val key = colliding[i]
            assertEquals(i < 3, ints.containsKey(key))
            assertEquals(i < 3, longs.containsKey(key))
            assertEquals(if (i < 3) values[i + 3] else 0, ints[key])
            assertEquals(if (i < 3) wide[i + 3] else 0L, longs[key])
            assertEquals(if (i < 3) values[i + 3] else -1, ints.getOrDefault(key, -1))
            assertEquals(if (i < 3) wide[i + 3] else -1L, longs.getOrDefault(key, -1L))
        }
        assertEquals(colliding.take(3).toSet(), ints.keysToIntArray().toSet())
        assertEquals(colliding.take(3).toSet(), longs.keysToIntArray().toSet())
    }

    @Test
    fun `build should count distinct keys and preserve sentinel values and holes`() {
        val keys = IntArray(513) { ((it * 7) % 17 - 8) * 2 }
        for (threshold in intArrayOf(0, 4)) {
            for (absent in intArrayOf(0, 7, -1)) {
                val values = IntArray(keys.size) { if (it % 3 == 0) absent else it }
                val wide = LongArray(keys.size) { if (it % 3 == 0) absent.toLong() else Long.MIN_VALUE + it }
                val expected = keys.indices.associate { keys[it] to values[it] }
                val expectedWide = keys.indices.associate { keys[it] to wide[it] }
                val ints = IntIntMap.build(keys, values, absent, threshold)
                val longs = IntLongMap.build(keys, wide, absent.toLong(), threshold)

                assertEquals(expected.size, ints.size)
                assertEquals(expectedWide.size, longs.size)
                for (key in -20..20) {
                    assertEquals(expected.containsKey(key), ints.containsKey(key))
                    assertEquals(expectedWide.containsKey(key), longs.containsKey(key))
                    assertEquals(expected[key] ?: absent, ints[key])
                    assertEquals(expectedWide[key] ?: absent.toLong(), longs[key])
                    assertEquals(expected[key] ?: -902, ints.getOrDefault(key, -902))
                    assertEquals(expectedWide[key] ?: -902L, longs.getOrDefault(key, -902L))
                }
            }
        }
    }

    @Test
    fun `empty and duplicate extreme-key builds should report their exact size`() {
        assertEquals(0, IntIntMap.build(intArrayOf(), intArrayOf()).size)
        assertEquals(0, IntLongMap.build(intArrayOf(), longArrayOf()).size)
        val keys = intArrayOf(Int.MAX_VALUE, 0, Int.MIN_VALUE, 0, Int.MAX_VALUE)
        val ints = IntIntMap.build(keys, intArrayOf(1, 2, 3, 4, 5))
        val longs = IntLongMap.build(keys, longArrayOf(1, 2, 3, 4, 5))
        assertEquals(3, ints.size)
        assertEquals(3, longs.size)
        assertEquals(4, ints[0])
        assertEquals(5, ints[Int.MAX_VALUE])
        assertEquals(4L, longs[0])
        assertEquals(5L, longs[Int.MAX_VALUE])
    }
}
