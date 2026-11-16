package com.eignex.util.collections

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IntDisjointSetTest {

    @Test
    fun `groups should handle empty singleton and single group partitions`() {
        assertTrue(IntDisjointSet(0).groups().isEmpty())
        val ds = IntDisjointSet(257)
        assertEquals((0..256).map { listOf(it) }, ds.groups().map { it.toList() })
        for (x in 0 until 256) ds.union(x, x + 1)
        assertContentEquals(IntArray(257) { it }, ds.groups().single())
        assertContentEquals(IntArray(257) { it }, ds.groups().single())
    }

    @Test
    fun `groups should order by first member regardless of root and return independent arrays`() {
        val ds = IntDisjointSet(6)
        ds.union(0, 5)
        ds.union(2, 4)
        ds.union(4, 3)
        val expected = listOf(listOf(0, 5), listOf(1), listOf(2, 3, 4))
        val snapshot = ds.groups()
        assertEquals(expected, snapshot.map { it.toList() })
        snapshot[0][0] = -1
        assertEquals(expected, ds.groups().map { it.toList() })
        ds.union(5, 1)
        assertEquals(listOf(listOf(0, 1, 5), listOf(2, 3, 4)), ds.groups().map { it.toList() })
    }

    @Test
    fun `union should be transitive across a chain`() {
        val ds = IntDisjointSet(5)
        ds.union(0, 1)
        ds.union(1, 2)
        ds.union(3, 4)
        assertTrue(ds.connected(0, 2))
        assertFalse(ds.connected(2, 3))
        assertTrue(ds.connected(3, 4))
    }

    @Test
    fun `groups should partition all elements into ascending members`() {
        val ds = IntDisjointSet(6)
        ds.union(0, 2)
        ds.union(2, 4)
        ds.union(1, 5)
        val groups = ds.groups().map { it.toList() }.toSet()
        assertEquals(setOf(listOf(0, 2, 4), listOf(1, 5), listOf(3)), groups)
    }
}
