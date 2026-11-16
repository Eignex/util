package com.eignex.util.collections.storage

import com.eignex.util.collections.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CollectionStorageTest {
    @Test
    fun `empty sets exclude shared storage and release drops owned capacity`() {
        val ints = IntHashSet()
        val longs = LongHashSet()
        assertEquals(0L, ints.primitiveCapacityBytes)
        assertEquals(0L, longs.primitiveCapacityBytes)
        ints.add(1)
        longs.add(1L)
        val bytes = longs.primitiveCapacityBytes
        assertTrue(ints.primitiveCapacityBytes > 0L)
        longs.clear()
        assertEquals(bytes, longs.primitiveCapacityBytes)
        longs.release()
        assertEquals(0L, longs.primitiveCapacityBytes)
    }

    @Test
    fun `borrowed list arrays share live values and must be reacquired after growth`() {
        val ints = IntArrayList(1).apply { add(7) }
        val longs = LongArrayList(1).apply { add(9L) }
        val intStorage = ints.borrowBackingArray()
        val longStorage = longs.borrowBackingArray()
        intStorage[0] = 8
        longStorage[0] = 10L
        assertEquals(8, ints[0])
        assertEquals(10L, longs[0])
        ints.add(11)
        longs.add(12L)
        assertNotSame(intStorage, ints.borrowBackingArray())
        assertNotSame(longStorage, longs.borrowBackingArray())
        assertEquals(8L, ints.primitiveCapacityBytes)
        assertEquals(16L, longs.primitiveCapacityBytes)
    }

    @Test
    fun `object map accounting separates primitive payload reference slots and owned objects`() {
        val value = Any()
        val ints = MutableIntObjectMap<Any?>(0)
        val longs = MutableLongObjectMap<Any?>(0)
        assertEquals(40L, ints.primitiveCapacityBytes)
        assertEquals(72L, longs.primitiveCapacityBytes)
        assertEquals(8, ints.referenceCapacity)
        assertEquals(8, longs.referenceCapacity)
        val original = ints.valueStorageIdentity
        ints.put(0, value)
        ints.put(1, null)
        assertSame(original, ints.valueStorageIdentity)
        val slots = mutableListOf<Any?>()
        ints.forEachValueSlot { slots.add(it) }
        assertEquals(ints.referenceCapacity, slots.size)
        assertEquals(1, slots.count { it === value })
        assertEquals(7, slots.count { it == null })
        repeat(10) { ints.put(it + 2, value) }
        assertNotSame(original, ints.valueStorageIdentity)
        ints.clear()
        ints.forEachValueSlot { assertEquals(null, it) }
        assertFalse(ints.containsKey(0))
        longs.put(0L, value)
        val longSlots = mutableListOf<Any?>()
        longs.forEachValueSlot { longSlots.add(it) }
        assertEquals(1, longSlots.count { it === value })
    }
}
