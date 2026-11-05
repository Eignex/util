package com.eignex.util.collections

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MutableLongObjectMapTest {
    @Test
    fun `growing the map preserves colliding and extreme keys`() {
        val map = MutableLongObjectMap<String>(1)
        val keys = LongArray(64) { Long.MIN_VALUE + it * 64L } + Long.MAX_VALUE
        for (key in keys) map.put(key, key.toString())

        assertEquals(keys.size, map.size)
        for (key in keys) assertEquals(key.toString(), map.getValue(key))
    }

    @Test
    fun `removal preserves other entries in a probe chain`() {
        val map = MutableLongObjectMap<String>(1)
        for (key in 0L..15L) map.put(key, key.toString())

        assertTrue(map.remove(5L))

        assertFalse(map.containsKey(5L))
        for (key in 0L..15L) if (key != 5L) assertEquals(key.toString(), map.getValue(key))
    }

    @Test
    fun `getValue returns a stored null and rejects an absent key`() {
        val map = MutableLongObjectMap<String?>()
        map.put(7L, null)

        assertNull(map.getValue(7L))
        assertTrue(map.containsKey(7L))
        assertFailsWith<NoSuchElementException> { map.getValue(8L) }
    }

    @Test
    fun `getOrPut replaces a stored null`() {
        val map = MutableLongObjectMap<String?>()
        map.put(7L, null)

        val value = map.getOrPut(7L) { "present" }

        assertEquals("present", value)
        assertEquals("present", map.getValue(7L))
        assertEquals(1, map.size)
    }
}
