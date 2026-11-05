package com.eignex.util.collections

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LongKeyedTest {
    @Test
    fun `interface traversal visits unique keys regardless of payload`() {
        val maps = listOf<LongKeyed>(
            MutableLongIntMap().apply {
                put(5, 0)
                put(-1, 7)
                put(5, 2)
            },
            MutableLongObjectMap<String?>().apply {
                put(5, null)
                put(-1, "a")
                put(5, null)
            },
        )

        for (map in maps) {
            val keys = LongArrayList()
            map.forEachKey(LongConsumer { keys.add(it) })

            assertEquals(2, map.size)
            assertContentEquals(longArrayOf(-1, 5), keys.toSortedLongArray())
            assertTrue(map.containsKey(5))
            assertFalse(map.containsKey(0))
        }
    }

    @Test
    fun `sorted key snapshots are independent of the map`() {
        val maps = listOf<LongKeyed>(
            MutableLongIntMap().apply {
                put(5, 0)
                put(-1, 7)
                put(5, 2)
            },
            MutableLongObjectMap<String?>().apply {
                put(5, null)
                put(-1, "a")
                put(5, null)
            },
        )

        for (map in maps) {
            val keys = map.sortedKeys()

            assertContentEquals(longArrayOf(-1, 5), keys)
            keys.fill(0)
            assertContentEquals(longArrayOf(-1, 5), map.sortedKeys())
        }
    }
}
