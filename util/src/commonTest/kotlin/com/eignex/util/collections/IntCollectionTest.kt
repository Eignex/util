package com.eignex.util.collections

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IntCollectionTest {
    @Test
    fun `interface traversal preserves the elements in its snapshot`() {
        val collections = listOf<IntCollection>(
            IntArrayList().apply {
                add(3)
                add(1)
                add(3)
            },
            IntHashSet().apply {
                add(3)
                add(1)
                add(3)
            },
            IntArrayDeque().apply {
                addLast(3)
                addLast(1)
                addLast(3)
            },
            IntSwapSet(4).apply {
                add(3)
                add(1)
            },
        )

        for (collection in collections) {
            val visited = IntArrayList()
            collection.forEach(IntConsumer { visited.add(it) })

            assertContentEquals(collection.toIntArray(), visited.toIntArray())
            assertEquals(collection.size, visited.size)
        }
    }

    @Test
    fun `sorted snapshots preserve membership and repeated values`() {
        val collections = listOf<IntCollection>(
            IntArrayList().apply {
                add(3)
                add(1)
                add(3)
            },
            IntHashSet().apply {
                add(3)
                add(1)
                add(3)
            },
            IntArrayDeque().apply {
                addLast(3)
                addLast(1)
                addLast(3)
            },
            IntSwapSet(4).apply {
                add(3)
                add(1)
            },
        )

        for (collection in collections) {
            val expected = collection.toIntArray().also { it.sort() }

            val sorted = collection.toSortedIntArray()

            assertContentEquals(expected, sorted)
            sorted.fill(0)
            assertTrue(3 in collection)
            assertFalse(2 in collection)
        }
    }

    @Test
    fun `indexed interface defaults handle empty and repeated elements`() {
        for (values in listOf(IntArray(0), intArrayOf(3, 1, 3))) {
            val collection = object : IntIndexedCollection {
                override val size: Int get() = values.size
                override fun get(index: Int): Int = values[index]
            }

            assertContentEquals(values, collection.toIntArray())
            assertEquals(values.isEmpty(), collection.isEmpty())
            assertEquals(values.contains(3), collection.contains(3))
            assertFalse(collection.contains(2))
        }
    }
}
