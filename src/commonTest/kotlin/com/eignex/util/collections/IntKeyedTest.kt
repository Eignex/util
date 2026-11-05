package com.eignex.util.collections

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IntKeyedTest {
    @Test
    fun `interface traversal visits unique keys regardless of payload`() {
        val maps = listOf<IntKeyed>(
            MutableIntIntMap().apply {
                put(5, 0)
                put(-1, 7)
                put(5, 2)
            },
            MutableIntLongMap().apply {
                put(5, 0L)
                put(-1, 7L)
                put(5, 2L)
            },
            MutableIntDoubleMap().apply {
                put(5, 0.0)
                put(-1, 7.0)
                put(5, 2.0)
            },
            MutableIntObjectMap<String?>().apply {
                put(5, null)
                put(-1, "a")
                put(5, null)
            },
            IntIntMap.build(intArrayOf(5, -1, 5), intArrayOf(0, 7, 2)),
            IntIntMap.build(intArrayOf(5, -1, 5), intArrayOf(0, 7, 2), denseThreshold = 0),
            IntLongMap.build(intArrayOf(5, -1, 5), longArrayOf(0, 7, 2)),
            IntLongMap.build(intArrayOf(5, -1, 5), longArrayOf(0, 7, 2), denseThreshold = 0),
        )

        for (map in maps) {
            val keys = IntArrayList()
            map.forEachKey(IntConsumer { keys.add(it) })

            assertEquals(2, map.size)
            assertContentEquals(intArrayOf(-1, 5), keys.toSortedIntArray())
            assertTrue(map.containsKey(5))
            assertFalse(map.containsKey(0))
        }
    }

    @Test
    fun `sorted key snapshots are independent of the map`() {
        val maps = listOf<IntKeyed>(
            MutableIntIntMap().apply {
                put(5, 0)
                put(-1, 7)
                put(5, 2)
            },
            MutableIntLongMap().apply {
                put(5, 0L)
                put(-1, 7L)
                put(5, 2L)
            },
            MutableIntDoubleMap().apply {
                put(5, 0.0)
                put(-1, 7.0)
                put(5, 2.0)
            },
            MutableIntObjectMap<String?>().apply {
                put(5, null)
                put(-1, "a")
                put(5, null)
            },
            IntIntMap.build(intArrayOf(5, -1, 5), intArrayOf(0, 7, 2)),
            IntIntMap.build(intArrayOf(5, -1, 5), intArrayOf(0, 7, 2), denseThreshold = 0),
            IntLongMap.build(intArrayOf(5, -1, 5), longArrayOf(0, 7, 2)),
            IntLongMap.build(intArrayOf(5, -1, 5), longArrayOf(0, 7, 2), denseThreshold = 0),
        )

        for (map in maps) {
            val keys = map.sortedKeys()

            assertContentEquals(intArrayOf(-1, 5), keys)
            keys.fill(0)
            assertContentEquals(intArrayOf(-1, 5), map.sortedKeys())
        }
    }
}
