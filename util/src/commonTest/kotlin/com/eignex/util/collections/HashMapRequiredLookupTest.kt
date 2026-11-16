package com.eignex.util.collections

import com.eignex.util.hash.fibonacciHash
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class HashMapRequiredLookupTest {
    @Test
    fun `required lookup should distinguish null payloads from missing colliding keys`() {
        val collisions = (1..8192).filter { fibonacciHash(it) and 63 == 63 }.take(25)
        val wideCollisions = (1..8192).map { it.toLong() shl 32 }
            .filter { fibonacciHash(it) and 63 == 63 }.take(25)
        val keys = listOf(Int.MIN_VALUE, Int.MAX_VALUE, 0) + collisions.take(24)
        val wideKeys = listOf(Long.MIN_VALUE, Long.MAX_VALUE, 0L) + wideCollisions.take(24)
        for (capacity in listOf(1, 64)) {
            val ints = MutableIntObjectMap<String?>(capacity)
            val longs = MutableLongObjectMap<String?>(capacity)
            assertFailsWith<NoSuchElementException> { ints.getValue(0) }
            assertFailsWith<NoSuchElementException> { longs.getValue(0L) }
            for (i in keys.indices) {
                val value = if (i % 3 == 0) null else "value-$i"
                ints.put(keys[i], value)
                longs.put(wideKeys[i], value)
            }
            for (i in keys.indices) {
                val value = if (i % 3 == 0) null else "value-$i"
                assertEquals(value, ints.getValue(keys[i]))
                assertEquals(value, longs.getValue(wideKeys[i]))
            }
            assertFailsWith<NoSuchElementException> { ints.getValue(collisions.last()) }
            assertFailsWith<NoSuchElementException> { longs.getValue(wideCollisions.last()) }
            for (i in keys.indices step 3) {
                assertTrue(ints.remove(keys[i]))
                assertTrue(longs.remove(wideKeys[i]))
            }
            for (i in keys.indices) {
                if (i % 3 == 0) {
                    assertFailsWith<NoSuchElementException> { ints.getValue(keys[i]) }
                    assertFailsWith<NoSuchElementException> { longs.getValue(wideKeys[i]) }
                } else {
                    assertEquals("value-$i", ints.getValue(keys[i]))
                    assertEquals("value-$i", longs.getValue(wideKeys[i]))
                }
            }
            ints.clear()
            longs.clear()
            for (i in keys.indices) {
                assertFailsWith<NoSuchElementException> { ints.getValue(keys[i]) }
                assertFailsWith<NoSuchElementException> { longs.getValue(wideKeys[i]) }
            }
        }
    }
}
