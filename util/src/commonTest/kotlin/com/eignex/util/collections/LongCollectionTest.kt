package com.eignex.util.collections

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LongCollectionTest {
    @Test
    fun `interface traversal preserves the elements in its snapshot`() {
        val collections = listOf<LongCollection>(
            LongArrayList().apply {
                add(3)
                add(1)
                add(3)
            },
            LongHashSet().apply {
                add(3)
                add(1)
                add(3)
            },
        )

        for (collection in collections) {
            val visited = LongArrayList()
            collection.forEach(LongConsumer { visited.add(it) })

            assertContentEquals(collection.toLongArray(), visited.toLongArray())
            assertEquals(collection.size, visited.size)
        }
    }

    @Test
    fun `sorted snapshots preserve membership and repeated values`() {
        val collections = listOf<LongCollection>(
            LongArrayList().apply {
                add(3)
                add(1)
                add(3)
            },
            LongHashSet().apply {
                add(3)
                add(1)
                add(3)
            },
        )

        for (collection in collections) {
            val expected = collection.toLongArray().also { it.sort() }

            val sorted = collection.toSortedLongArray()

            assertContentEquals(expected, sorted)
            sorted.fill(0)
            assertTrue(3 in collection)
            assertFalse(2 in collection)
        }
    }

    @Test
    fun `indexed interface defaults handle empty and repeated elements`() {
        for (values in listOf(LongArray(0), longArrayOf(3L, 1L, 3L))) {
            val collection = object : LongIndexedCollection {
                override val size: Int get() = values.size
                override fun get(index: Int): Long = values[index]
            }

            assertContentEquals(values, collection.toLongArray())
            assertEquals(values.isEmpty(), collection.isEmpty())
            assertEquals(values.contains(3), collection.contains(3))
            assertFalse(collection.contains(2))
        }
    }
}
