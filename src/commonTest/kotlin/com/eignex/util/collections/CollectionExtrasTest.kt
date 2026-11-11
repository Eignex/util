package com.eignex.util.collections

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CollectionExtrasTest {
    @Test
    fun `hash collection constructors reject unsupported capacities`() {
        val constructors: List<(Int) -> Any> = listOf(
            { IntHashSet(it) },
            { LongHashSet(it) },
            { MutableIntIntMap(it) },
            { MutableIntLongMap(it) },
            { MutableIntDoubleMap(it) },
            { MutableIntObjectMap<String>(it) },
            { MutableLongIntMap(it) },
            { MutableLongObjectMap<String>(it) },
        )
        for (capacity in listOf(Int.MIN_VALUE, -1, (1 shl 29) + 1, 1 shl 30, Int.MAX_VALUE)) {
            for (construct in constructors) {
                assertFailsWith<IllegalArgumentException>("capacity=$capacity") { construct(capacity) }
            }
        }
    }

    @Test
    fun `table sizing provides half load capacity without overflow`() {
        for ((entries, capacity) in listOf(0 to 8, 4 to 8, 5 to 16, 1000 to 2048, (1 shl 29) to (1 shl 30))) {
            assertEquals(capacity, hashTableCapacity(entries))
        }
        assertFailsWith<IllegalArgumentException> { hashTableCapacity(-1) }
        assertFailsWith<IllegalArgumentException> { hashTableCapacity((1 shl 29) + 1) }
    }

    @Test
    fun `toSortedIntArray should return an ascending snapshot`() {
        val list = IntArrayList().apply {
            add(3)
            add(1)
            add(2)
            add(1)
        }
        assertContentEquals(intArrayOf(1, 1, 2, 3), list.toSortedIntArray())
        val set = IntHashSet().apply {
            add(9)
            add(-4)
            add(0)
        }
        assertContentEquals(intArrayOf(-4, 0, 9), set.toSortedIntArray())
    }

    @Test
    fun `toSortedLongArray should return an ascending snapshot`() {
        val list = LongArrayList().apply {
            add(3L)
            add(1L)
        }
        assertContentEquals(longArrayOf(1L, 3L), list.toSortedLongArray())
        val set = LongHashSet().apply {
            add(9L)
            add(-4L)
        }
        assertContentEquals(longArrayOf(-4L, 9L), set.toSortedLongArray())
    }

    @Test
    fun `sortedKeys should return the keys ascending for every map flavour`() {
        val ii = MutableIntIntMap().apply {
            put(5, 0)
            put(-1, 0)
            put(3, 0)
        }
        assertContentEquals(intArrayOf(-1, 3, 5), ii.sortedKeys())
        val il = MutableIntLongMap().apply {
            put(5, 0L)
            put(-1, 0L)
        }
        assertContentEquals(intArrayOf(-1, 5), il.sortedKeys())
        val id = MutableIntDoubleMap().apply {
            put(5, 0.0)
            put(-1, 0.0)
        }
        assertContentEquals(intArrayOf(-1, 5), id.sortedKeys())
        val io = MutableIntObjectMap<String>().apply {
            put(5, "a")
            put(-1, "b")
        }
        assertContentEquals(intArrayOf(-1, 5), io.sortedKeys())
    }
}
