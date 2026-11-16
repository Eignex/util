package com.eignex.util.collections

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HashMapClearTest {
    @Test
    fun `clearing fresh and drained maps should retain storage release references and permit reuse`() {
        val ints = MutableIntIntMap(32)
        val longs = MutableIntLongMap(32)
        val doubles = MutableIntDoubleMap(32)
        val wide = MutableLongIntMap(32)
        val objects = MutableIntObjectMap<Any?>(32)
        val wideObjects = MutableLongObjectMap<Any?>(32)
        val payload = Any()
        val storage = listOf(
            ints.keysInternal, ints.valuesInternal, ints.usedInternal,
            longs.keysInternal, longs.valuesInternal, longs.usedInternal,
            doubles.keysInternal, doubles.valuesInternal, doubles.usedInternal,
            wide.keysInternal, wide.valuesInternal, wide.usedInternal,
            objects.keysInternal, objects.valuesInternal, objects.usedInternal,
            wideObjects.keysInternal, wideObjects.valuesInternal, wideObjects.usedInternal,
        )
        fun fill(key: Int) {
            ints.put(key, 0)
            longs.put(key, Long.MIN_VALUE)
            doubles.put(key, Double.NaN)
            wide.put(key.toLong(), 0)
            objects.put(key, if (key == 0) payload else null)
            wideObjects.put(key.toLong(), if (key == 0) payload else null)
        }
        fun clearAll() {
            ints.clear()
            longs.clear()
            doubles.clear()
            wide.clear()
            objects.clear()
            wideObjects.clear()
        }
        fun assertEmpty() {
            for (size in listOf(ints.size, longs.size, doubles.size, wide.size, objects.size, wideObjects.size)) {
                assertEquals(0, size)
            }
            for (key in listOf(0, Int.MIN_VALUE, Int.MAX_VALUE)) {
                assertFalse(ints.containsKey(key))
                assertFalse(longs.containsKey(key))
                assertFalse(doubles.containsKey(key))
                assertFalse(wide.containsKey(key.toLong()))
                assertFalse(objects.containsKey(key))
                assertFalse(wideObjects.containsKey(key.toLong()))
                assertEquals(73, ints.getOrDefault(key, 73))
                assertEquals(73L, longs.getOrDefault(key, 73L))
                assertEquals(73.0, doubles.getOrDefault(key, 73.0))
                assertEquals(73, wide.getOrDefault(key.toLong(), 73))
                assertNull(objects[key])
                assertNull(wideObjects[key.toLong()])
            }
            for (value in objects.valuesInternal) assertNull(value)
            for (value in wideObjects.valuesInternal) assertNull(value)
            val current = listOf(
                ints.keysInternal, ints.valuesInternal, ints.usedInternal,
                longs.keysInternal, longs.valuesInternal, longs.usedInternal,
                doubles.keysInternal, doubles.valuesInternal, doubles.usedInternal,
                wide.keysInternal, wide.valuesInternal, wide.usedInternal,
                objects.keysInternal, objects.valuesInternal, objects.usedInternal,
                wideObjects.keysInternal, wideObjects.valuesInternal, wideObjects.usedInternal,
            )
            storage.zip(current).forEach { (before, after) -> assertSame(before, after) }
        }
        clearAll()
        assertEmpty()
        fill(0)
        fill(Int.MIN_VALUE)
        for (key in listOf(0, Int.MIN_VALUE)) {
            assertTrue(ints.remove(key))
            assertTrue(longs.remove(key))
            assertTrue(doubles.remove(key))
            assertTrue(wide.remove(key.toLong()))
            assertTrue(objects.remove(key))
            assertTrue(wideObjects.remove(key.toLong()))
        }
        // An empty map reached through deletion must already have released its references.
        assertEmpty()
        clearAll()
        assertEmpty()
        fill(Int.MAX_VALUE)
        clearAll()
        clearAll()
        assertEmpty()
    }
}
