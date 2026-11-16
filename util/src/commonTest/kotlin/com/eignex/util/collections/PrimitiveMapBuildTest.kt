package com.eignex.util.collections

import kotlin.test.Test
import kotlin.test.assertEquals

class PrimitiveMapBuildTest {
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
